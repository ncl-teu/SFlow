package net.gripps.ccn.core;

import net.gripps.ccn.CCNUtil;
import net.gripps.ccn.Logger.CCNLog;
import net.gripps.ccn.icnsfc.AutoUtil;
import net.gripps.ccn.icnsfc.core.AutoEnvironment;
import net.gripps.ccn.icnsfc.process.AutoSFCMgr;
import net.gripps.ccn.process.CCNMgr;
import net.gripps.cloud.core.ComputeHost;
import net.gripps.cloud.nfv.sfc.SFC;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Hidehiro Kanemitsu on 2018/11/12.
 */
public class CCNNode extends AbstractNode {


    /**
     * ノードID
     */
    protected Long nodeID;

    /**
     * <名前, コンテンツ>で保持された，最初から持っているコンテンツMap
     */
    protected HashMap<String, CCNContents> ownContentsMap;

    /**
     * 取得したコンテンツを格納するMap
     */
    //protected LinkedList< CCNContents> obtainedContentsMap;

    /**
     * 当該ノードからアクセス可能なCCNルータの集合
     */
    protected HashMap<Long, CCNRouter> routerMap;

    /**
     * 取得するコンテンツ数の最大値
     */
    protected int  max_num_request_contents;

    /**
     * 状態
     */
     protected  int state;



    /**
     * 保留時間
     */
    protected double lambda;


    /**
     * インタレストパケットを保持するためのキュー（Faceではなくて，開始時に必要）
     * つまり，送信用インタレストパケットのキュー
     */
    protected LinkedBlockingQueue<InterestPacket> start_interestQueue;

    /**
     * FIB
     */
    protected FIB FIBEntry;


    public CCNNode( Long nodeID) {
        super(new LinkedBlockingQueue<InterestPacket>());
        this.type = CCNUtil.NODETYPE_NODE;
        this.nodeID = nodeID;
        this.FIBEntry = new FIB();

        this.ownContentsMap = new HashMap<String, CCNContents>();
        //this.obtainedContentsMap = new LinkedList< CCNContents>();
        this.routerMap = new HashMap<Long, CCNRouter>();
        this.max_num_request_contents = CCNUtil.ccn_node_request_num;
        this.state = CCNUtil.STATE_NODE_NONE;
        this.lambda = CCNUtil.genDouble(CCNUtil.ccn_request_exp_dist_lambda_min, CCNUtil.ccn_request_exp_dist_lambda_max);;
        this.start_interestQueue = new LinkedBlockingQueue<InterestPacket>();
        //受信プロセス開始
        Thread t = new Thread(this.receiver);
        t.start();
    }

    /**
     * オリジナルコンテンツを保持するノードであり，コンテンツを要求するノードでもある．
     * ある一定確率のもとで，コンテンツ要求，及びコンテンツ配信を行う．
     */
    @Override
    public void run() {
        long start = System.currentTimeMillis();
        //要求数 = スタートキューの要素数とする．
        this.max_num_request_contents = this.start_interestQueue.size();
        //取得最大数に達するまでのループ
        while(true){
            try{
                Thread.sleep(100);
                //まだ，要求したコンテンツ全ては取得していない場合
                if(this.contentsQueue.size() < this.max_num_request_contents){
                    //Interestパケット送信用キューが空でない
                    if(!this.start_interestQueue.isEmpty()){
                        //まだ要求数の最大でない場合は，Interestパケットを取り出して送る．
                        //ヒマなときは，インタレストパケット送信処理を行う．
                        if(this.state == CCNUtil.STATE_NODE_NONE){
                            this.processSendInterest();
                        }

                    }else{
                        //まだ未ゲットなコンテンツはあるが，Interestは全て送った場合．
                        //空であれば，要求は行わず，ただデータ提供用サーバとして振る舞う．
                        //状態に関係なく，要求を待つ．

                    }
                }else{
                   // System.out.println("[ALL GET!!!]@"+this.getNodeID());
                    //コンテンツはすべてゲットした．
                    //まだ要求を出していない場合
                    if(!this.start_interestQueue.isEmpty()){
                        if(this.contentsQueue.size() < this.max_num_request_contents){
                            //まだ要求数の最大でない場合は，Interestパケットを取り出して送る．
                            if(this.state == CCNUtil.STATE_NODE_NONE){
                                this.processSendInterest();
                            }
                        }
                    }else{
                        //コンテンツもすべてゲットして，Interestは全て送った場合．
                        //空であれば，要求は行わず，ただデータ提供用サーバとして振る舞う．
                        //状態に関係なく，要求を待つ．

                    }
                }
                //インタレストパケットがやってきた．
                if(!this.interestQueue.isEmpty()){
                    if(this.state != CCNUtil.STATE_NODE_DATA_REQUESTED){
                        //他のデータがやってきて処理中でなければ，処理する．
                        //取り出して，データを返す．
                        this.state = CCNUtil.STATE_NODE_DATA_REQUESTED;
                        InterestPacket p = this.interestQueue.poll();
                        this.processDataReturn(p);

                    }else{
                        //Thread.sleep(500);
                    }



                }
                //終了命令が来たら，終了させる．
                if(this.state == CCNUtil.STATE_NODE_END){
                    this.receiver.setState(CCNUtil.STATE_NODE_END);
                    break;
                }
            }catch(Exception e){
                e.printStackTrace();
            }


        }

        long end = System.currentTimeMillis();

    }

    /**
     * Interestパケットが到着して，データを返送するための処理です．
     *
     * @param p
     * @return
     */
    public boolean processDataReturn(InterestPacket p){
        //Interestパケット更新
        LinkedList<ForwardHistory> fList = p.getHistoryList();
        ForwardHistory h = fList.getLast();
        h.setArrivalTime(System.currentTimeMillis());
        p.setCount(p.getCount()+1);

        /////ログ用////

        CCNLog.getIns().log(",1,"+p.getPrefix()+","+"-,"+fList.getFirst().getStartTime()+","+h.getArrivalTime()+","+(h.getArrivalTime()-fList.getFirst().getStartTime())+","+
        p.getFromNodeId()+","+this.getNodeID()+","+fList.size()+","+"-,"+"o"+",-");
        //p.getFromIDのルータ（？）に対し，コンテンツを返送する．
        if(this.ownContentsMap.containsKey(p.getPrefix())){
            CCNContents c = this.ownContentsMap.get(p.getPrefix());
            c.setCache(false);
            CCNRouter toRouter = CCNMgr.getIns().getRouterMap().get(h.getFromID());
            if(toRouter == null){
                CCNLog.getIns().log(",2,"+p.getPrefix()+",-"+","+fList.getFirst().getStartTime()+","+ fList.getLast().getArrivalTime()+","+
                        (fList.getLast().getArrivalTime()-fList.getFirst().getStartTime())+","+
                        p.getHistoryList().getFirst().getFromID()+",-"+","+fList.size()+",-"+","+"x"+","+"-");
                return false;
            }
            //履歴作成
            ForwardHistory contentsHistory = new ForwardHistory(this.getNodeID(), CCNUtil.NODETYPE_NODE, toRouter.getRouterID(), CCNUtil.NODETYPE_ROUTER,
                    System.currentTimeMillis(), -1);
            c.getHistoryList().add(contentsHistory);
            //最小BWを取得(Mbps)
            long bw = p.getMinBW();
            //データサイズは，
           // long time = c.getSize() / (bw/8);
            try{
                //指定時間だけ待つ．
               // Thread.sleep(time);

            }catch(Exception e){
                e.printStackTrace();
            }
            toRouter.forwardData(c);

        }else{
            //System.out.println("【Data not Found】prefix: "+p.getPrefix() + "@ Node "+this.nodeID);
            CCNLog.getIns().log(",1,"+p.getPrefix()+",-"+","+fList.getFirst().getStartTime()+","+ fList.getLast().getArrivalTime()+","+
                    (fList.getLast().getArrivalTime()-fList.getFirst().getStartTime())+","+
                    p.getHistoryList().getFirst().getFromID()+",-"+","+fList.size()+",-"+","+"x"+","+"-");
        }
        this.state = CCNUtil.STATE_NODE_NONE;


        return true;
    }

    public boolean containsStartQueue(String prefix){
        boolean  ret = false;
        Iterator<InterestPacket> pIte = this.start_interestQueue.iterator();
        while(pIte.hasNext()){
            InterestPacket p = pIte.next();
            //一致すればOK
            if(p.getPrefix().equals(prefix)){
                ret = true;
                break;
            }
        }
        return ret;
    }


    /**
     * 開始キューにまだ余裕があれば，インタレストパケットを自分で入れる．
     */
  /**  public void remainedIntereQueueing(){
        if(this.start_interestQueue.size() < CCNUtil.ccn_node_request_num){
            int remain =  CCNUtil.ccn_node_request_num -  CCNUtil.ccn_node_request_num;
            for(int i=0;i<remain;i++){
                //残りの分だけ，キューに入れる．
                ForwardHistory h = new ForwardHistory(this.nodeID, CCNUtil.NODETYPE_NODE, (long)-1, -1, -1, -1);
                LinkedList<ForwardHistory> hList = new LinkedList<ForwardHistory>();
                hList.add(h);
                long id = this.start_interestQueue.size() + 1;
                //Interestパケットを生成する．
                InterestPacket p = new InterestPacket(prefix, new Long(id), 1500, this.nodeID, size + 1, hList);
            }
        }
    }**/

    /**
     * Interestパケットを送る準備が整えば，送信するための処理です．
     * 一定時間毎ではなく，指数分布に従った送信間隔でInterestパケットを近隣ルータへ送ります．
     *
     */
    public void processSendInterest(){
        //とりあえず状態変更
        //リクエスティングの間は，Interestを送れない．他のことはもちろんできる．
        //ロックする．
            synchronized (this){
                this.state = CCNUtil.STATE_NODE_REQUESTING;

            }

        try{
            //リクエスト送信することが決まったら，指数分布の累積分布関数の確率に従い，
            //Interestパケットを送る．
            double t = 1;
            double comulative_p = 0.0d;
            while(true){
                //指数分布による，累積の確率密度を算出．
                comulative_p = 1- Math.pow(Math.E, (-1)*t*this.lambda);
                //1秒だけ待つ．
               // System.out.println("NodeID:"+this.nodeID);
                Thread.sleep(1000);
                double randomValue = Math.random();
                //Interestパケット送信可能状態となった
                if(randomValue <= comulative_p){
                   // System.out.println("NodeID:"+this.nodeID+ "Start Interest");
                    break;
                }
                t++;
            }
            //Interestパケットを送信する．
            //ルータをランダムで選択する．
           /* Object[] arrID = this.routerMap.keySet().toArray();
            int len = arrID.length;
            int idx = CCNUtil.genInt(0, len-1);
            Long destID = (Long)arrID[idx];
            CCNRouter r = this.routerMap.get(destID);
            */
            CCNRouter r = null;
            InterestPacket packet =  this.start_interestQueue.poll();
            if(CCNMgr.getIns().isSFCMode()){
                if(CCNMgr.getIns().isMasterWorker()){
                    //Master-workerの場合は，特定のmasterに処理を移譲する．
                    AutoEnvironment env = AutoSFCMgr.getIns().getEnv();
                    ComputeHost master = env.getMasterNode();
                    //masterのキューへ要求を入れる．
                    SFC sfc = master.scheduleSFC(packet);
                    packet.getAppParams().put(AutoUtil.SFC_NAME, sfc);
                    r = this.usedRouting.selectRouter(this.getRouterMap(), packet);

                    Long destID = r.getRouterID();

                    InterestPacket p  = genInterestPacket(destID, packet);

                    long minBW = Math.min(Math.min(this.getBw(), r.getBw()), p.getMinBW());

                    p.setMinBW(minBW);
                    long startTime = System.currentTimeMillis();
                    r.sendInterest(p);

                }else{
                    r = this.usedRouting.selectRouter(this.getRouterMap(), packet);

                    Long destID = r.getRouterID();

                    InterestPacket p  = genInterestPacket(destID, packet);

                    long minBW = Math.min(Math.min(this.getBw(), r.getBw()), p.getMinBW());

                    p.setMinBW(minBW);
                    long startTime = System.currentTimeMillis();
                    r.sendInterest(p);
                }


            }else{
                r = this.usedRouting.selectRouter(this.routerMap, packet);
                Long destID = r.getRouterID();

                //先頭を取り出す．
                //そして，近隣ルータへ送る．
                InterestPacket p  = genInterestPacket(destID, packet);
                long minBW = Math.min(Math.min(this.getBw(), r.getBw()), p.getMinBW());
                p.setMinBW(minBW);
                r.sendInterest(p);
            }



            //再びヒマ状態とする．
            synchronized(this){
                this.state = CCNUtil.STATE_NODE_NONE;
            }
        }catch(Exception e){
            e.printStackTrace();
        }


    }

    public InterestPacket genInterestPacket(Long routerID, InterestPacket packet){
        //パケットを取得する．
        //何番目かをセットする．
        packet.setCount( this.max_num_request_contents -this.start_interestQueue.size() );
        ForwardHistory h = packet.getHistoryList().getFirst();
        h.setFromID(this.getNodeID());
        h.setFromType(CCNUtil.NODETYPE_NODE);
        h.setStartTime(System.currentTimeMillis());
        h.setToID(routerID);
        h.setToType(CCNUtil.NODETYPE_ROUTER);
//System.out.println("<Interest Sent> from Node"+this.nodeID + "-->"+"Router"+routerID+ " for prefix:"+packet.getPrefix());
        //System.out.println("test");
        return packet;
    }


    /**
     * Interestパケット受信を待つための処理
     */
    public void processRecvInterestPacket(){
        //もしInterestパケットが来たら，それに基づいてデータを探す．
        if(!this.interestQueue.isEmpty()){
            InterestPacket packet = this.interestQueue.poll();
            //Interestパケットから，prefixを見る．
            if(this.ownContentsMap.containsKey(packet.getPrefix())){
                CCNContents contents = this.ownContentsMap.get(packet.getPrefix());
            }else{
                //データがなかった

            }
        }
    }



    public HashMap<String, CCNContents> getOwnContentsMap() {

        return ownContentsMap;
    }

    public void setOwnContentsMap(HashMap<String, CCNContents> ownContentsMap) {
        this.ownContentsMap = ownContentsMap;
    }

    public Long getNodeID() {
        return nodeID;
    }

    public void setNodeID(Long nodeID) {
        this.nodeID = nodeID;
    }

    public HashMap<Long, CCNRouter> getRouterMap() {
        return routerMap;
    }

    public void setRouterMap(HashMap<Long, CCNRouter> routerMap) {
        this.routerMap = routerMap;
    }

    public double getLambda() {
        return lambda;
    }

    public void setLambda(double lambda) {
        this.lambda = lambda;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }


    public FIB getFIBEntry() {
        return FIBEntry;
    }

    public void setFIBEntry(FIB FIBEntry) {
        this.FIBEntry = FIBEntry;
    }

    public int  getMax_num_request_contents() {
        return max_num_request_contents;
    }

    public void setMax_num_request_contents(int  max_num_request_contents) {
        this.max_num_request_contents = max_num_request_contents;
    }

    public LinkedBlockingQueue<InterestPacket> getStart_interestQueue() {
        return start_interestQueue;
    }

    public void setStart_interestQueue(LinkedBlockingQueue<InterestPacket> start_interestQueue) {
        this.start_interestQueue = start_interestQueue;
    }
}
