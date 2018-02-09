/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jzelda.solar;

import com.jzelda.solar.pattern.DataModel;
import com.jzelda.solar.pattern.DeltaInverterModbus;
import com.jzelda.solar.pattern.Immediate;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 發電資料寫入DB
 * @author engin
 */
public class BatchRecord extends TimerTask {
    final static int dbStructFields = 10;
    
    String name;
    int amount;
    
    Map<Integer, Object[]> elements;
    //timer is isset?
    Boolean isset;
    ScheduledExecutorService service;
    
    BatchRecord(FactoryMember factor){
        this.name = factor.name;        
        amount = factor.inverterIdList.size();
        elements = new HashMap();
        service = Executors.newScheduledThreadPool(1);
        isset = false;
    }
    
    private String getStoreString(Class c){
        Class dataProperty = c;
        String sql = null;
        
        switch(dataProperty.getSimpleName()){
            case "Day1":
                sql = "insert into historyPow(no, power) "
                        + "select ?,? from dual where ? not in "
                        + "(select date_format(date, \"%Y/%m/%d\") from historyPow where no=?)";
                break;
                
            case "Day30":
                sql = "insert into historyPow(no, power, date) "
                        + "select ?,?,? from dual where ? not in "
                        + "(select date_format(date, \"%Y/%m/%d\") from historyPow where no=?)";
                break;
                
            case "Immediate":
                sql = "insert into immediate "
                        + "(no,voltageDC,currentDC,voltageAC,currentAC,wattage,frequency,todayWatt,ambTemp,boostHsTemp,invHsTemp,stamp) "
                        + "values(?,?,?,?,?,?,?,?,?,?,?,?)";
                break;
        }
        
        return sql;
    }
    
    private void toSave(DeltaInverterModbus model){
        String storeSql = getStoreString(model.getClass());
        Object[] data = model.getDataSet();
        
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        
        try (PreparedStatement ps = Env.conn.prepareStatement(storeSql)){
            for(int i=0; i<data.length; i++){
                String date_str  = sdf.format(cal.getTime());
                
                int x = 4;
                int y = 0;
                ps.setObject(1, model.getInverterNo());
                ps.setObject(2, data[i]);
                ps.setObject(3, date_str);
                //30天資料要帶日期
                if(model.getClass().getSimpleName().equals("Day30")){
                    y =1;
                    ps.setObject(x, date_str);
                }                
                ps.setObject(x+y, model.getInverterNo());
                
                ps.execute();
                Env.conn.commit();
                
                cal.add(Calendar.DATE, -1);
            }
        } catch (SQLException ex) {
            Env.logger.warn("sql execute error, message: " + ex.getMessage());
        }
    }
    
    private void addElements(Object[] o){
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
                }
                if( hold == 0 && i == 1){
                    i+=2;
                }
                newArray[i] = o[it];
                i++;
            }
            elements.put((Integer)o[0], newArray);
        }
    }
    
    /**
     * 傳入資料並寫入DB
     * @param model 
     */
    public void addElements(DataModel model){
        String classType = model.getClass().getSimpleName();
        
        switch(classType){
            case "Day1":
            case "Day30":
                DeltaInverterModbus day = (DeltaInverterModbus)model;
                day.parseField();
                toSave(day);
                break;
                
            case "Immediate":
                Immediate immediate = (Immediate)model;
                immediate.parseField();
                int id = immediate.getInverterNo();
                Object[] obj = immediate.getDataSet();                
                obj[0] = id;                
                addElements( obj );
                startTimer();
                break;
                
            case "UnDefined":
                break;
        }
    }
    
    private void batchWrite(){        
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
    
    /**
     * 搭配addElements(Object[] o),做交直流一併寫入
     */
    private void startTimer(){
        synchronized(this){
            if(isset){
                Env.logger.info(name + " timer has setting.");
                return;
            }
            
            isset = true;
        }
        Env.logger.info(name + " set timer");
        service.schedule(this, Env.cmdSendPeriod*4 +20, TimeUnit.SECONDS);
        //timer.cancel();
    }

    @Override
    public void run() {
        batchWrite();
    }
}
