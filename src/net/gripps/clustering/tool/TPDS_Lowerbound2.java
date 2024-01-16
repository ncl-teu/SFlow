package net.gripps.clustering.tool;

import net.gripps.clustering.common.aplmodel.*;
import net.gripps.environment.CPU;
import net.gripps.environment.Environment;
import net.gripps.mapping.ClusterMappingManager;

import java.io.FileInputStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;

/**
 * Author: H. Kanemitsu
 * Date: 15/04/30
 */
public class TPDS_Lowerbound2 {
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
                    BBTask apl_cmwsl05 = (BBTask) apl.deepCopy();
                    BBTask apl_cmwsl08 = (BBTask) apl.deepCopy();
                    BBTask apl_cmwsl12 = (BBTask) apl.deepCopy();
                    BBTask apl_cmwsl15 = (BBTask) apl.deepCopy();
                    BBTask apl_cmwsl20 = (BBTask) apl.deepCopy();





                    //ClusterMappingManager経由で，マッピングアルゴリズムを実行する．
                    //引数：設定ファイル，クラスタリング後のDAG，ピアの数
                    ClusterMappingManager cm_manager = new ClusterMappingManager(filename, apl, apl.getTaskList().size());


                    /**ENVのコピー**/
                    Environment env_cmwsl05 = (Environment) cm_manager.getEnv().deepCopy();
                    LinkedList<BBTask> aplList = new LinkedList<BBTask>();
                    /**MWSLアルゴリズムSTART**/
                    BBTask cmwsl_apl = cm_manager.multiCoreProcess();
                    aplList.add(cmwsl_apl);
                    /**MWSLアルゴリズムEND**/

                      //Schedule Length Ratio(SLR)の計算
                    double SLR_mwsl = Calc.getRoundedValue((double) (cmwsl_apl.getMakeSpan() / (double) cmwsl_apl.getMinCriticalPath()));

                    //結果から，CPUリストを取得する．
                    Hashtable<Long, CPU> cpu_table_heft = new Hashtable<Long, CPU>();

                    //クラスタを取得して，その割り当て先CPUを取得する．
                    Iterator<TaskCluster> clusterIte = cmwsl_apl.getTaskClusterList().values().iterator();
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
                    double e_value_mwsl = Calc.getRoundedValue((double) (cmwsl_apl.getMaxWeight() / maxSpeed) / ((double)  set.getList().size()* (double) cmwsl_apl.getMakeSpan()));


                    System.out.print("CMWSL(応答時間):" + cmwsl_apl.getMakeSpan() + ":" + set.getList().size() + ":" + cmwsl_apl.getWorstLevel() + ":" + SLR_mwsl + ":" + e_value_mwsl);


                    /**
                     * static版の実行
                     */
                    ClusterMappingManager cm_manager05 = new ClusterMappingManager(filename, apl_cmwsl05, apl_cmwsl05.getTaskList().size());
                    //cm_manager05.setEnv(env_cmwsl05);
                    BBTask cmwsl_apl05 = cm_manager05.multiCoreProcess_size(0.5);
                    aplList.add(cmwsl_apl05);
                    double SLR_mwsl05 = Calc.getRoundedValue((double) (cmwsl_apl05.getMakeSpan() / (double) cmwsl_apl05.getMinCriticalPath()));

                    //結果から，CPUリストを取得する．
                    Hashtable<Long, CPU> cpu_table05 = new Hashtable<Long, CPU>();

                    //クラスタを取得して，その割り当て先CPUを取得する．
                    Iterator<TaskCluster> clusterIte05 = cmwsl_apl05.getTaskClusterList().values().iterator();
                    CustomIDSet mSet05 = new CustomIDSet();
                    CustomIDSet set05 = new CustomIDSet();

                    while (clusterIte05.hasNext()) {
                        TaskCluster cls = clusterIte05.next();
                        if(cls.getTaskSet().isEmpty()){
                            continue;
                        }
                        //System.out.println("ID: "+cls.getClusterID()+"/Num:"+cls.getTaskSet().getList().size());
                        CPU cpu = cls.getCPU();

                        cpu.clear();
                        mSet05.add(cpu.getMachineID());
                        cpu_table05.put(cpu.getCpuID(), cpu);
                        set05.add(cpu.getCpuID());
                    }

                      //Efficiencyの計算
                    double e_value_mwsl05 = Calc.getRoundedValue((double) (cmwsl_apl.getMaxWeight() / maxSpeed) / ((double)  set05.getList().size() * (double) cmwsl_apl05.getMakeSpan()));

                    System.out.print(":MWSL0.5(応答時間):" + cmwsl_apl05.getMakeSpan() +":" + set05.getList().size() + ":" + cmwsl_apl05.getWorstLevel() + ":" + SLR_mwsl05 + ":" + e_value_mwsl05);


                    /**
                     * static版の実行
                     */
                    ClusterMappingManager cm_manager08 = new ClusterMappingManager(filename, apl_cmwsl08, apl_cmwsl08.getTaskList().size());
                    //cm_manager08.setEnv(env_cmwsl08);
                    BBTask cmwsl_apl08 = cm_manager08.multiCoreProcess_size(0.8);
                    aplList.add(cmwsl_apl08);
                    double SLR_mwsl08 = Calc.getRoundedValue((double) (cmwsl_apl08.getMakeSpan() / (double) cmwsl_apl08.getMinCriticalPath()));

                    //結果から，CPUリストを取得する．
                    Hashtable<Long, CPU> cpu_table08 = new Hashtable<Long, CPU>();

                    //クラスタを取得して，その割り当て先CPUを取得する．
                    Iterator<TaskCluster> clusterIte08 = cmwsl_apl08.getTaskClusterList().values().iterator();
                    CustomIDSet mSet08 = new CustomIDSet();
                    CustomIDSet set08 = new CustomIDSet();

                    while (clusterIte08.hasNext()) {
                        TaskCluster cls = clusterIte08.next();
                        if(cls.getTaskSet().isEmpty()){
                            continue;
                        }
                        //System.out.println("ID: "+cls.getClusterID()+"/Num:"+cls.getTaskSet().getList().size());
                        CPU cpu = cls.getCPU();

                        cpu.clear();
                        mSet08.add(cpu.getMachineID());
                        cpu_table08.put(cpu.getCpuID(), cpu);
                        set08.add(cpu.getCpuID());
                    }

                    //Efficiencyの計算
                    double e_value_mwsl08 = Calc.getRoundedValue((double) (cmwsl_apl.getMaxWeight() / maxSpeed) / ((double)  set08.getList().size() * (double) cmwsl_apl08.getMakeSpan()));

                    System.out.print(":MWSL0.8(応答時間):" + cmwsl_apl08.getMakeSpan() +":" + set08.getList().size() + ":" + cmwsl_apl08.getWorstLevel() + ":" + SLR_mwsl08 + ":" + e_value_mwsl08);



                    /**
                     * static版の実行
                     */
                    ClusterMappingManager cm_manager12 = new ClusterMappingManager(filename, apl_cmwsl12, apl_cmwsl12.getTaskList().size());
                    //cm_manager12.setEnv(env_cmwsl12);
                    BBTask cmwsl_apl12 = cm_manager12.multiCoreProcess_size(1.2);
                    aplList.add(cmwsl_apl12);
                    double SLR_mwsl12 = Calc.getRoundedValue((double) (cmwsl_apl12.getMakeSpan() / (double) cmwsl_apl12.getMinCriticalPath()));

                    //結果から，CPUリストを取得する．
                    Hashtable<Long, CPU> cpu_table12 = new Hashtable<Long, CPU>();

                    //クラスタを取得して，その割り当て先CPUを取得する．
                    Iterator<TaskCluster> clusterIte12 = cmwsl_apl12.getTaskClusterList().values().iterator();
                    CustomIDSet mSet12 = new CustomIDSet();
                    CustomIDSet set12 = new CustomIDSet();

                    while (clusterIte12.hasNext()) {
                        TaskCluster cls = clusterIte12.next();
                        if(cls.getTaskSet().isEmpty()){
                            continue;
                        }
                        //System.out.println("ID: "+cls.getClusterID()+"/Num:"+cls.getTaskSet().getList().size());
                        CPU cpu = cls.getCPU();

                        cpu.clear();
                        mSet12.add(cpu.getMachineID());
                        cpu_table12.put(cpu.getCpuID(), cpu);
                        set12.add(cpu.getCpuID());
                    }

                    //Efficiencyの計算
                    double e_value_mwsl12 = Calc.getRoundedValue((double) (cmwsl_apl.getMaxWeight() / maxSpeed) / ((double)  set12.getList().size() * (double) cmwsl_apl12.getMakeSpan()));

                    System.out.print(":MWSL1.2(応答時間):" + cmwsl_apl12.getMakeSpan() + ":" + set12.getList().size() + ":" + cmwsl_apl12.getWorstLevel() + ":" + SLR_mwsl12 + ":" + e_value_mwsl12);



                    /**
                     * static版の実行
                     */
                    ClusterMappingManager cm_manager15 = new ClusterMappingManager(filename, apl_cmwsl15, apl_cmwsl15.getTaskList().size());
                    //cm_manager15.setEnv(env_cmwsl15);
                    BBTask cmwsl_apl15 = cm_manager15.multiCoreProcess_size(1.5);
                    aplList.add(cmwsl_apl15);
                    double SLR_mwsl15 = Calc.getRoundedValue((double) (cmwsl_apl15.getMakeSpan() / (double) cmwsl_apl15.getMinCriticalPath()));

                    //結果から，CPUリストを取得する．
                    Hashtable<Long, CPU> cpu_table15 = new Hashtable<Long, CPU>();

                    //クラスタを取得して，その割り当て先CPUを取得する．
                    Iterator<TaskCluster> clusterIte15 = cmwsl_apl15.getTaskClusterList().values().iterator();
                    CustomIDSet mSet15 = new CustomIDSet();
                    CustomIDSet set15 = new CustomIDSet();

                    while (clusterIte15.hasNext()) {
                        TaskCluster cls = clusterIte15.next();
                        if(cls.getTaskSet().isEmpty()){
                            continue;
                        }
                        //System.out.println("ID: "+cls.getClusterID()+"/Num:"+cls.getTaskSet().getList().size());
                        CPU cpu = cls.getCPU();

                        cpu.clear();
                        mSet15.add(cpu.getMachineID());
                        cpu_table15.put(cpu.getCpuID(), cpu);
                        set15.add(cpu.getCpuID());
                    }

                    //Efficiencyの計算
                    double e_value_mwsl15 = Calc.getRoundedValue((double) (cmwsl_apl.getMaxWeight() / maxSpeed) / ((double)  set15.getList().size() * (double) cmwsl_apl15.getMakeSpan()));

                    System.out.print(":MWSL1.5(応答時間):" + cmwsl_apl15.getMakeSpan() + ":" + set15.getList().size() + ":" + cmwsl_apl15.getWorstLevel() + ":" + SLR_mwsl15 + ":" + e_value_mwsl15);

                    //  System.out.println();

                    /**
                     * static版の実行
                     */
                    ClusterMappingManager cm_manager20 = new ClusterMappingManager(filename, apl_cmwsl20, apl_cmwsl20.getTaskList().size());
                    //cm_manager20.setEnv(env_cmwsl20);
                    BBTask cmwsl_apl20 = cm_manager20.multiCoreProcess_size(2);
                    aplList.add(cmwsl_apl20);
                    double SLR_mwsl20 = Calc.getRoundedValue((double) (cmwsl_apl20.getMakeSpan() / (double) cmwsl_apl20.getMinCriticalPath()));

                    //結果から，CPUリストを取得する．
                    Hashtable<Long, CPU> cpu_table20 = new Hashtable<Long, CPU>();

                    //クラスタを取得して，その割り当て先CPUを取得する．
                    Iterator<TaskCluster> clusterIte20 = cmwsl_apl20.getTaskClusterList().values().iterator();
                    CustomIDSet mSet20 = new CustomIDSet();
                    CustomIDSet set20 = new CustomIDSet();

                    while (clusterIte20.hasNext()) {
                        TaskCluster cls = clusterIte20.next();
                        if(cls.getTaskSet().isEmpty()){
                            continue;
                        }
                        //System.out.println("ID: "+cls.getClusterID()+"/Num:"+cls.getTaskSet().getList().size());
                        CPU cpu = cls.getCPU();

                        cpu.clear();
                        mSet20.add(cpu.getMachineID());
                        cpu_table20.put(cpu.getCpuID(), cpu);
                        set20.add(cpu.getCpuID());
                    }

                    //Efficiencyの計算
                    double e_value_mwsl20 = Calc.getRoundedValue((double) (cmwsl_apl.getMaxWeight() / maxSpeed) / ((double)  set20.getList().size() * (double) cmwsl_apl20.getMakeSpan()));

                    System.out.println(":MWSL2.0(応答時間):" + cmwsl_apl20.getMakeSpan() + ":" + set20.getList().size() + ":" + cmwsl_apl20.getWorstLevel() + ":" + SLR_mwsl20 + ":" + e_value_mwsl20);



                }


            } catch (Exception e) {
                e.printStackTrace();
            }
    }
}
