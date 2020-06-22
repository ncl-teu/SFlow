package org.ncl.workflow.comm;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by Hidehiro Kanemitsu on 2020/06/13.
 */
public class InterestHopInfo implements Serializable {

    private String prefix;

    private LinkedList<String> ipList;

    public InterestHopInfo(String prefix, LinkedList<String> ipList) {
        this.prefix = prefix;
        this.ipList = ipList;
    }

    public InterestHopInfo() {
        this.ipList = new LinkedList<String>();
    }

    public boolean isExists(String ip){
        boolean ret = false;
        Iterator<String> ipIte = this.ipList.iterator();
        while(ipIte.hasNext()){
            String ipAddr = ipIte.next();
            if(ipAddr == ip){
                ret = true;
                break;
            }
        }
        return ret;
    }

    public void addIp(String ip){
        this.ipList.add(ip);
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public LinkedList<String> getIpList() {
        return ipList;
    }

    public void setIpList(LinkedList<String> ipList) {
        this.ipList = ipList;
    }
}
