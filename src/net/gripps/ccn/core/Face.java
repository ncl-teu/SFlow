package net.gripps.ccn.core;

import net.gripps.cloud.core.VCPU;
import net.gripps.cloud.nfv.sfc.VNF;

import java.util.HashMap;

/**
 * Created by kanemih on 2018/11/02.
 */
public class Face {

    /**
     * Face ID．
     */
    private Long faceID;

    /**
     * このFaceが紐付いている先のID
     * ルータ，もしくはノードのIDを記載する．
     */
    private Long pointerID;

    /**
     * pointaerIDが指し示す先がルータなのか，もしくはノードなのか
     * を意味します．
     */
    private int type;

    /**
     * 優先度．この値が小さければ良い．
     */
    private double metric;


    /**
     * 割り当て済みMap.
     * <JobID, <TaskID, vCPUID>>
     */
    private HashMap<Long, HashMap<Long, String>> allocMap;




    public Face(Long faceID, Long pointerID, int type) {
        this.faceID = faceID;
        this.pointerID = pointerID;
        this.type = type;
        this.metric = 0.0d;
        this.allocMap = new HashMap<Long, HashMap<Long, String>>();
    }

    /**
     * VNFを指定vCPUへ割り当てる．上書きしてしまう．
     * @param vnf
     * @param vcpu
     */
    public void allocateVNF(VNF vnf, VCPU vcpu){
        Long jobID = vnf.getIDVector().get(0);
        if(this.allocMap.containsKey(jobID)){
            HashMap<Long, String> subMap = this.allocMap.get(jobID);
            subMap.put(vnf.getIDVector().get(1), vcpu.getPrefix());
        }else{
            //JobIDのキーがなければ，新規作成．
            HashMap<Long, String> newMap = new HashMap<Long, String>();
            newMap.put(vnf.getIDVector().get(1), vcpu.getPrefix());
            this.allocMap.put(jobID, newMap);
        }

    }

    /**
     *
     * @param vnf
     * @return 割り当て済みならvCPUのprefix, なければnullを返す．
     */
    public String  isAllocated(VNF vnf){
        Long jobID = vnf.getIDVector().get(0);
        if(this.allocMap.containsKey(jobID)){
            HashMap<Long, String> subMap = this.allocMap.get(jobID);
            if (subMap.containsKey(vnf.getIDVector().get(1))) {
                return subMap.get(vnf.getIDVector().get(1));
            }else{
                return null;
            }
        }else{
            //キーがなければ，nullを返す．
            return null;
        }
    }

    public HashMap<Long, HashMap<Long, String>> getAllocMap() {
        return allocMap;
    }

    public void setAllocMap(HashMap<Long, HashMap<Long, String>> allocMap) {
        this.allocMap = allocMap;
    }

    public Long getFaceID() {
        return faceID;
    }

    public void setFaceID(Long faceID) {
        this.faceID = faceID;
    }

    public Long getPointerID() {
        return pointerID;
    }

    public void setPointerID(Long pointerID) {
        this.pointerID = pointerID;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public double getMetric() {
        return metric;
    }

    public void setMetric(double metric) {
        this.metric = metric;
    }
}
