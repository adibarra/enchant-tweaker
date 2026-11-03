# PLAN — Port Enchant Tweaker to Minecraft 26.2 (Fabric)

| Field | Value |
|---|---|
| Status | Draft for review (Argon High review pending; see `context.md` for the working context pack) |
| Date | 2026-11-03 |
| Repo | https://github.com/adibarra/enchant-tweaker (cloned at `main`, commit `dd07ca3`) |
| Source baseline | Minecraft 1.20.5, Yarn 1.20.5+build.1, Loader 0.15.11, Fabric API 0.97.4+1.20.5, Loom 1.15.3, Java 21 |
| Target | Minecraft **26.2** (release 2026-06-16), Fabric Loader, Java 25, Mojang names (unobfuscated game) |
| Scope | Complete feature parity for all 140 config keys, all 42 mixins, commands, config UI, networking, tests, CI and release |

> **Fact provenance.** Platform facts below were verified live on 2026-11-03 against
> `meta.fabricmc.net`, `maven.fabricmc.net`, `piston-meta.mojang.com`, the Minecraft 26.2
> client jar (downloaded and inspected with `javap`), `docs.fabricmc.net`, `fabricmc.net/blog`
> and `docs.neoforged.net/primer`. Items marked **[verify]** still need a hands-on check during
> implementation.

---

## 1. Objective

Port Enchant Tweaker end-to-end to Minecraft 26.2 on Fabric Loader, preserving every gameplay
behaviour (139 settings), the `/et` command suite, the Mod Menu config screen, client↔server config
sync, and the GameTest suite — while moving from Yarn to Mojang names in an unobfuscated world.

**Not** in scope: new features, backporting to 1.21.x, Forge/NeoForge, and Quilt support (see §5.8).

**Success criteria**

1. `./gradlew build` is green on Java 25 with Loom 1.19.x and zero references to Yarn/intermediary.
2. `build` runs the server GameTests and they pass (new Loom `fabricApi.configureTests` harness).
3. Client GameTest(s) boot a world, open the config screen, and pass.
4. A manual smoke matrix (singleplayer, dedicated server, multiplayer join-sync, Mod Menu) passes.
5. Modrinth release `2.0.0+mc26.2` builds from CI and installs on a vanilla 26.2 launcher profile.

---

## 2. Target platform matrix (pins)

| Component | Pin | Fallback | Evidence |
|---|---|---|---|
| Minecraft | `26.2` | — | piston-meta: release 2026-06-16, javaVersion.majorVersion = 25 |
| Fabric Loader | `0.20.1` | `0.19.5` | meta.fabricmc.net latest stable; 26.2 blog recommended 0.19.3. **Verified:** `fabric-gametest-api-v1` 4.0.43 and `fabric-client-gametest-api-v1` 6.0.16 both declare `fabricloader >=0.20.0` |
| Fabric API | `0.161.0+26.2` | `0.156.0+26.2` | maven metadata; aggregate requires loader >=0.18.4, java >=25, minecraft ~26.2 |
| Fabric Loom | `1.19.3` | `1.17.21` | maven.fabricmc.net; 26.2-era Loom was 1.17, Loom 1.16+ needs Gradle >=9.4 |
| Gradle | `9.8.2` | `9.5.1` | services.gradle.org current (2026-10-19) |
| Java (Gradle JVM + release) | `25` | — | MC 26.2 requires Java 25; Loom 1.18+ uses Java 25 FFM |
| Mod Menu (optional) | `20.0.3` | `20.0.0` | Modrinth versions for 26.2 |

**Source/target compatibility:** `options.release = 25`, `sourceCompatibility/targetCompatibility = 25`.
**Mixin compatibility level:** raise to `JAVA_25` if the bundled Mixin supports it, otherwise keep the
highest supported value **[verify at first boot]**.

**Plugin id:** `net.fabricmc.fabric-loom` (the old `fabric-loom` id is deprecated; Loom 1.14+
introduced the new id).

**No `mappings` dependency.** In 26.1+ the game ships unobfuscated with Mojang names, so
`minecraft "com.mojang:minecraft:26.2"` is enough. All Fabric/Minecraft dependencies become plain
`implementation` (no remapping, no `modImplementation`).

---

## 3. Baseline inventory (what is being ported)

### 3.1 Size

| Area | Files | LOC |
|---|---|---|
| `src/main/java` | 76 | 5,523 |
| `src/gametest/java` | 15 | 13,546 |
| Mixins | 42 | ~1,600 |
| Total Java | 91 | 19,069 |

### 3.2 Architecture

- `EnchantTweaker` (main entrypoint): registers `ConfigSyncPayload` via `PayloadTypeRegistry.playS2C()`,
  sends config on `ServerPlayConnectionEvents.JOIN`, registers commands/handlers when enabled.
- `ETMixinPlugin implements IMixinConfigPlugin`: reads the config at mixin-load time and gates every
  mixin by feature flag (`shouldApplyMixin`), with compatibility overrides for *AxesAreWeapons* and
  *Fabrication*.
- `ADConfig` + `ETConfigSchema`: 140-key typed config (bool/int/double/list), properties file with
  comments, live reload, validation, suggestions.
- `ETCommands` + `commands/*`: `/et` tree (help, reload, config get/set/list/reset, diagnose) with
  Brigadier suggestions and feedback.
- `client/*`: Mod Menu integration, config screen + value screen, roman-numeral language mixin,
  shiny-name mixin, client-side anvil hint mixin.
- `network/ConfigSyncPayload`: S2C config map sync.
- `AnvilRepairHandler`: sneak+right-click anvil with iron to repair.
- Tests: 15 GameTest classes (13.5k LOC) covering 104 of 140 config keys (uncovered keys are the 36 `trade_*`
  toggles); `ETTestHelper` provides anvil/merge helpers.

### 3.3 Feature surface (by config group)

| Group | Keys | Description |
|---|---|---|
| master | `mod_enabled` | master switch, restart-required |
| anvil | `cheap_names`, `not_too_expensive`, `nte_max_cost`, `prior_work_cheaper`, `pw_cost_multiplier`, `prior_work_free`, `sturdy_anvils`, `anvil_damage_chance`, `anvil_repair`, `anvil_repair_ingot_cost` | rename cost, 40-level cap, prior-work penalty, break chance, anvil repair |
| enhanced | `more_binding`, `more_binding_step`, `more_channeling`, `more_flame`, `more_flame_per_level`, `more_infinity`, `more_infinity_pct`, `more_looting`, `more_looting_multiplier`, `more_mending`, `more_mending_step`, `more_mending_floor`, `more_multishot`, `more_multishot_per_level`, `more_protection`, `more_protection_base`, `more_fire_protection`, `more_fire_protection_base`, `more_blast_protection`, `more_blast_protection_base` | enchantment scaling |
| tweaks | `axes_not_tools`, `axe_weapons`, `better_mending`, `bow_infinity_fix`, `bow_looting`, `god_armor`, `god_weapons`, `infinite_mending`, `loyal_void_tridents`, `multishot_piercing`, `no_mending_unbreaking`, `no_soul_speed_backlash`, `no_thorns_backlash`, `protection_bypass_enabled`, `protection_bypass_types`, `roman_numerals`, `shiny_name`, `xp_scaling`, `xp_scaling_base`, `xp_scaling_step`, `trident_weapons` | compatibility / behaviour |
| grindstone | `grindstone_disenchant`, `grindstone_disenchant_keep_item` | disenchant into books |
| trades | `villager_trade_limits`, `enchant_trade_max_uses`, `enchant_trade_restock`, `enchant_trade_no_restock`, 39x `trade_*` | per-enchantment trade availability |
| capmod | `capmod_enabled` + 39 per-enchantment caps | max level overrides |
| disable | `disable_enchantments_enabled`, `disable_enchantments` | remove enchantments from circulation |

### 3.4 Baseline build

- CI on `main` is green (JDK 21, `./gradlew build`, `./gradlew runGametest`, Quilt smoke test).
- Local baseline build **verified green** during planning: JDK 21 (Homebrew 21.0.12.1), Gradle 9.3.1,
  Loom 1.15.3, 6m24s; artefacts `build/libs/enchanttweaker-1.6.0+mc1.20.5.jar` (174 KB) and the sources
  jar. The build already warns that deprecated Gradle features make it incompatible with Gradle 10
  (see §6.1).

---

## 4. Platform deltas (why this is a large port)

### 4.0 How a Fabric mod is built and loaded on 26.2

Short version, for anyone joining the port:

1. **No remapping.** The game jar is deobfuscated already, so Loom does not remap the mod jar or the
   game; `remapJar` becomes a near no-op and `modImplementation`/`modCompileOnly` are unnecessary —
   plain `implementation`, `compileOnly`, `runtimeOnly` are used instead.
2. **No refmap.** Mixin configs no longer carry a `refmap`; `@Inject(method=...)`, `@At(target=...)`
   strings are official names/descriptors. A wrong descriptor fails at boot with `MixinApplyError`.
3. **`fabric.mod.json` is unchanged** (schemaVersion 1, entrypoints `main`/`client`/`modmenu`/
   `fabric-gametest`/`fabric-client-gametest`); only the `depends` version ranges change
   (`"minecraft": "~26.2"`, `"java": ">=25"`).
4. **Fabric API is still a bundle of modules** with separate versions; the aggregate
   `net.fabricmc.fabric-api:fabric-api:0.161.0+26.2` pulls them in. Test modules
   (`fabric-gametest-api-v1`, `fabric-client-gametest-api-v1`) are added by Loom's test DSL.
5. **Class tweakers**: Loom 1.16+ exposes the new "class tweaker" file format
   (`.classtweaker`) for access widening/extension. This mod needs none today, but if a private
   target resists mixin (e.g. `GrindstoneMenu.createResult`), an access widener/class tweaker is the
   fallback rather than reflection.
6. **Content is data-driven**: enchantments, tags, damage types, loot, etc. are loaded from
   datapacks/registries. Mods can ship `data/<ns>/...` JSON and can also influence vanilla's
   `data/minecraft/enchantment/*.json` through tags and runtime hooks — this is where most of the
   "enchantment tweak" behaviour now lives.
7. **Rendering/GUI**: 26.2's GUI is "extract render state" based (`GuiGraphicsExtractor`,
   `extractRenderState`), and `Minecraft.gui` owns screen management; direct framebuffer/GL calls are
   being retired (Vulkan backend), so stay on Blaze3D/GUI APIs.
8. **Loader 0.19/0.20**: adds enum extensions and modern class tweakers; mods that don't use them are
   unaffected. Loader 0.20.x is what the 26.2 test modules require.

### 4.1 Obfuscation removed / Yarn is dead

- Minecraft **26.1 is the first unobfuscated release**; 26.2 ships Mojang names **and** parameter
  names. Intermediary is no longer used and **Yarn is frozen at 1.21.11**.
- Consequence: no `mappings` line, no refmaps, no remapping of the mod jar, and every class/method
  reference in source and in `@Inject(method = "...")` descriptors must be rewritten.
- Mappings are **Mojang names**, which differ from Yarn in almost every API name (see Appendix B).

### 4.2 Toolchain

- Loom 1.14 added non-obfuscated support; 1.16 raised the Gradle floor to 9.4 and added enum
  extensions; 1.17 introduced the property-based run-configuration API; 1.18 switched to
  Gradle-task-based run configs and Java 25 FFM; 1.19 is current.
- New plugin id `net.fabricmc.fabric-loom`. The old DSL (`loom { runs { ... } }`) is deprecated but
  still present; new test setup is `fabricApi { configureTests { ... } }`.
- Fabric Loader 0.19.0+ adds the enum-extension API (not needed here). Loader 0.20.1 is current.

### 4.3 Fabric API renames (26.2)

| Old (1.20.5) | New (26.2) |
|---|---|
| `PayloadTypeRegistry.playS2C()` | `PayloadTypeRegistry.clientboundPlay()` |
| `PayloadTypeRegistry.playC2S()` | `PayloadTypeRegistry.serverboundPlay()` |
| `itemgroup.v1.ItemGroupEvents` | `creativetab.v1.CreativeModeTabEvents` |
| `screen.ExtendedScreenHandlerFactory/Type` | `menu.ExtendedMenuProvider/Type` |
| `rendering.ColorProviderRegistry` | `BlockColorRegistry` + `BlockTintSources` (client rendering; only if used) |

`ServerPlayNetworking`, `ServerPlayConnectionEvents`, `ServerLifecycleEvents`,
`ServerPlayerEvents`, `UseBlockCallback` keep their names (signatures verified in 26.2 jars).

### 4.4 Vanilla renames that hit this mod directly

| Yarn 1.20.5 | 26.2 Mojang |
|---|---|
| `Text` / `MutableText` | `Component` / `MutableComponent` |
| `Identifier` | `Identifier` (Mojang renamed `ResourceLocation` -> `Identifier` in 26.x) |
| `Registries.ENCHANTMENT` | Dynamic registry key (`ResourceKey<Registry<Enchantment>>`). There is **no** `BuiltInRegistries.ENCHANTMENT`; resolve with `RegistryAccess.lookupOrThrow(Registries.ENCHANTMENT)` and carry `Holder<Enchantment>` |
| `AnvilScreenHandler` | `AnvilMenu` (`updateResult` -> `createResult`) |
| `GrindstoneScreenHandler` | `GrindstoneMenu` (private `createResult`) |
| `PlayerEntity` / `PlayerInventory` | `Player` / `Inventory` |
| `World` / `ServerWorld` | `Level` / `ServerLevel` |
| `ExperienceOrbEntity` | `ExperienceOrb` (`repairPlayerGears` -> `repairPlayerItems`) |
| `PersistentProjectileEntity` | `AbstractArrow` (`net.minecraft.world.entity.projectile.arrow`) |
| `TridentEntity` | `ThrownTrident` (same package) |
| `MiningToolItem` | removed; `AxeItem`, `Item.postHurtEnemy` |
| `RangedWeaponItem` | `ProjectileWeaponItem` (`load` -> `draw`/`useAmmo`) |
| `TradeOffer` | `MerchantOffer` |
| `TranslationStorage` | `Language` |
| `Screen.drawForeground` / `render` | `extractRenderState(GuiGraphicsExtractor,int,int,float)` |
| `DrawContext` | `GuiGraphicsExtractor` |
| `Minecraft.setScreen` | `Minecraft.gui.setScreen` |
| `ItemStack` NBT | data components (`ItemEnchantments`, `DataComponents`) |

### 4.5 Enchantments are data-driven (the biggest redesign)

Since 1.21 the `DamageEnchantment`, `ProtectionEnchantment`, `ThornsEnchantment`,
`InfinityEnchantment`, `MultishotEnchantment`, `PiercingEnchantment` classes are **gone**.
`Enchantment` is now a **record**:

```java
public final class Enchantment extends Record {
    Component description;
    Enchantment.EnchantmentDefinition definition;   // weight, maxLevel, minCost, maxCost, anvilCost, slots
    HolderSet<Enchantment> exclusiveSet;
    DataComponentMap effects;                       // EnchantmentEffectComponents.*
    public int getMaxLevel();                        // still exists (reads definition)
    public static boolean areCompatible(Holder<Enchantment>, Holder<Enchantment>);
    public boolean isSupportedItem(ItemStack);
    public boolean isPrimaryItem(ItemStack);
    public void modifyDamageProtection(...); public void modifyProjectileCount(...);
    public void modifyAmmoCount(...); public void modifyDurabilityChange(...);
}
```

Vanilla data lives in `data/minecraft/enchantment/*.json` inside the game jar, e.g.
`protection.json` (`exclusive_set: "#minecraft:exclusive_set/armor"`, `effects.minecraft:damage_protection`),
`mending.json` (`effects.minecraft:repair_with_xp`), `thorns.json` (`post_attack` ->
`damage_entity` + `change_item_damage`), `soul_speed.json` (attribute effects + `change_item_damage`),
`infinity.json` (`ammo_use`), `multishot.json` (`projectile_count`/`projectile_spread`).

So the old "subclass a specific enchantment class" strategy becomes one of:

- **P1 — Level-scaling hook**: mixin the `Enchantment.modify*` methods (they all receive `int level`)
  and multiply/emulate the level for the matching enchantment id.
- **P2 — Compatibility hook**: mixin `Enchantment.areCompatible` (static) or
  `isSupportedItem`/`isPrimaryItem`, gated per id pair.
- **P3 — Data/tag edit**: override the enchantment JSON or the `exclusive_set/*` and
  `enchantable/*` item tags.
- **P4 — Helper interception**: intercept `EnchantmentHelper.*` (e.g. `processEquipmentDropChance`,
  `processAmmoUse`, `modifyDurabilityToRepairFromXp`, `getDamageProtection`).

Which one is right is decided per feature in §7 (and each decision is validated by a GameTest).

### 4.6 GUI / rendering overhaul

26.2's GUI pipeline replaces `GuiGraphics` with `GuiGraphicsExtractor` and `render` with
`extractRenderState`:

```java
public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float partialTick);
protected void init();                       // Screen.init(int,int) is final now
public boolean keyPressed(KeyEvent event);
public void onPress(InputWithModifiers input); // Button
```

`AnvilScreen` now exposes `extractLabels(...)`, `extractBackground(...)`, `extractErrorIcon(...)`.
The config screen (~430 LOC of `ETConfigScreen` + `ETConfigValueScreen`) must be rewritten against
this API; `Screen.addRenderableWidget`, `EditBox`, `Button.builder(...)` survive.

### 4.7 Testing framework

Loom's new DSL replaces the hand-rolled `runGametest` run config:

```groovy
fabricApi {
    configureTests {
        createSourceSet = true
        modId = "enchanttweaker-test"
        enableGameTests = true
        enableClientGameTests = true
        eula = true
    }
}
```

- Server game tests run automatically as part of `build`.
- Client game tests run with `./gradlew runClientGameTest` (or a `ClientProductionRunTask` with
  `-Dfabric.client.gametest` and `useXVFB = true` in CI).
- Entrypoints: `fabric-gametest` (`net.fabricmc.fabric.api.gametest.v1.GameTest`,
  `CustomTestMethodInvoker`) and `fabric-client-gametest` (`FabricClientGameTest`).
- Vanilla helper is `net.minecraft.gametest.framework.GameTestHelper` (now in `net.minecraft.gametest`).
- Unit tests are possible via `testImplementation "net.fabricmc:fabric-loader-junit:${loader_version}"` +
  `test { useJUnitPlatform() }`.
---

## 5. Migration strategy and phases

Order matters: **migrate names at 1.20.5 first**, then bump the platform. Migrating after the bump is
impossible because Yarn does not exist for 26.x.

```
main -> m0-port-baseline -> m1-mojmap-1.20.5 -> m2-26.2-toolchain -> m3-compile-green
     -> m4-mixins -> m5-tests -> m6-release
```

### M0 — Baseline and safety net (0.5 day)

- Create branch `port/26.2`; tag the pre-port commit `pre-26.2`.
- Record baseline artefacts: `./gradlew build` (verified locally: green, 6m24s, JDK 21, Gradle 9.3.1,
  Loom 1.15.3, jar `enchanttweaker-1.6.0+mc1.20.5.jar`), `./gradlew runGametest`.
- Snapshot the user-facing contract: `README.md` feature list, `enchant-tweaker.properties`,
  `CHANGELOG.md` — they are the acceptance spec.
- Add `.plan/` to `.gitignore` (planning artefacts, not shipped).

**Gate:** baseline build green locally (JDK 21) — **done 2026-11-03**.

### M1 — Yarn -> Mojang mappings on 1.20.5 (1–2 days)

1. Backup: `git commit` everything; the task overwrites sources.
2. Run Loom's migration (Loom 1.15.3 supports mixin-aware migration via MercuryMixin):
```bash
JAVA_HOME=/path/to/jdk21 ./gradlew migrateMappings \
  --mappings "net.minecraft:mappings:1.20.5" \
  --input src/main/java --output remappedSrc \
  --overrideInputsIHaveABackup
```
   Then review `git diff --no-index src/main/java remappedSrc`, and copy over when sane.
3. Repeat for the GameTest source set:
```bash
./gradlew migrateMappings --mappings "net.minecraft:mappings:1.20.5" \
  --input src/gametest/java --output remappedGametestSrc --overrideInputsIHaveABackup
```
4. In `build.gradle`, keep the 1.20.5 platform but switch mappings:
   `mappings loom.officialMojangMappings()` and remove the `yarn_mappings` property.
5. Compile-fix loop: Loom remaps simple names but not string mixin descriptors reliably; expect
   hand-fixing of `@Inject(method = "...")`, `@At(target = "...")`, `@Shadow` descriptors, and
   simple-name collisions (`Pair` -> `Tuple`, `Player` mis-resolution, package `world` -> `level`).
6. **Do not** run the GameTests yet (they still target the old platform semantics); compile only.

**Gate:** `./gradlew build -x test` green at 1.20.5 + Mojmap; zero Yarn references
(`grep -rn "net.minecraft.enchantment\\|net.minecraft.entity\\|net.minecraft.screen" src` is clean).

> Why at 1.20.5: the migrator needs a mapping pair Yarn -> Mojmap for the *same* version. 26.2 has no
> Yarn. Doing this first keeps the diff reviewable (pure rename) and separates name churn from API
> churn.

### M2 — Platform bump to 26.2 (1–2 days)

1. `gradle.properties`: `minecraft_version=26.2`, `loader_version=0.20.1`,
   `fabric_version=0.161.0+26.2`, `modmenu_version=20.0.3`, remove `yarn_mappings`, add
   `loom_version=1.19.3` and `java_version=25`.
2. `build.gradle`: plugin id `net.fabricmc.fabric-loom`, Java 25 release, drop the `mappings` line,
   switch dependencies to `implementation`, add `fabricApi.configureTests`, update Mod Menu config.
3. Wrapper: `./gradlew wrapper --gradle-version 9.8.2` (keep `distributionSha256Sum`).
4. `fabric.mod.json`: `"minecraft": "~26.2"`, `"java": ">=25"`, loader `">=0.19.3"`; keep the
   `fabric-api` dependency and entrypoints.
5. `mixins.json`: `compatibilityLevel: "JAVA_25"` **[verify]**, drop any `refmap` key if present.
6. Toolchain spike: confirm the combo builds before touching code (see §5.7 fallbacks).

**Gate:** `./gradlew build` reaches compilation of mod sources (expected to fail with thousands of
errors — that is the input to M3).

### M3 — Compile-green pass (2–4 days)

Mechanical sweep over ~8k LOC:

1. Renames from Appendix B (`Text` -> `Component`, `World` -> `Level`, `Identifier` semantics, registries).
2. `ItemStack` data components: `ItemEnchantments`, `DataComponents.REPAIR_COST`,
   `EnchantmentHelper.updateEnchantments/getEnchantmentsForCrafting`.
3. Networking payload: `clientboundPlay()`, `CustomPacketPayload.Type`, `StreamCodec`,
   `ServerPlayNetworking.canSend/send`.
4. Commands: Brigadier names unchanged, but the permission model changed. `CommandSourceStack`
   no longer has `hasPermission(int)`; use `Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source)`
   (verified in 26.2: `Commands.LEVEL_*`, `PermissionProviderCheck.test`, `Permissions.COMMANDS_*`).
5. Client: `Component`, `MutableComponent`, `Component.translatable`, `GuiGraphicsExtractor`,
   `extractRenderState`, `Minecraft.gui.setScreen`.

**Gate:** `./gradlew compileJava compileGametestJava` green (no tests yet).

### M4 — Mixin port and redesign (5–8 days)

Work through the §7 matrix, subsystem by subsystem, in this order (each ends with a build and a
targeted GameTest):

1. Anvil (6 mixins) — mostly direct renames.
2. Grindstone (3 mixins) — direct renames + private-method access strategy.
3. XP / mending (4 mixins) — new host methods.
4. Projectile / trident (5 mixins) — new host classes.
5. Enchantment definition family (10 mixins) — the redesign (§8).
6. Client (3 mixins) — GUI overhaul.
7. Trades / misc (2 mixins).

**Gate:** every mixin loads without `MixinApplyError`; `-Dmixin.debug.export=true` spot checks.

### M5 — Tests and CI (3–5 days)

1. Adopt `fabricApi.configureTests`; move `src/gametest` content to the generated source set
   (**[verify generated path, likely `src/gametest`]**).
2. Port helpers first (`ETTestHelper`, `ADConfigGameTest`), then the suite class by class;
   the tests are the behavioural spec, so port them **before** rewriting behaviour where possible.
3. Add unit tests for pure logic (`ADConfig`, `ETConfigSchema`, `ADRoman`, `ADUtils` math,
   `AnvilCost`) with `fabric-loader-junit`.
4. Add client GameTests: boot singleplayer, open config screen via Mod Menu/mod API, type a value,
   save, assert the config file; screenshot for humans.
5. CI: JDK 25, `./gradlew build` (server gametests run inside), `./gradlew runClientGameTest`
   job with xvfb; keep the Quilt smoke test only if Quilt supports 26.x (see §5.8).

**Gate:** CI green, test count >= baseline, client gametest screenshot uploaded.

### M6 — Docs and release (1 day)

- Version `2.0.0+mc26.2`; CHANGELOG entry "Port to Minecraft 26.2 (unobfuscated, Mojang mappings)".
- README: supported versions, Java 25, install notes, drop Quilt badges if applicable.
- Modrinth: `gameVersions = ["26.2"]`, `loaders = ["fabric"]`, dependencies fabric-api required,
  modmenu optional.
- Release workflow: JDK 25, `./gradlew build` (runs gametests), `writeReleaseNotes`, publish.

### 5.7 Toolchain risk and fallbacks

| Risk | Detection | Fallback |
|---|---|---|
| Loom 1.19.3 + Gradle 9.8.2 incompatibility | `./gradlew build` config phase | Loom 1.17.21 + Gradle 9.5.1 (26.2-era combination used by other mods) |
| Java 25 on CI/IDE | `java -version` in the Gradle JVM | pin `org.gradle.java.home` to JDK 25 |
| Mixin compatibility level rejection | boot log `Invalid compatibility level` | keep highest accepted level |
| Mod Menu 20 API changes | compile error in `ETModMenuIntegration` | guard the entrypoint behind reflection or drop the optional integration temporarily |

### 5.8 Quilt

The repo currently advertises Fabric+Quilt and runs a Quilt server smoke test. Quilt Loader has no
26.x support as of this plan **[verify]**. Decision: **drop Quilt** from README/Modrinth metadata and
delete `.github/scripts/quilt-server-smoke.sh` unless a 26.2-compatible Quilt loader exists.

---

## 6. Workstream A — build files (target state)

`gradle.properties`
```properties
org.gradle.jvmargs=-Xmx3G
org.gradle.parallel=true
org.gradle.configuration-cache=true

minecraft_version=26.2
loader_version=0.20.1
loom_version=1.19.3
fabric_version=0.161.0+26.2
modmenu_version=20.0.3

mod_version=2.0.0
maven_group=com.adibarra.enchanttweaker
archives_base_name=enchanttweaker
```

`build.gradle` (shape)
```groovy
plugins {
    id 'net.fabricmc.fabric-loom' version "${loom_version}"
    id 'maven-publish'
    id 'com.modrinth.minotaur' version '2.9.0'
    id 'com.diffplug.spotless' version '8.8.0'
}

dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    implementation "net.fabricmc:fabric-loader:${project.loader_version}"
    implementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_version}"
    compileOnly "maven.modrinth:modmenu:${project.modmenu_version}"
    runtimeOnly "maven.modrinth:modmenu:${project.modmenu_version}"
}

fabricApi {
    configureTests {
        createSourceSet = true
        modId = "enchanttweaker-test"
        enableGameTests = true
        enableClientGameTests = true
        eula = true
    }
}

tasks.withType(JavaCompile).configureEach { it.options.release = 25 }
java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
    withSourcesJar()
}
```

Notes:
- `modImplementation`/`modCompileOnly` may still exist in Loom 1.19 but are unnecessary/unsupported
  for unobfuscated deps; use plain configurations and adjust if the plugin complains.
- Keep `processResources` `expand("version")` for `fabric.mod.json`.
- Keep Minotaur and the changelog extraction logic; update `gameVersions` to `26.2` and `loaders` to
  `[fabric]`.
- **Publishing artefact:** with the non-remapping Loom there is no separate `remapJar` output; the
  `jar` task produces the distributable. Update Minotaur (`uploadFile = jar`) and the release
  workflow's `build/libs/enchanttweaker-${mod}+mc26.2.jar` validation accordingly **[verify]**, and
  keep `sourcesJar` as the additional file.
- Spotless formatter stays, but the Eclipse JDT version must understand Java 25 syntax; bump it if
  the formatter fails on `release = 25` (the Argon planning pass flagged the same class of issue for
  google-java-format 1.22).
- MC 26.1+ class files are version 69 (Java 25); do not lower the release level for convenience.

### 6.1 Gradle hygiene (baseline warnings)

The 1.20.5 baseline build already reports *"Deprecated Gradle features were used in this build, making
it incompatible with Gradle 10"*. Fix during M2/M3, verified with `--warning-mode all`:

- replace property-assignment syntax (`base { archivesName = ... }`, `debugMode = ...`) with the
  Gradle 9 property API (`archivesName.set(...)`, `debugMode.set(...)`); Minotaur/Spotless may need
  upgrades to their Gradle-9-compatible releases;
- avoid `project.hasProperty(...)` inside task configuration (configuration-cache unfriendly);
  compute the value at configuration time or use providers;
- ensure all task inputs/outputs are declared through providers (`layout.buildDirectory.file(...)`)
  so `org.gradle.configuration-cache=true` can be enabled (the Fabric template enables it);
- keep `distributionSha256Sum` when moving the wrapper to 9.8.2.

### 6.2 Per-mixin "done" criteria (used by every worker)

A mixin port is complete only when all five hold:

1. Target class/method **verified** against `client-26.2.jar` (or decompiled Loom sources), with the
   descriptor pasted into the commit message / work log;
2. `@Mixin` applies at boot: no `MixinApplyError`, `@Inject` `require = 1` satisfied
   (run with `-Dmixin.debug.export=true` once and inspect the exported class);
3. the config gate works in both states (enabled → behaviour changes; disabled → vanilla);
4. a GameTest (or a manual step, documented) asserts the user-visible behaviour, not the internal call;
5. environment annotation is right (`@Environment(EnvType.CLIENT)` / server-only target), and the
   dedicated-server boot does not classload client classes.

---

## 7. Workstream B — mixin port matrix (all 42)

Strategy codes: **R** direct rename, **H** host-class/method moved, **E** effect/behaviour redesign
(data-driven enchantments), **D** data/tag approach, **X** verify obsolescence.

| # | Mixin | 1.20.5 target | 26.2 target | Strategy | Risk |
|---|---|---|---|---|---|
| 1 | client/anvil/NotTooExpensive | `AnvilScreen.drawForeground` | `AnvilScreen.extractLabels` / `extractRenderState` | H | M |
| 2 | client/lang/RomanNumeral | `TranslationStorage.get/hasTranslation` | `net.minecraft.client.resources.language.ClientLanguage.getOrDefault(String,String)` / `has(String)` (verified) | H | L |
| 3 | client/tweak/ShinyName | `Enchantment.getName(int)` | `Enchantment.getFullname(Holder,int)` | H | M |
| 4 | server/anvil/CheapNames | `AnvilScreenHandler.updateResult` | `AnvilMenu.createResult` | R | L |
| 5 | server/anvil/NotTooExpensive | `updateResult` (ordinal 2) | `createResult` | R | M |
| 6 | anvil/PriorWorkCheaper | `getNextCost(int)` | `AnvilMenu.calculateIncreasedRepairCost(int)` | R | M |
| 7 | anvil/PriorWorkFree | `updateResult` | `createResult` | R | M |
| 8 | anvil/SturdyAnvils | `method_24922(...)` | `lambda$onTake$0` / `onTake` (verified: the `0.12f` anvil-damage chance lives next to `RandomSource.nextFloat` in this method) | H | M |
| 9 | capmod/Capmod | `Enchantment.getMaxLevel` | same (record method) | R | L |
| 10 | enhanced/MoreBinding | `PlayerInventory.dropAll` | `Inventory.dropAll` + `EnchantmentHelper.processEquipmentDropChance` | H/E | H |
| 11 | enhanced/MoreBlastProtection | `ProtectionEnchantment.transformExplosionKnockback` | `ServerExplosion.hurtEntities()` wrap of `LivingEntity.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE)` (blast protection is an attribute now) | E | H |
| 12 | enhanced/MoreChanneling | `TridentEntity.onEntityHit` (`isThundering`) | `WeatherCheck.test(LootContext)` — channeling's `post_attack` effect is gated by `weather_check(thundering)`; read `LootContextParams.ENCHANTMENT_LEVEL` to allow rain at level > 1 | E | H |
| 13 | enhanced/MoreFireProtection | `ProtectionEnchantment.transformFireDuration` | fire-tick handling / `DAMAGE_IMMUNITY` | E | H |
| 14 | enhanced/MoreFlame | `PersistentProjectileEntity.applyEnchantmentEffects` + NBT | `AbstractArrow` + `ValueInput/ValueOutput` | H | M |
| 15 | enhanced/MoreFlameRangedWeapon | `RangedWeaponItem.createArrowEntity` | `ProjectileWeaponItem.createProjectile` | H | M |
| 16 | enhanced/MoreInfinity | `RangedWeaponItem.getProjectile/isInfinity` | `ProjectileWeaponItem.draw/useAmmo` + `EnchantmentHelper.processAmmoUse` | H/E | H |
| 17 | enhanced/MoreLooting | `LivingEntity.dropXp` | `LivingEntity.dropExperience(ServerLevel, Entity)` -> `ExperienceOrb.award(...)` (verified) | H | M |
| 18 | enhanced/MoreMending | `ExperienceOrbEntity.repairPlayerGears/getMendingRepairCost` | `ExperienceOrb.repairPlayerItems` + `EnchantmentHelper.modifyDurabilityToRepairFromXp` + `Enchantment.modifyDurabilityToRepairFromXp` (verified: the orb calls the helper, which iterates enchantments) | E | H |
| 19 | enhanced/MoreMultishot | `RangedWeaponItem.load` (constant 3) | `ProjectileWeaponItem.draw` / `processProjectileCount` | E | M |
| 20 | enhanced/MoreProtection | `LivingEntity.modifyAppliedDamage` + `DamageUtil` | `LivingEntity.getDamageAfterMagicAbsorb(DamageSource,float)` wrapping `CombatRules.getDamageAfterMagicAbsorb(float,float)` (both verified) | E | M |
| 21 | grindstone/GrindstoneDisenchant | `GrindstoneScreenHandler.updateResult` | `GrindstoneMenu.createResult` (private) | R | M |
| 22 | grindstone/GrindstoneInputSlot | inner classes 2/3 | inner classes 2/3 (verify ordinals) | H | M |
| 23 | grindstone/GrindstoneOutputSlot | inner class 4 | inner class 4 | H | M |
| 24 | tweak/AxeWeapons | `Enchantment.canAccept` | `Enchantment.areCompatible` / `isSupportedItem` + tags | D/E | M |
| 25 | tweak/AxesNotTools | `MiningToolItem.postHit` | `Item.postHurtEnemy` or tag removal | D/X | M |
| 26 | tweak/BetterMending | `ExperienceOrbEntity.repairPlayerGears` | `ExperienceOrb.repairPlayerItems` | E | M |
| 27 | tweak/BowInfinityFix | `BowItem.use/onStoppedUsing` | `BowItem.releaseUsing` + `useAmmo` | H | M |
| 28 | tweak/BowLooting | `Enchantment.canAccept` | `isSupportedItem` + `enchantable/bow` tags | D/E | M |
| 29 | tweak/DisableEnchantmentsLoot | `EnchantRandomlyLootFunction.process` | `EnchantRandomlyFunction` | H | L |
| 30 | tweak/DisableEnchantments | `isAvailableForRandomSelection/isAvailableForEnchantedBookOffer` | tag-driven; filter in `getAvailableEnchantmentResults`/`EnchantRandomlyFunction`/trades | E | H |
| 31 | tweak/GodArmor | `ProtectionEnchantment.canAccept` | `areCompatible` + `exclusive_set/armor` | D/E | M |
| 32 | tweak/GodWeapons | `DamageEnchantment.canAccept` | `areCompatible` | D/E | M |
| 33 | tweak/InfiniteMending | `InfinityEnchantment.canAccept` | `areCompatible` + `exclusive_set/bow` | E | L |
| 34 | tweak/LoyalVoidTridents | `TridentEntity.tick` | `ThrownTrident.tick` | H | L |
| 35 | tweak/MultishotPiercing | `Multishot/Piercing.canAccept` | `areCompatible` | E | L |
| 36 | tweak/NoMendingUnbreaking | `Enchantment.canAccept` | `areCompatible` | E | L |
| 37 | tweak/NoSoulSpeedBacklash | `LivingEntity.addSoulSpeedBoostIfNeeded` | data `change_item_damage` / `Enchantment.modifyDurabilityChange` | E | M |
| 38 | tweak/NoThornsBacklash | `ThornsEnchantment.onUserDamaged` | thorns `change_item_damage` effect / `doPostAttack` | E | M |
| 39 | tweak/ProtectionBypass | `LivingEntity.modifyAppliedDamage` | Same `getDamageAfterMagicAbsorb` + `CombatRules` hook as #20; branch on `DamageSource`/`ResourceKey<DamageType>` from config | E | M |
| 40 | tweak/TridentWeapons | `Enchantment.isAcceptableItem/isPrimaryItem` | `isSupportedItem/isPrimaryItem` + tags | D/E | M |
| 41 | tweak/VillagerTradeLimits | `TradeOffer.isDisabled/resetUses` | `MerchantOffer.isOutOfStock()` / `needsRestock()` / `resetUses()` (all verified) + data-driven `villager_trade` registry / `#minecraft:tradeable` tag (§8.4) | H/D | H |
| 42 | tweak/XpScaling | `PlayerEntity.getNextLevelExperience` | `Player.getXpNeededForNextLevel` | R | L |

### 7.1 Non-mixin code

| File | Change |
|---|---|
| `EnchantTweaker` | `PayloadTypeRegistry.clientboundPlay()`, `ServerPlayNetworking.canSend(player, ID)` |
| `ConfigSyncPayload` | 26.2 `CustomPacketPayload`/`StreamCodec` shapes (stable, names verified) |
| `ETMixinPlugin` | unchanged structurally; `MixinConfigPlugin` still supported; confirm mixin list paths after moving tests |
| `ETConfigSchema`/`ADConfig` | pure Java; add unit tests; no MC API dependency |
| `ADUtils` | enchantment identity via `Holder<Enchantment>`/`ResourceKey` (no `BuiltInRegistries.ENCHANTMENT`); `ItemStack` component access; `Inventory` |
| `ETCommands`/`commands/*` | Brigadier unchanged; `Component` literals, sendFeedback/sendError signatures; permission checks |
| `client/*` | `GuiGraphicsExtractor`, `extractRenderState`, `Button/EditBox` input API, `Minecraft.gui` |
| `AnvilRepairHandler` | `UseBlockCallback` unchanged; `InteractionResult` same |
| `MendingLevelAccess`, `GrindstoneDisenchantAccess`, `FlameLevelAccess` | duck interfaces for `@Unique` state; check record/final targets |

### 7.2 Verified target map (from the Argon mapping audit)

A full per-mixin table with bytecode ordinals/offsets was produced during planning and lives at
`.plan/argon-mixin-mapping.md` (240 lines, every row checked with `javap -p -s -c` against
`client-26.2.jar`). Its headline corrections to the matrix above:

| Area | Verified 26.2 mechanism |
|---|---|
| Enchantment identity | `Enchantments.*` are `ResourceKey<Enchantment>`; registries are dynamic (`RegistryAccess.lookupOrThrow(Registries.ENCHANTMENT)`); `ItemEnchantments` stores `Holder<Enchantment>` |
| Anvil constants | `createResult` has three `bipush 40` (rename cap, 40-level gate, too-expensive wipe) — `ordinal 0/1/2` map to the old hooks; `calculateIncreasedRepairCost` is `public static int(int)`; anvil damage `0.12f` is in `lambda$onTake$0` |
| Player equipment | `PlayerInventory.armor/offHand/combinedInventory` are gone; `Inventory` has `items` + `EntityEquipment`; `EntityEquipment.dropAll(LivingEntity)` is the binding-curse hook |
| Entity persistence | `readAdditionalSaveData(ValueInput)` / `addAdditionalSaveData(ValueOutput)` replace NBT methods (`MoreFlame`) |
| GUI colors | `GuiGraphicsExtractor` requires full ARGB (alpha byte), else nothing renders |
| Chat events | `ClickEvent`/`HoverEvent` are sealed records (`new ClickEvent.RunCommand(...)`, `new HoverEvent.ShowText(...)`) |
| Russian-doll targets | `MoreProtection`/`ProtectionBypass` -> `LivingEntity.getDamageAfterMagicAbsorb` + `CombatRules.getDamageAfterMagicAbsorb`; `MoreBlastProtection` -> `ServerExplosion.hurtEntities` + `Attributes.EXPLOSION_KNOCKBACK_RESISTANCE`; `MoreChanneling` -> `WeatherCheck.test`; `NoSoulSpeedBacklash` -> `ChangeItemDamage.apply`; `NoThornsBacklash` -> thorns `DamageEntity`/`ChangeItemDamage` |
| Disable enchantments | `isAvailableForRandomSelection`/`isAvailableForEnchantedBookOffer` gone; block by returning `0` from `getMaxLevel` + `false` from `canEnchant`/`isSupportedItem`/`isPrimaryItem`, and filter `getAvailableEnchantmentResults`/`EnchantRandomlyFunction` |
| Axes | `MiningToolItem` gone; self-damage is `DataComponents.WEAPON`'s `Weapon.itemDamagePerAttack()` consumed in `ItemStack.postHurtEnemy` |
| Villager trades | `MerchantOffer.isOutOfStock()` (plus `needsRestock()`, `resetUses()`); offers come from the data-driven `villager_trade` registry + `#minecraft:tradeable` |
| Client language | `ClientLanguage.getOrDefault(String,String)`/`has(String)` (not `Language` directly) |


---

## 8. Workstream C — enchantment redesign decisions

Group the 20 "enhanced"/compatibility features into four mechanisms and **prototype each once**.

### 8.1 Level-scaling via `Enchantment` effect hooks (P1)

`Enchantment` exposes `modify*` methods taking an `int level`. A single generic mixin can scale the
level before the effect runs, keyed by enchantment id + config:

```java
@Mixin(Enchantment.class)
abstract class EnchantmentLevelScaleMixin {
    @ModifyVariable(method = "modifyProjectileCount", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int et$scaleProjectileLevel(int level, ServerLevel lvl, int l, ItemStack stack, Entity e, MutableFloat out) {
        // ...
    }
}
```

Candidates: `more_protection`, `more_fire_protection`, `more_blast_protection`, `more_multishot`,
`more_mending`, `more_binding`, `more_flame`, `more_infinity`, `more_looting`.

**Decision rule:** if the vanilla effect is computed through a method that receives the level, scale
the level; otherwise use P4.

### 8.2 Compatibility (P2/P3)

`Enchantment.areCompatible` is the single choke point for "which enchantments can coexist".
A static mixin can return `true`/`false` for configured pairs (`god_armor`, `god_weapons`,
`infinite_mending`, `multishot_piercing`, `no_mending_unbreaking`, `bow_looting`,
`axe_weapons`, `trident_weapons`).

Item-applicability features (`bow_looting`, `axe_weapons`, `trident_weapons`, `axes_not_tools`) may be
cleaner as **tag overrides** (`enchantable/bow`, `enchantable/sharp_weapon`, `enchantable/mining`).
Note: vanilla 26.2 **already** puts axes in `enchantable/sharp_weapon` (so `axe_weapons` may be
partly obsolete **[verify]**), and soul-speed/thorns backlash are now data effects.

### 8.3 Effect-data interception (P4)

- `more_looting`: XP scaling at `LivingEntity.dropExperience` (verify method name) — same as today.
- `more_mending`: `EnchantmentHelper.modifyDurabilityToRepairFromXp` + `ExperienceOrb.repairPlayerItems`.
- `more_infinity`: `EnchantmentHelper.processAmmoUse` / `ProjectileWeaponItem.useAmmo`.
- `no_thorns_backlash` / `no_soul_speed_backlash`: intercept the `change_item_damage` effect
  application (e.g. `Enchantment.modifyDurabilityChange` or the `ChangeItemDamage` effect class)
  and skip for thorns/soul_speed when the flag is on.
- `disable_enchantments`: filter `EnchantmentHelper.getAvailableEnchantmentResults`,
  `EnchantRandomlyFunction`, and merchant-offer generation by id.

### 8.4 Villager trades are data-driven in 26.2 (affects 43 config keys: 39 `trade_*` + 4 trade controls)

Verified in the 26.2 jar: trades moved out of Java inner classes into the **`villager_trade` registry**,
e.g. `data/minecraft/villager_trade/librarian/1/emerald_and_book_enchanted_book.json`:

@@@json
{
  "wants": { "id": "minecraft:emerald", "count": 0.0 },
  "additional_wants": { "id": "minecraft:book" },
  "gives": { "id": "minecraft:enchanted_book" },
  "given_item_modifiers": [
    { "function": "minecraft:enchant_randomly", "options": "#minecraft:tradeable", ... },
    { "function": "minecraft:filtered", "on_fail": { "function": "minecraft:discard" } }
  ],
  "max_uses": 12.0,
  "double_trade_price_enchantments": "#minecraft:double_trade_price"
}
@@@

Consequences for `villager_trade_limits` + the 45 `trade_*` toggles:

- "which enchantments can be sold" is now the **`minecraft:tradeable` enchantment tag** consumed by
  `enchant_randomly`; the natural implementation is an options filter on
  `EnchantRandomlyFunction` when the loot context is a merchant trade (the mod already patches that
  class for loot in `DisableEnchantmentsLootMixin`), or a tag override built at datapack load.
- "max uses / restock" is per-trade `max_uses` plus `MerchantOffer.needsRestock()/resetUses()`
  (`MerchantOffer` verified in 26.2).
- per-enchantment trade toggles can share the same "disabled enchantment" filter used by
  `disable_enchantments` — one code path, two config groups.

This replaces the old `TradeOffer.isDisabled()/resetUses()` mixin; expect a rewrite, not a rename.

### 8.5 Verified hook inventory (vanilla data -> Java hook)

All hooks below were confirmed present in the 26.2 jar with `javap`. Workers should start from this
table instead of rediscovering it.

| Config group | Vanilla mechanism (data) | Java hook (26.2) |
|---|---|---|
| more_protection, more_fire_protection | `minecraft:damage_protection` effect (EPF) | `Enchantment.modifyDamageProtection(...)`, `EnchantmentHelper.getDamageProtection(...)` |
| more_blast_protection | `minecraft:knockback` effect | `Enchantment.modifyKnockback(...)` |
| more_multishot | `minecraft:projectile_count` | `Enchantment.modifyProjectileCount(...)`, `EnchantmentHelper.processProjectileCount(...)` |
| more_infinity | `minecraft:ammo_use` | `Enchantment.modifyAmmoCount(...)`, `EnchantmentHelper.processAmmoUse(...)` |
| more_mending | `minecraft:repair_with_xp` | `Enchantment.modifyDurabilityToRepairFromXp(int level, ...)` inside `EnchantmentHelper.modifyDurabilityToRepairFromXp(...)` (both verified) |
| more_binding | `prevent_armor_change` + equipment drop chance | `EnchantmentHelper.processEquipmentDropChance(...)`, `Inventory.dropAll()` |
| more_looting | `equipment_drops` + loot tables | `LivingEntity.dropExperience(...)` (XP tweak) |
| more_flame | `post_attack` -> `minecraft:ignite` | `Enchantment.doPostAttack(...)`, `AbstractArrow` |
| more_channeling | `post_attack` -> `minecraft:summon_entity` (lightning) | `Enchantment.doPostAttack(...)`, `ThrownTrident` |
| better_mending | `repair_with_xp` on XP pickup | `ExperienceOrb.repairPlayerItems(...)` |
| infinite_mending, god_armor, god_weapons, multishot_piercing, no_mending_unbreaking | `exclusive_set` tags | `Enchantment.areCompatible(Holder, Holder)` (static) |
| bow_looting, axe_weapons, trident_weapons | `supported_items`/`primary_items` tags | `Enchantment.isSupportedItem(...)`, `isPrimaryItem(...)`, tag overrides |
| no_thorns_backlash, no_soul_speed_backlash | `change_item_damage` inside `post_attack` / `location_changed` | `Enchantment.modifyDurabilityChange(...)` or filter the effect list |
| disable_enchantments | `#minecraft:in_enchanting_table`, `#minecraft:tradeable` tags | `EnchantmentHelper.getAvailableEnchantmentResults(...)`, `EnchantRandomlyFunction` |
| trade_* (45 keys) | `villager_trade` registry + `#minecraft:tradeable` | filter `enchant_randomly` options for merchant contexts (§8.4) |
| capmod (41 keys) | `max_level` in `EnchantmentDefinition` | `Enchantment.getMaxLevel()` |
| axes_not_tools, axe_weapons | item tags `enchantable/mining` / `enchantable/sharp_weapon` | tag override or `isSupportedItem` |

### 8.6 New vanilla enchantments in 26.2 (config completeness)

26.2 ships **43** vanilla enchantments (`data/minecraft/enchantment/*.json`), four of which did not
exist in 1.20.5 and are missing from the mod's config surface:

| Enchantment | Source | Needs config keys |
|---|---|---|
| `density` | 1.21 mace | `capmod` cap + `trade_density` + schema/properties/lang |
| `breach` | 1.21 mace | `capmod` cap + `trade_breach` + schema/properties/lang |
| `wind_burst` | 1.21 mace | `capmod` cap + `trade_wind_burst` + schema/properties/lang |
| `lunge` | 26.2 spear | `capmod` cap + `trade_lunge` + schema/properties/lang |

Decide explicitly: either add them (recommended: feature completeness, and `god_weapons` should cover
`density`/`breach`) or document them as out of scope in the README/changelog. This must be settled
before M6 because it changes the config contract (140 -> 148 keys) and the acceptance tests.

### 8.7 Behavioural spec source

The 13.5k LOC GameTests are the authoritative spec. Port them **first** (M5 starts in parallel with
M4 per subsystem) and treat failures as behavioural regressions, not test bugs. Where a test relied
on a removed internal (e.g. `EnchantmentHelper.getEquipmentLevel`), rewrite the assertion against
observable behaviour (damage numbers, item components, trade lists).

---

## 9. Workstream D — client UI migration

`ETConfigScreen` (260 LOC) and `ETConfigValueScreen` (167 LOC) plus `ETConfigScreenModel` (167 LOC, pure
logic) and `ETModMenuIntegration`.

1. Keep `ETConfigScreenModel` untouched (pure Java; add unit tests).
2. Port drawing: `drawTextWithShadow` -> `ctx.text(font, component, x, y, color)`;
   `fill` -> `ctx.fill`; scissor unchanged; `renderBackground` -> `extractBackground`.
3. Event hooks (verified): `mouseClicked(MouseButtonEvent, boolean)`, `mouseReleased(MouseButtonEvent)`,
   `mouseDragged(MouseButtonEvent, double, double)`, `keyPressed(KeyEvent)`, `charTyped(CharacterEvent)`.
4. `Button.builder(Component, OnPress).bounds(x,y,w,h).build()`; `onPress(InputWithModifiers)`.
5. Mod Menu 20 (verified): `com.terraformersmc.modmenu.api.ModMenuApi.getModConfigScreenFactory()`
   returns `ConfigScreenFactory<?>`; the interface is unchanged from 1.x, only the MC types inside it moved.
6. Add client GameTests for: open screen from title, search, edit value, save, reset; plus a
   screenshot artefact for review.
---

## 10. Workstream E — tests, CI, release

### 10.1 Test pyramid

| Layer | What | Tool |
|---|---|---|
| Unit | config parse/validate/migrate, roman numerals, math helpers, schema | JUnit 5 + `fabric-loader-junit` |
| GameTest (server) | anvil costs, grindstone, enchant compatibility, mending/xp, trades, commands, payload | vanilla `GameTestHelper` + `fabric-gametest` |
| Client GameTest | config screen, tooltips, roman numerals rendering | `fabric-client-gametest` (verified API: `FabricClientGameTest`, `ClientGameTestContext`, `TestWorldBuilder`, `TestInput`, `TestScreenshotOptions`) |
| Manual | singleplayer, dedicated server, multiplayer sync, Mod Menu | checklist §14 |

### 10.2 Porting order for 13.5k LOC of tests

1. `ETTestHelper` (setup, config snapshot/restore, anvil output helpers, reflection fixes).
2. `ADConfigGameTest`, `ConfigSchemaGameTest`, `PluginConfigGameTest` (no MC gameplay).
3. `AnvilGameTest` -> `GrindstoneGameTest` -> `TweakGameTest` -> `EnhancedGameTest` -> `CapmodGameTest`
   -> `CommandsGameTest` -> `PayloadGameTest` -> `RomanNumeralGameTest`/`ShinyNameGameTest`.
4. Add the missing `trade_*` coverage while porting `VillagerTradeLimits`.

Expect renames in helpers (setBlock, assertBlockPresent, and the new GameTestEntityBuilder/
GameTestMobBuilder spawn API in 26.2).

#### 10.2.1 Test API mapping (verified against 26.2 + fabric-gametest-api-v1 4.0.43)

| Old (repo) | New (26.2) |
|---|---|
| net.minecraft.test.TestContext | net.minecraft.gametest.framework.GameTestHelper |
| net.minecraft.test.GameTest (annotation) | net.fabricmc.fabric.api.gametest.v1.GameTest |
| templateName = EMPTY_STRUCTURE | structure = "" / default; attributes are environment, dimension, structure, maxTicks, setupTicks, required, rotation, manualOnly, maxAttempts, requiredSuccesses, skyAccess, padding |
| implements FabricGameTest | implements CustomTestMethodInvoker (only when per-test setup is needed) |
| helper.setBlockState(pos, state) | helper.setBlock(pos, state) |
| helper.expectBlock(block, pos) | helper.assertBlockPresent(block, pos) |
| helper.dontExpectBlock(...) | helper.assertBlockNotPresent(...) |
| helper.complete() | helper.succeed() |
| Identifier.of(ns, path) | Identifier.fromNamespaceAndPath(ns, path) |
| CustomPayload.Id / PacketCodec / RegistryByteBuf | CustomPacketPayload.Type / StreamCodec / RegistryFriendlyByteBuf |

### 10.3 CI (github workflow ci.yml)

```yaml
- uses: actions/setup-java@...
  with: { distribution: temurin, java-version: 25 }
- run: ./gradlew build --console=plain          # includes server gametests
- run: ./gradlew runClientGameTest --console=plain
- uses: actions/upload-artifact@...
  with: { path: build/run/clientGameTest/screenshots }
```

### 10.4 Release

- `mod_version=2.0.0`, MC `26.2`, loaders `fabric`.
- Release workflow JDK 25; changelog extraction unchanged.
- Modrinth: `gameVersions=["26.2"]`, required `fabric-api`, optional `modmenu`.
- Tag `v2.0.0+mc26.2`.

---

## 11. Skills & MCPs — research results

### 11.1 Already available in this session

- **context7 MCP** with Fabric docs: `/fabricmc/fabric-docs` (1,706 snippets) and
  `/fabricmc/fabric-api` (5,360 snippets), plus `/websites/fabricmc_net` (versions 1.21.1/1.21.11 —
  too old for 26.2). Use for Loader/API/how-to questions.
- **Delegation skills**: `delegate` (agy/opencode), `delegate-to-agy`, `delegate-to-opencode`,
  `delegate-to-any`; **planning/review models** `gemini-4-argon-high` (primary) and
  `claude-opus-5-5-high` (fallback), per the user's instruction.
- **find-docs / context7-docs** skills for library lookups.

### 11.2 Recommended external MCP servers (install before M3)

| MCP | Why | Install |
|---|---|---|
| `@adhisang/minecraft-modding-mcp` | 41 tools: browse/search decompiled MC source, convert obf/yarn/intermediary/mojmap names, diff class signatures between versions, validate Mixin/AW files, inspect mod jars. Ideal for target verification without juggling javap. | `npx -y @adhisang/minecraft-modding-mcp` (Node >=22.13, Java >=17) |
| `@mcdxai/minecraft-dev-mcp` | Decompile/remap/search MC sources; also ships a CLI (`minecraft-dev-cli`) usable from scripts and CI. | `npx -y @mcdxai/minecraft-dev-mcp` |
| `mcmodding-mcp` | Semantic search over Fabric/NeoForge docs + code examples. | `npm i -g mcmodding-mcp` |
| `fabric-docs-mcp` | Lightweight docs-only alternative. | `npx -y fabric-docs-mcp` |

Add them through the harness MCP settings (stdio command + args). If an MCP cannot be added, the
fallback loop is: `./gradlew genSources` + `grep`/`javap` against the Loom cache or a downloaded
jar (what this plan already used).

### 11.3 Skills to create for the port (project-local)

1. `mc26-target-verify` — given a Yarn symbol or FQCN, locate the 26.2 class, confirm the method
   descriptor via `javap`/decompiled sources, and record evidence in the mapping file.
2. `mc26-gametest` — scaffold/run server + client GameTests with the new Loom DSL.
3. `fabric-26-migration` — checklist that encodes the §4 deltas (API renames, GUI, data components,
   networking).
4. Optional: mixin-debugging skill (export, audit, apply errors).

Where: `~/.dsh/skills/` (per-user) or a repo `.dsh/skills/` if the harness supports project skills;
otherwise document the procedures in `.github/PORTING.md`.

### 11.4 Research sources used (and to cite in the PR)

- Fabric blog: *Removing Obfuscation from Fabric* (2025-10-31), *Fabric for Minecraft 26.1* (2026-03-14),
  *Fabric for Minecraft 26.2* (2026-06-15).
- Fabric docs: *Porting to 26.2*, *Migrating Mappings* + *Loom* tab (migrateMappings,
  migrateClientMappings, migrateClassTweakerMappings), *Automated Testing*, *fabric.mod.json*.
- NeoForge migration primers: `https://docs.neoforged.net/primer/docs/26.1/` and `/26.2/`
  (ChampionAsh5357) — the most complete vanilla-change list.
- Minecraft Wiki: `Java Edition 26.2`.
- Real-world migration write-up: `xxfast/fabric-witness/MIGRATION.md` (1.21.11 -> 26.2), incl.
  `PayloadTypeRegistry` / GUI / ModMenu 20 churn.
- Fabric example mod + `fabric-docs/reference/latest` (26.2 test setup, client gametests).

---

## 12. Risk register

| ID | Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|---|
| R1 | Enchantment redesign (10 mixins) takes longer than estimated | High | High | Prototype P1/P2/P4 once in a spike branch; choose per feature by passing test; timebox |
| R2 | `migrateMappings` produces wrong/colliding renames | High | Medium | Two-stage migration; review diff; compile-fix loop; git tags per phase |
| R3 | Mixin target descriptors silently wrong (no refmap errors) | Medium | High | `-Dmixin.debug.export=true`; assert mixin count; boot client+server each milestone |
| R4 | GUI rewrite (`extractRenderState`) breaks the config screen | Medium | Medium | Port screen early; client GameTest + screenshots |
| R5 | Test harness migration loses coverage | Medium | High | Port tests before behaviours; baseline (104 keys covered); CI gate |
| R6 | Toolchain incompatibility (Loom/Gradle/Java) | Medium | High | Version fallback matrix §5.7; spike M2 first |
| R7 | Mod Menu 20 API changes | Low | Low | Optional dependency; guard entrypoint |
| R8 | Networking payload API churn | Low | Medium | API verified in 26.2 jars (`clientboundPlay`) |
| R9 | 26.2 Vulkan/rendering changes affect client mixins | Low | Medium | Config UI only draws text/fill; anvil hint uses labels |
| R10 | Quilt support loss | Medium | Low | Document in README/changelog |
| R11 | Config contract misses 4 new 26.2 enchantments (`density`, `breach`, `wind_burst`, `lunge`) | High | Medium | Decide in §8.6 before M6; add capmod/trade keys + schema + lang, or document as out of scope |
| R12 | GUI colour literals lose alpha in `GuiGraphicsExtractor` (invisible text) | High | Medium | Wrap all colours as ARGB (`0xFF000000 \| rgb`) in the config screens; client GameTest screenshot check |
| R13 | `Inventory` armour moved to `EntityEquipment`, breaking binding-curse drop logic | Certain | High | Port to `EntityEquipment.dropAll(LivingEntity)` + `player.getItemBySlot(...)` (§7.2) |
| R14 | `createResult` prior-work cost changed type (`int` -> `long`), silently breaking a `@ModifyConstant` | Certain | High | Verified: use `@ModifyConstant(longValue = 0L, ordinal = 0)` in `PriorWorkFreeMixin` |
| R15 | `ThrownTrident` passes no weapon to `AbstractArrow`, so knockback/looting may not apply to thrown tridents | Medium | Medium | Verify with a GameTest; if needed, mixin the trident constructor or `doKnockback` |

---

## 13. Delegation & review protocol (as requested)

1. **Planning** — Argon High (`gemini-4-argon-high`, `[plan]`) drafts the phase/task plan and the
   mixin matrix. Fallback: `claude-opus-5-5-high`.
2. **Implementation** — parallel workers per subsystem, each with the §7 rows, the verified target
   list, and the requirement to attach `javap` evidence; they must run `./gradlew build` and report
   the test command output.
3. **Review** — Argon High reviews the merged diff against this plan (checklist: no Yarn names, no
   `modImplementation` leftovers, mixin targets verified, tests updated, config keys preserved).
   Fallback: Opus 5.5 High.
4. **Human gates** — after M1 (mapping diff), after M4 (mixin audit), before M6 (release).

---

## 14. Definition of done

- [ ] Build pins 26.2, Loader 0.20.1, Fabric API 0.161.0+26.2, Loom 1.19.3, Gradle 9.8.2, Java 25.
- [ ] `grep -rE "net\\.minecraft\\.(enchantment|entity|screen|item)\\." src` returns nothing Yarn-ish.
- [ ] All 42 mixins applied (log count) and covered by at least one test or manual check.
- [ ] All 139 options (+ `config_version` = 140 entries) still load, validate, reload, sync, and appear in `/et config list`.
- [ ] Server GameTests green in `build`; client GameTest green; screenshots uploaded.
- [ ] Manual matrix: singleplayer anvil/enchant/grindstone/mending; dedicated server; multiplayer
      join-sync; Mod Menu screen; `/et diagnose`.
- [ ] README/CHANGELOG/Modrinth updated; Quilt claim removed or verified.
- [ ] Release `2.0.0+mc26.2` published.

---

## 15. Task backlog (ordered, with estimates)

| # | Task | Est. | Owner |
|---|---|---|---|
| 1 | Branch/tag baseline, freeze artefact, run baseline build | 0.5d | lead |
| 2 | M1 migrateMappings main + gametest, compile-fix | 1.5d | worker A |
| 3 | M2 toolchain bump + spike build | 1d | worker A |
| 4 | M3 compile-green sweep (main) | 3d | workers A+B |
| 5 | M3 compile-green sweep (gametest) | 2d | worker C |
| 6 | Anvil subsystem (6 mixins + tests) | 1d | worker B |
| 7 | Grindstone subsystem (3 + tests) | 1d | worker B |
| 8 | XP/mending subsystem (4 + tests) | 1.5d | worker C |
| 9 | Projectile/trident subsystem (5 + tests) | 1.5d | worker C |
| 10 | Enchantment definition redesign (10 + tests) | 3–4d | worker B (spike first) |
| 11 | Client UI (3 mixins + screens + client gametest) | 2–3d | worker D |
| 12 | Trades (data-driven rewrite), commands (permissions), networking | 2.5d | worker A |
| 13 | Test harness migration + unit tests | 2d | worker D |
| 14 | CI/release/docs | 1d | lead |
| 15 | Review passes (Argon), fixes | 1–2d | lead |

Total ≈ 22–28 engineer-days; with 3–4 parallel workers ≈ 7–10 calendar days.

---

## Appendix A — exact commands

```bash
# M1: migrate at 1.20.5 (JDK 21)
JAVA_HOME=/path/to/jdk21 ./gradlew migrateMappings \
  --mappings "net.minecraft:mappings:1.20.5" \
  --input src/main/java --output remappedSrc --overrideInputsIHaveABackup
JAVA_HOME=/path/to/jdk21 ./gradlew migrateMappings \
  --mappings "net.minecraft:mappings:1.20.5" \
  --input src/gametest/java --output remappedGametestSrc --overrideInputsIHaveABackup

# M2: toolchain
./gradlew wrapper --gradle-version 9.8.2
./gradlew --version
./gradlew build --console=plain

# M4/M5 test loop
./gradlew runClientGameTest --console=plain
./gradlew test --console=plain

# Target verification without an MCP
unzip -l ~/.gradle/caches/.../client-26.2.jar | grep Enchantment
javap -p -classpath client-26.2.jar net.minecraft.world.item.enchantment.Enchantment
```

## Appendix B — Yarn -> Mojang cheat sheet (used by this mod)

| Yarn | Mojang 26.2 |
|---|---|
| `net.minecraft.text.Text/MutableText` | `net.minecraft.network.chat.Component/MutableComponent` |
| `net.minecraft.util.Identifier` | `net.minecraft.resources.Identifier` |
| `net.minecraft.registry.Registries` | `net.minecraft.core.registries.BuiltInRegistries` (static registries only; enchantments are dynamic — see §7.2) |
| `net.minecraft.registry.RegistryKeys` | `net.minecraft.core.registries.Registries` |
| `net.minecraft.enchantment.Enchantment` | `net.minecraft.world.item.enchantment.Enchantment` |
| `net.minecraft.enchantment.Enchantments` | `net.minecraft.world.item.enchantment.Enchantments` |
| `net.minecraft.enchantment.EnchantmentHelper` | `net.minecraft.world.item.enchantment.EnchantmentHelper` |
| `net.minecraft.enchantment.InfoEnchantment` | `EnchantmentInstance` |
| `net.minecraft.item.ItemStack` | `net.minecraft.world.item.ItemStack` |
| `net.minecraft.item.Items` | `net.minecraft.world.item.Items` |
| `net.minecraft.item.Item` | `net.minecraft.world.item.Item` |
| `net.minecraft.item.* components` | `net.minecraft.core.component.DataComponents` |
| `net.minecraft.entity.LivingEntity` | `net.minecraft.world.entity.LivingEntity` |
| `net.minecraft.entity.player.PlayerEntity` | `net.minecraft.world.entity.player.Player` |
| `net.minecraft.entity.player.PlayerInventory` | `net.minecraft.world.entity.player.Inventory` |
| `net.minecraft.entity.ExperienceOrbEntity` | `net.minecraft.world.entity.ExperienceOrb` |
| `net.minecraft.entity.projectile.PersistentProjectileEntity` | `net.minecraft.world.entity.projectile.arrow.AbstractArrow` |
| `net.minecraft.entity.projectile.TridentEntity` | `net.minecraft.world.entity.projectile.arrow.ThrownTrident` |
| `net.minecraft.entity.damage.DamageSource` | `net.minecraft.world.damagesource.DamageSource` |
| `net.minecraft.world.World` | `net.minecraft.world.level.Level` |
| `net.minecraft.server.world.ServerWorld` | `net.minecraft.server.level.ServerLevel` |
| `net.minecraft.screen.AnvilScreenHandler` | `net.minecraft.world.inventory.AnvilMenu` |
| `net.minecraft.screen.GrindstoneScreenHandler` | `net.minecraft.world.inventory.GrindstoneMenu` |
| `net.minecraft.screen.ScreenHandler` | `net.minecraft.world.inventory.AbstractContainerMenu` |
| `net.minecraft.screen.slot.Slot` | `net.minecraft.world.inventory.Slot` |
| `net.minecraft.village.TradeOffer` | `net.minecraft.world.item.trading.MerchantOffer` |
| `net.minecraft.command.CommandRegistryAccess` | `net.minecraft.commands.CommandBuildContext` |
| `net.minecraft.server.command.ServerCommandSource` | `net.minecraft.commands.CommandSourceStack` |
| `net.minecraft.client.gui.screen.Screen` | `net.minecraft.client.gui.screens.Screen` |
| `net.minecraft.client.gui.DrawContext` | `net.minecraft.client.gui.GuiGraphicsExtractor` |
| `net.minecraft.client.resource.language.TranslationStorage` | `net.minecraft.locale.Language` |
| `net.minecraft.client.MinecraftClient` | `net.minecraft.client.Minecraft` |
| `net.minecraft.client.network.ClientPlayerEntity` | `net.minecraft.client.player.LocalPlayer` |
| `net.minecraft.nbt.NbtCompound` | `net.minecraft.nbt.CompoundTag` + `ValueInput/ValueOutput` |
| `ServerPlayNetworking.send/canSend` | unchanged (payload registries renamed: `clientboundPlay`) |

## Appendix C — references

- Fabric blog *Fabric for Minecraft 26.2* — https://fabricmc.net/2026/06/15/262.html
- Fabric blog *Removing Obfuscation from Fabric* — https://fabricmc.net/2025/10/31/obfuscation.html
- Fabric docs *Migrating Mappings* — https://docs.fabricmc.net/develop/porting/mappings/
- Fabric docs *Loom mapping migration* — https://docs.fabricmc.net/1.21.11/develop/porting/mappings/loom
- Fabric docs *Automated Testing* — https://docs.fabricmc.net/develop/automatic-testing
- NeoForge primers — https://docs.neoforged.net/primer/docs/26.2/ and https://docs.neoforged.net/primer/docs/26.1/
- Fabric example mod — https://github.com/FabricMC/fabric-example-mod
- Fabric docs reference project (26.2 test setup) — https://github.com/FabricMC/fabric-docs/tree/main/reference/latest
- Real-world 26.2 migration notes — https://github.com/xxfast/fabric-witness/blob/master/MIGRATION.md
- Minecraft Wiki 26.2 — https://minecraft.wiki/w/Java_Edition_26.2

---

## Appendix D — open questions

1. ~~Does the 26.2 build of `fabric-api` include the gametest modules via Loom's DSL?~~ **Resolved:**
   the modules are separate Maven artifacts (`fabric-gametest-api-v1` 4.0.x,
   `fabric-client-gametest-api-v1` 6.0.x) that Loom's `fabricApi.configureTests` adds; both declare
   `fabricloader >=0.20.0`, which pins the loader choice.
2. Exact 26.2 signatures for `MouseButtonEvent`/`KeyEvent` and `InputWithModifiers` used by the
   config screen.
3. Which mechanism (P1 vs P4) preserves behaviour for `more_protection` (vanilla now computes
   protection via `EnchantmentHelper.getDamageProtection` + EPF)?
4. Whether `axe_weapons` is now vanilla (axes are in `enchantable/sharp_weapon`) and should be
   deprecated instead of ported.
5. Whether Quilt Loader supports 26.2 (determines dropping the Quilt smoke test).

---

## Appendix E — planning and review provenance

| Step | Model / lane | Result |
|---|---|---|
| Platform research (versions, blogs, docs, primer, live maven/meta APIs) | lead agent, live fetches + `javap` | verified 2026-11-03 |
| Baseline build | local JDK 21 + Gradle 9.3.1 | green, 6m24s, jar `enchanttweaker-1.6.0+mc1.20.5.jar` |
| Planning draft | agy / Gemini 4 Argon (High), `[plan]` | `.plan/argon-port-plan.md` (954 lines) |
| Mixin/API mapping | agy / Gemini 4 Argon (High) | `.plan/argon-mixin-mapping.md` (240 lines, javap-verified) |
| Plan review | agy / Gemini 4 Argon (High) | timed out at provider stage; superseded by the fallback below |
| Fallback audit | agy / Claude Opus 5.5 (High) | provider quota exhausted mid-run |
| Final review | DSH subagent (opencode-go) | recorded in `.plan/` |
| Synthesis + verification | lead agent | this file; spot-checked every correction against `client-26.2.jar` |

Notes:
- The agy provider hit its individual quota during the run (`stage: prompt, category: quota`), which is
  why the review lane failed; the mapping and planning artefacts were still produced and are kept as
  evidence.
- Key Argon findings were independently re-verified by the lead before being folded in:
  dynamic enchantment registry, `createResult` `long` constant, `CombatRules`/`getDamageAfterMagicAbsorb`,
  `ClientLanguage`, `MerchantOffer.isOutOfStock`, `Enchantments.*` being `ResourceKey`s,
  `EntityEquipment`, and the 43-enchantment roster.

