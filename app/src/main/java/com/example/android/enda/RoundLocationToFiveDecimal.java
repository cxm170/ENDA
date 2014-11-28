package com.example.android.enda;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Created by cxm170 on 11/27/2014.
 */
public final class RoundLocationToFiveDecimal {
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
