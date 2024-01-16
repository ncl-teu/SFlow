package net.gripps.ccn.main;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    public static void main(String[] args){
        try{
            ServerSocket socket = new ServerSocket(1234);
            while(true){
                Thread.sleep(100);
                Socket client = socket.accept();
                InputStream is = client.getInputStream();
                ObjectInputStream in = new ObjectInputStream(is);
                Person p = (Person)in.readObject();
                System.out.println("age:"+p.getAge() + "/ name:"+p.getName());

               //BufferedReader reader = new BufferedReader(
                       // new InputStreamReader(client.getInputStream()));
                //PrintWriter writer = new PrintWriter(client.getOutputStream(), true);
                //writer.println();
               // String msg = reader.readLine();
              //  System.out.println("Received Msg: " + msg);

            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
