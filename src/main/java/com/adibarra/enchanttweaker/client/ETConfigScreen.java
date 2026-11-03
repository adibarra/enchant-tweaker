package com.adibarra.enchanttweaker.client;

import java.util.LinkedHashMap;
import java.util.Map;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

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
    private EditBox search;
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
        boolean readOnly = minecraft.level != null;
        boolean searching = model.hasSearchQuery();

        Button previousCategory = Button.builder(Component.literal("<"), button -> {
            model.previousCategory();
            rebuildWidgets();
        }).bounds(left, 26, 20, 20).build();
        previousCategory.active = !searching && model.categories().size() > 1;
        addRenderableWidget(previousCategory);

        Component categoryLabel = searching
            ? uiText("search_results", "Search Results")
            : categoryName(model.currentCategory());
        Button category = Button.builder(categoryLabel, button -> {
            model.nextCategory();
            rebuildWidgets();
        }).bounds(left + 24, 26, contentWidth - 48, 20).build();
        category.setTooltip(Tooltip.create(uiText("next_category", "Click to select the next category")));
        category.active = !searching && model.categories().size() > 1;
        addRenderableWidget(category);

        Button nextCategory = Button.builder(Component.literal(">"), button -> {
            model.nextCategory();
            rebuildWidgets();
        }).bounds(left + contentWidth - 20, 26, 20, 20).build();
        nextCategory.active = !searching && model.categories().size() > 1;
        addRenderableWidget(nextCategory);

        search = new EditBox(font, left, 50, contentWidth, 20,
            uiText("search.narration", "Search configuration settings"));
        search.setMaxLength(80);
        search.setHint(uiText("search.placeholder", "Search settings..."));
        search.setValue(model.searchQuery());
        search.setResponder(query -> {
            if (query.trim().equalsIgnoreCase(model.searchQuery()))
                return;
            int cursor = search.getCursorPosition();
            model.setSearchQuery(query);
            rebuildWidgets();
            setInitialFocus(search);
            search.moveCursorTo(Math.min(cursor, search.getValue().length()), false);
        });
        addRenderableWidget(search);

        int rowY = 72;
        for (String key : model.visibleKeys()) {
            Button valueButton = Button.builder(valueLabel(key), button -> editValue(key))
                .bounds(left, rowY, contentWidth, 18).tooltip(Tooltip.create(valueTooltip(key))).build();
            valueButton.active = !readOnly;
            addRenderableWidget(valueButton);
            rowY += ROW_HEIGHT;
        }

        int pagerY = height - 46;
        Button previousPage = Button.builder(Component.literal("<"), button -> {
            model.previousPage();
            rebuildWidgets();
        }).bounds(left, pagerY, 50, 20).build();
        previousPage.active = model.pageIndex() > 0;
        addRenderableWidget(previousPage);

        Button nextPage = Button.builder(Component.literal(">"), button -> {
            model.nextPage();
            rebuildWidgets();
        }).bounds(left + contentWidth - 50, pagerY, 50, 20).build();
        nextPage.active = model.pageIndex() + 1 < model.pageCount();
        addRenderableWidget(nextPage);

        int actionY = height - 26;
        int actionWidth = (contentWidth - 8) / 3;
        addRenderableWidget(Button.builder(uiText("cancel", "Cancel"), button -> requestClose())
            .bounds(left, actionY, actionWidth, 20).build());

        Button reset = Button.builder(uiText("reset_category", "Reset Category"), button -> {
            model.resetCurrentCategory();
            rebuildWidgets();
        }).bounds(left + actionWidth + 4, actionY, actionWidth, 20).build();
        reset.active = !readOnly && !searching;
        addRenderableWidget(reset);

        Button save = Button.builder(uiText("save_done", "Save & Done"), button -> save())
            .bounds(left + (actionWidth + 4) * 2, actionY, actionWidth, 20).build();
        save.active = !readOnly && model.isDirty();
        addRenderableWidget(save);
    }

    static int boundedContentWidth(int screenWidth) {
        return Math.max(MIN_CONTENT_WIDTH, Math.min(CONTENT_WIDTH, screenWidth - 40));
    }

    private void editValue(String key) {
        if (ETConfigSchema.typeOf(key) == ETConfigSchema.ValueType.BOOLEAN) {
            model.toggle(key);
            rebuildWidgets();
        } else {
            minecraft.gui.setScreen(new ETConfigValueScreen(this, model, key));
        }
    }

    private Component valueLabel(String key) {
        return uiText("option_value", "%s: %s", optionName(key), ADText.colorValue(model.value(key)));
    }

    private Component valueTooltip(String key) {
        MutableComponent tooltip = Component.empty().append(optionName(key));
        String description = ETConfigSchema.descriptionOf(key);
        if (!description.isEmpty()) {
            tooltip.append(Component.literal("\n"))
                .append(Component.translatableWithFallback(TRANSLATION_PREFIX + "description." + key, description)
                    .withStyle(ChatFormatting.GRAY));
        }
        return tooltip.append(Component.literal("\n"))
            .append(uiText("tooltip.key", "Key: %s", Component.literal(key)).withStyle(ChatFormatting.DARK_GRAY))
            .append(Component.literal("\n"))
            .append(uiText("tooltip.expected", "Expected: %s", ETConfigSchema.expected(key))
                .withStyle(ChatFormatting.DARK_GRAY))
            .append(Component.literal("\n"))
            .append(uiText("tooltip.default", "Default: %s", ETConfigSchema.defaultOf(key))
                .withStyle(ChatFormatting.DARK_GRAY));
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
        minecraft.gui.setScreen(parent);
    }

    private void requestClose() {
        if (!model.isDirty()) {
            minecraft.gui.setScreen(parent);
            return;
        }
        minecraft.gui.setScreen(new ConfirmScreen(discard -> minecraft.gui.setScreen(discard ? parent : this),
            uiText("discard.title", "Discard unsaved changes?"),
            uiText("discard.message", "Your Enchant Tweaker changes have not been saved."),
            uiText("discard.confirm", "Discard"), uiText("discard.cancel", "Keep Editing")));
    }

    @Override
    public void onClose() {
        requestClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(context, mouseX, mouseY, partialTick);
        context.centeredText(font, title, width / 2, 10, 0xFFFFFFFF);
        context.centeredText(font, uiText("page", "Page %s / %s", model.pageIndex() + 1, model.pageCount()), width / 2,
            height - 44, 0xFFA0A0A0);
        if (model.hasSearchQuery() && model.visibleKeys().isEmpty()) {
            context.centeredText(font, uiText("search.empty", "No matching settings"), width / 2, 100, 0xFFA0A0A0);
        }
        if (saveFailed) {
            context.centeredText(font, uiText("save_failed", "Configuration is unavailable; changes were not saved")
                .withStyle(ChatFormatting.RED), width / 2, height - 62, 0xFFFFFFFF);
        } else if (minecraft.level != null) {
            context.centeredText(font,
                uiText("read_only", "Leave the world before editing local settings").withStyle(ChatFormatting.YELLOW),
                width / 2, height - 62, 0xFFFFFFFF);
        } else if (model.hasRestartRequiredChanges()) {
            context.centeredText(font,
                uiText("restart", "Changing Mod Enabled requires a restart").withStyle(ChatFormatting.LIGHT_PURPLE),
                width / 2, height - 62, 0xFFFFFFFF);
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

    static Component optionName(String key) {
        return Component.translatableWithFallback(TRANSLATION_PREFIX + "option." + key, displayName(key));
    }

    private static Component categoryName(String category) {
        return Component.translatableWithFallback(TRANSLATION_PREFIX + "category." + category, displayName(category));
    }

    static MutableComponent uiText(String key, String fallback, Object... arguments) {
        return Component.translatableWithFallback(TRANSLATION_PREFIX + key, fallback, arguments);
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
