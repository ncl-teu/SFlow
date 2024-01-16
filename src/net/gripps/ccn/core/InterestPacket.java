package net.gripps.ccn.core;

import net.gripps.ccn.CCNUtil;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by kanem on 2018/11/12.
 */
public class InterestPacket {

    /**
     * ほしいコンテンツ名
     */
    String prefix;

    /**
     * 当該パケットのID
     */
    Long ID;

    /**
     * パケット長
     */
    long  length;

    /**
     * このインタレストパケットの送信元となる
     * ノードID
     */
    Long fromNodeId;

    /**
     * 当該ノードの何番目のインタレストパケットか
     */
    long count;

    /**
     *
     */
    long minBW;

    /**
     * BCによって転送されたかどうかのフラグ
     */
    boolean isForwardedByBC;

    /**
     * 転送履歴のリスト．この中身を見れば詳細情報がわかる．
     */
    LinkedList<ForwardHistory> historyList;

    protected HashMap<String, Object> appParams;

    public InterestPacket(String prefix, Long ID, long length, Long fromNodeId, long count, LinkedList<ForwardHistory> historyList) {
        this.prefix = prefix;
        this.ID = ID;
        this.length = length;
        this.fromNodeId = fromNodeId;
        this.count = count;
        this.historyList = historyList;
        this.minBW = CCNUtil.MAXValue;
        this.isForwardedByBC = false;
        this.appParams = new HashMap<String, Object>();
    }

    public HashMap<String, Object> getAppParams() {
        return appParams;
    }

    public void setAppParams(HashMap<String, Object> appParams) {
        this.appParams = appParams;
    }

    public long getMinBW() {
        return minBW;
    }

    public void setMinBW(long minBW) {
        this.minBW = minBW;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public LinkedList<ForwardHistory> getHistoryList() {
        return historyList;
    }

    public void setHistoryList(LinkedList<ForwardHistory> historyList) {
        this.historyList = historyList;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public Long getID() {
        return ID;
    }

    public void setID(Long ID) {
        this.ID = ID;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long  length) {
        this.length = length;
    }

    public Long getFromNodeId() {
        return fromNodeId;
    }

    public void setFromNodeId(Long fromNodeId) {
        this.fromNodeId = fromNodeId;
    }

    public boolean isForwardedByBC() {
        return isForwardedByBC;
    }

    public void setForwardedByBC(boolean forwardedByBC) {
        isForwardedByBC = forwardedByBC;
    }
}
