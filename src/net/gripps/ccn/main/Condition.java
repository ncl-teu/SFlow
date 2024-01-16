package net.gripps.ccn.main;

public class Condition {
    protected int age;

    public Condition(int age) {
        this.age = age;
    }

    public void judge(Person p){
        if(this.age < p.getAge()){
            System.out.println(this.age + "or mote");
        }else{
            System.out.println(this.age + "lower");
        }
    }
}
