# Changelog

## 1.6.0

### Added

- Sync server config to clients on join, reload, and config change
  - ShinyNames and capmod levels update automatically without reconnecting
- Update `/et reload` to also re-sync connected clients
- Add `/et config list [category] [page]` to browse config values by category, paginated
- Add `/et config reset <key>` and `/et config reset all` to restore bundled defaults
- Add `/et diagnose` to explain settings that are not working and show config details
- Add value tab-completion to `/et config`
- Validate `/et config` values by type and reject invalid input with a hint instead of saving it
- Add anvil repair: sneak and right-click a damaged anvil with iron to reverse one degradation step at a configurable ingot cost; iron blocks are accepted when the cost is a multiple of 9 (`anvil_repair`, `anvil_repair_ingot_cost`)
- Add grindstone disenchanting: extract enchantments into books, with book splitting support (`grindstone_disenchant`)
- Add custom XP scaling to replace vanilla's tiered XP formula with a configurable linear curve (`xp_scaling`)
- Add configurable villager trade limits for each enchantment (`villager_trade_limits`)
- Add enchantment disabling to keep specific enchantments from appearing anywhere (`disable_enchantments_enabled`)
- Add multiplicative Protection damage reduction (`more_protection`)
- Add multiplicative Fire Protection duration reduction (`more_fire_protection`)
- Add multiplicative Blast Protection knockback reduction (`more_blast_protection`)
- Add per-damage-type Protection bypass toggle (`protection_bypass_enabled`)
- Add configurable free-arrow rate for MoreInfinity (`more_infinity_pct`)
- Add Looting XP boost for mob kills with a configurable multiplier (`more_looting`)
- Add Mending/Unbreaking incompatibility toggle (`no_mending_unbreaking`)
- Allow Looting on bows and crossbows (`bow_looting`)
- Generate Roman numerals for enchantment levels above X (`roman_numerals`)
- Make enhanced enchantment formula constants configurable (protection bases, mending step/floor, multishot/flame per-level, binding step)

### Fixed

- Fix More Mending computing worst-case cost when Better Mending is also enabled
- Fix non-Flame fire arrows getting a shortened burn time
- Fix flame level being read from the wrong source in MoreFlame
- Fix renaming costing more than one level with `cheap_names` and `prior_work_free` combined
- Fix race conditions in the MoreBinding, MoreMultishot, and MoreFlame mixins
- Fix dead channeling entry in capmod config
- Regenerate a blank or unreadable config from defaults instead of deleting it
- Clamp out-of-range config values (protection bases, looting multiplier, multishot per-level, xp scaling overflow, mending floor)
- Preserve MoreFlame levels on fired projectiles across weapon switches and save/load
- Keep CheapNames from changing costs when an anvil operation has no valid output
- Reject malformed booleans and non-finite decimal config values
- Ignore malformed Protection bypass IDs instead of crashing damage handling
- Clear pending MoreBinding armor when a player disconnects
- Include exact random-threshold boundary rolls for MoreBinding and MoreInfinity

### Internal

- Consolidate the capmod system into a single unified mixin
- Cache feature flags and capmod levels (performance improvement)
- Add a config migration framework and internal `config_version` key for future upgrades
- Make config storage thread-safe for concurrent sync and gameplay reads
- Version the client sync payload and exclude client-only cosmetic prefs (`roman_numerals`, `shiny_name`) from sync
- Declare `fabric-api` and `minecraft` dependencies in fabric.mod.json
- Add a GitHub Actions CI workflow that builds the mod and runs the gametests
- Add a Quilt Loader server smoke test to CI
- Upgrade to Java 21, Gradle 9, and Fabric Loom 1.15
- Expand the test suite to 422 gametests

## 1.5.0

- Add support for modifying config at runtime when using commands without needing to restart
- Fixes several bugs

## 1.4.9.1

- Fix crash on 1.20.1

## 1.4.9

- Fix config saving in wrong directory

## 1.4.8

- Fix `/et reload` command not saving changes
- Clarify which changes require server/client restarts
- Clean up command outputs

## 1.4.7

- Fix enchanting table sometimes adding duplicate enchants

## 1.4.6

- Add support for MC 1.20.4
- Build against new fabric loader
  - Remove bundled MixinExtras
  - Reduces jar size by 48%

## 1.4.5

- Clarify which tweaks require client-side install
- Disable all tweaks in config by default
- Fix some settings not saving when using in-game commands
- Bump MixinExtras dependency version

## 1.4.4

- Fixes translation strings
- Adds more enchantment level roman numerals

## 1.4.3

- Adds support for MC 1.20+
- Fixes bad default config value
  - `anvil_damage_chance` 0.6 -> 0.06
- Compresses mod icon to reduce jar size
- Updates mod description

## 1.4.2

- Updated dependency `MixinExtras`
  - Fixes issues with other mods that use `MixinExtras`

## 1.4.1

- Adds `AxesAreWeapons` mod compatibility
  - Auto disables similar tweak giving priority to `AxesAreWeapons`

## 1.4.0

- Adds Quilt loader compatibility
- Adds BetterMending
  - `better_mending=true` to enable
  - Check readme/description for more info
- Adds MoreInfinity
  - `more_infinity=true` to enable (overrides bow_infinity_fix)
  - Check readme/description for more info
  - Thanks to James103 for the suggestion!
- Condensed multiple commands into one
  - `/et config <key> [value]` supersedes `keys`, `set`, and `get` cmds
  - Gets the current value if not supplied one, sets the value if it is
- Updates logo/icon

## 1.3.1

- Fixes tridents not accepting some enchantments
- Improves compatibility with other mods
- Adds minor performance improvements

## 1.3.0

- Fixed non-infinity bow playing pulling animation without arrows
- Adds curse enchantments now also affected by Shiny Names
- Adds option to allow Multishot and Piercing to coexist
  - `multishot_piercing=true` to enable it
- Adds level scaling for Curse of Binding
  - `more_binding=true` to enable it
  - See readme/description for what it does

## 1.2.1

- Fix broken 'click to run' command popups
- Improve compatibility system
- Set command now persists changes across restarts
- Remove default custom enchantment levels

## 1.2.0

- Fixed bug when shooting item frames with a flame arrow
- Added more commands
  - `/et help` for the full command list
  - Get and set the values for config keys in game
  - Set command does not (yet) persist changes across restarts
- More optimizations, cleanup, and improvements under the hood

## 1.1.0

> **Note:** If previously installed, delete the old config file (or add `capmod_enabled=true` to re-enable custom enchantment levels).

- Fix Fabrication incompatibility
- Config system overhaul to only apply enabled mixins
- Large codebase refactor
- More optimization
- Remove bloat from jar

## 1.0.2

- LuckEnchantments now respect config
- Re-enable Shiny Max Enchantment Names
- Clean up config keys

## 1.0.1

- Adds support for MC 1.19.4

## 1.0.0

- Initial Release
