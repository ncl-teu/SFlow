package org.ncl.workflow.comm;


import net.gripps.cloud.core.VM;
import net.gripps.cloud.nfv.NFVEnvironment;
import net.gripps.cloud.nfv.sfc.SFC;
import net.gripps.cloud.nfv.sfc.VNF;
import org.ncl.workflow.util.NCLWUtil;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Hidehiro Kanemitsu on 2019/05/01.
 * サーバ側で空いているポートにbindして送信する．
 *
 */
public class SendThread implements Runnable {

    protected LinkedBlockingQueue<NCLWData> dataQueue;

    public SendThread(LinkedBlockingQueue<NCLWData> dataQueue) {
        this.dataQueue = dataQueue;
    }

    public SendThread() {
        this.dataQueue = new LinkedBlockingQueue<NCLWData>();

    }


    @Override
    public void run() {
        try {
            while (true) {
                if (!this.dataQueue.isEmpty()) {
                    NCLWData data = this.dataQueue.poll();
                    this.sendFileProcess(data);
                } else {
                    Thread.sleep(100);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

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
                Socket socket = new Socket(ipAddr, port);
                ObjectOutputStream outStrm=new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream inStrm=new ObjectInputStream(socket.getInputStream());
                if(isFile){
                    File file = new File(readPath);
                    data.setFile(file);
                    if(srcVNF != null){
                        VM srcVM = NCLWUtil.findVM(env, srcVNF.getvCPUID());
                        String srcIP = srcVM.getIpAddr();
                        if(srcIP.equals(data.getIpAddr())){
                            data.setIpAddr("localhost");
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
                    data.setBytes(bout.toByteArray());

                        //return bout.toByteArray();


                   // FileInputStream fis = new FileInputStream(file);
                    //data.setFis(fis);
                }else{

                }
                outStrm.writeObject(data);

                outStrm.flush();
               // System.out.println("Sent data");
            }catch(Exception e){
                e.printStackTrace();
            }

    }

    public LinkedBlockingQueue<NCLWData> getDataQueue() {
        return dataQueue;
    }

    public void setDataQueue(LinkedBlockingQueue<NCLWData> dataQueue) {
        this.dataQueue = dataQueue;
    }
}
