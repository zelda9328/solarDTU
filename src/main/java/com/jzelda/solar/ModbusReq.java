/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jzelda.solar;

import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.TimerTask;

/**
 *
 * @author engin
 */
public class ModbusReq extends TimerTask{

    @Override
    public void run() {
        List<SocketChannel> clients = Acceptor.clients;
        if(clients != null){
            for(SocketChannel s: clients){
                
            }
        }
    }
    
}
