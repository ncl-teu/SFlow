package net.gripps.clustering.algorithms.loadbacancing;


import java.util.Comparator;

/**
 * Author: H. Kanemitsu
 * Date: 2008/02/09
 */
public class ClusterInfoComparator implements Comparator{
    public int compare(Object o1, Object o2){

        long value = ((ClusterInfo)o1).getSize() - ((ClusterInfo)o2).getSize();
        int intValue = (int)value;

        return intValue;
 }

}
