package io.github.uclarocketproject.daqd.deviceTypes;

import com.ghgande.j2mod.modbus.facade.ModbusSerialMaster;
import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.util.BitVector;
import com.ghgande.j2mod.modbus.util.SerialParameters;
import io.github.uclarocketproject.daqd.config.DaqDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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
    public int[] poll(String idStr) {
        idStr = idStr.toUpperCase();
        String[] segments = idStr.split("|");

        int deviceId = Integer.parseInt(segments[0]);
        char mbObj = segments[1].charAt(0);
        int reg = Integer.parseInt(segments[2]);
        int numReg = Integer.parseInt(segments[3]);

        try {
            int[] ret = new int[numReg];
            BitVector bitVec = null;
            if(mbObj == 'C') {
                bitVec = mb.readCoils(deviceId, reg, numReg);
            }
            else if(mbObj == 'D') {
                bitVec = mb.readInputDiscretes(deviceId, reg, numReg);
            }
            if(bitVec != null) {
                for(int i = 0; i < numReg; i++) {
                    ret[i] = bitVec.getBit(i) ? 1 : 0;
                }
                return ret;
            }
            InputRegister[] regs;
            if(mbObj == 'I') {
                regs = mb.readInputRegisters(deviceId, reg, numReg);
            }
            else if(mbObj == 'H') {
                regs = mb.readMultipleRegisters(deviceId, reg, numReg);
            }
            else {
                return new int[0];
            }
            for(int i = 0; i < numReg; i++) {
                ret[i] = regs[i].getValue();
            }
            return ret;
        }
        catch(Exception e) {
            log.error("Error sampling modbus: ", e);
            return new int[0];
        }
    }
}
