package net.gripps.clustering.algorithms.triplet;

import net.gripps.environment.CPU;

import java.util.Comparator;

/**
 * Author: H. Kanemitsu
 * Date: 14/12/21
 */
public class CPUComparator implements Comparator {
    public int compare(Object o1, Object o2) {
          //優先度の大きい順にソートする．
          // long value = ((AbstractTask)o1).getPriorityBlevel() - ((AbstractTask)o2).getPriorityBlevel();
          CPU m1 = (CPU) o1;
        CPU m2 = (CPU) o2;
          if (m1.getBw() > m2.getBw()) {
              return -1;
          }
          if (m1.getBw() < m2.getBw()) {
              return 1;
          }

          if (m1.getBw() == m2.getBw()) {
              //BWが同じであれば，処理速度を見る．
              if (m1.getSpeed() >= m2.getSpeed()) {
                  return -1;
              } else {
                  return 1;
              }
          }
          return 0;
      }
}
