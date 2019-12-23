package org.ncl.workflow.ccn.sfc.main;

import com.intel.jndn.forwarder.Forwarder;
import com.intel.jndn.forwarder.api.FaceManager;
import com.intel.jnfd.deamon.fw.ForwardingPipeline;
import org.ncl.workflow.ccn.core.NclwNFDMgr;
import org.ncl.workflow.ccn.sfc.process.NFDTaskEngine;
import org.ncl.workflow.ccn.sfc.process.NclwNFD;
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
            NclwNFD nfd = new NclwNFD();
            nfd.process(nodeID, propName, hostFile);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
