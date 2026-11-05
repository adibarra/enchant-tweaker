package com.adibarra.enchanttweaker.test;

import java.util.Map;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.MixinEnvironment;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.enchanttweaker.client.ETConfigScreen;
import com.adibarra.enchanttweaker.network.ConfigSyncPayload;

/**
 * covers what the server game tests cannot reach: client mixins, the mod menu
 * entrypoint, and joining singleplayer and dedicated servers
 */
public class ClientFeaturesClientGameTest implements FabricClientGameTest {

    // 39 common configs, one extra target for GrindstoneInputSlotMixin, 3 client
    private static final int EXPECTED_MIXIN_APPLICATIONS = 43;

    @Override
    public void runTest(ClientGameTestContext context) {
        assertEveryMixinApplies(context);
        assertRomanNumerals(context);
        assertModMenuFactory(context);

        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            assertShinyNames(context);
            assertAnvilLabels(context, world.getServer());
        }

        try (TestDedicatedServerContext server = context.worldBuilder().createServer();
            TestDedicatedServerConnection connection = server.connect()) {
            connection.waitForChunksRender();
            assertDedicatedServerJoin(context, server, connection);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }

    private static void assertEveryMixinApplies(ClientGameTestContext context) {
        context.runOnClient(mc -> {
            MixinEnvironment.getCurrentEnvironment().audit();
            check(ETMixinPlugin.getNumMixins() == EXPECTED_MIXIN_APPLICATIONS,
                "Expected " + EXPECTED_MIXIN_APPLICATIONS + " mixin applications, got " + ETMixinPlugin.getNumMixins());
        });
    }

    private static void assertRomanNumerals(ClientGameTestContext context) {
        context.runOnClient(mc -> {
            Map<String, String> original = ETTestHelper.snapshotConfig("roman_numerals");
            try {
                Language language = Language.getInstance();
                ETTestHelper.setFeature("roman_numerals", true);
                check("XIV".equals(language.getOrDefault("enchantment.level.14", "fallback")),
                    "Level 14 should translate to XIV");
                check(language.has("enchantment.level.14"), "Level 14 should report a translation");
                ETTestHelper.setFeature("roman_numerals", false);
                check("fallback".equals(language.getOrDefault("enchantment.level.14", "fallback")),
                    "Roman numerals off should leave level 14 untranslated");
            } finally {
                ETTestHelper.restoreConfig(original);
            }
        });
    }

    private static void assertModMenuFactory(ClientGameTestContext context) {
        context.runOnClient(mc -> {
            check(FabricLoader.getInstance().isModLoaded("modmenu"), "Mod Menu should be present at runtime");
            // mod menu is not on the test compile classpath
            Object entrypoint = FabricLoader.getInstance().getEntrypoints("modmenu", Object.class).stream()
                .filter(entry -> entry.getClass().getSimpleName().equals("ETModMenuIntegration")).findFirst()
                .orElseThrow(() -> new AssertionError("ETModMenuIntegration entrypoint missing"));
            Object screen;
            try {
                Object factory = entrypoint.getClass().getMethod("getModConfigScreenFactory").invoke(entrypoint);
                screen = Class.forName("com.terraformersmc.modmenu.api.ConfigScreenFactory")
                    .getMethod("create", Screen.class).invoke(factory, (Screen) null);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError("Mod Menu factory failed", e);
            }
            check(screen instanceof ETConfigScreen, "Mod Menu factory should create ETConfigScreen");
        });
    }

    private static void assertShinyNames(ClientGameTestContext context) {
        context.runOnClient(mc -> {
            Holder<Enchantment> sharpness = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SHARPNESS);
            TextColor yellow = TextColor.fromLegacyFormat(ChatFormatting.YELLOW);
            Map<String, String> original = ETTestHelper.snapshotConfig("shiny_name");
            try {
                ETTestHelper.setFeature("shiny_name", true);
                check(yellow.equals(Enchantment.getFullname(sharpness, 5).getStyle().getColor()),
                    "Sharpness V should be shiny");
                check(!yellow.equals(Enchantment.getFullname(sharpness, 4).getStyle().getColor()),
                    "Sharpness IV should not be shiny");
                ETTestHelper.setFeature("shiny_name", false);
                check(!yellow.equals(Enchantment.getFullname(sharpness, 5).getStyle().getColor()),
                    "Shiny names off should leave Sharpness V unstyled");
            } finally {
                ETTestHelper.restoreConfig(original);
            }
        });
    }

    private static void assertAnvilLabels(ClientGameTestContext context, TestServerContext server) {
        Map<String, String> original = ETTestHelper.snapshotConfig("not_too_expensive", "nte_max_cost");
        try {
            server.runOnServer(minecraftServer -> {
                ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
                player.setGameMode(GameType.SURVIVAL);
                player.openMenu(new SimpleMenuProvider(
                    (id, inventory, opener) -> new AnvilMenu(id, inventory, ContainerLevelAccess.NULL),
                    Component.translatable("container.repair")));
            });
            context.waitForScreen(AnvilScreen.class);

            server.runOnServer(minecraftServer -> {
                ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
                ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
                sword.set(DataComponents.REPAIR_COST, 60);
                ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
                book.enchant(player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.SHARPNESS), 1);
                AnvilMenu menu = (AnvilMenu) player.containerMenu;
                ETTestHelper.setFeature("not_too_expensive", false);
                menu.getSlot(AnvilMenu.INPUT_SLOT).set(sword);
                menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(book);
                menu.createResult();
            });
            // vanilla threshold: the server clears the result and the client shows the
            // label
            context.waitFor(mc -> mc.player.containerMenu instanceof AnvilMenu menu && menu.getCost() >= 40
                && !menu.getSlot(AnvilMenu.RESULT_SLOT).hasItem());
            context.waitTicks(2);
            context.takeScreenshot("enchanttweaker-anvil-too-expensive");

            server.runOnServer(minecraftServer -> {
                ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
                ETTestHelper.setFeature("not_too_expensive", true);
                ETTestHelper.setConfigValue("nte_max_cost", "1000");
                ((AnvilMenu) player.containerMenu).createResult();
            });
            // raised threshold: the result comes back and the label shows the cost instead
            context.waitFor(mc -> mc.player.containerMenu instanceof AnvilMenu menu && menu.getCost() >= 40
                && menu.getSlot(AnvilMenu.RESULT_SLOT).hasItem());
            context.waitTicks(2);
            context.takeScreenshot("enchanttweaker-anvil-not-too-expensive");
        } finally {
            ETTestHelper.restoreConfig(original);
        }
        context.runOnClient(mc -> mc.player.closeContainer());
        context.waitTicks(2);
    }

    private static void assertDedicatedServerJoin(ClientGameTestContext context, TestDedicatedServerContext server,
        TestDedicatedServerConnection connection) {
        context.runOnClient(mc -> check(ClientPlayNetworking.getReceived().contains(ConfigSyncPayload.ID.id()),
            "Client should register the config sync channel"));
        server.runOnServer(minecraftServer -> {
            ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
            check(ServerPlayNetworking.canSend(player, ConfigSyncPayload.ID),
                "Dedicated server should see the client's config sync channel");
        });
        server.runCommand("et diagnose");
        server.runCommand("et reload");
        connection.waitForClientboundPackets();
        context.waitTicks(5);
        context.takeScreenshot("enchanttweaker-dedicated-server");
    }
}
