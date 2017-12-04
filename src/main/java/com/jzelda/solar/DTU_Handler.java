/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jzelda.solar;

import com.jzelda.math.crc.CRC16_IBM;
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

/**
 *
 * @author engin
 */
public class DTU_Handler implements Runnable{
    static int baseDataLeng = 10;
    //function code 03 data length
    static int minLEng = 7;
    static int heartbeat = 3;
    
    SocketChannel socket;
    ByteBuffer buffer;
    String ip;
    Calendar lastTime;
    
    DTU_Handler(SocketChannel s){
        socket = s;
        try{
            ip = s.getRemoteAddress().toString().split("/")[1];
        } catch( IOException e){
            Env.logger.error("socket get address fail.");
        }
        buffer = ByteBuffer.allocate(256*1024);
        buffer.clear();
        
        lastTime = Calendar.getInstance();
    }

    @Override
    public void run(){
        Env.socketReadAgnet(socket, buffer);
        
        buffer.flip();

        int bufferLeng = buffer.limit();
        byte[] data = new byte[bufferLeng];
        buffer.get(data, 0, bufferLeng);
        buffer.clear();
        save(data);
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
    
    public static String toHex(byte b){
return (""+"0123456789ABCDEF".charAt(0xf&b>>4)+"0123456789ABCDEF".charAt(b&0xf));} 
    
    void save(byte[] data){
        if(data.length != DTU_Handler.heartbeat
                && data.length < DTU_Handler.baseDataLeng+DTU_Handler.minLEng){
            //資料長度不符預期
            Env.logger.info("receive data format leng error");
            return;
        }
        
        lastTime = Calendar.getInstance();        
        if(data.length == DTU_Handler.heartbeat){
            Env.logger.info("receive heartbeat from " + ip);
            return;
        }
        
        byte[] slaveName_ori = Arrays.copyOfRange(data, 0, 9);
        String slaveName = new String(slaveName_ori).trim();
        packingRelation(slaveName);
        
        byte[] modbus = Arrays.copyOfRange(data, 10, data.length-2);
        int crc =  CRC16_IBM.getCRC(modbus);
        byte crcH = (byte)((crc & 0xff00) >> 8);
        //byte a = crcH.byteValue();
        byte crcL = (byte)(crc & 0xff);
        if(crcL != data[data.length-2]
                || crcH != data[data.length-1]){
            //crc is different
            String msg = String.format("CRC check error from %s", slaveName);
            Env.logger.warn(msg);
            return;
        }
        //byte[] id_ori = Arrays.copyOfRange(data, 10, 10);
        int id = (int)data[10];
        String msg = String.format("data is from id => %d, factory => %s", id, slaveName);
        Env.logger.info(msg);
        
        int inverterId = 0;
        for(FactoryMember m : Env.factories){
            if(m.name.equals(slaveName)){
                inverterId = m.inverterIdList.get(id -1);
                //inverterId = id + m.shift;
            }
        }
        Env.logger.info(String.format("transfer id to db index: %d", inverterId));
        //byte[] func_ori = Arrays.copyOfRange(data, 11, 11);
        int func = (int)data[11];
                
        
        switch(func){
            case 4:                
                int dataLeng = (int)data[12];
                savePowerData(Arrays.copyOfRange(data, 13, 13+dataLeng), inverterId, slaveName);
            
            case 1:
                
                break;
        }
        
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
            String msg = String.format("socket power data; %s %S", toHex(pow[i]), toHex(pow[i-1]));
            Env.logger.info(msg);
            double rate = Math.pow(16, (i-1)*2);
            sum += (Byte.toUnsignedInt(pow[i]))* rate;
            sum += (Byte.toUnsignedInt(pow[i-1]))* rate * 16*16;
        }
        return (int)sum;
    }
    
    protected void finalize() throws Throwable {
        if(socket != null)    socket.close();
        buffer = null;
    }
}
