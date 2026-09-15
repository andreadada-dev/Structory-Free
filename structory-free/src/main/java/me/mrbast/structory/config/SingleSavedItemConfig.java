package me.mrbast.structory.config;

import me.mrbast.dadaconfig.logic.Config;
import me.mrbast.structory.Structory;
import me.mrbast.structory.saveditem.SavedItemProvider;
import me.mrbast.structory.util.AtomicFileUtil;
import me.mrbast.structory.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SingleSavedItemConfig extends Config {

    public static final Logger LOGGER = Structory.getPlugin(Structory.class).getLogger();

    private final SavedItemProvider savedItemProvider;
    private final File file;

    public SingleSavedItemConfig(SavedItemProvider savedItemProvider) {
        this.savedItemProvider = savedItemProvider;
        this.file = FileUtil.prepare("customitem/" + savedItemProvider.getKey().getKey() + ".yml", false);
    }

    @Override
    public void load() {
    }

    @Override
    public void save() {
        write(SavedItemProvider.class, "", savedItemProvider);
        try {
            AtomicFileUtil.writeWithBackup(file, super::save);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING,
                    "Couldn't save custom item '" + savedItemProvider.getKey().getKey() + "' config", e);
        }
    }

    public void delete() {
        try {
            AtomicFileUtil.deleteWithBackup(file);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING,
                    "Couldn't delete custom item '" + savedItemProvider.getKey().getKey() + "' config", e);
        }
    }
}
