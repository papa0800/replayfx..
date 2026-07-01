package com.replayfx.playback;

import com.replayfx.recording.EffectSnapshot;
import com.replayfx.recording.Frame;
import com.replayfx.recording.Recording;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.particle.EntityEffectParticleEffect;
import net.minecraft.particle.ParticleTypes;

/**
 * Plays a {@link Recording} back by driving the client player's position and
 * rotation frame-by-frame and re-emitting the particles that were present
 * during recording (potion-effect swirls, crit sparks).
 * <p>
 * NOTE: this repositions the real player entity client-side, which is the
 * simplest thing that works reliably in singleplayer / spectator mode. A
 * fully detached free camera (so you can walk around while a "ghost" plays
 * back) needs a GameRenderer camera mixin -- see the README for pointers on
 * extending this.
 */
public class PlaybackController {
    private Recording recording;
    private int frameIndex;
    private boolean playing;

    // Remembers whether the player was in spectator-ish flight before playback
    // started so we don't mess with survival gameplay unexpectedly.
    private boolean wasFlying;

    public boolean isPlaying() {
        return playing;
    }

    public void play(Recording recording) {
        if (recording == null || recording.frames.isEmpty()) return;
        this.recording = recording;
        this.frameIndex = 0;
        this.playing = true;

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            wasFlying = player.getAbilities().flying;
            player.getAbilities().flying = true;
        }
    }

    public void stop() {
        playing = false;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            player.getAbilities().flying = wasFlying;
        }
        recording = null;
    }

    /** Call once per client tick. Advances playback and spawns effects. */
    public void tick(MinecraftClient client) {
        if (!playing || recording == null || client.player == null || client.world == null) return;

        if (frameIndex >= recording.frames.size()) {
            stop();
            return;
        }

        Frame frame = recording.frames.get(frameIndex);
        ClientPlayerEntity player = client.player;

        player.setPosition(frame.x, frame.y, frame.z);
        player.setYaw(frame.yaw);
        player.setPitch(frame.pitch);
        player.setHeadYaw(frame.yaw);

        for (EffectSnapshot effect : frame.effects) {
            client.world.addParticle(
                    new EntityEffectParticleEffect(ParticleTypes.ENTITY_EFFECT, effect.color),
                    frame.x + jitter(), frame.y + 1.0 + jitter(), frame.z + jitter(),
                    0.0, 0.05, 0.0
            );
        }

        if (frame.crit) {
            for (int i = 0; i < 6; i++) {
                client.world.addParticle(
                        ParticleTypes.CRIT,
                        frame.x + jitter(), frame.y + 1.0 + jitter(), frame.z + jitter(),
                        0.0, 0.0, 0.0
                );
            }
        }

        frameIndex++;
    }

    private double jitter() {
        return (Math.random() - 0.5) * 0.4;
    }
}
