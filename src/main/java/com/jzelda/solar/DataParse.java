/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jzelda.solar;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author engin
 */
public class DataParse implements Runnable{
    
    DataParse(){
        
    }

    @Override
    public void run() {
        int i=1;
        while(true){
            System.out.println(++i);            
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                Logger.getLogger(DataParse.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
}
