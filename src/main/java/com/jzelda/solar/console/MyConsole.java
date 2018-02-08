/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jzelda.solar.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author engin
 */
public class MyConsole {
    private final static String Prompt = "[solar] $> ";
    private final static String RmiSrc = "rmi://localhost/cmd";
    final static String[] HelpFlag = {"help", "h", "?"};
    private static MyConsole created;
    com.jzelda.rmi.ICmd rmiCmd;
    
    private MyConsole(){
        try{
            rmiCmd = (com.jzelda.rmi.ICmd)Naming.lookup(RmiSrc);
        } catch(Exception e){
            System.out.println("Not found RMI service");
            System.exit(1);
        }
    }
    
    public static MyConsole getInstance(){
        if(created == null)
            created = new MyConsole();
        
        return created;
    }
    
    public static void run() throws IOException{
        String cmd = null;
        BufferedReader buf = new BufferedReader(new InputStreamReader(System.in));
        System.out.print(Prompt);

        while(!(cmd = buf.readLine()).equalsIgnoreCase("exit")){
            
            String[] cmd_sr = cmd.split(" ");
            Optional<ICommand> cmdOPT = parse(cmd_sr[0]);
            
            if(cmdOPT.isPresent()){
                String[] args = null;
                if(cmd_sr.length > 1){
                    args = Arrays.copyOfRange(cmd_sr, 1, cmd_sr.length);
                }
            
                cmdOPT.get().exec(Optional.ofNullable(args));
            }
            
            
            
            System.out.println();
            System.out.print(Prompt);
        }
    }
    
    private static Optional<ICommand> parse(String cmd){
        ICommand returnCmd = null;
        switch(cmd.toLowerCase()){
            case "list":
                returnCmd = new List();
                break;
                
            case "get":
                returnCmd = new GetType();
                break;
        }
        return returnCmd == null? Optional.empty() : Optional.of(returnCmd);
    }
}
