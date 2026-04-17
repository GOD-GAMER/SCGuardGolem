# SecurityCraft Guard Golem Addon

A **SecurityCraft** addon that adds a configurable **Security Guard Golem** with patrol routes, module-based abilities, a 3-tab GUI, loot collection, and player allow/deny lists.

| | |
|---|---|
| **Minecraft** | 1.21.8 |
| **Mod Loader** | NeoForge 21.8+ |
| **SecurityCraft** | 1.10.1+ |
| **Java** | 21+ |
| **Mod Version** | 1.3.0 |

---

## Installation

1. Install **Minecraft 1.21.8** with **NeoForge**
2. Install **SecurityCraft** 1.10.1+
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
git clone -b mc/1.21.8 https://github.com/GOD-GAMER/SCGuardGolem.git
cd SCGuardGolem
./gradlew build
```

---

## License

MIT License
