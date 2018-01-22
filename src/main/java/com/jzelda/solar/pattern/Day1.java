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
import java.util.Arrays;
import java.util.Calendar;

/**
 *
 * @author engin
 */
public class Day1 extends DeltaInverterModbus{
    int yesterdayPower;

    public void toSave() {
        String sql = "insert into historyPow(no, power) "
                + "select ?,? from dual where ? not in "
                + "(select date_format(date, \"%Y/%m/%d\") from historyPow where no=?)";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        Calendar cal = Calendar.getInstance();
        String recDate = sdf.format(cal.getTime());

        try (PreparedStatement ps = Env.getConnnection().prepareStatement(sql);){
            ps.setInt(1, inverterNo);
            ps.setInt(2,yesterdayPower);
            ps.setString(3, recDate);
            ps.setInt(4, inverterNo);
            ps.execute();
            Env.getConnnection().commit();
        } catch (SQLException ex) {
            Env.getlogger().debug(String.format("Day1 class toSave() exception: %s", ex.getMessage()));
        }
    }

    @Override
    public void parseField() {
        byte[] pow = Arrays.copyOfRange(modbusData, 3, 7);
        yesterdayPower = count2fieldPow(pow);
    }

    @Override
    public Object[] getDataSet() {
        return new Object[]{yesterdayPower};
    }
}
