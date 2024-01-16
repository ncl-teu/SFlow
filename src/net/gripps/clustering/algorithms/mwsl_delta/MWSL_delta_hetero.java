package net.gripps.clustering.algorithms.mwsl_delta;

import net.gripps.clustering.common.aplmodel.*;
import net.gripps.clustering.tool.Calc;
import net.gripps.environment.CPU;
import net.gripps.environment.Environment;

import java.io.FileInputStream;
import java.util.*;

/**
 * Author: H. Kanemitsu
 * Date: 12/08/09
 * <p/>
 * ヘテロ環境における，タスククラスタリングアルゴリズムです．
 * 各プロセッサに割り当てるべきクラスタ実行時間の下限値を算出し，
 * そしてsl_wの上限が最小化されるプロセッサを選択します．
 * その後，その下限値に達するまでタスククラスタリングを行います．
 * <p/>
 * タスクのtlevel/blevel: wtlevel/wblevel
 * クラスタのtlevel/blevel: tlevel/blevel
 */
public class MWSL_delta_hetero extends MWSL_delta {

    /**
     * マシンの処理速度，マシン間のデータ転送速度を定義した情報
     */
    protected Environment env;

    /**
     * 仮想的なプロセッサ集合．
     * 既存のプロセッサ速度と帯域の最大値をもつ．初期状態では，
     * 各タスクがこのプロセッサに割り当てられ4ているものとする．
     */
    protected Environment virtualEnv;

    /**
     * 最大の処理速度
     */
    protected long maxSpeed;

    /**
     * 最大の通信帯域幅
     */
    protected long maxBW;

    /**
     * 最大のタスクサイズ
     */
    private long maxTaskSize;

    /**
     * 最大のデータサイズ
     */
    private long maxDataSize;

    /**
     * 最小の処理速度
     */
    protected long minSpeed;

    /**
     * 最小の通信帯域幅
     */
    protected long minBW;


    /**
     * 現段階のWCP値
     */
    protected long currentWCP;


    /**
     *
     */
    protected CustomIDSet maxSequence;


    /**
     * seq_precのタスク集合
     * クリティカルパスでよい？？？
     * つまり，CPUを考慮して，その都度経路長が最大のやつを特定すればよい？？
     */
    protected CustomIDSet maxPrecSequence;


    /**
     * 未割り当てのCPUリスト
     */
    protected CustomIDSet unAssignedCPUs;

    /**
     * 設定ファイルから情報をロードします．
     */
    public void load() {
        Properties prop = new Properties();
        try {
            //create input stream from file
            prop.load(new FileInputStream(this.fileName));
            this.maxSpeed = Integer.valueOf(prop.getProperty("cpu.speed.max")).longValue();
            this.maxBW = Integer.valueOf(prop.getProperty("cpu.link.max")).longValue();
            this.minSpeed = Integer.valueOf(prop.getProperty("cpu.speed.min")).longValue();
            this.minBW = Integer.valueOf(prop.getProperty("cpu.link.min")).longValue();
            this.maxTaskSize = Integer.valueOf(prop.getProperty("task.instructions.max")).longValue();
            this.maxDataSize = Integer.valueOf(prop.getProperty("task.ddedge.size.max")).longValue();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * コンストラクタ
     *
     * @param task
     * @param file
     * @param env
     */
    public MWSL_delta_hetero(BBTask task, String file, Environment env) {
        super(task, file);
        //実際のプロセッサ環境を用意する．
        this.env = env;
        //見割り当てプロセッサリストの初期化
        this.unAssignedCPUs = new CustomIDSet();

        this.maxSequence = new CustomIDSet();

        //seq_precの初期化
        this.maxPrecSequence = new CustomIDSet();

        this.load();

    }




    /**
     * @param task
     * @param set
     * @return
     */
    public CustomIDSet calcMaxInitialWSL(AbstractTask task, CustomIDSet set) {


        //もしENDタスクであれば，終了．
        if (task.getDsucList().isEmpty()) {
            task.setBlevel(task.getMaxWeight() / this.maxSpeed);

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

                CustomIDSet retSet = this.calcMaxInitialWSL(sucTask, set);
                set.addAll(retSet);

            }
            TaskCluster cluster = this.retApl.findTaskCluster(task.getClusterID());

            long tmpValue = sucTask.getWblevel() + dsuc.getMaxDataSize() / this.maxBW + task.getMaxWeight() / this.maxSpeed;
            if (tmpValue >= wBlevel) {
                wBlevel = tmpValue;
            }

        }
        set.add(task.getIDVector().get(1));
        task.setWblevel(wBlevel);
        //クラスタのBlevelをタスクBlevelとする．
        TaskCluster cls = this.retApl.findTaskCluster(task.getIDVector().get(1));
        cls.setBlevel(wBlevel);

        return set;

    }

    /**
     * @return
     */
    public long getMaxInitialWSL() {
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
                    CustomIDSet retSet = this.calcMaxInitialWSL(sucTask, set);
                    set.addAll(retSet);

                }

                long tmpValue = sucTask.getWblevel() + dsuc.getMaxDataSize() / this.maxBW + startTask.getMaxWeight() / this.maxSpeed;
                if (tmpValue >= wBlevel) {
                    wBlevel = tmpValue;
                }

            }
            set.add(startTask.getIDVector().get(1));
            startTask.setWblevel(wBlevel);
            TaskCluster cls = this.retApl.findTaskCluster(startTask.getIDVector().get(1));
            cls.setBlevel(wBlevel);

            if (retBlevel <= wBlevel) {
                retBlevel = wBlevel;
            }

        }
        return retBlevel;

    }


    /**
     * Startタスクから順に走査することによって，各タスクの
     * Blevel値を設定する．
     *
     * @param task
     * @param set
     * @return
     */
    public long getMaxBlevel(AbstractTask task, CustomIDSet set) {
        set.add(task.getIDVector().get(1));
        AbstractTask dominatingTask = null;


        //ENDタスクであれば，blevelをそのまま帰す．
        CPU c = this.getCPU(task);
        long speed = 0;
        if(c.getCpuID() == -1){
            speed = this.maxSpeed;

        }else{
            speed = c.getSpeed();
        }
        if (task.getDsucList().isEmpty()) {
            long endBlevel = task.getMaxWeight() / speed;
            task.setBlevel(endBlevel);
            return endBlevel;
        }

        //先行タスクのtlevel値を取得する．
        Iterator<DataDependence> dsucIte = task.getDsucList().iterator();
        long maxBlevel = 0;
        long realBlevel = 0;

        //先行タスクに対するループ
        while (dsucIte.hasNext()) {
            DataDependence dsuc = dsucIte.next();
            AbstractTask sucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));
            long sucBlevel = 0;

            if (set.contains(sucTask.getIDVector().get(1))) {
                sucBlevel = sucTask.getBlevel();
            } else {
                //もし未チェックであれば，再計算する．
                sucBlevel = this.getMaxBlevel(sucTask, set);
                set.add(sucTask.getIDVector().get(1));
            }
            CPU c2 =  this.getCPU(task);
            long spd = 0;
            if(c2.getCpuID() == -1){
                spd = this.maxSpeed;

            }else{
                spd = c2.getSpeed();
            }
            //System.out.println("time:"+ this.getComTime(task, sucTask));
            //後続タスクから，自身のTlevel値を計算する．
            //realBlevel = sucBLevel + (predTask.getMaxWeight() / this.getCPU(predTask).getSpeed()) + this.getComTime(predTask, task);
            realBlevel = task.getMaxWeight() /spd + this.getComTime(task, sucTask) + sucBlevel;

            if (maxBlevel <= realBlevel) {
                maxBlevel = realBlevel;
                task.setBlevel(realBlevel);
                task.setBsuc(sucTask.getIDVector());
                dominatingTask = sucTask;
            }
        }

        if(dominatingTask!=null){
            //最大のシーケンスにその先行タスクを入れる．
            this.maxSequence.add(dominatingTask.getIDVector().get(1));
        }

        return realBlevel;

    }

    /**
     * Seq_{max}を取得します．
     * 指定クラスタ内のTop_Setの中で最大のTlevelを持つタスク(topTask)を取得する
     * Top_Setの内，最大のTlevel値のタスクを特定
     * そのタスクの先行タスクが属するクラスタを特定
     * それを再帰的に繰り返す．
     * <p/>
     * と思ったけど，その時のクリティカル・パスを計算することにした．
     * tlevelとblevelが関係する．
     *
     * @param task
     * @param set
     * @return
     */
    public long getMaxTlevel(AbstractTask task, CustomIDSet set) {
        set.add(task.getIDVector().get(1));
        AbstractTask dominatingTask = null;

        //Startタスクであれば，tlevel=0を返す．
        if (task.getDpredList().isEmpty()) {
            task.setTlevel(0);
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

            if (set.contains(predTask.getIDVector().get(1))) {
                predTlevel = predTask.getTlevel();
            } else {
                //もし未チェックであれば，再計算する．
                predTlevel = this.getMaxTlevel(predTask, set);
                set.add(predTask.getIDVector().get(1));
            }
          //  long time = this.getComTime(predTask, task);
            //先行タスクから，自身のTlevel値を計算する．
            long speed = 0;
            CPU c = this.getCPU(predTask);
            if(c.getCpuID() == -1){
                speed = this.maxSpeed;
            }else{
                speed = this.getCPU(predTask).getSpeed();
            }
            realTlevel = predTlevel + (predTask.getMaxWeight() /speed) + this.getComTime(predTask, task);

            if (maxTlevel <= realTlevel) {
                maxTlevel = realTlevel;
                task.setTlevel(realTlevel);
                task.setTpred(predTask.getIDVector());
                dominatingTask = predTask;
            }

        }
        if(dominatingTask != null){
            //最大のシーケンスにその先行タスクを入れる．
            this.maxSequence.add(dominatingTask.getIDVector().get(1));
        }

        return realTlevel;

    }


    public long getMaxWTlevel(AbstractTask task, CustomIDSet set) {
        set.add(task.getIDVector().get(1));
        AbstractTask dominatingTask = null;


        //Startタスクであれば，tlevel=0を返す．
        if (task.getDpredList().isEmpty()) {
            return 0;
        }
        //先行タスクのtlevel値を取得する．
        Iterator<DataDependence> dpredIte = task.getDpredList().iterator();
        long maxWTlevel = 0;
        long realWTlevel = 0;

        //先行タスクに対するループ
        while (dpredIte.hasNext()) {
            DataDependence dpred = dpredIte.next();
            AbstractTask predTask = this.retApl.findTaskByLastID(dpred.getFromID().get(1));
            long predWTlevel = 0;

            if (set.contains(predTask.getIDVector().get(1))) {
                predWTlevel = predTask.getTlevel();
            } else {
                //もし未チェックであれば，再計算する．
                predWTlevel = this.getMaxWTlevel(predTask, set);
            }
            //先行タスクから，自身のTlevel値を計算する．
            realWTlevel = predWTlevel + (predTask.getMaxWeight() / this.getCPU(predTask).getSpeed());

            if (maxWTlevel <= realWTlevel) {
                maxWTlevel = realWTlevel;
                task.setWtlevel(realWTlevel);
            }

        }
        //最大のシーケンスにその先行タスクを入れる．
        // this.maxSequence.add(dominatingTask.getIDVector().get(1));
        return realWTlevel;

    }


    /**
     * @param fromTask
     * @param toTask
     * @return
     */
    public long getComTime(AbstractTask fromTask, AbstractTask toTask) {
        CPU fromCPU = this.getCPU(fromTask);
        CPU toCPU = this.getCPU(toTask);
        long setupTime = this.env.getSetupTime();

        LinkedList<DataDependence> dpredList = toTask.getDpredList();
        Iterator<DataDependence> dpredIte = dpredList.iterator();

        if (fromCPU.getCpuID().longValue() == toCPU.getCpuID().longValue()) {
            if(!fromCPU.isVirtual() && !toCPU.isVirtual()){
                return 0;

            }
        }
        DataDependence dd = toTask.findDDFromDpredList(fromTask.getIDVector(), toTask.getIDVector());
        if(dd == null){
           // System.out.println("nullです");

           // this.retApl.findDDFromDpredList(fromTask.getIDVector(), toTask.getIDVector());
        }
        //双方，仮想CPUであれば，最大の帯域幅とする．
        if (fromCPU.isVirtual() && toCPU.isVirtual()) {
            return setupTime+dd.getMaxDataSize() / this.maxBW;
            //fromが仮想であれば，toの方の最大の帯域幅を使う．
        } else if (fromCPU.isVirtual() && !toCPU.isVirtual()) {
            return setupTime+dd.getMaxDataSize() / this.env.getMaxInLink(Long.valueOf(toCPU.getCpuID()).intValue());

        } else if (!fromCPU.isVirtual() && toCPU.isVirtual()) {
            return setupTime+dd.getAveDataSize() / this.env.getMaxOutLink(Long.valueOf(fromCPU.getCpuID()).intValue());
        } else {
            return setupTime+dd.getMaxDataSize() / this.env.getLink(Long.valueOf(fromCPU.getCpuID()).intValue(),
                    Long.valueOf(toCPU.getCpuID()).intValue());
        }
    }

    /**
     * 指定タスクから，そのタスクが割り当てられているCPUを返します．
     *
     * @param task
     * @return
     */
    public CPU getCPU(AbstractTask task) {
        TaskCluster cluster = this.retApl.findTaskCluster(task.getClusterID());
        return cluster.getCPU();
    }


    /**
     * DAGに対して必要な情報をセットする．
     * ここでは，
     */
    protected void prepare() {
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
                vlinks[i][j] = this.maxBW;
            }
        }
        //仮想リンク集合をセットする．
        this.virtualEnv.setLinkMatrix(vlinks);

        double taskMaxG = 0.0;

        //タスククラスタの生成
        //各タスクに対するループ
        while (ite.hasNext()) {
            AbstractTask task = ite.next();
            //CPU作成
            CPU vCPU = new CPU(task.getIDVector().get(1), this.maxSpeed, new Vector<Long>(), new Vector<Long>());
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
        this.level = cLevel;
        long end1 = System.currentTimeMillis();
        //System.out.println("レベル反映時間: "+(end1-start));

    }

    /**
     * メイン処理です．
     *
     * @return
     */
    public BBTask process() {
        try {
            this.prepare();

            long start = System.currentTimeMillis();
            //メイン処理
            this.mainProcess();
            long end = System.currentTimeMillis();
            retApl.setProcessTime((end - start));

            //次は，δに満たさなかったものに対する，クラスタリング処f理
            //ここでは，LBに基づくものとする．
            this.mainProcessLB();
            //後処理を行う．
            this.postProcess();

            return this.retApl;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Freeクラスタリストから，LV値が最大のタスクを選択する．
     *
     * @return
     */
    public TaskCluster getMaxLevelCluster() {
        Iterator<Long> ite = this.freeClusterList.iterator();
        long tmpLV = 0;
        TaskCluster retCluster = null;
        while (ite.hasNext()) {
            Long id = ite.next();
            TaskCluster cluster = this.retApl.findTaskCluster(id);
            long lv = cluster.getTlevel() + cluster.getBlevel();
            if (lv >= tmpLV) {
                tmpLV = lv;
                retCluster = cluster;
            }
        }
        return retCluster;
    }

    /**
     * 指定されたクラスタに対して，割り当てるべきプロセッサを決める．
     */
    public CPU getNextPE(TaskCluster cluster) {

        //未割り当てCPUを選択する．
        Iterator<Long> cpuIte = this.unAssignedCPUs.iterator();
        CPU retCPU = null;
        double tmpValue = 100000000;

        //CPUのループ
        while (cpuIte.hasNext()) {
            Long cpuID = cpuIte.next();
            CPU cpu = this.env.findCPU(cpuID);
            //CPUの情報をΔsl_w,upへ代入する．
            double d_slwup = this.getDelta_slUp(cpu.getSpeed(), this.getMinLink(cpu));
            if (tmpValue >= d_slwup) {
                retCPU = cpu;
                tmpValue = d_slwup;
            }

        }
        return retCPU;

    }

    /**
     * 当該CPUのout/inにおける最小の帯域幅を取得します．
     * もし設定されていなければ，検索します．既に設定されていれば，
     * その値を返すだけです．
     *
     * @param cpu
     * @return
     */
    public long getMinLink(CPU cpu) {
        long retLink;
        if (cpu.getMinLink() == 0) {
            retLink = this.env.getMinLink(cpu.getCpuID());
            cpu.setMinLink(retLink);

        } else {
            retLink = cpu.getMinLink();
        }

        return retLink;
    }


    /**
     * 各プロセッサの処理速度異，帯域幅から，Δsl_upの値を算出します．
     *
     * @param speed
     * @param bandwidth
     * @return
     */
    public double getDelta_slUp(long speed, long bandwidth) {
        long endID = this.retApl.getTaskList().size();
        AbstractTask endTask = this.retApl.findTaskByLastID(new Long(endID));
        //ENDタスクのWレベル値を計算する．
        long wtlevel = this.getMaxWTlevel(endTask, new CustomIDSet());
        double k = Calc.getRoundedValue((double) (speed / bandwidth));
        this.currentWCP = wtlevel + endTask.getMaxWeight() / this.getCPU(endTask).getSpeed();
        double d_wsl_up = Math.sqrt((this.currentWCP) * (Calc.getRoundedValue((double) this.maxDataSize / k) +
                this.maxTaskSize)) * Calc.getRoundedValue((double) 2 / speed);

        return d_wsl_up;
    }

    /**
     * 指定プロセッサのクラスタ実行時間の下限値を算出する．
     *
     * @param cpu
     * @return
     */
    public long getDelta_opt(CPU cpu) {
        double delta_opt = Math.sqrt((double) this.currentWCP * (Calc.getRoundedValue((double) this.maxTaskSize / cpu.getSpeed()
                + Calc.getRoundedValue((double) this.maxDataSize / this.getMinLink(cpu)))));

        return (long) delta_opt;

    }

    /**
     * 現在割り当ててるクラスタのクラスタ実行時間が，しきい値を超えているかどうか
     * のチェックを行います．
     *
     * @param cluster
     * @param cpu
     * @return
     */
    public boolean isAboveThreshold(TaskCluster cluster, CPU cpu) {
        Iterator<Long> taskIDIte = cluster.getTaskSet().iterator();
        long execTime = 0;
        while (taskIDIte.hasNext()) {
            Long taskID = taskIDIte.next();
            AbstractTask task = this.retApl.findTaskByLastID(taskID);
            execTime += task.getMaxWeight();
        }

        if (execTime / cpu.getSpeed() < cpu.getThresholdTime()) {
            return false;
        } else {
            return true;
        }
    }


    /**
     * pivot内の特定タスクを元にして，targetを取得します．
     *
     *
     * @param pivot 起点となるクラスタ
     * @param orgTask   pivot内のタスク（実際の起点となるタスク）
     * @return
     */
    public TaskCluster getTarget(TaskCluster pivot, AbstractTask orgTask) {
        Iterator<DataDependence> dsucIte = orgTask.getDsucList().iterator();

        long retBlevel = 0;
        long tmpBlevel = 0;
        TaskCluster target = null;

        //Bsucタスクの後続タスクたちに対するループ
        while (dsucIte.hasNext()) {
            DataDependence dsuc = dsucIte.next();
            //後続タスクを取得する．
            AbstractTask dsucTask = this.retApl.findTaskByLastID(dsuc.getToID().get(1));
            //後続タスクが属するクラスタを取得する．
            TaskCluster dsucCluster = this.retApl.findTaskCluster(dsucTask.getClusterID());
            //もし同一クラスタなら，処理を飛ばす．
            if (dsucCluster.getClusterID().longValue() == pivot.getClusterID().longValue()) {
                continue;
            }
            //後続タスクが，2以上の要素を持つクラスタに属していても，飛ばす．
            if (dsucCluster.getTaskSet().getList().size() > 1) {
                continue;
            }

            //異なるクラスタならば，blevel値を取得する．
            tmpBlevel = orgTask.getMaxWeight() / this.getCPU(orgTask).getSpeed() + this.getComTime(orgTask, dsucTask) + dsucTask.getBlevel();
            if (tmpBlevel >= retBlevel) {
                retBlevel = tmpBlevel;
                target = dsucCluster;
            }
        }

        return target;


    }


    /**
     * 指定のpivotを元にして，targetを決定します．
     * そのあと，実際のクラスタリングを行います．
     * 事前に必要なこと：
     *
     * 各クラスタのBsucタスク（クラスタのblevel値を支配し，かつその後続タスクが別クラスタに属するようなタスク）を決めること
     * Topタスクの集合を決めること
     * Outタスクの集合を決めること
     *  TopタスクのTlevelを決めておくこと＋TopタスクのTlevelを支配するタスク（Tpred)を決めること
     *  Bottomタスクの集合を決めること
     *
     * @param pivot
     * @return
     */
    public TaskCluster clustering(TaskCluster pivot) {
        //A. pivotのBsucタスクを取得する．
        AbstractTask bsucTask = this.retApl.findTaskByLastID(pivot.getBsucTaskID());
        TaskCluster target = null;
        target = this.getTarget(pivot, bsucTask);

        //もしtargetが見つからなければ，別候補から選ぶ．

        //B. Bsucの後続タスクが全て集約済みクラスタである場合は，Outから選択する．
        //Outタスクのうち，BlevelがMaxのものを選択する．
        if (target == null) {
            Iterator<Long> outIte = pivot.getOut_Set().iterator();
            PriorityQueue<AbstractTask> outQueue = new PriorityQueue<AbstractTask>(5, new BlevelComparator());
            //Outタスクを，Blevelの大きい順にソートする．
            while (outIte.hasNext()) {
                Long id = outIte.next();
                AbstractTask outTask = this.retApl.findTaskByLastID(id);
                outQueue.offer(outTask);
            }
            //Blevelの大きい順にoutTaskを取り出す．
            while (!outQueue.isEmpty()) {
                AbstractTask outTask = outQueue.poll();
                target = this.getTarget(pivot, outTask);
            }
        }

        //C. Outタスクの後続タスクが全て集約済みクラスタであれば，Topの先行タスクから選択する．
        if (target == null) {
            //Topタスクのリストを取得する．
            Iterator<Long> topIte = pivot.getTop_Set().iterator();
            PriorityQueue<AbstractTask> topQueue = new PriorityQueue<AbstractTask>(5, new TlevelComparator());
            //Topタスクに対するループ
            while (topIte.hasNext()) {
                AbstractTask topTask = this.retApl.findTaskByLastID(topIte.next());
                //Tlevel値の大きい順にソートする．
                topQueue.offer(topTask);
            }
            //Topタスクがなくなるまで，Tlevelを支配する先行タスクを取得する．
            while (!topQueue.isEmpty()) {
                AbstractTask topTask = topQueue.poll();
                if (topTask.getTpred() == null) {
                    continue;
                }
                AbstractTask tpredTask = this.retApl.findTaskByLastID(topTask.getTpred().get(1));
                //tpredタスクが属するクラスタをターゲットとする．
                target = this.retApl.findTaskCluster(tpredTask.getClusterID());
            }
        }

        //D. AもBも満たさない場合（topがSTARTタスクのみであり，Outの後続タスクも全て集約済み），Bottomタスクの後続タスクの集約済みクラスタを
        //targetとする．
        if (target == null) {
            Iterator<Long> bottomIte = pivot.getBottomSet().iterator();
            long bblevel = 0;
            AbstractTask ret_bTask = null;

            //Bottomタスクのうち，Blevelが最大のものを選択する．
            while(bottomIte.hasNext()){
                AbstractTask bTask = this.retApl.findTaskByLastID(bottomIte.next());
                if(bblevel <= bTask.getBlevel()){
                    bblevel = bTask.getBlevel();
                    ret_bTask = bTask;
                }
            }
            target = this.getTarget(pivot, ret_bTask);
        }

        //実際のクラスタリング処理を行う．
        return this.clusteringClusters(pivot, target);
    }


    /**
     * タスククラスタリング処理
     * @param pivot
     * @param target
     * @return
     */
    public TaskCluster clusteringClusters(TaskCluster pivot, TaskCluster target){

        Long topTaskID = pivot.getTopTaskID();
        if (pivot.getClusterID().longValue() > target.getClusterID().longValue()) {
            return this.clusteringClusters(target, pivot);
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

        //pivotのIn/Outを更新
        //Topタスク→Topじゃなくなるかも(どちらかがTopでのこり，他方がTopじゃなくなる）
        //Outタスク→Outじゃなくなるかも（すくなくともTopにはならない）
        //それ以外→それ以外のまま
        //まずはclusterのtopを，pivot側にする．というかもともとそうなっているので無視
        //だけど，outSetだけは更新しなければならない．
        this.updateOutSet(pivot, target);
        //InSetを更新する（後のレベル値の更新のため）
        this.updateInSet(pivot, target);

        pivot.setTopTaskID(topTaskID);
        //各タスクの，destSetの更新をする．

        CustomIDSet allSet = pivot.getTaskSet();
        Iterator<Long> idIte = allSet.iterator();
        CustomIDSet startIDs = new CustomIDSet();
        //まずはTopタスクのIDを追加しておく．

        AbstractTask startTask = this.retApl.findTaskByLastID(pivot.getTopTaskID());
        pivot.getDestCheckedSet().getObjSet().clear();
        this.updateDestTaskList2(new CustomIDSet(), startTask, pivot.getClusterID());

        //targetクラスタをDAGから削除する．
        this.retApl.removeTaskCluster(target.getClusterID());
        //targetクラスタをUEXから削除する．
        this.uexClusterList.remove(target.getClusterID());
        //targetクラスタをFreeから削除する．
        this.freeClusterList.remove(target.getClusterID());

        //あとは，新pivotがEXなのかUEXなのかの判断をする．

        /**
         * pivotが，
         * - δ以上(Topの入力辺がすべて"Checked"になっている．Inについてはどうでもよい)
         *   →pivotをEXへ入れる(UEXから削除する)
         *   →pivotをFreeから削除する
         *   →pivotのOutタスクの，後続タスクの入力辺を"Checked"とする．
         * - δ未満の場合
         * 　→pivotはFreeのまま
         *   →pivotはUEXのまま
         *
         */
        if (this.isAvobeThreshold(pivot)) {
            Long endID = new Long(this.retApl.getTaskList().size());
            if ((pivot.isLinear()) && (!pivot.getTaskSet().contains(endID))) {

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
                        //もしクラスタのInタスクがすべてCheckeであれば，そのクラスタをFreeListへ入れる．
                        this.addFreeClusterList(clusterid);
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
                    this.freeClusterList.add(pivot.getClusterID());
                }

            }

        }
        // BL, Tpred, Bsuc, tlevel, blevel, Tpred, Bsucの更新をする．
        // TL(pivot)は不変なので，考慮する必要はない．
        this.updateLevel(pivot);

        return null;
    }

    /**
     * アルゴリズムの主要な処理部です．
     * <p/>
     * 1. まず，seq_{s-1}\prec となるタスク集合を特定する．
     * 2. 1で決まったタスク集合についてΔsl_{w,up}を計算して，各プロセッサ情報を代入する．
     * 3. Δsl_{w,up}が最小となるようなプロセッサpを特定する．
     * 4. プロセッサpについてのクラスタ実行時間の下限値δを決定する．
     * 5. タスククラスタリングの処理を行う．
     */
    public void mainProcess() {
        while (!this.uexClusterList.isEmpty()) {
            //Freeクラスタリストから，LV値が最大のものを選択する．
            TaskCluster dominatingCluster = this.getMaxLevelCluster();
            //支配クラスタを元にして作られる新クラスタに割り当てるプロセッサを決める．
            CPU nextCPU = this.getNextPE(dominatingCluster);
            //そのプロセッサ用のクラスタ実行時間の下限値を算出する．
            long delta_opt = this.getDelta_opt(nextCPU);

            //CPUに，クラスタ実行時間の下限値を設定する．
            nextCPU.setThresholdTime(delta_opt);

            //選ばれたCPUを，実割り当てリストから削除する．
            this.unAssignedCPUs.remove(nextCPU.getCpuID());

            //指定された下限値となるまで，タスククラスタリングを行う．
            TaskCluster pivot = new TaskCluster(new Long(-1));
            TaskCluster target = new TaskCluster(new Long(-1));

            //pivotを，支配クラスタとする．
            TaskCluster pivotCandidate = dominatingCluster;

            this.clustering(pivotCandidate);

        }
    }
}
