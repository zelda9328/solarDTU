/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jzelda.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 *
 * @author engin
 */
public interface ICmd extends Remote{
    public byte[] listFactory() throws RemoteException;
    public void get(String[] args) throws RemoteException;
}
