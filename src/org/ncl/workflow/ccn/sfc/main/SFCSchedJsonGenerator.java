package org.ncl.workflow.ccn.sfc.main;

import net.gripps.cloud.nfv.NFVEnvironment;
import net.gripps.cloud.nfv.NFVUtil;
import net.gripps.cloud.nfv.clustering.HierarchicalVNFClusteringAlgorithm;
import net.gripps.cloud.nfv.clustering.SF_CUVAlgorithm;
import net.gripps.cloud.nfv.listscheduling.FWS_VNFAlgorithm;
import net.gripps.cloud.nfv.listscheduling.HEFT_VNFAlgorithm;
import net.gripps.cloud.nfv.listscheduling.PEFT_VNFAlgorithm;
import net.gripps.cloud.nfv.optimization.CoordVNFAlgorithm;
import net.gripps.cloud.nfv.sfc.SFC;
import org.ncl.workflow.comm.WorkflowJob;
import org.ncl.workflow.delegator.EnvJsonLoader;
import org.ncl.workflow.delegator.SchedGenerator;
import org.ncl.workflow.delegator.WorkflowJsonLoader;
import org.ncl.workflow.util.NCLWUtil;

/**
 * Created by Hidehiro Kanemitsu on 2020/06/10.
 */
public class SFCSchedJsonGenerator {

    public static void main(String[] args){
        //Format: [environment json file] [ job json file]
        //Obtain the env file.
        String envJsonFile = args[0];
        String jobJsonFile = args[1];
        String prop = args[2];
        String hostFile = args[3];

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
            case 5:
                PEFT_VNFAlgorithm p = new PEFT_VNFAlgorithm(env, sfc);
                p.mainProcess();
                System.out.println("SLR[PEFT]:"+ "makespan:"+NFVUtil.getRoundedValue(p.getMakeSpan()/*/sf_cuv.getTotalCPProcTimeAtMaxSpeed()*/) +" / # of vCPUs: "+p.getAssignedVCPUMap().size()+ "/ # of Hosts:"+p.getHostSet().size()
                        +"/# of Ins:"+p.calcTotalFunctionInstanceNum());
                sfc = p.getSfc();
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
        //次に,sfcオブジェクトをjsonファイルへ書き出す．
        SchedGenerator gen = new SchedGenerator(jobLoader, sfc, "SFCRet.json",env);

        gen.process();

    }
}
