import subprocess, os

os.chdir(r"C:\Users\cabca\Desktop\addon")

branches = {
    "mc/1.20.4": {"mc": "1.20.4", "loader": "NeoForge 20.4.251+", "sc": "1.9.11+", "java": "21+", "loader_short": "NeoForge"},
    "mc/1.21.1":  {"mc": "1.21.1",  "loader": "NeoForge 21.1+",    "sc": "1.9.11+", "java": "21+", "loader_short": "NeoForge"},
    "mc/1.21.8":  {"mc": "1.21.8",  "loader": "NeoForge 21.8+",    "sc": "1.10.1+", "java": "21+", "loader_short": "NeoForge"},
    "mc/1.21.10": {"mc": "1.21.10", "loader": "NeoForge 21.10+",   "sc": "1.10.1+", "java": "21+", "loader_short": "NeoForge"},
    "mc/1.21.11": {"mc": "1.21.11", "loader": "NeoForge 21.11+",   "sc": "1.10.1+", "java": "21+", "loader_short": "NeoForge"},
    "mc/26.1":    {"mc": "26.1",    "loader": "NeoForge 26.1+",     "sc": "1.10.3+", "java": "21+", "loader_short": "NeoForge"},
}

changelog = """# Changelog

## 1.3.0
- **3-Tab GUI**: Config, Loot, and Lists tabs accessible via Wire Cutters right-click
- **Scrollable Loot Inventory**: Up to 6 rows with mouse wheel scrolling
- **Allow/Deny Lists**: Entity picker UI for managing player targeting lists
- **Bell Recall**: Ring a bell within 48 blocks to summon nearby owned golems
- **Reinforced Lever Waypoints**: Place reinforced levers to auto-add patrol waypoints
- **4 Module Slots**: Reduced from 6 for cleaner UI layout
- **Sprite-based GUI Rendering**: Flat color panel rendering for all MC versions
- **Multi-version Support**: Ported to MC 1.20.1, 1.20.4, 1.21.1, 1.21.8, 1.21.10, 1.21.11, 26.1

## 1.2.0
- Wire Cutters GUI: right-click your golem with SC Wire Cutters to open configuration screen
- Module-based upgrades: Harming, Speed, Smart modules (stack count = level)
- Allowlist/Denylist modules for player targeting lists
- Loot collection system with Storage Module (auto-pickup, upgradeable chest rows)
- Camera toggle for golem perspective viewing
- Patrol and threat mode toggleable from GUI buttons
- Modules and loot drop on golem death
- Updated in-game manual with full v1.2 documentation
- Removed old command-based upgrade/list system (use GUI modules instead)

## 1.1.0
- Initial release for MC 26.1
- Security Guard Golem with patrol, badge, threat, and upgrade systems
- Player ignore/attack lists
- In-game manual book
"""

def make_readme(info):
    return f"""# SecurityCraft Guard Golem Addon

A **SecurityCraft** addon that adds a configurable **Security Guard Golem** with patrol routes, module-based abilities, a 3-tab GUI, loot collection, and player allow/deny lists.

| | |
|---|---|
| **Minecraft** | {info['mc']} |
| **Mod Loader** | {info['loader']} |
| **SecurityCraft** | {info['sc']} |
| **Java** | {info['java']} |
| **Mod Version** | 1.3.0 |

---

## Installation

1. Install **Minecraft {info['mc']}** with **{info['loader_short']}**
2. Install **SecurityCraft** {info['sc']}
3. Download the latest JAR from CurseForge or GitHub Releases
4. Place the JAR in your `.minecraft/mods/` folder
5. Launch the game

---

## Features

### Converting a Golem
- Right-click any vanilla **Iron Golem** with a **SecurityCraft Keycard** to convert it
- The keycard is consumed and you become the golem's owner

### 3-Tab GUI (Wire Cutters)
Right-click your golem with **SecurityCraft Wire Cutters** to open the configuration screen:
- **Config Tab** - Toggle patrol, set threat mode (Warn/Follow/Attack), adjust patrol speed
- **Loot Tab** - Scrollable inventory (up to 6 rows), requires Storage Module
- **Lists Tab** - Allow list and Deny list with entity picker

### Module Upgrades (4 Slots)

| Module | Effect per Level | Max |
|---|---|---|
| **Harming** | +3 attack damage | 5 |
| **Speed** | +0.03 movement speed | 5 |
| **Smart** | +4 block detection range | 5 |
| **Storage** | +1 loot inventory row | 5 |
| **Allowlist** | Enables allow list | 1 |
| **Denylist** | Enables deny list | 1 |

Stack count = upgrade level (e.g., 3 Harming modules = level 3).

### Patrol System
- Define waypoints via commands or reinforced lever placement
- Golem patrols waypoints in a loop at configurable speed (0.1x - 3.0x)

### Loot Collection
- With a **Storage Module**, the golem auto-collects dropped items nearby
- Loot inventory scales with module level (1-6 rows, scrollable)

### Bell Recall
- Ring a **Bell** within 48 blocks to recall all nearby owned golems

### Player Lists
- **Allow list** - Players/entities the golem will never target
- **Deny list** - Players/entities the golem will always target

### In-Game Manual
- Craftable book item with a complete usage guide

---

## Commands

All commands require operator permissions (level 2).

| Command | Description |
|---|---|
| `/scgolem status` | Full status report |
| `/scgolem setowner` | Claim ownership |
| `/scgolem patrol start/stop` | Toggle patrol |
| `/scgolem patrol speed <0.1-3.0>` | Set patrol speed |
| `/scgolem patrol waypoint add/addhere/remove/clear/list` | Manage waypoints |
| `/scgolem threat warn/follow/attack` | Set threat mode |

---

## Supported Versions

| Minecraft | Mod Loader | Branch |
|---|---|---|
| **26.1** | NeoForge | `mc/26.1` |
| **1.21.11** | NeoForge | `mc/1.21.11` |
| **1.21.10** | NeoForge | `mc/1.21.10` |
| **1.21.8** | NeoForge | `mc/1.21.8` |
| **1.21.1** | NeoForge | `mc/1.21.1` |
| **1.20.4** | NeoForge | `mc/1.20.4` |
| **1.20.1** | Forge | `mc/1.20.1` |

---

## Building from Source

```
git clone -b mc/{info['mc']} https://github.com/GOD-GAMER/SCGuardGolem.git
cd SCGuardGolem
./gradlew build
```

---

## License

MIT License
"""

new_desc = "A SecurityCraft addon that adds a configurable Security Guard Golem with patrol routes, module-based upgrades, a 3-tab GUI, scrollable loot collection, and player allow/deny lists."

for branch, info in branches.items():
    print(f"\n=== {branch} ===")
    subprocess.run(["git", "checkout", branch], check=True)
    
    # Write README
    with open("README.md", "w", encoding="utf-8") as f:
        f.write(make_readme(info))
    
    # Write CHANGELOG
    with open("CHANGELOG.md", "w", encoding="utf-8") as f:
        f.write(changelog)
    
    # Update mods.toml description (try both filenames)
    for toml_path in ["src/main/resources/META-INF/mods.toml", "src/main/resources/META-INF/neoforge.mods.toml"]:
        if os.path.exists(toml_path):
            with open(toml_path, "r", encoding="utf-8") as f:
                content = f.read()
            # Replace description block
            import re
            content = re.sub(
                r"description\s*=\s*'''[^']*'''",
                f"description = '''\n{new_desc}\n'''",
                content
            )
            with open(toml_path, "w", encoding="utf-8") as f:
                f.write(content)
            print(f"  Updated {toml_path}")
    
    # Commit and push
    subprocess.run(["git", "add", "-A"], check=True)
    result = subprocess.run(["git", "diff", "--cached", "--quiet"])
    if result.returncode != 0:
        subprocess.run(["git", "commit", "-m", "Update README, CHANGELOG, descriptions for v1.3.0 release"], check=True)
        subprocess.run(["git", "push", "--force", "origin", branch], check=True)
        print(f"  Pushed {branch}")
    else:
        print(f"  No changes on {branch}")

print("\n=== ALL DONE ===")
