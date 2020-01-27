package io.github.uclarocketproject.daqd.json;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DaqConfigJson {
    public String logPath = "ROTATE";
    public Map<String, DeviceJson> devices = new HashMap<>();
    public Map<String, List<ItemJson>> items = new HashMap<>();
}
