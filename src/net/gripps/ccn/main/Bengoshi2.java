package net.gripps.ccn.main;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class Bengoshi2 implements InvocationHandler {
    /**
     * 代理対象のオブジェクト（任意の型で良い）
     */
    private Object hikoku;

    public Bengoshi2(Object hikoku) {
        this.hikoku = hikoku;
    }

    @Override
    public Object invoke(Object obj, Method method, Object[] args) throws Throwable {
        //対象のメソッドの前に処理を追加
        long start = System.currentTimeMillis();
        //対象のメソッドを実行
        Object out = method.invoke(this.hikoku, args);

        long end = System.currentTimeMillis();
        System.out.println("time:"+ (end-start));

        return out;
    }
}
