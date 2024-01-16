package net.gripps.ccn.main;

import java.io.ObjectOutputStream;
import java.net.Socket;

public class Client {

    public static void main(String[] args){
        try{
            //ソケットの準備
            Socket socket = new Socket("localhost", 1234);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(new Person(28, "tanaka"));
            out.close();
        }catch(Exception e){
            e.printStackTrace();
        }

    }
}
