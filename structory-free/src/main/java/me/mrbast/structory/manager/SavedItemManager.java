package me.mrbast.structory.manager;

import me.mrbast.structory.Structory;
import me.mrbast.structory.config.SingleSavedItemConfig;
import me.mrbast.structory.saveditem.SavedItemProvider;
import me.mrbast.structory.util.SchedulerUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SavedItemManager {

    public static final int MAX_SAVED_ITEMS = 10;

    private static final SavedItemManager instance = new SavedItemManager();

    public static SavedItemManager getInstance() {
        return instance;
    }

    private SavedItemManager() {
    }

    private final Map<NamespacedKey, SavedItemProvider> items = new ConcurrentHashMap<>();
    private final Map<String, NamespacedKey> keys = new ConcurrentHashMap<>();

    /**
     * Registers an item in memory.
     *
     * @return false only when the free-edition limit would be exceeded.
     */
    public boolean register(SavedItemProvider item) {
        NamespacedKey key = item.getKey();
        if (!items.containsKey(key) && items.size() >= MAX_SAVED_ITEMS) {
            return false;
        }

        items.put(key, item);
        keys.put(key.getKey(), key);
        return true;
    }

    public boolean isAtLimit() {
        return items.size() >= MAX_SAVED_ITEMS;
    }

    public int size() {
        return items.size();
    }

    public void clear() {
        items.clear();
        keys.clear();
    }

    public SavedItemProvider prepare(ItemStack stack) {
        return prepare(UUID.randomUUID().toString(), stack);
    }

    public SavedItemProvider prepare(String name, ItemStack stack) {
        if (name == null) name = UUID.randomUUID().toString();
        NamespacedKey namespacedKey = new NamespacedKey(Structory.getPlugin(Structory.class), name);
        return new SavedItemProvider(namespacedKey, stack);
    }

    public boolean has(String itemName) {
        return keys.containsKey(itemName);
    }

    public SavedItemProvider delete(String itemName) {
        NamespacedKey val = keys.remove(itemName);
        if (val == null) return null;
        return items.remove(val);
    }

    public SavedItemProvider get(String key) {
        NamespacedKey val = keys.get(key);
        if (val == null) return null;
        return items.get(val);
    }

    public Collection<SavedItemProvider> getAll() {
        return items.values();
    }

    public void delete(SavedItemProvider provider) {
        keys.remove(provider.getKey().getKey());
        items.remove(provider.getKey());
    }

    /**
     * Replaces an item and persists the new value. If the item does not exist,
     * it is created only when the free-edition item limit allows it.
     *
     * @return true when the replacement/creation was accepted.
     */
    public boolean replace(String itemName, ItemStack savedItem) {
        SavedItemProvider itemProvider = get(itemName);
        if (itemProvider == null) {
            itemProvider = prepare(itemName, savedItem);
            if (!register(itemProvider)) {
                return false;
            }
        } else {
            itemProvider.changeItem(savedItem);
        }

        SavedItemProvider finalItemProvider = itemProvider;
        SchedulerUtil.async(() -> new SingleSavedItemConfig(finalItemProvider).save());
        return true;
    }
}
