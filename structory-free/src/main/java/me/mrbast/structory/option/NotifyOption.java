package me.mrbast.structory.option;

import me.mrbast.dadaconfig.logic.ConfigSection;
import me.mrbast.platform.Platform;
import me.mrbast.structory.Structory;
import me.mrbast.structory.enums.StructureMessage;
import me.mrbast.structory.enums.StructureSpacedKey;
import me.mrbast.structory.event.AltarCreateEvent;
import me.mrbast.structory.event.Listener;
import me.mrbast.structory.event.StructureEventHandler;
import me.mrbast.structory.format.Format;
import me.mrbast.structory.format.Formatter;
import me.mrbast.structory.structure.Structure;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class NotifyOption implements Option {

    private static final Platform platform = Structory.getPlugin(Structory.class).getPlatform();
    private static final NotifyOption INSTANCE = new NotifyOption();

    public static Option getInstance() {
        return INSTANCE;
    }

    public static class Notify {
        private String message;
        private String title;
        private String subtitle;
        private String actionBar;
        private int duration;
        private int fadeIn;
        private int fadeOut;

        public void send(@NotNull Player player) {
            if (isEmpty()) return;
            if (message != null && !message.isEmpty()) platform.sendMessage(player, message);
            if (actionBar != null && !actionBar.isEmpty()) platform.sendActionbar(player, actionBar);
        }

        public boolean isEmpty() {
            return message == null && title == null && subtitle == null && actionBar == null;
        }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getSubtitle() { return subtitle; }
        public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
        public String getActionBar() { return actionBar; }
        public void setActionBar(String actionBar) { this.actionBar = actionBar; }
        public int getDuration() { return duration; }
        public void setDuration(int duration) { this.duration = duration; }
        public int getFadeIn() { return fadeIn; }
        public void setFadeIn(int fadeIn) { this.fadeIn = fadeIn; }
        public int getFadeOut() { return fadeOut; }
        public void setFadeOut(int fadeOut) { this.fadeOut = fadeOut; }
    }

    private final Map<Structure, Notify> notifies = new HashMap<>();

    private final Listener eventListener = new Listener() {
        @StructureEventHandler
        public void onEventCreate(AltarCreateEvent event) {
            Notify notify = notifies.get(event.getInstance().getData().getStructure());
            if (notify == null || notify.isEmpty()) {
                StructureMessage.ALTAR_CREATED.send(event.getInteractEvent().getPlayer(),
                        Format.of("altar", "structure").as(
                                event.getInstance().getData().getStructure().getData().getName(),
                                event.getInstance().getData().getStructure().getData().getName()));
                return;
            }
            notify.send(event.getInteractEvent().getPlayer());
        }
    };

    public String format(Structure structure, String text) {
        return new Formatter(text).format(Format.of("altar", "structure")
                .as(structure.getData().getName(), structure.getData().getName()));
    }

    @Override
    public void read(Structure structure, ConfigSection section) {
        Notify notify = new Notify();
        section.readString("message").ifPresent(value -> notify.setMessage(format(structure, value)));
        section.readString("title").ifPresent(value -> notify.setTitle(format(structure, value)));
        section.readString("subtitle").ifPresent(value -> notify.setSubtitle(format(structure, value)));
        section.readString("actionbar").ifPresent(value -> notify.setActionBar(format(structure, value)));
        section.readInt("duration").ifPresent(notify::setDuration);
        section.readInt("fade_in").ifPresent(notify::setFadeIn);
        section.readInt("fade_out").ifPresent(notify::setFadeOut);
        notifies.put(structure, notify);
    }

    @Override public void write(ConfigSection configSection) { }
    @Override public StructureSpacedKey getKey() { return StructureSpacedKey.OPTION_NOTIFY; }
    @Override public void init(Structure structure) { }
    @Override public void init() { }

    @Override
    public void onDisable() {
        notifies.clear();
    }
}
