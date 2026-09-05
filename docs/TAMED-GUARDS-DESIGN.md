# Tamed-Mob Guards — design note (Goal 5)

Written before implementation, per the rebuild prompt. Goal 5: convert/tame hostile
mobs into owned "guard" units that reuse the **same** framework as the Iron Golem
guard — `Owner` ownership, `IModuleInventory`, allow/deny via modules, threat modes,
`IPasscodeProtected` loot, and IMC-registered targeting.

## 1. The sharing problem, and the chosen abstraction

The Iron Golem guard is `SecurityGolemEntity extends IronGolem`. A tamed zombie guard
would want to be `... extends Zombie`. Two entities with **different** vanilla parents
can't share a base class — so inheritance is out. This is exactly what ARCHITECTURE.md
§4 anticipated: **share by composition, not inheritance.**

Two pieces:

- **`GuardCore`** — a plain (non-`Entity`) object that holds and implements all the
  *non-synced, parent-independent* guard state + logic: the module inventory
  (`IModuleInventory` read-view + binary effects), the passcode/salt/cooldown
  (`IPasscodeProtected`), the loot inventory, the waypoint route, and the threat/patrol
  helpers. It never touches `Entity` internals directly — it calls back through a small
  `Host` interface for the few things that need the live entity (`level()`, `blockPosition()`,
  the owner, sending a screen packet).

- **`IGuardEntity`** — an interface a guard entity implements. It `extends IOwnable,
  IModuleInventory, IEMPAffected, IPasscodeProtected` and adds `GuardCore guardCore()`
  plus threat-mode/patrol accessors. **Default methods delegate** the `IModuleInventory`
  and `IPasscodeProtected` surface to `guardCore()`, so an implementing entity gets the
  whole SC contract for free and only supplies: its synced entity-data (owner, shut-down,
  threat mode — these **must** stay per-class because `EntityDataAccessor`s are static
  per entity class and carry the client sync), and the `GuardCore` instance.

Why synced state stays on the entity: `Owner`, `ShutDown`, and `ThreatMode` are
`SynchedEntityData` — the mechanism that replicates them to the client is keyed to the
concrete entity class, so they can't live in a shared plain object. Everything else
(modules, passcode, loot, waypoints — bulky and identical between the two) moves into
`GuardCore`, which is the real win.

Result: `SecurityGolemEntity` and the new `TamedGuardEntity` both `implement IGuardEntity`,
both hold one `GuardCore`, and the goals + the IMC target-check are typed to `IGuardEntity`
so they drive either entity unchanged.

## 2. The tamed entity — one type, not one-per-mob

Registering a separate entity type per eligible mob (zombie-guard, skeleton-guard, …) is
a combinatorial mess and breaks with modded mobs. Instead: **one** `TamedGuardEntity
extends Monster` that stores the *source* `EntityType` it was tamed from (synced, for the
renderer) and copies that mob's max-health/attack attributes at taming time.

**Rendering** is the one genuinely cross-version-hard part (the renderer API changed
repeatedly: sprite/pipeline at 1.21.1, the render-state rework at 26.1). Rather than
re-dispatch to each source mob's renderer across 8 divergent render APIs, the tamed guard
renders with a **single stable renderer** that draws the source mob's **spawn-egg-tinted
marker**: it reuses the golem's minimal renderer pattern (no custom model) and shows the
guard at the mob's size, with a nameplate carrying the source mob's name. This keeps the
feature fully functional and version-portable; a per-mob look-alike renderer is a
follow-up that isn't blocked by this design. (The Sentry itself sidesteps arbitrary-mob
rendering the same way — it disguises as a *block*, never a live mob.)

## 3. Taming trigger, eligibility, ownership

- **Trigger:** right-click a hostile mob with any SecurityCraft **Keycard** — the exact
  mechanic that converts an Iron Golem. One consistent verb across both guard kinds. The
  keycard is consumed; the interacting player becomes the `Owner`.
- **Eligible mobs:** anything the golem itself treats as a base threat — `Monster`
  (`Enemy`) that isn't a boss and isn't already a guard. Bosses (`!canChangeDimensions`/
  wither/dragon — checked via a max-health cap + a denylist) are excluded so you can't
  trivially tame a raid boss. Creepers are excluded (consistent with the golem's own
  threat filter). Config-free for now; the eligibility predicate lives in one method so a
  future allow/deny config is a one-line change.
- **Ownership & persistence:** identical to the golem — a synced `Owner` (SC serializer),
  `IOwnable` with team support, saved via `Owner.CODEC`/`owner.save` per era. `GuardCore`
  serializes modules/passcode/loot/waypoints through the entity's `addAdditionalSaveData`/
  `readAdditionalSaveData` (era-guarded), plus the source `EntityType` id.

## 4. Module-governed stats/AI

Same binary-module model as the golem (SC modules don't stack):

- **Harming** → higher attack damage. **Speed** → higher movement speed. **Smart** →
  larger detection radius. **Storage** → loot rows + item pickup. **Allowlist/Denylist**
  → `isAllowed`/`isDenied` targeting exemptions/forces.
- Base attack/health come from the *source mob's* attributes at taming (a tamed
  Vindicator hits harder than a tamed Zombie), then modules add on top. The threat-mode
  state machine (Warn / Follow / Attack), patrol/waypoints, bell-recall, and EMP shutdown
  are the golem's exact behaviors, shared via `GuardCore` + the shared goals.
- Targeting goes through the same `IGuardEntity` predicate that consults
  `SecurityCraftAPI.getRegisteredSentryAttackTargetChecks()`, so a tamed guard and a
  Sentry agree on threats.

## 5. Scope of this pass

**Implemented now:** `IGuardEntity` (the shared contract) + `GuardCore` (the shared,
reusable implementation of the module inventory, binary effects, and passcode loot lock);
a working `TamedGuardEntity` that is a `GuardCore`-backed owned combat guard — tamed from
a hostile mob with a keycard, attack damage/health copied from the source mob and boosted
by modules, allow/deny + threat via modules, EMP shutdown, ownership + persistence, and
IMC-consistent targeting; its entity type + a single stable renderer + the taming
interaction + eligibility; and `SecurityGolemEntity` implementing `IGuardEntity` so the
IMC target-check and eligibility logic are shared. Compiling on all 8 targets, boot-
validated on the newest.

**Deliberate follow-ups (not blockers):** collapsing the golem's *own* (already-validated)
module/passcode internals onto `GuardCore` so the two share one implementation rather than
the golem keeping its inline copy behind the same interface; giving the tamed guard the
golem's full patrol-route + config-GUI surface (this pass ships it as a combat guard with
the passcode-gated loot but not the waypoint editor); the per-source-mob look-alike
renderer; and a taming allow/deny config. Each is mechanical and unblocked by this design.
