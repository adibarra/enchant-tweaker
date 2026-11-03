# context.md — working context for the Enchant Tweaker 26.2 port

> **Purpose.** A compact, selectively readable context pack so a future session (or a delegate)
> can pick up this work without redoing research. Read only the section(s) you need; each is
> self-contained and cross-references others by ID.
>
> **Status snapshot:** 2026-11-03 19:00 (Asia/Calcutta).
> Research + plan are complete and written (`PLAN.md`, 1,052 lines). No port code has been written yet.
> Working tree is clean except for the new untracked `PLAN.md`, `context.md` and `.plan/`.

## Index — read what you need

| ID | Section | Read this when… |
|---|---|---|
| C0 | TL;DR and next actions | you just need to know what to do next |
| C1 | Mission and constraints | you need the exact ask / scope boundaries |
| C2 | Environment, toolchain and artefacts | you need JDKs, paths, downloaded jars, build state |
| C3 | Repository baseline | you need file/LOC/config/mixin/test inventory |
| C4 | Target platform facts (verified) | you need exact versions and their evidence |
| C5 | Verified 26.2 API facts | you are writing code and need the new names/hooks |
| C6 | Decisions already made | you are about to choose an approach |
| C7 | Risks and open questions | you are planning a milestone |
| C8 | Artefacts, provenance, delegation log | you wonder "where did this come from" |
| C9 | Command cheat sheet | you want to run something |
| C10 | Gotchas and lessons | something failed and you want the known traps |
| C11 | Quick-start checklist | you are resuming implementation |

---

## C0. TL;DR and next actions

1. The port target is **Minecraft 26.2** on Fabric Loader (Java 25, no mappings, Mojang names).
2. Research is done; `PLAN.md` at the repo root is the authoritative plan (phases M0–M6, 42-mixin
   matrix, test strategy, risk register).
3. Nothing has been ported yet. Start at **PLAN.md §5 M0**:
   `git tag pre-26.2`, add `.plan/` to `.gitignore`, confirm baseline build.
4. Then **M1** (Yarn → Mojang at 1.20.5 via `migrateMappings`), then **M2** (bump to 26.2).
5. Do not skip the ordering: migrating names first keeps the diff reviewable and is the only way to
   use Loom's migrator (Yarn does not exist for 26.x).
6. Delegation is currently blocked (agy quota exhausted, see C8). Work directly until it resets.

---

## C1. Mission and constraints

- Repo: https://github.com/adibarra/enchant-tweaker — a Fabric mod with 139 user-facing settings that
  tweaks enchantment/anvil/grindstone/mending/trade mechanics while keeping vanilla feel.
- Ask: "completely port this to Minecraft 26.2 for Fabric loader; investigate how it can be done and
  tested; research skills/MCPs; use delegation to plan and review with Argon High (fallback Opus 5.5
  High); plan it out completely; write PLAN.md."
- Constraints/decisions already taken:
  - Target 26.2 (not 26.3), Fabric only.
  - Preserve all behaviour and the config contract (currently 139 options / 140 entries).
  - Drop Quilt support (no 26.x support expected; verify before deleting the Quilt smoke test).
  - No new features beyond what the port needs (except the 4 new vanilla enchantments, see C7/Q1).

---

## C2. Environment, toolchain and artefacts

| Item | Value |
|---|---|
| Workspace | `/Users/sid/Documents/proj/enchant tweaker` |
| OS | macOS (Apple Silicon, Homebrew) |
| Default JDK | OpenJDK **25.0.4.1** (Homebrew) |
| JDK 21 | `/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home` (21.0.12.1) |
| Gradle | no system Gradle; repo wrapper **9.3.1** (`gradle/wrapper/gradle-wrapper.properties`) |
| Baseline build | `JAVA_HOME=<jdk21> ./gradlew build -x test` → **BUILD SUCCESSFUL in 6m24s**; artefacts `build/libs/enchanttweaker-1.6.0+mc1.20.5.jar` (174 KB) + `-sources.jar` (119 KB) |
| Git | branch `main`, commit `dd07ca3`; new untracked files: `PLAN.md`, `context.md`, `.plan/` |

Research artefacts downloaded to `/tmp/et-research/` (safe to reuse; re-download if missing):

| File | What it is |
|---|---|
| `client-26.2.jar` | Minecraft 26.2 client jar, 37 MB, 10,952 classes, unobfuscated |
| `classes26/` | extracted `net/minecraft/**` classes from the above |
| `262.json`, `262-full.json` | Mojang version metadata for 26.2 |
| `fapi.jar` | Fabric API `0.161.0+26.2` aggregate (2.4 MB) |
| `fapi-x/META-INF/jars/*` | extracted Fabric API modules (networking 6.3.4, command-api 3.1.0, …) |
| `modmenu.jar` | Mod Menu `20.0.3` |
| `fabric-gametest-api-v1.jar` | `4.0.43` (requires loader ≥ 0.20.0) |
| `fabric-client-gametest-api-v1.jar` | `6.0.16` (requires loader ≥ 0.20.0) |

The plan file: `/Users/sid/Documents/proj/enchant tweaker/PLAN.md`.
Detailed research files: `.plan/argon-port-plan.md` (954 lines) and `.plan/argon-mixin-mapping.md`
(240 lines, per-mixin bytecode-verified table).

---

## C3. Repository baseline

| Metric | Value |
|---|---|
| Java files | 91 (76 in `src/main/java`, 15 in `src/gametest/java`) |
| Total Java LOC | 19,069 (5,523 main, 13,546 GameTests) |
| Mixins | 42 (3 client, 39 server) |
| Config | 140 entries = 139 options + `config_version`; 39 `trade_*`; `capmod_enabled` + 39 cap keys |
| Test coverage | 104 of 140 config keys referenced by tests; the uncovered keys are the `trade_*` toggles |
| CI | `.github/workflows/ci.yml`: JDK 21, `./gradlew build`, `./gradlew runGametest`, Quilt smoke test |
| Release | `.github/workflows/release.yml`: JDK 21, build + gametest, Minotaur dry-run/publish |

Key architecture (all of this must keep working):

- `EnchantTweaker` — main entrypoint; registers `ConfigSyncPayload`, pushes config on join.
- `ETMixinPlugin implements IMixinConfigPlugin` — reads config at mixin-load time, gates each mixin by
  feature flag (`shouldApplyMixin`), with compat overrides for *AxesAreWeapons* and *Fabrication*.
- `ADConfig` + `ETConfigSchema` — typed properties config (bool/int/double/list), validation,
  live reload, suggestions.
- `ETCommands` + `commands/*` — `/et` tree (help, reload, config get/set/list/reset, diagnose).
- `client/*` — Mod Menu entrypoint, config screen + value screen, roman numerals, shiny names,
  client anvil hint.
- `network/ConfigSyncPayload` — S2C map sync.
- `AnvilRepairHandler` — sneak-right-click anvil with iron to repair.
- Tests — 15 GameTest classes + `ETTestHelper`; `@GameTest` + `TestContext` (Yarn API).

---

## C4. Target platform facts (verified 2026-11-03)

| Component | Pin | Fallback | How verified |
|---|---|---|---|
| Minecraft | 26.2 | — | piston-meta manifest: release 2026-06-16, `javaVersion.majorVersion = 25`, client 39,193,383 B |
| Java | 25 | — | same manifest; 26.1+ class files v69 |
| Fabric Loader | 0.20.1 | 0.19.5 | meta.fabricmc.net `/v2/versions/loader` |
| Fabric API | 0.161.0+26.2 | 0.156.0+26.2 | maven metadata + aggregate `fabric.mod.json` (loader ≥0.18.4, java ≥25, minecraft `~26.2-`) |
| Loom | 1.19.3 | 1.17.21 | maven metadata; plugin marker exists |
| Gradle | 9.8.2 | 9.5.1 | services.gradle.org current (2026-10-19) |
| Mod Menu | 20.0.3 | 20.0.0 | Modrinth version API filtered to 26.2 |
| Gametest modules | 4.0.43 / 6.0.16 | — | maven; both declare `fabricloader >=0.20.0` |

Important platform changes (all verified):

- **Obfuscation removed** in 26.1; 26.2 ships Mojang names **and** parameter names. Yarn frozen at
  1.21.11; no `mappings` dependency; no intermediary; no refmaps; mod jars are not remapped.
- **Loom plugin id** is `net.fabricmc.fabric-loom`. Loom 1.14 added unobfuscated support; 1.16 set
  Gradle ≥9.4 and added enum extension; 1.17 introduced property-based run configs; 1.18 moved to
  Gradle-task run configs and Java 25 FFM; 1.19 is current.
- **Fabric API bundle** still exists but the gametest modules are **not** inside the aggregate jar
  (verified nested-jar listing) — Loom's `fabricApi.configureTests` adds them.
- **Tests**: `fabricApi { configureTests { createSourceSet; modId; enableGameTests;
  enableClientGameTests; eula } }`; server gametests run as part of `build`; client gametests via
  `./gradlew runClientGameTest`; entrypoints `fabric-gametest` / `fabric-client-gametest`.

---

## C5. Verified 26.2 API facts (use these instead of guessing)

### C5.1 Names and registries

- `net.minecraft.resources.Identifier` (Mojang renamed `ResourceLocation` → `Identifier` in 26.x);
  create with `Identifier.fromNamespaceAndPath(ns, path)`.
- Enchantments are a **dynamic registry**: `Registries.ENCHANTMENT` is a
  `ResourceKey<Registry<Enchantment>>`; there is **no** `BuiltInRegistries.ENCHANTMENT`. Access via
  `RegistryAccess.lookupOrThrow(Registries.ENCHANTMENT)`; carry `Holder<Enchantment>`.
- `Enchantments.*` constants are `ResourceKey<Enchantment>`, not `Enchantment` instances.
- 26.2 has **43** vanilla enchantments; new vs 1.20.5: `density`, `breach`, `wind_burst` (1.21 mace)
  and `lunge` (26.2 spear).

### C5.2 Enchantment model

```java
public final class Enchantment extends Record {
    Component description;
    Enchantment.EnchantmentDefinition definition;   // max level, costs, anvil cost, slots, weight
    HolderSet<Enchantment> exclusiveSet;
    DataComponentMap effects;                       // EnchantmentEffectComponents.*
    public int getMaxLevel();
    public static boolean areCompatible(Holder<Enchantment>, Holder<Enchantment>);
    public boolean canEnchant(ItemStack); public boolean isSupportedItem(ItemStack); public boolean isPrimaryItem(ItemStack);
    public static Component getFullname(Holder<Enchantment>, int);
    public void modifyDamageProtection(...); public void modifyKnockback(...);
    public void modifyProjectileCount(...); public void modifyAmmoCount(...);
    public void modifyDurabilityChange(...); public void modifyDurabilityToRepairFromXp(...);
}
```

Vanilla data: `data/minecraft/enchantment/*.json` (e.g. `protection.json` with
`exclusive_set: "#minecraft:exclusive_set/armor"`; `mending.json` → `repair_with_xp`; `thorns.json`
→ `post_attack` with `damage_entity` + `change_item_damage`; `soul_speed.json` → attribute effects
+ `change_item_damage`; `infinity.json` → `ammo_use`; `multishot.json` → `projectile_count`).

### C5.3 Containers and gameplay

- `AnvilMenu` (was `AnvilScreenHandler`): `public void createResult()`, `public static int
  calculateIncreasedRepairCost(int)`, `setItemName`, `getCost`, `mayPickup`, `onTake`,
  `lambda$onTake$0` (contains the `0.12f` anvil damage chance). `createResult` contains three
  `bipush 40` (ordinals 0/1/2) and a `long totalCost` (`lconst_0` at offset 26).
- `GrindstoneMenu`: `private createResult()`, called from `slotsChanged`; inner classes `$2/$3`
  (input slots) and `$4` (output slot).
- `ExperienceOrb`: `private int repairPlayerItems(ServerPlayer, int)`; internally calls
  `EnchantmentHelper.modifyDurabilityToRepairFromXp(ServerLevel, ItemStack, int)`.
- `LivingEntity`: `getDamageAfterMagicAbsorb(DamageSource, float)` (protected) wrapping
  `CombatRules.getDamageAfterMagicAbsorb(float, float)`; `dropExperience(ServerLevel, Entity)`.
- `Inventory`: `dropAll()`, `items` + `EntityEquipment equipment`; `PlayerInventory.armor/offHand`
  are gone. `EntityEquipment.dropAll(LivingEntity)`.
- `ProjectileWeaponItem` (was `RangedWeaponItem`): `draw(...)`, `useAmmo(...)` (calls
  `EnchantmentHelper.processAmmoUse`), `createProjectile(...)`.
- `AbstractArrow` / `ThrownTrident` live in `net.minecraft.world.entity.projectile.arrow`;
  `ThrownTrident.tick()`, `onHitEntity(EntityHitResult)`.
- `MerchantOffer`: `isOutOfStock()`, `needsRestock()`, `resetUses()`, `getMaxUses()`.
- Villager trades are **data-driven**: `data/minecraft/villager_trade/**` registry +
  `tags/villager_trade/**`; the librarian book trade uses `enchant_randomly` with
  `options: "#minecraft:tradeable"` and `max_uses`.
- Save data: `readAdditionalSaveData(ValueInput)` / `addAdditionalSaveData(ValueOutput)`.
- Commands: `CommandSourceStack` has no `hasPermission(int)`; use
  `Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source)` (or
  `source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)`).

### C5.4 Client and networking

- `GuiGraphicsExtractor` replaces `DrawContext`/`GuiGraphics`; `Screen.extractRenderState(ctx, mx, my,
  partialTick)` replaces `render`; `extractBackground` replaces `renderBackground`; `Minecraft.gui`
  owns `setScreen`.
- Input: `mouseClicked(MouseButtonEvent, boolean)`, `mouseReleased(MouseButtonEvent)`,
  `keyPressed(KeyEvent)`, `charTyped(CharacterEvent)`, `Button.onPress(InputWithModifiers)`.
- `GuiGraphicsExtractor.text(Font, Component, x, y, color)` and `fill(...)` need **full ARGB**
  colours (alpha byte), else they render invisible.
- `AnvilScreen.extractLabels(GuiGraphicsExtractor, int, int)` is where "Too Expensive" is drawn.
- Roman numerals: client language lookups go through
  `net.minecraft.client.resources.language.ClientLanguage.getOrDefault(String, String)` /
  `has(String)`.
- Networking: `PayloadTypeRegistry.clientboundPlay()` / `serverboundPlay()`;
  `CustomPacketPayload.Type`; `StreamCodec`; `RegistryFriendlyByteBuf`;
  `ServerPlayNetworking.canSend/send`; `ServerPlayConnectionEvents.JOIN` unchanged.

### C5.5 Tests

- `net.minecraft.gametest.framework.GameTestHelper`: `setBlock`, `assertBlockPresent`,
  `assertBlockNotPresent`, `assertBlockState`, `succeed`, `spawnEntity`/`spawnMob` builders.
- Fabric annotation `net.fabricmc.fabric.api.gametest.v1.GameTest` attributes: `environment`,
  `dimension`, `structure`, `maxTicks`, `setupTicks`, `required`, `rotation`, `manualOnly`,
  `maxAttempts`, `requiredSuccesses`, `skyAccess`, `padding`.
- `CustomTestMethodInvoker` for per-test setup; client API: `FabricClientGameTest`,
  `ClientGameTestContext`, `TestWorldBuilder`, `TestInput`, `TestScreenshotOptions`.

---

## C6. Decisions already made

1. **Two-stage migration.** Migrate Yarn → Mojang at 1.20.5 first
   (`./gradlew migrateMappings --mappings "net.minecraft:mappings:1.20.5" --input … --output …
   --overrideInputsIHaveABackup`, run twice: `src/main/java` and `src/gametest/java`), switch to
   `loom.officialMojangMappings()`, compile-green. Then bump the platform to 26.2 and remove mappings.
2. **Pins** as in C4; fallbacks documented.
3. **Build shape**: `net.fabricmc.fabric-loom` 1.19.3, Java 25, plain `implementation` dependencies,
   `fabricApi.configureTests` block, Gradle 9.8.2 wrapper.
4. **Publishing artefact**: with the non-remapping Loom there is no `remapJar`; publish `jar`
   (update Minotaur's `uploadFile`; verify).
5. **Data-driven enchantment redesign** uses four patterns (P1 level-scaling hooks on
   `Enchantment.modify*`; P2 compatibility on `areCompatible`; P3 tag/data edits; P4 helper
   interception), chosen per feature — see PLAN.md §8.
6. **Tests as spec**: port `ETTestHelper` first, then the suites; add unit tests for pure logic
   (`ADConfig`, `ETConfigSchema`, `ADRoman`, `ADUtils` math); add client GameTests for the config UI.
7. **Quilt dropped** (verify the loader situation, then remove the smoke test and claims).
8. **Versioning**: `2.0.0+mc26.2`, Modrinth `gameVersions=["26.2"]`, `loaders=["fabric"]`.

---

## C7. Risks and open questions

Top risks (full register in PLAN.md §12): enchantment redesign effort; `migrateMappings` collisions;
silent mixin-descriptor failures (no refmap); GUI rewrite; test-harness migration; toolchain
incompatibility.

Open questions:

- **Q1 — new enchantments.** Add config keys for `density`, `breach`, `wind_burst`, `lunge` (capmod +
  trade + lang + schema), or document them as out of scope? Changes the config contract
  140 → 144/148.
- **Q2 — protection hook.** `more_protection`/`protection_bypass`: wrap
  `CombatRules.getDamageAfterMagicAbsorb` via `LivingEntity.getDamageAfterMagicAbsorb` (both verified).
  Confirm the exact semantics with a GameTest.
- **Q3 — publish artefact.** Confirm whether `remapJar` exists under Loom 1.19.3's non-remap plugin;
  if not, Minotaur must upload `jar`.
- **Q4 — Mixin compatibility level.** `JAVA_25` may not be accepted by the bundled Mixin; keep the
  highest accepted value.
- **Q5 — Quilt.** Does any Quilt loader support 26.x? If not, drop the smoke test and badges.

---

## C8. Artefacts, provenance and delegation log

| Artefact | Path | Notes |
|---|---|---|
| Port plan (authoritative) | `PLAN.md` | 1,052 lines; phases M0–M6, 42-mixin matrix, tests, risks |
| This context pack | `context.md` | selectively readable; C0–C11 |
| Argon planning draft | `.plan/argon-port-plan.md` | 954 lines; strong on toolchain/test detail, some invented file names — verify before use |
| Argon mapping audit | `.plan/argon-mixin-mapping.md` | 240 lines; per-mixin bytecode offsets/ordinals; spot-checked by the lead |

Delegation history (2026-11-03):

- `gemini-4-argon-high` (agy) produced both artefacts above, then **failed at provider stage**
  (timeout, then **quota exhausted**, reset in ~99 h as of 18:15).
- `claude-opus-5-5-high` (agy) fallback also hit the same provider quota.
- A DSH `subagent` review was launched as the final fallback; its result had not landed in `.plan/`
  when this file was written. Do not assume it exists.
- No port code has been written; `git status` shows only untracked `PLAN.md`, `context.md`, `.plan/`.

---

## C9. Command cheat sheet

```bash
# Baseline build (1.20.5, JDK 21)
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
  ./gradlew build -x test --console=plain

# M1 — migrate mappings at the CURRENT version
JAVA_HOME=<jdk21> ./gradlew migrateMappings \
  --mappings "net.minecraft:mappings:1.20.5" \
  --input src/main/java --output remappedSrc --overrideInputsIHaveABackup
JAVA_HOME=<jdk21> ./gradlew migrateMappings \
  --mappings "net.minecraft:mappings:1.20.5" \
  --input src/gametest/java --output remappedGametestSrc --overrideInputsIHaveABackup

# M2 — toolchain
./gradlew wrapper --gradle-version 9.8.2
./gradlew --version
./gradlew build --console=plain

# Tests (after the Loom test DSL is configured)
./gradlew test                     # unit tests
./gradlew build                    # includes server game tests
./gradlew runClientGameTest        # client game tests

# Verify any 26.2 API without an MCP
javap -p -classpath /tmp/et-research/client-26.2.jar net.minecraft.world.item.enchantment.Enchantment
unzip -l /tmp/et-research/client-26.2.jar | grep enchantment
```

---

## C10. Gotchas and lessons

1. **The 1.20.5 baseline build warns about Gradle 10 deprecations** — fix property assignments and
   configuration-cache unfriendly code during M2/M3 (PLAN.md §6.1).
2. **No refmap anymore** — a wrong `@Inject(method=...)` string fails at boot with
   `MixinApplyError`; enable `-Dmixin.debug.export=true` while porting.
3. **Dynamic enchantment registry** — `BuiltInRegistries.ENCHANTMENT` does not exist; use
   `RegistryAccess.lookupOrThrow(Registries.ENCHANTMENT)`. `Enchantments.*` are `ResourceKey`s.
4. **ARGB colours** — `GuiGraphicsExtractor` text/fill need the alpha byte or nothing appears.
5. **`lconst_0`** — `AnvilMenu.createResult` uses a `long` prior-work total; the free-prior-work hook
   needs `@ModifyConstant(longValue = 0L, ordinal = 0)`, not `intValue`.
6. **`ClientLanguage`** — roman numeral hooks target the client language class, not vanilla
   `Language`; verify before porting.
7. **`MerchantOffer.isOutOfStock()`** exists (in addition to `needsRestock()`).
8. **Gametest modules need Loader ≥ 0.20.0**, which is why the pin is 0.20.1 even though the 26.2
   blog mentioned 0.19.3.
9. **The current repo is 1.20.5-era** — it advertises Quilt and uses `runGametest`; both change.
10. **Do not trust the Argon plan draft blindly** — it invented some test file names and claimed the
    aggregate Fabric API bundles gametest modules (false). Everything folded into `PLAN.md` was
    spot-checked against the jar.

---

## C11. Quick-start checklist (resume here)

1. Read `PLAN.md` §5 (phases) and §7 (mixin matrix).
2. `git tag pre-26.2`; add `.plan/` to `.gitignore`.
3. Re-run the baseline build to confirm the toolchain (C9, first command).
4. Execute M1 (mapping migration), review the diff, commit.
5. Execute M2 (bump to 26.2), then M3 (compile-green).
6. Use C5 as the API source of truth; verify anything missing with
   `javap -p -classpath /tmp/et-research/client-26.2.jar <fqcn>`.
