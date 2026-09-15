package me.mrbast.structory;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginDescriptorTest {

    @Test
    void descriptorMatchesRuntimeCommandPermissions() throws Exception {
        YamlConfiguration plugin = load("/plugin.yml");

        assertEquals("26.2-SNAPSHOT", plugin.getString("version"));
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
    }

    private YamlConfiguration load(String resource) throws Exception {
        try (InputStream stream = getClass().getResourceAsStream(resource)) {
            assertNotNull(stream, resource);
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
    }
}
