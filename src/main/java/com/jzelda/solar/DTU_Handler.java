/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jzelda.solar;

import com.jzelda.math.crc.CRC16_IBM;
import com.jzelda.solar.pattern.Convert;
import com.jzelda.solar.pattern.DataModel;
import com.jzelda.solar.pattern.Day1;
import com.jzelda.solar.pattern.Day30;
import com.jzelda.solar.pattern.HeartBeat;
import com.jzelda.solar.pattern.Immediate;
import com.jzelda.solar.pattern.ModbusFunc6;
import com.jzelda.solar.pattern.RegPack;
import com.jzelda.solar.pattern.UnDefined;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.DatatypeConverter;

/**
 *
 * @author engin
 */
public class DTU_Handler implements Runnable{
    static int baseDataLeng = 10;
    //function code 03 data length
    static int minLEng = 7;    
    final static int heartbeat = 3;
    final static int Func6 = 18;
    final static int YeserdaySize = 19;
    final static int Reg = 21;
    final static int Immediate = 69;
    final static int DAY30Size = 139;
    final static byte[] HeartBeatValue = {0x31, 0x32, 0x33};
    final static int ModbusLengPos = 12;
    final static int RegPackLeng = 10;
    final static int ModbusIdLeng = 1;
    final static int ModbusFuncLeng = 1;
    final static int ModbusInstructDataLeng = 1;
    final static int CRCLeng = 2;
    final static long CmdExpired = 300;
    
    SocketChannel socket;
    ByteBuffer buffer, dataBuf;
    String ip;
    byte[] regName = createRegName();
    Calendar lastTime;
    
    DTU_Handler(SocketChannel s){
        socket = s;
        try{
            ip = s.getRemoteAddress().toString().split("/")[1];
        } catch( IOException e){
            Env.logger.error("socket get address fail.");
        }
        buffer = ByteBuffer.allocate(1024);
        buffer.clear();
        dataBuf = ByteBuffer.allocate(4*1024);
        dataBuf.clear();
        
        lastTime = Calendar.getInstance();
    }

    @Override
    public void run(){
        Calendar nowTime = Calendar.getInstance();
        long timeGap = nowTime.getTimeInMillis() - lastTime.getTimeInMillis();
        //限定資料保存時限
        if(timeGap > CmdExpired){
            dataBuf.clear();
        }
        lastTime = nowTime;
        
        Env.socketReadAgnet(socket, buffer);        
        buffer.flip();
        
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data, 0, data.length);
        buffer.clear();
        
        Env.logger.debug(String.format("receive socket channel data: %s", Convert.toStringType(data)));
        
        //資料超過0x7f,直接轉字串會被換成0x3f,用16進制字串處理
        String msgHex = DatatypeConverter.printHexBinary(data);
        String trimFlag = DatatypeConverter.printHexBinary(regName);
        String[] trimReg = msgHex.split(trimFlag);
        String noRegNameCmd = "";
        for(String s : trimReg){
            noRegNameCmd = noRegNameCmd.concat(s);
        }
        byte[] noRegData = DatatypeConverter.parseHexBinary(noRegNameCmd);
        
        if(data.length != Reg){
            if( !Arrays.equals(data, HeartBeatValue)){
                if(dataBuf.position() != 0){
                    dataBuf.flip();
                    byte[] lastData = new byte[dataBuf.remaining()];
                    dataBuf.get(lastData);
                    dataBuf.limit(dataBuf.capacity());

                    Env.logger.debug(String.format("last remaining data: %s", Convert.toStringType(lastData)));
                    Env.logger.debug(String.format("receive data: %s", Convert.toStringType(noRegData)));
                    dataBuf.put(noRegData, 0, noRegData.length);
                } else{
                    dataBuf.put(noRegData);
                }

                int modbusDataLeng = (int)dataBuf.get(ModbusLengPos-10);
                int fullMOdbusLeng = ModbusIdLeng + ModbusFuncLeng
                        + ModbusInstructDataLeng + modbusDataLeng + CRCLeng;
                if( dataBuf.position() >= fullMOdbusLeng){
                    dataBuf.flip();
                    data = new byte[dataBuf.remaining() + RegPackLeng];
                    System.arraycopy(regName, 0, data, 0, regName.length);
                    dataBuf.get(data, RegPackLeng, dataBuf.remaining());
                    dataBuf.clear();

                } else return;
            }

        }
        
        Env.logger.debug(String.format("data class context: %s",Convert.toStringType(data)));
        DataModel model = getDataModel(data);
        
        
        model.analyze();
        String factoryName = model.fromWhere();
        Env.logger.info("receive data type class: " + model.getClass().getSimpleName());
        Env.logger.info(String.format("%s, the contenet is: %s",factoryName, Convert.toStringType(data)));
        if(factoryName == null) return;
        packingRelation(factoryName);
        Env.getBatchRecord(factoryName).addElements(model);
    }
    
    void packingRelation(String name){
        if(Env.factories != null){
            for(FactoryMember m : Env.factories){
                if(name.equals(m.name)){
                    renewSocket(m);
                }
            }
        }
    }
    
    private void renewSocket(FactoryMember member){        
        if(member.socket == null || !member.socket.isOpen()){
            Env.logger.info(String.format("socket:%s associate with %s.",
                ip, member.name));
            byte[] name_byte = member.name.getBytes();
            System.arraycopy(name_byte, 0, regName, 0, name_byte.length);
            member.socket = this.socket;
        } else {
            try {
                if(member.socket.isOpen()){
                    String cmp1 = member.socket.getRemoteAddress().toString().split("/")[1];
                    if(!cmp1.equals(ip)){
                        String msg = String.format("socket association has duplicated from %s.", cmp1);
                        Env.logger.info(msg);
                        Env.closeChannel(socket);
                    }
                }
                
            } catch (IOException ex) {
                Logger.getLogger(DTU_Handler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
   
    private static byte[] createRegName(){
        ByteBuffer buf = ByteBuffer.allocate(10);
        while(buf.hasRemaining()){
            buf.put(SendCmd.Vacancy);
        }
        
        return buf.array();
    }
    
    private DataModel getDataModel(byte[] data){
        DataModel dataModel = null;
        
        switch(data.length){
            case DTU_Handler.heartbeat:
                dataModel = new HeartBeat();
                break;
                
            case DTU_Handler.YeserdaySize:
                dataModel = new Day1();
                break;
                
            case DTU_Handler.Immediate:
                dataModel = new Immediate();
                break;
                
            case DTU_Handler.DAY30Size:
                dataModel = new Day30();
                break;
                
            case DTU_Handler.Func6:
                dataModel = new ModbusFunc6();
                break;
                
            case DTU_Handler.Reg:
                dataModel = new RegPack();
                break;
                
            default:
                dataModel = new UnDefined();
                Env.logger.debug("The UnDefined class length is: " + data.length);
                break;
        }
        
        dataModel.set(data);
        return dataModel;
    }
    
    protected void finalize() throws Throwable {
        if(socket != null)    socket.close();
        buffer = null;
    }
}
