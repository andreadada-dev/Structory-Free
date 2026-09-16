package me.mrbast.structory.config;

import me.mrbast.dadaconfig.logic.Config;
import me.mrbast.structory.Structory;
import me.mrbast.structory.enums.StructureMessage;
import me.mrbast.structory.format.Formatter;
import me.mrbast.structory.message.Message;
import org.bukkit.configuration.InvalidConfigurationException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MessageConfig extends Config {

    private static final MessageConfig instance;

    static {
        try {
            instance = new MessageConfig();
        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static MessageConfig getInstance() {
        return instance;
    }

    private static final Map<String, String> legacyToTag = new HashMap<String, String>() {{
        put("&0", "<black>"); put("&1", "<dark_blue>");
        put("&2", "<dark_green>"); put("&3", "<dark_aqua>");
        put("&4", "<dark_red>"); put("&5", "<dark_purple>");
        put("&6", "<gold>"); put("&7", "<grey>");
        put("&8", "<dark_gray>"); put("&9", "<blue>");
        put("&a", "<green>"); put("&b", "<aqua>");
        put("&c", "<red>"); put("&d", "<light_purple>");
        put("&e", "<yellow>"); put("&f", "<white>");
        put("&l", "<bold>"); put("&o", "<italic>");
        put("&n", "<underline>"); put("&m", "<strikethrough>");
        put("&k", "<obfuscated>"); put("&r", "<reset>");
    }};

    public MessageConfig() throws IOException, InvalidConfigurationException {
        super();
        init("messages.yml", true);
    }

    public String fromLegacy(String text) {
        for (Map.Entry<String, String> entry : legacyToTag.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
        }
        return text;
    }

    private volatile Map<Message, Formatter> messages = Map.of();

    @Override
    public synchronized void load() {
        Map<Message, Formatter> parsed = new HashMap<>();

        this.getKeys(false).forEach(key -> this.getSection(key).ifPresent(section ->
                section.getKeys(false).forEach(messageKey -> {
                    try {
                        StructureMessage message = StructureMessage.valueOf(messageKey.toUpperCase());
                        Optional<String> value = section.readString(messageKey);
                        parsed.put(message, new Formatter(fromLegacy(value.orElse(""))));
                    } catch (IllegalArgumentException ignored) {
                        // Unknown keys are intentionally ignored for forward/backward compatibility.
                    }
                })));

        messages = Map.copyOf(parsed);
    }

    public Map<Message, Formatter> getMessages() {
        return messages;
    }

    public Formatter getMessage(Message message) {
        return messages.getOrDefault(message, new Formatter(""));
    }

    public synchronized void reload() {
        try {
            init("messages.yml", true);
            load();
        } catch (IOException | InvalidConfigurationException e) {
            Structory.getPlugin(Structory.class).getLogger().severe("Could not reload messages.yml: " + e.getMessage());
            throw new IllegalStateException("Could not reload messages.yml", e);
        }
    }
}
