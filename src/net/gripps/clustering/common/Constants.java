package net.gripps.clustering.common;

/**
 * Author: Hidehiro Kanemitsu<br>
 * Date: 2007/07/06
 */
public interface Constants {

    /**
     * Loop
     */
    public static int TYPE_LOOP = 1;

    /**
     * Function call
     */
    public static int TYPE_FUNCTION_CALL = 2;

    /**
     * Basic Block which consists of sequential assignment statements
     */
    public static int TYPE_BASIC_BLOCK = 3;

    /**
     * Single Assignment
     */
    public static int TYPE_CONDITION = 4;

    /**
     *
     */
    public static int ORDER_NON_DECREASING_BLEVEL = 0;

    /**
     * 
     */
    public static int ORDER_NON_DECREASING_INSTRUCTION = 1;

    public static int INFINITY= -1;

    public static long MAXValue = (long)Double.POSITIVE_INFINITY;



}
