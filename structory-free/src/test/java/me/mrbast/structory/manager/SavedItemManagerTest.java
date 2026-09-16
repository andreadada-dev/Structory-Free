package me.mrbast.structory.manager;

import me.mrbast.structory.saveditem.SavedItemProvider;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SavedItemManagerTest {

    private final SavedItemManager manager = SavedItemManager.getInstance();

    @BeforeEach
    void setUp() {
        manager.clear();
    }

    @AfterEach
    void tearDown() {
        manager.clear();
    }

    @Test
    void rejectsTheEleventhUniqueSavedItem() {
        for (int i = 0; i < SavedItemManager.MAX_SAVED_ITEMS; i++) {
            assertTrue(manager.register(item("item_" + i)));
        }

        assertEquals(SavedItemManager.MAX_SAVED_ITEMS, manager.size());
        assertFalse(manager.register(item("item_over_limit")));
        assertEquals(SavedItemManager.MAX_SAVED_ITEMS, manager.size());
    }

    private SavedItemProvider item(String key) {
        SavedItemProvider provider = mock(SavedItemProvider.class);
        when(provider.getKey()).thenReturn(new NamespacedKey("structory", key));
        return provider;
    }
}
