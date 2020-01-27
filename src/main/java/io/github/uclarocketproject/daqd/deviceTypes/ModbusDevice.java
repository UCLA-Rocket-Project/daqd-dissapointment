package io.github.uclarocketproject.daqd.deviceTypes;

import com.ghgande.j2mod.modbus.facade.ModbusSerialMaster;
import com.ghgande.j2mod.modbus.util.SerialParameters;
import io.github.uclarocketproject.daqd.config.DaqDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.ghgande.j2mod.modbus.net.AbstractSerialConnection.NO_PARITY;

public class ModbusDevice extends DaqDevice {
    static ModbusSerialMaster mb;
    static Logger log = LoggerFactory.getLogger("modbus");
    public ModbusDevice(String params) {
        super(params);
        if(mb == null) {
            String device = params;
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
    public double poll(String idStr) {
        idStr = idStr.toUpperCase();
        String[] segments = idStr.split("|");

        int deviceId = Integer.parseInt(segments[0]);
        char mbObj = segments[1].charAt(0);
        int reg= Integer.parseInt(segments[2]);

        try {
            switch(mbObj) {
                case 'C':
                    return mb.readCoils(deviceId, reg, 1).getBit(0) ? 1 : 0;
                case 'D':
                    return mb.readInputDiscretes(deviceId, reg, 1).getBit(0) ? 1 : 0;
                case 'I':
                    return mb.readInputRegisters(deviceId, reg, 1)[0].getValue();
                case 'H':
                    return mb.readMultipleRegisters(deviceId, reg, 1)[0].getValue();
                default:
                    return Double.NEGATIVE_INFINITY;
            }
        }
        catch(Exception e) {
            log.error("Error sampling modbus: ", e);
            return Double.NEGATIVE_INFINITY;
        }
    }
}
