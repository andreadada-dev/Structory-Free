package me.mrbast.structory.command;

import me.mrbast.structory.Structory;
import me.mrbast.structory.config.SingleSavedItemConfig;
import me.mrbast.structory.enums.StructureMessage;
import me.mrbast.structory.format.Format;
import me.mrbast.structory.manager.SavedItemManager;
import me.mrbast.structory.manager.StructoryReloadService;
import me.mrbast.structory.saveditem.SavedItemProvider;
import me.mrbast.structory.util.SchedulerUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class StructoryCommand implements CommandExecutor {

    private static final ArgumentTrieTabCompleter SAVED_ITEM_COMPLETER = new ArgumentTrieTabCompleter() {
        @Override
        public @Nullable List<String> onTabComplete(ArgumentTrie trie, @NotNull CommandSender commandSender,
                                                    @NotNull Command command, @NotNull String s,
                                                    @NotNull String[] strings) {
            Set<String> args = trie.getArguments();
            if (strings.length == 0) return args.stream().toList();

            String arg = strings[0];
            return SavedItemManager.getInstance().getAll().stream()
                    .filter(x -> x.getKey().getKey().startsWith(arg))
                    .map(x -> x.getKey().getKey())
                    .toList();
        }
    };

    private final ArgumentTrie argumentTrie;

    public StructoryCommand() {
        argumentTrie = new ArgumentTrie("");
        argumentTrie.setCommand(true);
        argumentTrie.setExecutor((sender, cmd, label, args) -> {
            StructureMessage.MAIN_COMMAND.send(sender);
            return true;
        });
        argumentTrie.setPermission(new Permission("structory.cmd.use"));
        registerCommandTree();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        argumentTrie.execute(sender, cmd, label, args);
        return true;
    }

    public ArgumentTrie getArgumentTrie() {
        return argumentTrie;
    }

    public void registerArgument(ArgumentTrie trie) {
        this.argumentTrie.registerArgument(trie);
    }

    private void registerCommandTree() {
        ArgumentTrie helpTrie = new ArgumentTrie("help", (sender, command, label, args) -> {
            StructureMessage.HELP_MAIN.send(sender);
            return true;
        });

        ArgumentTrie reloadTrie = new ArgumentTrie("reload", (sender, command, label, args) -> {
            try {
                StructoryReloadService.reload();
                StructureMessage.CMD_RELOAD.send(Format.of().as(), sender);
            } catch (RuntimeException exception) {
                Structory.getPlugin(Structory.class).getLogger().severe("Structory reload failed: " + exception.getMessage());
                StructureMessage.CMD_RELOAD_FAILED.send(sender);
            }
            return true;
        });

        ArgumentTrie performanceTrie = new ArgumentTrie("performance", new SchedulerDiagnosticsCommand());

        ArgumentTrie itemTrie = new ArgumentTrie("item", (sender, cmd, alias, strings) -> {
            StructureMessage.HELP_ITEM.send(sender);
            return true;
        });

        itemTrie.registerCommand(new ArgumentTrie("help", (sender, cmd, alias, strings) -> {
            StructureMessage.HELP_ITEM.send(sender);
            return true;
        }, SAVED_ITEM_COMPLETER), "structory.cmd.item.help");

        itemTrie.registerCommand(new ArgumentTrie("save", (sender, cmd, alias, strings) -> {
            if (strings.length == 0) return true;
            if (!(sender instanceof Player)) {
                StructureMessage.PLAYER_ONLY.send(sender);
                return true;
            }

            String itemName = strings[0];
            if (itemName.isEmpty()) itemName = UUID.randomUUID().toString();

            Player player = (Player) sender;
            if (SavedItemManager.getInstance().has(itemName)) {
                StructureMessage.CUSTOM_ITEM_ALREADY_EXISTS.send(Format.of("name").as(itemName), player);
                return true;
            }

            ItemStack itemStack = player.getInventory().getItemInMainHand().clone();
            SavedItemProvider savedItem = SavedItemManager.getInstance().prepare(itemName, itemStack);
            if (!SavedItemManager.getInstance().register(savedItem)) {
                StructureMessage.CUSTOM_ITEM_LIMIT_REACHED.send(sender);
                return true;
            }

            StructureMessage.CUSTOM_ITEM_CREATED.send(Format.of("name").as(savedItem.getKey().getKey()), player);
            SchedulerUtil.async(() -> new SingleSavedItemConfig(savedItem).save());
            return true;
        }, SAVED_ITEM_COMPLETER), "structory.cmd.item.save");

        itemTrie.registerCommand(new ArgumentTrie("get", (sender, cmd, alias, strings) -> {
            if (strings.length == 0) return true;
            if (!(sender instanceof Player)) {
                StructureMessage.PLAYER_ONLY.send(sender);
                return true;
            }

            String itemName = strings[0];
            if (!SavedItemManager.getInstance().has(itemName)) {
                StructureMessage.CUSTOM_ITEM_DONT_EXISTS.send(Format.of("name").as(itemName), sender);
                return true;
            }

            SavedItemProvider provider = SavedItemManager.getInstance().get(itemName);
            ((Player) sender).getInventory().addItem(provider.getItem());
            return true;
        }, SAVED_ITEM_COMPLETER), "structory.cmd.item.get");

        itemTrie.registerCommand(new ArgumentTrie("replace", (sender, cmd, alias, strings) -> {
            if (strings.length == 0) return true;
            if (!(sender instanceof Player)) {
                StructureMessage.PLAYER_ONLY.send(sender);
                return true;
            }

            String itemName = strings[0];
            if (itemName.isEmpty() || !SavedItemManager.getInstance().has(itemName)) {
                StructureMessage.CUSTOM_ITEM_DONT_EXISTS.send(Format.of("name").as(itemName), sender);
                return true;
            }

            Player player = (Player) sender;
            ItemStack itemStack = player.getInventory().getItemInMainHand().clone();
            if (SavedItemManager.getInstance().replace(itemName, itemStack)) {
                StructureMessage.CUSTOM_ITEM_REPLACED.send(Format.of("name").as(itemName), player);
            }
            return true;
        }, SAVED_ITEM_COMPLETER), "structory.cmd.item.replace");

        itemTrie.registerCommand(new ArgumentTrie("delete", (sender, cmd, alias, strings) -> {
            if (strings.length == 0) return true;

            String itemName = strings[0];
            if (itemName.isEmpty()) return true;

            if (!SavedItemManager.getInstance().has(itemName)) {
                StructureMessage.CUSTOM_ITEM_DONT_EXISTS.send(Format.of("name").as(itemName), sender);
                return true;
            }

            SavedItemProvider provider = SavedItemManager.getInstance().get(itemName);
            SavedItemManager.getInstance().delete(provider);
            StructureMessage.CUSTOM_ITEM_DELETED.send(Format.of("name").as(provider.getKey().getKey()), sender);
            SchedulerUtil.async(() -> new SingleSavedItemConfig(provider).delete());
            return true;
        }, SAVED_ITEM_COMPLETER), "structory.cmd.item.delete");

        argumentTrie.registerCommand(reloadTrie, "structory.cmd.reload");
        argumentTrie.registerCommand(helpTrie, "structory.cmd.help");
        argumentTrie.registerCommand(performanceTrie, "structory.cmd.performance");
        argumentTrie.registerCommand(itemTrie, "structory.cmd.item.use");
    }
}
