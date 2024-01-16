package net.gripps.clustering.tool;

import net.gripps.clustering.common.aplmodel.*;
import net.gripps.environment.CPU;
import net.gripps.environment.Environment;
import net.gripps.grouping.CEFT.CEFTCandidateNodeSelectionManager;
import net.gripps.grouping.CandidateNodeSelectionManager;
import net.gripps.grouping.HEFT.HEFTCandidateNodeSelectionManager;
import net.gripps.grouping.HSV.HSVCandidateNodeSelectionManager;
import net.gripps.grouping.PEFT.PEFTCandidateNodeSelectionManager;
import net.gripps.mapping.ClusterMappingManager;
import net.gripps.scheduling.algorithms.heterogeneous.ceft.CEFT_Algorithm;
import net.gripps.scheduling.algorithms.heterogeneous.heft.HEFT_Algorithm;
import net.gripps.scheduling.algorithms.heterogeneous.hsv.HSV_Algorithm;
import net.gripps.scheduling.algorithms.heterogeneous.peft.PEFT_Algorithm;
import net.gripps.util.EnvLoader;

import java.io.FileInputStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;

/**
 * Author: H. Kanemitsu
 * Date: 16/07/29
 */
public class JPDC_Example {
    /**
     * 論文中の例題のためのクラスです．
     *LBCNS_HEFT, LBCNS_CEFT, LBCNS_HSV, LBCNS_PEFTをそれぞれ行い，
     * 各方式での出力ノード集合，スケジュール長を出す．
     * @param args
     */
    public static void main(String[] args) {
            int ALGORITHM_NUM = 4;
            int LOOP_NUM = 1;


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



                    /**APLのコピー**/
                    BBTask apl_heft = (BBTask)apl.deepCopy();
                    BBTask apl_ceft = (BBTask)apl.deepCopy();
                    BBTask apl_hsv = (BBTask)apl.deepCopy();
                    BBTask apl_peft =(BBTask)apl.deepCopy();


                    //ClusterMappingManager経由で，マッピングアルゴリズムを実行する．
                    //引数：設定ファイル，クラスタリング後のDAG，ピアの数
                    ClusterMappingManager cm_manager = new ClusterMappingManager(filename, apl, apl.getTaskList().size());

                    Environment env_heft = (Environment) cm_manager.getEnv().deepCopy();
                    Environment env_ceft = (Environment) cm_manager.getEnv().deepCopy();
                    Environment env_hsv = (Environment) cm_manager.getEnv().deepCopy();
                    Environment env_peft = (Environment) cm_manager.getEnv().deepCopy();

                    /***グルーピング処理 for HEFT**/
                    HEFTCandidateNodeSelectionManager grouping_heft = new HEFTCandidateNodeSelectionManager(apl_heft, filename, env_heft);
                    Hashtable<Long, CPU> cpuMap_heft = grouping_heft.deriveCaindidateCPUMapByAverageDiffBlevel2();
                    Iterator<CPU> cIte = cpuMap_heft.values().iterator();
                    System.out.print("HEFT nodes:");
                    while(cIte.hasNext()){
                        CPU cpu = cIte.next();
                        System.out.print(cpu.getOldCPUID().longValue());
                        System.out.print(":");
                    }
                    EnvLoader env_group_heft = new EnvLoader(filename,cpuMap_heft);
                    /***グルーピング処理END**/

                    LinkedList<BBTask> aplList = new LinkedList<BBTask>();

                    /**HEFTアルゴリズムSTART**/
                    HEFT_Algorithm heft = new HEFT_Algorithm(apl_heft, filename, env_group_heft);
                    BBTask retapl_heft = heft.process();
                    aplList.add(retapl_heft);
                    /**HEFTアルゴリズムEND**/

                    //Schedule Lengtmh Ratio(SLR)の計算
                    double SLR_mwsl = Calc.getRoundedValue((double) (retapl_heft.getMakeSpan() / (double) retapl_heft.getMinCriticalPath()));
                    //Efficiencyの計算
                    double e_value_mwsl = Calc.getRoundedValue((double) (retapl_heft.getMaxWeight() / maxSpeed) / ((double) retapl_heft.getTaskClusterList().size() * (double) retapl_heft.getMakeSpan()));

                    //結果から，CPUリストを取得する．
                    Hashtable<Long, CPU> cpu_table_distance = new Hashtable<Long, CPU>();

                    //クラスタを取得して，その割り当て先CPUを取得する．
                    Iterator<TaskCluster> clusterIte = retapl_heft.getTaskClusterList().values().iterator();
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


                    System.out.print("HEFT:" + retapl_heft.getMakeSpan() + ":" + mSet.getList().size() +":" + retapl_heft.getWorstLevel() + ":" + SLR_mwsl + ":" + e_value_mwsl);

//CEFT
                    CEFTCandidateNodeSelectionManager grouping_ceft = new CEFTCandidateNodeSelectionManager(apl_ceft, filename, env_ceft);
                    Hashtable<Long, CPU> cpuMap_ceft = grouping_ceft.deriveCaindidateCPUMapByCEFT2();
                    Iterator<CPU> cIte_ceft = cpuMap_ceft.values().iterator();
                    System.out.print("CEFT nodes:");
                    while(cIte_ceft.hasNext()){
                        CPU cpu = cIte_ceft.next();
                        System.out.print(cpu.getOldCPUID().longValue());
                        System.out.print(":");
                    }
                    EnvLoader env_group_ceft = new EnvLoader(filename,cpuMap_ceft);



                    Hashtable<Long, CPU> cpu_table_ceft = new Hashtable<Long, CPU>();
                  //  CMWSL_Algorithm hetero2 = new CMWSL_Algorithm(apl_grouping,filename, env_group);
                    CEFT_Algorithm ceft = new CEFT_Algorithm(apl_ceft, filename, env_group_ceft);
                    BBTask retapl_ceft = ceft.process();

                    //メイン処理を行う．

                    aplList.add(retapl_ceft);

                    //SLRの計算
                    double SLR_ceft = Calc.getRoundedValue((double) (retapl_ceft.getMakeSpan() / (double) retapl_ceft.getMinCriticalPath()));
                    //Efficiencyの計算
                    double e_value_ceft  = Calc.getRoundedValue((double) (retapl_ceft.getMaxWeight() / maxSpeed) / ((double) retapl_ceft.getTaskClusterList().size() * (double) retapl_ceft.getMakeSpan()));

                    Iterator<TaskCluster> clsIte_ceft = retapl_ceft.getTaskClusterList().values().iterator();
                    CustomIDSet retSet_ceft = new CustomIDSet();
                    CustomIDSet set_ceft = new CustomIDSet();

                    while (clsIte_ceft.hasNext()) {
                        TaskCluster cls = clsIte_ceft.next();
                        CPU cpu = cls.getCPU();
                        if (set_ceft.contains(cpu.getCpuID())) {
                            continue;
                        }
                        retSet_ceft.add(cpu.getMachineID());
                        set_ceft.add(cpu.getCpuID());

                        cpu.clear();
                        //mSet.add(cpu.getMachineID());
                        cpu_table_ceft.put(cpu.getCpuID(), cpu);
                    }


                    System.out.print(":CEFT:" + retapl_ceft.getMakeSpan() + ":" + retSet_ceft.getList().size() + ":" + retapl_ceft.getWorstLevel() + ":" + SLR_ceft + ":" + e_value_ceft);



                    System.out.println();

//HSV
                    HSVCandidateNodeSelectionManager grouping_hsv = new HSVCandidateNodeSelectionManager(apl_hsv, filename, env_hsv);
                    Hashtable<Long, CPU> cpuMap_hsv = grouping_hsv.deriveCaindidateCPUMapbyHSV();
                    // Hashtable<Long, CPU> cpuMap2 = grouping2.deriveCandidateCPUMap();
                    EnvLoader env_group_hsv  = new EnvLoader(filename,cpuMap_hsv);
                    Iterator<CPU> cIte_hsv = cpuMap_hsv.values().iterator();
                    System.out.print("HSV nodes:");
                    while(cIte_hsv.hasNext()){
                        CPU cpu = cIte_hsv.next();
                        System.out.print(cpu.getOldCPUID().longValue());
                        System.out.print(":");
                    }

                    /***グルーピング処理END**/

                    //LinkedList<BBTask> aplList = new LinkedList<BBTask>();

                    /**CMWSLアルゴリズムSTART**/
                    // BBTask mwsl_apl = HEFT_Algorithm..multiCoreProcess();
                    HSV_Algorithm hsv = new HSV_Algorithm(apl_hsv, filename,env_group_hsv );
                    BBTask hsv_apl = hsv.process();
                    aplList.add(hsv_apl);
                    /**CMWSLアルゴリズムEND**/

                    //Schedule Lengtmh Ratio(SLR)の計算
                    double SLR_hsv = Calc.getRoundedValue((double) (hsv_apl.getMakeSpan() / (double) hsv_apl.getMinCriticalPath()));
                    //Efficiencyの計算
                    double e_value_hsv = Calc.getRoundedValue((double) (hsv_apl.getMaxWeight() / maxSpeed) / ((double) hsv_apl.getTaskClusterList().size() *
                            (double) hsv_apl.getMakeSpan()));

                    //結果から，CPUリストを取得する．
                    Hashtable<Long, CPU> cpu_table_distance_hsv = new Hashtable<Long, CPU>();

                    //クラスタを取得して，その割り当て先CPUを取得する．
                    Iterator<TaskCluster> clusterIte_hsv = hsv_apl.getTaskClusterList().values().iterator();
                    CustomIDSet mSet_hsv = new CustomIDSet();
                    CustomIDSet set_hsv = new CustomIDSet();

                    while (clusterIte_hsv.hasNext()) {
                        TaskCluster cls = clusterIte_hsv.next();
                        if(cls.getTaskSet().isEmpty()){
                            continue;
                        }
                        //System.out.println("ID: "+cls.getClusterID()+"/Num:"+cls.getTaskSet().getList().size());
                        CPU cpu = cls.getCPU();

                        cpu.clear();
                        mSet_hsv.add(cpu.getMachineID());
                        cpu_table_distance_hsv.put(cpu.getCpuID(), cpu);
                        set_hsv.add(cpu.getCpuID());
                    }


                    System.out.print("HSV:" + hsv_apl.getMakeSpan() + ":" + mSet.getList().size() +":" + hsv_apl.getWorstLevel() + ":" + SLR_hsv + ":" + e_value_hsv);

//PEFT
                    PEFTCandidateNodeSelectionManager grouping_peft = new PEFTCandidateNodeSelectionManager(apl_peft, filename, env_peft);
                    Hashtable<Long, CPU> cpuMap_peft = grouping_peft.deriveCaindidateCPUMapbyPEFT();
                    // Hashtable<Long, CPU> cpuMap2 = grouping2.deriveCandidateCPUMap();
                    EnvLoader env_group_peft = new EnvLoader(filename,cpuMap_peft);
                    /***グルーピング処理END**/
                    Iterator<CPU> cIte_peft = cpuMap_peft.values().iterator();
                    System.out.print("PEFT nodes:");
                    while(cIte_peft.hasNext()){
                        CPU cpu = cIte_peft.next();
                        System.out.print(cpu.getOldCPUID().longValue());
                        System.out.print(":");
                    }


                    //LinkedList<BBTask> aplList = new LinkedList<BBTask>();

                    /**PEFTアルゴリズムSTART**/
                    // BBTask mwsl_apl = HEFT_Algorithm..multiCoreProcess();
                    PEFT_Algorithm peft = new PEFT_Algorithm(apl_peft, filename, env_group_peft);
                    BBTask retapl_peft = peft.process();
                    aplList.add(retapl_peft);
                    /**CMWSLアルゴリズムEND**/

                    //Schedule Lengtmh Ratio(SLR)の計算
                    double SLR_peft = Calc.getRoundedValue((double) (retapl_peft.getMakeSpan() / (double) retapl_peft.getMinCriticalPath()));
                    //Efficiencyの計算
                    double e_value_peft = Calc.getRoundedValue((double) (retapl_peft.getMaxWeight() / maxSpeed) / ((double) retapl_peft.getTaskClusterList().size() * (double) retapl_peft.getMakeSpan()));

                    //結果から，CPUリストを取得する．
                    Hashtable<Long, CPU> cpu_table_peft = new Hashtable<Long, CPU>();

                    //クラスタを取得して，その割り当て先CPUを取得する．
                    Iterator<TaskCluster> clusterIte_peft = retapl_peft.getTaskClusterList().values().iterator();
                    CustomIDSet mSet_peft = new CustomIDSet();
                    CustomIDSet set_peft = new CustomIDSet();

                    while (clusterIte_peft.hasNext()) {
                        TaskCluster cls = clusterIte_peft.next();
                        if(cls.getTaskSet().isEmpty()){
                            continue;
                        }
                        //System.out.println("ID: "+cls.getClusterID()+"/Num:"+cls.getTaskSet().getList().size());
                        CPU cpu = cls.getCPU();

                        cpu.clear();
                        mSet_peft.add(cpu.getMachineID());
                        cpu_table_peft.put(cpu.getCpuID(), cpu);
                        set_peft.add(cpu.getCpuID());
                    }


                    System.out.println("PEFT:" + retapl_peft.getMakeSpan() + ":" + mSet_peft.getList().size() +":" + retapl_peft.getWorstLevel() + ":" + SLR_peft + ":" + e_value_peft);




                }


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
}
