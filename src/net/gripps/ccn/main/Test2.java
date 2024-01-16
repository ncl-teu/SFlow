package net.gripps.ccn.main;

public class Test2 {
    public static void main(String[] args){
        IPerson hikoku = new Hikoku();
        IPerson b = new Bengoshi(hikoku);
        b.hello();
    }
}
