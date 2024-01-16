package net.gripps.clustering.common.aplmodel;

import java.util.Vector;
import java.io.Serializable;

/**
 * Author: Hidehiro Kanemitsu<br>
 * Date: 2007/07/06
 */
public class ControlDependence implements Serializable{

    /**
     * Task which belongs to "From" in this dependency
     */
    private Vector<Long> labelFromID;

    /**
     * Task which belongs to "To" in this dependency
     */
    private Vector<Long> labelToID;

    /**
     * Actual destination task
     */
    private Vector<Long> CsucID;

   /**
    *
    * @param from
    * @param to
    * @param suc
    */
    public ControlDependence(Vector<Long> from,
                             Vector<Long> to,
                             Vector<Long> suc){
       this.setCsucID(suc);
       this.setLabelFromID(from);
       this.setLabelToID(to);


    }


    public Vector<Long> getLabelFromID() {
        return labelFromID;
    }

    public void setLabelFromID(Vector<Long> labelFromID) {
        this.labelFromID = labelFromID;
    }

    public Vector<Long> getLabelToID() {
        return labelToID;
    }

    public void setLabelToID(Vector<Long> labelToID) {
        this.labelToID = labelToID;
    }

    public Vector<Long> getCsucID() {
        return CsucID;
    }

    public void setCsucID(Vector<Long> csucID) {
        CsucID = csucID;
    }
}
