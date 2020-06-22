package org.ncl.workflow.ccn.autoicnsfc;

import net.gripps.cloud.nfv.NFVEnvironment;
import net.gripps.cloud.nfv.sfc.SFC;
import org.ncl.workflow.ccn.util.ResourceMgr;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by Hidehiro Kanemitsu on 2020/03/04
 */
public class AutoICNSFCMgr implements Serializable {

    private static AutoICNSFCMgr own;

    AutoICNSFCScheduling sched;

    NFVEnvironment env;

    private HashMap<String, Integer> interestSentMap;

    public static AutoICNSFCMgr getIns(){
        if(AutoICNSFCMgr.own == null){
            AutoICNSFCMgr.own = new AutoICNSFCMgr();
        }
        return AutoICNSFCMgr.own;
    }

    public AutoICNSFCMgr() {
        this.buildEnv();
        this.interestSentMap = new HashMap<String, Integer>();
    }

    /**
     * 自身の環境を構築します．ResourceMgr
     */
    public void buildEnv(){
        //ResourceMgr.getIns().initResource();

    }

    public boolean isKeyExist(String key){
        boolean ret = false;
        Iterator<String> kIte = this.interestSentMap.keySet().iterator();
        while(kIte.hasNext()){
            String val = kIte.next();
            if(val.equals(key)){

                ret = true;
                break;
            }
        }
        return ret;
    }


    public AutoICNSFCScheduling getSched() {
        return sched;
    }

    public void setSched(AutoICNSFCScheduling sched) {
        this.sched = sched;
        this.env = (NFVEnvironment)this.sched.getEnv();
    }

    public NFVEnvironment getEnv() {
        return env;
    }

    public void setEnv(NFVEnvironment env) {
        this.env = env;
    }


    public HashMap<String, Integer> getInterestSentMap() {
        return interestSentMap;
    }

    public void setInterestSentMap(HashMap<String, Integer> interestSentMap) {
        this.interestSentMap = interestSentMap;
    }
}
