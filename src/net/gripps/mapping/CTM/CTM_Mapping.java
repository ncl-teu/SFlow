package net.gripps.mapping.CTM;

import net.gripps.environment.CPU;
import net.gripps.mapping.AbstractMapping;
import net.gripps.clustering.common.aplmodel.BBTask;
import net.gripps.clustering.common.aplmodel.TaskCluster;
import net.gripps.clustering.common.aplmodel.AbstractTask;
import net.gripps.clustering.common.aplmodel.DataDependence;
import net.gripps.environment.Environment;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Communication Traffic Minimizing手法に基づくクラスタマッピングです．
 * 異なるクラスタ（かつ未割り当て）に属するタスク間で，最もデータ転送サイズの大きな
 * タスク組（つまり，クラスタ組）を選択する．
 * そして，それぞれFrom/TOのクラスタとして，Link情報にもそれに応じたCPUの組を（未割り当ての中から）選択する．
 * そして2クラスタそれぞれを，CPUへマッピングする．
 * 
 * Author: H. Kanemitsu
 * Date: 2009/05/24
 */
public class CTM_Mapping extends AbstractMapping {

    public CTM_Mapping(BBTask task, String file) {
        super(task, file);
    }

    public CTM_Mapping(BBTask apl, String file, Environment env) {
        super(apl, file, env);
    }

    /**
     * 未割り当てのクラスタのうち，from/toとなる2クラスタを選択する．
     * @return
     */
    public HashMap<String, Long> selectClusters(){

        Iterator<Long> uCIte = this.umClusterSet.iterator();
        HashMap<String, Long> retMap = new HashMap<String, Long>();

        long retDataSize = 0;
        //クラスタに対するループ
        while(uCIte.hasNext()){
            Long id = uCIte.next();
            TaskCluster cluster = this.retApl.findTaskCluster(id);
            Iterator<Long> taskIte = cluster.getOut_Set().iterator();

            long maxDataInCluster = 0;
            //Outタスクに対するループ(クラスタ内のループ）
            while(taskIte.hasNext()){
                Long tID = taskIte.next();
                AbstractTask task = this.retApl.findTaskByLastID(tID);
                //もしfromタスクがENDタスクであれば，飛ばす．
                if(task.getDsucList().isEmpty()){
                    continue;
                }
                Long toID = new Long(0);

                Iterator<DataDependence> dsucIte = task.getDsucList().iterator();
                //Outタスクの後続タスクたちに対するループ
                while(dsucIte.hasNext()){
                    DataDependence dd = dsucIte.next();
                    AbstractTask toTask = this.retApl.findTaskByLastID(dd.getToID().get(1));
                    //異なるクラスタ同士の場合に，比較計算を行う
                    if(id.longValue() != toTask.getClusterID().longValue()){
                        if(maxDataInCluster <= dd.getMaxDataSize()){
                            maxDataInCluster = dd.getMaxDataSize();
                            toID = toTask.getClusterID();
                        }
                    }
                }
                if(maxDataInCluster >= retDataSize){
                    retMap.put("FROM", id);
                    retMap.put("TO",toID);
                   // System.out.println("FROM_ID:"+id+" TO_ID:"+toID);

                }
            }
        }


        //System.out.println("RETURN:"+retMap.get("FROM") + "/"+retMap.get("TO"));
        return retMap;
        
    }

    /**
     *
     * @return
     */
    public TaskCluster selectTaskCluster2(){
        Iterator<Long> mIte = this.umClusterSet.iterator();
        long retValue = 0;
        TaskCluster retCluster = null;

        //クラスタたちに対するループ
        while(mIte.hasNext()){
            Long mID = mIte.next();
            TaskCluster cluster = this.retApl.findTaskCluster(mID);
            Iterator<Long> outIte = cluster.getOut_Set().iterator();
            long sumValue = 0;
            long outDegNum = 0;
            //outタスクたちに対するループ
            while(outIte.hasNext()){
                Long oID = outIte.next();
                AbstractTask outTask = this.retApl.findTaskByLastID(oID);
                LinkedList<DataDependence> dsucList = outTask.getDsucList();
                Iterator<DataDependence> dsucIte = dsucList.iterator();
                //outタスクの後続タスクたちに対するループ
                while(dsucIte.hasNext()){
                    outDegNum++;
                    DataDependence dd = dsucIte.next();
                    AbstractTask dsucTask = this.retApl.findTaskByLastID(dd.getToID().get(1));
                    if(dsucTask.getClusterID().longValue() != mID.longValue()){
                        sumValue += dd.getMaxDataSize();
                        /*
                        long tmpValue = dd.getMaxDataSize();
                        if(tmpValue >= retValue){
                            retValue = tmpValue;
                            retCluster = cluster;
                        }*/

                    }
                }


            }
            //データサイズの平均値による比較
            long ave_value = sumValue/outDegNum;
            if(retValue <= ave_value){
                retValue = ave_value;
                retCluster = cluster;
            }
        }

        //ENDクラスタであるかどうかのチェック
        if(this.umClusterSet.getList().size() == 1){
            Long retID =  this.umClusterSet.getList().get(0);
            retCluster = this.retApl.findTaskCluster(retID);

        }

        return retCluster;
    }

    /**
     *
     * @return
     */
    public BBTask mapping(){

        //未割り当てクラスタが存在する間のループ
        while(!this.umClusterSet.isEmpty()){
            //ボトルネックとなっているデータサイズを持つクラスタを選択する．
            TaskCluster  cluster = this.selectTaskCluster2();
            CPU CPU = this.selectMachinePair2();
            cluster.setCPU(CPU);
            CPU.setTaskClusterID(cluster.getClusterID());
            this.umClusterSet.remove(cluster.getClusterID());
            this.unMappedCPU.remove(CPU.getCpuID());
            
        }
        //チェック
      /*  Iterator<TaskCluster> cIte = this.retApl.clusterIterator();
        while(cIte.hasNext()){
            TaskCluster cluster = cIte.next();
            //System.out.println(cluster.getCPU().getCpuID().longValue());
        }
        */
        //System.out.println("CTM END");

        return this.retApl;

    }

    /**
     *
     * @return
     */
    public CPU selectMachinePair2(){
        Iterator<Long> mIte = this.unMappedCPU.iterator();
        long maxLink = 0;
        CPU CPU = new CPU();

        HashMap<String, Long> retMap = new HashMap<String, Long>();

        int idx = 0;
        while(mIte.hasNext()){
            Long mID = mIte.next();
            int currentIDX = mID.intValue();
            long[] linkArray = this.env.getLinkRow(currentIDX);
            int len = linkArray.length;
            long sumValue = 0;
            for(int i=0;i<len;i++){
                sumValue += linkArray[i];
            }

            if(sumValue >= maxLink){
                maxLink = sumValue;
                CPU = this.env.findCPU(mID);
            }

        }


        return CPU;


    }

    /**
     * 未割り当てのマシンから，転送速度が最大のリンクを特定する．　　
     *
     * @return
     */
    public HashMap<String, Long> selectMachinePair(){
        Iterator<Long> mIte = this.unMappedCPU.iterator();
        long maxLink = 0;
        HashMap<String, Long> retMap = new HashMap<String, Long>();

        int idx = 0;
        while(mIte.hasNext()){
            Long mID = mIte.next();
            int currentIDX = mID.intValue();
            long[] linkArray = this.env.getLinkRow(currentIDX);
            int len = linkArray.length;
            for(int i=0;i<len;i++){
                if(this.unMappedCPU.contains(new Long(i)) && (i != mID.longValue())){
                    //もし，相手のクラスタも含まれていれば，選択対象とする．
                    if(maxLink <= linkArray[i]){
                        maxLink = linkArray[i];
                        retMap.put("FROM", mID);
                        retMap.put("TO", new Long(i));
                    }
                }else{
                    ////System.out.println("Batu");
                }
            }
        }

        //再チェック
        if((!retMap.containsKey("FROM"))||(!retMap.containsKey("TO"))){
            //ランダムに取得する．
            retMap.put("FROM", this.unMappedCPU.getList().get(0));
            if(this.unMappedCPU.getList().size() == 1){
                retMap.put("TO", this.unMappedCPU.getList().get(0)); 
            }else{
                retMap.put("TO", this.unMappedCPU.getList().get(1));
            }
        }
        if(retMap.isEmpty()){
            //System.out.println("NG");
        }

        long from = retMap.get("FROM").longValue();
        long to = retMap.get("TO").longValue();
        //System.out.println("FROM : "+ from + "/TO: "+to);

        return retMap;
        

    }

}
