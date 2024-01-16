package net.gripps.ccn.main;

import java.util.Iterator;
import java.util.LinkedList;

public class Test4 {

    public static void main(String[] args){
        LinkedList<Condition> cList = new LinkedList<Condition>();

        cList.add(new Condition(10));
        cList.add(new Condition(20));
        cList.add(new Condition(30));
        cList.add(new Condition2(28));


        Person p = new Person(25, "tanaka");

        Iterator<Condition> cIte = cList.iterator();
        while(cIte.hasNext()){
            Condition c = cIte.next();

            c.judge(p);
        }
    }
}
