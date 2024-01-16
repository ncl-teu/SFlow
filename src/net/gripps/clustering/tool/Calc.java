/**
 * Created by IntelliJ IDEA.
 * User: kanemih
 * Date: 2009/12/26
 * Time: 22:29:28
 * To change this template use File | Settings | File Templates.
 */
package net.gripps.clustering.tool;

import java.math.BigDecimal;

public class Calc {
    private static Calc ourInstance = new Calc();

    public static Calc getInstance() {
        return ourInstance;
    }

    private Calc() {
    }

    /**
     *
     * @param value1
     * @return
     */
    public static double getRoundedValue(double value1) {
      //  try{
            BigDecimal value2 = new BigDecimal(String.valueOf(value1));
            double retValue = value2.setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
            return retValue;

     //   }catch(NumberFormatException e){
      //      System.out.println("Format Error!!!"+value1);
            


     //   }
     //   return 0;



    }
}
