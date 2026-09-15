package me.mrbast.structory.enums;

import me.mrbast.structory.config.MessageConfig;
import me.mrbast.structory.format.Format;
import me.mrbast.structory.message.Message;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public enum StructureMessage implements Message {

    PLAYER_ONLY,

    DESTROY_AGAIN_TO_BREAK,
    ERROR_BLOCK_DESTROY,
    ERROR_ALTAR_NEARBY,

    CUSTOM_ITEM_CREATED,
    CUSTOM_ITEM_ALREADY_EXISTS,
    CUSTOM_ITEM_DONT_EXISTS,
    CUSTOM_ITEM_DELETED,
    CUSTOM_ITEM_REPLACED,
    CUSTOM_ITEM_LIMIT_REACHED,

    CMD_RELOAD,
    CMD_RELOAD_FAILED,

    ALTAR_CREATED,
    COMMAND_NOT_FOUND,
    MAIN_COMMAND,
    NO_PEX,
    HELP_MAIN,
    HELP_ITEM;

    public void send(CommandSender sender) {
        MessageConfig.getInstance().getMessage(this).send(sender);
    }

    public void send(Player player, Format format) {
        MessageConfig.getInstance().getMessage(this).send(format, player);
    }

    public void send(Player player) {
        MessageConfig.getInstance().getMessage(this).send(player);
    }

    public void send(Format f, Player player) {
        MessageConfig.getInstance().getMessage(this).send(f, player);
    }

    public void send(Format f, Player... player) {
        for (Player p : player) send(f, p);
    }

    public String get() {
        return MessageConfig.getInstance().getMessage(this).format(Format.of().as());
    }

    public String get(Format format) {
        return MessageConfig.getInstance().getMessage(this).format(format);
    }

    public void send(Format format, CommandSender sender) {
        MessageConfig.getInstance().getMessage(this).send(format, sender);
    }
}
