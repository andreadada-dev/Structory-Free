#!/usr/bin/env python3
from __future__ import annotations

import sys
import xml.etree.ElementTree as ET
from pathlib import Path

NS = {"m": "http://maven.apache.org/POM/4.0.0"}
ALLOW_SNAPSHOT_PROPERTIES = {"paper.version", "spigot.api.version"}
ALLOW_SNAPSHOT_GROUPS = {"io.papermc.paper", "org.spigotmc"}


def text(node):
    return "" if node is None or node.text is None else node.text.strip()


def check_pom(path: Path) -> list[str]:
    root = ET.parse(path).getroot()
    failures: list[str] = []

    version = text(root.find("m:version", NS))
    if not version:
        version = text(root.find("m:parent/m:version", NS))
    if version.endswith("-SNAPSHOT"):
        failures.append(f"{path}: project version is still {version}")

    props = root.find("m:properties", NS)
    if props is not None:
        for child in list(props):
            name = child.tag.split("}")[-1]
            value = text(child)
            if value.endswith("-SNAPSHOT") and name not in ALLOW_SNAPSHOT_PROPERTIES:
                failures.append(f"{path}: property {name}={value}")

    for dep in root.findall(".//m:dependency", NS):
        group = text(dep.find("m:groupId", NS))
        artifact = text(dep.find("m:artifactId", NS))
        version = text(dep.find("m:version", NS))
        scope = text(dep.find("m:scope", NS))
        system_path = text(dep.find("m:systemPath", NS))

        if version.endswith("-SNAPSHOT") and group not in ALLOW_SNAPSHOT_GROUPS:
            failures.append(f"{path}: dependency {group}:{artifact}:{version}")
        if scope == "system" or system_path:
            failures.append(
                f"{path}: dependency {group}:{artifact} uses non-reproducible system scope/path"
            )

    return failures


def main() -> int:
    paths = [Path(arg) for arg in sys.argv[1:]] or [Path("pom.xml")]
    failures: list[str] = []
    for path in paths:
        failures.extend(check_pom(path))

    if failures:
        print("Stable release gate failed. Resolve mutable/non-reproducible coordinates before publishing:")
        for failure in failures:
            print(f"  - {failure}")
        return 1

    print("Stable release dependency gate passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
