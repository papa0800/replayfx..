package com.replayfx.gui;

import com.replayfx.recording.Recording;
import com.replayfx.recording.RecordingIO;
import com.replayfx.playback.PlaybackController;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Minimal list screen: one row per saved recording with Play / Delete
 * buttons. Deliberately not using EntryListWidget to keep this file short --
 * fine for tens of recordings, swap to a proper list widget if you save
 * hundreds.
 */
public class RecordingsScreen extends Screen {
    private final PlaybackController playback;
    private List<Path> files;

    public RecordingsScreen(PlaybackController playback) {
        super(Text.translatable("replayfx.screen.title"));
        this.playback = playback;
    }

    @Override
    protected void init() {
        files = RecordingIO.list();

        int y = 40;
        int rowHeight = 24;
        int maxRows = Math.min(files.size(), 10);

        for (int i = 0; i < maxRows; i++) {
            Path file = files.get(i);
            int rowY = y + i * rowHeight;

            addDrawableChild(ButtonWidget.builder(Text.literal(fileLabel(file)), b -> {
                        Recording recording = RecordingIO.load(file);
                        playback.play(recording);
                        close();
                    })
                    .dimensions(width / 2 - 150, rowY, 220, 20)
                    .build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Delete"), b -> {
                        try {
                            Files.deleteIfExists(file);
                        } catch (IOException ignored) {
                        }
                        clearAndInit();
                    })
                    .dimensions(width / 2 + 75, rowY, 75, 20)
                    .build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> close())
                .dimensions(width / 2 - 50, y + maxRows * rowHeight + 20, 100, 20)
                .build());
    }

    private String fileLabel(Path file) {
        String name = file.getFileName().toString();
        return name.length() > 30 ? name.substring(0, 27) + "..." : name;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
