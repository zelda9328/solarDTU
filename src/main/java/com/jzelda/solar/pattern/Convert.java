/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jzelda.solar.pattern;

/**
 *
 * @author engin
 */
public class Convert {
    public static String toStringType(byte[] data){
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for(int i=0; i<data.length; i++){
            sb.append(toHex(data[i]));
            
            if(i != data.length-1)
                sb.append(',');
            else
                sb.append(']');
        }
        
        return sb.toString();
    }
    
    public static String toHex(byte b){
        return (""+"0123456789ABCDEF".charAt(0xf&b>>4)+"0123456789ABCDEF".charAt(b&0xf));
    }
    
    public static int byte2int(byte high, byte low){
        int high_int = (int)(high & 0xff) << 8;
        int low_int = (int)(low & 0xff);
        return high_int + low_int;
    }
}
