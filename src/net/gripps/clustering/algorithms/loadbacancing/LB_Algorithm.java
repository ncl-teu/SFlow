package net.gripps.clustering.algorithms.loadbacancing;

import net.gripps.clustering.algorithms.AbstractClusteringAlgorithm;
import net.gripps.clustering.common.aplmodel.CustomIDSet;
import net.gripps.clustering.common.aplmodel.*;

import java.util.*;

/**
 * Author: H. Kanemitsu
 * Date: 2008/02/09
 */
public class LB_Algorithm extends AbstractClusteringAlgorithm{

    private long longestPath;



    /**
     *
     */
    //private CustomIDSet freeClusterList;


    /**
     * Lontest Pathに含まれるタスククラスタのリスト
     */
    private CustomIDSet lpIDList;

    private String filename;

    private int taskNum;


    public LB_Algorithm(BBTask task) {
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
    public LB_Algorithm(BBTask task, String file) {
        super(task,file, 3);

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
        int size = this.retApl.getTaskList().size();
        AbstractTask endTask = this.retApl.findTaskByLastID(new Long(size));
        this.retApl.setCpLen(endTask.getTlevel() + endTask.getBlevel());
  /*      Iterator<AbstractTask> taskIte = this.retApl.taskIerator();
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
        */
        this.mainProcess();
       // long end = System.currentTimeMillis();
       // System.out.println(" - クラスタリング処理時間: "+ (end-start) + "msec");
        this.calcLevel();

       // this.printDAG(this.retApl);
       // System.out.println("TIME: "+(end-start));
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
     * ロードバランシングアルゴリズムのメイン処理です．
     * - FreeClusterリストのうちで，最小のサイズのクラスタを取り出す　　
     * - そのクラスタの後続タスクのうちで，最小のサイズのタスクとクラスタする．
     * それだけ．
     */
    public void mainProcess(){

        while(!this.uexClusterList.isEmpty()){
            //まず，UEXから最小のクラスタを取得する．
            TaskCluster checkCluster = this.getMinSizeCluster(this.uexClusterList);
            //後続クラスタたちを取得する．
            //実際にはクラスタIDのリストが入っている
            CustomIDSet sucClusterIDSet = this.getClusterSuc(checkCluster);
            //もしcheckClusterがENDクラスタであれば，先行クラスタをクラスタリングする．
            if(sucClusterIDSet.isEmpty()){
                //this.uexClusterList.remove(checkCluster.getClusterID());
                TaskCluster pivotCluster = this.getClusterPred(checkCluster);
                if(pivotCluster == null){
                    long clusterSize = 1000000000;
                    //TaskCluster newPivot = null;
                    //pivotが取得できなければ，他のクラスタの中から最小のものをpivotとする．
                    Iterator<TaskCluster> remainClusterIte = this.retApl.clusterIterator();
                    while(remainClusterIte.hasNext()){
                        TaskCluster rCluster = remainClusterIte.next();
                        if(rCluster.getClusterID().longValue() == checkCluster.getClusterID().longValue()){
                            continue;
                        }else{
                            long size = this.getClusterInstruction(rCluster);
                            if(size <= clusterSize){
                                clusterSize = size;
                                pivotCluster = rCluster;
                            }
                        }
                    }

                }
                this.clusteringClusterLB(pivotCluster, checkCluster, this.uexClusterList);
                continue;
            }

            //後続クラスタたちのIDを取得する．
            Iterator<Long> sucClsIte =  sucClusterIDSet.iterator();
            long size = 1000000000;
            TaskCluster toCluster = null;
            //サイズが最小の後続クラスタを決定するためのループ
            while(sucClsIte.hasNext()){
                TaskCluster sucCluster = this.retApl.findTaskCluster(sucClsIte.next());
                long value =  this.getClusterInstruction(sucCluster);
                if(value<= size){
                    size =  value;
                    toCluster = sucCluster;
                }
            }
            //そしてクラスタリング処理
            this.clusteringClusterLB(checkCluster,toCluster, this.uexClusterList);
        }

    }

      public boolean isTaskIn(AbstractTask task, Long clusterID){
        LinkedList<DataDependence> dpredList = task.getDpredList();
        Iterator<DataDependence> dpredIte = dpredList.iterator();
        boolean ret = false;
        while(dpredIte.hasNext()){
            DataDependence dd = dpredIte.next();
            AbstractTask dpredTask = this.retApl.findTask(dd.getFromID());
            if(dpredTask.getClusterID().longValue() != clusterID.longValue()){
                //異なるクラスタIDのタスクが存在する時点でtrueを設定する．
                ret = true;
                break;
            }

        }

        return ret;
    }


    /*public void updateOutSet(TaskCluster cluster){
        CustomIDSet set = cluster.getObjSet();
        Long clusterid = cluster.getClusterID();
        //まずはInSetを初期化する．
        cluster.initializeOut_Set();

        Iterator<Long> ite = set.iterator();

        while(ite.hasNext()){
            Long  taskid = ite.next();
            AbstractTask t = this.retApl.findTaskByLastID(taskid);
            LinkedList<DataDependence> dsuc = t.getDsucList();
            Iterator<DataDependence> dsucIte = dsuc.iterator();
            int dsucSize = dsuc.size();
            int cnt = 0;

            while(dsucIte.hasNext()){
                DataDependence dd = dsucIte.next();
                Vector<Long> toIDVector = dd.getToID();
                AbstractTask dsucTask = this.retApl.findTask(toIDVector);
                //もし先行タスクのうちで同じクラスタに属しているものがあればbreakなる。
                //じゃなくて，異なるクラスタに属する後続タスクが見つかった時点でaddしてbreakする．
                if(dsucTask.getClusterID().longValue() != clusterid.longValue()){
                    cluster.addOut_Set(t.getIDVector().get(1));
                    //余計なループを減らすため，ここで抜ける．
                    break;
                }else{
                }
            }
        }
    } */





    /**
     * @return
     */
    /*public long calculateLP() {
        AbstractTask endTask = this.retApl.getTaskList().getLast();
        return endTask.getTlevel() + endTask.getBlevel();

    } */





}
