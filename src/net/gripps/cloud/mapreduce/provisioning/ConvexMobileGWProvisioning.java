package net.gripps.cloud.mapreduce.provisioning;

import net.gripps.cloud.mapreduce.core.MRCloudEnvironment;
import net.gripps.cloud.mapreduce.core.MRVCPU;

import java.util.HashMap;

public class ConvexMobileGWProvisioning extends ConvexProvisioningAlgorithm{

    public ConvexMobileGWProvisioning(MRCloudEnvironment in_env, long mapperNum, long reducerNum, HashMap<String, MRVCPU> mapperMap, HashMap<String, MRVCPU> reducerMap) {
        super(in_env, mapperNum, reducerNum, mapperMap, reducerMap);
    }

    public ConvexMobileGWProvisioning(MRCloudEnvironment in_env) {
        super(in_env);
    }

    /**
     * GWがReducerを行うため，1を返す．
     * @return
     */
    @Override
    public long calcReducerNum() {
        this.reducerNum = 1;
        return 1;
    }
}
