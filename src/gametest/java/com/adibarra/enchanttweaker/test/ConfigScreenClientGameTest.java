package com.adibarra.enchanttweaker.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.lwjgl.glfw.GLFW;

import com.adibarra.enchanttweaker.client.ETConfigScreen;

/**
 * drives the config screens with real input: search, value editing, invalid
 * input, and the discard prompt
 */
public class ConfigScreenClientGameTest implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        context.setScreen(() -> new ETConfigScreen(null));
        context.waitForScreen(ETConfigScreen.class);
        context.waitTicks(2);
        context.takeScreenshot("enchanttweaker-config");

        // searching narrows the list down to the numeric option
        click(context, "Search configuration settings");
        context.getInput().typeChars("more_flame_per");
        context.waitTicks(2);
        context.takeScreenshot("enchanttweaker-config-search");
        click(context, "More Flame Per Level");
        context.waitFor(mc -> "ETConfigValueScreen".equals(mc.gui.screen().getClass().getSimpleName()));

        // invalid input must disable apply and stay visible
        context.getInput().typeChars("x");
        context.waitTicks(2);
        boolean applyActive = context.computeOnClient(mc -> widget(mc.gui.screen().children(), "Apply").active);
        if (applyActive)
            throw new AssertionError("Apply should be disabled for an invalid integer");
        context.takeScreenshot("enchanttweaker-config-invalid-value");
        click(context, "Cancel");
        context.waitForScreen(ETConfigScreen.class);

        // toggling a boolean marks the model dirty, so leaving asks for confirmation
        click(context, "Search configuration settings");
        for (int i = 0; i < "more_flame_per".length(); i++) {
            context.getInput().pressKey(GLFW.GLFW_KEY_BACKSPACE);
        }
        context.getInput().typeChars("cheap_names");
        context.waitTicks(2);
        click(context, "Cheap Names");
        context.waitTicks(2);
        click(context, "Cancel");
        context.waitForScreen(ConfirmScreen.class);
        click(context, "Discard");
        context.waitForScreen(TitleScreen.class);
    }

    private static void click(ClientGameTestContext context, String label) {
        double[] center = context.computeOnClient(mc -> {
            AbstractWidget widget = widget(mc.gui.screen().children(), label);
            double scale = (double) mc.getWindow().getScreenWidth() / mc.getWindow().getGuiScaledWidth();
            return new double[]{(widget.getX() + widget.getWidth() / 2.0) * scale,
                    (widget.getY() + widget.getHeight() / 2.0) * scale};
        });
        context.getInput().setCursorPos(center[0], center[1]);
        context.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
        context.waitTick();
    }

    private static AbstractWidget widget(Iterable<? extends GuiEventListener> children, String label) {
        AbstractWidget prefixMatch = null;
        for (GuiEventListener child : children) {
            if (!(child instanceof AbstractWidget widget))
                continue;
            String message = widget.getMessage().getString();
            // the discard prompt's title also starts with its button's label
            if (message.equals(label))
                return widget;
            if (prefixMatch == null && message.startsWith(label))
                prefixMatch = widget;
        }
        if (prefixMatch != null)
            return prefixMatch;
        throw new AssertionError("No widget labelled '" + label + "' on the current screen");
    }
}
