package me.mrbast.structory.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AtomicFileUtilTest {

    @TempDir
    Path tempDir;

    @Test
    void replacesTargetAndKeepsPreviousVersionAsBackup() throws Exception {
        Path target = tempDir.resolve("instance.yml");
        Files.writeString(target, "old", StandardCharsets.UTF_8);

        AtomicFileUtil.writeWithBackup(target.toFile(), file ->
                Files.writeString(file.toPath(), "new", StandardCharsets.UTF_8));

        assertEquals("new", Files.readString(target));
        assertEquals("old", Files.readString(tempDir.resolve("instance.yml.bak")));
    }

    @Test
    void deleteCreatesBackupBeforeRemovingTarget() throws Exception {
        Path target = tempDir.resolve("item.yml");
        Files.writeString(target, "data", StandardCharsets.UTF_8);

        assertTrue(AtomicFileUtil.deleteWithBackup(target.toFile()));
        assertFalse(Files.exists(target));
        assertEquals("data", Files.readString(tempDir.resolve("item.yml.bak")));
    }
}
