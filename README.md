# SecurityCraft Guard Golem Addon

A **SecurityCraft** addon that adds a configurable **Security Guard Golem** with patrol routes, dwell time, a 4-tab GUI, loot collection, route particle visualization, and player allow/deny lists.

| | |
|---|---|
| **Minecraft** | See table below |
| **Mod Loader** | NeoForge (Forge for 1.20.1) |
| **SecurityCraft** | 1.9.x – 1.10.x depending on MC version |
| **Java** | 21+ |
| **Mod Version** | 1.4.0 |

---

## Installation

1. Install **Minecraft** + **NeoForge** (or Forge for 1.20.1)
2. Install **SecurityCraft** for your MC version
3. Download the JAR for your version from [CurseForge](https://www.curseforge.com) or [GitHub Releases](https://github.com/GOD-GAMER/SCGuardGolem/releases)
4. Place the JAR in your `.minecraft/mods/` folder
5. Launch the game

---

## Features

### Converting a Golem
- Right-click any vanilla **Iron Golem** with a **SecurityCraft Keycard** to convert it
- The keycard is consumed and you become the golem's owner

### 4-Tab GUI (Wire Cutters)
Right-click your golem with **SecurityCraft Wire Cutters** to open the 4-tab configuration screen:

| Tab | Contents |
|---|---|
| **Config** | Module slots, patrol toggle, threat mode, patrol speed, Clear Route button |
| **Loot** | Scrollable loot inventory (up to 6 rows), loot filter — requires Storage Module |
| **Lists** | Allow list and Deny list with entity picker |
| **Route** | All waypoints with coordinates, dwell time `[-]`/`[+]` controls, per-waypoint `[x]` remove |

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

### Patrol Route System
- Hold **Wire Cutters** and **crouch twice within 1.5 seconds** to place a waypoint at your feet
- The nearest owned golem (within 64 blocks) receives the waypoint
- A burst of happy-villager particles confirms placement
- Manage all waypoints from the **Route tab** — view coordinates, remove entries, set dwell time
- Use **Clear Route** in the Config tab to delete all waypoints at once

### Route Particle Visualizer
- While holding **Wire Cutters**, a live particle trail shows your nearest golem's full route:
  - 🟠 **Orange flame** — current patrol target
  - ⚪ **White end-rods** — other waypoints and connecting lines
  - ✦ **Crit dot** — marks where your next waypoint will land

### Dwell Time
- Set how long (in seconds) the golem pauses at each waypoint before moving on
- Adjusted with `[-]` / `[+]` in the Route tab — value syncs live to the client

### Loot Collection
- With a **Storage Module**, the golem auto-collects dropped items nearby
- Loot inventory scales with module level (1–6 rows, scrollable)
- Optional **loot filter**: when configured, only listed item types are collected

### Bell Recall
- Right-click a **Bell** within 64 blocks to recall all nearby owned golems to their first waypoint
- Golems stop patrolling and wait; use the **Patrol** button to restart

### Player Lists
- **Allow list** — players/entities the golem will never target
- **Deny list** — players/entities the golem will always target

### In-Game Manual
- Craftable book item with a 10-page usage guide updated for v1.4

---

## Commands

All commands require operator permissions (level 2).

| Command | Description |
|---|---|
| `/scgolem status` | Full status report |
| `/scgolem setowner` | Claim ownership of nearest golem |
| `/scgolem patrol start/stop` | Toggle patrol |
| `/scgolem patrol speed <0.1-3.0>` | Set patrol speed multiplier |
| `/scgolem patrol waypoint add/addhere/remove/clear/list` | Manage waypoints |
| `/scgolem threat warn/follow/attack` | Set threat mode |

---

## Supported Versions

| Minecraft | Mod Loader | SecurityCraft | Branch |
|---|---|---|---|
| **26.1** | NeoForge 26.1+ | 1.10.x | `mc/26.1` |
| **1.21.11** | NeoForge 21.11+ | 1.10.x | `mc/1.21.11` |
| **1.21.10** | NeoForge 21.8+ | 1.10.x | `mc/1.21.10` |
| **1.21.8** | NeoForge 21.8+ | 1.10.x | `mc/1.21.8` |
| **1.21.1** | NeoForge 21.1+ | 1.9.x | `mc/1.21.1` |
| **1.20.4** | NeoForge 20.4+ | 1.9.x | `mc/1.20.4` |
| **1.20.1** | Forge 47+ | 1.9.x | `mc/1.20.1` |

---

## Building from Source

```bash
# Clone the branch for your target MC version
git clone -b mc/26.1 https://github.com/GOD-GAMER/SCGuardGolem.git
cd SCGuardGolem
./gradlew build
# Output JAR: build/libs/
```

---

## License

MIT License
