package net.gripps.environment;

/**
 * Author: H. Kanemitsu
 * Date: 11/06/14
 */
public class CPUInfo {
    private Long fromID;
    private Long toID;
    private Long cpuSpeed;

    public CPUInfo(Long fromID, Long toID, Long cpuSpeed) {
        this.fromID = fromID;
        this.toID = toID;
        this.cpuSpeed = cpuSpeed;
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

    public Long getCpuSpeed() {
        return cpuSpeed;
    }

    public void setCpuSpeed(Long cpuSpeed) {
        this.cpuSpeed = cpuSpeed;
    }
}
