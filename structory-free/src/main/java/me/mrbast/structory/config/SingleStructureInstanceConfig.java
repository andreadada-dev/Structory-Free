package me.mrbast.structory.config;

import me.mrbast.dadaconfig.logic.Config;
import me.mrbast.structory.Structory;
import me.mrbast.structory.structure.StructureInstance;
import me.mrbast.structory.util.AtomicFileUtil;
import me.mrbast.structory.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SingleStructureInstanceConfig extends Config {
    public static final Logger LOGGER = Structory.getPlugin(Structory.class).getLogger();

    private final StructureInstance structureInstance;
    private final File file;

    public SingleStructureInstanceConfig(StructureInstance structureInstance) {
        this.structureInstance = structureInstance;
        this.file = FileUtil.prepare("instances/" + structureInstance.getData().getUUID() + ".yml", false);
    }

    @Override
    public void load() {
    }

    @Override
    public void save() {
        write(StructureInstance.class, "", structureInstance);
        try {
            AtomicFileUtil.writeWithBackup(file, super::save);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING,
                    "Couldn't save structure instance '" + structureInstance.getData().getUUID() + "' config", e);
        }
    }

    public void delete() {
        try {
            AtomicFileUtil.deleteWithBackup(file);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING,
                    "Couldn't delete structure instance '" + structureInstance.getData().getUUID() + "' config", e);
        }
    }
}
