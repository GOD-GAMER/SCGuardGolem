#!/usr/bin/env python3
"""Create GitHub releases for all SCGuardGolem branches."""
import subprocess, sys, glob, os

BRANCHES = ["mc/1.20.1", "mc/1.20.4", "mc/1.21.1", "mc/1.21.8", "mc/1.21.10", "mc/1.21.11", "mc/26.1"]
GRADLE = "gradlew.bat" if sys.platform == "win32" else "./gradlew"

NOTES = """## SC Guard Golem Addon v1.3.0

### New Features
- **3-Tab GUI**: Config, Loot, and Lists tabs accessible via Wire Cutters right-click
- **Scrollable Loot Inventory**: Up to 6 rows with mouse wheel scrolling
- **Allow/Deny Lists**: Entity picker UI for managing player targeting lists
- **Bell Recall**: Ring a bell within 48 blocks to summon nearby owned golems
- **Reinforced Lever Waypoints**: Place reinforced levers to auto-add patrol waypoints
- **4 Module Slots**: Reduced from 6 for cleaner UI layout
- **Sprite-based GUI Rendering**: Flat color panel rendering for all MC versions

Full changelog: https://github.com/GOD-GAMER/SCGuardGolem/blob/{branch}/CHANGELOG.md
"""

def run(cmd):
    print(f"  > {cmd}")
    r = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    if r.returncode != 0:
        print(f"  STDERR: {r.stderr[-1500:]}")
    return r.returncode == 0, r.stdout.strip()

def main():
    results = {}
    for branch in BRANCHES:
        mc = branch.replace("mc/", "")
        tag = f"v1.3.0-mc{mc}"
        print(f"\n{'='*60}\n{branch} -> {tag}\n{'='*60}")

        if not run(f"git checkout {branch}")[0]:
            results[branch] = "FAILED: checkout"
            continue

        # Clean and build
        run(f"{GRADLE} clean --no-daemon")
        if not run(f"{GRADLE} jar --no-daemon")[0]:
            results[branch] = "FAILED: build"
            continue

        # Find the JAR
        jars = glob.glob("build/libs/*.jar")
        if not jars:
            results[branch] = "FAILED: no JAR found"
            continue
        jar = jars[0]
        print(f"  JAR: {jar}")

        # Create tag
        run(f"git tag -d {tag}")  # delete if exists
        run(f"git tag {tag}")
        run(f"git push origin {tag} --force")

        # Create GitHub release
        notes = NOTES.replace("{branch}", branch)
        notes_file = "_release_notes.md"
        with open(notes_file, "w") as f:
            f.write(notes)

        # Delete existing release if any
        run(f'gh release delete {tag} --yes')
        
        ok, _ = run(f'gh release create {tag} "{jar}" --title "v1.3.0 for MC {mc}" --notes-file {notes_file} --target {branch}')
        if ok:
            results[branch] = "SUCCESS"
        else:
            results[branch] = "FAILED: gh release"

    if os.path.exists("_release_notes.md"):
        os.remove("_release_notes.md")

    print(f"\n{'='*60}\nRESULTS\n{'='*60}")
    for b, s in results.items():
        print(f"  {b}: {s}")

if __name__ == "__main__":
    main()
