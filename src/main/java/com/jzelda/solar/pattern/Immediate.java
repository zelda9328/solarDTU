/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jzelda.solar.pattern;

import com.jzelda.math.crc.CRC16_IBM;
import com.jzelda.solar.Env;
import com.jzelda.solar.FactoryMember;
import java.math.BigDecimal;
import java.util.Arrays;

/**
 *
 * @author engin
 */
public class Immediate extends DeltaInverterModbus{
    int hold;
    float voltage;
    float current;
    float freq;
    int watt;
    int powerToday;
    int ampTemp;
    int boostHsTemp;
    int invHsTemp;

    @Override
    public void parseField() {
        //這是為修正之前程式碼截取的起始位址
        int revise = 3;
        
        hold = Convert.byte2int(modbusData[revise+0], modbusData[revise+1]);
        BigDecimal a = new BigDecimal(Convert.byte2int(modbusData[revise+2], modbusData[revise+3]));
        BigDecimal b = new BigDecimal("0.1");
        voltage = a.multiply(b).floatValue();
        a = new BigDecimal(Convert.byte2int(modbusData[revise+4], modbusData[revise+5]));
        b = new BigDecimal("0.01");
        current = a.multiply(b).floatValue();
        watt = Convert.byte2int(modbusData[revise+6], modbusData[revise+7]);
        a = new BigDecimal(Convert.byte2int(modbusData[revise+8], modbusData[revise+9]));
        freq = a.multiply(b).floatValue();

        byte[] power_array = {modbusData[revise+32], modbusData[revise+33], modbusData[revise+34], modbusData[revise+35]};
        powerToday = count2fieldPow(power_array)*10;
        ampTemp = Convert.byte2int(modbusData[revise+48], modbusData[revise+49]);
        boostHsTemp = Convert.byte2int(modbusData[revise+50], modbusData[revise+51]);
        invHsTemp = Convert.byte2int(modbusData[revise+52], modbusData[revise+53]);
    }

    @Override
    public Object[] getDataSet() {
        Object[] values = {(int)modbusData[0], hold, voltage, current, watt, freq, watt, ampTemp, boostHsTemp,invHsTemp};
        return values;
    }
}
