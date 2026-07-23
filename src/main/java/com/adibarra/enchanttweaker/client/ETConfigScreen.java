package com.adibarra.enchanttweaker.client;

import java.util.LinkedHashMap;
import java.util.Map;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import com.adibarra.enchanttweaker.ETConfigSchema;
import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.enchanttweaker.config.ETConfigScreenModel;
import com.adibarra.utils.ADConfig;
import com.adibarra.utils.ADText;

@Environment(EnvType.CLIENT)
public final class ETConfigScreen extends Screen {

    private static final String TRANSLATION_PREFIX = "config.enchanttweaker.";
    private static final int CONTENT_WIDTH = 360;
    private static final int MIN_CONTENT_WIDTH = 49;
    static final int MAX_VALUE_LENGTH = 4096;
    private static final int ROW_HEIGHT = 20;

    private final Screen parent;
    private final ETConfigScreenModel model;
    private TextFieldWidget search;
    private boolean saveFailed;

    public ETConfigScreen(Screen parent) {
        this(parent, new ETConfigScreenModel(currentValues()));
    }

    ETConfigScreen(Screen parent, ETConfigScreenModel model) {
        super(uiText("title", "Enchant Tweaker Configuration"));
        this.parent = parent;
        this.model = model;
    }

    @Override
    protected void init() {
        int contentWidth = boundedContentWidth(width);
        int left = (width - contentWidth) / 2;
        boolean readOnly = client.world != null;
        boolean searching = model.hasSearchQuery();

        ButtonWidget previousCategory = ButtonWidget.builder(Text.literal("<"), button -> {
            model.previousCategory();
            clearAndInit();
        }).dimensions(left, 26, 20, 20).build();
        previousCategory.active = !searching && model.categories().size() > 1;
        addDrawableChild(previousCategory);

        Text categoryLabel = searching
            ? uiText("search_results", "Search Results")
            : categoryName(model.currentCategory());
        ButtonWidget category = ButtonWidget.builder(categoryLabel, button -> {
            model.nextCategory();
            clearAndInit();
        }).dimensions(left + 24, 26, contentWidth - 48, 20).build();
        category.setTooltip(Tooltip.of(uiText("next_category", "Click to select the next category")));
        category.active = !searching && model.categories().size() > 1;
        addDrawableChild(category);

        ButtonWidget nextCategory = ButtonWidget.builder(Text.literal(">"), button -> {
            model.nextCategory();
            clearAndInit();
        }).dimensions(left + contentWidth - 20, 26, 20, 20).build();
        nextCategory.active = !searching && model.categories().size() > 1;
        addDrawableChild(nextCategory);

        search = new TextFieldWidget(textRenderer, left, 50, contentWidth, 20,
            uiText("search.narration", "Search configuration settings"));
        search.setMaxLength(80);
        search.setPlaceholder(uiText("search.placeholder", "Search settings..."));
        search.setText(model.searchQuery());
        search.setChangedListener(query -> {
            if (query.trim().equalsIgnoreCase(model.searchQuery()))
                return;
            int cursor = search.getCursor();
            model.setSearchQuery(query);
            clearAndInit();
            setInitialFocus(search);
            search.setCursor(Math.min(cursor, search.getText().length()), false);
        });
        addDrawableChild(search);

        int rowY = 72;
        for (String key : model.visibleKeys()) {
            ButtonWidget valueButton = ButtonWidget.builder(valueLabel(key), button -> editValue(key))
                .dimensions(left, rowY, contentWidth, 18).tooltip(Tooltip.of(valueTooltip(key))).build();
            valueButton.active = !readOnly;
            addDrawableChild(valueButton);
            rowY += ROW_HEIGHT;
        }

        int pagerY = height - 46;
        ButtonWidget previousPage = ButtonWidget.builder(Text.literal("<"), button -> {
            model.previousPage();
            clearAndInit();
        }).dimensions(left, pagerY, 50, 20).build();
        previousPage.active = model.pageIndex() > 0;
        addDrawableChild(previousPage);

        ButtonWidget nextPage = ButtonWidget.builder(Text.literal(">"), button -> {
            model.nextPage();
            clearAndInit();
        }).dimensions(left + contentWidth - 50, pagerY, 50, 20).build();
        nextPage.active = model.pageIndex() + 1 < model.pageCount();
        addDrawableChild(nextPage);

        int actionY = height - 26;
        int actionWidth = (contentWidth - 8) / 3;
        addDrawableChild(ButtonWidget.builder(uiText("cancel", "Cancel"), button -> requestClose())
            .dimensions(left, actionY, actionWidth, 20).build());

        ButtonWidget reset = ButtonWidget.builder(uiText("reset_category", "Reset Category"), button -> {
            model.resetCurrentCategory();
            clearAndInit();
        }).dimensions(left + actionWidth + 4, actionY, actionWidth, 20).build();
        reset.active = !readOnly && !searching;
        addDrawableChild(reset);

        ButtonWidget save = ButtonWidget.builder(uiText("save_done", "Save & Done"), button -> save())
            .dimensions(left + (actionWidth + 4) * 2, actionY, actionWidth, 20).build();
        save.active = !readOnly && model.isDirty();
        addDrawableChild(save);
    }

    static int boundedContentWidth(int screenWidth) {
        return Math.max(MIN_CONTENT_WIDTH, Math.min(CONTENT_WIDTH, screenWidth - 40));
    }

    private void editValue(String key) {
        if (ETConfigSchema.typeOf(key) == ETConfigSchema.ValueType.BOOLEAN) {
            model.toggle(key);
            clearAndInit();
        } else {
            client.setScreen(new ETConfigValueScreen(this, model, key));
        }
    }

    private Text valueLabel(String key) {
        return uiText("option_value", "%s: %s", optionName(key), ADText.colorValue(model.value(key)));
    }

    private Text valueTooltip(String key) {
        MutableText tooltip = Text.empty().append(optionName(key));
        String description = ETConfigSchema.descriptionOf(key);
        if (!description.isEmpty()) {
            tooltip.append(Text.literal("\n"))
                .append(Text.translatableWithFallback(TRANSLATION_PREFIX + "description." + key, description)
                    .formatted(Formatting.GRAY));
        }
        return tooltip.append(Text.literal("\n"))
            .append(uiText("tooltip.key", "Key: %s", Text.literal(key)).formatted(Formatting.DARK_GRAY))
            .append(Text.literal("\n"))
            .append(uiText("tooltip.expected", "Expected: %s", ETConfigSchema.expected(key))
                .formatted(Formatting.DARK_GRAY))
            .append(Text.literal("\n")).append(uiText("tooltip.default", "Default: %s", ETConfigSchema.defaultOf(key))
                .formatted(Formatting.DARK_GRAY));
    }

    private void save() {
        ADConfig config = ETMixinPlugin.getConfig();
        if (config == null) {
            ETMixinPlugin.reloadConfig();
            config = ETMixinPlugin.getConfig();
        }
        if (config == null) {
            saveFailed = true;
            return;
        }
        config.setAllAndPersist(model.changes());
        ETMixinPlugin.clearCaches();
        client.setScreen(parent);
    }

    private void requestClose() {
        if (!model.isDirty()) {
            client.setScreen(parent);
            return;
        }
        client.setScreen(new ConfirmScreen(discard -> client.setScreen(discard ? parent : this),
            uiText("discard.title", "Discard unsaved changes?"),
            uiText("discard.message", "Your Enchant Tweaker changes have not been saved."),
            uiText("discard.confirm", "Discard"), uiText("discard.cancel", "Keep Editing")));
    }

    @Override
    public void close() {
        requestClose();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
            uiText("page", "Page %s / %s", model.pageIndex() + 1, model.pageCount()), width / 2, height - 44, 0xA0A0A0);
        if (model.hasSearchQuery() && model.visibleKeys().isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, uiText("search.empty", "No matching settings"), width / 2,
                100, 0xA0A0A0);
        }
        if (saveFailed) {
            context.drawCenteredTextWithShadow(textRenderer,
                uiText("save_failed", "Configuration is unavailable; changes were not saved").formatted(Formatting.RED),
                width / 2, height - 62, 0xFFFFFF);
        } else if (client.world != null) {
            context.drawCenteredTextWithShadow(textRenderer,
                uiText("read_only", "Leave the world before editing local settings").formatted(Formatting.YELLOW),
                width / 2, height - 62, 0xFFFFFF);
        } else if (model.hasRestartRequiredChanges()) {
            context.drawCenteredTextWithShadow(textRenderer,
                uiText("restart", "Changing Mod Enabled requires a restart").formatted(Formatting.LIGHT_PURPLE),
                width / 2, height - 62, 0xFFFFFF);
        }
    }

    private static Map<String, String> currentValues() {
        Map<String, String> values = new LinkedHashMap<>();
        ADConfig config = ETMixinPlugin.getConfig();
        for (Map.Entry<String, String> entry : ETConfigSchema.defaults().entrySet()) {
            String value = config == null ? entry.getValue() : config.getOrDefault(entry.getKey(), entry.getValue());
            values.put(entry.getKey(), value);
        }
        return values;
    }

    static Text optionName(String key) {
        return Text.translatableWithFallback(TRANSLATION_PREFIX + "option." + key, displayName(key));
    }

    private static Text categoryName(String category) {
        return Text.translatableWithFallback(TRANSLATION_PREFIX + "category." + category, displayName(category));
    }

    static MutableText uiText(String key, String fallback, Object... arguments) {
        return Text.translatableWithFallback(TRANSLATION_PREFIX + key, fallback, arguments);
    }

    private static String displayName(String key) {
        StringBuilder result = new StringBuilder();
        for (String word : key.split("_")) {
            if (!result.isEmpty())
                result.append(' ');
            if (!word.isEmpty())
                result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }
}
