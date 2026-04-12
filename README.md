# SecurityCraft Guard Golem Addon - MC 1.20.1

A SecurityCraft addon that adds a **Security Guard Golem** entity with patrol routes,
badge-based player detection, configurable combat behavior, upgrades, and player target lists.

| | |
|---|---|
| **Minecraft** | 1.20.1 |
| **Forge** | 47.1.3+ |
| **SecurityCraft** | 1.9.8+ |
| **Java** | 17 |

---

## Installation

1. Install **Minecraft 1.20.1** with **Forge 47.1.3** or later
2. Install **SecurityCraft 1.9.8** or later for MC 1.20.1
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
git clone -b mc/1.20.1 https://github.com/GOD-GAMER/SCGuardGolem.git
cd SCGuardGolem
# Place SecurityCraft JAR in libs/ folder
./gradlew build
```

---

## Other Versions

| Minecraft | Mod Loader | Branch | SC Version |
|---|---|---|---|
| **26.1** | NeoForge 26.1 | [`mc/26.1`](https://github.com/GOD-GAMER/SCGuardGolem/tree/mc/26.1) | 1.10.1-beta2 |
| **1.21.11** | NeoForge 21.11 | [`mc/1.21.11`](https://github.com/GOD-GAMER/SCGuardGolem/tree/mc/1.21.11) | 1.10.1-beta2 |
| **1.21.10** | NeoForge 21.10 | [`mc/1.21.10`](https://github.com/GOD-GAMER/SCGuardGolem/tree/mc/1.21.10) | 1.10.1 |
| **1.21.8** | NeoForge 21.8 | [`mc/1.21.8`](https://github.com/GOD-GAMER/SCGuardGolem/tree/mc/1.21.8) | 1.10.1 |
| **1.21.1** | NeoForge 21.1 | [`mc/1.21.1`](https://github.com/GOD-GAMER/SCGuardGolem/tree/mc/1.21.1) | 1.10.1 |
| **1.20.4** | NeoForge 20.4 | [`mc/1.20.4`](https://github.com/GOD-GAMER/SCGuardGolem/tree/mc/1.20.4) | 1.9.8+ |
| **1.20.1** | Forge 47.1 | [`mc/1.20.1`](https://github.com/GOD-GAMER/SCGuardGolem/tree/mc/1.20.1) | 1.9.8+ |

## License

MIT License - see [LICENSE](LICENSE)

---

## For Developers — CurseForge Publishing Setup

Each branch includes a `curseforge` Gradle task and a GitHub Actions release workflow.
To enable automatic CurseForge publishing when you create a GitHub release:

1. **Create a CurseForge project** at [authors.curseforge.com](https://authors.curseforge.com)
2. **Get your API token** from [Account ? API Tokens](https://authors.curseforge.com/account/api-tokens)
3. **Add the token as a GitHub secret**:
   - Go to your repo ? Settings ? Secrets and variables ? Actions
   - Create a new secret named `CURSEFORGE_TOKEN` with your API token
4. **Add the project ID as a GitHub variable**:
   - In the same Settings ? Secrets and variables ? Actions ? Variables tab
   - Create a new variable named `CURSEFORGE_PROJECT_ID` with your numeric project ID
5. **Create a release** on the appropriate branch — the workflow will automatically build,
   attach the JAR to the GitHub release, and publish to CurseForge

You can also publish manually from any branch:
```bash
./gradlew curseforge -PcurseforgeProjectId=YOUR_ID -PcurseforgeApiToken=YOUR_TOKEN
```
