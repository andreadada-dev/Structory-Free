package me.mrbast.structory.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Deterministic lifecycle registry for Structory addons.
 *
 * <p>The registry is intentionally independent from Bukkit and from the concrete
 * Structory module. Free and Premium can therefore expose the same addon
 * lifecycle contract while supplying their own event bus implementation.</p>
 */
public final class StructoryAddonRegistry {

    private final Map<String, Registration> registrations = new LinkedHashMap<>();
    private final Consumer<Object> subscriber;
    private final Consumer<Object> unsubscriber;

    public StructoryAddonRegistry(Consumer<Object> subscriber, Consumer<Object> unsubscriber) {
        this.subscriber = Objects.requireNonNull(subscriber, "subscriber");
        this.unsubscriber = Objects.requireNonNull(unsubscriber, "unsubscriber");
    }

    public synchronized void register(StructoryAddon addon) {
        Objects.requireNonNull(addon, "addon");
        String id = normalizeId(addon.id());
        if (registrations.containsKey(id)) {
            throw new IllegalArgumentException("A Structory addon with id '" + id + "' is already registered");
        }

        Registration registration = new Registration(id, addon);
        registrations.put(id, registration);
        try {
            registration.context.subscribe(addon);
            addon.onEnable(registration.context);
        } catch (RuntimeException | Error error) {
            registrations.remove(id);
            registration.context.deactivateAndUnsubscribeAll();
            try {
                addon.onDisable();
            } catch (RuntimeException | Error cleanupError) {
                error.addSuppressed(cleanupError);
            }
            throw error;
        }
    }

    public synchronized boolean unregister(String addonId) {
        String id = normalizeId(addonId);
        Registration registration = registrations.remove(id);
        if (registration == null) return false;

        RuntimeException runtimeFailure = null;
        Error errorFailure = null;
        try {
            registration.addon.onDisable();
        } catch (RuntimeException error) {
            runtimeFailure = error;
        } catch (Error error) {
            errorFailure = error;
        } finally {
            registration.context.deactivateAndUnsubscribeAll();
        }

        if (runtimeFailure != null) throw runtimeFailure;
        if (errorFailure != null) throw errorFailure;
        return true;
    }

    /**
     * Disables addons in reverse registration order and always removes their
     * listeners. All addons are given a chance to shut down before a failure is
     * propagated.
     */
    public synchronized void shutdown() {
        List<String> ids = new ArrayList<>(registrations.keySet());
        Collections.reverse(ids);

        Throwable firstFailure = null;
        for (String id : ids) {
            try {
                unregister(id);
            } catch (RuntimeException | Error error) {
                if (firstFailure == null) firstFailure = error;
                else firstFailure.addSuppressed(error);
            }
        }

        registrations.clear();
        if (firstFailure instanceof RuntimeException) throw (RuntimeException) firstFailure;
        if (firstFailure instanceof Error) throw (Error) firstFailure;
    }

    public synchronized boolean contains(String addonId) {
        return registrations.containsKey(normalizeId(addonId));
    }

    public synchronized Optional<StructoryAddon> get(String addonId) {
        Registration registration = registrations.get(normalizeId(addonId));
        return registration == null ? Optional.empty() : Optional.of(registration.addon);
    }

    public synchronized Map<String, StructoryAddon> snapshot() {
        Map<String, StructoryAddon> snapshot = new LinkedHashMap<>();
        registrations.forEach((id, registration) -> snapshot.put(id, registration.addon));
        return Collections.unmodifiableMap(snapshot);
    }

    public synchronized Set<String> ids() {
        return Collections.unmodifiableSet(snapshot().keySet());
    }

    public synchronized int size() {
        return registrations.size();
    }

    private static String normalizeId(String id) {
        String normalized = Objects.requireNonNull(id, "addon id").trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) throw new IllegalArgumentException("addon id cannot be blank");
        return normalized;
    }

    private final class Registration {
        private final StructoryAddon addon;
        private final TrackingContext context;

        private Registration(String id, StructoryAddon addon) {
            this.addon = addon;
            this.context = new TrackingContext(id);
        }
    }

    private final class TrackingContext implements StructoryAddonContext {
        private final String addonId;
        private final Set<Object> listeners = Collections.newSetFromMap(new IdentityHashMap<>());
        private boolean active = true;

        private TrackingContext(String addonId) {
            this.addonId = addonId;
        }

        @Override
        public String addonId() {
            return addonId;
        }

        @Override
        public void subscribe(Object listener) {
            Object target = Objects.requireNonNull(listener, "listener");
            synchronized (StructoryAddonRegistry.this) {
                ensureActive();
                if (!listeners.add(target)) return;
                try {
                    subscriber.accept(target);
                } catch (RuntimeException | Error error) {
                    listeners.remove(target);
                    throw error;
                }
            }
        }

        @Override
        public void unsubscribe(Object listener) {
            if (listener == null) return;
            synchronized (StructoryAddonRegistry.this) {
                if (!active || !listeners.remove(listener)) return;
                unsubscriber.accept(listener);
            }
        }

        private void ensureActive() {
            if (!active) {
                throw new IllegalStateException("Structory addon context '" + addonId + "' is no longer active");
            }
        }

        private void deactivateAndUnsubscribeAll() {
            active = false;
            List<Object> snapshot = new ArrayList<>(listeners);
            listeners.clear();
            for (Object listener : snapshot) {
                unsubscriber.accept(listener);
            }
        }
    }
}
