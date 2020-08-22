package org.ncl.workflow.ccn.sfc.process;

import org.ncl.workflow.comm.NCLWData;
import org.ncl.workflow.comm.RecvThread;
import org.ncl.workflow.comm.SendThread;
import org.ncl.workflow.comm.WorkflowJob;
import org.ncl.workflow.engine.NCLWEngine;
import org.ncl.workflow.engine.Task;
import org.ncl.workflow.logger.NclwLog;
import org.ncl.workflow.util.NCLWUtil;

import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Hidehiro Kanemitsu on 2019/11/22.
 */
public class NFDTaskEngine implements Serializable, Runnable {


    private  static NFDTaskEngine  own;


    private HashMap<String, NFDTask> taskPool;


    /**
     * Node ID.
     * This process is run for each vCPU.
     */
    protected long nodeID;

    /**
     * Thread Pool for sending/receiving data
     */
    protected ExecutorService exec;


    /**
     * Map of workflow jobs.
     */
    protected HashMap<Long, WorkflowJob> jobMap;



    protected int port;

    public static NFDTaskEngine getIns() {
        if (NFDTaskEngine.own == null) {
            NFDTaskEngine.own = new NFDTaskEngine();
        }

        return NFDTaskEngine.own;
    }


    private  NFDTaskEngine() {
        this.initialize();
    }

    public void initialize() {

        this.port = NCLWUtil.NFD_PORT;
        this.exec = Executors.newFixedThreadPool(10);
        this.jobMap = new HashMap<Long, WorkflowJob>();
        this.taskPool = new HashMap<String, NFDTask>();


    }


    @Override
    public void run() {
        //Start send/recv threads from the threadpool.
        this.startSendRecvThreads();

        while (true) {
            try {
            //指定秒ごとに，リソース情報取得のInterestを投げる．

            } catch (Exception e) {

            }
        }
    }




    /**
     * Dataパケット送受信用スレッド
     */
    public void startSendRecvThreads(){
        try{
            ServerSocket listen_socket = new ServerSocket(NCLWUtil.port);
            //this.exec.submit(new NclwNFDSendThread());
            NclwLog.getIns().log("Listening DATA  at Port:"+NCLWUtil.port);

            while(true){
                Thread.sleep(100);
                Socket client = listen_socket.accept();
                this.exec.submit(new NclwNFDRecvThread(new LinkedBlockingQueue<NCLWData>(), NCLWUtil.port, client));
            }


        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public HashMap<String, NFDTask> getTaskPool() {
        return taskPool;
    }

    public void setTaskPool(HashMap<String, NFDTask> taskPool) {
        this.taskPool = taskPool;
    }

    public long getNodeID() {
        return nodeID;
    }

    public void setNodeID(long nodeID) {
        this.nodeID = nodeID;
    }

    public ExecutorService getExec() {
        return exec;
    }

    public void setExec(ExecutorService exec) {
        this.exec = exec;
    }

    public HashMap<Long, WorkflowJob> getJobMap() {
        return jobMap;
    }

    public void setJobMap(HashMap<Long, WorkflowJob> jobMap) {
        this.jobMap = jobMap;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
