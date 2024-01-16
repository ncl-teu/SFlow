package net.gripps.ccn.main;

import net.gripps.ccn.CCNUtil;
import net.gripps.ccn.Logger.CCNLog;
import net.gripps.ccn.process.CCNMgr;

/**
 * Created by Hidehiro Kanemitsu on 2018/11/24.
 */
public class CCNTest {

    public static void main(String[] args){
/*
        CCNLog s = new CCNLog();
        s.runSample();
*/
        CCNLog.setIsSFCMode(false);
        CCNLog.getIns().log(",type(1:InterestARRIVED->/2:Org_DataGET<-/13:CacheARRIVED->/3:CacheGET<-/4:CacheARRIVEDByBC->/5:RouterJOIN/6:RouterLEAVE), " +
                "prefix,DataSize(MB), StartTime,FinishTime,duration(ms),Interest_senderID,Data(Cache)holdingNodeID, Hop#,# of SharedConnections," +
                "ContentsFound/Not,ByBC?,Memo");
        String fileName = args[0];
        CCNUtil.getIns().initialize(fileName);
        CCNMgr.getIns().process();
        //CCNMgr自体のスレッドを起動
        Thread mgrThread = new Thread(CCNMgr.getIns());
        mgrThread.start();




    }
}
