package org.ncl.workflow.ccn.sfc.main;

import net.gripps.cloud.core.VCPU;
import net.gripps.cloud.core.VM;
import net.gripps.cloud.nfv.NFVEnvironment;
import net.gripps.cloud.nfv.NFVUtil;
import net.gripps.cloud.nfv.clustering.HierarchicalVNFClusteringAlgorithm;
import net.gripps.cloud.nfv.clustering.SF_CUVAlgorithm;
import net.gripps.cloud.nfv.listscheduling.FWS_VNFAlgorithm;
import net.gripps.cloud.nfv.listscheduling.HEFT_VNFAlgorithm;
import net.gripps.cloud.nfv.optimization.CoordVNFAlgorithm;
import net.gripps.cloud.nfv.sfc.SFC;
import net.gripps.cloud.nfv.sfc.VNF;
import org.ncl.workflow.ccn.core.NclwNFDMgr;
import org.ncl.workflow.ccn.sfc.process.NclwNFDSendThread;
import org.ncl.workflow.comm.NCLWData;
import org.ncl.workflow.comm.RecvThread;
import org.ncl.workflow.comm.SendThread;
import org.ncl.workflow.comm.WorkflowJob;
import org.ncl.workflow.delegator.EnvJsonLoader;
import org.ncl.workflow.delegator.WorkflowJsonLoader;
import org.ncl.workflow.engine.Task;
import org.ncl.workflow.util.NCLWUtil;
import org.ncl.workflow.util.ProcessMgr;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Hidehiro Kanemitsu on 2019/11/19
 * Interestパケット中に，
 */
public class NclwNFDelegator {
    public static void main(String[] args){
        //Format: [environment json file] [ job json file]
        //Obtain the env file.
        String envJsonFile = args[0];
        String jobJsonFile = args[1];
        String prop = args[2];
        if(envJsonFile.isEmpty() || jobJsonFile.isEmpty()||prop.isEmpty()){
            System.out.println("Please input env. json file, job json file, and property file.");
            System.exit(-1);
        }
        NCLWUtil.getIns().initialize(prop);
        EnvJsonLoader envLoader = new EnvJsonLoader();
        //Obtain the env. info.
        NFVEnvironment env = envLoader.loadEnv(envJsonFile);
        WorkflowJsonLoader jobLoader = new WorkflowJsonLoader();
        SFC sfc = jobLoader.loadNFDJob(jobJsonFile);
        WorkflowJob job = jobLoader.getJob();

        switch(NCLWUtil.sched_algorithm){
            //SF-CUV
            case 0:
                SF_CUVAlgorithm sf_cuv = new SF_CUVAlgorithm(env, sfc);
                //alg5.setUpdateMode(0);
                sf_cuv.mainProcess();
                //Obtain the resultant SFC (Service Function Chain).
                sfc = sf_cuv.getSfc();
                System.out.println("SLR[SF_CUV]:"+ "makespan:"+ NFVUtil.getRoundedValue(sf_cuv.getMakeSpan()/*/sf_cuv.getTotalCPProcTimeAtMaxSpeed()*/) +" / # of vCPUs: "+sf_cuv.getAssignedVCPUMap().size()+ "/ # of Hosts:"+sf_cuv.getHostSet().size()
                        +"/# of Ins:"+sf_cuv.calcTotalFunctionInstanceNum());
                break;
            //HEFT
            case 1:
                HEFT_VNFAlgorithm heft = new HEFT_VNFAlgorithm(env, sfc);
                heft.mainProcess();
                sfc = heft.getSfc();
                System.out.println("SLR[HEFT]:"+ "makespan:"+NFVUtil.getRoundedValue(heft.getMakeSpan()/*/sf_cuv.getTotalCPProcTimeAtMaxSpeed()*/) +" / # of vCPUs: "+heft.getAssignedVCPUMap().size()+ "/ # of Hosts:"+heft.getHostSet().size()
                        +"/# of Ins:"+heft.calcTotalFunctionInstanceNum());
                break;
            //FWS
            case 2:
                FWS_VNFAlgorithm fws = new FWS_VNFAlgorithm(env, sfc);
                //alg6.setMaxHostNum(alg5.getHostSet().size());
                fws.mainProcess();
                sfc = fws.getSfc();
                System.out.println("SLR[FWS]:"+ "makespan:"+NFVUtil.getRoundedValue(fws.getMakeSpan()/*/sf_cuv.getTotalCPProcTimeAtMaxSpeed()*/) +" / # of vCPUs: "+fws.getAssignedVCPUMap().size()+ "/ # of Hosts:"+fws.getHostSet().size()
                        +"/# of Ins:"+fws.calcTotalFunctionInstanceNum());
                break;
            case 3:
                CoordVNFAlgorithm coord = new CoordVNFAlgorithm(env,sfc);
                coord.mainProcess();;
                sfc = coord.getSfc();
                System.out.println("SLR[CoordVNF]:"+ "makespan:"+NFVUtil.getRoundedValue(coord.getMakeSpan()/*/sf_cuv.getTotalCPProcTimeAtMaxSpeed()*/) +" / # of vCPUs: "+coord.getAssignedVCPUMap().size()+ "/ # of Hosts:"+coord.getHostSet().size()
                        +"/# of Ins:"+coord.calcTotalFunctionInstanceNum());
                break;
            case 4:
                HierarchicalVNFClusteringAlgorithm h = new HierarchicalVNFClusteringAlgorithm(env,sfc);
                h.configLevel();
                h.mainProcess();;
                sfc = h.getSfc();
                System.out.println("SLR[HClustering]:"+ "makespan:"+NFVUtil.getRoundedValue(h.getMakeSpan()/*/sf_cuv.getTotalCPProcTimeAtMaxSpeed()*/) +" / # of vCPUs: "+h.getAssignedVCPUMap().size()+ "/ # of Hosts:"+h.getHostSet().size()
                        +"/# of Ins:"+h.calcTotalFunctionInstanceNum());
                break;
            default:
                SF_CUVAlgorithm sf_cuv2 = new SF_CUVAlgorithm(env, sfc);

                sf_cuv2.mainProcess();
                //Obtain the resultant SFC (Service Function Chain).
                sfc = sf_cuv2.getSfc();
                System.out.println("SLR[SF_CUV]:"+ "makespan:"+NFVUtil.getRoundedValue(sf_cuv2.getMakeSpan()/*/sf_cuv.getTotalCPProcTimeAtMaxSpeed()*/) +" / # of vCPUs: "+sf_cuv2.getAssignedVCPUMap().size()+ "/ # of Hosts:"+sf_cuv2.getHostSet().size()
                        +"/# of Ins:"+sf_cuv2.calcTotalFunctionInstanceNum());
                break;

        }
        Iterator<VNF> vnfIte = sfc.getVnfMap().values().iterator();
        while(vnfIte.hasNext()){
            VNF vnf = vnfIte.next();
            VCPU vcpu = env.getGlobal_vcpuMap().get(vnf.getvCPUID());
            VM vm = NCLWUtil.findVM(env, vnf.getvCPUID());
            System.out.println("VNF:"+vnf.getIDVector().get(1) + "@"+vnf.getvCPUID()+"@"+vm.getIpAddr());
        }


        //-> END VNFがわらいてられているノードに対してInterestパケットを送る．
        Iterator<Long> endIte = sfc.getEndVNFSet().iterator();

        ProcessMgr.getIns().setStartTime(System.currentTimeMillis());
        NclwNFDSendThread sender = new NclwNFDSendThread();
        Thread sendThread = new Thread(sender);
        sendThread.start();

        ProcessMgr.getIns().setStartTime(System.currentTimeMillis());

        while(endIte.hasNext()){
            Long endID = endIte.next();
            VNF endVNF = sfc.findVNFByLastID(endID);
            VCPU vcpu = env.getGlobal_vcpuMap().get(endVNF.getvCPUID());

            VM host = NCLWUtil.findVM(env, vcpu.getPrefix());
            Task endTask = job.getNfdTaskMap().get(endID);
            //readFilePath: 送るファイルのpath, //相手側で受信後，入力ファイル生成path
            NCLWData data = new NCLWData(endID, -1, host.getIpAddr(), NCLWUtil.NFD_PORT, sfc, env, job);
            data.setFile(false);
            //当該データを，startタスクのノードへ送る．
            sender.getInterestDataQueue().add(data);
            //sender.getInterestDataQueue().add(data);
        }

    }


}
