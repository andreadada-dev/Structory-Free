#!/usr/bin/env python3
"""Reject direct Bukkit scheduler usage in active Java code.

Structory declares Folia support and routes scheduling through DadaPlatform via
SchedulerUtil. Comments and string/char literals are removed before scanning so
archived/decompiled snippets cannot produce false positives.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

TOKEN = re.compile(
    r"//.*?$|/\*.*?\*/|\"(?:\\.|[^\"\\])*\"|'(?:\\.|[^'\\])*'",
    re.MULTILINE | re.DOTALL,
)

FORBIDDEN = {
    "Bukkit.getScheduler()": re.compile(r"\bBukkit\s*\.\s*getScheduler\s*\("),
    "Server#getScheduler()": re.compile(r"\.\s*getScheduler\s*\(\s*\)\s*\.\s*(?:runTask|runTaskLater|runTaskTimer|scheduleSync)"),
    "BukkitRunnable": re.compile(r"\bBukkitRunnable\b"),
}


def active_source(text: str) -> str:
    return TOKEN.sub("", text)


def scan(root: Path) -> list[str]:
    failures: list[str] = []
    if not root.exists():
        return failures

    for path in root.rglob("*.java"):
        source = active_source(path.read_text(encoding="utf-8", errors="replace"))
        for label, pattern in FORBIDDEN.items():
            if pattern.search(source):
                failures.append(f"{path}: direct scheduler usage ({label})")
    return failures


def main() -> int:
    roots = [Path(arg) for arg in sys.argv[1:]] or [Path("structory-core/src/main/java"), Path("structory-free/src/main/java")]
    failures: list[str] = []
    for root in roots:
        failures.extend(scan(root))

    if failures:
        print("Folia scheduler guard failed:")
        for failure in failures:
            print(f"  - {failure}")
        print("Use SchedulerUtil/Platform global, region or entity scheduling instead.")
        return 1

    print("Folia scheduler guard passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
