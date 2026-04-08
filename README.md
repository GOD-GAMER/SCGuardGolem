# SecurityCraft Guard Golem Addon - MC 1.21.10

A SecurityCraft addon that adds a **Security Guard Golem** entity with patrol routes,
badge-based player detection, configurable combat behavior, upgrades, and player target lists.

| | |
|---|---|
| **Minecraft** | 1.21.10 |
| **NeoForge** | 21.10.63+ |
| **SecurityCraft** | 1.10.1+ |
| **Java** | 21 |

---

## Installation

1. Install **Minecraft 1.21.10** with **NeoForge 21.10.63** or later
2. Install **SecurityCraft 1.10.1** or later for MC 1.21.10
3. Download the JAR from the [Releases](https://github.com/GOD-GAMER/SCGuardGolem/releases) page
4. Place the JAR in your `.minecraft/mods/` folder
5. Launch the game

---

## Features

### Security Guard Golem
- Right-click any vanilla **Iron Golem** with a **SecurityCraft Keycard** to convert it
- The keycard is consumed and you become its owner
- Retains Iron Golem appearance and combat AI but gains new security features

### Patrol System
- Define waypoints for the golem to follow in a loop
- Adjustable patrol speed (0.1x to 3.0x)
- Patrols pause automatically when the golem engages a target

### Threat Detection
- **WARN** - Sends chat warning to untrusted players
- **FOLLOW** - Follows untrusted players without attacking
- **ATTACK** - Attacks untrusted players on sight

### Upgrades (0-5 levels each)
- **Damage** - +3 attack damage per level
- **Speed** - +0.03 movement speed per level
- **Detection** - +4 block detection radius per level (base 16 blocks)

### Player Lists
- **Ignore list** - Players who are never targeted
- **Attack list** - Players who are always targeted regardless of trust

### In-Game Manual
- Book item with complete usage guide (Creative tab: Security Guard Golem)

---

## Commands

All commands require operator permissions (level 2) and target the nearest Security Golem within 32 blocks.

| Command | Description |
|---|---|
| `/scgolem status` | Show full status report |
| `/scgolem setowner` | Claim ownership of nearest golem |
| `/scgolem patrol start` | Start patrol loop |
| `/scgolem patrol stop` | Stop patrol |
| `/scgolem patrol speed <0.1-3.0>` | Set patrol speed |
| `/scgolem patrol waypoint addhere` | Add waypoint at your position |
| `/scgolem patrol waypoint add <x> <y> <z>` | Add waypoint at coordinates |
| `/scgolem patrol waypoint list` | List all waypoints |
| `/scgolem patrol waypoint remove <index>` | Remove waypoint by index |
| `/scgolem patrol waypoint clear` | Clear all waypoints |
| `/scgolem threat warn` | Set threat mode to WARN |
| `/scgolem threat follow` | Set threat mode to FOLLOW |
| `/scgolem threat attack` | Set threat mode to ATTACK |
| `/scgolem upgrade damage <0-5>` | Set damage upgrade level |
| `/scgolem upgrade speed <0-5>` | Set speed upgrade level |
| `/scgolem upgrade detection <0-5>` | Set detection upgrade level |
| `/scgolem list ignore add <name>` | Add player to ignore list |
| `/scgolem list ignore remove <name>` | Remove player from ignore list |
| `/scgolem list attack add <name>` | Add to always-attack list |
| `/scgolem list attack remove <name>` | Remove from always-attack list |
| `/scgolem list show` | Show both player lists |

---

## Building from Source

```bash
git clone -b mc/1.21.10 https://github.com/GOD-GAMER/SCGuardGolem.git
cd SCGuardGolem
# Place SecurityCraft JAR in libs/ folder
./gradlew build
# Output: build/libs/SecurityGolemAddon-1.21.10-SC1.10.1-1.0.0.jar
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
