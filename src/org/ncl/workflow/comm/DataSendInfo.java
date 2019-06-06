package org.ncl.workflow.comm;

import java.io.Serializable;
import java.util.LinkedList;

/**
 * Created by Hidehiro Kanemitsu on 2019/05/13
 */
public class DataSendInfo implements Serializable {

    /**
     * output data message
     */
    private String msg;

    private LinkedList<Long> targetIDList;


    public DataSendInfo(String msg, LinkedList<Long> targetIDList) {
        this.msg = msg;
        this.targetIDList = targetIDList;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public LinkedList<Long> getTargetIDList() {
        return targetIDList;
    }

    public void setTargetIDList(LinkedList<Long> targetIDList) {
        this.targetIDList = targetIDList;
    }
}
