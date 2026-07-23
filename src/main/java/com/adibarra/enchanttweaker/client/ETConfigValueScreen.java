package com.adibarra.enchanttweaker.client;

import java.util.Locale;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import com.adibarra.enchanttweaker.ETConfigSchema;
import com.adibarra.enchanttweaker.config.ETConfigScreenModel;

@Environment(EnvType.CLIENT)
final class ETConfigValueScreen extends Screen {

    private final ETConfigScreen parent;
    private final ETConfigScreenModel model;
    private final String key;
    private TextFieldWidget input;
    private ButtonWidget applyButton;
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

        input = new TextFieldWidget(textRenderer, left, 64, fieldWidth, 20,
            ETConfigScreen.uiText("edit.narration", "Edit %s", ETConfigScreen.optionName(key)));
        input.setMaxLength(ETConfigScreen.MAX_VALUE_LENGTH);
        input.setText(pendingText == null ? model.value(key) : pendingText);
        addDrawableChild(input);

        int buttonWidth = Math.max(1, (fieldWidth - 8) / 3);
        addDrawableChild(ButtonWidget.builder(ETConfigScreen.uiText("cancel", "Cancel"), button -> close())
            .dimensions(left, 100, buttonWidth, 20).build());
        addDrawableChild(ButtonWidget
            .builder(ETConfigScreen.uiText("default", "Default"),
                button -> input.setText(ETConfigSchema.defaultOf(key)))
            .dimensions(left + buttonWidth + 4, 100, buttonWidth, 20).build());
        applyButton = addDrawableChild(ButtonWidget.builder(ETConfigScreen.uiText("apply", "Apply"), button -> apply())
            .dimensions(left + (buttonWidth + 4) * 2, 100, buttonWidth, 20).build());
        input.setChangedListener(value -> {
            pendingText = value;
            updateValidity();
        });
        updateValidity();
        setInitialFocus(input);
    }

    private void updateValidity() {
        applyButton.active = ETConfigSchema.isValid(key, normalizedInput());
        input.setEditableColor(applyButton.active ? TextFieldWidget.DEFAULT_EDITABLE_COLOR : 0xFF5555);
    }

    private String normalizedInput() {
        return input.getText().trim().toLowerCase(Locale.ROOT);
    }

    private void apply() {
        if (model.setValue(key, normalizedInput())) {
            client.setScreen(parent);
        }
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 20, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(key).formatted(Formatting.GRAY), width / 2, 38,
            0xFFFFFF);
        context.drawCenteredTextWithShadow(
            textRenderer, ETConfigScreen.uiText("edit.expected_default", "Expected: %s  Default: %s",
                ETConfigSchema.expected(key), ETConfigSchema.defaultOf(key)).formatted(Formatting.DARK_GRAY),
            width / 2, 126, 0xFFFFFF);
        if (!ETConfigSchema.isValid(key, normalizedInput())) {
            context.drawCenteredTextWithShadow(textRenderer, ETConfigScreen
                .uiText("edit.invalid", "Enter %s", ETConfigSchema.expected(key)).formatted(Formatting.RED), width / 2,
                144, 0xFFFFFF);
        }
    }
}
