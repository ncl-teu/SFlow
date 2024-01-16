package net.gripps.mapping.level;

import net.gripps.environment.CPU;
import net.gripps.mapping.CHP.CHP_Mapping;
import net.gripps.clustering.common.aplmodel.BBTask;
import net.gripps.clustering.common.aplmodel.TaskCluster;
import net.gripps.clustering.common.aplmodel.CustomIDSet;
import net.gripps.clustering.common.aplmodel.AbstractTask;
import net.gripps.environment.Environment;

import java.util.Iterator;

/**
 * CPLBマッピングです．
 * Critical Path上のクラスタのうちで，最もデータサイズ＋クラスタサイズの大きなものを選択する．
 * そして，Partial Scheduleしてみて，最も応答時間の抑えられるマシンを選択する．
 * 具体的には，
 * 1. tlevel + blevelの最大のtopタスク（クラスタ）を選択する．
 * 2. partial scheduleをして，割り当て済みクラスタの完了時刻を最小にするようなマシンを
 *    選択する．
 * 3. そのクラスタを2で選択されたマシンへマッピングする．
 * 
 *
 * Author: H. Kanemitsu
 * Date: 2009/05/26
 */
public class Level_Mapping extends CHP_Mapping {


    /**
     *
     * @param apl
     * @param file
     * @param env
     */
    public Level_Mapping(BBTask apl, String file, Environment env) {
        super(apl, file, env);    //To change body of overridden methods use File | Settings | File Templates.
    }



    /**
     *
     * @return
     */
    public TaskCluster selectTaskCluster(){
        Iterator<Long> cIte = this.umClusterSet.iterator();
        long currentBlevel = 0;
        TaskCluster retCluster = null;

       //クラスタごとのループ
        while(cIte.hasNext()){
            Long cID = cIte.next();
            TaskCluster cluster = this.retApl.findTaskCluster(cID);
            CustomIDSet tSet = cluster.getTop_Set();
            Iterator<Long> tIte = tSet.iterator();
            long cLevel = 0;
            //Topタスクたちに対するループ
            while(tIte.hasNext()){
                Long tID = tIte.next();
                AbstractTask topTask = this.retApl.findTaskByLastID(tID);
                long tmpBlevel = topTask.getBlevel();
                long tmpTlevel = topTask.getTlevel();
                long tmpLevel = tmpTlevel + tmpBlevel;

                if(cLevel <= tmpLevel){
                    cLevel = tmpLevel;
                }
            }

            if(currentBlevel <= cLevel){
                currentBlevel = cLevel;
                retCluster = cluster;
            }
        }
        return retCluster;
        

    }

    /**
     *
     * @return
     */
    public CustomIDSet getDSClusterSet(){
        return null;
    }

    /**
     * CriticalPathを求める．
     * @return
     */
    public BBTask mapping(){
        long start2 = System.currentTimeMillis();
        //未割り当てクラスタが存在する間のループ
        while(!this.umClusterSet.isEmpty()){
            //blevel値がMaxのクラスタを取得する．
            TaskCluster cluster = this.selectTaskCluster();
            //次は，マシンの選択を行う．現在割り当て済み＋これから割り当てようとするCPUでのメイクスパンを最小にする
            //CPUを割り当て先CPUとする．
            //具体的には，「割り当て済みクラスタのmax{bottomタスクの完了時刻}を最小にするCPUのこと．
            Iterator<Long> uMappedCPUIte = this.unMappedCPU.iterator();
            long makeSpan = 1000000000;
            CPU retCPU = null;
            long start = System.currentTimeMillis();
            this.mappedClusterSet.add(cluster.getClusterID());

            while(uMappedCPUIte.hasNext()){
                Long cpuID = uMappedCPUIte.next();
                // long s5 = System.currentTimeMillis();
                CPU mac = this.env.findCPU(cpuID);

                //マシンを割り当ててみる．
                cluster.setCPU(mac);


                //部分スケジュール上でのメイクスパンを取得する．
                long tmpMakeSpan = this.calcMakeSpan(this.retApl, this.mappedClusterSet, cluster.getClusterID());

                if(makeSpan >= tmpMakeSpan){
                    makeSpan = tmpMakeSpan;
                    retCPU = mac;
                    //cluster.setCPU(mac);

                }
            }
            long end = System.currentTimeMillis();
           // System.out.println("tmpMakeSpan:"+makeSpan+ " /SEC: "+ (end-start));
            //最終的なマシンをセットする．
            cluster.setCPU(retCPU);
            retCPU.setTaskClusterID(cluster.getClusterID());
            this.unMappedCPU.remove(retCPU.getCpuID());
            this.umClusterSet.remove(cluster.getClusterID());
            ////System.out.println("おわった");

            //そして，blevel値の設定を行う．
            Iterator<Long> startIte = this.retApl.getStartTaskSet().iterator();
            CustomIDSet blevelSet = new CustomIDSet();
            //STARTタスクに対するループ
            while(startIte.hasNext()){
                Long sID = startIte.next();
                AbstractTask sTask = this.retApl.findTaskByLastID(sID);
                this.updateBlevel(sTask,blevelSet );
            }

            long endID = this.retApl.getTaskList().size();
            AbstractTask endTask = this.retApl.findTaskByLastID(new Long(endID));
            this.updateTlevel(this.retApl, endTask, new CustomIDSet());
        }

        long end2 = System.currentTimeMillis();
        //System.out.println("CHP END: TIME: "+(end2-start2));
        //後処理
        Iterator<AbstractTask> taskIte = this.retApl.taskIerator();
        while(taskIte.hasNext()){
            AbstractTask task = taskIte.next();
            task.setStartTime(-1);
        }




        return this.retApl;
    }

}
