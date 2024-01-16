package net.gripps.test;

/**
 * Author: H. Kanemitsu
 * Date: 15/11/18
 */
public class TimeTest {
    public static void main(String[] args){
        int count = 1000;
        long start = System.nanoTime();
        for (int i=0;i<count;i++){
            long a = 1;
            long b = 3;
            long c = a+b;
        }
        // Some processing
        long end = System.nanoTime();
        double dif_by_micro = (end-start)/(double)1000;
        double mean_micro = dif_by_micro/(double)count;
        //System.out.println("Time:" + (end - start) / 1000000f + "ms");
        System.out.println("Time:" + dif_by_micro + "マイクロs");
        System.out.println("1演算時間:"+ mean_micro+"マイクロ秒");
    }
}
