package me.mrbast.structory.api;

/**
 * Lifecycle-scoped services exposed to a registered Structory addon.
 */
public interface StructoryAddonContext {

    String addonId();

    /**
     * Registers every {@code @StructureEventHandler} declared by the listener.
     * The listener is automatically unregistered with the addon.
     */
    void subscribe(Object listener);

    /**
     * Removes a listener previously registered by this addon.
     */
    void unsubscribe(Object listener);
}
