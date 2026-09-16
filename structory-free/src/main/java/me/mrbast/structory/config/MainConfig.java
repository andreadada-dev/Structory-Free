package me.mrbast.structory.config;

import me.mrbast.dadaconfig.logic.Config;
import me.mrbast.structory.Structory;
import me.mrbast.structory.util.LoggerColor;
import me.mrbast.structory.util.SchedulerUtil;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class MainConfig extends Config {

    private static final MainConfig instance = new MainConfig();
    public static MainConfig getInstance() {
        return instance;
    }

    private long breakConfirmTime;
    public static boolean shiftToTake;
    public boolean disableItemPickup;
    public double distance;
    private boolean metrics;
    public boolean debug;

    private static final Logger LOGGER = Structory.getPlugin(Structory.class).getLogger();

    public MainConfig(){
        super();
    }

    public void updateFile() throws IOException, InvalidConfigurationException {
        if (!this.contains("version")) return;

        String currentVersion = this.getString("version");
        String expectedVersion = bundledConfigVersion();
        if (expectedVersion == null || Objects.equals(expectedVersion, currentVersion)) return;

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        String backupName = "config_backup_" + now.format(formatter) + ".yml";
        File backup = new File(Structory.getPlugin(Structory.class).getDataFolder(), backupName);

        Files.move(configFile.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
        LOGGER.severe("New configuration schema found!");
        LOGGER.severe("Created backup for current config.yml (" + LoggerColor.RED + backupName + LoggerColor.RESET + ")");
        LOGGER.severe("If something does not work as expected, check the new formatting and restore the backup if needed");
        this.init("config.yml", true);
    }

    private String bundledConfigVersion() throws IOException {
        try (InputStream resource = Structory.getPlugin(Structory.class).getResource("config.yml")) {
            if (resource == null) return null;
            YamlConfiguration bundled = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(resource, StandardCharsets.UTF_8));
            return bundled.getString("version");
        }
    }

    @Override
    public void load() {
        try {
            init("config.yml", true);
            updateFile();
        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }

        // Reset runtime values so removed keys cannot retain values from a previous reload.
        metrics = true;
        shiftToTake = true;
        disableItemPickup = true;
        distance = 32.0D;
        breakConfirmTime = 5000L;

        this.getSection("metrics").flatMap(metrics -> metrics.readBoolean("enable"))
                .ifPresent(enable -> this.metrics = enable);

        this.getSection("scheduler").ifPresent(scheduler -> {
            scheduler.readInt("max_pool_size").ifPresent(maxPoolSize -> SchedulerUtil.getAsyncExecutor().setMaxPoolSize(maxPoolSize));
            scheduler.readInt("core_pool_size").ifPresent(corePoolSize -> SchedulerUtil.getAsyncExecutor().setCorePoolSize(corePoolSize));
            scheduler.readInt("keep_alive_time").ifPresent(keepAliveTime -> SchedulerUtil.getAsyncExecutor().setKeepAliveTime(keepAliveTime));
            scheduler.getSection("on_every").flatMap(onEvery -> onEvery.readInt("check"))
                    .ifPresent(check -> SchedulerUtil.getAsyncExecutor().setOnEvery(0, check, TimeUnit.MILLISECONDS));
        });

        this.getSection("structures").ifPresent(structures -> {
            structures.read(Double.class, "distance").ifPresent(x -> distance = x);
            structures.read(Boolean.class, "shift_to_remove_item").ifPresent(x -> shiftToTake = !x);
            structures.read(Boolean.class, "disable_item_pickup").ifPresent(x -> disableItemPickup = x);
            structures.read(Integer.class, "break_confirm_time").ifPresent(x -> breakConfirmTime = x);
        });
    }

    public long getBreakConfirmTime() {
        return breakConfirmTime;
    }

    public boolean metrics() {
        return metrics;
    }
}
