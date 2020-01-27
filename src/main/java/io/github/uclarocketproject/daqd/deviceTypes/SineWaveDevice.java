package io.github.uclarocketproject.daqd.deviceTypes;

import io.github.uclarocketproject.daqd.config.DaqDevice;

import java.util.LinkedList;
import java.util.List;

public class SineWaveDevice extends DaqDevice {
    double amp;
    double offset;
    public SineWaveDevice(String params) {
        super(params);
        String[] tokens = params.split("\\+");
        this.amp = Double.parseDouble(tokens[0]);
        this.offset = Double.parseDouble(tokens[1]);
    }
    @Override
    public int[] poll(String idStr) {
        int id = Integer.parseInt(idStr);
        double phase = id / 5.0;
        double sine = Math.sin(System.currentTimeMillis() * phase / 1000.0);
        int[] arr = new int[1];
        arr[0] = (int)(sine * 100);
        return arr;
    }
}
