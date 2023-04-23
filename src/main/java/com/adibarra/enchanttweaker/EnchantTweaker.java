package com.adibarra.enchanttweaker;

import com.magistermaks.simpleconfig.SimpleConfig;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnchantTweaker implements ModInitializer {

	private static final Logger LOGGER = LoggerFactory.getLogger("EnchantTweaker");

	private static SimpleConfig CONFIG;

	private static boolean MOD_ENABLED;

    @Override
    public void onInitialize() {
		reloadConfig();
		LOGGER.info(String.format("Enchant Tweaker %sabled!", MOD_ENABLED ? "En" : "Dis"));
		if (MOD_ENABLED) {
			ETCommands.registerCommands();
			for (String mixin : ETMixinPlugin.getConflicts().keySet()) {
				ETUtils.Conflict c = ETMixinPlugin.getConflicts().get(mixin);
				if (c.condition().getAsBoolean())
					LOGGER.info("[COMPAT] Disabled " + mixin + ". Reason: " + c.reason());
			}
			int active_mixins = ETMixinPlugin.getNumMixins() - ETMixinPlugin.getConflicts().size();
			LOGGER.info("Enchant Tweaker is ready to go! " + String.format("Applied %d Mixins.", active_mixins));
		}
	}

	public static void reloadConfig() {
		EnchantTweaker.CONFIG = SimpleConfig
			.of("enchant-tweaker")
			.provider(EnchantTweaker::getDefaultConfig)
			.request();
		EnchantTweaker.MOD_ENABLED = getConfig().getOrDefault("mod_enabled", true);
	}

    @SuppressWarnings({"SameReturnValue", "unused"})
	public static String getDefaultConfig(String filename) {
		return
    		"""
			####################################################################################################
			####  Enchant Tweaker Config
			####################################################################################################
			
				################################################################################
				###  Master Switch:
				###    Enable/Disable the entire mod.
				###
					  mod_enabled=true
				###
				################################################################################
			
				################################################################################
				###  Anvil Tweaks:
				###    Some small anvil related tweaks. Lightly alters the anvil's mechanics.
				###
				###    ########################################
				###    ##  Cheap Names:
				###    ##    Normally renaming an item will cost a similar amount of levels as adding an enchantment onto an item.
				###    ##    Enabling this will force the cost for renaming items to always be one level.
				###    ##    For those who don't enjoy spending nineteen levels to rename a pickaxe... again.
				###    ##
							cheap_names=true
				###    ##
				###    ########################################
				###    ##  Not Too Expensive:
				###    ##    Normally once an item's enchant/repair cost reaches 40 levels you can no longer enchant or repair it.
				###    ##    Enabling this tweak alters the "Too Expensive!" mechanic in the anvil changing the level it activates at to one of your choosing.
				###    ##
							not_too_expensive=true
							nte_max_cost=2147483647
				###    ##
				###    ########################################
				###    ##  Prior Work is Cheaper:
				###    ##    Normally when enchanting/repairing an item, each operation will double the cost of the next action.
				###    ##    Enabling this tweak will let you customize the penalty.
				###    ##
							prior_work_cheaper=true
							pw_cost_multiplier=1.33
				###    ##
				###    ########################################
				###    ##  Prior Work is Free:
				###    ##    Normally when enchanting/repairing an item, each operation will double the cost of the next action.
				###    ##    Enabling this tweak completely disables the prior work penalty for items enchanted/repaired at an anvil.
				###    ##    This means that the enchant/repair cost for an item will always stay at the minimum value for that given procedure.
				###    ##
							prior_work_free=false
				###    ##
				###    ########################################
				###    ##  Sturdy Anvils:
				###    ##    Normally an anvil has a 12% (0.12) chance to take damage when used.
				###    ##    Enabling this tweak will let you customize the damage chance.
				###    ##
							sturdy_anvils=true
							anvil_damage_chance=0.6
				###    ##
				###    ########################################
				###
				################################################################################
			
				################################################################################
				###  Enhanced Enchantments:
				###    Some vanilla enchantments tweaked to scale better.
				###    Some of these require the enchantment's max level to be increased to take full advantage of the tweak.
				###
				###    ########################################
				###    ##  More Channeling:
				###    ##    Normally Channeling only works during thunderstorms.
				###    ##    Enabling this tweak will allow Channeling II to work during rain as well.
				###    ##    No further special effects for higher levels.
				###    ##
							more_channeling=true
				###    ##
				###    ########################################
				###    ##  More Flame:
				###    ##    Normally Flame lasts 5 seconds.
				###    ##    Enabling this tweak will allow Flame to scale with enchantment level.
				###    ##    Each additional level will add 2 seconds to the duration.
				###    ##    Effect does not max out.
				###    ##
							more_flame=true
				###    ##
				###    ########################################
				###    ##  More Mending:
				###    ##    Enabling this tweak will allow Mending to scale with enchantment level.
				###    ##    Mending II is the same as vanilla Mending.
				###    ##    Mending I has ~10% XP efficiency loss and Mending III has ~10% XP efficiency gain.
				###    ##    The effect maxes out at level X.
				###    ##    Formula: Repair Cost = 0.6 - 0.05 * mendingLevel.
				###    ##
							more_mending=true
				###    ##
				###    ########################################
				###    ##  More Multishot:
				###    ##    Enabling this tweak will allow Multishot to scale with enchantment level.
				###    ##    Each additional level will add 2 arrows to the shot.
				###    ##    Crossbows take damage for each Multishot arrow shot.
				###    ##    Effect does not max out.
				###    ##
							more_multishot=true
				###    ##
				###    ########################################
				###
				################################################################################
			
				################################################################################
				###  Other Tweaks:
				###    Some small tweaks that don't fit into the other categories.
				###    These are some of the more popular ones.
				###
				###    ########################################
				###    ##  Axes are Not Tools:
				###    ##    Normally axes are treated as tools when used in combat.
				###    ##    This causes them to take double durability damage when they are used in combat.
				###    ##    Enabling this tweak removes the double durability damage penalty.
				###    ##
							axes_not_tools=true
				###    ##
				###    ########################################
				###    ##  Axe Weapons:
				###    ##    Allow the addition of some weapon enchantments that that normally can not be added onto axes.
				###    ##    Enabling this tweak allows you to add the following enchantments to axes: Fire Aspect, Knockback, and Looting.
				###    ##
							axe_weapons=true
				###    ##
				###    ########################################
				###    ##  Bow Infinity Fix:
				###    ##    Normally even though you have Infinity on a bow, you need to have arrows in your inventory to shoot.
				###    ##    Enabling this tweak will allow you to shoot arrows without having them in your inventory.
				###    ##
							bow_infinity_fix=true
				###    ##
				###    ########################################
				###    ##  God Armor:
				###    ##    Allow the combination of damage negation enchantments that normally can not be added together.
				###    ##    Enabling this tweak allows you to combine the following enchantments: Protection, Blast Protection, Fire Protection, and Projectile Protection.
				###    ##
							god_armor=true
				###    ##
				###    ########################################
				###    ##  God Weapons:
				###    ##    Allow the combination of damage enhancement enchantments that normally can not be added together.
				###    ##    Enabling this tweak allows you to combine the following enchantments: Sharpness, Smite, and Bane of Arthropods.
				###    ##
							god_weapons=true
				###    ##
				###    ########################################
				###    ##  Infinite Mending:
				###    ##    Normally you need to choose between having either Mending or Infinity.
				###    ##    Enabling this tweak allows both enchantments to coexist.
				###    ##
							infinite_mending=true
				###    ##
				###    ########################################
				###    ##  Loyal Void Tridents:
				###    ##    Normally tridents enchanted with Loyalty will be lost if thrown into the void.
				###    ##    Enabling this tweak will allow those tridents to return to the player.
				###    ##
							loyal_void_tridents=true
				###    ##
				###    ########################################
				###    ##  No Soul Speed Backlash:
				###    ##    Normally boots will take damage when walking on soul sand with Soul Speed.
				###    ##    Enabling this tweak will prevent your boots from taking damage from the enchantment.
				###    ##
							no_soul_speed_backlash=true
				###    ##
				###    ########################################
				###    ##  No Thorns Backlash:
				###    ##    Normally armor will take damage when Thorns is triggered.
				###    ##    Enabling this tweak will prevent your armor from taking damage from the enchantment.
				###    ##
							no_thorns_backlash=true
				###    ##
				###    ########################################
				###    ##  Trident Weapons:
				###    ##    Allow the addition of some weapon enchantments that normally can not be added to tridents.
				###    ##    Enabling this tweak allows you to add the following enchantments to tridents: Sharpness, Smite, Bane of Arthropods, Fire Aspect, Knockback, and Looting.
				###    ##
							trident_weapons=true
				###    ##
				###    ########################################
				###
				################################################################################
			
				################################################################################
				###  Modify Max Enchantment Levels:
				###    This is useful if you want to make individual enchantments more powerful or less powerful.
				###    Not all vanilla enchantments scale by default.
				###    Some may require enabling their respective tweak to scale properly.
				###
				###    Accepted values: 1-255, 0 (disabled), -1 (default)
				###
				###    ########################################
				###    ##  Armor Enchantments:
				###    ##
							protection=10
							blast_protection=10
							fire_protection=10
							projectile_protection=10
							feather_falling=7
							soul_speed=5
							swift_sneak=4
							thorns=5
							respiration=10
							depth_strider=-1
							frost_walker=-1
							aqua_affinity=-1
				###    ##
				###    ########################################
				###    ##  Curse Enchantments:
				###    ##
							curse_of_binding=-1
							curse_of_vanishing=-1
				###    ##
				###    ########################################
				###    ##  Melee Enchantments:
				###    ##
							sharpness=10
							smite=10
							bane_of_arthropods=10
							knockback=-1
							fire_aspect=3
							looting=5
							sweeping_edge=5
							knockback=-1
							impaling=-1
				###    ##
				###    ########################################
				###    ##  Ranged Enchantments:
				###    ##
							power=10
							punch=-1
							flame=2
							infinity=-1
							multishot=3
							piercing=5
							quick_charge=-1
							riptide=-1
							channeling=2
							loyalty=5
				###    ##
				###    ########################################
				###    ##  Tool Enchantments:
				###    ##
							efficiency=10
							silk_touch=-1
							fortune=5
							lure=-1
							luck_of_the_sea=5
							mending=3
							unbreaking=10
				###    ##
				###    ########################################
				###
				################################################################################
			
			####################################################################################################
			####  End of Enchant Tweaker Config
			####################################################################################################
			""";
    }

    public static SimpleConfig getConfig() {
        return CONFIG;
    }

    public static Boolean isEnabled() {
        return EnchantTweaker.MOD_ENABLED;
    }

    public static Logger getLogger() {
        return EnchantTweaker.LOGGER;
    }
}
