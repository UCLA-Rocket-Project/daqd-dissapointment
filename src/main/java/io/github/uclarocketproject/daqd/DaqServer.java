package io.github.uclarocketproject.daqd;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import io.github.uclarocketproject.daqd.config.DaqConfig;
import io.github.uclarocketproject.daqd.json.DaqConfigJson;
import io.github.uclarocketproject.daqd.json.PrintConfigJson;
import io.github.uclarocketproject.daqd.json.ReadJson;
import io.github.uclarocketproject.daqd.json.RecordingJson;
import org.newsclub.net.unix.AFUNIXServerSocket;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DaqServer extends AFUNIXServerSocket {
    SocketAddress addr;
    volatile boolean die = false;
    Logger log = LoggerFactory.getLogger("daq socket");
    Map<Class, SockHandler> handlers = new HashMap<>();
    private DaqConfig conf;
    private PollThread pollThread;
    private ReentrantReadWriteLock lock;

    DaqServer(DaqConfig conf, PollThread pollThread, ReentrantReadWriteLock lock) throws IOException {
        super();
        this.conf = conf;
        this.pollThread = pollThread;
        this.lock = lock;
        File sockFile = new File(System.getProperty("java.io.tmpdir"), "daqd.sock");
        addr = new AFUNIXSocketAddress(sockFile);
        bind(addr);
        bindHandlers();
        log.info("Bound socket to address: "+addr);
    }
    private void bindHandlers() {
        handlers.put(DaqConfigJson.class, this.handleConfig);
        handlers.put(ReadJson.class, this.handleRead);
        handlers.put(RecordingJson.class, this.handleRecording);
        handlers.put(PrintConfigJson.class, this.handlePrintConfig);
    }
    public void die() {
        die = true;
        try {
            conf.ensureClosedState(true);
        }
        catch (IOException e) {
            log.error("error closing config: ", e);
        }
        if(!this.isClosed()) {
            try {
                this.close();
            }
            catch (IOException e) {
                log.error("error closing server: ", e);
            }
        }
        log.info("Ready for death");
    }
    void listen() {
        log.info("Listening for clients");
        while (!die) {
            try {
                acceptSock();
            }
            catch(SocketException e) {
                log.error("Issue with the socket. Will return from listen");
                return;
            }
            catch (IOException e) {
                log.error("Error during socket acceptance: ", e);
            }
        }
        log.info("Server thread exited");
    }
    void acceptSock() throws IOException {
        AFUNIXSocket sock = accept();
        Scanner input = new Scanner(sock.getInputStream());
        try {
            log.info("New client connected");
            sock.setSoTimeout(1000);
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
        finally {
            input.close();
        }
    }
    String getResponse(String jsonLine) throws Exception {
        for(Map.Entry<Class, SockHandler> entry : handlers.entrySet()) {
            try {
                Object o = Main.mapper.readValue(jsonLine, entry.getKey());
                log.info("Request of type: "+entry.getKey());
                return entry.getValue().handle(o);
            }
            catch(JsonMappingException e) {
                //log.info("Was not a "+entry.getKey());
            }
            catch (IOException e) {
                throw e;
            }
        }
        throw new Exception("Could not find a handler");
    }
    SockHandler<DaqConfigJson> handleConfig = (DaqConfigJson arg) -> {
        lock.writeLock().lock();
        try {
            conf.loadConfig(arg);
            log.info("Changed configuration");
            conf.writeToFile();
            log.info("Flushed configuration");
        }
        catch(Exception e) {
            log.error("Error updating configuration", e);
        }
        String ret = conf.toString();
        lock.writeLock().unlock();
        return ret;
    };
    SockHandler<ReadJson> handleRead = (ReadJson arg) -> {
        lock.readLock().lock();
        String ret = Main.mapper.writeValueAsString(conf.items);
        lock.readLock().unlock();
        log.info("Completed read request");
        return ret;
    };
    SockHandler<PrintConfigJson> handlePrintConfig = (PrintConfigJson arg) -> {
        lock.readLock().lock();
        String ret = conf.toString();
        lock.readLock().unlock();
        log.info("Completed print config request");
        return ret;
    };
    SockHandler<RecordingJson> handleRecording = (RecordingJson arg) -> {
        lock.writeLock().lock();
        pollThread.setRecordingState(arg.recording);
        lock.writeLock().unlock();
        log.info("Recording state: "+arg.recording);
        return Main.mapper.writeValueAsString(arg);
    };
}
