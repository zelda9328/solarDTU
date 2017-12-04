/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jzelda.solar;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author engin
 */
public class FactoryMember {
    String name;
    List<Integer> inverterIdList;
    SocketChannel socket;
    ConcurrentHashMap<Integer, BatchRecord> sendMonitor;
    FactoryMember(){
        sendMonitor = new ConcurrentHashMap<Integer, BatchRecord>();
        inverterIdList = new ArrayList();
    }
}
