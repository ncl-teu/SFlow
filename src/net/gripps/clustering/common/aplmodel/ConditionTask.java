package net.gripps.clustering.common.aplmodel;

/**
 * Author: Hidehiro Kanemitsu<br>
 * Date: 2007/07/23
 */
public class ConditionTask extends AbstractTask{
    public ConditionTask(/*Vector<Long> in_id,*/
                        int in_type,
                        long in_maxweight,
                        long in_aveweight,
                        long in_minweight){
        super(in_type,in_maxweight,in_aveweight,in_minweight);
    }


}
