/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jzelda.solar;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

/**
 *
 * @author engin
 */
public class Concern extends Observable{
    private SocketChannel socket;
    
    Concern(){
        super();
        socket = null;
    }
    
    public boolean setSubject(SocketChannel subject){
        boolean result = false;
        if( socket == null){
            socket = subject;
            result = true;
        }
        
        return result;
    }
    
    public void notice(){
        this.setChanged();
        this.notifyObservers();
    }
}
