package io.github.uclarocketproject.daqd.config;

import io.github.uclarocketproject.daqd.Main;
import io.github.uclarocketproject.daqd.json.DaqConfigJson;
import io.github.uclarocketproject.daqd.json.DeviceJson;
import io.github.uclarocketproject.daqd.json.ItemJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class DaqConfig {
    public PrintWriter writer;
    public Map<String, DaqDevice> devices = new HashMap<>();
    public Map<String, List<DaqItem>> items = new HashMap<>();
    public DaqConfigJson configJson;
    boolean writerClosed = false;
    Logger log = LoggerFactory.getLogger("DaqConfig");
    private File backingFile;

    public DaqConfig(DaqConfigJson json) throws NoSuchMethodException, InstantiationException, IOException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        backingFile = new File("DaqConfig.json");
        loadConfig(json);
    }
    public DaqConfig(File file) throws IOException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        backingFile = file;
        DaqConfigJson json = Main.mapper.readValue(file, DaqConfigJson.class);
        loadConfig(json);
    }

   public void loadConfig(DaqConfigJson configJson) throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        boolean prevClosed = isClosed();
        ensureClosedState(true);
        this.configJson = configJson;
        openWriter();

        for(DaqDevice dev : devices.values()) {
            dev.close();
        }
        devices.clear();
        for (Map.Entry<String, DeviceJson> entry : configJson.devices.entrySet()) {
            DaqDevice dev = loadDevice(entry.getValue().className, entry.getValue().params);
            dev.pollRate = entry.getValue().pollRate;
            dev.pollMode = entry.getValue().pollMode;
            devices.put(entry.getKey(), dev);
        }
       items.clear();
       for(Map.Entry<String, List<ItemJson>> entry : configJson.items.entrySet()) {
            List<DaqItem> itemList = new LinkedList<>();
            for(ItemJson it : entry.getValue()) {
                itemList.add(new DaqItem(it));
            }
            items.put(entry.getKey(), itemList);
        }
        ensureClosedState(prevClosed);
        log.info("New config fully loaded: "+this);
    }
    public void writeToFile() throws IOException {
        Main.mapper.writeValue(backingFile, configJson);
    }
    DaqDevice loadDevice(String className, String params) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class<DaqDevice> devClass = (Class<DaqDevice>) Class.forName(className);
        Constructor<DaqDevice> ctor = devClass.getConstructor(String.class);
        return ctor.newInstance(params);
    }
    public boolean isClosed() {
        return writerClosed;
    }
    public void ensureClosedState(boolean closed) throws IOException {
        if(closed == writerClosed) {
            return;
        }
        if(!closed) {
            openWriter();
        }
        else {
            if(writer != null) {
                writer.close();
            }
            log.info("Closed log file");
            writerClosed = true;
        }
    }
    public void logItem(DaqItem item) {
        printItem(item);
        writer.flush();
    }
    public void logItems(List<DaqItem> items) {
        for(DaqItem item : items) {
            printItem(item);
        }
        writer.flush();
    }
    private void printItem(DaqItem item) {
        writer.print(item.name);
        writer.print('|');
        for(int i = 0; i < item.values.length; i++) {
            writer.print(item.values[i]);
            if(i != item.values.length - 1) {
                writer.print(',');
            }
        }
        writer.print('|');
        writer.println(item.lastUpdate);
    }

    void openWriter() throws IOException {
        String logPath = configJson.logPath;
        if(logPath.contains("ROTATE")) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            dateFormat.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
            String dateFmt = dateFormat.format(new Date());
            logPath = logPath.replace("ROTATE", dateFmt);
            configJson.logPath = logPath;
        }
        File logFile = new File(logPath);
        logFile.getParentFile().mkdirs();
        FileWriter fw = new FileWriter(logFile, true);
        writer = new PrintWriter(fw);
        log.info("Opened log file @ "+logPath);
        writerClosed = false;
    }

    @Override
    public String toString() {
        try {
            return Main.mapper.writeValueAsString(this.configJson);
        }
        catch (IOException e) {
            return "howtf";
        }
    }
}
