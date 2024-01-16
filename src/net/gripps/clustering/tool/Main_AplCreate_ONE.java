package net.gripps.clustering.tool;

import net.gripps.clustering.common.aplmodel.*;
import net.gripps.clustering.algorithms.ClusteringAlgorithmManager;
import net.gripps.clustering.algorithms.PostClusteringManager;
import net.gripps.mapping.ClusterMappingManager;

import java.util.*;
import java.io.FileInputStream;
import java.math.BigDecimal;


/**
 * Author: Hidehiro Kanemitsu<br>
 * Date: 2007/07/17
 */
public class Main_AplCreate_ONE {
    public static void main(String[] args) {
        int ALGORITHM_NUM = 1;
        int LOOP_NUM = 2;


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

        //double sum_level_lb = 0;
        double sum_mkspan_casssingle = 0;
        long sum_clusternum_casssingle = 0;

        //double sum_level_lb = 0;
        double sum_mkspan_dscsingle = 0;
        long sum_clusternum_dscsingle = 0;

        long sum_optDelta = 0;

        int taskNum = 0;

                long sum_ptime_lbc = 0;
        long sum_ptime_cass = 0;
        long sum_ptime_dsc = 0;
        long sum_ptime_lb = 0;


        try {
            //クラスタリングアルゴリズムの実行回数
            //各ループでAPLは新規生成される．
            for (int i = 0; i < LOOP_NUM; i++) {
                //load property file from commnad line
                String filename = args[0];

                                Properties prop = new Properties();
                //create input stream from file
                prop.load(new FileInputStream(filename));
                //int taskNum = Integer.valueOf(prop.getProperty("task.NumOfTask.max")).intValue();
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
                    FFTOperator.getInstance().constructTask(filename);
                    //依存関係と，命令数を割り当てる．最下位にあるタスク命令数は決まっているので，読み込むのみ
                    BBTask APL = FFTOperator.getInstance().assignDependencyProcess();
                    apl = FFTOperator.getInstance().getApl();

                }


                BBTask LBaplCopy = (BBTask)apl.deepCopy();

                //DAGManager.getInstance().writeDAG(APL, "sample");
                //      BBTask APL = DAGManager.getInstance().readDAG("sample");
                //      AplOperator.getInstance().setApl(APL);

                //各種クラスタリングアルゴリズムを実行する
                LinkedList<BBTask> aplList = ClusteringAlgorithmManager.getInstance().process(filename);

                //結果的に得られたAPLに対して，今度はスケジューリングを行う．
               // Iterator<BBTask> aplIte = aplList.iterator();

              // int machineNum = 100000;
                //マシン数:
                int machineNum = aplList.get(0).getTaskClusterList().size();

                //マシン数を決定させる．
                /*while (aplIte.hasNext()) {
                    BBTask apl = aplIte.next();
                    int num = apl.getTaskClusterList().size();
                    if (machineNum >= num) {
                        machineNum = num;
                    }

                }*/

                //LBC
                PostClusteringManager pMgr_LBC = new PostClusteringManager(aplList.get(0), filename, 0);
                BBTask retApl_lbc = pMgr_LBC.postClusteringLB(machineNum);
                aplList.set(0, retApl_lbc);



                //CASS-II
                PostClusteringManager pMgr_CASS = new PostClusteringManager(aplList.get(1), filename, 0, machineNum);
                BBTask retApl_cass = pMgr_CASS.postClusteringLB(machineNum);
                aplList.set(1,retApl_cass);


                //DSC
                PostClusteringManager pMgr_DSC = new PostClusteringManager(aplList.get(2), filename, 0, machineNum);
                BBTask retApl_dsc = pMgr_DSC.postClusteringWP2(machineNum);
                aplList.set(2,retApl_dsc);


                //LB
                PostClusteringManager pMgr_LB = new PostClusteringManager(LBaplCopy, filename, 0, machineNum);
                BBTask retApl_lb = pMgr_LB.process();
                aplList.add(retApl_lb);




                //タスククラスタリングOnly処理結果も入れる．
          //      aplList.add(cassIIApl);
          //      aplList.add(DSCApl);


                Iterator<BBTask> aplIte2 = aplList.iterator();


                int idx = 1;
                BBTask apl_first = aplList.get(0);
                long opt_delta = apl_first.getOptDelta();

                System.out.print(apl_first.getTaskList().size()+":"+apl_first.getTaskWeight() + ":" + outDegree_Max + ":" + edgeSize_Max + ":" + apl_first.getEdgeNum() + ":" + apl_first.getEdgeWeight() + ":");
                sum_taskweight += apl_first.getTaskWeight();
                sum_edgenum += apl_first.getEdgeNum();
                sum_edgeweight += apl_first.getEdgeWeight();

                double CCR = (double)apl_first.getEdgeWeight() / (double)apl_first.getTaskWeight();

                BigDecimal bd = new BigDecimal(String.valueOf(CCR));
                double CCR_RET = bd.setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
                System.out.print(CCR_RET + ":" + opt_delta + ":" + apl_first.getMaxWeight() / minSpeed + ":" + apl_first.getCpLen() + ":");

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


                    System.out.print(ret_levelRate + ":" + ret_mkRate + ":" + apl2.getTaskClusterList().size() + ":");
                    if (idx % ALGORITHM_NUM == 1) {
                        sum_level_lbc += ret_levelRate;
                        sum_mkspan_lbc += ret_mkRate;
                        sum_clusternum_lbc += apl2.getTaskClusterList().size();
                        if(i==0){

                        }else{
                          sum_ptime_lbc += apl2.getProcessTime();
                            System.out.print(":LBC:"+apl2.getProcessTime());
                        }

                    } else if (idx % ALGORITHM_NUM == 2) {
                        sum_level_cass += ret_levelRate;
                        sum_mkspan_cass += ret_mkRate;
                        sum_clusternum_cass += apl2.getTaskClusterList().size();
                        if(i==0){

                        }else{
                            sum_ptime_cass += apl2.getProcessTime();
                            System.out.print("CASS:"+apl2.getProcessTime());
                        }




                    } else if (idx % ALGORITHM_NUM == 3) {
                        sum_level_dsc += ret_levelRate;
                        sum_mkspan_dsc += ret_mkRate;
                        sum_clusternum_dsc += apl2.getTaskClusterList().size();
                        if(i==0){

                        }else{
                            sum_ptime_dsc += apl2.getProcessTime();
                            System.out.print("DSC:"+apl2.getProcessTime());
                        }



                    }else {
                        sum_level_lb += ret_levelRate;
                        sum_mkspan_lb += ret_mkRate;
                        sum_clusternum_lb += apl2.getTaskClusterList().size();
                        if(i==0){

                        }else{
                            sum_ptime_lb += apl2.getProcessTime();
                            System.out.print("LB:"+apl2.getProcessTime()+":");

                        }


                    }


                   // System.out.println("****メイクスパン: "+ apl2.getMakeSpan());
                    idx++;

                }


                System.out.println();
            }


            String filename = args[0];
            System.out.println("RESULT:");
            Properties prop = new Properties();
            //create input stream from file
            prop.load(new FileInputStream(filename));
            //int taskNum = Integer.valueOf(prop.getProperty("task.NumOfTask.max")).intValue();
            System.out.print(taskNum + ":");
            int taskSize_Min = Integer.valueOf(prop.getProperty("task.instructions.min")).intValue();
            int taskSize_Max = Integer.valueOf(prop.getProperty("task.instructions.max")).intValue();
            System.out.print(taskSize_Min + "-" + taskSize_Max + ":");

            int outDegree_Max = Integer.valueOf(prop.getProperty("task.ddedge.maxnum")).intValue();
            int edgeSize_Max = Integer.valueOf(prop.getProperty("task.ddedge.size.max")).intValue();
            int delta = Integer.valueOf(prop.getProperty("task.instructions.threshold")).intValue();
            int minSpeed = Integer.valueOf(prop.getProperty("cpu.speed.min")).intValue();
            System.out.print(sum_taskweight / LOOP_NUM + ":" + outDegree_Max + ":" + edgeSize_Max + ":" + sum_edgenum / LOOP_NUM + ":" + sum_edgeweight / LOOP_NUM + ":");

            double CCR2 = sum_ccr / LOOP_NUM;
            BigDecimal bd3 = new BigDecimal(String.valueOf(CCR2));
            double CCR_AVE = bd3.setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
            System.out.print("CCR/"+CCR_AVE + ":d_opt/" + sum_optDelta/LOOP_NUM + ":" + (sum_mkspan_machine / LOOP_NUM) / minSpeed + ":" + sum_cp / LOOP_NUM + ":");

            double LevelRateLBC = (sum_level_lbc) / LOOP_NUM;
            BigDecimal bdLBC = new BigDecimal(String.valueOf(LevelRateLBC));
            double ret_levelRateLBC = bdLBC.setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
            System.out.print("\nLevel(LBC): "+ret_levelRateLBC + "\n");

            double mkRateLBC = (sum_mkspan_lbc) / LOOP_NUM;
            BigDecimal bd2LBC = new BigDecimal(String.valueOf(mkRateLBC));
            double ret_mkRateLBC = bd2LBC.setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
            System.out.print("MSpan(LBC): "+ret_mkRateLBC + "\n" + "ClusterNum(LBC): "+sum_clusternum_lbc / LOOP_NUM + "\n");
            System.out.println();

            double LevelRateCASS = (sum_level_cass) / LOOP_NUM;
            BigDecimal bdLCASS = new BigDecimal(String.valueOf(LevelRateCASS));
            double ret_levelRateCASS = bdLCASS.setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
            System.out.print("Level(CASS): "+ret_levelRateCASS + "\n");


            double mkRateCASS = (sum_mkspan_cass) / LOOP_NUM;
            BigDecimal bd2CASS = new BigDecimal(String.valueOf(mkRateCASS));
            double ret_mkRateCASS = bd2CASS.setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
            System.out.print("MSpan(CASS): "+ret_mkRateCASS + "\n" +"ClusterNum(CASS): "+ sum_clusternum_cass / LOOP_NUM + "\n");
            System.out.println();

            double LevelRateDSC = (sum_level_dsc) / LOOP_NUM;
            BigDecimal bdDSC = new BigDecimal(String.valueOf(LevelRateDSC));
            double ret_levelRateDSC = bdDSC.setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
            System.out.print("Level(DSC): "+ret_levelRateDSC + "\n");

            double mkRateDSC = (sum_mkspan_dsc) / LOOP_NUM;
            BigDecimal bd2DSC = new BigDecimal(String.valueOf(mkRateDSC));
            double ret_mkRateDSC = bd2DSC.setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
            System.out.print("MSpan(DSC): "+ret_mkRateDSC + "\n" + "ClusterNum(DSC):"+sum_clusternum_dsc / LOOP_NUM + "\n");
            System.out.println();


            double LevelRateLB = (sum_level_lb) / LOOP_NUM;
            BigDecimal bdLB = new BigDecimal(String.valueOf(LevelRateLB));
            double ret_levelRateLB = bdLB.setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
            System.out.print("Level(LB): "+ret_levelRateLB + "\n");

            double mkRateLB = (sum_mkspan_lb) / LOOP_NUM;
            BigDecimal bd2LB = new BigDecimal(String.valueOf(mkRateLB));
            double ret_mkRateLB = bd2LB.setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
            System.out.print("MSpan(LB): "+ret_mkRateLB + "\n" + "ClusterNum(LB): "+sum_clusternum_lb / LOOP_NUM /*+ ":"*/);
            System.out.println();


            double mkoptDelta = (sum_optDelta) / LOOP_NUM;
            BigDecimal opt = new BigDecimal(String.valueOf(mkoptDelta));
            double ret_opt = opt.setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
            System.out.print( ":" + ret_opt );

              long ptime_lbc1 = (sum_ptime_lbc) / (LOOP_NUM-1);
                /* BigDecimal ptime_lbc2 = new BigDecimal(String.valueOf(ptime_lbc1));
                 double ptime_lbc3 = ptime_lbc2.setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
                 */
                 System.out.print( ":LBC:" + ptime_lbc1 );

                 long ptime_cass1 = (sum_ptime_cass)/(LOOP_NUM-1);
                 /*BigDecimal ptime_cass2 = new BigDecimal(String.valueOf(ptime_cass1));
                 double ptime_cass3 = ptime_cass2.setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
                 */
                 System.out.print( "CASS:" + ptime_cass1 );

                 long ptime_dsc1 = (sum_ptime_dsc) / (LOOP_NUM-1);
                /* BigDecimal ptime_dsc2 = new BigDecimal(String.valueOf(ptime_dsc1));
                 double ptime_dsc3 = ptime_dsc2.setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
                 */
                 System.out.print( "DSC:" + ptime_dsc1 );

                 long ptime_lb1 = (sum_ptime_lb) / (LOOP_NUM-1);
                 /*BigDecimal ptime_lb2 = new BigDecimal(String.valueOf(ptime_lb1));
                 double ptime_lb3 = ptime_lb2.setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
                 */
                 System.out.print( "LB:" + ptime_lb1 );







        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}