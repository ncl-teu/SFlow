package org.ncl.workflow.ccn.sfc.process;

import net.gripps.cloud.nfv.sfc.SFC;
import org.ncl.workflow.ccn.core.NclwNFDMgr;
import org.ncl.workflow.comm.NCLWData;
import org.ncl.workflow.comm.RecvThread;
import org.ncl.workflow.comm.WorkflowJob;
import org.ncl.workflow.engine.NCLWEngine;
import org.ncl.workflow.engine.Task;
import org.ncl.workflow.util.NCLWUtil;
import org.ncl.workflow.util.ProcessMgr;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Hidehiro Kanemitsu on 2019/11/22.
 */
public class NclwNFDRecvThread extends RecvThread {

    public NclwNFDRecvThread() {
    }

    public NclwNFDRecvThread(LinkedBlockingQueue<NCLWData> recvDataQueue, int port, Socket in_socket) {
        super(recvDataQueue, port, in_socket);
    }

    public NclwNFDRecvThread(int port, Socket in_client) {
        super(port, in_client);
    }

    public NclwNFDRecvThread(LinkedBlockingQueue<NCLWData> recvDataQueue, int port) {
        super(recvDataQueue, port,null);
    }

    public NclwNFDRecvThread(int port) {
        super(port, null);
    }

    /**
     * NCLWDataを受信して，タスクへと渡す．
     */
    @Override
    public void recvProcess() {
        byte[] buffer = new byte[512]; // ファイル受信時のバッファ
        String msg = null;
        try {
/*
            ServerSocket listen_socket = new ServerSocket(this.port);
            Socket client = listen_socket.accept();
*/
            InputStream inputStream = this.client.getInputStream();


            ObjectInputStream in = new ObjectInputStream(inputStream);
            ObjectOutputStream out = new ObjectOutputStream(this.client.getOutputStream());
            NCLWData data = (NCLWData) in.readObject();

            //System.out.println("ip:" + data.getIpAddr() + "/readpath:" + data.getReadFilePath() + "/writePath:" + data.getWriteFilePath());
            //もしFromTaskIDがnullなら，startTask
            WorkflowJob job = data.getJob();
            SFC sfc = data.getSfc();
            NFDTask task;

            //まず，pitIpAddr = 自分であれば色々処理する．
            //そうでなければ，pitIpAddrにセットして送る．
            if(data.getPitIPAddr().equals(NclwNFDMgr.getIns().getOwnIPAddr())){
                if(data.getToTaskID() == -1){
                    if(data.isFile()){
                        File file = data.getFile();
                        String path = file.getPath();
                        String dir = file.getParent();
                        if(!(dir == null)){
                            File dir2 = new File(file.getParent());
                            if(!dir2.exists()){
                                dir2.mkdirs();
                            }
                        }

                        // String outPath = "/"+file.getParent()+ "/"+file.getName();
                        //FileInputStream fis = new FileInputStream(file);
                        ByteArrayInputStream bis = new ByteArrayInputStream(data.getBytes());
                        //FileInputStream fis = data.getFis();
                        FileOutputStream fos = new FileOutputStream(data.getWriteFilePath());
                        int fileLength;
                        while ((fileLength = bis.read(buffer)) > 0) {
                            //while ((fileLength = fis.read(buffer)) > 0) {
                            fos.write(buffer, 0, fileLength);
                        }
                        fos.flush();
                        fos.close();
                        //  fis.close();
                        bis.close();
                        out.flush();
                        out.close();
                        in.close();
                        System.out.println("Obtained File:"+file.getPath());
                    }
                    System.out.println("Obtained Msg:"+data.getMsg());
                    ProcessMgr.getIns().setFinishTime(System.currentTimeMillis());
                    System.out.println("Elappsed time:"+ NCLWUtil.getRoundedValue((ProcessMgr.getIns().getFinishTime() - ProcessMgr.getIns().getStartTime())/(double)1000) + "(sec)");
                    System.exit(0);

                }
                String prefix = job.getJobID()+"^"+data.getToTaskID();
                HashMap<String, NFDTask> taskPool = NFDTaskEngine.getIns().getTaskPool();
                if(taskPool.containsKey(prefix)){
                    task = NFDTaskEngine.getIns().getTaskPool().get(prefix);
                }else{
                    task = job.getNfdTaskMap().get(data.getToTaskID());
                    NFDTaskEngine.getIns().getTaskPool().put(prefix, task);
                }


                System.out.println("Data arrived:"+data.getFromTaskID() + "->"+data.getToTaskID());

                Thread taskThread = new Thread(task);
                task.setVnf(sfc.findVNFByLastID(data.getToTaskID()));
                task.setJobID(job.getJobID());
                task.setJob(job);
                task.setSfc(sfc);
                task.setEnv(data.getEnv());


                //The case that from Delegator due to backtracking
                if(data.getFromTaskID() == -1){


                }else{
                    if(data.isFile()){

                        File file = data.getFile();
                        String path = file.getPath();
                        String dir = file.getParent();
                        if(!(dir == null)){
                            File dir2 = new File(file.getParent());
                            if(!dir2.exists()){
                                dir2.mkdirs();
                            }
                        }

                        // String outPath = "/"+file.getParent()+ "/"+file.getName();
                        //FileInputStream fis = new FileInputStream(file);
                        ByteArrayInputStream bis = new ByteArrayInputStream(data.getBytes());
                        //FileInputStream fis = data.getFis();
                        if(file.exists()){
                        }else{
                            FileOutputStream fos = new FileOutputStream(data.getWriteFilePath());
                            int fileLength;
                            while ((fileLength = bis.read(buffer)) > 0) {
                                //while ((fileLength = fis.read(buffer)) > 0) {
                                fos.write(buffer, 0, fileLength);
                            }
                            fos.flush();
                            fos.close();
                        }


                        //  fis.close();
                        bis.close();
                        out.flush();
                        out.close();
                        in.close();
                        System.out.println("Obtained File:"+file.getPath());

                    }
                    task.getInDataMap().put(data.getFromTaskID(), data);
                    //   int currentCnt = task.getArrivedCnt();
                    // task.setArrivedCnt(currentCnt+1);
                    //System.out.println("TaskID:"+task.getTaskID() + "CNT:"+task.getArrivedCnt());

                }

                if(!task.isStarted()){
                    taskThread.start();

                }



                out.flush();
                out.close();
                in.close();
                //listen_socket.close();
                this.client.close();
                this.endFlg = true;
            }else{
                //Pitに対して送信する準備
                //自身のPitから取り出して更新する．
                NclwNFDMgr.getIns().getPipeline().processSendData(data);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
