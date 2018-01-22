/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jzelda.solar.pattern;

import com.jzelda.math.crc.CRC16_IBM;
import com.jzelda.solar.Env;
import com.jzelda.solar.FactoryMember;
import java.util.Arrays;

/**
 *
 * @author engin
 */
public abstract class ModbusModel extends DataModel{
    /**
     * 這個變數存放資料庫逆變器的編號ID
     * 不是自身機器上的ID
     */
    protected int inverterNo;
    protected String slaveName;
    protected byte[] modbusData;
    
    @Override
    public void analyze(){
        modbusData = Arrays.copyOfRange(data, 10, data.length-2);
        Boolean upshot = checkCRC(modbusData);
        if(!upshot){
            String data_str = Convert.toStringType(data);
            Env.getlogger().info("modbus data CRC check error.");
            Env.getlogger().debug(String.format("original data is: %s",data_str));
            return;
        }
        
        int modbusId = (int)data[10];
        byte[] slaveName_ori = Arrays.copyOfRange(data, 0, 9);
        slaveName = new String(slaveName_ori).trim();
        for(FactoryMember m : Env.getFactories()){
            if(m.getName().equals(slaveName)){
                inverterNo = m.mapId(modbusId);
            }
        }
    }
    
    @Override
    public String fromWhere(){
        return slaveName;
    }
    
    private Boolean checkCRC(byte[] modbus){
        Boolean upshot = true;
        
        int crc =  CRC16_IBM.getCRC(modbus);
        byte crcH = (byte)((crc & 0xff00) >> 8);
        byte crcL = (byte)(crc & 0xff);
        if(crcL != data[data.length-2]
                || crcH != data[data.length-1]){
            //crc is different
            upshot = false;
        }
        
        return upshot;
    }
    
    public int getInverterNo(){
        return inverterNo;
    }
}
