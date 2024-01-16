package net.gripps.clustering.tool;

import net.gripps.clustering.algorithms.ClusteringAlgorithmManager;
import net.gripps.clustering.common.aplmodel.AplOperator;
import net.gripps.clustering.common.aplmodel.BBTask;
import net.gripps.clustering.common.aplmodel.FFTOperator;
import net.gripps.clustering.common.aplmodel.GaussianOperator;
import net.gripps.mapping.ClusterMappingManager;

import java.io.FileInputStream;
import java.util.LinkedList;
import java.util.Properties;

/**
 * Author: H. Kanemitsu
 * Date: 11/06/13
 */
public class P2PTest {
        public static void main(String[] args){
        int ALGORITHM_NUM = 4;
        int LOOP_NUM = 5;
        long sum_optDelta = 0;
        int taskNum = 0;

        try {
            //クラスタリングアルゴリズムの実行回数
            //各ループでAPLは新規生成される．
            for (int i = 0; i < LOOP_NUM; i++) {
                //コマンドラインから，設定ファイルを読み込む
                String filename = args[0];
                Properties prop = new Properties();
                //create input stream from file
                prop.load(new FileInputStream(filename));
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
                //BBTask LBaplCopy = (BBTask)apl.deepCopy();

                //まずは，MWSL-δクラスタリングを実行する．そして，その結果のDAGをもらう．
                LinkedList<BBTask> aplList = ClusteringAlgorithmManager.getInstance().process(filename);
                BBTask retApl = aplList.get(0);
                //マシン数:
                int machineNum = aplList.get(0).getTaskClusterList().size();

                //ClusterMappingManager経由で，マッピングアルゴリズムを実行する．
                //引数：設定ファイル，クラスタリング後のDAG，ピアの数
                ClusterMappingManager cm_manager = new ClusterMappingManager(filename, retApl, retApl.getTaskList().size());
                //マッピング処理
                cm_manager.process();
                System.out.println("test");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
