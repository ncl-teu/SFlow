package org.ncl.workflow.ccn.sfc.process;

import net.gripps.cloud.core.VCPU;
import net.gripps.cloud.core.VM;
import net.gripps.cloud.nfv.NFVEnvironment;
import net.gripps.cloud.nfv.sfc.SFC;
import net.gripps.cloud.nfv.sfc.VNF;
import net.named_data.jndn.*;
import net.named_data.jndn.util.Blob;
import org.ncl.workflow.ccn.core.NclwNFDMgr;
import org.ncl.workflow.ccn.core.NclwNameInfo;
import org.ncl.workflow.comm.NCLWData;
import org.ncl.workflow.comm.SendThread;
import org.ncl.workflow.comm.WorkflowJob;
import org.ncl.workflow.engine.Task;
import org.ncl.workflow.util.NCLWUtil;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
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
        try {
            while (true) {
                if (!this.interestDataQueue.isEmpty()) {
                    NCLWData data = this.interestDataQueue.poll();
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


            //data.getJob().getTaskMap().get(data.get)

           Face face = new Face(ipAddr);
            //com.intel.jndn.forwarder.api.Face face = NclwNFDMgr.getIns().createFace(ipAddr);

            Interest interest = new Interest();
            interest.setName(name);
            interest.setCanBePrefix(true);

            //子供側のタスクIDをchildへ設定する．
            //interest.setChildSelector((Integer.valueOf(toVNF.getIDVector().get(1).toString())).intValue());
            byte[] bs = null;
            bs = data.getAllBytes();
            interest.setApplicationParameters(new Blob(bs));
            //NclwNFDMgr.getIns().getPipeline().processSendData(data);
            //PipelineのonContentStoreHitやonIncomingDataメソッドをcall
            //してdataを送る．
            //Interestパケット送信
           face.expressInterest(interest, this);


            while (true) {
                face.processEvents();
                if(this.finishFlag){
                    break;
                }
                try {
                    Thread.sleep(10);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
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
                    System.out.println("****269***");
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
