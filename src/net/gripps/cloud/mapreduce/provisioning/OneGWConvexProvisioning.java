package net.gripps.cloud.mapreduce.provisioning;

import net.gripps.cloud.mapreduce.core.MRCloudEnvironment;
import net.gripps.cloud.mapreduce.core.MRVCPU;

import java.util.HashMap;

public class OneGWConvexProvisioning extends ConvexProvisioningAlgorithm{

    public OneGWConvexProvisioning(MRCloudEnvironment in_env, long mapperNum, long reducerNum, HashMap<String, MRVCPU> mapperMap, HashMap<String, MRVCPU> reducerMap) {
        super(in_env, mapperNum, reducerNum, mapperMap, reducerMap);
    }

    public OneGWConvexProvisioning(MRCloudEnvironment in_env) {
        super(in_env);
    }

    @Override
    public long calcReducerNum() {
        //Reducer = GW1つなので，1を返す．
        return 1;
    }
}
