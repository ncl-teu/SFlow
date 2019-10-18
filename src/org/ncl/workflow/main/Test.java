package org.ncl.workflow.main;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * Created by Hidehiro Kanemitsu on 2019/10/09
 */
public class Test {

    public static void main(String[] args) throws Exception{

            MBeanServer mbs    = ManagementFactory.getPlatformMBeanServer();
            ObjectName name    = ObjectName.getInstance("java.lang:type=OperatingSystem");
            AttributeList list = mbs.getAttributes(name, new String[]{ "ProcessCpuLoad" });

            if (list.isEmpty())     System.out.println( Double.NaN);

            Attribute att = (Attribute)list.get(0);
            Double value  = (Double)att.getValue();

            // usually takes a couple of seconds before we get real values
            if (value == -1.0)      System.out.println( Double.NaN);
            // returns a percentage value with 1 decimal point precision
            System.out.println( ((int)(value * 1000) / 10.0));
        }

}
