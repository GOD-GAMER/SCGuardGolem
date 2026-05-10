#!/usr/bin/env python3
"""Push description to CurseForge via API."""
import requests

API_TOKEN = "cbe64b99-1d94-4289-be0b-b9c37e9a89ba"
PROJECT_ID = 1511550

DESCRIPTION = r"""# SC Guard Golem Addon

**A Security Craft addon that adds customizable Security Guard Golems to protect your base.**

Convert any Iron Golem into a Security Guard Golem using the **Golem Conversion Kit**. Your golem will patrol waypoints, check for security badges, and attack unauthorized players — all configurable through an intuitive GUI.

---

## Features

### Security Guard Golem
- Patrols custom waypoints around your base
- Checks nearby players for a valid **Security Badge**
- Attacks unauthorized players (configurable allow/deny lists)
- Drops all modules and collected loot on death

### 3-Tab GUI (Wire Cutters)
Right-click your golem with **SC Wire Cutters** to open the configuration screen:

- **Config Tab** — Toggle patrol/threat modes, manage 4 module slots
- **Loot Tab** — Scrollable inventory (up to 6 rows) for collected items
- **Lists Tab** — Entity picker UI to manage player allow/deny targeting lists

### Module System
Insert modules into the 4 module slots to upgrade your golem:

| Module | Effect |
|--------|--------|
| **Harming Module** | Increases attack damage (stack count = level) |
| **Speed Module** | Increases movement speed (stack count = level) |
| **Smart Module** | Improves AI targeting behavior |
| **Storage Module** | Enables loot auto-pickup, extra stacks = more chest rows |
| **Allowlist Module** | Adds player allow list for targeting |
| **Denylist Module** | Adds player deny list for targeting |

### Bell Recall
Ring a **Bell** within 48 blocks to summon all nearby owned golems to your location.

### Reinforced Lever Waypoints
Place **Reinforced Levers** to automatically add patrol waypoints for your golem.

### In-Game Manual
Craft the **SCG Manual** for full documentation on golem setup, modules, and commands.

---

## Requirements
- [Security Craft](https://www.curseforge.com/minecraft/mc-mods/security-craft) (required dependency)

## Supported Versions
- Minecraft 1.20.1 (Forge)
- Minecraft 1.20.4, 1.21.1, 1.21.8, 1.21.10, 1.21.11, 26.1 (NeoForge)
"""

headers = {
    "Accept": "application/json",
    "X-Api-Token": API_TOKEN,
}

# Try the CurseForge API v2 endpoint for updating project description
url = f"https://minecraft.curseforge.com/api/projects/{PROJECT_ID}/description"
r = requests.put(url, headers=headers, json={"description": DESCRIPTION})
print(f"PUT {url}")
print(f"Status: {r.status_code}")
print(f"Response: {r.text[:1000]}")

if r.status_code not in (200, 204):
    # Try alternate API endpoint
    url2 = f"https://api.curseforge.com/v1/mods/{PROJECT_ID}/description"
    r2 = requests.put(url2, headers=headers, json={"description": DESCRIPTION})
    print(f"\nPUT {url2}")
    print(f"Status: {r2.status_code}")
    print(f"Response: {r2.text[:1000]}")

    if r2.status_code not in (200, 204):
        # Try the legacy authors API
        url3 = f"https://authors.curseforge.com/api/projects/{PROJECT_ID}"
        r3 = requests.patch(url3, headers=headers, json={"description": DESCRIPTION})
        print(f"\nPATCH {url3}")
        print(f"Status: {r3.status_code}")
        print(f"Response: {r3.text[:1000]}")
