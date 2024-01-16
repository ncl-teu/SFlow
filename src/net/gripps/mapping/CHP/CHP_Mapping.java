package net.gripps.mapping.CHP;

import net.gripps.environment.CPU;
import net.gripps.mapping.Tlevel.Tlevel_Mapping;
import net.gripps.clustering.common.aplmodel.*;
import net.gripps.environment.Environment;
import net.gripps.environment.P2PEnvironment;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Author: H. Kanemitsu
 * CHPアルゴリズムによるクラスタマッピングです．
 * クラスタの先頭タスクのうち，blevelが最大のものからクラスタを選択し，
 * かつそのクラスタを含めたマッピング済みクラスタのメイクスパンをminにするCPUを
 * 割り当て先とする．
 *
 * Date: 2008/10/26
 */
public class CHP_Mapping extends Tlevel_Mapping {

    /**
     *
     * @param retApl
     * @param env
     */
    public CHP_Mapping(BBTask retApl, String file, Environment env) {
        super(retApl, file, env);
    }

    public CHP_Mapping(BBTask retApl, String file, P2PEnvironment env){
        super(retApl, file, env);

    }

    /**
     * クラスタ取得
     * blevel値に基づいて，クラスタを取得する．
     * もし同じblevel値ならば，affinityに基づいて選択する．
     *
     */
   /**
     * 何らかの優先度に基づき，クラスタを選択します．
     * @return
     */
    public  TaskCluster selectTaskCluster(){
        Iterator<Long> cIte = this.umClusterSet.iterator();
        long currentBlevel = 0;
        TaskCluster retCluster = null;

       //クラスタごとのループ
        while(cIte.hasNext()){
            Long cID = cIte.next();
            TaskCluster cluster = this.retApl.findTaskCluster(cID);
            CustomIDSet tSet = cluster.getTop_Set();
            Iterator<Long> tIte = tSet.iterator();
            long cBlevel = 0;
            //Topタスクたちに対するループ
            while(tIte.hasNext()){
                Long tID = tIte.next();
                AbstractTask topTask = this.retApl.findTaskByLastID(tID);
                long tmpBlevel = topTask.getBlevel();
                if(cBlevel <= tmpBlevel){
                    cBlevel = tmpBlevel;
                }
            }
            if(currentBlevel <= cBlevel){
                currentBlevel = cBlevel;
                retCluster = cluster;
            }
        }
        return retCluster;
    }

    public Environment getEnv() {
        return super.getEnv();    //To change body of overridden methods use File | Settings | File Templates.
    }

    public void setEnv(Environment env) {
        super.setEnv(env);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public BBTask getRetApl() {
        return super.getRetApl();    //To change body of overridden methods use File | Settings | File Templates.
    }

    public void setRetApl(BBTask retApl) {
        super.setRetApl(retApl);    //To change body of overridden methods use File | Settings | File Templates.
    }

    /**
     * マッピング処理のCHP実装です．
     * 1. クラスタのtopタスクのblevelの大きい順にクラスタを選択する
     * 2. あるスケジューリングの基づき，割り当て済みのクラスタたちの完了時刻を最小にするCPUを選択する．
     *    # 論文によると，「割り当て済みのクラスタのメイクスパンを最小にするCPUを選択する」と書いてある．
     * 3. 未割り当てクラスタたちに対して，そのblevelを更新する．
     * これの繰り返しを行う．
     *
     * @return
     */
    public BBTask mapping() {
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
                //long es5 = System.currentTimeMillis();
                //System.out.println("TIME: "+ (e5-s5));
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

    /**
     *
     * @param task
     * @param set
     */
    public long updateBlevel(AbstractTask task, CustomIDSet set){
        Long id = task.getIDVector().get(1);
        TaskCluster cluster = this.retApl.findTaskCluster(task.getClusterID());
        CPU CPU = cluster.getCPU();

        if(set.contains(id)){
            return task.getBlevel();
        }

        //以降は，初めてのblevel計算
        LinkedList<DataDependence> dsucList = task.getDsucList();
        Iterator<DataDependence> dsucIte= dsucList.iterator();
        long retBlevel = 0;

        if(dsucList.isEmpty()){
            //ENDタスクについては，blevel値を当該重み戸する。
            long ret = task.getMaxWeight()/ CPU.getSpeed();
            task.setBlevel(ret);
            set.add(id);
            return ret;
        }

        while(dsucIte.hasNext()){
            DataDependence dd = dsucIte.next();
            AbstractTask sucTask = this.retApl.findTaskByLastID(dd.getToID().get(1));
            long tmpBlevel = task.getMaxWeight()/ CPU.getSpeed() +
                    this.getNWTime(id, sucTask.getIDVector().get(1), dd.getMaxDataSize()) + this.updateBlevel(sucTask, set);
            if(retBlevel <= tmpBlevel){
                retBlevel = tmpBlevel;
            }
        }

        set.add(id);
        task.setBlevel(retBlevel);
        return retBlevel;
    }
}
