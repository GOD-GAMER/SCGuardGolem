# SCGuardGolem — Rebuild Architecture

Written 2026-07-12, based on a full read of this repo (all 7 `mc/*` branches + tags) and of
SecurityCraft (branches `1.20.1`, `eol/1.20.4`, `1.21.1`, `eol/1.21.8`, `eol/1.21.10`,
`1.21.11`, `26.1`, `26.2`). Every version number cited here was verified against a published
artifact — sources are listed inline. Companion file: [`versions.matrix.toml`](versions.matrix.toml).

---

## 1. Current state (what the audit actually found)

The rebuild prompt's five problem statements all check out, and two are worse than described:

- **Branch drift is not hypothetical — it has already shipped broken jars.** The `mc/1.21.10`
  and `mc/1.21.11` branches (and their `v1.2.0`+ release tags) carry **26.1** source and
  metadata: `gradle.properties` says `mcVersion=26.1`, and the shipped `neoforge.mods.toml`
  declares `neoforge "[26,)"` / `minecraft "[26.1,)"`. A real 1.21.11 install runs NeoForge
  21.11.x on MC 1.21.11 — both fail those ranges, so **the current "1.21.10" and "1.21.11"
  releases cannot load on the versions they're named for.** Three of the seven branches
  (1.21.10, 1.21.11, 26.1) are byte-identical today.
- **The SecurityCraft pins are stale.** Branches for 1.20.1/1.20.4/1.21.1 pin SC 1.9.11
  (Oct 2024). SecurityCraft published **v1.10.2.1 for every still-supported MC version —
  including 1.20.1 — on 2026-06-28**. The README's "1.9.x below 1.21.8 / 1.10.x above" split
  no longer exists upstream. Only 1.20.4 is frozen (SC branch archived as `eol/1.20.4`, last
  file v1.10.1).
- **Reinvented systems** (details in §4): owner is two `String` entity-data fields; modules are a
  `SimpleContainer` where *stack count 1–5 = upgrade level*; allow/deny lists are
  `TreeSet<String>` synced as `"\0"`-joined strings; the loot lock is a **plaintext password in
  NBT** (`LootPassword`); targeting is a hardcoded `instanceof Enemy && !(instanceof Creeper)`
  filter; the "manual" is 164 lines of hardcoded `Component.literal` book text.
- **148 hardcoded user-facing string sites** (`Component.literal` / `§` codes) vs 14 lang keys.
- **Hygiene:** `main` tracks 92 `build/` files; the `mc/*` branches track stray helper scripts
  (`_publish_all.py`, `_release_gh.py`, `_update_cf_desc.py`, `build_out.txt`,
  `entity_methods.txt`). SecurityCraft dev jars are vendored in `libs/` and consumed via
  `flatDir` + `compileOnly(fileTree(...))`.

Total addon source is small — **16 Java files, ~2,600 lines** — which makes a from-structure
rebuild cheap relative to the cost of untangling seven branches.

---

## 2. Multi-version tooling: Stonecutter + ModDevGradle (+ MDG Legacy for 1.20.1)

**Chosen: [Stonecutter](https://stonecutter.kikugie.dev/) 0.9.6 as the version switcher, with
[ModDevGradle](https://github.com/neoforged/ModDevGradle) 2.0.141 per target (plugin
`net.neoforged.moddev` for the seven NeoForge targets, `net.neoforged.moddev.legacyforge` for
1.20.1/Forge).** Versions verified on the Gradle Plugin Portal 2026-07-12 (Stonecutter 0.9.6,
2026-06-15; MDG 2.0.141, 2026-03-22).

Why this and not the alternatives:

- **Architectury — rejected.** Architectury solves *cross-loader* (Fabric ⟷ (Neo)Forge) for a
  single MC version; its transformer/injectables machinery is dead weight here because there is
  no Fabric target and never will be one for a SecurityCraft addon (SecurityCraft is
  NeoForge-only across this range, except 1.20.1/Forge). It does not solve *multi-version* at
  all — we'd still need Stonecutter or branches on top. Architectury Loom 1.17 is alive
  (July 2026), but it's the wrong axis.
- **Plain MDG with hand-rolled per-version subprojects — rejected.** Feasible (it's roughly what
  Stonecutter generates), but we'd be maintaining our own comment-preprocessor, version-switched
  source sets, and `chiseled`-style aggregate tasks. Stonecutter is exactly that machinery,
  maintained by someone else.
- **Stonecutter + MDG — chosen.** Stonecutter is build-plugin-agnostic: it manages one source
  tree with per-version preprocessing comments (`//? if <1.21.11 { ... }`) and spawns one Gradle
  subproject per matrix row; each subproject applies MDG exactly the way SecurityCraft itself
  does. MDG is what SecurityCraft uses on every NeoForge branch (2.0.76 → 2.0.141), so run
  configs, parchment wiring, and AT handling stay aligned with upstream, and 2.0.141 is proven
  against every target line including 26.2. For 1.20.1, MDG-Legacy ("mods using the Forge
  platform, up to 1.20.1" — its stated purpose) keeps the whole matrix in one plugin family
  instead of dragging ForgeGradle 6 in for one row.

Risk note: MDG-Legacy needs its remapping configuration
(`obfuscation.createRemappingConfiguration`) for the SecurityCraft dependency on 1.20.1, because
Forge 1.20.1 release jars are SRG-obfuscated (SC's own 1.20.1 branch wraps every mod dep in
`fg.deobf(...)` for the same reason). This is contained build-script complexity on one target;
see §8 for the full 1.20.1 cost assessment.

**Repo shape** (single `main` branch):

```
main
├── ARCHITECTURE.md, README.md, versions.matrix.toml
├── stonecutter.gradle.kts          # targets generated FROM versions.matrix.toml
├── build.gradle.kts                # shared build; applies MDG / MDG-legacy per target
├── src/main/java/...               # ONE source tree, stonecutter comments at boundaries
├── src/main/resources/
│   └── META-INF/neoforge.mods.toml # ONE template, ${...} tokens (Forge/20.4 rows emit mods.toml)
└── .github/workflows/ci.yml        # compile+load+run matrix, one job per target
```

The seven `mc/*` branches become read-only history after migration (§9).

---

## 3. Target set — 8 targets, and the 1.21.11/26.1 answer

| target | loader | Java | build vs | SC (published) |
|---|---|---|---|---|
| 1.20.1 | Forge 47.4.20 | 17 | MDG-legacy | v1.10.2.1 |
| 1.20.4 | NeoForge 20.4.251 | 17 | MDG | v1.10.1 (upstream EOL) |
| 1.21.1 | NeoForge 21.1.206 | 21 | MDG | v1.10.2.1 |
| 1.21.8 | NeoForge 21.8.53 | 21 | MDG | v1.10.2.1 |
| 1.21.10 | NeoForge 21.10.63 | 21 | MDG | v1.10.2.1 |
| 1.21.11 | NeoForge 21.11.42 | 21 | MDG | v1.10.2.1 |
| 26.1 | NeoForge 26.1.2.43-beta | 25 | MDG | v1.10.2.1 |
| 26.2 *(new)* | NeoForge 26.2.0.7-beta | 25 | MDG | v1.10.2.1-beta1 |

**Do 1.21.11 and 26.1 collapse into one build? No — keep them distinct targets.** The prompt's
premise ("their source was identical") is true but misleading: they were identical because the
26.1 code was *pasted over* the 1.21.11 branch, which is precisely what broke the shipped
1.21.11 build (§1). They compile against different MC/NeoForge artifact sets, declare different
dependency ranges, and SecurityCraft publishes a separate file for each. A single jar spanning
both (`minecraft [1.21.11,26.2)`, compiled against 1.21.11, boot-tested on both) is *technically*
defensible post-Identifier-rename, but it saves nothing once the source is shared — an extra
target is one matrix row — and it complicates the "lower bound = compiled against" rule. Revisit
only if release-publishing overhead ever matters.

**Java for the newest line: 25, but only for 26.x.** The prompt asked whether 1.21.11 needs
Java 25 — no: SecurityCraft's own 1.21.11 branch builds with toolchain **21**; 26.1 and 26.2
build with **25**. (Verified from SC's per-branch build files.)

**Range strategy** (mechanized in the matrix file):
- `securitycraft`: lower bound = the exact version compiled against (v1.10.2.1 / v1.10.1 /
  v1.10.2.1-beta1 per row), upper capped at `1.11` (next SC minor). The old README-derived caps
  (`[1.9,1.10)` for older lines) are obsolete — upstream moved.
- `minecraft`: capped at the next target boundary, because the boundaries in §5 are *known*
  breaks — that's why they're separate targets. Only 26.2 is open-ended.
- `neoforge`/`forge`: lower bound = compiled-against; upper open (the MC cap already fences the
  line, since each NeoForge major tracks one MC version).

---

## 4. SecurityCraft API surfaces we will depend on (replace, don't reinvent)

Everything below was read from SecurityCraft source on the current branches; the golem should be
built the way `entity/sentry/Sentry.java` is built, with one deliberate exception (modules).

| Addon system today | Replacement | Key facts from SC source |
|---|---|---|
| `OWNER_UUID`/`OWNER_NAME` string entity data | `api.Owner` + `api.IOwnable` | Sentry pattern: `EntityDataAccessor<Owner>` via `Owner.getSerializer()`; save with `Owner.CODEC` (26.x) / `owner.save(tag)` (older). **Must override `onOwnerChanged` — the interface default casts `this` to `BlockEntity`** (IOwnable.java:69), which would crash an entity. `isOwnedBy(...)` already handles SC *teams* via `Owner.isTreatedTheSameAs` → `TeamUtils.areOnSameTeam` — replacing the custom `isOwner` string-compare gives team support for free. |
| 4-slot `SimpleContainer`, stack count = level 1–5 | `api.IModuleInventory` with `acceptedModules() = {HARMING, SPEED, SMART, STORAGE, ALLOWLIST, DENYLIST}` | Unlike Sentry (which predates this and stores its 3 modules ad-hoc), `IModuleInventory` is not BlockEntity-bound — it needs `getInventory()`, `acceptedModules()`, `isModuleEnabled/toggleModuleState`, `myLevel()`, `myPos()`; the two defaults that touch `BlockEntity` (`onModuleInserted/Removed`) are overridable, same shape as the `onOwnerChanged` case. **Gameplay change to sign off on (§10):** SC modules are one-per-type presence toggles, not stackable levels — the 1–5 upgrade-level mechanic becomes binary (module present = full effect), unless we keep a side-channel level. Recommendation: go binary, tune the constants; it's what SC users expect from every other SC device. |
| `TreeSet<String>` ignore/attack lists + `"\0"` sync + GUI Lists tab | ALLOWLIST/DENYLIST module items via `IModuleInventory.isAllowed(entity)` / `isDenied(entity)` | These defaults exist on **every** target: backed by `ListModuleData` components on 1.21.1+, by `ModuleItem.getPlayersFromModule` NBT on 1.20.x — SC absorbs the divergence, our call sites are version-uniform. Both paths include team checks. The GUI Lists tab becomes unnecessary: players edit list modules the SC way (module item screen) and install them. |
| `String lootPassword` in NBT (plaintext) | `api.IPasscodeProtected` | Gives salted+hashed passcodes (`hashAndSetPasscode`, salt via `SaltData`/UUID key), brute-force cooldown, Codebreaker interop (`extends ICodebreakable`), and SC's passcode screens. Note: **`registerPasscodeConvertible` does not apply here** — `IPasscodeConvertible` is block-only (`isUnprotectedBlock(BlockState)`...); the prompt's suggestion works for blocks, not entity conversion. The golem simply implements `IPasscodeProtected` and opens the passcode GUI gating the Loot tab. |
| `instanceof Enemy && !(instanceof Creeper)` + `PlayerThreatGoal` | Sentry-style targeting + IMC | Two directions: (a) our target goal **consumes** `SecurityCraftAPI.getRegisteredSentryAttackTargetChecks()` exactly like `TargetNearestPlayerOrMobGoal.isSupportedTarget` does (also honoring SC's `sentryAttackableEntitiesDenylist` config), so the golem threat-matches the Sentry and respects other addons' registered checks; (b) we **register** our own `IAttackTargetCheck` via `InterModComms.sendTo("securitycraft", SecurityCraftAPI.IMC_SENTRY_ATTACK_TARGET_MSG, ...)` during `InterModEnqueueEvent` so Sentries and golems agree on threats. `IAttackTargetCheck` exists on all 8 targets. |
| Hardcoded written-book manual (`SCGManualItem`, 164 lines) | `SCManualPage` into `SCManualItem.PAGES` | SC populates `PAGES` **server-side at server start** (it needs the `RecipeManager` for recipe displays) and syncs the whole list to clients via the `SendManualPages` packet on login (SCEventHandler:193-195); the client handler clears+replaces the list. SC's own pages come from scanning `@HasManualPage` on `SCContent` fields only — addons add to the public `PAGES` list directly, at the same lifecycle point (ServerStarted, low priority), and ride the existing sync. Delete `SCGManualItem` entirely; the conversion recipe display goes in the page. |
| — (not handled today) | `api.IEMPAffected` | Trivial (`isShutDown/setShutDown` on synced data + `shutDown()` clearing target, per Sentry). Makes the golem respect EMP-style addon devices. Do it. |
| Bare panel GUI (`GolemScreen`, 660 lines, hardcoded strings) | SC screen conventions | Reuse SC's screen idioms: their texture/sprite conventions, `Utils.localize` for every string, option-button patterns. Keep tabs Config / Loot / Route (Lists tab dissolves into module handling, above). |

**Goal 5 (tame hostile mobs) abstraction:** extract the above into a
`GuardEntityCore` composition object (owner data + module inventory + passcode + targeting
predicate) attached to both `SecurityGolemEntity` and a `TamedGuardEntity` family, rather than a
base-class hierarchy — the golem extends `IronGolem` while tamed mobs keep their own parents, so
inheritance can't be shared; composition + a `IGuardEntity` interface (implementing `IOwnable`,
`IModuleInventory`, `IEMPAffected`, `IPasscodeProtected`) can. Design note comes with phase (e),
per the prompt.

---

## 5. Version boundaries (where the single source tree needs guards)

Boundaries verified by diffing SecurityCraft branches and MC APIs; each becomes a Stonecutter
guard site or a small platform-layer class:

| Boundary | Below | Above | Blast radius |
|---|---|---|---|
| **1.21.10 → 1.21.11**: `ResourceLocation` → `Identifier` rename | `ResourceLocation.parse/fromNamespaceAndPath` | `Identifier.*` (SC 1.21.11+ uses it throughout; SC pins an old TOP build as compile-incompatible for exactly this) | Every sprite/texture/registry-key reference; alias in one platform class |
| **1.21.1 → 1.21.8**: NBT rework | `addAdditionalSaveData(CompoundTag)`, manual tag juggling | `ValueInput`/`ValueOutput`, codec-based `tag.store(...)` | Entity persistence — the biggest single divergence in the current code |
| **1.20.4 → 1.21.1**: data components | No `ListModuleData`; module NBT helpers | `ListModuleData` component | Contained *inside* SC's `isAllowed/isDenied` — near-zero for us if we stay on `IModuleInventory` |
| **1.21.1 → 1.21.8**: `SCManualPage` shape | 6-field record (no recipes supplier) | 7-field (`Supplier<Optional<List<RecipeDisplay>>>`) | One constructor call site |
| **1.20.1 → 1.20.4**: loader split | Forge event/registration namespaces (`net.minecraftforge.*`), `NetworkHooks.openScreen`, SRG dev-remapping | NeoForge namespaces, `serverPlayer.openMenu` + `writeClientSideData` | The platform layer; §8 |
| **20.4 → 20.5 (NeoForge)**: metadata rename | `META-INF/mods.toml` (1.20.1 + 1.20.4) | `META-INF/neoforge.mods.toml` (1.21.1+) | Template output path only — the matrix's `metadata_file` per row (the draft matrix's "1.20.4 → neoforge.mods.toml" was wrong; SC 1.20.4 ships `mods.toml`) |
| **1.21.11 → 26.x**: Java 21 → 25, NeoForge version scheme `21.x` → `26.x.y` | | | Toolchain per matrix row; no code impact found |
| Misc MC drift | `EntitySpawnReason` (vs `MobSpawnType` pre-1.21.2-ish), `snapTo` vs `moveTo`, `player.permissions().hasPermission(Permissions...)` vs `player.hasPermissions(int)`, `getGameProfile().id()` vs `.getId()` | | Scattered small guards; enumerate during port (phase b) with the compile rung as the checklist |

APIs that are **stable across all 8 targets** (verified present and same-shaped):
`Owner.getSerializer()`, `IOwnable`, `IModuleInventory` core + `isAllowed/isDenied`,
`ModuleType.{HARMING,SPEED,SMART,STORAGE,ALLOWLIST,DENYLIST}`, `IPasscodeProtected`,
`IAttackTargetCheck` + IMC message names, `SCManualItem.PAGES`, `KeycardItem`,
`WireCuttersItem`, `IEMPAffected`.

---

## 6. Metadata & dependency generation (matrix-driven, no hand-written numbers)

- `versions.matrix.toml` is parsed at Gradle configuration time (TOML via a tiny buildSrc
  helper or `tomlj`); it produces (a) the Stonecutter target list, (b) per-target dependency
  coordinates, (c) the `processResources { expand(...) }` token map.
- **One metadata template** with `${...}` tokens for modId, version, and every
  `versionRange` — mirroring SC's own `processResources` expansion. Rows with
  `metadata_file = "META-INF/mods.toml"` (1.20.1, 1.20.4) emit the template at that path
  (1.20.1 additionally with Forge's `loaderVersion`/LowCodeML header differences handled in the
  template's conditional tokens).
- **SecurityCraft comes from Curse Maven** (`curse.maven:security-craft-64760:<fileId>` —
  resolution verified live) **with Modrinth Maven as the documented fallback**
  (`maven.modrinth:security-craft:<versionId>`); both repos wired. The `libs/` vendored jars are
  deleted — no target needs the fallback today (every row has both a Curse fileId and a Modrinth
  version id, all fetched from the published listings on 2026-07-12).
- The lower bound stamped into metadata is *read from the same matrix key* used to resolve the
  compile dependency. Drift is structurally impossible — which matters, because drift is exactly
  what shipped the broken 1.21.11 build.

## 7. CI: three rungs × eight targets

GitHub Actions, one job per matrix row (fail-fast off, so one bad target doesn't mask others):

1. **COMPILE** — `gradle :<target>:build` against the pinned SC + loader artifacts. Catches API
   breaks (this rung alone would have flagged the Identifier rename and the ValueInput rework).
2. **LOAD** — headless dedicated-server boot (`runServer` with a watchdog that stops after
   mod-load completes, or NeoForge's gametest server task) so the loader validates the declared
   ranges against real artifacts. This is the rung that makes §1's shipped-range bug impossible;
   NeoForge fails with an explicit incompatible-dependency error.
3. **RUN** — a smoke `@GameTest` batch: spawn Iron Golem → convert with keycard → assert
   `SecurityGolemEntity` exists with correct `Owner` → open menu server-side → assert the manual
   page is registered in `SCManualItem.PAGES`. (GUI pixels aren't gametestable; opening the menu
   container + page registration is the right server-side proxy.)

Adding a new MC/SC version = add a matrix row → CI proves it → widen/ship. Java toolchains
(17/21/25) auto-provision via the existing foojay resolver.

## 8. The 1.20.1/Forge target: cost assessment

Kept, with a defined exit. The real costs, enumerated:
1. MDG-Legacy instead of MDG (same plugin family, one row).
2. SRG remapping of the SC dependency in dev (`createRemappingConfiguration`) — build-script
   only.
3. Platform-layer duplication: Forge event bus + `NetworkHooks.openScreen` + Forge registration
   namespaces (~the 550-line divergence the old branches showed, but now isolated in a thin
   layer instead of a whole branch).
4. Oldest API floor everywhere (CompoundTag saves, 6-field `SCManualPage`, no components) —
   though SC's `isAllowed/isDenied` shields the biggest one.

That's bounded and mostly one-time — **recommendation: keep 1.20.1** (it's the only remaining
Forge audience and SC still actively ships v1.10.2.x for it). **Drop trigger:** if during
phase (b) the menu/screen layer needs a third materially-different implementation *just* for
1.20.1 (beyond the Forge/NeoForge open-menu split), or Goal 5's tamed-mob AI needs
per-version entity plumbing that 1.20.1 lacks, cut it in that phase rather than carrying it —
the matrix row and its guards delete cleanly.

## 9. Migration plan from 7 branches

1. **(a) Skeleton** — on `main`: Stonecutter + MDG workspace generated from the matrix; port the
   `mc/26.1` source (newest, currently green) as the baseline; get all 8 targets through CI
   rung 1, then rungs 2–3 with a placeholder gametest. `libs/` jars deleted; `.gitignore`
   fixed; `build/` artifacts and scratch files (`_*.py`, `build_out.txt`, `entity_methods.txt`)
   untracked; the old `mc/*` branches are frozen (kept for history, README pointer, no more
   pushes). Old per-branch tags remain valid release history.
2. **(b) API port** — replace owner/modules/lists/passcode/targeting per §4, with the §5 guard
   sites introduced as each subsystem ports. Suite stays green per-commit via the CI matrix.
   *This phase intentionally changes save data* (owner + modules move to SC formats): ship a
   one-shot NBT upgrader that reads the legacy keys (`GolemOwnerUUID`, `Modules` counts,
   `AllowDenyLists`, `LootPassword` → hash it) and writes the new ones, so existing worlds
   survive. The plaintext-password field is dropped after hashing.
3. **(c) GUI + manual** — SC-style screen reskin; delete `SCGManualItem` + item assets in favor
   of the `SCManualPage` registration; gametest asserts page presence.
4. **(d) Localization** — every `Component.literal`/`§` site (148 today) moves to lang keys;
   expand `en_us.json`; add a CI grep-gate so `Component.literal(` outside dev-only code fails
   the build.
5. **(e) Tamed-mob guards** — short design note first (per prompt), then implement on the
   `GuardEntityCore` composition from §4.

## 10. Flags & open questions for the owner

1. **Module levels become binary** (§4, modules row) — the 1–5 stack-count upgrade mechanic
   doesn't survive the move to real SC modules. OK to go presence-based with retuned constants?
2. **The Lists GUI tab dissolves** into ALLOWLIST/DENYLIST module items (SC-native flow). OK?
3. **Today's 1.21.10/1.21.11 releases are broken on their own versions** (§1). Worth pulling or
   re-tagging those CurseForge files *before* the rebuild lands, since they can't load at all.
4. **26.2 added as a target** (SC's default branch; the addon never shipped it) and **1.20.4 is
   carried as upstream-EOL** (frozen at SC v1.10.1, mods.toml, Java 17) — confirm both.
5. **Unstable-API watch list:** everything on the 26.x line is `-beta` (NeoForge 26.2.0.7-beta,
   SC 1.10.2.1-beta1); `ValueInput/ValueOutput` is still churning upstream; `SCManualPage` has
   grown a field once already (1.21.8) and lives in `misc/`, not `api/` — SC gives no stability
   promise there, same for `SCManualItem.PAGES` and screen internals (`SCManualScreen`,
   sprite atlases). The manual and GUI phases should keep their SC-facing touchpoints in one
   class each so an upstream change is a one-file fix.
