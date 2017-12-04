/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jzelda.solar;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author engin
 */
public class BatchRecord extends TimerTask {
    final static int dbStructFields = 10;
    
    String name;
    int amount;
    
    Map<Integer, Object[]> elements;
    Timer timer;
    //timer is isset?
    Boolean isset;
    ScheduledExecutorService service;
    
    BatchRecord(FactoryMember factor){
        this.name = factor.name;        
        amount = factor.inverterIdList.size();
        elements = new HashMap();
        timer = new Timer(this.name);
        service = Executors.newScheduledThreadPool(1);
        isset = false;
    }
    
    void addElements(Object[] o){
        if(o.length != dbStructFields){
            Env.logger.warn("data length waiting write into DB is not correct.");
            return;
        }
        
        int hold = (Integer)o[1];
        if(elements.containsKey(o[0])){
            Object[] t = elements.get(o[0]);
            Env.logger.info("syntax is right? the key elements get is: " + o[0]);
            if(hold == 48){
                t[1] = o[2];
                t[2] = o[3];
            }
            if(hold == 0){
                t[3] = o[2];
                t[4] = o[3];
                t[6] = o[5];
            }
        } else{
            Object[] newArray = new Object[11];
            
            int i = 0;
            for(int it = 0; it < o.length; it++){
                if( it == 1) {
                    //i++;
                    continue;
                }
                if( hold == 48 && i == 3){
                    i+=2;
                    //continue;
                }
                if( hold == 0 && i == 1){
                    i+=2;
                    //continue;
                }
                newArray[i] = o[it];
                i++;
            }
            elements.put((Integer)o[0], newArray);
        }
        /*
        if(elements.size() == amount){
            Env.logger.info(name + " BatchRecord has max amount, execute run()");
            run();
        }
        */
    }
    
    void batchWrite(){        
        if(elements.size() == 0){
            isset = false;
            return;
        }
        
        String sql = "insert into immediate(no,voltageDC,currentDC,voltageAC,currentAC,wattage,frequency,todayWatt,ambTemp,boostHsTemp,invHsTemp,stamp) "
                + "values(?,?,?,?,?,?,?,?,?,?,?,?)";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                Calendar now = Calendar.getInstance();
                String nowStr = sdf.format(now.getTime());
        try (PreparedStatement ps = Env.conn.prepareStatement(sql);){
            
            
            for(Object[] collection : elements.values()){
                int i = 1;
                for(Object o : collection){
                    ps.setObject(i, o);
                    i++;
                }
                ps.setString(collection.length+1, nowStr);
                ps.addBatch();
            }
            
            ps.executeBatch();
            Env.conn.commit();

        } catch (SQLException ex) {
            Env.logger.warn("sql execute error, message: " + ex.getMessage());
        }
        
        Env.logger.info(name + " timer execute has over.");
        elements = new HashMap();
        isset = false;
    }
    
    void startTimer(){
        synchronized(this){
            if(isset){
                Env.logger.info(name + " timer has setting.");
                return;
            }
            
            isset = true;
        }
        Env.logger.info(name + " set timer");
        //timer.schedule(this, Env.cmdSendPeriod * 4 * 1000 +20000);
        service.schedule(this, Env.cmdSendPeriod*4 +20, TimeUnit.SECONDS);
        //timer.cancel();
    }

    @Override
    public void run() {
        batchWrite();
    }
    
    protected void finalize() throws Throwable {
        timer.cancel();
    }
}
