package com.replayfx;

import com.replayfx.camera.FreeCamController;
import com.replayfx.gui.RecordingsScreen;
import com.replayfx.gui.ReplayFxMenuScreen;
import com.replayfx.playback.PlaybackController;
import com.replayfx.recording.Recorder;
import com.replayfx.recording.Recording;
import com.replayfx.recording.RecordingIO;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class ReplayFxClient implements ClientModInitializer {
    private final Recorder recorder = new Recorder();
    private final PlaybackController playback = new PlaybackController();
    private final FreeCamController freeCam = new FreeCamController();

    private KeyBinding toggleRecordKey;
    private KeyBinding togglePlaybackKey;
    private KeyBinding openScreenKey;
    private KeyBinding toggleFreeCamKey;
    private KeyBinding openMenuKey;

    // Remembers the last thing we saved this session so the "quick playback"
    // key has something to play without opening the list screen.
    private Recording lastRecording;

    @Override
    public void onInitializeClient() {
        recorder.init();

        toggleRecordKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.replayfx.toggle_record",
                Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "key.category.replayfx"
        ));

        togglePlaybackKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.replayfx.toggle_playback",
                Type.KEYSYM,
                GLFW.GLFW_KEY_T,
                "key.category.replayfx"
        ));

        openScreenKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.replayfx.open_screen",
                Type.KEYSYM,
                GLFW.GLFW_KEY_Y,
                "key.category.replayfx"
        ));

        toggleFreeCamKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.replayfx.toggle_freecam",
                Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "key.category.replayfx"
        ));

        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.replayfx.open_menu",
                Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "key.category.replayfx"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(MinecraftClient client) {
        // Recording toggle
        while (toggleRecordKey.wasPressed()) {
            if (recorder.isRecording()) {
                Recording finished = recorder.stop();
                RecordingIO.save(finished);
                lastRecording = finished;
                sendFeedback(client, Text.translatable("replayfx.recording.stopped", finished.frames.size()));
            } else {
                recorder.start();
                sendFeedback(client, Text.translatable("replayfx.recording.started"));
            }
        }

        // Quick playback of the last recording made this session
        while (togglePlaybackKey.wasPressed()) {
            if (playback.isPlaying()) {
                playback.stop();
                sendFeedback(client, Text.translatable("replayfx.playback.stopped"));
            } else if (lastRecording != null) {
                playback.play(lastRecording);
                sendFeedback(client, Text.translatable("replayfx.playback.started"));
            }
        }

        // Open the recordings browser directly
        while (openScreenKey.wasPressed()) {
            client.setScreen(new RecordingsScreen(playback));
        }

        // Free camera toggle
        while (toggleFreeCamKey.wasPressed()) {
            freeCam.toggle(client.player);
            sendFeedback(client, freeCam.isActive()
                    ? Text.translatable("replayfx.freecam.started")
                    : Text.translatable("replayfx.freecam.stopped"));
        }

        // Open the full settings/help hub
        while (openMenuKey.wasPressed()) {
            client.setScreen(new ReplayFxMenuScreen(recorder, playback, freeCam, lastRecording,
                    finished -> lastRecording = finished));
        }

        // Safety net: if the player entity disappears (disconnect, dimension
        // change edge case) while free cam is on, don't leave abilities in a
        // weird state next time they join.
        if (client.player == null) {
            freeCam.forceDisable(null);
        }

        playback.tick(client);
    }

    private void sendFeedback(MinecraftClient client, Text text) {
        if (client.player != null) {
            client.player.sendMessage(text, true);
        }
    }
}
