# Changelog

## 1.5.1

- Server config now syncs to clients on join, reload, and config change
  - ShinyNames and capmod levels update automatically without reconnecting
- Consolidate capmod system into a single unified mixin
- Add caching for feature flags and capmod levels (performance improvement)
- Fix race conditions in MoreBinding, MoreMultishot, and MoreFlame mixins
- Fix flame level being read from the wrong source in MoreFlame
- Fix dead channeling entry in capmod config
- Add configurable free-arrow rate for MoreInfinity (`more_infinity_pct`)
- Add Mending/Unbreaking incompatibility toggle (`no_mending_unbreaking`)
- Add multiplicative Protection damage reduction (`more_protection`)
- Add multiplicative Fire Protection duration reduction (`more_fire_protection`)
- Add multiplicative Blast Protection knockback reduction (`more_blast_protection`)
- Add per-damage-type Protection bypass toggle (`protection_bypass_enabled`)
- Upgrade to Java 21, Gradle 9, and Fabric Loom 1.15

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
- Config system overhaul — only apply enabled mixins
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
