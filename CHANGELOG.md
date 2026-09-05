# Changelog

## 2.0.1
- **Fixed raw text in the Lists tab.** `scguardgolem.gui.lists.allow` / `.deny` were
  referenced by the config screen but never defined, so the Lists tab rendered the raw
  translation keys instead of "Allowlist: installed" / "Denylist: none".
- **New: `tools/verify-assets.js`, wired into CI as Rung 0.** Validates every lang key
  referenced in code, every model parent and texture reference, and every GUI sprite.
  These are client-side failures that a compile and a dedicated-server boot are blind
  to — which is why the above shipped in 2.0.0. Proven to fail against injected
  regressions, so it cannot pass vacuously.

## 2.0.0

### Patrol fixes (this is the one that matters if your golem got stuck)
- **Golem no longer stops forever on a waypoint.** With any dwell time above 0 the
  patrol could stall permanently on the first waypoint. `PatrolGoal` is now an
  explicit TRAVEL/DWELL state machine, so the dwell timer cannot re-arm itself.
- **Dwell time is now the length you set.** Minecraft ticks AI goals on alternate
  ticks unless a goal opts in, so every dwell was silently running twice as long.
- **Waypoints you can't quite stand on no longer deadlock the route.** Arrival is
  measured from the golem's real position and scaled to its width; if a waypoint
  turns out to be unreachable the golem now gives up after 3 seconds and moves to
  the next one instead of re-pathing forever.
- **Bell recall resumes the patrol** instead of parking the golem at waypoint 1
  with patrolling switched off and no way to restart it from the GUI.
- **New: wander radius (0–8 blocks).** While dwelling, the golem can roam around
  the waypoint instead of standing still. Set it in the Route tab or with
  `/scgolem patrol wander <radius>`. 0 (the default) keeps the old stand-still behaviour.
- **Fixed the golem's name.** It was displaying as "Security Golem Configuration"
  in `/summon` output, death messages and name tags, and overrode custom name tags.

### One codebase, every version
- The mod is now built from **a single source tree** covering MC 1.20.1 (Forge) and
  1.20.4 / 1.21.1 / 1.21.8 / 1.21.10 / 1.21.11 / 26.1 / 26.2 (NeoForge), instead of
  seven diverging branches. Every supported version is compiled, boot-tested and
  patrol-tested from the same commit, so a fix can no longer reach one version and
  miss the others (which is exactly what happened to the 1.4.0 patrol fix).

### Native SecurityCraft integration (replaces the addon's parallel systems)
- **Ownership** uses SecurityCraft's own `Owner`/`IOwnable` — so **SC teams now work**.
- **Modules** use `IModuleInventory`: real SC modules, with SC's on/off semantics.
- **Allow/deny** is now driven by SC's Allowlist/Denylist modules.
- **Loot is protected by a real SC passcode** — salted, hashed, Codebreaker-compatible —
  instead of a plaintext password. Existing golems migrate automatically.
- **Targeting** registers through SecurityCraft's IMC sentry-target API, so the golem
  agrees with the Sentry about what counts as a threat.
- **EMP support** (`IEMPAffected`): an EMP shuts a guard down; redstone reactivates it.

### New content
- **EMP Gun** (base + redstone-charged variant) — SecurityCraft ships no EMP of its
  own, so this is the addon's. Reuses SC's taser model with an EMP palette.
- **Tamed Guards** — right-click a hostile mob with a Keycard to convert it into an
  owned guard that takes modules, holds passcode-locked loot, and responds to EMP.
- **In-game guide** rewritten as **8 native SecurityCraft manual pages** instead of a
  hardcoded written book.
- **Animated effects** — EMP detonation with a shockwave, a rising power-up helix,
  threat alerts, and a beam from the EMP gun.
- **New artwork** — redesigned items and icons in each of the six module slots.

### Notes
- Verified on all 8 versions: compiles, boots a dedicated server, and completes a
  multi-waypoint patrol with dwell.
- Interactive surfaces (passcode screens, manual rendering, taming, the config GUI)
  load cleanly but are best confirmed in your own world — please report anything odd.

## 1.4.0
> **Correction:** the "Patrol Dwell Sentinel Fix" listed below shipped **only** in the
> `mc26.1` build. The 1.20.1 / 1.20.4 / 1.21.1 / 1.21.8 / 1.21.10 / 1.21.11 files for
> 1.4.0 were built before that fix landed, so the patrol stall persisted there. Fixed
> for every version in 2.0.0.
- **4-Tab GUI**: Added new "Route" tab for managing patrol waypoints directly from the GUI
- **Patrol Dwell Time**: Set seconds to pause at each waypoint before moving on; adjusted with `[-]`/`[+]` in the Route tab; syncs live to the client via ContainerData
- **Waypoint Manager UI**: View all waypoints with coordinates, remove individual waypoints from the GUI; current waypoint highlighted in green
- **Loot Filter**: Storage module now supports an item filter — when configured, only matching items are collected; empty filter = collect all
- **Patrol Resume After Combat**: Golem resumes patrol from saved waypoint index after losing a target
- **Double-Crouch Route Setup**: Hold Wire Cutters and crouch twice within 1.5s to place a waypoint at your feet — no block required
- **Route Particle Visualizer**: Holding Wire Cutters shows a live particle trail connecting all waypoints of your nearest golem; current target glows orange (flame), others show as white end-rods; a crit-dot marks where the next waypoint will land
- **Waypoint Client Sync Fix**: Waypoints are now serialized into the menu-open packet and hydrated on the client, so the Route tab always shows the correct list immediately on GUI open
- **Patrol Dwell Sentinel Fix**: Patrol goal now uses a sentinel countdown to prevent getting stuck indefinitely on the first waypoint
- **GUI Style Pass**: Panel and button rendering updated to match SecurityCraft's visual style using the `scg_panel` nine-slice sprite
- **Removed reinforced lever requirement**: Waypoints no longer need a reinforced lever item to place
- **Bell recall radius**: Expanded from 48 to 64 blocks
- **In-game manual**: Updated to v1.4 with 10 pages covering the full route workflow, 4-tab GUI, dwell controls, and visualizer

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
