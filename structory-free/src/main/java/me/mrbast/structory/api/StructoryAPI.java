package me.mrbast.structory.api;

import me.mrbast.structory.Structory;
import me.mrbast.structory.event.StructureEvent;
import me.mrbast.structory.manager.ListenerManager;
import me.mrbast.structory.manager.StructureInstanceManager;
import me.mrbast.structory.manager.StructureManager;
import me.mrbast.structory.structure.Structure;
import me.mrbast.structory.structure.StructureInstance;
import me.mrbast.structory.util.SchedulerUtil;
import org.bukkit.NamespacedKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Stable public facade for Structory Free integrations.
 *
 * <p>The API exposes the existing Structory event bus, managed addon lifecycle
 * and read-only snapshots of loaded structures/instances. Mutating runtime
 * state remains owned by Structory managers.</p>
 */
public final class StructoryAPI {

    private static final Set<Object> EXTERNAL_LISTENERS =
            Collections.newSetFromMap(new IdentityHashMap<>());

    private static Structory plugin;
    private static StructoryAddonRegistry addonRegistry;

    private StructoryAPI() {
    }

    /** Called by Structory during plugin enable. */
    public static synchronized void bind(Structory structory) {
        Objects.requireNonNull(structory, "structory");
        if (plugin != null && plugin != structory) {
            throw new IllegalStateException("Structory API is already bound to another plugin instance");
        }
        plugin = structory;
        if (addonRegistry == null) {
            addonRegistry = new StructoryAddonRegistry(
                    ListenerManager.getInstance()::subscribe,
                    ListenerManager.getInstance()::unsubscribe);
        }
    }

    public static synchronized Structory plugin() {
        return requirePlugin();
    }

    /**
     * Registers an unmanaged event listener. Prefer {@link #registerAddon(StructoryAddon)}
     * when the listener has a lifecycle of its own.
     */
    public static synchronized void subscribe(Object listener) {
        requirePlugin();
        Object target = Objects.requireNonNull(listener, "listener");
        if (!EXTERNAL_LISTENERS.add(target)) return;
        try {
            ListenerManager.getInstance().subscribe(target);
        } catch (RuntimeException | Error error) {
            EXTERNAL_LISTENERS.remove(target);
            throw error;
        }
    }

    public static synchronized void unsubscribe(Object listener) {
        if (listener == null) return;
        if (!EXTERNAL_LISTENERS.remove(listener)) return;
        ListenerManager.getInstance().unsubscribe(listener);
    }

    public static void callEvent(StructureEvent event) {
        requirePlugin();
        ListenerManager.getInstance().call(Objects.requireNonNull(event, "event"));
    }

    public static synchronized void registerAddon(StructoryAddon addon) {
        requireRegistry().register(addon);
    }

    public static synchronized boolean unregisterAddon(String addonId) {
        return requireRegistry().unregister(addonId);
    }

    public static synchronized Optional<StructoryAddon> addon(String addonId) {
        return requireRegistry().get(addonId);
    }

    public static synchronized Map<String, StructoryAddon> addons() {
        return requireRegistry().snapshot();
    }

    public static synchronized Set<String> addonIds() {
        return requireRegistry().ids();
    }

    public static Collection<Structure> structures() {
        requirePlugin();
        return Collections.unmodifiableList(new ArrayList<>(
                StructureManager.getInstance().getStructuresFiltered(ignored -> true)));
    }

    public static Optional<Structure> structure(String name) {
        requirePlugin();
        return Optional.ofNullable(StructureManager.getInstance().getStructureByName(name));
    }

    public static Optional<Structure> structure(NamespacedKey key) {
        requirePlugin();
        return Optional.ofNullable(StructureManager.getInstance().getStructureByUUID(key));
    }

    public static Collection<StructureInstance> instances() {
        requirePlugin();
        return Collections.unmodifiableList(new ArrayList<>(
                StructureInstanceManager.getInstance().getFiltered(ignored -> true)));
    }

    public static Optional<StructureInstance> instance(UUID uuid) {
        requirePlugin();
        return StructureInstanceManager.getInstance().get(uuid);
    }

    public static SchedulerUtil.SchedulerSnapshot schedulerSnapshot() {
        requirePlugin();
        return SchedulerUtil.snapshot();
    }

    public static boolean isFolia() {
        requirePlugin();
        return SchedulerUtil.isFolia();
    }

    /** Called by Structory during disable before runtime managers are torn down. */
    public static synchronized void shutdown() {
        Structory currentPlugin = plugin;
        RuntimeException shutdownFailure = null;

        if (addonRegistry != null) {
            try {
                addonRegistry.shutdown();
            } catch (RuntimeException error) {
                shutdownFailure = error;
            } catch (Error error) {
                if (currentPlugin != null) {
                    currentPlugin.getLogger().log(Level.SEVERE, "Structory addon shutdown failed", error);
                }
            }
        }

        for (Object listener : new ArrayList<>(EXTERNAL_LISTENERS)) {
            ListenerManager.getInstance().unsubscribe(listener);
        }
        EXTERNAL_LISTENERS.clear();
        addonRegistry = null;
        plugin = null;

        if (shutdownFailure != null && currentPlugin != null) {
            currentPlugin.getLogger().log(Level.SEVERE, "Structory addon shutdown failed", shutdownFailure);
        }
    }

    private static Structory requirePlugin() {
        Structory current = plugin;
        if (current == null) {
            throw new IllegalStateException("Structory API is not available before plugin enable or after disable");
        }
        return current;
    }

    private static StructoryAddonRegistry requireRegistry() {
        requirePlugin();
        StructoryAddonRegistry current = addonRegistry;
        if (current == null) throw new IllegalStateException("Structory addon registry is not available");
        return current;
    }
}
