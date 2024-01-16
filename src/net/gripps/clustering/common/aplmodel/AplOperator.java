package net.gripps.clustering.common.aplmodel;

import net.gripps.clustering.common.Constants;
import net.gripps.clustering.common.aplmodel.CustomIDSet;

import java.util.*;
import java.io.*;

import net.gripps.clustering.tool.Calc;
import org.apache.commons.math.random.RandomDataImpl;

/**
 * Author: Hidehiro Kanemitsu<br>
 * Date: 2007/07/08
 */
public  class AplOperator {
    /**
     *
     */
    protected  static AplOperator operatorSingleton;

    private static GaussianOperator gaussianSingleton;

    /**
     *
     */
    protected  static BBTask apl;

    protected  TreeSet aplIDSet;




    protected long numOfLayer_min;
    protected long numOfLayer_max;

    //各タスク内のタスク数
    protected long numOfTask_min;
    protected long numOfTask_max;

    //最下層における命令数の閾値
    protected long threshold;

    protected Random ran = new Random();

    protected RandomDataImpl rData = new RandomDataImpl();
    protected RandomDataImpl rDataCom = new RandomDataImpl();


    //命令数の範囲
    protected long inst_max;
    protected long inst_min;

    //データ依存辺の本数の範囲
    protected long ddedge_min;
    protected long ddedge_max;

    //データサイズの範囲
    protected long ddedge_sizemin;
    protected long ddedge_sizemax;

    //ラベルの種類の数
    protected long cdedge_labelmin;
    protected long cdedge_labelmax;

    //同一ラベルの出数の数
    protected long edge_samemin;
    protected long edge_samemax;

    protected long loopnummin;
    protected long loopnummax;

    protected int calcmethod;

    protected int distType;

    protected CustomIDSet startSet;

    /**
     * 平均
     */
    protected double dist_mu;

    protected double dist_mu_data;

    /*
     * 標準偏差
     */
    protected double dist_sigma;

    protected static boolean operatorLoaded = false;

    protected double startNumRate;

    protected int depth_alpha;

    protected PriorityQueue<AbstractTask> dQueue;

    protected int depth;

    protected int multipleNum;

    protected double multipleCCRMin;

    protected double multipleCCRMax;

    protected long multipleTasknumMin;

    protected long multipleTasknumMax;

    protected long multiplecurrentidx;

    /**
     * The Factory Method as Singleton
     *
     * @return
     */
    public static AplOperator getInstance() {

        if (AplOperator.operatorSingleton == null) {
            AplOperator.operatorSingleton = new AplOperator();
        }
        return AplOperator.operatorSingleton;
    }


    public double getDist_mu() {
        return dist_mu;
    }

    public void setDist_mu(double dist_mu) {
        this.dist_mu = dist_mu;
    }

    public double getDist_sigma() {
        return dist_sigma;
    }

    public void setDist_sigma(double dist_sigma) {
        this.dist_sigma = dist_sigma;
    }

    /**
     *
     */
    protected  AplOperator() {
        this.aplIDSet = new TreeSet();


    }

    public int getDistType() {
        return distType;
    }

    public void setDistType(int distType) {
        this.distType = distType;
    }


    public double getDist_mu_data() {
        return dist_mu_data;
    }

    public void setDist_mu_data(double dist_mu_data) {
        this.dist_mu_data = dist_mu_data;
    }

    /**
     * @return Basic Block as a whole program
     */
    public BBTask getApl() {
        return AplOperator.apl;
    }

    /**
     * @param task
     */
    public void setApl(BBTask task) {
        AplOperator.apl = task;
    }

    /**
     * タスクを追加する処理です．指定IDは，親タスクのIDから自動生成される
     *
     * @param parentid 親タスクのID
     * @param task     追加したいタスク
     * @return 自動生成されたID
     */
    public AbstractTask addTask(Vector<Long> parentid, AbstractTask task) {
        //親タスクを取り出す．
        Vector<Long> newid = null;
        AbstractTask newtask = new   AbstractTask();
        if (AplOperator.getInstance().findTask(parentid) != null) {
            //もう一度呼ぶ．
            newtask = AplOperator.getInstance().findTask(parentid).addTask(task);
        }

        return newtask;
    }

    /**
     * @param parentid
     * @param task
     * @return
     */
    public boolean removeTask(Vector<Long> parentid, AbstractTask task) {
        boolean ret = false;
        if (this.isIDContained(parentid, task.getIDVector())) {
            if (AplOperator.getInstance().findTask(parentid) != null) {
                //もう一度呼ぶ．
                ret = AplOperator.getInstance().findTask(parentid).removeTask(task);
            }
        } else {
            return false;
        }

        return ret;
    }


    /**
     * <p>指定IDを基にして，上の層からタスクを探します．返されるタスクは，テンポラリデータではなく生データ
     * なので，直接操作できます．
     *
     * @param id
     * @return
     */
    public AbstractTask findTask(Vector<Long> id) {
        return AplOperator.getInstance().getApl().findTask(id);
    }


    public BBTask generateMultipleDAGs(String filename){
        try{

            Properties prop = new Properties();
            //create input stream from file
            prop.load(new FileInputStream(filename));
            //DAG数
            this.multipleNum =  Integer.valueOf(prop.getProperty("task.multiple.num")).intValue();
            //CCR
            this.multipleCCRMin = Double.valueOf(prop.getProperty("task.multiple.ccr.min")).doubleValue();

            this.multipleCCRMax = Double.valueOf(prop.getProperty("task.multiple.ccr.max")).doubleValue();

            this.multipleTasknumMin = Long.valueOf(prop.getProperty("task.multiple.tasknum.min")).longValue();

            this.multipleTasknumMax = Long.valueOf(prop.getProperty("task.multiple.tasknum.max")).longValue();

            this.load(filename);
            long idx = 2;
            //this.multiplecurrentidx = 2;
            BBTask retApl = null;
            //一通りのタスクをセットしたら，次はセットする．
            MultipleDAGOperator multiple = new MultipleDAGOperator();
            //複数DAG生成のためのループ
            for(int i=0;i<this.multipleNum;i++){
                this.aplIDSet.clear();
//System.out.println("DAG#"+i);
                BBTask apl = new BBTask(Constants.TYPE_BASIC_BLOCK, 0, 0, 0);
                //最上位レイヤ(第一層)におけるタスク数
                long tasknum = this.generateLongValue(this.multipleTasknumMin, this.multipleTasknumMax);
                Vector<Long> id = new Vector<Long>();
                id.add(new Long(1));
                apl.setIDVector(id);
                this.multiplecurrentidx = idx;
  //System.out.println("####idx:"+this.multiplecurrentidx);
                //タスク追加のためのループ
                for (int j=0;j<tasknum;j++){
                    AbstractTask updatedTask = new AbstractTask();
                    updatedTask = this.buildChildTask(true);
                    Vector<Long> idvec = new Vector<Long>();
                    idvec.add(new Long(1));
                    idvec.add(new Long(idx));
                    updatedTask.setIDVector(idvec);
                    //IDの設定
                    apl.addTaskMulti(updatedTask, this.multiplecurrentidx);
                    idx++;
                    if(j==tasknum-1){
                        //ENDタスクであればセットする．
                        apl.setEndTask(idvec);
                    }

                }
                //依存関係のセット
                apl = this.assignDDForMultipleDAG(apl);


                retApl = multiple.addDAG(apl);

            }
            AplOperator.getInstance().setApl(retApl);

            return AplOperator.getInstance().getApl();

        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public void load(String filename){
        try{
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

            this.setDistType(Integer.valueOf(prop.getProperty("size.distibution")).intValue());

            this.setDist_mu(Double.valueOf(prop.getProperty("random.mu")).doubleValue());

            this.setDist_mu_data(Double.valueOf(prop.getProperty("random.mu.com")).doubleValue());

            this.setDist_sigma(Integer.valueOf(prop.getProperty("random.sigma")).doubleValue());

            this.startNumRate = Double.valueOf(prop.getProperty("task.NumOfStartRate")).doubleValue();

            this.depth_alpha =Integer.valueOf(prop.getProperty("task.depth.alpha")).intValue();

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * @param filename
     */
    public void constructTask(String filename) {

        try {
            this.load(filename);

            //最上位レイヤ(第一層)におけるタスク数
            long tasknum = this.generateLongValue(this.getNumOfTask_min(), this.getNumOfTask_max());



            //APLを生成して，シングルトンにセットする．
            BBTask apl = new BBTask(Constants.TYPE_BASIC_BLOCK, 0, 0, 0);
            Vector<Long> id = new Vector<Long>();
            id.add(new Long(1));
            apl.setIDVector(id);
            AplOperator.getInstance().setApl(apl);

            // long childnum = this.generateLongValue(this.numOfTask_min,this.numOfTask_max);

            //第一層の各タスクが，あと何層もつかを決める．
            // int layernum = (int)this.generateLongValue(getNumOfLayer_min(), getNumOfLayer_max());

           // System.out.println("1_タスク数:" + tasknum);
            //第一層の各タスクをチェックするためのループ
            for (int i = 0; i < tasknum; i++) {
                //第一層の各タスクが，あと何層もつかを決める．
                int layernum = (int) this.generateLongValue(getNumOfLayer_min(), getNumOfLayer_max());
                //System.out.println("レイヤ数"+layernum);
                Vector<Long> topid = null;
                AbstractTask updatedTask = new AbstractTask();

                //階層構造になる場合，階層構造構築ルーチン
                if (layernum >= 3) {

                    //1階層目のタスクたちをつくる
                    AbstractTask newTask = this.buildChildTask(false);
                    //まずはAPLへ追加してIDを付与してもらう
                    newTask = AplOperator.getInstance().getApl().addTask(newTask);

                    //2階層目のタスクの数
                    long childnum = this.generateLongValue(this.numOfTask_min, this.numOfTask_max);
                    //System.out.println("2_タスク数:"+childnum);
                    //2階層以降のタスクをつくる
                    //ここが大事!!!
                    updatedTask = this.buildTask(newTask, childnum, 2, layernum);
                    AplOperator.getInstance().getApl().updateTask(updatedTask);

                } else if (layernum == 2) {
                    //1階層目のタスクたちをつくる
                    updatedTask = this.buildChildTask(false);
                    updatedTask = AplOperator.getInstance().getApl().addTask(updatedTask);

                    //2階層目のタスクの数
                    long childnum = this.generateLongValue(this.numOfTask_min, this.numOfTask_max);
                    //System.out.println("2_タスク数:"+childnum);

                    for (int j = 0; j < childnum; j++) {
                        AbstractTask grandchaildTask = this.buildChildTask(true);
                        updatedTask.addTask(grandchaildTask);
                    }
                    AplOperator.getInstance().getApl().updateTask(updatedTask);


                } else {
                    //最大階層が1であるときは，そのまま実際の値を作る．
                    updatedTask = this.buildChildTask(true);
                    AplOperator.getInstance().getApl().addTask(updatedTask);

                }


            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * @param pTask
     * @return
     */
    public AbstractTask buildTask(AbstractTask pTask, long tasknum, int currentlayer, int maxlayer) {

        //下から2番目の階層であれば、子タスクに対して各種値を決める必要がある。
        if (currentlayer + 1 == maxlayer) {
            // System.out.println(currentlayer+"_タスク数:"+tasknum);
            for (int i = 0; i < tasknum; i++) {
                AbstractTask childtask = buildChildTask(true);
                pTask.addTask(childtask);
            }
            //子タスクをつくって，親タスクにセットする．
            //そして，その親タスクを返す．
            return pTask;
        } else {
            //System.out.println(currentlayer+"_タスク数:"+tasknum);

            for (int i = 0; i < tasknum; i++) {
                //孫のタスク数を決める
                long childtasknum = this.generateLongValue(this.numOfTask_min, this.numOfTask_max);
                AbstractTask newTask = this.buildChildTask(false);
                //IDを付与してもらう．
                newTask = pTask.addTask(newTask);

                //子タスクに対して同様に再帰CALLして孫をセットする．
                AbstractTask updatedTask = this.buildTask(newTask, childtasknum, currentlayer + 1, maxlayer);
                //孫タスクの子タスクを，親タスクにセットする．
                pTask.updateTask(updatedTask);
            }
            return pTask;

        }

    }

    /**
     * @param lowest
     * @return
     */
    public AbstractTask buildChildTask(boolean lowest) {
        long weight = 0;
        if (lowest) {
            weight = this.generateLongValueForSize(this.inst_min, this.inst_max);
        }
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

    public double generateDoubleValue(double min, double max){
        return Calc.getRoundedValue(min + (double)(Math.random()*(max-min+1)));

    }




    public long generateLongValueForSize(long min, long max){
        if(this.distType == 1){
            return min + (long) (Math.random() * (max - min + 1));

        }else{
            double meanValue2 = min + (max-min)* this.dist_mu;
           // double sig = (double)(max - min)/(double)6;
            double ave = (max + min)/2;

            double sig = (double)(Math.max((meanValue2-min), (max-meanValue2)))/3;
            //double sig = (double)(max - min);
            double ran2 = this.rData.nextGaussian(meanValue2,sig);

            if(ran2 < min){
                ran2 =(double) min;
            }

            if(ran2 > max){
                ran2 = (double)max;
            }

            return (long) ran2;

        }

    }

        public long generateLongValueForSize2(long min, long max){
        if(this.distType == 1){
            return min + (long) (Math.random() * (max - min + 1));


        }else{
            double meanValue2 = min + (max-min)* this.dist_mu_data;
            double sig = (double)(Math.max((meanValue2-min), (max-meanValue2)))/3;
            double ran2 = this.rDataCom.nextGaussian(meanValue2,sig);


            if(ran2 < min){
                ran2 =(double) min;
            }

            if(ran2 > max){
                ran2 = (double)max;
            }

            return (long) ran2;
        }

    }

    public int generateIntValue(int min, int max) {
        return min + (int) (Math.random() * (max - min + 1));
    }

    /**
     * @param min
     * @param max
     * @param error
     * @return
     */
    public int generateIntValue(int min, int max, int error) {
        int ret = 0;
        while ((ret = this.generateIntValue(min, max)) == error) {
            if (ret != error) {
                break;
            }
        }
        return ret;

    }

    /**
     * @param min
     * @param max
     * @param errors
     * @return
     */
    public int generateIntValue(int min, int max, int[] errors) {
        int ret = 0;
        boolean errorflg = true;

        while (!errorflg) {
            ret = this.generateIntValue(min, max);
            int len = errors.length;
            for (int i = 0; i < len; i++) {
                if (ret == errors[i]) {
                    //見つかった時点でbreak
                    errorflg = false;
                    break;
                }
            }
        }
        if (errorflg) {
            return ret;
        } else {
            return 0;
        }
    }


    /**
     * @param min
     * @param max
     * @return
     */
    public long generateLayerNum(long min, long max) {
        return min + (long) (Math.random() * (max - min + 1));
    }

    /**
     * @param min
     * @param max
     * @return
     */
    public long generateInstruction(long min, long max) {
        return min + (long) (Math.random() * (max - min + 1));

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
     * @param shortID
     * @param longID
     * @return
     */
    public boolean isIDContained(Vector<Long> shortID, Vector<Long> longID) {
        if (shortID.size() > longID.size()) {
            return false;
        }

        int s_size = shortID.size();
        for (int i = 0; i < s_size; i++) {
            if (shortID.get(i).longValue() != longID.get(i).longValue()) {
                return false;
            }
        }

        return true;
    }


    /**
     * @return
     */
    public long getNumOfLayer_min() {
        return numOfLayer_min;
    }

    /**
     * @param numOfLayer_min
     */
    public void setNumOfLayer_min(long numOfLayer_min) {
        this.numOfLayer_min = numOfLayer_min;
    }

    /**
     * @return
     */
    public long getNumOfLayer_max() {
        return numOfLayer_max;
    }

    /**
     * @param numOfLayer_max
     */
    public void setNumOfLayer_max(long numOfLayer_max) {
        this.numOfLayer_max = numOfLayer_max;
    }

    /**
     * @return
     */
    public long getNumOfTask_min() {
        return numOfTask_min;
    }

    /**
     * @param numOfTask_min
     */
    public void setNumOfTask_min(long numOfTask_min) {
        this.numOfTask_min = numOfTask_min;
    }

    /**
     * @return
     */
    public long getNumOfTask_max() {
        return numOfTask_max;
    }

    /**
     * @param numOfTask_max
     */
    public void setNumOfTask_max(long numOfTask_max) {
        this.numOfTask_max = numOfTask_max;
    }

    /**
     * @return
     */
    public long getThreshold() {
        return threshold;
    }

    /**
     * @param threshold
     */
    public void setThreshold(long threshold) {
        this.threshold = threshold;
    }

    /**
     * @return
     */
    public long getInst_max() {
        return inst_max;
    }

    /**
     * @param inst_max
     */
    public void setInst_max(long inst_max) {
        this.inst_max = inst_max;
    }

    /**
     * @return
     */
    public long getInst_min() {
        return inst_min;
    }

    /**
     * @param inst_min
     */
    public void setInst_min(long inst_min) {
        this.inst_min = inst_min;
    }

    /**
     * @return
     */
    public long getDdedge_min() {
        return ddedge_min;
    }

    /**
     * @param ddedge_min
     */
    public void setDdedge_min(long ddedge_min) {
        this.ddedge_min = ddedge_min;
    }

    /**
     * @return
     */
    public long getDdedge_max() {
        return ddedge_max;
    }

    /**
     * @param ddedge_max
     */
    public void setDdedge_max(long ddedge_max) {
        this.ddedge_max = ddedge_max;
    }

    /**
     * @return
     */
    public long getDdedge_sizemin() {
        return ddedge_sizemin;
    }

    /**
     * @param ddedge_sizemin
     */
    public void setDdedge_sizemin(long ddedge_sizemin) {
        this.ddedge_sizemin = ddedge_sizemin;
    }

    /**
     * @return
     */
    public long getDdedge_sizemax() {
        return ddedge_sizemax;
    }

    /**
     * @param ddedge_sizemax
     */
    public void setDdedge_sizemax(long ddedge_sizemax) {
        this.ddedge_sizemax = ddedge_sizemax;
    }

    /**
     * @return
     */
    public long getCdedge_labelmin() {
        return cdedge_labelmin;
    }

    /**
     * @param cdedge_labelmin
     */
    public void setCdedge_labelmin(long cdedge_labelmin) {
        this.cdedge_labelmin = cdedge_labelmin;
    }

    /**
     * @return
     */
    public long getCdedge_labelmax() {
        return cdedge_labelmax;
    }

    /**
     * @param cdedge_labelmax
     */
    public void setCdedge_labelmax(long cdedge_labelmax) {
        this.cdedge_labelmax = cdedge_labelmax;
    }

    /**
     * @return
     */
    public long getEdge_samemin() {
        return edge_samemin;
    }

    /**
     * @param edge_samemin
     */
    public void setEdge_samemin(long edge_samemin) {
        this.edge_samemin = edge_samemin;
    }

    /**
     * @return
     */
    public long getEdge_samemax() {
        return edge_samemax;
    }

    /**
     * @param edge_samemax
     */
    public void setEdge_samemax(long edge_samemax) {
        this.edge_samemax = edge_samemax;
    }

    /**
     * @return
     */
    public long getLoopnummin() {
        return loopnummin;
    }

    /**
     * @param loopnummin
     */
    public void setLoopnummin(long loopnummin) {
        this.loopnummin = loopnummin;
    }

    /**
     * @return
     */
    public long getLoopnummax() {
        return loopnummax;
    }

    /**
     * @param loopnummax
     */
    public void setLoopnummax(long loopnummax) {
        this.loopnummax = loopnummax;
    }

    /**
     * @return
     */
    public int getCalcmethod() {
        return calcmethod;
    }

    /**
     * @param calcmethod
     */
    public void setCalcmethod(int calcmethod) {
        this.calcmethod = calcmethod;
    }



    public PriorityQueue<AbstractTask> createTaskSet(Hashtable<Long, AbstractTask> tasklist){
        Collection<AbstractTask> taskCollection = tasklist.values();
        Iterator<AbstractTask> ite = taskCollection.iterator();
        PriorityQueue<AbstractTask> depthQueue = new PriorityQueue<AbstractTask>(5, new DepthComparator());

        while(ite.hasNext()){
            AbstractTask task = ite.next();
            depthQueue.add(task);

        }

        return  depthQueue;

    }
    /**
     * @param tasklist
     * @return
     */
    public TreeSet<Long> createIDSet(Hashtable<Long, AbstractTask> tasklist) {
        Collection<AbstractTask> taskCollection = tasklist.values();
        Iterator<AbstractTask> ite = taskCollection.iterator();
        TreeSet<Long> retset = new TreeSet<Long>();
        //long startmillis = System.currentTimeMillis();

        while (ite.hasNext()) {

            AbstractTask task = ite.next();
            Vector<Long> idlist = task.getIDVector();

            Long lastid = idlist.lastElement();


            retset.add(lastid);

        }
      //  long endmillis = System.currentTimeMillis();
      //  System.out.println("Span:"+(endmillis-startmillis));



         //System.out.println("サイズ:"+retset.size());
        //必ず複製する！
        return (TreeSet<Long>) retset.clone();
    }


    /**
     * 対象となるDsucを，自分よりもIDの後のやつのみに絞り込む．
     * @param pTask
     * @param cTask
     * @param isdd
     * @return
     */
    public TreeSet<Long> getCandidateTaskFromOldOnly(AbstractTask pTask,
                                                     AbstractTask cTask,
                                                     boolean isdd){
        TreeSet allSet = new TreeSet<Long>();
        if(pTask.getIDVector().size() == 1){
            if(this.aplIDSet.isEmpty()){
                this.aplIDSet = this.createIDSet(AplOperator.getInstance().getApl().getTaskList());
            }else{

            }
            allSet = (TreeSet<Long>)this.aplIDSet.clone();

        }else{
            allSet = this.createIDSet(pTask.getTaskList());
        }

        //allSetから，start候補を外す．
        int startLen = (int)Math.ceil((this.startNumRate*AplOperator.getInstance().getApl().getTaskClusterList().size()));

        for(int i=1;i<=startLen;i++){
            allSet.remove(new Long(i));
        }
        Long cID = cTask.getIDVector().lastElement();
        TreeSet<Long> retSet = (TreeSet<Long>)allSet.tailSet(cID);
        retSet.remove(cID);
        return retSet;
    }

    public TreeSet<Long> getCandidateTaskFromOldOnlyMulti(BBTask pTask,
                                                     AbstractTask cTask,
                                                     boolean isdd){
        TreeSet allSet = new TreeSet<Long>();
        if(pTask.getIDVector().size() == 1){
            if(this.aplIDSet.isEmpty()){
                this.aplIDSet = this.createIDSet(pTask.getTaskList());
            }else{

            }
            allSet = (TreeSet<Long>)this.aplIDSet.clone();

        }else{
            allSet = this.createIDSet(pTask.getTaskList());
        }

        //allSetから，start候補を外す．
      //  int startLen = (int)Math.ceil((this.startNumRate*AplOperator.getInstance().getApl().getTaskClusterList().size()));

        Iterator<Long> startIte = pTask.getStartTaskSet().iterator();
        while(startIte.hasNext()){
            Long sID = startIte.next();
            allSet.remove(sID);
        }

        Long cID = cTask.getIDVector().lastElement();
        TreeSet<Long> retSet = (TreeSet<Long>)allSet.tailSet(cID);
        retSet.remove(cID);
        return retSet;
    }


    public void getCandidateTaskFrom(AbstractTask pTask,
                                                     AbstractTask cTask,
                                                     boolean isdd){
        TreeSet allSet = new TreeSet<Long>();
        if(pTask.getIDVector().size() == 1){
            if(this.aplIDSet.isEmpty()){
                this.aplIDSet = this.createIDSet(AplOperator.getInstance().getApl().getTaskList());
            }else{

            }
            allSet = (TreeSet<Long>)this.aplIDSet.clone();

        }else{
            allSet = this.createIDSet(pTask.getTaskList());
        }

        //allSetから，start候補を外す．
        int startLen = (int)(this.startNumRate*AplOperator.getInstance().getApl().getTaskClusterList().size());

        for(int i=1;i<=startLen;i++){
            allSet.remove(new Long(i));
        }
        Long cID = cTask.getIDVector().lastElement();
        TreeSet<Long> retSet = (TreeSet<Long>)allSet.tailSet(cID);
        retSet.remove(cID);
        //retSetに含まれるタスクを優先度キューに入れる．

        if(this.dQueue == null){
          this.dQueue = new PriorityQueue<AbstractTask>(5, new DepthComparator());
          Iterator<Long> tIte = allSet.iterator();
          while(tIte.hasNext()){
              AbstractTask t =  AplOperator.getInstance().getApl().findTaskByLastID(tIte.next());
              this.dQueue.add(t);
          }
        }


    }

    /**
     * @param pTask 親タスク
     * @param task  当該タスク
     * @param isdd  true:データ依存
     * @return
     */
    public TreeSet<Long> getCandidateTaskIDSet(AbstractTask pTask,
                                               AbstractTask task, boolean isdd) {
        if(pTask.getIDVector().size() == 1){
            //もしpTaskが最上タスクなら，別途処理をする．(処理の高速化)
            return this.getCandidateTaskIDSetFromApl(task,isdd);
        }


        Hashtable<Long, AbstractTask> tasklist = pTask.getTaskList();
        TreeSet allSet = this.createIDSet(tasklist);



        //全部 - 先祖 - 自分 == (出力なしの未チェックタスクたち) + (チェック済みで先祖でないタスクたち)
        allSet.removeAll(task.getAncestorIDList());
        allSet.remove(task.getIDVector().lastElement());
        //さらに，STARTタスクも除外する
        allSet.remove(pTask.getStartTask().lastElement());

        //さらに，自分の後続タスクも除外する
        //制御依存でCALLされた場合，制御依存の後続タスクが×で、データ依存はOK．
        //つまり，制御依存の後続タスクのみを除外
        if (!isdd) {
            LinkedList<ControlDependence> csucIDlist = task.getCsucList();
            Iterator<ControlDependence> ite = csucIDlist.iterator();
            while (ite.hasNext()) {
                ControlDependence cd = ite.next();
                Vector<Long> toid = cd.getLabelToID();
                //対象から，制御依存の後続タスクを除外する．
                allSet.remove(toid.lastElement());

            }
        } else {
            //データ依存の場合は，制御依存の後続タスクは○で，データ依存が×．
            LinkedList<DataDependence> dsucIDList = task.getDsucList();
            Iterator<DataDependence> ite = dsucIDList.iterator();
            while (ite.hasNext()) {
                DataDependence dd = ite.next();
                Vector<Long> toid = dd.getToID();
                //対象から，データ依存の後続タスクを除外する
                allSet.remove(toid.lastElement());
            }

        }
        //次は，データ依存の場合


        return allSet;

    }

    public TreeSet<Long> getCandidateTaskIDSetFromApl(AbstractTask task, boolean isdd) {
        TreeSet allSet = new TreeSet();

        if(this.aplIDSet.isEmpty()){
            this.aplIDSet = this.createIDSet(AplOperator.getInstance().getApl().getTaskList());
        }else{
        }
        allSet = (TreeSet)this.aplIDSet.clone();


        //LinkedList<AbstractTask> tasklist = pTask.getTaskList();
        //HashSet allSet = this.createIDSet(tasklist);



        //全部 - 先祖 - 自分 == (出力なしの未チェックタスクたち) + (チェック済みで先祖でないタスクたち)
        allSet.removeAll(task.getAncestorIDList());
        allSet.remove(task.getIDVector().lastElement());
        //さらに，STARTタスクも除外する
        allSet.remove(AplOperator.getInstance().getApl().getStartTask().lastElement());

        //さらに，自分の後続タスクも除外する
        //制御依存でCALLされた場合，制御依存の後続タスクが×で、データ依存はOK．
        //つまり，制御依存の後続タスクのみを除外
        if (!isdd) {
            LinkedList<ControlDependence> csucIDlist = task.getCsucList();
            Iterator<ControlDependence> ite = csucIDlist.iterator();
            while (ite.hasNext()) {
                ControlDependence cd = ite.next();
                Vector<Long> toid = cd.getLabelToID();
                //対象から，制御依存の後続タスクを除外する．
                allSet.remove(toid.lastElement());

            }
        } else {
            //データ依存の場合は，制御依存の後続タスクは○で，データ依存が×．
            LinkedList<DataDependence> dsucIDList = task.getDsucList();
            Iterator<DataDependence> ite = dsucIDList.iterator();
            while (ite.hasNext()) {
                DataDependence dd = ite.next();
                Vector<Long> toid = dd.getToID();
                //対象から，データ依存の後続タスクを除外する
                allSet.remove(toid.lastElement());
            }

        }
        //次は，データ依存の場合


        return allSet;

    }

    public BBTask assignDDForMultipleDAG(BBTask apl){

        //関数CALLの場合，子タスクは外部との依存はない．ただし，STARTとENDは違うけど．
        //親タスクがレイヤkの場合: startはレイヤkのほかタスクからの入力を持ち，endはレイヤkへの出力がある．

        double ccr = this.generateDoubleValue(this.multipleCCRMin, this.multipleCCRMax);


        //1レイヤ目のタスク要素数を見る。
        BBTask upperTask = apl;
        Hashtable<Long, AbstractTask> tmpTaskList = upperTask.getTaskList();

        int tasknum = tmpTaskList.size();
        int startLen = (int)Math.ceil((tasknum*this.startNumRate));
        this.depth = (int) (Math.sqrt(tasknum)/this.depth_alpha);


        //START, ENDを決める．
      //  AplOperator.getInstance().getApl().setStartTask(upperTask.getTaskList().get(new Long(1)).getIDVector());
        int size = upperTask.getTaskList().size();
        AbstractTask end = upperTask.getTaskList().get(new Long(size));

        //もしENDが条件分岐であれば，BasicBlockに変更する．
/*        if (end.getType() == Constants.TYPE_CONDITION) {
            apl.findTask(end.getIDVector()).setType(Constants.TYPE_BASIC_BLOCK);
        }
*/
        CustomIDSet startSet = new CustomIDSet();

        //Startタスクの最大数
        int start_num =(int)Math.ceil(Double.valueOf(tasknum*startNumRate));
        //currentnum ~ currentNum+start_num-1までが，startタスクの番号となる．
        int max_start = (int)this.multiplecurrentidx+start_num-1;
        for(int i=(int)this.multiplecurrentidx;i<=max_start;i++){
            apl.getStartTaskSet().add(new Long(i));
        }

        //深さ
        int depth = Double.valueOf(Math.sqrt(tasknum)/this.depth_alpha).intValue();

        Iterator<AbstractTask> taskIte =   apl.getTaskList().values().iterator();
        CustomIDSet tmpStartSet = new CustomIDSet();
        while(taskIte.hasNext()){
            AbstractTask t = taskIte.next();
            if(t.getIDVector().get(1) - this.multiplecurrentidx+1 <= start_num){
                continue;
            }else{
                tmpStartSet.add(t.getIDVector().get(1));
            }
        }

        long endidx = apl.getEndTask().get(1);

        //トップレベルにおける依存関係の決定のためのループ
        for (int i = (int)this.multiplecurrentidx; i <= endidx; i++) {
            //最新のタスクを取得する
           // AbstractTask task =apl.getTaskList().get(new Long(i));
            AbstractTask task = apl.findTaskByLastID(new Long(i));
            //AbstractTask task = aplite.next();
            //もしStartタスクであれば，StartSetへ追加しておく．
            //if(task.getDpredList().isEmpty()){
            startSet = apl.getStartTaskSet();




            //当該タスクがENDタスクであれば，何もしない
            if (this.isIDEqual(task.getIDVector(),apl.getEndTask())) {
                continue;
            }

            //以下，全タスクのタイプに対する処理
            //データ依存の出力辺を追加するための処理
            //候補タスクのIDを取得する．制御依存とかぶってもよいので，最初からやり直し．
            TreeSet<Long> remainSet =
                    this.getCandidateTaskFromOldOnlyMulti(apl,task,true);


            // this.getCandidateTaskFrom(AplOperator.getInstance().getApl(),task,true);

            //データ依存辺の出力辺数を決める．
            long ddoutnum = this.generateLongValue(this.getDdedge_min(), this.getDdedge_max());
            int dsucidx;
            // int dsize = this.dQueue.size();
            int dsize = remainSet.size();
            Long dsucID = new Long(0);
            Long[] dcandidates = remainSet.toArray(new Long[0]);
            // Long[] dcandidates = this.dQueue.toArray(new Long[0]);
            int inverval = (int)(tasknum/this.depth);

            //long currentID = task.getIDVector().lastElement().longValue();
            SortedSet<Long> tmpSet = null;

            for (int j = 0; j < ddoutnum; j++) {
                if (dsize > 0) {
                    if(j ==0){
                        long tmpnextID = task.getIDVector().get(1) + inverval;
                        dsucID = Math.min(apl.getEndTask().get(1).longValue(), tmpnextID);
                    }else{
                        if(tmpStartSet.isEmpty()){
                            //全タスクが規定の深さまで行っていれば，通常通りランダム操作にうつる．
                            dsucidx = this.generateIntValue(0, dsize - 1);
                            dsucID = dcandidates[dsucidx];
                          //  System.out.println("dsuc1:"+dsucID);
                            // }

                        }else{
                            tmpSet = tmpStartSet.getObjSet().tailSet(task.getIDVector().get(1));
                            tmpSet.remove(task.getIDVector().get(1));
                            if(tmpSet.isEmpty()){
                                continue;
                            }
                            dsucID = tmpSet.first();
                            tmpStartSet.remove(dsucID);
                         //   System.out.println("dsuc2:"+dsucID);

                        }
                    }

                }else {
                    break;
                }

                AbstractTask dsucTask = apl.findTaskByLastID(dsucID);

                DataDependence dd = new DataDependence(task.getIDVector(), dsucTask.getIDVector(), 0, 0, 0);
//Added 2008/04/05
                // long datasize = 0;
                long tmp = 0;
               /* if(j == 0){
                    datasize = this.generateLongValueForSize2(this.ddedge_sizemin, this.ddedge_sizemax);
                    tmp = datasize;
                }else{
                    datasize =tmp;
                }*/
               // long datasize = this.generateLongValueForSize2(this.ddedge_sizemin, this.ddedge_sizemax);
                //データサイズの決定
                long workload = this.generateLongValue(this.inst_min, this.inst_max);

                long datasize = (long)(ccr*workload);

                dd.setMaxDataSize(datasize);
                dd.setAveDataSize(datasize);
                dd.setMinDataSize(datasize);
                remainSet.remove(dsucID);
                if(dd.getFromID()==null || dd.getToID()==null){
                    continue;
                }
                int maxDepth = 0;
                //当該タスクに，後続タスクをセット
                if(task.addDsuc(dd)){
                    if(upperTask.getMaxData()<=dd.getMaxDataSize()){
                        upperTask.setMaxData(dd.getMaxDataSize());
                    }
                    if(upperTask.getMinData()>=dd.getMaxDataSize()){
                        upperTask.setMinData(dd.getMaxDataSize());
                    }
                    //後続タスクに，先行タスクをセット
                    dsucTask.addDpred(dd);
                    //dsucのdepthを更新
                    Iterator<DataDependence> dpredIte = dsucTask.getDpredList().iterator();
                    while(dpredIte.hasNext()){
                        AbstractTask preTask = apl.findTaskByLastID(dpredIte.next().getFromID().get(1));
                        int d = preTask.getDepth();
                        if(d >= maxDepth){
                            maxDepth = d;
                        }
                    }
                    dsucTask.setDepth(maxDepth+1);
                    //this.dQueue.add(dsucTask);
                }
            }

        }
        AbstractTask endTask = apl.findTaskByLastID(apl.getEndTask().get(1));
        CustomIDSet ansSet = new CustomIDSet();
        ansSet.add(endTask.getIDVector().get(1));
        LinkedList<DataDependence> dpredList = endTask.getDpredList();
        Iterator<DataDependence> dpredIte = dpredList.iterator();
        while(dpredIte.hasNext()){
            DataDependence dd = dpredIte.next();
            AbstractTask dpredTask = apl.findTaskByLastID(dd.getFromID().get(1));
//            this.updateAncestor(ansSet, dpredTask);
        }
        //全体のPGの命令数を集計する．
        BBTask task = (BBTask)AplOperator.getInstance().calculateInstructions(apl);
        //AplOperator.getInstance().setApl(task);
        return apl;

    }


    /**
     * 1: 上位レベルからデータ依存，制御依存関係を決める
     * 2:
     */
    public BBTask assignDependencyProcess() {
        //関数CALLの場合，子タスクは外部との依存はない．ただし，STARTとENDは違うけど．
        //親タスクがレイヤkの場合: startはレイヤkのほかタスクからの入力を持ち，endはレイヤkへの出力がある．

        //1レイヤ目のタスク要素数を見る。
        BBTask upperTask = AplOperator.getInstance().getApl();
        Hashtable<Long, AbstractTask> tmpTaskList = upperTask.getTaskList();

        int tasknum = tmpTaskList.size();
        int startLen = (int)Math.ceil((tasknum*this.startNumRate));
        this.depth = (int) (Math.sqrt(tasknum)/this.depth_alpha);


        //START, ENDを決める．
        AplOperator.getInstance().getApl().setStartTask(upperTask.getTaskList().get(new Long(1)).getIDVector());
       // AbstractTask end = upperTask.getTaskList().getLast();
        int size = upperTask.getTaskList().size();
        AbstractTask end = upperTask.getTaskList().get(new Long(size));

        //もしENDが条件分岐であれば，BasicBlockに変更する．
        if (end.getType() == Constants.TYPE_CONDITION) {
            AplOperator.getInstance().findTask(end.getIDVector()).setType(Constants.TYPE_BASIC_BLOCK);
        }

        AplOperator.getInstance().getApl().setEndTask(end.getIDVector());
        //START: 入力辺がない
        //END: 出辺がない．

        //ENDのインデックスは，最後のタスクとする．
        //HashSet<Long> allSet = this.createIDSet(tmpTaskList);
        //Iterator<AbstractTask> aplite = AplOperator.getInstance().getApl().getTaskList().iterator();

        CustomIDSet startSet = new CustomIDSet();

        //Startタスクの最大数
        int start_num =Double.valueOf(tasknum*startNumRate).intValue();

        //深さ
        int depth = Double.valueOf(Math.sqrt(tasknum)/this.depth_alpha).intValue();

        //Hashtable tmpStartTaskList = AplOperator.getInstance().getApl().getTaskList();
        Iterator<AbstractTask> taskIte =   AplOperator.getInstance().getApl().getTaskList().values().iterator();
        CustomIDSet tmpStartSet = new CustomIDSet();
        while(taskIte.hasNext()){
            AbstractTask t = taskIte.next();
            if(t.getIDVector().get(1) <= start_num){
                continue;
            }else{
                  tmpStartSet.add(t.getIDVector().get(1));
            }
        }


        ///while(aplite.hasNext()){
        //トップレベルにおける依存関係の決定のためのループ
        for (int i = 0; i < tasknum; i++) {
            //最新のタスクを取得する
            AbstractTask task = AplOperator.getInstance().getApl().getTaskList().get(new Long(i+1));
            //AbstractTask task = aplite.next();
            //もしStartタスクであれば，StartSetへ追加しておく．
            if(task.getDpredList().isEmpty()){
                startSet.add(task.getIDVector().get(1));
            }


            //もし選んだタスクが条件分岐であれば，制御依存の出力辺をつける．
            //当該タスクの後続となるタスクたち
            //HashSet<Long> sucsessorIDSet = new HashSet<Long>();

            if (task.getType() == Constants.TYPE_CONDITION) {
                //ラベル種類数を決める
                long labeltype = this.generateLongValue(this.getCdedge_labelmin(), this.getCdedge_labelmax());
                //各ラベルでの出辺の数を決める
                for (int j = 0; j < labeltype; j++) {
                    //同一ラベルの数を決める
                    long samelabel = this.generateLongValue(this.getEdge_samemin(), this.getEdge_samemax());
                    //Destination候補のID集合を取得する(Cyclicにしないための対処)
                    TreeSet<Long> candidateIDSet =
                            this.getCandidateTaskIDSetFromApl(task, false);
                    //csize: 後続タスク候補の数
                    int csize = candidateIDSet.size();
                    Long[] candidates = candidateIDSet.toArray(new Long[0]);

                    int csucidx;
                    Long csucID = new Long(0);
                    if (csize >= 2) {
                        //まずは，制御依存の実際の後続タスクを一つ決める．
                        csucidx = this.generateIntValue(0, csize - 1);
                        csucID = candidates[csucidx];
                    } else if (csize == 1) {
                        csucID = candidates[0];
                    } else if (csize == 0) {
                        break;
                    }
                    AbstractTask csucTask = AplOperator.getInstance().getApl().findTaskByLastID(csucID);
                    //AbstractTask csucTask = AplOperator.getInstance().getApl().findTaskByLastID(csucID);
                    ControlDependence cd =
                            new ControlDependence(task.getIDVector(), csucTask.getIDVector(), csucTask.getIDVector());
                    csucTask.addCpred(cd);
                    task.addCsuc(cd);
                    //csucTaskのancestorに，当該タスクの先祖達をコピーする
                    csucTask.getAncestorIDList().addAll(task.getAncestorIDList());

                    //あとは，csucに対して当該タスクもancestorに加える
                    csucTask.getAncestorIDList().add(task.getIDVector().lastElement());

                    //当該タスクとcsucタスクを更新する
                    AplOperator.getInstance().getApl().updateTask(csucTask);
                    AplOperator.getInstance().getApl().updateTask(task);
                    //候補タスクから，csucタスクを削除する
                    candidateIDSet.remove(csucID);

                    //制御依存関係の定義．同一ラベル内において，各csucを決める
                    for (int k = 0; k < samelabel; k++) {
                        int size1 = candidateIDSet.size();
                        if (size1 != 0) {
                            int idx = this.generateIntValue(0, size1 - 1);
                            //次の候補者をきめる
                            Long candidateID = candidates[idx];
                            AbstractTask csuc2 = AplOperator.getInstance().getApl().findTaskByLastID(candidateID);
                            //制御依存関係の定義
                            ControlDependence cd2 = new ControlDependence(task.getIDVector(), csucTask.getIDVector(), csuc2.getIDVector());
                            task.addCsuc(cd2);
                            csuc2.addCpred(cd2);
                            csuc2.getAncestorIDList().addAll(task.getAncestorIDList());
                            csuc2.getAncestorIDList().add(task.getIDVector().lastElement());
                            AplOperator.getInstance().getApl().updateTask(csucTask);
                            AplOperator.getInstance().getApl().updateTask(csuc2);
                            //後続タスクは，制御依存の候補者セットから削除する
                            candidateIDSet.remove(candidateID);

                        } else {
                            //候補者がゼロになった場合は，そのままbreakする
                            break;
                        }

                    }
                    //upperTaskをシングルトンに反映する
                    //AplOperator.getInstance().setApl(upperTask);

                }
            }

            //当該タスクがENDタスクであれば，何もしない
            if (this.isIDEqual(task.getIDVector(),
                    AplOperator.getInstance().getApl().getEndTask())) {
                continue;
            }
            /**
             * 31
             */
            //以下，全タスクのタイプに対する処理
            //データ依存の出力辺を追加するための処理
            //候補タスクのIDを取得する．制御依存とかぶってもよいので，最初からやり直し．
            TreeSet<Long> remainSet =
                    //this.getCandidateTaskIDSetFromApl(task, true);
                    this.getCandidateTaskFromOldOnly(AplOperator.getInstance().getApl(),task,true);


           // this.getCandidateTaskFrom(AplOperator.getInstance().getApl(),task,true);

            //データ依存辺の出力辺数を決める．
            long ddoutnum = this.generateLongValue(this.getDdedge_min(), this.getDdedge_max());
            int dsucidx;
           // int dsize = this.dQueue.size();
            int dsize = remainSet.size();
            Long dsucID = new Long(0);
            Long[] dcandidates = remainSet.toArray(new Long[0]);
           // Long[] dcandidates = this.dQueue.toArray(new Long[0]);
           int inverval = (int)(tasknum/this.depth);

            //long currentID = task.getIDVector().lastElement().longValue();
            SortedSet<Long> tmpSet = null;
            for (int j = 0; j < ddoutnum; j++) {
                if (dsize > 0) {
                    if(j ==0){
                        long tmpnextID = task.getIDVector().get(1) + inverval;
                        dsucID = Math.min(tasknum, tmpnextID);
                    }else{
                        if(tmpStartSet.isEmpty()){

                                 //Depthの一番低いものを選ぶ．
                                 /*AbstractTask t = this.dQueue.peek();
                                 if(t.getDepth() < this.depth){
                                     dsucID = t.getIDVector().get(1);
                                     this.dQueue.remove(t);
                                 } else{   */
                                     //全タスクが規定の深さまで行っていれば，通常通りランダム操作にうつる．
                                     dsucidx = this.generateIntValue(0, dsize - 1);
                                     dsucID = dcandidates[dsucidx];
                                // }

                             }else{
                                 tmpSet = tmpStartSet.getObjSet().tailSet(task.getIDVector().get(1));
                                 tmpSet.remove(task.getIDVector().get(1));
                            if(tmpSet.isEmpty()){
                                dsucidx = this.generateIntValue(0, dsize - 1);
                                dsucID = dcandidates[dsucidx];
                            }else{
                                dsucID = tmpSet.first();
                                tmpStartSet.remove(dsucID);
                            }

                             }
                    }

                }else {
                    break;
                }

                AbstractTask dsucTask = AplOperator.getInstance().getApl().findTaskByLastID(dsucID);
                DataDependence dd = new DataDependence(task.getIDVector(), dsucTask.getIDVector(), 0, 0, 0);
//Added 2008/04/05
               // long datasize = 0;
                long tmp = 0;
               /* if(j == 0){
                    datasize = this.generateLongValueForSize2(this.ddedge_sizemin, this.ddedge_sizemax);
                    tmp = datasize;
                }else{
                    datasize =tmp;
                }*/
                long datasize = this.generateLongValueForSize2(this.ddedge_sizemin, this.ddedge_sizemax);

                dd.setMaxDataSize(datasize);
                dd.setAveDataSize(datasize);
                dd.setMinDataSize(datasize);
                remainSet.remove(dsucID);
                int maxDepth = 0;
                //当該タスクに，後続タスクをセット
                if(task.addDsuc(dd)){
                    if(upperTask.getMaxData()<=dd.getMaxDataSize()){
                        upperTask.setMaxData(dd.getMaxDataSize());
                    }
                    if(upperTask.getMinData()>=dd.getMaxDataSize()){
                        upperTask.setMinData(dd.getMaxDataSize());
                    }
                    //後続タスクに，先行タスクをセット
                    dsucTask.addDpred(dd);
                    //dsucのdepthを更新
                    Iterator<DataDependence> dpredIte = dsucTask.getDpredList().iterator();
                    while(dpredIte.hasNext()){
                        AbstractTask preTask = AplOperator.getInstance().getApl().findTaskByLastID(dpredIte.next().getFromID().get(1));
                        int d = preTask.getDepth();
                        if(d >= maxDepth){
                            maxDepth = d;
                        }
                    }
                    dsucTask.setDepth(maxDepth+1);
                    //this.dQueue.add(dsucTask);
                }
            }

        }
        AbstractTask endTask = AplOperator.getInstance().getApl().findTaskByLastID(new Long(tasknum));
        CustomIDSet ansSet = new CustomIDSet();
        ansSet.add(endTask.getIDVector().get(1));
        LinkedList<DataDependence> dpredList = endTask.getDpredList();
        Iterator<DataDependence> dpredIte = dpredList.iterator();
        while(dpredIte.hasNext()){
            DataDependence dd = dpredIte.next();
            AbstractTask dpredTask = AplOperator.getInstance().getApl().findTaskByLastID(dd.getFromID().get(1));
            this.updateAncestor(ansSet, dpredTask);
        }
        //全体のPGの命令数を集計する．
        BBTask task = (BBTask)AplOperator.getInstance().calculateInstructions(AplOperator.getInstance().getApl());
        AplOperator.getInstance().setApl(task);

        return task;

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
            AbstractTask dpredTask = AplOperator.getInstance().getApl().findTaskByLastID(dd.getFromID().get(1));
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
     * 後続タスクに対して，先祖タスクをセットします　　
     *
     * @param task
     */
    public void  updateAncestor(AbstractTask predTask, AbstractTask task, HashSet<Long> anSet){
        //新規先祖セットを生成する．
        HashSet<Long> newAnsSet = new HashSet<Long>();
        newAnsSet.addAll(anSet);

        newAnsSet.add(predTask.getIDVector().get(1));
        //先行タスクの持分を追加させる．
        task.getAncestorIDList().addAll(newAnsSet);

        //もし後続タスクがENDタスクであれば，そこでリターンする．
        if(task.getDsucList().isEmpty()){
            return;
        }else{
            //当該タスクがENDタスクでない場合の処理
            LinkedList<DataDependence> dsucList = task.getDsucList();
            Iterator<DataDependence> dsucIte = dsucList.iterator();
            while(dsucIte.hasNext()){
                DataDependence dd = dsucIte.next();
                AbstractTask dsucTask = AplOperator.getInstance().getApl().findTaskByLastID(dd.getToID().get(1));
                this.updateAncestor(task, dsucTask, task.getAncestorIDList());
            }
        }

    }

        /**
     *
     * @return
     */
    public Serializable deepCopyAns(HashSet<Long> anSet){
        try{
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bout);
            out.writeObject(anSet);
            out.close();
            byte[] bytes = bout.toByteArray();
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
            Object newObject = in.readObject();
            in.close();
            return (Serializable) newObject;
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     *
     * @param aTask
     * @param lastid
     */
    public void addAncestor(AbstractTask aTask, Long lastid){

        //まずは当該ノードにおいて，新規IDを追加する．
        aTask.getAncestorIDList().add(lastid);

        LinkedList<DataDependence> ddlist = aTask.getDsucList();
        Iterator<DataDependence> ite = ddlist.iterator();
        while(ite.hasNext()){
            DataDependence dd = ite.next();
            Vector<Long> id = dd.getToID();
            AbstractTask cTask = AplOperator.getInstance().findTask(id);
            //後続タスクに対して新規IDを追加していく．
            addAncestor(cTask,lastid);
            AplOperator.getInstance().getApl().updateTask(cTask);
        }

    }

    public void addAncestorSet(AbstractTask aTask, AbstractTask cTask){

        //まずは当該ノードにおいて，新規IDを追加する．
        cTask.getAncestorIDList().addAll(aTask.getAncestorIDList());

        LinkedList<DataDependence> ddlist = cTask.getDsucList();
        Iterator<DataDependence> ite = ddlist.iterator();
        while(ite.hasNext()){
            DataDependence dd = ite.next();
            Vector<Long> id = dd.getToID();
            AbstractTask gTask = AplOperator.getInstance().findTask(id);
            //後続タスクに対して新規IDを追加していく．
            addAncestorSet(cTask,gTask);
            AplOperator.getInstance().getApl().updateTask(gTask);
        }

    }


    /**
     * @param pTask
     * @return
     */
    public AbstractTask updateParenetDependency(AbstractTask pTask) {
        //まずは親タスクからの依存情報マッピングをする．
        LinkedList<DataDependence> ddPredList = pTask.getDpredList();
        Iterator<DataDependence> ddPredite = ddPredList.iterator();

        LinkedList<DataDependence> ddSucList = pTask.getDsucList();
        Iterator<DataDependence> ddSucite = ddSucList.iterator();

        LinkedList<ControlDependence> cdPredList = pTask.getCpredList();
        Iterator<ControlDependence> cdPredite = cdPredList.iterator();

        LinkedList<ControlDependence> cdSucList = pTask.getCsucList();
        Iterator<ControlDependence> cdSucite = cdSucList.iterator();


        //もしpTaskが関数CALLである場合，pTaskにおける入出力はそれぞれSTART/ENDにすべてマッピングされる．
        if (pTask.getType() == Constants.TYPE_FUNCTION_CALL) {
            AbstractTask start = pTask.findTask(pTask.getStartTask());
            AbstractTask end = pTask.findTask(pTask.getEndTask());

            while (ddPredite.hasNext()) {
                DataDependence dd = ddPredite.next();
                //データ依存の宛先をSTARTへ更新する．
                dd.setToID(start.getIDVector());
                start.addDpred(dd);
            }

            while (cdPredite.hasNext()) {
                ControlDependence cd = cdPredite.next();
                cd.setCsucID(start.getIDVector());
                //ラベル宛先までは，無理・・・．
                start.addCpred(cd);
            }
            while (ddSucite.hasNext()) {
                DataDependence dd = ddSucite.next();
                dd.setFromID(end.getIDVector());
                end.addDsuc(dd);

            }
            while (cdSucite.hasNext()) {
                ControlDependence cd = cdSucite.next();
                cd.setLabelFromID(end.getIDVector());
                end.addCsuc(cd);
            }
            //親タスクのSTARTを更新する
            pTask.updateTask(start);
            pTask.updateTask(end);
        } else {
            //pTaskが関数CALLでない場合，任意に選択する．
            int size = pTask.getTaskList().size();
            while (ddPredite.hasNext()) {
                DataDependence dd = ddPredite.next();
                //データ依存の宛先をSTARTへ更新する．
                AbstractTask task = pTask.getTaskList().get(new Long(this.generateIntValue(0, size - 1))+1);
                dd.setToID(task.getIDVector());
                task.addDpred(dd);
                pTask.updateTask(task);
            }

            while (cdPredite.hasNext()) {
                ControlDependence cd = cdPredite.next();
                AbstractTask task = pTask.getTaskList().get(new Long(this.generateIntValue(0, size - 1)+1));

                cd.setCsucID(task.getIDVector());
                //ラベル宛先までは，無理・・・．
                task.addCpred(cd);
                pTask.updateTask(task);
            }
            while (ddSucite.hasNext()) {
                DataDependence dd = ddSucite.next();
                AbstractTask task = pTask.getTaskList().get(new Long(this.generateIntValue(0, size - 1)+1));
                dd.setFromID(task.getIDVector());
                task.addDsuc(dd);
                pTask.updateTask(task);

            }
            while (cdSucite.hasNext()) {
                ControlDependence cd = cdSucite.next();
                AbstractTask task = pTask.getTaskList().get(new Long(this.generateIntValue(0, size - 1)+1));
                cd.setLabelFromID(task.getIDVector());
                task.addCsuc(cd);
                pTask.updateTask(task);
            }

        }

        return pTask;
    }

    /**
     * 1: 親タスクからの入力・出力を，子タスクへのマッピング
     * 2: 子タスク同士の依存関係の定義をする
     * 3: 子の子へさらに再帰CALL
     *
     * @param pTask 親タスク
     * @return
     */
    public AbstractTask assignDependency(AbstractTask pTask) {

        Hashtable<Long, AbstractTask> tmpTaskList = pTask.getTaskList();
        //もしpTaskが最下層タスクであれば，ここで終了．
        if (tmpTaskList.isEmpty()) {
            //命令数の集計を行う．
            //もし制御依存により実行される場合は，最大，平均，最小を
            //定義する必要がある．
            if(!pTask.getCpredList().isEmpty()){
                //Min == [実行さえないとき]なので，0とする．
                pTask.setMinWeight(0);
                //平均は，「実行される場合とされない場合の平均」である．
                //よって，Max+0を2で割ればよい．
                pTask.setAveWeight((pTask.getMaxWeight()+0)/2);

            }

            return pTask;
        }
        //START, ENDを決める．
        pTask.setStartTask(pTask.getTaskList().get(new Long(1)).getIDVector());
        int len = pTask.getTaskList().size();
        //AbstractTask end = pTask.getTaskList().getLast();
        AbstractTask end = pTask.getTaskList().get(new Long(len));

        //もしENDが条件分岐であれば，BasicBlockに変更する．
        if (end.getType() == Constants.TYPE_CONDITION) {
            end.setType(Constants.TYPE_BASIC_BLOCK);
            //END自体の
            pTask.updateTask(end);

        }
        pTask.setEndTask(end.getIDVector());

        //pTaskに関する依存関係を子タスクへ反映させる．
        pTask = this.updateParenetDependency(pTask);

        int tasknum = tmpTaskList.size();

        //START: 入力辺がない
        //END: 出辺がない．

        //ENDのインデックスは，最後のタスクとする．
        //HashSet<Long> allSet = this.createIDSet(tmpTaskList);

        //トップレベルにおける依存関係の決定のためのループ

        for (int i = 0; i < tasknum; i++) {
            //最新のタスクを取得する
            //AbstractTask task = tmpTaskList.get(i);
            AbstractTask task = pTask.getTaskList().get(new Long(i+1));
            //もし選んだタスクが条件分岐であれば，制御依存の出力辺をつける．
            //当該タスクの後続となるタスクたち
            //HashSet<Long> sucsessorIDSet = new HashSet<Long>();

            if (task.getType() == Constants.TYPE_CONDITION) {
                //ラベル種類数を決める
                long labeltype = this.generateLongValue(this.getCdedge_labelmin(), this.getCdedge_labelmax());
                //各ラベルでの出辺の数を決める
                for (int j = 0; j < labeltype; j++) {
                    //同一ラベルの数を決める
                    long samelabel = this.generateLongValue(this.getEdge_samemin(), this.getEdge_samemax());
                    //Destination候補のID集合を取得する(Cyclicにしないための対処)
                    TreeSet<Long> candidateIDSet =
                            this.getCandidateTaskIDSet(pTask, task, false);
                    //csize: 後続タスク候補の数
                    int csize = candidateIDSet.size();
                    Long[] candidates = candidateIDSet.toArray(new Long[0]);
                    int csucidx;
                    Long csucID = new Long(0);
                    if (csize >= 2) {
                        //まずは，制御依存の実際の後続タスクを一つ決める．
                        csucidx = this.generateIntValue(0, csize - 1);
                        csucID = candidates[csucidx];
                    } else if (csize == 1) {
                        csucID = candidates[0];
                    } else if (csize == 0) {
                        break;
                    }
                    AbstractTask csucTask = pTask.findTaskByLastID(csucID);
                    ControlDependence cd =
                            new ControlDependence(task.getIDVector(), csucTask.getIDVector(), csucTask.getIDVector());
                    csucTask.addCpred(cd);
                    task.addCsuc(cd);
                    //csucTaskのancestorに，当該タスクの先祖達をコピーする
                    csucTask.getAncestorIDList().addAll(task.getAncestorIDList());

                    //あとは，csucに対して当該タスクもancestorに加える
                    csucTask.getAncestorIDList().add(task.getIDVector().lastElement());

                    //当該タスクとcsucタスクを更新する
                    pTask.updateTask(csucTask);
                    pTask.updateTask(task);
                    //候補タスクから，csucタスクを削除する
                    candidateIDSet.remove(csucID);

                    //制御依存関係の定義．同一ラベル内において，各csucを決める
                    for (int k = 0; k < samelabel; k++) {
                        int size = candidateIDSet.size();
                        if (size != 0) {
                            int idx = this.generateIntValue(0, size - 1);
                            //次の候補者をきめる
                            Long candidateID = candidates[idx];
                            AbstractTask csuc2 = pTask.findTaskByLastID(candidateID);
                            //制御依存関係の定義
                            ControlDependence cd2 = new ControlDependence(task.getIDVector(), csucTask.getIDVector(), csuc2.getIDVector());
                            task.addCsuc(cd2);
                            csuc2.addCpred(cd2);
                            csuc2.getAncestorIDList().addAll(task.getAncestorIDList());
                            csuc2.getAncestorIDList().add(task.getIDVector().lastElement());
                            pTask.updateTask(csucTask);
                            pTask.updateTask(csuc2);

                            //あとは，候補タスクセットを更新
                            candidateIDSet.remove(candidateID);

                        } else {
                            //候補者がゼロになった場合は，そのままbreakする
                            break;
                        }

                    }


                }
            }
            //当該タスクがENDタスクであれば，何もしない
            if (this.isIDEqual(task.getIDVector(),
                    pTask.getEndTask())) {
                continue;
            }

            //データ依存の出力辺を追加するための処理
            //候補タスクのIDを取得する．制御依存とかぶってもよいので，最初からやり直し．
            long startmillis = System.currentTimeMillis();
            TreeSet<Long> remainSet =
                    this.getCandidateTaskIDSet(pTask, task, true);
            long endmillis = System.currentTimeMillis();
           // System.out.println("millis:"+(endmillis - startmillis));
            //データ依存辺の出力辺数を決める．
            long ddoutnum = this.generateLongValue(this.getDdedge_min(), this.getDdedge_max());
            int dsucidx;
            int dsize = remainSet.size();
            Long dsucID = new Long(0);
            Long[] dcandidates = remainSet.toArray(new Long[0]);

            for (int j = 0; j < ddoutnum; j++) {
                if (dsize > 0) {
                    dsucidx = this.generateIntValue(0, dsize - 1);
                    dsucID = dcandidates[dsucidx];

                } else {
                    break;
                }
                AbstractTask dsucTask = pTask.findTaskByLastID(dsucID);
                DataDependence dd = new DataDependence(task.getIDVector(), dsucTask.getIDVector(), 0, 0, 0);
                task.addDsuc(dd);

                dsucTask.addDpred(dd);
                dsucTask.getAncestorIDList().addAll(task.getAncestorIDList());
                dsucTask.getAncestorIDList().add(task.getIDVector().lastElement());
                pTask.updateTask(task);
                pTask.updateTask(dsucTask);
                remainSet.remove(dsucID);
            }


        }
        //第N層における，データ依存＋制御依存関係の構築は終わったので，あとは再帰的に呼び出す．
        //現在のレイヤ内タスクが終わってから，最初からまた繰り返す．
        for (int k = 0; k < tasknum; k++) {
            AbstractTask task = pTask.getTaskList().get(new Long(k+1));
            //今度は，各要素を親タスクとして，再帰的に呼び出しをする．
            task = this.assignDependency(task);
            //子タスクを更新する．
            pTask.updateTask(task);

        }
        //命令数の集計を行う．
        pTask = this.calculateInstructions(pTask);

        return pTask;
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



    public long countTaskNum(AbstractTask task){
        if(task.getTaskList().isEmpty()){
            return 1;
        }
        int size = task.getTaskList().size();
        long cnt = 0;
        for(int i=0;i<size;i++){
            cnt += countTaskNum(task.getTaskList().get(new Long(i)));

        }
        return cnt;
    }



}
