package net.gripps.cloud.nfv;

import net.gripps.ccn.process.CCNMgr;
import net.gripps.cloud.core.CloudEnvironment;
import net.gripps.cloud.core.ComputeHost;
import net.gripps.cloud.core.VCPU;

import java.util.HashMap;
import java.util.Iterator;

public class NFVEnvironment extends CloudEnvironment {

    /**
     * Dockerリポジトリ
     */
    protected  ComputeHost dockerRepository;

    /**
     * マスタノード
     */
    protected ComputeHost masterNode;


    public ComputeHost getMasterNode() {
        return masterNode;
    }

    public void setMasterNode(ComputeHost masterNode) {
        masterNode = masterNode;
    }

    public ComputeHost getDockerRepository() {
        return dockerRepository;
    }

    public void setDockerRepository(ComputeHost dockerRepository) {
        this.dockerRepository = dockerRepository;
    }

    public NFVEnvironment() {
        super();
        //新たに，dockerリポジトリを生成する．
        this.dockerRepository = new ComputeHost(-1, null, 0, null, (long)1, null, 1000);
        this.masterNode = new ComputeHost(-1, null, 0, null, (long)1, null, 1000);



        //vcpuリストを取得．
        /*Iterator<VCPU> vIte = this.getGlobal_vcpuMap().values().iterator();
        while(vIte.hasNext()){
            VCPU vcpu  = vIte.next();
            long mips = vcpu.getMips();
            if(mips <1000){
                vcpu.setVoltage(1.0);
            }else if(mips <2000){
                vcpu.setVoltage(2.0);
            }else{
                vcpu.setVoltage(3.0);
            }
        }*/


    }
}
