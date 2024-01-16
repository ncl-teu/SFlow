package net.gripps.ccn.main;

public class Hikoku implements IPerson{

    @Override
    public void hello() {
        System.out.println("I am Hikoku");
    }

    public void bye(){
        System.out.println("Bye Bye");
    }
}
