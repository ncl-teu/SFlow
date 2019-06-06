package org.ncl.workflow.resource;

import com.sun.management.OperatingSystemMXBean;
import org.ncl.workflow.util.NCLWUtil;

import java.lang.management.ManagementFactory;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Hidehiro Kanemitsu on 2019/04/25.
 */
public class ResourceMgr implements Runnable {

    /**
     * own Instance
     */
    protected static ResourceMgr own;

    /**
     * MXBean for handling Operating Systems.
     */
    protected  OperatingSystemMXBean osBean;


    /**
     * Factory method
     * @return
     */
    public static ResourceMgr getIns(){
        if(ResourceMgr.own == null){
            ResourceMgr.own = new ResourceMgr();
        }

        return ResourceMgr.own;
    }

    /**
     * Constructor
     */
    public ResourceMgr(){
        this.osBean =(OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();



    }

    /**
     * It returns the available processors.
     * @return
     */
    public int getvCPUNum(){
        return this.osBean.getAvailableProcessors();

    }

    /**
     *  It returns the average vCPU load values.
     * @return
     */
    public double getAveVCPULoad(){
        int vcpuNum = this.getvCPUNum();
        double ave = NCLWUtil.getRoundedValue(this.getvCPUNum()*this.getTotalCPULoad() );

        return Math.min(100, ave);
    }

    /**
     * It returns the CPU load as a whole.
     * @return
     */
    public  double getTotalCPULoad(){
        return NCLWUtil.getRoundedValue(100*this.osBean.getSystemCpuLoad());
    }

    public OperatingSystemMXBean getOsBean() {

        return osBean;
    }

    public void setOsBean(OperatingSystemMXBean osBean) {
        this.osBean = osBean;

    }

    @Override
    public void run() {

    }
}
