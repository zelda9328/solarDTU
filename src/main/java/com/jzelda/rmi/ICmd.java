/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jzelda.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * RMI指令定義
 * @author engin
 */
public interface ICmd extends Remote{
    /**
     * 列出發電廠名
     * @return
     * @throws RemoteException 
     */
    public byte[] listFactory() throws RemoteException;
    
    /**
     * 資料請求
     * @param args
     * @throws RemoteException 
     */
    public void get(String[] args) throws RemoteException;
}
