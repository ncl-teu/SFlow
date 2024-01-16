package net.gripps.clustering.common.aplmodel;

import net.gripps.clustering.common.aplmodel.TaskCluster;

import java.util.*;

/**
 * Author: Hidehiro Kanemitsu<br>
 * Date: 2007/07/17
 */
public class BBTask extends AbstractTask{

    /**
     *
     */
   // private LinkedList<TaskCluster> taskClusterList;

    private Hashtable<Long, TaskCluster> taskClusterList;

    private long makeSpan;


    private long worstLevel;

    private long cpLen;

    private long initial_wcp;

    private long wcp;

    private long edgeNum;

    private long edgeWeight;

    private long taskWeight;

    private long bestLevel;

    private long processTime;

    private long linearNum;

    private long nonLinearNum;

    private Long maxClusterID;

    private long minCriticalPath;





    


    /**
     *
     * @param in_type
     * @param in_maxweight
     * @param in_aveweight
     * @param in_minweight
     */
    public BBTask(/*Vector<Long> in_id,*/
                        int in_type,
                        long in_maxweight,
                        long in_aveweight,
                        long in_minweight){
        super(in_type,in_maxweight,in_aveweight,in_minweight);
        this.taskClusterList = new Hashtable<Long, TaskCluster>();
        this.makeSpan = 0;

        this.worstLevel = 0;
        this.cpLen = 0;
        this.edgeNum = 0;
        this.edgeWeight = 0;
        this.taskWeight = 0;
        this.bestLevel = 0;
        this.processTime = 0;
        this.linearNum = 0;
        this.nonLinearNum = 0;
        this.wcp = 0;
        this.initial_wcp = 0;
        this.minCriticalPath = 0;


    }


    public long getMinCriticalPath() {
        return minCriticalPath;
    }

    public void setMinCriticalPath(long minCriticalPath) {
        this.minCriticalPath = minCriticalPath;
    }

    public long getInitial_wcp() {
        return initial_wcp;
    }

    public void setInitial_wcp(long initial_wcp) {
        this.initial_wcp = initial_wcp;
    }

    public long getNonLinearNum() {
        return nonLinearNum;
    }

    public void setNonLinearNum(long nonLinearNum) {
        this.nonLinearNum = nonLinearNum;
    }

    public long getWcp() {
        return wcp;
    }

    public Long getMaxClusterID() {
        return maxClusterID;
    }

    public void setMaxClusterID(Long maxClusterID) {
        this.maxClusterID = maxClusterID;
    }

    public void setWcp(long wcp) {
        this.wcp = wcp;
    }

    public long getLinearNum() {
        return linearNum;
    }

    public void setLinearNum(long linearNum) {
        this.linearNum = linearNum;
    }

    public Iterator<AbstractTask> taskIerator(){
        Collection<AbstractTask> taskCol = this.getTaskList().values();
        return taskCol.iterator();
    }

    public Iterator<TaskCluster> clusterIterator(){
        Collection<TaskCluster> clusterCol = this.getTaskClusterList().values();
        return clusterCol.iterator();
    }

    public long getMakeSpan() {
        return makeSpan;
    }

    public void setMakeSpan(long makeSpan) {
        this.makeSpan = makeSpan;
    }

    public Hashtable<Long, TaskCluster> getTaskClusterList() {
        return taskClusterList;
    }

    public void setTaskClusterList(Hashtable<Long, TaskCluster> taskClusterList) {
        this.taskClusterList = taskClusterList;
    }

    public long getProcessTime() {
        return processTime;
    }

    public void setProcessTime(long processTime) {
        this.processTime = processTime;
    }

    /**
     * クラスタを，当該タスクグラフへ追加します．<br>
     * @param cluster : IDがまだ割り当てられてないタスククラスタ
     * @return : 新規生成されたクラスタID
     */
    public Long addTaskCluster(TaskCluster cluster){
        Long clusterID = cluster.getClusterID();
        this.taskClusterList.put(clusterID, cluster);

        return clusterID;
        /*if(this.taskClusterList.isEmpty()){
            Long newid = new Long(1);
            cluster.setClusterID(newid);
            this.taskClusterList.put(newid, cluster);
            return new Long(1);
        }else{
            int size = this.taskClusterList.size();
            Long newid = new Long(size+1);
            cluster.setClusterID(newid);
            this.taskClusterList.put(newid, cluster);
            return newid;
        }
        */

    }

    /**
     * 当該DAGから，指定IDのタスククラスタを削除します．
     * @param id
     */
    public void removeTaskCluster(Long id){
        this.taskClusterList.remove(id);
    }

    /**
     *
     * @param id
     * @return
     */
    public TaskCluster findTaskCluster(Long id){
        return this.taskClusterList.get(id);


    }

    public long getWorstLevel() {
        return worstLevel;
    }

    public void setWorstLevel(long worstLevel) {
        this.worstLevel = worstLevel;
    }

    public long getCpLen() {
        return cpLen;
    }

    public void setCpLen(long cpLen) {
        this.cpLen = cpLen;
    }

    public long getEdgeNum() {
        return edgeNum;
    }

    public void setEdgeNum(long edgeNum) {
        this.edgeNum = edgeNum;
    }

    public long getEdgeWeight() {
        return edgeWeight;
    }

    public void setEdgeWeight(long edgeWeight) {
        this.edgeWeight = edgeWeight;
    }

    public long getTaskWeight() {
        return taskWeight;
    }

    public void setTaskWeight(long taskWeight) {
        this.taskWeight = taskWeight;
    }                  

    public long getBestLevel() {
        return bestLevel;
    }

    public void setBestLevel(long bestLevel) {
        this.bestLevel = bestLevel;
    }
}
