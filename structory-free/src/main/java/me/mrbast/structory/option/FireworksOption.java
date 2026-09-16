package me.mrbast.structory.option;

import me.mrbast.dadaconfig.logic.ConfigSection;
import me.mrbast.structory.enums.StructureSpacedKey;
import me.mrbast.structory.event.AltarCreateEvent;
import me.mrbast.structory.event.Listener;
import me.mrbast.structory.event.StructureEventHandler;
import me.mrbast.structory.structure.Structure;
import me.mrbast.structory.structure.StructureInstance;
import me.mrbast.structory.util.ColorUtil;
import me.mrbast.structory.util.FireworkUtil;
import org.bukkit.Color;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FireworksOption implements Option {

    public interface FireworksSupplierToInstance {
        void spawn(StructureInstance instance);
    }

    public static class Fireworks {
        private final FireworksSupplierToInstance fireworksSupplier;

        public Fireworks(int amount, int power, boolean flicker, Color[] mainColor, Color[] fadeColor) {
            fireworksSupplier = instance -> {
                for (int i = 0; i < amount; i++) {
                    FireworkUtil.prepareFirework(
                            instance.getData().getCenter().clone().add(0.5, 1.5, 0.5),
                            power,
                            flicker,
                            mainColor,
                            fadeColor
                    ).detonate();
                }
            };
        }
    }

    private static final Map<Structure, Fireworks> structures = new HashMap<>();

    @Override
    public void read(Structure structure, ConfigSection section) {
        Optional<String> type = section.read(String.class, "type");
        int amount = section.read(Integer.class, "amount").orElse(0);
        int power = section.read(Integer.class, "power").orElse(0);
        boolean flicker = section.read(Boolean.class, "flicker").orElse(false);
        Optional<String> colorsConfig = section.read(String.class, "colors");
        Optional<String> fadeConfig = section.read(String.class, "fade");

        type.filter("RANDOM"::equalsIgnoreCase).ifPresent(ignored -> {
            Color[] colors = parseColors(colorsConfig.orElse(""));
            Color[] fades = parseColors(fadeConfig.orElse(""));
            structures.put(structure, new Fireworks(amount, power, flicker, colors, fades));
        });
    }

    private Color[] parseColors(String value) {
        if (value == null || value.isBlank()) return new Color[0];
        String[] raw = value.split(",\\s*");
        Color[] result = new Color[raw.length];
        for (int i = 0; i < raw.length; i++) result[i] = ColorUtil.getColor(raw[i]);
        return result;
    }

    @Override
    public void write(ConfigSection configSection) {
    }

    @Override
    public StructureSpacedKey getKey() {
        return StructureSpacedKey.OPTION_FIREWORK;
    }

    private static final Listener eventListener = new Listener() {
        @StructureEventHandler
        public void onEventCreate(AltarCreateEvent event) {
            Fireworks fireworks = structures.get(event.getInstance().getData().getStructure());
            if (fireworks != null) fireworks.fireworksSupplier.spawn(event.getInstance());
        }
    };

    @Override
    public void init(Structure structure) {
    }

    @Override
    public void init() {
    }

    @Override
    public void onDisable() {
        structures.clear();
    }
}
