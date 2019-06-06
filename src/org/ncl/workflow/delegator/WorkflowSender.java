package org.ncl.workflow.delegator;

import net.gripps.cloud.nfv.sfc.SFC;
import net.gripps.clustering.common.aplmodel.CustomIDSet;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Hidehiro Kanemitsu on 2019/05/24.
 */
public class WorkflowSender implements Runnable {

    protected LinkedBlockingQueue<SFC> sfcQueue;

    public WorkflowSender() {
        this.sfcQueue = new LinkedBlockingQueue<SFC>();
    }

    @Override
    public void run() {
        while(true){
            try{
                Thread.sleep(1000);
                if(!this.sfcQueue.isEmpty()){
                    SFC sfc = this.sfcQueue.poll();
                    CustomIDSet startSet = sfc.getStartVNFSet();
                    //send sfc to the starting node

                }

            }catch(Exception e){
                e.printStackTrace();
            }
        }

    }

    public LinkedBlockingQueue<SFC> getSfcQueue() {
        return sfcQueue;
    }

    public void setSfcQueue(LinkedBlockingQueue<SFC> sfcQueue) {
        this.sfcQueue = sfcQueue;
    }
}
