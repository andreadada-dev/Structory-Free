package me.mrbast.structory;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HardeningRegressionTest {

    @Test
    void playerOnlyItemCommandsGuardSenderBeforeCasting() throws Exception {
        String source = source("command/StructoryCommand.java");
        assertGuardBeforeCast(source, "new ArgumentTrie(\"save\"", "Player player = (Player) sender");
        assertGuardBeforeCast(source, "new ArgumentTrie(\"get\"", "((Player) sender)");
        assertGuardBeforeCast(source, "new ArgumentTrie(\"replace\"", "Player player = (Player) sender");
    }

    @Test
    void reloadCommandUsesDedicatedServiceInsteadOfPluginLifecycleCallbacks() throws Exception {
        String source = source("command/StructoryCommand.java");
        assertTrue(source.contains("StructoryReloadService.reload();"));
        assertFalse(source.contains(".onDisable()"));
        assertFalse(source.contains(".onEnable()"));
    }

    @Test
    void reloadIsSchedulerBarrierThenRebuildsRuntime() throws Exception {
        String source = source("manager/StructoryReloadService.java");
        assertOrdered(source,
                "particleScheduler.stop();",
                "SchedulerUtil.cancelPlatformTasks();",
                "crafting.clearRuntimeState();",
                "StructureInstanceManager.getInstance().clear();",
                "SavedItemManager.getInstance().clear();",
                "OptionManager.getInstance().init();",
                "ConfigManager.getInstance().load();",
                "particleScheduler.start();");
    }

    @Test
    void configManagerReloadsMessagesFromDiskButDoesNotOwnSchedulerLifecycle() throws Exception {
        String source = source("manager/ConfigManager.java");
        assertTrue(source.contains("MessageConfig.getInstance().reload();"));
        assertFalse(source.contains("SchedulerUtil"));
        assertFalse(source.contains("getAsyncExecutor().init()"));

        String scheduler = coreSource("util/SchedulerUtil.java");
        assertOrdered(scheduler,
                "asyncExecutor = new AsyncExecutor();",
                "asyncExecutor.init();");
        assertTrue(scheduler.contains("cancelPlatformTasks();"));
    }

    @Test
    void structurePersistenceNeverBlocksRegionThread() throws Exception {
        String validator = source("structure/validator/StructureValidator.java");
        String manager = source("manager/StructureInstanceManager.java");
        assertTrue(validator.contains("SchedulerUtil.async(() -> new SingleStructureInstanceConfig(inst).save())"));
        assertFalse(validator.contains("SchedulerUtil.region(inst.getData().getCenter(), () -> new SingleStructureInstanceConfig(inst).save())"));
        assertTrue(manager.contains("SchedulerUtil.async(() -> new SingleStructureInstanceConfig(instance).delete())"));
    }

    @Test
    void runtimeDiagnosticsAndCompatibilityStatusRemainExposed() throws Exception {
        String command = source("command/StructoryCommand.java");
        String version = source("version/Version.java");
        assertTrue(command.contains("new ArgumentTrie(\"performance\", new SchedulerDiagnosticsCommand())"));
        assertTrue(version.contains("isExplicitlySupported()"));
        assertTrue(version.contains("1\\\\.(21.*|21|20.*|20|19.*|19)"));
        assertTrue(version.contains("1\\\\.(17.*|17|18.*|18)"));
        assertTrue(version.contains("outside Structory's explicit 1.17-1.21 compatibility matrix"));
    }

    @Test
    void persistenceDirectoryScansIgnoreBackupFiles() throws Exception {
        assertTrue(source("config/DirectorySavedItemConfig.java").contains("endsWith(\".yml\")"));
        assertTrue(source("config/DirectoryStructureInstanceConfig.java").contains("endsWith(\".yml\")"));
    }

    @Test
    void mainConfigUsesBundledSchemaAndResetsReloadableValues() throws Exception {
        String source = source("config/MainConfig.java");
        assertTrue(source.contains("String expectedVersion = bundledConfigVersion();"));
        assertTrue(source.contains("metrics = true;"));
        assertTrue(source.contains("shiftToTake = true;"));
        assertTrue(source.contains("disableItemPickup = true;"));
        assertTrue(source.contains("distance = 32.0D;"));
        assertTrue(source.contains("breakConfirmTime = 5000L;"));
    }

    private static void assertGuardBeforeCast(String source, String commandStartToken, String castToken) {
        int commandStart = source.indexOf(commandStartToken);
        int guard = source.indexOf("if (!(sender instanceof Player))", commandStart);
        int cast = source.indexOf(castToken, commandStart);
        assertTrue(commandStart >= 0, "Missing command branch: " + commandStartToken);
        assertTrue(guard > commandStart, "Missing Player guard for " + commandStartToken);
        assertTrue(cast > guard, "Player cast occurs before sender guard for " + commandStartToken);
    }

    private static void assertOrdered(String source, String... tokens) {
        int previous = -1;
        for (String token : tokens) {
            int index = source.indexOf(token);
            assertTrue(index > previous, "Expected ordered token: " + token);
            previous = index;
        }
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(Path.of("src/main/java/me/mrbast/structory", relativePath), StandardCharsets.UTF_8);
    }

    private static String coreSource(String relativePath) throws IOException {
        return Files.readString(Path.of("../structory-core/src/main/java/me/mrbast/structory", relativePath), StandardCharsets.UTF_8);
    }
}
