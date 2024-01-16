package net.gripps.clustering.tool;

import net.gripps.clustering.common.aplmodel.AplOperator;
import net.gripps.clustering.common.aplmodel.BBTask;
import net.gripps.clustering.common.aplmodel.TaskInfo;
import net.gripps.clustering.common.aplmodel.GaussianOperator;
import net.gripps.clustering.algorithms.ClusteringAlgorithmManager;
import net.gripps.mapping.ClusterMappingManager;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.text.DateFormat;


/**
 * Author: Hidehiro Kanemitsu<br>
 * Date: 2007/07/17
 * SIRT-δ_optによるタスククラスタリングの後，各種クラスタ割り当て方針
 * を行い，各種スケジューリングを行う．そして，それらのメイクスパンを比較する．
 */
public class Main_AplCreate2 {
    public static void main(String[] args) {
        int ALGORITHM_NUM = 4;
        int LOOP_NUM = 1;

        long sum_taskweight = 0;
        long sum_edgenum = 0;
        long sum_edgeweight = 0;
        double sum_ccr = 0;
        long sum_mkspan_machine = 0;
        long sum_cp = 0;

        double sum_level_lbc = 0;
        double sum_mkspan_lbc = 0;
        long sum_clusternum_lbc = 0;

        double sum_level_cass = 0;
        double sum_mkspan_cass = 0;
        long sum_clusternum_cass = 0;

        double sum_level_dsc = 0;
        double sum_mkspan_dsc = 0;
        long sum_clusternum_dsc = 0;

        double sum_level_lb = 0;
        double sum_mkspan_lb = 0;
        long sum_clusternum_lb = 0;

        long sum_optDelta = 0;

        try {
            //クラスタリングアルゴリズムの実行回数
            //各ループでAPLは新規生成される．
            for (int i = 0; i < LOOP_NUM; i++) {
                //load property file from commnad line
                String filename = args[0];
                //設定ファイルを読み込む
                Properties prop = new Properties();
                //create input stream from file
                prop.load(new FileInputStream(filename));
                int taskNum = Integer.valueOf(prop.getProperty("task.NumOfTask.max")).intValue();
                //System.out.print(taskNum + ":");
                int taskSize_Min = Integer.valueOf(prop.getProperty("task.instructions.min")).intValue();
                int taskSize_Max = Integer.valueOf(prop.getProperty("task.instructions.max")).intValue();
                //System.out.print(taskSize_Min + "-" + taskSize_Max + ":");

                int outDegree_Max = Integer.valueOf(prop.getProperty("task.ddedge.maxnum")).intValue();
                int edgeSize_Max = Integer.valueOf(prop.getProperty("task.ddedge.size.max")).intValue();
                int delta = Integer.valueOf(prop.getProperty("task.instructions.threshold")).intValue();
                int minSpeed = Integer.valueOf(prop.getProperty("cpu.speed.min")).intValue();
                int maxSpeed = Integer.valueOf(prop.getProperty("cpu.speed.max")).intValue();

                int dataMin = Integer.valueOf(prop.getProperty("task.ddedge.size.min")).intValue();
                int dataMax = Integer.valueOf(prop.getProperty("task.ddedge.size.max")).intValue();
                int linkMin = Integer.valueOf(prop.getProperty("cpu.link.min")).intValue();
                int linkMax = Integer.valueOf(prop.getProperty("cpu.link.max")).intValue();

                int dagtype = Integer.valueOf(prop.getProperty("task.dagtype")).intValue();

                BBTask apl = null;
                //1: random 2: GJ
                if(dagtype == 1){
                 //タスクグラフを構築する
                    AplOperator.getInstance().constructTask(filename);
                    //依存関係と，命令数を割り当てる．最下位にあるタスク命令数は決まっているので，読み込むのみ
                    BBTask APL = AplOperator.getInstance().assignDependencyProcess();
                    apl = AplOperator.getInstance().getApl();

                }else if(dagtype == 2){
                    GaussianOperator.getInstance().constructTask(filename);
                    //依存関係と，命令数を割り当てる．最下位にあるタスク命令数は決まっているので，読み込むのみ
                    BBTask APL = GaussianOperator.getInstance().assignDependencyProcess();
                    apl = GaussianOperator.getInstance().getApl();
                }else{

                }


                BBTask LBaplCopy = (BBTask) apl.deepCopy();

                //各種クラスタリングアルゴリズムを実行する
                //long start = System.currentTimeMillis();
                LinkedList<BBTask> aplList = ClusteringAlgorithmManager.getInstance().process(filename);
                //long end = System.currentTimeMillis();
                //System.out.println("TIME: "+ (end-start));

                //結果的に得られたAPLに対して，今度はスケジューリングを行う．
                Iterator<BBTask> aplIte = aplList.iterator();

                // int machineNum = 100000;
                //マシン数:
                int machineNum = aplList.get(0).getTaskClusterList().size();

                //SIRT-δ(まあ，一応・・・）
                /*PostClusteringManager pMgr_LBC = new PostClusteringManager(aplList.get(0), filename, 0);
                BBTask retApl_lbc = pMgr_LBC.postClusteringLB(machineNum);
                */
                aplList.set(0, aplList.get(0));


                Iterator<BBTask> aplIte2 = aplList.iterator();


                int idx = 1;
                BBTask apl_first = aplList.get(0);

                System.out.print(apl_first.getTaskWeight() + ":" + outDegree_Max + ":" + edgeSize_Max + ":" + apl_first.getEdgeNum() + ":" + apl_first.getEdgeWeight() + ":");
                sum_taskweight += apl_first.getTaskWeight();
                sum_edgenum += apl_first.getEdgeNum();
                sum_edgeweight += apl_first.getEdgeWeight();

                double CCR = apl_first.getEdgeWeight() / apl_first.getTaskWeight();
                BigDecimal bd = new BigDecimal(String.valueOf(CCR));
                double CCR_RET = bd.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                long opt_delta = apl_first.getOptDelta();

                double LevelRate = (double) apl_first.getWorstLevel() / (double) apl_first.getCpLen();
                BigDecimal bd2 = new BigDecimal(String.valueOf(LevelRate));
                double ret_levelRate = bd2.setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();

                double g_task_min = (double)taskSize_Min/(double)dataMax;
                BigDecimal g_task_mindecimal = new BigDecimal(String.valueOf(g_task_min));
                double g_task_min2 = g_task_mindecimal.setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();

                double g_task_max = (double)taskSize_Max/(double)dataMin;
                BigDecimal g_task_maxdecimal = new BigDecimal(String.valueOf(g_task_min));
                double g_task_max2 = g_task_maxdecimal.setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();

                double g_cpu_min = (double)minSpeed/(double)linkMax;
                 BigDecimal g_cpu_mindecimal = new BigDecimal(String.valueOf(g_task_min));
                double g_cpu_min2 = g_cpu_mindecimal.setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();

                double g_cpu_max =(double)maxSpeed/(double)linkMin;
                BigDecimal g_cpu_maxdecimal = new BigDecimal(String.valueOf(g_task_min));
                double g_cpu_max2 = g_cpu_maxdecimal.setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();

                System.out.print(CCR_RET + ":"  + apl_first.getMaxWeight() / maxSpeed + ":" + apl_first.getCpLen() + ":"+ret_levelRate + ":"+ apl_first.getTaskClusterList().size()+":"+opt_delta);
                System.out.print(":"+g_task_min+":"+g_task_max+":"+g_cpu_min+":"+g_cpu_max+":");
                sum_ccr += CCR_RET;
                sum_mkspan_machine += apl_first.getMaxWeight() / minSpeed;

                sum_cp += apl_first.getCpLen();

                sum_optDelta += apl_first.getOptDelta();

                //アルゴリズムごとのループ
                //この場合，SIRT-δによって生成されたクラスタに対して処理が実行される．
                //while (aplIte2.hasNext()) {
                    long cplen = 0;
                    BBTask apl2 = aplIte2.next();
                    
                    ClusterMappingManager cm = new ClusterMappingManager(filename, apl2, machineNum);
                    LinkedList<BBTask> retList = cm.allProcess();
                    Iterator<BBTask> retIte = retList.iterator();
                    //ここで出力する．
                    while(retIte.hasNext()){
                        BBTask dag = retIte.next();
                        double one_rate = apl_first.getMaxWeight()/maxSpeed;
                        double mkRate = dag.getMakeSpan() / one_rate;
                        //System.out.println("Max W:"+apl_first.getMaxWeight());
                        BigDecimal bd2DSC = new BigDecimal(String.valueOf(mkRate));
                        double ret_mkRate = bd2DSC.setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
                        System.out.print(ret_mkRate + ":");
                        //System.out.print( dag.getMakeSpan()+":");

                    }
                    //apl2 = cm.process();


                //}


                System.out.println();
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}