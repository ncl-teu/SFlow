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
public class Main_AplCreate_Optimal {
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

        double ccr2 = 0.0;
        double sum_ccr2 = 0.0;

        int taskNum = 0;

        long sum_ptime_lbc = 0;
        long sum_ptime_cass = 0;
        long sum_ptime_dsc = 0;
        long sum_ptime_lb = 0;
        LinkedList<StaObject> allList_cass = new LinkedList<StaObject>();

        LinkedList<StaObject> allList_dsc = new LinkedList<StaObject>();


        LinkedList<StaObject> allList_lb = new LinkedList<StaObject>();


        try {

           for(int k = 0; k < 9; k++){
                StaObject obj_cass =  new StaObject();
                StaObject obj_dsc = new StaObject();
                StaObject obj_lb = new StaObject();

                allList_cass.add(obj_cass);
                allList_dsc.add(obj_dsc);
                allList_lb.add(obj_lb);

            }

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


                BBTask LBaplCopy = (BBTask)apl.deepCopy();

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

                BBTask cassDAG = (BBTask)aplList.get(1).deepCopy();
                PostClusteringManager pMgr_CASS = new PostClusteringManager(aplList.get(1), filename, 0, machineNum);
                BBTask retApl_cass = pMgr_CASS.postClusteringLB(machineNum);
                aplList.set(1,retApl_cass);

                //DSC
                BBTask dscDAG = (BBTask)aplList.get(2).deepCopy();
                PostClusteringManager pMgr_DSC = new PostClusteringManager(aplList.get(2), filename, 0, machineNum);
                BBTask retApl_dsc = pMgr_DSC.postClusteringWP2(machineNum);
                aplList.set(2,retApl_dsc);


                //LB
                BBTask lbDAG =(BBTask)LBaplCopy.deepCopy();
                PostClusteringManager pMgr_LB = new PostClusteringManager(LBaplCopy, filename, 0, machineNum);
                BBTask retApl_lb = pMgr_LB.process();
                aplList.add(retApl_lb);


                //CASS-II
                //CASS-IIのマシン数を取得
                int machineNum_cass= cassDAG.getTaskClusterList().size();
                int machineNum_dsc = dscDAG.getTaskClusterList().size();

                
                for(int j=1;j < 10; j++){

                    int mNum = (Math.min(machineNum_cass,machineNum_dsc)*j)/15;

                    BBTask dag_cass = (BBTask)cassDAG.deepCopy();
                    PostClusteringManager pMgr_CASS2 = new PostClusteringManager(dag_cass, filename, 0, mNum);
                    BBTask retApl_cass2 = pMgr_CASS2.postClusteringLB(mNum);
                    //スケジューリング
                    ClusterMappingManager cm = new ClusterMappingManager(filename, retApl_cass2, retApl_cass2.getTaskClusterList().size());
                    retApl_cass2 = cm.process();
                    double level_cass = (double)retApl_cass2.getWorstLevel() / (double)retApl_cass2.getCpLen();
                    allList_cass.get(j-1).addLevelRate(Calc.getRoundedValue(level_cass));
                    double mk_cass = (double)retApl_cass2.getMakeSpan() / (double)retApl_cass2.getTaskWeight()/(double)minSpeed;
                    allList_cass.get(j-1).addMakeSpanRate(Calc.getRoundedValue(mk_cass));
                    allList_cass.get(j-1).addClusterNum(retApl_cass2.getTaskClusterList().size());



                    //DSC
                    BBTask dag_dsc = (BBTask)dscDAG.deepCopy();
                    PostClusteringManager pMgr_DSC2 = new PostClusteringManager(dag_dsc, filename, 0, mNum);
                    BBTask retApl_dsc2 = pMgr_DSC2.postClusteringWP2(mNum);
                    //スケジューリング
                    ClusterMappingManager cm2 = new ClusterMappingManager(filename, retApl_dsc2, retApl_dsc2.getTaskClusterList().size());
                    retApl_dsc2 = cm2.process();
                    double level_dsc = (double)retApl_dsc2.getWorstLevel() / (double)retApl_dsc2.getCpLen();
                    allList_dsc.get(j-1).addLevelRate(Calc.getRoundedValue(level_dsc));
                    double mk_dsc = (double)retApl_dsc2.getMakeSpan() / (double)retApl_cass2.getTaskWeight()/(double)minSpeed;
                    allList_dsc.get(j-1).addMakeSpanRate(Calc.getRoundedValue(mk_dsc));
                    allList_dsc.get(j-1).addClusterNum(retApl_dsc2.getTaskClusterList().size());



                    //LB
                    BBTask dag_lb = (BBTask)lbDAG.deepCopy();

                    PostClusteringManager pMgr_LB2 = new PostClusteringManager(dag_lb, filename, 0, mNum);
                    BBTask retApl_lb2 = pMgr_LB2.process();
                     //スケジューリング
                    ClusterMappingManager cm3 = new ClusterMappingManager(filename, retApl_lb2, retApl_lb2.getTaskClusterList().size());
                    retApl_lb2 = cm3.process();
                    double level_lb = (double)retApl_lb2.getWorstLevel() / (double)retApl_lb2.getCpLen();
                    allList_lb.get(j-1).addLevelRate(Calc.getRoundedValue(level_lb));
                    double mk_lb = (double)retApl_lb2.getMakeSpan() / (double)retApl_lb2.getTaskWeight()/(double)minSpeed;
                    allList_lb.get(j-1).addMakeSpanRate(Calc.getRoundedValue(mk_lb));
                    allList_lb.get(j-1).addClusterNum(retApl_lb2.getTaskClusterList().size());

                }

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

                double ave_task_tmp = (double)apl_first.getTaskWeight()/apl_first.getTaskList().size();
                double ave_task = Calc.getRoundedValue(ave_task_tmp);

                double ave_edge_tmp = (double)apl_first.getEdgeWeight()/apl_first.getEdgeNum();
                double ave_edge = Calc.getRoundedValue(ave_edge_tmp);

                sum_ccr2 += Calc.getRoundedValue(ave_edge/ave_task);

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
                        sum_ptime_lbc += apl2.getProcessTime();
                        System.out.print(":LBC:"+apl2.getProcessTime());

                    } else if (idx % ALGORITHM_NUM == 2) {
                        sum_level_cass += ret_levelRate;
                        sum_mkspan_cass += ret_mkRate;
                        sum_clusternum_cass += apl2.getTaskClusterList().size();
                         sum_ptime_cass += apl2.getProcessTime();
                        System.out.print("CASS:"+apl2.getProcessTime());



                    } else if (idx % ALGORITHM_NUM == 3) {
                        sum_level_dsc += ret_levelRate;
                        sum_mkspan_dsc += ret_mkRate;
                        sum_clusternum_dsc += apl2.getTaskClusterList().size();
                         sum_ptime_dsc += apl2.getProcessTime();
                        System.out.print("DSC:"+apl2.getProcessTime());


                    }else {
                        sum_level_lb += ret_levelRate;
                        sum_mkspan_lb += ret_mkRate;
                        sum_clusternum_lb += apl2.getTaskClusterList().size();
                        sum_ptime_lb += apl2.getProcessTime();
                        System.out.print("LB:"+apl2.getProcessTime()+":");

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
            System.out.print("CCR/"+CCR_AVE +"CCR2/ "+Calc.getRoundedValue(sum_ccr2/LOOP_NUM)+ ":d_opt/" + sum_optDelta/LOOP_NUM + ":" + (sum_mkspan_machine / LOOP_NUM) / minSpeed + ":" + sum_cp / LOOP_NUM + ":");


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

            for(int m = 1 ; m < 10; m++){
                System.out.print(m+"/10: "+"[CASS:"+ Calc.getRoundedValue(allList_cass.get(m-1).getLevelRate()/LOOP_NUM)
                +":"+Calc.getRoundedValue(allList_cass.get(m-1).getMakeSpanRate()/LOOP_NUM)+":"+ allList_cass.get(m-1).getClusterNum()/LOOP_NUM+"]");

                System.out.print("[DSC:"+ Calc.getRoundedValue(allList_dsc.get(m-1).getLevelRate()/LOOP_NUM)
                +":"+Calc.getRoundedValue(allList_dsc.get(m-1).getMakeSpanRate()/LOOP_NUM)+":"+ allList_dsc.get(m-1).getClusterNum()/LOOP_NUM+"]");

                 System.out.print("[LB:"+ Calc.getRoundedValue(allList_lb.get(m-1).getLevelRate()/LOOP_NUM)
                +":"+Calc.getRoundedValue(allList_lb.get(m-1).getMakeSpanRate()/LOOP_NUM)+":"+ allList_lb.get(m-1).getClusterNum()/LOOP_NUM+"]");
                System.out.println();

            }



        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}