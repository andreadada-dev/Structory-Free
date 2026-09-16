package me.mrbast.structory.manager;

import me.mrbast.structory.async.StructureParticleScheduler;
import me.mrbast.structory.crafting.option.CraftingOption;

public final class StructoryReloadService {
    private StructoryReloadService() {
    }

    public static void reload() {
        CraftingOption crafting = CraftingOption.getInstance();
        crafting.dropAllRecipeItems();
        crafting.clearRuntimeState();
        StructureParticleScheduler.getInstance().clear();

        StructureInstanceManager.getInstance().clear();
        StructureManager.getInstance().clear();
        RecipeManager.getInstance().clear();
        SavedItemManager.getInstance().clear();

        ConfigManager.getInstance().load();
    }
}
