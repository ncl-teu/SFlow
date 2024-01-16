package net.gripps.environment;

/**
 * Author: H. Kanemitsu
 * Date: 2010/06/05
 */
public class HopInfo {

    private Long fromID;
    private Long toID;
    private Long hop;

    public HopInfo(Long fromID, Long toID, Long hop) {
        this.fromID = fromID;
        this.toID = toID;
        this.hop = hop;
    }

    public Long getFromID() {
        return fromID;
    }

    public void setFromID(Long fromID) {
        this.fromID = fromID;
    }

    public Long getToID() {
        return toID;
    }

    public void setToID(Long toID) {
        this.toID = toID;
    }

    public Long getHop() {
        return hop;
    }

    public void setHop(Long hop) {
        this.hop = hop;
    }
}
