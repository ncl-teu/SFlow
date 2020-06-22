package org.ncl.workflow.ccn.autoicnsfc;

import net.gripps.cloud.nfv.sfc.VNF;
import net.gripps.clustering.common.aplmodel.AbstractTask;

import java.util.Comparator;

/**
 * Created by Hidehiro Kanemitsu on 2020/05/08.
 */
public class BWSTComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            //優先度の大きい順にソートする．
            // long value = ((AbstractTask)o1).getPriorityBlevel() - ((AbstractTask)o2).getPriorityBlevel();
            VNF m1 = (VNF)o1;
            VNF  m2 = (VNF)o2;
            if(m1.getBlevelWST() > m2.getBlevelWST()){
                return -1;
            }
            if(m1.getBlevelWST() <= m2.getBlevelWST()){
                return 1;
            }

            return 0;
        }


}
