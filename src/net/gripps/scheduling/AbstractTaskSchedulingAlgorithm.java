package net.gripps.scheduling;

import net.gripps.clustering.common.aplmodel.*;
import net.gripps.environment.CPU;
import net.gripps.environment.Environment;

import java.util.LinkedList;
import java.util.Vector;
import java.util.Properties;
import java.util.Iterator;
import java.io.FileInputStream;

/**
 * Author: H. Kanemitsu
 * Date: 2008/10/24
 */
public class AbstractTaskSchedulingAlgorithm {

    /**
     *
     */
    protected BBTask retApl;

    /**
     *
     */
    protected Environment env;

    /**
     *
     */
    protected String file;

    /**
     *
     */
    protected int mode;

    /**
     *
     */
    protected long minSpeed;

    protected long maxSpeed;


    /**
     *
     */
    protected long minLink;

    protected int coreMode;



    /**
     * コンストラクタ
     *
     * @param filename
     * @param apl
     * @param environment
     */
    public AbstractTaskSchedulingAlgorithm(
            String filename,
            BBTask apl,
            Environment environment) {

        this.retApl = apl;
        this.file = filename;
        this.env = environment;


        try {
            Properties prop = new Properties();
            prop.load(new FileInputStream(this.file));
            //スケジューリング方針を設定する．
            this.mode = Integer.valueOf(prop.getProperty("task.weight.calcmethod"));
            this.minSpeed = Long.valueOf(prop.getProperty("cpu.speed.min")).longValue();
            this.minLink = Long.valueOf(prop.getProperty("cpu.link.min")).longValue();
            this.coreMode = Integer.valueOf(prop.getProperty("cpu.multicore")).intValue();
            this.maxSpeed =  Integer.valueOf(prop.getProperty("cpu.speed.max")).intValue();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public boolean isHetero(){
        if(this.minSpeed == this.maxSpeed){
            return false;
        }else{
            return true;
        }
    }

    public AbstractTaskSchedulingAlgorithm(){
       try {
            Properties prop = new Properties();
            prop.load(new FileInputStream(this.file));
            //スケジューリング方針を設定する．
            this.mode = Integer.valueOf(prop.getProperty("task.weight.calcmethod"));
            this.minSpeed = Long.valueOf(prop.getProperty("cpu.speed.min")).longValue();
            this.minLink = Long.valueOf(prop.getProperty("cpu.link.min")).longValue();

        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    /**
     * @return
     */
    public BBTask process() {
        //仮想的な依存辺を，aplへセット
        this.schedule();

        //メイクスパンを，aplへセット
        this.calcMakeSpan();

        return this.retApl;

    }

    /**
     * スケジュールするためのメイン処理です．
     */
    public BBTask schedule() {


        return null;
    }

    /**
     * @param task
     * @return
     */
    public long getInstrunction(AbstractTask task) {

        if (task == null) {
            return 0;
        }
        switch (this.mode) {
            //Max
            case 1:
                return task.getMaxWeight();
            case 2:
                return task.getAveWeight();
            case 3:
                return task.getMinWeight();
            default:
                return 0;

        }

    }


    /**
     *
     */
    public void calcMakeSpan() {
        int size = this.retApl.getTaskList().size();

        //ENDタスクを取得する．
        AbstractTask endTask = this.retApl.findTaskByLastID(new Long(size));

        //ENDタスクのTlevel，すなわちStartTimeを取得する．
        long tlevel = this.calcScheduledStartTime(endTask, false);

        //ENDタスクが属しているタスククラスタを取得する．
        TaskCluster endCluster = this.retApl.findTaskCluster(endTask.getClusterID());

        //ENDクラスタが割り当てられているマシンを取得する．
        CPU endCPU = endCluster.getCPU();

        //MakeSpanを計算する．
        long makeSpan = tlevel + this.getInstrunction(endTask) / endCPU.getSpeed();

        this.retApl.setMakeSpan(makeSpan);

        // return this.retApl;


    }


    /**
     * 各タスクの実行開始時刻の設定です．
     * この時点で，実行順はすでに決まっている．
     *
     * @return
     */
    public long calcScheduledStartTime(AbstractTask task, boolean recalculate) {
        LinkedList<DataDependence> DpredList = task.getDpredList();
        Iterator<DataDependence> dpredIte = DpredList.iterator();
        int size = DpredList.size();
        long retTlevel = 0;

        //モードが再計算でないならば，すでに値が入っていればそのまま返す．
        if (task.getStartTime() != -1) {
            if (!recalculate) {
                return task.getStartTime();
            }
        }
        if (DpredList.size() == 0) {
            task.setStartTime(0);
            //this.retApl.getStartTaskSet().add(task.getIDVector().get(1));
            return 0;
        }

        long maxValue = 0;
        //for (int i = 0; i < size; i++) {
        while (dpredIte.hasNext()) {
            // DataDependence dd = DpredList.get(i);
            DataDependence dd = dpredIte.next();
            Vector<Long> fromid = dd.getFromID();
            /*   if(fromid.get(1).longValue() > task.getIDVector().get(1).longValue()){
                System.out.println("NG**");
            }
            */
            AbstractTask fromTask = this.retApl.findTaskByLastID(fromid.get(1));
            //fromTaskが属するタスククラスタを取得
            TaskCluster fromCluster = this.retApl.findTaskCluster(fromTask.getClusterID());

            //toTaskを取得
            //AbstractTask toTask = this.retApl.findTaskByLastID(dd.getToID().get(1));
            //toTaskが属するタスククラスタを取得
            TaskCluster toCluster = this.retApl.findTaskCluster(task.getClusterID());
            long fromID = fromCluster.getCPU().getCpuID().longValue();
            long toID = toCluster.getCPU().getCpuID().longValue();
            long link = 0;
             if(fromID == -1 || toID == -1){
                     link = this.minLink;
            }else if (fromID == toID) {
                link = 0;
            } else {
                    link = this.env.getLink((int) fromID, (int) toID);

            }
            long fromTlevel = 0;
            if(this.isHetero()){
                long nw_time = 0;
                if(fromCluster.getCPU().getMachineID() == toCluster.getCPU().getMachineID()){

                }else{
                    nw_time = this.env.getSetupTime()+ dd.getMaxDataSize()/this.env.getNWLink(fromCluster.getCPU().getCpuID(), toCluster.getCPU().getCpuID());
                }
                fromTlevel = this.calcScheduledStartTime(fromTask, recalculate) + (this.getInstrunction(fromTask) / fromCluster.getCPU().getSpeed()) + nw_time;

            }else{
                 fromTlevel = this.calcScheduledStartTime(fromTask, recalculate) + (this.getInstrunction(fromTask) / fromCluster.getCPU().getSpeed()) +
                                    this.getNWTime(dd.getFromID().get(1), dd.getToID().get(1), dd.getMaxDataSize());
            }

            if (maxValue < fromTlevel) {
                maxValue = fromTlevel;
                //task.setTpred(fromTask.getIDVector());
            }
        }

        //this.retApl.findTaskByLastID(task.getIDVector().lastElement()).setTlevel(maxValue);
        task.setStartTime(maxValue);
        return maxValue;
    }

    /**
     * 定められたデータ転送速度上において，タスク間のデータ転送時間を計算します．
     *
     * @param fromTaskID
     * @param toTaskID
     * @return
     */
    public long getNWTime(Long fromTaskID, Long toTaskID, long data) {
        AbstractTask fromTask = this.retApl.findTaskByLastID(fromTaskID);
        TaskCluster fromCluster = this.retApl.findTaskCluster(fromTask.getClusterID());

        AbstractTask toTask = this.retApl.findTaskByLastID(toTaskID);
        TaskCluster toCluster = this.retApl.findTaskCluster(toTask.getClusterID());

        //もし双方のタスクが同じクラスタに属していれば，データ転送時間は0となる．
        if (fromTask.getClusterID().longValue() == toTask.getClusterID().longValue()) {
            return 0;
        } else if (fromCluster.getCPU().getCpuID().longValue() == toCluster.getCPU().getCpuID().longValue()) {
            return 0;
        } else if(fromCluster.getCPU().getMachineID() == toCluster.getCPU().getMachineID()) {
            return 0;
        } else {
            //if(this.coreMode == 1){
            if(this.isHetero()){
                return this.env.getSetupTime()+data/this.env.getNWLink(fromCluster.getCPU().getCpuID(), toCluster.getCPU().getCpuID());
            }else{
                long link = 0;
                long fromID = fromCluster.getCPU().getCpuID().longValue();
                long toID = toCluster.getCPU().getCpuID().longValue();
                if(this.isHetero()){
                   link = this.env.getNWLink(fromCluster.getCPU().getCpuID(), toCluster.getCPU().getCpuID());
                }else{
                    if((fromID == -1)||(toID == -1)){
                               link = this.minLink;
                           }else{
                                link = this.env.getLink((int) fromCluster.getCPU().getCpuID().longValue(),
                                        (int) toCluster.getCPU().getCpuID().longValue());


                           }
                }
                return this.env.getSetupTime()+data / link;
            }

        }

    }

    /**
     * @param task
     * @param recalculate
     * @return
     */
    public long calcPriorityBlevel(AbstractTask task, boolean recalculate) {
        LinkedList<DataDependence> DsucList = task.getDsucList();
        Iterator<DataDependence> dsucIte = DsucList.iterator();
        int size = DsucList.size();
        TaskCluster fromCluster = this.retApl.findTaskCluster(task.getClusterID());

        //もしすでにBlevelの値が入っていれば，そのまま返す．
        if (task.getPriorityBlevel() != -1) {
            if (!recalculate) {
                return task.getPriorityBlevel();
            }
        }

        //もし後続タスクがない場合，blevel=自分の命令数となる．
        long instruction = this.getInstrunction(task);
        if (DsucList.size() == 0) {
            long execTime = instruction / fromCluster.getCPU().getSpeed();
            task.setPriorityBlevel(execTime);
            return execTime;
        }

        long maxValue = 0;
        while (dsucIte.hasNext()) {
            DataDependence dd = dsucIte.next();
            //DataDependence dd = DsucList.get(i);
            Vector<Long> toid = dd.getToID();

            AbstractTask toTask = this.retApl.findTaskByLastID(toid.get(1));
            //toTaskの属するクラスタを取得する．
            TaskCluster toCluster = this.retApl.findTaskCluster(toTask.getClusterID());

            long nw_time = 0;
            if(fromCluster.getCPU().getMachineID() == toCluster.getCPU().getMachineID()){

            }else{
                  nw_time = this.getNWTime(dd.getFromID().get(1), dd.getToID().get(1), dd.getMaxDataSize());
            }
            long toBlevel =  (instruction / toCluster.getCPU().getSpeed()) +nw_time +this.calcPriorityBlevel(toTask, recalculate);

            if (maxValue <= toBlevel) {
                maxValue = toBlevel;
            }
        }
        task.setPriorityBlevel(maxValue);
        return maxValue;
    }

    /**
     * @param task
     * @param recalculate
     * @return
     */
    public long calcPriorityTlevel(AbstractTask task, boolean recalculate) {
        LinkedList<DataDependence> DpredList = task.getDpredList();
        Iterator<DataDependence> dpredIte = DpredList.iterator();
        int size = DpredList.size();
        TaskCluster fromCluster = this.retApl.findTaskCluster(task.getClusterID());

        //もしすでにTlevelの値が入っていれば，そのまま返す．
        if (task.getPriorityTlevel() != -1) {
            if (!recalculate) {
                return task.getPriorityTlevel();
            }
        }

        //もし後続タスクがない場合，blevel=自分の命令数となる．
        long instruction = this.getInstrunction(task);
        if (DpredList.size() == 0) {
            //long execTime = instruction / fromCluster.getCPU().getSpeed();
            task.setPriorityTlevel(0);
            this.retApl.getStartTaskSet().add(task.getIDVector().get(1));
            return 0;
        }

        long maxValue = 0;
        while (dpredIte.hasNext()) {
            DataDependence dd = dpredIte.next();
            //DataDependence dd = DsucList.get(i);
            Vector<Long> fromid = dd.getFromID();

            AbstractTask fromTask = this.retApl.findTaskByLastID(fromid.get(1));
            //fromTaskの属するクラスタを取得する．
            TaskCluster toCluster = this.retApl.findTaskCluster(fromTask.getClusterID());

            long nw_time = 0;
             if(fromCluster.getCPU().getMachineID() == toCluster.getCPU().getMachineID()){

             }else{
                 nw_time =  this.getNWTime(dd.getFromID().get(1), dd.getToID().get(1), dd.getMaxDataSize());
             }
            TaskCluster clster = this.retApl.findTaskCluster(fromTask.getClusterID());
            CPU cpu = clster.getCPU();
            long fromTlevel = this.calcPriorityTlevel(fromTask, recalculate) + fromTask.getMaxWeight()/cpu.getSpeed()+ nw_time;

            if (maxValue <= fromTlevel) {
                maxValue = fromTlevel;
            }
        }
        task.setPriorityTlevel(maxValue);
        return maxValue;
    }

    /**
     * @param predTask
     * @param sucTask
     */
    public void addVirtualEdge(AbstractTask predTask, AbstractTask sucTask) {
        //predとsucの間にデータ依存を加える．
        if(predTask.getIDVector().get(1).longValue() == sucTask.getIDVector().get(1).longValue()){

        }else if (predTask.findDDFromDsucList(predTask.getIDVector(), sucTask.getIDVector()) == null) {
            //データ依存がなければ，追加する．
            DataDependence dd = new DataDependence(predTask.getIDVector(), sucTask.getIDVector(), 0, 0, 0);
            dd.setReady(true);
            predTask.addDsuc(dd);
            sucTask.addDpred(dd);
        } else {

        }
    }

    /**
     * tlevel値を更新します．
     * 全タスクに対してtlevelを更新する．
     *
     *
     * @param task
     * @return
     */
    public long updateTlevel(BBTask apl, AbstractTask task, CustomIDSet set) {
        //先行タスクを取得する．
        LinkedList<DataDependence> DpredList = task.getDpredList();
        //先行タスクのサイズを取得する．
        int size = DpredList.size();
        Iterator<DataDependence> dpredIte = DpredList.iterator();
        long retTlevel = 0;

        //すでにtlevel値が計算済みであれば，返す．
        if(set.contains(task.getIDVector().get(1))){
            return task.getTlevel();
        }

        //startタスクであれば，値を0に設定する．
        if (DpredList.size() == 0) {
            task.setTlevel(0);
            set.add(task.getIDVector().get(1));
            return 0;
        }

        TaskCluster fromCluster = this.retApl.findTaskCluster(task.getClusterID());
        long maxValue = 0;
        //先行タスクたちに対するループ
       // for (int i = 0; i < size; i++) {
        while(dpredIte.hasNext()){

            DataDependence dd =dpredIte.next();
            Vector<Long> fromid = dd.getFromID();
           // AbstractTask fromTask = this.findTaskAsTop(fromid);
            AbstractTask fromTask = apl.findTaskByLastID(fromid.get(1));
            TaskCluster cluster = apl.findTaskCluster(fromTask.getClusterID());
            CPU CPU = cluster.getCPU();
            long nw_time = 0;
            if(fromCluster.getCPU().getMachineID() == CPU.getMachineID()){

            }else{
                nw_time =this.getNWTime(dd.getFromID().get(1), dd.getToID().get(1), dd.getMaxDataSize());
            }
            //先行タスクに対して，再帰的に呼び出す．
            long fromTlevel = this.updateTlevel(apl,fromTask,set) + (this.getInstrunction(fromTask) / CPU.getSpeed()) +nw_time;

            if (maxValue <= fromTlevel) {
                maxValue = fromTlevel;
            }

        }

        task.setTlevel(maxValue);
        set.add(task.getIDVector().get(1));
        return maxValue;

    }

    /**
     * 一つのクラスタ内タスクに対してのみ，tlevel値を更新する．
     *
     * @param apl
     * @param task
     * @param set
     * @return
     */
    public long updateTlevelInCluster(BBTask apl, AbstractTask task, CustomIDSet set) {

        //先行タスクを取得する．
        LinkedList<DataDependence> DpredList = task.getDpredList();
        //先行タスクのサイズを取得する．
        int size = DpredList.size();
        Iterator<DataDependence> dpredIte = DpredList.iterator();
        long retTlevel = 0;

        //すでにtlevel値が計算済みであれば，返す．
        if(set.contains(task.getIDVector().get(1))){
            return task.getTlevel();
        }

        //startタスクであれば，値を0に設定する．
        if (DpredList.size() == 0) {
            task.setTlevel(0);
            set.add(task.getIDVector().get(1));
            return 0;
        }

        long maxValue = 0;
        //先行タスクたちに対するループ
       // for (int i = 0; i < size; i++) {
        while(dpredIte.hasNext()){

            DataDependence dd =dpredIte.next();
            Vector<Long> fromid = dd.getFromID();
           // AbstractTask fromTask = this.findTaskAsTop(fromid);
            AbstractTask fromTask = apl.findTaskByLastID(fromid.get(1));
            TaskCluster cluster = apl.findTaskCluster(fromTask.getClusterID());
            CPU CPU = cluster.getCPU();
            //先行タスクに対して，再帰的に呼び出す．
            long fromTlevel = this.updateTlevel(apl,fromTask,set) + (this.getInstrunction(fromTask) / CPU.getSpeed()) +
                    this.getNWTime(dd.getFromID().get(1), dd.getToID().get(1), dd.getMaxDataSize());
            if (maxValue < fromTlevel) {
                maxValue = fromTlevel;
            }

        }

        task.setTlevel(maxValue);
        set.add(task.getIDVector().get(1));
        return maxValue;

    }

    /**
     *
     * @return
     */
    public BBTask getRetApl() {
        return retApl;
    }

    public void setRetApl(BBTask retApl) {
        this.retApl = retApl;
    }

    public Environment getEnv() {
        return env;
    }

    public void setEnv(Environment env) {
        this.env = env;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }
}
