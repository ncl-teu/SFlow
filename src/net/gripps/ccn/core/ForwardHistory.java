package net.gripps.ccn.core;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by Hidehiro Kanemitsu on 2018/12/03.
 */
public class ForwardHistory implements Serializable {
    /**
     * ルータID or ノードID
     */
    Long fromID;

    //ルータにいたのか，それともノードにいたのか
    int fromType;

    //宛先のID（送出時にセットされる）
    Long toID;

    //宛先のタイプ（ルータ/ノード）
    int toType;

    //送出時刻（送出時にInterestパケットに設定される）
    long startTime;

    //到着時刻（相手に到着時に，セットされる）
    long arrivalTime;

    long maxConnectionNum;

    /**
     * BCによって転送されたかどうか
     */
    boolean isForwardedByBC;

    private HashMap<String, Object> customMap;

    public ForwardHistory(Long fromID, int fromType, Long toID, int toType, long startTime, long arrivalTime) {
        this.fromID = fromID;
        this.fromType = fromType;
        this.toID = toID;
        this.toType = toType;
        this.startTime = startTime;
        this.arrivalTime = arrivalTime;
        this.maxConnectionNum = 0;
        this.isForwardedByBC = false;
        this.customMap = new HashMap<String, Object>();

    }

    public HashMap<String, Object> getCustomMap() {
        return customMap;
    }

    public void setCustomMap(HashMap<String, Object> customMap) {
        this.customMap = customMap;
    }

    public boolean isForwardedByBC() {
        return isForwardedByBC;
    }

    public void setForwardedByBC(boolean forwardedByBC) {
        isForwardedByBC = forwardedByBC;
    }

    public long getMaxConnectionNum() {
        return maxConnectionNum;
    }

    public void setMaxConnectionNum(long maxConnectionNum) {
        this.maxConnectionNum = maxConnectionNum;
    }

    public Long getFromID() {
        return fromID;
    }

    public void setFromID(Long fromID) {
        this.fromID = fromID;
    }

    public int getFromType() {
        return fromType;
    }

    public void setFromType(int fromType) {
        this.fromType = fromType;
    }

    public Long getToID() {
        return toID;
    }

    public void setToID(Long toID) {
        this.toID = toID;
    }

    public int getToType() {
        return toType;
    }

    public void setToType(int toType) {
        this.toType = toType;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(long arrivalTime) {
        this.arrivalTime = arrivalTime;
    }
}
