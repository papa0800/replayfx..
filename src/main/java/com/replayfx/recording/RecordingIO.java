package com.replayfx.recording;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** Reads and writes {@link Recording} objects to <code>.minecraft/replayfx/</code>. */
public final class RecordingIO {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private RecordingIO() {}

    public static Path folder() {
        Path dir = MinecraftClient.getInstance().runDirectory.toPath().resolve("replayfx");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create ReplayFX recordings folder", e);
        }
        return dir;
    }

    public static Path save(Recording recording) {
        Path file = folder().resolve(sanitize(recording.name) + ".rfx.json");
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(recording, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save recording " + recording.name, e);
        }
        return file;
    }

    public static Recording load(Path file) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, Recording.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load recording from " + file, e);
        }
    }

    /** Lists saved recordings, most recent first. */
    public static List<Path> list() {
        List<Path> result = new ArrayList<>();
        try (Stream<Path> stream = Files.list(folder())) {
            stream.filter(p -> p.toString().endsWith(".rfx.json"))
                    .sorted(Comparator.comparingLong(RecordingIO::lastModifiedSafe).reversed())
                    .forEach(result::add);
        } catch (IOException e) {
            // Folder empty or unreadable -- just return what we have (nothing).
        }
        return result;
    }

    private static long lastModifiedSafe(Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9-_]", "_");
    }
}
