package net.gripps.ccn.fibrouting;

import net.gripps.ccn.CCNUtil;
import net.gripps.ccn.core.*;
import net.gripps.ccn.process.CCNMgr;
import net.gripps.clustering.common.aplmodel.CustomIDSet;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by Hidehiro Kanemitsu on 2018/12/04.
 * DHT(Distributed Hash Table)の一つである，Chord
 * アルゴリズムに基づいて，FIBのエントリを埋めます．
 * ルータの集合は，routerMapだけでなく，idSetでも管理しています．
 * 従って，削除等は，
 *
 */
public class ChordDHTRouting extends BaseRouting {

    protected CustomIDSet idSet;


    /**
     * コンストラクタ
     *
     * @param nodeMap
     * @param routerMap
     */
    public ChordDHTRouting(HashMap<Long, CCNNode> nodeMap, HashMap<Long, CCNRouter> routerMap) {
        super(nodeMap, routerMap);
        this.idSet = new CustomIDSet();

    }

    public ChordDHTRouting() {
        super();
        this.idSet = new CustomIDSet();
    }

    @Override
    public CCNRouter selectRouter(HashMap<Long, CCNRouter> rMap, InterestPacket packet) {
        Iterator<CCNRouter> rIte = rMap.values().iterator();
        CustomIDSet set = new CustomIDSet();
        while(rIte.hasNext()){
            CCNRouter r = rIte.next();
            set.add(r.getRouterID());
        }
        long hid = this.calcHashCode(packet.getPrefix().toString().hashCode());
        long routerID = this.getNearestIDFromSet(set.getObjSet(), hid);
        return rMap.get(routerID);
    }

    @Override
    public Long addFaceToFIBAsNewEntry(String prefix, CCNRouter router) {
        long hid =  this.calcHashCode(prefix.toString().hashCode());
        //Faceリストから，最も近いものを選択する．
        Iterator<Face> fIte = router.getFace_routerMap().values().iterator();
        long id = -1;
        while(fIte.hasNext()){

            //hidに近くて，かつInterestQueueのサイズが少ないルータのidとするように変更．
            id = this.getNearestIDFromIterator(fIte, hid);
            CCNRouter retRouter = CCNMgr.getIns().getRouterMap().get(id);
            //ルータのPITを取得する．
            PIT pit = retRouter.getPITEntry();
            if(pit.getTable().size() > CCNUtil.ccn_chord_pit_threshold){
                fIte.next();
                continue;
            }


            //転送先となるルータのIDを見つかったので，Faceを作成
            Face f = new Face(null, id, CCNUtil.NODETYPE_ROUTER);
            //FIBに追加する．
            router.getFIBEntry().addFace(prefix, f);
            break;

        }


        return id;

    }

    /**
     *
     * @param fIte
     * @param hid
     * @return
     */
    public long getNearIDAndShortQueueRouter(Iterator<Face> fIte, long hid){
        return 0;
    }





    @Override
    public long calcID(long index) {
        long id = -1;
        if (index == 0) {
            //0番目なら，0を返す．
            id = 0;
        } else {
            //均等にIDが分布するように決める．
            //とりあえず，indexから，ID空間における何分割目なのかを考える．
            long base_id = CCNUtil.ccn_maxID / (CCNUtil.ccn_router_num);
            id = base_id * index;
            if (id > CCNUtil.ccn_maxID) {
                id = CCNUtil.ccn_maxID;
            }
        }
        //IDセットへ入れておく．
        this.idSet.add(new Long(id));
        return id;
    }

    /**
     * 指定IDの次のものを探す．
     *
     * @param id
     * @return
     */
    @Override
    public long getNextID(long id) {
        boolean isFound = false;
        long retValue = -1;
        Iterator<Long> valIte = this.idSet.iterator();
        while (valIte.hasNext()) {
            Long val = valIte.next();
            if (val.longValue() == id) {
                isFound = true;
                break;
            }
        }
        if (isFound) {
            if (valIte.hasNext()) {
                //最後でなければ，次を返す．
                retValue = valIte.next().longValue();
            } else {
                //最後の値だったら，0を返す．
                retValue = 0;
            }
        } else {
            retValue = CCNUtil.MINUS_VAUE;
        }

        return retValue;

    }

    public CustomIDSet getRequestingNodes(String prefix) {
        CustomIDSet set = new CustomIDSet();
        Iterator<CCNNode> nIte = this.nodeMap.values().iterator();
        while (nIte.hasNext()) {
            CCNNode n = nIte.next();
            //ノードから，prefixをもつものを選択する．
            //prefixを含むかどうかだけを見れればよい．．
            if (n.containsStartQueue(prefix)) {
                set.add(n.getNodeID());
            }
        }

        return set;
    }


    /**
     * -1になるかもしれない．
     *
     * @param fid
     * @return
     */
    public long getNearEstID(Long fid) {
        Iterator<Long> idIte = this.idSet.iterator();
        long mindif = CCNUtil.MAXValue;
        long retID = CCNUtil.MINUS_VAUE;
        while (idIte.hasNext()) {
            //fid < idかつ，差分が最小のもの
            Long id = idIte.next();
            if (id < fid) {
                //continue;
                /** long dif = id.longValue() - fid.longValue();
                 if(mindif >= dif){
                 mindif = dif;
                 retID = id.longValue();
                 }
                 **/
            } else {
                long dif = id.longValue() - fid.longValue();
                if (mindif >= dif) {
                    mindif = dif;
                    retID = id.longValue();
                }

                //retID = id.longValue();

                //break;
            }
        }
        if (retID == CCNUtil.MINUS_VAUE) {
            retID = this.idSet.getList().getLast();
        }
        return retID;
    }

    /**
     * 指定のルータにおいて，fingerIDに該当するルータをFIB
     * @param router
     * @param rMap
     */
    public void buildFingerFaces(CCNRouter router, HashMap<Long, CCNRouter> rMap){

        //CCNRouter router = rMap.get(id);
        long id = router.getRouterID();
        Long tmpValue = 0L;
        // System.out.print("RID:"+router.getRouterID());
        for (int k = 1; k <= CCNUtil.ccn_max_pow; k++) {
            long distance = (CCNUtil.ccn_maxID + 1) / CCNUtil.ccn_router_num;
            Long fingerValue = (long) (id + distance*Math.pow(2, k - 1)) % (CCNUtil.ccn_maxID + 1);
            //fingerValueより大きいIDのうち，最も近いIDを取得する．
            Long fingerID = this.getNearEstID(fingerValue);

            //routerのFaceに追加する．
            Face f = new Face(null, fingerID, CCNUtil.NODETYPE_ROUTER);
            //現在のface数を求める．
            long fNum = router.getFace_nodeMap().size() + router.getFace_routerMap().size();
            if (router.getFace_num() <= fNum) {
                //もう上限に達していれば抜ける
                break;
            } else {
                //まだ余裕があれば入れる．
                router.addFace(f, router.getFace_routerMap());
                //そして，fingerIDのルータにもFaceを追加する．
                CCNRouter fingerRouter = rMap.get(fingerID);
                Face fingerF = new Face(null, router.getRouterID(), CCNUtil.NODETYPE_ROUTER);
                fingerRouter.addFace(fingerF, fingerRouter.getFace_routerMap());
            }
            tmpValue = fingerValue;
        }
    }

    /**
     * ルータ同士でとびとびのFaceIDを埋める．
     */
    @Override
    public void buildFaces() {
        // long distance = CCNUtil.ccn_maxID / CCNUtil.ccn_router_num;

        //各ルータのFaceを追加する．
        Iterator<Long> idIte = this.idSet.iterator();
        while (idIte.hasNext()) {
            Long id = idIte.next();
            CCNRouter router = this.routerMap.get(id);
            this.buildFingerFaces(router, this.routerMap);
        }

    }

    public CustomIDSet getIdSet() {
        return idSet;
    }

    public void setIdSet(CustomIDSet idSet) {
        this.idSet = idSet;
    }


    public long calcHashCode(long uid) {
        return (Math.abs(uid) % (CCNUtil.ccn_maxID + 1));
    }

    @Override
    public void buildFIBProcess() {
        //CCNコンテンツのprefixから，ハッシュ値を生成する．
        Iterator<CCNNode> nIte = this.nodeMap.values().iterator();
        //ノードごとのループ
        while (nIte.hasNext()) {
            CCNNode n = nIte.next();
            Iterator<CCNContents> cIte = n.getOwnContentsMap().values().iterator();
            //コンテンツごとのループ
            while (cIte.hasNext()) {
                CCNContents c = cIte.next();
                //コンテンツのハッシュ値を計算して，その値以上，最も近いルータIDを探す．
                //さらに，隣接ルータすべてのFIBに，このエントリを埋める．
                //UUID uid = UUID.randomUUID();
                String uid = c.getPrefix();
                //long hashCode = this.calcHashCode(uid.toString().hashCode());
                long hashCode = c.getCustomID();
                //ハッシュIDを取得する．
                //long hashCode = (long)(Math.abs(uid.hashCode())%(CCNUtil.ccn_maxID+1));

                //コンテンツのハッシュIDをセットする．
                //c.setCustomID(hashCode);

                //hashCodeに最も近いルータを探す．
                long nearestID = this.getNearEstID(hashCode);
                //System.out.println("hash:"+hashCode+": NearistID:"+nearestID);
                //ルータを取得
                CCNRouter router = this.routerMap.get(new Long(nearestID));
                //System.out.println("****Hash:"+hashCode+" from Router:"+nearestID +"to Node"+n.getNodeID());

                //System.out.println("ContentID:"+hashCode+" RouterID:"+nearestID);
                //ルータrouterのFIBに，ノードnのFaceをprefixとともに追加する．
                // LinkedList<Face> fList = new LinkedList<Face>();
                Face f = new Face(null, n.getNodeID(), CCNUtil.NODETYPE_NODE);

                router.addFace(f, router.getFace_nodeMap());

                //FIBに，すでに，prefixがあるかどうか
                FIB fib = router.getFIBEntry();
                //fibに対して，face fを追加する．
                //もしあれば，追加しない．
                fib.addFace(c.getPrefix(), f);
                //System.out.println("test");
            }

        }

        /*
        HashMap<String, CCNContents> cMap = CCNMgr.getIns().getcMap();
        //各コンテンツにつき，ランダムで選ばれたルータを起点として，保持ノードを探す処理
        Iterator<CCNContents> cIte2 = cMap.values().iterator();
        while(cIte2.hasNext()){
            CCNContents c2 = cIte2.next();
            //ルータをランダムで選ぶ．
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
            Long nId = r.getRouterID();
            this.routingProcess(nId, c2, new CustomIDSet());


        }
        */

        //もう一度，ノードからの転送を行う．
        Iterator<CCNNode> nIte2 = this.nodeMap.values().iterator();
        while (nIte2.hasNext()) {
            CCNNode n = nIte2.next();
            //コンテンツに対するループ
            // Iterator<CCNContents> cIte2 = n.getOwnContentsMap().values().iterator();
            Iterator<InterestPacket> pIte = n.getStart_interestQueue().iterator();
            while (pIte.hasNext()) {
                InterestPacket c = pIte.next();
                //ハッシュ値を取得
                long hid = this.calcHashCode(c.getPrefix().hashCode());
                // long hid = c.getCustomID();
                //FaceIDから，hid以上で最も近いものを取得する．
                long nId = this.getNearestIDFromSet(n.getRouterMap().keySet(), hid);

                //nidを，転送先として，prefix, Face(nid)を追加する．そして，nidルータに対して同様の処理を行う．
                this.routingProcess(nId, hid, c.getPrefix(), new CustomIDSet());

            }
        }

    }


    /**
     * ルータの，「ノード用faceリスト」から，hidを保持しているコンテンツをもつノードがいるか
     * をチェックする．
     *
     * @param router
     * @param hid
     * @return
     */
    public Long findContentHoldingNode(CCNRouter router, long hid) {
        Long retID = CCNUtil.MINUS_VAUE;

        Iterator<Face> fIte = router.getFace_nodeMap().values().iterator();
        while (fIte.hasNext()) {
            Face f = fIte.next();
            CCNNode n = this.nodeMap.get(f.getPointerID());
            Iterator<CCNContents> cIte = n.getOwnContentsMap().values().iterator();
            while (cIte.hasNext()) {
                CCNContents c = cIte.next();
                if (c.getCustomID() == hid) {
                    //みつかれば，そのLong値を返す．
                    retID = n.getNodeID();
                    break;
                }
            }
        }
        return retID;


    }

    /**
     * ルータのFaceリストから，当該ルータの次のIDを持つルータのIDを返します．
     *
     * @param r
     * @return
     */
    public long getNextIDFromFaces(CCNRouter r){
        long rID = r.getRouterID().longValue();
        long retID = CCNUtil.MINUS_VAUE;
        long dif = CCNUtil.MAXValue;

        Iterator<Face> fIte = r.getFace_routerMap().values().iterator();
        while(fIte.hasNext()){
            Face f = fIte.next();
            if(f.getType() == CCNUtil.NODETYPE_ROUTER){
                long id = f.getPointerID().longValue();
                if(id > rID){
                    long tmp = id - rID;
                    if(tmp <= dif){
                        retID = id;
                        dif = tmp;
                    }
                }
            }else{
                continue;
            }
        }
        return retID;
    }

    public long getPredIDFromFaces(CCNRouter r){
        long rID = r.getRouterID().longValue();
        long retID = CCNUtil.MINUS_VAUE;
        long dif = CCNUtil.MAXValue;

        Iterator<Face> fIte = r.getFace_routerMap().values().iterator();
        while(fIte.hasNext()){
            Face f = fIte.next();
            if(f.getType() == CCNUtil.NODETYPE_ROUTER){
                long id = f.getPointerID().longValue();
                if(id < rID){
                    long tmp = rID - id;
                    if(tmp <= dif){
                        retID = id;
                        dif = tmp;
                    }
                }
            }else{
                continue;
            }
        }
        return retID;
    }

    public long getPredID(long id){

        Iterator<Long> ite = this.idSet.iterator();
        long retID = CCNUtil.MINUS_VAUE;
        Long predID = CCNUtil.MINUS_VAUE;
        predID = ite.next();
        boolean isFound = false;
        while(ite.hasNext()){
           Long tmpID = ite.next();
           if(predID.longValue() <id) {
               if(tmpID.longValue() > id) {
                   retID = predID.longValue();
                   isFound = true;
                   break;
               }
           } else{
                continue;
            }
           predID = tmpID;
        }
        if(!isFound){
            retID = predID;
        }
        return retID;
    }



    /**
     * インタレストパケットがループする場合にどうするかを実装
     * @param p
     * @param f
     * @param router
     * @return
     */
    @Override
    public long getNextRouterIDIfInterestLoop(InterestPacket p, Face f, CCNRouter router) {
        String prefix = p.getPrefix();
        //Faceのあて先を見る．
        long toID = f.getPointerID();
        //prefixから，hash値を計算する．
        long hcode = this.calcHashCode(prefix.toString().hashCode());

        long rID = router.getRouterID();

        //toIDよりひとつ小さいものを選択する
        Iterator<Face> fIte = router.getFace_routerMap().values().iterator();
        long retID = CCNUtil.MINUS_VAUE;
        int type = f.getType();
        //toIDをループするということは，toIDはルータということ．
        CCNRouter toRouter = this.routerMap.get(toID);

        if(hcode >= toID){
            //ハッシュ値が大きいのであれば，nextを取得
            retID = this.getNextIDFromFaces(toRouter);
        }else{
            retID = this.getPredIDFromFaces(toRouter);
        }

        /*
        //hcodのほうがおおきければ，toIDよりひとつ大きいものを選択する．
        if(toID <= hcode){

        }else{

        }

        while(fIte.hasNext()){
            Face face  = fIte.next();
            if((face.getPointerID() == toID)&&(type==face.getType())){
                continue;
            }else if ((face.getPointerID() < toID)&&(type == face.getType())){

                if(retID <= face.getPointerID().longValue()){
                    retID = face.getPointerID();
                }
            }else{
                //retID = face.getPointerID()
                continue;
            }

        }
        */
        if(retID == CCNUtil.MINUS_VAUE){
            if(hcode >= router.getRouterID().longValue()){
                //ハッシュ値が大きいのであれば，nextを取得
                retID = this.getNextIDFromFaces(router);
            }else{
                retID = this.getPredIDFromFaces(router);
            }

            retID = this.getNextID(router.getRouterID());
        }

        //それではマイナスなら，ランダム
        if(retID == CCNUtil.MINUS_VAUE){
            Object[] array = router.getFace_routerMap().values().toArray();
            int idx = CCNUtil.genInt(0, array.length-1);
            Face face = (Face)array[idx];
            while(face.getType() != f.getType()){
               array = router.getFace_routerMap().values().toArray();
                idx = CCNUtil.genInt(0, array.length-1);
                face = (Face)array[idx];
            }
        }
        //redIDに対して転送する．
        return retID;
    }

    /**
     * @param fromID
     * @return
     */
    public boolean routingProcess(Long fromID, long hid, String prefix, CustomIDSet set) {
        //転送元ルータを探す．
        CCNRouter fromRouter = this.routerMap.get(fromID);
        // long hid = content.getCustomID();

        Long nodeID = this.findContentHoldingNode(fromRouter, hid);
        //もしFIBにあれば，何もしない．
        if (fromRouter.getFIBEntry().getTable().containsKey(prefix)) {
            set.add(fromRouter.getRouterID());
            //System.out.println("すでにFIBにあったから無視@" + hid);
            return true;
        }


        //もしノード用Faceリストにあるノードに，対象コンテンツ保持ノードがあれば，それをFIBにいれる．
        if (nodeID != CCNUtil.MINUS_VAUE) {
            Face f = fromRouter.findFaceByID(nodeID, fromRouter.getFace_nodeMap());
            fromRouter.getFIBEntry().addFace(prefix, f);
            set.add(fromRouter.getRouterID());
           // System.out.println("【到達】" + fromID + "で，" + hid + "のFIB追加");

            return true;

        } else {
            //もしノードFaceリスト内のノードにコンテンツ保持ノードがいなければ，さらにルータへ転送する．
            //ルータFaceリストから，hidに最も近いFaceを選択する．
            Iterator<Face> fIte = fromRouter.getFace_routerMap().values().iterator();
            Long foundID = getNearestIDFromIterator(fIte, hid);
            //みつかったfoundIDのFaceを取得する．
            Face foundFace = fromRouter.findFaceByID(foundID, fromRouter.getFace_routerMap());
            fromRouter.getFIBEntry().addFace(prefix, foundFace);
           // System.out.println(fromID + "で，" + hid + "のFIB追加");

            set.add(fromRouter.getRouterID());
            if (set.contains(foundID)) {
                //System.out.println("すでにチェック済みだから無視@" + hid);

                //すでに他ノードから到達しているのであれば，終了．
                return true;
            } else {
                Iterator<Face> fIte2 = fromRouter.getFace_routerMap().values().iterator();
                //System.out.print(fromID + " --> " + foundID + "へ転送@" + hid);

                //   fromRouter.getFIBEntry().addFace(prefix, foundFace);
                //まだ未到達ノードであれば，そのノードに転送する．
                return this.routingProcess(foundID, hid, prefix, set);
            }

        }

    }


    /**
     * Set<Long>から，最もIDが近いものを返す．
     *
     * @return
     */
    public long getNearestIDFromSet(Set<Long> set, Long fid) {
        Iterator<Long> idIte = set.iterator();
        long mindif = CCNUtil.MAXValue;
        long retID = CCNUtil.MINUS_VAUE;
        Long maxID = CCNUtil.MINUS_VAUE;

        while (idIte.hasNext()) {
            Long id = idIte.next();

            if (id.longValue() >= fid.longValue()) {
                long dif = id.longValue() - fid.longValue();
                if (mindif >= dif) {
                    mindif = dif;
                    retID = id.longValue();
                }

            } else {
                //idが小さい場合，とりあえず最大値を保持しておく．
                if (maxID <= id) {
                    maxID = id;
                }
                continue;
            }
        }
        if (retID == CCNUtil.MINUS_VAUE) {
            return maxID;
        } else {
            return retID;

        }
    }


    public long getNearestIDFromIterator(Iterator<Face> fIte, Long fid) {
        long mindif = CCNUtil.MAXValue;
        long retID = CCNUtil.MINUS_VAUE;
        Long maxID = CCNUtil.MINUS_VAUE;
        while (fIte.hasNext()) {
            Face f = fIte.next();
            Long id = f.getPointerID();
            if (id.longValue() >= fid.longValue()) {
                long dif = id.longValue() - fid.longValue();
              //  if(CCNMgr.getIns().getRouterMap().containsKey(id)){
               //    CCNRouter r = CCNMgr.getIns().getRouterMap().get(id);
                    //if(r.getPITEntry().getTable().size() > CCNUtil.ccn_chord_pit_threshold){

              //      }else{
                        if (mindif >= dif) {
                            mindif = dif;
                            retID = id.longValue();
                        }
                  //  }
              //  }


            } else {
                //idが小さい場合，とりあえず最大値を保持しておく．
                if (maxID <= id) {
                    maxID = id;
                }
                continue;
            }
        }
        if (retID == CCNUtil.MINUS_VAUE) {
            return maxID;
        } else {
            return retID;

        }
    }


}

