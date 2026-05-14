# SecurityCraft Guard Golem Addon

A **SecurityCraft** addon that adds a configurable **Security Guard Golem** with patrol routes,
module-based abilities, a 4-tab GUI, loot collection, and player allow/deny lists.

> **This is the root repository.** Each Minecraft version lives on its own branch — see the table below.

---

## Supported Versions

| Minecraft | Mod Loader | SecurityCraft | Mod Version | Branch |
|---|---|---|---|---|
| **26.1** | NeoForge 26.1+ | 1.10.x | 1.4.0 | [`mc/26.1`](https://github.com/GOD-GAMER/SCGuardGolem/tree/mc/26.1) |
| **1.21.11** | NeoForge 21.11+ | 1.10.x | 1.4.0 | [`mc/1.21.11`](https://github.com/GOD-GAMER/SCGuardGolem/tree/mc/1.21.11) |
| **1.21.10** | NeoForge 21.10+ | 1.10.x | 1.4.0 | [`mc/1.21.10`](https://github.com/GOD-GAMER/SCGuardGolem/tree/mc/1.21.10) |
| **1.21.8** | NeoForge 21.8+ | 1.10.x | 1.4.0 | [`mc/1.21.8`](https://github.com/GOD-GAMER/SCGuardGolem/tree/mc/1.21.8) |
| **1.21.1** | NeoForge 21.1+ | 1.9.x | 1.4.0 | [`mc/1.21.1`](https://github.com/GOD-GAMER/SCGuardGolem/tree/mc/1.21.1) |
| **1.20.4** | NeoForge 20.4+ | 1.9.x | 1.4.0 | [`mc/1.20.4`](https://github.com/GOD-GAMER/SCGuardGolem/tree/mc/1.20.4) |
| **1.20.1** | Forge 47+ | 1.9.x | 1.4.0 | [`mc/1.20.1`](https://github.com/GOD-GAMER/SCGuardGolem/tree/mc/1.20.1) |

---

## What''s New in v1.4.0

- **Route Tab** — 4th GUI tab for full waypoint management (add, rename, remove, reorder)
- **Named Waypoints** — custom name per waypoint shown in the Route Tab
- **Dwell Time** — set how long (in ticks) the golem waits at each waypoint
- **Double-Crouch Waypoint Placement** — crouch twice while holding Wire Cutters to drop a waypoint
- **Remove Button** — `[x]` per waypoint row in the Route Tab
- **Waypoint Sync** — full route data synced to client on GUI open

See [CHANGELOG.md](CHANGELOG.md) for the full history.

---

## Features

### Converting a Golem
- Right-click any vanilla **Iron Golem** with a **SecurityCraft Keycard** to convert it
- The keycard is consumed and you become the golem''s owner

### 4-Tab GUI (Wire Cutters)
| Tab | Contents |
|---|---|
| **Config** | Module slots, patrol toggle, threat mode, patrol speed, Clear Route |
| **Loot** | Scrollable loot inventory (up to 6 rows) — requires Storage Module |
| **Lists** | Allow list and Deny list with entity picker |
| **Route** | All waypoints with dwell time controls and `[x]` remove per entry |

### Module Upgrades (4 Slots)
| Module | Effect per Level | Max |
|---|---|---|
| **Harming** | +3 attack damage | 5 |
| **Speed** | +0.03 movement speed | 5 |
| **Smart** | +4 block detection range | 5 |
| **Storage** | +1 loot inventory row | 5 |
| **Allowlist** | Enables allow list | 1 |
| **Denylist** | Enables deny list | 1 |

### Patrol Route System
- **Double-crouch** while holding Wire Cutters to place a waypoint at your position
- Manage waypoints from the **Route tab** — view coords, dwell time, remove entries
- **Clear Route** button in the Config tab removes all waypoints

### Bell Recall
- Right-click a **Bell** within 64 blocks to recall all nearby owned golems

### Loot Collection
- Storage Module enables auto-pickup of nearby dropped items (1-6 rows, scrollable)

---

## Installation

1. Pick the branch for your Minecraft version from the table above
2. Install the matching **Forge** or **NeoForge** loader
3. Install **SecurityCraft** for your MC version
4. Download the JAR from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/scguardgolem) or [GitHub Releases](https://github.com/GOD-GAMER/SCGuardGolem/releases)
5. Place the JAR in your `.minecraft/mods/` folder

---

## Building from Source

```bash
git clone -b mc/1.21.11 https://github.com/GOD-GAMER/SCGuardGolem.git
cd SCGuardGolem
./gradlew build
# Output: build/libs/
```

Replace `mc/1.21.11` with the branch for your target version.

---

## License

MIT License
