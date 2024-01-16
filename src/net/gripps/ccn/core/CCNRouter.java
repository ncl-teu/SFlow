package net.gripps.ccn.core;

import net.gripps.ccn.CCNUtil;
import net.gripps.ccn.Logger.CCNLog;
import net.gripps.ccn.breadcrumbs.BC;
import net.gripps.ccn.breadcrumbs.BaseBreadCrumbsAlgorithm;
import net.gripps.ccn.breadcrumbs.BreadCrumbsAlgorithm;
import net.gripps.ccn.breadcrumbs.NoBreadCrumbsAlgorithm;
import net.gripps.ccn.caching.BaseCachingAlgorithm;
import net.gripps.ccn.caching.NoCaching;
import net.gripps.ccn.caching.OnPathCaching;
import net.gripps.ccn.caching.OnPathPlus;
import net.gripps.ccn.icnsfc.AutoUtil;
import net.gripps.ccn.icnsfc.core.AutoEnvironment;
import net.gripps.ccn.icnsfc.fnj.PassiveTimer;
import net.gripps.ccn.icnsfc.logger.ISLog;
import net.gripps.ccn.icnsfc.process.AutoSFCMgr;
import net.gripps.ccn.icnsfc.routing.AutoRouting;
import net.gripps.ccn.process.CCNMgr;
import net.gripps.cloud.CloudUtil;
import net.gripps.cloud.core.VCPU;
import net.gripps.cloud.core.VM;
import net.gripps.cloud.nfv.sfc.SFC;
import net.gripps.cloud.nfv.sfc.VNF;
import net.gripps.clustering.common.aplmodel.CustomIDSet;
import net.gripps.clustering.common.aplmodel.DataDependence;
import net.gripps.util.CopyUtil;
import org.ncl.workflow.util.NCLWUtil;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by kanemih on 2018/11/02.
 */
public class CCNRouter extends AbstractNode {


    /**
     * ルータのID
     */
    protected Long routerID;

    /**
     * FIB
     */
    protected FIB FIBEntry;

    /**
     * PIT
     */
    protected PIT PITEntry;

    /**
     * Contents Store
     */
    protected CS CSEntry;

    /**
     * Faceのリスト（ルータ宛）
     */
    protected HashMap<Long, Face> face_routerMap;

    /**
     * Faceリスト（ノード宛）
     */
    protected HashMap<Long, Face> face_nodeMap;

    /**
     * Faceの最大保持数
     */
    protected int face_num;

    /**
     * CSの最大保持数
     */
    protected int cs_num;

    /**
     * FIBの最大保持数
     */
    protected int fib_num;

    /**
     * PITの最大保持数
     */
    protected int pit_num;

    /**
     * BreadCrumbs(パンくず）のマップ
     * (prefix, パンくず)のMap構造．
     */
    protected HashMap<String, BC> bcMap;


    /**
     * キャッシュアルゴリズムの配列
     */
    protected BaseCachingAlgorithm[] cachings;

    /**
     * 実際に使われるキャッシュアルゴリズム
     */
    protected BaseCachingAlgorithm usedCaching;

    /**
     * BreadCrumbsアルゴリズムの配列
     */
    protected BaseBreadCrumbsAlgorithm[] bcs;

    /**
     * 実際に使われるBreadCrumbsアルゴリズム
     */
    protected BaseBreadCrumbsAlgorithm usedBC;


    /**
     * 状態
     */
    protected int state;

    protected HashMap<Long, SFC> sfcMap;

    /**
     * <SFC_ID^toID, <FromID, 1>>
     */
    protected HashMap<String, HashMap<Long, Integer>> inputMap;

    /**
     * 指定のprefixでのinterestパケットが届いたかどうかのキャシュ
     */
    protected TreeSet<String> interestArrivedSet;

    /**
     * Interestの到着回数
     */
    protected long interestArriveNum;
    //protected long interesetArrivedNum;

    /**
     * 参加時刻
     */
    protected long joinTime;


    /**
     * @param routerID
     */
    public CCNRouter(Long routerID) {

        super(new LinkedBlockingQueue<InterestPacket>());
        this.vCPUMap = new HashMap<String, VCPU>();


        //this.contentsQueue = new LinkedBlockingQueue<CCNContents>();
        this.type = CCNUtil.NODETYPE_NODE;
        this.routerID = routerID;
        this.FIBEntry = new FIB();
        this.PITEntry = new PIT();
        this.CSEntry = new CS();
        this.face_routerMap = new HashMap<Long, Face>();
        this.face_nodeMap = new HashMap<Long, Face>();
        this.face_num = CCNUtil.genInt(CCNUtil.ccn_node_face_num_min, CCNUtil.ccn_node_face_num_max);
        this.cs_num = CCNUtil.genInt(CCNUtil.ccn_cs_entry_min, CCNUtil.ccn_cs_entry_max);
        this.fib_num = CCNUtil.genInt(CCNUtil.ccn_fib_entry_min, CCNUtil.ccn_fib_entry_max);
        this.pit_num = CCNUtil.genInt(CCNUtil.ccn_pit_entry_min, CCNUtil.ccn_pit_entry_max);
        this.bcMap = new HashMap<String, BC>();
        this.cachings = new BaseCachingAlgorithm[CCNUtil.ccn_caching_allnum];
        /***ここに，キャッシングアルゴリズムを列挙してください**/
        this.cachings[0] = new OnPathCaching();
        this.cachings[1] = new NoCaching();
        this.cachings[2] = new OnPathPlus();

        /***ここまで**/
        this.usedCaching = this.cachings[CCNUtil.ccn_caching_no];

        this.bcs = new BaseBreadCrumbsAlgorithm[CCNUtil.ccn_bc_allnum];
        this.bcs[0] = new NoBreadCrumbsAlgorithm();
        this.bcs[1] = new BreadCrumbsAlgorithm();

        this.usedBC = this.bcs[CCNUtil.ccn_bc_enable];
        this.state = CCNUtil.STATE_NODE_NONE;
        this.inputMap = new HashMap<String, HashMap<Long, Integer>>();
        this.sfcMap = new HashMap<Long, SFC>();
        this.interestArrivedSet = new TreeSet<String>();
        this.joinTime = System.currentTimeMillis();
        this.interestArriveNum = 0;
        //データ受信プロセスを起動
        Thread t = new Thread(this.receiver);
        t.start();
    }


    public CCNRouter() {
        super();
        Thread t = new Thread(this.receiver);
        t.start();
    }

    public long getInterestArriveNum() {
        return interestArriveNum;
    }

    public void setInterestArriveNum(long interestArriveNum) {
        this.interestArriveNum = interestArriveNum;
    }

    public long getJoinTime() {
        return joinTime;
    }

    public void setJoinTime(long joinTime) {
        this.joinTime = joinTime;
    }

    @Override
    public void run() {
        try {
            //Passiveモードの場合は，Interestによるタスク処理結果の送信スケジュールを行う．
            if(AutoUtil.fnj_checkmode == 1){
                Timer timer = new Timer();

                PassiveTimer ex = new PassiveTimer(this);
                //指定時間間隔で，ExchangeTimerを実行する．
                //各ノードで，指定ms + 0~1000ms程度のばらつきの時間間隔とする．
                timer.scheduleAtFixedRate(ex, 5000, AutoUtil.fnj_passive_duration + CCNUtil.genLong(0, 1000));

            }
            /**
             * パケットを転送するためのループ
             */
            while (true) {
                //  System.out.println("RouterID:"+this.routerID);
                Thread.sleep(100);
                if (!this.interestQueue.isEmpty()) {
                    //InterestPacketを取り出す．
                    InterestPacket p = this.interestQueue.poll();
                    //そして中身を見る．
                    this.processInterest(p);

                } else {
                    //Interestパケットがこなければ，何もしない．
                }
                if (!this.contentsQueue.isEmpty()) {
                        //コンテンツが来たら，処理
                        CCNContents c = this.contentsQueue.poll();
                    if(CCNMgr.getIns().isSFCMode()){
                        this.processInputData(c);
                    }else{
                        this.processContents(c);

                    }
                }
                if (this.state == CCNUtil.STATE_NODE_END) {
                    break;
                }
            }
            System.out.println("[Leave] Router " + this.getRouterID() + " Leaved....");
            CCNLog.getIns().log(",6," + "-" + "," + "-" + "," + "-" + "," + "-" + "," +
                    "-" + "," + "-" + "," + "-" + "," + "-" + "," + "-" + "," + "x" + "," + "-" + "," + this.getRouterID());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public VCPU findVCPU(VNF vnf){
        Iterator<VCPU> vIte = this.vCPUMap.values().iterator();
        VCPU retVCPU = null;
        while(vIte.hasNext()){
            VCPU vcpu = vIte.next();
            if (vcpu.contiansVNF(vnf)) {
                retVCPU = vcpu;
                break;
            }

        }
        return retVCPU;
    }

    public void removeFaces(HashMap<String, Face> map){
        Iterator<String> keyIte = map.keySet().iterator();
        while(keyIte.hasNext()){
            String prefix = keyIte.next();
            Face face = map.get(prefix);
            this.getPITEntry().removeFace(prefix, face);
        }

        Iterator<Face> rIte = this.face_routerMap.values().iterator();
        while(rIte.hasNext()){
            Face f = rIte.next();
            //各ルータを取得
            CCNRouter r = CCNMgr.getIns().getRouterMap().get(f.getPointerID());
        }
    }

    /**
     * SFCにおいて，入力データがやってきたときの処理．
     * Pitを見て，
     * - もし当該prefixのpitエントリがあれば，それ宛に転送する．
     * - かつ，vcpu->vnfQueueにtoVNFがあれば，入力データがそろったかどうか調べる．
     *     - もし揃えば，実行開始させる．
     * @param c
     */
    public void processInputData(CCNContents c){
        //cに含まれている，宛先タスクの情報を取
        Long jobID = AutoSFCMgr.getIns().getJobID(c.getPrefix());
        Long toID = AutoSFCMgr.getIns().getSucVNFID(c.getPrefix());
        Long fromID = AutoSFCMgr.getIns().getPredVNFID(c.getPrefix());
        SFC sfc = this.sfcMap.get(jobID);

        HashMap<String, Face> removeMap = new HashMap<String, Face>();
        if(sfc == null){
            //ルータにとって未知のジョブであれば，どっかから取ってくる．
            //interst送信時に認識しているはずだが，キャッシュとして送られた場合は，
            //その限りではない．つまり，
           //sfc = AutoSFCMgr.getIns().
            sfc = AutoSFCMgr.getIns().getSfcMap().get(jobID);
            //System.out.println();
        }
        String prefix = AutoSFCMgr.getIns().createPrefix(sfc.findVNFByLastID(fromID), sfc.findVNFByLastID(toID));


        int len = 0;
        //Pitにあれば，まずはそれを転送させる．
        if(this.getPITEntry().getTable().containsKey(prefix)){
            LinkedList<Face> fList = this.getPITEntry().getTable().get(prefix);
            len = fList.size();
            Iterator<Face> fIte = fList.iterator();
            HashMap<String, Face> removeFaceMap = new HashMap<String, Face>();
            while(fIte.hasNext()){
                Face face = fIte.next();
                if(face.getType()==CCNUtil.NODETYPE_ROUTER){
                    //ルータを探す
                    CCNRouter r = CCNMgr.getIns().getRouterMap().get(face.getPointerID());
                    //キャッシュ
                    this.cacheContents(c, prefix);
                    c.setAplID(sfc.getAplID());
                    r.forwardData(c);
                    //転送先が自分自身以外の場合のときだけ,pitを削除する．
                    if(r.getRouterID().longValue() == this.getRouterID().longValue()){
                        this.execProcess(sfc, jobID, fromID, toID, c);

                    }else{


                    }
                    removeFaceMap.put(prefix, face);
                   // this.getPITEntry().removeFace(prefix, face);

                }else{
                    CCNNode n = CCNMgr.getIns().getNodeMap().get(face.getPointerID());
                    this.cacheContents(c, prefix);
                    c.setAplID(sfc.getAplID());
                    c.setCache(true);
                    c.setFNJ(true);
//Dataの転送を実行を分けてしまっているのが問題．
                    n.forwardData(c);
                   // removeFList.add(face);
                    //this.getPITEntry().removeFace(prefix, face);
                    removeFaceMap.put(prefix, face);

                }
                removeMap.put(prefix, face);
            }
            Iterator<String> pIte = removeFaceMap.keySet().iterator();
            while(pIte.hasNext()){
                String pre = pIte.next();
                Face f = removeFaceMap.get(pre);
                this.getPITEntry().removeFace(pre, f);

            }
            //Pitから削除
            // this.getPITEntry().getTable().remove(prefix);
        }
       this.execProcess(sfc, jobID, fromID, toID,c);


    }

    public void  execProcess(SFC sfc, Long jobID, Long fromID, Long toID, CCNContents c){
        //実行対象のVNFを見る．
        VNF vnf = sfc.findVNFByLastID(toID);
        //CCNRouter router = (CCNRouter) NCLWUtil.findVM(AutoSFCMgr.getIns().getEnv(), vnf.getvCPUID());
        VCPU tVCPU = this.findVCPU(vnf);
        //実行対象でなければここで終わり．
        if(tVCPU == null){
            //System.out.println("*************No VCPU for VNF"+toID + "@"+this.routerID);
            //this.removeFaces(removeMap);
            return;
        }
        String vCPUID = tVCPU.getPrefix();
        long current = System.currentTimeMillis();
        long duration = 0;
        if(c.getHistoryList().getFirst().getCustomMap().containsKey("proctime")){
            duration = ((Long)(c.getHistoryList().getFirst().getCustomMap().get("proctime"))).longValue();

        }else{

        }
        int mode = 0;
        if(duration == 0){
            mode = 1;
        }
        long comTime = System.currentTimeMillis() - c.getHistoryList().getFirst().getStartTime();
        ISLog.getIns().log(",Data.,"+mode+","+sfc.getAplID() + ","+sfc.getSfcID() + ","+c.getPrefix()+","+duration+","+fromID+",R"+c.getHistoryList().getLast().getFromID()+"->,"+toID +"@R"+this.getRouterID() + ","+
                c.getHistoryList().size() +","+ comTime + ","+c.getSize()+","+CloudUtil.getInstance().getHostPrefix(vCPUID) + ","+ this.getVMID() + ","+vCPUID+","+"-,"+"-,"+current);


        StringBuffer buf = new StringBuffer(jobID.toString());
        buf.append("^");
        buf.append(toID.toString());
        String key = buf.toString();
        //toVNF:
        //<JobID^ToTaskID, Map<FromID, 1>>
        HashMap<Long, Integer> countMap = this.inputMap.get(key);
        //そもそもこのルータで実行すべきものなのかを判断する必要がある．

        if(this.inputMap.containsKey(key)){
            countMap.put(fromID, new Integer(1));
        }else{
            HashMap<Long, Integer> newMap = new HashMap<Long, Integer>();
            newMap.put(fromID, new Integer(1));
            this.inputMap.put(key, newMap);
            countMap = newMap;
        }
        VNF toVNF = sfc.findVNFByLastID(toID);

        if(countMap.size() >= toVNF.getDpredList().size()){

            //入力データが揃えば，readyとなる．
            //まずは，この分をクリアしておく，
            this.inputMap.remove(key);
            //そして，実行させる．
            VCPU targetVCPU = this.findVCPU(toVNF);
            if(targetVCPU == null){
                //NULLの場合は，Contentsを転送する．

            }else{
                toVNF.setAplID(sfc.getAplID());
                //FNJ問題において，ActiveCheckの処理が走る．

                targetVCPU.exec(toVNF);
                AutoEnvironment env = AutoSFCMgr.getIns().getEnv();
                VM vm = env.getGlobal_vmMap().get(targetVCPU.getVMID());
                //SFインスタンスの保存
                AutoSFCMgr.getIns().saveUpdatedSFInsNum(sfc, toVNF,vm );
                AutoSFCMgr.getIns().saveUpdatedVCPU(sfc, targetVCPU);
                AutoSFCMgr.getIns().saveUpdatedVM(sfc, vm);

                //execしたら，消す．
                this.inputMap.remove(key);
                //return targetVCPU.getPrefix();
            }
        }else{
            //this.removeFaces(removeMap);
        }


    }

    public synchronized boolean FNJ_ActiveSend(CCNContents cache){
        Iterator<String> pIte = this.FIBEntry.getTable().keySet().iterator();


        //FIBのprefixに対するループ（レコード単位）
        LinkedList<Face> faceList = new LinkedList<Face>();

        String prefix = cache.getPrefix();
        while(pIte.hasNext()){
            LinkedList<Face> fList = this.FIBEntry.getTable().get(pIte.next());
            Iterator<Face> fIte = fList.iterator();
            while(fIte.hasNext()){
                Face f = fIte.next();
                if(f.getType() == CCNUtil.NODETYPE_ROUTER){

                    CCNRouter router = CCNMgr.getIns().getRouterMap().get(f.getPointerID());
                    LinkedList<ForwardHistory> forwardList = new LinkedList<ForwardHistory>();

                    InterestPacket p = new InterestPacket(AutoUtil.FNJ_ACTIVE, new Long(-1),
                            1500, this.getRouterID(), -1,forwardList );
                    //Cacheの情報をInterestパケットにセットする．
                    p.getAppParams().put(AutoUtil.FNJ_ACTIVE, cache);
                    //Faceに向けて情報を送信する．
                    ForwardHistory newHistory = new ForwardHistory(this.getRouterID(), CCNUtil.NODETYPE_ROUTER,
                            (long)router.getRouterID(), CCNUtil.NODETYPE_ROUTER, System.currentTimeMillis(), -1);
                    p.getHistoryList().add(newHistory);
                    Face tFace = null;
                    if(!this.getFace_routerMap().containsKey(router.getRouterID())){
                        tFace = new Face(null, router.getRouterID(), CCNUtil.NODETYPE_ROUTER);
                        this.getFace_routerMap().put(router.getRouterID(), tFace);

                    }else{
                        tFace = this.getFace_routerMap().get(router.getRouterID());

                    }
                    //this.FIBEntry.setLock(false);
                    faceList.add(tFace);

                    synchronized (this.FIBEntry){
                        //FIBに登録する．
                        //this.FIBEntry.addFace(p.getPrefix(), tFace);
                    }

                    //router.addFacetoPit(p, this.getRouterID(), CCNUtil.NODETYPE_ROUTER);
                    //ルータへ転送する．
                    router.forwardInterest(tFace,p);

                }

            }

        }
        Iterator<Face> retFIte = faceList.iterator();
        while(retFIte.hasNext()){
            Face f = retFIte.next();
            this.FIBEntry.addFace(prefix, f);
        }
        return true;
    }

    public void cacheContents(CCNContents contents, String prefix){
        //キャッシュだけする．
        //System.out.println("***Chached:"+prefix+"@"+this.getRouterID());

        CCNContents copyContents = (CCNContents) contents.deepCopy();
        copyContents.setGeneratedTimeAtCache(System.currentTimeMillis());
        switch(AutoUtil.fnj_checkmode){
            case 0:
            this.FNJ_ActiveSend(copyContents);
            break;
            default:
                break;
        }

        if (this.cs_num <= this.CSEntry.getCacheMap().size()) {
            this.usedCaching.chachingIFCSFULL(copyContents, this);
        } else {
            //キャッシング処理
            this.usedCaching.cachingProcess(copyContents, this);
        }
    }

    /**
     * @param c
     */
    public void processContents(CCNContents c) {
        if (c.getHistoryList().size() >= CCNUtil.ccn_interest_ttl) {
            ForwardHistory last = c.getHistoryList().getLast();

            CCNLog.getIns().log(",x," + c.getPrefix() + "," + "-" + "," + c.getHistoryList().getFirst().getStartTime() + "," + c.getHistoryList().getLast().getArrivalTime() + "," +
                    (c.getHistoryList().getLast().getArrivalTime() - c.getHistoryList().getFirst().getStartTime()) + "," +
                    last.getToID() + "," + "-" + "," + c.getHistoryList().size() + "," + "-" + "," + "x" + "," + "-");

        } else {
            boolean ret = this.usedBC.forwardBCData(this, c);
            if (ret) {


            } else {
                if(c.isFNJ()){
                    //FNJであれば，終わり．
                    c.setFNJ(false);
                    this.cacheContents(c, c.getPrefix());
                    return;
                }
                ForwardHistory lastH = c.getHistoryList().getLast();
                lastH.setArrivalTime(System.currentTimeMillis() + this.ccn_hop_per_delay);
                //キャッシュでなければ，BCチェックをする．



                //コンテンツが来た時，PITを見る．
                if (this.getPITEntry().getTable().containsKey(c.getPrefix())) {
                    //もしPITにあれば，そのエントリ全てに送る．
                    Iterator<Face> fIte = this.getPITEntry().getTable().get(c.getPrefix()).iterator();
                    while (fIte.hasNext()) {
                        Face f = fIte.next();
                        //fのpinterIDへ送る
                        //PITのpre
                        if (f.getType() == CCNUtil.NODETYPE_NODE) {
                            //ノードへ送る準備．
                            CCNNode node = CCNMgr.getIns().getNodeMap().get(f.getPointerID());
                            //node.getContentsQueue().offer(c);
                            //転送履歴を作成して，追加
                            ForwardHistory f2 = new ForwardHistory(this.routerID, CCNUtil.NODETYPE_ROUTER, node.getNodeID(), CCNUtil.NODETYPE_NODE,
                                    System.currentTimeMillis(), -1);
                            c.getHistoryList().add(f2);
                            //BC作成処理
                            if (!c.isBC()) {
                                this.usedBC.createBC(this, c);
                            }

                            //ノードへデータを転送
                            node.forwardData(c);
                            //System.out.println("要求元ノードへデータ到着:@"+ c.getPrefix());
                        } else {

                            if (f.getPointerID() == this.getRouterID()) {
                                //PITエントリの中に，Faceのターゲットが自分であるもの
                                //System.out.println("test");

                            } else {
                                LinkedList<ForwardHistory> fList = c.getHistoryList();
                                //あて先がルータであれば，ルータに送る．
                                CCNRouter router = CCNMgr.getIns().getRouterMap().get(f.getPointerID());

                                if (router == null) {
                                    CCNLog.getIns().log(",3," + c.getPrefix() + ",-" + "," + fList.getFirst().getStartTime() + "," + fList.getLast().getArrivalTime() + "," +
                                            (fList.getLast().getArrivalTime() - fList.getFirst().getStartTime()) + "," +
                                            c.getHistoryList().getFirst().getFromID() + ",-" + "," + fList.size() + ",-" + "," + "x" + "," + "-");
                                    //return;
                                    if(CCNMgr.getIns().isSFCMode()){
                                        long current = System.currentTimeMillis();
                                        long duration = current - c.getHistoryList().getFirst().getStartTime();
                                        String cap = null;
                                        //cに含まれている，宛先タスクの情報を取
                                        Long jobID = AutoSFCMgr.getIns().getJobID(c.getPrefix());
                                        Long toID = AutoSFCMgr.getIns().getSucVNFID(c.getPrefix());
                                        Long fromID = AutoSFCMgr.getIns().getPredVNFID(c.getPrefix());
                                        SFC sfc = this.sfcMap.get(jobID);


                                        String prefix = AutoSFCMgr.getIns().createPrefix(sfc.findVNFByLastID(fromID), sfc.findVNFByLastID(toID));
                                        cap = "R";
                                        AutoEnvironment env = AutoSFCMgr.getIns().getEnv();
                                        //ここでは，転送先のルータIDを取得するのみ
                                        Long predID = AutoSFCMgr.getIns().getPredVNFID(c.getPrefix());


//System.out.println(sfc_int.getAplID() + ":Candidate VCPU for "+p.getPrefix() + ":"+vCPUID+"@"+this.getRouterID());
                                        ISLog.getIns().log(",Data,1,"+c.getPrefix() + ",0,"+sfc.getSfcID()+","+fromID+ ","+"R" +c.getHistoryList().getLast().getFromID() + "->,"+toID+",@"+this.getRouterID() +fromID + ","+
                                                c.getHistoryList().size() +","+ duration + ","+c.getSize()+","+CloudUtil.getInstance().getHostPrefix(this.getVMID()) + ","+ this.getVMID() + ",-"+","+"-,-,"+current);

                                    }
                                }
                                //転送履歴を作成して，追加
                                ForwardHistory f2 = new ForwardHistory(this.routerID, CCNUtil.NODETYPE_ROUTER, router.getRouterID(), CCNUtil.NODETYPE_ROUTER,
                                        System.currentTimeMillis(), -1);
                                c.getHistoryList().add(f2);
                                if (!c.isBC()) {
                                    this.usedBC.createBC(this, c);
                                }
                                //ルータへデータを転送
                                router.forwardData(c);
                            }

                        }
                        this.cacheContents(c, c.getPrefix());

                    }
                    //そしてPITからエントリを削除する．
                    this.getPITEntry().removeByKey(c.getPrefix());
                    //PITから指定エントリを消す．
                    //this.getPITEntry().removeFace(c.getPrefix(), lastH.fromID);
                } else {
                    CCNContents copyContents = (CCNContents) c.deepCopy();
                    //PITになければどうするか
                    //とりあえずキャッシュする？
                    if (this.cs_num <= this.CSEntry.getCacheMap().size()) {
                        this.usedCaching.chachingIFCSFULL(c, this);
                    } else {
                        this.usedCaching.chachingProcessIfNoPITEntry(copyContents, this);

                    }
                }
            }
        }


    }

    /**
     * @param id
     * @return
     */
    public boolean containsIDinRouterFaceMap(Long id) {
        boolean ret = false;
        Iterator<Face> fIte = this.getFace_routerMap().values().iterator();
        while (fIte.hasNext()) {
            Face f = fIte.next();
            // if(f.getFaceID().longValue() == id.longValue()){
            if (f.getPointerID().longValue() == id.longValue()) {
                ret = true;
                break;
            }
        }

        return ret;
    }

    /**
     * @param id
     * @return
     */
    public CCNRouter findCCNRouterFromFaceList(Long id) {
        Iterator<Face> fIte = this.face_routerMap.values().iterator();
        Face retFace;
        boolean isfound = false;
        while (fIte.hasNext()) {
            Face f = fIte.next();
            if (f.getPointerID().longValue() == id.longValue()) {
                isfound = true;
                retFace = f;
                return CCNMgr.getIns().getRouterMap().get(retFace.getPointerID());

                //break;
            }
        }
        return null;

    }

    /**
     * @param id
     * @return
     */
    public Face findFaceByID(Long id, HashMap<Long, Face> map) {
        Iterator<Face> fIte = map.values().iterator();
        Face retFace;
        boolean isfound = false;
        while (fIte.hasNext()) {
            Face f = fIte.next();
            if (f.getPointerID().longValue() == id.longValue()) {
                //見つかれば，そのfaceを返す．
                return f;
            }
        }
        return null;

    }

    /**
     * VNFの後続タスクたちへ，処理結果のデータを送る．
     * @param vnf
     */
    public void sendResultantData(VNF vnf, long duration){
        Iterator<DataDependence> dsucIte = vnf.getDsucList().iterator();
        SFC sfc = this.sfcMap.get(vnf.getIDVector().get(0));
        //もしENDタスクなら，ここで終了．
        if(vnf.getDsucList().isEmpty()){
            //pitからCCNNodeを取得する．
            //PIT全てに対して送る．
            String prefix = AutoSFCMgr.getIns().createEndPrefix(vnf);
            LinkedList<Face> pitList = this.getPITEntry().getTable().get(prefix);
            Iterator<Face> fIte = pitList.iterator();
            while(fIte.hasNext()){
                Face face = fIte.next();
                if(face.getType() == CCNUtil.NODETYPE_ROUTER){
                    //faceに該当するルータへ送る．
                    CCNRouter targetRouter = CCNMgr.getIns().getRouterMap().get(face.getPointerID());
                    // CCNContents c = new CCNContents(dsuc.getMaxDataSize(), prefix, this.getRouterID(), CCNUtil.NODETYPE_ROUTER,
                    //        System.currentTimeMillis(), -1, false);
                    //転送履歴を作成して，追加
                    ForwardHistory f2 = new ForwardHistory(this.getRouterID(), CCNUtil.NODETYPE_ROUTER, targetRouter.getRouterID(), CCNUtil.NODETYPE_ROUTER,
                            System.currentTimeMillis(), -1);
                    //f2に，処理時間を仕込んでおく．
                    f2.getCustomMap().put("proctime", new Long(duration));
                    //キャッシュだけする．
                    CCNContents c = new CCNContents(0, prefix, this.getRouterID(), CCNUtil.NODETYPE_ROUTER,
                            System.currentTimeMillis(), -1, false);
                    ///CCNContents cache = (CCNContents) c.deepCopy();
                    //cache.setGeneratedTimeAtCache(System.currentTimeMillis());
                    //キャッシュ
                    this.cacheContents(c, prefix);
                    c.setAplID(vnf.getAplID());
                    c.getHistoryList().add(f2);

                    //ルータへデータを転送
                    targetRouter.forwardData(c);

                }else{
                    //ノードが宛先
                    CCNNode node = CCNMgr.getIns().getNodeMap().get(face.getPointerID());
                    //転送履歴を作成して，追加
                    ForwardHistory f2 = new ForwardHistory(this.getRouterID(), CCNUtil.NODETYPE_ROUTER, node.getNodeID(), CCNUtil.NODETYPE_NODE,
                            System.currentTimeMillis(), -1);
                    f2.getCustomMap().put("proctime", new Long(duration));

                    //キャッシュだけする．
                    CCNContents c = new CCNContents(0, prefix, this.getRouterID(), CCNUtil.NODETYPE_ROUTER,
                            System.currentTimeMillis(), -1, false);
                    //キャッシュ
                    this.cacheContents(c, prefix);
                    c.getHistoryList().add(f2);
                    c.setAplID(vnf.getAplID());
                    //ルータへデータを転送
                    node.forwardData(c);
                    //System.out.println("***Data sent: VNF:"+vnf.getIDVector().get(1)+ "@"+this.getRouterID() + "->VNF:"+vnf.getIDVector().get(1)+"@NODE"+node.getNodeID());


                }
                this.getPITEntry().removeFace(prefix, face);


            }
        }
        while(dsucIte.hasNext()){
            DataDependence dsuc = dsucIte.next();
            VNF sucVNF = sfc.findVNFByLastID(dsuc.getToID().get(1));
            String prefix = AutoSFCMgr.getIns().createPrefix(vnf, sucVNF);
            LinkedList<Face> pitList = this.getPITEntry().getTable().get(prefix);
            //キャッシュだけする．
            CCNContents c = new CCNContents(dsuc.getMaxDataSize(), prefix, this.getRouterID(), CCNUtil.NODETYPE_ROUTER,
                    System.currentTimeMillis(), -1, false);
            c.setAplID(vnf.getAplID());
            //キャッシュ
            this.cacheContents(c, prefix);

            //pitにないということは，他で処理しているので無視．
            if(pitList == null){
                continue;
            }
            synchronized (pitList){
                //PIT全てに対して送る．
                Iterator<Face> fIte = pitList.iterator();
                while(fIte.hasNext()){
                    Face face = fIte.next();

                    //faceに該当するルータへ送る．
                    CCNRouter targetRouter = CCNMgr.getIns().getRouterMap().get(face.getPointerID());
                    // CCNContents c = new CCNContents(dsuc.getMaxDataSize(), prefix, this.getRouterID(), CCNUtil.NODETYPE_ROUTER,
                    //        System.currentTimeMillis(), -1, false);
                    //転送履歴を作成して，追加
                    ForwardHistory f2 = new ForwardHistory(this.getRouterID(), CCNUtil.NODETYPE_ROUTER, targetRouter.getRouterID(), CCNUtil.NODETYPE_ROUTER,
                            System.currentTimeMillis(), -1);
                    f2.getCustomMap().put("proctime", new Long(duration));

                    c.getHistoryList().add(f2);
                    c.setAplID(vnf.getAplID());

                    //ルータへデータを転送
                    targetRouter.forwardData(c);
                    //System.out.println("***Data sent: VNF:"+vnf.getIDVector().get(1)+ "@"+this.getRouterID() + "->VNF:"+sucVNF.getIDVector().get(1)+"@"+targetRouter.getRouterID());
                    //this.getPITEntry().removeFace(prefix, face);

                }
            }

            //そしてPITからエントリを削除する．
            this.getPITEntry().removeByKey(prefix);
        }

    }


    public boolean processSendData() {
        return true;
    }


    public CCNContents getLatestExecCache(){
        Iterator<CCNContents> cIte = this.CSEntry.getCacheMap().values().iterator();
        long MaxTime = -1;
        CCNContents ret = null;
        while(cIte.hasNext()){
            CCNContents c = cIte.next();
            if(MaxTime <= c.getGeneratedTimeAtCache()){
                MaxTime = c.getGeneratedTimeAtCache();
                ret = c;
            }
        }

        return ret;

    }

    /**
     * InrerestPacketを処理します．
     * 1. CSをみて，コンテンツがないかチェックする．
     * 2. もし一致するPrefixがあれば，データとして返す．なければ，PITを見る．
     * 3. もし一致するPrefixがPITにあれば，PITエントリにFace情報を追加する．なければ何もしない
     * 4. FIBを見て，一致するPrefixがあれば，対応するFaceにかかれてあるあて先へ転送する．
     *
     * @param p
     */
    public void processInterest(InterestPacket p) {
        ForwardHistory h = p.getHistoryList().getLast();
        SFC sfc = (SFC)p.getAppParams().get(AutoUtil.SFC_NAME);
        if(sfc != null){
            AutoSFCMgr.getIns().saveStartTime(p, sfc);
        }


        //とりあえずhの到着時刻を設定
        h.setArrivalTime(System.currentTimeMillis() + this.ccn_hop_per_delay);
        Long toID = h.getFromID();
        int toType = h.getFromType();
        p.setCount(p.getCount() + 1);
        //ココに，BCの処理を入れる．
        //もしfalseの場合のみ，↓の処理へ移行する．
        boolean retBC = this.usedBC.forwardRequestByBC(this, p);
        if (retBC) {
            return;
        }
        LinkedList<ForwardHistory> fList = p.getHistoryList();
        //事前処理
        /////////FNJ問題への対処////////////
        if(p.getPrefix().equals(AutoUtil.FNJ_ACTIVE)){
            //パケットを取り出す．
            CCNContents contents = (CCNContents)p.getAppParams().get(AutoUtil.FNJ_ACTIVE);
            //送信元ルータID
            Long srcID = p.getFromNodeId();
            //そしてコンテンツをキャッシュする．
            this.CSEntry.getCacheMap().put(contents.getPrefix(), contents);

            Face srcFace = null;
            //そしてFIBに登録する．
            if(this.containsIDinRouterFaceMap(srcID)){
                srcFace = this.findFaceByID(srcID, this.face_routerMap);

            }else{
                srcFace = new Face(null, toID, toType);
                this.addFace(srcFace, this.face_routerMap);

            }
            //FIBに追加する．
            this.FIBEntry.addFace(contents.getPrefix(), srcFace);
            return;


        }else if(p.getPrefix().equals(AutoUtil.FNJ_PASSIVE)){
            //パケットを取り出す．
            //CCNContents contents = (CCNContents)p.getAppParams().get(AutoUtil.FNJ_PASSIVE);
            //送信元ルータID
            Long srcID = p.getFromNodeId();
            //そしてコンテンツをキャッシュする．
           // this.CSEntry.getCacheMap().put(contents.getPrefix(), contents);
            Face srcFace = null;
            //まずはmapに登録．
            if(this.containsIDinRouterFaceMap(srcID)){
                srcFace = this.findFaceByID(srcID, this.face_routerMap);

            }else{
                srcFace = new Face(null, toID, toType);
                this.addFace(srcFace, this.face_routerMap);

            }
            //自分が持っているContentsを送信する．
            CCNContents c = this.getLatestExecCache();
            if(c == null){
                return;
            }else{
                //あれば，Contentsを返す処理を行う．
                c.getHistoryList().clear();

                CCNRouter r = CCNMgr.getIns().getRouterMap().get(srcFace.getPointerID());

                // System.out.println("<CS内キャッシュを要求元ルータへ返送> from " + "Router" + this.routerID + "-->" + "Router" + toID + " for prefix:" + p.getPrefix());
                ForwardHistory f = new ForwardHistory(this.routerID, CCNUtil.NODETYPE_ROUTER, r.getRouterID(), CCNUtil.NODETYPE_ROUTER,
                        System.currentTimeMillis(), -1);
                f.getCustomMap().put("proctime", new Long(0));
                c.getHistoryList().add(f);

                r.forwardData(c);

            }

            //FIBに追加する．
           // this.FIBEntry.addFace(contents.getPrefix(), srcFace);
            return;
        }else if(p.getPrefix().equals(AutoUtil.FNJ_HYBRID)){

        }else{
            //何もしない
        }

        ////////FNJ問題END ///////////////

        //もしCSにあれば，データを返す．
        //  if(!CCNMgr.getIns().isSFCMode()){
        if (this.CSEntry.getCacheMap().containsKey(p.getPrefix())) {
            //System.out.println( ":CacheHit  VCPU for "+p.getPrefix()+"@"+this.getRouterID());

            CCNContents c_org = this.CSEntry.getCacheMap().get(p.getPrefix());
            CCNContents c = (CCNContents)c_org.deepCopy();

            c.setCache(true);
            if(CCNMgr.own.isSFCMode()){
                c.setAplID(sfc.getAplID());

            }

            //最新の履歴を見て，送信もとを特定する．
            if (h.getFromType() == CCNUtil.NODETYPE_ROUTER) {
                //ルータならば，ルータへCCNContentsを送る．
                CCNRouter r = findCCNRouterFromFaceList(toID);

                if (r == null) {
                    if(toID == this.getRouterID()){
                        r = this;
                    }else{
                        CCNLog.getIns().log(",1," + p.getPrefix() + ",-" + "," + fList.getFirst().getStartTime() + "," + fList.getLast().getArrivalTime() + "," +
                                (fList.getLast().getArrivalTime() - fList.getFirst().getStartTime()) + "," +
                                p.getHistoryList().getFirst().getFromID() + ",-" + "," + fList.size() + ",-" + "," + "x" + "," + "-");
                        return;
                    }

                }
                c.getHistoryList().clear();
                // CCNContents c = this.CSEntry.getCacheMap().get(p.getPrefix());
                //後は送信処理だが，分割して送る？
                // System.out.println("<CS内キャッシュを要求元ルータへ返送> from " + "Router" + this.routerID + "-->" + "Router" + toID + " for prefix:" + p.getPrefix());
                ForwardHistory f = new ForwardHistory(this.routerID, CCNUtil.NODETYPE_ROUTER, r.getRouterID(), CCNUtil.NODETYPE_ROUTER,
                        System.currentTimeMillis(), -1);
                f.getCustomMap().put("proctime", new Long(0));
                c.getHistoryList().add(f);

                r.forwardData(c);
                //this.getPITEntry().removeByKey(p.getPrefix());
                CCNLog.getIns().log(",13," + p.getPrefix() + ",-" + "," + fList.getFirst().getStartTime() + "," + fList.getLast().getArrivalTime() + "," +
                        (fList.getLast().getArrivalTime() - fList.getFirst().getStartTime()) + "," +
                        p.getHistoryList().getFirst().getFromID() + "," + this.getRouterID() + "," + fList.size() + ",-" + "," + "o" + "," + "-");

                if(CCNMgr.getIns().isSFCMode()){
                    long current = System.currentTimeMillis();
                    long duration = current - p.getHistoryList().getFirst().getStartTime();
                    String cap = null;
                    if(toType == CCNUtil.NODETYPE_NODE){
                        cap = "N";

                    }else{
                        cap = "R";
                    }
                    AutoEnvironment env = AutoSFCMgr.getIns().getEnv();
                    //ここでは，転送先のルータIDを取得するのみ
                    AutoRouting auto = (AutoRouting) this.usedRouting;
                    String vCPUID = auto.findNextRouter(p, this);
                    Long predID = AutoSFCMgr.getIns().getPredVNFID(p.getPrefix());
                    SFC sfc_int = (SFC) p.getAppParams().get(AutoUtil.SFC_NAME);
                    VNF predVNF = sfc_int.findVNFByLastID(predID);
//キャッシュヒット
//System.out.println(sfc_int.getAplID() + ":Candidate VCPU for "+p.getPrefix() + ":"+vCPUID+"@"+this.getRouterID());
                    CCNRouter router = (CCNRouter) NCLWUtil.findVM(AutoSFCMgr.getIns().getEnv(), vCPUID);
                    ISLog.getIns().log(",Int.,1,"+sfc_int.getAplID() + "," + sfc_int.getSfcID()+","+p.getPrefix()+","+predID+"@R"+this.getRouterID()+","+toID +",<-" + cap + fList.getLast().getFromID() + ","+
                            p.getHistoryList().size() +","+ 0 + ","+CloudUtil.getInstance().getHostPrefix(vCPUID) + ","+ this.getVMID() + ","+vCPUID+","+current);
                    //保存
                    AutoSFCMgr.getIns().saveUpdatedCacheHitNum(sfc_int, 1);
                }



            } else {
                // System.out.println("<CS内キャッシュを要求元ノードへ返送> from " + "Router" + this.routerID + "-->" + "Node" + toID + " for prefix:" + p.getPrefix());
                CCNNode n = CCNMgr.getIns().getNodeMap().get(toID);
                //System.out.println("要求元ノードへデータ到着:@"+ c.getPrefix());
                ForwardHistory f = new ForwardHistory(this.routerID, CCNUtil.NODETYPE_ROUTER, n.getNodeID(), CCNUtil.NODETYPE_NODE,
                        System.currentTimeMillis(), -1);
                c.getHistoryList().add(f);
                n.forwardData(c);
                CCNLog.getIns().log(",13," + p.getPrefix() + ",-" + "," + fList.getFirst().getStartTime() + "," + fList.getLast().getArrivalTime() + "," +
                        (fList.getLast().getArrivalTime() - fList.getFirst().getStartTime()) + "," +
                        p.getHistoryList().getFirst().getFromID() + "," + this.getRouterID() + "," + fList.size() + ",-" + "," + "o" + "," + "-");

                if(CCNMgr.getIns().isSFCMode()){
                    long current = System.currentTimeMillis();
                    long duration = current - p.getHistoryList().getFirst().getStartTime();
                    String cap = null;
                    if(toType == CCNUtil.NODETYPE_NODE){
                        cap = "N";

                    }else{
                        cap = "R";
                    }
                    AutoEnvironment env = AutoSFCMgr.getIns().getEnv();
                    //ここでは，転送先のルータIDを取得するのみ
                    AutoRouting auto = (AutoRouting) this.usedRouting;
                    String vCPUID = auto.findNextRouter(p, this);
                    Long predID = AutoSFCMgr.getIns().getPredVNFID(p.getPrefix());
                    Long sucID = AutoSFCMgr.getIns().getSucVNFID(p.getPrefix());
                    SFC sfc_int = (SFC) p.getAppParams().get(AutoUtil.SFC_NAME);
                    VNF predVNF = sfc_int.findVNFByLastID(predID);

//System.out.println(sfc_int.getAplID() + ":Candidate VCPU for "+p.getPrefix() + ":"+vCPUID+"@"+this.getRouterID());
                    CCNRouter router = (CCNRouter) NCLWUtil.findVM(AutoSFCMgr.getIns().getEnv(), vCPUID);
                    ISLog.getIns().log(",Int.,1,"+sfc_int.getAplID() + ","+sfc_int.getSfcID()+","+p.getPrefix()+","+predID+"@R"+this.getRouterID()+","+sucID +",<-" + cap + fList.getLast().getFromID() + ","+
                            p.getHistoryList().size() +","+ 0 + ","+CloudUtil.getInstance().getHostPrefix(vCPUID) + ","+ this.getVMID() + ","+vCPUID+","+current);
                    AutoSFCMgr.getIns().saveUpdatedCacheHitNum(sfc_int, 1);

                }


            }
            //自身のprocessContents
            //this.processContents(c);
        } else {
            //CSになければ，PITに追加する．
            boolean isNotFound = false;
            //PITへの反映
            this.addFacetoPit(p, toID, toType);
            //System.out.println("PIT ADD:"+p.getPrefix());
            //SFCモードであれば，別の処理を行う．
            if (CCNMgr.getIns().isSFCMode()) {
                //当該VNF宛のinterestが過去に来たことがあれば，先行VNF宛のinterestは送らない．
                /*Long job_id = AutoSFCMgr.getIns().getJobID(p.getPrefix());
                Long fromVNFID = AutoSFCMgr.getIns().getPredVNFID(p.getPrefix());
                StringBuffer buf = new StringBuffer(String.valueOf(job_id));
                buf.append("^");
                buf.append(String.valueOf(fromVNFID));
                String job_vnf = buf.toString();
*/

                if(this.interestArrivedSet.contains(p.getPrefix())){
                    //System.out.println("NG:"+p.getPrefix()+"@"+this.getRouterID());
                    //return;
                }else{
                    this.interestArrivedSet.add(p.getPrefix());
                }
                this.interestArrivedSet.add(p.getPrefix());
                AutoEnvironment env = AutoSFCMgr.getIns().getEnv();
                //ここでは，転送先のルータIDを取得するのみ
                AutoRouting auto = (AutoRouting) this.usedRouting;
                String vCPUID = auto.findNextRouter(p, this);
                SFC sfc_int = (SFC) p.getAppParams().get(AutoUtil.SFC_NAME);
//System.out.println(sfc_int.getAplID() + ":Candidate VCPU for "+p.getPrefix() + ":"+vCPUID+"@"+this.getRouterID());
                CCNRouter router = (CCNRouter) NCLWUtil.findVM(AutoSFCMgr.getIns().getEnv(), vCPUID);
                //結局，この時点でp->SFCとr->SFCは同じものを指していることになる．

                //SFC sfc_router = this.getSfcMap().get(sfc_int.getSfcID());
                //もし同一であれば，自分で実行する．かつ，SFCの当該タスクの割当先を自分に設定する．
                if (this.getRouterID().equals(router.getRouterID())||(this.containsSameRouter(router.getRouterID(), p))) {
                    //System.out.println(sfc_int.getAplID() + ":Fixed  VCPU for "+p.getPrefix() + ":"+vCPUID+"@"+this.getRouterID() + "From:"+toID);
                    Long predID = AutoSFCMgr.getIns().getPredVNFID(p.getPrefix());
                    VNF predVNF = sfc_int.findVNFByLastID(predID);
                    Long sucID = AutoSFCMgr.getIns().getSucVNFID(p.getPrefix());
                    //この時点で，p->predVNF == r->PredVNFである．
                    predVNF.setvCPUID(vCPUID);
                    //   predVNF_r.setvCPUID(vCPUID);
                    //そして，当該VCPUにてpredVNFを割り当てる.
                    VCPU vcpu = env.getGlobal_vcpuMap().get(vCPUID);
                    //vCPUのキューへ追加する．
                    vcpu.getVnfQueue().offer(predVNF);
                    long current = System.currentTimeMillis();
                    long duration = current - p.getHistoryList().getFirst().getStartTime();
                    String cap = null;
                    if(toType == CCNUtil.NODETYPE_NODE){
                        cap = "N";

                    }else{
                        cap = "R";
                    }
                    ISLog.getIns().log(",Int.,0,"+sfc_int.getAplID() + ","+sfc_int.getSfcID()+","+p.getPrefix()+","+predID+"@R"+this.getRouterID()+","+sucID +",<-" + cap + fList.getLast().getFromID() + ","+
                            p.getHistoryList().size() +","+ duration + ","+CloudUtil.getInstance().getHostPrefix(vCPUID) + ","+ this.getVMID() + ","+vCPUID+","+current);
                    //FIBに登録する．
                    //this.getFIBEntry().addFace(p.getPrefix(), )
                    //PredVNFがstartであれば，実行する．
                    //ちょっとまって，すべての後続からのinterestが届いてから？
                    //他にも同一startタスクを実行するノードがあるので，ここでは同期はとれない．
                    //interest送信側，つまり後続タスク側にて，「ccncontentsが来て，
                    if(predVNF.getDpredList().isEmpty()){
                        //vcpuのキューから取り出して実行させる．
                        VCPU startVCPU =  env.getGlobal_vcpuMap().get(predVNF.getvCPUID());
//System.out.println(sfc_int.getAplID() + ":START  VCPU for "+p.getPrefix() + ":"+vCPUID+"@"+this.getRouterID());
                        predVNF.setAplID(sfc.getAplID());
                        //処理をさせる．
                        startVCPU.exec(predVNF);
                        VM vm = env.getGlobal_vmMap().get(startVCPU.getVMID());
                        //SFインスタンスの保存
                        AutoSFCMgr.getIns().saveUpdatedSFInsNum(sfc_int, predVNF,vm );
                        AutoSFCMgr.getIns().saveUpdatedVCPU(sfc_int, startVCPU);
                        AutoSFCMgr.getIns().saveUpdatedVM(sfc_int, vm);

                    }else{
                        //そして，predVNFのVNFへInterestパケットを投げる．
                        //先行タスク用のprefixを生成する．
                        Iterator<DataDependence> dpredIte = predVNF.getDpredList().iterator();
                        while(dpredIte.hasNext()){
                            DataDependence dpred = dpredIte.next();
                            VNF ppVNF = sfc_int.findVNFByLastID(dpred.getFromID().get(1));
                            String prefix = AutoSFCMgr.getIns().createPrefix(ppVNF, predVNF);
                            //転送先を決める．

                            ForwardHistory newHistory = new ForwardHistory(this.getRouterID(), CCNUtil.NODETYPE_ROUTER,
                                    (long)-1, -1, System.currentTimeMillis(), -1);
                            LinkedList<ForwardHistory> newFList = new LinkedList<ForwardHistory>();
                            newFList.add(newHistory);
                            InterestPacket newInterest = new InterestPacket(prefix, p.getCount()+1,
                                    1500, this.getRouterID(), p.getCount()+1, newFList);

                            newInterest.getAppParams().put(AutoUtil.SFC_NAME, sfc_int);
                            //this.getInterestQueue().add(newInterest);
                            //転送先を決める．かならずルータになる．
                            String newVCPUPrefix = auto.findNextRouter(newInterest, this);
                            //targetルータのIDを取得する．
                            CCNRouter nextRouter = (CCNRouter) NCLWUtil.findVM(AutoSFCMgr.getIns().getEnv(), newVCPUPrefix);

                            //履歴情報の更新．
                            newHistory.setToID(nextRouter.getRouterID());
                            newHistory.setToType(CCNUtil.NODETYPE_ROUTER);

                            nextRouter.getInterestQueue().add(newInterest);


                        }

                    }


                } else {
                    //転送モードのとき
                    //historyに加えてから，宛先へ転送する．
                    ForwardHistory newHistory = new ForwardHistory(this.getRouterID(), CCNUtil.NODETYPE_ROUTER,
                            (long)router.getRouterID(), CCNUtil.NODETYPE_ROUTER, System.currentTimeMillis(), -1);
                    p.getHistoryList().add(newHistory);
                    Face tFace = null;
                    if(!this.getFace_routerMap().containsKey(router.getRouterID())){
                        tFace = new Face(null, router.getRouterID(), CCNUtil.NODETYPE_ROUTER);
                        this.getFace_routerMap().put(router.getRouterID(), tFace);
                    }else{
                        tFace = this.getFace_routerMap().get(router.getRouterID());

                    }
                    //FIBに登録する．
                    this.getFIBEntry().addFace(p.getPrefix(), tFace);
                    //tFaceを
                    //this.addFacetoPit(p, router.getRouterID(), CCNUtil.NODETYPE_ROUTER);
                    router.addFacetoPit(p, this.getRouterID(), CCNUtil.NODETYPE_ROUTER);

                    router.forwardInterest(tFace,p);
                    //System.out.println("****FORWARDED: "+this.routerID + "->"+router.getRouterID() + "for "+p.getPrefix());


                }


            } else {
                //以降は，FIBに対する操作を行う．FIBを見て，prefixがあれば，そのFaceのpointerへ転送する．
                if (this.FIBEntry.getTable().containsKey(p.getPrefix())) {
                    //もしあれば，faceを見る．
                    LinkedList<Face> fList2 = this.FIBEntry.getTable().get(p.getPrefix());
                    Iterator<Face> fIte = fList2.iterator();
                    LinkedList<Face> rList = new LinkedList<Face>();

                    //該当エントリに対するループ
                    while (fIte.hasNext()) {
                        //当該エントリのfaceList分にだけ，転送する．
                        Face f = fIte.next();
                        if (f.getType() == CCNUtil.NODETYPE_ROUTER) {
                            rList.add(f);
                        }
                        //とりあえず．先頭をみる．
                        // Face f = fList.getFirst();
                        //Faceとpを使って，Interestを転送する．
                        this.forwardInterest(f, p);
                    }

                } else {
                    //Faceから，Prefixのハッシュに近いものを選択する．
                    Long routerID = this.usedRouting.addFaceToFIBAsNewEntry(p.getPrefix(), this);

                    Face face2 = this.findFaceByID(routerID, this.getFace_routerMap());
                    if (face2 == null) {
                        face2 = this.findFaceByID(routerID, this.getFace_nodeMap());
                    }
                    if (face2 == null) {
                        //System.out.println("NULL");
                    } else {
                        this.forwardInterest(face2, p);

                    }

                }
            }

            // }else{
            //FIBにもなければ，破棄する．

            //     }
        }


    }

    public boolean containsSameRouter(Long routerID, InterestPacket p){
        Iterator<ForwardHistory> hIte = p.getHistoryList().iterator();
        boolean ret = false;
        while(hIte.hasNext()){
            ForwardHistory f = hIte.next();
            if((routerID.longValue() == f.getToID().longValue())&&(f.getToType()== CCNUtil.NODETYPE_ROUTER) ){
                ret = true;
                break;
            }
        }
        return ret;
    }



    public boolean isInterestForwardable(InterestPacket p, Long toID, int type) {
        boolean ret = true;
        if (p.getHistoryList().size() >= CCNUtil.ccn_interest_ttl) {
            ret = false;
        } else {

        }
        /**
         Iterator<ForwardHistory> dIte = p.getHistoryList().iterator();
         while(dIte.hasNext()){
         ForwardHistory h = dIte.next();
         //もし過去に同じ宛先であれば，false
         if((h.getToID().longValue() == toID.longValue())&&(h.getToType() == type)){
         ret = false;
         break;

         }
         }**/
        return ret;

    }

    /**
     * @param prefix
     * @param f
     * @return
     */
    public boolean isFaceContains(String prefix, Face f) {
        boolean ret = false;
        if (this.getFIBEntry().getTable().containsKey(prefix)) {
            LinkedList<Face> fList = this.getFIBEntry().getTable().get(prefix);
            Iterator<Face> fIte = fList.iterator();
            while (fIte.hasNext()) {
                Face rf = fIte.next();
                if ((rf.getPointerID() == f.getPointerID()) && (rf.getType() == f.getType())) {
                    ret = true;
                    break;
                }
            }

        } else {
            return false;
        }

        return ret;
    }

    public boolean isIntestLooped(InterestPacket p, Long toID, int type) {
        boolean ret = false;

        Iterator<ForwardHistory> dIte = p.getHistoryList().iterator();
        while (dIte.hasNext()) {
            ForwardHistory h = dIte.next();
            //もし過去に同じ宛先であれば，false
            if ((h.getToID().longValue() == toID.longValue()) && (h.getToType() == type)) {
                ret = true;
                break;
            }
        }
        return ret;
    }

    /**
     * @param f
     * @param p
     * @return
     */
    public boolean forwardInterest(Face f, InterestPacket p) {

        //Interestパケットが指定のあて先(f.getPointerID)へ転送可能かどうかのチェック
        //TTL以内の場合
        if (isInterestForwardable(p, f.getPointerID(), f.getType())) {
            //転送可能であれば，転送する．
            if (f.getType() == CCNUtil.NODETYPE_ROUTER) {
                long retID = -1L;
                //Interestパケットがループしそうなら，ルーティングアルゴリズムに任せる．
                if (this.usedRouting.getRouterMap() == null) {
                    this.usedRouting.setRouterMap(CCNMgr.getIns().getRouterMap());
                }
                if (this.isIntestLooped(p, f.getPointerID(), f.getType())) {
                    if(CCNMgr.getIns().isSFCMode()){
                        retID = this.getRouterID();

                    }else{
                        retID = this.usedRouting.getNextRouterIDIfInterestLoop(p, f, this);

                    }
                } else {
                    retID = f.getPointerID();
                }

                //もしルータなら，ルータへ送る．
                CCNRouter r = CCNMgr.getIns().getRouterMap().get(retID);
                LinkedList<ForwardHistory> fList = p.getHistoryList();
                if (r == null) {
                    CCNLog.getIns().log(",1," + p.getPrefix() + ",-" + "," + fList.getFirst().getStartTime() + "," + fList.getLast().getArrivalTime() + "," +
                            (fList.getLast().getArrivalTime() - fList.getFirst().getStartTime()) + "," +
                            p.getHistoryList().getFirst().getFromID() + ",-" + "," + fList.size() + ",-" + "," + "x" + "," + "-");
                    return false;
                }
                //Interestを追記する．
                ForwardHistory newHistory = new ForwardHistory(this.routerID, CCNUtil.NODETYPE_ROUTER, r.getRouterID(), CCNUtil.NODETYPE_ROUTER,
                        System.currentTimeMillis(), -1);
                long minBW = Math.min(Math.min(this.getBw(), r.getBw()), p.getMinBW());
                p.setMinBW(minBW);

                p.getHistoryList().add(newHistory);
                //System.out.println("【Interest:"+((ChordDHTRouting)this.usedRouting).calcHashCode(p.getPrefix().hashCode())+" をルータ->ルータへ転送】FromID:"+this.routerID+"toID:"+f.getPointerID());

                r.getInterestQueue().offer(p);
            } else {
                // System.out.println("【Interestがルータ->コンテンツ保持ノードへ転送】FromID:"+this.routerID+"toID:"+f.getPointerID());

                CCNNode n = CCNMgr.getIns().getNodeMap().get(f.getPointerID());
                ForwardHistory newHistory = new ForwardHistory(this.routerID, CCNUtil.NODETYPE_ROUTER, n.getNodeID(), CCNUtil.NODETYPE_NODE,
                        System.currentTimeMillis(), -1);
                p.getHistoryList().add(newHistory);
                long minBW = Math.min(Math.min(this.getBw(), n.getBw()), p.getMinBW());
                p.setMinBW(minBW);
                n.getInterestQueue().offer(p);
            }
        } else {
            //何もせず，breakする．
            //破棄する．
            ForwardHistory last = p.getHistoryList().getLast();

            CCNLog.getIns().log(",x," + p.getPrefix() + "," + "-" + "," + p.getHistoryList().getFirst().getStartTime() + "," + p.getHistoryList().getLast().getArrivalTime() + "," +
                    (p.getHistoryList().getLast().getArrivalTime() - p.getHistoryList().getFirst().getStartTime()) + "," +
                    last.getToID() + "," + "-" + "," + p.getHistoryList().size() + "," + "-" + "," + "x" + "," + "-");
            // System.out.println("【Dropped at FIB】fromID:" + f.getPointerID() + "@Router " + this.routerID);
        }


        return true;
    }

    public boolean addFacetoPit(InterestPacket p, Long toID, int toType) {
        boolean isNotFound = true;
        boolean ret = true;
        //CSになければ，PITを見る．
        //もしあれば，faceListへ追加する．
        HashMap<String, LinkedList<Face>> pitTable = this.PITEntry.getTable();
        Face f;
        /*
        if(pitTable.containsKey(p.getPrefix())){
            //pitにprefixがすでにあれば，faceを追加する．
            //そこで，faceを持っているか探す．
            if (toType == CCNUtil.NODETYPE_ROUTER) {
                f = this.findFaceByID(toID, this.face_routerMap);
            } else {
                f = this.findFaceByID(toID, this.face_nodeMap);
            }
            if(f != null){
                //もしtoIDに該当するfaceを持っていれば，pitTableへ追加
            }else{
                f = new Face(null, toID, toType);
                //もしtoIDに該当するFaceを持っていなければ，新規作成
            }
            //PITへFaceを追加する
            this.getPITEntry().addFace(p.getPrefix(), f);

        }else{
            //prefixがない場合，
        }*/
        if (this.pit_num <= this.getPITEntry().getTable().size()) {

            return false;
        }
        if (toType == CCNUtil.NODETYPE_ROUTER) {
            f = this.findFaceByID(toID, this.face_routerMap);
        } else {
            f = this.findFaceByID(toID, this.face_nodeMap);
        }
        if (f != null) {
            //もしtoIDに該当するfaceを持っていれば，pitTableへ追加
        } else {
            f = new Face(null, toID, toType);
            //もしtoIDに該当するFaceを持っていなければ，新規作成
        }
        //PITへFaceを追加する
        //this.getPITEntry().addFace(p.getPrefix(), f);
        //FaceをPITへ追加する
        //System.out.println("【PITへ追加】@"+this.routerID+":Prefix:"+p.getPrefix());
        this.PITEntry.addFace(p.getPrefix(), f);
        //fList.add(f);
        //  isNotFound = false;
        //   }else{
        //もしなければ，追加するのみ．

        // isNotFound = true;
        //   }
        // return isNotFound;
        return true;
    }


    /**
     * IDが付与されていないFaceを追加します．
     * 単に，ルータのFaceListにFaceを追加するのみ．
     *
     * @param f
     */
    public Long addFace(Face f, HashMap<Long, Face> map) {
        if (this.face_num <= this.face_routerMap.size() + this.face_nodeMap.size()) {
            return CCNUtil.MINUS_VAUE;
        }
        if ((f.getPointerID() == this.getRouterID()) && (f.getType() == CCNUtil.NODETYPE_ROUTER)) {
            return CCNUtil.MINUS_VAUE;
        }
        Iterator<Long> keyIte = map.keySet().iterator();
        long maxValue = 0;
        boolean isExist = false;
        while (keyIte.hasNext()) {
            Long id = keyIte.next();
            if (maxValue <= id) {
                maxValue = id;
            }
            Face orgFace = map.get(id);
            //同じものが存在すれば，抜ける．
            if ((f.getPointerID().longValue() == orgFace.getPointerID().longValue()) &&
                    (f.getType() == orgFace.getType())) {
                isExist = true;
                break;
            }
        }
        if (map.size() >= this.face_num) {
            return CCNUtil.MINUS_VAUE;
        }

        if (isExist) {
            return CCNUtil.MINUS_VAUE;
        } else {
            Long newValue = new Long(maxValue + 1);
            f.setFaceID(newValue);
            //新しい値をputする．
            map.put(newValue, f);
            return newValue;
        }
    }

    public boolean sendInterest(InterestPacket p) {
        this.interestQueue.offer(p);
        return true;
    }

    public LinkedBlockingQueue<CCNContents> getContentsQueue() {
        return contentsQueue;
    }

    public void setContentsQueue(LinkedBlockingQueue<CCNContents> contentsQueue) {
        this.contentsQueue = contentsQueue;
    }

    public HashMap<Long, Face> getFace_routerMap() {
        return face_routerMap;
    }

    public void setFace_routerMap(HashMap<Long, Face> face_routerMap) {
        this.face_routerMap = face_routerMap;
    }

    public HashMap<Long, Face> getFace_nodeMap() {
        return face_nodeMap;
    }

    public void setFace_nodeMap(HashMap<Long, Face> face_nodeMap) {
        this.face_nodeMap = face_nodeMap;
    }

    public Long getRouterID() {
        return routerID;
    }

    public void setRouterID(Long routerID) {
        this.routerID = routerID;
    }

    public FIB getFIBEntry() {
        return FIBEntry;
    }

    public void setFIBEntry(FIB FIBEntry) {
        this.FIBEntry = FIBEntry;
    }

    public PIT getPITEntry() {
        return PITEntry;
    }

    public void setPITEntry(PIT PITEntry) {
        this.PITEntry = PITEntry;
    }

    public CS getCSEntry() {
        return CSEntry;
    }

    public void setCSEntry(CS CSEntry) {
        this.CSEntry = CSEntry;
    }

    public int getCs_num() {
        return cs_num;
    }

    public void setCs_num(int cs_num) {
        this.cs_num = cs_num;
    }

    public int getFib_num() {
        return fib_num;
    }

    public void setFib_num(int fib_num) {
        this.fib_num = fib_num;
    }

    public int getPit_num() {
        return pit_num;
    }

    public void setPit_num(int pit_num) {
        this.pit_num = pit_num;
    }

    public HashMap<String, BC> getBcMap() {
        return bcMap;
    }

    public void setBcMap(HashMap<String, BC> bcMap) {
        this.bcMap = bcMap;
    }


    public int getFace_num() {
        return face_num;
    }

    public void setFace_num(int face_num) {
        this.face_num = face_num;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public HashMap<Long, SFC> getSfcMap() {
        return sfcMap;
    }

    public void setSfcMap(HashMap<Long, SFC> sfcMap) {
        this.sfcMap = sfcMap;
    }

    public HashMap<String , HashMap<Long, Integer>> getInputMap() {
        return inputMap;
    }

    public void setInputMap(HashMap<String, HashMap<Long, Integer>> inputMap) {
        this.inputMap = inputMap;
    }
}
