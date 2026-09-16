package me.mrbast.structory;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginDescriptorTest {

    @Test
    void descriptorMatchesRuntimeCommandPermissions() throws Exception {
        YamlConfiguration plugin = load("/plugin.yml");

        String version = plugin.getString("version");
        assertNotNull(version);
        assertFalse(version.contains("${"), "plugin.yml version was not filtered by Maven");
        assertTrue(plugin.getBoolean("folia-supported"));

        for (String permission : List.of(
                "structory.cmd.use",
                "structory.cmd.help",
                "structory.cmd.reload",
                "structory.cmd.item.use",
                "structory.cmd.item.help",
                "structory.cmd.item.save",
                "structory.cmd.item.get",
                "structory.cmd.item.replace",
                "structory.cmd.item.delete")) {
            assertTrue(plugin.isConfigurationSection("permissions." + permission),
                    "Missing permission declaration: " + permission);
        }

        for (String permission : List.of(
                "use", "help", "save", "get", "replace", "delete")) {
            assertTrue(plugin.getBoolean("permissions.structory.cmd.item.*.children.structory.cmd.item." + permission),
                    "Saved-item wildcard does not include: " + permission);
        }

        assertTrue(plugin.getBoolean("permissions.structory.cmd.*.children.structory.cmd.use"));
        assertTrue(plugin.getBoolean("permissions.structory.cmd.*.children.structory.cmd.help"));
        assertTrue(plugin.getBoolean("permissions.structory.cmd.*.children.structory.cmd.reload"));
        assertTrue(plugin.getBoolean("permissions.structory.cmd.*.children.structory.cmd.item.*"));
        assertTrue(plugin.getBoolean("permissions.structory.*.children.structory.cmd.*"));
    }

    private YamlConfiguration load(String resource) throws Exception {
        try (InputStream stream = getClass().getResourceAsStream(resource)) {
            assertNotNull(stream, resource);
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
    }
}
