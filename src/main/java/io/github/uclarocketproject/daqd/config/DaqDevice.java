package io.github.uclarocketproject.daqd.config;

import java.io.Closeable;
import java.util.List;

public abstract class DaqDevice implements Closeable {
    String name;
    public PollMode pollMode;
    public long lastPoll;
    public long pollRate;
    public int pollIndex;
    public abstract int[] poll(String id);
    public DaqDevice(String params) {}
    @Override
    public void close() {}
}
