package net.gripps.clustering.algorithms.mwsl_delta;

import net.gripps.clustering.algorithms.AbstractClusteringAlgorithm;
import net.gripps.clustering.common.aplmodel.*;
import net.gripps.clustering.tool.Calc;

import java.util.*;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 提案するクラスタリングアルゴリズムです．<br>
 * MWSL-δ（Minimizing WSL under δ Constriant)アルゴリズムのヘテロ版です<br>
 * <p/>
 * Author: H. Kanemitsu
 */
public class MWSL_delta extends AbstractClusteringAlgorithm {


    protected  BBTask lcTask = null;

    protected  String fileName = null;

    protected boolean isThresholdSet = false;

    /**
     * @param task
     * @param
     */
    public MWSL_delta(BBTask task, String file) {
        super(task, file, 1);

        this.fileName = file;

        //this.lc = new LBC_LinearClustering(task, file);
    }

    public MWSL_delta(BBTask task, String file, long opt){
        this(task, file);
        this.threshold = opt;
        this.isThresholdSet = true;
        this.retApl.setOptDelta(opt);
    }
                        
    /**
     * @return
     */
    long getMaxClusterNumInPath() {
        Iterator<Long> startIDIte = this.lcTask.getStartTaskSet().iterator();
        CustomIDSet set = new CustomIDSet();

        int retNum = 0;
        //startタスクたちに対するループ
        while (startIDIte.hasNext()) {
            Long id = startIDIte.next();
            //STARTタスクを取得する．
            AbstractTask startTask = this.lcTask.findTaskByLastID(id);

            TaskCluster cluster = this.lcTask.findTaskCluster(startTask.getClusterID());
            AbstractTask bottomTask = this.lcTask.findTaskByLastID(cluster.getBsucTaskID());

            Long cID = startTask.getClusterID();

            Iterator<DataDependence> dsucIte = bottomTask.getDsucList().iterator();
            int cNum = 0;
            //タスクの後続タスクたちに対するループ
            while (dsucIte.hasNext()) {
                DataDependence dsuc = dsucIte.next();
                Long dsucID = dsuc.getToID().get(1);
                AbstractTask dsucTask = this.lcTask.findTaskByLastID(dsucID);
                int tmpNum = 0;
                //後続タスクがチェック済み
                if (set.contains(dsucTask.getIDVector().get(1))) {

                } else {
                    CustomIDSet retSet = this.traceClusterNum(dsucTask, set);
                    set.addAll(retSet);

                }

                //後続タスクのクラスタID集合に，startタスクが属するクラスタがあるかどうか
                if (dsucTask.getClusterSet().contains(startTask.getClusterID())) {
                    tmpNum = dsucTask.getClusterSet().getList().size();

                } else {
                    tmpNum = dsucTask.getClusterSet().getList().size() + 1;
                }
                if (cNum <= tmpNum) {
                    startTask.getClusterSet().getObjSet().clear();
                    startTask.getClusterSet().addAll(dsucTask.getClusterSet());
                    startTask.getClusterSet().add(startTask.getClusterID());
                }
            }
            int clusterNum = startTask.getClusterSet().getList().size();

            if (retNum <= clusterNum) {
                retNum = clusterNum;
            }
        }
        return retNum;

    }


    CustomIDSet traceClusterNum(AbstractTask task, CustomIDSet set) {

        //ENDタスク対策
        if (task.getDsucList().isEmpty()) {
            set.add(task.getIDVector().get(1));
            task.getClusterSet().add(task.getClusterID());
            return set;
        }

        TaskCluster cluster = this.lcTask.findTaskCluster(task.getClusterID());
        AbstractTask bottomTask = this.lcTask.findTaskByLastID(cluster.getBsucTaskID());
        if (bottomTask.getDsucList().isEmpty()) {
            set.add(task.getIDVector().get(1));
            task.getClusterSet().add(task.getClusterID());
            bottomTask.getClusterSet().add(task.getClusterID());
            return set;

        }

        Iterator<DataDependence> dsucIte = bottomTask.getDsucList().iterator();
        int cNum = 0;
        //タスクの後続タスクたちに対するループ
        while (dsucIte.hasNext()) {
            DataDependence dsuc = dsucIte.next();
            Long dsucID = dsuc.getToID().get(1);
            AbstractTask dsucTask = this.lcTask.findTaskByLastID(dsucID);

            int tmpNum = 0;
            //後続タスクがチェック済み
            if (set.contains(dsucTask.getIDVector().get(1))) {

            } else {
                CustomIDSet retSet = this.traceClusterNum(dsucTask, set);
                set.addAll(retSet);

            }

            //後続タスクのクラスタID集合に，タスクが属するクラスタがあるかどうか
            if (dsucTask.getClusterSet().contains(task.getClusterID())) {
                tmpNum = dsucTask.getClusterSet().getList().size();

            } else {
                tmpNum = dsucTask.getClusterSet().getList().size() + 1;
            }
            if (cNum <= tmpNum) {
                task.getClusterSet().getObjSet().clear();
                task.getClusterSet().addAll(dsucTask.getClusterSet());
                task.getClusterSet().add(task.getClusterID());
            }

            set.add(task.getIDVector().get(1));
        }

        return set;

    }

    /**
     * @return
     */
    public long getMaxTaskNumInPath() {
        Iterator<Long> startIte = this.retApl.getStartTaskSet().iterator();
        CustomIDSet set = new CustomIDSet();

        long retNum = 0;
        //startタスクに対するループ
        while (startIte.hasNext()) {
            Long startID = startIte.next();
            AbstractTask startTask = this.retApl.findTaskByLastID(startID);
            //後続タスク
            Iterator<DataDependence> dsucIte = startTask.getDsucList().iterator();
            long sucNum = 0;
            //後続タスクに対するループ
            while (dsucIte.hasNext()) {
                DataDependence dsuc = dsucIte.next();
                long tmpNum = 0;
                AbstractTask sucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));
                if (set.contains(dsuc.getToID().get(1))) {

                } else {
                    CustomIDSet retSet = this.calcMaxTaskNumInPath(sucTask, set);
                    // set.add(sucTask.getIDVector().get(1));
                    set.addAll(retSet);

                }
                //もし後続タスクが既にチェック済みであれば，その値を取得する．
                tmpNum = sucTask.getSucTaskNum();
                if (tmpNum >= sucNum) {
                    sucNum = tmpNum;
                }
            }
            set.add(startTask.getIDVector().get(1));
            sucNum = sucNum + 1;
            //自分自身を加える
            startTask.setSucTaskNum(sucNum);
            if (retNum <= sucNum) {
                retNum = sucNum;
            }

        }

        return retNum;
       // return set;
    }

    /**
     * 
     * @param task
     * @param tmpSet
     * @return
     */
    public CustomIDSet getPredTaskSet(AbstractTask task, CustomIDSet tmpSet){
        Iterator<DataDependence> dpredIte = task.getDpredList().iterator();
        long retSize = 0;
        CustomIDSet retSet = new CustomIDSet();
        if(task.getDpredList().isEmpty()){
            task.getAnsPathSet().add(task.getIDVector().get(1));
            tmpSet.add(task.getIDVector().get(1));
            return task.getAnsPathSet();
        }

        //先行タスクに対するループ
        while(dpredIte.hasNext()){
            DataDependence dpred = dpredIte.next();
            //先行タスクを取得する．
            AbstractTask dpredTask = this.retApl.findTaskByLastID(dpred.getFromID().get(1));
            CustomIDSet pSet = new CustomIDSet();
            //既に処理済みであれば，その中の要素を取り出す．
            if(tmpSet.contains(dpredTask.getIDVector().get(1))){
                pSet = dpredTask.getAnsPathSet();
            }else{
                //未処理なら，再帰CALL
                pSet = this.getPredTaskSet(dpredTask, tmpSet);
            }
            if(retSize <= pSet.getList().size()){
                //retSet.getObjSet().clear();
                retSize = pSet.getList().size();
                retSet = pSet;
            }
        }
        retSet.add(task.getIDVector().get(1));
        task.setAnsPathSet(retSet);
        tmpSet.add(task.getIDVector().get(1));

        return retSet;
    }

    /**
     * 
     * @return
     */
    public CustomIDSet getMaxTaskSetInPath() {
        //ENDタスクを取得する．
        AbstractTask endTask = this.retApl.findTaskByLastID(this.retApl.getEndTask().get(1));
        CustomIDSet tmpSet = new CustomIDSet();

        //先行タスクに対するループ
        Iterator<DataDependence> dpredIte = endTask.getDpredList().iterator();
        long taskNum = 0;
        CustomIDSet retSet = new CustomIDSet();
        
        while(dpredIte.hasNext()){
            DataDependence dpred = dpredIte.next();
            AbstractTask dpredTask = this.retApl.findTaskByLastID(dpred.getFromID().get(1));
            CustomIDSet predSet = this.getPredTaskSet(dpredTask, tmpSet);
            long tmpNum = predSet.getList().size();
            if(taskNum <= tmpNum){
                //retSet.getObjSet().clear();
                retSet = predSet;
                taskNum = tmpNum;
            }
        }
        retSet.add(endTask.getIDVector().get(1));
        endTask.setAnsPathSet(retSet);
        return retSet;
    }


    /**
     * @param set
     * @return
     */
    public CustomIDSet calcMaxTaskNumInPath(AbstractTask task, CustomIDSet set) {
        Iterator<Long> sucIte = task.getStartTaskSet().iterator();
        if (task.getDsucList().isEmpty()) {
            //ENDタスクであれば，1を返す．
            task.setSucTaskNum(1);
            set.add(task.getIDVector().get(1));
            return set;
        }

        long retNum = 0;

        //後続タスク
        Iterator<DataDependence> dsucIte = task.getDsucList().iterator();
        long sucNum = 0;
        //後続タスクに対するループ
        while (dsucIte.hasNext()) {
            DataDependence dsuc = dsucIte.next();
            long tmpNum = 0;
            AbstractTask sucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));
            if (set.contains(dsuc.getToID().get(1))) {

            } else {
                CustomIDSet retSet = this.calcMaxTaskNumInPath(sucTask, set);
                set.addAll(retSet);

                //set.add(sucTask.getIDVector().get(1));
            }
            //もし後続タスクが既にチェック済みであれば，その値を取得する．
            tmpNum = sucTask.getSucTaskNum();

            if (tmpNum >= sucNum) {
                sucNum = tmpNum;
            }
        }
        set.add(task.getIDVector().get(1));
        sucNum = sucNum + 1;
        //自分自身を加える
        task.setSucTaskNum(sucNum);
        return set;


    }


    public long getMinWCP2() {
        Iterator<Long> startTaskIte = this.lcTask.getStartTaskSet().iterator();
        CustomIDSet set = new CustomIDSet();

        long retBlevel = 10000000;
        //startタスクに対するループ
        while (startTaskIte.hasNext()) {
            Long id = startTaskIte.next();
            AbstractTask startTask = this.lcTask.findTaskByLastID(id);
            //クラスタサイズを取得

            TaskCluster cluster = this.lcTask.findTaskCluster(startTask.getClusterID());
            long clusterSize = this.getClusterInstruction(cluster);

            //Dtaskを取得する．
            AbstractTask dtask = this.lcTask.findTaskByLastID(cluster.getBsucTaskID());

            //dtaskの後続タスクを取得
            Iterator<DataDependence> dsucIte = dtask.getDsucList().iterator();

            long wBlevel = 10000000;
            //後続タスクたちに対するループ
            while (dsucIte.hasNext()) {

                DataDependence dsuc = dsucIte.next();
                AbstractTask sucTask = this.lcTask.findTaskByLastID(dsuc.getToID().get(1));
                //sucTaskが後続クラスタのtopタスクであれば，OK
                if (sucTask.getIDVector().get(1).longValue() == sucTask.getClusterID().longValue()) {
                    //もし後続タスクがチェック済みであれば，すっ飛ばす．
                    if (set.contains(dsuc.getToID().get(1))) {

                    } else {
                        CustomIDSet retSet = this.calcMinWCP2(sucTask, set);
                        set.addAll(retSet);
                    }

                } else {
                    //そうでなければ，すっ飛ばす
                    //sucTask.setWblevel(10000000);
                    continue;


                }

                long tmpValue = sucTask.getWblevel() + clusterSize / this.minSpeed;
                if (tmpValue < wBlevel) {
                    wBlevel = tmpValue;
                }

            }
            set.add(startTask.getIDVector().get(1));
            startTask.setWblevel(wBlevel);
            if (retBlevel > wBlevel) {
                retBlevel = wBlevel;
            }

        }

        return retBlevel;

    }


    /**
     * @param task
     * @param set
     * @return
     */
    public CustomIDSet calcMinWCP2(AbstractTask task, CustomIDSet set) {
        TaskCluster cluster = this.lcTask.findTaskCluster(task.getClusterID());
        long clusterSize = this.getClusterInstruction(cluster);
        AbstractTask bottomTask = this.lcTask.findTaskByLastID(cluster.getBsucTaskID());
        if (bottomTask.getDsucList().isEmpty()) {
            //ENDタスクを含んでいれば，終わり．
            task.setWblevel(this.getClusterInstruction(cluster));
            set.add(task.getIDVector().get(1));
            return set;
        }

        //bottomタスクの後続タスクを取得
        Iterator<DataDependence> dsucIte = bottomTask.getDsucList().iterator();
        long wBlevel = 10000000;
        //後続タスクたちに対するループ
        while (dsucIte.hasNext()) {
            DataDependence dsuc = dsucIte.next();
            AbstractTask sucTask = this.lcTask.findTaskByLastID(dsuc.getToID().get(1));
            if (sucTask.getIDVector().get(1).longValue() == sucTask.getClusterID().longValue()) {
                //もし後続タスクがチェック済みであれば，すっ飛ばす．
                if (set.contains(dsuc.getToID().get(1))) {

                } else {
                    CustomIDSet retSet = this.calcMinWCP2(sucTask, set);
                    set.addAll(retSet);

                }

            } else {
                //sucTask.setWblevel(10000000);
                continue;
            }

            long tmpValue = sucTask.getWblevel() + clusterSize / this.minSpeed;
            if (tmpValue < wBlevel) {
                wBlevel = tmpValue;
            }

        }
        set.add(task.getIDVector().get(1));
        task.setWblevel(wBlevel);

        return set;

    }


    /**
     * @return
     */
    public long getMinWCP() {
        Iterator<Long> startTaskIte = this.lcTask.getStartTaskSet().iterator();
        CustomIDSet set = new CustomIDSet();

        long retBlevel = 10000000;
        //startタスクに対するループ
        while (startTaskIte.hasNext()) {
            Long id = startTaskIte.next();
            AbstractTask startTask = this.lcTask.findTaskByLastID(id);
            //後続タスクを取得
            Iterator<DataDependence> dsucIte = startTask.getDsucList().iterator();
            long wBlevel = 10000000;
            //後続タスクたちに対するループ
            while (dsucIte.hasNext()) {
                DataDependence dsuc = dsucIte.next();
                AbstractTask sucTask = this.lcTask.findTaskByLastID(dsuc.getToID().get(1));

                //もし後続タスクがチェック済みであれば，すっ飛ばす．
                if (set.contains(dsuc.getToID().get(1))) {

                } else {
                    CustomIDSet retSet = this.calcMinWCP(sucTask, set);
                    set.addAll(retSet);

                }
                long tmpValue = sucTask.getWblevel() + startTask.getMaxWeight() / this.minSpeed;
                if (tmpValue <= wBlevel) {
                    wBlevel = tmpValue;
                }

            }
            set.add(startTask.getIDVector().get(1));
            startTask.setWblevel(wBlevel);
            if (retBlevel >= wBlevel) {
                retBlevel = wBlevel;
            }

        }

        return retBlevel;

    }

    public long getMinWCPValue() {
        Iterator<Long> startTaskIte = this.retApl.getStartTaskSet().iterator();
        CustomIDSet set = new CustomIDSet();

        long retBlevel = 10000000;
        while (startTaskIte.hasNext()) {
            Long id = startTaskIte.next();
            AbstractTask startTask = this.retApl.findTaskByLastID(id);
            long value = this.calcWBlevel(startTask, false);
            if (value <= retBlevel) {
                retBlevel = value;
            }

        }

        return retBlevel;

    }


    /**
     * @param task
     * @param recalculate
     * @return
     */
    public long calcWBlevel(AbstractTask task, boolean recalculate) {
        LinkedList<DataDependence> DsucList = task.getDsucList();
        int size = DsucList.size();
        //もしすでにBlevelの値が入っていれば，そのまま返す．

        if (recalculate) {
        }
        if (task.getWblevel() != -1) {
            if (!recalculate) {
                return task.getWblevel();
            }
        } else {
            // System.out.println("-1です");
        }
        //もし後続タスクがない場合，blevel=自分の命令数となる．
        long instruction = this.getInstrunction(task);
        if (DsucList.size() == 0) {
            task.setWblevel(instruction / this.minSpeed);
            return instruction;
        }

        long minValue = 1000000;
        for (int i = 0; i < size; i++) {
            // DataDependence dd = ddite.next();
            DataDependence dd = DsucList.get(i);
            Vector<Long> toid = dd.getToID();
            //System.out.println("FromID:"+fromid.lastElement().longValue());
            //エラーOverFlow
            AbstractTask toTask = (AbstractTask) this.retApl.findTaskByLastID(toid.get(1));
            if (toTask == null) {
                continue;
            }

            long toBlevel = (instruction / this.minSpeed) + this.calcWBlevel(toTask, recalculate);

            if (minValue >= toBlevel) {
                //task.setBsuc(toTask.getIDVector());
                minValue = toBlevel;
            }

        }
        task.setWblevel(minValue);
        //task.setBlevel_in(maxValue);

        return minValue;
    }


    /**
     * @param task
     * @param set
     * @return
     */
    public CustomIDSet calcMinWCP(AbstractTask task, CustomIDSet set) {

        //もしENDタスクであれば，終了．
        if (task.getDsucList().isEmpty()) {
            task.setWblevel(task.getMaxWeight() / this.minSpeed);
            set.add(task.getIDVector().get(1));
            return set;
        }

        //後続タスクを取得
        Iterator<DataDependence> dsucIte = task.getDsucList().iterator();
        long wBlevel = 10000000;
        //後続タスクたちに対するループ
        while (dsucIte.hasNext()) {
            DataDependence dsuc = dsucIte.next();
            AbstractTask sucTask = this.lcTask.findTaskByLastID(dsuc.getToID().get(1));

            //もし後続タスクがチェック済みであれば，すっ飛ばす．
            if (set.contains(dsuc.getToID().get(1))) {

            } else {
                CustomIDSet retSet = this.calcMinWCP(sucTask, set);
                set.addAll(retSet);

            }
            long tmpValue = sucTask.getWblevel() + task.getMaxWeight() / this.minSpeed;
            if (tmpValue <= wBlevel) {
                wBlevel = tmpValue;
            }

        }
        set.add(task.getIDVector().get(1));
        task.setWblevel(wBlevel);

        return set;

    }




    /**
     * @return
     */
    public long getMaxWCP() {
        Iterator<Long> startTaskIte = this.retApl.getStartTaskSet().iterator();
        CustomIDSet set = new CustomIDSet();

        long retBlevel = 0;
        //startタスクに対するループ
        while (startTaskIte.hasNext()) {
            Long id = startTaskIte.next();
            AbstractTask startTask = this.retApl.findTaskByLastID(id);
            //後続タスクを取得
            Iterator<DataDependence> dsucIte = startTask.getDsucList().iterator();
            long wBlevel = 0;
            //後続タスクたちに対するループ
            while (dsucIte.hasNext()) {
                DataDependence dsuc = dsucIte.next();
                AbstractTask sucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));

                //もし後続タスクがチェック済みであれば，すっ飛ばす．
                if (set.contains(dsuc.getToID().get(1))) {

                } else {
                    CustomIDSet retSet = this.calcMaxWCP(sucTask, set);
                    set.addAll(retSet);

                }
                long tmpValue = sucTask.getWblevel() + startTask.getMaxWeight() / this.minSpeed;
                if (tmpValue >= wBlevel) {
                    wBlevel = tmpValue;
                }

            }
            set.add(startTask.getIDVector().get(1));
            startTask.setWblevel(wBlevel);
            if (retBlevel <= wBlevel) {
                retBlevel = wBlevel;
            }

        }

        return retBlevel;

    }

    /**
     * @param task
     * @param set
     * @return
     */
    public CustomIDSet calcMaxWCP(AbstractTask task, CustomIDSet set) {

        //もしENDタスクであれば，終了．
        if (task.getDsucList().isEmpty()) {
            task.setWblevel(task.getMaxWeight() / this.minSpeed);
            set.add(task.getIDVector().get(1));
            return set;
        }

        //後続タスクを取得
        Iterator<DataDependence> dsucIte = task.getDsucList().iterator();
        long wBlevel = 0;
        //後続タスクたちに対するループ
        while (dsucIte.hasNext()) {
            DataDependence dsuc = dsucIte.next();
            AbstractTask sucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));

            //もし後続タスクがチェック済みであれば，すっ飛ばす．
            if (set.contains(dsuc.getToID().get(1))) {

            } else {
                CustomIDSet retSet = this.calcMaxWCP(sucTask, set);
                set.addAll(retSet);

            }



            long tmpValue = sucTask.getWblevel() + task.getMaxWeight() / this.minSpeed;
            if (tmpValue >= wBlevel) {
                wBlevel = tmpValue;
            }

        }
        set.add(task.getIDVector().get(1));
        task.setWblevel(wBlevel);

        return set;

    }

    public long calcGMAX_task(AbstractTask task){
        //まずは先行タスク
        
        return 0;

    }

    /**
     * 局所的な粒度を求めます．
     * @param task
     * @return
     */
    private double getGofTask(AbstractTask task){
        //まず，先行タスクに対する処理
        Iterator<DataDependence> dpredIte = task.getDpredList().iterator();
        double g_pred = 0.0;
        double g_suc = 0.0;
        
        while(dpredIte.hasNext()){
            DataDependence dpred = dpredIte.next();
            //先行タスクを取得
            AbstractTask dpredTask = this.retApl.findTaskByLastID(dpred.getFromID().get(1));
            //gを求める．
            double tmpG1 = (double)(dpredTask.getMaxWeight()/this.minSpeed)/(dpred.getMaxDataSize()/this.minLink);
            double tmpG = Calc.getRoundedValue(tmpG1);
            if(tmpG >= g_pred){
                g_pred = tmpG;
            }
        }

        Iterator<DataDependence> dsucIte = task.getDsucList().iterator();
        while(dsucIte.hasNext()){
            DataDependence dsuc = dsucIte.next();
            //後続タスクを取得する．
            AbstractTask dsucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));
            //gを求める．
            double tmpSucG1 = (double)(dsucTask.getMaxWeight()/this.minSpeed)/(dsuc.getMaxDataSize()/this.minLink);
            double tmpSucG = Calc.getRoundedValue(tmpSucG1);
            if(tmpSucG >= g_suc){
                g_suc = tmpSucG;
            }
        }

        return Math.max(g_pred, g_suc);
    }


    /**
     * クラスタサイズの決定処理
     */
    public void configureDeltaOpt(){
        int taskNum = this.retApl.getTaskList().size();
        //1経路上の最大タスクサイズ和を求める．
        long wcp = this.getMaxWCP();
        this.retApl.setWcp(wcp);

        Iterator<AbstractTask> taskIte = this.retApl.taskIerator();
         long w_max = 0;
        double maxRate = 0.0;
        int dagmode = 0;
        Properties prop  = new Properties();
         try{
            //create input stream from file
            prop.load(new FileInputStream(this.fileName));
             w_max = Integer.valueOf(prop.getProperty("task.instructions.max")).longValue();
             dagmode = Integer.valueOf(prop.getProperty("task.dagtype")).intValue();


        }catch(Exception e){
            e.printStackTrace();
        }

        //各タスクに対するループ
        while(taskIte.hasNext()){
            AbstractTask task = taskIte.next();
            //g_max(n_k)を求める
            double local_g_max = this.getGofTask(task);
            //後続タスクのうちで，最大のタスクサイズを求める．
            /*long maxTaskSize = this.getMaxSucTaskSize(task);  */
            long maxTaskSize = 0;
          //  if((dagmode == 1)){
            //    maxTaskSize = this.getMaxSucTaskSize(task);
           // }else{
            maxTaskSize = w_max;
            //}
            double tmpValue1 = (double)maxTaskSize/local_g_max;
            double tmpValue = Calc.getRoundedValue(tmpValue1);
            if(maxRate <= tmpValue){
                maxRate = tmpValue;
            }  
        }

        long optimalSize = (long)Math.sqrt(maxRate * wcp);

        this.retApl.setOptDelta((long) optimalSize);
        //Optimal Delta値を閾値としてセットする．
        this.threshold = (long) optimalSize;
    }


    public void configureDeltaOpt2(){
        int taskNum = this.retApl.getTaskList().size();

        Iterator<AbstractTask> taskIte = this.retApl.taskIerator();
        double g_all = 0;
        double g_ave = 0;
        long sumTaskSize = 0;
        while(taskIte.hasNext()){
            AbstractTask task = taskIte.next();
            sumTaskSize += task.getMaxWeight()/this.minSpeed;
            //先行タスクたちを取得する．
            int predNum = task.getDpredList().size();
            int sucNum = task.getDsucList().size();
            //先行タスクたちのデータサイズを取得する．
            Iterator<DataDependence> dpredIte = task.getDpredList().iterator();
            long predDataSum = 0;
            long sucDataSum = 0;
            long predTaskSum = 0;
            long sucTaskSum = 0;
            int devValue = 0;
            if((predNum == 0)||(sucNum ==0)){
                devValue = 1;
            }else{
                devValue = 2;
            }
            double g_pred = 0;
            double g_suc = 0;

            if(!task.getDpredList().isEmpty()){
                while(dpredIte.hasNext()){
                    DataDependence dd = dpredIte.next();
                    long size = dd.getMaxDataSize()/this.minLink;
                    predDataSum += size;
                    AbstractTask predTask = this.retApl.findTaskByLastID(dd.getFromID().get(1));
                    predTaskSum += predTask.getMaxWeight()/this.minSpeed;
                }
                double g_predtmp = (double)predTaskSum/predDataSum;
                BigDecimal g_predtmp2 = new BigDecimal(String.valueOf(g_predtmp));
                g_pred =g_predtmp2.setScale(3,RoundingMode.HALF_UP).doubleValue();
            }


            //後続データサイズの和
            Iterator<DataDependence> dsucIte = task.getDsucList().iterator();
            if(!task.getDsucList().isEmpty()){
                while(dsucIte.hasNext()){
                    DataDependence dsuc = dsucIte.next();
                    long sucDataSize = dsuc.getMaxDataSize()/this.minLink;
                    sucDataSum += sucDataSize;
                    AbstractTask sucTask = this.retApl.findTaskByLastID(dsuc.getFromID().get(1));
                    sucTaskSum += sucTask.getMaxWeight()/this.minSpeed;

                }
                double g_suctmp = (double)sucTaskSum/sucDataSum;

                BigDecimal g_suctmp2 = new BigDecimal(String.valueOf(g_suctmp));
                g_suc = g_suctmp2.setScale(3,RoundingMode.HALF_UP).doubleValue();
            }

            

            //タスクの平均粒度を求める．
            double g_tasktmp = (g_pred + g_suc)/devValue;

            BigDecimal g_tasktmp2 = new BigDecimal(String.valueOf(g_tasktmp));
            double g_task = g_tasktmp2.setScale(3,RoundingMode.HALF_UP).doubleValue();
            g_all += g_task;

            BigDecimal g_all2 = new BigDecimal(g_all);
            g_all = g_all2.setScale(3, RoundingMode.HALF_UP).doubleValue();
            //g_all += (double)g_task;  
        }
        g_ave = g_all/taskNum;

        Properties prop = new Properties();
        try{
            //create input stream from file
            prop.load(new FileInputStream(this.fileName));
            long maxTaskNum = this.getMaxTaskNumInPath();
            long w_max = Integer.valueOf(prop.getProperty("task.instructions.max")).longValue();
            long w_min = Integer.valueOf(prop.getProperty("task.instructions.min")).longValue();
            long c_min = Long.valueOf(prop.getProperty("task.ddedge.size.min")).longValue();
            double w_ave = sumTaskSize/taskNum;
            double optimalSize = Math.sqrt(maxTaskNum / g_ave) * w_ave;

            this.retApl.setOptDelta((long) optimalSize);

            //Optimal Delta値を閾値としてセットする．
            this.threshold = (long) optimalSize;

        }catch(Exception e){
            e.printStackTrace();
        }



    }

    /**
     * 共通インタフェース
     */
    public BBTask process() {
        try {
            this.prepare();
            int size = this.retApl.getTaskList().size();
            AbstractTask endTask = this.retApl.findTaskByLastID(new Long(size));
            this.retApl.setCpLen(endTask.getTlevel() + endTask.getBlevel());

            //DAGの経路における，最大のタスク数の計算
            //long maxTaskNum = this.getMaxTaskNumInPath();
            // System.out.println("最大タスク数: " + maxTaskNum);

            //DAGの経路における，CP_wの最小値の計算
            // long minWCP = this.getMinWCP();
            long minWCP = this.getMinWCPValue();
            if(isThresholdSet){
            }else{
                configureDeltaOpt();
            }

            long start = System.currentTimeMillis();
            this.mainProcess();
            long end = System.currentTimeMillis();
            retApl.setProcessTime((end-start));
           // System.out.println("SIRT time:"+ (end-start));

            //次は，δに見た中なったものに対する，クラスタリング処f理
            //ここでは，LBに基づくものとする．
            this.mainProcessLB();
            //後処理を行う．
            this.postProcess();


            return this.retApl;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;


    }

    /**
     *
     */
    public void mainProcessCTM() {
        int cnt = 0;
        //まずはUnderリストへの追加処理
        Iterator<TaskCluster> allClusterIte = this.retApl.clusterIterator();
        while (allClusterIte.hasNext()) {
            TaskCluster cluster = allClusterIte.next();
            if (this.isClusterAboveThreshold(cluster)) {

            } else {
                this.underThresholdClusterList.add(cluster.getClusterID());
                if (this.isStartCluster(cluster)) {
                    cnt++;
                }


            }
        }
        // System.out.println("満たさない数: "+this.underThresholdClusterList.getList().size());
        // System.out.println("スタートクラスタは: "+ cnt);

        while (!this.underThresholdClusterList.isEmpty()) {
            //まず，UEXからレベルが最高っぽいものを取得する．
            TaskCluster checkCluster = this.getMaxTlevelCluster(this.underThresholdClusterList);
            if (checkCluster == null) {
                return;
            }
            //Bsucを取得する．
            Long bsucID = checkCluster.getBsucTaskID();
            AbstractTask bsucTask = this.retApl.findTaskByLastID(bsucID);

            //BsucのBsucを取得する．
            AbstractTask bsucBsucTask = this.retApl.findTaskByLastID(bsucTask.getBsuc().get(1));
            TaskCluster sucCluster = this.retApl.findTaskCluster(bsucBsucTask.getClusterID());
            this.clusteringClusterLB(checkCluster, sucCluster, this.underThresholdClusterList);
        }


    }


    /**
     *
     */
    public void mainProcessLB() {
        int cnt = 0;
        //まずはUnderリストへの追加処理
        Iterator<TaskCluster> allClusterIte = this.retApl.clusterIterator();
        while (allClusterIte.hasNext()) {
            TaskCluster cluster = allClusterIte.next();
            if (this.isClusterAboveThreshold(cluster)) {

            } else {
                this.underThresholdClusterList.add(cluster.getClusterID());
                if (this.isStartCluster(cluster)) {
                    cnt++;
                }

            }
        }
        // System.out.println("満たさない数: "+this.underThresholdClusterList.getList().size());
        // System.out.println("スタートクラスタは: "+ cnt);

        while (!this.underThresholdClusterList.isEmpty()) {
            //まず，UEXから最小のクラスタを取得する．
            TaskCluster checkCluster = this.getMinSizeCluster(this.underThresholdClusterList);
            if (checkCluster == null) {
                return;
            }
            //後続クラスタたちを取得する．
            //実際にはクラスタIDのリストが入っている
            CustomIDSet sucClusterIDSet = this.getClusterSuc(checkCluster);
            //もしcheckClusterがENDクラスタであれば，先行クラスタをクラスタリングする．
            if (sucClusterIDSet.isEmpty()) {
                //this.uexClusterList.remove(checkCluster.getClusterID());
                TaskCluster pivotCluster = this.getClusterPred(checkCluster);
                TaskCluster retCluster = this.clusteringClusterLB(pivotCluster, checkCluster, this.underThresholdClusterList);
                continue;
            }

            //後続クラスタたちのIDを取得する．
            Iterator<Long> sucClsIte = sucClusterIDSet.iterator();
            long size = 10000000;
            TaskCluster toCluster = null;
            //サイズが最小の後続クラスタを決定するためのループ
            while (sucClsIte.hasNext()) {
                TaskCluster sucCluster = this.retApl.findTaskCluster(sucClsIte.next());
                long value = this.getClusterInstruction(sucCluster);
                if (value <= size) {
                    size = value;
                    toCluster = sucCluster;
                }
            }
            //そしてクラスタリング処理
            TaskCluster retCluster = this.clusteringClusterLB(checkCluster, toCluster, this.underThresholdClusterList);

        }

    }


    /**
     *
     */
    public void printAll() {
        this.printAllDAG();
        printFreeClusterList();
        //System.out.println();

    }

    /**
     * pivot, targetとのクラスタリング処理です．
     * <p/>
     * 1. 2つのクラスタの集約, Top, In/Outの更新, DAGからのtargetの削除
     * 2. マージ後の一つのクラスタをUE/UEXに入れるかどうかの判断．
     * 3. TL, BL, Tpred, Bsuc, tlevel, blevel, Tpred, Bsucの更新をする．
     * <p/>
     * <p/>
     * （もし一定値以上であればUXに入れる）
     *
     * @param pivot
     * @param target
     * @return
     */
    public TaskCluster clusteringCluster(TaskCluster pivot, TaskCluster target) {
        Long topTaskID = pivot.getTopTaskID();
        if (pivot.getClusterID().longValue() > target.getClusterID().longValue()) {
            return this.clusteringCluster(target, pivot);
        }

        //  System.out.println("****クラスタリング処理: Pivot: "+ pivot.getClusterID().longValue() + "Target: "+target.getClusterID().longValue());
        //  System.out.println();
        //    this.printAll();
        //targetの全タスク集合を取得する．
        CustomIDSet IDSet = target.getTaskSet();
        Iterator<Long> taskIte = IDSet.iterator();

        //target内の全タスクの所属クラスタの変更処理
        while (taskIte.hasNext()) {
            Long taskID = taskIte.next();
            //fromClusterへタスクを追加する．
            pivot.addTask(taskID);
            //toClusterにあったタスクの所属クラスタの変更
            AbstractTask task = this.retApl.findTaskByLastID(taskID);
            task.setClusterID(pivot.getClusterID());
        }

        //pivotのIn/Outを更新
        //Topタスク→Topじゃなくなるかも(どちらかがTopでのこり，他方がTopじゃなくなる）
        //Outタスク→Outじゃなくなるかも（すくなくともTopにはならない）
        //それ以外→それ以外のまま
        //まずはclusterのtopを，pivot側にする．というかもともとそうなっているので無視
        //だけど，outSetだけは更新しなければならない．
        this.updateOutSet(pivot, target);
        //InSetを更新する（後のレベル値の更新のため）
        this.updateInSet(pivot, target);

        pivot.setTopTaskID(topTaskID);
        //各タスクの，destSetの更新をする．

        CustomIDSet allSet = pivot.getTaskSet();
        Iterator<Long> idIte = allSet.iterator();
        CustomIDSet startIDs = new CustomIDSet();
        //まずはTopタスクのIDを追加しておく．

        AbstractTask startTask = this.retApl.findTaskByLastID(pivot.getTopTaskID());
        pivot.getDestCheckedSet().getObjSet().clear();
        this.updateDestTaskList2(new CustomIDSet(), startTask, pivot.getClusterID());

        //targetクラスタをDAGから削除する．
        this.retApl.removeTaskCluster(target.getClusterID());
        //targetクラスタをUEXから削除する．
        this.uexClusterList.remove(target.getClusterID());
        //targetクラスタをFreeから削除する．
        this.freeClusterList.remove(target.getClusterID());

        //あとは，新pivotがEXなのかUEXなのかの判断をする．

        /**
         * pivotが，
         * - δ以上(Topの入力辺がすべて"Checked"になっている．Inについてはどうでもよい)
         *   →pivotをEXへ入れる(UEXから削除する)
         *   →pivotをFreeから削除する
         *   →pivotのOutタスクの，後続タスクの入力辺を"Checked"とする．
         * - δ未満の場合
         * 　→pivotはFreeのまま
         *   →pivotはUEXのまま
         *
         */
        if (this.isAvobeThreshold(pivot)) {
            Long endID = new Long(this.retApl.getTaskList().size());
            if ((pivot.isLinear()) && (!pivot.getTaskSet().contains(endID))) {

            } else {
                this.freeClusterList.remove(pivot.getClusterID());
                //pivotをEXへ入れる(つまり，UEXからpivotを削除する）
                this.uexClusterList.remove(pivot.getClusterID());
                //Outタスクの後続タスクの入力辺を"Checked"とする．
                CustomIDSet pOutSet = pivot.getOut_Set();
                Iterator<Long> pOutIte = pOutSet.iterator();
                CustomIDSet clsSet = new CustomIDSet();
                this.underThresholdClusterList.remove(pivot.getClusterID());

                //Outタスクに対するループ
                while (pOutIte.hasNext()) {
                    Long oID = pOutIte.next();
                    AbstractTask oTask = this.retApl.findTaskByLastID(oID);
                    //Outタスクの後続タスク集合を取得する．
                    LinkedList<DataDependence> dsucList = oTask.getDsucList();
                    Iterator<DataDependence> dsucIte = dsucList.iterator();
                    //Outタスクの，後続タスクたちに対するループ
                    while (dsucIte.hasNext()) {
                        DataDependence dd = dsucIte.next();
                        AbstractTask sucTask = this.retApl.findTaskByLastID(dd.getToID().get(1));
                        if (sucTask.getClusterID().longValue() != pivot.getClusterID().longValue()) {
                            //sucTaskの入力辺に"Checked"フラグをつける．
                            sucTask.setCheckedFlgToDpred(oTask.getIDVector(), sucTask.getIDVector(), true);
                            clsSet.add(sucTask.getClusterID());
                        }
                    }
                }

                Iterator<Long> clsIte = clsSet.iterator();
                while (clsIte.hasNext()) {
                    Long clusterid = clsIte.next();
                    TaskCluster clster = this.retApl.findTaskCluster(clusterid);
                    if (this.isAllInEdgeChecked(clster)) {
                        //もしクラスタのInタスクがすべてCheckeであれば，そのクラスタをFreeListへ入れる．
                        this.addFreeClusterList(clusterid);
                    }
                }

            }
        } else {
            //pivotがstartかつENDクラスタである場合，
            if ((this.isStartCluster(pivot)) && (pivot.getOut_Set().isEmpty())) {
                pivot = this.checkEXSingleCluster(pivot);

            } else {
                //以降，pivotのサイズ<δである場合の処理

                if (this.isAllInEdgeChecked(pivot)) {
                    this.freeClusterList.add(pivot.getClusterID());
                }

            }

        }
        // BL, Tpred, Bsuc, tlevel, blevel, Tpred, Bsucの更新をする．
        // TL(pivot)は不変なので，考慮する必要はない．
        this.updateLevel(pivot);
        return pivot;

    }

    /**
     * レベルの反映をします．<br>
     * 1. cluster(pivot)内の各タスクのtlevel値を更新する．この際，outタスクのみを更新すればよいことに注意  <br>
     * # pivotのTL値は，freeであったときからは不変．よって，topタスクのTpredタスクも不変であるから考慮する必要はない．<br>
     * # さらに，topタスク以外のoutタスクのTpredは，更新する必要はない（使わないので）．<br>
     * 2.  FreeクラスタたちのTL, Tpred(top(cls(Free)), tlevel(Out(cls(Free))) のみを更新する． <br>
     * # ここで，Freeクラスタ内の各タスクのblevel値は不変である．さらにS(cls(free), タスク)値も不変．よって，Bsuc(cls(free))も不変     <br>
     * よって，Freeクラスタのblevel, BL, Bsucはそのままで何もしない  <br>
     * pivotの後続タスクのうち，「freeクラスタ」であるものに対してレベルを更新する <br>
     * outタスクの後続タスクが所属するクラスタがfreeクラスタならば，  <br>
     * 2-1. そのクラスタのtopタスクを取得する  <br>
     * 2-2. topのtlevel値を計算し，topタスクのTpredを更新する．<br>
     * 2-3. そして，当該クラスタのoutタスクを取得して，そいつらのtlevel値を更新する．<br>
     * # さらに，TL値は同一増分がすべてのoutタスクに加算されるため，freeクラスタのBsucタスクも不変であるのでBsucも更新する必要はない． <br>
     * 3.  pivot内のタスクたちのOutタスクに対して，blevel値，bsucタスクの更新をする．さらに，このときにBL(pivot)の更新もする．     <br>
     *
     * @param pivot
     */
    public void updateLevel(TaskCluster pivot) {

        /**
         *  1の処理
         */
        Long pivotID = pivot.getClusterID();
        //pivotのoutタスクを取得する．
        CustomIDSet set = pivot.getOut_Set();
        Iterator<Long> ite = set.iterator();
        long clusterTlevel = 0;
        //とりあえずはクラスタ全体の処理時間を取得する．
        long totalEsecTime = this.getClusterInstruction(pivot) / this.minSpeed;

        //Outタスク全体に対するループ
        while (ite.hasNext()) {
            Long taskID = ite.next();
            //outタスクを取得
            AbstractTask task = this.retApl.findTaskByLastID(taskID);
            //Outタスクの先行タスクを取得
            LinkedList<DataDependence> dpredList = task.getDpredList();
            //Iterator<DataDependence> ddIte = dpredList.iterator();

            //CustomIDSet destSet = this.getDestTaskList(new CustomIDSet(),task,task.getClusterID());
            CustomIDSet destSet = task.getDestTaskSet();
            //もし先行タスクが同クラスタであれば、同クラスタ用のTlevel計算処理に入る
            long destValue = this.calculateSumValue(destSet);
            //tlevel値の計算
            long value = pivot.getTlevel() + totalEsecTime - (destValue / this.minSpeed);
            task.setTlevel(value);

        }

        /**
         * 2の処理
         * pivotの後続タスクのうち，「freeクラスタ」であるものに対してレベルを更新する
         * outタスクの後続タスクが所属するクラスタがfreeクラスタならば，
         * 1. その後続タスクが，その所属クラスタのtopタスクであれば，2へ
         * 2. topのtlevel値を計算し，topタスクのTpredを更新する．
         * 3. そして，当該クラスタのoutタスクを取得して，そいつらのtlevel値を更新する．
         *
         */
        //pivotがまだFreeかどうか
        if (this.freeClusterList.contains(pivot.getClusterID())) {
            //まだFreeであれば（今回のクラスタでもまだ閾値を満たされなかった場合），何もしない
            //なぜなら，pivotの後続クラスタはまだfreeではないから　
            // しかも，この場合は(1) == (2)である．

        } else {
            //pivotがすでにExaminedであれば，その後続クラスタはfreeになっている可能性がある．
            //まずはoutタスクを取得する．
            CustomIDSet outSet = pivot.getOut_Set();
            Iterator<Long> outIte = outSet.iterator();
            CustomIDSet tmpClusterIDList = new CustomIDSet();

            //outタスクに対するループ
            while (outIte.hasNext()) {
                Long oID = outIte.next();
                //outタスクを取得する．
                AbstractTask outTask = this.retApl.findTaskByLastID(oID);
                LinkedList<DataDependence> dsucList = outTask.getDsucList();
                Iterator<DataDependence> dsucIte = dsucList.iterator();
                //後続タスクに対するループ
                while (dsucIte.hasNext()) {
                    DataDependence dsuc = dsucIte.next();
                    //outタスクの後続タスクを取得する．
                    AbstractTask dsucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));
                    Long sClusterID = dsucTask.getClusterID();
                    //後続タスクが別クラスタであれば，freeかどうかのチェックをする
                    if (sClusterID.longValue() != pivotID.longValue()) {
                        //もしfreeクラスタであれば，1,2,3の処理へ移行する
                        if (this.freeClusterList.contains(sClusterID)) {
                            TaskCluster freeCluster = this.retApl.findTaskCluster(sClusterID);
                            /*if((!this.freeClusterList.contains(pivot.getClusterID())) &&(freeCluster.getObjSet().getList().size() > 1)){
                                System.out.println("FREEのタスク数: "+ freeCluster.getObjSet().getList().size());
                            }*/

                            //計算量削減のために，一クラスタのレベル計算は一度だけ
                            //tmpに入っていれば何もしない
                            if (tmpClusterIDList.contains(sClusterID)) {
                                continue;
                            } else {
                                //初めての計算となるから，ここで1,2,3の処理を行う．
                                TaskCluster sucCluster = this.retApl.findTaskCluster(sClusterID);
                                //dsucTaskが，topタスクのときだけ更新する．
                                if (dsucTask.getIDVector().get(1).longValue() == sucCluster.getTopTaskID().longValue()) {

                                    //topタスクの，tlevel値とTpredタスクを更新する．
                                    calculsteTlevelForTopTask(dsucTask);
                                    //pivotのTL値を更新する．
                                    pivot.setTlevel(dsucTask.getTlevel());
                                    //一度計算しましたよというマークをつける．
                                    tmpClusterIDList.add(sClusterID);
                                }
                            }
                        } else {
                            //freeでなければ何もしない
                            continue;
                        }
                    }
                }
            }
        }

        /**
         * 3の処理
         * pivotのoutタスクに対して，blevel+bsucタスクを更新する．
         * そして，BL(pivot)を更新する．
         *
         */
        Iterator<Long> outIte = set.iterator();
        //pivot用の一時bsucタスクのID
        Long bottomID = new Long(0);
        //pivotのBL値
        long tmpBLValue = 0;

        //outタスクに対するループ
        while (outIte.hasNext()) {
            Long outID = outIte.next();
            AbstractTask outTask = this.retApl.findTaskByLastID(outID);
            //outタスクの後続タスクを取得する．
            LinkedList<DataDependence> dsucList = outTask.getDsucList();
            Iterator<DataDependence> dsucIte = dsucList.iterator();
            //一つのoutタスク用のblevel
            long tmpValue = 0;
            AbstractTask tmpTask = new AbstractTask();

            //outタスクの後続タスクに対するループ
            while (dsucIte.hasNext()) {
                DataDependence dsuc = dsucIte.next();
                //後続タスクを取得する．
                AbstractTask dsucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));

                //後続タスクが別クラスタであれば，N/W遅延時間が発生する．
                long value = outTask.getMaxWeight() / this.minSpeed +
                        this.getNWTime(outTask.getIDVector().get(1), dsucTask.getIDVector().get(1), dsuc.getMaxDataSize(), this.minLink) + dsucTask.getBlevel();
                //blevel値と
                if ((value >= tmpValue) && (pivot.getClusterID().longValue() != dsucTask.getClusterID().longValue())) {
                    tmpValue = value;
                    tmpTask = dsucTask;
                }
            }

            //一つのoutタスクの，blevel+dsucタスクの更新
            outTask.setBlevel(tmpValue);
            outTask.setBsuc(tmpTask.getIDVector());
            //S値の計算
            long destValue = this.calculateSumValue(outTask.getDestTaskSet());

            long candidateBLValue = (totalEsecTime - destValue / this.minSpeed) + outTask.getBlevel();
            //BL値の更新処理
            if (candidateBLValue >= tmpBLValue) {
                bottomID = outTask.getIDVector().get(1);
                tmpBLValue = candidateBLValue;

            }
        }
        pivot.setBlevel(tmpBLValue);
        pivot.setBsucTaskID(bottomID);

    }


    /**
     * topタスクの先行クラスタから，新pivotを取得します．
     *
     * @param pivot
     * @return
     */
    public TaskCluster getNewPivot(TaskCluster pivot) {
        //Topタスクを取得する．
        AbstractTask topTask = this.retApl.findTaskByLastID(pivot.getTopTaskID());
        //Topタスクの先行タスクたちを取得する．
        LinkedList<DataDependence> dpredList = topTask.getDpredList();
        Iterator<DataDependence> dpredIte = dpredList.iterator();
        long totalExecNum_Pivot = this.getClusterInstruction(pivot);
        long retTLValue = 100000000;
        TaskCluster retCluster = null;

        //topタスクのTpredを取得する．
        Long tpredID = topTask.getTpred().get(1);
        AbstractTask tpredTask = this.retApl.findTaskByLastID(tpredID);
        Long predClusterID = tpredTask.getClusterID();
        TaskCluster predCluster = this.retApl.findTaskCluster(predClusterID);
        //Tpredタスクが先行クラスタのBsucかつ先行クラスタがLinearかつpivot自体がlinearであれば，結果的に生成される
        //クラスタもLinearである．よって，この先行クラスタを新Pivotとする．
        if ((predCluster.getBsucTaskID().longValue() == tpredTask.getIDVector().get(1).longValue()) && (predCluster.isLinear()) &&
                (pivot.isLinear())) {
            return predCluster;

        }

        //topタスクの先行タスクたちに対するループ
        while (dpredIte.hasNext()) {
            DataDependence dd = dpredIte.next();
            //先行タスクを取得する.
            AbstractTask dpredTask = this.retApl.findTaskByLastID(dd.getFromID().get(1));

            if (dpredTask.getClusterID().longValue() != pivot.getClusterID().longValue()) {
                TaskCluster newPivot = this.retApl.findTaskCluster(dpredTask.getClusterID());
                //pivot内の，他のタスクたちの大まかなBL値を計算する．
                long TL_others = this.getTLValue(newPivot, pivot, dpredTask);
                //target(もともとのpivot)の，outタスクのTlevelの上限値を計算する．
                long TL_target = this.getTLValueOfTarget(newPivot, pivot);

                long TLValue = Math.max(TL_others, TL_target);
                if (TLValue <= retTLValue) {
                    retTLValue = TLValue;
                    retCluster = newPivot;
                }
            }
        }
        //System.out.println("NON-LINEARになった");
        retCluster.setLinear(false);
        return retCluster;
    }


    /**
     * topタスクの先行クラスタから，新pivotを取得します．
     *
     * @param pivot
     * @return
     */
    public TaskCluster getNewPivot2(TaskCluster pivot) {
        //Topタスクを取得する．
        AbstractTask topTask = this.retApl.findTaskByLastID(pivot.getTopTaskID());
        //Topタスクの先行タスクたちを取得する．
        LinkedList<DataDependence> dpredList = topTask.getDpredList();
        Iterator<DataDependence> dpredIte = dpredList.iterator();
        long totalExecNum_Pivot = this.getClusterInstruction(pivot);
        long retTLValue = 100000000;
        TaskCluster retCluster = null;

        //topタスクのTpredを取得する．
        Long tpredID = topTask.getTpred().get(1);
        AbstractTask tpredTask = this.retApl.findTaskByLastID(tpredID);
        Long predClusterID = tpredTask.getClusterID();
        TaskCluster predCluster = this.retApl.findTaskCluster(predClusterID);
        //Tpredタスクが先行クラスタのBsucかつ先行クラスタがLinearかつpivot自体がlinearであれば，結果的に生成される
        //クラスタもLinearである．よって，この先行クラスタを新Pivotとする．
        if ((predCluster.getBsucTaskID().longValue() == tpredTask.getIDVector().get(1).longValue()) && (predCluster.isLinear()) &&
                (pivot.isLinear())) {
            return predCluster;

        }

        retCluster.setLinear(false);
        //そうでない場合は，Top(pivot)のTpredタスクを含んでいる先行クラスタを，新Pivotとする．
        return predCluster;
    }


    /**
     * @param pivot
     * @param target
     * @param sucTask
     * @return
     */
    public long getTLValue(TaskCluster pivot, TaskCluster target, AbstractTask sucTask) {
        long totalExecNum = this.getClusterInstruction(pivot);
        CustomIDSet outSet = pivot.getOut_Set();
        Iterator<Long> outIte = outSet.iterator();
        long retTLValue = 0;
        //pivotのoutタスクたちに対するループ
        while (outIte.hasNext()) {
            Long outID = outIte.next();
            AbstractTask outTask = this.retApl.findTaskByLastID(outID);
            if (outTask.getIDVector().get(1).longValue() == sucTask.getIDVector().get(1).longValue()) {
                continue;
            } else {
                long destValue = this.calculateSumValue(outTask.getDestTaskSet());
                long TLValue = (totalExecNum - destValue + this.getClusterInstruction(target)) / this.minSpeed /*+ outTask.getBlevel()*/;
                if (retTLValue <= TLValue) {
                    retTLValue = TLValue;
                }

            }
        }
        return retTLValue;

    }


    /**
     * @param pivot
     * @param target
     * @param sucTask
     * @return
     */
    public long getTLValueOfPivot(TaskCluster pivot, TaskCluster target, AbstractTask outTask, AbstractTask sucTask) {
        long totalExecNum = this.getClusterInstruction(pivot);
        CustomIDSet outSet = pivot.getOut_Set();
        Iterator<Long> outIte = outSet.iterator();
        long retTLValue = 0;

        CustomIDSet predSet = new CustomIDSet();
        CustomIDSet sucSet = new CustomIDSet();

        Iterator<DataDependence> dpredIte = sucTask.getDpredList().iterator();
        while (dpredIte.hasNext()) {
            DataDependence dpred = dpredIte.next();
            AbstractTask dpredTask = this.retApl.findTaskByLastID(dpred.getFromID().get(1));
            if (dpredTask.getClusterID().longValue() == pivot.getClusterID().longValue()) {
                // predSet.add(dpredTask.getIDVector().get(1));
                //dpredTaskの先行タスクを入れる．
                Iterator<Long> sucIte = dpredTask.getDestTaskSet().iterator();
                while (sucIte.hasNext()) {
                    sucSet.add(sucIte.next());

                }
                //sucSetから自分自身を取り除く
                sucSet.remove(dpredTask.getIDVector().get(1));


            }
        }
        //pivotのoutタスクたちに対するループ
        while (outIte.hasNext()) {
            Long outID = outIte.next();
            AbstractTask outTask2 = this.retApl.findTaskByLastID(outID);
            long destValue = this.calculateSumValue(outTask2.getDestTaskSet());
            long TLValue = 0;
            long BLValue = 0;
            //pivot内のoutTask->sucTaskの組み合わせの場合，outTaskのSTART時刻のMaxにsucTaskを含めない
            if (!sucSet.contains(outID)) {
                TLValue = (totalExecNum - destValue) / this.minSpeed;

            } else {
                TLValue = (totalExecNum - destValue + this.getClusterInstruction(target)) / this.minSpeed /*+ outTask.getBlevel()*/;

            }
            if (retTLValue <= TLValue) {
                retTLValue = TLValue;
            }

        }
        return retTLValue;

    }

    /**
     * @param pivot
     * @param target
     * @return
     */
    public long getTLValueOfTarget(TaskCluster pivot, TaskCluster target) {
        long pivottotalExecNum = this.getClusterInstruction(pivot);
        long targettotalExecNum = this.getClusterInstruction(pivot);
        CustomIDSet outSet = target.getOut_Set();
        Iterator<Long> outIte = outSet.iterator();
        long retTLValue = 0;
        //targetのoutタスクたちに対するループ
        while (outIte.hasNext()) {
            Long outID = outIte.next();
            AbstractTask outTask = this.retApl.findTaskByLastID(outID);

            long destValue = this.calculateSumValue(outTask.getDestTaskSet());
            long TLValue = (pivottotalExecNum + targettotalExecNum - destValue) / this.minSpeed /*+ outTask.getBlevel()*/;
            if (retTLValue <= TLValue) {
                retTLValue = TLValue;
            }

        }
        return retTLValue;

    }

    /**
     * @param pivot
     * @return
     */
    public TaskCluster getTarget2(TaskCluster pivot) {
        CustomIDSet outSet = pivot.getOut_Set();
        Iterator<Long> outIte = outSet.iterator();
        CustomIDSet clusterSet = new CustomIDSet();
        long totalExecNum = this.getClusterInstruction(pivot);

        long retBLValue = 10000000;
        TaskCluster retCluster = null;
        Long bsucTaskID = pivot.getBsucTaskID();
        AbstractTask bsucTask = this.retApl.findTaskByLastID(bsucTaskID);
        //pivotがlinearであるとき
        if (pivot.isLinear()) {
            //bottomタスクを特定する．
            Iterator<Long> outIte0 = outSet.iterator();
            //実際には一度のループのみである．
            while (outIte0.hasNext()) {
                Long outID = outIte0.next();
                AbstractTask outTask = this.retApl.findTaskByLastID(outID);
                if (this.isBottomTask(outTask)) {
                    //buttomタスクのDsucを取得
                    AbstractTask bsucBsucTask = this.retApl.findTaskByLastID(bsucTask.getBsuc().get(1));
                    TaskCluster tmpCluster = this.retApl.findTaskCluster(bsucBsucTask.getClusterID());
                    //buttom-dsucタスクが単一クラスタであればそれを返す．
                    if ((!this.isClusterAboveThreshold(tmpCluster)) && (tmpCluster.getTopTaskID().longValue() == bsucBsucTask.getIDVector().get(1).longValue())) {
                        retCluster = tmpCluster;
                        this.cnt_1++;
                        return retCluster;
                    } else {
                        //そうでなければbuttomの後続タスクのうち，単一クラスタとなるもののうちでblevelが最大のものを取得
                        Iterator<DataDependence> dsucIte = outTask.getDsucList().iterator();
                        long tmpBlevel = 0;

                        while (dsucIte.hasNext()) {
                            DataDependence dsuc = dsucIte.next();
                            AbstractTask sucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));
                            TaskCluster sucCluster = this.retApl.findTaskCluster(sucTask.getClusterID());
                            //sucTaskがtopタスクであるクラスタ == 単一クラスタということ．
                            if ((!this.isClusterAboveThreshold(sucCluster)) && (sucCluster.getTopTaskID().longValue() == sucTask.getIDVector().get(1).longValue())) {
                                long tmpValue = this.getNWTime(outTask.getIDVector().get(1), sucTask.getIDVector().get(1), dsuc.getMaxDataSize(), this.minLink) + sucTask.getBlevel();
                                if (tmpValue >= tmpBlevel) {
                                    tmpBlevel = tmpValue;
                                    retCluster = sucCluster;

                                }


                            }
                        }
                        //retClusterがnullでなければそれを返す．
                        if (retCluster != null) {
                            return retCluster;
                        }
                    }


                }
            }
            /**
             //Dtaskがbottomであれば，そのBsucをtargetとすればよい．
             if (this.isBottomTask(bsucTask)) {
             //pivotがLinearかつBsucタスク==Bottomタスクであれば，bsucのbsucが属するクラスタをtargetとする
             //このとき，targetは，ひとつのみのタスクで構成されているはずである．
             AbstractTask bsucBsucTask = this.retApl.findTaskByLastID(bsucTask.getBsuc().get(1));
             TaskCluster tmpCluster = this.retApl.findTaskCluster(bsucBsucTask.getClusterID());
             if ((!this.isClusterAboveThreshold(tmpCluster)) && (tmpCluster.getTopTaskID().longValue() == bsucBsucTask.getIDVector().get(1).longValue())) {
             retCluster = tmpCluster;
             this.cnt_1++;
             return retCluster;
             }
             }**/
        }
        //そうでなければ，pivotはnon-linearとなるはずである．

        AbstractTask tmpSucTask = null;
        long level = 100000000;

        //outタスクたちに対するループ
        while (outIte.hasNext()) {
            Long outID = outIte.next();
            //outタスクを取得する．
            AbstractTask outTask = this.retApl.findTaskByLastID(outID);
            LinkedList<DataDependence> ddList = outTask.getDsucList();
            Iterator<DataDependence> ddIte = ddList.iterator();
            long destValue = this.calculateSumValue(outTask.getDestTaskSet());

            //outタスクの後続タスクたちに対するループ
            while (ddIte.hasNext()) {
                DataDependence dd = ddIte.next();
                AbstractTask sucTask = this.retApl.findTaskByLastID(dd.getToID().get(1));

                Long sucClusterID = sucTask.getClusterID().longValue();
                long TLValue = 0;
                //  long BLValue = 0;
                if (sucClusterID.longValue() != pivot.getClusterID().longValue()) {
                    //後続クラスタを見つけた場合，後続クラスタを取得する．
                    TaskCluster sucCluster = this.retApl.findTaskCluster(sucClusterID);
                    if (sucCluster.getClusterID().longValue() == sucTask.getIDVector().get(1).longValue()) {

                        //クラスタリングした後のBsucタスクのblevelを計算する．
                        if (bsucTask.findDDFromDsucList(bsucTask.getIDVector(), sucTask.getIDVector()) != null) {
                            //もしbsucの後続タスクにsucTaskが含まれていれば，TL値は不変である
                            TLValue = bsucTask.getTlevel();
                            //  BLValue = this.getInstrunction(bsucTask)/this.minSpeed + bsucTask.getBlevel();


                        } else {
                            TLValue = bsucTask.getTlevel() + this.getInstrunction(sucTask) / this.minSpeed;
                            // BLValue = bsucTask.getBlevel();
                        }
                        //次にBlevel値を取得する．
                        long BLValue = this.getBsucBlevel(pivot, bsucTask, sucTask);
                        //BsucのLevel値が最も抑えられるもの
                        if (level >= TLValue + BLValue) {
                            level = TLValue + BLValue;
                            retCluster = sucCluster;
                            tmpSucTask = sucTask;
                        }

                    } else {
                    }
                }
            }
        }

        //System.out.println("もともとのレベル: "+( bsucTask.getTlevel()+bsucTask.getBlevel()+"/ 更新後のレベル: "+ level));
        //pivotがlinearかつBsucがbottomであり，上の条件でクラスタリングされればpivotはlinear．
        //よって，このときに初めてクラスタリング後のpivotはnon-linearとする．
        if (retCluster != null) {
            if ((this.isClusterAboveThreshold(pivot)) && (pivot.isLinear())) {
                if (!this.isBottomTask(tmpSucTask)) {
                    return null;
                }
            }
            if ((pivot.isLinear()) && (this.isBottomTask(bsucTask)) && (bsucTask.findDDFromDsucList(bsucTask.getIDVector(), tmpSucTask.getIDVector()) != null)) {
                pivot.setLinear(true);
                this.cnt_3++;

            } else {

                pivot.setLinear(false);
                this.cnt_2++;
                /*if(bsucTask.getIDVector().get(1).longValue() < tmpSucTask.getIDVector().get(1).longValue()){
                    System.out.println("○");
                }else{
                    System.out.println("×");

                }*/
                //   System.out.println("NON-LINEARになった");


            }

        } else {
            //System.out.println("CCC");
        }

        return retCluster;
    }

    /**
     * @param pivot
     * @param bsucTask
     * @param sucTask
     * @return
     */
    public long getBsucBlevel(TaskCluster pivot, AbstractTask bsucTask, AbstractTask sucTask) {
        Iterator<DataDependence> dsucIte = bsucTask.getDsucList().iterator();

        long blevel = 0;
        //bsucの後続タスクたち
        while (dsucIte.hasNext()) {
            DataDependence dd = dsucIte.next();
            long tmpValue = 0;
            if (dd.getToID().get(1).longValue() == sucTask.getIDVector().get(1)) {
                tmpValue = this.getInstrunction(bsucTask) / this.minSpeed + sucTask.getBlevel();
            } else {
                tmpValue = this.getInstrunction(bsucTask) / this.minSpeed +
                        this.getNWTime(bsucTask.getIDVector().get(1), dd.getToID().get(1), dd.getMaxDataSize(), this.minLink) + sucTask.getBlevel();
            }
            if (tmpValue >= blevel) {
                blevel = tmpValue;
            }

        }
        return blevel;

    }




    public boolean isLinearForClustering(TaskCluster cluster) {
        AbstractTask bsucTask = this.retApl.findTaskByLastID(cluster.getBsucTaskID());
        if ((cluster.isLinear()) && (this.isBottomTask(bsucTask))) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * メイン処理です．以下の処理を行います．<br>
     */
    public void mainProcess() {
        //UEXクラスタが残っている場合の処理
        while (!this.uexClusterList.isEmpty()) {
            TaskCluster pivot = new TaskCluster(new Long(-1));
            TaskCluster target = new TaskCluster(new Long(-1));
            long start = System.currentTimeMillis();
            //Freeクラスタリストから，levelが最大のものを取得する．
            //Freeであるクラスタには，一つ以上のタスクが含まれている場合があることに注意．
            TaskCluster pivotCandidate = this.getMaxLevelCluster(this.freeClusterList);

            if (this.isAvobeThreshold(pivotCandidate)) {
                this.checkEXSingleCluster(pivotCandidate);
                continue;

            }

            //もしpivotCandidateが，出辺を持っていないとき，つまりENDクラスタである場合(targetが選べない場合)
            if (pivotCandidate.getOut_Set().isEmpty()) {
                if (this.isStartCluster(pivotCandidate)) {
                    //StartクラスタかつENDクラスタであれば，終わりとする．
                    this.checkEXSingleCluster(pivotCandidate);
                    continue;
                } else {
                    //pivotCandidateがENDクラスタであり，かつStartクラスタではない場合．
                    //pivotを，TL値を決定づけるタスククラスタ/targetを自分自身とする．
                    pivot = this.getNewPivot(pivotCandidate);
                    // System.out.println("pivot候補のタスク数: "+ pivotCandidate.getObjSet().getList().size());
                    //targetを，pivot候補とする．
                    target = pivotCandidate;
                }
            } else {
                //targetを取得する．
                target = this.getTarget2(pivotCandidate);
                if (target == null) {
                    if (this.isStartCluster(pivotCandidate)) {
                        this.checkEXSingleCluster(pivotCandidate);
                        continue;
                    } else {
                        //targetが取得できなければ，上へクラスタ
                        //pivotを，TL値を決定づけるタスククラスタ/targetを自分自身とする．
                        pivot = this.getNewPivot(pivotCandidate);
                        // System.out.println("pivot候補のタスク数: "+ pivotCandidate.getObjSet().getList().size());
                        //targetを，pivot候補とする．
                        target = pivotCandidate;
                    }
                } else {
                    pivot = pivotCandidate;
                    // System.out.println("TARGET数: "+ target.getObjSet().getList().size());
                }
            }

            //クラスタリング処理
            this.clusteringCluster(pivot, target);

        }
    }

    /**
     * @param cluster
     * @return
     */
    public TaskCluster checkEXSingleCluster(TaskCluster cluster) {
        Long topID = cluster.getTopTaskID();

        //クラスタをUEXから削除する．
        this.uexClusterList.remove(cluster.getClusterID());
        //targetクラスタをFreeから削除する．
        this.freeClusterList.remove(cluster.getClusterID());

        //Outタスクの後続タスクの入力辺を"Checked"とする．
        CustomIDSet pOutSet = cluster.getOut_Set();
        Iterator<Long> pOutIte = pOutSet.iterator();
        CustomIDSet clusterSet = new CustomIDSet();

        //Outタスクに対するループ
        while (pOutIte.hasNext()) {
            Long oID = pOutIte.next();
            AbstractTask oTask = this.retApl.findTaskByLastID(oID);
            //Outタスクの後続タスク集合を取得する．
            LinkedList<DataDependence> dsucList = oTask.getDsucList();
            Iterator<DataDependence> dsucIte = dsucList.iterator();
            //Outタスクの，後続タスクたちに対するループ
            while (dsucIte.hasNext()) {
                DataDependence dd = dsucIte.next();
                AbstractTask sucTask = this.retApl.findTaskByLastID(dd.getToID().get(1));
                if (sucTask.getClusterID().longValue() != cluster.getClusterID().longValue()) {
                    //sucTaskの入力辺に"Checked"フラグをつける．
                    sucTask.setCheckedFlgToDpred(oTask.getIDVector(), sucTask.getIDVector(), true);
                    clusterSet.add(sucTask.getClusterID());
                }

            }
        }

        Iterator<Long> clusterIte = clusterSet.iterator();
        while (clusterIte.hasNext()) {
            Long cID = clusterIte.next();
            TaskCluster cls = this.retApl.findTaskCluster(cID);
            if ((this.isAllInEdgeChecked(cls)) && (this.uexClusterList.contains(cID))) {
                this.addFreeClusterList(cID);

            }
        }
        cluster.setTopTaskID(topID);

        return cluster;

    }


    /**
     * @return
     */
    public CustomIDSet getFreeClusterList() {
        return freeClusterList;
    }

    /**
     * @param freeClusterList
     */
    public void setFreeClusterList(CustomIDSet freeClusterList) {
        this.freeClusterList = freeClusterList;
    }

    /**
     * @param id
     */
    public void addFreeList(Long id) {
        this.addFreeClusterList(id);
    }

    /**
     * @param id
     */
    public void removeFreeList(Long id) {
        this.freeClusterList.remove(id);
    }


}
