package net.gripps.clustering.algorithms.mwsl_delta;

import net.gripps.clustering.algorithms.CyclicBean;
import net.gripps.clustering.algorithms.CyclicMap;
import net.gripps.clustering.common.Constants;
import net.gripps.clustering.common.aplmodel.*;
import net.gripps.clustering.tool.Calc;
import net.gripps.environment.CPU;
import net.gripps.environment.Environment;

import java.io.FileInputStream;
import java.util.*;

/**
 * Author: H. Kanemitsu
 * Date: 13/05/13
 */
public class CMWSL_Algorithm extends MWSL_delta_hetero {

    protected long minData;
    protected long maxData;
    protected long maxLink;
    protected long minWorkload;
    protected long maxWorkload;
    protected long maxSpeed;

    protected CustomIDSet assignedMachineSet;

    protected long wcp;

    protected boolean isUpdateAll;
    protected double sizeRate;
    protected TaskCluster incommingCluster;
    protected long incomming_dataSize;
    protected int virtualmode;
    protected int sched_mode;
    protected CustomIDSet wslTaskSet;
    long sum_deltaopt = 0;
    long sum_deltaopt2 = 0;
    private int LBCount;
    /**
     * trueであれば，常に同じパスから下限値を算出する．
     */
    private boolean isDeltaStatic;


    /**
     * コンストラクタです．この時点で，設定ファイルから必要な情報を読み込んでおく．
     *
     * @param task
     * @param file
     * @param env_tmp
     */
    public CMWSL_Algorithm(BBTask task, String file, Environment env_tmp) {
        super(task, file, env_tmp);
        this.env = env_tmp;
        this.isDeltaStatic = false;
        Properties prop = new Properties();
        try {
            //create input stream from file
            prop.load(new FileInputStream(this.fileName));

           /* this.minData = Integer.valueOf(prop.getProperty("task.ddedge.size.min")).longValue();
            this.maxData = Integer.valueOf(prop.getProperty("task.ddedge.size.max")).longValue();
            this.maxLink = Integer.valueOf(prop.getProperty("cpu.link.max")).longValue();
            this.minWorkload = Integer.valueOf(prop.getProperty("task.instructions.min")).longValue();
            this.maxWorkload = Integer.valueOf(prop.getProperty("task.instructions.max")).longValue();
            this.maxSpeed = Integer.valueOf(prop.getProperty("cpu.speed.max")).longValue();
        */
            this.minData = Constants.MAXValue;
            this.maxData = 0;
            this.maxLink = 0;
            this.minLink = Constants.MAXValue;
            this.minWorkload = Constants.MAXValue;
            this.maxWorkload = 0;
            this.maxSpeed = 0;
            this.minSpeed = Constants.MAXValue;
            //  long start  = System.currentTimeMillis();
            this.setMaxMin();
            // long end = System.currentTimeMillis();
            // System.out.println("Time: "+(end-start)/1000);

            this.virtualmode = Integer.valueOf(prop.getProperty("mwsl.virtual.condition")).intValue();
            this.sched_mode = Integer.valueOf(prop.getProperty("mwsl.scheduling.all")).intValue();
            this.isUpdateAll = false;
            this.LBCount = 0;
            this.incommingCluster = null;
            this.incomming_dataSize = 0;
            this.sizeRate = 1;
            this.assignedMachineSet = new CustomIDSet();
            this.wslTaskSet = new CustomIDSet();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setMaxMin() {
        this.maxWorkload = this.retApl.getMaxWorkload();
        this.minWorkload = this.retApl.getMinWorkload();
        this.maxData = this.retApl.getMaxData();
        this.minData = this.retApl.getMinData();
        this.maxSpeed = this.env.getMaxSpeed();
        this.minSpeed = this.env.getMinSpeed();
        this.maxLink = this.env.getMaxLinkSpeed();
        this.minLink = this.env.getMinLinkSpeed();


    }

    public void printRet() {
        Iterator<TaskCluster> clusterIte = this.retApl.getTaskClusterList().values().iterator();
        CustomIDSet mSet = new CustomIDSet();
        CustomIDSet set = new CustomIDSet();

        while (clusterIte.hasNext()) {
            TaskCluster cls = clusterIte.next();
            CPU cpu = cls.getCPU();

            //cpu.clear();
            mSet.add(cpu.getMachineID());
            System.out.println("MID:" + cpu.getMachineID());
            //cpu_table_heft.put(cpu.getCpuID(), cpu);
            set.add(cpu.getCpuID());
            System.out.println("CPUID:" + cpu.getCpuID());
        }
        //System.out.println("/マシン数:" + mSet.getList().size() + "/プロセッサ数(クラスタ数):" + this.retApl.getTaskClusterList().size() );

    }


    public BBTask process() {
        try {
            this.prepare();
            this.wcp = this.calcWCP();
            this.retApl.setMinCriticalPath(this.wcp / this.maxSpeed);
            // this.retApl.setWcp()

            long start = System.currentTimeMillis();
            //メイン処理
            this.mainProcess();
            // System.out.println("合計:"+this.sum_deltaopt);
            // System.out.println("数は："+this.underThresholdClusterList.getList().size());
            long end = System.currentTimeMillis();
            retApl.setProcessTime((end - start));
            //System.out.println("提案手法の処理時間:" + retApl.getProcessTime());
            //クラスタを取得して，その割り当て先CPUを取得する．
            //this.printRet();
            //クラスタ再マッピング処理
            this.cluster_remapping();

            //this.mainProcessLB();
            //System.out.println(92);
            //後処理を行う．
            this.postProcess();
            //System.out.println("LBクラスタ数:"+this.LBCount);
            //  System.out.println(97);

            return this.retApl;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * クラスタの再マッピング処理を行います．
     * 元々の処理では，未クラスタの部分を通信ありとして割り当てていたので，
     * 実際の割り当てでは同一マシンで実行した方が早いかもしれない．
     * そこで，同一マシンの他コアに割り当てられないかを検査し，
     * クラスタを移動させる．
     */
    public void reMappingProcess() {

    }


    public WSLInfo getNonClusteredSize(TaskCluster cluster) {
        long tmpWSL_weight = 0;
        long minTaskSize = Constants.MAXValue;
        long maxTaskSize = 0;
        long minDataSize = Constants.MAXValue;
        long maxDataSize = 0;

        WSLInfo info = new WSLInfo();

        //まず，WSL値を取得する．これは，指定のclusterのレベル値でわかる．
        //当該クラスタからみて，次の集約済みクラスタまでの単一クラスタ集合を取得する．
        //当該クラスタのblevelを支配しているタスクを取得する．
        AbstractTask bottomTask = this.retApl.findTaskByLastID(cluster.getBsucTaskID());
        if (bottomTask.getDpredList().isEmpty()) {

        } else {
            AbstractTask tpredTask = this.retApl.findTaskByLastID(bottomTask.getTpred().get(1));
            while (!tpredTask.getDpredList().isEmpty()) {
                Vector oldID = tpredTask.getIDVector();
                tmpWSL_weight += tpredTask.getMaxWeight();

                tpredTask = this.retApl.findTaskByLastID(tpredTask.getTpred().get(1));
                if (tpredTask.getMaxWeight() > 0) {
                    info.setMaxTaskSize(Math.max(tpredTask.getMaxWeight(), info.getMaxTaskSize()));
                    info.setMinTaskSize(Math.min(tpredTask.getMaxWeight(), info.getMinTaskSize()));
                }

                if (tpredTask.getDpredList().isEmpty()) {
                    break;
                } else {
                    DataDependence dd = tpredTask.findDDFromDsucList(tpredTask.getIDVector(), oldID);
                    if (dd.getMaxDataSize() > 0) {
                        info.setMaxDataSize(Math.max(dd.getMaxDataSize(), info.getMaxDataSize()));
                        info.setMinDataSize(Math.min(dd.getMaxDataSize(), info.getMinDataSize()));
                    }
                }


            }
        }

        //Bsucタスクを取得する．
     /*   if(bottomTask.getBsuc().size()<1){
           return bottomTask.getMaxWeight();

        } */
        AbstractTask bsucTask = this.retApl.findTaskByLastID(bottomTask.getBsuc().get(1));

        //bsucTaskが属するクラスタ情報を取得する．
        TaskCluster bsucCluster = this.retApl.findTaskCluster(bsucTask.getClusterID());
        long sumTaskSize = bottomTask.getMaxWeight();
        //System.out.println("bsucID;"+bsucTask.getIDVector().get(1));
        //単一クラスタである限りのループ
        int cnt = 0;
        DataDependence dd = null;
        this.incomming_dataSize = 0;
        //
        // while (bsucCluster.getTaskSet().getList().size() == 1) {
        while (this.uexClusterList.contains(bsucCluster.getClusterID())) {
            //サイズを加算する．
            sumTaskSize += bsucTask.getMaxWeight();
            if (bsucTask.getMaxWeight() > 0) {

                info.setMaxTaskSize(Math.max(bsucTask.getMaxWeight(), info.getMaxTaskSize()));
                info.setMinTaskSize(Math.min(bsucTask.getMaxWeight(), info.getMinTaskSize()));
            }
            //もしENDタスクであれば，終了する．
            if (bsucTask.getDsucList().size() == 0) {
                //if(bsucTask.getBsuc() == null){
                //sumTaskSize += bsucTask.getMaxWeight();
                break;
            }

            dd = bsucTask.findDDFromDsucList(bsucTask.getIDVector(), bsucTask.getBsuc());

            if (dd.getMaxDataSize() > 0) {
                info.setMaxDataSize(Math.max(dd.getMaxDataSize(), info.getMaxDataSize()));
                info.setMinDataSize(Math.min(dd.getMaxDataSize(), info.getMinDataSize()));
            }


            bsucTask = this.retApl.findTaskByLastID(bsucTask.getBsuc().get(1));
            bsucCluster = this.retApl.findTaskCluster(bsucTask.getClusterID());


            //System.out.println("bsucID;"+bsucTask.getIDVector().get(1));
            //cnt++;
        }
        tmpWSL_weight += sumTaskSize;

        //もしBsucTaskがENDタスク出ない場合，残りのサイズを計算する．
        if (bsucTask.getDsucList().size() > 0) {
            //bsucTaskを起点にして，ENDタスクまで見る．
            while (bsucTask.getDsucList().size() != 0) {
                tmpWSL_weight += bsucTask.getMaxWeight();
                if (bsucTask.getMaxWeight() > 0) {

                    info.setMaxTaskSize(Math.max(bsucTask.getMaxWeight(), info.getMaxTaskSize()));
                    info.setMinTaskSize(Math.min(bsucTask.getMaxWeight(), info.getMinTaskSize()));
                }


                dd = bsucTask.findDDFromDsucList(bsucTask.getIDVector(), bsucTask.getBsuc());

                bsucTask = this.retApl.findTaskByLastID(bsucTask.getBsuc().get(1));

                if (dd.getMaxDataSize() > 0) {
                    info.setMaxDataSize(Math.max(dd.getMaxDataSize(), info.getMaxDataSize()));
                    info.setMinDataSize(Math.min(dd.getMaxDataSize(), info.getMinDataSize()));
                }


            }


        }
        this.incommingCluster = bsucCluster;
        if (dd != null) {
            this.incomming_dataSize = dd.getMaxDataSize();
        }

        info.setWsl_weight(tmpWSL_weight);
        info.setNonClusteredTaskSize(sumTaskSize);

        //System.out.println("***END***");
        //System.out.println("タスク数:"+cnt);
        // System.out.println("size:"+sumTaskSize);
        // return sumTaskSize;
        return info;
    }

    /**
     * 指定のクラスタとコアで，${\delta_{opt}}$を計算する．
     * WSLの経路上で，クラスタリング済みである部分とそうでない部分の
     * レベル値を計算する．
     * 1. クラスタリング済みであるクラスタたちのレベル値の合計値（=WSL値の一部）
     * を計算する．
     * 2. WSL - 1の値も計算しておく．
     * なお，この処理は，freeクラスタリストに対してのみ行われる．
     *
     * @param cluster
     * @param cpu
     * @return
     */
    public long calcDeltaWSL(TaskCluster cluster, CPU cpu, WSLInfo info) {
        //当該CPUの最小の帯域幅を取得する．
        long minLink = this.env.getMinLink(cpu.getCpuID());
        long ownLink = this.env.getMachineList().get(cpu.getMachineID()).getBw();
        //wsl = this.wcp;
        //δ_{opt}値を計算する．
        /*long delta_opt = (long)Math.sqrt(Calc.getRoundedValue(Calc.getRoundedValue((double)1/cpu.getSpeed())*((single_wsl * this.maxData*
                Calc.getRoundedValue((double)1/minLink - (double)1/this.maxLink))+
                Calc.getRoundedValue((double)this.minData/this.maxLink)*(wsl-single_wsl))));
        */

        //this.calcWCP();
        //当該クラスタのTpredを取得する。
        long delta_link = 0;
        long delta_comtime = 0;

        long delta_incomming_comtime = 0;


        AbstractTask topTask = this.retApl.findTaskByLastID(cluster.getTopTaskID());

        if (topTask.getDpredList().isEmpty()) {

        } else {
            //topTaskのTpredのタスクが属するクラスタを取得
            AbstractTask predTask = this.retApl.findTaskByLastID(topTask.getTpred().get(1));
            //predTaskが属するクラスタを取得する．
            TaskCluster predCluster = this.retApl.findTaskCluster(predTask.getClusterID());
            //CPUを取得する．
            // if (predCluster.getCPU().getCpuID().longValue() != cpu.getCpuID().longValue()) {
            if (predCluster.getCPU().getMachineID() != cpu.getMachineID()) {
                delta_link = this.env.getNWLink(predCluster.getCPU().getCpuID(), cluster.getCPU().getCpuID());
                DataDependence dd = topTask.findDDFromDpredList(predTask.getIDVector(), topTask.getIDVector());
                delta_comtime = this.env.getSetupTime() + dd.getMaxDataSize() / delta_link;
            } else {
                //delta_comtime = -10000;
            }
        }
        //System.out.println("comtime:"+delta_comtime);

        CPU in_cpu = this.incommingCluster.getCPU();
        long in_nwtime = 0;
        if (!in_cpu.isVirtual()) {
            //マシンが異なれば，通信時間が発生
            if (cpu.getMachineID() != in_cpu.getMachineID()) {
                in_nwtime = this.env.getSetupTime() + this.incomming_dataSize / this.env.getNWLink(cpu.getCpuID(), in_cpu.getCpuID());
            }

        }

        /*long delta_opt = (long) Math.sqrt(Calc.getRoundedValue(Calc.getRoundedValue((double) 1 / cpu.getSpeed()) * (single_wsl *
                ((Calc.getRoundedValue((double) this.maxData / ownLink - (double) this.maxData / this.maxLink)
                        + (double) this.minWorkload / this.maxSpeed + (double) this.minData / this.maxLink))
                + (Calc.getRoundedValue((double) this.minWorkload / cpu.getSpeed()) * (this.wcp - single_wsl)))));
                */
        //もしstaticモードであれば，そのまま返す．
        long delta_opt = 0;
        long delta_opt2 = 0;

        //     long minimum_link = this.env.getMinLink(cpu.getCpuID());
        //   long minimum_link =this.env.getBWFromCPU(cpu);
        //  long minimum_link =  this.env.getAveLink(Long.valueOf(cpu.getCpuID()).intValue());
        long minimum_link = ownLink;
        ///    long minimum_link = this.env.getAveLink(Long.valueOf(cpu.getCpuID()).intValue());

        //System.out.println("ORG_WSL:"+ info.getWsl_weight() +"Fin_Part:"+info.getNonClusteredTaskSize());
        if (this.isDeltaStatic) {
            //  delta_opt = (long)Calc.getRoundedValue(Math.sqrt(((double)this.wcp/(double)cpu.getSpeed())*((double)this.maxWorkload/(double)cpu.getSpeed()+(double)this.maxData/(double)ownLink)));
            delta_opt = (long) Calc.getRoundedValue(
                    Math.sqrt(((double) info.getWsl_weight() / (double) cpu.getSpeed()) *
                            ((double) this.maxWorkload / (double) cpu.getSpeed() + (double) this.maxData / (double) ownLink)));

        } else {
            /*オリジナル版*/
            /*delta_opt = (long) Math.sqrt(Calc.getRoundedValue(Calc.getRoundedValue((double) 1 / cpu.getSpeed()) * (info.getNonClusteredTaskSize() *
                    ((Calc.getRoundedValue((double) info.getMaxDataSize() / minimum_link - (double) info.getMaxDataSize() / this.maxLink)
                            + (double) info.getMinTaskSize() / this.maxSpeed + (double) info.getMinDataSize() / this.maxLink))
                    + (Calc.getRoundedValue((double) info.getMinTaskSize() / cpu.getSpeed()) * (info.getWsl_weight() - info.getNonClusteredTaskSize())))));
                    */
            /*変更版*/
            delta_opt = (long) Math.sqrt(Calc.getRoundedValue(Calc.getRoundedValue((double) 1 / cpu.getSpeed()) * (info.getNonClusteredTaskSize() *
                    ((Calc.getRoundedValue((double) info.getMaxDataSize() / minimum_link /*- (double) info.getMaxDataSize() / this.maxLink*/)
                            + (double) info.getMaxTaskSize() / this.minSpeed + (double) info.getMaxDataSize() / this.minLink))
                    + (Calc.getRoundedValue((double) info.getMaxTaskSize() / cpu.getSpeed()) * (info.getWsl_weight() - info.getNonClusteredTaskSize())))));


//               delta_opt = (long)Calc.getRoundedValue(Math.sqrt(((double)this.wcp/(double)cpu.getSpeed())*((double)this.maxWorkload/(double)cpu.getSpeed()+(double)this.maxData/(double)ownLink)));


     /*       delta_opt = (long) Math.sqrt(Calc.getRoundedValue(Calc.getRoundedValue((double) 1 / cpu.getSpeed()) * (info.getNonClusteredTaskSize() *
                               ((Calc.getRoundedValue((double) info.getMaxDataSize() / minimum_link- (double) info.getMaxDataSize() / this.maxLink)
                                       + (double) info.getMaxTaskSize() / this.maxSpeed + (double) info.getMaxDataSize() / this.maxLink))
                               + (Calc.getRoundedValue((double) info.getMaxTaskSize() / cpu.getSpeed()) * (info.getWsl_weight()- info.getNonClusteredTaskSize())))));
*/


        }
        long tmpValue = (long) Calc.getRoundedValue(Calc.getRoundedValue((double) 1 / (delta_opt * cpu.getSpeed())) * (info.getNonClusteredTaskSize() *
                ((Calc.getRoundedValue((double) info.getMaxDataSize() / (double) ownLink - (double) info.getMaxDataSize() / (double) this.maxLink)
                        + (double) info.getMinTaskSize() / (double) this.maxSpeed + (double) info.getMinDataSize() / (double) this.maxLink))
                + (Calc.getRoundedValue((double) info.getMinTaskSize() / (double) cpu.getSpeed()) * (info.getWsl_weight() - info.getNonClusteredTaskSize()))));


        //当該CPUに対して，クラスタ実行時間のしきい値を設定する．
        cpu.setThresholdTime((long) this.sizeRate * delta_opt);
        //cpu.setThresholdTime(delta_opt);


        //次に，算出した下限値を考慮して，ΔWSLの上限値を計算する．
        //なお，βの値は，「自分と性能が同一で，帯域幅が同等以上であるような計算機への割り当て」
        //を想定した場合のWSLの増分の上限値を意味している．そのため，βの値は，コアではなくて計算機のbw値
        //をそのまま使うことになる．

        long Delta_wsl = (delta_opt + info.getMaxTaskSize() / cpu.getSpeed() + tmpValue - (info.getWsl_weight() - info.getNonClusteredTaskSize()) / info.getMaxTaskSize()
                + info.getWsl_weight() / cpu.getSpeed() + delta_comtime + in_nwtime);

        //自身のclusterの後続タスク?クラスタ済みであるクラスタまでの経路上の
        //タスクサイズの合計値を取得する．
        return Delta_wsl;
    }

    public double getSizeRate() {
        return sizeRate;
    }

    public void setSizeRate(double sizeRate) {
        this.sizeRate = sizeRate;
    }

    /**
     * @param task
     * @param set
     * @return
     */
    public CustomIDSet calcMaxWCP(AbstractTask task, CustomIDSet set) {

        //もしENDタスクであれば，終了．
        if (task.getDsucList().isEmpty()) {
            task.setWblevel(task.getMaxWeight());
            set.add(task.getIDVector().get(1));
            return set;
        }

        //後続タスクを取得
        Iterator<DataDependence> dsucIte = task.getDsucList().iterator();
        long wBlevel = 0;
        //後続タスクたちに対するループ
        while (dsucIte.hasNext()) {
            DataDependence dsuc = dsucIte.next();
            AbstractTask sucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));

            //もし後続タスクがチェック済みであれば，すっ飛ばす．
            if (set.contains(dsuc.getToID().get(1))) {

            } else {
                CustomIDSet retSet = this.calcMaxWCP(sucTask, set);
                set.addAll(retSet);

            }
            TaskCluster cluster = this.retApl.findTaskCluster(task.getClusterID());
            CPU cpu = cluster.getCPU();

            //long tmpValue = sucTask.getWblevel() + task.getMaxWeight() / cpu.getSpeed();
            long tmpValue = sucTask.getWblevel() + task.getMaxWeight();
            if (tmpValue >= wBlevel) {
                wBlevel = tmpValue;
            }

        }
        set.add(task.getIDVector().get(1));
        task.setWblevel(wBlevel);

        return set;

    }


    /**
     * @return
     */
    public long calcWCP() {
        Iterator<Long> startTaskIte = this.retApl.getStartTaskSet().iterator();
        CustomIDSet set = new CustomIDSet();

        long retBlevel = 0;
        //startタスクに対するループ
        while (startTaskIte.hasNext()) {
            Long id = startTaskIte.next();
            AbstractTask startTask = this.retApl.findTaskByLastID(id);
            //後続タスクを取得
            Iterator<DataDependence> dsucIte = startTask.getDsucList().iterator();
            long wBlevel = 0;
            //後続タスクたちに対するループ
            while (dsucIte.hasNext()) {
                DataDependence dsuc = dsucIte.next();
                AbstractTask sucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));

                //もし後続タスクがチェック済みであれば，すっ飛ばす．
                if (set.contains(dsuc.getToID().get(1))) {

                } else {
                    CustomIDSet retSet = this.calcMaxWCP(sucTask, set);
                    set.addAll(retSet);

                }
                long tmpValue = sucTask.getWblevel() + startTask.getMaxWeight();
                if (tmpValue >= wBlevel) {
                    wBlevel = tmpValue;
                }

            }
            set.add(startTask.getIDVector().get(1));
            startTask.setWblevel(wBlevel);
            if (retBlevel <= wBlevel) {
                retBlevel = wBlevel;
            }

        }

        return retBlevel;

    }

    /**
     * @param pivot
     * @param bsucTask
     * @param sucTask
     * @return
     */
    public long getBsucBlevel(TaskCluster pivot, AbstractTask bsucTask, AbstractTask sucTask) {
        Iterator<DataDependence> dsucIte = bsucTask.getDsucList().iterator();
        CPU fromCPU = pivot.getCPU();
        long blevel = 0;
        //bsucの後続タスクたち
        while (dsucIte.hasNext()) {
            DataDependence dd = dsucIte.next();
            long tmpValue = 0;
            if (dd.getToID().get(1).longValue() == sucTask.getIDVector().get(1)) {
                tmpValue = this.getInstrunction(bsucTask) / fromCPU.getSpeed() + sucTask.getBlevel();
            } else {
                TaskCluster sucCluster = this.retApl.findTaskCluster(sucTask.getClusterID());
                CPU toCPU = sucCluster.getCPU();
                if (fromCPU.getMachineID() == toCPU.getMachineID()) {
                    tmpValue = this.getInstrunction(bsucTask) / fromCPU.getSpeed() + sucTask.getBlevel();
                } else {
                    tmpValue = this.getInstrunction(bsucTask) / fromCPU.getSpeed() +
                            this.getNWTime(bsucTask.getIDVector().get(1), dd.getToID().get(1), dd.getMaxDataSize(), this.env.getNWLink(fromCPU.getCpuID(), toCPU.getCpuID())) + sucTask.getBlevel();
                }

            }
            if (tmpValue >= blevel) {
                blevel = tmpValue;
            }

        }
        return blevel;

    }

    /**
     * @param pivot
     * @return
     */
    public TaskCluster findTarget(TaskCluster pivot) {
        CustomIDSet outSet = pivot.getOut_Set();
        //CustomIDSet outSet = pivot.getBottomSet();
        Iterator<Long> outIte = outSet.iterator();
        CustomIDSet clusterSet = new CustomIDSet();
        long totalExecNum = this.getClusterInstruction(pivot);
        CPU fromCPU = pivot.getCPU();
        // Iterator<Long> outIte3 = pivot.getOut_Set().iterator();
        Iterator<Long> outIte3 = pivot.getBottomSet().iterator();

        long retBLValue = 10000000;
        TaskCluster retCluster = null;
        Long bsucTaskID = pivot.getBsucTaskID();
        AbstractTask bsucTask = this.retApl.findTaskByLastID(bsucTaskID);
        //pivotがlinearであるとき
        if (pivot.isLinear()) {


            long tmpBlevel = 0;
            //bottomタスクを特定する．
            Iterator<Long> outIte0 = outSet.iterator();
            //実際には一度のループのみである．
            while (outIte0.hasNext()) {
                Long outID = outIte0.next();
                AbstractTask outTask = this.retApl.findTaskByLastID(outID);
                if (this.isBottomTask(outTask)) {
                    //buttomタスクのDsucを取得

                    //AbstractTask bsucBsucTask = this.retApl.findTaskByLastID(bsucTask.getBsuc().get(1));
                    //outTaskのbsucタスクを取得する．
                    AbstractTask bsucBsucTask = this.retApl.findTaskByLastID(outTask.getBsuc().get(1));
                    TaskCluster tmpCluster = this.retApl.findTaskCluster(bsucBsucTask.getClusterID());
                    //buttom-dsucタスクが単一クラスタであればそれを返す．
                    //if ((!this.isClusterAboveThreshold(tmpCluster)) && (tmpCluster.getTopTaskID().longValue() == bsucBsucTask.getIDVector().get(1).longValue())) {

                    if ((!this.isAboveThreshold(tmpCluster)) && (tmpCluster.getTopTaskID().longValue() == bsucBsucTask.getIDVector().get(1).longValue())) {
                        // if((tmpCluster.getTopTaskID().longValue() == bsucBsucTask.getIDVector().get(1).longValue())){
                        retCluster = tmpCluster;
                        //pivot.setLinear(true);
                        //  System.out.println(576);
                        pivot.setLinear(true);
                        return retCluster;
                    } else {
                        //そうでなければbuttomの後続タスクのうち，単一クラスタとなるもののうちでblevelが最大のものを取得
                        Iterator<DataDependence> dsucIte = outTask.getDsucList().iterator();
                        while (dsucIte.hasNext()) {
                            DataDependence dsuc = dsucIte.next();
                            AbstractTask sucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));
                            TaskCluster sucCluster = this.retApl.findTaskCluster(sucTask.getClusterID());

                            //sucTaskがtopタスクであるクラスタ == 単一クラスタということ．
                            //単一クラスタであれば，実際の計算処理にうつる．
                            //if ((!this.isAboveThreshold(sucCluster)) && (sucCluster.getTopTaskID().longValue() == sucTask.getIDVector().get(1).longValue())) {
                            if ((sucCluster.getTopTaskID().longValue() == sucTask.getIDVector().get(1).longValue()) && sucCluster.getTaskSet().getList().size() == 1) {

                                //Machine fromMachine = this.env.getMachineList().get(fromCPU.getMachineID());
                                CPU toCPU = sucCluster.getCPU();
                                long tmpValue = 0;
                                if (fromCPU.getMachineID() == toCPU.getMachineID()) {
                                    tmpValue = sucTask.getBlevel();
                                } else {
                                    tmpValue = this.getNWTime(outTask.getIDVector().get(1), sucTask.getIDVector().get(1), dsuc.getMaxDataSize(),
                                            this.env.getNWLink(fromCPU.getCpuID(), toCPU.getCpuID())) + sucTask.getBlevel();
                                }

                                if (tmpValue >= tmpBlevel) {
                                    tmpBlevel = tmpValue;
                                    retCluster = sucCluster;

                                }
                            }
                        }

                    }
                }
            }

            //retClusterがnullでなければそれを返す．
            if (retCluster != null) {
                // pivot.setLinear(true);
                // System.out.println(617);
                pivot.setLinear(true);

                return retCluster;
            }

            //Dtaskがbottomであれば，そのBsucをtargetとすればよい．
           /* if (this.isBottomTask(bsucTask)) {
                //pivotがLinearかつBsucタスク==Bottomタスクであれば，bsucのbsucが属するクラスタをtargetとする
                //このとき，targetは，ひとつのみのタスクで構成されているはずである．
                AbstractTask bsucBsucTask = this.retApl.findTaskByLastID(bsucTask.getBsuc().get(1));
                TaskCluster tmpCluster = this.retApl.findTaskCluster(bsucBsucTask.getClusterID());
                if ((!this.isAboveThreshold(tmpCluster)) && (tmpCluster.getTopTaskID().longValue() == bsucBsucTask.getIDVector().get(1).longValue())) {
                    retCluster = tmpCluster;
                    pivot.setLinear(true);
                   System.out.println(550);
                    return retCluster;
                }
            }*/


        }
        //そうでなければ，pivotはnon-linearとなるはずである．
        //TopタスクのTpredタスクが属するクラスタがlinearであれば，まとめる．
        AbstractTask tTask = this.retApl.findTaskByLastID(pivot.getTopTaskID());
        if (tTask.getDpredList().isEmpty()) {

        } else {
            if (!this.isAboveThreshold(pivot)) {
                AbstractTask tpredTask = this.retApl.findTaskByLastID(tTask.getTpred().get(1));
                TaskCluster tpredCluster = this.retApl.findTaskCluster(tpredTask.getClusterID());
                if ((tpredCluster.isLinear()) && (pivot.isLinear()) && tpredCluster.getBottomSet().contains(tpredTask.getIDVector().get(1))) {
                    pivot.setLinear(true);

                    return tpredCluster;
                } else {
                    Iterator<DataDependence> tIte = tTask.getDpredList().iterator();
                    long tmpT = 0;
                    TaskCluster tmpRetCluster = null;
                    while (tIte.hasNext()) {
                        DataDependence dd = tIte.next();
                        AbstractTask preTask = this.retApl.findTaskByLastID(dd.getFromID().get(1));
                        TaskCluster c = this.retApl.findTaskCluster(preTask.getClusterID());
                        if ((c.getClusterID().longValue() != pivot.getClusterID().longValue()) && (c.isLinear()) && (pivot.isLinear())) {
                            long nw_time = 0;
                            if (this.isBottomTask(preTask)) {
                                if (c.getCPU().getMachineID() == pivot.getCPU().getMachineID()) {

                                } else {
                                    nw_time = this.getNWTime(preTask.getIDVector().get(1), tTask.getIDVector().get(1), dd.getMaxDataSize(),
                                            this.env.getNWLink(c.getCPU().getCpuID(), pivot.getCPU().getCpuID()));
                                }
                                long tt = preTask.getTlevel() + preTask.getMaxWeight() / c.getCPU().getSpeed() + nw_time;
                                if (tt >= tmpT) {
                                    tmpT = tt;
                                    tmpRetCluster = c;
                                }
                            }

                        }
                    }
                    if (tmpRetCluster != null) {
                        retCluster = tmpRetCluster;
                        // pivot.setLinear(false);
                        //System.out.println(665);
                        pivot.setLinear(true);

                        return retCluster;
                    }

                }
            }

        }

        pivot.setLinear(false);
        /*if ((pivot.getTop_Set().getList().size() == 1) && (pivot.getBottomSet().getList().size() == 1)) {
            pivot.setLinear(true);
        } else {
            pivot.setLinear(false);
        }*/

        AbstractTask tmpSucTask = null;
        long level = Constants.MAXValue;

        long minClsSize = Constants.MAXValue;
        TaskCluster minCluster = null;
        AbstractTask bbsucTask = this.retApl.findTaskByLastID(bsucTask.getBsuc().get(1));
        TaskCluster bbsucCluster = this.retApl.findTaskCluster(bbsucTask.getClusterID());

        if (pivot.getClusterID() != bbsucTask.getClusterID()) {
            if (!this.isAboveThreshold(bbsucCluster)) {
                retCluster = this.retApl.findTaskCluster(bbsucTask.getClusterID());
                // pivot.setLinear(false);
                //  System.out.println(688);

                return retCluster;
            }

        }

        //bottomタスクの後続タスクからtargetが見つからない場合は，
        // outタスクたちに対するループをして後続の単一クラスタを探す．
        while (outIte.hasNext()) {
            Long outID = outIte.next();
            //outタスクを取得する．
            AbstractTask outTask = this.retApl.findTaskByLastID(outID);
            LinkedList<DataDependence> ddList = outTask.getDsucList();
            Iterator<DataDependence> ddIte = ddList.iterator();
            long destValue = this.calculateSumValue(outTask.getDestTaskSet());

            //outタスクの後続タスクたちに対するループ
            while (ddIte.hasNext()) {
                DataDependence dd = ddIte.next();
                AbstractTask sucTask = this.retApl.findTaskByLastID(dd.getToID().get(1));

                Long sucClusterID = sucTask.getClusterID().longValue();
                long TLValue = 0;
                //  long BLValue = 0;
                if (sucClusterID.longValue() != pivot.getClusterID().longValue()) {
                    //後続クラスタを見つけた場合，後続クラスタを取得する．
                    TaskCluster sucCluster = this.retApl.findTaskCluster(sucClusterID);
                    long tmpSize = this.getClusterInstruction(sucCluster);
                    if (tmpSize <= minClsSize) {
                        minClsSize = tmpSize;
                        minCluster = sucCluster;
                    }
                    //sucTaskがTopタスクである場合
                    //   if (sucCluster.getClusterID().longValue() == sucTask.getIDVector().get(1).longValue()) {
                    if (sucCluster.getTopTaskID() == sucTask.getIDVector().get(1).longValue()) {
                        if (this.isAboveThreshold(sucCluster)) {
                            continue;
                        }

                        //クラスタリングした後のBsucタスクのblevelを計算する．
                        if (bsucTask.findDDFromDsucList(bsucTask.getIDVector(), sucTask.getIDVector()) != null) {
                            //もしbsucの後続タスクにsucTaskが含まれていれば，TL値は不変である
                            TLValue = bsucTask.getTlevel();
                            //  BLValue = this.getInstrunction(bsucTask)/this.minSpeed + bsucTask.getBlevel();


                        } else {
                            TLValue = bsucTask.getTlevel() + this.getInstrunction(sucTask) / fromCPU.getSpeed();
                            // BLValue = bsucTask.getBlevel();
                        }
                        //次にBlevel値を取得する．
                        long BLValue = this.getBsucBlevel(pivot, bsucTask, sucTask);
                        //BsucのLevel値が最も抑えられるもの
                        if (level >= TLValue + BLValue) {
                            level = TLValue + BLValue;
                            retCluster = sucCluster;
                            tmpSucTask = sucTask;
                        }

                    } else {

                    }
                }
            }
        }

        //System.out.println("もともとのレベル: "+( bsucTask.getTlevel()+bsucTask.getBlevel()+"/ 更新後のレベル: "+ level));
        //pivotがlinearかつBsucがbottomであり，上の条件でクラスタリングされればpivotはlinear．
        //よって，このときに初めてクラスタリング後のpivotはnon-linearとする．
        if (retCluster != null) {
            // if ((this.isClusterAboveThreshold(pivot)) && (pivot.isLinear())) {
            if ((this.isAboveThreshold(pivot)) && (pivot.isLinear())) {
                if (!this.isBottomTask(tmpSucTask)) {
                    //return null;
                }
            }
            return retCluster;
        } else {
            //sucTaskがTopタスクでない場合の処理．つまり(e)の場合
            PriorityQueue<LevelInfo> levelQueue = new PriorityQueue<LevelInfo>(5, new LevelComparator());
            PriorityQueue<LevelInfo> linearQueue = new PriorityQueue<LevelInfo>(5, new LevelComparator());


            while (outIte3.hasNext()) {
                Long id = outIte3.next();
                //outタスク
                AbstractTask outTask = this.retApl.findTaskByLastID(id);
                //outタスクのBsucを取得
                if(outTask.getDsucList().isEmpty()){
                    continue;
                }
                AbstractTask outbsucTask = this.retApl.findTaskByLastID(outTask.getBsuc().get(1));
                TaskCluster outbsucCluster = this.retApl.findTaskCluster(outbsucTask.getClusterID());
                CPU outbsucCPU = outbsucCluster.getCPU();
                CPU pivotCPU = pivot.getCPU();
                DataDependence dd = outTask.findDDFromDsucList(outTask.getIDVector(), outbsucTask.getIDVector());

                long destValue = this.calculateSumValue(outTask.getDestTaskSet());
                long totalExecTime = this.calculateSumValue(pivot.getTaskSet());
                long Svalue = ((totalExecTime - destValue) / pivotCPU.getSpeed());
               /* if (this.isAboveThreshold(outbsucCluster)) {
                    continue;
                }*/
                //outタスクのbsucタスクが異なるクラスタであれば，計算する．
                if ((pivot.getClusterID() != outbsucCluster.getClusterID()) && !this.isAboveThreshold(outbsucCluster)) {
                    long nw_time = 0;
                    if (pivotCPU.getMachineID() == outbsucCPU.getMachineID()) {

                    } else {
                        nw_time = this.getNWTime(outTask.getIDVector().get(1), outbsucTask.getIDVector().get(1), dd.getMaxDataSize(),
                                this.env.getNWLink(pivotCPU.getCpuID(), outbsucCPU.getCpuID()));
                    }

                    long bvalue = Svalue + nw_time + outbsucTask.getBlevel();
                    LevelInfo info = new LevelInfo(outTask.getIDVector().get(1), outbsucTask.getIDVector().get(1), bvalue);
                    levelQueue.offer(info);
                }

                Iterator<DataDependence> dsucIte11 = outTask.getDsucList().iterator();
                while (dsucIte11.hasNext()) {
                    AbstractTask sucsucTask = this.retApl.findTaskByLastID(dsucIte11.next().getToID().get(1));
                    TaskCluster sucsucCluster = this.retApl.findTaskCluster(sucsucTask.getClusterID());
                    //異なるクラスタであれば，計算対象
                    if (pivot.getClusterID() != sucsucCluster.getClusterID()) {
                        if (sucsucCluster.isLinear()/*&&(sucsucCluster.getTopTaskID()==sucsucTask.getIDVector().get(1))*/) {
                            long nw_time = 0;
                            if (pivotCPU.getMachineID() == outbsucCPU.getMachineID()) {

                            } else {
                                nw_time = this.getNWTime(outTask.getIDVector().get(1), outbsucTask.getIDVector().get(1), dd.getMaxDataSize(),
                                        this.env.getNWLink(pivotCPU.getCpuID(), outbsucCPU.getCpuID()));
                            }
                            long bvalue2 = Svalue + nw_time + outbsucTask.getBlevel();
                            LevelInfo info2 = new LevelInfo(outTask.getIDVector().get(1), sucsucTask.getIDVector().get(1), bvalue2);
                            linearQueue.offer(info2);
                        }

                    }
                }
            }
            if (levelQueue.isEmpty()) {
                //一応，後続クラスタのうちでlinearなものを探し，
                retCluster = null;
                //System.out.println("来た");
                //後続クラスタから選択する．
                //System.out.println("pivot数"+pivot.getTaskSet().getList().size());
                //pivotのbottomタスクのbsucタスクが属するクラスタとする．
                Iterator<Long> out10 = pivot.getBottomSet().iterator();
                long finalLevel = Constants.MAXValue;

                while (out10.hasNext()) {
                    AbstractTask outT = this.retApl.findTaskByLastID(out10.next());
                    // if (this.isBottomTask(outT)) {
                    //outTのBsucが属するクラスタを返す．
                    AbstractTask bsuc = this.retApl.findTaskByLastID(outT.getBsuc().get(1));
                    TaskCluster bsucC = this.retApl.findTaskCluster(bsuc.getClusterID());
                   /*long destValue = this.calculateSumValue(bsuc.getDestTaskSet());
                    long totalExecTime = this.calculateSumValue(pivot.getTaskSet())+this.calculateSumValue(bsucC.getTaskSet());
                    long Svalue = ((totalExecTime - destValue) / fromCPU.getSpeed());

                    long tmpLevel = Svalue + bsucC.getBlevel();
                    if(finalLevel >= tmpLevel){
                         retCluster = bsucC;
                     }
                   */
                    /**追加分 START**/
                    //クラスタリングすることにより，pivotのレベルが増加すればダメ．
                    //まず，Tlevelは，targetのTlevelによって決まる．
                    if (bsucC.getClusterID().longValue() != pivot.getClusterID().longValue()) {
                        //とりあえず，仮想的なクラスタを生成する．
                        Iterator<Long> pivotTopIte = pivot.getTop_Set().iterator();
                        Iterator<Long> targetTopIte = bsucC.getTop_Set().iterator();
                        Iterator<Long> targetTIte = bsucC.getTaskSet().iterator();

                        Iterator<Long> pivotTIte = pivot.getTaskSet().iterator();
                        //pivotのtmpDest初期化
                        while (pivotTIte.hasNext()) {
                            Long id = pivotTIte.next();
                            AbstractTask t = this.retApl.findTaskByLastID(id);
                            t.getTmpDestIDSet().initializeTaskSet();
                            t.seteBlevel(-1);
                        }

                        //targetのtmpDest初期化
                        while (targetTIte.hasNext()) {
                            Long id = targetTIte.next();
                            AbstractTask t = this.retApl.findTaskByLastID(id);
                            t.getTmpDestIDSet().initializeTaskSet();
                            t.seteBlevel(-1);
                        }
                        //pivot内のDestの一時更新
                        while (pivotTopIte.hasNext()) {
                            Long tID = pivotTopIte.next();
                            AbstractTask pTask = this.retApl.findTaskByLastID(tID);
                            this.updateTempDestTask(new CustomIDSet(), pTask, pivot, bsucC);

                        }
                        //target内のDestの一時更新
                        while (targetTopIte.hasNext()) {
                            Long tID = targetTopIte.next();
                            AbstractTask pTask = this.retApl.findTaskByLastID(tID);
                            this.updateTempDestTask(new CustomIDSet(), pTask, pivot, bsucC);
                        }

                        long destValue = this.calculateSumValue(outT.getTmpDestIDSet());
                        long totalExecTime = this.calculateSumValue(pivot.getTaskSet()) + this.calculateSumValue(bsucC.getTaskSet());
                        long Svalue = ((totalExecTime - destValue) / fromCPU.getSpeed());
                        long afterLevel = Math.max(pivot.getTlevel(), bsucC.getTlevel()) + Svalue + this.getTmpBlevel(new CustomIDSet(), outT, pivot, bsucC);
                        if (finalLevel >= afterLevel) {
                            if (pivot.getTlevel() + pivot.getBlevel() >= afterLevel) {
                                finalLevel = afterLevel;
                                retCluster = bsucC;
                            }
                        }

                    }

                    if (retCluster != null) {
                        return retCluster;

                    } else {
                        //それでもない場合は，最小サイズのクラスタをtargetとする．
                        /*Iterator<Long> clusterIte = this.uexClusterList.iterator();

                        long sizeValue = 9999999;

                        while(clusterIte.hasNext()){
                            Long id = clusterIte.next();
                            TaskCluster cls = this.retApl.findTaskCluster(id);
                            //TaskCluster cls = clusterIte.next();
                            if(cls.getClusterID() == pivot.getClusterID()){
                                continue;
                            }else{

                                long tmpSize = this.calculateSumValue(cls.getTaskSet());
                                if(tmpSize <= sizeValue){
                                    sizeValue = tmpSize;
                                    retCluster = cls;
                                }
                            }

                        }
                        return retCluster;
                        */
                        return null;
                    }


                    /**追加分 END**/


                }
            } else {
                LevelInfo maxInfo = levelQueue.peek();
                AbstractTask retTask = this.retApl.findTaskByLastID(maxInfo.getToTaskID());
                retCluster = this.retApl.findTaskCluster(retTask.getClusterID());

            }

            if (linearQueue.isEmpty()) {

            } else {
                LevelInfo maxInfo = linearQueue.peek();
                AbstractTask retTask = this.retApl.findTaskByLastID(maxInfo.getToTaskID());
                retCluster = this.retApl.findTaskCluster(retTask.getClusterID());

            }

        }

        return retCluster;
    }

    /**
     * pivotがlinearな場合に，WSLタスクに含まれている場合に続けるかどうか
     *
     * @param pivot
     * @return
     */
    public boolean isBottomInWSL(TaskCluster pivot) {
        if (pivot.isLinear()) {
            if (pivot.getBottomSet().getList().size() == 1) {
                Long bottomID = pivot.getBottomSet().getList().get(0);

            } else {
                return false;
            }
        } else {
            return false;
        }

        return false;

    }

    public long getTmpBlevel(CustomIDSet set, AbstractTask t, TaskCluster pivot, TaskCluster target) {
        if (set.contains(t.getIDVector().get(1))) {
            return t.geteBlevel();
        }

        Iterator<DataDependence> dsucIte = t.getDsucList().iterator();
        long tmpValue = 0;

        while (dsucIte.hasNext()) {
            DataDependence dsuc = dsucIte.next();
            AbstractTask dsucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));
            long eValue = 0;
            if ((dsucTask.getClusterID().longValue() != pivot.getClusterID().longValue()) && (dsucTask.getClusterID().longValue() != target.getClusterID().longValue())) {
                eValue = t.geteBlevel();
                set.add(dsucTask.getIDVector().get(1));
                continue;
            } else {
                eValue = this.getTmpBlevel(set, dsucTask, pivot, target);
            }


            if (eValue >= tmpValue) {
                tmpValue = eValue;
            }

        }
        return t.getMaxWeight() / pivot.getCPU().getSpeed() + tmpValue;

    }


    public CustomIDSet updateTempDestTask(CustomIDSet set,
                                          AbstractTask task,
                                          TaskCluster pivot,
                                          TaskCluster target
            /*CustomIDSet tmpTaskSet*/) {
        //TaskCluster pivot = this.retApl.findTaskCluster(clusterID);
        CustomIDSet checkedSet = pivot.getDestCheckedSet();
        //task.getTmpDestIDSet().initializeTaskSet();

        //既にチェック済みであればそのまま返す．
        if (checkedSet.contains(task.getIDVector().get(1))) {
            return task.getDestTaskSet();
        }

        //もし後続タスク自身が異なるクラスタであれば，ここで終了．
        //補題: なぜなら，それ以降はかならず異なるクラスタとなるはずだから．
        if ((task.getClusterID().longValue() != pivot.getClusterID()) && (task.getClusterID().longValue() != target.getClusterID())) {

            return set;
        } else {
            //タスク自身が，当該クラスタ内に属しているときの処理
            set.add(task.getIDVector().get(1));
            LinkedList<DataDependence> dsuclist = task.getDsucList();
            Iterator<DataDependence> ite = dsuclist.iterator();
            if (dsuclist.isEmpty()) {

                //後続タスクがなければ,そのままリターン
                return set;
            } else {
                while (ite.hasNext()) {
                    DataDependence dd = ite.next();
                    AbstractTask dsuc = this.retApl.findTaskByLastID(dd.getToID().get(1));
                    //再帰呼び出しにより,後続処理結果のセットをマージする．
                    CustomIDSet retSet = this.updateTempDestTask(new CustomIDSet(), dsuc, pivot, target);
                    set.getObjSet().addAll(retSet.getObjSet());

                }
                //タスク自身にセットする．
                task.setTmpDestIDSet(set);
                task.getTmpDestIDSet().add(task.getIDVector().get(1));

                //pivot.getDestCheckedSet().add(task.getIDVector().get(1));
                //set.add(task.getIDVector().get(1));
                return set;
            }
        }
    }


    /**
     * 当該クラスタが，STARTクラスタかどうかを判断します．
     * 具体的には、
     * - INタスク集合が一つであること
     * - Topタスクが，Startタスク(先行タスクがない）であること
     * のいずれも満たせば，STARTクラスタと判断されます．
     *
     * @param cluster
     * @return
     */
    public boolean isStartCluster(TaskCluster cluster) {
        //まずはINタスク集合を取得します．
        CustomIDSet inSet = cluster.getIn_Set();
        //IN集合が一つかどうか．
        //一つでなければ，この時点でSTARTクラスタではないと判断される
        /**if(inSet.getList().size() != 1){
         return false;
         }**/
        boolean isStart = true;
        Iterator<Long> idIte = inSet.iterator();
        while (idIte.hasNext()) {
            Long id = idIte.next();
            AbstractTask t = this.retApl.findTaskByLastID(id);
            if (t.getDpredList().isEmpty()) {
                continue;
            } else {
                isStart = false;
                break;
            }

        }
        return isStart;
    }

    public HashMap<String, TaskCluster> getClusterPair(TaskCluster pivot, TaskCluster target) {
        HashMap<String, TaskCluster> retMap = new HashMap<String, TaskCluster>();
        //TaskCluster pivot = null;
        // TaskCluster target = null;
        //UEXから，Δsl_{w,up}^{s-1}(δ_{opt}^s(P_i)値が最小のクラスタを取得する．
        TaskCluster pivotCandidate = null;
        // System.out.println("num:"+this.freeClusterList.getList().size());
        if (pivot != null) {
            pivotCandidate = pivot;
        } else {
            pivotCandidate = this.getMaxLevelCluster(this.freeClusterList);
        }

        //もしpivotCandidateが，出辺を持っていないとき，つまりENDクラスタである場合(targetが選べない場合)


        if (pivotCandidate.getOut_Set().isEmpty()) {
            if (this.isStartCluster(pivotCandidate)) {
                //StartクラスタかつENDクラスタであれば，終わりとする．
                this.checkEXSingleCluster(pivotCandidate);
                // System.out.println("*****470*****");

                return null;
                //continue;
            } else {
                //pivotCandidateがENDクラスタであり，かつStartクラスタではない場合．
                //pivotを，TL値を決定づけるタスククラスタ/targetを自分自身とする．
                pivot = this.getNewPivot(pivotCandidate);
                // System.out.println("pivot候補のタスク数: "+ pivotCandidate.getObjSet().getList().size());
                //targetを，pivot候補とする．
                target = pivotCandidate;
            }
        } else {
            //targetを取得する．
            target = this.findTarget(pivotCandidate);
            if (target == null) {
                if (this.isStartCluster(pivotCandidate)) {
                    //STARTクラスタであれば，後続クラスタを選ぶしか無い．
                    this.checkEXSingleCluster(pivotCandidate);
                    //System.out.println("*****486*****");
                    return null;
                } else {
                    //targetが取得できなければ，上へクラスタ
                    //pivotを，TL値を決定づけるタスククラスタ/targetを自分自身とする．
                    //pivot = this.getNewPivot(pivotCandidate);
//先行クラスタを取得できない場合は，後続クラスタからOutSetを見てtargetを決める．
                    if (pivot == null) {
                        //System.out.println("****553***");

                    }
                    // System.out.println("pivot候補のタスク数: "+ pivotCandidate.getTask.Set().getList().size());
                    //targetを，pivot候補とする．
                    target = pivotCandidate;
                    this.checkEXSingleCluster(pivotCandidate);
                    // System.out.println("*****908*****");

                    return null;


                }
            } else {
                if ((target.getClusterID() < pivotCandidate.getClusterID()) && pivotCandidate.isLinear() && target.isLinear()) {

                    TaskCluster tmpCluster = pivotCandidate;
                    pivot = target;
                    target = tmpCluster;
                } else {
                    pivot = pivotCandidate;
                }

            }
        }

        retMap.put("pivot", pivot);
        retMap.put("target", target);

        return retMap;
    }

    /**
     * @param
     * @return
     */
    /*public TaskCluster checkEXSingleCluster(TaskCluster cluster) {
        //System.out.println("単一クラスタになる");
        Long topID = cluster.getTopTaskID();
        //クラスタをUEXから削除する．
        this.uexClusterList.remove(cluster.getClusterID());
        //targetクラスタをFreeから削除する．
        this.freeClusterList.remove(cluster.getClusterID());

        //Outタスクの後続タスクの入力辺を"Checked"とする．
        CustomIDSet pOutSet = cluster.getOut_Set();
        Iterator<Long> pOutIte = pOutSet.iterator();
        CustomIDSet clusterSet = new CustomIDSet();

        //Outタスクに対するループ
        while (pOutIte.hasNext()) {
            Long oID = pOutIte.next();
            AbstractTask oTask = this.retApl.findTaskByLastID(oID);
            //Outタスクの後続タスク集合を取得する．
            LinkedList<DataDependence> dsucList = oTask.getDsucList();
            Iterator<DataDependence> dsucIte = dsucList.iterator();
            //Outタスクの，後続タスクたちに対するループ
            while (dsucIte.hasNext()) {
                DataDependence dd = dsucIte.next();
                AbstractTask sucTask = this.retApl.findTaskByLastID(dd.getToID().get(1));
                if (sucTask.getClusterID().longValue() != cluster.getClusterID().longValue()) {
                    //sucTaskの入力辺に"Checked"フラグをつける．
                    sucTask.setCheckedFlgToDpred(oTask.getIDVector(), sucTask.getIDVector(), true);
                    clusterSet.add(sucTask.getClusterID());
                }

            }
        }

        Iterator<Long> clusterIte = clusterSet.iterator();
        while (clusterIte.hasNext()) {
            Long cID = clusterIte.next();
            TaskCluster cls = this.retApl.findTaskCluster(cID);
            if ((this.isAllInEdgeChecked(cls)) && (this.uexClusterList.contains(cID))) {
                this.addFreeClusterList(cID);

            }
        }
        cluster.setTopTaskID(topID);

        return cluster;

    }
    */
    public CPU getAssignedCPU(TaskCluster pivot) {
        CPU assignedCPU = null;
        Iterator<Long> cpuIte = this.unAssignedCPUs.getList().iterator();

        long tmp_wsl = (long) Double.POSITIVE_INFINITY;
        long tmp_wsl2 = 0;
        // long sumTaskSize = this.getNonClusteredSize(pivot);
        WSLInfo info = this.getNonClusteredSize(pivot);
        //WSL値を保存する．
        long wsl = pivot.getTlevel() + pivot.getBlevel();

        //のこりのクラスタ済み部分の値を求める．


        while (cpuIte.hasNext()) {
            Long cpuID = cpuIte.next();
            CPU cpu = this.env.getCPU(cpuID);
            //その時のWSL値を取得する．
            // long wsl_remian_wp = this.calcClusteredWSL_wp(pivot);
            //しきい値の計算＋Δ_wslの取得
            // long delta_wsl = this.calcDeltaWSL(pivot, cpu, this.wcp, sumTaskSize);
            long delta_wsl = this.calcDeltaWSL(pivot, cpu, info);
            if (tmp_wsl > delta_wsl) {
                tmp_wsl = delta_wsl;
                assignedCPU = cpu;
                cpu.setDelta_WSL(tmp_wsl);


            }
        }
        if (unAssignedCPUs.isEmpty()) {

        } else {
            if (assignedCPU != null) {
                this.unAssignedCPUs.remove(assignedCPU.getCpuID());

            }

        }
        //System.out.println("しきい値サイズ:"+assignedCPU.getThresholdTime()*assignedCPU.getSpeed());

        return assignedCPU;
    }

    /**
     * @param cluster
     * @return
     */
    public TaskCluster checkEXSingleCluster(TaskCluster cluster) {
        //System.out.println("単一クラスタになる");
        Long topID = cluster.getTopTaskID();
        //クラスタをUEXから削除する．
        this.uexClusterList.remove(cluster.getClusterID());
        //targetクラスタをFreeから削除する．
        this.freeClusterList.remove(cluster.getClusterID());
        if (!this.isAboveThreshold(cluster)) {
            this.underThresholdClusterList.add(cluster.getClusterID());
        }

        if (cluster.getCPU().isVirtual()) {
            CPU cpu = this.getAssignedCPU(cluster);
            cluster.setCPU(cpu);
            cpu.setTaskClusterID(cluster.getClusterID());
        }

        //Outタスクの後続タスクの入力辺を"Checked"とする．
        CustomIDSet pOutSet = cluster.getOut_Set();
        Iterator<Long> pOutIte = pOutSet.iterator();
        CustomIDSet clusterSet = new CustomIDSet();


        //Iterator<Long> pOutIte = cluster.getOut_Set().iterator();
        CustomIDSet clsSet = new CustomIDSet();
        //Outタスクに対するループ
        while (pOutIte.hasNext()) {
            Long oID = pOutIte.next();
            AbstractTask oTask = this.retApl.findTaskByLastID(oID);
            //Outタスクの後続タスク集合を取得する．
            LinkedList<DataDependence> dsucList = oTask.getDsucList();
            Iterator<DataDependence> dsucIte = dsucList.iterator();
            //Outタスクの，後続タスクたちに対するループ
            while (dsucIte.hasNext()) {
                DataDependence dd = dsucIte.next();
                AbstractTask sucTask = this.retApl.findTaskByLastID(dd.getToID().get(1));
                if (sucTask.getClusterID().longValue() != cluster.getClusterID().longValue()) {
                    //sucTaskの入力辺に"Checked"フラグをつける．
                    sucTask.setCheckedFlgToDpred(oTask.getIDVector(), sucTask.getIDVector(), true);
                    clsSet.add(sucTask.getClusterID());
                }
            }
        }

        Iterator<Long> clsIte = clsSet.iterator();
        while (clsIte.hasNext()) {
            Long clusterid = clsIte.next();
            TaskCluster clster = this.retApl.findTaskCluster(clusterid);
            if (this.isAllInEdgeChecked(clster)) {
                //もしクラスタのInタスクがすべてCheckeであれば，そのクラスタをFreeListへ入れる．
                this.addFreeClusterList(clusterid);
            }
        }
          /*
           //Outタスクに対するループ
           while (pOutIte.hasNext()) {
               Long oID = pOutIte.next();
               AbstractTask oTask = this.retApl.findTaskByLastID(oID);
               //Outタスクの後続タスク集合を取得する．
               LinkedList<DataDependence> dsucList = oTask.getDsucList();
               Iterator<DataDependence> dsucIte = dsucList.iterator();
               //Outタスクの，後続タスクたちに対するループ
               while (dsucIte.hasNext()) {
                   DataDependence dd = dsucIte.next();
                   AbstractTask sucTask = this.retApl.findTaskByLastID(dd.getToID().get(1));
                   if (sucTask.getClusterID().longValue() != cluster.getClusterID().longValue()) {
                       //sucTaskの入力辺に"Checked"フラグをつける．
                       sucTask.setCheckedFlgToDpred(oTask.getIDVector(), sucTask.getIDVector(), true);
                       clusterSet.add(sucTask.getClusterID());
                   }

               }
           }

           Iterator<Long> clusterIte = clusterSet.iterator();
           while (clusterIte.hasNext()) {
               Long cID = clusterIte.next();
               TaskCluster cls = this.retApl.findTaskCluster(cID);
               if ((this.isAllInEdgeChecked(cls)) && (this.uexClusterList.contains(cID))) {
                   this.addFreeClusterList(cID);

               }
           }
           */
        cluster.setTopTaskID(topID);

        return cluster;

    }

    public void mainProcessLB() {
        int cnt = 0;
        //まずはUnderリストへの追加処理
        //System.out.println("数は："+this.underThresholdClusterList.getList().size());
       /* if(this.underThresholdClusterList.getList().size() ==1){
            return;
        }*/
        long preID = 0;
        long afterID = 1;
        while (!this.underThresholdClusterList.isEmpty()) {
            //  System.out.println("under数:"+this.underThresholdClusterList.getList().size());
            if (this.underThresholdClusterList.getList().size() == 1) {
                return;
            }
            //まず，UEXから最小のクラスタを取得する．
            //TaskCluster checkCluster = this.getMinSizeCluster(this.underThresholdClusterList);
            TaskCluster checkCluster = this.getMaxLevelCluster(this.underThresholdClusterList);
            if (preID == checkCluster.getClusterID()) {
                //    System.out.println("test");
            }
            // System.out.println("checkのID:"+checkCluster.getClusterID());
            if (checkCluster == null) {
                return;
            }


            //後続クラスタたちを取得する．
            //実際にはクラスタIDのリストが入っている
                 /*  CustomIDSet sucClusterIDSet = this.getClusterSuc(checkCluster);
                   //もしcheckClusterがENDクラスタであれば，先行クラスタをクラスタリングする．
                   if (sucClusterIDSet.isEmpty()) {
                       //this.uexClusterList.remove(checkCluster.getClusterID());
                       TaskCluster pivotCluster = this.getClusterPred(checkCluster);
                       TaskCluster retCluster = this.clusteringClusterLB(pivotCluster, checkCluster, this.underThresholdClusterList);
                       continue;
                   }

                //後続クラスタたちのIDを取得する．
               Iterator<Long> sucClsIte = sucClusterIDSet.iterator();
               */
            while (!this.isAboveThreshold(checkCluster)) {
                long size = Constants.MAXValue;
                TaskCluster toCluster = null;
                //サイズが最小の後続クラスタを決定するためのループ
                Iterator<Long> clsIte = this.underThresholdClusterList.iterator();
                while (clsIte.hasNext()) {
                    Long id = clsIte.next();
                    if (id.longValue() != checkCluster.getClusterID().longValue()) {
                        TaskCluster sucCluster = this.retApl.findTaskCluster(id);
                        long value = this.getClusterInstruction(sucCluster);
                        if (value <= size) {
                            size = value;
                            toCluster = sucCluster;
                        }
                    } else {
                        continue;
                    }


                }
                //System.out.println("toのID:"+toCluster.getClusterID());
                // System.out.println();
                //そしてクラスタリング処理
                checkCluster = this.clusteringClusterLB(checkCluster, toCluster, this.underThresholdClusterList);


            }
            this.underThresholdClusterList.remove(checkCluster.getClusterID());
            preID = checkCluster.getClusterID();
        }

    }


    /**
     * @param fromCluster
     * @param toCluster
     * @return
     */
    public TaskCluster clusteringClusterLB(TaskCluster fromCluster, TaskCluster toCluster, CustomIDSet targetList) {
        //toClusterの全タスク集合を取得する．
        CustomIDSet IDSet = toCluster.getTaskSet();
        Iterator<Long> taskIte = IDSet.iterator();
        //とりあえずはunderリストから削除する．
        TaskCluster retCluster = null;

        this.unAssignedCPUs.add(toCluster.getCPU().getCpuID());
        toCluster.setCPU(null);
        this.retApl.removeTaskCluster(toCluster.getClusterID());


        //toCluster内のタスクたちに対するループ
        while (taskIte.hasNext()) {
            Long taskID = taskIte.next();
            //fromClusterへタスクを追加する．
            fromCluster.addTask(taskID);
            //toClusterにあったタスクの所属クラスタの変更
            AbstractTask task = this.retApl.findTaskByLastID(taskID);
            task.setClusterID(fromCluster.getClusterID());

            //this.retApl.updateTask(task);
        }

        //以降は，retAplへの更新処理
        //toTaskの反映
        // this.retApl.updateTask((AbstractTask) toTask.deepCopy());
        //fromClusterのIn/Outを更新
        this.updateInOut(fromCluster, toCluster);

        CustomIDSet topSet = this.getTopList(fromCluster);
        fromCluster.setTop_Set(topSet);
        Iterator<Long> topIte = topSet.iterator();
        fromCluster.getDestCheckedSet().getObjSet().clear();
        while (topIte.hasNext()) {
            Long topID = topIte.next();
            AbstractTask startTask = this.retApl.findTaskByLastID(topID);
            this.updateDestTaskList2(new CustomIDSet(), startTask, fromCluster.getClusterID());
        }

        //そしてretAplへの，fromClusterの反映
        //this.retApl.updateTaskCluster(fromCluster);
        //toClusterの削除
        this.retApl.removeTaskCluster(toCluster.getClusterID());
        //UEXからのtoClusterの削除
        targetList.remove(toCluster.getClusterID());


        //閾値を超えていればクラスタをUEXリストから削除する．
        if (this.isAboveThreshold(fromCluster)) {
            //UEXクラスタリストから削除する
            targetList.remove(fromCluster.getClusterID());
        }
        retCluster = fromCluster;

        return retCluster;

    }


    /**
     * topタスクの先行クラスタから，新pivotを取得します．
     *
     * @param pivot
     * @return
     */
    public TaskCluster getNewPivot(TaskCluster pivot) {
        //Topタスクを取得する．
        AbstractTask topTask = this.retApl.findTaskByLastID(pivot.getTopTaskID());
        //Topタスクの先行タスクたちを取得する．
        LinkedList<DataDependence> dpredList = topTask.getDpredList();
        Iterator<DataDependence> dpredIte = dpredList.iterator();
        long totalExecNum_Pivot = this.getClusterInstruction(pivot);
        long retTLValue = Constants.MAXValue;
        TaskCluster retCluster = null;

        Long tpredID = null;
        if (topTask.getTpred().isEmpty()) {
            return null;
        } else {
            //topタスクのTpredを取得する．
            tpredID = topTask.getTpred().get(1);
        }

        AbstractTask tpredTask = this.retApl.findTaskByLastID(tpredID);
        Long predClusterID = tpredTask.getClusterID();
        TaskCluster predCluster = this.retApl.findTaskCluster(predClusterID);
        //Tpredタスクが先行クラスタのBsucかつ先行クラスタがLinearかつpivot自体がlinearであれば，結果的に生成される
        //クラスタもLinearである．よって，この先行クラスタを新Pivotとする．
        if ((predCluster.getBsucTaskID().longValue() == tpredTask.getIDVector().get(1).longValue()) && (predCluster.isLinear()) &&
                (pivot.isLinear())) {
            return predCluster;

        }

        //topタスクの先行タスクたちに対するループ
        while (dpredIte.hasNext()) {
            DataDependence dd = dpredIte.next();
            //先行タスクを取得する.
            AbstractTask dpredTask = this.retApl.findTaskByLastID(dd.getFromID().get(1));

            if (dpredTask.getClusterID().longValue() != pivot.getClusterID().longValue()) {
                TaskCluster newPivot = this.retApl.findTaskCluster(dpredTask.getClusterID());
                //pivot内の，他のタスクたちの大まかなBL値を計算する．
                long TL_others = this.getTLValue(newPivot, pivot, dpredTask);
                //target(もともとのpivot)の，outタスクのTlevelの上限値を計算する．
                long TL_target = this.getTLValueOfTarget(newPivot, pivot);

                long TLValue = Math.max(TL_others, TL_target);
                if (TLValue <= retTLValue) {
                    retTLValue = TLValue;
                    retCluster = newPivot;
                }
            }
        }
        //System.out.println("NON-LINEARになった");
        retCluster.setLinear(false);
        return retCluster;
    }

    /**
     * pivotの先行・後続タスクから，
     *
     * @param
     * @return
     */
    public CustomIDSet getWSLSet(TaskCluster cluster) {
        CustomIDSet set = new CustomIDSet();

        AbstractTask downTask = this.retApl.findTaskByLastID(cluster.getBsucTaskID());
        set.add(downTask.getIDVector().get(1));
        while (downTask.getDsucList().isEmpty()) {
            downTask = this.retApl.findTaskByLastID(downTask.getBsuc().get(1));
            set.add(downTask.getIDVector().get(1));
        }


        return set;

    }

    public TaskCluster getMaxLevelFromAllClusters() {
        //   Iterator<TaskCluster> clusterIte = this.retApl.getTaskClusterList().values().iterator();
        Iterator<Long> clusterIte = this.freeClusterList.iterator();
        long maxLevel = 0;
        TaskCluster retCluster = null;

        while (clusterIte.hasNext()) {
            Long id = clusterIte.next();
            TaskCluster cls = this.retApl.findTaskCluster(id);
            //   TaskCluster cls = clusterIte.next();
            if (maxLevel <= cls.getTlevel() + cls.getBlevel()) {
                maxLevel = cls.getTlevel() + cls.getBlevel();
                retCluster = cls;
            }

        }
        return retCluster;
    }


    /**
     * メインのアルゴリズムです．
     * 1.  freeクラスタの中からWSLが最大のものをpivotとして選択する．
     * 2.  後続クラスタで，WSLを支配するものをtargetとする．
     * 3. クラスタリングする．そして優先度を更新する．
     * 4. 1で選ばれたpivotについて，下限値を超えるまで2と3を繰り返す．
     */
    public void mainProcess() {
        //未チェックタスクがなくなり，かつ各クラスタサイズが規定値以上である
        //ことが確認されるまでのループ
        long total_time = 0;
        while (!this.uexClusterList.isEmpty() && !this.unAssignedCPUs.isEmpty()) {
            if (this.freeClusterList.isEmpty()) {
                break;
            }
            HashMap<String, TaskCluster> clsMap = this.getClusterPair(null, null);
            if (clsMap == null) {
                continue;
            }
            CPU assignedCPU = null;

            TaskCluster pivot = clsMap.get("pivot");
            TaskCluster target = clsMap.get("target");
            if (!pivot.getCPU().isVirtual()) {
                assignedCPU = pivot.getCPU();
            } else {
                //次は，残りのCPUに対してΔWSL値を計算し，比較する．
                //この時，各CPUにはδ_opt値が設定されることになる．
                Iterator<Long> cpuIte = this.unAssignedCPUs.getList().iterator();

                long tmp_wsl = 100000000;
                long tmp_wsl2 = 0;
                assignedCPU = this.getAssignedCPU(pivot);

                long end = System.currentTimeMillis();
                //total_time += end-start;

            }


            //pivotクラスタに，CPUをセットする．
            pivot.setCPU(assignedCPU);
            this.sum_deltaopt += assignedCPU.getThresholdTime();


            //CPUのみ割り当てリストから，当該CPUを削除する．
            this.unAssignedCPUs.remove(assignedCPU.getCpuID());
            this.assignedMachineSet.add(assignedCPU.getMachineID());

            //この時点で，CPUは決定されている．
            //そして，delta_optを満たすまで，タスク集約を行う．
            pivot = this.clusteringCluster(pivot, target, assignedCPU);

            while (this.uexClusterList.contains(pivot.getClusterID())) {

                long org_size = this.calculateSumValue(pivot.getTaskSet());
                target = null;
                //targetを決める．
                HashMap<String, TaskCluster> retMap = this.getClusterPair(pivot, target);
                if (retMap == null) {

                } else {
                    pivot = retMap.get("pivot");
                    target = retMap.get("target");
                    if (pivot == null) {
                        break;
                    }

                    pivot = this.clusteringCluster(pivot, target, assignedCPU);


                    if (this.isAboveThreshold(pivot)) {
                        Long endID = new Long(this.retApl.getTaskList().size());

                        //リニアかつENDタスクを含まない場合
                        if ((pivot.isLinear()) && (!pivot.getTaskSet().contains(endID))) {
                            //bottomタスクがwslのタスク集合に含まれていれば，そのまま後続タスクをクラスタリングする．
                            //WSLを支配するタスク集合を生成する．
                            TaskCluster wslCluster = this.getMaxLevelFromAllClusters();
                            //もしwslを支配するクラスタがpivotのままであれば，そのままクラスタリングする．
                            if (pivot.getClusterID().longValue() == wslCluster.getClusterID().longValue()) {
                                continue;
                                //   System.out.println("きたよ");
                            } else {
                                CustomIDSet wslSet = this.getWSLSet(wslCluster);
                                if (wslSet.contains(pivot.getBottomSet().getList().get(0))) {
                                    continue;
                                } else {
                                    break;
                                }
                                //他のfreeクラスタが支配するのであれば，そのタスク集合を取得する．
                                // System.out.println("他になったよ");
                                //WSlClusterのTpredの先行タスク，Bsucタスクの後続タスクを集合にセットする．

                                //  break;
                            }

                            //リニアまたはENDタスクを含む場合
                            // System.out.println("test");

                        } else {
                            this.freeClusterList.remove(pivot.getClusterID());
                            //pivotをEXへ入れる(つまり，UEXからpivotを削除する）
                            this.uexClusterList.remove(pivot.getClusterID());
                            //Outタスクの後続タスクの入力辺を"Checked"とする．
                            CustomIDSet pOutSet = pivot.getOut_Set();
                            Iterator<Long> pOutIte = pOutSet.iterator();
                            CustomIDSet clsSet = new CustomIDSet();
                            this.underThresholdClusterList.remove(pivot.getClusterID());

                            //Outタスクに対するループ
                            while (pOutIte.hasNext()) {
                                Long oID = pOutIte.next();
                                AbstractTask oTask = this.retApl.findTaskByLastID(oID);
                                //Outタスクの後続タスク集合を取得する．
                                LinkedList<DataDependence> dsucList = oTask.getDsucList();
                                Iterator<DataDependence> dsucIte = dsucList.iterator();
                                //Outタスクの，後続タスクたちに対するループ
                                while (dsucIte.hasNext()) {
                                    DataDependence dd = dsucIte.next();
                                    AbstractTask sucTask = this.retApl.findTaskByLastID(dd.getToID().get(1));
                                    if (sucTask.getClusterID().longValue() != pivot.getClusterID().longValue()) {
                                        //sucTaskの入力辺に"Checked"フラグをつける．
                                        sucTask.setCheckedFlgToDpred(oTask.getIDVector(), sucTask.getIDVector(), true);
                                        clsSet.add(sucTask.getClusterID());
                                    }
                                }
                            }

                            Iterator<Long> clsIte = clsSet.iterator();
                            while (clsIte.hasNext()) {
                                Long clusterid = clsIte.next();
                                TaskCluster clster = this.retApl.findTaskCluster(clusterid);
                                if (this.isAllInEdgeChecked(clster)) {
                                    if (!this.isAboveThreshold(clster)) {
                                        //もしクラスタのInタスクがすべてCheckeであれば，そのクラスタをFreeListへ入れる．
                                        this.addFreeClusterList(clusterid);
                                    }

                                }
                            }

                        }
                    } else {
                        //pivotがstartかつENDクラスタである場合，
                        if ((this.isStartCluster(pivot)) && (pivot.getOut_Set().isEmpty())) {
                            pivot = this.checkEXSingleCluster(pivot);

                        } else {
                            //以降，pivotのサイズ<δである場合の処理

                            if (this.isAllInEdgeChecked(pivot)) {

                                // this.freeClusterList.add(pivot.getClusterID());
                            }

                        }

                    }

                }

                //long size = this.calculateSumValue(pivot.getTaskSet());

            }

        }


    }

    /**
     * @param cluster
     * @return
     */
    public boolean isAboveThreshold(TaskCluster cluster) {
        CustomIDSet IDSet = cluster.getTaskSet();
        Iterator<Long> ite = IDSet.iterator();
        long value = 0;

        while (ite.hasNext()) {
            Long id = ite.next();
            AbstractTask task = this.retApl.findTaskByLastID(id);
            value += this.getInstrunction(task);
        }
        CPU cpu = cluster.getCPU();
        double execTime = Calc.getRoundedValue((double) value / cpu.getSpeed());
        //割り当てられているCPUが，仮想CPUでない，かつクラスタ実行時間がr値以上である場合はtrue
        return (execTime >= cpu.getThresholdTime()) && (!cpu.isVirtual());

    }


    public void prepare() {
        //アプリから，タスクリストを取得
        Hashtable<Long, AbstractTask> tasklist = this.retApl.getTaskList();
        Collection<AbstractTask> col = tasklist.values();
        Iterator<AbstractTask> ite = col.iterator();

        long start = System.currentTimeMillis();
        CustomIDSet startSet = new CustomIDSet();


        //CPUを全て未割り当て状態とする．
        Iterator<CPU> umIte = this.env.getCpuList().values().iterator();

        //マシンに対するループ
        //各マシンを未割り当てCPUリストへ追加させる．
        while (umIte.hasNext()) {
            CPU cpu = umIte.next();
            if (this.maxLink <= cpu.getBw()) {
                this.maxLink = cpu.getBw();
            }
            this.unAssignedCPUs.add(cpu.getCpuID());
        }

        //仮想CPUと仮想リンクを作り，各タスクはそれぞれ仮想CPUへ割り当てられているものとする．
        // int vcpu_num = this.env.getCpuList().size();
        int vcpu_num = this.retApl.getTaskList().size();
        this.virtualEnv = new Environment();

        this.retApl.getTaskList().values().iterator();


        /*   for(int i=0;i<vcpu_num; i++){
               CPU vCPU = new CPU(new Long(i+1), this.maxSpeed, new Vector<Long>(), new Vector<Long>());
              vCPU.setVirtual(true);
           }
        */
        Hashtable vcpuList = new Hashtable<Long, CPU>();
        //CPUリストをセットする．
        this.virtualEnv.setCpuList(vcpuList);
        long[][] vlinks = new long[vcpu_num][vcpu_num];

        //仮想リンク情報を作成する．
        for (int i = 0; i < vcpu_num; i++) {
            for (int j = 0; j < vcpu_num; j++) {
                // vlinks[i][j] = this.maxBW;
                vlinks[i][j] = this.maxLink;
            }
        }
        //仮想リンク集合をセットする．
        this.virtualEnv.setLinkMatrix(vlinks);
        double cpuSum = 0.0;
        double taskMaxG = 0.0;
        Iterator<CPU> cpuIte = this.env.getCpuList().values().iterator();
        while (cpuIte.hasNext()) {
            CPU cpu = cpuIte.next();
            //cpuSum += Calc.getRoundedValue((double)1/cpu.getSpeed());
            cpuSum += Calc.getRoundedValue(cpu.getSpeed());
            if (this.maxSpeed <= cpu.getSpeed()) {
                this.maxSpeed = cpu.getSpeed();
            }
        }
        long taskSum = 0;
        long cpuNum = this.env.getCpuList().size();
        double ave_speed = Calc.getRoundedValue(cpuSum / cpuNum);


        //タスククラスタの生成
        //各タスクに対するループ
        while (ite.hasNext()) {
            AbstractTask task = ite.next();
            //CPU作成
            long speed = 0;

            if (this.virtualmode == 0) {
                speed = this.maxSpeed;

            } else {
                speed = (long) ave_speed;
            }
            //CPU vCPU = new CPU(task.getIDVector().get(1), this.maxSpeed, new Vector<Long>(), new Vector<Long>());
            CPU vCPU = new CPU(new Long(-1), speed, new Vector<Long>(), new Vector<Long>());
            vCPU.setVirtual(true);
            //まずは自分自身をDestへ追加する．
            task.addDestTask(task.getIDVector().get(1));

            //タスクをクラスタへ入れる．
            TaskCluster cluster = new TaskCluster(task.getIDVector().get(1));
            //一つのタスクしか入らないので，当然Linearである．
            cluster.setLinear(true);
            //クラスタに，CPUをセットする．
            //CPU tmpCPU = this.virtualEnv.getCPU(task.getIDVector().get(1));
            cluster.setCPU(vCPU);
            //CPU側でも，割り当て済みクラスタIDをセットする．
            vCPU.setTaskClusterID(task.getIDVector().get(1));
            this.virtualEnv.getCpuList().put(task.getIDVector().get(1), vCPU);

            task.setClusterID(cluster.getClusterID());
            cluster.addTask(task.getIDVector().get(1));

            // タスククラスタに対して，各種情報をセットする．
            /**このときは，各クラスタには一つのみのタスクが入るため，
             * 以下のような処理が明示的に可能である．
             */
            //ここで，top/outタスクは，自分自身のみをセットしておく．
            cluster.setBsucTaskID(task.getIDVector().get(1));
            cluster.getBottomSet().add(task.getIDVector().get(1));
            cluster.addIn_Set(task.getIDVector().get(1));
            cluster.setTopTaskID(task.getIDVector().get(1));
            CustomIDSet topSet = new CustomIDSet();
            topSet.add(task.getIDVector().get(1));
            cluster.setTop_Set(topSet);


            //もし後続タスクがあれば，自身のタスクをOutセットへ入れる
            if (!task.getDsucList().isEmpty()) {
                cluster.addOut_Set(task.getIDVector().get(1));
            }


            //先行タスクがなけｒば，スタートセットに入れる．
            if (task.getDpredList().isEmpty()) {
                startSet.add(task.getIDVector().get(1));
            }
            //タスク自身に，所属するクラスタIDをセットする．
            //このとき，クラスタIDをタスクIDとしてセットしておく．
            task.setClusterID(task.getIDVector().get(1));
            //クラスタ自体をDAGへ反映
            Long clusterID = this.retApl.addTaskCluster(cluster);

            //この時点で，UEXを格納しておく．
            this.uexClusterList.add(clusterID);
        }

        //まずは，CPwの値を決める．
        this.retApl.setInitial_wcp(this.getMaxInitialWSL());

        //そして，そのマシンに対する割り当てサイズ（タスク実行時間の和）
        //を算出する．
        //   long delta_opt = this.calcDelta_opt(initialCPU);

        long endID = this.retApl.getTaskList().size();
        AbstractTask endTask = this.retApl.findTaskByLastID(new Long(endID));
        //Tlevelのセットをする．
        this.getMaxTlevel(endTask, new CustomIDSet());
        //Blevelのセットをする．
        Iterator<Long> startIDIte = startSet.iterator();
        CustomIDSet idSet = new CustomIDSet();

        while (startIDIte.hasNext()) {
            Long sID = startIDIte.next();
            AbstractTask startTask = this.retApl.findTaskByLastID(sID);
            this.getMaxBlevel(startTask, idSet);
        }

        AbstractTask tmpTask = endTask;
        while (!tmpTask.getDpredList().isEmpty()) {
            this.wslTaskSet.add(tmpTask.getIDVector().get(1));
            tmpTask = this.retApl.findTaskByLastID(tmpTask.getTpred().get(1));

        }

        //   this.calculateInitialTlevel(endTask, initialCPU, false);
        this.retApl.setStartTaskSet(startSet);

        Hashtable<Long, AbstractTask> taskTable = this.retApl.getTaskList();
        Collection<AbstractTask> taskCollection = taskTable.values();
        Iterator<AbstractTask> ite2 = taskCollection.iterator();

        //各タスククラスタへのレベル反映処理
        long start1 = System.currentTimeMillis();
        long cLevel = 0;
        //各タスクに対するループ処理
        while (ite2.hasNext()) {
            AbstractTask task = ite2.next();
            //もしタスククラスタがSTARTノードであれば，この時点でFreeリストへ入れる．
            TaskCluster cls = this.retApl.findTaskCluster(task.getClusterID());
            if (task.getDpredList().isEmpty()) {
                this.freeClusterList.add(cls.getClusterID());
            }
            //初期状態では，クラスタのレベルはタスクのレベルと同じである．
            cls.setTlevel(task.getTlevel());
            cls.setBlevel(task.getBlevel());
            long value = cls.getTlevel() + cls.getBlevel();
            if (cLevel <= value) {
                cLevel = value;
            }

        }

        this.retApl.setMinCriticalPath(cLevel);
        this.level = cLevel;
        long end1 = System.currentTimeMillis();
        //System.out.println("レベル反映時間: "+(end1-start));

    }


    /**
     * pivot, targetとのクラスタリング処理です．
     * <p/>
     * 1. 2つのクラスタの集約, Top, In/Outの更新, DAGからのtargetの削除
     * あとは，当該クラスタをプロセッサへ割り当てる．
     * 2. マージ後の一つのクラスタをUE/UEXに入れるかどうかの判断．
     * 3. TL, BL, Tpred, Bsuc, tlevel, blevel, Tpred, Bsucの更新をする．
     * <p/>
     * <p/>
     * （もし一定値以上であればUXに入れる）
     *
     * @param pivot
     * @param target
     * @return
     */
    public TaskCluster clusteringCluster(TaskCluster pivot, TaskCluster target, CPU cpu) {
        if (pivot == null) {
//            System.out.println("nullだよ");
        }
        Long topTaskID = pivot.getTopTaskID();

        //CPUに，タスククラスタIDをセットする．
        cpu.setTaskClusterID(pivot.getClusterID());

        if (pivot.getClusterID().longValue() > target.getClusterID().longValue()) {
            return this.clusteringCluster(target, pivot, cpu);
        }

        //targetの全タスク集合を取得する．
        CustomIDSet IDSet = target.getTaskSet();
        Iterator<Long> taskIte = IDSet.iterator();

        //target内の全タスクの所属クラスタの変更処理
        while (taskIte.hasNext()) {
            Long taskID = taskIte.next();
            //fromClusterへタスクを追加する．
            pivot.addTask(taskID);
            //toClusterにあったタスクの所属クラスタの変更
            AbstractTask task = this.retApl.findTaskByLastID(taskID);
            task.setClusterID(pivot.getClusterID());

        }

        //もしtargetに実際のCPUが割り当てられていれば、戻す。
        if (!target.getCPU().isVirtual()) {
            this.unAssignedCPUs.add(target.getCPU().getCpuID());
        }


        //pivotのIn/Outを更新
        //Topタスク→Topじゃなくなるかも(どちらかがTopでのこり，他方がTopじゃなくなる）
        //Outタスク→Outじゃなくなるかも（すくなくともTopにはならない）
        //それ以外→それ以外のまま
        //まずはclusterのtopを，pivot側にする．というかもともとそうなっているので無視
        //だけど，outSetだけは更新しなければならない．
        pivot = this.updateOutSet(pivot, target);
        //InSetを更新する（後のレベル値の更新のため）
        pivot = this.updateInSet(pivot, target);

        pivot = this.updateTopSet(pivot, target);

        Iterator<Long> topIte = pivot.getTop_Set().iterator();
        Long topID = null;
        long topTlevel = 0;
        while (topIte.hasNext()) {
            Long id = topIte.next();
            AbstractTask topTask = this.retApl.findTaskByLastID(id);
            if (topTlevel <= topTask.getTlevel()) {
                topTlevel = topTask.getTlevel();
                topID = topTask.getIDVector().get(1);
            }

        }
        pivot.setTopTaskID(topID);
        //各タスクの，destSetの更新をする．

        CustomIDSet allSet = pivot.getTaskSet();
        Iterator<Long> idIte = allSet.iterator();
        CustomIDSet startIDs = new CustomIDSet();
        //まずはTopタスクのIDを追加しておく．

//        AbstractTask startTask = this.retApl.findTaskByLastID(pivot.getTopTaskID());
        pivot.getDestCheckedSet().getObjSet().clear();
        Iterator<Long> topIte2 = pivot.getTop_Set().iterator();
        while (topIte2.hasNext()) {
            Long id = topIte2.next();
            AbstractTask startTask2 = this.retApl.findTaskByLastID(id);
            this.updateDestTaskList2(new CustomIDSet(), startTask2, pivot.getClusterID());

        }

        //targetクラスタをDAGから削除する．
        this.retApl.removeTaskCluster(target.getClusterID());
        //targetクラスタをUEXから削除する．
        this.uexClusterList.remove(target.getClusterID());
        //targetクラスタをFreeから削除する．
        this.freeClusterList.remove(target.getClusterID());

        this.underThresholdClusterList.remove(target.getClusterID());

        if ((pivot.getTop_Set().getList().size() == 1) && (pivot.getBottomSet().getList().size() == 1)) {
            pivot.setLinear(true);
        } else {
            pivot.setLinear(false);
        }

        //あとは，新pivotがEXなのかUEXなのかの判断をする．

        // BL, Tpred, Bsuc, tlevel, blevel, Tpred, Bsucの更新をする．
        // TL(pivot)は不変なので，考慮する必要はない．
        if (this.isUpdateAll) {
            this.calcPostLevel();
        } else {
            this.updateLevelFromPivot(pivot);
        }

        return pivot;

    }

    public void setUpdateAll(boolean value) {
        this.isUpdateAll = value;
    }

    /**
     * 指定クラスタのTlevel，及び内部タスクのtlevelを更新します．
     *
     * @param pivot
     * @return
     */
    public long updateTlevelofCluster(TaskCluster pivot) {
        Iterator<Long> topIte = pivot.getTop_Set().iterator();
        CPU pCPU = pivot.getCPU();
        long topTlevel = 0;
        AbstractTask tpredTask = this.retApl.findTaskByLastID(pivot.getTopTaskID());
        if (pivot.getTop_Set().getList().size() == 1) {
            AbstractTask task = this.retApl.findTaskByLastID(pivot.getTopTaskID());
            if (task.getDpredList().isEmpty()) {
                return 0;
            }
        }
        //Topタスクレベル更新と，pivotのTlevel更新
        while (topIte.hasNext()) {
            Long topID = topIte.next();

            AbstractTask topTask = this.retApl.findTaskByLastID(topID);
            long oneTopTlevel = 0;
            //topタスクの先行タスクを見る．
            Iterator<DataDependence> dpredIte = topTask.getDpredList().iterator();
            while (dpredIte.hasNext()) {
                DataDependence dpred = dpredIte.next();
                AbstractTask predTask = this.retApl.findTaskByLastID(dpred.getFromID().get(1));
                TaskCluster predCluster = this.retApl.findTaskCluster(predTask.getClusterID());
                long tmpTopTlevel = predTask.getTlevel() + (predTask.getMaxWeight() / predCluster.getCPU().getSpeed()) +
                        this.getNWTime(predTask.getIDVector().get(1), topTask.getIDVector().get(1), dpred.getMaxDataSize(),
                                this.env.getNWLink(predCluster.getCPU().getCpuID().longValue(), pCPU.getCpuID().longValue()));
                long nwtime = this.getNWTime(predTask.getIDVector().get(1), topTask.getIDVector().get(1), dpred.getMaxDataSize(),
                        this.env.getNWLink(predCluster.getCPU().getCpuID().longValue(), pCPU.getCpuID().longValue()));
                //  System.out.println("tmpTopTlevel:"+tmpTopTlevel);


                if (oneTopTlevel <= tmpTopTlevel) {
                    oneTopTlevel = tmpTopTlevel;
                    topTask.setTpred(predTask.getIDVector());
                }
                if (tmpTopTlevel >= topTlevel) {
                    topTlevel = tmpTopTlevel;
                    tpredTask = topTask;
                }
            }
        }
        //topタスクとpivotのtlevelの更新
        pivot.setTopTaskID(tpredTask.getIDVector().get(1));
        pivot.setTlevel(topTlevel);

        //次に，top以外のタスクin pivotのtlevel値の更新
        Iterator<Long> taskIte = pivot.getTaskSet().iterator();
        while (taskIte.hasNext()) {
            Long id = taskIte.next();
            AbstractTask task = this.retApl.findTaskByLastID(id);
            if (pivot.getTop_Set().contains(id)) {
                continue;
            } else {
                //top以外のタスクであれば，tlevel計算対象となる．
                //CustomIDSet destSet = this.getDestTaskList(new CustomIDSet(),task,task.getClusterID());
                CustomIDSet destSet = task.getDestTaskSet();
                //もし先行タスクが同クラスタであれば、同クラスタ用のTlevel計算処理に入る
                long totalExecTime = this.getClusterInstruction(pivot) / pCPU.getSpeed();
                long destValue = this.calculateSumValue(destSet);
                // System.out.println("SValue:"+(totalExecTime - (destValue / pCPU.getSpeed())));
                //tlevel値の計算
                long value = pivot.getTlevel() + totalExecTime - (destValue / pCPU.getSpeed());
                task.setTlevel(value);
                task.setSValue(totalExecTime - (destValue / pCPU.getSpeed()));
            }

        }

        return pivot.getTlevel();

    }

    public long updateBlevelOfCluster(TaskCluster pivot) {
        CustomIDSet exSet = new CustomIDSet();
        Iterator<Long> bottomIte = pivot.getBottomSet().iterator();
        while (bottomIte.hasNext()) {
            long b_newblevel = 0;
            long b_tmpblevel = 0;
            Vector<Long> b_bsucTaskID = null;

            Long bID = bottomIte.next();
            exSet.add(bID);

            AbstractTask bottomTask = this.retApl.findTaskByLastID(bID);
            Iterator<DataDependence> bdsucIte = bottomTask.getDsucList().iterator();
            while (bdsucIte.hasNext()) {
                DataDependence dsuc = bdsucIte.next();
                AbstractTask sucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));
                TaskCluster sucCluster = this.retApl.findTaskCluster(sucTask.getClusterID());
                long nw_time = this.getNWTime(bottomTask.getIDVector().get(1), sucTask.getIDVector().get(1), dsuc.getMaxDataSize(),
                        this.env.getNWLink(pivot.getCPU().getCpuID().longValue(), sucCluster.getCPU().getCpuID().longValue()));
                b_tmpblevel = bottomTask.getMaxWeight() / pivot.getCPU().getSpeed() + nw_time + sucTask.getBlevel();
                if (b_tmpblevel >= b_newblevel) {
                    b_newblevel = b_tmpblevel;
                    b_bsucTaskID = sucTask.getIDVector();
                    bottomTask.setBsuc(b_bsucTaskID);
                }
            }
        }

        long newBL = 0;
        Long clusterBsuc = null;
        long tmp_newBL = 0;

        Iterator<Long> outIte2 = pivot.getOut_Set().iterator();
        while (outIte2.hasNext()) {
            long newblevel = 0;
            long tmpblevel = 0;
            Vector<Long> bsucTaskID = null;

            Long id = outIte2.next();
            AbstractTask outTask = this.retApl.findTaskByLastID(id);
            Iterator<DataDependence> dsucIte2 = outTask.getDsucList().iterator();
            if (exSet.contains(outTask.getIDVector().get(1))) {
                tmp_newBL = outTask.getSValue() + outTask.getBlevel();
                if (tmp_newBL >= newBL) {
                    newBL = tmp_newBL;
                    clusterBsuc = outTask.getIDVector().get(1);
                }
            } else {
                while (dsucIte2.hasNext()) {
                    DataDependence dsuc = dsucIte2.next();
                    AbstractTask outSucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));
                    long nw_time = 0;
                    if (outSucTask.getClusterID().longValue() == pivot.getClusterID().longValue()) {
                        nw_time = 0;
                        if (exSet.contains(dsuc.getToID().get(1))) {
                            tmpblevel = outTask.getMaxWeight() / pivot.getCPU().getSpeed() + nw_time + outSucTask.getBlevel();
                        } else {
                            //pivot内タスクで，かつ未チェックなものがあれば，bottomに行くか他クラスタ内タスクまで走査する．
                            tmpblevel = this.calcBlevel(pivot, outSucTask, exSet);
                        }

                    } else {
                        TaskCluster sucCluster = this.retApl.findTaskCluster(outSucTask.getClusterID());
                        nw_time = this.getNWTime(outTask.getIDVector().get(1), outSucTask.getIDVector().get(1), dsuc.getMaxDataSize(),
                                this.env.getNWLink(pivot.getCPU().getCpuID().longValue(), sucCluster.getCPU().getCpuID().longValue()));
                        tmpblevel = outTask.getMaxWeight() / pivot.getCPU().getSpeed() + nw_time + outSucTask.getBlevel();

                    }
                    //既に新blevelが更新済みなタスクにぶち当たれば，それを取得する．
                    if (tmpblevel >= newblevel) {
                        newblevel = tmpblevel;
                        bsucTaskID = outSucTask.getIDVector();
                        outTask.setBsuc(bsucTaskID);
                    }

                }

                exSet.add(outTask.getIDVector().get(1));

            }
            tmp_newBL = outTask.getSValue() + outTask.getBlevel();
            if (tmp_newBL >= newBL) {
                newBL = tmp_newBL;
                clusterBsuc = outTask.getIDVector().get(1);
            }

        }
        pivot.setBsucTaskID(clusterBsuc);
        pivot.setBlevel(newBL);

        return pivot.getBlevel();
    }

    /**
     * @param task
     * @param set
     * @return
     */
    public long calcBlevel(TaskCluster pivot, AbstractTask task, CustomIDSet set) {
        TaskCluster cluster = this.retApl.findTaskCluster(task.getClusterID());
        long tmpBlevel = 0;
        long newBlevel = 0;
        Vector<Long> bsucID = null;
        //taskがpivotに属していることを前提としている．

        //所属クラスタがpivot以外であれば，それ以上の走査は行わない．
        Iterator<DataDependence> dsucIte = task.getDsucList().iterator();
        while (dsucIte.hasNext()) {
            DataDependence dsuc = dsucIte.next();
            AbstractTask sucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));
            TaskCluster sucCluster = this.retApl.findTaskCluster(sucTask.getClusterID());
            if (set.contains(dsuc.getToID().get(1)) || (sucTask.getClusterID().longValue() != pivot.getClusterID().longValue())) {
                long nw_time = this.getNWTime(task.getIDVector().get(1), sucTask.getIDVector().get(1), dsuc.getMaxDataSize(),
                        this.env.getNWLink(pivot.getCPU().getCpuID().longValue(), sucCluster.getCPU().getCpuID().longValue()));
                tmpBlevel = task.getMaxWeight() / pivot.getCPU().getSpeed() + nw_time + sucTask.getBlevel();

            } else {
                tmpBlevel = this.calcBlevel(pivot, sucTask, set);
            }
            if (tmpBlevel >= newBlevel) {
                newBlevel = tmpBlevel;
                bsucID = sucTask.getIDVector();
                task.setBsuc(bsucID);
            }

        }
        set.add(task.getIDVector().get(1));
        return newBlevel;

    }

    /**
     * @param pivot
     */
    public void updateLevelFromPivot(TaskCluster pivot) {
        CustomIDSet outSet = new CustomIDSet();
        CustomIDSet inSet = new CustomIDSet();
        //pivotのTlevelと各タスクのtlevelを更新する．
        // System.out.println("前: "+(pivot.getTlevel()));

        this.updateTlevelofCluster(pivot);
        //pivotのBlevelと各タスクのblevelを更新する．
        updateBlevelOfCluster(pivot);

        //   System.out.println("後: "+(pivot.getTlevel()));
        //System.out.println();


        //outSet(outタスクの後続タスクでかつpivot以外のクラスタのタスク)のtlevelを更新する．
        Iterator<Long> outIte = pivot.getOut_Set().iterator();
        Long dominatingBLTaskID = null;

        while (outIte.hasNext()) {
            Long id = outIte.next();
            AbstractTask outTask = this.retApl.findTaskByLastID(id);
            Iterator<DataDependence> dsucIte = outTask.getDsucList().iterator();
            while (dsucIte.hasNext()) {
                DataDependence dsuc = dsucIte.next();
                AbstractTask outSucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));

                if (outSucTask.getClusterID().longValue() != pivot.getClusterID().longValue()) {
                    //後続タスクが別クラスタの場合は，outTaskClusterをoutSetへ入れる．
                    //これは，後のTlevel及びtlevel更新対象となる．
                    outSet.add(outSucTask.getClusterID());
                }
            }
        }

        Iterator<Long> inIte = pivot.getIn_Set().iterator();
        while (inIte.hasNext()) {
            Long id = inIte.next();
            AbstractTask inTask = this.retApl.findTaskByLastID(id);
            Iterator<DataDependence> dpredIte = inTask.getDpredList().iterator();
            while (dpredIte.hasNext()) {
                DataDependence dpred = dpredIte.next();
                AbstractTask predTask = this.retApl.findTaskByLastID(dpred.getFromID().get(1));
                if (predTask.getClusterID().longValue() == pivot.getClusterID().longValue()) {

                } else {
                    inSet.add(predTask.getClusterID());
                }
            }
        }
        //outSetに対し，tlevelの更新を行う．
        Iterator<Long> outClusterIte = outSet.iterator();
        while (outClusterIte.hasNext()) {
            Long outID = outClusterIte.next();
            TaskCluster outCluster = this.retApl.findTaskCluster(outID);
            this.updateTlevelofCluster(outCluster);
        }

        //inSetに対し，blevelの更新を行う．
        Iterator<Long> inClusterIte = inSet.iterator();
        while (inClusterIte.hasNext()) {
            Long inID = inClusterIte.next();
            TaskCluster inCluster = this.retApl.findTaskCluster(inID);
            this.updateBlevelOfCluster(inCluster);
        }


    }


    /**
     * Topタスクのtlevelを計算します．
     * つまり，先行タスクがすべて別クラスタに存在することを前提とした計算をします（N/W時間がある）．
     *
     * @param topTask
     * @return
     */
    public AbstractTask calculsteTlevelForTopTask(AbstractTask topTask) {
        LinkedList<DataDependence> dpredList = topTask.getDpredList();
        Iterator<DataDependence> dpredIte = dpredList.iterator();
        long tmpValue = 0;
        AbstractTask tpredTask = new AbstractTask();
        TaskCluster topCluster = this.retApl.findTaskCluster(topTask.getClusterID());
        CPU topCPU = topCluster.getCPU();


        //topタスクの先行タスクに対するループ
        while (dpredIte.hasNext()) {
            DataDependence dpred = dpredIte.next();
            //先行タスクを取得する．
            AbstractTask predTask = this.retApl.findTaskByLastID(dpred.getFromID().get(1));
            TaskCluster predCluster = this.retApl.findTaskCluster(predTask.getClusterID());
            CPU predCPU = predCluster.getCPU();
            long nw_time = 0;
            if (predCPU.getMachineID() == topCPU.getCpuID()) {

            } else {
                nw_time = this.env.getSetupTime() + (dpred.getMaxDataSize() / this.env.getNWLink(predCPU.getCpuID(), topCPU.getCpuID()));
            }
            //データ到着時刻を取得する．
            long value = predTask.getTlevel() + nw_time;
            //値の更新
            if (tmpValue <= value) {
                tmpValue = value;
                tpredTask = predTask;
            }
        }
        //最終的に決まった値を反映する．
        topTask.setTlevel(tmpValue);
        topTask.setTpred(tpredTask.getIDVector());
        //topClusterのTopタスクを更新する．
        Iterator<Long> topIte = topCluster.getTop_Set().iterator();
        long tlevel = 0;
        Long retTopID = null;
        while (topIte.hasNext()) {
            Long topID = topIte.next();
            AbstractTask tTask = this.retApl.findTaskByLastID(topID);
            long tmpLevel = tTask.getTlevel();
            if (tmpLevel > tlevel) {
                tlevel = tmpLevel;
                retTopID = tTask.getIDVector().get(1);
            }
            topCluster.setTopTaskID(retTopID);


        }


        return topTask;
    }

    /**
     * クラスタのTlevel値をセットします．
     * クラスタのTlevel = Max{topタスクのtlevel}であるので，
     * - 下からトレースして，当該クラスタのtopタスクたちを見る．
     * - そして，各topタスクに対して，その先行クラスタへと順次tlevelを更新していく．
     */
    public void calcClusterTlevel() {
        //ENDタスクを取得する．
        AbstractTask endTask = this.retApl.findTaskByLastID(new Long(this.retApl.getTaskList().size()));
        Long endClusterID = endTask.getClusterID();
        TaskCluster endCluster = this.retApl.findTaskCluster(endClusterID);
        // this.setCyclicTopTask(endCluster, new CustomIDSet());
        //taskSetを初期化する．
        // this.IDSet = new CustomIDSet();
        //ENDクラスタを始点として，クラスタのTlevelを設定していく．
        this.configureTlevelofCluster(endCluster, new CustomIDSet(), false);
        long totalSum = this.getClusterInstruction(endCluster);
        long destValue = this.calculateSumValue(endTask.getDestTaskSet());
        CPU endCPU = endCluster.getCPU();
        //ENDタスクのtlevelを決める．
        long tlevel = endCluster.getTlevel() + (totalSum - destValue) / endCPU.getSpeed();
        endTask.setTlevel(tlevel);

    }


    /**
     * クラスタのTlevel値を計算する．これは，当該クラスタの各Topタスクにおける，先行タスクの
     * tlevel値が分かっていないと計算できない．よって，先行タスクが属するクラスタ（つまり先行クラスタ）が
     * 未チェックであれば，再帰的にこのメソッドがCallされることになる．
     *
     * @param cluster
     * @param tmpClusterSet
     * @param isSecond
     * @return
     */
    public CyclicBean configureTlevelofCluster(
            TaskCluster cluster,
            CustomIDSet tmpClusterSet,
            boolean isSecond) {

        CustomIDSet topSet = cluster.getTop_Set();
        Iterator<Long> topIte = topSet.iterator();

        //以降は，未チェッククラスタのTlevel値を取得する．
        long tmpTlevel = 0;
        Long topTaskID = null;

        CyclicBean bean = new CyclicBean();
        tmpClusterSet.add(cluster.getClusterID());
        //Top候補に対するループ
        while (topIte.hasNext()) {
            Long topID = topIte.next();
            //Long topID = cluster.getTopTaskID();

            //topタスクを取得する．
            AbstractTask topTask = this.retApl.findTaskByLastID(topID);

            //topタスクの先行タスクたちを取得する．
            LinkedList<DataDependence> dpredList = topTask.getDpredList();
            Iterator<DataDependence> dpredIte = dpredList.iterator();

            //一つのtop候補のtlevel値
            Vector<Long> tpredID = null;
            long tmpTlevel2 = 0;

            if (dpredList.isEmpty()) {
                tmpTlevel = 0;
                tmpTlevel2 = 0;
                tpredID = null;
            }

            //topタスクの先行タスクたちに対するループ
            while (dpredIte.hasNext()) {
                DataDependence dd = dpredIte.next();
                Long dpredID = dd.getFromID().get(1);
                AbstractTask dpredTask = this.retApl.findTaskByLastID(dpredID);

                long cTlevel = 0;
                TaskCluster predCluster = this.retApl.findTaskCluster(dpredTask.getClusterID());

                //先行クラスタが既に設定済みのクラスタであれば，クラスタのtlevelをそのままリターン
                if (this.checkedClusterSet.contains(predCluster.getClusterID())) {
                    cTlevel = predCluster.getTlevel();
                } else {
                    //先行クラスタのtlevelが未設定の場合
                    //戻ってしまった場合はエラー値を返す．
                    if (tmpClusterSet.contains(predCluster.getClusterID())) {
                        bean.setTlevel(-1);
                        bean.getCyclicClusterSet().add(cluster.getClusterID());
                        DataDependence dataDD = topTask.findDDFromDpredList(dpredTask.getIDVector(), topTask.getIDVector());
                        CyclicMap cyclicMap = new CyclicMap(dpredTask.getIDVector().get(1),
                                topTask.getIDVector().get(1),
                                dataDD.getMaxDataSize());
                        long orgWeight = topTask.getMaxWeight();
                        long newValue = dd.getMaxDataSize() + dd.getMaxDataSize();
                        // topTask.setMaxWeight(orgWeight + newValue);

                        this.cyclicClusterSet.add(cluster.getClusterID());

                        String key = this.getKey(dpredTask, topTask);
                        this.cyclicLIst.put(key, cyclicMap);

                        topTask.getCyclicPredTaskSet().add(dpredTask.getIDVector().get(1));
                        return bean;
                    }

                    //先行クラスタのtlevel値を取得する．
                    CyclicBean retBean = this.configureTlevelofCluster(predCluster, tmpClusterSet, isSecond);
                    //当該top候補タスクからのトレースでcyclicが生じた場合は，当該topタスクをtopSetから削除する．
                    if (retBean.getTlevel() == -1) {
                        CustomIDSet ctopset = cluster.getCyclicTopSet();
                        ctopset.add(topID);
                        bean.getCyclicClusterSet().getObjSet().addAll(retBean.getCyclicClusterSet().getList());
                        bean.getCyclicClusterSet().add(cluster.getClusterID());
                        cluster.getCyclicClusterSet().getObjSet().addAll(bean.getCyclicClusterSet().getList());
                        this.cyclicClusterSet.add(cluster.getClusterID());
                        DataDependence dataDD = topTask.findDDFromDpredList(dpredTask.getIDVector(), topTask.getIDVector());
                        topTask.getCyclicPredTaskSet().add(dpredTask.getIDVector().get(1));
                        CyclicMap cyclicMap = new CyclicMap(dpredTask.getIDVector().get(1),
                                topTask.getIDVector().get(1),
                                dataDD.getMaxDataSize());
                        long orgWeight = topTask.getMaxWeight();
                        long newValue = dd.getMaxDataSize() + dd.getMaxDataSize();
                        //topTask.setMaxWeight(orgWeight + newValue);
                        String key = this.getKey(dpredTask, topTask);
                        this.cyclicLIst.put(key, cyclicMap);
                        continue;
                    }
                }
                //topタスクの先行タスクのtlevelを設定・取得する．
                long taskTlevel = this.calcTaskTlevel(predCluster, dpredTask, tmpClusterSet, isSecond);

                //先行タスクに，チェック済みというマークをつける
                this.IDSet.add(dpredTask.getIDVector().get(1));


                TaskCluster dpredCluster = this.retApl.findTaskCluster(dpredTask.getClusterID());
                CPU dpredCPU = dpredCluster.getCPU();

                TaskCluster topCluster = this.retApl.findTaskCluster(topTask.getClusterID());
                CPU topCPU = topCluster.getCPU();

                long nw_time = 0;
                if (dpredCPU.getMachineID() == topCPU.getMachineID()) {

                } else {
                    nw_time = this.getNWTime(dpredID, topID, dd.getMaxDataSize(),
                            this.env.getNWLink(dpredCPU.getCpuID(), topCPU.getCpuID()));
                }
                //値を計算する．
                long value = taskTlevel + nw_time;
                if (value >= tmpTlevel2) {
                    tmpTlevel2 = value;
                    tpredID = dd.getFromID();
                }
            }
            topTask.setTpred(tpredID);
            topTask.setTlevel(tmpTlevel2);

            //Topタスクたちの中で最大のTlevelのものをクラスタのtlevelとする．
            if (tmpTlevel2 >= tmpTlevel) {
                tmpTlevel = tmpTlevel2;
                topTaskID = topID;
            }
        }

        cluster.setTlevel(tmpTlevel);
        cluster.setTopTaskID(topTaskID);
        this.checkedClusterSet.add(cluster.getClusterID());
        if (topTaskID != null) {
            this.IDSet.add(topTaskID);
        }

        bean.setTlevel(tmpTlevel);

        return bean;
    }

    /**
     * タスクのtlevelを取得・設定します．クラスタのtlvelが設定された後に呼び出されます．
     * 当該クラスタのTLEVELは既に判明している場合は当該タスクのtlevelを計算し，そうでない場合は
     * configureTlevelOfClusterメソッドをCallします．
     *
     * @param cluster
     * @param task
     * @return
     */
    public long calcTaskTlevel(TaskCluster cluster, AbstractTask task, CustomIDSet tmpClusterSet, boolean isSecond) {
        if (IDSet.contains(task.getIDVector().get(1))) {
            return task.getTlevel();
        } else {
            if (this.checkedClusterSet.contains(cluster.getClusterID())) {
                //既にチェック済み(TLEVELが分かっている)クラスタであれば，tlevelの計算をする．
                //ここでは，クラスタのTLEVELが計算済み∧当該タスクのtlevelがまだという場合
                if (isSecond) {
                    return 0;


                } else {
                    long totalEsecTime = this.getClusterInstruction(cluster) / cluster.getCPU().getSpeed();
                    //CustomIDSet destSet = this.getDestTaskList(new CustomIDSet(),task,task.getClusterID());
                    CustomIDSet destSet = task.getDestTaskSet();
                    //もし先行タスクが同クラスタであれば、同クラスタ用のTlevel計算処理に入る
                    long destValue = this.calculateSumValue(destSet);
                    long value = cluster.getTlevel() + totalEsecTime - (destValue / cluster.getCPU().getSpeed());
                    task.setTlevel(value);
                    IDSet.add(task.getIDVector().get(1));
                    return value;

                }

            } else {
                tmpClusterSet.add(cluster.getClusterID());
                if (isSecond) {
                    return 0;

                } else {
                    //当該クラスタのtlevelが未決定であるから，計算処理をする．
                    CyclicBean bean = this.configureTlevelofCluster(cluster, tmpClusterSet, isSecond);
                    long cTlevel = bean.getTlevel();
                    //cyclicなtopタスクであれば，tlevel値を計算しなおす．

                    //既にチェック済みクラスタであれば，tlevelの計算をする．
                    //まずは，当該クラスタでの開始時刻の最悪値の計算．
                    long totalEsecTime = this.getClusterInstruction(cluster) / cluster.getCPU().getSpeed();
                    //CustomIDSet destSet = this.getDestTaskList(new CustomIDSet(),task,task.getClusterID());
                    CustomIDSet destSet = task.getDestTaskSet();
                    //もし先行タスクが同クラスタであれば、同クラスタ用のTlevel計算処理に入る
                    long destValue = this.calculateSumValue(destSet);
                    long value = cTlevel + totalEsecTime - (destValue / cluster.getCPU().getSpeed());
                    task.setTlevel(value);
                    IDSet.add(task.getIDVector().get(1));
                    return value;

                }
            }

        }


    }

    /**
     * タスククラスタの各タスクの，blevelを設定します．
     */
    public AbstractTask configureTaskBlevel(AbstractTask task) {
        if (this.IDSetBlevel.contains(task.getIDVector().get(1))) {
            return task;
        }

        Long clusterID = task.getClusterID();
        TaskCluster cluster = this.retApl.findTaskCluster(clusterID);

        //移行は，タスクが未チェックのときの処理
        CustomIDSet destSet = task.getDestTaskSet();
        Iterator<Long> destIte = destSet.iterator();
        long taskDestSumValue = this.calculateSumValue(destSet);

        long blevel_in = 0;
        long blevel_out = 0;
        Vector<Long> bsucID = null;

        //まずはblevel_inを取得する．
        //タスクのDestセットに対するループ
        while (destIte.hasNext()) {
            Long destID = destIte.next();
            //自分自身なら，次へ行く．
            if (destID.longValue() == task.getIDVector().get(1).longValue()) {
                continue;
            }
            AbstractTask dTask = this.retApl.findTaskByLastID(destID);
            long dTaskDestSumValue = this.calculateSumValue(dTask.getDestTaskSet());
            //dTaskのblevel値を計算する．
            dTask = this.configureTaskBlevel(dTask);
            //dTaskの値を計算する
            long value = (taskDestSumValue - dTaskDestSumValue) / cluster.getCPU().getSpeed() + dTask.getBlevel();
            if (blevel_in <= value) {
                blevel_in = value;
            }
        }

        task.setBlevel_in(blevel_in);

        //次に，タスク自身のblevel_outを取得する．
        if (cluster.getOut_Set().contains(task.getIDVector().get(1))) {
            //タスクが，outタスクであるときのみに計算をする．
            //まずは後続タスクから，他クラスタにあるものを取得する．
            LinkedList<DataDependence> dsucList = task.getDsucList();
            Iterator<DataDependence> dsucIte = dsucList.iterator();
            //後続タスクに対するループ
            while (dsucIte.hasNext()) {
                DataDependence dd = dsucIte.next();
                //後続タスクを取得する．
                AbstractTask dsucTask = this.retApl.findTaskByLastID(dd.getToID().get(1));
                if (dsucTask.getClusterID().longValue() != clusterID.longValue()) {
                    TaskCluster dsucCluster = this.retApl.findTaskCluster(dsucTask.getClusterID());
                    //後続タスクが別クラスタであれば，後続タスクのblevel値を取得する．
                    dsucTask = this.configureTaskBlevel(dsucTask);
                    long nw_time = 0;
                    if (dsucCluster.getCPU().getMachineID() == cluster.getCPU().getMachineID()) {
                        nw_time = 0;
                    } else {
                        nw_time = this.env.getSetupTime() + dd.getMaxDataSize() / this.env.getNWLink(cluster.getCPU().getCpuID(), dsucCluster.getCPU().getCpuID()) + dsucTask.getBlevel_in();
                    }
                    long value2 = this.getInstrunction(task) / cluster.getCPU().getSpeed()
                            + nw_time;
                    if (blevel_out <= value2) {
                        blevel_out = value2;
                        bsucID = dd.getToID();
                    }
                }
            }
            task.setBlevel(blevel_out);
            task.setBsuc(bsucID);
        }

        //最後に，inとoutの大きいほうを，blevel_inにセットする．
        long retBlevel = Math.max(blevel_in, blevel_out);
        task.setBlevel_in(retBlevel);
        //チェック済み集合へ追加する．
        this.IDSetBlevel.add(task.getIDVector().get(1));

        return task;
    }

    /**
     * クラスタの移動処理です．
     * そのままだと，帯域幅が理想な状態を想定してクラスタリング＋割り当てを
     * していたが，実際にはそれよりも小さな帯域幅で通信が行われ，オーバーヘッド
     * が大きくなってしまう．そこで，実際の帯域幅を考慮して，WSLが小さくなりそうな
     * コアを探しだし，そのコアへクラスタを移動させる処理を行う．
     */
    public void cluster_remapping() {


    }


    /**
     *
     */
    public void calcClusterBlevel() {
        Iterator<TaskCluster> clusterIte = this.retApl.clusterIterator();
        //クラスタに対するループ

        while (clusterIte.hasNext()) {
            TaskCluster cluster = clusterIte.next();
            CustomIDSet outSet = cluster.getOut_Set();
            Iterator<Long> outIte = outSet.iterator();
            long sumValue = this.calculateSumValue(cluster.getTaskSet());
            long blevel_out = 0;
            Long bsucID = null;

            //outセットに対するループ
            while (outIte.hasNext()) {
                Long outTaskID = outIte.next();
                AbstractTask outTask = this.retApl.findTaskByLastID(outTaskID);
                long dSumValue = this.calculateSumValue(outTask.getDestTaskSet());
                TaskCluster thisCluster = this.retApl.findTaskCluster(outTask.getClusterID());

                //blevel値の計算をする．
                long value = (sumValue - dSumValue) / thisCluster.getCPU().getSpeed() + outTask.getBlevel();
                outTask.setSValue((sumValue - dSumValue));
                if (blevel_out <= value) {
                    blevel_out = value;
                    bsucID = outTask.getIDVector().get(1);
                }
            }
            cluster.setBlevel(blevel_out);
            cluster.setBsucTaskID(bsucID);

        }


    }

    public boolean isDeltaStatic() {
        return isDeltaStatic;
    }

    public void setDeltaStatic(boolean isDeltaStatic) {
        this.isDeltaStatic = isDeltaStatic;
    }

    public CustomIDSet getWslTaskSet() {
        return wslTaskSet;
    }

    public void setWslTaskSet(CustomIDSet wslTaskSet) {
        this.wslTaskSet = wslTaskSet;
    }
}
