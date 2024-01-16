package net.gripps.ccn.main;

public class Condition2 extends Condition{

    public Condition2(int age) {
        super(age);
    }

    @Override
    public void judge(Person p) {
        if(p.getAge() < this.age){
            System.out.println("*****child*****");
        }
    }
}
