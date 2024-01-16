package net.gripps.ccn.main;

import java.io.Serializable;
import java.util.List;

public  class Person implements Serializable {

    private int age;

    private String name;

    public Person(int age, String name) {
        //List<Character> list = List.of();
        //list.add('a');

        this.age = age;
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
