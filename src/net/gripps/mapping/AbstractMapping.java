package net.gripps.mapping;

import net.gripps.environment.CPU;
import net.gripps.environment.Environment;
import net.gripps.clustering.common.aplmodel.BBTask;
import net.gripps.clustering.common.aplmodel.TaskCluster;
import net.gripps.clustering.common.aplmodel.CustomIDSet;
import net.gripps.clustering.common.aplmodel.AbstractTask;
import net.gripps.clustering.algorithms.AbstractClusteringAlgorithm;
import net.gripps.scheduling.algorithms.Tlevel_FirstScheduling;
import net.gripps.scheduling.algorithms.*;
import net.gripps.scheduling.algorithms.WorstBlevel_FirstScheduling;
import net.gripps.scheduling.AbstractTaskSchedulingAlgorithm;

import java.util.Properties;
import java.util.Iterator;
import java.util.HashMap;
import java.io.FileInputStream;

/**
 * Author: H. Kanemitsu
 * Date: 2008/11/01
 */
public  class AbstractMapping extends AbstractClusteringAlgorithm {
    /**
     *
     */
    protected Environment env;

    protected String fileName;

    protected int scheduleMode;

    protected int pShceduleMode;

    protected HashMap<String, AbstractTaskSchedulingAlgorithm> schedMap;




    protected CustomIDSet set = new CustomIDSet();

    /**
     * 未マップのクラスタ集合
     */
    protected CustomIDSet umClusterSet = new CustomIDSet();

    /**
     * 未マップのマシン集合
     */
    protected CustomIDSet unMappedCPU = new CustomIDSet();

    protected CustomIDSet mappedClusterSet = new CustomIDSet();

    /**
     * マッピング済みのCPU集合
     */
    protected CustomIDSet mappedCPU = new CustomIDSet();

    /**
     *
     */
    //protected BBTask retApl;

    public AbstractMapping(BBTask task, String file) {
        super(task, file, 1);
        this.fileName = file;
        this.schedMap = new HashMap<String, AbstractTaskSchedulingAlgorithm>();

        //this.lc = new LBC_LinearClustering(task, file);
    }

    /**
     *
     * @param apl
     * @param file
     * @param env
     */
    public AbstractMapping( BBTask apl,String file, Environment env) {
        super(file, apl);
        this.fileName = file;
         this.schedMap = new HashMap<String, AbstractTaskSchedulingAlgorithm>();
          try{
            Properties prop = new Properties();
            prop.load(new FileInputStream(this.fileName));
            //スケジューリング方針を設定する．
            this.scheduleMode = Integer.valueOf(prop.getProperty("algorithm.scheduling.using"));
            this.pShceduleMode = Integer.valueOf(prop.getProperty("algorithm.pscheduling.using"));

        }catch(Exception e){
            e.printStackTrace();
        }

        this.env = env;
        //APLの複製をここで生成する．
//2010.06.21 Kanemitsu START
       // this.retApl  = (BBTask) apl.deepCopy();
//2010.06.21 Kanemtisu END

        Iterator<TaskCluster> clusterIte = this.retApl.clusterIterator();
        while(clusterIte.hasNext()){
            TaskCluster cluster = clusterIte.next();
            this.umClusterSet.add(cluster.getClusterID());

        }
        //CPUを一時集合へセットする．
        Iterator<CPU> machineIte = env.getCpuList().values().iterator();
        while(machineIte.hasNext()){
            CPU CPU = machineIte.next();
            this.unMappedCPU.add(CPU.getCpuID());
        }

    }

    public Environment getEnv() {
        return env;
    }

    public void setEnv(Environment env) {
        this.env = env;
    }

    public BBTask getRetApl() {
        return retApl;
    }

    public void setRetApl(BBTask retApl) {
        this.retApl = retApl;

    }

    

    /**
     * 何らかの優先度に基づき，クラスタを選択します．
     * @return
     */
    public  TaskCluster selectTaskCluster(){
        return null;
    }

    /**
     * 何らかの優先度に基づき，マシンを選択します．
     * @return
     */
    public CPU selectMachine(){
        return null;
    }


    /**
     *
     * @return
     */
    public BBTask mapping(){
        
        return this.retApl;

    }

    /**
     * 指定スケジュールにて，メイクスパンを計算します．
     * スケジューリングを行い，指定クラスタ集合の中でのメイクスパン（クラスタのうちで完了時刻が最も大きいもの）
     * を算出します．
     *
     * メイクスパンを決めうるタスクは，bottomタスクであることに注意すべきである．
     * また，Insertion Techniqueも用いることとする．
     * 
     * @return
     */
    public long calcMakeSpan(BBTask apl, CustomIDSet set, Long cID){
        long pMakeSpan = 0;
        //そして，スケジューリング処理
        switch(this.pShceduleMode){
            
            //Sarkar's Algorithm(blevel first)
            //blevelの大きいものから順にスケジューリングを行う．
            case 1:
                Blevel_FirstScheduling sa = new Blevel_FirstScheduling(this.file, this.retApl, this.env);
                //Scheduled DAGを取得する．
                this.retApl = sa.schedule();

                break;
            //Tlevel First(used by DSC)
            case 2:
                 Tlevel_FirstScheduling ta = null;

                if(this.schedMap.containsKey("TLEVEL")){
                    ta = (Tlevel_FirstScheduling)this.schedMap.get("TLEVEL");

                }else{
                    ta = new Tlevel_FirstScheduling(this.file, this.retApl, this.env);
                }
              //  Tlevel_FirstScheduling ta = new Tlevel_FirstScheduling(this.file, this.retApl, this.env);

                pMakeSpan = ta.partialSchedule(apl, set, cID);

            //RCP(Ready Critical Path: T.Yang's Algorithm)
                break;
            case 3:
                RCP_Scheduling rcp = new RCP_Scheduling(this.file, this.retApl, this.env);
                this.retApl = rcp.schedule();
                pMakeSpan =  retApl.getMakeSpan();

                break;

            case 4:
                WorstBlevel_FirstScheduling bcp = new WorstBlevel_FirstScheduling(this.file, this.retApl, this.env);
                this.retApl = bcp.schedule();
                break;
        }

        return pMakeSpan;


    }

      /**
     * 定められたデータ転送速度上において，タスク間のデータ転送時間を計算します．
     *
     * @param fromTaskID
     * @param toTaskID
     * @return
     */
    public long getNWTime(Long fromTaskID, Long toTaskID, long data) {
        AbstractTask fromTask = this.retApl.findTaskByLastID(fromTaskID);
        TaskCluster fromCluster = this.retApl.findTaskCluster(fromTask.getClusterID());

        AbstractTask toTask = this.retApl.findTaskByLastID(toTaskID);
        TaskCluster toCluster = this.retApl.findTaskCluster(toTask.getClusterID());

        //もし双方のタスクが同じクラスタに属していれば，データ転送時間は0となる．
        if (fromTask.getClusterID().longValue() == toTask.getClusterID().longValue()) {
            return 0;
        } else if (fromCluster.getCPU().getCpuID().longValue() == toCluster.getCPU().getCpuID().longValue()) {
            return 0;
        } else {
            long link = 0;
            long fromID = fromCluster.getCPU().getCpuID().longValue();
            long toID = toCluster.getCPU().getCpuID().longValue();
            if((fromID == -1)||(toID == -1)){
                link = this.maxLink;
            }else{
                 /*link = this.env.getLink((int) fromCluster.getCPU().getCpuID().longValue(),
                         (int) toCluster.getCPU().getCpuID().longValue());
                         */
                link = this.env.getNWLink((int) fromCluster.getCPU().getCpuID().longValue(),
                                         (int) toCluster.getCPU().getCpuID().longValue());
            }

            return this.env.getSetupTime()+data / link;
        }

    }

    public CustomIDSet getMappedClusterSet() {
        return mappedClusterSet;
    }

    public void setMappedClusterSet(CustomIDSet mappedClusterSet) {
        this.mappedClusterSet = mappedClusterSet;
    }
}
