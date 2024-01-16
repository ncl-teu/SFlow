package net.gripps.ccn.breadcrumbs;

import net.gripps.ccn.CCNUtil;

/**
 * BreadCrumbsを表すクラスです．
 * ルータがこのBCを保持する．
 * Interestパケットが来て，prefixが一致するBCがあれば，
 * そのBCが指し示す場所へ転送する．
 * downHop or upHopがnullならば，その場所がBC trailの最後（最初）．
 * (オリジナルファイルの位置方向)upHop -> BC保持Router -> downHop(キャッシュの位置方向)
 * という関係である．
 *
 * upHopがnull->BC保持ルータのFIBに，オリジナルファイル保持ノードのIDがあるはず．
 * downHopがnull->BC保持ルータが，キャッシュコンテンツを持っている．
 *
 */
public class BC {

    /**
     * コンテンツのprefixをそのまま設定してください．
     * ContentIDのこと．
     */
    protected String prefix;

    /**
     * このBC保持ルータの，前ルータのID
     */
    protected  long upHop;

    /**
     * このBC保持ルータの，後ルータのID
     */
    protected  long downHop;

    /**
     * 最後にダウンロードされた時刻．
     * つまり，このルータをファイルが通過した時刻
     */
    protected long downloadTime;

    /**
     * 最後にリクエストされた時刻
     */
    protected long requestTime;

    public BC(String prefix, long upHop, long downHop, long downloadTime) {
        this.prefix = prefix;
        this.upHop = upHop;
        this.downHop = downHop;
        this.downloadTime = downloadTime;
        this.requestTime = CCNUtil.MINUS_VAUE;

    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public long getUpHop() {
        return upHop;
    }

    public void setUpHop(long upHop) {
        this.upHop = upHop;
    }

    public long getDownHop() {
        return downHop;
    }

    public void setDownHop(long downHop) {
        this.downHop = downHop;
    }

    public long getDownloadTime() {
        return downloadTime;
    }

    public void setDownloadTime(long downloadTime) {
        this.downloadTime = downloadTime;
    }

    public long getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(long requestTime) {
        this.requestTime = requestTime;
    }
}
