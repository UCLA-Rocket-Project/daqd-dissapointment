package io.github.uclarocketproject.daqd.deviceTypes;

import io.github.uclarocketproject.daqd.config.DaqDevice;

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
    public double poll(int id) {
        double phase = id / 5.0;
        double sine = Math.sin(System.currentTimeMillis() * phase / 1000.0);
        return amp*sine + offset;
    }
}
