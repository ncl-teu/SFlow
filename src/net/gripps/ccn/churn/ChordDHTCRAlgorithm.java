package net.gripps.ccn.churn;

import net.gripps.ccn.CCNUtil;
import net.gripps.ccn.Logger.CCNLog;
import net.gripps.ccn.core.*;
import net.gripps.ccn.fibrouting.BaseRouting;
import net.gripps.ccn.fibrouting.ChordDHTRouting;
import net.gripps.ccn.process.CCNMgr;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Chordアルゴリズムに基づいて，FIB/PIT/CS/BCを管理します．
 *
 */
public class ChordDHTCRAlgorithm extends BaseChurnResilienceAlgorithm {

    /**
     * FIBルーティングに，Chordを用いるのを強制する．
     */
    private ChordDHTRouting routing;

    /**
     * コンストラクタ．引数として渡されるroutingには，既にrouteMapにルータが入っていることが
     * 必須条件です．
     * @param routing
     */
    public ChordDHTCRAlgorithm(ChordDHTRouting routing) {
        super(routing);

        if(routing instanceof  ChordDHTRouting){
            this.routing = routing;
        }else{
            System.out.println("****Please use ChordDHTRouting as FIB routing algorithm!!****");
            System.exit(1);
        }

    }

    /**
     * FIB/PIT/CS+BCを設定し，さらにFaceももらう．
     * さらに，他ルータに，FIBを自分に向けてもらうようにする．
     * さらに，他ルータに，自分宛てのFaceを持ってもらうようにする．
     * rをnullとすると，ルータを自動生成して動作させます．
     * @param r 追加したいルータ（nullであれば，このメソッド内でインスタンス生成してください）
     * @param routerMap 参加前のルータ集合
     * @return
     */
    @Override
    public synchronized boolean ccnJoin(CCNRouter r, HashMap<Long, CCNRouter> routerMap) {
        //まずはルータのIDを決める．
        //この時，すでにidSetには値が追加されている．
        long newID = this.calcIDWithoutAdding(routerMap);
        CCNRouter router;

        if(r != null){
            router = r;
            router.setRouterID(newID);
        }else{
            router = new CCNRouter(new Long(newID));
        }

        //新規ルータIDよりも大きく，かつ差分が一番小さなIDを探す．
        Long nextID = this.routing.getNearEstID(newID);

        //まだMapにはputせず，探す．
        CCNRouter nextRouter = routerMap.get(nextID);
        //nextRouterのpredFaceをrに向ける．
        //そのために，もともとのpred(nextID)を取得．
        long org_predID = this.routing.getPredID(nextID);
        CCNRouter org_predRouter = routerMap.get(org_predID);
        //nextRouterのFaceから，元々のpredRouterのFaceを取得．
        Face face = nextRouter.findFaceByID(org_predID, nextRouter.getFace_routerMap());
        if(face != null){
            //そして，nextRouter -> predを，新規ルータにする．
            face.setPointerID(newID);
        }


        //元々の前ルータのFaceで，nextRouter向けのポインタをrへ変更する．
        Face faceAtPred = org_predRouter.findFaceByID(nextID, org_predRouter.getFace_routerMap());
        if(faceAtPred != null){
            faceAtPred.setPointerID(newID);
        }

        //NextRouterのFaceリストをもらう．
        //まずは，faceリストに，Next自体を追加する．
        Face f = new Face(null, nextID, CCNUtil.NODETYPE_ROUTER);
        router.addFace(f, router.getFace_routerMap());
        //一つ前のFaceも入れる．
        Face pf = new Face(null, org_predID, CCNUtil.NODETYPE_ROUTER);
        router.addFace(pf, router.getFace_routerMap());

        //そして，nextRouterのFaceリストをもらう処理を行う．
        Iterator<Face> fIte = nextRouter.getFace_routerMap().values().iterator();
        while(fIte.hasNext()){
            Face faceElement = fIte.next();
            router.addFace(faceElement, router.getFace_routerMap());
        }
        //FIBは，org_preRouterID~ (A)~routerID~ (B)~ next RouterID
        //において，(A)に該当するもののみもらう．
        FIB nextFIB = nextRouter.getFIBEntry();
        Iterator<String> list = nextFIB.getTable().keySet().iterator();
        while(list.hasNext()){
            String prefix = list.next();
            //LinkedListを取得．
            LinkedList<Face> l = nextFIB.getTable().get(prefix);
            long hashCode = this.routing.calcHashCode(prefix.toString().hashCode());
            //Aの範囲であれば，移動させる．
            if((hashCode > org_predID) &&(hashCode <= newID)){

                CCNContents c = CCNMgr.getIns().getcMap().get(prefix);
                Long ownerID = c.getOrgOwnerID();
                if(nextRouter.findFaceByID(ownerID, nextRouter.getFace_nodeMap())==null){
                    //見つからなければ，何もしない
                }else{
                    //Faceに所有者IDがあれば，移動して，FIBも移動する．
                    router.getFIBEntry().getTable().put(prefix, l);
                    //そして，nextRouterからFIBは，あえて残す戦略．
                   // nextRouter.getFIBEntry().removeByKey(prefix);
                    //Faceの移動
                    Face newF = new Face(null, ownerID, CCNUtil.NODETYPE_NODE);
                    router.addFace(newF, router.getFace_nodeMap());

                }

            }
        }

        //あとは，routerからみて，飛び飛びの値も入れる．
       this.routing.buildFingerFaces(router, routerMap );

        //PIT/CSでは，何もしない？

        //ついに，ネットワークへ参加させる．
        routerMap.put(router.getRouterID(), router);
        this.routing.getIdSet().add(router.getRouterID());

        Thread t = new Thread(router);
        //新規ルータを起動する．
        t.start();
       System.out.println(" New Router JOINED!!! Current Num: "+ routerMap.size());
        CCNLog.getIns().log(",5,"+"-"+","+"-"+","+"-"+","+ "-"+","+
                "-"+","+"-"+","+"-"+","+"-"+","+"-"+","+"x"+","+"-"+","+router.getRouterID());

        return true;
    }

    /**
     * r -> surRouterへ，FIB/PITをすべてあげる処理です．
     * @param r
     * @param sucRouter
     * @param fromTable
     * @param toTable
     * @return
     */
    public boolean delegateTable(CCNRouter r, CCNRouter sucRouter,
                                 HashMap<String, LinkedList<Face>> fromTable, HashMap<String, LinkedList<Face>> toTable){

        //FIBの処理．まずは，当該ルータのFIBを，すべてsucRouterへ渡す．
        Iterator<String> pIte = fromTable.keySet().iterator();
        //当該ルータのFIBのprefixに対するループ
        while(pIte.hasNext()){
            String prefix = pIte.next();
            //もしsucRouterに，同一prefixが見つかれば，既存リストに各Faceを追加する．
            if(toTable.containsKey(prefix)){
                LinkedList<Face> sucFaceList =toTable.get(prefix);
                LinkedList<Face> fList = fromTable.get(prefix);
                Iterator<Face> fIte = fList.iterator();
                //当該ルータの，1つのPrefixの各faceに対するループ
                while(fIte.hasNext()){
                    Face f = fIte.next();
                    if(f.getType() == CCNUtil.NODETYPE_ROUTER){
                        sucRouter.addFace(f, sucRouter.getFace_routerMap());
                    }else{
                        sucRouter.addFace(f, sucRouter.getFace_nodeMap());
                        //ノード側も更新する必要あり．
                       // CCNNode node = CCNMgr.getIns().getNodeMap().get(f.getPointerID());

                    }
                    //そしてFIBに登録する．
                    if(sucRouter.isFaceContains(prefix, f)){
                        //何もしない
                    }else{
                        sucFaceList.add(f);
                    }
                }
            }
        }
        return true;

    }

    /**
     * ルータが離脱する処理です．
     * ルータ離脱時，predルータに対して，nextルータの情報を教えてあげる．
     * 1. predルータのFace->当該ルータ　を，->nextルータへ　とする．
     * 2. nextルータのFace->当該ルータ　を，->predルータへ　とする．
     * 3. 当該ルータのFace全部を，pred/nextルータへあげる．
     * 4. predRouter
     *  5.
     * @param r
     * @param routerMap 離脱前のルータ集合
     * @return
     */
    @Override
    public synchronized boolean ccnLeave(CCNRouter r, HashMap<Long, CCNRouter> routerMap) {
        //rがnullであれば，決める．
        long index = 0;
        long size = routerMap.size();
        long val = CCNUtil.genLong(0, size-1);
        synchronized (this){
            if(r == null){
                Iterator<CCNRouter> rIte = routerMap.values().iterator();
                while(rIte.hasNext()){
                    CCNRouter router  = rIte.next();
                    if(val == index){
                       r = router;
                       break;
                    }
                    index++;
                }

            }
        }

        long predID = this.routing.getPredIDFromFaces(r);
        CCNRouter predRouter = routerMap.get(predID);

        long sucID = this.routing.getNextIDFromFaces(r);
        CCNRouter sucRouter = routerMap.get(sucID);

       // 1. predルータのFace->当該ルータ　を，->nextルータへ　とする．
        Face f1 = predRouter.findFaceByID(r.getRouterID(), predRouter.getFace_routerMap());
        if(f1 != null){
            f1.setPointerID(sucID);
        }
      //2. nextルータのFace->当該ルータ　を，->predルータへ　とする．
        Face f2 = sucRouter.findFaceByID(r.getRouterID(), sucRouter.getFace_routerMap());
        if(f2 != null){
            f2.setPointerID(predID);
        }
       // 3. 当該ルータのFace全部を，pred/nextルータへあげる．
        HashMap<Long, Face> facesRouter = r.getFace_routerMap();
        Iterator<Face> rFaceIte = facesRouter.values().iterator();
        while(rFaceIte.hasNext()){
            Face rF = rFaceIte.next();
            predRouter.addFace(rF, predRouter.getFace_routerMap());
            sucRouter.addFace(rF, sucRouter.getFace_routerMap());
        }
        HashMap<Long, Face> facesNode = r.getFace_nodeMap();
        Iterator<Face> nFaceIte = facesNode.values().iterator();
        while(nFaceIte.hasNext()){
            Face nF = nFaceIte.next();
            predRouter.addFace(nF, predRouter.getFace_nodeMap());
            sucRouter.addFace(nF, predRouter.getFace_nodeMap());
        }

        //FIBの処理．まずは，当該ルータのFIBを，すべてsucRouterへ渡す．
        this.delegateTable(r, sucRouter, r.getFIBEntry().getTable(), sucRouter.getFIBEntry().getTable());

        //predRouterの，当該向けのFIBエントリをsucRouterへと向ける．
        Iterator<LinkedList<Face>> fListIte = predRouter.getFIBEntry().getTable().values().iterator();
        while(fListIte.hasNext()){
            LinkedList<Face> fList = fListIte.next();
            Iterator<Face> fIte = fList.iterator();
            while(fIte.hasNext()){
                Face f = fIte.next();
                if((f.getPointerID() == r.getRouterID())&&(f.getType() == CCNUtil.NODETYPE_ROUTER)){
                    f.setPointerID(sucID);
                }
            }
        }
        //PIT/CSの移譲をどうするか？？？
        //PITをsucRouterへあげる．
        this.delegateTable(r, sucRouter, r.getPITEntry().getTable(), sucRouter.getPITEntry().getTable());

        //Mapから削除
        routerMap.remove(r.getRouterID());
        this.routing.getIdSet().remove(r.getRouterID());
        //終了シグナル送信．
        r.setState(CCNUtil.STATE_NODE_END);
        return true;
    }

    /**
     *
     * @param routerMap
     * @return
     */
    public long calcIDWithoutAdding(HashMap<Long, CCNRouter> routerMap) {
        long id = -1;
        long routerNum = routerMap.size();

        //均等にIDが分布するように決める．
        //とりあえず，indexから，ID空間における何分割目なのかを考える．
        long unitSize = CCNUtil.ccn_maxID / (routerMap.size()+1);
        long randomID = CCNUtil.genLong(1, routerMap.size()-1);
        id = unitSize * randomID;
        while(routerMap.containsKey(id)){
            randomID = CCNUtil.genLong(1, routerMap.size()-1);
            id = unitSize * randomID;
            if (id > CCNUtil.ccn_maxID) {
                id = CCNUtil.ccn_maxID;
            }
        }

        return id;
    }
}
