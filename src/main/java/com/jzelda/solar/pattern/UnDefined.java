/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jzelda.solar.pattern;

import com.jzelda.solar.Env;

/**
 *
 * @author engin
 */
public class UnDefined extends DataModel{

    @Override
    public void analyze() {
        String msg = "This package is unknown data";
        Env.getlogger().info(msg);
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
