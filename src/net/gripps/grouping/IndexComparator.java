package net.gripps.grouping;

import net.gripps.clustering.common.aplmodel.AbstractTask;

import java.util.Comparator;

/**
 * Created by kanemih on 2016/01/16.
 */

public class IndexComparator implements Comparator {

    public int compare(Object o1, Object o2) {
        //優先度の小さい順にソートする．
        IndexInfo m1 = (IndexInfo) o1;
        IndexInfo m2 = (IndexInfo) o2;
        if (m1.getIndexValue() < m2.getIndexValue()) {
            return -1;
        }
        if (m1.getIndexValue() > m2.getIndexValue()) {
            return 1;
        }

        return 0;
    }

}
