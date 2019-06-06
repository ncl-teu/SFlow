package org.ncl.workflow.delegator;

import net.gripps.cloud.core.ComputeHost;
import net.gripps.cloud.core.VCPU;
import net.gripps.cloud.core.VM;
import net.gripps.cloud.nfv.NFVEnvironment;
import net.gripps.cloud.nfv.NFVUtil;
import net.gripps.cloud.nfv.clustering.SF_CUVAlgorithm;
import net.gripps.cloud.nfv.sfc.SFC;
import net.gripps.cloud.nfv.sfc.VNF;
import org.ncl.workflow.comm.NCLWData;
import org.ncl.workflow.comm.RecvThread;
import org.ncl.workflow.comm.SendThread;
import org.ncl.workflow.comm.WorkflowJob;
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
 * Created by Hidehiro Kanemitsu on 2019/05/17.
 */
public class NCLWDelegator {
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
        SFC sfc = jobLoader.loadJob(jobJsonFile);
        WorkflowJob job = jobLoader.getJob();

        SF_CUVAlgorithm sf_cuv = new SF_CUVAlgorithm(env, sfc);


        //alg5.setUpdateMode(0);
        sf_cuv.mainProcess();
        //Obtain the resultant SFC (Service Function Chain).
        sfc = sf_cuv.getSfc();

        System.out.println("SLR[SF_CUV]:"+ "makespan:"+NFVUtil.getRoundedValue(sf_cuv.getMakeSpan()/*/sf_cuv.getTotalCPProcTimeAtMaxSpeed()*/) +" / # of vCPUs: "+sf_cuv.getAssignedVCPUMap().size()+ "/ # of Hosts:"+sf_cuv.getHostSet().size()
                +"/# of Ins:"+sf_cuv.calcTotalFunctionInstanceNum());

        Iterator<Long> startIte = sfc.getStartVNFSet().iterator();
        SendThread sender = new SendThread();
        Thread sendThread = new Thread(sender);
        sendThread.start();
        ProcessMgr.getIns().setStartTime(System.currentTimeMillis());

        while(startIte.hasNext()){
            Long startID = startIte.next();
            VNF startVNF = sfc.findVNFByLastID(startID);
            VCPU vcpu = env.getGlobal_vcpuMap().get(startVNF.getvCPUID());
            VM host = NCLWUtil.findVM(env, vcpu.getPrefix());
            Task startTask = job.getTaskMap().get(startID);
            //readFilePath: 送るファイルのpath, //相手側で受信後，入力ファイル生成path
            NCLWData data = new NCLWData(-1, startID, host.getIpAddr(), NCLWUtil.port, sfc, env, job);

            data.setFile(false);
            //当該データを，startタスクのノードへ送る．

            sender.getDataQueue().add(data);
            System.out.println("@Delegator: info is sent to tartTaskID:"+startID +"@vCPU:"+vcpu.getPrefix()+"@"+host.getIpAddr() );
        }
        ExecutorService exec;
        exec = Executors.newFixedThreadPool(10);
        try{
            ServerSocket listen_socket = new ServerSocket(NCLWUtil.port);
            while(true){
                Thread.sleep(100);
                Socket client = listen_socket.accept();
                //RecvThread receiver = new RecvThread( port, client);
                exec.submit(new RecvThread(new LinkedBlockingQueue<NCLWData>(), NCLWUtil.port, client));

                //Thread t = new Thread(new RecvThread(new LinkedBlockingQueue<NCLWData>(), this.port, client));

                // Thread t = new Thread(receiver);
                //    t.start();
            }
        }catch(Exception e){
            e.printStackTrace();
        }

    }
}
