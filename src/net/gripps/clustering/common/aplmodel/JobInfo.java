package net.gripps.clustering.common.aplmodel;

/**
 * Created by kanemih on 2016/10/01.
 */
public class JobInfo {
    private double CCR;

    private String fName;

    public double getCCR() {
        return CCR;
    }

    public void setCCR(double CCR) {
        this.CCR = CCR;
    }

    public String getfName() {
        return fName;
    }

    public void setfName(String fName) {
        this.fName = fName;
    }

    public JobInfo(double CCR, String fName) {

        this.CCR = CCR;
        this.fName = fName;
    }
}
