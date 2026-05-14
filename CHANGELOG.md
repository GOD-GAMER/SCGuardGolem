# Changelog

## 1.4.0
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
