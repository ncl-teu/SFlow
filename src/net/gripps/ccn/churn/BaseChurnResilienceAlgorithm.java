package net.gripps.ccn.churn;

import net.gripps.ccn.core.CCNRouter;
import net.gripps.ccn.fibrouting.BaseRouting;

import java.util.HashMap;

/**
 * Churn耐性を実現するためのベースとなるクラスです．
 * 参加・離脱の関するメソッドを持っています．
 * 参加では，FIB/PIT（＋CSも？）を他ルータからもらい，
 * 一方，離脱では，FIB/PIT(+CSも？）を他ルータへ渡すことを
 * します．また，どのルータから（へ）もらう（渡す）かの
 * 基準は，実装するアルゴリズムに依存します．
 * また，BCがONになっている場合は，BCの受け渡しも必要になるかと
 * 思われます．
 *
 */
public abstract class BaseChurnResilienceAlgorithm {

    BaseRouting routing;


    public BaseChurnResilienceAlgorithm(BaseRouting routing) {
        this.routing = routing;
    }

    /**
     * CCNに新規参加するためのメソッドです．
     * 新規参加できたらtrue,参加できなかったらfalseを返してください．
     * @param r
     * @param routerMap 参加前のルータ集合
     * @return
     */
    public abstract boolean ccnJoin(CCNRouter r, HashMap<Long, CCNRouter>routerMap);

    /**
     * CCNから離脱するためのメソッドです．
     * 離脱できたらtrue，離脱できなかったらfalseを返してください．
     * @param r
     * @param routerMap 離脱前のルータ集合
     * @return
     */
    public abstract boolean ccnLeave(CCNRouter r, HashMap<Long, CCNRouter> routerMap);


}
