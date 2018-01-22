/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jzelda.solar.pattern;

import com.jzelda.solar.Env;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author engin
 */
public class HeartBeat extends DataModel{
    
    @Override
    public void analyze() {
        if(data.length == 0 || data == null){
            Env.getlogger().debug("No data content or assign yet.");
            return;
        }
        
        String content = Convert.toStringType(data);        
        Env.getlogger().debug(String.format("the heartbeat content is: %s", content));
    }

    @Override
    public String fromWhere() {
        return null;
    }

    @Override
    public Object[] getDataSet() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
