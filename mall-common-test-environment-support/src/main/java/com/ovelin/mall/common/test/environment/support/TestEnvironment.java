package com.ovelin.mall.common.test.environment.support;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

public final class TestEnvironment {
    private static final System.Logger LOGGER = System.getLogger(TestEnvironment.class.getName());
    private static final String SCRIPT = "docker/test.sh";
    private static final String HASH_FILE = ".docker.shash";

    public static synchronized void resetOnce() {
        Path script = locateScript();
        Path dockerDirectory = script.getParent();
        Path hashFile = dockerDirectory.getParent().resolve(HASH_FILE);
        try {
            String currentHash = hashDirectory(dockerDirectory);
            if (Files.isRegularFile(hashFile)
                    && currentHash.equals(Files.readString(hashFile, StandardCharsets.UTF_8).trim())) {
                LOGGER.log(System.Logger.Level.INFO,
                        "Docker test environment is unchanged (SHA-256: {0}); skipping setup.", currentHash);
                return;
            }

            LOGGER.log(System.Logger.Level.INFO,
                    "Docker test environment changed; running {0}.", script);
            Process p = new ProcessBuilder("bash", script.toString())
                    .directory(dockerDirectory.toFile())
                    .inheritIO()
                    .start();
            if (!p.waitFor(5, TimeUnit.MINUTES)) {
                p.destroyForcibly();
                LOGGER.log(System.Logger.Level.ERROR, "Timed out while running {0}.", script);
                throw new IllegalStateException("test.sh timeout");
            }
            if (p.exitValue() != 0) {
                LOGGER.log(System.Logger.Level.ERROR,
                        "{0} failed with exit code {1}.", script, p.exitValue());
                throw new IllegalStateException("test.sh failed, exit=" + p.exitValue());
            }
            saveHash(hashFile, currentHash);
            LOGGER.log(System.Logger.Level.INFO,
                    "Docker test environment setup completed; saved hash to {0}.", hashFile);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOGGER.log(System.Logger.Level.ERROR, "Failed to prepare Docker test environment.", e);
            throw new IllegalStateException(e);
        }
    }

    private static String hashDirectory(Path directory) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }

        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.comparing(p ->
                    directory.relativize(p).toString())).toList()) {
                String relativePath = directory.relativize(path).toString().replace(path.getFileSystem().getSeparator(), "/");
                updateString(digest, relativePath);

                if (Files.isSymbolicLink(path)) {
                    digest.update((byte) 'L');
                    updateString(digest, Files.readSymbolicLink(path).toString());
                } else if (Files.isDirectory(path)) {
                    digest.update((byte) 'D');
                } else if (Files.isRegularFile(path)) {
                    digest.update((byte) 'F');
                    digest.update(ByteBuffer.allocate(Long.BYTES).putLong(Files.size(path)).array());
                    try (InputStream input = Files.newInputStream(path)) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = input.read(buffer)) != -1) {
                            digest.update(buffer, 0, read);
                        }
                    }
                } else {
                    digest.update((byte) 'O');
                }
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void updateString(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }

    private static void saveHash(Path hashFile, String hash) throws IOException {
        Path temporaryFile = Files.createTempFile(hashFile.getParent(), HASH_FILE, ".tmp");
        try {
            Files.writeString(temporaryFile, hash + System.lineSeparator(), StandardCharsets.UTF_8);
            try {
                Files.move(temporaryFile, hashFile,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporaryFile, hashFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private static Path locateScript() {
        // 从当前目录向上查找
        String userDir = System.getProperty("user.dir");
        Path dir = Path.of(userDir).toAbsolutePath();
        while (dir != null) {
            Path candidate = dir.resolve(SCRIPT);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException(
                "找不到 " + SCRIPT + "，请在项目目录下运行，或用 -Dtest.env.script 指定路径");
    }
}