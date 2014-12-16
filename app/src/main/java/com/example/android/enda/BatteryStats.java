package com.example.android.enda;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Created by cxm170 on 12/5/2014.
 */
public class BatteryStats {
    public static String dumpBatteryInfo() {

        try {
            String cmd = "dumpsys batterystats com.example.android.enda";
            Log.e("error", "EXECUTING CMD: " + cmd);
//            Process script = Runtime.getRuntime().exec(cmd);
            Process script = Runtime.getRuntime().exec(new String[] { "su", "-c", "dumpsys batterystats com.example.android.enda"});
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(script.getInputStream()));

            Log.e("error", "****** >> Battery stats *******");

            StringBuilder builder = new StringBuilder();


            String line = null;
            boolean display = false;
            while ((line = in.readLine()) != null) {
//                if(line.equals("  u0a154:")) display = true;
//                if(line.contains("Uid u0a154")) builder.append(line+"\n");
//                if(display) builder.append(line+"\n");
                builder.append(line+"\n");
            }






            Log.e("error", "******/Battery stats *******");

            return builder.toString();

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static void resetBatteryInfo() {

        try {


            Runtime.getRuntime().exec(new String[] { "su", "-c", "dumpsys batterystats --reset"});



        } catch (Exception ex) {
            ex.printStackTrace();

        }
    }
}
