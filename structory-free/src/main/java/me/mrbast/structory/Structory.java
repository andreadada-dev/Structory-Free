package me.mrbast.structory;

import me.mrbast.dadagui.bukkit.BukkitGuiManager;
import me.mrbast.platform.Platform;
import me.mrbast.structory.api.StructoryAPI;
import me.mrbast.structory.async.StructureParticleScheduler;
import me.mrbast.structory.command.StructoryCommand;
import me.mrbast.structory.config.ConfigInit;
import me.mrbast.structory.config.MainConfig;
import me.mrbast.structory.crafting.option.CraftingOption;
import me.mrbast.structory.listener.AltarGenericInteractionListener;
import me.mrbast.structory.listener.AltarGriefListener;
import me.mrbast.structory.listener.AltarInteractionListener;
import me.mrbast.structory.manager.ConfigManager;
import me.mrbast.structory.manager.OptionManager;
import me.mrbast.structory.manager.RecipeManager;
import me.mrbast.structory.manager.SavedItemManager;
import me.mrbast.structory.manager.StructureInstanceManager;
import me.mrbast.structory.manager.StructureManager;
import me.mrbast.structory.metrics.Metrics;
import me.mrbast.structory.oldblockdata.newer.CustomBlockData;
import me.mrbast.structory.structure.builder.Builder;
import me.mrbast.structory.util.SchedulerUtil;
import me.mrbast.structory.version.Version;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Logger;

public class Structory extends JavaPlugin {

    private Metrics metrics;
    private Platform platform;
    private final Logger logger = getLogger();
    private BukkitGuiManager guiManager;
    private StructoryCommand structoryCommand;

    @Override
    public void onEnable() {
        Version.prepare(this);
        platform = Platform.prepare(this);
        SchedulerUtil.init(this, platform);
        StructoryAPI.bind(this);

        ConfigInit.init();

        guiManager = new BukkitGuiManager(this, (player, task) -> platform.runFor(player, task));
        guiManager.register();

        Builder.init();
        CustomBlockData.registerListener(this);

        ConfigManager.getInstance().load();
        OptionManager.getInstance().init();
        StructureParticleScheduler.getInstance().start();

        Bukkit.getServer().getPluginManager().registerEvents(new AltarInteractionListener(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new AltarGenericInteractionListener(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new AltarGriefListener(), this);

        if (MainConfig.getInstance().metrics()) metrics = new Metrics(this, 23235);
        logger.info("Metrics: " + (metrics != null ? "enabled" : "disabled"));

        structoryCommand = new StructoryCommand();
        Objects.requireNonNull(getCommand("structory")).setExecutor(structoryCommand);
        Objects.requireNonNull(getCommand("structory")).setTabCompleter((sender, command, label, args) ->
                structoryCommand.getArgumentTrie().tabComplete(
                        structoryCommand.getArgumentTrie(), sender, command, label, args));

        logger.info("Plugin loaded!");
    }

    public StructoryCommand getStructoryCommand() {
        return structoryCommand;
    }

    @Override
    public void onDisable() {
        StructoryAPI.shutdown();

        if (guiManager != null) guiManager.unregister();
        HandlerList.unregisterAll(this);

        CraftingOption crafting = CraftingOption.getInstance();
        crafting.dropAllRecipeItems();
        crafting.clearRuntimeState();

        StructureParticleScheduler particles = StructureParticleScheduler.getInstance();
        particles.stop();
        particles.clear();

        OptionManager.getInstance().clear();
        SchedulerUtil.shutdown();
        if (metrics != null) metrics.shutdown();

        RecipeManager.getInstance().clear();
        SavedItemManager.getInstance().clear();
        StructureManager.getInstance().clear();
        StructureInstanceManager.getInstance().clear();

        logger.info("Plugin disabled");
    }

    public Platform getPlatform() {
        return platform;
    }

    public BukkitGuiManager getGuiManager() {
        return guiManager;
    }
}
