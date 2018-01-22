/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jzelda.solar.pattern;

/**
 *
 * @author engin
 */
public abstract class DataModel {
    protected byte[] data;
    
    public void set(byte[] data){
        this.data = data;
    }
    
    abstract public void analyze();
    
    abstract public String fromWhere();
    
    abstract public Object[] getDataSet();
}
