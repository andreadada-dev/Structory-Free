package me.mrbast.structory.config;

import me.mrbast.dadaconfig.logic.Config;
import me.mrbast.structory.event.LoadStructureInstance;
import me.mrbast.structory.manager.ListenerManager;
import me.mrbast.structory.manager.StructureInstanceManager;
import me.mrbast.structory.structure.StructureInstance;

import java.util.Optional;

public class DirectoryStructureInstanceConfig extends Config {

    public static final DirectoryStructureInstanceConfig INSTANCE = new DirectoryStructureInstanceConfig();
    public static DirectoryStructureInstanceConfig getInstance(){return INSTANCE;}

    @Override
    public void load() {
        getAllSubFiles("instances", file -> {
            if (!file.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".yml")) return;

            Optional<StructureInstance> instance = file.read(StructureInstance.class);
            instance.ifPresent(structureInstance -> {
                StructureInstanceManager.getInstance().register(structureInstance);
                structureInstance.init();
                ListenerManager.getInstance().call(new LoadStructureInstance(structureInstance));
            });
        });
    }
}
