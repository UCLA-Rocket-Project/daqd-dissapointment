package io.github.uclarocketproject.daqd.config;

import io.github.uclarocketproject.daqd.json.ItemJson;

public class DaqItem {
    public String name;
    public String id;
    public double value;
    public double offset;
    public double scale;
    public long lastUpdate;

    public DaqItem(ItemJson json) {
        name = json.name;
        id = json.id;
        offset = json.offset;
        scale = json.scale;
    }

    public void updateVal(double newVal) {
        value = (newVal + offset) * scale;
        lastUpdate = System.currentTimeMillis();
    }
}
