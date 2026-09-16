package me.mrbast.structory.command;

import me.mrbast.structory.util.SchedulerUtil;
import me.mrbast.structory.version.Version;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class SchedulerDiagnosticsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        SchedulerUtil.SchedulerSnapshot snapshot = SchedulerUtil.snapshot();

        sender.sendMessage("§6Structory runtime diagnostics");
        sender.sendMessage("§7Platform: §f" + snapshot.getPlatformLabel()
                + " §8| §7Folia: §f" + snapshot.isFolia());
        sender.sendMessage("§7Minecraft: §f" + Version.getDetectedBukkitVersion()
                + " §8| §7Adapter: §f" + Version.getSelectedAdapter()
                + " §8| §7Explicitly supported: §f" + Version.isExplicitlySupported());
        sender.sendMessage("§7Platform tasks: §f" + snapshot.getPlatformTasks()
                + " §8(global " + snapshot.getGlobalTasks()
                + ", region " + snapshot.getRegionTasks()
                + ", entity " + snapshot.getEntityTasks() + ")");
        sender.sendMessage("§7Platform executions/failures: §f" + snapshot.getPlatformExecutions()
                + "§8/§f" + snapshot.getPlatformFailures());
        sender.sendMessage("§7Async executor: §f" + snapshot.isAsyncRunning()
                + " §8| §7active: §f" + snapshot.getActiveAsyncWorkers()
                + " §8| §7queued: §f" + snapshot.getQueuedAsyncTasks()
                + " §8| §7tracked: §f" + snapshot.getTrackedAsyncTasks());
        sender.sendMessage("§7Async submitted/completed/failed: §f" + snapshot.getSubmittedAsyncTasks()
                + "§8/§f" + snapshot.getCompletedAsyncTasks()
                + "§8/§f" + snapshot.getFailedAsyncTasks());
        return true;
    }
}
