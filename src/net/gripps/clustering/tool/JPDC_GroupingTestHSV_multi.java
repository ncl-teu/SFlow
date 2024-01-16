package net.gripps.clustering.tool;

import net.gripps.clustering.common.aplmodel.*;
import net.gripps.environment.CPU;
import net.gripps.environment.Environment;
import net.gripps.grouping.CandidateNodeSelectionManager;
import net.gripps.grouping.HSV.HSVCandidateNodeSelectionManager;
import net.gripps.mapping.ClusterMappingManager;
import net.gripps.scheduling.algorithms.heterogeneous.hsv.HSV_Algorithm;
import net.gripps.util.EnvLoader;

import java.io.FileInputStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;

/**
 * Created by kanemih on 2016/01/12.
 */
public class JPDC_GroupingTestHSV_multi {
    public static void main(String[] args) {
        int ALGORITHM_NUM = 4;
        int LOOP_NUM = 10;


        //SortedSet sSet = new SortedSet();
        try {
            //クラスタリングアルゴリズムの実行回数
            //各ループでAPLは新規生成される．
            for (int i = 0; i < LOOP_NUM; i++) {
                String filename = args[0];

                Properties prop = new Properties();
                //create input stream from file
                prop.load(new FileInputStream(filename));
                int maxSpeed = Integer.valueOf(prop.getProperty("cpu.speed.max")).intValue();

                int dagtype = Integer.valueOf(prop.getProperty("task.dagtype")).intValue();

                BBTask apl = null;
                //1: random 2: GJ
                switch (dagtype) {
                    case 1:
                        //タスクグラフを構築する
                      //タスクグラフを構築する
                        apl = AplOperator.getInstance().generateMultipleDAGs(filename);
                        break;
                    case 2:
                        GaussianOperator.getInstance().constructTask(filename);
                        //依存関係と，命令数を割り当てる．最下位にあるタスク命令数は決まっているので，読み込むのみ
                        GaussianOperator.getInstance().assignDependencyProcess();
                        apl = GaussianOperator.getInstance().getApl();
                        break;
                    case 3:
                        FFTRealOperator.getInstance().constructTask(filename);
                        //依存関係と，命令数を割り当てる．最下位にあるタスク命令数は決まっているので，読み込むのみ
                        FFTRealOperator.getInstance().assignDependencyProcess();
                        apl = FFTRealOperator.getInstance().getApl();
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


                //double CCR = Calc.getRoundedValue(aveEdgeSize/aveTaskSize);
                //  System.out.println("CCR:"+CCR);

                // BBTask LBaplCopy = (BBTask)apl.deepCopy();
                /**APLのコピー**/
                BBTask apl_grouping = (BBTask) apl.deepCopy();
                BBTask apl_distance = (BBTask) apl.deepCopy();

                //ClusterMappingManager経由で，マッピングアルゴリズムを実行する．
                //引数：設定ファイル，クラスタリング後のDAG，ピアの数
                ClusterMappingManager cm_manager = new ClusterMappingManager(filename, apl, apl.getTaskList().size());

                Environment env2 = (Environment) cm_manager.getEnv().deepCopy();
                Environment env3 = (Environment) cm_manager.getEnv().deepCopy();


                /***グルーピング処理**/
                CandidateNodeSelectionManager grouping = new CandidateNodeSelectionManager(apl_grouping, filename, env2);
                grouping.prepare();
                Hashtable<Long, CPU> cpuMap = grouping.deriveCandidateCPUMap();
                EnvLoader env_group = new EnvLoader(filename,cpuMap);

                HSVCandidateNodeSelectionManager  grouping2 = new HSVCandidateNodeSelectionManager(apl_distance, filename, env3);
                Hashtable<Long, CPU> cpuMap2 = grouping2.deriveCaindidateCPUMapbyHSV();
               // Hashtable<Long, CPU> cpuMap2 = grouping2.deriveCandidateCPUMap();
                EnvLoader env_group2 = new EnvLoader(filename,cpuMap2);
                System.out.println("mapping1:" + cpuMap.size());
               System.out.println("mapping2:"+cpuMap2.size());
                /***グルーピング処理END**/

                LinkedList<BBTask> aplList = new LinkedList<BBTask>();

                /**CMWSLアルゴリズムSTART**/
               // BBTask mwsl_apl = HEFT_Algorithm..multiCoreProcess();
                HSV_Algorithm heft = new HSV_Algorithm(apl, filename, cm_manager.getEnv());
                BBTask mwsl_apl = heft.process();
                aplList.add(mwsl_apl);
                /**CMWSLアルゴリズムEND**/

                //Schedule Lengtmh Ratio(SLR)の計算
                double SLR_mwsl = Calc.getRoundedValue((double) (mwsl_apl.getMakeSpan() / (double) mwsl_apl.getMinCriticalPath()));
                //Efficiencyの計算
                double e_value_mwsl = Calc.getRoundedValue((double) (mwsl_apl.getMaxWeight() / maxSpeed) / ((double) mwsl_apl.getTaskClusterList().size() * (double) mwsl_apl.getMakeSpan()));

                //結果から，CPUリストを取得する．
                Hashtable<Long, CPU> cpu_table_distance = new Hashtable<Long, CPU>();

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
                    cpu_table_distance.put(cpu.getCpuID(), cpu);
                    set.add(cpu.getCpuID());
                }


                System.out.print("HSV:" + mwsl_apl.getMakeSpan() + ":" + mSet.getList().size() +":" + mwsl_apl.getWorstLevel() + ":" + SLR_mwsl + ":" + e_value_mwsl);


                Hashtable<Long, CPU> cpu_table_peft = new Hashtable<Long, CPU>();
              //  CMWSL_Algorithm hetero2 = new CMWSL_Algorithm(apl_grouping,filename, env_group);
                HSV_Algorithm hetero2 = new HSV_Algorithm(apl_grouping, filename, env_group);
                //メイン処理を行う．
                BBTask ret_group = hetero2.process();
                aplList.add(ret_group);

                //SLRの計算
                double SLR_distance = Calc.getRoundedValue((double) (ret_group.getMakeSpan() / (double) ret_group.getMinCriticalPath()));
                //Efficiencyの計算
                double e_value_distance = Calc.getRoundedValue((double) (ret_group.getMaxWeight() / maxSpeed) / ((double) ret_group.getTaskClusterList().size() * (double) ret_group.getMakeSpan()));

                Iterator<TaskCluster> clsIte = ret_group.getTaskClusterList().values().iterator();
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


                System.out.print(":HSV2:" + ret_group.getMakeSpan() + ":" + retSet.getList().size() + ":" + ret_group.getWorstLevel() + ":" + SLR_distance + ":" + e_value_distance);


                Hashtable<Long, CPU> cpu_table_peft2 = new Hashtable<Long, CPU>();

                HSV_Algorithm hetero3 = new HSV_Algorithm(apl_distance, filename, env_group2);
                //メイン処理を行う．
                BBTask ret_group2 = hetero3.process();
                aplList.add(ret_group2);

                //SLRの計算
                double SLR_distance2 = Calc.getRoundedValue((double) (ret_group2.getMakeSpan() / (double) ret_group2.getMinCriticalPath()));
                //Efficiencyの計算
                double e_value_distance2 = Calc.getRoundedValue((double) (ret_group2.getMaxWeight() / maxSpeed) / ((double) ret_group2.getTaskClusterList().size() * (double) ret_group2.getMakeSpan()));

                Iterator<TaskCluster> clsIte2 = ret_group2.getTaskClusterList().values().iterator();
                CustomIDSet retSet2 = new CustomIDSet();
                CustomIDSet set3 = new CustomIDSet();

                while (clsIte2.hasNext()) {
                    TaskCluster cls = clsIte2.next();
                    CPU cpu = cls.getCPU();
                    if (set3.contains(cpu.getCpuID())) {
                        continue;
                    }
                    retSet2.add(cpu.getMachineID());
                    set3.add(cpu.getCpuID());

                    cpu.clear();
                    //mSet.add(cpu.getMachineID());
                    cpu_table_peft2.put(cpu.getCpuID(), cpu);
                }


                System.out.print(":HSV3:" + ret_group2.getMakeSpan() + ":" + retSet2.getList().size() + ":" + ret_group2.getWorstLevel() + ":" + SLR_distance2 + ":" + e_value_distance2);


                System.out.println();


            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
