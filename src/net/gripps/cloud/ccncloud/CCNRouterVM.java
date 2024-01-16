package net.gripps.cloud.ccncloud;

import net.gripps.ccn.core.CCNRouter;
import net.gripps.cloud.core.VCPU;
import net.gripps.cloud.core.VM;
import net.gripps.environment.CPU;

import java.util.HashMap;
import java.util.TreeMap;

/**
 * Created by kanemih on 2018/11/02.
 */
public class CCNRouterVM extends VM {
    /**
     * CCTルータプロセスのリスト．通常は1つのみだが，拡張性を考慮してMapにした．
     */
    private HashMap<Long, CCNRouter> ccnRouterMap;

    public CCNRouterVM(String machineID,
                       String hostID,
                       TreeMap<Long, CPU> cpuMap,

                       HashMap<String, VCPU> vCPUMap,
                       long ramSize,
                       String  orgVMID,
                       HashMap<Long, CCNRouter> ccnRouterMap)
    {
        super(machineID, hostID, vCPUMap, ramSize, orgVMID);
        this.ccnRouterMap = ccnRouterMap;
    }

    public HashMap<Long, CCNRouter> getCcnRouterMap() {
        return ccnRouterMap;
    }

    public void setCcnRouterMap(HashMap<Long, CCNRouter> ccnRouterMap) {
        this.ccnRouterMap = ccnRouterMap;
    }
}
