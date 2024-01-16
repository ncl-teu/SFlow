package net.gripps.clustering.tool;

import net.gripps.clustering.common.aplmodel.*;
import net.gripps.environment.CPU;
import net.gripps.environment.Environment;
import net.gripps.mapping.ClusterMappingManager;
import net.gripps.scheduling.algorithms.heterogeneous.heft.HEFT_Algorithm;
import net.gripps.scheduling.algorithms.heterogeneous.peft.PEFT_Algorithm;

import java.io.FileInputStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;


/**
 * Author: Hidehiro Kanemitsu<br>
 * Date: 2007/07/17
 */
public class HeteroMapping2 {
    public static void main(String[] args) {
        int ALGORITHM_NUM = 5;
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

                   //ImageCompressを使う場合
		    case 5:
                        ImageCompress.getInstance().constructTask(filename);
                        //依存関係と，命令数を割り当てる．最下位にあるタスク命令数は決まっているので，読み込むのみ
                        ImageCompress.getInstance().assignDependencyProcess();
                        apl = ImageCompress.getInstance().getApl();
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

                LinkedList<BBTask> aplList = new LinkedList<BBTask>();

                /**MWSLアルゴリズムSTART**/
                BBTask mwsl_apl = cm_manager.multiCoreProcess();
                aplList.add(mwsl_apl);
                /**MWSLアルゴリズムEND**/


               //Schedule Length Ratio(SLR)の計算
                double SLR_mwsl = Calc.getRoundedValue((double) (mwsl_apl.getMakeSpan() / (double) mwsl_apl.getMinCriticalPath()));
                //Efficiencyの計算
                double e_value_mwsl = Calc.getRoundedValue((double) (mwsl_apl.getMaxWeight() / maxSpeed) / ((double) mwsl_apl.getTaskClusterList().size() * (double) mwsl_apl.getMakeSpan()));

                //結果から，CPUリストを取得する．
                Hashtable<Long, CPU> cpu_table_heft = new Hashtable<Long, CPU>();

                //クラスタを取得して，その割り当て先CPUを取得する．
                Iterator<TaskCluster> clusterIte = mwsl_apl.getTaskClusterList().values().iterator();
                CustomIDSet mSet = new CustomIDSet();
                CustomIDSet set = new CustomIDSet();

                while (clusterIte.hasNext()) {
                    TaskCluster cls = clusterIte.next();
                    CPU cpu = cls.getCPU();

                    cpu.clear();
                    mSet.add(cpu.getMachineID());
                    cpu_table_heft.put(cpu.getCpuID(), cpu);
                    set.add(cpu.getCpuID());
                }


                System.out.println("MWSL(応答時間):" + mwsl_apl.getMakeSpan() + "/マシン数:" + mSet.getList().size() + "/プロセッサ数(クラスタ数):" + set.getList().size() + "/WSL:" + mwsl_apl.getWorstLevel() + "/SLR:" + SLR_mwsl + "/Efficiency:" + e_value_mwsl);


                Hashtable<Long, CPU> cpu_table_peft = new Hashtable<Long, CPU>();


                /**HEFTアルゴリズムSTART**/
                HEFT_Algorithm heft = new HEFT_Algorithm(apl_heft, filename, env_heft/*,cpu_table_heft*/);
                BBTask heft_apl = heft.process();
                aplList.add(heft_apl);
                /**HEFTアルゴリズムEND**/

                //SLRの計算
                double SLR_heft = Calc.getRoundedValue((double) (heft_apl.getMakeSpan() / (double) mwsl_apl.getMinCriticalPath()));
                //Efficiencyの計算
                double e_value_heft = Calc.getRoundedValue((double) (heft_apl.getMaxWeight() / maxSpeed) / ((double) heft_apl.getTaskClusterList().size() * (double) heft_apl.getMakeSpan()));

                Iterator<TaskCluster> clsIte = heft_apl.getTaskClusterList().values().iterator();
                CustomIDSet retSet = new CustomIDSet();
                CustomIDSet set2 = new CustomIDSet();

                while (clsIte.hasNext()) {
                    TaskCluster cls = clsIte.next();
                    CPU cpu = cls.getCPU();
                    if (set2.contains(cpu.getCpuID())) {
                        continue;
                    }
                    retSet.add(cpu.getMachineID());
                    set2.add(cpu.getCpuID());

                    cpu.clear();
                    //mSet.add(cpu.getMachineID());
                    cpu_table_peft.put(cpu.getCpuID(), cpu);
                }


                System.out.println("HEFT(応答時間):" + heft_apl.getMakeSpan() + "/マシン数:" + retSet.getList().size() + "/プロセッサ数(クラスタ数):" + set2.getList().size() + "/WSL:" + heft_apl.getWorstLevel() + "/SLR:" + SLR_heft + "/Efficiency:" + e_value_heft);

                Hashtable<Long, CPU> peft_cputable = new Hashtable<Long, CPU>();

                if (cpu_table_heft.size() > cpu_table_peft.size()) {
                    peft_cputable = cpu_table_heft;
                } else {
                    peft_cputable = cpu_table_peft;
                }

                /**PEFTアルゴリズムSTART**/
                PEFT_Algorithm peft = new PEFT_Algorithm(apl_peft, filename, env_peft/*, peft_cputable*/);
                BBTask peft_apl = peft.process();
                /**PEFTアルゴリズムEND**/


                 //Schedule Length Ratio(SLR)の計算
               double SLR_peft = Calc.getRoundedValue((double) (peft_apl.getMakeSpan() / (double) mwsl_apl.getMinCriticalPath()));
                //Efficiencyの計算
                double e_value_peft = Calc.getRoundedValue((double) (peft_apl.getMaxWeight() / maxSpeed) / ((double) peft_apl.getTaskClusterList().size() * (double) peft_apl.getMakeSpan()));
                CustomIDSet retSet_peft = new CustomIDSet();
                Iterator<TaskCluster> clusterIte_peftIte = peft_apl.getTaskClusterList().values().iterator();
                while(clusterIte_peftIte.hasNext()){
                    TaskCluster cls =clusterIte_peftIte.next();
                    CPU cpu = cls.getCPU();
                    retSet_peft.add(cpu.getMachineID());

                }

                System.out.println("PEFT(応答時間):" + peft_apl.getMakeSpan() +"/マシン数:"+retSet_peft.getList().size()+ "/プロセッサ数(クラスタ数):" + peft_apl.getTaskClusterList().size() + "/WSL:" + peft_apl.getWorstLevel()+"/SLR:"+SLR_peft+"/Efficiency:"+e_value_peft);


                /*FCS_Algorithm fcs = new FCS_Algorithm(apl_fcs, filename, env_fcs);
               BBTask fcs_apl = fcs.process();
                System.out.print("FCS:"+fcs_apl.getMakeSpan()+"/ClusterNUM:"+fcs_apl.getTaskClusterList().size() + "/WSL:"+fcs_apl.getWorstLevel());
               */

                System.out.println();


            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
