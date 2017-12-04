/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jzelda.solar;
import com.jzelda.math.crc.CRC16_IBM;
import com.jzelda.util.MysqlProperty;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
/**
 *
 * @author engin
 */
public class Env {
    static Connection conn;
    static HashSet<FactoryMember> factories;
    static byte[] lock;
    final static Logger logger;
    final static int DelayUnit = 1000;
    final static int cmdSendPeriod = 60;
    final static byte[] cmdPattern = {0x04, 0x04, (byte)0x1f, 0x00, 0x03};
    
    static Map<String, BatchRecord> mapBatch;
    static Set<DTU_Handler> connectionManager;
    private static final long TolerableGap = 2*60*1000;
    private static final long MaxIdleGap = 5*60*1000;
    private static int maxAmount;
    
    static {
        String log4jFile = "/log4j2.xml";
        //URL url = Env.class.getResource(log4jFile);
        InputStream in = Env.class.getResourceAsStream(log4jFile);
              //  System.out.println(in);
        try{
            ConfigurationSource cs = new ConfigurationSource(in);
            Configurator.initialize(null, cs);
        } catch (IOException e){
            //java.util.logging.Logger.getLogger(Env.class.getName()).log(Level.SEVERE, "read log4j2.xml fail");
            java.util.logging.Logger.getLogger(Env.class.getName()).log(Level.SEVERE, null, e);
            //System.exit(1);
        }
        
        logger = LogManager.getLogger();
        connectionManager = new HashSet();
    }
    
    Env(){
        try {
            //maxAmount = 0;
            factories = new HashSet();
            mapBatch = new HashMap();
            lock = new byte[0];
            MysqlProperty sqlArgs = new MysqlProperty(this, "/resource.xml");
            //Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(sqlArgs.connectArgs, sqlArgs.user, sqlArgs.passwd);
            conn.setAutoCommit(false);
            
            //String sql_getRelation = "select name,count(sn) from inverter"
            //        + " join factory where inverter.appertain=factory.no group by appertain";
            //String sql_getRelation = "select name,count(sn),sn-1 as sn from "
            String sql_getRelation = "select name,inverter.no ,appertain from inverter join factory "
                    + "where inverter.appertain=factory.no order by appertain, inverter.sn";
            PreparedStatement ps = conn.prepareStatement(sql_getRelation);
            ResultSet rs = ps.executeQuery();
            
            while(rs.next()){
                String tmpName = rs.getString(1);
            
                //factorymember has created?
                Boolean isExist = false;
                for(FactoryMember fm :factories){
                    if(fm.name.equals(tmpName)){
                        isExist = true;
                        fm.inverterIdList.add(rs.getInt(2));
                        break;
                    }
                }
                
                if(!isExist){
                    FactoryMember member = new FactoryMember();
                    member.name = tmpName;
                    member.inverterIdList.add(rs.getInt(2));
                    
                    factories.add(member);
                    mapBatch.put(member.name, new BatchRecord(member));
                }
                
            }
            rs.close();
            ps.close();            
        } catch (SQLException ex) {
            logger.fatal(ex);
        }
        
        maxAmount = 0;
        for(FactoryMember fm : factories){
            int fmInverterSize = fm.inverterIdList.size();
            maxAmount = maxAmount > fmInverterSize? maxAmount : fmInverterSize;
        }
        
        logger.info(String.format("factory max inverters :%d", maxAmount));
    }
    
    Set<FactoryMember> getFactoryMember(){
        return factories;
    }
    
    
    static int getMAXamount(){
        return maxAmount;
    }
    
    static BatchRecord getBatchRecord(String name){
        return mapBatch.get(name);
    }
    
    static void queryReg(SocketChannel socket){
        ByteBuffer cmdComplete = ByteBuffer.allocate(cmdPattern.length+3);
        cmdComplete.clear();
        cmdComplete.put((byte)0x01);
        cmdComplete.put(cmdPattern);
        cmdComplete.flip();
        byte[] modbusData = new byte[cmdComplete.limit()-cmdComplete.position()];
        cmdComplete.get(modbusData, 0, 6);
        int crc = CRC16_IBM.getCRC(modbusData);
        byte crcH = (byte)((crc & 0xff00) >> 8);
        //byte a = crcH.byteValue();
        byte crcL = (byte)(crc & 0xff);

        cmdComplete.limit(cmdComplete.capacity());
        cmdComplete.put(crcL);
        cmdComplete.put(crcH);
        cmdComplete.flip();
        while(cmdComplete.hasRemaining()){
            try{
                socket.write(cmdComplete);
            } catch(IOException e){
                logger.warn("send queryReg happen IO error: " + e.getMessage());
                break;
            }
        }
    }
    
    static void checkIdleConnection(){
        logger.info("ready to check idle connection");
        Calendar nowTime = Calendar.getInstance();
        
        DTU_Handler[] handlerArray = connectionManager.toArray(new DTU_Handler[0]);
        for(DTU_Handler handler : handlerArray){            
            Calendar lastTime = handler.lastTime;
                long gap = nowTime.getTimeInMillis() - lastTime.getTimeInMillis();
                if(gap > TolerableGap && gap < MaxIdleGap){
                    queryReg(handler.socket);
                }
                
                if(gap > MaxIdleGap){
                    closeChannel(handler.socket);
                    //connectionManager.remove(handler);
                }
        }
    }
    
    static int  socketReadAgnet(SocketChannel s, ByteBuffer buffer){
        int n = 0;
        try{
            n = s.read(buffer);
            if(n == -1){
                closeChannel(s);
                logger.info(s.getRemoteAddress().toString().split("/")[1] + " close");
            }
        } catch(IOException e){
            logger.warn("channel read fail. cause: "+ e.getMessage());
            closeChannel(s);
        }
        return n;
    }
    
    static Boolean socketWriteAgent(SocketChannel s, ByteBuffer buffer){
        Boolean rs = false;
        if(s != null && s.isOpen()){
            rs = true;
            while(buffer.hasRemaining()){
                try{
                    s.write(buffer);
                } catch(IOException e){
                    closeChannel(s);
                    rs = false;
                }
            }
        }
        
        return rs;
    }
    
    static void closeChannel(SocketChannel s){
        try{
            DTU_Handler[] handlerArray = connectionManager.toArray(new DTU_Handler[0]);
            for(DTU_Handler handler : handlerArray){
                if(handler.socket == s){
                    connectionManager.remove(handler);
                    break;
                }
            }
            s.close();            
        } catch(IOException e){
            logger.warn("close channel happen error"+ e.getMessage());
        }
    }
}