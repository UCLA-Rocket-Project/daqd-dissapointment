package io.github.uclarocketproject.daqd.config;

public abstract class DaqDevice {
    String name;
    public PollMode pollMode;
    public long lastPoll;
    public long pollRate;
    public int pollIndex;
    public abstract double poll(int id);
    public DaqDevice(String params) {
    }
}
