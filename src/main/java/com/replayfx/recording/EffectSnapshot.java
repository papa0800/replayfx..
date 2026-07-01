package com.replayfx.recording;

/**
 * A lightweight, serializable snapshot of one active status effect at the
 * moment a frame was captured. We store the packed color instead of a live
 * StatusEffect reference so recordings stay independent of registry state
 * and are trivial to (de)serialize with Gson.
 */
public class EffectSnapshot {
    public String effectId;   // e.g. "minecraft:speed"
    public int amplifier;     // 0 = level I
    public int duration;      // ticks remaining, informational only
    public int color;         // packed RGB, used to tint playback particles

    public EffectSnapshot() {
        // no-arg constructor required by Gson
    }

    public EffectSnapshot(String effectId, int amplifier, int duration, int color) {
        this.effectId = effectId;
        this.amplifier = amplifier;
        this.duration = duration;
        this.color = color;
    }
}
