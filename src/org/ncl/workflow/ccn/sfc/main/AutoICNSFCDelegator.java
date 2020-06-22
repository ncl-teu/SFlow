package org.ncl.workflow.ccn.sfc.main;

import com.intel.jnfd.deamon.face.tcp.TcpFace;
import net.gripps.cloud.core.VCPU;
import net.gripps.cloud.core.VM;
import net.gripps.cloud.nfv.NFVEnvironment;
import net.gripps.cloud.nfv.sfc.SFC;
import net.gripps.cloud.nfv.sfc.VNF;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;
import org.ncl.workflow.ccn.autoicnsfc.AutoICNSFCMgr;
import org.ncl.workflow.ccn.autoicnsfc.AutoICNSFCScheduling;
import org.ncl.workflow.ccn.core.NclwNFDMgr;
import org.ncl.workflow.ccn.sfc.process.NFDTask;
import org.ncl.workflow.ccn.sfc.process.NclwNFD;
import org.ncl.workflow.ccn.sfc.process.NclwNFDSendThread;
import org.ncl.workflow.ccn.sfc.strategy.AutoICNFaceThread;
import org.ncl.workflow.ccn.util.ResourceMgr;
import org.ncl.workflow.comm.NCLWData;
import org.ncl.workflow.comm.WorkflowJob;
import org.ncl.workflow.delegator.EnvJsonLoader;
import org.ncl.workflow.delegator.WorkflowJsonLoader;
import org.ncl.workflow.engine.Task;
import org.ncl.workflow.util.NCLWUtil;
import org.ncl.workflow.util.ProcessMgr;

import java.util.Iterator;

/**
 * Created by Hidehiro Kanemitsu on 2020/02/26
 */
public class AutoICNSFCDelegator {
    /**
     * 自律的スケジューリングのmainメソッドです．
     * @param args
     */
    public static void main(String[] args){
        String envJsonFile = args[0];
        String jobJsonFile = args[1];
        String prop = args[2];
        String hostFile = args[3];

        if(envJsonFile.isEmpty() || jobJsonFile.isEmpty()||prop.isEmpty()){
            System.out.println("Please input env. json file, job json file, and property file.");
            System.exit(-1);
        }
        NCLWUtil.getIns().initialize(prop);

        //ResourceMgr.getIns().initResource();


        EnvJsonLoader envLoader = new EnvJsonLoader();
        //Obtain the env. info.
        NFVEnvironment env = envLoader.loadEnv(envJsonFile);
        WorkflowJsonLoader jobLoader = new WorkflowJsonLoader();
        SFC sfc = jobLoader.loadNFDJob(jobJsonFile);
        WorkflowJob job = jobLoader.getJob();
        ResourceMgr.getIns().setEnv(env);


        AutoICNSFCScheduling autoICN = new AutoICNSFCScheduling(env, sfc);
        sfc = autoICN.constructAutoSFC();
        //Schedをセットしておく．schedにはsfc+envが入っている．
        AutoICNSFCMgr.getIns().setSched(autoICN);
        //NFDプロセスを起動させる．
        //ここで，FIBの初期化がなされる．
        NclwNFD nfd = new NclwNFD();
        nfd.process(0,prop, hostFile );

        //送信用スレッドを起動させる．
        ProcessMgr.getIns().setStartTime(System.currentTimeMillis());
        NclwNFDSendThread sender = new NclwNFDSendThread();
        Thread sendThread = new Thread(sender);
        sendThread.start();
        Iterator<Long> endIte = sfc.getEndVNFSet().iterator();

        ProcessMgr.getIns().setStartTime(System.currentTimeMillis());
        //END SFの割当先に対してInterestパケットを送る．
        while(endIte.hasNext()){
            Long endID = endIte.next();
            VNF endVNF = sfc.findVNFByLastID(endID);
            //END SFの割当先を決める．
            AutoICNSFCScheduling sched = AutoICNSFCMgr.getIns().getSched();
            sched.scheduleEndSF(endVNF, job);
            ///割り当て前提の部分
            VCPU vcpu = env.getGlobal_vcpuMap().get(endVNF.getvCPUID());
            VM host = NCLWUtil.findVM(env, vcpu.getPrefix());
            NFDTask endTask = job.getNfdTaskMap().get(endID);
            //readFilePath: 送るファイルのpath, //相手側で受信後，入力ファイル生成path
            NCLWData data = new NCLWData(endID, -1, host.getIpAddr(), NCLWUtil.NFD_PORT, sfc, env, job);
            data.setFile(false);
            Name endPrefix = NclwNFDMgr.getIns().createPrefix(endTask, null);
            /*AutoICNFaceThread ft = new AutoICNFaceThread(host.getIpAddr(), NclwNFDMgr.getIns().getOwnIPAddr(), endPrefix);
            Thread fThread = new Thread(ft);
            fThread.start();
            */
            TcpFace toFace = NclwNFDMgr.getIns().createFace(host.getIpAddr(), NclwNFDMgr.getIns().getOwnIPAddr());
            NclwNFDMgr.getIns().getFib().insert(endPrefix, toFace, 1);


            System.out.println("**INTEEST SEND: FROM:"+ResourceMgr.getIns().getOwnIPAddr() + "/To:"+host.getIpAddr());
            Name targetName = NclwNFDMgr.getIns().createPrefix(endTask, null);
            Interest newInterest = new Interest(targetName);
            newInterest.setApplicationParameters(new Blob(data.getAllBytes()));

            //toFace.sendInterest(newInterest);
            //当該データを，END SFのノードへ送る．
            sender.getInterestDataQueue().add(data);
        }




    }
}
