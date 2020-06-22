package org.ncl.workflow.ccn.autoicnsfc;

import net.gripps.clustering.common.aplmodel.DataDependence;

import java.io.Serializable;

/**
 * Created by Hidehiro Kanemitsu on 2020/02/27.
 * 仮想的な依存関係を表します．
 */
public class VDependence implements Serializable {
    /**
     * ソース
     */
    Long fromID;

    /**
     * 宛先
     */
    Long toID;

    /**
     * 親となるべきSF ID
     */
    Long parentID;

    public VDependence(Long fromID, Long toID, Long parentID) {
        this.fromID = fromID;
        this.toID = toID;
        this.parentID = parentID;
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

    public Long getParentID() {
        return parentID;
    }

    public void setParentID(Long parentID) {
        this.parentID = parentID;
    }
}
