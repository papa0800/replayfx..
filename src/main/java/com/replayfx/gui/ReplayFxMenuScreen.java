package com.replayfx.gui;

import com.replayfx.camera.FreeCamController;
import com.replayfx.playback.PlaybackController;
import com.replayfx.recording.Recorder;
import com.replayfx.recording.Recording;
import com.replayfx.recording.RecordingIO;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * The "press V" hub screen: everything about ReplayFX in one place.
 * <ul>
 *   <li>Recording section -- explains R, has a Start/Stop button</li>
 *   <li>Playback section -- explains T, has a Play/Stop button</li>
 *   <li>Free Camera section -- explains G, has a toggle button</li>
 *   <li>Saved Recordings -- the actual list, shown inline, with Play/Delete
 *       buttons per row, so you don't have to jump to a separate screen</li>
 * </ul>
 */
public class ReplayFxMenuScreen extends Screen {
    private static final int LEFT = 40;
    private static final int TEXT_COLOR = 0xE0E0E0;
    private static final int HEADER_COLOR = 0xFFD200;
    private static final int MAX_LISTED_RECORDINGS = 6;

    private final Recorder recorder;
    private final PlaybackController playback;
    private final FreeCamController freeCam;
    private final Recording lastRecording;
    private final Consumer<Recording> onRecordingSaved;

    private int recordingSectionY;
    private int playbackSectionY;
    private int freeCamSectionY;
    private int recordingsListHeaderY;

    private List<Path> savedRecordings;

    public ReplayFxMenuScreen(Recorder recorder, PlaybackController playback,
                               FreeCamController freeCam, Recording lastRecording,
                               Consumer<Recording> onRecordingSaved) {
        super(Text.translatable("replayfx.menu.title"));
        this.recorder = recorder;
        this.playback = playback;
        this.freeCam = freeCam;
        this.lastRecording = lastRecording;
        this.onRecordingSaved = onRecordingSaved;
    }

    @Override
    protected void init() {
        savedRecordings = RecordingIO.list();

        int y = 36;

        // ---- Recording section ----
        recordingSectionY = y;
        y += 34;
        addDrawableChild(ButtonWidget.builder(recordButtonLabel(), b -> {
                    ClientPlayerEntity player = MinecraftClient.getInstance().player;
                    if (player == null) return;
                    if (recorder.isRecording()) {
                        Recording finished = recorder.stop();
                        RecordingIO.save(finished);
                        onRecordingSaved.accept(finished);
                    } else {
                        recorder.start();
                    }
                    clearAndInit();
                })
                .dimensions(LEFT, y, 200, 20)
                .build());
        y += 44;

        // ---- Playback section ----
        playbackSectionY = y;
        y += 34;
        addDrawableChild(ButtonWidget.builder(playbackButtonLabel(), b -> {
                    if (playback.isPlaying()) {
                        playback.stop();
                    } else if (lastRecording != null) {
                        playback.play(lastRecording);
                    }
                    clearAndInit();
                })
                .dimensions(LEFT, y, 200, 20)
                .build());
        y += 44;

        // ---- Free camera section ----
        freeCamSectionY = y;
        y += 34;
        addDrawableChild(ButtonWidget.builder(freeCamButtonLabel(), b -> {
                    freeCam.toggle(MinecraftClient.getInstance().player);
                    clearAndInit();
                })
                .dimensions(LEFT, y, 200, 20)
                .build());
        y += 44;

        // ---- Saved recordings, listed right here ----
        recordingsListHeaderY = y;
        y += 20;

        int shown = Math.min(savedRecordings.size(), MAX_LISTED_RECORDINGS);
        for (int i = 0; i < shown; i++) {
            Path file = savedRecordings.get(i);
            int rowY = y + i * 22;

            addDrawableChild(ButtonWidget.builder(Text.literal(shortLabel(file)), b -> {
                        Recording recording = RecordingIO.load(file);
                        playback.play(recording);
                        close();
                    })
                    .dimensions(LEFT, rowY, 230, 20)
                    .build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Delete"), b -> {
                        try {
                            Files.deleteIfExists(file);
                        } catch (IOException ignored) {
                        }
                        clearAndInit();
                    })
                    .dimensions(LEFT + 235, rowY, 65, 20)
                    .build());
        }
        y += shown * 22 + 6;

        if (savedRecordings.isEmpty()) {
            y += 16; // room for the "no recordings yet" line drawn in render()
        } else if (savedRecordings.size() > MAX_LISTED_RECORDINGS) {
            addDrawableChild(ButtonWidget.builder(Text.translatable("replayfx.menu.browse"), b -> {
                        close();
                        MinecraftClient.getInstance().setScreen(new RecordingsScreen(playback));
                    })
                    .dimensions(LEFT, y, 230, 20)
                    .build());
            y += 26;
        }

        addDrawableChild(ButtonWidget.builder(Text.translatable("replayfx.menu.close"), b -> close())
                .dimensions(LEFT, y + 8, 100, 20)
                .build());
    }

    private String shortLabel(Path file) {
        String name = file.getFileName().toString();
        return name.length() > 32 ? name.substring(0, 29) + "..." : name;
    }

    private Text recordButtonLabel() {
        return recorder.isRecording()
                ? Text.translatable("replayfx.menu.recording.stop")
                : Text.translatable("replayfx.menu.recording.start");
    }

    private Text playbackButtonLabel() {
        if (playback.isPlaying()) return Text.translatable("replayfx.menu.playback.stop");
        return lastRecording != null
                ? Text.translatable("replayfx.menu.playback.start")
                : Text.translatable("replayfx.menu.playback.none");
    }

    private Text freeCamButtonLabel() {
        return freeCam.isActive()
                ? Text.translatable("replayfx.menu.freecam.stop")
                : Text.translatable("replayfx.menu.freecam.start");
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFF);

        drawSection(context, recordingSectionY,
                "replayfx.menu.recording.header",
                "replayfx.menu.recording.desc");

        drawSection(context, playbackSectionY,
                "replayfx.menu.playback.header",
                "replayfx.menu.playback.desc");

        drawSection(context, freeCamSectionY,
                "replayfx.menu.freecam.header",
                "replayfx.menu.freecam.desc");

        context.drawTextWithShadow(textRenderer, Text.translatable("replayfx.menu.saved.header"),
                LEFT, recordingsListHeaderY - 12, HEADER_COLOR);

        if (savedRecordings.isEmpty()) {
            context.drawTextWithShadow(textRenderer, Text.translatable("replayfx.menu.saved.empty"),
                    LEFT, recordingsListHeaderY + 4, TEXT_COLOR);
        }
    }

    private void drawSection(DrawContext context, int sectionY, String headerKey, String descKey) {
        context.drawTextWithShadow(textRenderer, Text.translatable(headerKey), LEFT, sectionY - 12, HEADER_COLOR);
        context.drawTextWithShadow(textRenderer, Text.translatable(descKey), LEFT, sectionY + 24, TEXT_COLOR);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
