package com.project.fim;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

// Watches a directory and verifies file integrity using SHA-256
class DirectoryWatcher {
    private static final String FOLDER_NAME = "watched_files";
    private static final String HASH_FILE = "hashes.json";
    private static final Map<Path, String> fileHashes = new HashMap<>();
    private static final Gson gson = new Gson();

    public static void startWatching() throws IOException {
        Path folderPath = Paths.get(FOLDER_NAME);
        Files.createDirectories(folderPath);
        loadHashes();

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
                        System.out.println("🗑️🗑️ Deleted: " + changedFile.getFileName());
                        fileHashes.remove(changedFile);
                        saveHashes();
                        logToSyslog("Deleted: " + changedFile.getFileName());
                        continue;
                    }

                    if (Files.isRegularFile(changedFile)) {
                        String newHash = computeSHA256(changedFile);
                        String oldHash = fileHashes.get(changedFile);

                        if (oldHash == null) {
                            System.out.println("New file hash recorded.");
                            logToSyslog("Created: " + changedFile.getFileName());
                        } else if (!newHash.equals(oldHash)) {
                            System.out.println("File content changed!");
                            System.out.println("   Old: " + oldHash);
                            System.out.println("   New: " + newHash);
                            logToSyslog("Modified: " + changedFile.getFileName());
                        } else {
                            System.out.println("File content unchanged.");
                        }

                        fileHashes.put(changedFile, newHash);
                        saveHashes();
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

    // Load saved hashes from file
    private static void loadHashes() {
        try (Reader reader = new FileReader(HASH_FILE)) {
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> stored = gson.fromJson(reader, type);
            if (stored != null) {
                stored.forEach((k, v) -> fileHashes.put(Paths.get(k), v));
            }
        } catch (FileNotFoundException e) {
            System.out.println("No saved hash baseline found.");
        } catch (IOException e) {
            System.out.println("Error reading hashes.json");
        }
    }

    // Save current hashes to file
    private static void saveHashes() {
        Map<String, String> toSave = new HashMap<>();
        for (Map.Entry<Path, String> entry : fileHashes.entrySet()) {
            toSave.put(entry.getKey().toString(), entry.getValue());
        }
        try (Writer writer = new FileWriter(HASH_FILE)) {
            gson.toJson(toSave, writer);
        } catch (IOException e) {
            System.out.println("Failed to save hashes.");
        }
    }

    // Send message to system log
    private static void logToSyslog(String message) {
        try {
            Runtime.getRuntime().exec(new String[] {
                    "logger", "FIM-Watcher: " + message
            });
        } catch (IOException e) {
            System.out.println("Failed to log to syslog.");
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
