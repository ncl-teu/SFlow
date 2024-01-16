package net.gripps.clustering.tool;

import net.gripps.clustering.algorithms.mwsl_delta.CMWSL_Algorithm;
import net.gripps.clustering.common.aplmodel.*;
import net.gripps.environment.CPU;
import net.gripps.environment.Environment;
import net.gripps.grouping.CandidateNodeSelectionManager;
import net.gripps.grouping.HEFT.HEFTCandidateNodeSelectionManager;
import net.gripps.mapping.ClusterMappingManager;
import net.gripps.scheduling.algorithms.heterogeneous.heft.HEFT_Algorithm;
import net.gripps.scheduling.algorithms.heterogeneous.mwsl.MWSL_RCPScheduling;
import net.gripps.util.EnvLoader;

import java.io.FileInputStream;
import java.util.*;

/**
 * Created by kanemih on 2016/01/12.
 */
public class JPDC_DAX_HEFT_multi {
    public static void main(String[] args) {
        int ALGORITHM_NUM = 4;
        int LOOP_NUM = 20;


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
                //DAXから読み込む
                BBTask APL = DAXOperator.getInstance().generateMultipleDAGs(filename);
                apl = DAXOperator.getInstance().getApl();
                //Job集合
                Hashtable<Long, JobInfo> multiList = DAXOperator.getInstance().getMultiList();


                //double CCR = Calc.getRoundedValue(aveEdgeSize/aveTaskSize);
                //  System.out.println("CCR:"+CCR);

                // BBTask LBaplCopy = (BBTask)apl.deepCopy();
                /**APLのコピー**/
                BBTask apl_grouping = (BBTask) apl.deepCopy();
                BBTask apl_priority = (BBTask) apl.deepCopy();

                BBTask apl_homo = (BBTask) apl.deepCopy();
                BBTask apl_com = (BBTask) apl.deepCopy();

                //ClusterMappingManager経由で，マッピングアルゴリズムを実行する．
                //引数：設定ファイル，クラスタリング後のDAG，ピアの数
                ClusterMappingManager cm_manager = new ClusterMappingManager(filename, apl, apl.getTaskList().size());

                Environment allnodes_env = (Environment) cm_manager.getEnv().deepCopy();
                Environment env2 = (Environment) cm_manager.getEnv().deepCopy();
                Environment env3 = (Environment) cm_manager.getEnv().deepCopy();
                Environment env_homo = (Environment) cm_manager.getEnv().deepCopy();
                Environment env_com = (Environment) cm_manager.getEnv().deepCopy();
                Environment envall_single = (Environment) cm_manager.getEnv().deepCopy();


                /***グルーピング処理**/
                CandidateNodeSelectionManager grouping = new CandidateNodeSelectionManager(apl_grouping, filename, env2);
                grouping.prepare();
                Hashtable<Long, CPU> cpuMap = grouping.deriveCandidateCPUMap();
                EnvLoader env_group = new EnvLoader(filename, cpuMap);
                EnvLoader env_group_single = (EnvLoader) env_group.deepCopy();


                HEFTCandidateNodeSelectionManager grouping2 = new HEFTCandidateNodeSelectionManager(apl_priority, filename, env3);
                Hashtable<Long, CPU> cpuMap2 = grouping2.deriveCaindidateCPUMapByAverageDiffBlevel2();
                // Hashtable<Long, CPU> cpuMap2 = grouping2.deriveCandidateCPUMap();
                EnvLoader env_group2 = new EnvLoader(filename, cpuMap2);
                EnvLoader env_group_priority_single = (EnvLoader) env_group2.deepCopy();


                //  System.out.println("mapping1:" + cpuMap.size());
                //System.out.println("mapping2:"+cpuMap2.size());

                /**SIMILAR**/
                CandidateNodeSelectionManager grouping_homo = new CandidateNodeSelectionManager(apl_homo, filename, env_homo);
                grouping_homo.prepare();
                Hashtable<Long, CPU> cpuMap_homo = grouping_homo.deriveCaindidateCPUMapByDistance();
                EnvLoader env_group_homo = new EnvLoader(filename, cpuMap_homo);
                EnvLoader env_group_homo_single = (EnvLoader) env_group_homo.deepCopy();


                /**COM**/
                CandidateNodeSelectionManager grouping_com = new CandidateNodeSelectionManager(apl_com, filename, env_com);
                grouping_com.prepare();
                Hashtable<Long, CPU> cpuMap_com = grouping_com.deriveCaindidateCPUMapByCom();
                EnvLoader env_group_com = new EnvLoader(filename, cpuMap_com);
                EnvLoader env_group_com_single = (EnvLoader) env_group_com.deepCopy();

                /***グルーピング処理END**/

                LinkedList<BBTask> aplList = new LinkedList<BBTask>();

                /**HEFTアルゴリズムSTART**/
                // BBTask mwsl_apl = HEFT_Algorithm..multiCoreProcess();
                HEFT_Algorithm heft = new HEFT_Algorithm(apl, filename, allnodes_env);
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
                    if (cls.getTaskSet().isEmpty()) {
                        continue;
                    }
                    //System.out.println("ID: "+cls.getClusterID()+"/Num:"+cls.getTaskSet().getList().size());
                    CPU cpu = cls.getCPU();

                    cpu.clear();
                    mSet.add(cpu.getMachineID());
                    cpu_table_distance.put(cpu.getCpuID(), cpu);
                    set.add(cpu.getCpuID());
                }


                System.out.print("AllNodes:" + mwsl_apl.getMakeSpan() + ":" + mSet.getList().size() + ":" + SLR_mwsl);


                Hashtable<Long, CPU> cpu_table_peft = new Hashtable<Long, CPU>();
                //  CMWSL_Algorithm hetero2 = new CMWSL_Algorithm(apl_grouping,filename, env_group);
                HEFT_Algorithm hetero2 = new HEFT_Algorithm(apl_grouping, filename, env_group);
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


                System.out.print(":DEFAULT:" + ret_group.getMakeSpan() + ":" + retSet.getList().size() + ":" + SLR_distance);


                Hashtable<Long, CPU> cpu_table_peft2 = new Hashtable<Long, CPU>();

                HEFT_Algorithm hetero3 = new HEFT_Algorithm(apl_priority, filename, env_group2);
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


                System.out.print(":Priority:" + ret_group2.getMakeSpan() + ":" + retSet2.getList().size() + ":" + SLR_distance2);


                Hashtable<Long, CPU> cpu_table_homo = new Hashtable<Long, CPU>();
                HEFT_Algorithm hetero_homo = new HEFT_Algorithm(apl_homo, filename, env_group_homo);

                //メイン処理を行う．
                BBTask ret_group_homo = hetero_homo.process();
                aplList.add(ret_group_homo);

                //SLRの計算
                double SLR_homo = Calc.getRoundedValue((double) (ret_group_homo.getMakeSpan() / (double) ret_group_homo.getMinCriticalPath()));
                //Efficiencyの計算
                double e_value_homo = Calc.getRoundedValue((double) (ret_group_homo.getMaxWeight() / maxSpeed) / ((double) ret_group_homo.getTaskClusterList().size() * (double) ret_group_homo.getMakeSpan()));

                Iterator<TaskCluster> clsIte_homo = ret_group_homo.getTaskClusterList().values().iterator();
                CustomIDSet retSet_homo = new CustomIDSet();
                CustomIDSet set_homo = new CustomIDSet();

                while (clsIte_homo.hasNext()) {
                    TaskCluster cls = clsIte_homo.next();
                    CPU cpu = cls.getCPU();
                    if (set_homo.contains(cpu.getCpuID())) {
                        continue;
                    }
                    retSet_homo.add(cpu.getMachineID());
                    set_homo.add(cpu.getCpuID());

                    cpu.clear();
                    //mSet.add(cpu.getMachineID());
                    cpu_table_homo.put(cpu.getCpuID(), cpu);
                }


                System.out.print(":SIMILAR:" + ret_group_homo.getMakeSpan() + ":" + retSet_homo.getList().size() + ":" + SLR_homo);

                Hashtable<Long, CPU> cpu_table_com = new Hashtable<Long, CPU>();


                HEFT_Algorithm hetero_com = new HEFT_Algorithm(apl_com, filename, env_group_com);

                //メイン処理を行う．
                BBTask ret_group_com = hetero_com.process();

                aplList.add(ret_group_com);

                //SLRの計算
                double SLR_com = Calc.getRoundedValue((double) (ret_group_com.getMakeSpan() / (double) ret_group_com.getMinCriticalPath()));
                //Efficiencyの計算
                double e_value_com = Calc.getRoundedValue((double) (ret_group_com.getMaxWeight() / maxSpeed) / ((double) ret_group_com.getTaskClusterList().size() * (double) ret_group_com.getMakeSpan()));

                Iterator<TaskCluster> clsIte_com = ret_group_com.getTaskClusterList().values().iterator();
                CustomIDSet retSet_com = new CustomIDSet();
                CustomIDSet set_com = new CustomIDSet();

                while (clsIte_com.hasNext()) {
                    TaskCluster cls = clsIte_com.next();
                    CPU cpu = cls.getCPU();
                    if (set_com.contains(cpu.getCpuID())) {
                        continue;
                    }
                    retSet_com.add(cpu.getMachineID());
                    set_com.add(cpu.getCpuID());

                    cpu.clear();
                    //mSet.add(cpu.getMachineID());
                    cpu_table_com.put(cpu.getCpuID(), cpu);
                }


                System.out.print(":COM:" + ret_group_com.getMakeSpan() + ":" + retSet_com.getList().size() + ":" + SLR_com);


                //各CPU部分集合を用いて, multiList内の各ジョブをスケジューリングする．
                int len = multiList.size();
                Enumeration<Long> jobIte = multiList.keys();
                int size = multiList.size();
                double slowdown_allnodes = 0;
                double[] sdall_list = new double[len];

                double slowdown_default = 0;
                double[] sddefault_list = new double[len];

                double slowdown_similar = 0;
                double[] sdsimilar_list = new double[len];

                double slowdown_com = 0;
                double[] sdcom_list = new double[len];

                double slowdown_priority = 0;
                double[] sdpriority_list = new double[len];

                int jobidx = 0;

                while (jobIte.hasMoreElements()) {
                    Long endID = jobIte.nextElement();
                    JobInfo jinfo = multiList.get(endID);
                    BBTask single_apl = DAXOperator.getInstance().loadDAXFromFile(jinfo.getfName(), filename, jinfo.getCCR());

                    //BBTask APL = DAXOperator.getInstance().assignDependencyProcess();
                    //   single_apl = DAXOperator.getInstance().getApl();
                    //AllNodes用

                    envall_single = (Environment) cm_manager.getEnv().deepCopy();
                    BBTask job_allnodes = (BBTask) single_apl.deepCopy();
                    HEFT_Algorithm allnodes = new HEFT_Algorithm(job_allnodes, filename, envall_single);
                    BBTask ret_allnodes = allnodes.process();

                    long mkSpan_allnodes = ret_allnodes.getMakeSpan();
                    AbstractTask eTask_allnodes = mwsl_apl.findTaskByLastID(endID);
                    TaskCluster cls_allnodes = mwsl_apl.findTaskCluster(eTask_allnodes.getClusterID());
                    CPU cpu_allnodes = cls_allnodes.getCPU();
                    long end_allnodes = eTask_allnodes.getStartTime() + eTask_allnodes.getMaxWeight() / cpu_allnodes.getSpeed();
                    slowdown_allnodes = +Calc.getRoundedValue((double) mkSpan_allnodes / (double) end_allnodes);
                    sdall_list[jobidx] = Calc.getRoundedValue((double) mkSpan_allnodes / (double) end_allnodes);

                    //System.out.println("MakeSpan:"+ret_allnodes2.getMakeSpan());

                    //LBCNS_DEFAULT用
                    env_group_single = (EnvLoader) env_group.deepCopy();
                    BBTask job_default = (BBTask) single_apl.deepCopy();
                    HEFT_Algorithm lbcns_default = new HEFT_Algorithm(job_default, filename, env_group_single);
                    BBTask ret_default = lbcns_default.process();

                    long mkSpan_default = ret_default.getMakeSpan();
                    AbstractTask eTask_default = ret_group.findTaskByLastID(endID);
                    TaskCluster cls_default = ret_group.findTaskCluster(eTask_default.getClusterID());
                    CPU cpu_default = cls_default.getCPU();
                    long end_default = eTask_default.getStartTime() + eTask_default.getMaxWeight() / cpu_default.getSpeed();
                    slowdown_default = +Calc.getRoundedValue((double) mkSpan_default / (double) end_default);
                    sddefault_list[jobidx] = Calc.getRoundedValue((double) mkSpan_default / (double) end_default);

                    //PRIORITY用
                    env_group_priority_single = (EnvLoader) env_group2.deepCopy();
                    BBTask job_priority = (BBTask) single_apl.deepCopy();
                    HEFT_Algorithm lbcns_priority = new HEFT_Algorithm(job_priority, filename, env_group_priority_single);
                    BBTask ret_priority = lbcns_priority.process();
                    long mkSpan_priority = ret_priority.getMakeSpan();
                    AbstractTask eTask_priority = ret_group2.findTaskByLastID(endID);
                    TaskCluster cls_priority = ret_group2.findTaskCluster(eTask_priority.getClusterID());
                    CPU cpu_priority = cls_priority.getCPU();
                    long end_priority = eTask_priority.getStartTime() + eTask_priority.getMaxWeight() / cpu_priority.getSpeed();
                    slowdown_priority = +Calc.getRoundedValue((double) mkSpan_priority / (double) end_priority);
                    sdpriority_list[jobidx] = Calc.getRoundedValue((double) mkSpan_priority / (double) end_priority);

                    //SIMILAR用
                    env_group_homo_single = (EnvLoader) env_group_homo.deepCopy();
                    BBTask job_similar = (BBTask) single_apl.deepCopy();
                    HEFT_Algorithm lbcns_similar = new HEFT_Algorithm(job_similar, filename, env_group_homo_single);
                    BBTask ret_similar = lbcns_similar.process();
                    long mkSpan_similar = ret_similar.getMakeSpan();
                    AbstractTask eTask_similar = ret_group_homo.findTaskByLastID(endID);
                    TaskCluster cls_similar = ret_group_homo.findTaskCluster(eTask_similar.getClusterID());
                    CPU cpu_similar = cls_similar.getCPU();
                    long end_similar = eTask_similar.getStartTime() + eTask_similar.getMaxWeight() / cpu_similar.getSpeed();
                    slowdown_similar = +Calc.getRoundedValue((double) mkSpan_similar / (double) end_similar);
                    sdsimilar_list[jobidx] = Calc.getRoundedValue((double) mkSpan_similar / (double) end_similar);

                    //COM用
                    env_group_com_single = (EnvLoader) env_group_com.deepCopy();

                    BBTask job_com = (BBTask) single_apl.deepCopy();
                    HEFT_Algorithm lbcns_com = new HEFT_Algorithm(job_com, filename, env_group_com_single);
                    BBTask ret_com = lbcns_com.process();
                    long mkSpan_com = ret_com.getMakeSpan();
                    AbstractTask eTask_com = ret_group_com.findTaskByLastID(endID);
                    TaskCluster cls_com = ret_group_com.findTaskCluster(eTask_com.getClusterID());
                    CPU cpu_com = cls_com.getCPU();
                    long end_com = eTask_com.getStartTime() + eTask_com.getMaxWeight() / cpu_com.getSpeed();
                    slowdown_com = +Calc.getRoundedValue((double) mkSpan_com / (double) end_com);
                    sdcom_list[jobidx] = Calc.getRoundedValue((double) mkSpan_com / (double) end_com);
                    jobidx++;

                }
/*
                System.out.print(":allnodes_single:"+Calc.getRoundedValue((double) slowdown_allnodes/(double)size));
                System.out.print(":defualt_single:"+Calc.getRoundedValue((double) slowdown_default/(double)size));
                System.out.print(":priority_single:"+Calc.getRoundedValue((double) slowdown_priority/(double)size));
                System.out.print(":similar_single:"+Calc.getRoundedValue((double) slowdown_similar/(double)size));
                System.out.print(":similar_com:"+Calc.getRoundedValue((double) slowdown_com/(double)size));
                */
                double ave_allnodes = Calc.getRoundedValue((double) slowdown_allnodes / (double) size);
                double ave_default = Calc.getRoundedValue((double) slowdown_default / (double) size);
                double ave_priority = Calc.getRoundedValue((double) slowdown_priority / (double) size);
                double ave_similar = Calc.getRoundedValue((double) slowdown_similar / (double) size);
                double ave_com = Calc.getRoundedValue((double) slowdown_com / (double) size);
                double total_allnodes = 0;
                double total_default = 0;
                double total_priority = 0;
                double total_similar = 0;
                double total_com = 0;

                for (int j = 0; j < len; j++) {
                    total_allnodes += Math.pow(Math.abs(sdall_list[j] - ave_allnodes), 2);
                    total_default += Math.pow(Math.abs(sddefault_list[j] - ave_default), 2);
                    total_priority += Math.pow(Math.abs(sdpriority_list[j] - ave_priority), 2);
                    total_similar += Math.pow(Math.abs(sdsimilar_list[j] - ave_similar), 2);
                    total_com += Math.pow(Math.abs(sdcom_list[j] - ave_com), 2);
                }
                System.out.print(":allnodes_single:" + Calc.getRoundedValue((double) slowdown_allnodes / (double) size) + ":" + Calc.getRoundedValue(total_allnodes / (double) size));
                System.out.print(":defualt_single:" + Calc.getRoundedValue((double) slowdown_default / (double) size) + ":" + Calc.getRoundedValue(total_default / (double) size));
                System.out.print(":priority_single:" + Calc.getRoundedValue((double) slowdown_priority / (double) size) + ":" + Calc.getRoundedValue(total_priority / (double) size));
                System.out.print(":similar_single:" + Calc.getRoundedValue((double) slowdown_similar / (double) size) + ":" + Calc.getRoundedValue(total_similar / (double) size));
                System.out.print(":similar_com:" + Calc.getRoundedValue((double) slowdown_com / (double) size) + ":" + Calc.getRoundedValue(total_com / (double) size));

                System.out.println();
                multiList.clear();


            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
