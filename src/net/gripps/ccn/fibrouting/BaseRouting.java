package net.gripps.ccn.fibrouting;

import net.gripps.ccn.CCNUtil;
import net.gripps.ccn.core.CCNNode;
import net.gripps.ccn.core.CCNRouter;
import net.gripps.ccn.core.Face;
import net.gripps.ccn.core.InterestPacket;
import net.gripps.ccn.process.CCNMgr;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by Hidehiro Kanemitsu on 2018/12/03.
 */
public abstract class BaseRouting {

    /**
     * ノード集合
     */
    protected HashMap<Long, CCNNode> nodeMap;

    /**
     * ルータ集合
     */
    protected HashMap<Long, CCNRouter> routerMap;

    protected String[] item_root = {"www", "yahoo", "google", "yama", "kawa", "teu","china", "america","europe",
    "africa", "japan", "thailand", "italy", "england", "germany", "example"};

    protected String[] item_mid = {"test", "site", "toyama", "osaka","nagoya","hokkaido","hiroshima","kanagawa","okinawa","chiba"};

    protected String[] item_last = {"test", "machi", "kawa","yama", "fafjask", "regjdf", "ffdai3", "fjfdsa2r", "fdsaa", "fdsai1",
    "845fjlfs", "rfjsalfjdsa"};


    public BaseRouting() {
      /*this.nodeMap = CCNMgr.getIns().getNodeMap();
        this.routerMap = CCNMgr.getIns().getRouterMap();
*/
    }

    /**
     *
     * @param nodeMap
     * @param routerMap
     */
    public BaseRouting(HashMap<Long, CCNNode> nodeMap, HashMap<Long, CCNRouter> routerMap) {
        this.nodeMap = nodeMap;
        this.routerMap = routerMap;
    }

    /**
     * ルータIDを計算するためのメソッド
     * 新規作成したルータに対して割り当てるべきID値を
     * 決定させてください．long値です．
     * @param index
     * @return
     */
    public abstract long calcID(long index);

    /**
     * 指定IDの次のID値を返すメソッド
     * 通常ならid+1ですが，ハッシュ空間を考えると，
     * Next(id)というように，+1にはならないです．
     * @param id
     * @return
     */
    public abstract long getNextID(long id);

    /**
     * 各ルータに対し，FIBを埋める．
     * つまり，ルータ->FIBの中身（prefix, LinkedList<Face>)を埋める処理
     * 具体的には，ルータ->FIBには，ノードID，あとはルータIDを含む．
     * できるだけノードIDは削除したい方向
     */
    public abstract void buildFIBProcess();

    /**
     * 各ルータ内のFaceを埋める
     * つまり，ルータ->Face_routerMapに対してFaceを埋める処理
     */
    public abstract void buildFaces();

    /**
     * ノード-->ルータへInterestパケットを送信し始めるときに，
     * どのルータへ送るかを決めるための処理
     * @param rMap
     * @param packet
     * @return
     */
    public abstract  CCNRouter selectRouter(HashMap<Long, CCNRouter> rMap, InterestPacket packet);


    /**
     * FIBにおいて，prefixがない場合（本来は破棄される場合），Faceリスト（ルータ）から，
     * 何らかの基準で転送先ルータIDを決める．なお，FaceリストからルータIDを選択して，
     * FIBに埋める処理を実装してください．その後，実際に埋められたFaceの宛先ID
     * をreturnしてください．
     * @return 選択されたルータID
     * @param prefix
     * @param router
     */
    public abstract Long  addFaceToFIBAsNewEntry(String prefix, CCNRouter router);


    public String  findNextRouter(InterestPacket p, CCNRouter r){
        return null;
    }


    /**
     * Interestパケットがループしそうな場合，どうするかを決めます．
     * 転送する／しない　まで行ってください．
     * @param p
     * @param f
     * @param router
     */
    public abstract long getNextRouterIDIfInterestLoop(InterestPacket p, Face f, CCNRouter router);


    /**
     *
     * @param uid
     * @return
     */
    public long calcHashCode(long uid) {
        return (Math.abs(uid) % (CCNUtil.ccn_maxID + 1));
    }

    /**
     * 各ノードにおいて，隣接ルータを設定する処理．
     *
     */
    public void buildNeighborRouters(){
        Iterator<CCNNode> nIte = this.nodeMap.values().iterator();
        //ノードごとのループ
        while(nIte.hasNext()){
            CCNNode n = (CCNNode)nIte.next();

            //ノード内の隣接ルータ数分だけのループ
            for(int i = 0; i< CCNUtil.ccn_node_routernum; i++){
                long id = CCNUtil.genLong(0, CCNUtil.ccn_router_num-1);
                long idx = 0;
                //ルータを選択する．
                Iterator<CCNRouter> rIte = this.routerMap.values().iterator();
                CCNRouter r = new CCNRouter();
                //ルータすべてから，指定Indexのルータを選択する．
                while(rIte.hasNext()){
                     r = rIte.next();
                    if(idx == id){
                        break;
                    }
                    idx++;
                }

              //  CCNRouter r = (CCNRouter)this.routerMap.get(new Long(id));
                n.getRouterMap().put(r.getRouterID(), r);
                //同時に，ルータ側のFaceに追記する．
                Face f = new Face(null, n.getNodeID(), CCNUtil.NODETYPE_NODE);
                r.addFace(f, r.getFace_nodeMap());
            }

        }
    }

    public String generatePrefix(){
        int del_num = CCNUtil.genInt(CCNUtil.ccn_prefix_delimiter_num_min, CCNUtil.ccn_prefix_delimiter_num_max);
        int idx_root = CCNUtil.genInt(0, this.item_root.length-1);
        String str_root = this.item_root[idx_root];
        StringBuffer buf = new StringBuffer("/"+str_root);
        //デリミタが続く間のループ
        for(int i=0;i<del_num-1;i++){
            buf.append("/");
            int idx_mid = CCNUtil.genInt(0, this.item_mid.length-1);
            String str_mid = this.item_mid[idx_mid];
            buf.append(str_mid);
        }
        buf.append("/");
        //最後に，ファイル名を追加する．
        int idx_file = CCNUtil.genInt(0, this.item_last.length-1);
        String fileName = this.item_last[idx_file];
        buf.append(fileName);

        return buf.toString();

    }

    public HashMap<Long, CCNNode> getNodeMap() {
        return nodeMap;
    }

    public void setNodeMap(HashMap<Long, CCNNode> nodeMap) {
        this.nodeMap = nodeMap;
    }

    public HashMap<Long, CCNRouter> getRouterMap() {
        return routerMap;
    }

    public void setRouterMap(HashMap<Long, CCNRouter> routerMap) {
        this.routerMap = routerMap;
    }
}
