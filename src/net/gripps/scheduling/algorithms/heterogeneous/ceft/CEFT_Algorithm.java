package net.gripps.scheduling.algorithms.heterogeneous.ceft;

import net.gripps.clustering.common.Constants;
import net.gripps.clustering.common.aplmodel.*;
import net.gripps.environment.CPU;
import net.gripps.environment.Environment;
import net.gripps.scheduling.algorithms.heterogeneous.heft.BlevelComparator;
import net.gripps.scheduling.algorithms.heterogeneous.heft.HEFT_Algorithm;
import net.gripps.scheduling.algorithms.heterogeneous.heft.StartTimeComparator;

import java.util.*;

/**
 * Author: H. Kanemitsu
 * Date: 14/11/18
 * CEFT(Constrained CriticalPath EFT)アルゴリズム実装です．
 */
public class CEFT_Algorithm extends HEFT_Algorithm {

    /**
     * CCP一つを格納するための領域
     */
    protected HashMap<Long, AbstractTask> ccpMap;

    /**
     * ccpMapを順序付けて格納するためのリスト
     */
    protected LinkedList<TreeMap<Long, AbstractTask>> ccpList;


    /**
     *
     */
    protected AbstractTask dmyStartTask;

    protected AbstractTask dmyEndTask;


    /**
     * @param apl
     * @param file
     * @param env
     */
    public CEFT_Algorithm(BBTask apl, String file, Environment env) {
        super(apl, file, env);
        this.ccpList = new LinkedList<TreeMap<Long, AbstractTask>>();

    }

    /**
     * @param apl
     * @param file
     * @param env
     * @param in_cpuList
     */
    public CEFT_Algorithm(BBTask apl, String file, Environment env, Hashtable<Long, CPU> in_cpuList) {
        super(apl, file, env, in_cpuList);
        this.ccpList = new LinkedList<TreeMap<Long, AbstractTask>>();
    }

    public long getMaxTlevel(AbstractTask task, CustomIDSet set, CustomIDSet ccpSet) {
        set.add(task.getIDVector().get(1));
        AbstractTask dominatingTask = null;
        //Startならば，0を返す．
        if (this.isStart(task)) {
            return 0;
        }

        //先行タスクのtlevel値を取得する．
        Iterator<DataDependence> dpredIte = task.getDpredList().iterator();
        long maxTlevel = 0;
        long realTlevel = 0;

        //先行タスクに対するループ
        while (dpredIte.hasNext()) {
            DataDependence dpred = dpredIte.next();
            AbstractTask predTask = this.retApl.findTaskByLastID(dpred.getFromID().get(1));
            long predTlevel = 0;

            if (predTask.isActive()) {
                if (set.contains(predTask.getIDVector().get(1))) {
                    predTlevel = predTask.getTlevel();
                } else {
                    //もし未チェックであれば，再計算する．
                    predTlevel = this.getMaxTlevel(predTask, set, ccpSet);
                }
            } else {
                continue;
            }

            //先行タスクから，自身のTlevel値を計算する．
            realTlevel = predTlevel + predTask.getAve_procTime() + this.getComTime(predTask, task);

            if (maxTlevel <= realTlevel) {
                maxTlevel = realTlevel;
                task.setTlevel(realTlevel);
                task.setTpred(predTask.getIDVector());
                dominatingTask = predTask;
            }

        }
        //ccpSet.add(dominatingTask.getIDVector().get(1));

        //最大のシーケンスにその先行タスクを入れる．
        //this.maxSequence.add(dominatingTask.getIDVector().get(1));
        return maxTlevel;

    }

    /**
     * 先行タスクがダミーのみかどうかの判断
     *
     * @param task
     * @return
     */
    public boolean isStart(AbstractTask task) {
        boolean ret = true;
        Iterator<DataDependence> dpredIte = task.getDpredList().iterator();
        if (task.getIDVector().get(1) == this.dmyEndTask.getIDVector().get(1)) {
            return false;
        }
        while (dpredIte.hasNext()) {
            DataDependence dpred = dpredIte.next();
            if (dpred.getFromID().get(1) == this.dmyStartTask.getIDVector().get(1)) {
                continue;
            } else {
                AbstractTask predTask = this.retApl.findTaskByLastID(dpred.getFromID().get(1));
                if (predTask.isActive()) {
                    ret = false;
                    break;
                } else {
                    continue;
                }
            }
        }

        return ret;
    }

    public boolean isEnd(AbstractTask task) {
        boolean ret = true;
        Iterator<DataDependence> dsucIte = task.getDsucList().iterator();
        while (dsucIte.hasNext()) {
            DataDependence dpred = dsucIte.next();
            if (dpred.getToID().get(1) == this.dmyEndTask.getIDVector().get(1)) {
                continue;
            } else {
                AbstractTask sucTask = this.retApl.findTaskByLastID(dpred.getToID().get(1));
                if (sucTask.isActive()) {
                    ret = false;
                    break;
                } else {
                    continue;
                }
            }
        }

        return ret;
    }


    /**
     * ダミーのSTARTタスク，ENDタスクをセットする処理
     */
    public void prepare() {
        super.prepare();

        this.dmyStartTask = new BBTask(Constants.TYPE_BASIC_BLOCK, 0, 0, 0);
        Vector<Long> sID = new Vector<Long>();
        sID.add(new Long(1));
        Long nID = new Long(-1);
        sID.add(nID);
        this.dmyStartTask.setIDVector(sID);
        this.dmyStartTask.setActive(false);
        //this.retApl.addTask(this.dmyStartTask);

        this.dmyEndTask = new BBTask(Constants.TYPE_BASIC_BLOCK, 0, 0, 0);
        Vector<Long> eID = new Vector<Long>();
        eID.add(new Long(1));
        Long mID = new Long(this.retApl.getTaskList().size() + 1);
        eID.add(mID);
        this.dmyEndTask.setIDVector(eID);
        this.dmyEndTask.setActive(false);
        //this.retApl.addTask(this.dmyEndTask);

        AbstractTask endTask = this.retApl.findTaskByLastID(this.retApl.getEndTask().get(1));
        Iterator<Long> startIte = this.retApl.getStartTaskSet().iterator();
        while (startIte.hasNext()) {
            Long id = startIte.next();
            AbstractTask sTask = this.retApl.findTaskByLastID(id);
            DataDependence dd = new DataDependence(this.dmyStartTask.getIDVector(), sTask.getIDVector(), 0, 0, 0);
            sTask.addDpred(dd);
            this.dmyStartTask.addDsuc(dd);

        }
        DataDependence edd = new DataDependence(endTask.getIDVector(), this.dmyEndTask.getIDVector(), 0, 0, 0);
        this.dmyEndTask.addDpred(edd);
        endTask.addDsuc(edd);

        this.constructCCP();

    }

    public CustomIDSet getCCPSet(AbstractTask task, CustomIDSet set) {
        Iterator<DataDependence> dpredIte = task.getDpredList().iterator();
        while (dpredIte.hasNext()) {
            DataDependence dpred = dpredIte.next();
            AbstractTask predTask = this.retApl.findTaskByLastID(dpred.getFromID().get(1));
            if (predTask.isActive()) {
                //もしまだ刈り取られてなければ、見る。


            } else {
                //先行タスクが既に刈り取られていれば，次のタスクへ進む．
            }
        }

        return null;
    }

    /**
     * 指定IDのタスクが含まれているリストのインデックスを取得する．
     *
     * @param id
     * @return
     */
    public int findIndexOfCCP(Long id) {
        int index = -1;
        Iterator<TreeMap<Long, AbstractTask>> mapIte = this.ccpList.iterator();
        int current_index = 0;
        boolean isfound = false;
        while (mapIte.hasNext()) {
            TreeMap<Long, AbstractTask> map = mapIte.next();
            if (map.containsKey(id)) {
                index = current_index;
                isfound = true;
                break;

            } else {
                current_index++;
            }
        }
        if (isfound) {
            return current_index;
        } else {
            return -1;
        }
        //   return current_index;

    }

    public void putCCP(int idx, AbstractTask task) {
        TreeMap<Long, AbstractTask> map = this.ccpList.get(idx);
        map.put(task.getIDVector().get(1), task);

    }

    public void removeVirtualEdge() {
        Iterator<AbstractTask> taskIte = this.retApl.getTaskList().values().iterator();

        while (taskIte.hasNext()) {
            AbstractTask task = taskIte.next();
            //先行タスクから削除
            task.delDpredSimply(this.dmyStartTask.getIDVector());
            //後続タスクから，削除
            task.delDsucSimply(this.dmyEndTask.getIDVector());
        }

    }

    /**
     * CCPを取得したとき，
     * 先行タスクを含むCCPのインデックスの最大値よりも後
     * 後続タスクを含むCCPのインデックスの最小値よりも前
     * となるように挿入する．
     */
    public void registCCP(Long id, TreeMap<Long, AbstractTask> map) {
        AbstractTask task = map.get(id);
        boolean isPredFound = false;
        boolean isSucFound = false;

        //先行タスクたちを取得
        Iterator<DataDependence> dpredIte = task.getDpredList().iterator();
        int maxPredIndex = 0;
        //先行タスクの位置を特定
        while (dpredIte.hasNext()) {
            DataDependence dpred = dpredIte.next();
            if (dpred.getFromID().get(1) == this.dmyStartTask.getIDVector().get(1)) {
                continue;
            } else {
                Long predID = dpred.getFromID().get(1);
                int idx = this.findIndexOfCCP(predID);
                if (idx != -1) {
                    isPredFound = true;
                    if (idx >= maxPredIndex) {
                        maxPredIndex = idx;
                    }
                } else {
                    //見つからなければ，既にCCPに入っているSTARTタスクを探す．
                    //その最大値+1の位置に挿入する．
                    //maxPredIndex = -1;
                }

            }
        }

        int max_val = this.retApl.getTaskList().size() + 1;
        int minSucIndex = max_val;
        //後続タスクの位置を特定
        Iterator<DataDependence> dsucIte = task.getDsucList().iterator();
        while (dsucIte.hasNext()) {
            DataDependence dsuc = dsucIte.next();
            if (dsuc.getToID().get(1) == this.dmyEndTask.getIDVector().get(1)) {
                continue;
            } else {

                Long sucID = dsuc.getToID().get(1);
                int idx = this.findIndexOfCCP(sucID);
                if (idx != -1) {
                    isSucFound = true;
                    if (idx <= minSucIndex) {
                        minSucIndex = idx;
                    }
                } else {
                    // minSucIndex = -1;
                }

            }
        }

        if (minSucIndex == max_val) {
            minSucIndex = -1;
        }
        /**if (maxPredIndex + 1 <= minSucIndex) {
         this.ccpList.add(maxPredIndex + 1, map);
         } else {
         this.ccpList.add(minSucIndex + 1, map);
         }**/
        //this.ccpList.add(maxPredIndex, map);
        //  System.out.println("pred:"+(maxPredIndex+1)+"/suc:"+minSucIndex);
        if (isPredFound) {
            if (maxPredIndex < this.ccpList.size() - 1) {
                // if(maxPredIndex + 1 <= minSucIndex){
                this.ccpList.add(maxPredIndex + 1, map);
                // }

            } else {
                this.ccpList.add(map);
            }

        } else {
            if (isSucFound) {
                this.ccpList.add(minSucIndex, map);
            } else {

                this.ccpList.add(0, map);
            }
        }


    }

    /**
     * 仮想辺をつけるための条件（全ての先行タスクがinactiveならば，ダミー
     * のSTARTタスクからの矢印をつける）
     *
     * @param task
     * @return
     */
    public boolean isPredAllInActive(AbstractTask task) {
        boolean ret = true;
        Iterator<DataDependence> dpredIte = task.getDpredList().iterator();
        while (dpredIte.hasNext()) {
            DataDependence dpred = dpredIte.next();
            AbstractTask predTask = this.retApl.findTaskByLastID(dpred.getFromID().get(1));
            if (predTask == null) {
                continue;
            }
            if (predTask.getIDVector().get(1) == this.dmyStartTask.getIDVector().get(1)) {
                continue;
            }
            if (predTask.isActive()) {
                ret = false;
                break;
            } else {

            }
        }
        return ret;


    }

    public boolean isSucAllInActive(AbstractTask task) {
        boolean ret = true;
        Iterator<DataDependence> dsucIte = task.getDsucList().iterator();
        while (dsucIte.hasNext()) {
            DataDependence dsuc = dsucIte.next();
            AbstractTask sucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));
            if (sucTask == null) {
                continue;
            }
            if (sucTask.getIDVector().get(1) == this.dmyEndTask.getIDVector().get(1)) {
                continue;
            }
            if (sucTask.isActive()) {
                ret = false;
                break;
            } else {

            }
        }
        return ret;

    }

    public void addCCPVirtualEdge(AbstractTask t, TreeMap map) {
        //先行タスクに対するチェック
        Iterator<DataDependence> dpredIte = t.getDpredList().iterator();
        while (dpredIte.hasNext()) {
            DataDependence dpred = dpredIte.next();
            AbstractTask predTask = this.retApl.findTaskByLastID(dpred.getFromID().get(1));

            if (predTask == null) {
                continue;
            }
            if (map.containsKey(predTask.getIDVector().get(1))) {
                continue;
            }
            if (predTask.getIDVector().get(1) == this.dmyStartTask.getIDVector().get(1)) {
                continue;
            }
            if (this.isPredAllInActive(predTask)) {
                DataDependence dd = new DataDependence(this.dmyStartTask.getIDVector(), predTask.getIDVector(), 0, 0, 0);
                predTask.addDpred(dd);
                this.dmyStartTask.addDsuc(dd);
            }
            if (this.isSucAllInActive(predTask)) {
                DataDependence edd = new DataDependence(predTask.getIDVector(), this.dmyEndTask.getIDVector(), 0, 0, 0);
                this.dmyEndTask.addDpred(edd);
                predTask.addDsuc(edd);
            }
        }

        //後続タスクに対するチェック
        Iterator<DataDependence> dsucIte = t.getDsucList().iterator();
        while (dsucIte.hasNext()) {

            DataDependence dsuc = dsucIte.next();
            AbstractTask sucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));

            if (sucTask == null) {
                continue;
            }
            if (map.containsKey(sucTask.getIDVector().get(1))) {
                continue;
            }
            if (sucTask.getIDVector().get(1) == this.dmyEndTask.getIDVector().get(1)) {
                continue;
            }
            if (this.isPredAllInActive(sucTask)) {
                DataDependence dd = new DataDependence(this.dmyStartTask.getIDVector(), sucTask.getIDVector(), 0, 0, 0);
                sucTask.addDpred(dd);
                this.dmyStartTask.addDsuc(dd);
            }
            if (this.isSucAllInActive(sucTask)) {
                DataDependence edd = new DataDependence(sucTask.getIDVector(), this.dmyEndTask.getIDVector(), 0, 0, 0);
                this.dmyEndTask.addDpred(edd);
                sucTask.addDsuc(edd);
            }
        }
    }

    /**
     * 同一CCP以外の先行タスクにおいて，すべてfalse or ダミーのみであればtrueを返す．
     * 逆に言うと，先行タスクのうちでactiveなccp以外のタスクが存在すればfalseとなる．
     * または，全てccpの先行タスクであってもfalse
     *
     * @param task
     * @param map
     * @return
     */
    public boolean isStart(AbstractTask task, TreeMap map) {
        CustomIDSet set = new CustomIDSet();
        boolean ret = true;
        int ccpCount = 0;
        Iterator<DataDependence> dpredIte = task.getDpredList().iterator();
        while (dpredIte.hasNext()) {
            DataDependence dpred = dpredIte.next();
            if (map.containsKey(dpred.getFromID().get(1))) {
                ccpCount++;
                continue;
            }
            if (dpred.getFromID().get(1) == this.dmyStartTask.getIDVector().get(1)) {
                continue;
            } else {
                AbstractTask predTask = this.retApl.findTaskByLastID(dpred.getFromID().get(1));
                set.add(predTask.getIDVector().get(1));
                /*if (predTask.isActive()) {
                    ret = false;
                    break;
                } else {
                    continue;
                }*/
            }
        }
        if (set.isEmpty() && ccpCount == 0) {
            return true;
        } else if (set.isEmpty() && ccpCount > 0) {
            //ccpの先行タスクのみ存在
            return false;
        } else {
            //ccpの先行タスク以外も存在
            Iterator<Long> retIte = set.iterator();

            while (retIte.hasNext()) {
                Long id = retIte.next();
                AbstractTask t = this.retApl.findTaskByLastID(id);
                if (t.isActive()) {
                    ret = false;
                    break;
                }

            }
        }

        return ret;
    }

    public boolean isTaskStartTaskInCCP(AbstractTask task, TreeMap map) {
        if (this.isPredActiveTaskExist(task, map)) {
            return false;
        } else {
            if (task.findDDFromDpredList(this.dmyStartTask.getIDVector(), task.getIDVector()) != null) {
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean isPredActiveTaskExist(AbstractTask task, TreeMap map) {
        boolean ret = false;
        Iterator<DataDependence> dpredIte = task.getDpredList().iterator();
        while (dpredIte.hasNext()) {
            DataDependence dpred = dpredIte.next();
            Long predID = dpred.getFromID().get(1);
            if (predID.longValue() == this.dmyStartTask.getIDVector().get(1).longValue()) {
                continue;
            }
            if (predID.longValue() == this.dmyEndTask.getIDVector().get(1).longValue()) {
                continue;
            }
            AbstractTask predTask = this.retApl.findTaskByLastID(predID);
            if (predTask.isActive()) {
                ret = true;
                break;
            }
        }
        return ret;
    }

    /**
     * ccpにおいて，
     * - 当該タスクの先行タスクが（同一CCP集合＋昔のCCPタスク＋ダミーSTARTタスク）以外，
     * つまりactiveなタスクが存在すれば，新規cpMapとして追加する．この時，active/inactive
     * を無視して，単に[先行タスクのlistインデックス最大値+1 ~ 後続タスクのlistインデックス最小値]
     * の間に挿入する．
     * <p/>
     * - activeな先行タスクがなければ，既存のcpMapへ追加する．この時，前回のループで走査された
     * タスクのmapへ追加される．
     */
    public void constructCCP() {
        CustomIDSet ccpTaksSet = new CustomIDSet();
        while (!(ccpTaksSet.getList().size() == this.retApl.getTaskList().size())) {
            //レベルの計算
            this.getMaxTlevel(this.dmyEndTask, new CustomIDSet(), new CustomIDSet());
            //CCPの取得
            AbstractTask task = this.dmyEndTask;

            TreeMap<Long, AbstractTask> cpMap = new TreeMap<Long, AbstractTask>();

            while (!this.isStart(task)) {
                if (task.getIDVector().get(1) != this.dmyEndTask.getIDVector().get(1)) {
                    cpMap.put(task.getIDVector().get(1), task);
                } else {

                }
                task = this.retApl.findTaskByLastID(task.getTpred().get(1));
            }
            cpMap.put(task.getIDVector().get(1), task);

            Iterator<Long> idIte = cpMap.keySet().iterator();
            int preIDX = 0;
            int afterIDX = 0;
            int idx = 0;
            //まずは分ける．
            //Map一つのキーに対するループ
            TreeMap<Long, AbstractTask> tMap;
            Long predID = new Long(0);
            while (idIte.hasNext()) {

                //IDを取得する．
                Long id = idIte.next();
                //先行タスクを含むCCPがあるかどうかを調べる．
                AbstractTask t = this.retApl.findTaskByLastID(id);
                //まずは，disableにする．
                t.setActive(false);
                ccpTaksSet.add(id);
                //仮想辺の更新
                this.addCCPVirtualEdge(t, cpMap);

                //if (this.isStart(t, cpMap)) {
                if (this.isTaskStartTaskInCCP(t, cpMap)) {
                    //Startタスクならば，入れる．
                    tMap = new TreeMap<Long, AbstractTask>();
                    tMap.put(id, t);
                    //cpListへ登録する．
                    this.registCCP(id, tMap);

                } else {
                    //if (this.isPredOnlyCCP(t, cpMap)) {
                    //activeな先行タスクが存在しない場合は，Max(先行タスクのindex最大値+1, 前回のタスクのindex）を計算する．
                    //もし前者が大きければ，新規にnewする．後者が大きければ，そのままputする．
                    if (!this.isPredActiveTaskExist(t, cpMap)) {
                        //cpListにおいて，前回のIDを含むmapへputする
                        int exist_idx = this.findIndexOfCCP(predID);
                        Iterator<DataDependence> dpredIte = t.getDpredList().iterator();
                        int max_pred_index = 0;
                        while(dpredIte.hasNext()){
                            DataDependence dpred = dpredIte.next();
                            Long p_id = dpred.getFromID().get(1);
                            if(p_id == this.dmyStartTask.getIDVector().get(1)){
                                continue;
                            }
                            if(cpMap.containsKey(p_id)){
                                continue;
                            }
                            int pred_idx = this.findIndexOfCCP(p_id);
                            if(pred_idx >= max_pred_index){
                                max_pred_index = pred_idx;
                            }

                        }
                        if(max_pred_index+1 > exist_idx){
                            tMap = new TreeMap<Long, AbstractTask>();
                            tMap.put(id, t);
                            if(max_pred_index+1 >= this.ccpList.size()) {
                                this.ccpList.add(tMap);
                            }else{
                                this.ccpList.add(max_pred_index+1, tMap);
                            }
                            //cpListへ登録する．
                           //this.registCCP(id, tMap);
                        }else{
                            //this.putCCP(exist_idx, t);
                            tMap = new TreeMap<Long, AbstractTask>();
                                            tMap.put(id, t);
                            this.registCCP(id, tMap);

                        }


                    } else {
                        //新規にMapを作成する．
                        //自分を追加する．
                        //cpListへ登録する．
                        tMap = new TreeMap<Long, AbstractTask>();
                        tMap.put(id, t);
                        //cpListへ登録する．
                        this.registCCP(id, tMap);

                    }

                }
                predID = id;
            }

        }


    }

    public boolean isPredOnlyCCP(AbstractTask t, TreeMap cpMap) {
        boolean isOnlyThisCCPPred = true;
        int idx2 = 0;
        Iterator<DataDependence> dpredIte = t.getDpredList().iterator();
        //一タスクの先行タスクに対するループ
        while (dpredIte.hasNext()) {
            DataDependence dpred = dpredIte.next();
            AbstractTask predTask = this.retApl.findTaskByLastID(dpred.getFromID().get(1));
            if (predTask.isActive()) {
                if (cpMap.containsKey(predTask.getIDVector().get(1))) {

                } else {
                    //先行タスクがなければ，falseとする
                    isOnlyThisCCPPred = false;
                    break;
                }
            } else {
                continue;
            }
        }
        return isOnlyThisCCPPred;
    }


    public long calcEST_CEFT(AbstractTask task, CPU cpu) {
        long arrival_time = 0;

        if (task.getDpredList().isEmpty()) {

        } else {
            Iterator<DataDependence> dpredIte = task.getDpredList().iterator();
            while (dpredIte.hasNext()) {
                DataDependence dpred = dpredIte.next();
                AbstractTask dpredTask = this.retApl.findTaskByLastID(dpred.getFromID().get(1));
                TaskCluster predCluster = this.retApl.findTaskCluster(dpredTask.getClusterID());
                CPU predCPU = predCluster.getCPU();
                long nw_time = 0;
                if (predCPU.getMachineID() == cpu.getMachineID()) {

                } else {
                    //先行タスクからのデータ到着時刻を取得する．
                    nw_time = this.env.getSetupTime()+dpred.getMaxDataSize() / this.env.getNWLink(predCPU.getCpuID(), cpu.getCpuID());
                }
                long tmp_arrival_time = dpredTask.getStartTime() + dpredTask.getMaxWeight() / predCPU.getSpeed() + nw_time;
                if (arrival_time <= tmp_arrival_time) {
                    arrival_time = tmp_arrival_time;
                }
            }
        }
        //arrival_time(DRT) ~ 最後のFinishTimeまでの範囲で，task/cpu速度の時間が埋められる
        //箇所があるかどうかを調べる．
        Object[] oa = cpu.getFtQueue().toArray();
        if (oa.length > 1) {
            //Tlevelの小さい順→blevelの大きい順にソートする．
            Arrays.sort(oa, new StartTimeComparator());
            int len = oa.length;
            for (int i = 0; i < len - 1; i++) {
                AbstractTask t = ((AbstractTask) oa[i]);
                long finish_time = t.getStartTime() + t.getMaxWeight() / cpu.getSpeed();
                //次の要素の開始時刻を取得する．
                AbstractTask t2 = ((AbstractTask) oa[i + 1]);
                long start_time2 = t2.getStartTime();
                long s_candidateTime = Math.max(finish_time, arrival_time);
                if (s_candidateTime +( task.getMaxWeight() / cpu.getSpeed()) <= start_time2) {
                    return s_candidateTime;
                } else {
                    continue;
                }
            }
            //挿入できない場合は，ENDテクニックを行う．
            AbstractTask finTask = ((AbstractTask) oa[len - 1]);
            return Math.max(finTask.getStartTime() + finTask.getMaxWeight() / cpu.getSpeed(), arrival_time);
        } else {
            return arrival_time;
        }

    }

    public boolean isAllPredTaskScheduled(TreeMap<Long, AbstractTask> map) {
        boolean ret = true;
        Iterator<AbstractTask> taskIte = map.values().iterator();
        while (taskIte.hasNext()) {
            AbstractTask t = taskIte.next();
            if (this.retApl.getStartTaskSet().contains(t.getIDVector().get(1))) {
                //startタスクならば，次のタスクを見る．
                continue;
            } else {
                //startタスクでないならば，先行タスクを見る．
                Iterator<DataDependence> dpredIte = t.getDpredList().iterator();
                while (dpredIte.hasNext()) {
                    DataDependence dpred = dpredIte.next();
                    if(dpred.getFromID().get(1) == this.dmyStartTask.getIDVector().get(1)){
                        continue;
                    }
                    //自分のmap内の先行タスクであれば，それは無視する．
                    if (map.containsKey(dpred.getFromID().get(1))) {
                        continue;
                    }
                    if (this.scheduledTaskSet.contains(dpred.getFromID().get(1))) {
                        continue;
                    } else {
                        //もし先行タスクで未スケジュールならば，その時点でfalseして終了．
                        ret = false;
                        break;
                    }
                }
                if (!ret) {
                    break;
                }
            }
        }
        return ret;

    }


    /**
     * メイン処理です．
     */
    public void mainProcess() {

        this.wcp = this.calcWCP();
        this.retApl.setMinCriticalPath(this.wcp / this.maxSpeed);

        long EFT = 0;
        /**
         * スケジュールリストが空にならない間のループ
         */
        while (!this.ccpList.isEmpty()) {
            Iterator<CPU> cpuIte = this.cpuTable.values().iterator();
           // System.out.println("ccp残り数:"+this.ccpList.size());

            TreeMap<Long, AbstractTask> assignMap = null;

            //当該タスクの先行タスクがスケジュールされているかわからないので，
            //とりあえずpeekする（削除はしない）
            Iterator<TreeMap<Long, AbstractTask>> ccpIte = this.ccpList.iterator();
            //ccpリストを最初から走査する．もしmap内の全タスクの全先行タスクがスケジュール済み
            //であれば，スケジュールする．
            while (ccpIte.hasNext()) {
                TreeMap<Long, AbstractTask> map = ccpIte.next();
                if (this.isAllPredTaskScheduled(map)) {
                    //まずは要素を削除する．
                    ccpIte.remove();
                    //map内のタスクを走査する．
                    assignMap = map;
                    break;
                } else {
                    continue;
                }
            }
            //各CPUについて，タスクtを実行した場合のEFTを計算する．
            //EFTが最も小さくなるようなCPUに割り当てる．
            EFT = Constants.MAXValue;
            long start_time = 0;
            CPU assignedCPU = null;

            if (assignMap != null) {
                //map内のタスクのループ
                //タスク内でのEFTの最大値をとり，それが最小のCPUを選択する．
                Iterator<AbstractTask> taskIte = assignMap.values().iterator();
                int idx = 0;
                while (taskIte.hasNext()) {
                    AbstractTask assignTask = taskIte.next();
                    //long tmpEFT = 9999999;
                     //CPUのループ
                    if(idx == 0){
                        while (cpuIte.hasNext()) {

                            CPU cpu = cpuIte.next();
                            long tmpStartTime = this.calcEST_CEFT(assignTask, cpu);

                            long tmpEFT = tmpStartTime + assignTask.getMaxWeight() / cpu.getSpeed();
                            if (EFT >= tmpEFT) {
                                EFT = tmpEFT;
                                assignedCPU = cpu;
                                start_time = tmpStartTime;
                            }

                        }
                    }else{
                        start_time = this.calcEST_CEFT(assignTask, assignedCPU);
                    }
                    assignTask.setStartTime(start_time);
                     //System.out.println("Task: "+assignTask.getIDVector().get(1) + "-> CPU:"+assignedCPU.getCpuID());
                     //taskをCPUへ割り当てる処理．

                     this.assignProcess(assignTask, assignedCPU);
                    idx++;
                    this.scheduledTaskSet.add(assignTask.getIDVector().get(1));

                }
            }
        }
    }


    public BBTask process() {
        try {
            //CEFT用の初期化処理
            //ここでは，各タスクと辺に平均時間をセットする．
            this.initialize();
            //レベル反映処理
            this.prepare();

            this.removeVirtualEdge();
            //CCP構築処理
            //this.constructCCP();

            long start = System.currentTimeMillis();
            //メイン処理
            this.mainProcess();
            long end = System.currentTimeMillis();
            retApl.setProcessTime((end - start));
            //後処理を行う．
            this.postProcess();
            AbstractTask endTask = this.retApl.findTaskByLastID(this.retApl.getEndTask().get(1));
            TaskCluster endCluster = this.retApl.findTaskCluster(endTask.getClusterID());
            CPU cpu = endCluster.getCPU();

            long makeSpan = endTask.getStartTime() + endTask.getMaxWeight() / cpu.getSpeed();
            this.retApl.setMakeSpan(makeSpan);
            return this.retApl;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


}
