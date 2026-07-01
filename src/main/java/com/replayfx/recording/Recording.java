package com.replayfx.recording;

import java.util.ArrayList;
import java.util.List;

/** A full recorded session: metadata + the ordered list of frames. */
public class Recording {
    public String name;
    public long createdAtEpochMillis;
    public String dimension;
    public List<Frame> frames = new ArrayList<>();

    public Recording() {
        // no-arg constructor required by Gson
    }

    public Recording(String name, String dimension) {
        this.name = name;
        this.dimension = dimension;
        this.createdAtEpochMillis = System.currentTimeMillis();
    }
}
