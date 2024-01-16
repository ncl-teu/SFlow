package net.gripps.ccn.caching;

import net.gripps.ccn.CCNUtil;
import net.gripps.ccn.core.CCNContents;
import net.gripps.ccn.core.CCNNode;
import net.gripps.ccn.core.CCNRouter;
import net.gripps.ccn.core.Face;
import net.gripps.ccn.process.CCNMgr;

import java.util.HashMap;
import java.util.Iterator;

public class TestCacheingAlgorithm  extends BaseCachingAlgorithm{
    public TestCacheingAlgorithm(HashMap<Long, CCNNode> nodeMap, HashMap<Long, CCNRouter> routerMap) {
        super(nodeMap, routerMap);
    }

    public TestCacheingAlgorithm() {
        super();

    }

    /**
     * 期待値を計算してみよう
     * @param r
     * @return
     */
    public double calcEXvalue(CCNRouter r){
        //まずは今までの参加時間を計算する．
        long current = System.currentTimeMillis();
        long lifeTime = current - r.getJoinTime();

        //指数分布に従って，次は何秒後にinterestが来るか
        //の期待値を計算する．
        //そのために，
        return 0.0;
    }

    @Override
    public boolean cachingProcess(CCNContents c, CCNRouter r) {
        //r: 自分自身がやってくる．今回は，隣接の中から決めるので，
        //選ぶ処理が必要．
        //faceリストを取得する．
        Iterator<Face> rIte = r.getFace_routerMap().values().iterator();
        while(rIte.hasNext()){
            //faceを取得する．
            Face face = rIte.next();
            //faceから，それに該当するルータを取得する．
            CCNRouter nRouter = CCNMgr.getIns().getRouterMap().get(face.getPointerID());
            double exValue = this.calcEXvalue(nRouter);




        }

        //コンテンツの中身をキャッシュとする．
        c.setCache(true);
        //履歴をクリアする．
        c.getHistoryList().clear();
        c.setCurrentOwnerID(r.getRouterID());
        c.setGeneratedTimeAtCache(System.currentTimeMillis());
        //rのcsへ追加する．
        r.getCSEntry().getCacheMap().put(c.getPrefix(), c);
        //その後，faceリストの他ルータのFIBに追加してあげる？
        Face f= new Face(null, r.getRouterID(), CCNUtil.NODETYPE_ROUTER);
        //隣接ルータに対して，それらのFIBに，当該ルータへのポインタを追加しておく．
        //this.addFacetoFIB(c.getPrefix(), f, r);
        Iterator<CCNContents> cIte = r.getCSEntry().getCacheMap().values().iterator();

        return false;
    }

    @Override
    public boolean chachingProcessIfNoPITEntry(CCNContents c, CCNRouter r) {
        return false;
    }

    @Override
    public boolean chachingIFCSFULL(CCNContents c, CCNRouter r) {
        return false;
    }

    @Override
    public HashMap<Long, CCNNode> getNodeMap() {
        return super.getNodeMap();
    }

    @Override
    public void setNodeMap(HashMap<Long, CCNNode> nodeMap) {
        super.setNodeMap(nodeMap);
    }

    @Override
    public HashMap<Long, CCNRouter> getRouterMap() {
        return super.getRouterMap();
    }

    @Override
    public void setRouterMap(HashMap<Long, CCNRouter> routerMap) {
        super.setRouterMap(routerMap);
    }
}
