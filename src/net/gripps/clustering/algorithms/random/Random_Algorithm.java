package net.gripps.clustering.algorithms.random;

import net.gripps.clustering.common.aplmodel.TaskCluster;
import net.gripps.clustering.algorithms.AbstractClusteringAlgorithm;
import net.gripps.clustering.common.aplmodel.*;

import java.util.*;
import java.io.FileInputStream;

/**
 * Author: H. Kanemitsu
 * Date: 2008/02/10
 */
public class Random_Algorithm extends AbstractClusteringAlgorithm{
    private long longestPath;



    /**
     *
     */
    //private CustomIDSet freeClusterList;


    private CustomIDSet exClusterList;

    /**
     *
     */
    private CustomIDSet uexClusterList;

    /**
     * Lontest Pathに含まれるタスククラスタのリスト
     */
    private CustomIDSet lpIDList;

    private String filename;

    private int taskNum;


    public Random_Algorithm(BBTask task) {
        super(task);
    }


    public long getClusterLP(){

        Hashtable<Long, TaskCluster> clusterTable = this.retApl.getTaskClusterList();
        Collection<TaskCluster> clusterCollection = clusterTable.values();

        Iterator<TaskCluster> ite = clusterCollection.iterator();
        long lpvalue = 0;

        while(ite.hasNext()){
            TaskCluster cls = ite.next();
            long value = cls.getTlevel()+cls.getBlevel();
            if(value>=lpvalue){
                lpvalue = value;
            }
        }

        return lpvalue;
    }

    public void printDAG(){
        Hashtable<Long, TaskCluster> clusterTable = this.retApl.getTaskClusterList();
        Collection<TaskCluster> clusterCollection = clusterTable.values();

        Iterator<TaskCluster> clsIte = clusterCollection.iterator();

    /*   while(clsIte.hasNext()){
            TaskCluster cls = clsIte.next();
            System.out.println("****ClusterID: "+cls.getClusterID().longValue()+"***");
         //   System.out.println("    Cluster_Tlevel: "+(cls.getTlevel()));

           // System.out.println("    ClusterLevel: "+(cls.getTlevel()+cls.getBlevel()));
            System.out.println("    SIZE: "+this.getClusterInstruction(cls));
            CustomIDSet IDSet = cls.getObjSet();
            Iterator<Long> tIte =  IDSet.iterator();
            while(tIte.hasNext()){
                Long taskID = tIte.next();
                AbstractTask t = this.retApl.findTaskByLastID(taskID);
                System.out.println("    taskID: "+taskID.longValue()+"Tlevel: "+t.getTlevel()+" Blevel: "+t.getBlevel()+" / Level: "+(t.getTlevel()+t.getBlevel()));
            }
            System.out.println();
        }*/
        AbstractTask task = this.retApl.findTaskByLastID(new Long(100));
        TaskCluster endCluster = this.retApl.findTaskCluster(task.getClusterID());
       System.out.println("クラスタ後のLP(Cluster): "+this.getClusterLP());
        System.out.println("LAST: "+"tlevel: "+(task.getTlevel()+"blevel: "+task.getBlevel()));

        Hashtable<Long, AbstractTask> taskTable = this.retApl.getTaskList();
        Collection<AbstractTask> taskCollection  = taskTable.values();

        Iterator<AbstractTask> taskIte = taskCollection.iterator();
        int org_edgeNum = 0;
        while(taskIte.hasNext()){
            AbstractTask t = taskIte.next();
            org_edgeNum+=t.getDpredList().size();
        }
        System.out.println("クラスタ前の辺の数"+org_edgeNum);
        int edgeNum = 0;
        while(clsIte.hasNext()){
            edgeNum+=this.getInEdgeNum(clsIte.next());
        }

        System.out.println("クラスタ後の辺の数:"+edgeNum);
        //System.out.println();



        /*LinkedList<AbstractTask> taskList = this.retApl.getTaskList();
        Iterator<AbstractTask> taskIte = taskList.iterator();

        while(taskIte.hasNext()){
            AbstractTask t = taskIte.next();
            System.out.println("TaskID:"+t.getIDVector().get(1).longValue()+" ClusterID:"+t.getClusterID().longValue());
        }
        */
    }

    /**
     *
     * @param cluster
     * @return
     */
    public int getInEdgeNum(TaskCluster cluster){
        CustomIDSet inSet = cluster.getIn_Set();
        Iterator<Long> inIte = inSet.iterator();
        int ret = 0;
        while(inIte.hasNext()){
            Long id = inIte.next();
            AbstractTask task = this.retApl.findTaskByLastID(id);
            LinkedList<DataDependence> dpredList = task.getDpredList();
            Iterator<DataDependence> dpredIte = dpredList.iterator();
            while(dpredIte.hasNext()){
                DataDependence dd = dpredIte.next();
                AbstractTask dpred = this.retApl.findTask(dd.getFromID());
                if(dpred.getClusterID().longValue() != cluster.getClusterID().longValue()){
                    //dpredが異なるクラスタに属すれば，カウントアップ
                    ret++;
                }
            }
        }
        return ret;
    }



    /**
     * @param task
     * @param file
     */
    public Random_Algorithm(BBTask task, String file) {
        super(task);
        try {
            Properties prop = new Properties();
            //create input stream from file
            prop.load(new FileInputStream(file));
            this.filename = file;
            this.mode = Integer.valueOf(prop.getProperty("task.weight.calcmethod")).intValue();
            this.threshold = Long.valueOf(prop.getProperty("task.instructions.threshold")).longValue();
                        //最小のマシン速度を返す．
            //this.minSpeed = Environment.getInstance(file).getMinSpeed();
            //最小の転送速度を返す．
            //this.minLink = Environment.getInstance().getMinLink();
            this.minSpeed = Integer.valueOf(prop.getProperty("cpu.speed.min")).intValue();
            this.minLink = Integer.valueOf(prop.getProperty("cpu.link.min")).intValue();
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.taskNum = this.retApl.getTaskList().size();

        /**this.setJoinFreeList(new LBM_TaskSet());
         this.setPjoinFreeList(new LBM_TaskSet());
         this.setForkFreeList(new LBM_TaskSet());
         this.setPforkFreeList(new LBM_TaskSet());
         **/
        this.setUexClusterList(new CustomIDSet());
        //this.setFreeClusterList(new CustomIDSet());
        this.setLpTaskList(new CustomIDSet());
        //this.setCdTaskList(new CustomIDSet());
        this. exClusterList = new CustomIDSet();
        //this.setExTaskList(new CustomIDSet());

        //this.prepare();

    }



    public long getLongestPath() {
        return longestPath;
    }

    public void setLongestPath(long longestPath) {
        this.longestPath = longestPath;
    }


    public CustomIDSet getUexClusterList() {
        return uexClusterList;
    }

    public void setUexClusterList(CustomIDSet uexClusterList) {
        this.uexClusterList = uexClusterList;
    }

    public CustomIDSet getLpTaskList() {
        return lpIDList;
    }

    public void setLpTaskList(CustomIDSet lpIDList) {
        this.lpIDList = lpIDList;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public int getTaskNum() {
        return taskNum;
    }

    public void setTaskNum(int taskNum) {
        this.taskNum = taskNum;
    }


    public BBTask  process() {
        this.prepare();
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
        System.out.println("****LB Algorithm****");
        System.out.println(" **初期状態**");
        System.out.println(" - タスク総数: "+ this.retApl.getTaskList().size());
        System.out.println(" - タスクの重み総和: "+ value);
        System.out.println(" - 一台のPCでの実行時間: "+(this.retApl.getMaxWeight() / this.minSpeed));
        System.out.println(" - 辺の総数: "+ edgeNum);
        System.out.println(" - 辺の重み総和: "+ comm);
        int size = this.retApl.getTaskList().size();
        AbstractTask endTask = this.retApl.findTaskByLastID(new Long(size));
        System.out.println(" - 初期APLのCP長: "+ (endTask.getTlevel() + endTask.getBlevel()));
        System.out.println(" **クラスタリング実行後**");

        long start = System.currentTimeMillis();
        this.mainProcess();
        long end = System.currentTimeMillis();
        System.out.println(" - クラスタリング処理時間: "+ (end-start) + "msec");
        this.calcLevel();

       // this.printDAG(this.retApl);
        System.out.println("TIME: "+(end-start));
         this.postProcess();

        return this.retApl;

    }

    public void calcLevel(){
        Hashtable<Long, AbstractTask> taskTable = this.retApl.getTaskList();
        Collection<AbstractTask> taskCollection  = taskTable.values();

        Iterator<AbstractTask> taskIte = taskCollection.iterator();

        while(taskIte.hasNext()){
            AbstractTask task = taskIte.next();
            Long clusterID = task.getClusterID();
            TaskCluster cluster = this.retApl.findTaskCluster(clusterID);
            //もしSTARTタスクであれば，クラスタのtlevel=0となる．
            if(task.getDpredList().isEmpty()){
                cluster.setTlevel(0);
            }else{
                //STARTタスクではない場合
                if(cluster.getTlevel() == 0){
                    //すでにクラスタのtlevelが0にセットされていれば，何もしない
                }else{
                    //クラスタのtlevelが0ではない場合は，再計算
                    CustomIDSet inSet = cluster.getIn_Set();
                    Iterator<Long> inIte = inSet.iterator();
                    //Inセットタスクに対する，Topタスク取得のためのループ
                    while(inIte.hasNext()){
                        Long taskID = inIte.next();
                        AbstractTask inTask = this.retApl.findTaskByLastID(taskID);
                        LinkedList<DataDependence> dpred = inTask.getDpredList();
                        Iterator<DataDependence> dpredIte = dpred.iterator();
                        boolean isTop = true;
                        long tlevel = 0;
                        while(dpredIte.hasNext()){
                            DataDependence dd = dpredIte.next();
                            AbstractTask dpredTask  = this.retApl.findTask(dd.getFromID());
                            if(dpredTask.getClusterID().longValue() == clusterID.longValue()){
                                isTop = false;
                                break;
                            }else{
                                long value = this.getInstrunction(dpredTask)+dpredTask.getTlevel();
                                if( value >= tlevel){
                                    tlevel = value;
                                }
                            }

                        }
                        if(isTop){
                            //ここにきたときは，すでにクラスタとしてのtlevelは決定している．
                            cluster.setTlevel(tlevel);
                        }
                    }
                }
            }
            //後は，タスクとしてのtlevel計算処理
            //まずはDestリストを取得
            CustomIDSet destSet = this.getDestTaskList(new CustomIDSet(),task,task.getClusterID());
            //System.out.println("DEST:"+ this.calculateSumValue(destSet));
            //もし先行タスクが同クラスタであれば、同クラスタ用のTlevel計算処理に入る
            long destValue = this.calculateSumValue(destSet);
            long sumValue = this.getClusterInstruction(cluster);
            long value = cluster.getTlevel()+sumValue - destValue;
            task.setTlevel(value);

        }

        //次は，クラスタのblevel値の計算
        Hashtable<Long, TaskCluster> clusterTable = this.retApl.getTaskClusterList();
        Collection<TaskCluster> clusterCollection = clusterTable.values();

        Iterator<TaskCluster> clusterIte = clusterCollection.iterator();
        //クラスタごとのループ
        while(clusterIte.hasNext()){
            TaskCluster cluster = clusterIte.next();
            CustomIDSet outSet = cluster.getOut_Set();
            Iterator<Long> outIte = outSet.iterator();
            long blevel = 0;
            long sumValue = this.getClusterInstruction(cluster);
            long clusterBlevel = 0;

            while(outIte.hasNext()){
                Long id = outIte.next();
                AbstractTask task = this.retApl.findTaskByLastID(id);
                CustomIDSet destSet = this.getDestTaskList(new CustomIDSet(),task,task.getClusterID());
                long destSize = this.getTotalInstructionInSet(destSet);
                long value = sumValue - (this.getInstrunction(task)+destSize) + task.getBlevel();
                //クラスタのblevelの更新
                if(value >= clusterBlevel){
                    clusterBlevel = value;
                }
            }
            cluster.setBlevel(clusterBlevel);
        }

    }

    /**
     *
     * @param set
     * @return
     */
    public long getTotalInstructionInSet(CustomIDSet set){
        Iterator<Long> ite = set.iterator();
        long totalInstruction = 0;

        while(ite.hasNext()){
            Long taskID = ite.next();
            AbstractTask task = this.retApl.findTaskByLastID(taskID);
            totalInstruction+=this.getInstrunction(task);
        }

        return totalInstruction;
    }



    /**
     * 指定タスクが属するクラスタ内において,そのタスクの経路上タスクを返します．
     * これらは,SetオブジェクトとしてタスクIDが格納されます．
     * @param task
     * @return
     */
    public CustomIDSet getDestTaskList(CustomIDSet set,
                                                    AbstractTask task,
                                                    Long clusterID){
        //もし後続タスクが異なるクラスタであれば，ここで終了．
        //補題: なぜなら，それ以降はかならず異なるクラスタとなるはずだから．
        if(task.getClusterID().longValue() != clusterID.longValue()){
            return set;
        }else{
            set.add(task.getIDVector().get(1));
            LinkedList<DataDependence> dsuclist = task.getDsucList();
            Iterator<DataDependence> ite = dsuclist.iterator();
            if(dsuclist.isEmpty()){
                //後続タスクがなければ,そのままリターン
                return set;
            }else{
                while(ite.hasNext()){
                    DataDependence dd = ite.next();
                    AbstractTask dsuc = this.retApl.findTask(dd.getToID());
                    //再帰呼び出しにより,後続処理結果のセットをマージする．
                    CustomIDSet retSet = this.getDestTaskList(set,dsuc,clusterID);
                    set.getObjSet().addAll(retSet.getObjSet());

                }
                return set;
            }
        }
    }

    /**
     *
     * @param set
     * @return
     */
    public long calculateSumValue(CustomIDSet set){
        LinkedList<Long> list = set.getList();
        Iterator<Long> ite = list.iterator();
        long value = 0;

        while(ite.hasNext()){
            Long id = ite.next();
            AbstractTask task = this.retApl.findTaskByLastID(id);
            value += this.getInstrunction(task);

        }

        return value;

    }


    /**
     * ランダムアルゴリズムのメイン処理です．
     * - UEXClusterリストのうちで，ランダムに2つのクラスタを取り出す　　
     * - そのクラスタの後続タスクのうちで，最小のサイズのタスクとクラスタする．
     * それだけ．
     */
    public void mainProcess(){
         //from, toのインデックスを取り出す．
        while(!this.uexClusterList.isEmpty()){
            //System.out.println("size:"+this.uexClusterList.getList().size());
            if(this.uexClusterList.getList().size() == 1){
                //もし一個しなけなければ，exClusterから取り出す．
                int targetIDX = (int)((this.exClusterList.getList().size() -1)*Math.random());
                Long targetID = this.exClusterList.getList().get(targetIDX);
                TaskCluster targetCluster = this.retApl.findTaskCluster(targetID);
                int fromIDX =(int) ((this.uexClusterList.getList().size()-1)*Math.random());
                Long fromID = this.uexClusterList.getList().get(fromIDX);
                TaskCluster fromCluster = this.retApl.findTaskCluster(fromID);

                //そしてクラスタリング処理
                this.clusteringCluster(fromCluster,targetCluster);
                break;
            }
            if(this.uexClusterList.getList().size() == 2){
                //もし二個しなければ，そいつらで確定
                int targetIDX = 1;
                Long targetID = this.exClusterList.getList().get(targetIDX);
                TaskCluster targetCluster = this.retApl.findTaskCluster(targetID);
                int fromIDX =0;
                Long fromID = this.uexClusterList.getList().get(fromIDX);
                TaskCluster fromCluster = this.retApl.findTaskCluster(fromID);

                //そしてクラスタリング処理
                this.clusteringCluster(fromCluster,targetCluster);
                break;
            }
            int fromIDX =(int) ((this.uexClusterList.getList().size()-1)*Math.random());
            int toIDX = (int) ((this.uexClusterList.getList().size()-1)*Math.random());
            while(toIDX == fromIDX){
                toIDX = (int) ((this.uexClusterList.getList().size()-1)*Math.random());
            }
            Long fromID = this.uexClusterList.getList().get(fromIDX);
            TaskCluster fromCluster = this.retApl.findTaskCluster(fromID);
            Long toID = this.uexClusterList.getList().get(toIDX);
            TaskCluster toCluster = this.retApl.findTaskCluster(toID);


            //そしてクラスタリング処理
            this.clusteringCluster(fromCluster,toCluster);
        }

    }

    /**
     *
     * @param fromCluster
     * @param toCluster
     * @return
     */
    public TaskCluster clusteringCluster(TaskCluster fromCluster, TaskCluster toCluster){
        //toClusterの全タスク集合を取得する．
        CustomIDSet IDSet = toCluster.getTaskSet();
        Iterator<Long> taskIte = IDSet.iterator();

        //toCluster内のタスクたちに対するループ
        while(taskIte.hasNext()){
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
        //そしてretAplへの，fromClusterの反映
        //this.retApl.updateTaskCluster(fromCluster);
        //toClusterの削除
        this.retApl.removeTaskCluster(toCluster.getClusterID());

        //閾値を超えていればクラスタをUEXリストから削除する．
        if(this.isClusterAboveThreshold(fromCluster)){
            //UEXクラスタリストから削除する
            this.uexClusterList.remove(fromCluster.getClusterID());
            //this.freeClusterList.remove(fromCluster.getClusterID());
            this.exClusterList.add(fromCluster.getClusterID());
        }

        //いずれにしてもUEXからtoClusterを削除する．
        this.uexClusterList.remove(toCluster.getClusterID());
        //this.freeClusterList.remove(toCluster.getClusterID());

        return fromCluster;

    }







    /**
     * クラスタから，後続クラスタたちを取得します．
     * @param cluster
     * @return
     */
    public CustomIDSet getClusterSuc(TaskCluster cluster){
        CustomIDSet retSet = new CustomIDSet();

        //Outセットを取得する
        CustomIDSet outSet = cluster.getOut_Set();
        Iterator<Long> outIte = outSet.iterator();

        //Outタスクに対するループ
        while(outIte.hasNext()){
            Long taskID = outIte.next();
            AbstractTask outTask = this.retApl.findTaskByLastID(taskID);
            LinkedList<DataDependence> sucList = outTask.getDsucList();
            Iterator<DataDependence> sucIte = sucList.iterator();
            //一つのOutタスクの後続タスクたちに対するループ
            while(sucIte.hasNext()){
                DataDependence dd = sucIte.next();
                //後続タスクを取得する
                AbstractTask sucTask = this.retApl.findTask(dd.getToID());
                Long clsID = sucTask.getClusterID();
                //もし，その後続タスクが「異なるクラスタ」に属すれば，結果セットに追加する
                if(clsID.longValue() != cluster.getClusterID().longValue()){
                    retSet.add(clsID);
                }

            }
        }

        return retSet;
    }

    public void updateInOut(TaskCluster cluster, TaskCluster toCluster){
        this.updateInSet(cluster, toCluster);
        this.updateOutSet(cluster, toCluster);
    }



    /**
     *  UEXから，サイズが最小のクラスタを取得する．
     *
     * @return
     */
    public TaskCluster getMinSizeCluster(){
        Iterator<Long> uexIte = uexClusterList.iterator();
        long size = 1000000;
        TaskCluster retCluster = null;

        while(uexIte.hasNext()){
            Long id = uexIte.next();
            TaskCluster  cls = this.retApl.findTaskCluster(id);
            long value = this.getClusterInstruction(cls);
            if(value <= size){
                size = value;
                retCluster = cls;
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


    /**
     * @param cluster
     * @return
     */
    public long getClusterInstruction(TaskCluster cluster) {
        CustomIDSet tset = cluster.getTaskSet();
        long value = 0;
        Iterator<Long> ite = tset.iterator();

        while (ite.hasNext()) {
            Long id = ite.next();
            AbstractTask t = this.retApl.findTaskByLastID(id);
            value += this.getInstrunction(t);

        }
        return value;
    }


    /**
     * @return
     */
   /* public long calculateLP() {
        AbstractTask endTask = this.retApl.getTaskList().getLast();
        return endTask.getTlevel() + endTask.getBlevel();

    }
    */


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
                //TaskCluster cls = this.retApl.findTaskCluster(task.getClusterID());
                //cls.setTlevel(0);
                return 0;
            }

            long maxValue = 0;
            for (int i = 0; i < size; i++) {
                DataDependence dd = DpredList.get(i);
                Vector<Long> fromid = dd.getFromID();
                AbstractTask fromTask = this.findTaskAsTop(fromid);
                long fromTlevel = this.calculateInitialTlevel(fromTask, recalculate) + this.getInstrunction(fromTask);
                if (maxValue < fromTlevel) {
                    maxValue = fromTlevel;
                    task.setTpred(fromTask.getIDVector());

                }

            }
            //this.retApl.findTaskByLastID(task.getIDVector().lastElement()).setTlevel(maxValue);
            task.setTlevel(maxValue);
            //TaskCluster cls = this.retApl.findTaskCluster(task.getClusterID());
            //cls.setTlevel(maxValue);
            return maxValue;

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
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
            //this.retApl.findTaskByLastID(task.getIDVector().lastElement()).setBlevel(instruction);
            task.setBlevel(instruction);
            //TaskCluster cls = this.retApl.findTaskCluster(task.getClusterID());
            //cls.setBlevel(instruction);
            //今のうちに，joinFreeListへ入れる．

            /*if(!recalculate && !this.isAvobeThreshold(task)){
                //this.joinFreeList.add(task.getIDVector().get(1));
            }*/
            return instruction;
        }

        long maxValue = 0;
        for (int i = 0; i < size; i++) {
            // DataDependence dd = ddite.next();
            DataDependence dd = DsucList.get(i);
            Vector<Long> toid = dd.getToID();
            //System.out.println("FromID:"+fromid.lastElement().longValue());
            //エラーOverFlow
            AbstractTask toTask = (AbstractTask) this.findTaskAsTop(toid);
            if (toTask == null) {
                continue;
            }

            long toBlevel = instruction + this.calculateInitialBlevel(toTask, recalculate);
            /*if(recalculate){
                System.out.println("ToTASK:"+toTask.getIDVector().get(1));
                System.out.println();
            }*/
            if (maxValue < toBlevel) {
                task.setBsuc(toTask.getIDVector());
                maxValue = toBlevel;
            }

        }
        //this.retApl.findTaskByLastID(task.getIDVector().lastElement()).setBlevel(maxValue);
        task.setBlevel(maxValue);
        //TaskCluster cls = this.retApl.findTaskCluster(task.getClusterID());
        //cls.setBlevel(maxValue);
        return maxValue;
    }





}
