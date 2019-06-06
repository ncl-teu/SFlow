package org.ncl.workflow.comm;

import java.io.Serializable;
import java.util.LinkedList;

/**
 * Created by Hidehiro Kanemitsu on 2019/05/13
 */
public class FileSendInfo implements Serializable {

    /**
     * output file path
     */
    private String path;

    /**
     * target Task ID
     */
    private LinkedList<Long> targetTaskIDList;

    //the write path at the target node after receiving it.
    private String writePath;


    public FileSendInfo(String path,  LinkedList<Long> idList, String wpath) {
        this.path = path;
        this.targetTaskIDList = idList;
        this.writePath = wpath;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }


    public LinkedList<Long> getTargetTaskIDList() {
        return targetTaskIDList;
    }

    public void setTargetTaskIDList(LinkedList<Long> targetTaskIDList) {
        this.targetTaskIDList = targetTaskIDList;
    }

    public String getWritePath() {
        return writePath;
    }

    public void setWritePath(String writePath) {
        this.writePath = writePath;
    }
}
