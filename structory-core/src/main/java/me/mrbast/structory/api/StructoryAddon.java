package me.mrbast.structory.api;

/**
 * Extension point for addons that integrate directly with Structory.
 *
 * <p>Addons are registered through the public Structory API. Their listeners are
 * tracked by the supplied context and are automatically removed when the addon
 * is unregistered or Structory shuts down.</p>
 */
public interface StructoryAddon {

    /**
     * Stable, case-insensitive addon identifier.
     */
    String id();

    /**
     * Called once after the addon has been registered.
     */
    default void onEnable(StructoryAddonContext context) {
    }

    /**
     * Called once when the addon is unregistered or Structory shuts down.
     */
    default void onDisable() {
    }
}
