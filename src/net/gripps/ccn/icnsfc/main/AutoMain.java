package net.gripps.ccn.icnsfc.main;

import net.gripps.ccn.Logger.CCNLog;
import net.gripps.ccn.core.CCNRouter;
import net.gripps.ccn.icnsfc.AutoUtil;
import net.gripps.ccn.icnsfc.core.AutoEnvironment;
import net.gripps.ccn.icnsfc.core.AutoInfo;
import net.gripps.ccn.icnsfc.logger.ISLog;
import net.gripps.ccn.CCNUtil;
import net.gripps.ccn.icnsfc.logger.StatisticalMgr;
import net.gripps.ccn.icnsfc.process.AutoSFCMgr;
import net.gripps.ccn.process.CCNMgr;
import net.gripps.cloud.CloudUtil;
import net.gripps.cloud.nfv.NFVUtil;
import net.gripps.cloud.nfv.sfc.SFC;
import net.gripps.cloud.nfv.sfc.SFCGenerator;

import java.util.HashMap;

/**
 * Created by Hidehiro Kanemitsu on 2020/08/27
 */
public class AutoMain {
    public static void main(String[] args){
/*
        CCNLog s = new CCNLog();
        s.runSample();
*/
            //欲しい情報のカテゴリ：startVNF開始,

            ISLog.getIns().log(",Int., 0:Interest/1:CacheHit, AplID, SFCID, prefix, fromSFID@R_ID, toSFID, <-R/N+ID, Hop, Duration, @Host, @VM, @vCPU, TimeStamp");
            ISLog.getIns().log(",Data., 0: ProcReturn/1: CacheReturn, AplID, SFCID, prefix, Proc.Time, fromSFID, R_ID->, toSFID@R/N+ID, Hop, " +
                    "ComTime, DataSize, @Host, @VM, @vCPU, BW, #ofSharedConnection, TimeStamp");
            CCNLog.setIsSFCMode(true);
            //設定ファイルを取得
            String fileName = args[0];
            //Utilの初期化（設定ファイルの値の読み込み）
            NFVUtil.getIns().initialize(fileName);
            CloudUtil.getInstance().initialize(fileName);
            CCNUtil.getIns().initialize(fileName);
            AutoUtil.getIns().initialize(fileName);
            AutoSFCMgr.getIns().setSFC(true);

            //CCNMgr: ルータ, ノードのDB
            CCNMgr.getIns().setSFCMode(true);
            //まずはクラウド側の初期設定
            AutoEnvironment env = new AutoEnvironment();

            AutoSFCMgr.getIns().setEnv(env);

            StatisticalMgr sMgr = new StatisticalMgr();
            sMgr.initialize();
            //env.writeFile("envOut");

            CCNMgr.getIns().process();


            /*
            CCNMgr.getIns().process();
            //CCNMgr自体のスレッドを起動
            Thread mgrThread = new Thread(CCNMgr.getIns());
            mgrThread.start();

             */





    }
}
