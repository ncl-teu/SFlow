package net.gripps.clustering.common.aplmodel;

import net.gripps.clustering.common.Constants;

import java.util.*;
import java.io.FileInputStream;

/**
 * User: Hidehiro Kanemitsu
 * Date: 2009/08/05
 * ガウス・ジョルダン法またはガウス消去法のDAGを生成するためのシングルトンクラスです．
 * DAGの生成・依存関係の定義を行います．
 *
 */
public class GaussianOperator extends AplOperator{

        /**
     *
     */
    private static BBTask apl;

    private TreeSet aplIDSet;


    private long numOfLayer_min;
    private long numOfLayer_max;

    //各タスク内のタスク数
    private long numOfTask_min;
    private long numOfTask_max;

    //最下層における命令数の閾値
    private long threshold;

    //命令数の範囲
    private long inst_max;
    private long inst_min;

    //データ依存辺の本数の範囲
    private long ddedge_min;
    private long ddedge_max;

    //データサイズの範囲
    private long ddedge_sizemin;
    private long ddedge_sizemax;

    //ラベルの種類の数
    private long cdedge_labelmin;
    private long cdedge_labelmax;

    //同一ラベルの出数の数
    private long edge_samemin;
    private long edge_samemax;

    private long loopnummin;
    private long loopnummax;

    private int calcmethod;

    private int LevelNum;

    private int taskunit;

    private int comunit;

    private int rate;

    private int startup;



    /**
     * Singleton Object
     */
    private static GaussianOperator singleton;

    /**
     *
     * @return
     */
    public static GaussianOperator getInstance(){
        if(GaussianOperator.singleton == null){
            GaussianOperator.singleton = new GaussianOperator();
        }

        return GaussianOperator.singleton;
    }

    private GaussianOperator() {
        this.aplIDSet = new TreeSet();

    }

    public int getCalcmethod() {
        return calcmethod;
    }

    public void setCalcmethod(int calcmethod) {
        this.calcmethod = calcmethod;
    }



    public TreeSet getAplIDSet() {
        return aplIDSet;
    }

    public void setAplIDSet(TreeSet aplIDSet) {
        this.aplIDSet = aplIDSet;
    }

    public long getNumOfLayer_min() {
        return numOfLayer_min;
    }

    public void setNumOfLayer_min(long numOfLayer_min) {
        this.numOfLayer_min = numOfLayer_min;
    }

    public long getNumOfLayer_max() {
        return numOfLayer_max;
    }

    public void setNumOfLayer_max(long numOfLayer_max) {
        this.numOfLayer_max = numOfLayer_max;
    }

    public long getNumOfTask_min() {
        return numOfTask_min;
    }

    public void setNumOfTask_min(long numOfTask_min) {
        this.numOfTask_min = numOfTask_min;
    }

    public long getNumOfTask_max() {
        return numOfTask_max;
    }

    public void setNumOfTask_max(long numOfTask_max) {
        this.numOfTask_max = numOfTask_max;
    }

    public long getThreshold() {
        return threshold;
    }

    public void setThreshold(long threshold) {
        this.threshold = threshold;
    }

    public long getInst_max() {
        return inst_max;
    }

    public void setInst_max(long inst_max) {
        this.inst_max = inst_max;
    }

    public long getInst_min() {
        return inst_min;
    }

    public void setInst_min(long inst_min) {
        this.inst_min = inst_min;
    }

    public long getDdedge_min() {
        return ddedge_min;
    }

    public void setDdedge_min(long ddedge_min) {
        this.ddedge_min = ddedge_min;
    }

    public long getDdedge_max() {
        return ddedge_max;
    }

    public void setDdedge_max(long ddedge_max) {
        this.ddedge_max = ddedge_max;
    }

    public long getDdedge_sizemin() {
        return ddedge_sizemin;
    }

    public void setDdedge_sizemin(long ddedge_sizemin) {
        this.ddedge_sizemin = ddedge_sizemin;
    }

    public long getDdedge_sizemax() {
        return ddedge_sizemax;
    }

    public void setDdedge_sizemax(long ddedge_sizemax) {
        this.ddedge_sizemax = ddedge_sizemax;
    }

    public long getCdedge_labelmin() {
        return cdedge_labelmin;
    }

    public void setCdedge_labelmin(long cdedge_labelmin) {
        this.cdedge_labelmin = cdedge_labelmin;
    }

    public long getCdedge_labelmax() {
        return cdedge_labelmax;
    }

    public void setCdedge_labelmax(long cdedge_labelmax) {
        this.cdedge_labelmax = cdedge_labelmax;
    }

    public long getEdge_samemin() {
        return edge_samemin;
    }

    public void setEdge_samemin(long edge_samemin) {
        this.edge_samemin = edge_samemin;
    }

    public long getEdge_samemax() {
        return edge_samemax;
    }

    public void setEdge_samemax(long edge_samemax) {
        this.edge_samemax = edge_samemax;
    }

    public long getLoopnummin() {
        return loopnummin;
    }

    public void setLoopnummin(long loopnummin) {
        this.loopnummin = loopnummin;
    }

    public long getLoopnummax() {
        return loopnummax;
    }

    public void setLoopnummax(long loopnummax) {
        this.loopnummax = loopnummax;
    }

    public int getLevelNum() {
        return LevelNum;
    }

    public void setLevelNum(int levelNum) {
        LevelNum = levelNum;
    }

    public int getTaskunit() {
        return taskunit;
    }

    public void setTaskunit(int taskunit) {
        this.taskunit = taskunit;
    }

    public int getComunit() {
        return comunit;
    }

    public void setComunit(int comunit) {
        this.comunit = comunit;
    }

    public int getRate() {
        return rate;
    }

    public void setRate(int rate) {
        this.rate = rate;
    }

    public int getStartup() {
        return startup;
    }

    public void setStartup(int startup) {
        this.startup = startup;
    }

    /**
     * @param filename
     */
    public void constructTask(String filename) {
        try {
            Properties prop = new Properties();
            //create input stream from file
            prop.load(new FileInputStream(filename));
            //レイヤの範囲
            this.setNumOfLayer_min(Long.valueOf(prop.getProperty("task.NumOfLayer.min")).longValue());
            this.setNumOfLayer_max(Long.valueOf(prop.getProperty("task.NumOfLayer.max")).longValue());

            //各タスク内のタスク数
            this.setNumOfTask_min(Long.valueOf(prop.getProperty("task.NumOfTask.min")).longValue());
            this.setNumOfTask_max(Long.valueOf(prop.getProperty("task.NumOfTask.max")).longValue());

            //最下層における命令数の閾値
            this.setThreshold(Long.valueOf(prop.getProperty("task.instructions.threshold")).longValue());

            //命令数の範囲
            this.setInst_max(Long.valueOf(prop.getProperty("task.instructions.max")).longValue());
            this.setInst_min(Long.valueOf(prop.getProperty("task.instructions.min")).longValue());

            //データ依存辺の本数の範囲
            this.setDdedge_min(Long.valueOf(prop.getProperty("task.ddedge.minnum")).longValue());
            this.setDdedge_max(Long.valueOf(prop.getProperty("task.ddedge.maxnum")).longValue());

            //データサイズの範囲
            this.setDdedge_sizemin(Long.valueOf(prop.getProperty("task.ddedge.size.min")).longValue());
            this.setDdedge_sizemax(Long.valueOf(prop.getProperty("task.ddedge.size.max")).longValue());

            //ラベルの種類の数
            this.setCdedge_labelmin(Long.valueOf(prop.getProperty("task.cdedge.labeltype.min")).longValue());
            this.setCdedge_labelmax(Long.valueOf(prop.getProperty("task.cdedge.labeltype.max")).longValue());

            //同一ラベルの出数の数
            this.setEdge_samemin(Long.valueOf(prop.getProperty("task.cdedge.samelabel.min")).longValue());
            this.setEdge_samemax(Long.valueOf(prop.getProperty("task.cdedge.samelabel.max")).longValue());

            this.setLoopnummin(Long.valueOf(prop.getProperty("task.loopNum.min")).longValue());
            this.setLoopnummax(Long.valueOf(prop.getProperty("task.loopNum.max")).longValue());

            this.setCalcmethod(Integer.valueOf(prop.getProperty("task.weight.calcmethod")).intValue());

            this.setLevelNum(Integer.valueOf(prop.getProperty("task.gj.layer")).intValue());

            this.setTaskunit(Integer.valueOf(prop.getProperty("task.gj.taskunit")).intValue());

            this.setComunit((Integer.valueOf(prop.getProperty("task.gj.comunit")).intValue()));

            //this.setStartup((Integer.valueOf(prop.getProperty("task.gj.startup")).intValue()));
            int startupTimes = Integer.valueOf(prop.getProperty("task.gj.startuptimes")).intValue();
           // this.startup = startupTimes * this.comunit;
            this.startup = startupTimes; 

            this.rate = this.comunit/(this.taskunit*2);

            //最上位レイヤ(第一層)におけるタスク数
            long tasknum = this.generateLongValue(this.getNumOfTask_min(), this.getNumOfTask_max());

            //APLを生成して，シングルトンにセットする．
            BBTask apl = new BBTask(Constants.TYPE_BASIC_BLOCK, 0, 0, 0);
            Vector<Long> id = new Vector<Long>();
            id.add(new Long(1));
            apl.setIDVector(id);
            GaussianOperator.getInstance().setApl(apl);

            //各レベルでの，横方向へのタスク生成を行う
            for(int i=0; i< this.LevelNum ; i++){
                long weight = this.taskunit*(2*(LevelNum - (i+1))+1);

               for(int j=1; j<= this.LevelNum-i;j++){

                    if(weight == 0){
                        System.out.println("タスクが0");
                    }
                   //System.out.println("weight:"+weight);

                    AbstractTask updatedTask = this.buildChildTask(true,weight);
                    //Aplへタスクを追加する．このとき，タスクIDは付与されている．
                    GaussianOperator.getInstance().getApl().addTask(updatedTask);
                    // 0 <= i <= LevelNum-1
                    //レベルiでのタスク数は(LevelNum-i)個
                    //レベル(i+1)での最初のタスク番号は，(i+1)LevelNum - i(i+1)/2 + 1
                    //よって，レベルi(0<=i<=LevelNum)でのタスク番号は，i*LevelNum - i(i-1)/2 + 1
                    // なので，レベルiにおけるタスク番号は，
                    // [i*LevelNum - i(i-1)/2 + 1]から[LevelNum + i(LevelNum-1) - i(i-1)/2]まで
                    // i*LevelNum - i(i-1)/2 + k (k≠1)の出辺は，(i+1)LevelNum - i(i+1)/2 + (k-1)のタスクへ出る．
                    // 1 <= k <=(LevelNum-i)　
                }

            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

      /**
     * @param lowest
     * @return
     *
     */
    public AbstractTask buildChildTask(boolean lowest,long weight) {
       /* long weight = 0;
        if (lowest) {
            weight = this.generateLongValue(this.inst_min, this.inst_max);
        }*/
        AbstractTask newTask = new AbstractTask();
        switch (this.generateTaskType()) {
            //ループであれば、そのタスクリストの要素*N回まわすということ．
            //よって，回数を決めてやる必要がある．
            case Constants.TYPE_LOOP:
                long loopNum = this.generateLongValue(this.loopnummin, this.loopnummax);
                //タスク生成(ここでのweight==一回のループでのweightのことを表す．
                newTask = new LoopTask(Constants.TYPE_LOOP, weight, weight, weight, loopNum);
                break;
            case Constants.TYPE_BASIC_BLOCK:
                //タスク生成
                newTask = new BBTask(Constants.TYPE_BASIC_BLOCK, weight, weight, weight);
                break;
            case Constants.TYPE_FUNCTION_CALL:
                //タスク生成
                newTask = new FunctionTask(Constants.TYPE_FUNCTION_CALL, weight, weight, weight);
                break;
            case Constants.TYPE_CONDITION:
                if(this.cdedge_labelmax==0){
                    //タスク生成
                    newTask = new BBTask(Constants.TYPE_BASIC_BLOCK, weight, weight, weight);
                }else{
                    //タスク生成
                    newTask = new ConditionTask(Constants.TYPE_CONDITION, weight, weight, weight);
                }
                break;
            default:
                //タスク生成
                newTask = new BBTask(Constants.TYPE_BASIC_BLOCK, weight, weight, weight);
                break;
        }

        return newTask;
    }

    /**
     * 1: 上位レベルからデータ依存，制御依存関係を決める
     * 2:
     */
    public BBTask assignDependencyProcess() {
        //関数CALLの場合，子タスクは外部との依存はない．ただし，STARTとENDは違うけど．
        //親タスクがレイヤkの場合: startはレイヤkのほかタスクからの入力を持ち，endはレイヤkへの出力がある．

        //1レイヤ目のタスク要素数を見る。
        BBTask upperTask = GaussianOperator.getInstance().getApl();
        Hashtable<Long, AbstractTask> tmpTaskList = upperTask.getTaskList();

        int tasknum = tmpTaskList.size();

        //START, ENDを決める．
        upperTask.setStartTask(upperTask.getTaskList().get(new Long(1)).getIDVector());
       // AbstractTask end = upperTask.getTaskList().getLast();
        int size = upperTask.getTaskList().size();
        AbstractTask end = upperTask.getTaskList().get(new Long(size));

        //もしENDが条件分岐であれば，BasicBlockに変更する．
        if (end.getType() == Constants.TYPE_CONDITION) {
            //GaussianOperator.getInstance().findTask(end.getIDVector()).setType(Constants.TYPE_BASIC_BLOCK);
            upperTask.findTaskByLastID(end.getIDVector().get(1)).setType(Constants.TYPE_BASIC_BLOCK);
        }

        upperTask.setEndTask(end.getIDVector());
        //START: 入力辺がない
        //END: 出辺がない．

        CustomIDSet startSet = new CustomIDSet();

        // 0 <= i <= LevelNum-1
        //レベルiでのタスク数は(LevelNum-i)個
        //レベル(i+1)での最初のタスク番号は，(i+1)LevelNum - i(i+1)/2 + 1
        //よって，レベルi(0<=i<=LevelNum)でのタスク番号は，i*LevelNum - i(i-1)/2 + 1
        // なので，レベルiにおけるタスク番号は，
        // [i*LevelNum - i(i-1)/2 + 1]から[LevelNum + i(LevelNum-1) - i(i-1)/2]まで
        // i*LevelNum - i(i-1)/2 + k (k≠1)の出辺は，(i+1)LevelNum - i(i+1)/2 + (k-1)のタスクへ出る．
        // 1 <= k <=(LevelNum-i)　


        ///while(aplite.hasNext()){
        //トップレベルにおける依存関係の決定のためのループ
        //for (int i = 1; i <= tasknum; i++) {
        //レベルに対するループ
        for(int i=0;i<this.LevelNum;i++){
            int maxColumn = this.LevelNum -i;
            long datasize = this.startup + this.comunit*(this.LevelNum - (i+1)+1);
            //System.out.println(datasize);
            //列に対するループ
            for(int k=1 ;k <= maxColumn;k++){
                //(i-1)レベルまでのタスク数
                long s_idx = i*this.LevelNum - i*(i-1)/2;
                //当該タスクのID
                long idx =  s_idx + k;
                //当該タスク
                AbstractTask task = upperTask.findTaskByLastID(idx);


                if(datasize == 0){
                    System.out.println("データが0");
                }
                
                //出辺の先となるタスクのIDを特定する
                long d_idx = (i+1)*this.LevelNum - i*(i+1)/2 + (k-1);
                if((k == 1)&&(i != this.LevelNum -1)){
                    //k=1のときの，相手の基点ID
                    long p_idx = (i+1)*this.LevelNum - i*(i+1)/2;

                    //レベルiにおける左端タスクであれば，(i+1)レベルの全タスクへブロードキャストする．
                    for(int r = 1;r <= maxColumn-1;r++){
                        long idx_r = p_idx + r;

                        //後続タスクを取得
                        AbstractTask sucTask = upperTask.findTaskByLastID(idx_r);
                        DataDependence dd = new DataDependence(task.getIDVector(), sucTask.getIDVector(), 0, 0, 0);
                        //long datasize = this.generateLongValue(this.ddedge_sizemin, this.ddedge_sizemax);
                       // System.out.println("data:"+datasize);
                        dd.setMaxDataSize(datasize);
                        dd.setAveDataSize(datasize);
                        dd.setMinDataSize(datasize);
                        if(upperTask.getMaxData()<=dd.getMaxDataSize()){
                            upperTask.setMaxData(dd.getMaxDataSize());
                        }
                        if(upperTask.getMinData()>=dd.getMaxDataSize()){
                            upperTask.setMinData(dd.getMaxDataSize());
                        }
                        //task/sucTaskにそれぞれ依存関係を組み込む．
                        task.addDsuc(dd);
                        sucTask.addDpred(dd);
                    }
                }else if((k != 1)&&(i != this.LevelNum -1)){
                    //直下のタスクへ依存関係を構築する
                    //後続タスクを取得
                    AbstractTask sucTask = upperTask.findTaskByLastID(d_idx);
                    DataDependence dd = new DataDependence(task.getIDVector(), sucTask.getIDVector(), 0, 0, 0);
                    //long datasize = this.generateLongValue(this.ddedge_sizemin, this.ddedge_sizemax);
                    dd.setMaxDataSize(datasize);
                    dd.setAveDataSize(datasize);
                    dd.setMinDataSize(datasize);
                    //task/sucTaskにそれぞれ依存関係を組み込む．
                    if(upperTask.getMaxData()<=dd.getMaxDataSize()){
                        upperTask.setMaxData(dd.getMaxDataSize());
                    }
                    if(upperTask.getMinData()>=dd.getMaxDataSize()){
                        upperTask.setMinData(dd.getMaxDataSize());
                    }
                    task.addDsuc(dd);
                    sucTask.addDpred(dd);

                }else{
                    //ENDタスクとなった場合は何もしない．
                    continue;
                }

            }

        }

        //次にDAG全体に対する各種設定処理
        Iterator<AbstractTask> taskIte = upperTask.taskIerator();
        while(taskIte.hasNext()){
            AbstractTask task = taskIte.next();

            //もしStartタスクであれば，StartSetへ追加しておく．
            if(task.getDpredList().isEmpty()){
                startSet.add(task.getIDVector().get(1));
            }

            //当該タスクがENDタスクであれば，何もしない
            if (this.isIDEqual(task.getIDVector(),
                    GaussianOperator.getInstance().getApl().getEndTask())) {
                continue;
            }
        }

        AbstractTask endTask = GaussianOperator.getInstance().getApl().findTaskByLastID(new Long(tasknum));
        CustomIDSet ansSet = new CustomIDSet();
        ansSet.add(endTask.getIDVector().get(1));
        LinkedList<DataDependence> dpredList = endTask.getDpredList();
        Iterator<DataDependence> dpredIte = dpredList.iterator();
        while(dpredIte.hasNext()){
            DataDependence dd = dpredIte.next();
            AbstractTask dpredTask = GaussianOperator.getInstance().getApl().findTaskByLastID(dd.getFromID().get(1));
            this.updateAncestor(ansSet, dpredTask);
        }
        //全体のPGの命令数を集計する．
        BBTask task = (BBTask)GaussianOperator.getInstance().calculateInstructions(GaussianOperator.getInstance().getApl());
        GaussianOperator.getInstance().setApl(task);

        AplOperator.getInstance().setApl(task);

        return task;

    }

    /**
     * taskの子タスクたちから，命令数を集計する．
     *
     * @param pTask
     * @return
     */
    public AbstractTask calculateInstructions(AbstractTask pTask) {
        return TaskInstructionProcessor.getInstance().calculate(pTask);
    }

     /**
     *
     * @param allSet
     * @param task
     * @return
     */
    public CustomIDSet updateAncestor(CustomIDSet allSet, AbstractTask task){
        //すでに自分がチェック済みであればそのままリターン
        if(allSet.contains(task.getIDVector().get(1))){
            return allSet;
        }

        //以降はまだチェック済みでない場合の処理
        //先行タスクの先祖たちをかき集めてからallSetへ自分を追加し，リターン

        LinkedList<DataDependence> dpredList = task.getDpredList();
        Iterator<DataDependence> dpredIte = dpredList.iterator();

        while(dpredIte.hasNext()){
            DataDependence dd= dpredIte.next();
            AbstractTask dpredTask = GaussianOperator.getInstance().getApl().findTaskByLastID(dd.getFromID().get(1));
            //まずは先行タスクのIDを先祖に追加する．
            task.getAncestorIDList().add(dpredTask.getIDVector().get(1));
            //再帰CALL
            this.updateAncestor(allSet, dpredTask);
            HashSet<Long> newSet = dpredTask.getAncestorIDList();
            task.getAncestorIDList().addAll(newSet);

        }

        //allSetへ自分を追加する．
        allSet.add(task.getIDVector().get(1));

        return allSet;

    }

        /**
     * @param id_a
     * @param id_b
     * @return
     */
    public boolean isIDEqual(Vector<Long> id_a, Vector<Long> id_b) {
        //もしサイズが異なった時点で，アウト
        if (id_a.size() != id_b.size()) {
            return false;
        }
        int size = id_a.size();
        boolean ret = true;
        for (int i = 0; i < size; i++) {
            long a = id_a.get(i).longValue();
            long b = id_b.get(i).longValue();
            if (a != b) {
                ret = false;
                break;
            }
        }
        return ret;
    }

        /**
     * @return
     */
    public int generateTaskType() {
        return Constants.TYPE_LOOP + (int) (Math.random() * (Constants.TYPE_CONDITION + 1));
    }


    /**
     * @param min
     * @param max
     * @return
     */
    public long generateLongValue(long min, long max) {
        return min + (long) (Math.random() * (max - min + 1));
    }



}
