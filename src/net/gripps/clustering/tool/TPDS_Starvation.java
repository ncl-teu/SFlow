package net.gripps.clustering.tool;

import net.gripps.clustering.algorithms.rac.RAC_Algorithm;
import net.gripps.clustering.algorithms.triplet.Triplet_Algorithm;
import net.gripps.clustering.common.aplmodel.*;
import net.gripps.environment.CPU;
import net.gripps.environment.Environment;
import net.gripps.mapping.ClusterMappingManager;
import net.gripps.scheduling.algorithms.RCP_Scheduling;
import net.gripps.scheduling.algorithms.heterogeneous.ceft.CEFT_Algorithm;
import net.gripps.scheduling.algorithms.heterogeneous.heft.HEFT_Algorithm;
import net.gripps.scheduling.algorithms.heterogeneous.hsv.HSV_Algorithm;
import net.gripps.scheduling.algorithms.heterogeneous.msl.MSL_Algorithm;

import java.io.FileInputStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;

/**
 * Created by kanemih on 2015/11/12.
 */
public class TPDS_Starvation {
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
                BBTask apl_msl = (BBTask)apl.deepCopy();
                BBTask apl_hsv = (BBTask)apl.deepCopy();


                BBTask apl_fcs = (BBTask) apl.deepCopy();
                BBTask apl_ceft = (BBTask)apl.deepCopy();
                BBTask apl_rac = (BBTask)apl.deepCopy();
                BBTask apl_triplet = (BBTask)apl.deepCopy();



                //ClusterMappingManager経由で，マッピングアルゴリズムを実行する．
                //引数：設定ファイル，クラスタリング後のDAG，ピアの数
                ClusterMappingManager cm_manager = new ClusterMappingManager(filename, apl, 7);
              //ClusterMappingManager cm_manager = new ClusterMappingManager(filename, apl, apl.getTaskList().size());


                /**ENVのコピー**/
                Environment env_heft = (Environment) cm_manager.getEnv().deepCopy();
                Environment env_peft = (Environment) cm_manager.getEnv().deepCopy();
                Environment env_ceft = (Environment) cm_manager.getEnv().deepCopy();
                Environment env_msl = (Environment) cm_manager.getEnv().deepCopy();
                Environment env_hsv = (Environment) cm_manager.getEnv().deepCopy();


                Environment env_fcs = (Environment) cm_manager.getEnv().deepCopy();
                Environment env_rac = (Environment)cm_manager.getEnv().deepCopy();
                Environment env_triplet =   (Environment)cm_manager.getEnv().deepCopy();


                LinkedList<BBTask> aplList = new LinkedList<BBTask>();

                /**MWSLアルゴリズムSTART**/
                BBTask mwsl_apl = cm_manager.multiCoreProcess();
                aplList.add(mwsl_apl);
                /**MWSLアルゴリズムEND**/

                //Schedule Lengtmh Ratio(SLR)の計算
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


                System.out.print("CMWSL:" + mwsl_apl.getMakeSpan() + ":" + mSet.getList().size() +":" + mwsl_apl.getWorstLevel() + ":" + SLR_mwsl + ":" + e_value_mwsl);



              /*FCS_Algorithm fcs = new FCS_Algorithm(apl_fcs, filename, env_fcs);
               BBTask fcs_apl = fcs.process();
                System.out.print("FCS:"+fcs_apl.getMakeSpan()+"/ClusterNUM:"+fcs_apl.getTaskClusterList().size() + "/WSL:"+fcs_apl.getWorstLevel());
              */

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


                System.out.print(":HEFT:" + heft_apl.getMakeSpan() + ":" + retSet.getList().size() + ":" + heft_apl.getWorstLevel() + ":" + SLR_heft + ":" + e_value_heft);

                Hashtable<Long, CPU> peft_cputable = new Hashtable<Long, CPU>();

                if (cpu_table_heft.size() > cpu_table_peft.size()) {
                    peft_cputable = cpu_table_heft;
                } else {
                    peft_cputable = cpu_table_peft;
                }

                /**PEFTアルゴリズムSTART**/
                //PEFT_Algorithm peft = new PEFT_Algorithm(apl_peft, filename, env_peft/*, peft_cputable*/);
              /*  BBTask peft_apl = peft.process();



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


                System.out.print(":PEFT:" + peft_apl.getMakeSpan() +":"+retSet_peft.getList().size() + ":" + peft_apl.getWorstLevel()+":"+SLR_peft+":"+e_value_peft);
                  */

                /** CEFTアルゴリズム**/
                CEFT_Algorithm ceft = new CEFT_Algorithm(apl_ceft, filename, env_ceft/*, peft_cputable*/);
                BBTask ceft_apl = ceft.process();

                double SLR_ceft = Calc.getRoundedValue((double) (ceft_apl.getMakeSpan() / (double) mwsl_apl.getMinCriticalPath()));
                //Efficiencyの計算
                double e_value_ceft = Calc.getRoundedValue((double) (ceft_apl.getMaxWeight() / maxSpeed) / ((double) ceft_apl.getTaskClusterList().size() * (double) ceft_apl.getMakeSpan()));
                CustomIDSet retSet_ceft = new CustomIDSet();
                Iterator<TaskCluster> clusterIte_ceftIte = ceft_apl.getTaskClusterList().values().iterator();
                while(clusterIte_ceftIte.hasNext()){
                    TaskCluster cls =clusterIte_ceftIte.next();
                    CPU cpu = cls.getCPU();
                    retSet_ceft.add(cpu.getMachineID());

                }

                System.out.print(":CEFT:" + ceft_apl.getMakeSpan() +":"+retSet_ceft.getList().size() + ":" + ceft_apl.getWorstLevel()+":"+SLR_ceft+":"+e_value_ceft);

                //MSLアルゴリズム
                MSL_Algorithm msl = new MSL_Algorithm(apl_msl, filename, env_msl/*, peft_cputable*/);
                BBTask msl_apl = msl.process();



                //Schedule Length Ratio(SLR)の計算
                double SLR_msl = Calc.getRoundedValue((double) (msl_apl.getMakeSpan() / (double) mwsl_apl.getMinCriticalPath()));
                //Efficiencyの計算
                double e_value_msl = Calc.getRoundedValue((double) (msl_apl.getMaxWeight() / maxSpeed) / ((double) msl_apl.getTaskClusterList().size() * (double) msl_apl.getMakeSpan()));
                CustomIDSet retSet_msl = new CustomIDSet();
                Iterator<TaskCluster> clusterIte_mslIte = msl_apl.getTaskClusterList().values().iterator();
                while(clusterIte_mslIte.hasNext()){
                    TaskCluster cls =clusterIte_mslIte.next();
                    CPU cpu = cls.getCPU();
                    retSet_msl.add(cpu.getMachineID());

                }


                System.out.print(":MSL:" + msl_apl.getMakeSpan() +":"+retSet_msl.getList().size() + ":" + msl_apl.getWorstLevel()+":"+SLR_msl+":"+e_value_msl);

                //HSVアルゴリズム
                HSV_Algorithm hsv = new HSV_Algorithm(apl_hsv, filename, env_hsv/*, peft_cputable*/);
                BBTask hsv_apl = hsv.process();

                //Schedule Length Ratio(SLR)の計算
                double SLR_hsv = Calc.getRoundedValue((double) (hsv_apl.getMakeSpan() / (double) mwsl_apl.getMinCriticalPath()));
                //Efficiencyの計算
                double e_value_hsv = Calc.getRoundedValue((double) (hsv_apl.getMaxWeight() / maxSpeed) / ((double) hsv_apl.getTaskClusterList().size() * (double) hsv_apl.getMakeSpan()));
                CustomIDSet retSet_hsv = new CustomIDSet();
                Iterator<TaskCluster> clusterIte_hsvIte = msl_apl.getTaskClusterList().values().iterator();
                while(clusterIte_hsvIte.hasNext()){
                    TaskCluster cls =clusterIte_hsvIte.next();
                    CPU cpu = cls.getCPU();
                    retSet_hsv.add(cpu.getMachineID());

                }


                System.out.print(":HSV:" + hsv_apl.getMakeSpan() +":"+retSet_hsv.getList().size() + ":" + hsv_apl.getWorstLevel()+":"+SLR_hsv+":"+e_value_hsv);
                //System.out.println();

                /**RACアルゴリズム**/
           //     RAC_Algorithm rac = new RAC_Algorithm(apl_rac, filename, env_rac/*, peft_cputable*/);
             /*   BBTask rac_apl = rac.process();
                RCP_Scheduling rcp = new RCP_Scheduling(filename, rac_apl, env_rac);
                BBTask rac_retapl = rcp.process();
                Iterator<TaskCluster> clusterIte_racIte = rac_retapl.getTaskClusterList().values().iterator();
                CustomIDSet retSet_rac = new CustomIDSet();
                double SLR_rac = Calc.getRoundedValue((double) (rac_retapl.getMakeSpan() / (double) mwsl_apl.getMinCriticalPath()));
                 //Efficiencyの計算
                 double e_value_rac = Calc.getRoundedValue((double) (rac_retapl.getMaxWeight() / maxSpeed) / ((double) rac_retapl.getTaskClusterList().size() * (double) rac_retapl.getMakeSpan()));

                while(clusterIte_racIte.hasNext()){
                    TaskCluster cls =clusterIte_racIte.next();
                    CPU cpu = cls.getCPU();
                    retSet_rac.add(cpu.getMachineID());

                }
                System.out.print(":RAC :" + rac_retapl.getMakeSpan() +":"+retSet_rac.getList().size()+  ":" + rac_retapl.getWorstLevel()+":"+SLR_rac+":"+e_value_rac);
*/

                /** Tripletアルゴリズム**/
          /*     Triplet_Algorithm triplet = new Triplet_Algorithm(apl_triplet, filename, env_triplet);
                BBTask triplet_apl = triplet.process();

                RCP_Scheduling rcp_triplet = new RCP_Scheduling(filename, triplet_apl, env_triplet);
                BBTask triplet_retapl = rcp_triplet.process();

                double SLR_triplet = Calc.getRoundedValue((double) (triplet_retapl.getMakeSpan() / (double) mwsl_apl.getMinCriticalPath()));
                 //Efficiencyの計算
                 double e_value_triplet = Calc.getRoundedValue((double) (triplet_retapl.getMaxWeight() / maxSpeed) / ((double) triplet_retapl.getTaskClusterList().size() * (double) triplet_retapl.getMakeSpan()));
                Iterator<TaskCluster> clusterIte_tripletIte = triplet_retapl.getTaskClusterList().values().iterator();
                CustomIDSet retSet_triplet = new CustomIDSet();


                while(clusterIte_tripletIte.hasNext()){
                    TaskCluster cls =clusterIte_tripletIte.next();
                    CPU cpu = cls.getCPU();
                    retSet_triplet.add(cpu.getMachineID());

                }
                System.out.println(":Triplet:" + triplet_retapl.getMakeSpan() +":"+retSet_triplet.getList().size() + triplet_retapl.getTaskClusterList().size() + ":" + triplet_retapl.getWorstLevel()+":"+SLR_triplet+":"+e_value_triplet);
*/

                   System.out.println();


            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
