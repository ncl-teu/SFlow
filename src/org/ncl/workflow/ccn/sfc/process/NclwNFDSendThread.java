package org.ncl.workflow.ccn.sfc.process;

import com.intel.jnfd.deamon.face.tcp.TcpFace;
import com.intel.jnfd.deamon.fw.RetxSuppression;
import com.intel.jnfd.deamon.table.fib.FibEntry;
import com.intel.jnfd.deamon.table.fib.FibNextHop;
import com.intel.jnfd.deamon.table.pit.PitEntry;
import net.gripps.cloud.core.VCPU;
import net.gripps.cloud.core.VM;
import net.gripps.cloud.nfv.NFVEnvironment;
import net.gripps.cloud.nfv.sfc.SFC;
import net.gripps.cloud.nfv.sfc.VNF;
import net.gripps.clustering.common.aplmodel.DataDependence;
import net.named_data.jndn.*;
import net.named_data.jndn.util.Blob;
import org.ncl.workflow.ccn.core.NclwNFDMgr;
import org.ncl.workflow.ccn.core.NclwNameInfo;
import org.ncl.workflow.ccn.util.NetInfo;
import org.ncl.workflow.comm.NCLWData;
import org.ncl.workflow.comm.SendThread;
import org.ncl.workflow.comm.WorkflowJob;
import org.ncl.workflow.engine.Task;
import org.ncl.workflow.logger.NclwLog;
import org.ncl.workflow.util.NCLWUtil;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Hidehiro Kanemitsu on 2019/11/19
 */
public class NclwNFDSendThread extends SendThread implements OnData, OnInterestCallback {
    private boolean finishFlag;

    /**
     * Interestパケット用のデータを格納するためのキュー
     */
    protected LinkedBlockingQueue<NCLWData> interestDataQueue;


    public NclwNFDSendThread(LinkedBlockingQueue<NCLWData> dataQueue) {
        super(dataQueue);
        this.interestDataQueue = new LinkedBlockingQueue<NCLWData>();
    }

    public NclwNFDSendThread() {
        super();
        this.interestDataQueue = new LinkedBlockingQueue<NCLWData>();
    }

    public boolean isFinishFlag() {
        return finishFlag;
    }

    public void setFinishFlag(boolean finishFlag) {
        this.finishFlag = finishFlag;
    }

    public LinkedBlockingQueue<NCLWData> getInterestDataQueue() {
        return interestDataQueue;
    }

    public void setInterestDataQueue(LinkedBlockingQueue<NCLWData> interestDataQueue) {
        this.interestDataQueue = interestDataQueue;
    }

    @Override
    public void onData(Interest interest, Data data) {
        System.out.println("***Data Arrived!!***");
        System.out.println("Prefix:" + interest.getName().toUri() + "/ Data:" + data.getContent().toString());
        //this.finishFlag = true;

    }

    @Override
    public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId,
                           InterestFilter filter) {
        System.out.println("***Interest Arrived!!***");


    }

    @Override
    public void run() {
        NclwLog.getIns().log("----NclwNFDSendThread START---");
        try {
            while (true) {
                if (!this.interestDataQueue.isEmpty()) {
                    NCLWData data = this.interestDataQueue.poll();
                    NclwLog.getIns().log("|InterestQueue| > 0.");
                    this.sendInterestProcess(data);
                }

                if (!this.dataQueue.isEmpty()) {
                    NCLWData data = this.dataQueue.poll();
                    this.sendFileProcess(data);
                }

                Thread.sleep(10);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * Inerestパケット送信用処理
     * Delegatorのみから呼ばれる．
     * それ以外のルータからは，PipelineのOnOutgoingInterestが呼ばれる．
     * @param data
     */
    public void sendInterestProcess(NCLWData data) {
        boolean isFile = data.isFile();
        String readPath = data.getReadFilePath();
        //data内の当該VNFを処理すべきホストのIP
        String ipAddr = data.getIpAddr();
        int port = data.getPortNumber();
        NFVEnvironment env = data.getEnv();

        SFC sfc = data.getSfc();
        VNF srcVNF = sfc.findVNFByLastID(data.getFromTaskID());
        VNF toVNF = sfc.findVNFByLastID(data.getToTaskID());

        VCPU pitVcpu = env.getGlobal_vcpuMap().get(data.getToTaskID());
        if(pitVcpu != null){
            VM host = NCLWUtil.findVM(data.getEnv(), pitVcpu.getPrefix());
            data.setPitIPAddr(host.getIpAddr());

        }else{
            //delegatorからのものであれば，pitをelegatorに設定
            data.setPitIPAddr(NCLWUtil.delegator_ip);
        }

        this.finishFlag = false;
        try {
            NFDTask task = (NFDTask)data.getJob().getNfdTaskMap().get(srcVNF.getIDVector().get(1));
            NFDTask toTask = null;
            if(toVNF != null){
                 toTask = (NFDTask)data.getJob().getNfdTaskMap().get(toVNF.getIDVector().get(1));



            }
            //Interest.setDefaultCanBePrefix(true);
            //Name規則をどうするか
            // Name name = new Name(NCLWUtil.NCLW_PREFIX+task.getCmd()/*+ Math.floor(Math.random() * 100000)*/);
            Name name = NclwNFDMgr.getIns().createPrefix(task, toTask);



           Face face = new Face(ipAddr);
            //com.intel.jndn.forwarder.api.Face face = NclwNFDMgr.getIns().createFace(ipAddr);

            Interest interest = new Interest();
            interest.setName(name);
            interest.setCanBePrefix(true);

            byte[] bs = null;
            bs = data.getAllBytes();
            interest.setApplicationParameters(new Blob(bs));
            //NclwNFDMgr.getIns().getPipeline().processSendData(data);
            //PipelineのonContentStoreHitやonIncomingDataメソッドをcall
            //してdataを送る．
            //Interestパケット送信
           //face.expressInterest(interest, this);
            this.processInterest(interest);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void processInterest(Interest interest){
        if(interest.getName().toUri().startsWith(NCLWUtil.NCLW_PREFIX)){
            NCLWData sfcData = NclwNFDMgr.getIns().fetchNCLWData(interest);
            WorkflowJob job = sfcData.getJob();
            NFDTask predTask = job.getNfdTaskMap().get(sfcData.getFromTaskID());

            String targetIP = sfcData.getIpAddr();
            FibEntry fibE = NclwNFDMgr.getIns().getFib().findExactMatch(interest.getName());
            List<FibNextHop> nList  = fibE.getNextHopList();
            Iterator<FibNextHop> fIte = nList.iterator();
            long minCost = Long.MAX_VALUE;
            TcpFace oFace = null;
            //Fibエントリのfaceのうち最低コストのFaceを決めるためのループ
            while(fIte.hasNext()){
                FibNextHop nextHop = fIte.next();
                long cost = nextHop.getCost();
                if(cost <= minCost){
                    minCost = cost;
                    oFace = (TcpFace)nextHop.getFace();
                }
            }

            PitEntry pitEntry = NclwNFDMgr.getIns().getPit().insert(interest).getFirst();
            pitEntry.insertOrUpdateInRecord(oFace, interest);

            NclwNFDMgr.getIns().getPipeline().onOutgoingInterest( (NFDTask)predTask, (NFDTask)null, sfcData, pitEntry, oFace, false);

        }



    }

    /**
     * 処理結果を送信するための処理です．
     * PipelineへNCLWDataを渡す．これだけ．
     * @param data
     */
    @Override
    public void sendFileProcess(NCLWData data) {
        boolean isFile = data.isFile();
        String readPath = data.getReadFilePath();
        String ipAddr = data.getIpAddr();
        int port = data.getPortNumber();

        NFVEnvironment env = data.getEnv();
        SFC sfc = data.getSfc();
        VNF srcVNF = sfc.findVNFByLastID(data.getFromTaskID());

        try{

            // ソケットの準備
            /*Socket socket = new Socket(ipAddr, port);
            ObjectOutputStream outStrm=new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream inStrm=new ObjectInputStream(socket.getInputStream());

             */
            if(isFile){
                File file = new File(readPath);
                data.setFile(file);
                if(srcVNF != null){
                    VM srcVM = NCLWUtil.findVM(env, srcVNF.getvCPUID());
                    String srcIP = srcVM.getIpAddr();
                    if(srcIP.equals(data.getIpAddr())){
                        //data.setIpAddr("localhost");
                    }
                }

                FileInputStream is = new FileInputStream(file);
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                while(true) {
                    int len = is.read(buffer);
                    if(len < 0) {
                        break;
                    }
                    bout.write(buffer, 0, len);

                }
                //結果がファイルの場合は，ファイルをバイト列に変換してセットする．
                data.setBytes(bout.toByteArray());

                //return bout.toByteArray();


                // FileInputStream fis = new FileInputStream(file);
                //data.setFis(fis);
            }else{

            }
            //送信元IP == 送信先IPの場合は，プールからタスクを取り出して，InMapにdataをセットするのみ．
           // SFC sfc = data.getSfc();
           // NFVEnvironment env = data.getEnv();
            VNF predVNF = sfc.findVNFByLastID(data.getFromTaskID());
            VNF toVNF = sfc.findVNFByLastID(data.getToTaskID());
            if(toVNF != null) {
                VM predHost = NCLWUtil.findVM(env, predVNF.getvCPUID());
                VM toHost = NCLWUtil.findVM(env, toVNF.getvCPUID());
                System.out.println("**PREFIX:"+data.getJob().getJobID() + "^" + data.getToTaskID());

                //もしpred/toが同一ホストに割り当てられているなら，Interestは送信せずにカウンタのみを+するだけ．
                //この時点で，自分が対象ホストであることが保証されている．
                if (predHost.getIpAddr().equals(toHost.getIpAddr())) {
                    WorkflowJob job = data.getJob();
                    String prefix = job.getJobID() + "^" + data.getToTaskID();
                    HashMap<String, NFDTask> taskPool = NFDTaskEngine.getIns().getTaskPool();
                    NFDTask task;
                    if (taskPool.containsKey(prefix)) {
                        task = (NFDTask) NFDTaskEngine.getIns().getTaskPool().get(prefix);
                    } else {
                        task = (NFDTask) job.getNfdTaskMap().get(data.getFromTaskID());
                        NFDTaskEngine.getIns().getTaskPool().put(prefix, task);
                    }

                    Thread taskThread = new Thread(task);
                    task.setVnf(sfc.findVNFByLastID(data.getToTaskID()));
                    task.setJobID(job.getJobID());

                    task.setJob(job);
                    task.setSfc(sfc);
                    task.setEnv(data.getEnv());
                    if(!task.isStarted()){
                        taskThread.start();

                    }
                    //結果を入力へ入れる．
                    task.getInDataMap().put(predVNF.getIDVector().get(1), data);
                    return;
                }else{
                    NclwNFDMgr.getIns().getPipeline().processSendData(data);
//Socketを使うように変更
                  //  this.processOutData(data);
                }
            }else{
                //Socketを使うように変更
               // this.processOutData(data);
                NclwNFDMgr.getIns().getPipeline().processSendData(data);

            }
            //PipelineへData送信を任せる．

        }catch(Exception e){
            e.printStackTrace();
        }

    }


}
