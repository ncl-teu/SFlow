package net.gripps.clustering.tool;

import net.gripps.clustering.algorithms.fcs.FCS_Algorithm;
import net.gripps.clustering.common.aplmodel.*;
import net.gripps.environment.CPU;
import net.gripps.environment.Environment;
import net.gripps.mapping.ClusterMappingManager;
import net.gripps.scheduling.algorithms.heterogeneous.heft.HEFT_Algorithm;
import net.gripps.scheduling.algorithms.heterogeneous.peft.PEFT_Algorithm;
import net.gripps.util.CopyUtil;


import java.io.FileInputStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;


/**
 * Author: Hidehiro Kanemitsu<br>
 * Date: 2007/07/17
 */
public class HeteroMappingBAK {
    public static void main(String[] args) {
        int ALGORITHM_NUM = 4;
        int LOOP_NUM = 10;


        //SortedSet sSet = new SortedSet();
        try {
            //クラスタリングアルゴリズムの実行回数
            //各ループでAPLは新規生成される．
            for (int i = 0; i < LOOP_NUM; i++) {
                //System.out.println("Math:"+Math.random());
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
                switch (dagtype) {
                    case 1:
                        //タスクグラフを構築する
                        AplOperator.getInstance().constructTask(filename);

                        //AplOperator.getInstance().constructTask(filename);
                        //依存関係と，命令数を割り当てる．最下位にあるタスク命令数は決まっているので，読み込むのみ
                        BBTask APL = AplOperator.getInstance().assignDependencyProcess();
                        apl = AplOperator.getInstance().getApl();


                        break;
                    case 2:
                        GaussianOperator.getInstance().constructTask(filename);
                        //依存関係と，命令数を割り当てる．最下位にあるタスク命令数は決まっているので，読み込むのみ
                        GaussianOperator.getInstance().assignDependencyProcess();
                        apl = GaussianOperator.getInstance().getApl();
                        break;
                    case 3:
                        FFTOperator.getInstance().constructTask(filename);
                        //依存関係と，命令数を割り当てる．最下位にあるタスク命令数は決まっているので，読み込むのみ
                        FFTOperator.getInstance().assignDependencyProcess();
                        apl = FFTOperator.getInstance().getApl();


                        break;
                    //HTMLpageOpereatorを使う場合
                    case 4:
                        HTMLpageOperator.getInstance().constructTask(filename);
                        //依存関係と，命令数を割り当てる．最下位にあるタスク命令数は決まっているので，読み込むのみ
                        HTMLpageOperator.getInstance().assignDependencyProcess();
                        apl = HTMLpageOperator.getInstance().getApl();
                        break;
                    default:
                        break;
                }


                // BBTask LBaplCopy = (BBTask)apl.deepCopy();
                /**APLのコピー**/
                BBTask apl_heft = (BBTask) apl.deepCopy();
                BBTask apl_peft = (BBTask) apl.deepCopy();
                BBTask apl_fcs = (BBTask) apl.deepCopy();

                //ClusterMappingManager経由で，マッピングアルゴリズムを実行する．
                //引数：設定ファイル，クラスタリング後のDAG，ピアの数
                ClusterMappingManager cm_manager = new ClusterMappingManager(filename, apl, apl.getTaskList().size());

                /**ENVのコピー**/
                Environment env_heft = (Environment) cm_manager.getEnv().deepCopy();
                Environment env_peft = (Environment) cm_manager.getEnv().deepCopy();
                Environment env_fcs = (Environment) cm_manager.getEnv().deepCopy();

                //マッピング処理
                LinkedList<BBTask> aplList = new LinkedList<BBTask>();

                BBTask mwsl_apl = cm_manager.multiCoreProcess();
                aplList.add(mwsl_apl);
                //System.out.println("MinCP:"+mwsl_apl.getMinCriticalPath());
                double SLR_mwsl = Calc.getRoundedValue((double) (mwsl_apl.getMakeSpan() / (double) mwsl_apl.getMinCriticalPath()));
                double e_value_mwsl = Calc.getRoundedValue((double) (mwsl_apl.getMaxWeight() / maxSpeed) / ((double) mwsl_apl.getTaskClusterList().size() * (double) mwsl_apl.getMakeSpan()));

                //結果から，CPUリストを取得する．
                Hashtable<Long, CPU> cpu_table_heft = new Hashtable<Long, CPU>();

                //クラスタを取得して，その割り当て先CPUを取得する．
                Iterator<TaskCluster> clusterIte = mwsl_apl.getTaskClusterList().values().iterator();
                CustomIDSet mSet = new CustomIDSet();
                int cnt = 0;
                long thresholdSUM = 0;
                double eTime = 0.0;
                double eAllTime = 0.0;
                CustomIDSet set = new CustomIDSet();

                while (clusterIte.hasNext()) {

                    TaskCluster cls = clusterIte.next();
                    if (cls.isLinear()) {
                        cnt++;
                    }
                    CPU cpu = cls.getCPU();
                    if (set.contains(cpu.getCpuID())) {
                        continue;
                    }

                    thresholdSUM += cpu.getThresholdTime() * cpu.getSpeed();
                    //当該CPUに割り当てられているタスク実行時間の和を計算する．
                    long retStime = 9999999;
                    Iterator<AbstractTask> tIte = cpu.getFtQueue().iterator();
                    //CPU単位のループ
                    while (tIte.hasNext()) {
                        AbstractTask t = tIte.next();
                        long stime = t.getStartTime();

                        if (stime < retStime) {
                            retStime = stime;
                        }

                        eTime += Calc.getRoundedValue((double) t.getMaxWeight() / (double) cpu.getSpeed());

                    }
                    eAllTime += cpu.getEndTime() - retStime;

                    //System.out.println("CPUID:"+cpu.getCpuID());
                    // cpu.setTaskClusterID(new Long(-1));
                    //System.out.println("Top:"+cls.getTop_Set().getList().size());
                    cpu.clear();
                    mSet.add(cpu.getMachineID());
                    cpu_table_heft.put(cpu.getCpuID(), cpu);
                    set.add(cpu.getCpuID());
                }
                double ENP = Calc.getRoundedValue(eTime / mwsl_apl.getMakeSpan());
                double E = Calc.getRoundedValue(ENP / (double) mwsl_apl.getTaskClusterList().size());
                double ENP_ALL = Calc.getRoundedValue(eAllTime / mwsl_apl.getMakeSpan());
                double E_ALL = Calc.getRoundedValue(ENP_ALL / (double) mwsl_apl.getTaskClusterList().size());

                System.out.println("MWSL:" + mwsl_apl.getMakeSpan() + "/ENP:" + ENP + "/E:" + E + "/ENP_ALL:" + ENP_ALL + "/E_ALL:" + E_ALL + "Mset;" + mSet.getList().size() + "/ClusterNUM:" + mwsl_apl.getTaskClusterList().size() + "/WSL:" + mwsl_apl.getWorstLevel() + "/SLR:" + SLR_mwsl + "/E_value:" + e_value_mwsl);

                //CPUテーブルをコピーする．
                //Hashtable<Long, CPU> cpu_table_peft = (Hashtable<Long, CPU>)CopyUtil.deepCopy2(cpu_table_heft);
                Hashtable<Long, CPU> cpu_table_peft = new Hashtable<Long, CPU>();


                /**HEFTアルゴリズム**/
                HEFT_Algorithm heft = new HEFT_Algorithm(apl_heft, filename, env_heft/*,cpu_table_heft*/);
                BBTask heft_apl = heft.process();
                aplList.add(heft_apl);
                double SLR_heft = Calc.getRoundedValue((double) (heft_apl.getMakeSpan() / (double) mwsl_apl.getMinCriticalPath()));
                double e_value_heft = Calc.getRoundedValue((double) (heft_apl.getMaxWeight() / maxSpeed) / ((double) heft_apl.getTaskClusterList().size() * (double) heft_apl.getMakeSpan()));

                Iterator<TaskCluster> clsIte = heft_apl.getTaskClusterList().values().iterator();
                CustomIDSet retSet = new CustomIDSet();
                double eTime_heft = 0.0;
                double eTimeALL_heft = 0.0;
                CustomIDSet set2 = new CustomIDSet();

                while (clsIte.hasNext()) {
                    TaskCluster cls = clsIte.next();
                    CPU cpu = cls.getCPU();
                    if (set2.contains(cpu.getCpuID())) {
                        continue;
                    }
                    retSet.add(cpu.getMachineID());
                    Iterator<AbstractTask> tIte = cpu.getFtQueue().iterator();
                    long retStime = 9999999;

                    while (tIte.hasNext()) {
                        AbstractTask t = tIte.next();
                        long stime = t.getStartTime();
                        if (stime < retStime) {
                            retStime = stime;
                        }

                        eTime_heft += Calc.getRoundedValue((double) t.getMaxWeight() / (double) cpu.getSpeed());


                    }
                    eTimeALL_heft += cpu.getEndTime() - retStime;
                    set2.add(cpu.getCpuID());

                    cpu.clear();
                    //mSet.add(cpu.getMachineID());
                    cpu_table_peft.put(cpu.getCpuID(), cpu);


                }
                double ENP_heft = Calc.getRoundedValue(eTime_heft / heft_apl.getMakeSpan());
                double E_heft = Calc.getRoundedValue(ENP_heft / (double) heft_apl.getTaskClusterList().size());
                double ENP_ALL_heft = Calc.getRoundedValue(eTimeALL_heft / heft_apl.getMakeSpan());
                double E_ALL_heft = Calc.getRoundedValue(ENP_ALL_heft / (double) heft_apl.getTaskClusterList().size());

                System.out.println(":HEFT:" + heft_apl.getMakeSpan() + "/ENP:" + ENP_heft + "/E:" + E_heft + "/ENP_ALL:" + ENP_ALL_heft + "/E_ALL:" + E_ALL_heft + "/Mset;" + retSet.getList().size() + "/ClusterNUM:" + heft_apl.getTaskClusterList().size() + "/WSL:" + heft_apl.getWorstLevel() + "/SLR_heft:" + SLR_heft + "/E_value:" + e_value_heft);

                Hashtable<Long, CPU> peft_cputable = new Hashtable<Long, CPU>();

                /**PEFTアルゴリズム**/
                if (cpu_table_heft.size() > cpu_table_peft.size()) {
                    peft_cputable = cpu_table_heft;
                } else {
                    peft_cputable = cpu_table_peft;
                }
                PEFT_Algorithm peft = new PEFT_Algorithm(apl_peft, filename, env_peft/*, peft_cputable*/);
                BBTask peft_apl = peft.process();
                System.out.print("PEFT:" + peft_apl.getMakeSpan() + "/ClusterNUM:" + peft_apl.getTaskClusterList().size() + "/WSL:" + peft_apl.getWorstLevel());


                /*FCS_Algorithm fcs = new FCS_Algorithm(apl_fcs, filename, env_fcs);
               BBTask fcs_apl = fcs.process();
                System.out.print("FCS:"+fcs_apl.getMakeSpan()+"/ClusterNUM:"+fcs_apl.getTaskClusterList().size() + "/WSL:"+fcs_apl.getWorstLevel());
               */
                System.out.println();

                //各種クラスタリングアルゴリズムを実行する
                // LinkedList<BBTask> aplList = ClusteringAlgorithmManager.getInstance().process(filename);
                /*
                int idx = 1;
                BBTask apl_first = aplList.get(0);
                long opt_delta = apl_first.getOptDelta();

                //System.out.print(apl_first.getTaskWeight() + ":" + outDegree_Max + ":" + edgeSize_Max + ":" + apl_first.getEdgeNum() + ":" + apl_first.getEdgeWeight() + ":");

                double CCR = (double)apl_first.getEdgeWeight() / (double)apl_first.getTaskWeight();

                BigDecimal bd = new BigDecimal(String.valueOf(CCR));
                double CCR_RET = bd.setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
                */


            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
