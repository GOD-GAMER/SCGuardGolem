# SecurityCraft Guard Golem Addon - MC 1.20.4

A SecurityCraft addon that adds a **Security Guard Golem** entity with patrol routes,
badge-based player detection, configurable combat behavior, upgrades, and player target lists.

| | |
|---|---|
| **Minecraft** | 1.20.4 |
| **NeoForge** | 20.4.241+ |
| **SecurityCraft** | 1.9.11+ |
| **Java** | 17 |

---

## Installation

1. Install **Minecraft 1.20.4** with **NeoForge 20.4.241** or later
2. Install **SecurityCraft 1.9.11** or later for MC 1.20.4
3. Download the JAR from the [Releases](https://github.com/GOD-GAMER/SCGuardGolem/releases) page
4. Place the JAR in your `.minecraft/mods/` folder
5. Launch the game

---

## Features

### Security Guard Golem
- Right-click any vanilla **Iron Golem** with a **SecurityCraft Keycard** to convert it
- The keycard is consumed and you become its owner

### Patrol System
- Define waypoints for the golem to follow in a loop
- Adjustable patrol speed (0.1x to 3.0x)

### Threat Detection
- **WARN** / **FOLLOW** / **ATTACK** modes

### Upgrades (0-5 levels each)
- **Damage** (+3/lvl) | **Speed** (+0.03/lvl) | **Detection** (+4 blocks/lvl)

### Player Lists
- **Ignore list** - Never targeted | **Attack list** - Always targeted

### In-Game Manual
- Book item with complete usage guide

---

## Commands

All commands require operator permissions (level 2).

| Command | Description |
|---|---|
| `/scgolem status` | Show full status report |
| `/scgolem setowner` | Claim ownership |
| `/scgolem patrol start/stop` | Control patrol |
| `/scgolem patrol speed <0.1-3.0>` | Set patrol speed |
| `/scgolem patrol waypoint addhere/add/remove/clear/list` | Manage waypoints |
| `/scgolem threat warn/follow/attack` | Set threat mode |
| `/scgolem upgrade damage/speed/detection <0-5>` | Set upgrade level |
| `/scgolem list ignore/attack add/remove <name>` | Manage player lists |
| `/scgolem list show` | Show player lists |

---

## Building from Source

```bash
git clone -b mc/1.20.4 https://github.com/GOD-GAMER/SCGuardGolem.git
cd SCGuardGolem
# Place SecurityCraft JAR in libs/ folder
./gradlew build
```

---

## Other Versions

| Minecraft | Branch |
|---|---|
| **1.21.10** | [`mc/1.21.10`](https://github.com/GOD-GAMER/SCGuardGolem/tree/mc/1.21.10) |
| **1.20.4** | [`mc/1.20.4`](https://github.com/GOD-GAMER/SCGuardGolem/tree/mc/1.20.4) |
| **1.20.1** | [`mc/1.20.1`](https://github.com/GOD-GAMER/SCGuardGolem/tree/mc/1.20.1) |

## License

MIT License - see [LICENSE](LICENSE)
