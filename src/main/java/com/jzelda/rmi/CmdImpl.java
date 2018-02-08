/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jzelda.rmi;

import com.jzelda.solar.Env;
import com.jzelda.solar.FactoryMember;
import com.jzelda.solar.SendCmd;
import java.nio.ByteBuffer;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.Optional;

/**
 *
 * @author engin
 */
public class CmdImpl extends UnicastRemoteObject implements ICmd{
    public final static byte[] separate = {0x7f,0x7f};
    
    public CmdImpl() throws RemoteException{
    }

    @Override
    public byte[] listFactory() throws RemoteException {
        HashSet<FactoryMember> factory = Env.getFactories();
        ByteBuffer buf = ByteBuffer.allocate(1024);
        factory.forEach(f -> {
            buf.put(f.getName().getBytes());
            buf.put(separate);
        });
        
        buf.flip();
        byte[] data = new byte[buf.remaining()];
        buf.get(data);
        return data;
    }

    @Override
    public void get(String[] args) throws RemoteException {
        //args seq should be: action,factoryName,which id
        switch(args[0]){
            case "30day":
                if(args.length != 3)    break;
                
                Optional opt = Env.getFactories().stream().filter(f -> f.getName().equals(args[1])).findAny();
                if(opt.isPresent()){
                    FactoryMember f = (FactoryMember)opt.get();
                    String instruction = new String(args[2]);
                    func_30day(f, instruction);
                }
                break;
        }
    }
    
    private void func_30day(FactoryMember f, String instruction){
        ByteBuffer buf = ByteBuffer.allocate(1024);
        byte[] semi_modbusCmd = {0x04,0x08,0x01,0x00,0x3e};
        byte delaySec = 0x03;
        
        try{
            int amount = instruction.equalsIgnoreCase("all")? f.getInverterAmount() : Integer.valueOf(instruction);
            int startId = instruction.equalsIgnoreCase("all")? 1 : Integer.valueOf(instruction);
                        
            for(int i = startId; i<=amount; i++){
                buf.put(f.getName().getBytes());
                buf.put(SendCmd.SepareateFlag);
                buf.put((byte)i);
                buf.put(semi_modbusCmd);
                buf.put(SendCmd.EndFlag);
                buf.put(SendCmd.MsgTimeDelay);
                buf.put(delaySec);
            }

            buf.flip();
            byte[] cmd = new byte[buf.limit()];
            buf.get(cmd);
            Env.getInstance().writePipe(cmd);
        } catch(NumberFormatException e){
            String msg = "request 30day function: id error, you give %s";
            Env.getlogger().debug(String.format(msg, instruction));
        }
    }
}
