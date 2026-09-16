package me.mrbast.structory.manager;

import me.mrbast.structory.async.StructureParticleScheduler;
import me.mrbast.structory.crafting.option.CraftingOption;
import me.mrbast.structory.util.SchedulerUtil;

public final class StructoryReloadService {
    private StructoryReloadService() {
    }

    public static void reload() {
        StructureParticleScheduler particleScheduler = StructureParticleScheduler.getInstance();
        particleScheduler.stop();
        SchedulerUtil.cancelPlatformTasks();

        CraftingOption crafting = CraftingOption.getInstance();
        crafting.dropAllRecipeItems();
        crafting.clearRuntimeState();

        OptionManager.getInstance().clear();
        particleScheduler.clear();

        StructureInstanceManager.getInstance().clear();
        StructureManager.getInstance().clear();
        RecipeManager.getInstance().clear();
        SavedItemManager.getInstance().clear();

        OptionManager.getInstance().init();
        ConfigManager.getInstance().load();
        particleScheduler.start();
    }
}
