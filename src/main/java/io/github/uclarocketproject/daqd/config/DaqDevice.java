package io.github.uclarocketproject.daqd.config;

import java.util.List;

public abstract class DaqDevice {
    String name;
    public PollMode pollMode;
    public long lastPoll;
    public long pollRate;
    public int pollIndex;
    public abstract int[] poll(String id);
    public DaqDevice(String params) {
    }
}
