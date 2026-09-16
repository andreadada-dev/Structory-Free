package me.mrbast.structory.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArgumentTrieTest {

    @Test
    void routesNestedCommandAndPassesOnlyRemainingArguments() {
        ArgumentTrie root = new ArgumentTrie("");
        ArgumentTrie item = new ArgumentTrie("item");
        AtomicReference<String[]> received = new AtomicReference<>();

        item.registerCommand(new ArgumentTrie("get", (sender, command, label, args) -> {
            received.set(args);
            return true;
        }), "structory.cmd.item.get");
        root.registerArgument(item);

        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission(any(org.bukkit.permissions.Permission.class))).thenReturn(true);
        Command command = mock(Command.class);

        root.execute(sender, command, "structory", new String[]{"item", "get", "my_item", "extra"});

        assertArrayEquals(new String[]{"my_item", "extra"}, received.get());
    }

    @Test
    void rootCommandExecutesWhenMarkedAsCommand() {
        ArgumentTrie root = new ArgumentTrie("");
        root.setCommand(true);
        AtomicReference<Integer> calls = new AtomicReference<>(0);
        root.setExecutor((sender, command, label, args) -> {
            calls.set(calls.get() + 1);
            assertEquals(0, args.length);
            return true;
        });

        root.execute(mock(CommandSender.class), mock(Command.class), "structory", new String[0]);
        assertEquals(1, calls.get());
    }
}
