package net.gripps.clustering.tool;

import net.gripps.clustering.common.aplmodel.*;
import net.gripps.clustering.algorithms.ClusteringAlgorithmManager;
import net.gripps.clustering.algorithms.PostClusteringManager;
import net.gripps.mapping.ClusterMappingManager;

import java.util.*;
import java.io.FileInputStream;
import java.math.BigDecimal;


/**
 * プロセッサ数の最適性を調べるための実験プログラムです．
 *
 * Author: Hidehiro Kanemitsu<br>
 * Date: 2007/07/17
 */
public class Main_AplCreate_Gauss1 {
    public static void main(String[] args) {
        int ALGORITHM_NUM = 1;
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

        int taskNum = 0;

        long sum_ptime_lbc = 0;
        long sum_ptime_cass = 0;
        long sum_ptime_dsc = 0;
        long sum_ptime_lb = 0;
        LinkedList<StaObject> allList_cass = new LinkedList<StaObject>();

        LinkedList<StaObject> allList_dsc = new LinkedList<StaObject>();


        LinkedList<StaObject> allList_lb = new LinkedList<StaObject>();


        try {

            //クラスタリングアルゴリズムの実行回数
            //各ループでAPLは新規生成される．
            for (int i = 0; i < LOOP_NUM; i++) {
                //load property file from commnad line
                String filename = args[0];

                                Properties prop = new Properties();
                //create input stream from file
                prop.load(new FileInputStream(filename));

                int outDegree_Max = Integer.valueOf(prop.getProperty("task.ddedge.maxnum")).intValue();
                int edgeSize_Max = Integer.valueOf(prop.getProperty("task.ddedge.size.max")).intValue();
                int minSpeed = Integer.valueOf(prop.getProperty("cpu.speed.min")).intValue();

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
                    FFTOperator.getInstance().constructTask(filename);
                    //依存関係と，命令数を割り当てる．最下位にあるタスク命令数は決まっているので，読み込むのみ
                    BBTask APL = FFTOperator.getInstance().assignDependencyProcess();
                    apl = FFTOperator.getInstance().getApl();

                }


                BBTask LBaplCopy = (BBTask)apl;

                //DAGManager.getInstance().writeDAG(APL, "sample");
                //      BBTask APL = DAGManager.getInstance().readDAG("sample");
                //      AplOperator.getInstance().setApl(APL);

                //各種クラスタリングアルゴリズムを実行する
                LinkedList<BBTask> aplList = ClusteringAlgorithmManager.getInstance().process(filename);

                //SIRT-δのマシン数:
                int machineNum = aplList.get(0).getTaskClusterList().size();


                //LBC
                PostClusteringManager pMgr_LBC = new PostClusteringManager(aplList.get(0), filename, 0);
                BBTask retApl_lbc = pMgr_LBC.postClusteringLB(machineNum);
                aplList.set(0, retApl_lbc);

                //タスククラスタリングOnly処理結果も入れる．
          //      aplList.add(cassIIApl);
          //      aplList.add(DSCApl);


                Iterator<BBTask> aplIte2 = aplList.iterator();


                int idx = 1;
                BBTask apl_first = aplList.get(0);
                long opt_delta = apl_first.getOptDelta();

                System.out.print(apl_first.getTaskWeight() + ":" + outDegree_Max + ":" + edgeSize_Max + ":" + apl_first.getEdgeNum() + ":" + apl_first.getEdgeWeight() + ":");
                sum_taskweight += apl_first.getTaskWeight();
                sum_edgenum += apl_first.getEdgeNum();
                sum_edgeweight += apl_first.getEdgeWeight();
                double CCR = (double)apl_first.getEdgeWeight() / (double)apl_first.getTaskWeight();

                BigDecimal bd = new BigDecimal(String.valueOf(CCR));
                double CCR_RET = bd.setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();

                long ave_edge = apl_first.getEdgeWeight()/apl_first.getEdgeNum();
                long ave_task = apl_first.getTaskWeight()/apl_first.getTaskList().size();
                double CCR2 =  (double)ave_edge/ave_task;

                //スケジュール長の下限を求める．
                double low_schedule = (double)apl_first.getWcp()/apl_first.getTaskWeight();

                //CP率を求める
                double cp_rate = (double)apl_first.getCpLen()/apl_first.getTaskWeight();

                System.out.print("CCR: "+CCR_RET + ":" + "d_opt: "+opt_delta + ":" + apl_first.getMaxWeight() / minSpeed + ":" + apl_first.getCpLen() + ":");
                System.out.println("\nTaskNum:"+ apl_first.getTaskList().size());
                System.out.println("CCR2: "+ Calc.getRoundedValue(CCR2)+ " \nLowBound: "+ Calc.getRoundedValue(low_schedule)+"\n");
                System.out.println("CP_Rate: "+ Calc.getRoundedValue(cp_rate));

                sum_ccr += CCR_RET;
                sum_mkspan_machine += apl_first.getMaxWeight() / minSpeed;

                sum_cp += apl_first.getCpLen();

                sum_optDelta += apl_first.getOptDelta();

                taskNum = apl_first.getTaskList().size();

                //アルゴリズムごとのループ
                while (aplIte2.hasNext()) {
                    long cplen = 0;
                    BBTask apl2 = aplIte2.next();

                    ClusterMappingManager cm = new ClusterMappingManager(filename, apl2, apl2.getTaskClusterList().size());
                    apl2 = cm.process();


                    double LevelRate = (double) apl2.getWorstLevel() / (double) apl2.getCpLen();
                    BigDecimal bd2 = new BigDecimal(String.valueOf(LevelRate));
                    double ret_levelRate = bd2.setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();

                    double mkRate = (double) apl2.getMakeSpan() / ((double) apl2.getTaskWeight() / (double) minSpeed);
                   // double mkRate = (double) apl2.getMakeSpan() / (double) apl2.getCpLen();
                    BigDecimal bd3 = new BigDecimal(String.valueOf(mkRate));
                    double ret_mkRate = bd3.setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();


                    System.out.print("\nLEVEL: "+ret_levelRate + "\n:MAKESPAN: " + ret_mkRate + "\n:ClusterNUM: " + apl2.getTaskClusterList().size() + ":");
                    System.out.println("Makespan/CP: "+ Calc.getRoundedValue((double)apl2.getMakeSpan()/apl2.getCpLen()));
                    System.out.println("CP: "+ apl2.getCpLen() + "/WCP: "+apl2.getWcp());
                    double value = (double)apl2.getTaskWeight()/(apl2.getTaskClusterList().size()*apl2.getMakeSpan());

                    System.out.println("Efficiency: "+ Calc.getRoundedValue(value));
           

                   // System.out.println("****メイクスパン: "+ apl2.getMakeSpan());
                    idx++;

                }


                System.out.println();
            }





        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}