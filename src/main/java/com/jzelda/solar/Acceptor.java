/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jzelda.solar;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.jzelda.math.crc.CRC16_IBM;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author engin
 */
public class Acceptor implements Runnable{
    private DTU_Handler handler;
    
    //byte[] cmdPattern = {0x03, 0x03, 0x1f, 0x00, 0x01};
    
    static ServerSocketChannel srvSocket;
    Selector selector;    
    static List<SocketChannel> clients;
    static HashMap<String, FactoryMember> clientMap;
    
    Acceptor(Usr usr){
        Acceptor.srvSocket = usr.sktChannel;
        //this.srvSocket = usr.sktChannel;        
        selector = usr.selector;
        
        clients = new ArrayList<>();
        clientMap = new HashMap<>();
    }

    static List<SocketChannel> getClients(){
        return clients;
    }
    @Override
    public void run() {
        try {
            SocketChannel socket = srvSocket.accept();
            socket.configureBlocking(false);
            
            String ip = socket.getRemoteAddress().toString().split("/")[1];
            Env.logger.info("accept ip: "+ip);
            
            //clients.add(socket);
            handler = new DTU_Handler(socket);
            socket.register(selector, SelectionKey.OP_READ, handler);
            Env.connectionManager.add(handler);
            
            Env.queryReg(socket);
        } catch (IOException ex) {
            Logger.getLogger(Acceptor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
