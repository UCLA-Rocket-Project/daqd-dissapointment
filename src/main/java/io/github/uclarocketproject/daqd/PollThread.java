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
    final long MEM_UPDATE_TIME = 30*1000;
    long lastMem = 0;
    public PollThread(DaqConfig config, ReentrantReadWriteLock lock) {
        conf = config;
        this.lock = lock;
    }
    @Override
    public void run() {
        while(true) {
            if (!recording) {
                continue;
            }
            try {
                pollDevices();
            }
            catch (Exception e) {
                log.error("Issue in poll loop: ", e);
            }
            long now = System.currentTimeMillis();
            if(now > lastMem + MEM_UPDATE_TIME) {
                log.info("MEM USAGE: "+( (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024 )+ " MB");
                lastMem = now;
            }
        }
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
            if(delta > 5) {
                log.warn("Missed target update rate for "+devName+" by "+delta+" ms");
            }
            log.debug("Polling for: "+devName);
            if(dev.pollMode == PollMode.SINGLE_SHOT) {
                for(DaqItem item : items) {
                    item.updateVal(dev.poll(item.id));
                    log.debug("Updated "+item.name);
                }
                conf.logItems(items);
            }
            else if(dev.pollMode == PollMode.ROUND_ROBIN) {
                DaqItem item = items.get(dev.pollIndex);
                double val = dev.poll(item.id);
                if(!Double.isNaN(val)) {
                    item.updateVal(val);
                    conf.logItem(item);
                    dev.pollIndex = (dev.pollIndex+1) % items.size();
                    log.debug("Updated "+item.name);
                }
            }
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
