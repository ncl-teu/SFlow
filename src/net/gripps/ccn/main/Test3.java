package net.gripps.ccn.main;

import java.lang.reflect.Proxy;

public class Test3 {

    public static void main(String[] args) throws Exception{
        Hikoku h = new Hikoku();
        Bengoshi2 b = new Bengoshi2(h);

        IPerson p = (IPerson) Proxy.newProxyInstance(
                h.getClass().getClassLoader(),
                new Class[]{IPerson.class},
                b);
        p.hello();
        p.bye();
    }
}

