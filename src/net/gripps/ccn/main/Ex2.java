package net.gripps.ccn.main;

public class Ex2 {


    public void test(Object[] val){
        System.out.println("A");
    }

    public void test(long[] val){
        System.out.println("b");
    }

    public void test(Object val){
        System.out.println("C");
    }
}