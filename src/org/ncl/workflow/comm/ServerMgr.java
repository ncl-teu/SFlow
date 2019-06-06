package org.ncl.workflow.comm;

import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Hidehiro Kanemitsu on 2019/05/24
 */
public class ServerMgr implements Serializable {

    private static ServerMgr own;

    public static ServerMgr getIns(){
        if(ServerMgr.own == null){
            ServerMgr.own = new ServerMgr();
        }else{

        }
        return ServerMgr.own;
    }

    private ServerMgr(){

    }

    public void process(){
        int port = 8001;
        try{
            ServerSocket listen_socket = new ServerSocket(port);


            while(true){
                //Thread.sleep(1000);
                Socket client = listen_socket.accept();
                RecvThread receiver = new RecvThread( port, client);
                Thread t = new Thread(receiver);
                t.start();
            }
        }catch(Exception e){
            e.printStackTrace();
        }


    }
}
