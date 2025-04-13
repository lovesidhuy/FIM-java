import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

// Watches a directory and verifies file integrity using SHA-256
class DirectoryWatcher {
    private static final String FOLDER_NAME = "watched_files";
    private static final Map<Path, String> fileHashes = new HashMap<>();

    public static void startWatching() throws IOException {
        Path folderPath = Paths.get(FOLDER_NAME);
        System.out.println("👀👀 Watching: " + folderPath.toAbsolutePath());

        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            folderPath.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE
            );

            while (true) {
                WatchKey key;
                try {
                    key = watchService.take();
                } catch (InterruptedException e) {
                    break;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> eventType = event.kind();
                    Path changedFile = folderPath.resolve((Path) event.context());

                    if (eventType == StandardWatchEventKinds.ENTRY_CREATE) {
                        System.out.println("🎁🎁 Created: " + changedFile.getFileName());
                    } else if (eventType == StandardWatchEventKinds.ENTRY_MODIFY) {
                        System.out.println("🔧🔧 Modified: " + changedFile.getFileName());
                    } else if (eventType == StandardWatchEventKinds.ENTRY_DELETE) {
                        System.out.println(" \uD83D\uDDD1\uFE0F \uD83D\uDDD1\uFE0FDeleted: " + changedFile.getFileName());
                    }

                    if (eventType == StandardWatchEventKinds.ENTRY_DELETE) {
                        fileHashes.remove(changedFile);
                    } else if (Files.isRegularFile(changedFile)) {
                        String newHash = computeSHA256(changedFile);
                        String oldHash = fileHashes.get(changedFile);

                        if (oldHash == null) {
                            System.out.println("New file hash recorded.");
                        } else if (!newHash.equals(oldHash)) {
                            System.out.println("File content changed!");
                            System.out.println("   Old: " + oldHash);
                            System.out.println("   New: " + newHash);
                        } else {
                            System.out.println("File content unchanged.");
                        }

                        fileHashes.put(changedFile, newHash);
                        System.out.println("SHA256: " + newHash);
                    }
                }

                if (!key.reset()) break;
            }
        }
    }

    // Computes SHA-256 hash of a file
    private static String computeSHA256(Path filePath) throws IOException {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(filePath);
            byte[] hashBytes = sha256.digest(fileBytes);

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new IOException("SHA-256 hash fail", e);
        }
    }
}
public class Main {
    public static void main(String[] args) {
        try {
            DirectoryWatcher.startWatching();
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
