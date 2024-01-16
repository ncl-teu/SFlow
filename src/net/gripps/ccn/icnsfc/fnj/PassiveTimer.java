package net.gripps.ccn.icnsfc.fnj;

import net.gripps.ccn.CCNUtil;
import net.gripps.ccn.core.*;
import net.gripps.ccn.icnsfc.AutoUtil;
import net.gripps.ccn.process.CCNMgr;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.TimerTask;

public class PassiveTimer extends TimerTask implements Runnable{

    private CCNRouter router;



    public PassiveTimer(CCNRouter r){
        this.router = r;

    }

    @Override
    public void run() {

        //アイドル状態であれば，タスク処理結果の要求Interestパケットを送信する．
        if(this.router.getInterestQueue().isEmpty() && this.router.getContentsQueue().isEmpty()){
            FIB fib = this.router.getFIBEntry();
            //よくわかんないから，とりあえず相手が持っている全てのキャッシュをもらう．
            Iterator<String> pIte = this.router.getFIBEntry().getTable().keySet().iterator();


            //FIBのprefixに対するループ（レコード単位）
            LinkedList<Face> faceList = new LinkedList<Face>();

            //String prefix = cache.getPrefix();
            while(pIte.hasNext()){
                LinkedList<Face> fList = fib.getTable().get(pIte.next());
                Iterator<Face> fIte = fList.iterator();
                while(fIte.hasNext()){
                    Face f = fIte.next();
                    if(f.getType() == CCNUtil.NODETYPE_ROUTER){

                        CCNRouter router = CCNMgr.getIns().getRouterMap().get(f.getPointerID());
                        LinkedList<ForwardHistory> forwardList = new LinkedList<ForwardHistory>();

                        InterestPacket p = new InterestPacket(AutoUtil.FNJ_PASSIVE, new Long(-1),
                                1500, this.router.getRouterID(), -1,forwardList );
                        //Cacheの情報をInterestパケットにセットする．
                        //p.getAppParams().put(AutoUtil.FNJ_ACTIVE, cache);
                        //Faceに向けて情報を送信する．
                        ForwardHistory newHistory = new ForwardHistory(this.router.getRouterID(), CCNUtil.NODETYPE_ROUTER,
                                (long)router.getRouterID(), CCNUtil.NODETYPE_ROUTER, System.currentTimeMillis(), -1);
                        p.getHistoryList().add(newHistory);
                        //Face tFace = null;

                        //ルータへ転送する．
                        router.forwardInterest(f,p);
                    }
                }

            }


        }
    }

    public CCNRouter getRouter() {
        return router;
    }

    public void setRouter(CCNRouter router) {
        this.router = router;
    }
}
