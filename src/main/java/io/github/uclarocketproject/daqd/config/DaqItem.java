package io.github.uclarocketproject.daqd.config;

import io.github.uclarocketproject.daqd.json.ItemJson;

import java.util.List;

public class DaqItem {
    public String name;
    public String id;
    public int[] values;
    public int offset;
    public int scale;
    public long lastUpdate;

    public DaqItem(ItemJson json) {
        name = json.name;
        id = json.id;
        offset = json.offset;
        scale = json.scale;
    }

    public void updateVals(int[] newVals) {
        if(values == null || values.length != newVals.length) {
            values = new int[newVals.length];
        }
        for(int i = 0; i < newVals.length; i++) {
            int newVal = newVals[i] * scale + offset;
            values[i] = newVal;
        }
        lastUpdate = System.currentTimeMillis();
    }
}
