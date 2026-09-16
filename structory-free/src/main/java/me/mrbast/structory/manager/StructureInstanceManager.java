package me.mrbast.structory.manager;

import me.mrbast.structory.config.SingleStructureInstanceConfig;
import me.mrbast.structory.structure.StructureInstance;
import me.mrbast.structory.structure.grief.StructureProtection;
import me.mrbast.structory.util.SchedulerUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class StructureInstanceManager {

    private static final StructureInstanceManager instance = new StructureInstanceManager();
    public static StructureInstanceManager getInstance() { return instance; }

    private final Map<UUID, StructureInstance> structureInstances = new ConcurrentHashMap<>();

    private StructureInstanceManager() {}

    public void register(StructureInstance instance) {
        structureInstances.put(instance.getData().getUUID(), instance);
    }

    public void clear(){
        structureInstances.clear();
    }

    public void delete(StructureInstance instance) {
        structureInstances.remove(instance.getData().getUUID());
        StructureProtection.getInstance().disable(instance);
        SchedulerUtil.async(() -> new SingleStructureInstanceConfig(instance).delete());
    }

    public Collection<StructureInstance> getFiltered(Predicate<StructureInstance> filter) {
        return structureInstances.values().stream().filter(filter).collect(Collectors.toSet());
    }

    public Optional<StructureInstance> get(UUID uuid) {
        return Optional.ofNullable(structureInstances.get(uuid));
    }
}
