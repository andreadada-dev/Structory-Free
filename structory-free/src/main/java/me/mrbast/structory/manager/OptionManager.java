package me.mrbast.structory.manager;

import me.mrbast.structory.crafting.option.CraftingOption;
import me.mrbast.structory.enums.StructureSpacedKey;
import me.mrbast.structory.option.FireworksOption;
import me.mrbast.structory.option.HasInstanceData;
import me.mrbast.structory.option.NotifyOption;
import me.mrbast.structory.option.Option;
import me.mrbast.structory.option.ParticleOption;
import org.bukkit.NamespacedKey;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class OptionManager {

    public static final OptionManager instance = new OptionManager();

    public static OptionManager getInstance() {
        return instance;
    }

    private final Map<NamespacedKey, Option> options = new HashMap<>();
    private final Map<String, NamespacedKey> stringToKey = new HashMap<>();
    private final Set<HasInstanceData> hasInstanceDataSet = new HashSet<>();

    private OptionManager() {
        registerOption(StructureSpacedKey.OPTION_FIREWORK.getNamespacedKey(), new FireworksOption());
        registerOption(StructureSpacedKey.OPTION_PARTICLE.getNamespacedKey(), ParticleOption.getInstance());
        registerOption(StructureSpacedKey.OPTION_CRAFTING.getNamespacedKey(), CraftingOption.getInstance());
        registerOption(StructureSpacedKey.OPTION_NOTIFY.getNamespacedKey(), NotifyOption.getInstance());
    }

    public void registerOption(NamespacedKey key, Option option) {
        options.put(key, option);
        stringToKey.put(key.getKey(), key);
        ListenerManager.getInstance().subscribe(option);
        if (option instanceof HasInstanceData) {
            hasInstanceDataSet.add((HasInstanceData) option);
        }
    }

    public Optional<Option> getOption(String keyStr) {
        if (keyStr == null) return Optional.empty();
        NamespacedKey key = stringToKey.get(keyStr);
        return key == null ? Optional.empty() : Optional.ofNullable(options.get(key));
    }

    public Set<HasInstanceData> getHasInstanceData() {
        return hasInstanceDataSet;
    }

    public Optional<Option> getOption(NamespacedKey key) {
        return Optional.ofNullable(options.get(key));
    }

    public void init() {
        options.values().forEach(Option::init);
    }

    /** Option definitions are static for the Free edition; runtime caches are cleared by their owners. */
    public void clear() {
    }
}
