package io.github.uclarocketproject.daqd;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.uclarocketproject.daqd.config.DaqConfig;
import io.github.uclarocketproject.daqd.json.DaqConfigJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Main {
    public static ObjectMapper mapper = new ObjectMapper();
    static Logger log = LoggerFactory.getLogger("daq main");
    static {
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }
    static ReentrantReadWriteLock lock;
    static DaqConfig config;
    static PollThread poller;
    static File confFile;
    static DaqServer server;
    public static void main(String[] args) {
        boolean debug = false;
        boolean auto = false;
        for(String arg : args) {
            if(arg.equals("debug")) {
                debug = true;
            }
            else if(arg.equals("auto")) {
                auto = true;
            }
        }
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(debug ? Level.DEBUG : Level.INFO);
        try {
            confFile = new File("DaqConfig.json");
            if(confFile.exists()) {
                log.info("Loading log from: "+confFile);
                config = new DaqConfig(confFile);
            }
            else {
                log.warn("No log file found! Initializing with empty default");
                config = new DaqConfig(new DaqConfigJson());
            }
            lock = new ReentrantReadWriteLock();
            poller = new PollThread(config, lock);
            poller.start();
            log.info("Spawned poller thread");
            poller.setRecordingState(auto);

            server = new DaqServer(config, poller, lock);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Stop signal caught");
                poller.die();
                server.die();
                log.warn("Goodbye!");
                Runtime.getRuntime().halt(0);
            }));

            server.listen();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}
