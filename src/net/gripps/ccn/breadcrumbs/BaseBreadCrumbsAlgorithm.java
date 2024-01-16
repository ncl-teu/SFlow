package net.gripps.ccn.breadcrumbs;

import net.gripps.ccn.CCNUtil;
import net.gripps.ccn.core.CCNContents;
import net.gripps.ccn.core.CCNRouter;
import net.gripps.ccn.core.ForwardHistory;
import net.gripps.ccn.core.InterestPacket;

public abstract class BaseBreadCrumbsAlgorithm {

    /*
     *  ルータからデータ転送時に呼ばれるメソッドで, BCを新規作成して残す処理です．
     *  また，エッジ側CCRルータにおいて，CCNコンテンツを配置します．
     *  もしこれらの処理をすればtrue/しなければfalseを返す．
     * @param r 送信元ルータ
     * @param c
     */
    public  abstract boolean createBC(CCNRouter r, CCNContents c);


    /**
     * 指定のprerfixのBCがあるかをチェックし，もしあればBCのdownHopの指し示す方へ行く．
     *
     * @param r
     * @param p
     * @return
     */
    public abstract boolean forwardRequestByBC(CCNRouter r, InterestPacket p);

    /**
     *
     * @param r
     * @param c
     * @return
     */
    public abstract boolean forwardBCData(CCNRouter r, CCNContents c);




    }
