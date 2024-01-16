package net.gripps.cloud.mapreduce.provisioning;

import net.gripps.cloud.mapreduce.core.MRCloudEnvironment;
import net.gripps.cloud.mapreduce.core.MRVCPU;

import java.util.HashMap;

public class BaseMobileGWProvisioning extends BaseProvisioningAlgorithm{

    public BaseMobileGWProvisioning(MRCloudEnvironment in_env, long mapperNum, long reducerNum, HashMap<String, MRVCPU> mapperMap, HashMap<String, MRVCPU> reducerMap) {
        super(in_env, mapperNum, reducerNum, mapperMap, reducerMap);
    }

    public BaseMobileGWProvisioning(MRCloudEnvironment in_env) {
        super(in_env);
    }

    @Override
    public long calcReducerNum() {
        this.reducerNum = 1;
        return 1;
    }
}
