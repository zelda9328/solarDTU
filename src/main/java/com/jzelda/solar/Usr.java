/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jzelda.solar;

import com.jzelda.math.crc.CRC16_IBM;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Set;
import java.nio.channels.Pipe;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Iterator;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;


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
    
    public void checkIdle(){
        
    }
    
    public static void main(String[] args) throws IOException, InterruptedException {
        // TODO code application logic here
        Usr usr = new Usr();
        usr.run();
        //Env.logger.fatal("test send mail msg");
        
        //System.in.read();
        /*
        ServerSocketChannel sktChannel = ServerSocketChannel.open();
        sktChannel.socket().bind(new InetSocketAddress(30001));
        SocketChannel s = sktChannel.accept();
        s.socket().setSoTimeout(30*1000);
        InputStream is = s.socket().getInputStream();
        byte[] a = new byte[500];
        System.out.println("pass connect");
        is.read(a);
        System.out.println(new String(a));
        */
        //ReadableByteChannel wrap = Channels.newChannel(is);
        
        //ByteBuffer b = ByteBuffer.allocate(500);
        //System.out.println("pass connect");
                //wrap.read(b);
                //System.out.println("pass read");
                //System.out.println(new String(b.array()));
        
        //t.cancel();
        //System.out.println("over");
        
            /*
            Pipe pipe = Pipe.open();
            Pipe.SourceChannel cmd = pipe.source();
            cmd.configureBlocking(false);
            //cmd.register(usr.selector, SelectionKey.OP_READ, new CmdSource());
             */ 

    }
    
    static void tmp() throws IOException{
        final int port = 30001;
        ServerSocket ss = new ServerSocket(port);
        
            Socket sk = ss.accept();
            InputStream os = sk.getInputStream();
            OutputStream is = sk.getOutputStream();
            BufferedInputStream bis = new BufferedInputStream(os);
            BufferedOutputStream bos = new BufferedOutputStream(is);
            BufferedReader breader = new BufferedReader(
                    new InputStreamReader(System.in));
            int a;
        while(true){
            /*
            String txt = breader.readLine();
            //bos.write(txt.getBytes(), 0, txt.length());
            bos.write(txt.getBytes());
            bos.flush();
            
            
            System.out.println(txt);
            */
            byte[] content = new byte[1024];
            while(bis.read(content) > 0){
                
                //bis.read(content);
                String s = new String(content);
                System.out.println(s);
            }
        }
    }
}
