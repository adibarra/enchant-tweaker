package com.adibarra.enchanttweaker.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.adibarra.enchanttweaker.ETConfigSchema;

/**
 * mutable, client-agnostic state for the paginated modmenu configuration screen
 */
public final class ETConfigScreenModel {

    public static final int PAGE_SIZE = 5;

    private final Map<String, String> originalValues = new LinkedHashMap<>();
    private final Map<String, String> pendingValues = new LinkedHashMap<>();
    private final List<String> categories = List.copyOf(ETConfigSchema.categories());
    private int categoryIndex;
    private int pageIndex;
    private String searchQuery = "";

    public ETConfigScreenModel(Map<String, String> currentValues) {
        Map<String, String> values = currentValues == null ? Map.of() : currentValues;
        for (Map.Entry<String, String> entry : ETConfigSchema.defaults().entrySet()) {
            String value = values.get(entry.getKey());
            if (value == null)
                value = entry.getValue();
            value = normalize(value);
            originalValues.put(entry.getKey(), value);
            pendingValues.put(entry.getKey(), value);
        }
    }

    public List<String> categories() {
        return categories;
    }

    public String currentCategory() {
        return categories.isEmpty() ? "" : categories.get(categoryIndex);
    }

    public int categoryIndex() {
        return categoryIndex;
    }

    public int pageIndex() {
        return pageIndex;
    }

    public int pageCount() {
        int keyCount = filteredKeys().size();
        return Math.max(1, (keyCount + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    public List<String> visibleKeys() {
        List<String> keys = filteredKeys();
        int start = Math.min(pageIndex * PAGE_SIZE, keys.size());
        int end = Math.min(start + PAGE_SIZE, keys.size());
        return List.copyOf(keys.subList(start, end));
    }

    public String searchQuery() {
        return searchQuery;
    }

    public boolean hasSearchQuery() {
        return !searchQuery.isEmpty();
    }

    public void setSearchQuery(String query) {
        searchQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        pageIndex = 0;
    }

    private List<String> filteredKeys() {
        if (!hasSearchQuery())
            return categories.isEmpty() ? List.of() : ETConfigSchema.keysIn(currentCategory());

        List<String> matches = new ArrayList<>();
        for (String key : ETConfigSchema.defaults().keySet()) {
            String description = ETConfigSchema.descriptionOf(key);
            if (key.contains(searchQuery) || key.replace('_', ' ').contains(searchQuery)
                || (description != null && description.toLowerCase(Locale.ROOT).contains(searchQuery))) {
                matches.add(key);
            }
        }
        return matches;
    }

    public void previousCategory() {
        if (categories.isEmpty() || hasSearchQuery())
            return;
        categoryIndex = Math.floorMod(categoryIndex - 1, categories.size());
        pageIndex = 0;
    }

    public void nextCategory() {
        if (categories.isEmpty() || hasSearchQuery())
            return;
        categoryIndex = Math.floorMod(categoryIndex + 1, categories.size());
        pageIndex = 0;
    }

    public void previousPage() {
        pageIndex = Math.max(0, pageIndex - 1);
    }

    public void nextPage() {
        pageIndex = Math.min(pageCount() - 1, pageIndex + 1);
    }

    public String value(String key) {
        return pendingValues.get(key);
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public boolean setValue(String key, String value) {
        if (!pendingValues.containsKey(key) || value == null)
            return false;
        String normalized = normalize(value);
        if (!ETConfigSchema.isValid(key, normalized))
            return false;
        pendingValues.put(key, normalized);
        return true;
    }

    public boolean toggle(String key) {
        if (ETConfigSchema.typeOf(key) != ETConfigSchema.ValueType.BOOLEAN)
            return false;
        return setValue(key, Boolean.toString(!Boolean.parseBoolean(value(key))));
    }

    public void resetCurrentCategory() {
        for (String key : ETConfigSchema.keysIn(currentCategory())) {
            pendingValues.put(key, ETConfigSchema.defaultOf(key));
        }
    }

    public boolean isDirty() {
        for (Map.Entry<String, String> entry : pendingValues.entrySet()) {
            if (!Objects.equals(entry.getValue(), originalValues.get(entry.getKey())))
                return true;
        }
        return false;
    }

    public boolean hasRestartRequiredChanges() {
        return !Objects.equals(pendingValues.get("mod_enabled"), originalValues.get("mod_enabled"));
    }

    public Map<String, String> changes() {
        Map<String, String> changes = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : pendingValues.entrySet()) {
            if (!entry.getValue().equals(originalValues.get(entry.getKey()))) {
                changes.put(entry.getKey(), entry.getValue());
            }
        }
        return changes;
    }
}
