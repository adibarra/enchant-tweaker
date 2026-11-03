package com.adibarra.enchanttweaker.client;

import java.util.Locale;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.adibarra.enchanttweaker.ETConfigSchema;
import com.adibarra.enchanttweaker.config.ETConfigScreenModel;

@Environment(EnvType.CLIENT)
final class ETConfigValueScreen extends Screen {

    private final ETConfigScreen parent;
    private final ETConfigScreenModel model;
    private final String key;
    private EditBox input;
    private Button applyButton;
    private String pendingText;

    ETConfigValueScreen(ETConfigScreen parent, ETConfigScreenModel model, String key) {
        super(ETConfigScreen.uiText("edit.title", "Edit %s", ETConfigScreen.optionName(key)));
        this.parent = parent;
        this.model = model;
        this.key = key;
    }

    @Override
    protected void init() {
        int fieldWidth = ETConfigScreen.boundedContentWidth(width);
        int left = (width - fieldWidth) / 2;

        input = new EditBox(font, left, 64, fieldWidth, 20,
            ETConfigScreen.uiText("edit.narration", "Edit %s", ETConfigScreen.optionName(key)));
        input.setMaxLength(ETConfigScreen.MAX_VALUE_LENGTH);
        input.setValue(pendingText == null ? model.value(key) : pendingText);
        addRenderableWidget(input);

        int buttonWidth = Math.max(1, (fieldWidth - 8) / 3);
        addRenderableWidget(Button.builder(ETConfigScreen.uiText("cancel", "Cancel"), button -> onClose())
            .bounds(left, 100, buttonWidth, 20).build());
        addRenderableWidget(Button
            .builder(ETConfigScreen.uiText("default", "Default"),
                button -> input.setValue(ETConfigSchema.defaultOf(key)))
            .bounds(left + buttonWidth + 4, 100, buttonWidth, 20).build());
        applyButton = addRenderableWidget(Button.builder(ETConfigScreen.uiText("apply", "Apply"), button -> apply())
            .bounds(left + (buttonWidth + 4) * 2, 100, buttonWidth, 20).build());
        input.setResponder(value -> {
            pendingText = value;
            updateValidity();
        });
        updateValidity();
        setInitialFocus(input);
    }

    private void updateValidity() {
        applyButton.active = ETConfigSchema.isValid(key, normalizedInput());
        input.setTextColor(applyButton.active ? EditBox.DEFAULT_TEXT_COLOR : 0xFF5555);
    }

    private String normalizedInput() {
        return input.getValue().trim().toLowerCase(Locale.ROOT);
    }

    private void apply() {
        if (model.setValue(key, normalizedInput())) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredString(font, title, width / 2, 20, 0xFFFFFF);
        context.drawCenteredString(font, Component.literal(key).withStyle(ChatFormatting.GRAY), width / 2, 38,
            0xFFFFFF);
        context.drawCenteredString(
            font, ETConfigScreen.uiText("edit.expected_default", "Expected: %s  Default: %s",
                ETConfigSchema.expected(key), ETConfigSchema.defaultOf(key)).withStyle(ChatFormatting.DARK_GRAY),
            width / 2, 126, 0xFFFFFF);
        if (!ETConfigSchema.isValid(key, normalizedInput())) {
            context.drawCenteredString(font, ETConfigScreen
                .uiText("edit.invalid", "Enter %s", ETConfigSchema.expected(key)).withStyle(ChatFormatting.RED),
                width / 2, 144, 0xFFFFFF);
        }
    }
}
