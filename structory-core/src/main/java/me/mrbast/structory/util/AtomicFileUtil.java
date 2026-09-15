package me.mrbast.structory.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class AtomicFileUtil {

    private AtomicFileUtil() {
    }

    @FunctionalInterface
    public interface FileWriter {
        void write(File file) throws IOException;
    }

    public static void writeWithBackup(File target, FileWriter writer) throws IOException {
        Path targetPath = target.toPath();
        Path parent = targetPath.toAbsolutePath().getParent();
        if (parent == null) throw new IOException("Target has no parent directory: " + target);

        Files.createDirectories(parent);
        Path temp = Files.createTempFile(parent, target.getName() + ".", ".tmp");
        Path backup = targetPath.resolveSibling(target.getName() + ".bak");

        try {
            writer.write(temp.toFile());

            if (Files.exists(targetPath)) {
                Files.copy(targetPath, backup,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES);
            }

            try {
                Files.move(temp, targetPath,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temp, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    public static boolean deleteWithBackup(File target) throws IOException {
        Path targetPath = target.toPath();
        if (!Files.exists(targetPath)) return false;

        Path backup = targetPath.resolveSibling(target.getName() + ".bak");
        Files.copy(targetPath, backup,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES);
        return Files.deleteIfExists(targetPath);
    }
}
