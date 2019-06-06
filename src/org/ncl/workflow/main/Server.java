package org.ncl.workflow.main;

import org.ncl.workflow.comm.NCLWData;
import org.ncl.workflow.comm.RecvThread;
import org.ncl.workflow.comm.SendThread;
import org.ncl.workflow.comm.ServerMgr;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Hidehiro Kanemitsu on 2019/05/03.
 */
public class Server {
    final static int PORT = 8001;	// 待受ポート番号
    static String writePath;

    public static void main(String[] args) {
        while(true){
            try{
                ServerMgr.getIns().process();

            }catch(Exception e){
                e.printStackTrace();
            }

        }

        //RecvThread receiver2 = new RecvThread(8001);



        //Thread t2 = new Thread(receiver2);
       // t2.start();

    }
}
