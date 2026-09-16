#!/usr/bin/env python3
"""Reject scheduler patterns that bypass Structory's Folia abstraction.

Structory routes scheduling through SchedulerUtil/DadaPlatform. Comments and
string/char literals are removed before scanning so archived snippets do not
produce false positives. The compatibility aliases are allowed only inside
SchedulerUtil itself; active plugin code must choose global/region/entity/async
explicitly.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

TOKEN = re.compile(
    r"//.*?$|/\*.*?\*/|\"(?:\\.|[^\"\\])*\"|'(?:\\.|[^'\\])*'",
    re.MULTILINE | re.DOTALL,
)

DIRECT_SCHEDULERS = {
    "Bukkit.getScheduler()": re.compile(r"\bBukkit\s*\.\s*getScheduler\s*\("),
    "Server#getScheduler()": re.compile(r"\.\s*getScheduler\s*\(\s*\)\s*\.\s*(?:runTask|runTaskLater|runTaskTimer|scheduleSync)"),
    "BukkitRunnable": re.compile(r"\bBukkitRunnable\b"),
    "raw CompletableFuture async": re.compile(r"\bCompletableFuture\s*\.\s*(?:runAsync|supplyAsync)\s*\("),
    "raw executor factory": re.compile(r"\bExecutors\s*\.\s*new(?:Cached|Fixed|SingleThread|Scheduled)ThreadPool\s*\("),
}

AMBIGUOUS_STRUCTORY_ALIASES = {
    "SchedulerUtil.sync": re.compile(r"\bSchedulerUtil\s*\.\s*sync\s*\("),
    "SchedulerUtil.safe": re.compile(r"\bSchedulerUtil\s*\.\s*safe\s*\("),
    "SchedulerUtil.bukkitSync": re.compile(r"\bSchedulerUtil\s*\.\s*bukkitSync\s*\("),
    "SchedulerUtil.asyncThenSync": re.compile(r"\bSchedulerUtil\s*\.\s*asyncThenSync\s*\("),
    "SchedulerUtil.syncThenAsync": re.compile(r"\bSchedulerUtil\s*\.\s*syncThenAsync\s*\("),
    "SchedulerUtil.wait*Sync": re.compile(r"\bSchedulerUtil\s*\.\s*(?:waitTimedTickThenSync|waitTickThenSync)\s*\("),
}


def active_source(text: str) -> str:
    return TOKEN.sub("", text)


def scan(root: Path) -> list[str]:
    failures: list[str] = []
    if not root.exists():
        return failures

    for path in root.rglob("*.java"):
        source = active_source(path.read_text(encoding="utf-8", errors="replace"))

        for label, pattern in DIRECT_SCHEDULERS.items():
            if pattern.search(source):
                failures.append(f"{path}: scheduler abstraction bypass ({label})")

        if path.name != "SchedulerUtil.java":
            for label, pattern in AMBIGUOUS_STRUCTORY_ALIASES.items():
                if pattern.search(source):
                    failures.append(
                        f"{path}: ambiguous Folia context ({label}); choose global/region/entity explicitly"
                    )
    return failures


def main() -> int:
    roots = [Path(arg) for arg in sys.argv[1:]] or [
        Path("structory-core/src/main/java"),
        Path("structory-free/src/main/java"),
    ]
    failures: list[str] = []
    for root in roots:
        failures.extend(scan(root))

    if failures:
        print("Folia scheduler guard failed:")
        for failure in failures:
            print(f"  - {failure}")
        print("Use SchedulerUtil global/region/entity/async APIs through DadaPlatform.")
        return 1

    print("Folia scheduler guard passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
