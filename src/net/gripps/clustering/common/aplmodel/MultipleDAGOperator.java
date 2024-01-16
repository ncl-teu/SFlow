package net.gripps.clustering.common.aplmodel;

import net.gripps.clustering.common.Constants;

import java.util.*;

/**
 * Author: H. Kanemitsu
 * Date: 16/04/28
 */
public class MultipleDAGOperator {

    /**
     * 複数ワークフローの集合
     */
    private LinkedList<BBTask> aplList;

    /**
     * 生成された合成DAG
     */
    private BBTask compositeDAG;


    /**
     * STARTタスク
     */
    private BBTask startTask;

    /**
     * ENDタスク
     */
    private BBTask endTask;

    /**
     * タスク総数
     */
    private long taskNum;

    /**
     * 新規追加すべきジョブの開始番号
     * 実際には+1した値から割り当てる
     */
    private long currentNumber;


    public MultipleDAGOperator() {
        this.aplList = new LinkedList<BBTask>();
        this.compositeDAG = new BBTask(Constants.TYPE_BASIC_BLOCK, 0, 0, 0);
        this.startTask = new BBTask(Constants.TYPE_BASIC_BLOCK, 0, 0, 0);
        this.endTask = new BBTask(Constants.TYPE_BASIC_BLOCK, 0, 0, 0);

        //START + ENDで2つをセットしておく．
        this.taskNum = 2;

        //STARTIDをセットする．
        Vector<Long> startID = new Vector<Long>();
        startID.add(new Long(1));
        startID.add(new Long(1));
        this.startTask.setIDVector(startID);

        //ENDタスクのIDを設定する．
        Vector<Long> endVec = new Vector<Long>();
        endVec.add(new Long(1));
        endVec.add(new Long(this.taskNum));
        this.endTask.setIDVector(endVec);
        this.currentNumber = 1;


        Vector<Long> id = new Vector<Long>();
        id.add(new Long(1));
        this.compositeDAG.setIDVector(id);
        this.compositeDAG.addTaskSimply(this.startTask);
        this.compositeDAG.addTaskSimply(this.endTask);
        this.compositeDAG.getStartTaskSet().add(this.startTask.getIDVector().get(1));
        this.compositeDAG.setStartTask(this.startTask.getIDVector());
        this.compositeDAG.setEndTask(this.endTask.getIDVector());


    }
/*
    public BBTask generateRandomMultipleDAGs(String filename){
        AplOperator.getInstance().constructTask(filename);

        //AplOperator.getInstance().constructTask(filename);
        //依存関係と，命令数を割り当てる．最下位にあるタスク命令数は決まっているので，読み込むのみ
        BBTask APL = AplOperator.getInstance().assignDependencyProcess();
        apl = AplOperator.getInstance().getApl();
    }
*/

    public BBTask calcMaxMin(BBTask apl) {
        Iterator<AbstractTask> taskIte = apl.getTaskList().values().iterator();
        long totalWorkload = 0;
        long totalDataSize = 0;
        long maxWorkload = 0;
        long minWorkload = Constants.MAXValue;

        long maxDataSize = 0;
        long minDataSize = Constants.MAXValue;

        while (taskIte.hasNext()) {
            AbstractTask t = taskIte.next();
            totalWorkload += t.getMaxWeight();

            if (t.getMaxWeight() >= maxWorkload) {
                maxWorkload = t.getMaxWeight();

            }

            if (t.getMaxWeight() <= minWorkload && t.getMaxWeight()>0) {
                minWorkload = t.getMaxWeight();
            }

            Iterator<DataDependence> dsucIte = t.getDsucList().iterator();
            while (dsucIte.hasNext()) {
                DataDependence dsuc = dsucIte.next();
                if (dsuc.getMaxDataSize() >= maxDataSize) {
                    maxDataSize = dsuc.getMaxDataSize();
                }

                if (dsuc.getMaxDataSize() <= minDataSize && dsuc.getMaxDataSize()>0) {
                    minDataSize = dsuc.getMaxDataSize();
                }

            }
            Iterator<DataDependence> dpredIte = t.getDpredList().iterator();
            while (dsucIte.hasNext()) {
                DataDependence dpred = dpredIte.next();
                if (dpred.getMaxDataSize() >= maxDataSize) {
                    maxDataSize = dpred.getMaxDataSize();
                }

                if (dpred.getMaxDataSize() <= minDataSize && dpred.getMaxDataSize()>0) {
                    minDataSize = dpred.getMaxDataSize();
                }

            }

            //最大、最小値
            apl.setMaxData(maxDataSize);
            apl.setMinData(minDataSize);
            apl.setMaxWorkload(maxWorkload);
            apl.setMinWorkload(minWorkload);
            //合計値
            apl.setMaxWeight(totalWorkload);

        }

        return apl;

    }

    /**
     * 新規DAGをリストに追加する．入力時点で，すでにIDは割り当てられる．
     *
     * @param apl
     */
    public BBTask addDAG(BBTask apl) {
        this.aplList.add(apl);

        long totalWorkload = 0;
        long totalDataSize = 0;
        long maxWorkload = 0;
        long minWorkload = Constants.MAXValue;

        long maxDataSize = 0;
        long minDataSize = Constants.MAXValue;


        apl = this.calcMaxMin(apl);
        if(apl.getMaxWorkload() >= maxWorkload){
            maxWorkload = apl.getMaxWorkload();
            this.compositeDAG.setMaxWorkload(maxWorkload);
        }

        if(apl.getMinWorkload() <= minWorkload){
            minWorkload = apl.getMinWorkload();
            this.compositeDAG.setMinWorkload(minWorkload);
        }

        if(apl.getMaxData() >= maxDataSize){
            maxDataSize = apl.getMaxData();
            this.compositeDAG.setMaxData(maxDataSize);
        }

        if(apl.getMinData() <= minDataSize){
            minDataSize = apl.getMinData();
            this.compositeDAG.setMinData(minDataSize);
        }
        totalWorkload += apl.getMaxWeight();


        this.taskNum += apl.getTaskList().size();
        //ENDタスクのID更新
        this.endTask.getIDVector().set(1, new Long(taskNum));
        this.compositeDAG.setEndTask(this.endTask.getIDVector());
        Iterator<AbstractTask> taskIte = apl.getTaskList().values().iterator();
        while (taskIte.hasNext()) {
            AbstractTask task = taskIte.next();

            this.compositeDAG.addTaskSimply(task);
        }

        Iterator<Long> startIte = apl.getStartTaskSet().iterator();

        while (startIte.hasNext()) {
            Long startID = startIte.next();
            AbstractTask start = apl.findTaskByLastID(startID);
            //START->aplのstartたちへのデータ依存辺を生成する．
            if (start == null) {
                System.out.println("NULL");
            }
            DataDependence dd = new DataDependence(this.startTask.getIDVector(), start.getIDVector(), 0, 0, 0);
            this.startTask.addDsucForce(dd);
            start.addDpredForce(dd);
        }

        //aplのENDタスクの依存辺追加処理
        AbstractTask end = apl.findTaskByLastID(apl.getEndTask().get(1));

        //ENDタスクのID更新
        Vector<Long> oldEndID = this.endTask.getIDVector();
        this.compositeDAG.getTaskList().remove(oldEndID);

        // Long newEndID = new Long(this.endTask.getIDVector().get(1)+apl.getTaskList().size());
        Long newEndID = new Long(apl.getEndTask().get(1).longValue() + 1);
        this.endTask.getIDVector().set(1, newEndID);
        //ENDタスクの既存のDD更新
        Iterator<DataDependence> dpredIte = this.endTask.getDpredList().iterator();
        while (dpredIte.hasNext()) {
            DataDependence dpred = dpredIte.next();
            dpred.setToID(this.endTask.getIDVector());
            AbstractTask predTask = this.compositeDAG.findTaskByLastID(dpred.getFromID().get(1));
            //Endタスクの先行タスクのdsuc更新
            DataDependence dsuc = predTask.findDDFromDsucList(predTask.getIDVector(), oldEndID);
            dsuc.setToID(this.endTask.getIDVector());

        }


        DataDependence dd = new DataDependence(apl.getEndTask(), this.endTask.getIDVector(), 0, 0, 0);
        end.addDsucForce(dd);
        this.endTask.addDpredForce(dd);
        this.compositeDAG.setEndTask(this.endTask.getIDVector());
        long oldTotalWD = this.compositeDAG.getMaxWeight();
        long newTotalWD = oldTotalWD + totalWorkload;
        this.compositeDAG.setMaxWeight(newTotalWD);


        this.compositeDAG.getTaskList().put(newEndID, this.endTask);

        return this.compositeDAG;


    }

    public LinkedList<BBTask> getAplList() {
        return aplList;
    }

    public void setAplList(LinkedList<BBTask> aplList) {
        this.aplList = aplList;
    }

    public BBTask getCompositeDAG() {
        return compositeDAG;
    }

    public void setCompositeDAG(BBTask compositeDAG) {
        this.compositeDAG = compositeDAG;
    }

    public BBTask getStartTask() {
        return startTask;
    }

    public void setStartTask(BBTask startTask) {
        this.startTask = startTask;
    }

    public BBTask getEndTask() {
        return endTask;
    }

    public void setEndTask(BBTask endTask) {
        this.endTask = endTask;
    }


}
