package net.gripps.clustering.tool;

import net.gripps.clustering.algorithms.mwsl_delta.CMWSL_Algorithm;
import net.gripps.clustering.common.aplmodel.*;
import net.gripps.environment.CPU;
import net.gripps.environment.Environment;
import net.gripps.mapping.ClusterMappingManager;
import net.gripps.scheduling.algorithms.heterogeneous.HeteroBlevelScheduling;
import net.gripps.scheduling.algorithms.heterogeneous.HeteroTlevelBlevelScheduling;
import net.gripps.scheduling.algorithms.heterogeneous.mwsl.MWSL_RCPScheduling;
import net.gripps.scheduling.algorithms.heterogeneous.mwsl.MWSL_RCPSchedulingLarge;

import java.io.FileInputStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;

/**
 * Created by kanemih on 2015/05/16.
 */
public class JCSE_Scheduling {

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

                //ClusterMappingManager経由で，マッピングアルゴリズムを実行する．
                //引数：設定ファイル，クラスタリング後のDAG，ピアの数
                ClusterMappingManager cm_manager = new ClusterMappingManager(filename, apl, apl.getTaskList().size());
                /**ENVのコピー**/
                Environment env_b = (Environment) cm_manager.getEnv().deepCopy();
                Environment env_c = (Environment) cm_manager.getEnv().deepCopy();
                Environment env_d = (Environment) cm_manager.getEnv().deepCopy();
                Environment env_e = (Environment) cm_manager.getEnv().deepCopy();

                LinkedList<BBTask> aplList = new LinkedList<BBTask>();

 /**CMMWSLアルゴリズムSTART**/
                CMWSL_Algorithm hetero = new CMWSL_Algorithm(apl,filename, cm_manager.getEnv());
               // hetero.setDeltaStatic(true);
               //hetero.setUpdateAll(true);
                //メイン処理を行う．
                BBTask cmwsl_apl = hetero.process();
                BBTask cmwsl_apl_b = (BBTask)cmwsl_apl.deepCopy();
                BBTask cmwsl_apl_c = (BBTask)cmwsl_apl.deepCopy();
                BBTask cmwsl_apl_d = (BBTask)cmwsl_apl.deepCopy();
                BBTask cmwsl_apl_e = (BBTask)cmwsl_apl.deepCopy();


                MWSL_RCPScheduling mwsl_rcp = new MWSL_RCPScheduling(cmwsl_apl, filename, cm_manager.getEnv());
                cmwsl_apl = mwsl_rcp.process();
/**CMWSLアルゴリズムEND**/

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


                System.out.print("CMWSL(応答時間):" + cmwsl_apl.getMakeSpan() + ":" + mSet.getList().size() + ":" + cmwsl_apl.getWorstLevel() + ":" + SLR_mwsl + ":" + e_value_mwsl+":"+mwsl_rcp.getDwtime());


/**パターンB START**/
              /*  MWSL_Scheduling mwsl_b = new MWSL_Scheduling(cmwsl_apl_b, filename, env_b);
                cmwsl_apl_b = mwsl_b.process();

                double SLR_mwsl_b = Calc.getRoundedValue((double) (cmwsl_apl_b.getMakeSpan() / (double) cmwsl_apl_b.getMinCriticalPath()));

                //結果から，CPUリストを取得する．
                Hashtable<Long, CPU> cpu_table_b = new Hashtable<Long, CPU>();

                //クラスタを取得して，その割り当て先CPUを取得する．
                Iterator<TaskCluster> clusterIte_b = cmwsl_apl_b.getTaskClusterList().values().iterator();
                CustomIDSet mSet_b = new CustomIDSet();
                CustomIDSet set_b = new CustomIDSet();

                while (clusterIte_b.hasNext()) {
                    TaskCluster cls = clusterIte_b.next();
                    if(cls.getTaskSet().isEmpty()){
                        continue;
                    }
                    //System.out.println("ID: "+cls.getClusterID()+"/Num:"+cls.getTaskSet().getList().size());
                    CPU cpu = cls.getCPU();

                    cpu.clear();
                    mSet_b.add(cpu.getMachineID());
                    cpu_table_b.put(cpu.getCpuID(), cpu);
                    set_b.add(cpu.getCpuID());
                }

                //Efficiencyの計算
                double e_value_mwsl_b = Calc.getRoundedValue((double) (cmwsl_apl_b.getMaxWeight() / maxSpeed) / ((double)  set_b.getList().size() * (double) cmwsl_apl_b.getMakeSpan()));
                System.out.print("/CMWSLB(応答時間):" + cmwsl_apl_b.getMakeSpan() + ":" + mSet_b.getList().size() +  ":" + cmwsl_apl_b.getWorstLevel() + ":" + SLR_mwsl_b + ":" + e_value_mwsl_b);
                 */
/**パターンC START**/

                HeteroBlevelScheduling mwsl_c = new HeteroBlevelScheduling(cmwsl_apl_c, filename, env_c);
                cmwsl_apl_c = mwsl_c.process();

                double SLR_mwsl_c = Calc.getRoundedValue((double) (cmwsl_apl_c.getMakeSpan() / (double) cmwsl_apl_c.getMinCriticalPath()));

                //結果から，CPUリストを取得する．
                Hashtable<Long, CPU> cpu_table_c = new Hashtable<Long, CPU>();

                //クラスタを取得して，その割り当て先CPUを取得する．
                Iterator<TaskCluster> clusterIte_c = cmwsl_apl_c.getTaskClusterList().values().iterator();
                CustomIDSet mSet_c = new CustomIDSet();
                CustomIDSet set_c = new CustomIDSet();

                while (clusterIte_c.hasNext()) {
                    TaskCluster cls = clusterIte_c.next();
                    if(cls.getTaskSet().isEmpty()){
                        continue;
                    }
                    //System.out.println("ID: "+cls.getClusterID()+"/Num:"+cls.getTaskSet().getList().size());
                    CPU cpu = cls.getCPU();

                    cpu.clear();
                    mSet_c.add(cpu.getMachineID());
                    cpu_table_c.put(cpu.getCpuID(), cpu);
                    set_c.add(cpu.getCpuID());
                }

                //Efficiencyの計算
                double e_value_mwsl_c = Calc.getRoundedValue((double) (cmwsl_apl_c.getMaxWeight() / maxSpeed) / ((double)  set_c.getList().size() * (double) cmwsl_apl_c.getMakeSpan()));
                System.out.print(":/CMWSLC(応答時間):" + cmwsl_apl_c.getMakeSpan() + ":" + mSet_c.getList().size() +  ":" + cmwsl_apl_c.getWorstLevel() + ":" + SLR_mwsl_c + ":" + e_value_mwsl_c+":"+mwsl_c.getDwtime());


/**パターンDの実行**/
                HeteroTlevelBlevelScheduling  mwsl_d = new HeteroTlevelBlevelScheduling(cmwsl_apl_d, filename, env_d);
                cmwsl_apl_d = mwsl_d.process();

                double SLR_mwsl_d = Calc.getRoundedValue((double) (cmwsl_apl_d.getMakeSpan() / (double) cmwsl_apl_d.getMinCriticalPath()));

                //結果から，CPUリストを取得する．
                Hashtable<Long, CPU> cpu_table_d = new Hashtable<Long, CPU>();

                //クラスタを取得して，その割り当て先CPUを取得する．
                Iterator<TaskCluster> clusterIte_d = cmwsl_apl_d.getTaskClusterList().values().iterator();
                CustomIDSet mSet_d = new CustomIDSet();
                CustomIDSet set_d = new CustomIDSet();

                while (clusterIte_d.hasNext()) {
                    TaskCluster cls = clusterIte_d.next();
                    if(cls.getTaskSet().isEmpty()){
                        continue;
                    }
                    //System.out.println("ID: "+cls.getClusterID()+"/Num:"+cls.getTaskSet().getList().size());
                    CPU cpu = cls.getCPU();

                    cpu.clear();
                    mSet_d.add(cpu.getMachineID());
                    cpu_table_d.put(cpu.getCpuID(), cpu);
                    set_d.add(cpu.getCpuID());
                }

                //Efficiencyの計算
                double e_value_mwsl_d = Calc.getRoundedValue((double) (cmwsl_apl_d.getMaxWeight() / maxSpeed) / ((double)  set_d.getList().size() * (double) cmwsl_apl_d.getMakeSpan()));
                System.out.print(":/CMWSLD(応答時間):" + cmwsl_apl_d.getMakeSpan() + ":" + mSet_d.getList().size() +  ":" + cmwsl_apl_d.getWorstLevel() + ":" + SLR_mwsl_d + ":" + e_value_mwsl_d+":"+mwsl_d.getDwtime());

/**パターンEの実行**/
                MWSL_RCPSchedulingLarge mwsl_e = new MWSL_RCPSchedulingLarge(cmwsl_apl_e, filename, env_e);
                cmwsl_apl_e = mwsl_e.process();

                double SLR_mwsl_e = Calc.getRoundedValue((double) (cmwsl_apl_e.getMakeSpan() / (double) cmwsl_apl_e.getMinCriticalPath()));

                //結果から，CPUリストを取得する．
                Hashtable<Long, CPU> cpu_table_e = new Hashtable<Long, CPU>();

                //クラスタを取得して，その割り当て先CPUを取得する．
                Iterator<TaskCluster> clusterIte_e = cmwsl_apl_e.getTaskClusterList().values().iterator();
                CustomIDSet mSet_e = new CustomIDSet();
                CustomIDSet set_e = new CustomIDSet();

                while (clusterIte_e.hasNext()) {
                    TaskCluster cls = clusterIte_e.next();
                    if(cls.getTaskSet().isEmpty()){
                        continue;
                    }
                    //System.out.println("ID: "+cls.getClusterID()+"/Num:"+cls.getTaskSet().getList().size());
                    CPU cpu = cls.getCPU();

                    cpu.clear();
                    mSet_e.add(cpu.getMachineID());
                    cpu_table_e.put(cpu.getCpuID(), cpu);
                    set_e.add(cpu.getCpuID());
                }

                //Efficiencyの計算
                double e_value_mwsl_e = Calc.getRoundedValue((double) (cmwsl_apl_e.getMaxWeight() / maxSpeed) / ((double)  set_e.getList().size() * (double) cmwsl_apl_e.getMakeSpan()));
                System.out.println(":/CMWSLE(応答時間):" + cmwsl_apl_e.getMakeSpan() + ":" + mSet_e.getList().size() +  ":" + cmwsl_apl_e.getWorstLevel() + ":" + SLR_mwsl_e + ":" + e_value_mwsl_e+":"+mwsl_e.getDwtime());




                //  System.out.println();


            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
