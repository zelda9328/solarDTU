/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jzelda.solar.pattern;

/**
 *
 * @author engin
 */
public abstract class DeltaInverterModbus extends ModbusModel{

    abstract public void parseField();
    
    protected int count2fieldPow(byte[] pow){
        double sum=0;
        if(pow.length != 4){
            return -1;
        }
        
        for(int i=1; i<pow.length;i+=2){            
            double rate = Math.pow(16, (i-1)*2);
            sum += (Byte.toUnsignedInt(pow[i]))* rate;
            sum += (Byte.toUnsignedInt(pow[i-1]))* rate * 16*16;
        }
        return (int)sum;
    }
    
    public int getInverterNo(){
        return super.inverterNo;
    }
}
