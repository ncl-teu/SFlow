package net.gripps.ccn.main;

public class Bengoshi implements IPerson{
    //被告への参照を持つ
    private IPerson hikoku;

    public Bengoshi(IPerson hikoku) {
        this.hikoku = hikoku;
    }

    @Override
    public void hello() {
        System.out.println("***START****");
        this.hikoku.hello();
        System.out.println("****END****");
    }

    public void bye(){
        System.out.println("Bye Bengoshi");
    }
}
