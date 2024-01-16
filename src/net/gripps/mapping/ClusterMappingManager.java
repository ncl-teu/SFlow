package net.gripps.mapping;

import net.gripps.clustering.algorithms.mwsl_delta.CMWSL_Algorithm;
import net.gripps.clustering.algorithms.mwsl_delta.MWSL_delta_hetero;
import net.gripps.clustering.common.aplmodel.BBTask;
import net.gripps.clustering.common.aplmodel.TaskCluster;
import net.gripps.clustering.common.aplmodel.AbstractTask;
import net.gripps.clustering.common.aplmodel.DataDependence;
import net.gripps.clustering.algorithms.AbstractClusteringAlgorithm;
import net.gripps.environment.CPU;
import net.gripps.environment.Environment;
import net.gripps.environment.P2PEnvironment;
import net.gripps.scheduling.algorithms.Blevel_FirstScheduling;
import net.gripps.scheduling.algorithms.Tlevel_FirstScheduling;
import net.gripps.scheduling.algorithms.RCP_Scheduling;
import net.gripps.scheduling.algorithms.WorstBlevel_FirstScheduling;
import net.gripps.mapping.LB.LB_Mapping;
import net.gripps.mapping.CHP.CHP_Mapping;
import net.gripps.mapping.Tlevel.Tlevel_Mapping;
import net.gripps.mapping.CTM.CTM_Mapping;
//import net.gripps.mapping.level.*;
import net.gripps.mapping.level.Level_Mapping;
import net.gripps.scheduling.algorithms.heterogeneous.HeteroBlevelScheduling;
import net.gripps.scheduling.algorithms.heterogeneous.HeteroTlevelBlevelScheduling;
import net.gripps.scheduling.algorithms.heterogeneous.mwsl.MWSL_RCPScheduling;
import net.gripps.scheduling.algorithms.heterogeneous.mwsl.MWSL_Scheduling;
import net.gripps.util.EnvLoader;


import java.util.Iterator;
import java.util.Properties;
import java.util.LinkedList;
import java.util.Hashtable;
import java.io.FileInputStream;

/**
 * Author: H. Kanemitsu
 * Date: 2008/10/24
 * <p/>
 * タスククラスタを各計算機へ割り当てるための管理クラスです．
 * - Homogeneousの場合: ファイルから「最低スペックのマシン」の情報を読み取り，
 * タスククラスタ数分の均一なマシンリストを生成する．そして値を割り当てる．
 * - Heterogeneousの場合: ファイルから「マシンスペックの範囲」の情報を読み取り，
 * ランダムで各計算機のスペックを定義する．
 */
public class ClusterMappingManager extends AbstractClusteringAlgorithm {
    /**
     *
     */
    private BBTask retApl;

    /**
     *
     */
    private int mappingMode;

    /**
     *
     */
    private String file;

    /**
     *
     */
    private Environment env;

    /**
     *
     */
    private P2PEnvironment p2penv;

    /**
     *
     */
    private int machineNum;

    /**
     *
     */
    private int scheduleMode;

    private int mode;

    private int mode_p2p;

    private int hetero_clusteringmode;

    private int coreNumRate;




    public ClusterMappingManager(String file, BBTask apl, String path){
        super(file,apl);
        this.retApl = apl;
        this.file = file;

        //this.p2penv = new P2PEnvironment(this.file, num);
        //ピアグループを生成する．
        //this.p2penv.process(apl);

        this.file = file;
        try {
            Properties prop = new Properties();
            prop.load(new FileInputStream(this.file));
            //スケジューリング方針を設定する．
            this.scheduleMode = Integer.valueOf(prop.getProperty("algorithm.scheduling.using"));
            this.mappingMode = Integer.valueOf(prop.getProperty("mapping.using"));
            this.minSpeed = Integer.valueOf(prop.getProperty("cpu.speed.min")).intValue();
            this.minLink = Integer.valueOf(prop.getProperty("cpu.link.min")).intValue();
            this.mode = Integer.valueOf(prop.getProperty("network.peergroup.mode")).intValue();
            this.mode_p2p = Integer.valueOf(prop.getProperty("network.mode.p2p")).intValue();
            this.coreNumRate = Integer.valueOf(prop.getProperty("cpu.core.numRate")).intValue();

          //  this.machineNum = num/coreNumRate;

            //環境クラスを生成する．
            this.env = new EnvLoader(path, this.file);
            this.machineNum = env.getMachineList().size();

            if (this.mode_p2p == 1) {
                this.env = this.p2penv;
            }
            // this.hetero_clusteringmode =  Integer.valueOf(prop.getProperty("algorithm.clustering.hetero.using")).intValue();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
    /**
     * @param file
     * @param apl
     */
    public ClusterMappingManager(String file, BBTask apl, int num) {

        super(file, apl);
        int totalTaskNum = apl.getTaskList().size();

        //super();
        this.retApl = apl;
        this.file = file;

        //this.p2penv = new P2PEnvironment(this.file, num);
        //ピアグループを生成する．
        //this.p2penv.process(apl);

        this.file = file;
        try {
            Properties prop = new Properties();
            prop.load(new FileInputStream(this.file));
            //スケジューリング方針を設定する．
            this.scheduleMode = Integer.valueOf(prop.getProperty("algorithm.scheduling.using"));
            this.mappingMode = Integer.valueOf(prop.getProperty("mapping.using"));
            this.minSpeed = Integer.valueOf(prop.getProperty("cpu.speed.min")).intValue();
            this.minLink = Integer.valueOf(prop.getProperty("cpu.link.min")).intValue();
            this.mode = Integer.valueOf(prop.getProperty("network.peergroup.mode")).intValue();
            this.mode_p2p = Integer.valueOf(prop.getProperty("network.mode.p2p")).intValue();
            this.coreNumRate = Integer.valueOf(prop.getProperty("cpu.core.numRate")).intValue();

            this.machineNum = num/coreNumRate;

            //環境クラスを生成する．
            this.env = new Environment(this.file, num/coreNumRate);
            if (this.mode_p2p == 1) {
                this.env = this.p2penv;
            }
            // this.hetero_clusteringmode =  Integer.valueOf(prop.getProperty("algorithm.clustering.hetero.using")).intValue();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * ヘテロ環境における，タスククラスタリングのための
     * 制御用処理です．
     * まず，
     *
     * @return
     */
    public LinkedList<BBTask> heteroClusteringProcess() {
        LinkedList<BBTask> retList = new LinkedList<BBTask>();

        //ヘテロ環境でのタスククラスタリングでの処理

        //MWSL_delta_heteroを実行する．
        //これにより，クラスタ ⇔ プロセッサの対応付けができる．
        MWSL_delta_hetero hetero = new MWSL_delta_hetero(this.retApl, this.file, this.env);
        //結果を格納する．
        this.retApl = hetero.process();
        //結果をリストに格納する（意味ないが）
        retList.add(this.retApl);

        Iterator<BBTask> retIte = retList.iterator();
        LinkedList<BBTask> resultList = new LinkedList<BBTask>();


        while (retIte.hasNext()) {
            BBTask apl = retIte.next();
            //そして，スケジューリング処理
            switch (this.scheduleMode) {
                //Sarkar's Algorithm(blevel first)
                case 1:
                    Blevel_FirstScheduling sa = new Blevel_FirstScheduling(this.file, apl, this.env);
                    //Scheduled DAGを取得する．
                    apl = sa.schedule();
                    resultList.add(apl);
                    break;
                //Tlevel First
                case 2:
                    Tlevel_FirstScheduling ta = new Tlevel_FirstScheduling(this.file, apl, this.env);
                    apl = ta.schedule();
                    resultList.add(apl);

                    //RCP(Ready Critical Path: T.Yang's Algorithm)
                    break;
                case 3:
                    RCP_Scheduling rcp = new RCP_Scheduling(this.file, apl, this.env);
                    apl = rcp.schedule();
                    resultList.add(apl);
                    break;

                case 4:
                    WorstBlevel_FirstScheduling bcp = new WorstBlevel_FirstScheduling(this.file, apl, this.env);
                    apl = bcp.schedule();
                    resultList.add(apl);
                    break;
            }
        }
        return resultList;
    }

    public BBTask multiCoreProcess_all() {

        LinkedList<BBTask> retList = new LinkedList<BBTask>();
        CMWSL_Algorithm hetero = new CMWSL_Algorithm(this.retApl, this.file, this.env);
        //hetero.setDeltaStatic(true);

        hetero.setUpdateAll(true);
        //メイン処理を行う．
        this.retApl = hetero.process();
        retList.add(this.retApl);


        Iterator<BBTask> retIte = retList.iterator();
        LinkedList<BBTask> resultList = new LinkedList<BBTask>();

        //そして，スケジューリング処理
        switch (this.scheduleMode) {
            //Sarkar's Algorithm(blevel first)
            case 1:
                Blevel_FirstScheduling sa = new Blevel_FirstScheduling(this.file, retList.poll(), this.env);
                //Scheduled DAGを取得する．
                apl = sa.schedule();
                resultList.add(apl);
                break;
            //Tlevel First
            case 2:
                Tlevel_FirstScheduling ta = new Tlevel_FirstScheduling(this.file, apl, this.env);
                apl = ta.schedule();
                resultList.add(apl);

                //RCP(Ready Critical Path: T.Yang's Algorithm)
                break;
            case 3:
                RCP_Scheduling rcp = new RCP_Scheduling(this.file, retList.poll(), this.env);
                apl = rcp.schedule();
                resultList.add(apl);
                //System.out.println(apl.getMakeSpan());
                break;

            case 4:
                WorstBlevel_FirstScheduling bcp = new WorstBlevel_FirstScheduling(this.file, apl, this.env);
                apl = bcp.schedule();
                resultList.add(apl);
                break;
            case 5:
                MWSL_Scheduling mwsl = new MWSL_Scheduling(retList.poll(), this.file, this.env);
                apl = mwsl.process();
                resultList.add(apl);
                break;
            case 6:
                MWSL_RCPScheduling mwsl_rcp = new MWSL_RCPScheduling(retList.poll(), this.file, this.env);
                apl = mwsl_rcp.process();
                resultList.add(apl);
                break;
        }


        return resultList.poll();
    }

    public LinkedList<BBTask> multiCoreProcess2(){
         //マルチコア分散環境用の処理です．

        LinkedList<BBTask> resultList = new LinkedList<BBTask>();
                 LinkedList<BBTask> retList = new LinkedList<BBTask>();
                Environment env2 = (Environment)this.env.deepCopy();



                CMWSL_Algorithm hetero = new CMWSL_Algorithm(this.retApl, this.file, this.env);
                 //メイン処理を行う．
                 this.retApl = hetero.process();
        BBTask task2 =(BBTask)this.retApl.deepCopy();

                MWSL_Scheduling mwsl = new MWSL_Scheduling(this.retApl, this.file, this.env);
                 this.retApl = mwsl.process();
        System.out.println("MWSL_SCHED:"+"  RT:"+this.retApl.getMakeSpan()+ "WSL:"+this.retApl.getWorstLevel());
                // resultList.add( this.retApl);


                RCP_Scheduling rcp = new RCP_Scheduling(this.file, task2, env2);
                task2 = rcp.process();
        System.out.println("RCP_SCHED:" + "  RT:" + task2.getMakeSpan() + "WSL:" + task2.getWorstLevel());

                 return resultList;


    }



    //マルチコア分散環境用の処理です．
    public BBTask multiCoreProcess() {

        LinkedList<BBTask> retList = new LinkedList<BBTask>();
       CMWSL_Algorithm hetero = new CMWSL_Algorithm(this.retApl, this.file, this.env);
       //hetero.setDeltaStatic(true);

      //hetero.setUpdateAll(true);
        //メイン処理を行う．
        this.retApl = hetero.process();
        retList.add(this.retApl);



        Iterator<BBTask> retIte = retList.iterator();
        LinkedList<BBTask> resultList = new LinkedList<BBTask>();

        //そして，スケジューリング処理
        switch (this.scheduleMode) {
            //Sarkar's Algorithm(blevel first)
            case 1:
                Blevel_FirstScheduling sa = new Blevel_FirstScheduling(this.file, retList.poll(), this.env);
                //Scheduled DAGを取得する．
                apl = sa.schedule();
                resultList.add(apl);
                break;
            //Tlevel First
            case 2:
                Tlevel_FirstScheduling ta = new Tlevel_FirstScheduling(this.file, apl, this.env);
                apl = ta.schedule();
                resultList.add(apl);

                //RCP(Ready Critical Path: T.Yang's Algorithm)
                break;
            case 3:
                RCP_Scheduling rcp = new RCP_Scheduling(this.file, retList.poll(), this.env);
                apl = rcp.schedule();
                resultList.add(apl);
                //System.out.println(apl.getMakeSpan());
                break;

            case 4:
                WorstBlevel_FirstScheduling bcp = new WorstBlevel_FirstScheduling(this.file, apl, this.env);
                apl = bcp.schedule();
                resultList.add(apl);
                break;
            case 5:
                MWSL_Scheduling mwsl = new MWSL_Scheduling(retList.poll(), this.file, this.env);
                apl = mwsl.process();
                resultList.add(apl);
                break;
            case 6:
                MWSL_RCPScheduling mwsl_rcp = new MWSL_RCPScheduling(retList.poll(), this.file, this.env);
                apl = mwsl_rcp.process();
                resultList.add(apl);
                break;
            case 7:
                HeteroTlevelBlevelScheduling tbs = new HeteroTlevelBlevelScheduling(retList.poll(),this.file, this.env);
                apl = tbs.process();
                resultList.add(apl);
                break;
            case 8:
                HeteroBlevelScheduling bs = new HeteroBlevelScheduling(retList.poll(), this.file, this.env);
                apl = bs.process();
                resultList.add(apl);
                break;
        }


        return resultList.poll();

    }

    //マルチコア分散環境用の処理です．
        public BBTask multiCoreProcess_org() {

            LinkedList<BBTask> retList = new LinkedList<BBTask>();
           CMWSL_Algorithm hetero = new CMWSL_Algorithm(this.retApl, this.file, this.env);
           // hetero.setDeltaStatic(true);
          //hetero.setUpdateAll(true);
            //メイン処理を行う．
            this.retApl = hetero.process();
            retList.add(this.retApl);



            Iterator<BBTask> retIte = retList.iterator();
            LinkedList<BBTask> resultList = new LinkedList<BBTask>();

            //そして，スケジューリング処理
            switch (this.scheduleMode) {
                //Sarkar's Algorithm(blevel first)
                case 1:
                    Blevel_FirstScheduling sa = new Blevel_FirstScheduling(this.file, retList.poll(), this.env);
                    //Scheduled DAGを取得する．
                    apl = sa.schedule();
                    resultList.add(apl);
                    break;
                //Tlevel First
                case 2:
                    Tlevel_FirstScheduling ta = new Tlevel_FirstScheduling(this.file, apl, this.env);
                    apl = ta.schedule();
                    resultList.add(apl);

                    //RCP(Ready Critical Path: T.Yang's Algorithm)
                    break;
                case 3:
                    RCP_Scheduling rcp = new RCP_Scheduling(this.file, retList.poll(), this.env);
                    apl = rcp.schedule();
                    resultList.add(apl);
                    //System.out.println(apl.getMakeSpan());
                    break;

                case 4:
                    WorstBlevel_FirstScheduling bcp = new WorstBlevel_FirstScheduling(this.file, apl, this.env);
                    apl = bcp.schedule();
                    resultList.add(apl);
                    break;

                case 5:
                    MWSL_Scheduling mwsl = new MWSL_Scheduling(retList.poll(), this.file, this.env);
                    apl = mwsl.process();
                    resultList.add(apl);
                    break;

                case 6:
                    MWSL_RCPScheduling mwsl_rcp = new MWSL_RCPScheduling(retList.poll(), this.file, this.env);
                    apl = mwsl_rcp.process();
                    resultList.add(apl);
                    break;
            }


            return resultList.poll();

        }

    //マルチコア分散環境用の処理です．
    public BBTask multiCoreProcess_size(double rate) {

        LinkedList<BBTask> retList = new LinkedList<BBTask>();
        CMWSL_Algorithm hetero = new CMWSL_Algorithm(this.retApl, this.file, this.env);
        hetero.setSizeRate(rate);
        //hetero.setUpdateAll(true);
        //メイン処理を行う．
        this.retApl = hetero.process();
        retList.add(this.retApl);



        Iterator<BBTask> retIte = retList.iterator();
        LinkedList<BBTask> resultList = new LinkedList<BBTask>();

        //そして，スケジューリング処理
        switch (this.scheduleMode) {
            //Sarkar's Algorithm(blevel first)
            case 1:
                Blevel_FirstScheduling sa = new Blevel_FirstScheduling(this.file, retList.poll(), this.env);
                //Scheduled DAGを取得する．
                apl = sa.schedule();
                resultList.add(apl);
                break;
            //Tlevel First
            case 2:
                Tlevel_FirstScheduling ta = new Tlevel_FirstScheduling(this.file, apl, this.env);
                apl = ta.schedule();
                resultList.add(apl);

                //RCP(Ready Critical Path: T.Yang's Algorithm)
                break;
            case 3:
                RCP_Scheduling rcp = new RCP_Scheduling(this.file, retList.poll(), this.env);
                apl = rcp.schedule();
                resultList.add(apl);
                //System.out.println(apl.getMakeSpan());
                break;

            case 4:
                WorstBlevel_FirstScheduling bcp = new WorstBlevel_FirstScheduling(this.file, apl, this.env);
                apl = bcp.schedule();
                resultList.add(apl);
            case 5:
                MWSL_Scheduling mwsl = new MWSL_Scheduling(retList.poll(), this.file, this.env);
                apl = mwsl.process();
                resultList.add(apl);

            case 6:
                MWSL_RCPScheduling mwsl_rcp = new MWSL_RCPScheduling(retList.poll(), this.file, this.env);
                apl = mwsl_rcp.process();
                resultList.add(apl);
                break;
        }


        return resultList.poll();

    }



    /**
     *
     */
    public BBTask process() {

        //もし均一マシン環境であれば，マッピング処理は不要．
        if (!this.env.isMachineHetero()) {
            //各タスククラスタ，及び各マシンに，相互参照をつける．
            Iterator<TaskCluster> taskClusterIte = this.retApl.getTaskClusterList().values().iterator();
            int idx = 0;

            /***************マッピング処理*******************/
            //タスククラスタに対するループ
            while (taskClusterIte.hasNext()) {
                TaskCluster cluster = taskClusterIte.next();
                //当該タスククラスタに対し，マシンをセットする．
                CPU CPU = this.env.findCPU(new Long(idx));
                cluster.setCPU(CPU);
                //また，マシン側にはタスククラスタIDをセットしておく．
                CPU.setTaskClusterID(cluster.getClusterID());
                idx++;
            }
            /*************マッピング処理終了******************/
        } else {

            //もし提案手法（＝10）であれば，独自の処理を行う．
            if (this.mappingMode == 10) {
                MWSL_delta_hetero hetero = new MWSL_delta_hetero(this.retApl, this.file, this.env);
                //メイン処理を行う．
                this.retApl = hetero.process();

            } else {
                Iterator<TaskCluster> cIte = this.retApl.clusterIterator();
                while (cIte.hasNext()) {
                    TaskCluster cluster = cIte.next();
                    //当該タスククラスタに対し，マシンをセットする．
                    CPU CPU = new CPU(new Long(-1), this.minSpeed, null, null);
                    cluster.setCPU(CPU);
                    //bottomセットの更新を行う．
                    Iterator<Long> taskIte = cluster.getTaskSet().iterator();
                    //タスクごとのループ
                    while (taskIte.hasNext()) {
                        Long tID = taskIte.next();
                        AbstractTask task = this.retApl.findTaskByLastID(tID);
                        Iterator<DataDependence> dsucIte = task.getDsucList().iterator();
                        boolean isBottom = true;
                        while (dsucIte.hasNext()) {
                            DataDependence dd = dsucIte.next();
                            AbstractTask sucTask = this.retApl.findTaskByLastID(dd.getToID().get(1));
                            //異なる時点でアウト
                            if (sucTask.getClusterID().longValue() != cluster.getClusterID().longValue()) {
                                isBottom = false;
                                break;
                            }
                        }
                        if (isBottom) {
                            cluster.getBottomSet().add(tID);
                        }
                    }
                }

                //以下，レベル値の初期化を行う．
                //全タスクのtlevel, blevel値を-1とする．（再計算のため）
                Iterator<AbstractTask> taskIte = this.retApl.taskIerator();
                while (taskIte.hasNext()) {
                    AbstractTask task = taskIte.next();
                    task.setBlevel(-1);
                    task.setTlevel(-1);
                }

                //提案手法以外であれば，先にtlvel/blevelを設定させる．
                if (this.mappingMode != 10) {
                    //tlevel値の設定を行う．
                    long endID = this.retApl.getTaskList().size();
                    AbstractTask endTask = this.retApl.findTaskByLastID(new Long(endID));
                    this.calculateInitialTlevel(endTask, false);

                    this.updateMinValus();
                    //そして，blevel値の設定を行う．
                    Iterator<Long> startIte = this.retApl.getStartTaskSet().iterator();
                    //STARTタスクに対するループ
                    while (startIte.hasNext()) {
                        Long sID = startIte.next();
                        AbstractTask sTask = this.retApl.findTaskByLastID(sID);
                        this.calculateInitialBlevel(sTask, false);
                    }
                }


                //もしヘテロ環境であれば，マッピング処理が必要．
                switch (this.mappingMode) {
                    //Load Balancing Mapping
                    case 1:
                        LB_Mapping lbmapping = new LB_Mapping(this.retApl, this.file, this.env);
                        this.retApl = lbmapping.mapping();
                        break;
                    //CHP(Blevelの大きい順)
                    case 2:
                        CHP_Mapping chp_mapping = new CHP_Mapping(this.retApl, this.file, this.env);
                        this.retApl = chp_mapping.mapping();
                        break;
                    //Tlevelの小さい順
                    case 3:
                        Tlevel_Mapping tlevel_mapping = new Tlevel_Mapping(this.retApl, this.file, this.env);
                        this.retApl = tlevel_mapping.mapping();
                        break;
                    /*
                    case 10:
                        MWSL_delta_hetero hetero = new MWSL_delta_hetero(this.retApl, this.file, this.env);
                        this.retApl = hetero.process();
                     */
                    default:
                        break;

                }

            }


        }

        //そして，スケジューリング処理
        switch (this.scheduleMode) {
            //Sarkar's Algorithm(blevel first)
            case 1:
                Blevel_FirstScheduling sa = new Blevel_FirstScheduling(this.file, this.retApl, this.env);
                //Scheduled DAGを取得する．
                this.retApl = sa.schedule();

                break;
            //Tlevel First
            case 2:
                Tlevel_FirstScheduling ta = new Tlevel_FirstScheduling(this.file, this.retApl, this.env);
                this.retApl = ta.schedule();

                //RCP(Ready Critical Path: T.Yang's Algorithm)
                break;
            case 3:
                RCP_Scheduling rcp = new RCP_Scheduling(this.file, this.retApl, this.env);
                this.retApl = rcp.schedule();
                break;

            case 4:
                WorstBlevel_FirstScheduling bcp = new WorstBlevel_FirstScheduling(this.file, this.retApl, this.env);
                this.retApl = bcp.schedule();
                break;
        }

        return this.retApl;

    }

    /**
     * ピアグルーピングの処理です．
     *
     * @return
     */
    public LinkedList<BBTask> groupingProcess() {
        LinkedList<BBTask> retList = new LinkedList<BBTask>();
        if (!this.env.isMachineHetero()) {

        } else {
            //ヘテロ環境ならば，ピアグルーピング処理を行う．
            Iterator<TaskCluster> cIte = this.retApl.clusterIterator();
            while (cIte.hasNext()) {
                TaskCluster cluster = cIte.next();
                //当該タスククラスタに対し，マシンをセットする．
                CPU CPU = new CPU(new Long(-1), this.minSpeed, null, null);
                cluster.setCPU(CPU);
                //bottomセットの更新を行う．
                Iterator<Long> taskIte = cluster.getTaskSet().iterator();
                //タスクごとのループ
                while (taskIte.hasNext()) {
                    Long tID = taskIte.next();
                    AbstractTask task = this.retApl.findTaskByLastID(tID);
                    Iterator<DataDependence> dsucIte = task.getDsucList().iterator();
                    boolean isBottom = true;
                    while (dsucIte.hasNext()) {
                        DataDependence dd = dsucIte.next();
                        AbstractTask sucTask = this.retApl.findTaskByLastID(dd.getToID().get(1));
                        //異なる時点でアウト
                        if (sucTask.getClusterID().longValue() != cluster.getClusterID().longValue()) {
                            isBottom = false;
                            break;
                        }
                    }
                    if (isBottom) {
                        cluster.getBottomSet().add(tID);
                    }
                }
            }

            //以下，レベル値の初期化を行う．
            //全タスクのtlevel, blevel値を-1とする．（再計算のため）
            Iterator<AbstractTask> taskIte = this.retApl.taskIerator();
            while (taskIte.hasNext()) {
                AbstractTask task = taskIte.next();
                task.setBlevel(-1);
                task.setTlevel(-1);
            }

            //tlevel値の設定を行う．
            long endID = this.retApl.getTaskList().size();
            AbstractTask endTask = this.retApl.findTaskByLastID(new Long(endID));
            this.calculateInitialTlevel(endTask, false);

            //そして，blevel値の設定を行う．
            Iterator<Long> startIte = this.retApl.getStartTaskSet().iterator();
            //STARTタスクに対するループ
            while (startIte.hasNext()) {
                Long sID = startIte.next();
                AbstractTask sTask = this.retApl.findTaskByLastID(sID);
                this.calculateInitialBlevel(sTask, false);
            }

            /*マッピング処理*/

            //p2p環境において，
            //マシンリスト，通信関連の情報を更新する．
            //モードに応じて，マッピング処理を行う．
            //全処理 or 個別処理
            switch (this.mode) {
                case 1:
                    BBTask apld = this.doMapping(this.p2penv.getHopDPeerGroup());
                    retList.add(apld);
                    break;
                case 2:
                    BBTask aplp = this.doMapping(this.p2penv.getHopPeerGroup());
                    retList.add(aplp);
                    break;
                case 3:
                    BBTask aplg = this.doMapping(this.p2penv.getGPeerGroup());
                    retList.add(aplg);
                    break;
                default:
                    BBTask apl1 = this.doMapping(this.p2penv.getHopDPeerGroup());
                    retList.add(apl1);
                    BBTask apl2 = this.doMapping(this.p2penv.getHopPeerGroup());
                    retList.add(apl2);
                    BBTask apl3 = this.doMapping(this.p2penv.getGPeerGroup());
                    retList.add(apl3);
                    break;
            }

            Iterator<BBTask> retIte = retList.iterator();
            LinkedList<BBTask> resultList = new LinkedList<BBTask>();

            /*スケジューリング処理*/
            while (retIte.hasNext()) {
                BBTask apl = retIte.next();
                //RCP(Ready Critical Path: T.Yang's Algorithm)
                RCP_Scheduling rcp = new RCP_Scheduling(this.file, apl, this.env);
                apl = rcp.schedule();
                resultList.add(apl);

            }
            return resultList;
        }
        return null;
    }

    public BBTask doMapping(Hashtable<Long, CPU> list) {
        this.p2penv.setCpuList(list);
        //さらに，転送速度を更新する．
        BBTask apl = (BBTask) this.retApl.deepCopy();
        //CHP(Blevelの大きい順)
        //このときに，各ピアグループからEnvironmentを生成する．
        CHP_Mapping chp_mapping = new CHP_Mapping(apl, this.file, this.p2penv);

        return chp_mapping.mapping();
    }

    /**
     * マッピング処理同士の比較を行うためのメソッドです．
     *
     * @return
     */
    public LinkedList<BBTask> allProcess() {
        LinkedList<BBTask> retList = new LinkedList<BBTask>();

        //もし均一マシン環境であれば，マッピング処理は不要．
        if (!this.env.isMachineHetero()) {
            //各タスククラスタ，及び各マシンに，相互参照をつける．
            Iterator<TaskCluster> taskClusterIte = this.retApl.getTaskClusterList().values().iterator();
            int idx = 0;

            /***************マッピング処理*******************/
            //タスククラスタに対するループ
            while (taskClusterIte.hasNext()) {
                TaskCluster cluster = taskClusterIte.next();
                //当該タスククラスタに対し，マシンをセットする．
                CPU CPU = this.env.findCPU(new Long(idx));
                cluster.setCPU(CPU);
                //また，マシン側にはタスククラスタIDをセットしておく．
                CPU.setTaskClusterID(cluster.getClusterID());
                idx++;

            }
            /*************マッピング処理終了******************/
        } else {

            Iterator<TaskCluster> cIte = this.retApl.clusterIterator();
            while (cIte.hasNext()) {
                TaskCluster cluster = cIte.next();
                //当該タスククラスタに対し，マシンをセットする．
                CPU CPU = new CPU(new Long(-1), this.minSpeed, null, null);
                cluster.setCPU(CPU);
                //bottomセットの更新を行う．
                Iterator<Long> taskIte = cluster.getTaskSet().iterator();
                //タスクごとのループ
                while (taskIte.hasNext()) {
                    Long tID = taskIte.next();
                    AbstractTask task = this.retApl.findTaskByLastID(tID);
                    Iterator<DataDependence> dsucIte = task.getDsucList().iterator();
                    boolean isBottom = true;
                    while (dsucIte.hasNext()) {
                        DataDependence dd = dsucIte.next();
                        AbstractTask sucTask = this.retApl.findTaskByLastID(dd.getToID().get(1));
                        //異なる時点でアウト
                        if (sucTask.getClusterID().longValue() != cluster.getClusterID().longValue()) {
                            isBottom = false;
                            break;
                        }
                    }
                    if (isBottom) {
                        cluster.getBottomSet().add(tID);
                    }
                }

            }

            //以下，レベル値の初期化を行う．
            //全タスクのtlevel, blevel値を-1とする．（再計算のため）
            Iterator<AbstractTask> taskIte = this.retApl.taskIerator();
            while (taskIte.hasNext()) {
                AbstractTask task = taskIte.next();
                task.setBlevel(-1);
                task.setTlevel(-1);
            }

            //tlevel値の設定を行う．
            long endID = this.retApl.getTaskList().size();
            AbstractTask endTask = this.retApl.findTaskByLastID(new Long(endID));
            this.calculateInitialTlevel(endTask, false);

            //そして，blevel値の設定を行う．
            Iterator<Long> startIte = this.retApl.getStartTaskSet().iterator();
            //STARTタスクに対するループ
            while (startIte.hasNext()) {
                Long sID = startIte.next();
                AbstractTask sTask = this.retApl.findTaskByLastID(sID);
                this.calculateInitialBlevel(sTask, false);
            }

            //もしヘテロ環境であれば，マッピング処理が必要．
            switch (this.mappingMode) {
                //Load Balancing Mapping
                case 1:
                    LB_Mapping lbmapping = new LB_Mapping(this.retApl, this.file, this.env);
                    this.retApl = lbmapping.mapping();
                    retList.add(this.retApl);

                    break;
                //CHP(Blevelの大きい順)
                case 2:
                    CHP_Mapping chp_mapping = new CHP_Mapping(this.retApl, this.file, this.env);
                    this.retApl = chp_mapping.mapping();
                    retList.add(this.retApl);
                    break;
                //Tlevelの小さい順
                case 3:
                    Tlevel_Mapping tlevel_mapping = new Tlevel_Mapping(this.retApl, this.file, this.env);
                    this.retApl = tlevel_mapping.mapping();
                    retList.add(this.retApl);
                    break;
                //CTM(Communication Traffic Minimizing)
                case 4:
                    CTM_Mapping ctm_mapping = new CTM_Mapping(this.retApl, this.file, this.env);
                    this.retApl = ctm_mapping.mapping();
                    retList.add(this.retApl);
                    break;
                //すべての方針
                case 7:
                    LinkedList<Environment> eList = new LinkedList<Environment>();
                    for (int i = 0; i < 5; i++) {
                        Environment env1 = new Environment(this.file, this.machineNum);
                        eList.add(env1);

                    }

                    BBTask apl = (BBTask) this.retApl.deepCopy();
                    LB_Mapping lbmapping2 = new LB_Mapping(apl, this.file, eList.get(0));

                    apl = lbmapping2.mapping();
                    retList.add(apl);


                    BBTask apl2 = (BBTask) this.retApl.deepCopy();
                    CHP_Mapping chp_mapping2 = new CHP_Mapping(apl2, this.file, eList.get(1));
                    apl2 = chp_mapping2.mapping();
                    retList.add(apl2);


                    BBTask apl3 = (BBTask) this.retApl.deepCopy();
                    Tlevel_Mapping tlevel_mapping2 = new Tlevel_Mapping(apl3, this.file, eList.get(2));
                    apl3 = tlevel_mapping2.mapping();
                    retList.add(apl3);

                    BBTask apl4 = (BBTask) this.retApl.deepCopy();
                    CTM_Mapping ctm_mapping2 = new CTM_Mapping(apl4, this.file, eList.get(3));
                    apl4 = ctm_mapping2.mapping();
                    retList.add(apl4);

                    BBTask apl5 = (BBTask) this.retApl.deepCopy();
                    Level_Mapping level_mapping2;
                    level_mapping2 = new Level_Mapping(apl4, this.file, eList.get(3));
                    apl5 = level_mapping2.mapping();
                    retList.add(apl5);

                    Iterator<BBTask> retIte = retList.iterator();
                    LinkedList<BBTask> resultList = new LinkedList<BBTask>();
                    int idx = 0;
                    while (retIte.hasNext()) {
                        BBTask aplTmp = retIte.next();

                        //そして，スケジューリング処理
                        switch (this.scheduleMode) {
                            //Sarkar's Algorithm(blevel first)
                            case 1:
                                Blevel_FirstScheduling sa = new Blevel_FirstScheduling(this.file, aplTmp, eList.get(idx));
                                //Scheduled DAGを取得する．
                                apl = sa.schedule();
                                resultList.add(apl);
                                break;
                            //Tlevel First
                            case 2:
                                Tlevel_FirstScheduling ta = new Tlevel_FirstScheduling(this.file, aplTmp, eList.get(idx));
                                apl = ta.schedule();
                                resultList.add(apl);

                                //RCP(Ready Critical Path: T.Yang's Algorithm)
                                break;
                            case 3:
                                RCP_Scheduling rcp = new RCP_Scheduling(this.file, aplTmp, eList.get(idx));
                                apl = rcp.schedule();
                                resultList.add(apl);
                                break;

                            case 4:
                                WorstBlevel_FirstScheduling bcp = new WorstBlevel_FirstScheduling(this.file, aplTmp, eList.get(idx));
                                apl = bcp.schedule();
                                resultList.add(apl);
                                break;
                        }
                        idx++;

                    }
                    return resultList;

                default:
                    break;
            }
        }

        Iterator<BBTask> retIte = retList.iterator();
        LinkedList<BBTask> resultList = new LinkedList<BBTask>();


        while (retIte.hasNext()) {
            BBTask apl = retIte.next();
            //そして，スケジューリング処理
            switch (this.scheduleMode) {
                //Sarkar's Algorithm(blevel first)
                case 1:
                    Blevel_FirstScheduling sa = new Blevel_FirstScheduling(this.file, apl, this.env);
                    //Scheduled DAGを取得する．
                    apl = sa.schedule();
                    resultList.add(apl);
                    break;
                //Tlevel First
                case 2:
                    Tlevel_FirstScheduling ta = new Tlevel_FirstScheduling(this.file, apl, this.env);
                    apl = ta.schedule();
                    resultList.add(apl);

                    //RCP(Ready Critical Path: T.Yang's Algorithm)
                    break;
                case 3:
                    RCP_Scheduling rcp = new RCP_Scheduling(this.file, apl, this.env);
                    apl = rcp.schedule();
                    resultList.add(apl);
                    break;

                case 4:
                    WorstBlevel_FirstScheduling bcp = new WorstBlevel_FirstScheduling(this.file, apl, this.env);
                    apl = bcp.schedule();
                    resultList.add(apl);
                    break;
            }
        }
        return resultList;
    }

    public Environment getEnv() {
        return env;
    }

    public void setEnv(Environment env) {
        this.env = env;
    }
}
