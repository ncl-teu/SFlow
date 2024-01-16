package net.gripps.clustering.tool;

import net.gripps.clustering.algorithms.mwsl_delta.CMWSL_Algorithm;
import net.gripps.clustering.algorithms.rac.RAC_Algorithm;
import net.gripps.clustering.algorithms.triplet.Triplet_Algorithm;
import net.gripps.clustering.common.aplmodel.*;
import net.gripps.environment.CPU;
import net.gripps.environment.Environment;
import net.gripps.mapping.ClusterMappingManager;
import net.gripps.scheduling.algorithms.RCP_Scheduling;
import net.gripps.scheduling.algorithms.heterogeneous.ceft.CEFT_Algorithm;
import net.gripps.scheduling.algorithms.heterogeneous.heft.HEFT_Algorithm;
import net.gripps.scheduling.algorithms.heterogeneous.mwsl.MWSL_RCPScheduling;

import java.io.FileInputStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;

/**
 * Author: H. Kanemitsu
 * Date: 15/04/30
 */
public class TPDS_Lowerbound {
    public static void main(String[] args){
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
                    BBTask apl_cmwsl_org = (BBTask) apl.deepCopy();

                    //ClusterMappingManager経由で，マッピングアルゴリズムを実行する．
                    //引数：設定ファイル，クラスタリング後のDAG，ピアの数
                    ClusterMappingManager cm_manager = new ClusterMappingManager(filename, apl, apl.getTaskList().size());

                    /**ENVのコピー**/
                    Environment env = cm_manager.getEnv();
                    Environment env_cmwsl_org = (Environment) env.deepCopy();
                    LinkedList<BBTask> aplList = new LinkedList<BBTask>();
                    /**MWSLアルゴリズムSTART**/
                   // BBTask cmwsl_apl = cm_manager.multiCoreProcess();
                    CMWSL_Algorithm heterocmwsl = new CMWSL_Algorithm(apl, filename, env);
                //    hetero.setDeltaStatic(true);

                 //   hetero.setUpdateAll(true);
                    //メイン処理を行う．
                    apl = heterocmwsl.process();
                    MWSL_RCPScheduling mwsl_rcp1 = new MWSL_RCPScheduling(apl, filename, env);
                    apl = mwsl_rcp1.process();
                   // aplList.add(cmwsl_apl);
                    /**MWSLアルゴリズムEND**/

                      //Schedule Length Ratio(SLR)の計算
                    double SLR_mwsl = Calc.getRoundedValue((double) (apl.getMakeSpan() / (double) apl.getMinCriticalPath()));

                    //結果から，CPUリストを取得する．
                    Hashtable<Long, CPU> cpu_table_heft = new Hashtable<Long, CPU>();

                    //クラスタを取得して，その割り当て先CPUを取得する．
                    Iterator<TaskCluster> clusterIte = apl.getTaskClusterList().values().iterator();
                    CustomIDSet mSet = new CustomIDSet();
                    CustomIDSet set = new CustomIDSet();

                    while (clusterIte.hasNext()) {
                        TaskCluster cls = clusterIte.next();
                        if(cls.getTaskSet().isEmpty()){
                            continue;
                        }
                        //System.out.println("ID: "+cls.getClusterID()+"/Num:"+cls.getTaskSet().getList().size());
                        CPU cpu = cls.getCPU();

                        cpu.clear();
                        mSet.add(cpu.getMachineID());
                        cpu_table_heft.put(cpu.getCpuID(), cpu);
                        set.add(cpu.getCpuID());
                    }

                       //Efficiencyの計算
                    double e_value_mwsl = Calc.getRoundedValue((double) (apl.getMaxWeight() / maxSpeed) / ((double)  set.getList().size()* (double) apl.getMakeSpan()));


                    System.out.print("CMWSL(応答時間):" + apl.getMakeSpan() + "/マシン数:" + mSet.getList().size() + "/プロセッサ数(クラスタ数):" + set.getList().size() + "/WSL:" + apl.getWorstLevel() + "/SLR:" + SLR_mwsl + "/Efficiency:" + e_value_mwsl);


                    /**
                     * static版の実行
                     */
               //     ClusterMappingManager cm_manager_org = new ClusterMappingManager(filename, apl_cmwsl_org, apl_cmwsl_org.getTaskList().size());
                //    BBTask cmwsl_apl_org = cm_manager_org.multiCoreProcess_org();

                    CMWSL_Algorithm hetero = new CMWSL_Algorithm(apl_cmwsl_org, filename, env_cmwsl_org);
                    hetero.setDeltaStatic(true);
                           //hetero.setUpdateAll(true);
                             //メイン処理を行う．
                    BBTask cmwsl_apl_org = hetero.process();
                    MWSL_RCPScheduling mwsl_rcp = new MWSL_RCPScheduling(cmwsl_apl_org, filename, env_cmwsl_org);
                    cmwsl_apl_org = mwsl_rcp.process();


                    aplList.add(cmwsl_apl_org);
                    double SLR_mwsl_org = Calc.getRoundedValue((double) (cmwsl_apl_org.getMakeSpan() / (double) cmwsl_apl_org.getMinCriticalPath()));

                    //結果から，CPUリストを取得する．
                    Hashtable<Long, CPU> cpu_table_org = new Hashtable<Long, CPU>();

                    //クラスタを取得して，その割り当て先CPUを取得する．
                    Iterator<TaskCluster> clusterIte_org = cmwsl_apl_org.getTaskClusterList().values().iterator();
                    CustomIDSet mSet_org = new CustomIDSet();
                    CustomIDSet set_org = new CustomIDSet();

                    while (clusterIte_org.hasNext()) {
                        TaskCluster cls = clusterIte_org.next();
                        if(cls.getTaskSet().isEmpty()){
                            continue;
                        }
                        //System.out.println("ID: "+cls.getClusterID()+"/Num:"+cls.getTaskSet().getList().size());
                        CPU cpu = cls.getCPU();

                        cpu.clear();
                        mSet_org.add(cpu.getMachineID());
                        cpu_table_org.put(cpu.getCpuID(), cpu);
                        set_org.add(cpu.getCpuID());
                    }

                      //Efficiencyの計算
                   double e_value_mwsl_org = Calc.getRoundedValue((double) (apl.getMaxWeight() / maxSpeed) / ((double)  set_org.getList().size() * (double) cmwsl_apl_org.getMakeSpan()));

                    System.out.println("/OMWSL(応答時間):" + cmwsl_apl_org.getMakeSpan() + "/マシン数:" + mSet_org.getList().size() + "/プロセッサ数(クラスタ数):" + set_org.getList().size() + "/WSL:" + cmwsl_apl_org.getWorstLevel() + "/SLR:" + SLR_mwsl_org + "/Efficiency:" + e_value_mwsl_org);



                  //  System.out.println();


                }


            } catch (Exception e) {
                e.printStackTrace();
            }
    }
}
