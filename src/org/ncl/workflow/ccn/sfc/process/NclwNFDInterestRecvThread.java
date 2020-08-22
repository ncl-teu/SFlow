package org.ncl.workflow.ccn.sfc.process;

import com.intel.jnfd.deamon.face.tcp.TcpFace;
import net.gripps.cloud.nfv.sfc.SFC;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;
import org.ncl.workflow.ccn.core.NclwInterest;
import org.ncl.workflow.ccn.core.NclwNFDMgr;
import org.ncl.workflow.comm.NCLWData;
import org.ncl.workflow.comm.RecvThread;
import org.ncl.workflow.comm.WorkflowJob;
import org.ncl.workflow.engine.NCLWEngine;
import org.ncl.workflow.engine.Task;
import org.ncl.workflow.logger.NclwLog;
import org.ncl.workflow.util.NCLWUtil;
import org.ncl.workflow.util.ProcessMgr;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

/**
 * Created by Hidehiro Kanemitsu on 2020/08/09.
 */
public class NclwNFDInterestRecvThread extends RecvThread {

    public NclwNFDInterestRecvThread(Socket in_client){
        this.client = in_client;
    }


    @Override
    public void recvProcess() {
        byte[] buffer = new byte[512]; // データ受信時のバッファ
        String msg = null;
        try {

/*
            ServerSocket listen_socket = new ServerSocket(NCLWUtil.NFD_PORT);
            Socket client = listen_socket.accept();
*/
            InputStream inputStream = this.client.getInputStream();

            ObjectInputStream in = new ObjectInputStream(inputStream);
            ObjectOutputStream out = new ObjectOutputStream(this.client.getOutputStream());

            //Interestを取得する．
            NclwInterest nclwInterest = (NclwInterest) in.readObject();
            Interest interest = new Interest(new Name(nclwInterest.getName()));
            interest.setApplicationParameters(new Blob(nclwInterest.getApplicationParameters()));
            NCLWData data = NclwNFDMgr.getIns().fetchNCLWData(interest);
            //interestのNclwDataから，送信元を取得する．
            String srcIP = data.getOrgIP();
            NclwLog.getIns().log("INTEREST Received from "+srcIP + ": Prefix:"+interest.getName().toString());

            //Faceを取得する．
            TcpFace remoteFace = NclwNFDMgr.getIns().getFace(srcIP);

            //ForwarderのonInterestを呼ぶ．
            NclwNFDMgr.getIns().getFw().onInterest(interest, remoteFace);


            out.flush();
            out.close();
            in.close();
            //listen_socket.close();
            this.client.close();
            this.endFlg = true;


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
