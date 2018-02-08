/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jzelda.solar.console;

import com.jzelda.rmi.CmdImpl;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author engin
 */
public class List implements ICommand{

    @Override
    public void exec(Optional<String[]> args) {
        if(args.isPresent()){
            System.out.println("command:list should not have args.");            
        }
        
        try {
            byte[] data = MyConsole.getInstance().rmiCmd.listFactory();
            String[] factorys = new String(data).split(new String(CmdImpl.separate));
            System.out.println("the names of factorys have:");
            Arrays.stream(factorys).forEach(name -> {
                String display =  String.format("%s   ", name);
                System.out.print(display);
            });
            
        } catch (RemoteException ex) {
            System.out.println("request factory list fail.");
        }
        
        System.out.println();
    }
}
