package net.gripps.cloud.nfv.listscheduling;

import net.gripps.cloud.core.CloudEnvironment;
import net.gripps.cloud.core.VCPU;
import net.gripps.cloud.nfv.sfc.SFC;
import net.gripps.cloud.nfv.sfc.VNF;
import net.gripps.clustering.common.aplmodel.DataDependence;

import java.util.HashMap;
import java.util.Iterator;

public class HEFTDAlgorithm  extends HEFT_VNFAlgorithm{

    public HEFTDAlgorithm(CloudEnvironment env, SFC sfc) {
        super(env, sfc);
    }

    @Override
    public void scheduleVNF(VNF vnf, HashMap<String, VCPU> map) {
        //先行タスクを取得
        Iterator<DataDependence> dpredIte = vnf.getDpredList().iterator();
        //先行タスクの入れてーたのループ
        while(dpredIte.hasNext()){
            DataDependence dpred = dpredIte.next();
            //先行タスクのID
            Long fromID = dpred.getFromID().get(1);
            //先行タスクを取得
            VNF predVNF = this.getSfc().findVNFByLastID(fromID);
            //predVNFが割り当てられているVCPUを取得
            String predVcpuID = predVNF.getvCPUID();
            //VCPUを取得
            VCPU predVCPU = this.env.getGlobal_vcpuMap().get(predVcpuID);

            //vcpuに割り当てられているタスクリスト
            //predVCPU.getVnfQueue()




        }


    }
}
