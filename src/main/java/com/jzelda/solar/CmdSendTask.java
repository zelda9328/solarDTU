/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jzelda.solar;

import com.jzelda.math.crc.CRC16_IBM;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author engin
 */
public class CmdSendTask extends TimerTask{
    private final static byte[] SepareateFlag = SendCmd.SepareateFlag;
    private final static byte[] EndFlag = SendCmd.EndFlag;
    private final static byte[] MsgTimeDelay = SendCmd.MsgTimeDelay;
    
    private final long delayTime = 3000;
    private final long tolerableGap = 5*60*1000;
    ByteBuffer cmdComplete;
    Boolean holdReg;
    byte volType;
    byte condicton;
    
    CmdSendTask(){
        cmdComplete = ByteBuffer.allocate(1024);
        holdReg = true;
        condicton = volType = 0x30;
    }

    @Override
    public void run() {
        Env.checkIdleConnection();
        /*
        Set<SelectionKey> skey = Usr.getSelector().keys();
        Calendar nowTime = Calendar.getInstance();
        Env.logger.info("ready to check idle connection");
        for(SelectionKey k : skey){
            Object o = k.attachment();
            if(o instanceof DTU_Handler){
                Calendar lastTime = ((DTU_Handler) o).lastTime;
                long gap = nowTime.getTimeInMillis() - lastTime.getTimeInMillis();
                if(gap > tolerableGap){
                    try {
                        ((DTU_Handler) o).socket.close();
                    } catch (IOException ex) {
                        Env.logger.warn("can't close unexpected socket");
                    }
                }
            }
        }
        */
        Env.logger.info("execute cmd send");
        if(Env.factories != null){            
            byte[] cmdArray = getCmd();
            int max = Env.getMAXamount();
            StringBuilder cmds = new StringBuilder();
            for(int i=1; i<=max; i++){
                cmds.append(createCmd(i, cmdArray));
                cmds.append(new String(MsgTimeDelay));
                cmds.append(new String(new byte[]{0x03}));
            }
            
            Env.getInstance().writePipe(cmds.toString());
        }
    }
    
    private byte[] getCmd(){
        Calendar cal = Calendar.getInstance();
        byte[] returnData = null;
        
        SimpleDateFormat sdf = new SimpleDateFormat ("HH");
        String hour = sdf.format(cal.getTime());
        int hour_int = Integer.parseInt(hour);
        
        if(hour_int == 1 ){
            Env.logger.info("send cmd is belong get history power");
            returnData = new byte[]{0x04,0x08,0x01,0x00,0x02};
            //returnData = new byte[]{0x04,0x08,0x13,0x00,0x02};
        } else{
            holdReg = !holdReg;
            if(holdReg){
                Env.logger.info("send cmd is belong setting holdreg value: " + volType);
                byte[] tmpData = {0x06,0x03,0x1f,0x00};
                ArrayList<Byte> byteObj = new ArrayList<Byte>();
                for(byte b : tmpData){
                    byteObj.add(b);
                }
                byteObj.add(volType);
                volType ^=  condicton;
                
                int i = 0;
                returnData = new byte[byteObj.size()];
                for(Byte B : byteObj){
                    returnData[i] = B;
                    i++;
                }
            } else{
                Env.logger.info("send cmd is belong get serial");
                returnData = new byte[]{0x04,0x04,0x1f,0x00,0x1B};
            }
        }
        
        return returnData;
    }
    
    private void sendCmd(int id, byte[] cmdCpn){
        for(FactoryMember m : Env.factories){
            if(id > m.inverterIdList.size())   continue;

            Env.logger.info("send to: "+m.name+" id: "+id);            
            cmdComplete.clear();
                    //byte[] req = {0x06, 0x03, (byte)0x1f, 0x30};
            cmdComplete.put((byte)id);
            cmdComplete.put(cmdCpn);
            cmdComplete.flip();
                    
            byte[] modbus1 = new byte[cmdComplete.remaining()];
            cmdComplete.get(modbus1, 0, modbus1.length);
            int crc1 = CRC16_IBM.getCRC(modbus1);
            byte crcH1 = (byte)((crc1 & 0xff00) >> 8);        
            byte crcL1 = (byte)(crc1 & 0xff);
                    
            cmdComplete.limit(cmdComplete.capacity());
            cmdComplete.put(crcL1);
            cmdComplete.put(crcH1);
            cmdComplete.flip();
            
            Boolean isComplete = Env.socketWriteAgent(m.socket, cmdComplete);
            /*
            while(cmdComplete.hasRemaining()){
                try {
                    if(m.socket != null && m.socket.isOpen()){
                        m.socket.write(cmdComplete);
                    }
                    else{
                        Env.logger.info(m.name + " socket Not association yet.");
                        isComplete = false;
                        break;
                    }
                } catch (IOException ex) {
                    try {
                        isComplete = false;
                        m.socket.close();
                        Env.logger.info(m.name + " socket association close.");
                        m.socket = null;
                    } catch (IOException ex1) {
                    } finally{
                        break;
                    }
                }
            }
            */
            if(isComplete)  Env.getBatchRecord(m.name).startTimer();
            
            try {
                Thread.sleep(delayTime);
            } catch (InterruptedException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
        
            
    }
    
    /**
     * 避免CRC與結束字串重覆
     * 此方法未計算
     * @param id inverter id
     * @param cmdCpn
     * @return 
     */
    private StringBuilder createCmd(int id, byte[] cmdCpn){
        StringBuilder returnVal = new StringBuilder();
        for(FactoryMember m : Env.factories){
            if(id > m.inverterIdList.size())   continue;

            Env.logger.info("create command prefix: "+m.name+" id: "+id);            
            cmdComplete.clear();
            cmdComplete.put(m.name.getBytes());
            cmdComplete.put(SepareateFlag);
            cmdComplete.put((byte)id);
            cmdComplete.put(cmdCpn);
            cmdComplete.put(EndFlag);
            cmdComplete.flip();
            byte[] tmp = new byte[cmdComplete.limit() - cmdComplete.position()];
            cmdComplete.get(tmp, 0, tmp.length);
            returnVal.append(new String(tmp));
            
            Env.getBatchRecord(m.name).startTimer();
        }
        return returnVal;
    }
}
