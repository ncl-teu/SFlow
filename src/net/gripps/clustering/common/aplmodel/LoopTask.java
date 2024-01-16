package net.gripps.clustering.common.aplmodel;

/**
 * Author: Hidehiro Kanemitsu<br>
 * Date: 2007/07/23
 */
public class LoopTask extends AbstractTask{
    private long loopNum;

    public LoopTask(/*Vector<Long> in_id,*/
                        int in_type,
                        long in_maxweight,
                        long in_aveweight,
                        long in_minweight,
                        long in_loopNum){
        super(in_type,in_maxweight,in_aveweight,in_minweight);
        this.setLoopNum(in_loopNum);

    }


    public long getLoopNum() {
        return loopNum;
    }

    public void setLoopNum(long loopNum) {
        this.loopNum = loopNum;
    }
}
