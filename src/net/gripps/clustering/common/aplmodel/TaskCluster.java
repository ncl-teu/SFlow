package net.gripps.clustering.common.aplmodel;

import net.gripps.clustering.tool.Calc;
import net.gripps.environment.CPU;

import java.util.LinkedList;
import java.util.PriorityQueue;
import java.io.*;

/**
 * Author: H. Kanemitsu
 * Date: 2007/12/11
 */
public class TaskCluster implements Serializable,Cloneable{
    /**
     *  このクラスタに含まれているタスク集合
     */
    private CustomIDSet IDSet;

    /**
     * クラスタを識別するためのID
     */
    private Long ClusterID;

    /**
     * 優先度なしのキュー
     */
    private PriorityQueue<Long> reservedPivot;

    /**
     * そのクラスタが線形かどうかのフラグ
     */
    private boolean isLinear;

    /**
     * このクラスタの，最悪tlevel値
     *
     */
    private long tlevel;

    /**
     * このクラスタの，blevel値
     */
    private long blevel;

    /**
     *  In集合
     */
    private CustomIDSet in_Set;

    /**
     *  Out集合
     */
    private CustomIDSet out_Set;

    /**
     * 
     */
    private CustomIDSet top_Set;

    /**
     *
     */
    private CustomIDSet cyclicTopSet;

    /**
     *
     */
    private CustomIDSet cyclicClusterSet;

    /**
     * 割り当て先マシン
     */
    private CPU CPU;


    /**
     * トップタスクのこと
     */
    private Long topTaskID;

    

    /**
     * そのBsucが別クラスタにあり，かつblevel値を決定づけるようなタスクのこと．
     * Outタスクであり，かつBsucを別クラスタ内のタスクにもつものをさす．
     */
    private Long bsucTaskID;

    /**
     *
     */
    private CustomIDSet destCheckedSet;

    /**
     *
     */
    private CustomIDSet parentCheckedSet;

    /**
     *
     */
    private long makeSpan;

    /**
     *
     */
    private LinkedList<Long> scheduledTaskList;

    /**
     *
     */
    private long clusterSize;

    /**
     *
     */
    private CustomIDSet bottomSet;

    /**
     * Partialスケジューリング用の順序保持集合
     */
    LinkedList<AbstractTask> pSchedSet;


    /**
     * ヘテロジニアス環境用に用いる，タスク自体の
     * クラスタ実行時間の下限値
     */
    private double threshold_time;

    /**
     * クラスタの，R/C値（FCSアルゴリズム）
     */
    private double beta;

    private double FCS_R;

    private double FCS_C;



//    private long cpLen;

    /**
     *
     * @param IDSet
     * @param clusterID
     */
    public TaskCluster(CustomIDSet IDSet, Long clusterID) {
        this.IDSet = IDSet;
        ClusterID = clusterID;
        this.tlevel = -1;
        this.blevel = -1;
        this.in_Set = new CustomIDSet();
        this.out_Set = new CustomIDSet();
        this.reservedPivot = new PriorityQueue<Long>();
        this.top_Set = new CustomIDSet();

        this.isLinear = true;
        this.cyclicTopSet = new CustomIDSet();
        this.cyclicClusterSet = new CustomIDSet();
        this.destCheckedSet = new CustomIDSet();
        this.CPU = new CPU();
        this.scheduledTaskList = new LinkedList<Long>();
        this.clusterSize = -1;
        this.parentCheckedSet = new CustomIDSet();
        this.bottomSet = new CustomIDSet();
        this.pSchedSet = new LinkedList<AbstractTask>();
        this.threshold_time = 0.0;
        this.beta = 0.0;
        this.FCS_R = 0.0;
        this.FCS_C = 0.0;


        //this.cpLen = -1;



    }

    /**
     *
     * @param IDSet
     */
    public TaskCluster(CustomIDSet IDSet) {
        this.IDSet = IDSet;
        this.tlevel = -1;
        this.blevel = -1;
        this.in_Set = new CustomIDSet();
        this.out_Set = new CustomIDSet();
        this.isLinear = true;
        this.reservedPivot = new PriorityQueue<Long>();
        this.top_Set = new CustomIDSet();
        this.cyclicTopSet = new CustomIDSet();
        this.cyclicClusterSet = new CustomIDSet();
        this.destCheckedSet = new CustomIDSet();
        this.CPU = new CPU();
        this.scheduledTaskList = new LinkedList<Long>();
        this.clusterSize = -1;
        this.parentCheckedSet = new CustomIDSet();
        this.bottomSet = new CustomIDSet();
        this.pSchedSet = new LinkedList<AbstractTask>();
        this.threshold_time = 0.0;
        this.beta = 0.0;
        this.FCS_R = 0.0;
        this.FCS_C = 0.0;
      //   this.cpLen = -1;
    }


    public CPU getCPU() {
        return CPU;
    }

    public void setCPU(CPU CPU) {
        this.CPU = CPU;
    }

    public CustomIDSet getTop_Set() {
        return top_Set;
    }

    public void setTop_Set(CustomIDSet top_Set) {
        this.top_Set = top_Set;
    }

    public Long getBsucTaskID() {
        return bsucTaskID;
    }

    public void setBsucTaskID(Long bsucTaskID) {
        this.bsucTaskID = bsucTaskID;
    }

    public Long getTopTaskID() {
        return topTaskID;
    }

    public void setTopTaskID(Long topTaskID) {
        this.topTaskID = topTaskID;
    }

    public double getThreshold_time() {
        return threshold_time;
    }

    public void setThreshold_time(double threshold_time) {
        this.threshold_time = threshold_time;
    }

    public boolean isLinear() {
        return isLinear;
    }

    public void setLinear(boolean linear) {
        isLinear = linear;
    }

    /**
     *
     * @return
     */
    public int getTaskSize(){
        return this.IDSet.getList().size();
    }

    /**
     *
     * @param task
     * @param clusterID
     */
    public TaskCluster(AbstractTask task, Long clusterID){
        //もしタスク集合がNULLならば，新規生成する
       if(this.IDSet == null){
           this.IDSet = new CustomIDSet();
       }
        this.IDSet.add(task.getIDVector().get(1));
        ClusterID = clusterID;
        this.tlevel = -1;
        this.blevel = -1;
        this.in_Set = new CustomIDSet();
        this.out_Set = new CustomIDSet();
        this.reservedPivot = new PriorityQueue<Long>();
        this.cyclicTopSet = new CustomIDSet();
         this.cyclicClusterSet = new CustomIDSet();
        this.destCheckedSet = new CustomIDSet();
        this.scheduledTaskList = new LinkedList<Long>();
        this.clusterSize = -1;
        this.bottomSet = new CustomIDSet();
      //   this.cpLen = -1;
        this.parentCheckedSet = new CustomIDSet();
        this.pSchedSet = new LinkedList<AbstractTask>();
        this.FCS_R = 0.0;
        this.FCS_C = 0.0;
    }

    public PriorityQueue<Long> getReservedPivot() {
        return reservedPivot;
    }

    public void setReservedPivot(PriorityQueue<Long> reservedPivot) {
        this.reservedPivot = reservedPivot;
    }



    /**
     *
     * @param taskID
     */
    public TaskCluster(Long taskID){
        //もしタスク集合がNULLならば，新規生成する
       if(this.IDSet == null){
           this.IDSet = new CustomIDSet();
       }
        //this.IDSet.add(taskID);
        this.ClusterID = taskID;
       // ClusterID = clusterID;
        this.tlevel = -1;
        this.blevel = -1;
        this.in_Set = new CustomIDSet();
        this.out_Set = new CustomIDSet();
        this.reservedPivot = new PriorityQueue<Long>();
        this.cyclicTopSet = new CustomIDSet();
         this.cyclicClusterSet = new CustomIDSet();
        this.destCheckedSet = new CustomIDSet();
          this.scheduledTaskList = new LinkedList<Long>();
                this.clusterSize = -1;
       //  this.cpLen = -1;
        this.parentCheckedSet = new CustomIDSet();
        this.bottomSet = new CustomIDSet();
        this.pSchedSet = new LinkedList<AbstractTask>();
        this.beta = 0.0;
        this.FCS_R = 0.0;
        this.FCS_C = 0.0;
    }







    /**
     *
     * @param IDSet
     * @param clusterID
     * @param tlevel
     * @param blevel
     */
    public TaskCluster(CustomIDSet IDSet, Long clusterID, long tlevel, long blevel) {
        this.IDSet = IDSet;
        ClusterID = clusterID;
        this.tlevel = tlevel;
        this.blevel = blevel;
        this.in_Set = new CustomIDSet();
        this.out_Set = new CustomIDSet();
        this.reservedPivot = new PriorityQueue<Long>();
        this.cyclicTopSet = new CustomIDSet();
         this.cyclicClusterSet = new CustomIDSet();
        this.destCheckedSet = new CustomIDSet();
         this.scheduledTaskList = new LinkedList<Long>();
                this.clusterSize = -1;
       //  this.cpLen = -1;
        this.parentCheckedSet = new CustomIDSet();
        this.bottomSet = new CustomIDSet();
        this.pSchedSet = new LinkedList<AbstractTask>();
        this.beta = 0.0;
        this.FCS_R = 0.0;
        this.FCS_C = 0.0;

    }

    /**
     *
     * @return
     */
    public Long getFirst(){

        LinkedList<Long> list = this.IDSet.getList();
        return list.getFirst();

    }


    /**
     *
     * @return
     */
    public Serializable deepCopy(){
        try{
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bout);
            out.writeObject(this);
            out.close();
            byte[] bytes = bout.toByteArray();
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
            Object newObject = in.readObject();
            in.close();
            return (Serializable) newObject;
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public CustomIDSet getDestCheckedSet() {
        return destCheckedSet;
    }

    public void setDestCheckedSet(CustomIDSet destCheckedSet) {
        this.destCheckedSet = destCheckedSet;
    }

  /*  public void initializeIn_Set(){
        this.in_Set.initializeTaskSet();
    }
    */

    public void initializeOut_Set(){
        this.out_Set.initializeTaskSet();
    }

    public void addIn_Set(Long id){
        this.in_Set.add(id);
    }

    public CustomIDSet getCyclicTopSet() {
        return cyclicTopSet;
    }

    public void setCyclicTopSet(CustomIDSet cyclicTopSet) {
        this.cyclicTopSet = cyclicTopSet;
    }
    /*
    public void remodeIn_Set(Long id){
        this.in_Set.remove(id);
    }
    */

    public CustomIDSet getCyclicClusterSet() {
        return cyclicClusterSet;
    }

    public void setCyclicClusterSet(CustomIDSet cyclicClusterSet) {
        this.cyclicClusterSet = cyclicClusterSet;
    }

    public void addOut_Set(Long id){
        this.out_Set.add(id);
    }

    public void removeOut_Set(Long id){
        this.out_Set.remove(id);
    }

    public void addTask(Long id){
        this.IDSet.add(id);

    }

    public void removeTask(Long id){
        this.IDSet.remove(id);
    }


    /**
     *
     * @return
     */
    public CustomIDSet getTaskSet() {
        return IDSet;
    }




    /**
     *
     * @param IDSet
     */
    public void setTaskSet(CustomIDSet IDSet) {
        this.IDSet = IDSet;
    }

    public Long getClusterID() {
        return ClusterID;
    }

    /**
     *
     * @param clusterID
     */
    public void setClusterID(Long clusterID) {
        ClusterID = clusterID;
    }

    public CustomIDSet getOut_Set() {
        return out_Set;
    }

    public void setOut_Set(CustomIDSet out_Set) {
        this.out_Set = out_Set;
    }


    public CustomIDSet getIn_Set() {
        return in_Set;
    }

    public void setIn_Set(CustomIDSet in_Set) {
        this.in_Set = in_Set;
    }
    

    public long getBlevel() {
        return blevel;
    }

    public void setBlevel(long blevel) {
        this.blevel = blevel;
    }

    public long getTlevel() {
        return tlevel;
    }

    public void setTlevel(long tlevel) {
        this.tlevel = tlevel;
    }

    /**
     *
     * @return
     */
    public Object clone() {
        try {
            return (super.clone());
        } catch (CloneNotSupportedException e) {
            throw (new InternalError(e.getMessage()));
        }
    }

    public LinkedList<Long> getScheduledTaskList() {
        return this.scheduledTaskList;
    }

    public void setScheduledTaskList(LinkedList<Long> shceduledTaskList) {
        this.scheduledTaskList = shceduledTaskList;
    }

    /*  public AbstractTask[] getShceduledTaskList() {
        return shceduledTaskList;
    }

    public void setShceduledTaskList(AbstractTask[] shceduledTaskList) {
        this.shceduledTaskList = shceduledTaskList;
    }
    */

    public long getClusterSize() {
        return clusterSize;
    }

    public void setClusterSize(long clusterSize) {
        this.clusterSize = clusterSize;
    }

    public long getMakeSpan() {
        return makeSpan;
    }

    public void setMakeSpan(long makeSpan) {
        this.makeSpan = makeSpan;
    }

    public CustomIDSet getParentCheckedSet() {
        return parentCheckedSet;
    }

    public void setParentCheckedSet(CustomIDSet parentCheckedSet) {
        this.parentCheckedSet = parentCheckedSet;
    }

    public CustomIDSet getBottomSet() {
        return bottomSet;
    }

    public void setBottomSet(CustomIDSet bottomSet) {
        this.bottomSet = bottomSet;
    }

    public LinkedList<AbstractTask> getPSchedSet() {
        return pSchedSet;
    }

    public void setPSchedSet(LinkedList<AbstractTask> pSchedSet) {
        this.pSchedSet = pSchedSet;
    }

    public double getFCS_R() {
        return FCS_R;
    }

    public void setFCS_R(double FCS_R) {
        this.FCS_R = FCS_R;
    }

    public double getFCS_C() {
        return FCS_C;
    }

    public void setFCS_C(double FCS_C) {
        this.FCS_C = FCS_C;
    }

    public double getBeta(){
        return Calc.getRoundedValue(this.FCS_R/this.FCS_C);
    }
}
