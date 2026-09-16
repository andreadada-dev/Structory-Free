package me.mrbast.structory.option;

import me.mrbast.dadaconfig.logic.ConfigSection;
import me.mrbast.structory.enums.Key;
import me.mrbast.structory.structure.Structure;

public interface Option {

    void read(Structure structure, ConfigSection configSection);

    void write(ConfigSection configSection);

    Key getKey();

    /** Called after reading this option for a specific structure. */
    void init(Structure structure);

    /** Called when the option subsystem is initialized. */
    void init();

    /**
     * Called before Structory runtime data is discarded on reload/shutdown.
     * Stateless options do not need to override it.
     */
    default void onDisable() {
    }
}
