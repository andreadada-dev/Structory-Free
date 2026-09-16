package me.mrbast.structory.manager;

import me.mrbast.structory.config.*;

public class ConfigManager {

    private static final ConfigManager INSTANCE = new ConfigManager();

    public static ConfigManager getInstance() { return INSTANCE; }

    public void load(){
        MainConfig.getInstance().load();
        DirectorySavedItemConfig.getInstance().load();
        RecipeConfig.getInstance().load();
        StructureConfig.getInstance().load();
        DirectoryStructureInstanceConfig.getInstance().load();
        MessageConfig.getInstance().reload();
    }
}
