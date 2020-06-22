package org.ncl.workflow.ccn.sfc.routing.autoicnsfc;

import org.ncl.workflow.util.NCLWUtil;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Date;

/**
 * Created by Hidehiro Kanemitsu on 2020/03/01.
 */
public class AutoICNSFCReceiver {


    public void recv() throws Exception {
        DatagramSocket dgSocket = new DatagramSocket(NCLWUtil.ccn_bcastport);

        byte buffer[] = new byte[1024];
        DatagramPacket packet =
                new DatagramPacket(buffer, buffer.length);

        while (true) {
            dgSocket.receive(packet);

            System.out.print (new String(packet.getData(),
                    0, packet.getLength()));
            System.out.println( ": " + new Date() );
        }
    }
}
