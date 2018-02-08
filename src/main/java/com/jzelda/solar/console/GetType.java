/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jzelda.solar.console;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author engin
 */
public class GetType implements ICommand{

    @Override
    public void exec(Optional<String[]> args) {
        if(!args.isPresent()){
            printHelp();
            return;
        }
        
        String[] args_sr = args.get();
        Optional helpful = Arrays.stream(MyConsole.HelpFlag).filter(flag -> flag.equalsIgnoreCase(args_sr[0])).findFirst();
        if(helpful.isPresent()){
            printHelp();
            return;
        }
        
        String[] subCmd = {"30day"};
        Optional optCmd = Arrays.stream(subCmd).filter(cmd -> cmd.equalsIgnoreCase(args_sr[0])).findAny();
        String[] argsAdd = new String[3];
        System.arraycopy(args_sr, 0, argsAdd, 0, args_sr.length);        
        if(optCmd.isPresent() && args_sr.length == 2){
            argsAdd[2] = "all";
        }

        try {
            MyConsole.getInstance().rmiCmd.get(argsAdd);
        } catch (RemoteException ex) {
            System.out.println("call RMI get function fail.");
        }
    }
    
    private void printHelp(){
        String msg = "usage: get option args\n" +
                "option:\n" +
                "  30day\t\targs is factoryname, can use list query.";
        System.out.println(msg);
    }
}
