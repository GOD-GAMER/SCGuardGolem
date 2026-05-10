#!/usr/bin/env python3
"""Publish SCGuardGolem v1.3.0 to CurseForge for all branches."""
import subprocess, sys

BRANCHES = ["mc/1.20.1", "mc/1.20.4", "mc/1.21.1", "mc/1.21.8", "mc/1.21.10", "mc/1.21.11", "mc/26.1"]
PROJECT_ID = "1511550"
API_TOKEN = "cbe64b99-1d94-4289-be0b-b9c37e9a89ba"

def run(cmd, **kw):
    print(f"  > {cmd}")
    r = subprocess.run(cmd, shell=True, capture_output=True, text=True, **kw)
    if r.returncode != 0:
        print(f"  STDOUT: {r.stdout[-2000:]}")
        print(f"  STDERR: {r.stderr[-2000:]}")
    return r.returncode == 0

def main():
    results = {}
    for branch in BRANCHES:
        print(f"\n{'='*60}\nPublishing {branch}\n{'='*60}")
        
        if not run(f"git checkout {branch}"):
            results[branch] = "FAILED: checkout"
            continue

        # Update curseforgeProjectId in gradle.properties
        with open("gradle.properties", "r") as f:
            content = f.read()
        if "curseforgeProjectId=REPLACE_ME" in content:
            content = content.replace("curseforgeProjectId=REPLACE_ME", f"curseforgeProjectId={PROJECT_ID}")
            with open("gradle.properties", "w") as f:
                f.write(content)
            print("  Updated curseforgeProjectId")

        # Use gradlew.bat on Windows
        gradle_cmd = "gradlew.bat" if sys.platform == "win32" else "./gradlew"
        cmd = f"{gradle_cmd} curseforge -PcurseforgeApiToken={API_TOKEN} --no-daemon --stacktrace"
        
        if run(cmd):
            results[branch] = "SUCCESS"
            # Commit the gradle.properties change
            run("git add gradle.properties")
            run('git commit -m "Set CurseForge project ID for publishing"')
        else:
            results[branch] = "FAILED: curseforge task"

    print(f"\n{'='*60}\nRESULTS\n{'='*60}")
    for branch, status in results.items():
        print(f"  {branch}: {status}")

if __name__ == "__main__":
    main()
