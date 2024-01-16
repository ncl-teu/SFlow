package net.gripps.cloud.core;

import net.gripps.ccn.CCNUtil;
import net.gripps.ccn.core.InterestPacket;
import net.gripps.ccn.icnsfc.AutoUtil;
import net.gripps.ccn.icnsfc.core.AutoEnvironment;
import net.gripps.ccn.icnsfc.process.AutoSFCMgr;
import net.gripps.cloud.nfv.NFVUtil;
import net.gripps.cloud.nfv.clustering.HierarchicalVNFClusteringAlgorithm;
import net.gripps.cloud.nfv.clustering.SF_CUVAlgorithm;
import net.gripps.cloud.nfv.listscheduling.FWS_VNFAlgorithm;
import net.gripps.cloud.nfv.listscheduling.HEFT_VNFAlgorithm;
import net.gripps.cloud.nfv.listscheduling.PEFT_VNFAlgorithm;
import net.gripps.cloud.nfv.optimization.CoordVNFAlgorithm;
import net.gripps.cloud.nfv.sfc.SFC;
import net.gripps.environment.CPU;
import net.gripps.environment.Machine;
import net.named_data.jndn.Interest;
import org.ncl.workflow.util.NCLWUtil;

import java.util.HashMap;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by Hidehiro Kanemitsu on 2018/11/01.
 */
public class ComputeHost extends Machine{

    /**
     * VMのMapです．同一VMで複製した場合も，別個のVMとして扱います．
     * ただし，VM内に，「オリジナルVMID」を保持させているので，どのVMからの複製かは
     * わかります．
     */
    protected HashMap<String, VM> vmMap;


    /**
     * 当該ホストが属するデータセンターID
     */
    protected Long dcID;

    /**
     * このホストのprefix（文字列）
     */
    protected String prefix;

    /**
     * このホストのIPアドレス
     */
    protected String ipAddr;


    protected LinkedBlockingDeque<InterestPacket> interestQueue;


    public ComputeHost(long machineID,
                       TreeMap<Long, CPU> cpuMap,
                       int num,
                       HashMap<String, VM> vmMap,
                       Long dcID,
                       String p,
                       long bw)
    {
        super(machineID, cpuMap, num);
        this.vmMap = vmMap;
        this.dcID = dcID;
        this.prefix =p;
        this.setBw(bw);
        this.ipAddr = null;
        this.interestQueue = new LinkedBlockingDeque<InterestPacket>();

    }



    public SFC scheduleSFC(InterestPacket packet){
        SFC sfc = (SFC)packet.getAppParams().get(AutoUtil.SFC_NAME);
        AutoEnvironment env = AutoSFCMgr.getIns().getEnv();

        switch(AutoUtil.sched_altorithm){
            //SF-CUV
            case 0:
                SF_CUVAlgorithm sf_cuv = new SF_CUVAlgorithm(env, sfc);
                //alg5.setUpdateMode(0);
                sf_cuv.mainProcess();
                //Obtain the resultant SFC (Service Function Chain).
                sfc = sf_cuv.getSfc();

                break;
            //HEFT
            case 1:
                HEFT_VNFAlgorithm heft = new HEFT_VNFAlgorithm(env, sfc);
                heft.mainProcess();
                sfc = heft.getSfc();

                break;
            //FWS
            case 2:
                FWS_VNFAlgorithm fws = new FWS_VNFAlgorithm(env, sfc);
                //alg6.setMaxHostNum(alg5.getHostSet().size());
                fws.mainProcess();
                sfc = fws.getSfc();

                break;
            case 3:
                CoordVNFAlgorithm coord = new CoordVNFAlgorithm(env,sfc);
                coord.mainProcess();
                sfc = coord.getSfc();

                break;
            case 4:
                HierarchicalVNFClusteringAlgorithm h = new HierarchicalVNFClusteringAlgorithm(env,sfc);
                h.configLevel();
                h.mainProcess();;
                sfc = h.getSfc();

                break;
            case 5:
                PEFT_VNFAlgorithm p = new PEFT_VNFAlgorithm(env, sfc);
                p.mainProcess();

                sfc = p.getSfc();
                break;

            default:

                SF_CUVAlgorithm sf_cuv2 = new SF_CUVAlgorithm(env, sfc);

                sf_cuv2.mainProcess();
                //Obtain the resultant SFC (Service Function Chain).
                sfc = sf_cuv2.getSfc();

                break;

        }
        return sfc;
    }

    public LinkedBlockingDeque<InterestPacket> getInterestQueue() {
        return interestQueue;
    }

    public void setInterestQueue(LinkedBlockingDeque<InterestPacket> interestQueue) {
        this.interestQueue = interestQueue;
    }

    public HashMap<String, VM> getVmMap() {
        return vmMap;
    }

    public void setVmMap(HashMap<String, VM> vmMap) {
        this.vmMap = vmMap;
    }

    public Long getDcID() {
        return dcID;
    }

    public void setDcID(Long dcID) {
        this.dcID = dcID;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getIpAddr() {
        return ipAddr;
    }

    public void setIpAddr(String ipAddr) {
        this.ipAddr = ipAddr;
    }
}



