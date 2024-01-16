package net.gripps.ccn.icnsfc.logger;

import net.gripps.ccn.icnsfc.core.AutoInfo;
import net.gripps.ccn.icnsfc.process.AutoSFCMgr;
import net.gripps.cloud.nfv.sfc.SFC;

import java.util.Iterator;

/**
 * Created by Hidehiro Kanemitsu on 2020/11/13
 */
public class StatisticalMgr {
    boolean finishFlag = false;

    public void initialize() {
        Iterator<SFC> sIte = AutoSFCMgr.getIns().getSfcQueue().iterator();
        while(sIte.hasNext()){
            SFC sfc = sIte.next();
            String autoID = AutoSFCMgr.getIns().genAutoID(sfc);
            AutoInfo info = new AutoInfo(autoID);

            info.setCCR(AutoSFCMgr.getIns().calcCCR(sfc));
            info.setSfNum(sfc.getVnfMap().size());
            AutoSFCMgr.getIns().getInfoMap().put(autoID, info);


        }

    }
}
