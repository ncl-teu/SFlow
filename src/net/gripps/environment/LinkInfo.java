package net.gripps.environment;

/**
 * Author: H. Kanemitsu
 * Date: 2009/12/17
 */
public class LinkInfo {
    private Long linkSpeed;
    private Long fromID;
    private Long toID;

    private Long averageLinkSpeed;

    public LinkInfo(Long linkSpeed, Long fromID, Long toID) {
        this.linkSpeed = linkSpeed;
        this.fromID = fromID;
        this.toID = toID;
    }

    public LinkInfo(Long linkSpeed, Long fromID, Long toID, Long aSpeed) {
        this.linkSpeed = linkSpeed;
        this.fromID = fromID;
        this.toID = toID;
         this.averageLinkSpeed = aSpeed;
    }


    public Long getAverageLinkSpeed() {
        return averageLinkSpeed;
    }

    public void setAverageLinkSpeed(Long averageLinkSpeed) {
        this.averageLinkSpeed = averageLinkSpeed;
    }

    public Long getLinkSpeed() {
        return linkSpeed;
    }

    public void setLinkSpeed(Long linkSpeed) {
        this.linkSpeed = linkSpeed;
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
}
