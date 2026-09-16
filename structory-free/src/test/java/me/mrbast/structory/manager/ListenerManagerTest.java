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

    private static final class TestEvent extends StructureEvent {
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
