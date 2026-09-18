package me.mrbast.structory.manager;

import me.mrbast.dadaconfig.logic.ConfigSection;
import me.mrbast.structory.enums.Key;
import me.mrbast.structory.event.Listener;
import me.mrbast.structory.event.StructureEvent;
import me.mrbast.structory.event.StructureEventHandler;
import me.mrbast.structory.option.Option;
import me.mrbast.structory.structure.Structure;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListenerManagerTest {

    private final ListenerManager manager = ListenerManager.getInstance();

    @AfterEach
    void cleanUp() {
        manager.clear();
    }

    @Test
    void optionSupportsDirectAndNestedHandlersWithoutDuplicates() {
        TestOption option = new TestOption();
        manager.subscribe(option);

        manager.call(new TestEvent());

        assertEquals(1, option.directCalls.get());
        assertEquals(1, option.nestedCalls.get());
    }

    @Test
    void arbitraryObjectCanSubscribeAndUnsubscribe() {
        PlainListener listener = new PlainListener();

        manager.subscribe((Object) listener);
        manager.call(new TestEvent());
        manager.unsubscribe(listener);
        manager.call(new TestEvent());

        assertEquals(1, listener.calls.get());
    }

    @Test
    void samePriorityListenersRunInRegistrationOrder() {
        List<String> calls = new ArrayList<>();
        manager.subscribe((Object) new OrderedListener("first", calls));
        manager.subscribe((Object) new OrderedListener("second", calls));
        manager.subscribe((Object) new OrderedListener("third", calls));

        manager.call(new TestEvent());

        assertEquals(List.of("first", "second", "third"), calls);
    }

    private static final class TestEvent extends StructureEvent {
    }

    private static final class PlainListener {
        private final AtomicInteger calls = new AtomicInteger();

        @StructureEventHandler
        public void onEvent(TestEvent event) {
            calls.incrementAndGet();
        }
    }

    private static final class OrderedListener {
        private final String id;
        private final List<String> calls;

        private OrderedListener(String id, List<String> calls) {
            this.id = id;
            this.calls = calls;
        }

        @StructureEventHandler
        public void onEvent(TestEvent event) {
            calls.add(id);
        }
    }

    private static final class TestOption implements Option {
        private final AtomicInteger directCalls = new AtomicInteger();
        private final AtomicInteger nestedCalls = new AtomicInteger();

        private final Listener nested = new Listener() {
            @StructureEventHandler
            public void onEvent(TestEvent event) {
                nestedCalls.incrementAndGet();
            }
        };

        @StructureEventHandler
        public void onEvent(TestEvent event) {
            directCalls.incrementAndGet();
        }

        @Override
        public void read(Structure structure, ConfigSection configSection) {
        }

        @Override
        public void write(ConfigSection configSection) {
        }

        @Override
        public Key getKey() {
            return null;
        }

        @Override
        public void init(Structure structure) {
        }

        @Override
        public void init() {
        }
    }
}
