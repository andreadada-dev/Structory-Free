package me.mrbast.structory.config;

import me.mrbast.dadaconfig.logic.Config;
import me.mrbast.structory.manager.StructureManager;
import me.mrbast.structory.structure.Structure;
import org.bukkit.configuration.InvalidConfigurationException;

import java.io.IOException;
import java.util.Optional;

public class StructureConfig extends Config {

    private static final StructureConfig instance;

    static {
        try {
            instance = new StructureConfig();
        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static StructureConfig getInstance() {
        return instance;
    }

    public StructureConfig() throws IOException, InvalidConfigurationException {
        super();
    }

    @Override
    public void load() {
        initDirectory("structures", true, config -> config.getNodes().forEach(node -> {
            Optional<Structure> structure = node.read(Structure.class);
            structure.ifPresent(StructureManager.getInstance()::registerStructure);
        }));
    }
}
