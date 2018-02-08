/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jzelda.solar.pattern;

import com.jzelda.solar.Env;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Stream;

/**
 *
 * @author engin
 */
public class Day30 extends DeltaInverterModbus{
    int[] powerDay30 = new int[31];

    public void toSave() {
        String sql = "insert into historyPow(no, power) "
                + "select ?,? from dual where ? not in "
                + "(select date_format(date, \"%Y/%m/%d\") from historyPow where no=?)";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        Calendar cal = Calendar.getInstance();
        String recDate = sdf.format(cal.getTime());

        try (PreparedStatement ps = Env.getConnnection().prepareStatement(sql);){
            for(int i=0; i<30; i++){
                ps.setInt(1, inverterNo);
                ps.setInt(2,powerDay30[i]);
                ps.setString(3, recDate);
                ps.setInt(4, inverterNo);
                ps.execute();
                cal.add(Calendar.DATE, -1);
                recDate = sdf.format(cal.getTime());
            }
            Env.getConnnection().commit();
        } catch (SQLException ex) {
            Env.getlogger().debug(String.format("Day30 class toSave() exception: %s", ex.getMessage()));
        }
    }

    @Override
    public void parseField() {
        int ratio = 4;
        for(int i=3; i<modbusData.length-2; i+=ratio){
            byte[] pow = Arrays.copyOfRange(modbusData, i, i+ratio);
            int index = (i-3)/ratio;
            powerDay30[index] = count2fieldPow(pow);
        }
    }

    @Override
    public Object[] getDataSet() {
        return Arrays.stream(powerDay30).mapToObj(data -> (Object)data).toArray();
    }
}
