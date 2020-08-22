package org.ncl.workflow.ccn.sfc.process;

import org.ncl.workflow.comm.NCLWData;
import org.ncl.workflow.logger.NclwLog;
import org.ncl.workflow.util.NCLWUtil;

import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Hidehiro Kanemitsu on 2020/08/09.
 */
public class NFDInterestEngine implements Serializable, Runnable {


    private static NFDInterestEngine own;

    protected long nodeID;

    /**
     * Thread Pool for sending/receiving data
     */
    protected ExecutorService exec;


    public static NFDInterestEngine getIns() {
        if (NFDInterestEngine.own == null) {
            NFDInterestEngine.own = new NFDInterestEngine();

        } else {

        }
        return NFDInterestEngine.own;
    }

    private NFDInterestEngine() {
        this.exec = Executors.newFixedThreadPool(10);

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
     * Interestパケット送受信用スレッド
     */
    public void startSendRecvThreads(){
        try{
            //NFDポートで待受
            ServerSocket listen_socket = new ServerSocket(NCLWUtil.NFD_PORT);
           // this.exec.submit(new NclwNFDSendThread());
            NclwLog.getIns().log("Listening INTEREST  at Port:"+NCLWUtil.NFD_PORT);
            while(true){
                Thread.sleep(10);
                Socket client = listen_socket.accept();

                this.exec.submit(new NclwNFDInterestRecvThread( client));
            }


        }catch(Exception e){
            e.printStackTrace();
        }
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
}
