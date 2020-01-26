package io.github.uclarocketproject.daqd.deviceTypes;

import com.ghgande.j2mod.modbus.facade.ModbusSerialMaster;
import com.ghgande.j2mod.modbus.util.SerialParameters;
import io.github.uclarocketproject.daqd.config.DaqDevice;

import static com.ghgande.j2mod.modbus.net.AbstractSerialConnection.NO_PARITY;

public class ModbusDevice extends DaqDevice {
    static ModbusSerialMaster mb;
    int deviceId;
    public ModbusDevice(String params) {
        super(params);
        String[] tokens = params.split("\\+");
        this.deviceId = Integer.parseInt(tokens[0]);
        if(mb == null) {
            String device = tokens[1];
            SerialParameters serParams = new SerialParameters();
            serParams.setPortName(device);
            serParams.setBaudRate(115200);
            serParams.setDatabits(8);
            serParams.setParity(NO_PARITY);
            serParams.setStopbits(1);
            mb = new ModbusSerialMaster(serParams);
        }
    }
    @Override
    public double poll(int id) {
        int regType = id / 10000;
        int offset = id - regType * 10000;
        try {
            switch(regType) {
                case 0:
                    return mb.readCoils(deviceId, offset, 1).getBit(0) ? 1 : 0;
                case 1:
                    return mb.readInputDiscretes(deviceId, offset, 1).getBit(0) ? 1 : 0;
                case 2:
                    return mb.readInputRegisters(deviceId, offset, 1)[0].getValue();
                case 3:
                    return mb.readMultipleRegisters(deviceId, offset, 1)[0].getValue();
                default:
                    return Double.NEGATIVE_INFINITY;
            }
        }
        catch(Exception e) {
            e.printStackTrace();
            return Double.NEGATIVE_INFINITY;
        }
    }
}
