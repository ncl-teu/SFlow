package org.ncl.workflow.ccn.sfc.main;

import com.intel.jndn.forwarder.Forwarder;
import com.intel.jndn.forwarder.api.FaceManager;
import com.intel.jnfd.deamon.fw.ForwardingPipeline;
import org.ncl.workflow.ccn.core.NclwNFDMgr;
import org.ncl.workflow.ccn.sfc.process.NFDTaskEngine;
import org.ncl.workflow.ccn.util.Setup;
import org.ncl.workflow.engine.NCLWEngine;
import org.ncl.workflow.util.NCLWUtil;

import java.net.Inet4Address;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by Hidehiro Kanemitsu on 2019/11/15
 */
public class NclwNFDProcess {
    public static void main(String[] args) {
        try {

            if (args.length <= 1) {
                System.out.println("****Error: Please input 2 arguments [node ID] [property file name][host listfile].**** ");
                System.exit(-1);
            }
            long nodeID = Long.valueOf(args[0]);
            //property file name
            String propName = args[1];
            String hostFile = args[2];

            //Set parameters
            NCLWUtil.getIns().initialize(propName);
            //Start NFD Engine.
            NFDTaskEngine.getIns().setNodeID(nodeID);
            Thread mgmtThread = new Thread(NFDTaskEngine.getIns());
            mgmtThread.start();


            //System.out.println("***Started at " + Inet4Address.getLocalHost().getHostAddress());
            final ScheduledExecutorService pool;
            final ForwardingPipeline pipeline;
            final FaceManager faceManager;

            Forwarder forwarder = new Forwarder();
            NclwNFDMgr.getIns().initializeTables(hostFile);

//テスト用にデータを準備して，テーブルへ入れておく処理
            Setup setup = new Setup();
            setup.prepare();
            forwarder.run();
        } catch (Exception e) {
            System.out.println("Please input correct arguments [node ID] [property file name] ");
            System.exit(-1);
        }


    }

}
