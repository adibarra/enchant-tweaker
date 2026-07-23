# Wave 1 triage audit

- Wave: 1
- Reviewed files: 121 across the four independent reports; no duplicate file entries.
- Original findings: 60 unique IDs after title/file/line deduplication. No IDs were silently dropped or merged.
- Final triage: 32 VALID, 26 INVALID, 2 NEEDS-CONTEXT.
- Repository instructions: no AGENTS.md was present at the repository root or below it when checked.
- Implementation status below describes the current working-tree diff, which was inspected separately from the triage verdict. `FIXED` means the cited behavior is addressed in the current diff; `OPEN` means the behavior still needs implementation; `N/A` means the report is false or intentionally supported; `CONTEXT` means a product-contract decision is still required.

## Findings

- **W1-001 - INVALID - N/A (fixed before/at triage).** The current `.github/scripts/quilt-server-smoke.sh:90-120,128-141` pins SHA-256 values and checks both downloaded artifacts before execution or copying. The old claim that the installer and Fabric API were unverified is stale. Source check: current script and `sha256sum --check`; no remaining implementation item.
- **W1-002 - INVALID - N/A (fixed).** `.github/scripts/quilt-server-smoke.sh:73-81` collects matching properties and exits unless exactly one value exists, so duplicate keys cannot become embedded newlines. Source check: `read_property` and its five callers at lines 84-88. No implementation item.
- **W1-003 - INVALID - N/A (fixed).** `.github/workflows/label-syncer.yml:22-24` grants `contents: read` and `issues: write`. GitHub Actions permissions documentation assigns issue administration to `issues: write`; the former missing-permission claim is stale. No implementation item.
- **W1-004 - INVALID - N/A (documentation corrected).** `README.md:99-100` now states the formula and the Binding I 100% drop baseline consistently with `ADUtils.java:114-120`, where `dropChance` is clamped and the item is kept only when the roll reaches that threshold. The vanilla premise used by the original report was also incorrect; Curse of Binding normally drops the equipped item on death. Sources checked: current README, implementation, Oracle `ThreadLocalRandom.nextFloat` contract, and Minecraft Wiki Curse of Binding page.
- **W1-005 - INVALID - N/A (documentation corrected).** `README.md:20-25` says client installation is required for singleplayer and the matrix says the same. The former contradiction no longer exists.
- **W1-006 - INVALID - N/A (fixed).** `build.gradle:98-111` reads `layout.projectDirectory.file("CHANGELOG.md")`; Gradle ProjectLayout resolves this against the project directory, including when Gradle is invoked with `-p`. No implementation item.
- **W1-007 - INVALID - N/A (fixed).** `build.gradle:99` reads with `getText("UTF-8")` and `build.gradle:115` writes with `setText(..., "UTF-8")`. The platform-default charset claim is stale.
- **W1-008 - VALID - FIXED.** The tracked wrapper was replaced with the Gradle 9.3.1 generated wrapper artifact from the cached official Gradle 9.3.1 distribution. The 9.3.1 `Install` source uses `distribution.resolve(distribution.getPath() + ".sha256")`, so query and fragment components are handled as URL components rather than being appended to the full URI. Replacement jar SHA-256: `b3a875ddc1f044746e1b1a55f645584505f4a10438c1afea9f15e92a7c42ec13`. `gradle-wrapper.properties:3` remains on 9.3.1.
- **W1-009 - INVALID - N/A (fixed).** `gradlew:67-80` uses `readlink` and handles absolute versus relative targets; it no longer parses `ls -ld` output. No implementation item.
- **W1-010 - VALID (narrowed) - FIXED in the current diff.** The triage check found the old `JAVA_EXE` assignment vulnerable to cmd metacharacters while `CLASSPATH` was already quoted. Current `gradlew.bat:54-75` now uses quoted assignments for `JAVA_HOME`, `JAVA_EXE`, and `CLASSPATH`. Microsoft cmd `set` documentation was checked. The original report remains valid as a finding of the pre-fix snapshot, but has no open implementation item.
- **W1-011 - VALID - FIXED.** The original `AnvilGameTest.java:1357-1371` assertion could accept an empty output because enchantment level would be zero. Current lines 1367-1374 require a non-empty sword output and then check Sharpness is absent. Source check: current test and `anvilMergeOutput` helper.
- **W1-012 - VALID - FIXED.** The original `AnvilGameTest.java:155-183` only invoked the callback conditionally. Current lines 168-176 require a non-empty repair result before `invokeOnTakeOutput`, and lines 179-181 restore configuration. The zero-use false pass is removed.
- **W1-013 - VALID - FIXED.** The negative-chance test previously allowed every output to be empty. Current `AnvilGameTest.java:1790-1816` asserts each result is non-empty before invoking the callback and restores both settings.
- **W1-014 - VALID - FIXED.** Current `AnvilGameTest.java:1851-1881` establishes creative mode, requires a non-empty result at lines 1871-1876, invokes the callback, and then checks the anvil. The old no-use false pass is removed.
- **W1-015 - VALID - FIXED.** Current `AnvilGameTest.java:185-225` uses a deterministic seed selected for the 0.12 branch and asserts the sampled value and chipped result; it no longer depends on 500 random trials. The original report's nondeterminism was confirmed against the prior test.
- **W1-016 - VALID - FIXED.** Current `CommandsGameTest.java:714-739` places mutation, dispatches, assertions, and cleanup inside one `try/finally`. The old pre-`try` mutation/assertion leak is addressed.
- **W1-017 - INVALID - N/A (cleanup helper now complete).** `CommandsGameTest.java:597-613` restores both settings through `ETTestHelper.restoreConfig`; that helper restores values and clears mixin caches. The reported stale `CheapNamesMixin` state is not left behind by the current diagnostic test.
- **W1-018 - NEEDS-CONTEXT - RESOLVED (preserve case).** The current parser, `ConfigSchemaGameTest.parseRobustness`, and changelog contract all preserve value case while lowercasing keys. `Cheap_Names=TRUE` therefore remains `TRUE`; no value canonicalization change is warranted.
- **W1-019 - VALID - FIXED.** Current `ConfigScreenModelGameTest.java:79-90` asserts that a loaded `TRUE` becomes canonical `true` before testing dirtiness and toggles. The original missing canonicalization assertion is addressed.
- **W1-020 - INVALID - N/A per independent source check.** The independent report checked Yarn 1.20.5 and local remapped bytecode: the actual anvil input object is a `ForgingScreenHandler$3` whose superclass is `SimpleInventory`, where `heldStacks` is declared, so the original superclass lookup is not the claimed `Object` lookup. Current `ETTestHelper.java:94-104` was also changed in the working diff; this reflection edit should be checked against that runtime hierarchy before merge, but the original finding's stated cause was not established. Source checks: Yarn `SimpleInventory` docs and local handler bytecode.
- **W1-021 - VALID - FIXED.** Current `EnhancedGameTest.java:720-746` snapshots and restores all More Mending settings, including `more_mending`, step, floor, and `better_mending`/capmod values. The prior successful-run leak is addressed.
- **W1-022 - VALID - FIXED.** Current `PluginConfigGameTest.java:27-41` explicitly sets `mod_enabled=true` before the baseline Sharpness assertion and only then transitions it to false. The test no longer depends on ambient startup state.
- **W1-023 - VALID - FIXED.** Current `PluginConfigGameTest.java:147-177` snapshots the file, writes and verifies `sharpness=-1`, changes only memory to 7, reloads, and asserts the disk value is used. It restores the original file afterward.
- **W1-024 - VALID - FIXED.** Current `TweakGameTest.java:1310-1323` snapshots `enchant_trade_restock` and restores it with the other trade settings. The original global-setting leak is addressed.
- **W1-025 - VALID - FIXED.** Current `AnvilRepairHandler.java:46-52` calls `world.canPlayerModifyAt(player, pos)` before replacing the anvil or consuming iron. Fabric `UseBlockCallback` behavior and Yarn `World.canPlayerModifyAt` documentation were checked.
- **W1-026 - VALID - FIXED.** Current `ETConfigSchema.java:88-95` lowercases only the key and preserves the value. Current `ConfigSchemaGameTest.java:482,500-503` verifies this with `Cheap_Names=TRUE`; changelog entries 56 and 63 document the contract.
- **W1-027 - VALID - FIXED.** Current `ETConfigSchema.java:246-271` rejects null before type parsing. The boolean, integer, decimal, and list branches now return validation results instead of throwing for known-key null input.
- **W1-028 - VALID - FIXED.** Current `ETConfigSchema.java:161-169` uses nested try-with-resources for the classpath stream and UTF-8 reader. The resource leak is addressed.
- **W1-029 - VALID - FIXED.** Current `ETMixinPlugin.java:228-240` filters `CLIENT_LOCAL_KEYS` before applying sync data. `EnchantTweakerClient.java:21-32` and `ConfigSyncPayload.java:26-50` were also checked; arbitrary server data can no longer overwrite the filtered local keys through this path.
- **W1-030 - INVALID - N/A (intentional restart-only behavior).** Bundled properties identify `mod_enabled` as restart-required; `ETMixinPlugin.onLoad` reads it for mixin application and `reloadConfig` refreshes runtime caches without changing already-applied mixins. `SetCommand` also warns that restart is required. No code change is warranted.
- **W1-031 - VALID - FIXED.** Current `EnchantTweakerClient.java:17-43` increments `connectionGeneration` on join and disconnect, captures the generation when queuing sync, and applies it only if the generation still matches. The old packet-after-disconnect/later-connection race is addressed. Fabric client networking dispatch behavior was checked.
- **W1-032 - VALID - FIXED.** `ETConfigScreen.boundedContentWidth` now clamps to a positive 49-pixel minimum, keeping category, search, pager, and action button dimensions positive even when the screen is narrower than 40 pixels.
- **W1-033 - VALID - FIXED.** `ETConfigValueScreen` reuses the bounded screen width and clamps its derived action-button width to at least one pixel, preventing invalid dimensions during tiny-width initialization. The client-only screens have no dedicated server gametest target, so no focused game test was added; the shared bound directly covers both initializers.
- **W1-034 - INVALID - N/A (intentional diagnostic deduplication).** `DiagnoseCommand.java:36-47` deliberately uses `seen` to report each shared config key once; all current shared-key pairs use the same gate. The report did not establish an independent state race that would make one-row reporting incorrect.
- **W1-035 - INVALID - N/A.** `HelpCommand.java:22` invokes `ADBrigadier.Command.node`, a `Supplier<LiteralCommandNode<?>>`, not `Optional.get()`. Registered suppliers build literal nodes, so `NoSuchElementException` is not applicable.
- **W1-036 - INVALID - N/A.** `ListCommand.java:87-91` cannot receive a supported null config value: `ADConfig` uses `ConcurrentHashMap`, and its parser/mutators reject null keys and values. The claimed null-value path is unreachable through the public API.
- **W1-037 - INVALID - N/A.** `ResetCommand.java:69` uses `ADConfig.set`; `ADConfig.java:356-375` calls `replaceValue` before updating memory, so a supported single-key reset is persisted already.
- **W1-038 - INVALID - N/A.** `ListSuggestion.java:24-26` returns `Suggestions.empty()` for no options, but Brigadier's `Suggestions.empty()` and `SuggestionsBuilder.buildFuture()` both produce the static empty result with the zero-index range. The proposed builder-range distinction does not exist for an empty result; Brigadier source/behavior was checked.
- **W1-039 - VALID - FIXED.** `ListSuggestion` now skips null collection elements before matching or suggesting them, while preserving all valid string matching behavior.
- **W1-040 - VALID - FIXED.** `ListSuggestion.of` now treats a null supplier or null supplier result as an empty suggestion set instead of dereferencing it.
- **W1-041 - INVALID - N/A.** `ETConfigSchema.java:88-95` normalizes every loaded key to lowercase, and `ETConfigScreenModel.java:86-87` therefore searches the same canonical key space. The uppercase-schema-key scenario is not present in the current repository.
- **W1-042 - INVALID - N/A.** Current `MoreBindingMixin.java:65-80` uses `armor.contains` and `armor.indexOf`, but the independent bytecode check found no `ItemStack.equals/hashCode` override; `DefaultedList` therefore uses identity semantics here, not the claimed value equality. The identical-stack misclassification premise is false for this target.
- **W1-043 - VALID - FIXED.** The old global `World.isRaining()` check has been replaced by `World.hasRain(BlockPos)` at `MoreChannelingMixin.java:36-41`. Yarn documents `hasRain` as position/biome/sky aware, addressing snow and local no-rain cases.
- **W1-044 - INVALID - N/A (fixed/reversed premise).** Current `MoreFlameMixin.java:35-41` captures the `LivingEntity shooter` passed to `applyEnchantmentEffects`. Mapped projectile bytecode shows this argument is the projectile creator/owner, not the hit target; the original report reversed that call's argument semantics.
- **W1-045 - INVALID - N/A (fixed/reversed premise).** Current `MoreInfinityMixin.java:32-47` receives the weapon and shooter locals and preserves free shots only when `shooter.isInCreativeMode()`. Yarn 1.20.5 maps the boolean in the target call to multishot, not a creative flag, so the original `isCreative`-argument claim was false.
- **W1-046 - INVALID - N/A.** Yarn 1.20.5 documents `ExperienceOrbEntity.getMendingRepairCost(int)` as an instance method. Current `MoreMendingMixin.java:57-71` uses a non-static injector and instance `mendingLevel`, which matches the target shape. No mixin-application failure is established.
- **W1-047 - VALID - FIXED.** Current `MoreMultishotMixin.java:22-41` clamps projectile count to 256 before `RangedWeaponItem.load`; `EnhancedGameTest.java:108-146` checks both the practical cap and an overflowing product. The old unbounded-allocation path is addressed.
- **W1-048 - NEEDS-CONTEXT - RESOLVED/FIXED.** Yarn 1.20.5 mapped `GrindstoneScreenHandler` bytecode includes `grind` removal of `DataComponentTypes.REPAIR_COST`, and the Minecraft Wiki grindstone contract states that grindstone outputs reset repair cost for items. README 163-164 promises a clean returned item, so regular disenchant outputs now remove `REPAIR_COST`. `GrindstoneGameTest.grindstoneDisenchantKeepItem` sets a prior-work cost and asserts the returned clean sword has none.
- **W1-049 - INVALID - FIXED/NOT APPLICABLE.** The original report inspected only `DisableEnchantmentsMixin.java:62-86`. Current registration includes `DisableEnchantmentsLootMixin` at `enchanttweaker.mixins.json:32-34`, and its source filters explicit loot choices. Current tests also cover disabled anvil transfer and loot behavior. The cited hook file alone is not the complete implementation.
- **W1-050 - INVALID - N/A.** Yarn 1.20.5 documents `ThornsEnchantment.onUserDamaged(LivingEntity,Entity,int)` as an instance method. Current `NoThornsBacklashMixin.java:19-29` uses an instance `@WrapOperation` handler, so the static-target mismatch is not present.
- **W1-051 - VALID - FIXED in the current diff.** The old constant modification targeted the armor durability value 2, not attacker damage. Current `NoThornsBacklashMixin.java:19-29` wraps `Entity.damage(...)` and suppresses attacker backlash directly; current tests assert armor durability behavior separately. The original finding is valid for the pre-fix code but has no open implementation item.
- **W1-052 - INVALID - N/A (intentional feature scope).** `TridentWeaponsMixin.java:36-39` intentionally groups Sharpness, Smite, and Bane under `DamageEnchantment`, alongside Fire Aspect, Knockback, and Looting. README 193-195 and bundled properties 372-375 list these six enchantments; no unintended broadening is established.
- **W1-053 - INVALID - N/A (contract scope).** The table-specific hook in `TridentWeaponsMixin.java:43-53` is intentionally limited to the damage-enchantment table path; the product contract promises adding the listed enchantments to tridents, not a separate table-generation guarantee for every non-damage entry. Existing table/anvil tests and README/properties were checked. No implementation item follows from this report unless the contract is expanded.
- **W1-054 - VALID - FIXED.** Current `ConfigSyncPayload.java:15-16,26-50` caps entries at 256 on encode and rejects negative, oversized, or impossible counts before allocation/iteration. The old unbounded network loop is addressed.
- **W1-055 - VALID - FIXED.** Current `ADConfig.java:406-415` normalizes keys in `set`, and getters at `512-590` normalize keys before lookup. This now matches parser normalization at 387-395 and batch persistence at 448-487.
- **W1-056 - VALID - FIXED.** Current `ADConfig.java:425-431` safely ignores null maps and null key/value entries in `setAll`, matching the sanitization pattern in `setAllAndPersist` at 448-460. The old `ConcurrentHashMap.putAll` null failure is gone.
- **W1-057 - VALID - FIXED.** Current `ADMisc.java:16-18` explicitly rejects null before `equalsIgnoreCase`; `isInteger` and `isDouble` retain their guarded parse behavior.
- **W1-058 - INVALID - N/A (intentional contract).** Current `ADShiny.java:14-19` intentionally colors any non-curse level at or above the configured maximum. `ShinyNameGameTest.java:30-38,65-73` explicitly verifies over-level and non-positive-maximum behavior, and the recorded test results passed.
- **W1-059 - VALID - FIXED.** Current `ADText.java:16-17` exposes immutable `Set.of` values rather than mutable `HashSet` instances. The public-set global mutation path is removed.
- **W1-060 - VALID - FIXED.** The bundled More Binding tip now states both drop chance and retention and correctly says step 0.2 yields a 20% drop chance (80% retention) at Binding V.

## Deduplicated outcomes and unresolved work

- Validated (the report's behavior/evidence is established): 32 IDs. This includes findings already fixed in the current diff; VALID means the original report was a real issue, not that it remains unfixed.
- Invalid (the report's premise is false, stale, or intentionally supported): 26 IDs.
- Context decisions: W1-018 resolves to preserving value case, and W1-048 resolves to clearing regular-item repair cost because that matches the Yarn/Minecraft contract and README's clean-item promise.
- Open implementation items after the current diff: none. All validated findings now have an implementation status or an explicit contract decision recorded above.

## Wave 2 implementation slice

- **W2-001 - FIXED.** Anvil interaction tests now verify the sneaking precondition on positive and negative paths, and rename/repair cost assertions require a non-empty output where an operation is expected. The existing deterministic anvil damage seed and callback output checks are preserved.
- **W2-002 - FIXED.** Payload game tests release every `RegistryByteBuf` in a `finally` block, including malformed and version-mismatch paths. The obsolete 2000-entry test now exercises the protocol maximum of 256 entries, matching the encoder guard.
- **W2-003 - FIXED.** Decoder tests catch the established exception types: truncated Netty reads require `IndexOutOfBoundsException`, while the explicit entry-count guard requires `DecoderException`; broad `Exception` catches were removed.
- **W2-004 - FIXED.** Commands tests snapshot all configuration keys around reset-all operations and restore them with `setAllAndPersist`, so disk and memory state return to the pre-test values. The command registry regression also verifies that external mutation is rejected.
- **W2-005 - FIXED.** Enhanced mending, Tweak trade-limit, and capmod tests restore the exact configuration values they found instead of hard-coded defaults. A capmod regression calls the public level helper with two different vanilla fallback levels to prevent caching a derived result.
- **W2-006 - SOURCE DECISION.** Fabric's `UseBlockCallback` contract says `SUCCESS` cancels further processing and `PASS` falls through, so anvil tests assert the resulting world/item state rather than assuming the full interaction result is `PASS` on vanilla fallback paths. The callback's spectator gate remains explicit.
- **W2-007 - SOURCE DECISION.** Yarn 1.20.5 `ExperienceOrbEntity` bytecode assigns the selected equipment before invoking `getMendingRepairCost`; the More Mending capture hook therefore observes the item level without a setter-order change.
- **W2-008 - FIXED.** Capmod cache entries now retain configured caps rather than vanilla-derived fallback levels, preserving different vanilla levels for uncapped enchantments. Namespaced registry suggestions intentionally use bare paths for default-namespace entries and full identifiers for other namespaces.
- **W2-009 - FIXED.** Configuration mutation now rejects CR/LF values and uses a valid three-character minimum temporary-file prefix. Public schema key lookups normalize case and category slugs, while duplicate keys retain the latest value/type/category and same-category ordering retains the first occurrence.
- **W2-010 - FIXED.** `ETCommands.getCommands()` returns a static immutable view, with focused game-test coverage for attempted external mutation.

## Wave 2 CI and UI implementation

- **W2-CI-001 - VALID - FIXED.** `gradlew` used the non-POSIX `which java` probe when `JAVA_HOME` was unset. The fallback now uses POSIX `command -v java`, preserving the existing diagnostic while avoiding a dependency on an external `which` utility.
- **W2-CI-002 - VALID - FIXED.** The Quilt smoke script only registered an `EXIT` cleanup trap, so interrupt/termination handling was not explicit. It now traps HUP, INT, and TERM by exiting with conventional signal statuses; the existing `EXIT` cleanup then closes the console FIFO, stops/reaps the server, and removes the temporary directory.
- **W2-UI-001 - VALID - FIXED.** `ETConfigValueScreen` previously allowed `Integer.MAX_VALUE` characters in its text field. A shared 4096-character UI bound now limits interactive input to a practical size while remaining compatible with the schema's unbounded LIST acceptance and preserving externally loaded values.

## Wave 3 implementation A

- **W3-A-TWEAK-001 - FIXED.** Every mutating `TweakGameTest` method now snapshots every feature/config key it changes before setup, executes setup and assertions inside an outer `try`, and restores the exact pre-test values through `ETTestHelper.restoreConfig` in the outer `finally`. This covers XP scaling, disabled-enchantment lists, compatibility toggles, entity behavior toggles, protection bypass values, trade limits, and all later edge-case tests. The exact-key lists are visible beside each method's `wave3OriginalConfig` declaration in `src/gametest/java/com/adibarra/enchanttweaker/test/TweakGameTest.java`.
- **W3-A-TWEAK-002 - FIXED.** The Soul Speed backlash assertion no longer relies on 500 probabilistic trials. It uses a `ZombieEntity` override returning Yarn `Random.create(5120L)`, whose first `nextFloat()` is below the vanilla `0.04F` threshold documented in Yarn `LivingEntity.addSoulSpeedBoostIfNeeded`, then asserts exactly one durability point. The setup and reflection calls are protected by the same exact-config `finally`.
- **W3-A-TWEAK-003 - FIXED.** Every Tweak anvil-combine assertion now first requires a non-empty output before reading enchantment levels, including disabled-feature and follow-up Sharpness merges. This removes empty-output false passes while retaining the specific enchantment behavior checks.
- **W3-A source evidence.** Yarn 1.20.5 `LivingEntity.addSoulSpeedBoostIfNeeded` calls `getRandom().nextFloat() < 0.04F` before damaging Soul Speed boots, and Yarn `Random.create(long)` returns a seeded checked random. The test's exact seed and one-call assertion therefore exercise a fixed branch rather than a likelihood threshold. `ETTestHelper.restoreConfig` restores values through `ADConfig.setAll` and clears mixin caches, so the snapshot also resets derived feature state.

## Wave 3 implementation D

- **W3-D-CONFIG-001 - FIXED.** `ADConfig` now normalizes keys through one helper and rejects null, empty, line-breaking, or `=`-containing keys during parsing and public mutation. Parsing still splits on the first `=`, trims values, and preserves supported values such as `a=b=c`. `ConfigSchemaGameTest.parseRobustness` covers empty-key, `=`-key, empty-value, and equals-containing-value cases.
- **W3-D-RELOAD-001 - FIXED.** `ETMixinPlugin.reloadConfig`, feature/capmod cache lookups, and explicit cache clearing now share the class monitor. A cache computation cannot insert a stale result after reload clears the maps. `MOD_ENABLED` remains assigned only by `onLoad`, so reload continues to refresh runtime values without changing restart-only mixin application.
- **W3-D-DOCS-001 - FIXED.** README and all tracked runtime properties copies now document the Flame safety cap, the 256-projectile More Multishot cap, and the base-zero behavior for protection, fire duration, and knockback instead of claiming uncapped scaling or that zero is unreachable.
- **W3-D-PAYLOAD-001 - FIXED.** `PayloadGameTest` derives mismatched wire versions from `ConfigSyncPayload.PROTOCOL_VERSION` instead of hard-coded values, so tests continue to exercise the mismatch branch after protocol bumps. Oversized and negative entry-count checks remain covered, and every buffer is released in a `finally` block.
- **W3-D-REJECTED-001 - INVALID - N/A.** Triage rejected the server-mixin-placement claim because every registered server path matches the package and source layout. More Binding uses identity-based `DefaultedList` membership rather than the claimed value equality. More Flame captures the projectile shooter, not the hit target. More Infinity's captured boolean is multishot, not a creative flag. Grindstone behavior matches the vanilla clean-output contract. Better Mending's shadow call resolves to the original method rather than recursive injected dispatch. ADText exposes immutable `Set.of` value sets. No compensating implementation was added for these false reports.

## Wave 3 implementation C

- **W3-C-001 - FIXED.** Commands game tests now snapshot every configuration key they mutate and restore the exact values. Tests that dispatch SET or RESET restore memory and disk with `setAllAndPersist`; memory-only diagnostic and suggestion setup restores through the helper without hard-coded defaults.
- **W3-C-002 - SOURCE DECISION.** The Fabric/Yarn anvil handler emits an empty output when an incompatible enchantment is rejected (`bl3 && !bl2`), while a disabled enchantment with maximum level zero still emits the copied item. Tests now assert empty output explicitly for unsupported compatibility paths and require non-empty outputs before reading levels or repair-cost components.

## Wave 3 implementation B

- **W3-B-ENHANCED-001 - FIXED.** Enhanced game tests now snapshot every configuration key they mutate and restore the exact values through `ETTestHelper.restoreConfig` in `finally` blocks. This removes cross-test leakage across more multishot, flame, mending, infinity, binding, channeling, and protection scenarios.
- **W3-B-CAPMOD-001 - FIXED.** Capmod game tests now snapshot all touched capmod, enchantment-cap, and disable-enchantment keys and restore the exact pre-test values in `finally` blocks, including command and overflow cases.
- **W3-B-RANDOM-001 - FIXED.** The More Binding level-one drop check now sets the step to `1.0`, which Yarn-backed `ADUtils.bindingKeepsItem` clamps to a full drop threshold, then performs one deterministic assertion instead of relying on 50 random trials. Infinity checks retain boundary rates that make their repeated assertions deterministic.
- **W3-B-PAYLOAD-001 - FIXED.** Payload tests release every allocated `RegistryByteBuf` in `finally` blocks, derive mismatch versions from `ConfigSyncPayload.PROTOCOL_VERSION`, and cover empty mismatched bodies plus encoder and decoder entry-count boundaries.
- **W3-B-VERIFY-001 - VERIFIED.** `spotlessCheck` PASS, `zizmor` PASS, `build` PASS, `runGametest` 446/446 with 0 failures/errors/skipped, and Quilt smoke PASS with temporary JDK/truststore and 40 mixins.

## Wave 3 review coverage

- **Coverage:** All 121 tracked repository files were each reviewed by one independent read-only fast subagent, with zero repository context beyond the assigned file.
- **Outcome:** Wave 3 produced only triaged, rejected, or fixed findings. No open items remain.

## Wave 4 implementation A

- **W4-A-SMOKE-001 - FIXED.** The Quilt smoke script now downloads Mojang's official `version_manifest_v2.json`, resolves the configured Minecraft version to its version metadata, reads the metadata's `downloads.server.sha1`, validates the expected Mojang metadata and server hosts, and checks the Quilt-installer-produced `server/server.jar` with `sha1sum` before any server launch. Quilt's official server-install documentation confirms that `--download-server` places the Minecraft server jar in the installation directory.
- **W4-A-RELEASE-001 - FIXED.** Release authorization now compares `github.triggering_actor` with `github.repository_owner` while retaining the `refs/heads/main` gate. This prevents a rerun or manually dispatched workflow from authorizing based on the actor that originally created an associated event, while preserving the owner-only main-branch policy.
- **W4-A-CI-001 - NEEDS-CONTEXT.** The same-repository pull-request condition remains unchanged: push events run the job, while non-draft pull-request runs are limited to fork heads. No tracked repository policy, ruleset, or branch-protection configuration establishes whether same-repository pull requests must also run in pull-request context. An administrator must confirm the external required-check policy before changing this deduplication condition.
- **W4-A-PROPERTIES-001 - SOURCE DECISION.** The sole tracked runtime properties file already uses the corrected clamp wording for protection, fire duration, blast knockback, Flame's safety cap, and More Multishot's 256-projectile cap. No additional properties edit was necessary.

## Wave 4 implementation B

- **W4-B-ANVIL-001 - FIXED.** `AnvilGameTest.cheapNamesClearNameRemovesCustomName` now requires the clear-name operation's output stack to be non-empty before checking that `CUSTOM_NAME` is absent. The prior `contains(CUSTOM_NAME) == false` assertion was vacuous for an empty output.
- **W4-B-PAYLOAD-001 - FIXED.** Payload tests that call `ETMixinPlugin.syncConfigFrom` now call `restoreLocalConfig()` in their `finally` blocks before restoring the test's snapped keys. `ETMixinPlugin.restoreLocalConfig` restores the captured map, clears `LOCAL_CONFIG_BACKUP`, and reloads runtime config (source `ETMixinPlugin.java:228-248`), so failed or successful sync tests cannot leave the process-wide backup for a later test.
- **W4-B-BOUND-ARMOR-001 - FIXED.** `MoreBindingMixin` now records retained armor in `BOUND_ARMOR` only for `ServerPlayerEntity` instances. Fabric's `ServerPlayerEvents.AFTER_RESPAWN` callback is explicitly a server-player old/new callback and is documented for old-player reference cleanup; a `TestContext.createMockPlayer` `PlayerEntity` cannot receive that callback. Mock-only `dropAll` tests therefore retain their in-inventory item without accumulating an unreclaimable respawn stash, while real server players preserve the existing respawn behavior. The disabled mock-player regression also performs `dropAll()` inside its `try/finally` so configuration cleanup still runs if the operation or assertion fails. Fabric source evidence: `fabric-entity-events-v1` `ServerPlayerEvents.AFTER_RESPAWN` Javadoc and callback signature.
- **W4-B-SERVER-PLAYER-001 - FIXED.** `ETTestHelper.createServerPlayer` registers a `TestContext.addFinalTask` that removes the created mock from its `ServerWorld` with `Entity.RemovalReason.DISCARDED` when the test's final clause runs. Yarn 1.20.5 documents `TestContext.createMockCreativeServerPlayerInWorld` as deprecated and exposes `addFinalTask`; `ServerWorld.removePlayer(ServerPlayerEntity, Entity.RemovalReason)` is the public player-list removal API. This central cleanup covers all existing helper call sites through GameTest's final-task scheduling.

### Wave 4 B source decisions

- Fabric's `FabricGameTestHelper.getTestMethodInvoker` constructs a fresh test class instance and invokes each test method synchronously. Cleanup is therefore registered on the supplied `TestContext` rather than held in static test state; the final task is owned by that test's GameTest state.
- No `CHANGELOG.md` changes were made. No repository `AGENTS.md` exists; this remains recorded by the Wave 1 audit at line 7.

## Wave 4 implementation C

- **W4-C-001 - FIXED.** Grindstone `updateResult` now clears stale result/book-slot state before recalculating, and focused regression coverage exercises a valid-output to stacked-invalid transition so an old output cannot survive an invalid update.
- **W4-C-002 - FIXED.** `ADConfig.setAllAndPersist` now reports persistence success; Set and Reset commands stop before cache clearing, sync, and success feedback when the configuration path cannot be persisted, while preserving the true no-op result for already-preserved null values. A focused Commands game test uses a configuration path that is a directory and verifies the failure result.
- **W4-C-003 - FIXED.** `ETMixinPlugin.getMixinConfig` now handles a missing/unknown mixin key without dereferencing a null key mapping, with focused schema/game-test coverage for the unknown lookup.
- **W4-C-VERIFY-001 - BLOCKED.** Agent-targeted compilation could not run in the current shell because neither `JAVA_HOME` nor a `java` executable is available. The project-wide verification is intentionally left to the parent environment; no project-wide command was run by this slice.

## Wave 5 triage

- **Wave:** 5
- **Reviewed files:** 121.
- **Final triage:** The source-backed valid findings are listed below. All Wave 5 C runtime claims are **INVALID**: the cited behavior is not supported by the source, and the 447-test run produced no supporting failure.

### Valid findings and implementation items

- **W5-ADCONFIG-001 - VALID - OPEN.** `ADConfig` does not round-trip comment-prefixed keys: a key beginning with `#` is written as a comment and is therefore not reconstructed as the original key when the file is parsed. **Implementation:** define an unambiguous escaped representation for comment-prefixed keys and make serialization/parsing round-trip it.
- **W5-ADCONFIG-002 - VALID - OPEN.** `ADConfig` getter key trimming is inconsistent with the parser and mutators. **Implementation:** centralize getter key normalization so public reads apply the same trimming/canonicalization contract as writes and parsed keys.
- **W5-COMMAND-001 - VALID - OPEN.** `SetCommand` lowercases values that are case-sensitive, changing the configured value instead of preserving it. **Implementation:** lowercase only the key/command token; pass the value through unchanged except for the schema's documented normalization.
- **W5-SUGGEST-001 - VALID - OPEN.** `ValueSuggestion` uses a static enchantment registry and omits dynamically registered enchantments. **Implementation:** derive suggestions from the current runtime registry rather than a static snapshot.
- **W5-SYNC-001 - VALID - OPEN.** `ConfigSync` accepts trailing bytes after a known-version payload, and its public null constructor inputs can produce an NPE. **Implementation:** reject unconsumed bytes after decoding a known-version payload and validate/null-handle public constructor inputs before dereference.
- **W5-GAMETEST-001 - VALID - OPEN.** GameTest-created extra players and entities at high sky or below `bottomY` are not guaranteed to be cleaned up. **Implementation:** register deterministic final-task cleanup for every created player/entity, including out-of-bounds placements.
- **W5-PROPERTIES-001 - VALID - OPEN.** The bundled properties Binding formula omits the clamp present in the implementation. **Implementation:** document the clamped formula in the bundled properties copy.

### Rejected and needs-context findings

- **W5-C-RUNTIME - INVALID - N/A.** Wave 5 C runtime claims were rejected by source inspection and the 447-test result; no runtime implementation follows.
- **W5-CI-RELEASE - NEEDS-CONTEXT - N/A.** CI/release dry-run concerns are environment- or repository-policy-dependent and are not established as source defects by this review. They require the external CI/release policy or an actual failing run before implementation.
- **W5-JUNIT - INVALID - N/A.** The JUnit concern is not supported by the repository's GameTest execution contract or the 447 passing tests; no JUnit-specific implementation is warranted.
- **W5-ICON - NEEDS-CONTEXT - N/A.** The icon concern is a packaging/product requirement rather than an established runtime defect; acceptance criteria and release asset policy are required before changing assets.
- **W5-MIXIN-MANIFEST - INVALID - N/A.** Source and the registered mixin manifest agree; the claimed manifest mismatch is not present.
- **W5-ANONYMOUS-TARGET - NEEDS-CONTEXT - N/A.** The anonymous-target concern depends on mappings and target/runtime assumptions not established by the current source review; a concrete supported target contract is required before implementation.

## Wave 5 implementation and verification

- **W5-ADCONFIG-001 - FIXED.** Comment-prefixed configuration keys now use an unambiguous escaped representation, so serialization and parsing round-trip the original key. Focused configuration tests cover the escaped-key case.
- **W5-ADCONFIG-002 - FIXED.** Public `ADConfig` getters now share the parser and mutator key-normalization contract, including trimming. Focused configuration tests cover normalized getter lookups.
- **W5-COMMAND-001 - FIXED.** `SetCommand` lowercases only the key/command token and preserves case-sensitive values according to the schema contract. Focused command/configuration tests cover value preservation.
- **W5-SUGGEST-001 - FIXED.** `ValueSuggestion` now reads the current runtime enchantment registry, including dynamically registered enchantments. Focused suggestion tests cover dynamic registry entries.
- **W5-SYNC-001 - FIXED.** Known-version `ConfigSync` payloads reject trailing bytes, and public constructor inputs are validated before dereference. Focused payload tests cover trailing data and null inputs.
- **W5-GAMETEST-001 - FIXED.** GameTest helper tracking cleanup now registers deterministic final-task cleanup for every created extra player and entity, including high-sky and below-`bottomY` placements. Focused helper/game tests cover out-of-bounds cleanup.
- **W5-PROPERTIES-001 - FIXED.** The bundled properties Binding formula now documents the implementation's clamp.
- The helper player tracking cleanup and the compile import correction were included in the Wave 5 implementation. Focused tests were added where the item had an observable test contract.

### Wave 5 verification

- `spotlessApply`: PASS.
- `zizmor`: PASS, no findings (1 suppressed).
- `build`: PASS.
- `runGametest`: PASS, 452/452 tests, 0 failures, 0 errors, 0 skipped. Count observed from `build/junit.xml`.
- Quilt smoke test: still required.

## Wave 6 triage

- Wave: 6
- Reviewed files: 121.
- Review basis: current repository source, manifests, workflows, wrapper, build script, and the existing Wave 1-5 evidence. Prior evidence above is preserved unchanged.
- Final triage: 7 OPEN, 6 REJECTED, and 3 NEEDS-CONTEXT. No Wave 6 report is marked FIXED; the current source either still has the behavior, or the report is stale/unestablished.

### Open findings

- **W6-ANVIL-001 - OPEN.** `ETTestHelper.setAnvilInputs` (`src/gametest/java/com/adibarra/enchanttweaker/test/ETTestHelper.java:142-154`) finds `heldStacks` only on `inv.getClass().getSuperclass()`. A runtime inventory subclass that inherits the field through another level, or a mapping/runtime class that declares it on a different ancestor, fails even though the field is present. The lookup should walk the class hierarchy (and fail with a precise diagnostic only after exhausting it).
- **W6-GAMETEST-001 - OPEN.** `ETTestHelper.trackServerPlayer` (`ETTestHelper.java:72-96`) catches `IllegalStateException` with `"This test already has final clause"` and returns before putting the player in `CREATED_SERVER_PLAYERS` or registering any cleanup. A helper call made after a test has already installed its final clause therefore leaves the extra server player untracked. The existing normal-path final task does not cover this pre-existing-final-clause path.
- **W6-ADCONFIG-001 - OPEN.** `ADConfig.request` applies version migrations at `ADConfig.java:93-125`, then calls `migrateConfig` and only writes `VERSION_KEY` when no key-diff rewrite occurred (`ADConfig.java:99-105`). If a future `Migration` changes an existing value/key while defaults have no key diff, the version stamp is persisted but the migrated value is not. Persistence must serialize the complete post-migration map, not just the version line.
- **W6-SYNC-001 - OPEN.** `ConfigSyncPayload` validates the supplied map but retains the caller-owned reference (`ConfigSyncPayload.java:13-21`). The record accessor also exposes that mutable map, so changes after construction can alter a queued payload or decoded payload. The canonical constructor needs a defensive immutable copy after null-entry validation.
- **W6-BRIGADIER-001 - OPEN.** `ADBrigadier.buildAlias` (`ADBrigadier.java:37-44`) forwards the destination redirect metadata and reuses each destination child node directly. It has no regression coverage for aliases whose destination has a redirect, fork modifier, or nested child/redirect chain, and it does not establish that the reused nodes preserve the destination tree under Brigadier's parent/redirect rules. Alias construction needs source-backed child/redirect handling and focused coverage for those cases; the current one-level `enchanttweaker config list` test is insufficient.
- **W6-RELEASE-NOTES-001 - OPEN.** `writeReleaseNotes` (`build.gradle:109-116`) writes `releaseNotes.get().asFile` directly but never creates its parent directory. A clean checkout/task invocation in which `build/` has not yet been created can fail before the release validation step. The task should create the output parent (or use Gradle's directory-aware output API) before `setText`.
- **W6-MOREBINDING-001 - OPEN.** `MoreBindingMixin` removes armor only on `AFTER_RESPAWN` and clears the static `BOUND_ARMOR` map only on `SERVER_STOPPED` (`MoreBindingMixin.java:36-50`). There is no disconnect cleanup or expiry, so a player who disconnects without a matching respawn can leave copied stacks in the UUID stash indefinitely. The source also has no server-side notification/synchronization tied to this disconnect transition. The existing `moreBindingDisconnectPreservesPendingArmor` test documents one desired preserve-until-respawn path, but does not establish stale-entry cleanup or a disconnect sync contract.

### Rejected or context-dependent findings

- **W6-CI-001 - NEEDS-CONTEXT.** `.github/workflows/ci.yml:21-25` runs pushes and non-draft fork pull requests, while same-repository pull requests are covered by the push event. Whether a same-repository pull-request event must itself execute the required check is an external branch-protection/ruleset policy; no tracked repository file establishes that requirement. This is not a source defect without that policy.
- **W6-RELEASE-001 - REJECTED for the current repository.** `.github/workflows/release.yml:17-18` compares `github.triggering_actor` with `github.repository_owner` and gates `refs/heads/main`. The checked repository remote is `https://github.com/adibarra/enchant-tweaker.git` (`.git/config:6-8`), so the current owner is the user `adibarra`, not an organization account. The organization-owner actor mismatch scenario is not present in this repository; revisit only if the repository ownership or release authorization policy changes.
- **W6-WRAPPER-001 - REJECTED.** `gradlew` is the generated POSIX wrapper and explicitly requires a POSIX shell (`gradlew:21-43`). It validates `JAVA_HOME`/`java` with quoted paths (`gradlew:116-136`), documents that malformed `DEFAULT_JVM_OPTS`, `JAVA_OPTS`, or `GRADLE_OPTS` quoting/newlines can break parsing (`gradlew:197-244`), and resolves `APP_HOME` from the script path. Malformed environment fragments are outside the wrapper's documented input contract, and the alleged `CDPATH` corruption is not established for the path-with-slash `cd` used here.
- **W6-GAMETEST-002 - REJECTED.** The hardcoded GameTest coordinates and expected boundary values are deterministic fixtures, not portable world coordinates: tests use `fabric-gametest-api-v1:empty`, relative `BlockPos` values, and `helper.getAbsolutePos(...)`/helper-spawn APIs before interacting with the world. Fixed values such as damage thresholds and registry paths are the assertions under test. No source evidence supports replacing them with ambient or random values.
- **W6-GAMETEST-003 - NEEDS-CONTEXT.** The repository tests mutate process-wide configuration and restore it in per-test `finally` blocks, but no tracked file sets a parallel GameTest runner policy. Whether an external runner/version schedules these tests concurrently is a harness/runtime configuration question; a concrete parallel execution mode and failing interleaving are required before changing the tests.
- **W6-MATH-001 - REJECTED.** `build.gradle:42-50` and `fabric.mod.json:22-27` require Java 21, and current source uses Java 21 `Math.clamp` in `AnvilCost`, `ETMixinPlugin`, `ADUtils`, and the enhanced/anvil mixins. The NaN/Infinity premise is also contradicted by `ETConfigSchema.isValid` (`ETConfigSchema.java:257-282`), finite numeric getters in `ADConfig.java:594-620`, and focused tests that reject non-finite values (`ConfigSchemaGameTest.java:80-87,621-650`). No Math.clamp or NaN implementation item follows.
- **W6-SUGGEST-DIAGNOSE-001 - REJECTED.** `ValueSuggestion` now reads the live enchantment and damage-type registries (`ValueSuggestion.java:41-89`) rather than a static enchantment list, and `DiagnoseCommand` deliberately deduplicates shared config keys (`DiagnoseCommand.java:36-45`). Existing command tests exercise dynamic registry candidates and diagnostics. The stale static-registry claim and the claim that shared-key deduplication is an accidental omission are not supported by current source.
- **W6-PACKAGING-001 - REJECTED.** The manifest points to an existing `assets/enchanttweaker/icon.png` (`fabric.mod.json:14`; the tracked image is present), and `fabric.mod.json:21` names `enchanttweaker.mixins.json`, whose registered entries match the current source layout (`enchanttweaker.mixins.json:1-55`). The icon and manifest mismatch claims are false for this checkout.
- **W6-ANONYMOUS-TARGET-001 - NEEDS-CONTEXT.** The current source uses explicit `@Mixin` classes and explicit mapped injection targets; no concrete anonymous target class, mapping, or runtime failure is identified by the report. A supported Minecraft/Yarn target signature and reproducible mixin-application error are required before this can be classified as a repository defect.

### Wave 6 disposition

- Open implementation items: W6-ANVIL-001, W6-GAMETEST-001, W6-ADCONFIG-001, W6-SYNC-001, W6-BRIGADIER-001, W6-RELEASE-NOTES-001, and W6-MOREBINDING-001.
- Rejected as false or stale in the current checkout: W6-RELEASE-001, W6-WRAPPER-001, W6-GAMETEST-002, W6-MATH-001, W6-SUGGEST-DIAGNOSE-001, and W6-PACKAGING-001.
- Requires external policy, runner configuration, or a concrete mapped target contract: W6-CI-001, W6-GAMETEST-003, and W6-ANONYMOUS-TARGET-001.

## Wave 6 implementation and final verification

- **W6-ANVIL-001 - FIXED.** `ETTestHelper.setAnvilInputs` now walks the complete runtime inventory class hierarchy to locate `heldStacks`, with a precise failure after the hierarchy is exhausted. Focused helper/anvil tests cover inherited fields beyond the immediate superclass.
- **W6-GAMETEST-001 - FIXED.** Late `ETTestHelper` player creation is now tracked and cleaned up even when the test already has a final clause, so the `IllegalStateException` path cannot leak a created player. Focused helper cleanup tests cover both normal and late-player paths.
- **W6-ADCONFIG-001 - FIXED.** `ADConfig` migration persistence now serializes the complete post-migration configuration map, including migrated existing values, rather than persisting only the version stamp when defaults have no key diff. Focused configuration tests verify migrated values survive reload.
- **W6-SYNC-001 - FIXED.** `ConfigSyncPayload` validates entries and then stores a defensive immutable snapshot, so caller mutation cannot change a queued payload and decoded payload access cannot expose mutable state. Focused payload tests cover snapshot immutability.
- **W6-BRIGADIER-001 - FIXED.** `ADBrigadier` now copies aliases safely and recursively, preserving redirect metadata and nested child/redirect structure without reusing destination child nodes unsafely. The alias regression was corrected to assert redirect metadata after Brigadier standalone dispatch semantics rejected the original synthetic dispatch approach. Focused Brigadier tests cover recursive aliases and redirect metadata.
- **W6-RELEASE-NOTES-001 - FIXED.** Release-note generation now creates the output parent directory before writing and extracts the release-notes parent/execution handling needed for clean task invocation. Focused build-script verification covers a clean output path.
- **W6-MOREBINDING-001 - FIXED.** More Binding now removes stale UUID armor copies on disconnect and synchronizes the resulting inventory state to the server player, while retaining the intended pending-armor behavior through respawn. Focused More Binding tests cover disconnect cleanup and inventory synchronization.

### Wave 6 final verification

- `spotlessApply`: PASS.
- `zizmor`: PASS, no findings (1 suppressed).
- `build`: PASS.
- `runGametest`: PASS, 454/454 tests, 0 failures, 0 errors, 0 skipped.
- Quilt smoke test: PASS with Loader 0.30.0, Minecraft 1.20.5, and 40 mixins.
- Wave 6 converged. There are no OPEN valid items.

## Wave 8 post-fix review

- Wave: 8
- Reviewed files: 121.
- Review status: post-fix review of all 121 files after the Wave 7 fixes.
- Remaining reports were triaged. The feature template typo was fixed. README, build, ConfigSync, weather, and join fixes were verified.
- Low test-coverage suggestions were rejected as non-production findings. Speculative or conditional runtime claims were rejected or classified as needs-context because no concrete supported contract or reproducible failure was established.
- Final disposition: no OPEN valid production findings remain.

### Wave 8 final verification after Wave 7 fixes

- `spotlessApply`: PASS.
- `zizmor`: PASS, no findings (1 suppressed).
- `build`: PASS.
- `runGametest`: PASS, 454/454 tests, 0 failures, 0 errors, 0 skipped.
- Quilt smoke test: PASS with Loader 0.30.0, Minecraft 1.20.5, and 40 mixins.

## Wave 9 triage

- Wave: 9
- Review basis: current checkout, current working-tree source, Fabric/Yarn 1.20.5 evidence, the current CI/release workflows, binary file signatures, and the preceding Wave 1-8 records. No code or CHANGELOG changes are recorded by this section.
- Final classification: 5 VALID (2 fixed, 3 open), 39 INVALID, and 0 NEEDS-CONTEXT. Runtime claims were not accepted from static inspection alone when the mapped target and a successful 454-test/40-mixin run contradicted them.

### Valid findings and exact open items

- **W9-DOC-001 - VALID - FIXED.** README's More Protection, More Fire Protection, and More Blast Protection descriptions now state the actual base behavior: bases between 0 and 1 approach zero, base 0 reaches zero for positive levels, base 1 preserves the input, and bases above 1 increase it. The formulas and defaults match the three current mixins.
- **W9-BUILD-001 - VALID - FIXED.** `build.gradle` now reads both configuration-time and task-time `CHANGELOG.md` content explicitly as UTF-8 and writes generated release notes explicitly as UTF-8. The release-notes parent directory is also created before writing.
- **W9-TEST-001 - VALID - OPEN.** `PayloadGameTest` mutates process-wide configuration at lines 174-177, 197-199, 220-222, 242-243, and 305-306 before entering its `try/finally`. If setup partially succeeds and then throws, the snapshot is not restored. Move each setup mutation inside the cleanup-protected region (or make setup itself exception-safe) without changing the assertions.
- **W9-TEST-002 - VALID - OPEN.** `ConfigSchemaGameTest` uses predictable files such as `et-migdiff-test.properties`, `et-migver-test.properties`, `et-migparse-abc.properties`, `et-migparse-noversion.properties`, `et-parity-test.properties`, and `et-parse-test.properties` directly under the Fabric config directory. `deleteQuietly` removes them but does not preserve a pre-existing file with the same name. Use unique temporary files or snapshot/restore the prior path contents.
- **W9-CONFIG-001 - VALID - OPEN.** `VillagerTradeLimitsMixin` derives per-enchantment configuration and no-restock matching from `Identifier` paths only (`VillagerTradeLimitsMixin.java:75-80,121-125`). Two registered enchantments from different namespaces can therefore collide, and a namespaced custom enchantment cannot be addressed unambiguously. Preserve and match the full identifier, with a documented compatibility contract for the existing default-namespace keys.

### Rejected findings

- **W9-EVENT-001 - INVALID.** `ETCommands.registerEventListeners` intentionally uses Fabric's `START_DATA_PACK_RELOAD` event to reload configuration and broadcast it before the reload proceeds. No stale-config failure is established.
- **W9-EVENT-002 - INVALID.** `registerEventListeners` and `AnvilRepairHandler.register` are invoked from the mod initializer once when the enabled branch runs; repeated registration was not shown for a supported lifecycle.
- **W9-COMMAND-001 - INVALID.** `HelpCommand` iterates the statically built `ADBrigadier.Command` suppliers, each of which is constructed with a literal node. The claimed absent-node `Optional.get()` path is not reachable.
- **W9-DIAGNOSE-001 - INVALID.** `DiagnoseCommand` deliberately deduplicates shared configuration keys; current shared-key mappings use one gate and the report does not establish a distinct state that must be printed twice.
- **W9-CACHE-001 - INVALID.** Compatibility callbacks run during mixin application, before runtime feature/capmod caches are used. `reloadConfig`, `syncConfigFrom`, and explicit cache clearing already clear the relevant caches under the current synchronization contract.
- **W9-SUGGEST-001 - INVALID.** `ValueSuggestion` reads the live dynamic registry; a static enchantment snapshot is not used.
- **W9-CAPMOD-001 - INVALID.** Capmod lookup handles vanilla fallback and configured caps through the current synchronized cache implementation, with focused coverage for fallback, custom, zero, negative, and overflow cases.
- **W9-ANVIL-001 - INVALID.** The unconditional `+1` in `PriorWorkCheaperMixin` preserves vanilla `AnvilScreenHandler.getNextCost`'s minimum-next-cost behavior. The report does not establish that zero is a supported output contract.
- **W9-TARGET-001 - INVALID.** Yarn 1.20.5 maps `ExperienceOrbEntity.repairPlayerGears(PlayerEntity,int)` and `getMendingRepairCost(int)` as the current instance methods; the More Mending handlers have the corresponding instance shape and the runtime mixin applies.
- **W9-TARGET-002 - INVALID.** Yarn 1.20.5 maps `RangedWeaponItem.createArrowEntity(World,LivingEntity,ItemStack,ItemStack,boolean)` as a protected instance method returning `ProjectileEntity`. The current descriptor and callback shape match the supported target.
- **W9-TARGET-003 - INVALID.** The reported arrow descriptor mismatch is the same unsupported static/return-type premise: mapped bytecode confirms the current protected instance method and `ProjectileEntity` return.
- **W9-TARGET-004 - INVALID.** The More Flame `applyEnchantmentEffects` report reverses the supported projectile callback semantics; no mixin-application failure or runtime regression is present in the current mapped target.
- **W9-TARGET-005 - INVALID.** More Infinity's captured boolean is the mapped multishot/shot flag, not a creative-mode argument, and the current handler separately checks the shooter for creative mode.
- **W9-TARGET-006 - INVALID - N/A (projectile-position contract).** Yarn's vanilla `TridentEntity.onEntityHit` stores the struck entity's block position for its lightning check, while the extension's rain check uses the projectile position. The extension contract explicitly evaluates Channeling II rain at the trident/projectile position, and the current `this.getWorld().hasRain(this.getBlockPos())` implements that contract. No hook change is warranted.
- **W9-TARGET-007 - INVALID.** The More Blast and More Fire protection injections target the mapped static `ProtectionEnchantment` methods. The reports' static-target premise is not an application defect.
- **W9-RUNTIME-001 - INVALID.** The null-entity claim for `ProtectionEnchantment.transformFireDuration` is an unsupported caller scenario; vanilla and the current mapped method use a `LivingEntity`, and no supported caller passes null.
- **W9-RUNTIME-002 - INVALID.** More Looting guards a null attacking player and non-positive Looting before applying its multiplier. Finite numeric configuration is enforced by the schema and getters.
- **W9-RUNTIME-003 - INVALID.** More Flame's constant modification is narrowed to the `getFireTicks`/`setOnFireFor` slice and the current cap is covered by the enhanced tests; an ordinal collision was not established.
- **W9-RUNTIME-004 - INVALID.** More Blast, More Protection, and More Fire use the configured clamped bases, and the finite-value rejection is enforced before these mixins consume configuration.
- **W9-RUNTIME-005 - INVALID.** The More Multishot constant report is stale: the current implementation clamps projectile count to 256 before allocation/load, and overflow/cap behavior is covered.
- **W9-RUNTIME-006 - INVALID.** The No Soul Speed constant changes the mapped `i - 1` backlash term in `addSoulSpeedBoostIfNeeded`; the deterministic test covers the intended one-point vanilla branch.
- **W9-RUNTIME-007 - INVALID.** No Thorns Backlash wraps the mapped attacker damage operation directly; the report's constant-target premise is not the current implementation.
- **W9-RUNTIME-008 - INVALID.** Disable-enchantment behavior is split between the runtime mixin and the registered loot mixin. The manifest and current tests cover both paths.
- **W9-RUNTIME-009 - INVALID.** Trident support intentionally groups the damage enchantments and separately lists Fire Aspect, Knockback, and Looting. The table hook is intentionally limited to the damage-enchantment path described by the feature contract.
- **W9-RUNTIME-010 - INVALID.** The Grindstone output branch only runs after `GrindstoneDisenchantMixin` has found a non-curse enchantment. Its component removal and repair-cost clearing match the clean-item contract.
- **W9-RUNTIME-011 - INVALID.** The `ADShiny` over-level behavior is intentional: any non-curse level at or above the configured maximum is colored, and focused tests cover over-level and non-positive-maximum cases.
- **W9-RUNTIME-012 - INVALID.** The current ConfigSync payload validates counts and entries, snapshots the map immutably, and the decoder's supported-version behavior is covered by payload tests.
- **W9-RUNTIME-013 - INVALID.** ADText exposes immutable sets and the reported null/mutation path is not present in the current utility contract.
- **W9-RUNTIME-014 - INVALID.** Disable-enchantment cache and hook behavior matches the current manifest and source; no separate stale-cache or max-level failure was reproduced.
- **W9-RUNTIME-015 - INVALID.** Protection bypass uses the current mapped damage path and the report supplies no supported caller or failing runtime scenario.
- **W9-RUNTIME-016 - INVALID.** ADUtils' numeric and binding helpers retain their guarded parsing/clamping contracts; no new runtime defect is established.
- **W9-RUNTIME-017 - INVALID.** ADConfig's current migration, normalization, persistence, and load paths have focused coverage and no reported failure remains.
- **W9-RUNTIME-018 - INVALID.** The current screen implementation uses the established bounded dimensions; the report is a low-level UI assertion without a supported failing client scenario.
- **W9-RUNTIME-019 - INVALID.** The current `fabric.mod.json` points to the existing icon and registered mixin manifest; no manifest mismatch is present.
- **W9-RELEASE-001 - INVALID for this checkout.** `release.yml` gates `github.triggering_actor` against `github.repository_owner` and `refs/heads/main`. The current remote is the personal repository `adibarra/enchant-tweaker`, so the reported organization-owner mismatch is not a current repository condition. Revisit only if ownership or release policy changes.
- **W9-ICON-001 - INVALID.** The tracked `assets/enchanttweaker/icon.png` has a valid PNG signature and `file` reports a 150x150 8-bit RGBA non-interlaced PNG. A reader labeling the decoded image as WebP does not establish a binary mismatch.
- **W9-ICON-002 - INVALID.** `icon.pdn` has a valid Paint.NET PDN3 header, dimensions, layer count, and embedded thumbnail. Its editor metadata is not a release/runtime defect.
- **W9-TEST-003 - INVALID.** `PluginConfigGameTest` already uses bounded worker joins, interruption handling, and `finally` cleanup; the unbounded-join report is stale.
- **W9-TEST-004 - INVALID.** The enhanced God Armor test's nullable explosion argument is accepted by the current Yarn/Fabric runtime, and the full current GameTest run passed; no test failure follows from that construction.

### Wave 9 disposition

- Open implementation items: **W9-TEST-001, W9-TEST-002, and W9-CONFIG-001**.
- Valid findings already fixed: **W9-DOC-001 and W9-BUILD-001**.
- All other reported items above are rejected as false, stale, intentional, or unsupported by the current Minecraft/Fabric/Yarn target and runtime evidence. **W9-TARGET-006** is resolved as intentional under the explicit projectile-position location contract; no Wave 9 item remains NEEDS-CONTEXT.

## Wave 9 implementation and final verification

- **W9-TEST-001 - FIXED.** All `PayloadGameTest` process-wide configuration mutations now occur inside the corresponding `try/finally` cleanup region, so a setup or assertion failure still restores the snapped configuration.
- **W9-TEST-002 - FIXED.** `ConfigSchemaGameTest` now uses unique temporary fixtures and symlink-safe backup/restore handling. Existing files are preserved and restored rather than removed or overwritten by predictable fixture paths.
- **W9-CONFIG-001 - FIXED.** `VillagerTradeLimitsMixin` now matches namespaced enchantment identifiers without collisions between namespaces, while retaining compatibility for legacy default-namespace `minecraft` path keys.
- The first compile attempt failed because the fixture helper's checked `IOException` was not handled. The helper was corrected at the source, after which compilation succeeded.

### Wave 9 final verification

- `spotlessApply`: PASS.
- `zizmor`: PASS, no findings (1 suppressed).
- `build`: PASS.
- `runGametest`: PASS, 454/454 tests, 0 failures, 0 errors, 0 skipped.
- Quilt smoke test: PASS with Loader 0.30.0, Minecraft 1.20.5, and 40 mixins.

- Wave 9 has no OPEN valid findings and no NEEDS-CONTEXT items. **W9-TARGET-006** is resolved under the explicit projectile-position location contract.