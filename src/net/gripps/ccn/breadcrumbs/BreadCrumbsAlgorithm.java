package net.gripps.ccn.breadcrumbs;

import net.gripps.ccn.CCNUtil;
import net.gripps.ccn.Logger.CCNLog;
import net.gripps.ccn.core.*;
import net.gripps.ccn.process.CCNMgr;

import java.util.Iterator;
import java.util.LinkedList;

public class BreadCrumbsAlgorithm extends BaseBreadCrumbsAlgorithm {

    public BreadCrumbsAlgorithm() {
    }

    /**
     *
     * @param r cを受信したルータ
     * @param c
     * @return
     */
    public boolean forwardBCData(CCNRouter r, CCNContents c){
       //分岐点

        //BCがあれば，upwordへ転送する．
        if ((r.getBcMap().containsKey(c.getPrefix()))&&(r.getPITEntry().getTable().containsKey(c.getPrefix()))) {
            c.setBC(true);
            //双方にあれば，PITを優先させる．
            return false;


        }else if((r.getBcMap().containsKey(c.getPrefix()))&&(!r.getPITEntry().getTable().containsKey(c.getPrefix()))){
            BC bc = r.getBcMap().get(c.getPrefix());
            c.setBC(true);
            //BCのみにあれば，upHopへ転送する．
            CCNRouter upHopRouter = CCNMgr.getIns().getRouterMap().get(bc.getUpHop());
            ForwardHistory f = new ForwardHistory(r.getRouterID(), CCNUtil.NODETYPE_ROUTER, upHopRouter.getRouterID(), CCNUtil.NODETYPE_ROUTER,
                    System.currentTimeMillis(), -1);
            f.setForwardedByBC(true);
            c.getHistoryList().add(f);
            upHopRouter.forwardData(c);
            return true;
        }else{
            return false;
        }


    }

    /*
     *  ルータからデータ転送時に呼ばれるメソッドで, BCを新規作成して残す処理です．
     *  また，エッジ側CCRルータにおいて，CCNコンテンツを配置します．
     *  もしこれらの処理をすればtrue/しなければfalseを返す．
     * @param r 送信元ルータ
     * @param c
     */
    @Override
    public boolean createBC(CCNRouter r, CCNContents c) {
        //最新の履歴を見る
        ForwardHistory h = c.getHistoryList().getLast();
        int fromType = h.getFromType();
        int toType = h.getToType();
        BC bc;
        //ルータからルータへの転送であれば，普通に残す．
        if ((fromType == CCNUtil.NODETYPE_ROUTER) && (toType == CCNUtil.NODETYPE_ROUTER)) {
            bc = new BC(c.getPrefix(), h.getFromID(), h.getToID(), System.currentTimeMillis());
            //BCをルータに配置する．
            r.getBcMap().put(c.getPrefix(), bc);
        } else {
            //もしノードからの転送であれば，BCのupHopを-1にする．
            if (fromType == CCNUtil.NODETYPE_NODE) {
                bc = new BC(c.getPrefix(), CCNUtil.MINUS_VAUE, r.getRouterID(), System.currentTimeMillis());
                //BCをルータに配置する．
                r.getBcMap().put(c.getPrefix(), bc);
            }
            //ノードへの転送であれば，downHopを-1にする
            if (toType == CCNUtil.NODETYPE_NODE) {
                bc = new BC(c.getPrefix(), r.getRouterID(), CCNUtil.MINUS_VAUE, System.currentTimeMillis());
                //BCをルータに配置する．
                r.getBcMap().put(c.getPrefix(), bc);

                //そして，CSにキャッシュする．
                c.setCache(true);
               // c.getHistoryList().clear();
                c.setCurrentOwnerID(r.getRouterID());
                c.setGeneratedTimeAtCache(System.currentTimeMillis());

                //rのcsへ追加する．
                r.getCSEntry().getCacheMap().put(c.getPrefix(), c);
            }
        }
        return true;

    }

    /**
     * 指定のprerfixのBCがあるかをチェックし，もしあればBCのdownHopの指し示す方へ行く．
     * 具体的には，Interestパケットを送る．
     *
     * @param r Interest送信元ルータ
     * @param p
     * @return データをupHopへ転送，もしくはInterestをdownHopへ転送するとtrue/転送しなければfalse
     */
    @Override
    public boolean forwardRequestByBC(CCNRouter r, InterestPacket p) {
        boolean ret = false;

        //もし当該ルータのBC集合に，prefixが見つかった．
        //この場合は，InterestパケットにBCフラグを立てて，転送する．
        if (r.getBcMap().containsKey(p.getPrefix())) {
            p.setForwardedByBC(true);
            //宛先を見る．
            BC bc = r.getBcMap().get(p.getPrefix());
            long toID = bc.getDownHop();
            long fromID = bc.getUpHop();
            //エッジルータでない場合は，通常転送
            if ((toID != CCNUtil.MINUS_VAUE) /*&& (fromID != CCNUtil.MINUS_VAUE)*/) {
                //toIDのルータに対してInterestを送る．
                ForwardHistory f = new ForwardHistory(r.getRouterID(), CCNUtil.NODETYPE_ROUTER, toID, CCNUtil.NODETYPE_ROUTER,
                        System.currentTimeMillis(), -1);
                //BCフラグを立てる．
                f.setForwardedByBC(true);
                CCNRouter targetRouter = CCNMgr.getIns().getRouterMap().get(toID);
                long minBW = Math.min(Math.min(r.getBw(), targetRouter.getBw()), p.getMinBW());
                p.setMinBW(minBW);
                p.getHistoryList().add(f);
                targetRouter.getInterestQueue().offer(p);
                ret = true;
            } else {
                //もしDLしたノード側のエッジノードであれば，当該ノードが保持している．
                //よって，CSから探す．そして見つかれば，upHopへデータを送る．
                //upHopへ送る．
                if (toID == CCNUtil.MINUS_VAUE) {
                    CCNRouter upHopRouter = CCNMgr.getIns().getRouterMap().get(fromID);
                    //少なくともfromID側はルータである．
                    if (r.getCSEntry().getCacheMap().containsKey(p.getPrefix())) {
                        //CSにあればOK.
                        CCNContents c = r.getCSEntry().getCacheMap().get(p.getPrefix());
                        //BCフラグを立てる．
                        c.setBC(true);

                        // CCNContents c = this.CSEntry.getCacheMap().get(p.getPrefix());
                        //後は送信処理だが，分割して送る？
                        // System.out.println("<CS内キャッシュを要求元ルータへ返送> from " + "Router" + this.routerID + "-->" + "Router" + toID + " for prefix:" + p.getPrefix());
                        ForwardHistory f = new ForwardHistory(r.getRouterID(), CCNUtil.NODETYPE_ROUTER, upHopRouter.getRouterID(), CCNUtil.NODETYPE_ROUTER,
                                System.currentTimeMillis(), -1);
                        f.setForwardedByBC(true);
                        c.getHistoryList().add(f);
                        upHopRouter.forwardData(c);

                        LinkedList<ForwardHistory> fList = p.getHistoryList();
                        ForwardHistory h = fList.getLast();

                        CCNLog.getIns().log(",4,"+p.getPrefix()+","+"-,"+fList.getFirst().getStartTime()+","+h.getArrivalTime()+","+(h.getArrivalTime()-fList.getFirst().getStartTime())+","+
                                p.getFromNodeId()+","+r.getRouterID()+","+fList.size()+","+"-,"+"o"+",o");
                        ret = true;
                    } else {
                        //なければNG．
                        ret = false;
                    }
                }
                //オリジナルソースのエッジルータであれば，FIBから探す．
           /*     if (fromID == CCNUtil.MINUS_VAUE) {
                    if (r.getFIBEntry().getTable().containsKey(p.getPrefix())) {
                        LinkedList<Face> fList = r.getFIBEntry().getTable().get(p.getPrefix());
                        Iterator<Face> fIte = fList.iterator();
                        boolean found = false;
                        Long nodeID = CCNUtil.MINUS_VAUE;
                        while(fIte.hasNext()){
                            Face f = fIte.next();
                            if(f.getType() == CCNUtil.NODETYPE_NODE){
                                found = true;
                                //タイプがノードであればOK
                                nodeID = f.getPointerID();
                                break;
                            }
                        }
                        if(found){
                            //見つかれば，ノードへInterest転送する．
                            CCNNode node = CCNMgr.getIns().getNodeMap().get(nodeID);
                            node.getInterestQueue().offer(p);
                            ret = true;
                        }else{
                            ret = false;
                        }
                    }else{
                        ret = false;
                    }
                }*/
            }
        } else {
            ret = false;
        }
        return ret;
    }
}



