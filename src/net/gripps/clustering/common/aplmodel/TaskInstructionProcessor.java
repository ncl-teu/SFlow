package net.gripps.clustering.common.aplmodel;

import java.util.*;

/**
 * Author: Hidehiro Kanemitsu<br>
 * Date: 2007/07/30
 */
public class TaskInstructionProcessor {

    /**
     *
     */
    private static TaskInstructionProcessor singleton;

    /**
     * @return
     */
    public static TaskInstructionProcessor getInstance() {
        if (TaskInstructionProcessor.singleton == null) {
            TaskInstructionProcessor.singleton = new TaskInstructionProcessor();
        }
        return TaskInstructionProcessor.singleton;
    }

    /**
     *
     */
    private TaskInstructionProcessor() {

    }

    /**
     * @param pTask 親タスク
     * @return
     */
    public AbstractTask calculate(AbstractTask pTask/*,int mode*/) {


        Hashtable<Long, AbstractTask> childTable = pTask.getTaskList();
        Collection<AbstractTask> childCollection = childTable.values();

        Iterator<AbstractTask> ite = childCollection.iterator();
        long max = 0;
        long min = 0;
        long ave = 0;

        CDMapList cdMapList = new CDMapList();

        while (ite.hasNext()) {
            AbstractTask task = ite.next();
            //もしデータ依存ノード(必ず実行されるノード)であれば，
            //そのまま加算する
            if (task.getCpredList().isEmpty()) {
                max += task.getMaxWeight();
                min += task.getMinWeight();
                ave += task.getAveWeight();
            } else {
                //制御依存により実行されるタスクに対しては，特別にグループを作る．
                LinkedList<ControlDependence> cdList = task.getCpredList();
                int size = cdList.size();
                for (int i = 0; i < size; i++) {
                    ControlDependence cd = cdList.get(i);
                    cdMapList.addCDMap(cd, task.getMaxWeight(), task.getAveWeight(), task.getMinWeight());
                }
            }


        }

        int mapSize = cdMapList.getMapList().size();

        for (int i = 0; i < mapSize; i++) {
            max += cdMapList.generateInstrucationSum(i, 1);

        }
        pTask.setMaxWeight(max);

        //Averate
        //条件分岐の中での平均値
        for (int i = 0; i < mapSize; i++) {
            ave += cdMapList.generateInstrucationSum(i, 2);

        }
        pTask.setAveWeight(ave);

        //Min
        //条件分岐のうちで，MInのもの
        for (int i = 0; i < mapSize; i++) {
            min += cdMapList.generateInstrucationSum(i, 3);

        }
        pTask.setMinWeight(min);


        //子タスクのリストから，分岐ごとにグループ化する．

        //計算モードによって，集計方法を決める．
        /*switch(mode){
            //Max
            // 条件分岐のうち，Maxのものを選ぶ
            case 1:
                for(int i=0;i<mapSize;i++){
                    max += cdMapList.generateInstrucationSum(i,1);

                }
                pTask.setMaxWeight(max);

                break;
            //Averate
            //条件分岐の中での平均値
            case 2:
                for(int i=0;i<mapSize;i++){
                    ave += cdMapList.generateInstrucationSum(i,2);

                }
                pTask.setAveWeight(ave);

                break;
            //Min
            //条件分岐のうちで，MInのもの
            case 3:
                for(int i=0;i<mapSize;i++){
                    min += cdMapList.generateInstrucationSum(i,3);

                }
                pTask.setMinWeight(min);

                break;
            default:
                break;
        } */
        return pTask;

    }

}
