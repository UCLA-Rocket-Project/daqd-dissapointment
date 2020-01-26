package io.github.uclarocketproject.daqd;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.uclarocketproject.daqd.config.DaqConfig;
import io.github.uclarocketproject.daqd.json.DaqConfigJson;
import io.github.uclarocketproject.daqd.json.ReadJson;
import org.newsclub.net.unix.AFUNIXServerSocket;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.Scanner;
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
    public static void main(String[] args) {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
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
            log.info("Initial config: "+mapper.writeValueAsString(config.items));
            poller.setRecordingState(true);
            openSocket();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    static AFUNIXServerSocket server;
    static void openSocket() throws IOException {
        File sockFile = new File(System.getProperty("java.io.tmpdir"), "daqd.sock");
        SocketAddress sockAddr = new AFUNIXSocketAddress(sockFile);
        server = AFUNIXServerSocket.newInstance();
        server.bind(sockAddr);
        log.info("Listening to file: "+sockFile);
        while(true) {
            acceptSock();
        }
    }
    static void acceptSock() throws IOException {
        AFUNIXSocket sock = server.accept();
        try (Scanner input = new Scanner(sock.getInputStream())) {
            log.info("New client connected");
            sock.setSoTimeout(5000);
            if (input.hasNextLine()) {
                String jsonLine = input.nextLine();
                String response = getResponse(jsonLine);
                sock.getOutputStream().write(response.getBytes());
            }
        }
        catch (Exception e) {
            log.error("Exception in handling socket", e);
            sock.getOutputStream().write(("{\"err\": \""+e.getMessage()+"\"}").getBytes());
        }
    }
    static String getResponse(String jsonLine) throws IOException {
        try {
            DaqConfigJson conf = mapper.readValue(jsonLine, DaqConfigJson.class);
            lock.writeLock().lock();
            boolean prevClosed = config.isClosed();
            config.ensureClosedState(true);
            try {
                config.loadConfig(conf);
                log.info("Changed configuration");
            }
            catch(Exception e) {
                log.error("Could not change the config: ", e);
            }
            config.writeToFile(confFile);
            config.ensureClosedState(prevClosed);
            String ret = mapper.writeValueAsString(config.configJson);
            lock.writeLock().unlock();
            return ret;
        }
        catch(JsonMappingException e) {
            try {
                ReadJson read = mapper.readValue(jsonLine, ReadJson.class);
                lock.readLock().lock();
                String ret = mapper.writeValueAsString(config.items);
                lock.readLock().unlock();
                log.info("Completed read request");
                return ret;
            }
            catch(JsonMappingException ex) {
                throw new IOException("invalid input json");
            }
        }
    }
}
