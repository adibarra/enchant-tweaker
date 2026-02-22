package com.adibarra.enchanttweaker.test;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * GameTest suite for EnchantTweaker
 */
public class EnchantTweakerGameTest implements FabricGameTest {

    // ─── helpers ──────────────────────────────────────────────────────────────

    private static void setCapmod(boolean enabled) {
        ETMixinPlugin.getConfig().set("capmod_enabled", String.valueOf(enabled));
    }

    private static void setEnchantCap(String key, int level) {
        ETMixinPlugin.getConfig().set(key, String.valueOf(level));
    }

    private static void setFeature(String key, boolean on) {
        ETMixinPlugin.getConfig().set(key, String.valueOf(on));
    }

    /** Calls the protected Enchantment.canAccept via reflection. */
    private static boolean canAccept(Enchantment enchantment, Enchantment other) {
        try {
            Method m = Enchantment.class.getDeclaredMethod("canAccept", Enchantment.class);
            m.setAccessible(true);
            return (boolean) m.invoke(enchantment, other);
        } catch (Exception e) {
            throw new RuntimeException("canAccept reflection failed", e);
        }
    }

    /** Calls the protected Enchantment.isAcceptableItem via reflection. */
    private static boolean isAcceptableItem(Enchantment enchantment, ItemStack stack) {
        try {
            Method m = Enchantment.class.getDeclaredMethod("isAcceptableItem", ItemStack.class);
            m.setAccessible(true);
            return (boolean) m.invoke(enchantment, stack);
        } catch (Exception e) {
            throw new RuntimeException("isAcceptableItem reflection failed", e);
        }
    }

    /** Calls static AnvilScreenHandler.getNextCost via reflection. */
    private static int getNextAnvilCost(int cost) {
        try {
            Method m = AnvilScreenHandler.class.getDeclaredMethod("getNextCost", int.class);
            m.setAccessible(true);
            return (int) m.invoke(null, cost);
        } catch (Exception e) {
            throw new RuntimeException("getNextCost reflection failed", e);
        }
    }

    /** Calls ExperienceOrbEntity.repairPlayerGears(PlayerEntity, int) via reflection. */
    private static int repairPlayerGears(ExperienceOrbEntity orb, PlayerEntity player, int amount) {
        try {
            Method m = ExperienceOrbEntity.class.getDeclaredMethod("repairPlayerGears", PlayerEntity.class, int.class);
            m.setAccessible(true);
            return (int) m.invoke(orb, player, amount);
        } catch (Exception e) {
            throw new RuntimeException("repairPlayerGears reflection failed", e);
        }
    }

    // ─── Smoke tests ─────────────────────────────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void smokeTestBlockPlace(TestContext helper) {
        BlockPos pos = new BlockPos(0, 2, 0);
        helper.setBlockState(pos, Blocks.STONE.getDefaultState());
        helper.expectBlock(Blocks.STONE, pos);
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void smokeTestBlockBreak(TestContext helper) {
        BlockPos pos = new BlockPos(0, 2, 0);
        helper.setBlockState(pos, Blocks.STONE.getDefaultState());
        helper.expectBlock(Blocks.STONE, pos);
        helper.setBlockState(pos, Blocks.AIR.getDefaultState());
        helper.dontExpectBlock(Blocks.STONE, pos);
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void smokeTestModLoaded(TestContext helper) {
        helper.assertTrue(ETMixinPlugin.getConfig() != null, "ETMixinPlugin config should be initialized");
        helper.complete();
    }

    // ─── Capmod master switch ────────────────────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void capmodDisabled(TestContext helper) {
        setCapmod(false);
        setEnchantCap("sharpness", 10);
        try {
            helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() == 5,
                "Sharpness should be vanilla 5 when capmod disabled (got " + Enchantments.SHARPNESS.getMaxLevel() + ")");
        } finally {
            setCapmod(true);
            setEnchantCap("sharpness", -1);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void capmodVanillaPassthrough(TestContext helper) {
        setCapmod(true);
        // all caps already -1 from run/gametest config; just verify passthrough
        helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() == 5,    "Sharpness -1 should passthrough to vanilla 5");
        helper.assertTrue(Enchantments.UNBREAKING.getMaxLevel() == 3,   "Unbreaking -1 should passthrough to vanilla 3");
        helper.assertTrue(Enchantments.LOOTING.getMaxLevel() == 3,      "Looting -1 should passthrough to vanilla 3");
        helper.assertTrue(Enchantments.PROTECTION.getMaxLevel() == 4,   "Protection -1 should passthrough to vanilla 4");
        helper.assertTrue(Enchantments.MENDING.getMaxLevel() == 1,      "Mending -1 should passthrough to vanilla 1");
        helper.complete();
    }

    // ─── Capmod DamageEnchant ────────────────────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void capmodDamageCustom(TestContext helper) {
        setCapmod(true);
        setEnchantCap("sharpness", 10);
        setEnchantCap("smite", 7);
        setEnchantCap("bane_of_arthropods", 8);
        try {
            helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() == 10,         "Sharpness should be 10");
            helper.assertTrue(Enchantments.SMITE.getMaxLevel() == 7,              "Smite should be 7");
            helper.assertTrue(Enchantments.BANE_OF_ARTHROPODS.getMaxLevel() == 8, "Bane of Arthropods should be 8");
        } finally {
            setEnchantCap("sharpness", -1);
            setEnchantCap("smite", -1);
            setEnchantCap("bane_of_arthropods", -1);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void capmodDamageClampMax(TestContext helper) {
        setCapmod(true);
        setEnchantCap("sharpness", 300);
        try {
            helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() == 255, "Sharpness 300 should clamp to 255");
        } finally {
            setEnchantCap("sharpness", -1);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void capmodDamageZero(TestContext helper) {
        setCapmod(true);
        setEnchantCap("sharpness", 0);
        try {
            helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() == 0, "Sharpness should be 0");
        } finally {
            setEnchantCap("sharpness", -1);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void capmodDamageNegPassthrough(TestContext helper) {
        setCapmod(true);
        setEnchantCap("sharpness", -1);
        helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() == 5, "Sharpness -1 should passthrough to vanilla 5");
        helper.complete();
    }

    // ─── Capmod GenericEnchant ───────────────────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void capmodGenericCustom(TestContext helper) {
        setCapmod(true);
        setEnchantCap("unbreaking", 10);
        setEnchantCap("efficiency", 8);
        try {
            helper.assertTrue(Enchantments.UNBREAKING.getMaxLevel() == 10, "Unbreaking should be 10");
            helper.assertTrue(Enchantments.EFFICIENCY.getMaxLevel() == 8,  "Efficiency should be 8");
        } finally {
            setEnchantCap("unbreaking", -1);
            setEnchantCap("efficiency", -1);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void capmodGenericClampMax(TestContext helper) {
        setCapmod(true);
        setEnchantCap("unbreaking", 300);
        try {
            helper.assertTrue(Enchantments.UNBREAKING.getMaxLevel() == 255, "Unbreaking 300 should clamp to 255");
        } finally {
            setEnchantCap("unbreaking", -1);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void capmodGenericAllRemaining(TestContext helper) {
        setCapmod(true);
        setEnchantCap("depth_strider",  5);
        setEnchantCap("fire_aspect",    4);
        setEnchantCap("frost_walker",   4);
        setEnchantCap("impaling",       8);
        setEnchantCap("knockback",      5);
        setEnchantCap("loyalty",        6);
        setEnchantCap("lure",           6);
        setEnchantCap("piercing",       8);
        setEnchantCap("power",          8);
        setEnchantCap("punch",          5);
        setEnchantCap("quick_charge",   5);
        setEnchantCap("respiration",    5);
        setEnchantCap("riptide",        6);
        setEnchantCap("soul_speed",     5);
        setEnchantCap("sweeping_edge",  5);
        setEnchantCap("swift_sneak",    5);
        setEnchantCap("thorns",         5);
        try {
            helper.assertTrue(Enchantments.DEPTH_STRIDER.getMaxLevel()    == 5, "Depth Strider should be 5");
            helper.assertTrue(Enchantments.FIRE_ASPECT.getMaxLevel()      == 4, "Fire Aspect should be 4");
            helper.assertTrue(Enchantments.FROST_WALKER.getMaxLevel()     == 4, "Frost Walker should be 4");
            helper.assertTrue(Enchantments.IMPALING.getMaxLevel()         == 8, "Impaling should be 8");
            helper.assertTrue(Enchantments.KNOCKBACK.getMaxLevel()        == 5, "Knockback should be 5");
            helper.assertTrue(Enchantments.LOYALTY.getMaxLevel()          == 6, "Loyalty should be 6");
            helper.assertTrue(Enchantments.LURE.getMaxLevel()             == 6, "Lure should be 6");
            helper.assertTrue(Enchantments.PIERCING.getMaxLevel()         == 8, "Piercing should be 8");
            helper.assertTrue(Enchantments.POWER.getMaxLevel()            == 8, "Power should be 8");
            helper.assertTrue(Enchantments.PUNCH.getMaxLevel()            == 5, "Punch should be 5");
            helper.assertTrue(Enchantments.QUICK_CHARGE.getMaxLevel()     == 5, "Quick Charge should be 5");
            helper.assertTrue(Enchantments.RESPIRATION.getMaxLevel()      == 5, "Respiration should be 5");
            helper.assertTrue(Enchantments.RIPTIDE.getMaxLevel()          == 6, "Riptide should be 6");
            helper.assertTrue(Enchantments.SOUL_SPEED.getMaxLevel()       == 5, "Soul Speed should be 5");
            helper.assertTrue(Enchantments.SWEEPING.getMaxLevel()         == 5, "Sweeping Edge should be 5");
            helper.assertTrue(Enchantments.SWIFT_SNEAK.getMaxLevel()      == 5, "Swift Sneak should be 5");
            helper.assertTrue(Enchantments.THORNS.getMaxLevel()           == 5, "Thorns should be 5");
        } finally {
            setEnchantCap("depth_strider",  -1);
            setEnchantCap("fire_aspect",    -1);
            setEnchantCap("frost_walker",   -1);
            setEnchantCap("impaling",       -1);
            setEnchantCap("knockback",      -1);
            setEnchantCap("loyalty",        -1);
            setEnchantCap("lure",           -1);
            setEnchantCap("piercing",       -1);
            setEnchantCap("power",          -1);
            setEnchantCap("punch",          -1);
            setEnchantCap("quick_charge",   -1);
            setEnchantCap("respiration",    -1);
            setEnchantCap("riptide",        -1);
            setEnchantCap("soul_speed",     -1);
            setEnchantCap("sweeping_edge",  -1);
            setEnchantCap("swift_sneak",    -1);
            setEnchantCap("thorns",         -1);
        }
        helper.complete();
    }

    // ─── Capmod LuckEnchant ──────────────────────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void capmodLuckCustom(TestContext helper) {
        setCapmod(true);
        setEnchantCap("looting", 10);
        setEnchantCap("fortune", 8);
        setEnchantCap("luck_of_the_sea", 6);
        try {
            helper.assertTrue(Enchantments.LOOTING.getMaxLevel() == 10,       "Looting should be 10");
            helper.assertTrue(Enchantments.FORTUNE.getMaxLevel() == 8,        "Fortune should be 8");
            helper.assertTrue(Enchantments.LUCK_OF_THE_SEA.getMaxLevel() == 6, "Luck of the Sea should be 6");
        } finally {
            setEnchantCap("looting", -1);
            setEnchantCap("fortune", -1);
            setEnchantCap("luck_of_the_sea", -1);
        }
        helper.complete();
    }

    // ─── Capmod ProtectionEnchant ────────────────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void capmodProtectionCustom(TestContext helper) {
        setCapmod(true);
        setEnchantCap("protection", 10);
        setEnchantCap("fire_protection", 8);
        try {
            helper.assertTrue(Enchantments.PROTECTION.getMaxLevel() == 10,      "Protection should be 10");
            helper.assertTrue(Enchantments.FIRE_PROTECTION.getMaxLevel() == 8,  "Fire Protection should be 8");
        } finally {
            setEnchantCap("protection", -1);
            setEnchantCap("fire_protection", -1);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void capmodProtectionAllTypes(TestContext helper) {
        setCapmod(true);
        setEnchantCap("protection", 6);
        setEnchantCap("fire_protection", 6);
        setEnchantCap("feather_falling", 6);
        setEnchantCap("blast_protection", 6);
        setEnchantCap("projectile_protection", 6);
        try {
            helper.assertTrue(Enchantments.PROTECTION.getMaxLevel() == 6,             "Protection should be 6");
            helper.assertTrue(Enchantments.FIRE_PROTECTION.getMaxLevel() == 6,        "Fire Protection should be 6");
            helper.assertTrue(Enchantments.FEATHER_FALLING.getMaxLevel() == 6,        "Feather Falling should be 6");
            helper.assertTrue(Enchantments.BLAST_PROTECTION.getMaxLevel() == 6,       "Blast Protection should be 6");
            helper.assertTrue(Enchantments.PROJECTILE_PROTECTION.getMaxLevel() == 6,  "Projectile Protection should be 6");
        } finally {
            setEnchantCap("protection", -1);
            setEnchantCap("fire_protection", -1);
            setEnchantCap("feather_falling", -1);
            setEnchantCap("blast_protection", -1);
            setEnchantCap("projectile_protection", -1);
        }
        helper.complete();
    }

    // ─── Capmod SpecialEnchant ───────────────────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void capmodSpecialCustom(TestContext helper) {
        setCapmod(true);
        setEnchantCap("mending", 5);
        setEnchantCap("infinity", 3);
        setEnchantCap("silk_touch", 2);
        try {
            helper.assertTrue(Enchantments.MENDING.getMaxLevel() == 5,    "Mending should be 5");
            helper.assertTrue(Enchantments.INFINITY.getMaxLevel() == 3,   "Infinity should be 3");
            helper.assertTrue(Enchantments.SILK_TOUCH.getMaxLevel() == 2, "Silk Touch should be 2");
        } finally {
            setEnchantCap("mending", -1);
            setEnchantCap("infinity", -1);
            setEnchantCap("silk_touch", -1);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void capmodSpecialClampMax(TestContext helper) {
        setCapmod(true);
        setEnchantCap("mending", 300);
        try {
            helper.assertTrue(Enchantments.MENDING.getMaxLevel() == 255, "Mending 300 should clamp to 255");
        } finally {
            setEnchantCap("mending", -1);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void capmodSpecialAllRemaining(TestContext helper) {
        setCapmod(true);
        setEnchantCap("aqua_affinity",   3);
        setEnchantCap("binding_curse",   5);
        setEnchantCap("channeling",      3);
        setEnchantCap("multishot",       5);
        setEnchantCap("vanishing_curse", 3);
        try {
            helper.assertTrue(Enchantments.AQUA_AFFINITY.getMaxLevel()   == 3, "Aqua Affinity should be 3");
            helper.assertTrue(Enchantments.BINDING_CURSE.getMaxLevel()   == 5, "Binding Curse should be 5");
            helper.assertTrue(Enchantments.CHANNELING.getMaxLevel()      == 3, "Channeling should be 3");
            helper.assertTrue(Enchantments.MULTISHOT.getMaxLevel()       == 5, "Multishot should be 5");
            helper.assertTrue(Enchantments.VANISHING_CURSE.getMaxLevel() == 3, "Vanishing Curse should be 3");
        } finally {
            setEnchantCap("aqua_affinity",   -1);
            setEnchantCap("binding_curse",   -1);
            setEnchantCap("channeling",      -1);
            setEnchantCap("multishot",       -1);
            setEnchantCap("vanishing_curse", -1);
        }
        helper.complete();
    }

    // ─── GodArmor ────────────────────────────────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void godArmorEnabled(TestContext helper) {
        setFeature("god_armor", true);
        helper.assertTrue(canAccept(Enchantments.PROTECTION, Enchantments.BLAST_PROTECTION),
            "Protection should accept Blast Protection when enabled");
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void godArmorDisabled(TestContext helper) {
        setFeature("god_armor", false);
        try {
            helper.assertTrue(!canAccept(Enchantments.PROTECTION, Enchantments.BLAST_PROTECTION),
                "Protection should not accept Blast Protection when disabled");
        } finally {
            setFeature("god_armor", true);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void godArmorAllTypes(TestContext helper) {
        setFeature("god_armor", true);
        // different types: all allowed
        helper.assertTrue(canAccept(Enchantments.PROTECTION, Enchantments.FIRE_PROTECTION),       "Protection accepts Fire Protection");
        helper.assertTrue(canAccept(Enchantments.PROTECTION, Enchantments.FEATHER_FALLING),       "Protection accepts Feather Falling");
        helper.assertTrue(canAccept(Enchantments.PROTECTION, Enchantments.BLAST_PROTECTION),      "Protection accepts Blast Protection");
        helper.assertTrue(canAccept(Enchantments.PROTECTION, Enchantments.PROJECTILE_PROTECTION), "Protection accepts Projectile Protection");
        // same type: still blocked
        helper.assertTrue(!canAccept(Enchantments.PROTECTION, Enchantments.PROTECTION),           "Protection should not accept itself");
        helper.complete();
    }

    // ─── GodWeapons ──────────────────────────────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void godWeaponsEnabled(TestContext helper) {
        setFeature("god_weapons", true);
        helper.assertTrue(canAccept(Enchantments.SHARPNESS, Enchantments.SMITE),             "Sharpness should accept Smite when enabled");
        helper.assertTrue(canAccept(Enchantments.SHARPNESS, Enchantments.BANE_OF_ARTHROPODS), "Sharpness should accept Bane when enabled");
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void godWeaponsDisabled(TestContext helper) {
        setFeature("god_weapons", false);
        try {
            helper.assertTrue(!canAccept(Enchantments.SHARPNESS, Enchantments.SMITE),
                "Sharpness should not accept Smite when disabled");
        } finally {
            setFeature("god_weapons", true);
        }
        helper.complete();
    }

    // ─── InfiniteMending ─────────────────────────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void infiniteMendingEnabled(TestContext helper) {
        setFeature("infinite_mending", true);
        helper.assertTrue(canAccept(Enchantments.INFINITY, Enchantments.MENDING),
            "Infinity should accept Mending when enabled");
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void infiniteMendingDisabled(TestContext helper) {
        setFeature("infinite_mending", false);
        try {
            helper.assertTrue(!canAccept(Enchantments.INFINITY, Enchantments.MENDING),
                "Infinity should not accept Mending when disabled");
        } finally {
            setFeature("infinite_mending", true);
        }
        helper.complete();
    }

    // ─── MultishotPiercing ───────────────────────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void multishotPiercingEnabled(TestContext helper) {
        setFeature("multishot_piercing", true);
        helper.assertTrue(canAccept(Enchantments.MULTISHOT, Enchantments.PIERCING),
            "Multishot should accept Piercing when enabled");
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void multishotPiercingDisabled(TestContext helper) {
        setFeature("multishot_piercing", false);
        try {
            helper.assertTrue(!canAccept(Enchantments.MULTISHOT, Enchantments.PIERCING),
                "Multishot should not accept Piercing when disabled");
        } finally {
            setFeature("multishot_piercing", true);
        }
        helper.complete();
    }

    // ─── TridentWeapons ──────────────────────────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void tridentWeaponsEnabled(TestContext helper) {
        setFeature("trident_weapons", true);
        ItemStack trident = new ItemStack(Items.TRIDENT);
        helper.assertTrue(isAcceptableItem(Enchantments.FIRE_ASPECT, trident), "Fire Aspect should be acceptable on trident");
        helper.assertTrue(isAcceptableItem(Enchantments.KNOCKBACK, trident),   "Knockback should be acceptable on trident");
        helper.assertTrue(isAcceptableItem(Enchantments.LOOTING, trident),     "Looting should be acceptable on trident");
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void tridentWeaponsDisabled(TestContext helper) {
        setFeature("trident_weapons", false);
        ItemStack trident = new ItemStack(Items.TRIDENT);
        try {
            helper.assertTrue(!isAcceptableItem(Enchantments.FIRE_ASPECT, trident), "Fire Aspect should not be acceptable on trident when disabled");
            helper.assertTrue(!isAcceptableItem(Enchantments.KNOCKBACK, trident),   "Knockback should not be acceptable on trident when disabled");
            helper.assertTrue(!isAcceptableItem(Enchantments.LOOTING, trident),     "Looting should not be acceptable on trident when disabled");
        } finally {
            setFeature("trident_weapons", true);
        }
        helper.complete();
    }

    // ─── AxeWeapons ──────────────────────────────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void axeWeaponsEnabled(TestContext helper) {
        setFeature("axe_weapons", true);
        ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
        helper.assertTrue(isAcceptableItem(Enchantments.FIRE_ASPECT, axe), "Fire Aspect should be acceptable on axe");
        helper.assertTrue(isAcceptableItem(Enchantments.KNOCKBACK, axe),   "Knockback should be acceptable on axe");
        helper.assertTrue(isAcceptableItem(Enchantments.LOOTING, axe),     "Looting should be acceptable on axe");
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void axeWeaponsDisabled(TestContext helper) {
        setFeature("axe_weapons", false);
        ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
        try {
            helper.assertTrue(!isAcceptableItem(Enchantments.FIRE_ASPECT, axe), "Fire Aspect should not be acceptable on axe when disabled");
            helper.assertTrue(!isAcceptableItem(Enchantments.KNOCKBACK, axe),   "Knockback should not be acceptable on axe when disabled");
            helper.assertTrue(!isAcceptableItem(Enchantments.LOOTING, axe),     "Looting should not be acceptable on axe when disabled");
        } finally {
            setFeature("axe_weapons", true);
        }
        helper.complete();
    }

    // ─── PriorWorkFree ───────────────────────────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void priorWorkFreeEnabled(TestContext helper) {
        setFeature("prior_work_free", true);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setRepairCost(20);
        try {
            helper.assertTrue(sword.getRepairCost() == 0,
                "getRepairCost() should be 0 when prior_work_free enabled (got " + sword.getRepairCost() + ")");
        } finally {
            setFeature("prior_work_free", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void priorWorkFreeDisabled(TestContext helper) {
        setFeature("prior_work_free", false);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setRepairCost(20);
        helper.assertTrue(sword.getRepairCost() == 20,
            "getRepairCost() should be 20 when prior_work_free disabled (got " + sword.getRepairCost() + ")");
        helper.complete();
    }

    // ─── PriorWorkCheaper ────────────────────────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void priorWorkCheaperEnabled(TestContext helper) {
        setFeature("prior_work_cheaper", true);
        ETMixinPlugin.getConfig().set("pw_cost_multiplier", "1.0");
        try {
            int result = getNextAnvilCost(4);
            // formula: round(1.0 * 4 + 1) = 5
            helper.assertTrue(result == 5,
                "getNextCost(4) with multiplier 1.0 should be 5 (got " + result + ")");
        } finally {
            ETMixinPlugin.getConfig().set("pw_cost_multiplier", "1.33");
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void priorWorkCheaperDisabled(TestContext helper) {
        setFeature("prior_work_cheaper", false);
        try {
            int result = getNextAnvilCost(4);
            // vanilla: 2*4+1 = 9
            helper.assertTrue(result == 9,
                "getNextCost(4) should be vanilla 9 when disabled (got " + result + ")");
        } finally {
            setFeature("prior_work_cheaper", true);
        }
        helper.complete();
    }

    // ─── AxesNotTools ────────────────────────────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void axesNotToolsEnabled(TestContext helper) {
        setFeature("axes_not_tools", true);
        ServerWorld world = helper.getWorld();
        ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
        ZombieEntity target  = EntityType.ZOMBIE.create(world);
        ZombieEntity attacker = EntityType.ZOMBIE.create(world);
        axe.getItem().postHit(axe, target, attacker);
        helper.assertTrue(axe.getDamage() == 1,
            "Axe should take 1 durability with axes_not_tools enabled (got " + axe.getDamage() + ")");
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void axesNotToolsDisabled(TestContext helper) {
        setFeature("axes_not_tools", false);
        ServerWorld world = helper.getWorld();
        ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
        ZombieEntity target  = EntityType.ZOMBIE.create(world);
        ZombieEntity attacker = EntityType.ZOMBIE.create(world);
        try {
            axe.getItem().postHit(axe, target, attacker);
            helper.assertTrue(axe.getDamage() == 2,
                "Axe should take 2 durability with axes_not_tools disabled (got " + axe.getDamage() + ")");
        } finally {
            setFeature("axes_not_tools", true);
        }
        helper.complete();
    }

    // ─── NoThornsBacklash ────────────────────────────────────────────
    // @Constant(intValue=2) intercepts ItemStack.damage(2, user, …) — the ARMOR durability hit.
    // At level 7: 0.15*7=1.05 > 1.0 → thorns always fires.

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void noThornsBacklashEnabled(TestContext helper) {
        setFeature("no_thorns_backlash", true);
        ZombieEntity user     = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ZombieEntity attacker = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(1, 2, 0));
        // Equip user with a Thorns VII chestplate so chooseEquipmentWith finds it
        ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
        chest.addEnchantment(Enchantments.THORNS, 7);
        user.equipStack(EquipmentSlot.CHEST, chest);
        Enchantments.THORNS.onUserDamaged(user, attacker, 7);
        // Mixin replaces damage(2,…) with damage(0,…) → armor is undamaged
        helper.assertTrue(user.getEquippedStack(EquipmentSlot.CHEST).getDamage() == 0,
            "Thorns armor should not lose durability when no_thorns_backlash enabled");
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void noThornsBacklashDisabled(TestContext helper) {
        setFeature("no_thorns_backlash", false);
        ZombieEntity user     = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ZombieEntity attacker = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(1, 2, 0));
        ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
        chest.addEnchantment(Enchantments.THORNS, 7);
        user.equipStack(EquipmentSlot.CHEST, chest);
        try {
            Enchantments.THORNS.onUserDamaged(user, attacker, 7);
            // Vanilla damage(2,…) → armor loses 2 durability
            helper.assertTrue(user.getEquippedStack(EquipmentSlot.CHEST).getDamage() == 2,
                "Thorns armor should lose 2 durability when no_thorns_backlash disabled (got "
                + user.getEquippedStack(EquipmentSlot.CHEST).getDamage() + ")");
        } finally {
            setFeature("no_thorns_backlash", true);
        }
        helper.complete();
    }

    // ─── LoyalVoidTridents ───────────────────────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void loyalVoidTridentsEnabled(TestContext helper) {
        setFeature("loyal_void_tridents", true);
        ServerWorld world = helper.getWorld();
        ItemStack tridentStack = new ItemStack(Items.TRIDENT);
        tridentStack.addEnchantment(Enchantments.LOYALTY, 1);
        ZombieEntity owner = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        TridentEntity trident = new TridentEntity(world, owner, tridentStack);
        BlockPos abs = helper.getAbsolutePos(new BlockPos(0, 0, 0));
        trident.setPos(abs.getX(), world.getBottomY() - 5.0, abs.getZ());
        world.spawnEntity(trident);
        trident.tick();
        try {
            Field dealtDamage = TridentEntity.class.getDeclaredField("dealtDamage");
            dealtDamage.setAccessible(true);
            helper.assertTrue((boolean) dealtDamage.get(trident),
                "dealtDamage should be true below world bottom when loyal_void_tridents enabled");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void loyalVoidTridentsDisabled(TestContext helper) {
        setFeature("loyal_void_tridents", false);
        ServerWorld world = helper.getWorld();
        ItemStack tridentStack = new ItemStack(Items.TRIDENT);
        tridentStack.addEnchantment(Enchantments.LOYALTY, 1);
        ZombieEntity owner = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        TridentEntity trident = new TridentEntity(world, owner, tridentStack);
        BlockPos abs = helper.getAbsolutePos(new BlockPos(0, 0, 0));
        trident.setPos(abs.getX(), world.getBottomY() - 5.0, abs.getZ());
        world.spawnEntity(trident);
        try {
            trident.tick();
            Field dealtDamage = TridentEntity.class.getDeclaredField("dealtDamage");
            dealtDamage.setAccessible(true);
            helper.assertFalse((boolean) dealtDamage.get(trident),
                "dealtDamage should stay false when loyal_void_tridents disabled");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            setFeature("loyal_void_tridents", true);
        }
        helper.complete();
    }

    // ─── NoSoulSpeedBacklash ─────────────────────────────────────────
    // @Constant(intValue=1) intercepts ItemStack.damage(1, …) inside addSoulSpeedBoostIfNeeded.
    // The method has a 4% random check before damaging boots. addTemporaryModifier throws if the
    // modifier already exists, so we call removeSoulSpeedBoost() before each attempt.
    // 500 calls gives 1 - 0.96^500 ≈ 100% probability of triggering the damage when disabled.

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void noSoulSpeedBacklashEnabled(TestContext helper) {
        setFeature("no_soul_speed_backlash", true);
        helper.setBlockState(new BlockPos(0, 1, 0), Blocks.SOUL_SAND.getDefaultState());
        ZombieEntity zombie = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack boots = new ItemStack(Items.GOLDEN_BOOTS);
        boots.addEnchantment(Enchantments.SOUL_SPEED, 1);
        zombie.equipStack(EquipmentSlot.FEET, boots);
        zombie.tick(); // land on soul sand so getLandingBlockState() / isOnSoulSpeedBlock() pass
        try {
            Method removeBoost = LivingEntity.class.getDeclaredMethod("removeSoulSpeedBoost");
            removeBoost.setAccessible(true);
            Method addBoost = LivingEntity.class.getDeclaredMethod("addSoulSpeedBoostIfNeeded");
            addBoost.setAccessible(true);
            // With mixin enabled, damage(1,…) → damage(0,…) — boots never lose durability
            for (int i = 0; i < 500; i++) {
                removeBoost.invoke(zombie); // clear existing modifier so addSoulSpeedBoostIfNeeded can run
                addBoost.invoke(zombie);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        helper.assertTrue(zombie.getEquippedStack(EquipmentSlot.FEET).getDamage() == 0,
            "Soul Speed boots should not take damage when no_soul_speed_backlash enabled (got "
            + zombie.getEquippedStack(EquipmentSlot.FEET).getDamage() + ")");
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void noSoulSpeedBacklashDisabled(TestContext helper) {
        setFeature("no_soul_speed_backlash", false);
        helper.setBlockState(new BlockPos(0, 1, 0), Blocks.SOUL_SAND.getDefaultState());
        ZombieEntity zombie = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack boots = new ItemStack(Items.GOLDEN_BOOTS);
        boots.addEnchantment(Enchantments.SOUL_SPEED, 1);
        zombie.equipStack(EquipmentSlot.FEET, boots);
        zombie.tick(); // land on soul sand
        try {
            Method removeBoost = LivingEntity.class.getDeclaredMethod("removeSoulSpeedBoost");
            removeBoost.setAccessible(true);
            Method addBoost = LivingEntity.class.getDeclaredMethod("addSoulSpeedBoostIfNeeded");
            addBoost.setAccessible(true);
            // 500 calls → ~100% chance of hitting the 4% boot-damage RNG at least once
            for (int i = 0; i < 500; i++) {
                removeBoost.invoke(zombie); // clear modifier so next addSoulSpeedBoostIfNeeded can run
                addBoost.invoke(zombie);
                if (zombie.getEquippedStack(EquipmentSlot.FEET).getDamage() > 0) break;
            }
            helper.assertTrue(zombie.getEquippedStack(EquipmentSlot.FEET).getDamage() > 0,
                "Soul Speed boots should take damage when no_soul_speed_backlash disabled (got 0 after 500 calls)");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            setFeature("no_soul_speed_backlash", true);
        }
        helper.complete();
    }

    // ─── BetterMending ───────────────────────────────────────────────
    // Vanilla mending only repairs equipped items; BetterMending scans the whole inventory.

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void betterMendingEnabled(TestContext helper) {
        setFeature("better_mending", true);
        ServerWorld world = helper.getWorld();
        PlayerEntity player = helper.createMockSurvivalPlayer();
        ItemStack mendingSword = new ItemStack(Items.DIAMOND_SWORD);
        mendingSword.addEnchantment(Enchantments.MENDING, 1);
        mendingSword.setDamage(100);
        // Slot 20: a general inventory slot that is neither hand nor armor
        player.getInventory().setStack(20, mendingSword);

        BlockPos pos = helper.getAbsolutePos(new BlockPos(0, 2, 0));
        ExperienceOrbEntity orb = new ExperienceOrbEntity(world, pos.getX(), pos.getY(), pos.getZ(), 50);
        repairPlayerGears(orb, player, 50);

        helper.assertTrue(player.getInventory().getStack(20).getDamage() < 100,
            "BetterMending should repair inventory item (not just equipped items)");
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void betterMendingDisabled(TestContext helper) {
        setFeature("better_mending", false);
        ServerWorld world = helper.getWorld();
        PlayerEntity player = helper.createMockSurvivalPlayer();
        ItemStack mendingSword = new ItemStack(Items.DIAMOND_SWORD);
        mendingSword.addEnchantment(Enchantments.MENDING, 1);
        mendingSword.setDamage(100);
        player.getInventory().setStack(20, mendingSword);

        BlockPos pos = helper.getAbsolutePos(new BlockPos(0, 2, 0));
        ExperienceOrbEntity orb = new ExperienceOrbEntity(world, pos.getX(), pos.getY(), pos.getZ(), 50);
        try {
            repairPlayerGears(orb, player, 50);
            helper.assertTrue(player.getInventory().getStack(20).getDamage() == 100,
                "Vanilla mending should NOT repair non-equipped inventory item when better_mending disabled");
        } finally {
            setFeature("better_mending", true);
        }
        helper.complete();
    }

    // ─── MoreMending ─────────────────────────────────────────────────
    // Vanilla 1.20.3: getMendingRepairCost(amount) = amount / 2 = 50 for amount=100.
    // MoreMending (mendingLevel=0 default): round(100 * clamp(0.6, 0.1, 0.6)) = 60.
    // Tests getMendingRepairCost directly to avoid infinite-loop risk in repairPlayerGears.

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreMendingEnabled(TestContext helper) {
        setFeature("more_mending", true);
        ServerWorld world = helper.getWorld();
        BlockPos pos = helper.getAbsolutePos(new BlockPos(0, 2, 0));
        ExperienceOrbEntity orb = new ExperienceOrbEntity(world, pos.getX(), pos.getY(), pos.getZ(), 0);
        try {
            Method getMRC = ExperienceOrbEntity.class.getDeclaredMethod("getMendingRepairCost", int.class);
            getMRC.setAccessible(true);
            boolean isStatic = java.lang.reflect.Modifier.isStatic(getMRC.getModifiers());
            int cost = (int) getMRC.invoke(isStatic ? null : orb, 100);
            // MoreMending formula (mendingLevel=0 default): round(100 * clamp(0.6, 0.1, 0.6)) = 60
            helper.assertTrue(cost == 60,
                "MoreMending getMendingRepairCost(100) with mendingLevel=0 should be 60 (got " + cost + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreMendingDisabled(TestContext helper) {
        setFeature("more_mending", false);
        ServerWorld world = helper.getWorld();
        BlockPos pos = helper.getAbsolutePos(new BlockPos(0, 2, 0));
        ExperienceOrbEntity orb = new ExperienceOrbEntity(world, pos.getX(), pos.getY(), pos.getZ(), 0);
        try {
            Method getMRC = ExperienceOrbEntity.class.getDeclaredMethod("getMendingRepairCost", int.class);
            getMRC.setAccessible(true);
            boolean isStatic = java.lang.reflect.Modifier.isStatic(getMRC.getModifiers());
            int cost = (int) getMRC.invoke(isStatic ? null : orb, 100);
            // Vanilla 1.20.3: amount / 2 = 50
            helper.assertTrue(cost == 50,
                "Vanilla getMendingRepairCost(100) should return 50 (got " + cost + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            setFeature("more_mending", true);
        }
        helper.complete();
    }

    // ─── MoreMultishot ───────────────────────────────────────────────
    // Multishot II with MoreMultishot: level*2+1 = 5 projectiles loaded vs vanilla 3.

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreMultishotEnabled(TestContext helper) {
        setFeature("more_multishot", true);
        setEnchantCap("multishot", 2);
        ServerPlayerEntity player = helper.createMockCreativeServerPlayerInWorld();
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.addEnchantment(Enchantments.MULTISHOT, 2);
        player.getInventory().insertStack(new ItemStack(Items.ARROW, 64));
        try {
            Method loadProjectiles = CrossbowItem.class.getDeclaredMethod("loadProjectiles", LivingEntity.class, ItemStack.class);
            loadProjectiles.setAccessible(true);
            loadProjectiles.invoke(null, player, crossbow);
            Method getProjectiles = CrossbowItem.class.getDeclaredMethod("getProjectiles", ItemStack.class);
            getProjectiles.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<ItemStack> projectiles = (List<ItemStack>) getProjectiles.invoke(null, crossbow);
            helper.assertTrue(projectiles.size() == 5,
                "Multishot II should load 5 projectiles with more_multishot enabled (got " + projectiles.size() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            setEnchantCap("multishot", -1);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreMultishotDisabled(TestContext helper) {
        setFeature("more_multishot", false);
        setEnchantCap("multishot", 2);
        ServerPlayerEntity player = helper.createMockCreativeServerPlayerInWorld();
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.addEnchantment(Enchantments.MULTISHOT, 2);
        player.getInventory().insertStack(new ItemStack(Items.ARROW, 64));
        try {
            Method loadProjectiles = CrossbowItem.class.getDeclaredMethod("loadProjectiles", LivingEntity.class, ItemStack.class);
            loadProjectiles.setAccessible(true);
            loadProjectiles.invoke(null, player, crossbow);
            Method getProjectiles = CrossbowItem.class.getDeclaredMethod("getProjectiles", ItemStack.class);
            getProjectiles.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<ItemStack> projectiles = (List<ItemStack>) getProjectiles.invoke(null, crossbow);
            helper.assertTrue(projectiles.size() == 3,
                "Multishot (vanilla) should load 3 projectiles when more_multishot disabled (got " + projectiles.size() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            setFeature("more_multishot", true);
            setEnchantCap("multishot", -1);
        }
        helper.complete();
    }

    // ─── MoreFlame ───────────────────────────────────────────────────
    // Flame II: burn time = 2*(2-1)+5 = 7 seconds (140 ticks) vs vanilla 5s (100 ticks).

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreFlameEnabled(TestContext helper) {
        setFeature("more_flame", true);
        setEnchantCap("flame", 2);
        ServerWorld world = helper.getWorld();
        ZombieEntity shooter = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack flameBow = new ItemStack(Items.BOW);
        flameBow.addEnchantment(Enchantments.FLAME, 2);
        shooter.equipStack(EquipmentSlot.MAINHAND, flameBow);

        BlockPos spawnPos = helper.getAbsolutePos(new BlockPos(0, 3, 0));
        ArrowEntity arrow = new ArrowEntity(world, (double) spawnPos.getX(), (double) spawnPos.getY(), (double) spawnPos.getZ(), new ItemStack(Items.ARROW));
        arrow.setOwner(shooter);
        arrow.setOnFireFor(10);
        world.spawnEntity(arrow);

        ZombieEntity target = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        try {
            Method onEntityHit = PersistentProjectileEntity.class.getDeclaredMethod("onEntityHit", EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(arrow, new EntityHitResult(target));
            // Flame II: setOnFireFor(7) = 140 ticks; vanilla Flame I = 100 ticks
            helper.assertTrue(target.getFireTicks() > 100,
                "Flame II should burn target > 5s (got " + target.getFireTicks() + " ticks)");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            setEnchantCap("flame", -1);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreFlameDisabled(TestContext helper) {
        setFeature("more_flame", false);
        setEnchantCap("flame", 2);
        ServerWorld world = helper.getWorld();
        ZombieEntity shooter = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack flameBow = new ItemStack(Items.BOW);
        flameBow.addEnchantment(Enchantments.FLAME, 2);
        shooter.equipStack(EquipmentSlot.MAINHAND, flameBow);

        BlockPos spawnPos = helper.getAbsolutePos(new BlockPos(0, 3, 0));
        ArrowEntity arrow = new ArrowEntity(world, (double) spawnPos.getX(), (double) spawnPos.getY(), (double) spawnPos.getZ(), new ItemStack(Items.ARROW));
        arrow.setOwner(shooter);
        arrow.setOnFireFor(10);
        world.spawnEntity(arrow);

        ZombieEntity target = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        try {
            Method onEntityHit = PersistentProjectileEntity.class.getDeclaredMethod("onEntityHit", EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(arrow, new EntityHitResult(target));
            // Flame II with mixin disabled: vanilla setOnFireFor(5) = 100 ticks (not 140)
            helper.assertTrue(target.getFireTicks() == 100,
                "Flame II with more_flame disabled should burn target 100 ticks (got " + target.getFireTicks() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            setFeature("more_flame", true);
            setEnchantCap("flame", -1);
        }
        helper.complete();
    }
}
