package net.gripps.clustering.common.aplmodel;




import java.util.*;
import java.io.FileInputStream;

import net.gripps.clustering.common.Constants;
/**
 * User: Hidehiro Kanemitsu
 * Date: 2009/08/05
 * ガウス・ジョルダン法またはガウス消去法のDAGを生成するためのシングルトンクラスです．
 * DAGの生成・依存関係の定義を行います．
 *
 */
public class ImageCompress {

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
    private int midTask1size;
    private int midTask2size;
    private int comunit;

    private int rate;

    private int startup;

    private int midsize;

    private int startTaskSize;
    private int endTaskSize;

    private int midTaskSize_min;
    private int midTaskSize_max;

    private int datasize_min;
    private int datasize_max;

    private int midTask1Size_max;

    private int midTask1Size_min;

    private int midTask2Size_max;

    private int midTask2Size_min;

    private int midsize1 = 120;
    private int midsize2 = 119;




    /**
     * Singleton Object
     */
    private static ImageCompress singleton;

    /**
     *
     * @return
     */
    public static ImageCompress getInstance(){
        if(ImageCompress.singleton == null){
            ImageCompress.singleton = new ImageCompress();
        }

        return ImageCompress.singleton;
    }

    private ImageCompress() {
        this.aplIDSet = new TreeSet();

    }

    public int getCalcmethod() {
        return calcmethod;
    }

    public void setCalcmethod(int calcmethod) {
        this.calcmethod = calcmethod;
    }

    public static BBTask getApl() {
        return apl;
    }

    public static void setApl(BBTask apl) {
        ImageCompress.apl = apl;
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

            this.startTaskSize = Integer.valueOf(prop.getProperty("task.image.starttasksize")).intValue();
            this.endTaskSize = Integer.valueOf(prop.getProperty("task.image.endtasksize")).intValue();
            this.midTask1Size_max =  Integer.valueOf(prop.getProperty("task.image.midTask1size.max")).intValue();
            this.midTask1Size_min =  Integer.valueOf(prop.getProperty("task.image.midTask1size.min")).intValue();
			this.midTask2Size_max =  Integer.valueOf(prop.getProperty("task.image.midTask2size.max")).intValue();
            this.midTask2Size_min =  Integer.valueOf(prop.getProperty("task.image.midTask2size.min")).intValue();
            this.datasize_max =  Integer.valueOf(prop.getProperty("task.image.datasize_max")).intValue();

            this.datasize_min =  Integer.valueOf(prop.getProperty("task.image.datasize_min")).intValue();
            this.midsize = Integer.valueOf(prop.getProperty("task.image.midTask1num")).intValue();
            this.midsize = Integer.valueOf(prop.getProperty("task.image.midTask2num")).intValue();


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
            ImageCompress.getInstance().setApl(apl);

            //各レベルでの，横方向へのタスク生成を行う
            for(int i=0; i < 4 ; i++){

		//スタートタスク
		if(i == 0) {
			long weight = this.startTaskSize;
			AbstractTask startTask = this.buildChildTask(true,weight);
              		ImageCompress.getInstance().getApl().addTask(startTask);


		}

		//中央タスク1(フレーム中の処理)
		if(i == 1) {
			//int midsize1 = 120;
			//3600個のタスクを追加。weightは1-100までの乱数
			//1秒に60フレーム、1分3600フレーム
			for(int j = 0; j < midsize1; j++) {
			//AbstractTask midTask = this.buildChildTask(true,(long)(Math.random() * 100) + 10);
            AbstractTask midTask1 = this.buildChildTask(true, (long)this.generateLongValue(this.midTaskSize_min, this.midTaskSize_max));
            ImageCompress.getInstance().getApl().addTask(midTask1);
			}
		}
		//中央タスク2(フレーム間の処理)
		if(i == 2) {
			//int midsize2 = 119;
			//3598個のタスクを追加。weightは1-100までの乱数
			//1秒に60フレーム、1分3600フレーム
			for(int j = 0; j < midsize2; j++) {
			//AbstractTask midTask = this.buildChildTask(true,(long)(Math.random() * 100) + 10);
            AbstractTask midTask2 = this.buildChildTask(true, (long)this.generateLongValue(this.midTaskSize_min, this.midTaskSize_max));
            ImageCompress.getInstance().getApl().addTask(midTask2);
			}
		}

		//エンドタスク
		if(i == 3) {

			long weight = this.endTaskSize;
			AbstractTask endTask = this.buildChildTask(true,weight);
              		ImageCompress.getInstance().getApl().addTask(endTask);
		}



            }

		System.out.println("タスク数は"+ImageCompress.getInstance().getApl().getTaskList().size()) ;
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
        BBTask upperTask = ImageCompress.getInstance().getApl();
        Hashtable<Long, AbstractTask> tmpTaskList = upperTask.getTaskList();

        int tasknum = tmpTaskList.size();

        //START, ENDを決める．
        upperTask.setStartTask(upperTask.getTaskList().get(new Long(1)).getIDVector());
       // AbstractTask end = upperTask.getTaskList().getLast();
        int size = upperTask.getTaskList().size();
        AbstractTask end = upperTask.getTaskList().get(new Long(size));

        //もしENDが条件分岐であれば，BasicBlockに変更する．
        if (end.getType() == Constants.TYPE_CONDITION) {
            //ImageCompress.getInstance().findTask(end.getIDVector()).setType(Constants.TYPE_BASIC_BLOCK);
            upperTask.findTaskByLastID(end.getIDVector().get(1)).setType(Constants.TYPE_BASIC_BLOCK);
        }

        upperTask.setEndTask(end.getIDVector());
        //START: 入力辺がない
        //END: 出辺がない．

	 //スタートタスク-中央タスク間のDD定義
	 AbstractTask startTask = upperTask.findTaskByLastID(new Long(1));
        for(int i=2;i<this.midsize1+2;i++){

            AbstractTask midTask1 = upperTask.findTaskByLastID(new Long(i));
           // long dataSize = (int)(Math.random() * 100) + 10; //1-100
            long dataSize = (long)this.generateLongValue(this.datasize_min, this.datasize_max);
            DataDependence dd = new DataDependence(startTask.getIDVector(), midTask1.getIDVector(), 0, 0, 0);
	        //引数は(矢印元タスクID,矢印先タスクID,0,0,0)
            //long datasize = this.generateLongValue(this.ddedge_sizemin, this.ddedge_sizemax);
            dd.setMaxDataSize(dataSize);
            dd.setAveDataSize(dataSize);
            dd.setMinDataSize(dataSize);
            //task/sucTaskにそれぞれ依存関係を組み込む．
            startTask.addDsuc(dd);
            midTask1.addDpred(dd);

        }


	//中央タスク間のDD定義
	AbstractTask midTask = upperTask.findTaskByLastID(new Long(midsize+1));
        	for(int i=2;i<this.midsize1+2;i++){
        		if(i == 2){
				AbstractTask midTask1 = upperTask.findTaskByLastID(new Long(i));
            	AbstractTask midTask2 = upperTask.findTaskByLastID(new Long(i+midsize1));
            	long dataSize = 50;
            	DataDependence dd = new DataDependence(midTask1.getIDVector(),midTask2.getIDVector(), 0, 0, 0);
	    		//引数は(矢印元タスクID,矢印先タスクID,0,0,0)
            	//long datasize = this.generateLongValue(this.ddedge_sizemin, this.ddedge_sizemax);
            	dd.setMaxDataSize(dataSize);
            	dd.setAveDataSize(dataSize);
            	dd.setMinDataSize(dataSize);
            	//task/sucTaskにそれぞれ依存関係を組み込む．
            	midTask1.addDsuc(dd);
            	midTask2.addDpred(dd);
        		}else if(i == midsize1+1){
            	//中間タスク <->中間タスクの依存関係の定義
            	AbstractTask midTask1 = upperTask.findTaskByLastID(new Long(i));
            	AbstractTask midTask2 = upperTask.findTaskByLastID(new Long(1+midsize1+midsize2));
            	long dataSize = 50;
            	DataDependence dd = new DataDependence(midTask1.getIDVector(),midTask2.getIDVector(), 0, 0, 0);
	    		//引数は(矢印元タスクID,矢印先タスクID,0,0,0)
            	//long datasize = this.generateLongValue(this.ddedge_sizemin, this.ddedge_sizemax);
            	dd.setMaxDataSize(dataSize);
            	dd.setAveDataSize(dataSize);
            	dd.setMinDataSize(dataSize);
            	//task/sucTaskにそれぞれ依存関係を組み込む．
            	midTask1.addDsuc(dd);
            	midTask2.addDpred(dd);
            	}else{
            	AbstractTask midTask1 = upperTask.findTaskByLastID(new Long(i+1));
          		AbstractTask midTask2 = upperTask.findTaskByLastID(new Long(i+midsize1));

                        AbstractTask midTask2a = upperTask.findTaskByLastID(new Long(i+midsize1+1));

                               long dataSize = 50;
                                           	DataDependence dd = new DataDependence(midTask1.getIDVector(),midTask2.getIDVector(), 0, 0, 0);
                               	    		//引数は(矢印元タスクID,矢印先タスクID,0,0,0)
                                           	//long datasize = this.generateLongValue(this.ddedge_sizemin, this.ddedge_sizemax);
                                           	dd.setMaxDataSize(dataSize);
                                           	dd.setAveDataSize(dataSize);
                                           	dd.setMinDataSize(dataSize);
                                           	//task/sucTaskにそれぞれ依存関係を組み込む．
                                           	midTask1.addDsuc(dd);
                                           	midTask2.addDpred(dd);
                                           	midTask2a.addDpred(dd);





        }

  }

	//中央タスク-エンドタスク間のDD定義
	AbstractTask endTask = upperTask.findTaskByLastID(new Long(midsize1+midsize2+2));

        for(int i=2+midsize1; i<this.midsize1+midsize2+2;i++){
            //中央タスク <->エンドタスクの依存関係の定義
            AbstractTask midTask2 = upperTask.findTaskByLastID(new Long(i));
            long dataSize = 50;
            DataDependence dd = new DataDependence(midTask2.getIDVector(),endTask.getIDVector(), 0, 0, 0);
            //引数は(矢印元タスクID,矢印先タスクID,0,0,0)
            //long datasize = this.generateLongValue(this.ddedge_sizemin, this.ddedge_sizemax);
            dd.setMaxDataSize(dataSize);
            dd.setAveDataSize(dataSize);
            dd.setMinDataSize(dataSize);
            //task/sucTaskにそれぞれ依存関係を組み込む．
            midTask2.addDsuc(dd);
            endTask.addDpred(dd);

        }

        BBTask task = (BBTask)AplOperator.getInstance().calculateInstructions(upperTask);
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
            AbstractTask dpredTask = ImageCompress.getInstance().getApl().findTaskByLastID(dd.getFromID().get(1));
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
