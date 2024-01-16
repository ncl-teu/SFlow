package net.gripps.environment;

import java.io.Serializable;
import java.util.TreeMap;

/**
 * Author: H. Kanemitsu
 * Date: 13/05/13
 */
public class Machine implements Serializable {

    /**
     * このマシン自体のID
     */
    private long machineID;

        /**
     * このマシンがもつコアのマップ
     */
    private TreeMap<Long, CPU> cpuMap;

    private int maxCoreNum;

    private long bw;

    public Machine(long machineID, TreeMap<Long, CPU> cpuMap, int num) {
        this.machineID = machineID;
        this.cpuMap = cpuMap;
        this.maxCoreNum = num;
        this.bw = 0;
    }

    public boolean addCore(CPU core){
        //すでに規定サイズに達していれば，ダメ
        if(this.cpuMap.size() >= this.maxCoreNum){
            return false;

        }else{
            if(this.cpuMap.containsKey(core.getCpuID())){
                return false;
            }else{
               this.cpuMap.put(core.getCpuID(), core);
               //コアの所属マシンIDを自分にセットする．
                core.setMachineID(this.machineID);
               return true;
            }

        }
    }

    public int getMaxCoreNum() {
        return maxCoreNum;
    }

    public void setMaxCoreNum(int maxCoreNum) {
        this.maxCoreNum = maxCoreNum;
    }

    public long getMachineID() {
        return machineID;
    }

    public void setMachineID(long machineID) {
        this.machineID = machineID;
    }

    public TreeMap<Long, CPU> getCpuMap() {
        return cpuMap;
    }

    public void setCpuMap(TreeMap cpuMap) {
        this.cpuMap = cpuMap;
    }

    public long getBw() {
        return bw;
    }

    public void setBw(long bw) {
        this.bw = bw;
    }
}
