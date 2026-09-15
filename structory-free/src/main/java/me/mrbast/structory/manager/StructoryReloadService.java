package me.mrbast.structory.manager;

import me.mrbast.structory.crafting.option.CraftingOption;

public final class StructoryReloadService {
    private StructoryReloadService() {
    }

    public static void reload() {
        CraftingOption.getInstance().dropAllRecipeItems();
        StructureInstanceManager.getInstance().clear();
        StructureManager.getInstance().clear();
        RecipeManager.getInstance().clear();
        SavedItemManager.getInstance().clear();
        ConfigManager.getInstance().load();
    }
}
