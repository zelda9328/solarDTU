/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jzelda.solar;

import com.jzelda.solar.console.MyConsole;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Set;
import java.nio.channels.SelectableChannel;
import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * tcp收發程式，server發送請求，並接放client期望資料
 * 要注意client不定時會斷線，造成例外，此部份要處理
 * @author engin
 */

/**
 * G781 custom modbus protocol
 * send:
 * | slaveid--1byte | function code--1byte | start addr--2byte
 * | assign recv msg--2 byte | crc low--1byte | crc high--1byte |
 * receive:
 * | register string--10bbyte | slaveid--1byte | function code--1byte
 * | represent data length--1byte | data-- n byte | crc low--1byte | crc high--1byte |
 */
public class Usr implements Runnable{

    /**
     * @param args the command line arguments
     */
    ServerSocketChannel sktChannel;
    static Selector selector;
    final int port = 30001;
    
    List<DTU_Handler> DTU_List;
    Env env;
    Timer timer;
    TimerTask cmdsend;
    
    Usr() throws IOException{
        env = new Env();
        DTU_List = new ArrayList<DTU_Handler>();
        sktChannel = ServerSocketChannel.open();
        sktChannel.socket().bind(new InetSocketAddress(port));
        sktChannel.configureBlocking(false);
        
        selector = Selector.open();
        sktChannel.register(selector, SelectionKey.OP_ACCEPT).attach(new Acceptor(this));
        
        DataParse checker = new DataParse();
    }
    
    static public Selector getSelector(){
        return selector;
    }
    
    public void run(){
        try {
            //new Timer().schedule(new CmdSendTask(), 60*1000, 60*1000);
            ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
            service.scheduleAtFixedRate(new CmdSendTask(), Env.cmdSendPeriod, 60, TimeUnit.SECONDS);
            while(!Thread.interrupted()){
                selector.select();
                
                Set<SelectionKey> selected = selector.selectedKeys();
                SelectionKey[] selectedArray = selector.selectedKeys().toArray(new SelectionKey[0]);
                
                for(SelectionKey key : selectedArray){
                    dispatch(key);
                    selected.remove(key);
                    //key.cancel();
                }
                //selected.clear();
            }
        } catch (IOException ex) {
            Env.logger.fatal(ex);
        }
    }
    
    void dispatch(SelectionKey key){
        Runnable r = (Runnable)key.attachment();
        SelectableChannel cl = key.channel();
        if (!key.isValid()){
            Env.logger.info("trigger selector: key is not valid.");
            key.cancel();
            r = null;
        }
        
        if(r != null){
            r.run();
        }
    }
    
    public static void main(String[] args) throws IOException, InterruptedException, NotBoundException {
        // TODO code application logic here
        if(args.length != 0){
            MyConsole c = MyConsole.getInstance();
            c.run();
        } else {
            Usr usr = new Usr();
            usr.run();
        }
    }
}
