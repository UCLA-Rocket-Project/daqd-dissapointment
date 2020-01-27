package io.github.uclarocketproject.daqd;

import io.github.uclarocketproject.daqd.config.DaqConfig;
import io.github.uclarocketproject.daqd.config.DaqDevice;
import io.github.uclarocketproject.daqd.config.DaqItem;
import io.github.uclarocketproject.daqd.config.PollMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class PollThread extends Thread {
    private volatile boolean recording = false;
    Logger log = LoggerFactory.getLogger("poll thread");
    private volatile DaqConfig conf;
    private volatile ReentrantReadWriteLock lock;
    private volatile boolean die = false;
    public PollThread(DaqConfig config, ReentrantReadWriteLock lock) {
        conf = config;
        this.lock = lock;
    }
    public void die() {
        try {
            conf.ensureClosedState(true);
        }
        catch (IOException e) {
            log.error("error closing config: ", e);
        }
        this.die = true;
        log.info("Ready for death");
    }
    @Override
    public void run() {
        while(!die) {
            if (recording) {
                try {
                    pollDevices();
                }
                catch (Exception e) {
                    log.error("Issue in poll loop: ", e);
                }
            }
        }
        log.info("Poll thread closed");
    }
    void pollDevices() throws Exception {
        lock.readLock().lock();
        long now = System.currentTimeMillis();
        for(Map.Entry<String, DaqDevice> entry : conf.devices.entrySet()) {
            String devName = entry.getKey();
            DaqDevice dev = entry.getValue();
            List<DaqItem> items = conf.items.get(devName);
            if(now < dev.lastPoll + dev.pollRate) {
                continue;
            }
            long delta = now - (dev.lastPoll + dev.pollRate);
            if(dev.lastPoll != 0 && delta > 5) {
                log.warn("Missed target update rate for '"+devName+"' by "+delta+" ms");
            }
            log.debug("Polling for: "+devName);
            if(dev.pollMode == PollMode.SINGLE_SHOT) {
                for(DaqItem item : items) {
                    item.updateVals(dev.poll(item.id));
                    log.debug("Updated "+item.name);
                }
                conf.logItems(items);
            }
            else if(dev.pollMode == PollMode.ROUND_ROBIN) {
                DaqItem item = items.get(dev.pollIndex);
                int[] val = dev.poll(item.id);
                if(val.length != 0) {
                    item.updateVals(val);
                    conf.logItem(item);
                    dev.pollIndex = (dev.pollIndex+1) % items.size();
                    log.debug("Updated "+item.name);
                }
            }
            long sampleTime = System.currentTimeMillis() - now;
            log.debug("Time to sample "+devName+": "+sampleTime+" ms");
            dev.lastPoll = now;
        }
        lock.readLock().unlock();
    }
    void setRecordingState(boolean record) {
        this.recording = record;
        log.warn(String.format("Recording status: %b", this.recording));
        try {
            conf.ensureClosedState(!record);
        }
        catch(IOException e) {
            log.warn("Could not open log file");
        }
    }
}
