package org.ncl.workflow.ccn.sfc.process;

import com.intel.jndn.forwarder.Forwarder;
import com.intel.jndn.forwarder.api.FaceManager;
import com.intel.jnfd.deamon.fw.ForwardingPipeline;
import org.ncl.workflow.ccn.core.NclwNFDMgr;
import org.ncl.workflow.ccn.util.Setup;
import org.ncl.workflow.logger.NclwLog;
import org.ncl.workflow.util.NCLWUtil;

import java.net.Inet4Address;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by Hidehiro Kanemitsu on 2019/12/23.
 */
public class NclwNFD {

    public NclwNFD() {
    }



    public void process(long nodeID, String propName, String hostFile) {
        try {
            //Set parameters
            NCLWUtil.getIns().initialize(propName);
            //Start Interest Engine
            NFDInterestEngine.getIns().setNodeID(nodeID);
            Thread interestThread = new Thread(NFDInterestEngine.getIns());
            interestThread.start();
            NclwLog.getIns().log("-----NFDInterestEngine for Receiving INTEREST START----");

            //Start Task  Engine.
            NFDTaskEngine.getIns().setNodeID(nodeID);
            Thread mgmtThread = new Thread(NFDTaskEngine.getIns());
            mgmtThread.start();
            NclwLog.getIns().log("-----NFDTaskEngine for Receiving DATA START----");




            NclwLog.getIns().log("------Started@" + Inet4Address.getLocalHost().getHostAddress());
            final ScheduledExecutorService pool;
            final ForwardingPipeline pipeline;
            final FaceManager faceManager;



//テスト用にデータを準備して，テーブルへ入れておく処理
            Setup setup = new Setup();
            setup.prepare();

            Forwarder forwarder = new Forwarder();
            Thread t = new Thread(forwarder);
            t.start();


            NclwNFDMgr.getIns().initializeTables(hostFile);

        } catch (Exception e) {
            e.printStackTrace();
            //System.out.println("Please input correct arguments [node ID] [property file name] ");
            System.exit(-1);
        }


    }
}
