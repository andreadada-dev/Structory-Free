package me.mrbast.structory.api;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructoryAddonRegistryTest {

    @Test
    void registrationNormalizesIdAndTracksAddonListener() {
        List<Object> subscribed = new ArrayList<>();
        List<Object> unsubscribed = new ArrayList<>();
        StructoryAddonRegistry registry = new StructoryAddonRegistry(subscribed::add, unsubscribed::add);
        TestAddon addon = new TestAddon("  Example_Addon  ");

        registry.register(addon);

        assertEquals(1, registry.size());
        assertTrue(registry.contains("example_addon"));
        assertEquals(addon, registry.get("EXAMPLE_ADDON").orElseThrow());
        assertEquals(List.of(addon, addon.extraListener), subscribed);

        assertTrue(registry.unregister("Example_Addon"));
        assertEquals(1, addon.disableCalls.get());
        assertEquals(2, unsubscribed.size());
        assertFalse(registry.contains("example_addon"));
    }

    @Test
    void duplicateIdsAreRejectedCaseInsensitively() {
        StructoryAddonRegistry registry = new StructoryAddonRegistry(ignored -> { }, ignored -> { });
        registry.register(new TestAddon("Example"));

        assertThrows(IllegalArgumentException.class, () -> registry.register(new TestAddon("example")));
    }

    @Test
    void failedEnableRollsBackRegistrationAndListeners() {
        List<Object> subscribed = new ArrayList<>();
        List<Object> unsubscribed = new ArrayList<>();
        StructoryAddonRegistry registry = new StructoryAddonRegistry(subscribed::add, unsubscribed::add);
        TestAddon addon = new TestAddon("broken") {
            @Override
            public void onEnable(StructoryAddonContext context) {
                super.onEnable(context);
                throw new IllegalStateException("boom");
            }
        };

        assertThrows(IllegalStateException.class, () -> registry.register(addon));
        assertEquals(0, registry.size());
        assertEquals(1, addon.disableCalls.get());
        assertEquals(subscribed.size(), unsubscribed.size());
    }

    @Test
    void shutdownDisablesInReverseRegistrationOrder() {
        List<String> disabled = new ArrayList<>();
        StructoryAddonRegistry registry = new StructoryAddonRegistry(ignored -> { }, ignored -> { });

        registry.register(new OrderedAddon("first", disabled));
        registry.register(new OrderedAddon("second", disabled));
        registry.register(new OrderedAddon("third", disabled));
        registry.shutdown();

        assertEquals(List.of("third", "second", "first"), disabled);
        assertEquals(0, registry.size());
    }

    @Test
    void snapshotsCannotMutateRegistry() {
        StructoryAddonRegistry registry = new StructoryAddonRegistry(ignored -> { }, ignored -> { });
        registry.register(new TestAddon("example"));

        Map<String, StructoryAddon> snapshot = registry.snapshot();
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        assertThrows(UnsupportedOperationException.class, () -> registry.ids().remove("example"));
        assertEquals(1, registry.size());
    }

    private static class TestAddon implements StructoryAddon {
        private final String id;
        private final Object extraListener = new Object();
        private final AtomicInteger disableCalls = new AtomicInteger();

        private TestAddon(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public void onEnable(StructoryAddonContext context) {
            context.subscribe(extraListener);
            context.subscribe(extraListener);
        }

        @Override
        public void onDisable() {
            disableCalls.incrementAndGet();
        }
    }

    private static final class OrderedAddon implements StructoryAddon {
        private final String id;
        private final List<String> disabled;

        private OrderedAddon(String id, List<String> disabled) {
            this.id = id;
            this.disabled = disabled;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public void onDisable() {
            disabled.add(id);
        }
    }
}
