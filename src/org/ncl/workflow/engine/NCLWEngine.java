package org.ncl.workflow.engine;

import org.ncl.workflow.comm.NCLWData;
import org.ncl.workflow.comm.RecvThread;
import org.ncl.workflow.comm.SendThread;
import org.ncl.workflow.comm.WorkflowJob;
import org.ncl.workflow.util.NCLWUtil;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Hidehiro Kanemitsu on 2019/04/26.
 * This starts monitoring resource information and waiting data from
 * predecessor tasks.
 */
public class NCLWEngine implements Runnable {

    /**
     * Singleton Instance
     */
    private static NCLWEngine own;

    /**
     * Node ID.
     * This process is run for each vCPU.
     */
    private long nodeID;

    /**
     * Thread Pool for sending/receiving data
     */
    private ExecutorService exec;


    /**
     * Map of workflow jobs.
     */
    private HashMap<Long, WorkflowJob> jobMap;


    private HashMap<String, Task> taskPool;

    private int port;


    private NCLWEngine() {
        this.initialize();
    }

    public static NCLWEngine getIns() {
        if (NCLWEngine.own == null) {
            NCLWEngine.own = new NCLWEngine();
        }

        return NCLWEngine.own;
    }

    /**
     * Initialize all processes related to NCLW.
     * load schedule file and configures tasks and threads.
     */
    public void initialize() {
        this.exec = Executors.newFixedThreadPool(10);
        this.jobMap = new HashMap<Long, WorkflowJob>();
        this.port = NCLWUtil.port;
        this.taskPool = new HashMap<String, Task>();

       // this.taskMap = new HashMap<Long, Task>(10);
        //Obtain IP address
    }

    public ExecutorService getExec() {
        return exec;
    }

    public void setExec(ExecutorService exec) {
        this.exec = exec;
    }


    @Override
    public void run() {
        //Start send/recv threads from the threadpool.
        this.startSendRecvThreads();

        while (true) {
            try {

            } catch (Exception e) {

            }
        }
    }

    public void startSendRecvThreads(){
        try{
            ServerSocket listen_socket = new ServerSocket(this.port);
            this.exec.submit(new SendThread());
            while(true){
                Thread.sleep(100);
                Socket client = listen_socket.accept();
                //RecvThread receiver = new RecvThread( port, client);
                this.exec.submit(new RecvThread(new LinkedBlockingQueue<NCLWData>(), this.port, client));
                //Thread t = new Thread(new RecvThread(new LinkedBlockingQueue<NCLWData>(), this.port, client));

               // Thread t = new Thread(receiver);
            //    t.start();
            }


        }catch(Exception e){
            e.printStackTrace();
        }
      /*
        for(int i=0;i<NCLWUtil.rmgr_recv_thread_num;i++){
            this.exec.submit(new RecvThread(new LinkedBlockingQueue<NCLWData>(), this.port));
        }

        for(int j=0;j<NCLWUtil.rmgr_send_thread_num;j++){
            this.exec.submit(new SendThread());
       }

       */
    }

    public HashMap<Long, WorkflowJob> getJobMap() {
        return jobMap;
    }

    public void setJobMap(HashMap<Long, WorkflowJob> jobMap) {
        this.jobMap = jobMap;
    }


    public long getNodeID() {
        return nodeID;
    }

    public void setNodeID(long nodeID) {
        this.nodeID = nodeID;
    }

    public HashMap<String, Task> getTaskPool() {
        return taskPool;
    }

    public void setTaskPool(HashMap<String, Task> taskPool) {
        this.taskPool = taskPool;
    }


}
