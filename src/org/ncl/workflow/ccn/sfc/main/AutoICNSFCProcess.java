package org.ncl.workflow.ccn.sfc.main;

import net.gripps.cloud.nfv.NFVEnvironment;
import org.ncl.workflow.ccn.autoicnsfc.AutoICNSFCMgr;
import org.ncl.workflow.ccn.autoicnsfc.AutoICNSFCScheduling;
import org.ncl.workflow.ccn.sfc.process.NclwNFD;
import org.ncl.workflow.ccn.util.ResourceMgr;
import org.ncl.workflow.delegator.EnvJsonLoader;
import org.ncl.workflow.logger.NclwLog;

/**
 * Created by Hidehiro Kanemitsu on 2020/03/04
 */
public class AutoICNSFCProcess {
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
            String envJsonFile = args[3];
            NclwLog.getIns().log("---AutoICNSFCProcess START----");
            //とりあえず，設定ファイルから環境情報を読み込む．
            EnvJsonLoader envLoader = new EnvJsonLoader();
            NFVEnvironment env = envLoader.loadEnv(envJsonFile);
            NclwLog.getIns().log(envJsonFile + " is loaded successfully.");

            //環境情報をセットする．
            AutoICNSFCScheduling autoICN = new AutoICNSFCScheduling(env);
            ResourceMgr.getIns().setEnv(env);
            //sfc = autoICN.constructAutoSFC();
            AutoICNSFCMgr.getIns().setSched(autoICN);

            NclwNFD nfd = new NclwNFD();
            nfd.process(nodeID, propName, hostFile);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
