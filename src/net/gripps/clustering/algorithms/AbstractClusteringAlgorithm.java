package net.gripps.clustering.algorithms;

import net.gripps.clustering.common.aplmodel.*;
import net.gripps.clustering.common.aplmodel.TaskCluster;
import net.gripps.environment.CPU;
import net.gripps.environment.Environment;
import net.gripps.environment.Machine;

import java.util.*;
import java.io.FileInputStream;

/**
 * Author: H. Kanemitsu
 * Date: 2007/12/07
 */
public class AbstractClusteringAlgorithm {

    /**
     *
     */
    protected long cnt_1;

    /**
     *
     */
    protected long cnt_2;

    /**
     *
     */
    protected long cnt_3;

    /**
     *
     */
    protected String file;

    /**
     *
     */
    protected CustomIDSet freeClusterList;

    protected CustomIDSet cyclicClusterSet;

    /**
     * ランダム生成された，オリジナルタスクグラフ
     */
    protected BBTask apl;

    /**
     * このクラスタリングアルゴリズムで生成されたDAG
     */
    protected BBTask retApl;

    /**
     *
     */
    protected int mode;

    protected long threshold;

    protected long minSpeed;

    protected long maxSpeed;

    protected long minLink;

    protected long maxLink;

    protected long level;

    protected long postLevel;
    protected long makeSpan;

    protected long criticalPath;

    /**
     *
     */
    protected CustomIDSet uexClusterList;

    protected CustomIDSet checkedClusterSet;
    protected CustomIDSet checkedClusterSetBlevel;
    protected CustomIDSet IDSetBlevel;

    protected CustomIDSet checkedClusterSet2;
    protected CustomIDSet checkedIDSet;

    protected CustomIDSet checkedIDSet2;

    protected CustomIDSet underThresholdClusterList;
    protected CustomIDSet IDSet;
    protected CustomIDSet IDSet2;

    protected BBTask retAplCopy;

    protected int algorithmID;

    protected HashMap<String, CyclicMap> cyclicLIst;

    protected int postUpdateLevel;

    protected long maxTaskSize;
    protected long setupTime;



    public AbstractClusteringAlgorithm(String file, BBTask apl){
        this.retApl = apl;
        //this.setApl((BBTask) apl.deepCopy());
        this.file = file;
        this.level = 0;
        this.postLevel = 0;
        this.checkedClusterSet = new CustomIDSet();
        this.checkedClusterSet2 = new CustomIDSet();
        this.checkedIDSet = new CustomIDSet();
        this.underThresholdClusterList = new CustomIDSet();
        this.checkedIDSet2 = new CustomIDSet();
        this.IDSet = new CustomIDSet();
        this.IDSet2 = new CustomIDSet();
        this.freeClusterList = new CustomIDSet();
        this.uexClusterList = new CustomIDSet();
        this.makeSpan = 0;
        this.cyclicClusterSet = new CustomIDSet();
        this.cyclicLIst = new HashMap<String, CyclicMap>();
        this.checkedClusterSetBlevel = new CustomIDSet();
        this.IDSetBlevel = new CustomIDSet();
        this.cnt_1 = 0;
        this.cnt_2 = 0;
        this.cnt_3 = 0;
        this.criticalPath = 0;
        this.maxTaskSize = 0;
        this.setupTime = 0;

      try {
            Properties prop = new Properties();
            //create input stream from file
            prop.load(new FileInputStream(file));
            //this.filename = file;
            this.mode = Integer.valueOf(prop.getProperty("task.weight.calcmethod")).intValue();
       //     this.threshold = Long.valueOf(prop.getProperty("task.instructions.threshold")).longValue();
       //     int size = this.retApl.getTaskClusterList().size();
            //this.minSpeed = Integer.valueOf(prop.getProperty("cpu.speed.min")).intValue();
          this.setupTime = Long.valueOf(prop.getProperty("env.setuptime")).longValue();

            int isHopExists = Integer.valueOf(prop.getProperty("network.ishopexists")).intValue();

            //もしホップ考慮なら，転送速度 = 帯域/ホップ
            if(isHopExists == 1){
                int maxHop = Integer.valueOf(prop.getProperty("network.hop.max")).intValue();
                int minHop = Integer.valueOf(prop.getProperty("network.hop.min")).intValue();
                //this.minLink = Integer.valueOf(prop.getProperty("cpu.link.min")).intValue()/maxHop;
                this.maxLink = Integer.valueOf(prop.getProperty("cpu.link.max")).intValue()/minHop;
                this.maxSpeed =  Integer.valueOf(prop.getProperty("cpu.speed.max")).intValue();
            }else{
                this.minLink = Integer.valueOf(prop.getProperty("cpu.link.min")).intValue();
                this.maxLink = Integer.valueOf(prop.getProperty("cpu.link.max")).intValue();
                this.maxSpeed = Integer.valueOf(prop.getProperty("cpu.speed.max")).intValue();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
      

    }

    /**
     * @param task
     */
    public AbstractClusteringAlgorithm(BBTask task) {
        //this.apl = task;
        //FailSafeのために，複製オブジェクトを使用する．
        this.setApl((BBTask) task.deepCopy());

        //this.retApl = new BBTask(Constants.TYPE_BASIC_BLOCK, 0, 0, 0);
        //結果のタスクをコピーしておく．
        this.retApl = (BBTask) this.apl.deepCopy();

        this.level = 0;
        this.postLevel = 0;
        this.checkedClusterSet = new CustomIDSet();
        this.checkedIDSet = new CustomIDSet();
        this.underThresholdClusterList = new CustomIDSet();
        this.checkedIDSet2 = new CustomIDSet();
        this.IDSet = new CustomIDSet();
        this.freeClusterList = new CustomIDSet();
        this.uexClusterList = new CustomIDSet();
        this.checkedClusterSet2 = new CustomIDSet();
        this.IDSet2 = new CustomIDSet();
        this.cyclicClusterSet = new CustomIDSet();
        this.cyclicLIst = new HashMap<String, CyclicMap>();
                this.cnt_1 = 0;
        this.cnt_2 = 0;
        this.cnt_3 = 0;
        this.criticalPath = 0;
        this.maxTaskSize = 0;
          //this.minLink = 1;
        //  this.minSpeed = 1;

    }

    public AbstractClusteringAlgorithm(BBTask task, String file, int algorithm) {
        //this.apl = task;
        //FailSafeのために，複製オブジェクトを使用する．
        this.setApl((BBTask) task.deepCopy());
        this.algorithmID = algorithm;

        //this.retApl = new BBTask(Constants.TYPE_BASIC_BLOCK, 0, 0, 0);
        //結果のタスクをコピーしておく．
        this.retApl = (BBTask) this.apl.deepCopy();

        this.level = 0;
        this.postLevel = 0;
        this.checkedClusterSet = new CustomIDSet();
        this.checkedClusterSet2 = new CustomIDSet();
        this.checkedIDSet = new CustomIDSet();
        this.underThresholdClusterList = new CustomIDSet();
        this.checkedIDSet2 = new CustomIDSet();
        this.IDSet = new CustomIDSet();
        this.IDSet2 = new CustomIDSet();
        this.freeClusterList = new CustomIDSet();
        this.uexClusterList = new CustomIDSet();
        this.makeSpan = 0;
        this.cyclicClusterSet = new CustomIDSet();
        this.cyclicLIst = new HashMap<String, CyclicMap>();
        this.checkedClusterSetBlevel = new CustomIDSet();
        this.IDSetBlevel = new CustomIDSet();
        this.cnt_1 = 0;
        this.cnt_2 = 0;
        this.cnt_3 = 0;
        this.criticalPath = 0;
        this.file = file;
        this.maxTaskSize = 0;

        try {
            Properties prop = new Properties();
            //create input stream from file
            prop.load(new FileInputStream(file));
            //this.filename = file;
            this.mode = Integer.valueOf(prop.getProperty("task.weight.calcmethod")).intValue();
            this.threshold = Long.valueOf(prop.getProperty("task.instructions.threshold")).longValue();
            int size = this.retApl.getTaskClusterList().size();
            this.minSpeed = Integer.valueOf(prop.getProperty("cpu.speed.min")).intValue();
            int isHopExists = Integer.valueOf(prop.getProperty("network.ishopexists")).intValue();
            this.setupTime = Long.valueOf(prop.getProperty("env.setuptime")).longValue();

            //もしホップ考慮なら，転送速度 = 帯域/ホップ
            if(isHopExists == 1){
                int maxHop = Integer.valueOf(prop.getProperty("network.hop.max")).intValue();
                int minHop = Integer.valueOf(prop.getProperty("network.hop.min")).intValue();
                this.minLink = Integer.valueOf(prop.getProperty("cpu.link.min")).intValue()/maxHop;
                this.maxLink = Integer.valueOf(prop.getProperty("cpu.link.max")).intValue()/minHop;
                this.maxSpeed =  Integer.valueOf(prop.getProperty("cpu.speed.max")).intValue();
            }else{
                this.minLink = Integer.valueOf(prop.getProperty("cpu.link.min")).intValue();
                this.maxLink = Integer.valueOf(prop.getProperty("cpu.link.max")).intValue();
                this.maxSpeed = Integer.valueOf(prop.getProperty("cpu.speed.max")).intValue();
            }
         // this.minLink = 1;
          //this.minSpeed = 1;

            this.postUpdateLevel = Integer.valueOf(prop.getProperty("algorithm.postprocess.updatelevel")).intValue();
            this.uexClusterList = new CustomIDSet();

        } catch (Exception e) {
            e.printStackTrace();
        }


    }


    public boolean isHetero(){
        if(this.minSpeed == this.maxSpeed){
            return false;
        }else{
            return true;
        }
    }

    /**
     * ヘテロ環境用に，実際の最小値を代入する．
     */
    public void updateMinValus(){
        try{
            Properties prop = new Properties();
            //create input stream from file
            prop.load(new FileInputStream(this.file));
                this.maxLink = Integer.valueOf(prop.getProperty("cpu.link.min")).intValue();
                this.maxSpeed = Integer.valueOf(prop.getProperty("cpu.speed.min")).intValue();
        }catch(Exception e){
            e.printStackTrace();
        }

    }

    public long getThreshold() {
        return threshold;
    }

    public void setThreshold(long threshold) {
        this.threshold = threshold;
    }

    public long getCnt_1() {
        return cnt_1;
    }

    public void setCnt_1(long cnt_1) {
        this.cnt_1 = cnt_1;
    }

    public long getCnt_2() {
        return cnt_2;
    }

    public void setCnt_2(long cnt_2) {
        this.cnt_2 = cnt_2;
    }

    public long getCnt_3() {
        return cnt_3;
    }

    public void setCnt_3(long cnt_3) {
        this.cnt_3 = cnt_3;
    }

    public BBTask getApl() {
        return apl;
    }

    public void setApl(BBTask apl) {
        this.apl = apl;
    }

    public BBTask getRetApl() {
        return retApl;
    }

    public void setRetApl(BBTask retApl) {
        this.retApl = retApl;
    }

    public long getLevel() {
        return level;
    }

    public void setLevel(long level) {
        this.level = level;
    }

    public long getPostLevel() {
        return postLevel;
    }

    public void setPostLevel(long postLevel) {
        this.postLevel = postLevel;
    }


    /**
     * @param set
     * @return
     */
    public TaskCluster getMinSizeCluster(CustomIDSet set) {
        Iterator<Long> ite = set.iterator();
        long value = 10000000;
        TaskCluster retCluster = null;

        while (ite.hasNext()) {
            Long cID = ite.next();
            TaskCluster cluster = this.retApl.findTaskCluster(cID);
            long tmpValue = this.getClusterInstruction(cluster);
            if (tmpValue <= value) {
                value = tmpValue;
                retCluster = cluster;
            }

        }

        return retCluster;
    }


    /**
     * @param cluster
     * @return
     */
    public boolean isClusterAboveThreshold(TaskCluster cluster) {
        if (this.getClusterInstruction(cluster) < this.threshold) {
            return false;
        } else {
            return true;
        }
    }

    public void updateInOut(TaskCluster cluster, TaskCluster toCluster) {
        this.updateInSet(cluster, toCluster);
        this.updateOutSet(cluster, toCluster);
    }


    /**
     * pivot内の，bottomタスク（後続タスクがすべて別クラスタ）である
     *
     * @param pivot
     * @return
     */
    public TaskCluster setBottomTask(TaskCluster pivot) {
        return null;
    }

    /**
     * Topタスクのtlevelを計算します．
     * つまり，先行タスクがすべて別クラスタに存在することを前提とした計算をします（N/W時間がある）．
     *
     * @param topTask
     * @return
     */
    public AbstractTask calculsteTlevelForTopTask(AbstractTask topTask) {
        LinkedList<DataDependence> dpredList = topTask.getDpredList();
        Iterator<DataDependence> dpredIte = dpredList.iterator();
        long tmpValue = 0;
        AbstractTask tpredTask = new AbstractTask();

        //topタスクの先行タスクに対するループ
        while (dpredIte.hasNext()) {
            DataDependence dpred = dpredIte.next();
            //先行タスクを取得する．
            AbstractTask predTask = this.retApl.findTaskByLastID(dpred.getFromID().get(1));
            //データ到着時刻を取得する．
            long value = predTask.getTlevel() + (dpred.getMaxDataSize() / this.minLink);
            //値の更新
            if (tmpValue <= value) {
                tmpValue = value;
                tpredTask = predTask;
            }
        }
        //最終的に決まった値を反映する．
        topTask.setTlevel(tmpValue);
        topTask.setTpred(tpredTask.getIDVector());

        return topTask;
    }

        /**
     * @param predTask
     * @param sucTask
     */
    public void addVirtualEdge(AbstractTask predTask, AbstractTask sucTask) {
        //predとsucの間にデータ依存を加える．
        if (predTask.findDDFromDsucList(predTask.getIDVector(), sucTask.getIDVector()) == null) {
            //データ依存がなければ，追加する．
            DataDependence dd = new DataDependence(predTask.getIDVector(), sucTask.getIDVector(), 0, 0, 0);
           // dd.setReady(true);
            predTask.addDsuc(dd);
            sucTask.addDpred(dd);
        } else {

        }
    }



    /**
     * pivotのoutタスクの後続タスク（後続クラスタk内タスク）を調べる．
     * そして，後続タスクk'がクラスタkのtopタスクであり，
     * かつタスクk' -> pivotのtopタスクへのパスが存在するかどうかのチェックをする．
     * もしあれば，これらのクラスタはクラスタリング対象となる．
     *
     * @param pivot
     * @return
     */
    public CustomIDSet getCyclicTarget(TaskCluster pivot) {
        CustomIDSet topSet = pivot.getTop_Set();
        CustomIDSet outSet = pivot.getOut_Set();
        //後続タスクたちのセット
        CustomIDSet sucClusterSet = new CustomIDSet();

        CustomIDSet retSet = new CustomIDSet();

        Iterator<Long> outIte = outSet.iterator();
        //pivotのoutタスクたちに対するループ
        while (outIte.hasNext()) {
            Long outTaskID = outIte.next();
            AbstractTask outTask = this.retApl.findTaskByLastID(outTaskID);
            LinkedList<DataDependence> dsucList = outTask.getDsucList();
            Iterator<DataDependence> dsucIte = dsucList.iterator();
            //一つのoutタスクの後続タスクに対するループ
            while (dsucIte.hasNext()) {
                DataDependence dd = dsucIte.next();
                AbstractTask sucTask = this.retApl.findTaskByLastID(dd.getToID().get(1));
                //後続タスクが別クラスタに属していれば
                if (sucTask.getClusterID().longValue() != pivot.getClusterID().longValue()) {
                    sucClusterSet.add(sucTask.getClusterID());
                }
            }
        }
        //後続クラスタたちに対するループ
        Iterator<Long> sucClusterIte = sucClusterSet.iterator();
        while (sucClusterIte.hasNext()) {
            Long sucClusterID = sucClusterIte.next();
            TaskCluster sucCluster = this.retApl.findTaskCluster(sucClusterID);
            //後続クラスタのtop集合を取得する．
            Iterator<Long> topIte = sucCluster.getTop_Set().iterator();
            //後続クラスタのtopタスクたちに対するループ
            while (topIte.hasNext()) {
                Long sucTopID = topIte.next();
                AbstractTask sucTopTask = this.retApl.findTaskByLastID(sucTopID);
                //後続タスクを取得する．
                LinkedList<DataDependence> dsucList = sucTopTask.getDsucList();
                Iterator<DataDependence> dsucIte = dsucList.iterator();
                while (dsucIte.hasNext()) {
                    DataDependence dd = dsucIte.next();
                    //後続クラスタの後続タスクを取得する．
                    if (topSet.contains(dd.getToID().get(1))) {
                        retSet.add(sucClusterID);
                    }
                }

            }
        }
        return retSet;


    }

    /**
     * @param fromCluster
     * @param toCluster
     * @return
     */
    public TaskCluster clusteringClusterLB(TaskCluster fromCluster, TaskCluster toCluster, CustomIDSet targetList) {
        //toClusterの全タスク集合を取得する．
        CustomIDSet IDSet = toCluster.getTaskSet();
        Iterator<Long> taskIte = IDSet.iterator();
        //とりあえずはunderリストから削除する．
        TaskCluster retCluster = null;

        //toCluster内のタスクたちに対するループ
        while (taskIte.hasNext()) {
            Long taskID = taskIte.next();
            //fromClusterへタスクを追加する．
            fromCluster.addTask(taskID);
            //toClusterにあったタスクの所属クラスタの変更
            AbstractTask task = this.retApl.findTaskByLastID(taskID);
            task.setClusterID(fromCluster.getClusterID());
            //this.retApl.updateTask(task);
        }

        //以降は，retAplへの更新処理
        //toTaskの反映
        // this.retApl.updateTask((AbstractTask) toTask.deepCopy());
        //fromClusterのIn/Outを更新
        this.updateInOut(fromCluster, toCluster);

        CustomIDSet topSet = this.getTopList(fromCluster);
        fromCluster.setTop_Set(topSet);
        Iterator<Long> topIte = topSet.iterator();
        fromCluster.getDestCheckedSet().getObjSet().clear();
        while (topIte.hasNext()) {
            Long topID = topIte.next();
            AbstractTask startTask = this.retApl.findTaskByLastID(topID);
            this.updateDestTaskList2(new CustomIDSet(), startTask, fromCluster.getClusterID());
        }

        //そしてretAplへの，fromClusterの反映
        //this.retApl.updateTaskCluster(fromCluster);
        //toClusterの削除
        this.retApl.removeTaskCluster(toCluster.getClusterID());
        //UEXからのtoClusterの削除
        targetList.remove(toCluster.getClusterID());

        //閾値を超えていればクラスタをUEXリストから削除する．
        if (this.isClusterAboveThreshold(fromCluster)) {
            //UEXクラスタリストから削除する
            targetList.remove(fromCluster.getClusterID());
        }
        retCluster = fromCluster;

        return retCluster;

    }

    /**
     * 
     * @param fromCluster
     * @param toCluster
     * @return
     */
    public TaskCluster clusteringProcess(TaskCluster fromCluster, TaskCluster toCluster) {
        //toClusterの全タスク集合を取得する．
        CustomIDSet IDSet = toCluster.getTaskSet();
        Iterator<Long> taskIte = IDSet.iterator();
        //とりあえずはunderリストから削除する．
        TaskCluster retCluster = null;

        //toCluster内のタスクたちに対するループ
        while (taskIte.hasNext()) {
            Long taskID = taskIte.next();
            //fromClusterへタスクを追加する．
            fromCluster.addTask(taskID);
            //toClusterにあったタスクの所属クラスタの変更
            AbstractTask task = this.retApl.findTaskByLastID(taskID);
            task.setClusterID(fromCluster.getClusterID());
            //this.retApl.updateTask(task);
        }

        //以降は，retAplへの更新処理
        //toTaskの反映
        // this.retApl.updateTask((AbstractTask) toTask.deepCopy());
        //fromClusterのIn/Outを更新
        this.updateInOut(fromCluster, toCluster);

        CustomIDSet topSet = this.getTopList(fromCluster);
        fromCluster.setTop_Set(topSet);
        Iterator<Long> topIte = topSet.iterator();
        while (topIte.hasNext()) {
            Long topID = topIte.next();
            AbstractTask startTask = this.retApl.findTaskByLastID(topID);
            this.updateDestTaskList(new CustomIDSet(), startTask, fromCluster.getClusterID());
        }

        //そしてretAplへの，fromClusterの反映
        //this.retApl.updateTaskCluster(fromCluster);
        //toClusterの削除
        this.retApl.removeTaskCluster(toCluster.getClusterID());
        //UEXからのtoClusterの削除
        this.underThresholdClusterList.remove(toCluster.getClusterID());

        //閾値を超えていればクラスタをUEXリストから削除する．
        if (this.isClusterAboveThreshold(fromCluster)) {
            //UEXクラスタリストから削除する
            this.underThresholdClusterList.remove(fromCluster.getClusterID());
        }
        retCluster = fromCluster;

        return retCluster;

    }

    /**
     * DAGに対して必要な情報をセットする．
     */
    protected void prepare() {
        Hashtable<Long, AbstractTask> tasklist = this.retApl.getTaskList();
        Collection<AbstractTask> col = tasklist.values();
        Iterator<AbstractTask> ite = col.iterator();

        long start = System.currentTimeMillis();

        CustomIDSet startSet = new CustomIDSet();
        long retMaxTaskSize = 0;


        //タスククラスタの生成
        //各タスクに対するループ
        while (ite.hasNext()) {
            AbstractTask task = ite.next();
            if(retMaxTaskSize <= task.getMaxWeight()){
                retMaxTaskSize = task.getMaxWeight();
            }


            //まずは自分自身をDestへ追加する．
            task.addDestTask(task.getIDVector().get(1));
          //  task.addParentTask(task.getIDVector().get(1));

            //タスクをクラスタへ入れる．
            TaskCluster cluster = new TaskCluster(task.getIDVector().get(1));
            //一つのタスクしか入らないので，当然Linearである．
            cluster.setLinear(true);
            cluster.addTask(task.getIDVector().get(1));

            // タスククラスタに対して，各種情報をセットする．
            /**このときは，各クラスタには一つのみのタスクが入るため，
             * 以下のような処理が明示的に可能である．
             */
            //ここで，top/outタスクは，自分自身のみをセットしておく．
            cluster.setBsucTaskID(task.getIDVector().get(1));


            cluster.addIn_Set(task.getIDVector().get(1));
            cluster.setTopTaskID(task.getIDVector().get(1));
            CustomIDSet topSet = new CustomIDSet();
            topSet.add(task.getIDVector().get(1));
            cluster.setTop_Set(topSet);

            if (!task.getDsucList().isEmpty()) {
                cluster.addOut_Set(task.getIDVector().get(1));
            }

            if (task.getDpredList().isEmpty()) {
                startSet.add(task.getIDVector().get(1));
            }

            task.setClusterID(task.getIDVector().get(1));
            //クラスタ自体をDAGへ反映
            Long clusterID = this.retApl.addTaskCluster(cluster);
            //タスクに対して，そのクラスタIDをセットする．
            //task.setClusterID(clusterID);

            //この時点で，UEXを格納しておく．
            //if (!this.isClusterAboveThreshold(cluster)) {
                this.uexClusterList.add(clusterID);
            //}

        }
        this.maxTaskSize = retMaxTaskSize;

        long endID = this.retApl.getTaskList().size();
        AbstractTask endTask = this.retApl.findTaskByLastID(new Long(endID));
        this.calculateInitialTlevel(endTask, false);
        this.retApl.setStartTaskSet(startSet);
        Iterator<Long> startIte = startSet.iterator();
        while (startIte.hasNext()) {
            Long startID = startIte.next();
            AbstractTask startTask = this.retApl.findTaskByLastID(startID);
            this.calculateInitialBlevel(startTask, false);
        }


        Hashtable<Long, AbstractTask> taskTable = this.retApl.getTaskList();

        Collection<AbstractTask> taskCollection = taskTable.values();

        Iterator<AbstractTask> ite2 = taskCollection.iterator();

        //各タスククラスタへのレベル反映処理
        long start1 = System.currentTimeMillis();
        long cLevel = 0;
        //各タスクに対するループ処理
        while (ite2.hasNext()) {
            AbstractTask task = ite2.next();
            //もしタスククラスタがSTARTノードであれば，この時点でFreeリストへ入れる．
            TaskCluster cls = this.retApl.findTaskCluster(task.getClusterID());
            if (task.getDpredList().isEmpty()) {
                this.freeClusterList.add(cls.getClusterID());
            }
            //初期状態では，クラスタのレベルはタスクのレベルと同じである．
            cls.setTlevel(task.getTlevel());
            cls.setBlevel(task.getBlevel());
            long value = cls.getTlevel() + cls.getBlevel();
            if (cLevel <= value) {
                cLevel = value;
            }

        }
        this.level = cLevel;
        long end1 = System.currentTimeMillis();
        //System.out.println("レベル反映時間: "+(end1-start));

    }


    /**
     * @param task 計算の起点
     * @return そのタスクのtlevel
     */
    public long calculateInitialTlevel(AbstractTask task, boolean recalculate) {
        try {
            LinkedList<DataDependence> DpredList = task.getDpredList();
            int size = DpredList.size();
            long retTlevel = 0;

            //モードが再計算でないならば，すでに値が入っていればそのまま返す．
            if (task.getTlevel() != -1) {
                if (!recalculate) {
                    return task.getTlevel();
                }
            }
            if (DpredList.size() == 0) {
                //this.retApl.findTaskByLastID(task.getIDVector().lastElement()).setTlevel(0);
                task.setTlevel(0);
                this.retApl.getStartTaskSet().add(task.getIDVector().get(1));
                //TaskCluster cls = this.retApl.findTaskCluster(task.getClusterID());
                //cls.setTlevel(0);
                return 0;
            }

            long maxValue = 0;
            for (int i = 0; i < size; i++) {
                DataDependence dd = DpredList.get(i);
                Vector<Long> fromid = dd.getFromID();
                //AbstractTask fromTask = this.findTaskAsTop(fromid);
                AbstractTask fromTask = this.retApl.findTaskByLastID(fromid.get(1));
                long fromTlevel = this.calculateInitialTlevel(fromTask, recalculate) + (this.getInstrunction(fromTask) / this.minSpeed) +
                        this.getNWTime(dd.getFromID().get(1), dd.getToID().get(1), dd.getMaxDataSize(), this.minLink);
                if (maxValue < fromTlevel) {
                    maxValue = fromTlevel;
                    task.setTpred(fromTask.getIDVector());

                }

            }
            if (task.getTpred() == null) {
              //  System.out.println("NULL です");

            }
            //this.retApl.findTaskByLastID(task.getIDVector().lastElement()).setTlevel(maxValue);
            task.setTlevel(maxValue);
            TaskCluster cls = this.retApl.findTaskCluster(task.getClusterID());
            cls.setTlevel(maxValue);
            return maxValue;

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }

    }


    /**
     * @param task 計算の起点
     * @return そのタスクのtlevel
     */
    public long calculatePriorityTlevel(AbstractTask task, boolean recalculate) {
        try {
            //先行タスクを取得する．
            LinkedList<DataDependence> DpredList = task.getDpredList();
            //先行タスクのサイズを取得する．
            int size = DpredList.size();
            long retTlevel = 0;

            //モードが再計算でないならば，すでに値が入っていればそのまま返す．

            if (task.getPriorityTlevel() != -1) {
                 //そのタスクのtlevel値が初期値かつfalseであれば，その値を返す．
                if (!recalculate) {
                    return task.getPriorityTlevel();
                }
            }
            //startタスクであれば，値を0に設定する．
            if (DpredList.size() == 0) {
                //this.retApl.findTaskByLastID(task.getIDVector().lastElement()).setTlevel(0);
                task.setPriorityTlevel(0);
                //this.retApl.getStartTaskSet().add(task.getIDVector().get(1));
                //TaskCluster cls = this.retApl.findTaskCluster(task.getClusterID());
                //cls.setTlevel(0);
                return 0;
            }

            long maxValue = 0;
            //先行タスクたちに対するループ
            for (int i = 0; i < size; i++) {
                DataDependence dd = DpredList.get(i);
                Vector<Long> fromid = dd.getFromID();
               // AbstractTask fromTask = this.findTaskAsTop(fromid);
                AbstractTask fromTask = this.retApl.findTaskByLastID(fromid.get(1));
                //先行タスクに対して，再帰的に呼び出す．
                long fromTlevel = this.calculatePriorityTlevel(fromTask, recalculate) + (this.getInstrunction(fromTask) / this.minSpeed) +
                        this.getNWTime(dd.getFromID().get(1), dd.getToID().get(1), dd.getMaxDataSize(), this.minLink);
                if (maxValue < fromTlevel) {
                    maxValue = fromTlevel;
                    //task.setTpred(fromTask.getIDVector());

                }

            }

            //this.retApl.findTaskByLastID(task.getIDVector().lastElement()).setTlevel(maxValue);
            task.setPriorityTlevel(maxValue);
            //TaskCluster cls = this.retApl.findTaskCluster(task.getClusterID());
            //cls.setTlevel(maxValue);
            return maxValue;

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }

    }

    public boolean isClusterOnlyOneTop(TaskCluster cluster) {
        CustomIDSet inSet = cluster.getIn_Set();
        Iterator<Long> inIte = inSet.iterator();
        long cnt = 0;
        while (inIte.hasNext()) {
            Long tid = inIte.next();
            AbstractTask task = this.retApl.findTaskByLastID(tid);
            LinkedList<DataDependence> dpredList = task.getDpredList();
            Iterator<DataDependence> dpredIte = dpredList.iterator();
            boolean isTop = true;
            if (dpredList.isEmpty()) {
                isTop = true;
            }
            while (dpredIte.hasNext()) {
                DataDependence dd = dpredIte.next();
                AbstractTask predTask = this.retApl.findTaskByLastID(dd.getFromID().get(1));
                if (cluster.getClusterID().longValue() != predTask.getClusterID().longValue()) {
                    isTop = false;
                    break;
                }
            }
            if (isTop) {
                cnt++;
            }
        }
        if (cnt == 1) {
            return true;
        } else {
            return false;
        }
    }


    public CustomIDSet getBottomSet(TaskCluster cluster){
        CustomIDSet retSet = new CustomIDSet();

        CustomIDSet taskSet = cluster.getTaskSet();
        Iterator<Long> taskIte = taskSet.iterator();
        Long cid = cluster.getClusterID();
        //タスクごとのループ
        while(taskIte.hasNext()){
            boolean isBottom = true;
            Long tid = taskIte.next();
            AbstractTask task = this.retApl.findTaskByLastID(tid);
            LinkedList<DataDependence> dsucList = task.getDsucList();
            Iterator<DataDependence> dsucIte = dsucList.iterator();

            //1タスクの後続タスクたちに対するループ
            while(dsucIte.hasNext()){
                DataDependence dd = dsucIte.next();
                AbstractTask sucTask = this.retApl.findTaskByLastID(dd.getToID().get(1));
                //後続タスクが同じクラスタに属するものがある時点で，次のループへ移る．
                if(cid.longValue() == sucTask.getClusterID().longValue()){
                    isBottom = false;
                    break;
                }
            }
            if(isBottom){
                retSet.add(task.getIDVector().get(1));
            }
        }
        return retSet;


    }
    /**
     * 後処理を行います．
     * - 各クラスタのtop/bottomタスクセットの更新
     * - 各クラスタのレベル設定
     * -
     */
    public void postProcess() {
        Hashtable<Long, TaskCluster> clusterSet = this.retApl.getTaskClusterList();
        Collection<TaskCluster> clusterCol = clusterSet.values();
        Iterator<TaskCluster> clusterIte = clusterCol.iterator();
        long cnt = 0;
        long count = 0;
        //タスククラスタに対するループ
        //topタスクセットの更新処理
        while (clusterIte.hasNext()) {
            TaskCluster cluster = clusterIte.next();
            int topsize = this.getTopList(cluster).getList().size();
            if (topsize == 1) {
                cnt++;
            }
            if (cluster.isLinear()) {
                count++;
            }
            CustomIDSet topSet = this.getTopList(cluster);

            cluster.setTop_Set(topSet);

            //bottomをセットする．
            CustomIDSet bottomSet = this.getBottomSet(cluster);
            cluster.setBottomSet(bottomSet);

            //サイズをセットする．
            long size = this.getClusterInstruction(cluster);
            cluster.setClusterSize(size);

        }


        Iterator<AbstractTask> taskIte = this.retApl.taskIerator();
        long value = 0;
        long comm = 0;
        long edgeNum = 0;
        while (taskIte.hasNext()) {
            AbstractTask task = taskIte.next();
            value += task.getMaxWeight();
            LinkedList<DataDependence> dsucList = task.getDsucList();
            Iterator<DataDependence> dsucIte = dsucList.iterator();
            edgeNum += dsucList.size();
            while (dsucIte.hasNext()) {
                DataDependence dd = dsucIte.next();
                comm += dd.getMaxDataSize();
            }
        }
        this.retApl.setEdgeNum(edgeNum);
        this.retApl.setEdgeWeight(comm);
        this.retApl.setTaskWeight(value);

        long start = System.currentTimeMillis();

        //後処理で，レベル更新をしない
        if(this.postUpdateLevel == 0){
            //クラスタのレベル更新
            this.calcPostLevel();

            this.processResult();

        }else{

        }


    }

    /**
     *
     */
    public void processResult() {
        Iterator<TaskCluster> cIte = this.retApl.clusterIterator();
        int size = 0;
        int size2 = 0;
        //クラスタ単位のループ
        while (cIte.hasNext()) {
            TaskCluster cluster = cIte.next();
            int value = cluster.getCyclicTopSet().getList().size();
            int value2 = cluster.getTop_Set().getList().size();

            if (size <= value) {
                size = value;
            }

            if (size2 <= value2) {
                size2 = value2;
            }
        }
        long value = 0;
        long val = 0;
        Iterator<TaskCluster> cIte2 = this.retApl.clusterIterator();
        Long  maxClusterID = null;
        while (cIte2.hasNext()) {
            TaskCluster c = cIte2.next();
            long level = c.getTlevel() + c.getBlevel();
            if (value <= level) {
                value = level;
                maxClusterID = c.getClusterID();
            }

            long tmpval = this.calculateSumValue(c.getTaskSet());
            //long tmpval = cSize/this.threshold;
            if(val<=tmpval){
                val = tmpval;
            }

        }

        this.retApl.setWorstLevel(value);
        this.retApl.setMaxClusterID(maxClusterID);

        Iterator<TaskCluster> cIte3 = this.retApl.clusterIterator();
        long linear=0;
        long nonlinear=0;

        while(cIte3.hasNext()){
            TaskCluster c = cIte3.next();
            if(c.isLinear()){
                linear++ ;
            }else{
               nonlinear++;
            }
        }

        this.retApl.setLinearNum(linear);
        this.retApl.setNonLinearNum(nonlinear);
        
        //次に，支配パス上での，linearとnon-linearなクラスタ数の集計を行う．
        //まず，level最大のクラスタを取得する．
        //TaskCluster maxCluster = this.retApl.findTaskCluster(this.retApl.getMaxClusterID());

        //maxClusterを基点に，
        //1. 上方向（TL=0であるクラスタが見つかるまで）
        //2. 下方向（ENDタスクが属するクラスタにい到達するまで）
        //の2処理のlinearチェック処理を行う．
        //まずは，1. 上方向のlinear/non-linear数を設定する．
        /*
        HashMap<String, Long> retMap = new HashMap<String, Long>();
        retMap.put("linear", new Long(0));
        retMap.put("nonlinear", new Long(0));
        HashMap<String, Long> map = this.processTlevel(retMap, maxCluster);

        //次に，2. 下方向のlinear/non-linear数を設定する．
        HashMap<String, Long> retMap2 = new HashMap<String, Long>();
        retMap2.put("linear", new Long(0));
        retMap2.put("nonlinear", new Long(0));
        CustomIDSet tmpSet = new CustomIDSet();
        HashMap<String, Long> map2 = this.processBlevel(retMap2, maxCluster, true, tmpSet);

        long linearNum = map.get("linear").longValue() + map2.get("linear").longValue();
        long nonlinearNum = map.get("nonlinear").longValue()+ map2.get("nonlinear").longValue();
        */
        


        
    }

    /**
     * 
     * @param map
     * @param cluster
     * @param isInitial
     * @return
     */
    public HashMap<String, Long> processBlevel(HashMap<String, Long> map, TaskCluster cluster, boolean isInitial, CustomIDSet tmpSet){
        Iterator<Long> outIte = cluster.getOut_Set().iterator();
        long retBlevel = 0;
        AbstractTask blevelTask = null;
        long linearNum = map.get("linear").longValue();
        long nonlinearNum = map.get("nonlinear").longValue();

        if(isInitial){

        }else{
            //System.out.println(cluster.getClusterID().longValue());
             if(this.isClusterLinear(cluster)){
                linearNum ++ ;
                map.put("linear", new Long(linearNum));
            }else{
                nonlinearNum ++;
                map.put("nonlinear", new Long(nonlinearNum));
            }

        }

        //ENDタスクを含むかどうか
        Long endID = this.retApl.getEndTask().get(1);
        //もしENDクラスタならば，ここでリターンする．
        if(cluster.getTaskSet().contains(endID)){
            tmpSet.add(cluster.getClusterID());
            return map;
        }

        //BsucTaskIDは決まっているので，このタスクの後続タスクのうちで，blevelを支配しているタスクを特定する．
        //そのあと，その支配しているタスクが属するクラスタに対して同様の処理を行う．
        Long bsucID = cluster.getBsucTaskID();
        AbstractTask bsucTask = this.retApl.findTaskByLastID(bsucID);
        Iterator<DataDependence> dsucIte = bsucTask.getDsucList().iterator();

        long  maxBlevel = 0;
        AbstractTask  maxBsuc = null;

        while(dsucIte.hasNext()){
            DataDependence dsuc = dsucIte.next();
            AbstractTask dsucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));
            if(dsucTask.getClusterID().longValue() == cluster.getClusterID().longValue()){
                continue;
            }

            if(tmpSet.contains(dsucTask.getClusterID())){
                continue;
            }
            long tmpValue = dsuc.getMaxDataSize()/this.minLink + dsucTask.getBlevel();
            if(maxBlevel <= tmpValue){
                maxBlevel = tmpValue;
                maxBsuc = dsucTask;
            }
        }
        //後続クラスタを取得する．
        TaskCluster nextCluster = this.retApl.findTaskCluster(maxBsuc.getClusterID());
         tmpSet.add(cluster.getClusterID());
        map = this.processBlevel(map, nextCluster,false, tmpSet);

        return map;

    }


    /**
     *
     * @param cluster
     * @return
     */
    public HashMap<String, Long> processTlevel(HashMap<String, Long> map, TaskCluster cluster){
        Iterator<Long> topIte = cluster.getTop_Set().iterator();
        long retTlevel = 0;
        AbstractTask tlevelTask = null;
        long linearNum = map.get("linear").longValue();
        long nonlinearNum = map.get("nonlinear").longValue();

        if(this.isClusterLinear(cluster)){
            linearNum ++ ;
            map.put("linear", new Long(linearNum));
        }else{
            nonlinearNum ++;
            map.put("nonlinear", new Long(nonlinearNum));
        }

        if(cluster.getTlevel() == 0){
            return map;
        }

//先行クラスタを特定するための処理
        //top集合に対するループ
        while(topIte.hasNext()){
            Long tId = topIte.next();
            AbstractTask topTask = this.retApl.findTaskByLastID(tId);
            long tlevel = topTask.getTlevel() + topTask.getMaxWeight()/this.minSpeed;
            if(retTlevel <= tlevel){
                retTlevel = tlevel;
                tlevelTask = topTask;
            }
        }

        //TL値を支配しているタスクの先行タスクのうちで，tlevel値を支配しているタスクを選択する．
        //そして，そのタスクが属するクラスタのIDを取得する．
        Iterator<DataDependence> dpredIte = tlevelTask.getDpredList().iterator();

        long tmpTlevel = 0;
        AbstractTask pTask = null;
        while(dpredIte.hasNext()){
            DataDependence dd = dpredIte.next();
            AbstractTask dpredTask = this.retApl.findTaskByLastID(dd.getFromID().get(1));
            long tl = dpredTask.getTlevel() + dpredTask.getMaxWeight()/this.minSpeed + dd.getMaxDataSize()/this.minLink;
            if(tmpTlevel <= tl){
                tmpTlevel = tl;
                pTask = dpredTask;
            }
        }
        TaskCluster nextCluster = this.retApl.findTaskCluster(pTask.getClusterID());
        map = this.processTlevel(map, nextCluster);
        return map;

    }

    /**
     *
     * @param cluster
     * @return
     */
    public boolean isClusterLinear(TaskCluster cluster){
        Iterator<Long> taskIte = cluster.getTaskSet().iterator();
        boolean ret = true;
        if(cluster.getTaskSet().getList().size()==1){
            return true;
        }

        //タスク集合に対するループ
        while(taskIte.hasNext()){
            Long id = taskIte.next();
            AbstractTask task = this.retApl.findTaskByLastID(id);
            //当該タスクについて，その先祖タスクの中に，「IDが若い他タスク」が含まれているかどうかのチェック
            //一つでも含まれていないものがあれば，non-linearとなる．
            if(!this.isTaskExists(
                    task.getIDVector().get(1).longValue(),
                    cluster.getTaskSet(),
                    task.getAncestorIDList())){
                ret = false;
                break;

            }

        }
        return ret;
    }


    /**
     * 先祖集合の中に，当該クラスタのほかタスクが入っているかどうかのチェック
     */
    public boolean isTaskExists(long targetTaskID, CustomIDSet taskSet, HashSet<Long> ansSet){
        Iterator<Long> taskIte = taskSet.iterator();
        boolean ret = true;

        //クラスタ内の各タスクに対するループ
        while(taskIte.hasNext()){
            Long id = taskIte.next();
            //linearであるためには，先祖に入っていなければならない．
            if(id.longValue() < targetTaskID){
                if(ansSet.contains(id)){

                }else{
                    //先祖になければ，この時点でループを抜ける
                    ret = false;
                    break;
                }

            }

        }

        return ret;

    }

    /**
     * アルゴリズム終了後の，クラスタのレベル値を計算します．
     */
    public void calcPostLevel() {
        Iterator<TaskCluster> clusterIte = this.retApl.clusterIterator();
        //クラスタのTlevelを計算・設定する．
        this.calcClusterTlevel();
        //クラスタの，cyclictopタスクのtlevelを計算する．

        AbstractTask endTask = this.retApl.findTaskByLastID(new Long(this.retApl.getTaskList().size()));
        Long endClusterID = endTask.getClusterID();
        TaskCluster endCluster = this.retApl.findTaskCluster(endClusterID);

        //startクラスタたちに対するループ
        //Iterator<Long> sClusterIte = startClusterSet.iterator();
        Iterator<Long> startTaskIte = this.retApl.getStartTaskSet().iterator();
        //startタスクに対するループ
        while (startTaskIte.hasNext()) {
            // Long sClusterID = sClusterIte.next();
            //startクラスタに対して，順にタスクのblevelを設定する．
            // TaskCluster sCluster = this.retApl.findTaskCluster(sClusterID);
            Long startTaskID = startTaskIte.next();
            AbstractTask task = this.retApl.findTaskByLastID(startTaskID);
            this.configureTaskBlevel(task);

        }

        this.calcClusterBlevel();
    }

    /**
     * タスククラスタの各タスクの，blevelを設定します．
     */
    public AbstractTask configureTaskBlevel(AbstractTask task) {
        if (this.IDSetBlevel.contains(task.getIDVector().get(1))) {
            return task;
        }

        Long clusterID = task.getClusterID();
        TaskCluster cluster = this.retApl.findTaskCluster(clusterID);

        //移行は，タスクが未チェックのときの処理
        CustomIDSet destSet = task.getDestTaskSet();
        Iterator<Long> destIte = destSet.iterator();
        long taskDestSumValue = this.calculateSumValue(destSet);

        long blevel_in = 0;
        long blevel_out = 0;
        Vector<Long> bsucID = null;

        //まずはblevel_inを取得する．
        //タスクのDestセットに対するループ
        while (destIte.hasNext()) {
            Long destID = destIte.next();
            //自分自身なら，次へ行く．
            if (destID.longValue() == task.getIDVector().get(1).longValue()) {
                continue;
            }
            AbstractTask dTask = this.retApl.findTaskByLastID(destID);
            long dTaskDestSumValue = this.calculateSumValue(dTask.getDestTaskSet());
            //dTaskのblevel値を計算する．
            dTask = this.configureTaskBlevel(dTask);
            //dTaskの値を計算する
            long value = (taskDestSumValue - dTaskDestSumValue) / this.minSpeed + dTask.getBlevel();
            if (blevel_in <= value) {
                blevel_in = value;
            }
        }

        task.setBlevel_in(blevel_in);

        if(cluster == null){
            System.out.println("----NULL!!!!---");
        }
        //次に，タスク自身のblevel_outを取得する．
        if (cluster.getOut_Set().contains(task.getIDVector().get(1))) {
            //タスクが，outタスクであるときのみに計算をする．
            //まずは後続タスクから，他クラスタにあるものを取得する．
            LinkedList<DataDependence> dsucList = task.getDsucList();
            Iterator<DataDependence> dsucIte = dsucList.iterator();
            //後続タスクに対するループ
            while (dsucIte.hasNext()) {
                DataDependence dd = dsucIte.next();
                //後続タスクを取得する．
                AbstractTask dsucTask = this.retApl.findTaskByLastID(dd.getToID().get(1));
                if (dsucTask.getClusterID().longValue() != clusterID.longValue()) {
                    //後続タスクが別クラスタであれば，後続タスクのblevel値を取得する．
                    dsucTask = this.configureTaskBlevel(dsucTask);

                    long value2 = this.getInstrunction(task) / this.minSpeed + dd.getMaxDataSize() / this.minLink + dsucTask.getBlevel_in();
                    if (blevel_out <= value2) {
                        blevel_out = value2;
                        bsucID = dd.getToID();
                    }
                }
            }
            task.setBlevel(blevel_out);
            task.setBsuc(bsucID);
        }

        //最後に，inとoutの大きいほうを，blevel_inにセットする．
        long retBlevel = Math.max(blevel_in, blevel_out);
        task.setBlevel_in(retBlevel);
        //チェック済み集合へ追加する．
        this.IDSetBlevel.add(task.getIDVector().get(1));

        return task;
    }

    /**
     *
     */
    public void calcClusterBlevel() {
        Iterator<TaskCluster> clusterIte = this.retApl.clusterIterator();
        //クラスタに対するループ

        while (clusterIte.hasNext()) {
            TaskCluster cluster = clusterIte.next();
            CustomIDSet outSet = cluster.getOut_Set();
            Iterator<Long> outIte = outSet.iterator();
            long sumValue = this.calculateSumValue(cluster.getTaskSet());
            long blevel_out = 0;
            Long bsucID = null;

            //outセットに対するループ
            while (outIte.hasNext()) {
                Long outTaskID = outIte.next();
                AbstractTask outTask = this.retApl.findTaskByLastID(outTaskID);
                long dSumValue = this.calculateSumValue(outTask.getDestTaskSet());
                //blevel値の計算をする．
                long value = (sumValue - dSumValue) / this.minSpeed + outTask.getBlevel();
                outTask.setSValue((sumValue - dSumValue));
                if (blevel_out <= value) {
                    blevel_out = value;
                    bsucID = outTask.getIDVector().get(1);
                }
            }
            cluster.setBlevel(blevel_out);
            cluster.setBsucTaskID(bsucID);

        }


    }


    /**
     * @param task
     * @return
     */
    public long getTaskBlevel(AbstractTask task) {
        if (this.checkedIDSet2.contains(task.getIDVector().get(1))) {
            return task.getBlevel();
        }
        //以降は，taskが未チェックのときの処理
        //まずは後続タスクを取得する．
        LinkedList<DataDependence> dsucList = task.getDsucList();
        if (dsucList.isEmpty()) {
            long blevel = this.getInstrunction(task) / this.minSpeed;
            task.setBlevel(blevel);
            this.checkedIDSet2.add(task.getIDVector().get(1));
            return blevel;
        }

        long blevel = 0;
        Vector<Long> sucIDVector = null;
        //以降は，未チェックでかつENDではないタスクの処理
        Iterator<DataDependence> dsucIte = dsucList.iterator();
        while (dsucIte.hasNext()) {
            DataDependence dd = dsucIte.next();
            //後続タスクを取得する．
            AbstractTask dsucTask = this.retApl.findTaskByLastID(dd.getToID().get(1));
            //後続タスクのblevelを取得する．
            long value = this.getInstrunction(task) / this.minSpeed + this.getNWTime(task.getIDVector().get(1), dd.getToID().get(1), dd.getMaxDataSize(), this.minLink) +
                    this.getTaskBlevel(dsucTask);
            if (blevel <= value) {
                blevel = value;
                sucIDVector = dd.getToID();
            }
        }
        task.setBlevel(blevel);
        task.setBsuc(sucIDVector);
        this.checkedIDSet2.add(task.getIDVector().get(1));

        return blevel;
    }

    /**
     * タスクのTlevel計算処理です．
     */
    public void calcTaskBlevel() {
        CustomIDSet startIDSet = this.retApl.getStartTaskSet();
        Iterator<Long> startTaskIte = startIDSet.iterator();
        //CustomIDSet checkedIDSet = new CustomIDSet();

        //startタスクに対する処理
        while (startTaskIte.hasNext()) {
            Long startTaskID = startTaskIte.next();
            //startタスクを取得する．
            AbstractTask startTask = this.retApl.findTaskByLastID(startTaskID);
            //startタスクの後続タスクたちを取得する．
            LinkedList<DataDependence> dsucTaskList = startTask.getDsucList();
            Iterator<DataDependence> dsucTaskIte = dsucTaskList.iterator();
            long blevel = 0;
            Vector<Long> sucIDVector = null;

            //startタスクの後続タスクに対するループ
            while (dsucTaskIte.hasNext()) {
                DataDependence dd = dsucTaskIte.next();
                //後続タスクを取得する．
                AbstractTask dsucTask = this.retApl.findTaskByLastID(dd.getToID().get(1));
                //後続タスクのblevelを取得する．
                long value = this.getInstrunction(startTask) / this.minSpeed + this.getNWTime(startTaskID, dd.getToID().get(1), dd.getMaxDataSize(), this.minLink) +
                        this.getTaskBlevel(dsucTask);
                if (blevel <= value) {
                    blevel = value;
                    sucIDVector = dd.getToID();
                }
            }
            //一つのstartタスクのblevelが確定する．
            startTask.setBlevel(blevel);
            startTask.setBsuc(sucIDVector);
        }
    }


    /**
     * クラスタのTlevel値をセットします．
     * クラスタのTlevel = Max{topタスクのtlevel}であるので，
     * - 下からトレースして，当該クラスタのtopタスクたちを見る．
     * - そして，各topタスクに対して，その先行クラスタへと順次tlevelを更新していく．
     */
    public void calcClusterTlevel() {
        //ENDタスクを取得する．
        AbstractTask endTask = this.retApl.findTaskByLastID(new Long(this.retApl.getTaskList().size()));
        Long endClusterID = endTask.getClusterID();
        TaskCluster endCluster = this.retApl.findTaskCluster(endClusterID);
        // this.setCyclicTopTask(endCluster, new CustomIDSet());
        //taskSetを初期化する．
        // this.IDSet = new CustomIDSet();
        //ENDクラスタを始点として，クラスタのTlevelを設定していく．

        this.configureTlevelofCluster(endCluster, new CustomIDSet(), false);


        long totalSum = this.getClusterInstruction(endCluster);
        long destValue = this.calculateSumValue(endTask.getDestTaskSet());

        long tlevel = 0;
        if(this.isHetero()){
            CPU cpu = endCluster.getCPU();
            tlevel =   endCluster.getTlevel() + (totalSum - destValue) / cpu.getSpeed();
        }else{
           //ENDタスクのtlevelを決める．
           tlevel = endCluster.getTlevel() + (totalSum - destValue) / this.minSpeed;
        }

        endTask.setTlevel(tlevel);

    }

    /**
     *
     */
    public void processCyclicClustering() {
        Collection<CyclicMap> col = this.cyclicLIst.values();
        Iterator<CyclicMap> mapIte = col.iterator();

        while (mapIte.hasNext()) {
            //マップをを取り出す．
            CyclicMap map = mapIte.next();

            Long toTaskID = map.getToTaskID();
            //toTaskを取得
            AbstractTask toTask = this.retApl.findTaskByLastID(toTaskID);
            //toTaskが属するクラスタを取得
            Long targetID = toTask.getClusterID();
            //targetを取得
            TaskCluster target = this.retApl.findTaskCluster(targetID);

            //toTask内の，サイクリックな先行タスクセットを取得
            CustomIDSet cyclicPredSet = toTask.getCyclicPredTaskSet();
            Iterator<Long> cyclicPredIte = cyclicPredSet.iterator();
            //toTaskのサイクリック先行タスクたちに対するループ
            //while(cyclicPredIte.hasNext()){
            //   Long predTaskID = cyclicPredIte.next();
            Long predTaskID = map.getFromTaskID();
            //先行タスクを取得する　　
            AbstractTask predTask = this.retApl.findTaskByLastID(predTaskID);
            //pivotを取得する．
            if (this.retApl.getTaskClusterList().containsKey(predTask.getClusterID())) {
                TaskCluster pivot = this.retApl.findTaskCluster(predTask.getClusterID());
                if (pivot.getClusterID().longValue() != target.getClusterID().longValue()) {
                    long addedValue = map.getDataSize();
                    long newWeight = predTask.getMaxWeight() + addedValue;
                    //predTask.setMaxWeight(newWeight);
                    clusteringCyclicCluster(pivot, target);
                } else {
                    //自分自身とクラスタはしないのでcontinue
                }
            } else {
                //もしpivotが無ければcontinue;
            }
            //  }
        }
        //cyclicSetの残り（つまり，最終的に出来上がったクラスタ）に対して，Top, InOut,  Destを更新する．
        //これは，当該クラスタの全タスクをトレースする必要がある．

        Iterator<Long> cIte = this.cyclicClusterSet.iterator();
        int cnt = 0;
        CustomIDSet newCyclicSet = new CustomIDSet();
        while (cIte.hasNext()) {
            Long cID = cIte.next();
            if (!this.retApl.getTaskClusterList().containsKey(cID)) {
                continue;
            }
            cnt++;

            TaskCluster pivot = this.retApl.findTaskCluster(cID);

            //fromClusterのIn/Outを更新
            this.updateInOut(pivot, null);

            //各タスクのDestSetの更新
            CustomIDSet topSet = this.getTopList(pivot);
            pivot.setTop_Set(topSet);
            Iterator<Long> topIte = topSet.iterator();
            while (topIte.hasNext()) {
                Long topID = topIte.next();
                AbstractTask startTask = this.retApl.findTaskByLastID(topID);
                this.updateDestTaskList2(new CustomIDSet(), startTask, pivot.getClusterID());
            }
            newCyclicSet.add(cID);

        }
        //サイクリックセットの更新
        this.cyclicClusterSet = newCyclicSet;
        System.out.println("残ったサイクリッククラスタ数：" + cnt);

    }

    /**
     * @param pivot
     * @param target
     */
    public void clusteringCyclicCluster(TaskCluster pivot, TaskCluster target) {
        //toClusterの全タスク集合を取得する．
        CustomIDSet IDSet = target.getTaskSet();
        Iterator<Long> taskIte = IDSet.iterator();
        //とりあえずはunderリストから削除する．
        TaskCluster retCluster = null;

        //toCluster内のタスクたちに対するループ
        while (taskIte.hasNext()) {
            Long taskID = taskIte.next();
            //fromClusterへタスクを追加する．
            pivot.addTask(taskID);
            //toClusterにあったタスクの所属クラスタの変更
            AbstractTask task = this.retApl.findTaskByLastID(taskID);
            task.setClusterID(pivot.getClusterID());

        }

        //そしてretAplへの，fromClusterの反映
        //this.retApl.updateTaskCluster(fromCluster);
        //toClusterの削除
        this.retApl.removeTaskCluster(target.getClusterID());


    }


    /**
     * 定められたデータ転送速度上において，タスク間のデータ転送時間を計算します．
     *
     * @param fromTaskID
     * @param toTaskID
     * @param link       2つのマシン間のデータ転送速度
     * @return
     */
    public long getNWTime(Long fromTaskID, Long toTaskID, long data, long link) {
        AbstractTask fromTask = this.retApl.findTaskByLastID(fromTaskID);
        AbstractTask toTask = this.retApl.findTaskByLastID(toTaskID);
        TaskCluster fromCluster = this.retApl.findTaskCluster(fromTask.getClusterID());
        TaskCluster toCluster = this.retApl.findTaskCluster(toTask.getClusterID());

        //もし双方のタスクが同じクラスタに属していれば，データ転送時間は0となる．
        if (fromTask.getClusterID().longValue() == toTask.getClusterID().longValue()) {
            return 0;
        } else if(fromCluster.getCPU().getMachineID() == toCluster.getCPU().getMachineID()) {
            return 0;
        }else {
            return this.setupTime+data / link;
        }

    }

    public long getNWTime(Environment env, Long fromTaskID, Long toTaskID, long data, long link) {
        AbstractTask fromTask = this.retApl.findTaskByLastID(fromTaskID);
        AbstractTask toTask = this.retApl.findTaskByLastID(toTaskID);

        //もし双方のタスクが同じクラスタに属していれば，データ転送時間は0となる．
        if (fromTask.getClusterID().longValue() == toTask.getClusterID().longValue()) {
            return 0;
        } else {
            if(this.isHetero()){
                TaskCluster fromCluster = this.retApl.findTaskCluster(fromTask.getClusterID());
                TaskCluster toCluster = this.retApl.findTaskCluster(toTask.getClusterID());
                return env.getSetupTime()+data / env.getNWLink(fromCluster.getCPU().getCpuID(), toCluster.getCPU().getCpuID());
            }else{
                return env.getSetupTime()+data / link;
            }

        }

    }



    /**
     * ホップを考慮して，データ転送時間を算出します．
     * @param fromTaskID
     * @param toTaskID
     * @param data
     * @param link
     * @return
     */
    public long getNWTimeHop(Long fromTaskID, Long toTaskID, long data, long link) {
        AbstractTask fromTask = this.retApl.findTaskByLastID(fromTaskID);
        AbstractTask toTask = this.retApl.findTaskByLastID(toTaskID);

        //もし双方のタスクが同じクラスタに属していれば，データ転送時間は0となる．
        if (fromTask.getClusterID().longValue() == toTask.getClusterID().longValue()) {
            return 0;
        } else {
            return this.setupTime+data / link;
        }
    }



    /**
     * タスクのtlevelを取得・設定します．クラスタのtlvelが設定された後に呼び出されます．
     * 当該クラスタのTLEVELは既に判明している場合は当該タスクのtlevelを計算し，そうでない場合は
     * configureTlevelOfClusterメソッドをCallします．
     *
     * @param cluster
     * @param task
     * @return
     */
    public long calcTaskTlevel(TaskCluster cluster, AbstractTask task, CustomIDSet tmpClusterSet, boolean isSecond) {
        if (IDSet.contains(task.getIDVector().get(1))) {
            return task.getTlevel();
        } else {
            if (this.checkedClusterSet.contains(cluster.getClusterID())) {
                //既にチェック済み(TLEVELが分かっている)クラスタであれば，tlevelの計算をする．
                //ここでは，クラスタのTLEVELが計算済み∧当該タスクのtlevelがまだという場合
                if (isSecond) {
                    return 0;


                } else {
                    long totalEsecTime = this.getClusterInstruction(cluster) / this.minSpeed;
                    //CustomIDSet destSet = this.getDestTaskList(new CustomIDSet(),task,task.getClusterID());
                    CustomIDSet destSet = task.getDestTaskSet();
                    //もし先行タスクが同クラスタであれば、同クラスタ用のTlevel計算処理に入る
                    long destValue = this.calculateSumValue(destSet);
                    long value = cluster.getTlevel() + totalEsecTime - (destValue / this.minSpeed);
                    task.setTlevel(value);
                    IDSet.add(task.getIDVector().get(1));
                    return value;

                }

            } else {
                tmpClusterSet.add(cluster.getClusterID());
                if (isSecond) {
                    return 0;

                } else {
                    //当該クラスタのtlevelが未決定であるから，計算処理をする．
                    CyclicBean bean = this.configureTlevelofCluster(cluster, tmpClusterSet, isSecond);
                    long cTlevel = bean.getTlevel();
                    //cyclicなtopタスクであれば，tlevel値を計算しなおす．

                    //既にチェック済みクラスタであれば，tlevelの計算をする．
                    //まずは，当該クラスタでの開始時刻の最悪値の計算．
                    long totalEsecTime = this.getClusterInstruction(cluster) / this.minSpeed;
                    //CustomIDSet destSet = this.getDestTaskList(new CustomIDSet(),task,task.getClusterID());
                    CustomIDSet destSet = task.getDestTaskSet();
                    //もし先行タスクが同クラスタであれば、同クラスタ用のTlevel計算処理に入る
                    long destValue = this.calculateSumValue(destSet);
                    long value = cTlevel + totalEsecTime - (destValue / this.minSpeed);
                    task.setTlevel(value);
                    IDSet.add(task.getIDVector().get(1));
                    return value;

                }
            }

        }


    }

    /**
     * @param set
     * @return
     */
    public long calculateSumValue(CustomIDSet set) {
        LinkedList<Long> list = set.getList();
        Iterator<Long> ite = list.iterator();
        long value = 0;

        while (ite.hasNext()) {
            Long id = ite.next();
            AbstractTask task = this.retApl.findTaskByLastID(id);
            value += this.getInstrunction(task);

        }

        return value;

    }

    /**
     * 当該クラスタが，STARTクラスタかどうかを判断します．
     * 具体的には、
     * - INタスク集合が一つであること
     * - Topタスクが，Startタスク(先行タスクがない）であること
     * のいずれも満たせば，STARTクラスタと判断されます．
     *
     * @param cluster
     * @return
     */
    public boolean isStartCluster(TaskCluster cluster) {
        //まずはINタスク集合を取得します．
        CustomIDSet inSet = cluster.getIn_Set();
        //IN集合が一つかどうか．
        //一つでなければ，この時点でSTARTクラスタではないと判断される
        /*  if(inSet.getList().size() != 1){
              return false;
          }
          */

        //以降は，Inタスクが一つであることで初めて動く処理．

        //topタスクを取得する．
        AbstractTask topTask = this.retApl.findTaskByLastID(cluster.getTopTaskID());

        //TopタスクがSTARTタスクかどうか？
        if (!topTask.getDpredList().isEmpty()) {
            return false;
        }

        //ここまで到達できればtrueを返す．
        return true;

    }


    /**
     * クラスタのTlevel値を計算する．これは，当該クラスタの各Topタスクにおける，先行タスクの
     * tlevel値が分かっていないと計算できない．よって，先行タスクが属するクラスタ（つまり先行クラスタ）が
     * 未チェックであれば，再帰的にこのメソッドがCallされることになる．
     *
     * @param cluster
     * @param tmpClusterSet
     * @param isSecond
     * @return
     */
    public CyclicBean configureTlevelofCluster(
            TaskCluster cluster,
            CustomIDSet tmpClusterSet,
            boolean isSecond) {

        CustomIDSet topSet = cluster.getTop_Set();
        Iterator<Long> topIte = topSet.iterator();

        //以降は，未チェッククラスタのTlevel値を取得する．
        long tmpTlevel = 0;
        Long topTaskID = null;

        CyclicBean bean = new CyclicBean();
        tmpClusterSet.add(cluster.getClusterID());
        //Top候補に対するループ
        while (topIte.hasNext()) {
            Long topID = topIte.next();
            //Long topID = cluster.getTopTaskID();

            //topタスクを取得する．
            AbstractTask topTask = this.retApl.findTaskByLastID(topID);

            //topタスクの先行タスクたちを取得する．
            LinkedList<DataDependence> dpredList = topTask.getDpredList();
            Iterator<DataDependence> dpredIte = dpredList.iterator();

            //一つのtop候補のtlevel値
            Vector<Long> tpredID = null;
            long tmpTlevel2 = 0;

            if (dpredList.isEmpty()) {
                tmpTlevel = 0;
                tmpTlevel2 = 0;
                tpredID = null;
            }

            //topタスクの先行タスクたちに対するループ
            while (dpredIte.hasNext()) {
                DataDependence dd = dpredIte.next();
                Long dpredID = dd.getFromID().get(1);
                AbstractTask dpredTask = this.retApl.findTaskByLastID(dpredID);

                long cTlevel = 0;
                TaskCluster predCluster = this.retApl.findTaskCluster(dpredTask.getClusterID());
 
                //先行クラスタが既に設定済みのクラスタであれば，クラスタのtlevelをそのままリターン
                if (this.checkedClusterSet.contains(predCluster.getClusterID())) {
                    cTlevel = predCluster.getTlevel();
                } else {
                    //先行クラスタのtlevelが未設定の場合
                    //戻ってしまった場合はエラー値を返す．
                    if (tmpClusterSet.contains(predCluster.getClusterID())) {
                        bean.setTlevel(-1);
                        bean.getCyclicClusterSet().add(cluster.getClusterID());
                        DataDependence dataDD = topTask.findDDFromDpredList(dpredTask.getIDVector(), topTask.getIDVector());
                        CyclicMap cyclicMap = new CyclicMap(dpredTask.getIDVector().get(1),
                                topTask.getIDVector().get(1),
                                dataDD.getMaxDataSize());
                        long orgWeight = topTask.getMaxWeight();
                        long newValue = dd.getMaxDataSize() + dd.getMaxDataSize();
                        topTask.setMaxWeight(orgWeight + newValue);

                        this.cyclicClusterSet.add(cluster.getClusterID());

                        String key = this.getKey(dpredTask, topTask);
                        this.cyclicLIst.put(key, cyclicMap);

                        topTask.getCyclicPredTaskSet().add(dpredTask.getIDVector().get(1));
                        return bean;
                    }

                    //先行クラスタのtlevel値を取得する．
                    CyclicBean retBean = this.configureTlevelofCluster(predCluster, tmpClusterSet, isSecond);
                    //当該top候補タスクからのトレースでcyclicが生じた場合は，当該topタスクをtopSetから削除する．
                    if (retBean.getTlevel() == -1) {
                        CustomIDSet ctopset = cluster.getCyclicTopSet();
                        ctopset.add(topID);
                        bean.getCyclicClusterSet().getObjSet().addAll(retBean.getCyclicClusterSet().getList());
                        bean.getCyclicClusterSet().add(cluster.getClusterID());
                        cluster.getCyclicClusterSet().getObjSet().addAll(bean.getCyclicClusterSet().getList());
                        this.cyclicClusterSet.add(cluster.getClusterID());
                        DataDependence dataDD = topTask.findDDFromDpredList(dpredTask.getIDVector(), topTask.getIDVector());
                        topTask.getCyclicPredTaskSet().add(dpredTask.getIDVector().get(1));
                        CyclicMap cyclicMap = new CyclicMap(dpredTask.getIDVector().get(1),
                                topTask.getIDVector().get(1),
                                dataDD.getMaxDataSize());
                        long orgWeight = topTask.getMaxWeight();
                        long newValue = dd.getMaxDataSize() + dd.getMaxDataSize();
                        topTask.setMaxWeight(orgWeight + newValue);
                        String key = this.getKey(dpredTask, topTask);
                        this.cyclicLIst.put(key, cyclicMap);
                        continue;
                    }
                }
                //topタスクの先行タスクのtlevelを設定・取得する．
                long taskTlevel = this.calcTaskTlevel(predCluster, dpredTask, tmpClusterSet, isSecond);

                //先行タスクに，チェック済みというマークをつける
                this.IDSet.add(dpredTask.getIDVector().get(1));
                long value = 0;
                //値を計算する．

                   value = taskTlevel + this.getNWTime(dpredID, topID, dd.getMaxDataSize(), this.minLink);


                if (value >= tmpTlevel2) {
                    tmpTlevel2 = value;
                    tpredID = dd.getFromID();
                }
            }
            topTask.setTpred(tpredID);
            topTask.setTlevel(tmpTlevel2);

            //Topタスクたちの中で最大のTlevelのものをクラスタのtlevelとする．
            if (tmpTlevel2 >= tmpTlevel) {
                tmpTlevel = tmpTlevel2;
                topTaskID = topID;
            }
        }

        cluster.setTlevel(tmpTlevel);
        cluster.setTopTaskID(topTaskID);
        this.checkedClusterSet.add(cluster.getClusterID());
        if (topTaskID != null) {
            this.IDSet.add(topTaskID);
        }

        bean.setTlevel(tmpTlevel);

        return bean;
    }

    public String getKey(AbstractTask dpredTask, AbstractTask topTask) {

        StringBuffer buf = new StringBuffer(String.valueOf(dpredTask.getIDVector().get(1).longValue()));
        buf.append(String.valueOf(topTask.getIDVector().get(1).longValue()));
        return buf.toString();
    }

    /**
     * 本当のTlevelの計算処理
     *
     * @param cluster
     */
    public void configureRealTlevelOfCluster(TaskCluster cluster) {
        CustomIDSet topSet = cluster.getTop_Set();
        Iterator<Long> topIte = topSet.iterator();

        /*if(this.checkedClusterSet.contains(cluster.getClusterID())){
            return cluster.getTlevel();
        }*/
        //以降は，未チェッククラスタのTlevel値を取得する．
        long tmpTlevel = 2000000000;
        Long topTaskID = null;

        //Top候補に対するループ
        while (topIte.hasNext()) {
            Long topID = topIte.next();
            if (cluster.getCyclicTopSet().contains(topID)) {
                //System.out.println("test2");
                continue;
            }
            //Long topID = cluster.getTopTaskID();

            //topタスクを取得する．
            AbstractTask topTask = this.retApl.findTaskByLastID(topID);

            //topタスクの先行タスクたちを取得する．
            LinkedList<DataDependence> dpredList = topTask.getDpredList();
            Iterator<DataDependence> dpredIte = dpredList.iterator();

            //一つのtop候補の値
            Vector<Long> tpredID = null;
            long tmpTlevel2 = 0;

            if (dpredList.isEmpty()) {
                tmpTlevel = 0;
                tmpTlevel2 = 0;
                tpredID = null;
            }

            //topタスクの先行タスクたちに対するループ
            while (dpredIte.hasNext()) {
                DataDependence dd = dpredIte.next();
                Long dpredID = dd.getFromID().get(1);
                AbstractTask dpredTask = this.retApl.findTaskByLastID(dpredID);

                long cTlevel = 0;
                TaskCluster predCluster = this.retApl.findTaskCluster(dpredTask.getClusterID());
                long taskTlevel = 0;
                long value = 0;
                //先行クラスタが既に設定済みのクラスタであれば，クラスタのtlevelそのままリターン
                if (this.checkedClusterSet2.contains(predCluster.getClusterID())) {
                    //先行クラスタがチェック済み==タスクのtlevelが設定済み
                    //cTlevel = predCluster.getTlevel();
                    //topタスクの先行タスクのtlevelを設定・取得する．
                    //taskTlevel = this.calcRealTaskTlevel(predCluster, dpredTask);


                } else {
                    //先行クラスタ内の全タスクのtlevelを設定する．
                    this.configureRealTlevelOfCluster(predCluster);

                }

                value = dpredTask.getTlevel() + this.getNWTime(dpredID, topID, dd.getMaxDataSize(), this.minLink);

                if (value >= tmpTlevel2) {
                    tmpTlevel2 = value;
                    tpredID = dd.getFromID();
                }
            }
            topTask.setTpred(tpredID);
            topTask.setTlevel(tmpTlevel2);

            //Topタスクたちの中で最小のTlevelのものをクラスタのtlevelとする．
            if (tmpTlevel2 <= tmpTlevel) {
                tmpTlevel = tmpTlevel2;
                topTaskID = topID;
            }
        }
        cluster.setTlevel(tmpTlevel);
        cluster.setTopTaskID(topTaskID);
        if (topTaskID == null) {
            // System.out.println("test");
            System.out.println("****TOPの数が" + cluster.getTop_Set().getList().size());

        }
        //次に，各topタスクのtlevelを設定する．
        Iterator<Long> topIte2 = topSet.iterator();
        //topタスクたちに対するループ
        while (topIte2.hasNext()) {
            Long tID = topIte2.next();
            if (tID.longValue() == topTaskID.longValue()) {
                continue;
            }
            AbstractTask tTask = this.retApl.findTaskByLastID(tID);
            //value1:
            long inTimeTlevel = tmpTlevel + (this.calculateSumValue(cluster.getTaskSet()) - this.calculateSumValue(tTask.getDestTaskSet())) / this.minSpeed;
            long NWTlevel = tTask.getTlevel();     
            long finalValue = 0;
            boolean isAbove = false;
            if (NWTlevel > inTimeTlevel) {
                finalValue = NWTlevel;
                isAbove = true;

            } else {
                finalValue = inTimeTlevel;
                isAbove = false;
            }
            //新tlevelを設定する．
            tTask.setTlevel(finalValue);
            LinkedList<DataDependence> dsucList = tTask.getDsucList();
            Iterator<DataDependence> dsucIte = dsucList.iterator();
            //tTaskの後続タスクたちに対するループ
            while (dsucIte.hasNext()) {
                DataDependence dd = dsucIte.next();
                AbstractTask dsucTask = this.retApl.findTaskByLastID(dd.getToID().get(1));
                if (dsucTask.getClusterID().longValue() == cluster.getClusterID().longValue()) {
                    //後続タスクたちのtlevelを設定していく．
                    this.configureTlevelOfAllTask(tTask, dsucTask, cluster, isAbove);

                }
            }
        }

        this.checkedClusterSet2.add(cluster.getClusterID());
        //return tmpTlevel;
    }

    /**
     * @param topTask
     * @param dsucTask
     * @param cluster
     * @param isAbove
     */
    public void configureTlevelOfAllTask(AbstractTask topTask, AbstractTask dsucTask, TaskCluster cluster, boolean isAbove) {
        long sumValue = this.calculateSumValue(cluster.getTaskSet());
        long tlevel = 0;

        //dsucTaskのtlevelを設定する．
        if (isAbove) {
            tlevel = topTask.getTlevel() + (this.calculateSumValue(topTask.getDestTaskSet()) -
                    this.calculateSumValue(dsucTask.getDestTaskSet())) / this.minSpeed;
        } else {
            tlevel = cluster.getTlevel() + (sumValue -
                    this.calculateSumValue(dsucTask.getDestTaskSet())) / this.minSpeed;
        }
        if (dsucTask.getTlevel() <= tlevel) {
            dsucTask.setTlevel(tlevel);
        }
        //さらに後続タスクのtlevelを設定する．
        LinkedList<DataDependence> dsucList = dsucTask.getDsucList();
        Iterator<DataDependence> dsucIte = dsucList.iterator();
        //tTaskの後続タスクたちに対するループ
        while (dsucIte.hasNext()) {
            DataDependence dd = dsucIte.next();
            AbstractTask dsucTask2 = this.retApl.findTaskByLastID(dd.getToID().get(1));
            if (dsucTask.getClusterID().longValue() == cluster.getClusterID().longValue()) {
                //後続タスクたちのtlevelを設定していく．
                this.configureTlevelOfAllTask(topTask, dsucTask2, cluster, isAbove);
            }
        }
    }

    /**
     * @param task
     * @param recalculate
     * @return
     */
    public long calculateInitialBlevel(AbstractTask task, boolean recalculate) {
        LinkedList<DataDependence> DsucList = task.getDsucList();
        int size = DsucList.size();
        //もしすでにBlevelの値が入っていれば，そのまま返す．

        if (recalculate) {
        }
        if (task.getBlevel() != -1) {
            if (!recalculate) {
                return task.getBlevel();
            }
        } else {
            // System.out.println("-1です");
        }
        //もし後続タスクがない場合，blevel=自分の命令数となる．
        long instruction = this.getInstrunction(task);
        if (DsucList.size() == 0) {
            task.setBlevel(instruction / this.minSpeed);
            return instruction;
        }

        long maxValue = 0;
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

            long toBlevel = (instruction / this.minSpeed) +
                    this.getNWTime(dd.getFromID().get(1), dd.getToID().get(1), dd.getMaxDataSize(), this.minLink) + this.calculateInitialBlevel(toTask, recalculate);

            if (maxValue <= toBlevel) {
                task.setBsuc(toTask.getIDVector());
                maxValue = toBlevel;
            }

        }
        task.setBlevel(maxValue);
        task.setBlevel_in(maxValue);

        TaskCluster cls = this.retApl.findTaskCluster(task.getClusterID());
        cls.setBlevel(maxValue);
        return maxValue;
    }

    /**
     * @param task
     * @param recalculate
     * @return
     */
    public long calculatePriorityBlevel(AbstractTask task, boolean recalculate) {
        LinkedList<DataDependence> DsucList = task.getDsucList();
        int size = DsucList.size();
        //もしすでにBlevelの値が入っていれば，そのまま返す．

        if (recalculate) {
        }
        if (task.getPriorityBlevel() != -1) {
            if (!recalculate) {
                return task.getPriorityBlevel();
            }
        } else {
            // System.out.println("-1です");
        }
        //もし後続タスクがない場合，blevel=自分の命令数となる．
        long instruction = this.getInstrunction(task);
        if (DsucList.size() == 0) {
            //this.retApl.findTaskByLastID(task.getIDVector().lastElement()).setBlevel(instruction);
            task.setPriorityBlevel(instruction / this.minSpeed);

            return instruction / this.minSpeed;
        }

        long maxValue = 0;
        for (int i = 0; i < size; i++) {
            // DataDependence dd = ddite.next();
            DataDependence dd = DsucList.get(i);
            Vector<Long> toid = dd.getToID();
            //System.out.println("FromID:"+fromid.lastElement().longValue());
            //エラーOverFlow
            //AbstractTask toTask =  this.findTaskAsTop(toid);
            AbstractTask toTask =  this.retApl.findTaskByLastID(toid.get(1));
            long toBlevel = (instruction / this.minSpeed) +
                    this.getNWTime(dd.getFromID().get(1), dd.getToID().get(1), dd.getMaxDataSize(), this.minLink) + this.calculatePriorityBlevel(toTask, recalculate);

            if (maxValue <= toBlevel) {
                maxValue = toBlevel;
            }

        }
        task.setPriorityBlevel(maxValue);

        return maxValue;
    }


    /**
     * 最上位のタスクを探します．指定IDがたとえ2階層以上のものでも，1階層目までしかみません．
     *
     * @param id
     * @return
     */
    public AbstractTask findTaskAsTop(Vector<Long> id) {
        Hashtable<Long, AbstractTask> taskTable = this.retApl.getTaskList();
        Collection<AbstractTask> taskCollection = taskTable.values();

        int tasknum = taskTable.size();
        AbstractTask rettask = null;
        int idx = 0;
        boolean found = false;
        // System.out.println("Numは:"+tasknum);
        Iterator<AbstractTask> ite = taskCollection.iterator();
        //System.out.println("NumEND");
        while (ite.hasNext()) {
            //for (int i = 0; i < tasknum; i++) {
            AbstractTask task = ite.next();
//            AbstractTask task = tasklist.get(i);
            Vector<Long> idlist = task.getIDVector();
            Long currentID = idlist.get(1);

            if (currentID.longValue() == id.get(1).longValue()) {
                //idx = i;
                //found = true;
                // break;
                return task;
            } else {
                continue;
            }
        }

        return null;
        /* if (found) {
             return tasklist.get(idx);
         } else {
             return null;
         }
         */

    }

    /**
     *
     */
    public void printDAG(BBTask retApl) {
        Hashtable<Long, TaskCluster> clusterTable = this.retApl.getTaskClusterList();
        Collection<TaskCluster> clusterCollection = clusterTable.values();

        Iterator<TaskCluster> clsIte = clusterCollection.iterator();

        while (clsIte.hasNext()) {
            TaskCluster cls = clsIte.next();
            System.out.println("****ClusterID: " + cls.getClusterID().longValue() + "***");

            System.out.println("    Cluster_Tlevel: " + (cls.getTlevel()));

            System.out.println("    ClusterLevel: " + (cls.getTlevel() + cls.getBlevel()));
            System.out.println("    SIZE: " + this.getClusterInstruction(cls));
            //System.out.println("     TOP NUM: "+ this.getTopList(cls).getList().size());
            CustomIDSet IDSet = cls.getTaskSet();
            Iterator<Long> tIte = IDSet.iterator();
            while (tIte.hasNext()) {
                Long taskID = tIte.next();
                AbstractTask t = this.retApl.findTaskByLastID(taskID);
                System.out.println("ClusterID: " + t.getClusterID().longValue() + "taskID: " +
                        taskID.longValue() + "Tlevel: " + t.getTlevel() + " Blevel: " + t.getBlevel() + " / Level: " + (t.getTlevel() + t.getBlevel()));
            }
            // System.out.println();
        }

        AbstractTask task = this.retApl.findTaskByLastID(new Long(100));
        Hashtable<Long, AbstractTask> taskTable = this.retApl.getTaskList();
        Collection<AbstractTask> taskCollection = taskTable.values();

        Iterator<AbstractTask> taskIte = taskCollection.iterator();

    }

    /**
     * @param cluster
     * @return
     */
    public int getInEdgeNum(TaskCluster cluster, BBTask retApl) {
        CustomIDSet inSet = cluster.getIn_Set();
        Iterator<Long> inIte = inSet.iterator();
        int ret = 0;
        while (inIte.hasNext()) {
            Long id = inIte.next();
            AbstractTask task = this.retApl.findTaskByLastID(id);
            LinkedList<DataDependence> dpredList = task.getDpredList();
            Iterator<DataDependence> dpredIte = dpredList.iterator();
            while (dpredIte.hasNext()) {
                DataDependence dd = dpredIte.next();
                AbstractTask dpred = retApl.findTaskByLastID(dd.getFromID().get(1));
                if (dpred.getClusterID().longValue() != cluster.getClusterID().longValue()) {
                    //dpredが異なるクラスタに属すれば，カウントアップ
                    ret++;
                }
            }
        }
        return ret;
    }

    /**
     * 指定タスク集合から，Level値がMaxのものを取得する．
     *
     * @param set
     * @return retAplのLevelMaxクラスタへの参照
     */
    public TaskCluster getMaxLevelCluster(CustomIDSet set) {
        Iterator<Long> ite = set.iterator();
        long maxLevel = 0;
        TaskCluster retCls = null;


        while (ite.hasNext()) {
            Long id = ite.next();
            TaskCluster cls = this.retApl.findTaskCluster(id);
            if(cls == null){
                //System.out.println("NULLだよ");
                return null;

            }
            long level = cls.getTlevel() + cls.getBlevel();

            if (maxLevel <= level) {
                maxLevel = level;
                retCls = cls;
            }
        }

        return retCls;
    }

    /**
     * @param set
     * @return
     */
    public TaskCluster getMaxTlevelCluster(CustomIDSet set) {
        Iterator<Long> ite = set.iterator();
        long maxLevel = 0;
        TaskCluster retCls = null;

        while (ite.hasNext()) {
            Long id = ite.next();
            TaskCluster cls = this.retApl.findTaskCluster(id);
            long level = cls.getTlevel();

            if (maxLevel <= level) {
                maxLevel = level;
                retCls = cls;
            }
        }

        return retCls;

    }

    /**
     * クラスタから，サイズが最小の先行クラスタを取得します．
     *
     * @param cluster
     * @return
     */
    public TaskCluster getClusterPred(TaskCluster cluster) {
        CustomIDSet retSet = new CustomIDSet();

        //Outセットを取得する
        CustomIDSet outSet = cluster.getOut_Set();
        Iterator<Long> outIte = outSet.iterator();

        long tmpValue = 1000000;
        TaskCluster retCluster = null;

        //Outタスクに対するループ
        while (outIte.hasNext()) {
            Long taskID = outIte.next();
            AbstractTask outTask = this.retApl.findTaskByLastID(taskID);
            LinkedList<DataDependence> sucList = outTask.getDsucList();
            Iterator<DataDependence> sucIte = sucList.iterator();
            //一つのOutタスクの後続タスクたちに対するループ
            while (sucIte.hasNext()) {
                DataDependence dd = sucIte.next();
                //後続タスクを取得する
                AbstractTask sucTask = this.retApl.findTask(dd.getToID());
                Long clsID = sucTask.getClusterID();
                //もし，その後続タスクが「異なるクラスタ」に属すれば，結果セットに追加する
                if (clsID.longValue() != cluster.getClusterID().longValue()) {
                    retSet.add(clsID);
                }

            }

        }

        Iterator<Long> predIterator = retSet.iterator();
        while (predIterator.hasNext()) {
            Long cID = predIterator.next();
            TaskCluster predCluster = this.retApl.findTaskCluster(cID);
            if (this.getClusterInstruction(predCluster) <= tmpValue) {
                retCluster = predCluster;
            }
        }

        return retCluster;
    }


    /**
     * クラスタから，後続クラスタたちを取得します．
     *
     * @param cluster
     * @return
     */
    public CustomIDSet getClusterSuc(TaskCluster cluster) {
        CustomIDSet retSet = new CustomIDSet();

        //Outセットを取得する
        CustomIDSet outSet = cluster.getOut_Set();
        Iterator<Long> outIte = outSet.iterator();

        //Outタスクに対するループ
        while (outIte.hasNext()) {
            Long taskID = outIte.next();
            AbstractTask outTask = this.retApl.findTaskByLastID(taskID);
            LinkedList<DataDependence> sucList = outTask.getDsucList();
            Iterator<DataDependence> sucIte = sucList.iterator();
            //一つのOutタスクの後続タスクたちに対するループ
            while (sucIte.hasNext()) {
                DataDependence dd = sucIte.next();
                //後続タスクを取得する
                AbstractTask sucTask = this.retApl.findTask(dd.getToID());
                Long clsID = sucTask.getClusterID();
                //もし，その後続タスクが「異なるクラスタ」に属すれば，結果セットに追加する
                if (clsID.longValue() != cluster.getClusterID().longValue()) {
                    retSet.add(clsID);
                }

            }
        }

        return retSet;
    }


    /**
     * @param cluster
     * @return
     */
    public CustomIDSet getTopList(TaskCluster cluster) {
        CustomIDSet inSet = cluster.getIn_Set();
        Iterator<Long> ite = inSet.iterator();
        Long CID = cluster.getClusterID();
        long clusterID = CID.longValue();
        CustomIDSet retSet = new CustomIDSet();

        while (ite.hasNext()) {
            Long id = ite.next();
            AbstractTask t = this.retApl.findTaskByLastID(id);
            LinkedList<DataDependence> dpredList = t.getDpredList();
            Iterator<DataDependence> dpredIte = dpredList.iterator();
            boolean isTop = true;
            //タスクがstartタスクであれば，topタスクIDをセットする．
            if (dpredList.isEmpty()) {
                cluster.setTopTaskID(t.getIDVector().get(1));
                cluster.setTlevel(0);
                retSet.add(t.getIDVector().get(1));
                continue;
            }

            while (dpredIte.hasNext()) {

                DataDependence dd = dpredIte.next();
                AbstractTask dpredTask = this.retApl.findTask(dd.getFromID());
                Long dpredClusterID = dpredTask.getClusterID();
                if (dpredClusterID.longValue() == clusterID) {
                    isTop = false;
                    break;
                    //retSet.add(dpredClusterID);
                }
            }
            if (isTop) {
                retSet.add(t.getIDVector().get(1));

            }

        }

        return retSet;

    }


    /**
     * @param cluster
     * @return
     */
    public CustomIDSet getTopList2(TaskCluster cluster) {
        //全タスク
        CustomIDSet tSet = cluster.getTaskSet();
        Iterator<Long> ite = tSet.iterator();
        Long CID = cluster.getClusterID();
        long clusterID = CID.longValue();
        CustomIDSet retSet = new CustomIDSet();

        while (ite.hasNext()) {
            Long id = ite.next();
            AbstractTask t = this.retApl.findTaskByLastID(id);
            LinkedList<DataDependence> dpredList = t.getDpredList();
            Iterator<DataDependence> dpredIte = dpredList.iterator();
            boolean isTop = true;
            //タスクがstartタスクであれば，topタスクIDをセットする．
            if (dpredList.isEmpty()) {
                cluster.setTopTaskID(t.getIDVector().get(1));
                cluster.setTlevel(0);
                retSet.add(t.getIDVector().get(1));
                continue;
            }

            while (dpredIte.hasNext()) {

                DataDependence dd = dpredIte.next();
                AbstractTask dpredTask = this.retApl.findTask(dd.getFromID());
                Long dpredClusterID = dpredTask.getClusterID();
                if (dpredClusterID.longValue() == clusterID) {
                    isTop = false;
                    break;
                    //retSet.add(dpredClusterID);
                }
            }
            if (isTop) {
                retSet.add(t.getIDVector().get(1));

            }

        }

        return retSet;

    }


    /**
     * クラスタ内のInタスクの入力辺が，すべてCheckedかどうかの判断をします.
     *
     * @return
     */
    public boolean isAllInEdgeChecked(TaskCluster cluster) {
        CustomIDSet inSet = cluster.getIn_Set();
        Iterator<Long> ite = inSet.iterator();
        boolean ret = true;

        while (ite.hasNext()) {
            Long taskID = ite.next();
            AbstractTask inTask = this.retApl.findTaskByLastID(taskID);
            if (!inTask.allDpredIsChecked()) {
                ret = false;
                break;
            } else {
                //タスクのすべての入力辺がチェック済みであれば，そのまま
                //continueする．

            }

        }
        return ret;
    }

    /**
     * クラスタ内のInタスクの入力辺が，すべてCheckedかどうかの判断をします.
     *
     * @return
     */
    public boolean isAllOutEdgeChecked(TaskCluster cluster) {
        CustomIDSet outSet = cluster.getOut_Set();
        Iterator<Long> ite = outSet.iterator();
        boolean ret = true;

        while (ite.hasNext()) {
            Long taskID = ite.next();
            AbstractTask inTask = this.retApl.findTaskByLastID(taskID);
            if (!inTask.allDsucIsChecked()) {
                ret = false;
                break;
            } else {
                //タスクのすべての入力辺がチェック済みであれば，そのまま
                //continueする．

            }

        }
        return ret;
    }

    /**
     * @param cluster
     * @return
     */
    public long getClusterInstruction(TaskCluster cluster) {
        CustomIDSet tset = cluster.getTaskSet();
        long value = 0;
        Iterator<Long> ite = tset.iterator();

        //各タスクについてのループ
        while (ite.hasNext()) {
            Long id = ite.next();
            AbstractTask t = this.retApl.findTaskByLastID(id);
            value += this.getInstrunction(t);

        }
        return value;
    }

    /**
     * @param cluster
     * @return
     */
    public boolean isAvobeThreshold(TaskCluster cluster) {
        CustomIDSet IDSet = cluster.getTaskSet();
        Iterator<Long> ite = IDSet.iterator();
        long value = 0;

        while (ite.hasNext()) {
            Long id = ite.next();
            AbstractTask task = this.retApl.findTaskByLastID(id);
            value += this.getInstrunction(task);
        }
        if (value >= this.threshold) {
            return true;

        } else {
            return false;
        }
    }

    public BBTask process() {
        return null;

    }

    public void println() {

    }

    /**
     * @param pivot
     * @param target
     * @return
     */
    public TaskCluster updateInSet(TaskCluster pivot, TaskCluster target) {
        CustomIDSet IDSet = pivot.getTaskSet();
        Iterator<Long> taskIte = IDSet.iterator();
        CustomIDSet retInSet = new CustomIDSet();
        while (taskIte.hasNext()) {
            Long tid = taskIte.next();
            AbstractTask task = this.retApl.findTaskByLastID(tid);
            if (this.isTaskIn(task, pivot.getClusterID())) {
                retInSet.add(tid);
            }
        }

        //pivotのOutタスク集合を更新する．
        pivot.setIn_Set(retInSet);

        return pivot;


    }

    public TaskCluster updateTopSet(TaskCluster pivot, TaskCluster target) {
        CustomIDSet IDSet = pivot.getTaskSet();
        Iterator<Long> taskIte = IDSet.iterator();
        CustomIDSet retInSet = new CustomIDSet();
        while (taskIte.hasNext()) {
            Long tid = taskIte.next();
            AbstractTask task = this.retApl.findTaskByLastID(tid);

            if (this.isTaskTop(task, pivot.getClusterID())) {
                retInSet.add(tid);
            }
        }

        //pivotのOutタスク集合を更新する．
        pivot.getTop_Set().initializeTaskSet();
        pivot.setTop_Set(retInSet);

        return pivot;


    }




    /**
     * @param task
     * @param clusterID
     * @return
     */
    public boolean isTaskIn(AbstractTask task, Long clusterID) {
        LinkedList<DataDependence> dpredList = task.getDpredList();
        Iterator<DataDependence> dpredIte = dpredList.iterator();
        boolean ret = false;
        if (task.getDpredList().isEmpty()) {
            return true;
        }
        while (dpredIte.hasNext()) {
            DataDependence dd = dpredIte.next();
            AbstractTask dpredTask = this.retApl.findTask(dd.getFromID());
            if (dpredTask.getClusterID().longValue() != clusterID.longValue()) {
                //異なるクラスタIDのタスクが存在する時点でtrueを設定する．
                ret = true;
                break;
            }

        }

        return ret;
    }

    public boolean isTaskTop(AbstractTask task, Long clusterID){
        LinkedList<DataDependence> dpredList = task.getDpredList();
        Iterator<DataDependence> dpredIte = dpredList.iterator();
        TaskCluster pivot = this.retApl.findTaskCluster(clusterID);
        boolean ret = true;
        if (task.getDpredList().isEmpty()) {
            return true;
        }
        while (dpredIte.hasNext()) {
            DataDependence dd = dpredIte.next();
            AbstractTask dpredTask = this.retApl.findTask(dd.getFromID());
           // if (dpredTask.getClusterID().longValue() != clusterID.longValue()) {
            if(!pivot.getTaskSet().contains(dpredTask.getIDVector().get(1))){
                //異なるクラスタIDのタスクが存在する時点でtrueを設定する．
                //ret = true;
                //break;
            }else{
                //先行タスクが同一クラスタがいる時点でfalse
                ret = false;
                break;
            }

        }

        return ret;


    }


    /**
     * @param set
     * @param task
     * @param clusterID
     * @return
     */
    public CustomIDSet updateDestTaskList(CustomIDSet set,
                                          AbstractTask task,
                                          Long clusterID) {
        //もし後続タスク自身が異なるクラスタであれば，ここで終了．
        //補題: なぜなら，それ以降はかならず異なるクラスタとなるはずだから．
        if (task.getClusterID().longValue() != clusterID.longValue()) {
            return set;
        } else {
            //タスク自身が，当該クラスタ内に属しているときの処理

            LinkedList<DataDependence> dsuclist = task.getDsucList();
            Iterator<DataDependence> ite = dsuclist.iterator();
            if (dsuclist.isEmpty()) {
                //後続タスクがなければ,そのままリターン
                return set;
            } else {
                while (ite.hasNext()) {
                    DataDependence dd = ite.next();
                    AbstractTask dsuc = this.retApl.findTaskByLastID(dd.getToID().get(1));
                    //再帰呼び出しにより,後続処理結果のセットをマージする．
                    CustomIDSet retSet = this.updateDestTaskList(new CustomIDSet(), dsuc, clusterID);
                    set.getObjSet().addAll(retSet.getObjSet());

                }
                //タスク自身にセットする．
                task.setDestTaskSet(set);
                task.getDestTaskSet().add(task.getIDVector().get(1));

                //set.add(task.getIDVector().get(1));
                return set;
            }
        }
    }

    public CustomIDSet updateDestTaskList2(CustomIDSet set,
                                           AbstractTask task,
                                           Long clusterID) {
        TaskCluster pivot = this.retApl.findTaskCluster(clusterID);
        CustomIDSet checkedSet = pivot.getDestCheckedSet();
        //既にチェック済みであればそのまま返す．
        if (checkedSet.contains(task.getIDVector().get(1))) {
            return task.getDestTaskSet();
        }

        //もし後続タスク自身が異なるクラスタであれば，ここで終了．
        //補題: なぜなら，それ以降はかならず異なるクラスタとなるはずだから．
        if (task.getClusterID().longValue() != clusterID.longValue()) {

            return set;
        } else {
            //タスク自身が，当該クラスタ内に属しているときの処理
            set.add(task.getIDVector().get(1));
            LinkedList<DataDependence> dsuclist = task.getDsucList();
            Iterator<DataDependence> ite = dsuclist.iterator();
            if (dsuclist.isEmpty()) {

                //後続タスクがなければ,そのままリターン
                return set;
            } else {
                while (ite.hasNext()) {
                    DataDependence dd = ite.next();
                    AbstractTask dsuc = this.retApl.findTaskByLastID(dd.getToID().get(1));
                    //再帰呼び出しにより,後続処理結果のセットをマージする．
                    CustomIDSet retSet = this.updateDestTaskList2(new CustomIDSet(), dsuc, clusterID);
                    set.getObjSet().addAll(retSet.getObjSet());

                }
                //タスク自身にセットする．
                task.setDestTaskSet(set);
                task.getDestTaskSet().add(task.getIDVector().get(1));

                pivot.getDestCheckedSet().add(task.getIDVector().get(1));
                //set.add(task.getIDVector().get(1));
                return set;
            }
        }
    }


    /**
     * Outタスク→Outでなくなるかもしれないというチェック
     * よって，調べるべきはOutタスクのみである．
     *
     * @param pivot
     * @param target
     */
    public TaskCluster updateOutSet(TaskCluster pivot, TaskCluster target) {
        CustomIDSet IDSet = pivot.getTaskSet();
        Iterator<Long> taskIte = IDSet.iterator();
        CustomIDSet retOutSet = new CustomIDSet();
        CustomIDSet newBottomSet = new CustomIDSet();

        while (taskIte.hasNext()) {
            Long taskID = taskIte.next();
            AbstractTask task = this.retApl.findTaskByLastID(taskID);
            if (this.isTaskOut(task, pivot.getClusterID())) {
                retOutSet.add(taskID);
            }

            if(this.isBottomTask(task)){
                newBottomSet.add(task.getIDVector().get(1));
            }

        }

        pivot.setOut_Set(retOutSet);
        pivot.getBottomSet().initializeTaskSet();
        pivot.setBottomSet(newBottomSet);

        return pivot;

    }

    public boolean isBottomTask(AbstractTask task) {
        LinkedList<DataDependence> dsucList = task.getDsucList();
        Iterator<DataDependence> dsucIte = dsucList.iterator();
        TaskCluster pivot = this.retApl.findTaskCluster(task.getClusterID());
        boolean ret = true;
        while (dsucIte.hasNext()) {
            DataDependence dd = dsucIte.next();
            AbstractTask dsucTask = this.retApl.findTaskByLastID(dd.getToID().get(1));
           // if (dsucTask.getClusterID().longValue() == task.getClusterID().longValue()) {
            if(pivot.getTaskSet().contains(dsucTask.getIDVector().get(1))){
                ret = false;
                break;
            }

        }
        return ret;
    }


    public void printFreeClusterList() {
        CustomIDSet clusterSet = this.freeClusterList;
        Iterator<Long> clusterIte = clusterSet.iterator();
        //System.out.println();
      ///  System.out.println("********Freeリスト********");
     /*   while (clusterIte.hasNext()) {
            System.out.println("clusterID: " + clusterIte.next().longValue());
        }
        */
    }


    /**
     * @param uexClusterList
     */
    public void setUexTaskList(CustomIDSet uexClusterList) {
        this.uexClusterList = uexClusterList;
    }


    /**
     * Freeクラスタリストへ，クラスタを追加します．
     * もし予約済みリストに存在すれば，追加しない．
     * 逆に予約済みリストになければ追加するということ．
     *
     * @param clusterID
     * @return
     */
    public boolean addFreeClusterList(Long clusterID) {
        //     TaskCluster cluster = this.retApl.findTaskCluster(clusterID);
        //     System.out.println("FREEになったクラスタのタスク数: "+ cluster.getObjSet().getList().size());

        this.freeClusterList.add(clusterID);
        return true;
        //もし予約済みリストにあれば，何もしない．

    }


    /**
     * @param pivot
     * @param target
     */
    public void updateBlevelInCluster(TaskCluster pivot, TaskCluster target) {
        //targetの複製を取得する．
        TaskCluster targetCopy = (TaskCluster) target.deepCopy();
        TaskCluster pivotCopy = (TaskCluster) pivot.deepCopy();

        //まずはpivot, targetのクラスタを仮想的に行い，レベルの更新を行う．
        CustomIDSet allSet = targetCopy.getTaskSet();
        Iterator<Long> allTaskIte = allSet.iterator();

        pivotCopy = this.updateOutSet(pivotCopy, targetCopy);
        pivotCopy = this.updateInSet(pivotCopy, targetCopy);

    }

    /**
     * タスクがOutタスクであるかどうかの判別です．
     *
     * @param task      チェック対象のタスク
     * @param clusterID
     * @return
     */
    public boolean isTaskOut(AbstractTask task, Long clusterID) {
        LinkedList<DataDependence> dsucList = task.getDsucList();
        Iterator<DataDependence> dsucIte = dsucList.iterator();
        boolean ret = false;
        while (dsucIte.hasNext()) {
            DataDependence dd = dsucIte.next();
            AbstractTask dsucTask = this.retApl.findTaskByLastID(dd.getToID().get(1));
            if (dsucTask.getClusterID().longValue() != clusterID.longValue()) {
                //異なるクラスタIDのタスクが存在する時点でtrueを設定する．
                ret = true;
                break;
            }

        }

        return ret;

    }


    public void printAllDAG() {
        Hashtable<Long, TaskCluster> table = this.retApl.getTaskClusterList();
        Collection<TaskCluster> clsCol = table.values();
        Iterator<TaskCluster> clsIte = clsCol.iterator();

        while (clsIte.hasNext()) {
            TaskCluster clster = clsIte.next();


            CustomIDSet IDSet = clster.getTaskSet();
            Iterator<Long> taskIDIte = IDSet.iterator();
            CustomIDSet outSet = clster.getOut_Set();
            Iterator<Long> outIte = outSet.iterator();
            while (outIte.hasNext()) {
                Long outID = outIte.next();

                AbstractTask outTask = this.retApl.findTaskByLastID(outID);
                System.out.println("OutID: " + outID.longValue() + " blevel: " + outTask.getBlevel() + " bsucID: " + outTask.getBsuc().get(1).longValue());
            }
            //タスクごとのループ
            while (taskIDIte.hasNext()) {

                AbstractTask task = this.retApl.findTaskByLastID(taskIDIte.next());
                System.out.println("    タスクID: " + task.getIDVector().get(1).longValue() + " SIZE: " + this.getInstrunction(task));
                CustomIDSet destSet = task.getDestTaskSet();
                Iterator<Long> destIDIte = destSet.iterator();
                while (destIDIte.hasNext()) {
                    System.out.println("DEST ID:" + destIDIte.next().longValue());
                }
                //System.out.println();
                //先行タスクを取得
                LinkedList<DataDependence> dpredList = task.getDpredList();
                Iterator<DataDependence> dpredIte = dpredList.iterator();
                while (dpredIte.hasNext()) {
                    System.out.println("        [先行]From: " + dpredIte.next().getFromID().get(1).longValue());

                }

                //後続タスクを取得
                LinkedList<DataDependence> dsucList = task.getDsucList();
                Iterator<DataDependence> dsucIte = dsucList.iterator();
                while (dsucIte.hasNext()) {
                    System.out.println("        [後続] To: " + dsucIte.next().getToID().get(1).longValue());

                }
            }
        }
    }


    /**
     * @param task
     * @return
     */
    public long getInstrunction(AbstractTask task) {

        if (task == null) {
            return 0;
        }
        switch (this.mode) {
            //Max
            case 1:
                return task.getMaxWeight();
            case 2:
                return task.getAveWeight();
            case 3:
                return task.getMinWeight();
            default:
                return 0;

        }

    }


}
