package net.gripps.ccn.caching;

import net.gripps.ccn.core.CCNContents;
import net.gripps.ccn.core.CCNNode;
import net.gripps.ccn.core.CCNRouter;

import java.util.HashMap;

public abstract class BaseCachingAlgorithm {

    /**
     * ノードの集合
     */
    protected HashMap<Long, CCNNode> nodeMap;

    /**
     * ルータの集合
     */
    protected HashMap<Long, CCNRouter> routerMap;

    /**
     *
     * @param nodeMap
     * @param routerMap
     */
    public BaseCachingAlgorithm(HashMap<Long, CCNNode> nodeMap, HashMap<Long, CCNRouter> routerMap) {
        this.nodeMap = nodeMap;
        this.routerMap = routerMap;
    }

    public BaseCachingAlgorithm() {
    }

    /**
     * 指定のコンテンツを，指定のルータにおいてキャッシュするかどうかを決めるメソッドです．
     * 実装した場合，特定の条件でCSにキャッシュ（もしくはしない）してから，キャッシュしたらtrue，
     * しなければfalseを返すようにしてください．
     * @param c
     * @param r
     * @return
     */
    public abstract boolean cachingProcess(CCNContents c, CCNRouter r);

    /**
     * PITエントリにコンテンツのprefixがない場合，どうするかを決めるためのメソッドです．
     * もしCSにキャッシュしたのであればtrue，キャッシュしなかったらfalseを返してください．
     * @param c
     * @param r
     * @return
     */
    public abstract boolean chachingProcessIfNoPITEntry(CCNContents c, CCNRouter r);

    /**
     * CSがいっぱいだった場合の処理
     * @param c
     * @param r
     * @return
     */
    public abstract boolean chachingIFCSFULL(CCNContents c, CCNRouter r);

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
