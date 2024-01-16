package net.gripps.clustering.algorithms.triplet;

import net.gripps.clustering.common.aplmodel.DataDependence;
import net.gripps.environment.CPU;

import java.util.Comparator;

/**
 * Author: H. Kanemitsu
 * Date: 14/12/26
 */
public class DataComparator implements Comparator {
    public int compare(Object o1, Object o2) {
          //優先度の大きい順にソートする．
          // long value = ((AbstractTask)o1).getPriorityBlevel() - ((AbstractTask)o2).getPriorityBlevel();
          DataDependence m1 = (DataDependence) o1;
        DataDependence m2 = (DataDependence) o2;
          if (m1.getMaxDataSize() >= m2.getMaxDataSize()) {
              return -1;
          }
          if (m1.getMaxDataSize() < m2.getMaxDataSize()) {
              return 1;
          }

          return 0;
      }
}
