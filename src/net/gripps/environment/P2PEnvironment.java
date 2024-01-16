package net.gripps.environment;

import net.gripps.clustering.common.aplmodel.*;
import net.gripps.clustering.tool.Calc;

import java.util.*;
import java.io.FileInputStream;

/**
 * Author: H. Kanemitsu
 * Date: 2010/06/01
 */
public class P2PEnvironment extends Environment {

    /**
     * ホップ数を管理する行列です．
     */
    protected long[][] hopMatrix;

    /**
     *
     */
    protected long hopMin;


    /**
     * 　最小ホップ数
     */
    protected int nbrNum;

    /**
     * 最大ホップ数
     */
    protected long hopMax;

    /**
     * DispatcherのピアID
     */
    protected long dispatcherID;


    /**
     * 必要となるピア数の最大値
     */
    protected long req_peerNum;


    /**
     * Grid処理のためのピアグループです．
     */
    protected Hashtable<Long, CPU> hopDPeerGroup;

    /**
     * ピア間のホップ数に基づいて決定された，ピアグループ
     */
    protected Hashtable<Long, CPU> hopPeerGroup;


    /**
     * 粒度＋ホップ数に基づいて決定された，ピアグループ
     */
    protected Hashtable<Long, CPU> gPeerGroup;


    /**
     * 行きと帰りの平均帯域幅で決定された，ピアグループ
     */
    protected Hashtable<Long, CPU> averageLinkPeerGroup;

    protected Hashtable<Long, CPU> cpuPeerGroup;

    /**
     * 指定の粒度値
     */
    protected double grain_const;

    protected int mode;

    /**
     * コンストラクタ
     *
     * @param fileName
     * @param peerNum
     */
    public P2PEnvironment(String fileName, int peerNum) {
        //マシンリスト及び，帯域の行列を生成する．
        super(fileName, peerNum);

        //あとは，このクラス独自の処理
        this.hopMatrix = new long[peerNum][peerNum];
        
        hopDPeerGroup = new Hashtable<Long, CPU>();
        hopPeerGroup  = new Hashtable<Long, CPU>();
        gPeerGroup = new Hashtable<Long, CPU>();
         averageLinkPeerGroup = new Hashtable<Long, CPU>();
        cpuPeerGroup = new Hashtable<Long, CPU>();

        try {
            Properties prop = new Properties();
            prop.load(new FileInputStream(fileName));
            this.hopMin = Long.valueOf(prop.getProperty("network.hop.min")).longValue();
            this.hopMax = Long.valueOf(prop.getProperty("network.hop.max")).longValue();
            this.nbrNum = Integer.valueOf(prop.getProperty("network.neighbors")).intValue();
            this.grain_const = Double.valueOf(prop.getProperty("grain.constant")).doubleValue();
            this.mode = Integer.valueOf(prop.getProperty("network.peergroup.mode")).intValue();

        } catch (Exception e) {
            e.printStackTrace();
        }

        //Dispatcherを決定する．
        this.dispatcherID = AplOperator.getInstance().generateLongValue(0, this.cpuList.size() - 1);

        //ホップ数行列を構築するためのループ
        //クラスタ数（必要プロセッサ数）分だけループする．
        //各要素に，hop数のランダム値を入れる．
        for (int i = 0; i < peerNum; i++) {
            for (int j = 0; j < peerNum; j++) {
                if (i == j) {
                    this.hopMatrix[i][j] = 0;
                } else {
                    this.hopMatrix[i][j] = AplOperator.getInstance().generateLongValue(this.hopMin, this.hopMax);
                }
            }
        }
    }

    /**
     * @param from
     * @param to
     * @return
     */
    public long getHop(int from, int to) {
        return this.hopMatrix[from][to];
    }

    /**
     * メイン処理です．
     * 各ピアについて，隣接ピアを構築する．
     */
    public void process(BBTask apl) {
        Iterator<CPU> mIte = (Iterator<CPU>) this.cpuList.elements();
        this.req_peerNum = apl.getTaskClusterList().size();
       

        // 隣接ピア構築→ピアグループ生成処理は，面倒なのでやめた．
        //各ピアに対するループ
        while (mIte.hasNext()) {
            //順番に，ピアを取得する．
            CPU m = mIte.next();
            //ピアMの隣接ピアのIDを格納する．
            this.constructNbr(m);
        }

        //隣接ピアを構築したので，dispatcherを中心にして，
        switch(this.mode){
            case 1:
                this.buildPeerGroupWithDispatcherHop();
                break;
            case 2:
                this.buildPeerGroupWithPeerHop();
                break;
            case 3:
                this.buildPeerGroupWithG(apl);
                break;
            /**
             * 3グループを構成し，その中から評価式に従って必要プロセッサを集める．
             */
            case 4:
                this.composePeerGroupByGroups(apl);
                //必要なプロセッサを集める．
                break;

            default:
                this.buildPeerGroupWithDispatcherHop();
                this.buildPeerGroupWithPeerHop();
                this.buildPeerGroupWithG(apl);
                break;
        }
        /*
        //リンク情報を更新する．
        int len = this.linkMatrix[0].length;
        for(int i=0;i<len;i++){
            for(int j=0; j< len; j++){
                if(i==j){
                   continue;
                }else{
                    this.linkMatrix[i][j] = this.linkMatrix[i][j]/this.hopMatrix[i][j];                    
                }
            }
        }
        */
    }

    /**
     *  ホップ数，帯域幅，処理速度の3グループを構成し，その中から評価式
     *  に基づいて必要数のピアグループを構成します．
     *
     */
    public void  composePeerGroupByGroups(BBTask apl){
        //ホップに応じたピアグループを構成する
        this.buildPeerGroupWithDispatcherHop();
        //BWの平均（行き，帰り）に応じてピアグループを構成する．
        this.buildPeerGroupWithAverageBW();
        //cpu速度に応じて，ピアグループを構成する．
        this.buildPeerGroupWithCPU();

        //DAGの，δ_opt値を取得する．
        //局所化されていない平均データサイズを取得する．
        Iterator<AbstractTask> taskIte = apl.taskIerator();
        long i = 0;
       // long j=0;
        long sum_data = 0;
        while(taskIte.hasNext()){
            AbstractTask task = taskIte.next();
            Iterator<DataDependence> sucIte = task.getDsucList().iterator();
            Long org_ClusterID = task.getClusterID();
            while(sucIte.hasNext()){
                DataDependence dd = sucIte.next();
                AbstractTask sucTask = apl.findTaskByLastID(dd.getToID().get(1));
                Long suc_ClusterID = sucTask.getClusterID();
                if(org_ClusterID.longValue() !=  suc_ClusterID.longValue()){
                    //もし異なるクラスタであれば，カウントする．
                    i++;
                    sum_data += dd.getMaxDataSize();
                }else{
                    //j++;
                }
            }
        }

        //平均データサイズを求める．
        long ave_data = sum_data / i;

        //3グループから，一番よさそうなピアを生成する．
        //いったんリストをクリアする．
        this.cpuList.clear();
        PriorityQueue<TmpMachine> mQueue = new PriorityQueue<TmpMachine>(5, new MachineComparator());
        //ホップ数によるピアグループ
        Iterator<CPU> hopIte = this.hopDPeerGroup.values().iterator();
        while(hopIte.hasNext()){
            CPU m = hopIte.next();
            //評価値を算出する．
            double evalValue = Calc.getRoundedValue((double)apl.getOptDelta()/m.getSpeed() + (double)(ave_data*this.getAveHop(m.getCpuID())/this.getAveLink(m.getCpuID().intValue())));
            TmpMachine tm = new TmpMachine(evalValue, m);
            //キューに追加する．
            mQueue.add(tm);
        }

        //帯域幅によるピアグループ
        Iterator<CPU> bwIte = this.averageLinkPeerGroup.values().iterator();
        while(bwIte.hasNext()){
            CPU m = bwIte.next();
            if(this.hopDPeerGroup.containsValue(m)){
                continue;
            }else{
                double evalValue = Calc.getRoundedValue((double)apl.getOptDelta()/m.getSpeed() + (double)(ave_data*this.getAveHop(m.getCpuID())/this.getAveLink(m.getCpuID().intValue())));
                TmpMachine tm = new TmpMachine(evalValue, m);
                mQueue.add(tm);
            }
        }

        //cpu処理速度のよるピアグループ
        Iterator<CPU> cpuIte = this.cpuPeerGroup.values().iterator();
        while(cpuIte.hasNext()){
            CPU m = cpuIte.next();
            if(this.hopDPeerGroup.containsValue(m) || this.averageLinkPeerGroup.containsValue(m)){
                continue;
            }else{
                double evalValue = Calc.getRoundedValue((double)apl.getOptDelta()/m.getSpeed() + (double)(ave_data*this.getAveHop(m.getCpuID())/this.getAveLink(m.getCpuID().intValue())));
                TmpMachine tm = new TmpMachine(evalValue, m);
                mQueue.add(tm);
            }
        }

        for(int j=0;j<req_peerNum;j++){
            TmpMachine tm = mQueue.poll();
            System.out.println(tm.getEvalValue());
            this.cpuList.put(tm.getCPU().getCpuID(), tm.getCPU());
        }



    }

    public double getAveHop(Long  mID){
        long len = this.hopMatrix[mID.intValue()].length;
        long totalValue = 0;
        for(int i=0;i<len;i++){
            long data = this.hopMatrix[mID.intValue()][i];
            totalValue += data;
        }

        for(int i=0;i<len;i++){
            long data = this.hopMatrix[i][mID.intValue()];
            totalValue += data;
        }

        return Calc.getRoundedValue((double)totalValue / (len*2));

    }



    /**
     *
     * @param m
     * @param ave_data
     * @return
     */
    private long getEvalValue(CPU m, long ave_data){

         return 0;

    }

    public void buildPeerGroupWithCPU(){
        PriorityQueue<CPU> predQueue = new PriorityQueue<CPU>(5, new CPUComparator());
        Iterator<CPU> machineIte = this.cpuList.values().iterator();
        while(machineIte.hasNext()){

            CPU m = machineIte.next();
            predQueue.add(m);
        }

         int predSize = predQueue.size();

        //あとは，指定プロセッサ数となるまでpollするのみ．
        for (int i = 0; i <this.req_peerNum; i++) {
            //優先度リストから，先頭を取得する．
            CPU CPU = predQueue.poll();
            this.cpuPeerGroup.put(CPU.getCpuID(),
                    this.cpuList.get(CPU.getCpuID()));
        }

    }

    /**
     * 帯域幅の平均（行き，帰り）に応じて，ピアグループを構成する．
     */
    public void buildPeerGroupWithAverageBW(){
        PriorityQueue<LinkInfo> predQueue = new PriorityQueue<LinkInfo>(5, new AverageLinkComparator());
        for(int i=0;i<this.req_peerNum;i++){
            if(i != this.dispatcherID){
                long value1 = this.linkMatrix[(int)this.dispatcherID][i];
                long value2 = this.linkMatrix[i][(int)this.dispatcherID];
                LinkInfo linkInfo = new LinkInfo(value1,new Long(this.dispatcherID), new Long(i), (value1+value2)/2);

                predQueue.add(linkInfo);

            }else{
                  continue;
            }
        }

         int predSize = predQueue.size();

        //あとは，指定プロセッサ数となるまでpollするのみ．
        for (int i = 0; i <predSize-1 /*this.req_peerNum - 1*/; i++) {
            //優先度リストから，先頭を取得する．
            LinkInfo  info = predQueue.poll();
            this.averageLinkPeerGroup.put(info.getToID(),
                    this.cpuList.get(info.getToID()));
        }

        //最後に，Dispatcher自身を入れる．
        this.averageLinkPeerGroup.put(new Long(this.dispatcherID),
                this.cpuList.get(new Long(this.dispatcherID)));

    }

    /**
     * Dispatcherからのhop数に基づいて，指定数のピアグループを集めます．
     */
    public void buildPeerGroupWithDispatcherHop() {
        //指定ピア数となるまで，ホップ数の小さい順にピアを集める．
        //そのために，Dispatcherからのホップ数の昇順に並び替える．
        //実際にはマージソートを行う．
        PriorityQueue<HopInfo> predQueue = new PriorityQueue<HopInfo>(5, new HopComparator());
        for (int i = 0; i <this.cpuList.size() /*- 2*/; i++) {
            //要素を取得する．
            if (i != this.dispatcherID) {
                //要素を格納する．
                long value = this.hopMatrix[(int) this.dispatcherID][i];
                long toID = i;
                //ソート用のオブジェクトを生成する．
                HopInfo hInfo = new HopInfo(new Long(this.dispatcherID), new Long(i), value);
                //優先度リストへ入れておく．
                predQueue.add(hInfo);
            } else {
               //ソート用のオブジェクトを生成する．
               // HopInfo hInfo = new HopInfo(new Long(this.dispatcherID), new Long(this.dispatcherID), 0);
                //優先度リストへ入れておく．
               // predQueue.add(hInfo);

                continue;
            }
        }
        int predSize = predQueue.size();

        //あとは，指定プロセッサ数となるまでpollするのみ．
        for (int i = 0; i <this.req_peerNum-1; i++) {
            //優先度リストから，先頭を取得する．
            HopInfo info = predQueue.poll();

            this.hopDPeerGroup.put(info.getToID(),
                    this.cpuList.get(info.getToID()));
        }

        //最後に，Dispatcher自身を入れる．
        this.hopDPeerGroup.put(new Long(this.dispatcherID),
                this.cpuList.get(new Long(this.dispatcherID)));

    }

    /**
     * ピア間のホップ数のみを参照し，ピアグループを構築します．
     */
    public void buildPeerGroupWithPeerHop() {
        //各ピアの入力／出力のホップ数で平均を取る．
        //そしてそのホップ数が小さい順にピアグループへ入れる．
        //まずは，Dispatcherからのホップ数の小さいものを見る．
        CustomIDSet peerSet = new CustomIDSet();
        Long minHopPeerId = this.getMinHopPeer(new Long(this.dispatcherID), peerSet);

        //ピアグループリストへ追加する．
        this.hopPeerGroup.put(minHopPeerId, this.cpuList.get(minHopPeerId));
        int i = 0;
        //それから，隣接ピアごとに繰り返す．
        //これは必要な最大ピア数に達するまで繰り返される．
        while (peerSet.getList().size() < req_peerNum) {
            //新しい，後続ピアを取得する．
            minHopPeerId = this.getMinHopPeer(minHopPeerId, peerSet);
            this.hopPeerGroup.put(minHopPeerId, this.cpuList.get(minHopPeerId));
        }
    }

    /**
     * 指定IDのピアから，最小のホップ数であるピアのIDを返します．
     *
     * @param id
     * @return 最小のホップ数を持つピアのID
     */
    public Long getMinHopPeer(Long id, CustomIDSet tmpSet) {
        CPU srcPeer = this.cpuList.get(id);
        Iterator<Long> nbrs = srcPeer.getNbrSet().iterator();

        long retHop = -1;
        Long peerID = null;
        //隣接ピアに対するループ
        while (nbrs.hasNext()) {
            Long nbrID = nbrs.next();
            //既に存在するピアであれば，次のループへ進む．
            if (tmpSet.contains(nbrID)) {
                continue;
            }
            //id, nbrIdとのホップ数を取得する．
            long hop = this.hopMatrix[(int) id.longValue()][(int) nbrID.longValue()];
            if (retHop < 0) {
                retHop = hop;
                peerID = nbrID;

            } else {
                if (hop < retHop) {
                    retHop = hop;
                    peerID = nbrID;
                }
            }
        }
        tmpSet.add(peerID);
        return peerID;
    }


    /**
     * 粒度＋ホップ数を見て，ピアグループを構築します．
     * - Dispatcherの隣接ピアを見る．
     * - その中で，Gの値に最も近い値のものを一つ選択する．
     * - さらに，そのピアの隣接ピアの中でGに最も近い値のものを一つ選択する．
     * - 以降，これを繰り返す．
     */
    public void buildPeerGroupWithG(BBTask apl) {
        //まずは，DAGにおける平均の粒度値を計算する．

        Iterator<AbstractTask> taskIte = apl.taskIerator();
        long value = 0;
        long comm = 0;
        long edgeNum = 0;
        while (taskIte.hasNext()) {
            AbstractTask task = taskIte.next();
            value += task.getMaxWeight();
            LinkedList<DataDependence> dsucList = task.getDsucList();
            Iterator<DataDependence> dsucIte = dsucList.iterator();
            edgeNum += dsucList.size();
            while (dsucIte.hasNext()) {
                DataDependence dd = dsucIte.next();
                comm += dd.getMaxDataSize();
            }
        }
        apl.setEdgeNum(edgeNum);
        apl.setEdgeWeight(comm);
        apl.setTaskWeight(value);

        //DAGにおける粒度（丸め済み）を引数にして，各ピアにおける粒度を求める．
        double g_dag = Calc.getRoundedValue((double) value / comm);
        //まずは，Dispatcherのg値を算出する．
        CustomIDSet tmpSet = new CustomIDSet();
        //Dispatcherの子供ピアのIDを取得する．
        Long childID = this.selectOptimalG(this.dispatcherID, tmpSet, g_dag);

        //必要最大ピア数に達するまで，ピアグループを構築する．
        while (tmpSet.getList().size() < this.req_peerNum) {
            //基点ピアから見て，最もGが近いピアを選択する．
            childID = this.selectOptimalG(this.dispatcherID, tmpSet, g_dag);
            CPU childCPU = this.cpuList.get(childID);
            //ピアグループリストへ追加する．
            this.gPeerGroup.put(childID, childCPU);
        }
    }

    /**
     * 指定IDのピアについて，その隣接ピアの中から最もGの値に近いピアのIDを返す．
     *
     * @param id
     * @param tmpSet
     * @return
     */
    public Long selectOptimalG(Long id, CustomIDSet tmpSet, double g_dag) {
        CPU m = this.cpuList.get(id);
        Iterator<Long> nbrIte = m.getNbrSet().iterator();
        double dif_g = -1.0;
        Long retID = null;
        //隣接ピアに対するループ
        while (nbrIte.hasNext()) {
            Long nbrID = nbrIte.next();
            //もし既にあれば，選択対象から取り除く．
            if (tmpSet.contains(nbrID)) {
                continue;
            }
            //隣接ピアの粒度を計算する．
            double g = this.getGofPeer(id, g_dag);
            if (dif_g < 0) {
                dif_g = Math.abs(this.grain_const - g);
                retID = nbrID;
            } else {
                double retdif_g = Math.abs(this.grain_const - g);
                if (retdif_g < dif_g) {
                    dif_g = retdif_g;
                    retID = nbrID;
                }
            }
        }
        tmpSet.add(retID);
        return retID;
    }


    /**
     * 指定IDのピアにおいて，粒度を計算する．
     * 粒度は，隣接ピアのみから算出するものとする（これが現実的）
     *
     * @param id
     * @return
     */
    public double getGofPeer(Long id, double g_dag) {
        double retDif = -1.0;
        CPU CPU = this.cpuList.get(id);
        CustomIDSet nbrSet = CPU.getNbrSet();
        Iterator<Long> nbrIte = nbrSet.iterator();
        Long retID;
        double ave_bw = 0.0;
        double ave_hop = 0.0;
        long nbrNum = CPU.getNbrSet().getList().size();
        long cpu = CPU.getSpeed();

        while (nbrIte.hasNext()) {
            Long nID = nbrIte.next();

            //帯域を加算する．
            ave_bw += this.linkMatrix[(int) id.longValue()][(int) nID.longValue()];
            //ホップ数を加算する．
            ave_hop += this.hopMatrix[(int) id.longValue()][(int) nID.longValue()];

        }
        ave_bw = Calc.getRoundedValue((double) ave_bw / nbrNum);
        ave_hop = Calc.getRoundedValue((double) ave_hop / nbrNum);
        //最終的な粒度を算出する．
        double grain = Calc.getRoundedValue(g_dag * (ave_bw / (ave_hop * cpu)));

        return grain;
    }

    /**
     * 指定ピアに対して，隣接ピアを生成します.
     *
     * @param m
     */
    public void constructNbr(CPU m) {
        //隣接ピア数を決定させるためのループ
        for (int i = 0; m.getNbrSet().getList().size() < this.nbrNum; i++) {
            long ranID = AplOperator.getInstance().generateLongValue(0, this.cpuList.size() - 1);
            if (m.getNbrSet().contains(new Long(ranID))) {
                continue;
            } else {
                m.addNbr(ranID);
            }
        }
    }

    public long getDispatcherID() {
        return dispatcherID;
    }

    public void setDispatcherID(long dispatcherID) {
        this.dispatcherID = dispatcherID;
    }

    public Hashtable<Long, CPU> getHopDPeerGroup() {
        return hopDPeerGroup;
    }

    public void setHopDPeerGroup(Hashtable<Long, CPU> hopDPeerGroup) {
        this.hopDPeerGroup = hopDPeerGroup;
    }

    public Hashtable<Long, CPU> getHopPeerGroup() {
        return hopPeerGroup;
    }

    public void setHopPeerGroup(Hashtable<Long, CPU> hopPeerGroup) {
        this.hopPeerGroup = hopPeerGroup;
    }

    public Hashtable<Long, CPU> getGPeerGroup() {
        return gPeerGroup;
    }

    public void setGPeerGroup(Hashtable<Long, CPU> gPeerGroup) {
        this.gPeerGroup = gPeerGroup;
    }

    public double getGrain_const() {
        return grain_const;
    }

    public void setGrain_const(double grain_const) {
        this.grain_const = grain_const;
    }
}
