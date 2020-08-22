package org.ncl.workflow.ccn.sfc.process;

import org.ncl.workflow.ccn.autoicnsfc.AutoICNSFCMgr;
import org.ncl.workflow.ccn.core.NclwNFDMgr;
import org.ncl.workflow.comm.DataSendInfo;
import org.ncl.workflow.comm.FileSendInfo;
import org.ncl.workflow.comm.SendThread;
import org.ncl.workflow.engine.Task;
import org.ncl.workflow.util.NCLWUtil;

import java.util.LinkedList;

/**
 * Created by Hidehiro Kanemitsu on 2019/11/28
 */
public class NFDTask extends Task {

    /**
     * Interestパケットが到着したカウンタ
     */
    protected int interestCounter;

    public NFDTask(long jobID, long taskID, String cmd, LinkedList<FileSendInfo> oFIL, DataSendInfo oDSI, LinkedList<Long> fromTaskList, LinkedList<Long> toTaskList, String targetID) {
        super(jobID, taskID, cmd, oFIL, oDSI, fromTaskList, toTaskList, targetID);
        this.interestCounter = 0;
        //AutoICNのときは，schedからsfcを取得する．

    }

    public void addInterestCounter(){
        this.interestCounter++;
    }

    @Override
    public SendThread processSend() {
        NclwNFDSendThread sender = new NclwNFDSendThread();
        Thread sendThread = new Thread(sender);
        //NCLWEngine.getIns().getExec().submit(sender);
        sendThread.start();

        return sender;
    }



    public int getInterestCounter() {
        return interestCounter;
    }

    public void setInterestCounter(int interestCounter) {
        this.interestCounter = interestCounter;
    }
}
