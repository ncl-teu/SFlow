package net.gripps.environment;

import net.gripps.clustering.common.aplmodel.AbstractTask;
import net.gripps.clustering.common.aplmodel.CustomIDSet;
import net.gripps.scheduling.algorithms.heterogeneous.heft.StartTimeComparator;

import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Vector;
import java.util.TreeSet;
import java.io.Serializable;

/**
 * Author: H. Kanemitsu
 * マシンを意味するクラスです．
 * マシン速度やリンク速度は，Environmentクラスで定義されます．<br>
 * このクラスでは，主に
 * - 割り当てられたタスク番号のリスト
 * - スケジュール後のタスク番号のリスト
 * を格納します．
 *
 * Date: 2008/10/06
 */
public class CPU implements Serializable {

    /**
     * このマシンの速度
     */
    private long speed;

    /**
     * CPUのID
     */
    private Long cpuID;

    /**
     * このCPUが属するマシンのID
     */
    private long MachineID;

    /**
     * このマシンに割り当てられたタスククラスタID
     */
    private Long taskClusterID;

    private long maxLinkSpeed;

    private TreeSet<LinkInfo> inLinkSet;

    private TreeSet<LinkInfo> outLinkSet;

    private CustomIDSet nbrSet;

    /**
     * 仮想的なプロセッサかどうか
     */
    private boolean isVirtual;

    /**
     * 複数のグループ用
     */
    private CustomIDSet nbrSet_hop;

    private CustomIDSet nbrSet_cpu;

    private CustomIDSet nbrSet_bw;

    private long minLink;

    private long thresholdTime;

    private long delta_WSL;

    protected long finishTime;

    protected PriorityQueue<AbstractTask> ftQueue;

    protected double powerRate;

    protected long processingCapacity;

    protected long bw;

    protected Long oldCPUID;




    /**
     * @param id
     * @param speed
     * @param assignedTaskList
     * @param scheduledTaskList
     */
    public CPU(Long id, long speed, Vector<Long> assignedTaskList, Vector<Long> scheduledTaskList) {
        this.speed = speed;
        this.taskClusterID = new Long(-1);
        this.cpuID = id;
        this.maxLinkSpeed = 0;
        this.inLinkSet = new TreeSet<LinkInfo>(new LinkComparator());
        this.outLinkSet = new TreeSet<LinkInfo>(new LinkComparator());
        this. nbrSet = new CustomIDSet();
        this.nbrSet_bw = new CustomIDSet();
        this.nbrSet_cpu = new CustomIDSet();
        this.nbrSet_hop = new CustomIDSet();
        this.isVirtual = false;

        this.minLink = 0;
        this.thresholdTime = 0;

        this.MachineID = -1;
        this.delta_WSL = -1;
        this.finishTime = 0;
        this.ftQueue = new PriorityQueue<AbstractTask>(5, new StartTimeComparator());
        this.powerRate = 0.0;
        this.processingCapacity = 0;
        this.bw = 0;
        this.oldCPUID = id;



    }

    public Long getOldCPUID() {
        return oldCPUID;
    }

    public void setOldCPUID(Long oldCPUID) {
        this.oldCPUID = oldCPUID;
    }

    public void clear(){


        this.taskClusterID = new Long(-1);

        this.inLinkSet = new TreeSet<LinkInfo>(new LinkComparator());
        this.outLinkSet = new TreeSet<LinkInfo>(new LinkComparator());
        this. nbrSet = new CustomIDSet();
        this.nbrSet_bw = new CustomIDSet();
        this.nbrSet_cpu = new CustomIDSet();
        this.nbrSet_hop = new CustomIDSet();
        this.isVirtual = false;


        this.thresholdTime = 0;

        this.delta_WSL = -1;
        this.finishTime = 0;
        this.powerRate = 0.0;
        this.processingCapacity = 0;
        this.setTaskClusterID(new Long(-1));
        this.ftQueue.clear();


    }





    /**
     *
     */
    public CPU() {
        this.speed = 0;
        this.taskClusterID = new Long(-1);
        this.cpuID = new Long(0);
        this.maxLinkSpeed = 0;
        this.inLinkSet = new TreeSet<LinkInfo>(new LinkComparator());
        this.outLinkSet = new TreeSet<LinkInfo>(new LinkComparator());
        this. nbrSet = new CustomIDSet();
        this.nbrSet_bw = new CustomIDSet();
        this.nbrSet_cpu = new CustomIDSet();
        this.nbrSet_hop = new CustomIDSet();
        this.isVirtual = false;
        this.minLink = 0;
        this.thresholdTime = 0;
                this.MachineID = -1;
        this.delta_WSL = -1;
        this.finishTime = 0;
        this.bw = 0;
        this.oldCPUID = new Long(0);


    }

    public long getBw() {
        return bw;
    }

    public void setBw(long bw) {
        this.bw = bw;
    }

    public double getPowerRate() {
        return powerRate;
    }

    public void setPowerRate(double powerRate) {
        this.powerRate = powerRate;
    }

    public long getProcessingCapacity() {
        return processingCapacity;
    }

    public void setProcessingCapacity(long processingCapacity) {
        this.processingCapacity = processingCapacity;
    }

    public long getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(long finishTime) {
        this.finishTime = finishTime;
    }

    public long getMachineID() {
        return MachineID;
    }

    public void setMachineID(long machineID) {
        MachineID = machineID;
    }

    public boolean isVirtual() {
        return isVirtual;
    }

    public void setVirtual(boolean virtual) {
        isVirtual = virtual;
    }

    /**
     *
     * @return
     */
    public long getSpeed() {
        return speed;
    }

    /**
     *
     * @param speed
     */
    public void setSpeed(long speed) {
        this.speed = speed;
        
        
    }

    public long getDelta_WSL() {
        return delta_WSL;
    }

    public void setDelta_WSL(long delta_WSL) {
        this.delta_WSL = delta_WSL;
    }

    public long getMinLink() {
        return minLink;
    }

    public void setMinLink(long minLink) {
        this.minLink = minLink;
    }

    public long getMaxLinkSpeed() {
        return maxLinkSpeed;
    }

    public void setMaxLinkSpeed(long maxLinkSpeed) {
        this.maxLinkSpeed = maxLinkSpeed;
    }

    /**
     *
     * @return
     */
    public Long getTaskClusterID() {
        return taskClusterID;
    }

    /**
     *
     * @param taskClusterID
     */
    public void setTaskClusterID(Long taskClusterID) {
        this.taskClusterID = taskClusterID;
    }

    public Long getCpuID() {
        return cpuID;
    }

    public void setCpuID(Long cpuID) {
        this.cpuID = cpuID;
    }


    public TreeSet<LinkInfo> getInLinkSet() {
        return inLinkSet;
    }

    public void setInLinkSet(TreeSet<LinkInfo> inLinkSet) {
        this.inLinkSet = inLinkSet;
    }

    public TreeSet<LinkInfo> getOutLinkSet() {
        return outLinkSet;
    }

    public void setOutLinkSet(TreeSet<LinkInfo> outLinkSet) {
        this.outLinkSet = outLinkSet;
    }

    public CustomIDSet getNbrSet() {
        return nbrSet;
    }

    public void setNbrSet(CustomIDSet nbrSet) {
        this.nbrSet = nbrSet;
    }

    public void addNbr(Long id){
        this.nbrSet.add(id);
    }

    public void addNbr(long id){
        this.nbrSet.add(new Long(id));
    }

    public CustomIDSet getNbrSet_hop() {
        return nbrSet_hop;
    }

    public void setNbrSet_hop(CustomIDSet nbrSet_hop) {
        this.nbrSet_hop = nbrSet_hop;
    }

    public CustomIDSet getNbrSet_cpu() {
        return nbrSet_cpu;
    }

    public void setNbrSet_cpu(CustomIDSet nbrSet_cpu) {
        this.nbrSet_cpu = nbrSet_cpu;
    }

    public CustomIDSet getNbrSet_bw() {
        return nbrSet_bw;
    }

    public void setNbrSet_bw(CustomIDSet nbrSet_bw) {
        this.nbrSet_bw = nbrSet_bw;
    }

    public long getThresholdTime() {
        return thresholdTime;
    }

    public void setThresholdTime(long thresholdTime) {
        this.thresholdTime = thresholdTime;
    }

    public PriorityQueue<AbstractTask> getFtQueue() {
        return ftQueue;
    }

    public void setFtQueue(PriorityQueue<AbstractTask> ftQueue) {
        this.ftQueue = ftQueue;
    }

    public long getEndTime(){
        Object[] oa = this.getFtQueue().toArray();
        //開始時刻の小さい順にソートする．
        Arrays.sort(oa, new StartTimeComparator());
        int len = oa.length;
        AbstractTask t =((AbstractTask)oa[len-1]);
        return t.getStartTime() + t.getMaxWeight()/this.getSpeed();
    }

}
