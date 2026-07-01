package com.replayfx.recording;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything ReplayFX needs to reconstruct one tick of gameplay for playback:
 * where the camera was, which way it was looking, which effects were active,
 * and whether a critical hit happened this tick (so we can re-spawn crit
 * particles later).
 */
public class Frame {
    public int tick;

    public double x;
    public double y;
    public double z;

    public float yaw;
    public float pitch;

    public boolean sprinting;
    public boolean sneaking;
    public boolean onGround;

    /** True if the player landed a critical hit on this tick. */
    public boolean crit;

    public List<EffectSnapshot> effects = new ArrayList<>();

    public Frame() {
        // no-arg constructor required by Gson
    }
}
