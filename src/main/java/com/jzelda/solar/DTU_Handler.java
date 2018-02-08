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
    //byte[] regName = {SendCmd.Vacancy, SendCmd.Vacancy, SendCmd.Vacancy, SendCmd.Vacancy, SendCmd.Vacancy, 
      //  SendCmd.Vacancy, SendCmd.Vacancy, SendCmd.Vacancy, SendCmd.Vacancy, SendCmd.Vacancy};
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
    
    public void run1() {
        try{
            int n = socket.read(buffer);
            if(n == -1){
                try {
                    Env.logger.info("client send close request.");
                    finalize();
                } catch (Throwable ex) {
                    Logger.getLogger(DTU_Handler.class.getName()).log(Level.SEVERE, null, ex);
                }
                return;
            }
            buffer.flip();
        
            int bufferLeng = buffer.limit();
            byte[] data = new byte[bufferLeng];
            buffer.get(data, 0, bufferLeng);
            buffer.clear();
            save(data);
            
        } catch (IOException ex) {
            Env.logger.warn("io error: "+ex.getMessage());
            try {
                Env.logger.info("close the socket.");
                finalize();
            } catch (Throwable ex1) {
                Logger.getLogger(DTU_Handler.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
        
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
    
    void save(byte[] data){
        
        
        lastTime = Calendar.getInstance();        
        
        
        
        
    }
    
    private void savePowerData(byte[] pow, int id, String name){
        String msg = String.format("receive data length is %d", pow.length);
        Env.logger.info(msg);
        String sql;
        SimpleDateFormat sdf;
        switch(pow.length){
            case 54:
                //int vol = byte2int(pow[0], pow[1]);
                //int cur = byte2int(pow[2], pow[3]);
                //int watt = byte2int(pow[4], pow[5]);
                int hold = byte2int(pow[0], pow[1]);
                BigDecimal a = new BigDecimal(byte2int(pow[2], pow[3]));
                BigDecimal b = new BigDecimal("0.1");
                float vol = a.multiply(b).floatValue();
                a = new BigDecimal(byte2int(pow[4], pow[5]));
                b = new BigDecimal("0.01");
                float cur = a.multiply(b).floatValue();
                int watt = byte2int(pow[6], pow[7]);
                a = new BigDecimal(byte2int(pow[8], pow[9]));
                float freq = a.multiply(b).floatValue();
                
                byte[] powdata = {pow[32], pow[33], pow[34], pow[35]};
                int powInt = count2fieldPow(powdata)*10;
                int ampTemp = byte2int(pow[48], pow[49]);
                int boostHsTemp = byte2int(pow[50], pow[51]);
                int invHsTemp = byte2int(pow[52], pow[53]);
                Env.logger.info("hold is " + hold);
                Env.logger.info("vol is " + vol);
                Env.logger.info("cur is " + cur);
                Env.logger.info("watt is " + watt);
                Env.logger.info("freq is " + freq);
                Env.logger.info("watt until now is " + powInt);
                Env.logger.info("ampTemp is " + ampTemp);
                Env.logger.info("boostHsTemp is " + boostHsTemp);
                Env.logger.info("invHsTemp is " + invHsTemp);
                
                Object[] values = {id, hold, vol, cur, watt, freq, powInt, ampTemp, boostHsTemp,invHsTemp};
                Env.getBatchRecord(name).addElements(values);
                break;
                
            case 4:
                int sum = count2fieldPow(pow);
                Env.logger.info("yesterday power is: "+sum);
                
                sql = "insert into historyPow(no, power) "
                        + "select ?,? from dual where ? not in "
                        + "(select date_format(date, \"%Y/%m/%d\") from historyPow where no=?)";
                sdf = new SimpleDateFormat("yyyy/MM/dd");
                Calendar cal = Calendar.getInstance();
                //cal.add(Calendar.DATE, -12);
                String recDate = sdf.format(cal.getTime());

                try (PreparedStatement ps = Env.conn.prepareStatement(sql);){
                    ps.setInt(1, id);
                    ps.setInt(2,(int)sum);
                    ps.setString(3, recDate);
                    ps.setInt(4, id);
                    ps.execute();
                    Env.conn.commit();
                } catch (SQLException ex) {
                    Env.logger.warn("sql execute error, message: " + ex.getMessage());
                }
                break;
        }
    }
    
    private int byte2int(byte high, byte low){
        int high_int = (int)(high & 0xff) << 8;
        int low_int = (int)(low & 0xff);
        return high_int + low_int;
    }
    /*
    private int bcd2int(byte high, byte low){
        int higt_int = (high & 0xf0 >> 4)*10 + high & 0x0f;
        int low_int = (low & 0xf0 >> 4)*10 + low & 0x0f;
        
        return high*100 + low;
    }
    */
    private int count2fieldPow(byte[] pow){
        double sum=0;
        if(pow.length != 4){
            String msg = String.format("argv length is not match in %s", this.getClass().getName());
            Env.logger.warn(msg);
        }
        for(int i=1; i<pow.length;i+=2){
            String msg = String.format("socket power data; %s %S", Convert.toHex(pow[i]), Convert.toHex(pow[i-1]));
            Env.logger.info(msg);
            double rate = Math.pow(16, (i-1)*2);
            sum += (Byte.toUnsignedInt(pow[i]))* rate;
            sum += (Byte.toUnsignedInt(pow[i-1]))* rate * 16*16;
        }
        return (int)sum;
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
