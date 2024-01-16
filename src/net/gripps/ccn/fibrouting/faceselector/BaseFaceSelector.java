package net.gripps.ccn.fibrouting.faceselector;

import net.gripps.ccn.core.FIB;
import net.gripps.ccn.core.Face;

import java.io.Serializable;
import java.util.LinkedList;

/**
 * Created by Hidehiro Kanemitsu on 2020/06/24.
 */
public class BaseFaceSelector implements Serializable {

    protected double calcMetric(FIB fib, String prefix, Face face){
        if(fib.getTable().containsKey(prefix)){

        }else{
            //もしprefixがなければ，新規追加する．
        }
        LinkedList<Face> fList = fib.getTable().get(prefix);
        return 0;

    }



}
