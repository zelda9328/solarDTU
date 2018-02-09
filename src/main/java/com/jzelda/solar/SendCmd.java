/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jzelda.solar;

import com.jzelda.math.crc.CRC16_IBM;
import com.jzelda.solar.pattern.Convert;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author engin
 */
public class SendCmd implements Runnable{
    public final static byte[] SepareateFlag = {0x7d,0x7d,0x7d,0x7d};
    public final static byte[] EndFlag = {0x7f,0x7f,0x7f,0x7f};
    public final static byte[] MsgTimeDelay = {0x7e,0x7e,0x7e};
    public final static byte Vacancy = 0x20;
    private ByteBuffer bf;

    //至此，待發送的指令格式應為
    //電廠站名+SepareateFlag+指令(不含CRC)+EndFlag...+MsgTimeDelay+秒數
    @Override
    public void run() {
        bf = ByteBuffer.allocate(1024);
        while(!Thread.interrupted()){
            byte[] transTmp = Env.getInstance().readPipe();
            String msg_str = new String(transTmp);
            
            String s = Convert.toStringType(transTmp);
            //System.out.println(s);
            String infout = String.format("message:\n%s", s);
            Env.logger.info(infout);
            
            int firstPos = msg_str.indexOf(new String(MsgTimeDelay));
            String timeDelay_str = msg_str.substring(firstPos, firstPos + MsgTimeDelay.length +1 );
            char sleepTime = timeDelay_str.charAt(timeDelay_str.length()-1);
            
            String[] modbusCmds = msg_str.split(timeDelay_str);
            Env.logger.info(Convert.toStringType(modbusCmds[0].getBytes()));
            for(String cmds : modbusCmds){
                send(cmds);
                try {
                    Thread.sleep(sleepTime * 1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SendCmd.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    private void send(String cmds){
        String[] cmd_with_stationName = cmds.split(new String(EndFlag));
        for(String cmd : cmd_with_stationName){
            String[] actualCmd = cmd.split(new String(SepareateFlag));
            if(actualCmd.length != 2){
                Env.logger.info("the command format that waiting to send is incorrect.");
                return;
            }
            
            for(FactoryMember m : Env.getFactories()){
                if(!m.name.equals(actualCmd[0])) continue;
                
                byte[] cmd_byte = actualCmd[1].getBytes();
                int crc1 = CRC16_IBM.getCRC(cmd_byte);
                byte crcH1 = (byte)((crc1 & 0xff00) >> 8);        
                byte crcL1 = (byte)(crc1 & 0xff);
                
                bf.clear();
                bf.put(cmd_byte);
                bf.put(crcL1);
                bf.put(crcH1);
                bf.flip();
                
                byte[] msg = new byte[bf.remaining()];
                bf.get(msg);
                Env.logger.info(String.format("%s: socket send: %s",actualCmd[0], Convert.toStringType(msg)));
                bf.flip();
                Env.socketWriteAgent(m.socket, bf);
            }
        }
    }
}
