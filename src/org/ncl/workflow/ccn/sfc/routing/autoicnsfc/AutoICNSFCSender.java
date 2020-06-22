package org.ncl.workflow.ccn.sfc.routing.autoicnsfc;

import org.ncl.workflow.util.NCLWUtil;

import java.io.IOException;
import java.io.Serializable;
import java.net.*;
import java.util.*;

/**
 * Created by Hidehiro Kanemitsu on 2020/03/01.
 * //情報を送信するためのメソッドです．
 * //ブロードキャスト: 初期の自信の情報
 * //LSA: 自身のFIBの情報
 */
public class AutoICNSFCSender implements Serializable, Runnable {

    private  DatagramSocket socket = null;



    private AutoICNSFCSender(){

    }

    @Override
    public void run() {
        while(true){

        }
    }

    public  void broadcastSend(
            String broadcastMessage) throws IOException {


        InetSocketAddress isAddress = new InetSocketAddress( NCLWUtil.ccn_bcastaddress,NCLWUtil.ccn_bcastport );
        byte[] buffer = "NOTIFICATION".getBytes();
        DatagramPacket packet = new DatagramPacket( buffer, buffer.length, isAddress );

        new DatagramSocket().send( packet );

    }

    List<InetAddress> listAllBroadcastAddresses() throws SocketException {
        List<InetAddress> broadcastList = new ArrayList<>();
        Enumeration<NetworkInterface> interfaces
                = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();

            if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                continue;
            }

            networkInterface.getInterfaceAddresses().stream()
                    .map(a -> a.getBroadcast())
                    .filter(Objects::nonNull)
                    .forEach(broadcastList::add);
        }
        return broadcastList;
    }
}
