package com.replayfx.recording;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.ActionResult;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Captures the local player's state once per client tick while recording is
 * active. Kept deliberately dumb: no interpolation or compression here, we
 * just append a Frame every tick and let playback smooth things out.
 */
public class Recorder {
    private boolean recording = false;
    private Recording current;
    private int tickCounter;

    // Set to true for exactly one tick after the player lands an attack that
    // qualifies as a critical hit (falling, not sprinting, not on a ladder,
    // no blindness -- the same rough conditions vanilla uses to spawn crit
    // particles client-side).
    private final AtomicBoolean critThisTick = new AtomicBoolean(false);

    public void init() {
        // Flag crits the tick an attack happens; consumed on the next capture.
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient() && isLikelyCrit(player)) {
                critThisTick.set(true);
            }
            return ActionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (recording && client.player != null) {
                captureFrame(client);
            }
        });
    }

    private boolean isLikelyCrit(net.minecraft.entity.player.PlayerEntity player) {
        // Simplified version of vanilla's crit condition: airborne (falling),
        // not sprinting, not climbing, no blindness/levitation weirdness.
        return player.fallDistance > 0.0F
                && !player.isOnGround()
                && !player.isClimbing()
                && !player.isSprinting()
                && !player.hasVehicle();
    }

    public boolean isRecording() {
        return recording;
    }

    /** Starts a new recording, discarding any unsaved in-progress one. */
    public void start() {
        current = new Recording("recording-" + System.currentTimeMillis(),
                MinecraftClient.getInstance().player != null
                        ? MinecraftClient.getInstance().player.getEntityWorld().getRegistryKey().getValue().toString()
                        : "unknown");
        tickCounter = 0;
        recording = true;
    }

    /** Stops recording and returns the finished Recording (never null after start()). */
    public Recording stop() {
        recording = false;
        Recording finished = current;
        current = null;
        return finished;
    }

    private void captureFrame(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        Frame frame = new Frame();
        frame.tick = tickCounter++;
        frame.x = player.getX();
        frame.y = player.getY();
        frame.z = player.getZ();
        frame.yaw = player.getYaw();
        frame.pitch = player.getPitch();
        frame.sprinting = player.isSprinting();
        frame.sneaking = player.isSneaking();
        frame.onGround = player.isOnGround();
        frame.crit = critThisTick.getAndSet(false);

        for (StatusEffectInstance instance : player.getStatusEffects()) {
            int color = instance.getEffectType().value().getColor();
            frame.effects.add(new EffectSnapshot(
                    instance.getEffectType().getIdAsString(),
                    instance.getAmplifier(),
                    instance.getDuration(),
                    color
            ));
        }

        current.frames.add(frame);
    }
}
