package net.gripps.clustering.common.aplmodel;

/**
 * Author: Hidehiro Kanemitsu<br>
 * Date: 2007/07/23
 * 関数CALLを表現したタスクです．このタスクでは，START/END以外の子タスクと外部タスクとの
 * 依存関係はありません．つまり，内部の依存関係のみです．
 * STARTは入力，ENDは出力です．
 */
public class FunctionTask extends AbstractTask{


    /**
     *
     * @param in_type
     * @param in_maxweight
     * @param in_aveweight
     * @param in_minweight
     */
    public FunctionTask(/*Vector<Long> in_id,*/
                        int in_type,
                        long in_maxweight,
                        long in_aveweight,
                        long in_minweight){
        super(in_type,in_maxweight,in_aveweight,in_minweight);
    }

}
