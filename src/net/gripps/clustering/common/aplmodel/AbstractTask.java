package net.gripps.clustering.common.aplmodel;

import net.gripps.clustering.common.Constants;
import net.gripps.clustering.common.aplmodel.CustomIDSet;

import java.io.*;
import java.util.*;

/**
 * Author: Hidehiro Kanemitsu<br>
 * Date: 2007/07/06
 */
public  class AbstractTask implements Serializable, Cloneable  {

    /**
     * <p>Unique ID for this Task
     * Each ID means that this task's ancestor's ID.
     *
     * @serial
     */
    protected Vector<Long> IDVector;

    protected Vector<Long> orgIDVector;


    /**
     *
     */
    boolean isDestCalculated;

    /**
     *
     */
    private CustomIDSet startIDSet;

    private CustomIDSet cyclicPredIDSet;

    private long blevel_in;

    //private int pathClusterNum;
    protected CustomIDSet clusterSet;



    /**
     * type of this task(loop, function call, basic block)
     */
    private int type;                                                         

    /**
     * @serialField num of instructions for completing this task
     */
    private long  maxWeight;

    /**
     * @serial
     */
    private long aveWeight;

    /**
     *
     */
    private long minWeight;

    /**
     * The task which belongs to Tlevel path in this task
     */
    private Vector<Long> Tpred;

    /**
     * The task which belongs to Blevel path in this task
     */
    private Vector<Long> Bsuc;

    /**
     * List of tasks included in this taskgraph
     */
   // private LinkedList<AbstractTask> taskList;

    private Hashtable<Long, AbstractTask> taskList;

    /**
     * Parent Task to which this task belongs.
     */
    private Vector<Long> parentTask;

    /**
     * List of predesessor tasks in data dependence.
     */
    private LinkedList<DataDependence> dpredList;

    /**
     * List of successor tasks in data dependence.
     */
    private LinkedList<DataDependence> dsucList;

    /**
     * List of predesessor tasks in control dependence
     */
    private LinkedList<ControlDependence> cpredList;

    /**
     * List of successor tasks in control dependence
     */
    private LinkedList<ControlDependence> csucList;


    /**
     * ID of Cluster to which this task belongs.
     */
    private Long clusterID;

    /**
     * The bottom level of this task
     */
    private long blevel;

    /**
     * The top level of this task
     */
    private long tlevel;

    private long priorityTlevel;

    private long priorityBlevel;

    private long scheduledTlevel;

    private long scheduledBlevel;



    private CustomIDSet cyclicClusterSet;
    /**
     *
     */
    private Vector<Long> startTask;

    private long startTime;

    private long wblevel;

    private long wtlevel;

   // private boolean isReady;


    private long pstartTime;



    /**
     *
     */
    private Vector<Long> endTask;

    protected  CustomIDSet destIDSet;

    protected CustomIDSet tmpDestIDSet;

    private CustomIDSet parentIDSet;

    private long sucTaskNum;

    private long tmpTlevel;

    private long tmpBlevel;

    private long tmpCount;

    protected long eBlevel;

    protected int direction;

    protected long minOCTValue;

    protected  long minOCTCPUID;

    protected long totalOCTValue;

    /**
     * CPUID, OCTvalueの組み合わせ
     */
    protected Hashtable<Long, Long> octMap;

    /**
     * このタスク内のタスクでの最大の仕事量
     */
    protected  long maxWorkload;

    /**
     * このタスクのタスクでの最小の仕事量
     */
    protected long minWorkload;

    protected long maxData;

    protected long minData;

    private long optDelta;

    /**
     *
     */
    private HashSet<Long> ancestorIDList;

    private HashSet<Long> dancestorIDList;

    private long sValue;

    private long cnt_max;

    private long cnt_min;

    private long minLevelW;

    private long maxLevelW;

    private CustomIDSet ansPathSet;

    protected long ave_procTime;

    protected long ave_oct;

    protected double ave_oct_double;

    protected int depth;

    protected boolean isActive;

    protected long msl_rank;

    protected Hashtable<Long, Long> hsv_blevel;

    protected long totalHSV_blevel;

    protected long hprv_rank;

    protected long blevelTotalTaskSize;

    protected long blevelTotalDataSize;

    protected double HSVRankG;

    protected double TotalHSVRankG;



    public long getMaxWorkload() {
        return maxWorkload;
    }

    public void setMaxWorkload(long maxWorkload) {
        this.maxWorkload = maxWorkload;
    }

    public long getMinWorkload() {
        return minWorkload;
    }

    public void setMinWorkload(long minWorkload) {
        this.minWorkload = minWorkload;
    }



    public CustomIDSet getTmpDestIDSet() {
        return tmpDestIDSet;
    }

    public void setTmpDestIDSet(CustomIDSet tmpDestIDSet) {
        this.tmpDestIDSet = tmpDestIDSet;
    }

    public CustomIDSet getDestTaskSet() {
        return destIDSet;
    }

    public void setDestTaskSet(CustomIDSet destIDSet) {
        this.destIDSet = destIDSet;
    }

    public void addDestTask(Long taskID){
        this.destIDSet.add(taskID);
    }

    public void removeDestTask(Long taskID){
        this.destIDSet.remove(taskID);
    }

    public void addParentTask(Long taskID){
        this.parentIDSet.add(taskID);
    }

    public void removeParentTask(Long taskID){
        this.parentIDSet.remove(taskID);
    }

    public long geteBlevel() {
        return eBlevel;
    }

    public void seteBlevel(long eBlevel) {
        this.eBlevel = eBlevel;
    }


    public AbstractTask(){
        this(0,0,0,0);
    }
    /**
     * @param in_type
     * @param in_maxweight
     * @param in_aveweight
     * @param in_minweight
     */
    public AbstractTask(/*Vector<Long> in_id,*/
            int in_type,
            long in_maxweight,
            long in_aveweight,
            long in_minweight) {

        /*this.IDVector = in_id;*/
        this.type = in_type;
        this.maxWeight = in_maxweight;
        this.aveWeight = in_aveweight;
        this.minWeight = in_minweight;
        this.depth = 0;

        //Initialization
        this.IDVector = new Vector<Long>();
        this.tlevel = -1;
        this.blevel = -1;
        this.dpredList = new LinkedList<DataDependence>();
        this.dsucList = new LinkedList<DataDependence>();
        this.cpredList = new LinkedList<ControlDependence>();
        this.csucList = new LinkedList<ControlDependence>();
        this.Tpred = new Vector<Long>();
        this.Bsuc = new Vector<Long>();
        this.taskList = new Hashtable<Long, AbstractTask>();
        this.parentTask = new Vector<Long>();
        this.startTask = new Vector<Long>();
        this.parentTask = new Vector<Long>();
        this.ancestorIDList = new HashSet<Long>();
        this.dancestorIDList = new HashSet<Long>();
        this.clusterID = new Long(0);
        this.destIDSet = new CustomIDSet();
        this.tmpDestIDSet = new CustomIDSet();
        this.isDestCalculated = false;
        this.startIDSet = new CustomIDSet();

        this.priorityBlevel = -1;

        this.priorityTlevel = -1;

        this.scheduledBlevel = -1;

        this.scheduledTlevel = -1;
        this.blevel_in  = -1;
        cyclicClusterSet = new CustomIDSet();
        this.cyclicPredIDSet = new CustomIDSet();
        this.startTime = -1;
        this.parentIDSet = new CustomIDSet();
        this.clusterSet = new CustomIDSet();
        this.sucTaskNum = 0;
        this.wblevel = -1;
        this.wtlevel = -1;
        this.optDelta = 0;
        this.pstartTime = -1;
        this.sValue = 0;
        this.cnt_max = 0;
        this.cnt_min = 0;
        this.maxLevelW = -1;
        this.minLevelW = -1;
        this.tmpTlevel = -1;
        this.tmpBlevel = -1;
        this.tmpCount = 0;
        this.ansPathSet = new CustomIDSet();
        this.ave_procTime = 0;
        this.ave_oct = Constants.INFINITY;
        this.isActive = true;
        this.direction = -1;
        //this.isReady = false;
        this.msl_rank = -1;
        this.hsv_blevel = new Hashtable<Long, Long>();
        this.hprv_rank = -1;
        this.totalHSV_blevel = 0;

        this.maxWorkload = 0;
        this.minWorkload = 99999999;

        this.maxData = 0;
        this.minData = 99999999;

        this.minOCTValue = Constants.MAXValue;
        this.minOCTCPUID = 0;
        this.octMap = new Hashtable<Long, Long>();

        this.blevelTotalDataSize = 0;
        this.blevelTotalTaskSize = 0;
        this.HSVRankG = 0;
        this.TotalHSVRankG = 0;
        this.totalOCTValue = 0;
        this.ave_oct_double = Constants.INFINITY;


    }

    public Vector<Long> getOrgIDVector() {
        return orgIDVector;
    }

    public void setOrgIDVector(Vector<Long> orgIDVector) {
        this.orgIDVector = orgIDVector;
    }

    /**
     * 初期値に戻す．
     */
    public void clear(){

        /*this.IDVector = in_id;*/

        this.depth = 0;

        //Initialization

        this.tlevel = -1;
        this.blevel = -1;

        this.clusterID = new Long(0);
        this.destIDSet = new CustomIDSet();
        this.tmpDestIDSet = new CustomIDSet();
        this.isDestCalculated = false;
        this.startIDSet = new CustomIDSet();


        this.priorityBlevel = -1;

        this.priorityTlevel = -1;

        this.scheduledBlevel = -1;

        this.scheduledTlevel = -1;
        this.blevel_in  = -1;
        cyclicClusterSet = new CustomIDSet();
        this.cyclicPredIDSet = new CustomIDSet();
        this.startTime = -1;
        this.parentIDSet = new CustomIDSet();
        this.clusterSet = new CustomIDSet();
        this.sucTaskNum = 0;
        this.wblevel = -1;
        this.wtlevel = -1;
        this.optDelta = 0;
        this.pstartTime = -1;
        this.sValue = 0;
        this.cnt_max = 0;
        this.cnt_min = 0;
        this.maxLevelW = -1;
        this.minLevelW = -1;
        this.tmpTlevel = -1;
        this.tmpBlevel = -1;
        this.tmpCount = 0;
        this.ansPathSet = new CustomIDSet();
        this.ave_procTime = 0;
        this.ave_oct = Constants.INFINITY;
        this.isActive = true;
        this.direction = -1;
        //this.isReady = false;
        this.msl_rank = -1;
        this.hsv_blevel = new Hashtable<Long, Long>();
        this.hprv_rank = -1;
        this.totalHSV_blevel = 0;

        this.maxWorkload = 0;
        this.minWorkload = 99999999;

        this.maxData = 0;
        this.minData = 99999999;

        this.minOCTValue = Constants.MAXValue;
        this.minOCTCPUID = 0;
        this.octMap = new Hashtable<Long, Long>();

        this.blevelTotalDataSize = 0;
        this.blevelTotalTaskSize = 0;
        this.HSVRankG = 0;
        this.TotalHSVRankG = 0;
        this.totalOCTValue = 0;
        this.ave_oct_double = Constants.INFINITY;


    }

    public double getAve_oct_double() {
        return ave_oct_double;
    }

    public void setAve_oct_double(double ave_oct_double) {
        this.ave_oct_double = ave_oct_double;
    }

    public long getTotalOCTValue() {
        return totalOCTValue;
    }

    public void setTotalOCTValue(long totalOCTValue) {
        this.totalOCTValue = totalOCTValue;
    }

    public Hashtable<Long, Long> getOctMap() {
        return octMap;
    }

    public void setOctMap(Hashtable<Long, Long> octMap) {
        this.octMap = octMap;
    }

    public long getMinOCTValue() {
        return minOCTValue;
    }

    public void setMinOCTValue(long minOCTValue) {
        this.minOCTValue = minOCTValue;
    }

    public long getMinOCTCPUID() {
        return minOCTCPUID;
    }

    public void setMinOCTCPUID(long minOCTCPUID) {
        this.minOCTCPUID = minOCTCPUID;
    }

    public void addTotalHSVRankG(double value){
        this.TotalHSVRankG += value;
    }
    public double getTotalHSVRankG() {
        return this.TotalHSVRankG;
    }

    public void setTotalHSVRankG(double totalHSVRankG) {
        TotalHSVRankG = totalHSVRankG;
    }

    public double getHSVRankG() {
        return HSVRankG;
    }

    public void setHSVRankG(double HSVRankG) {
        this.HSVRankG = HSVRankG;
    }

    public long getBlevelTotalTaskSize() {
        return blevelTotalTaskSize;
    }

    public void setBlevelTotalTaskSize(long blevelTotalTaskSize) {
        this.blevelTotalTaskSize = blevelTotalTaskSize;
    }

    public long getBlevelTotalDataSize() {
        return blevelTotalDataSize;
    }

    public void setBlevelTotalDataSize(long blevelTotalDataSize) {
        this.blevelTotalDataSize = blevelTotalDataSize;
    }

    public long getMaxData() {
        return maxData;
    }

    public void setMaxData(long maxData) {
        this.maxData = maxData;
    }

    public long getMinData() {
        return minData;
    }

    public void setMinData(long minData) {
        this.minData = minData;
    }

    public long getTotalHSV_blevel() {
        return totalHSV_blevel;
    }

    public void setTotalHSV_blevel(long totalHSV_blevel) {
        this.totalHSV_blevel = totalHSV_blevel;
    }

    public Hashtable<Long, Long> getHsv_blevel() {
        return hsv_blevel;
    }

    public void setHsv_blevel(Hashtable<Long, Long> hsv_blevel) {
        this.hsv_blevel = hsv_blevel;
    }

    public long getHprv_rank() {
        return hprv_rank;
    }

    public void setHprv_rank(long hprv_rank) {
        this.hprv_rank = hprv_rank;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    public CustomIDSet getAnsPathSet() {
        return ansPathSet;
    }

    public void setAnsPathSet(CustomIDSet ansPathSet) {
        this.ansPathSet = ansPathSet;
    }

    public long getTmpCount() {
        return tmpCount;
    }

    public void setTmpCount(long tmpCount) {
        this.tmpCount = tmpCount;
    }

    public long getSValue() {
        return sValue;
    }

    public void setSValue(long sValue) {
        this.sValue = sValue;
    }

    /**
     *
     * @return
     */
    public long getPstartTime() {
        return pstartTime;
    }

    public long getTmpTlevel() {
        return tmpTlevel;
    }

    public void setTmpTlevel(long tmpTlevel) {
        this.tmpTlevel = tmpTlevel;
    }

    public long getTmpBlevel() {
        return tmpBlevel;
    }

    public void setTmpBlevel(long tmpBlevel) {
        this.tmpBlevel = tmpBlevel;
    }

    /**
     * 
     * @param pstartTime
     */
    public void setPstartTime(long pstartTime) {
        this.pstartTime = pstartTime;
    }

    public long getOptDelta() {
        return optDelta;
    }

    public void setOptDelta(long optDelta) {
        this.optDelta = optDelta;
    }

    public long getWblevel() {
        return wblevel;
    }

    public void setWblevel(long wblevel) {
        this.wblevel = wblevel;
    }

    public long getWtlevel() {
        return wtlevel;
    }

    public void setWtlevel(long wtlevel) {
        this.wtlevel = wtlevel;
    }

    public long getSucTaskNum() {
        return sucTaskNum;
    }

    public void setSucTaskNum(long sucTaskNum) {
        this.sucTaskNum = sucTaskNum;
    }

    public long getBlevel_in() {
        return blevel_in;
    }

    public void setBlevel_in(long blevel_in) {
        this.blevel_in = blevel_in;
    }

    public CustomIDSet getCyclicPredTaskSet() {
        return cyclicPredIDSet;
    }

    public void setCyclicPredTaskSet(CustomIDSet cyclicPredIDSet) {
        this.cyclicPredIDSet = cyclicPredIDSet;
    }

    public CustomIDSet getStartTaskSet() {
        return startIDSet;
    }

    public void setStartTaskSet(CustomIDSet startIDSet) {
        this.startIDSet = startIDSet;
    }

    public boolean isDestCalculated() {
        return isDestCalculated;
    }

    public void setDestCalculated(boolean destCalculated) {
        isDestCalculated = destCalculated;
    }

    public long getMinLevelW() {
        return minLevelW;
    }

    public void setMinLevelW(long minLevelW) {
        this.minLevelW = minLevelW;
    }

    public long getMaxLevelW() {
        return maxLevelW;
    }

    public void setMaxLevelW(long maxLevelW) {
        this.maxLevelW = maxLevelW;
    }

    public long getMsl_rank() {
        return msl_rank;
    }

    public void setMsl_rank(long msl_rank) {
        this.msl_rank = msl_rank;
    }



    /**
     *
     * @return
     */
    public  synchronized Serializable deepCopy(){
        //System.gc();

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


    public CustomIDSet getCyclicClusterSet() {
        return cyclicClusterSet;
    }

    public void setCyclicClusterSet(CustomIDSet cyclicClusterSet) {
        this.cyclicClusterSet = cyclicClusterSet;
    }

    public boolean allDpredIsChecked(){
        LinkedList<DataDependence> ddList = this.getDpredList();
        Iterator<DataDependence> ite = ddList.iterator();
        boolean ret = true;

        while(ite.hasNext()){
            DataDependence dd = ite.next();
            if(!dd.getIsChecked()){
                ret = false;
                break;
            }
        }
        return ret;
    }


    public boolean allDsucIsChecked(){
        LinkedList<DataDependence> ddList = this.getDsucList();
        Iterator<DataDependence> ite = ddList.iterator();
        boolean ret = true;

        while(ite.hasNext()){
            DataDependence dd = ite.next();
            if(!dd.getIsChecked()){
                ret = false;
                break;
            }
        }
        return ret;
    }

    /**
     * 未チェックである入力辺の数をカウントします．
     * @return
     */
     public int countUnCheckedInEdges(){
        LinkedList<DataDependence> ddList = this.getDpredList();
        Iterator<DataDependence> ite = ddList.iterator();
        boolean ret = true;
        int count = 0;

        while(ite.hasNext()){
            DataDependence dd = ite.next();
            if(!dd.getIsChecked()){
                count++;

            }
        }
        return count;
    }



    /**
     *
     * @return
     */
    public Long getClusterID() {
        return clusterID;
    }

    /**
     *
     * @param clusterID
     */
    public void setClusterID(Long clusterID) {
        this.clusterID = clusterID;
    }

    /**
     *
     * @return
     */
    public boolean allOutDDIsChecked(){
        LinkedList<DataDependence> ddList = this.getDsucList();
        Iterator<DataDependence> ite = ddList.iterator();
        boolean ret = true;
        if(this.dsucList.isEmpty()){
            return true;
        }
        while(ite.hasNext()){
            DataDependence dd = ite.next();
            if(!dd.isOutIsChecked()){
                ret = false;
                break;
            }
        }
        return ret;
    }



    public void setCheckedFlgToDpred(Vector<Long> fromID, Vector<Long> toID, boolean flg){
        DataDependence dd = this.findDDFromDpredList(fromID, toID);
        if(dd == null){
            return;
        }
        dd.setIsChecked(flg);
        //this.updateDpred(dd);
    }

    public void setCheckedFlgToDsuc(Vector<Long> fromID, Vector<Long> toID, boolean flg){
        DataDependence dd = this.findDDFromDsucList(fromID, toID);
        if(dd == null){
            return;
        }
        dd.setIsChecked(flg);
        //this.updateDpred(dd);
    }

    /**
     * @return
     */
    public int getType() {
        return type;
    }

    /**
     * @param type
     */
    public void setType(int type) {
        this.type = type;
    }

    /**
     * @return
     */
    public Hashtable<Long, AbstractTask> getTaskList() {
        return taskList;
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    /**
     * @param taskList
     */
    public void setTaskList(Hashtable<Long, AbstractTask> taskList) {
        this.taskList = taskList;
    }

    /**
     * @return
     */
    public Vector<Long> getParentTask() {
        return parentTask;
    }

    /**
     * @param parentTask
     */
    public void setParentTask(Vector<Long> parentTask) {
        this.parentTask = parentTask;
    }

    /**
     * @return
     */
    public LinkedList<DataDependence> getDpredList() {
        return dpredList;
    }

    /**
     * @param dpredList
     */
    public void setDpredList(LinkedList<DataDependence> dpredList) {
        this.dpredList = dpredList;
    }

    /**
     * @return
     */
    public LinkedList<DataDependence> getDsucList() {
        return dsucList;
    }

    /**
     * @param dsucList
     */
    public void setDsucList(LinkedList<DataDependence> dsucList) {
        this.dsucList = dsucList;
    }

    /**
     * @return
     */
    public LinkedList<ControlDependence> getCpredList() {
        return cpredList;
    }

    /**
     * @param cpredList
     */
    public void setCpredList(LinkedList<ControlDependence> cpredList) {
        this.cpredList = cpredList;
    }

    /**
     * @return
     */
    public LinkedList<ControlDependence> getCsucList() {
        return csucList;
    }

    /**
     * @param csucList
     */
    public void setCsucList(LinkedList<ControlDependence> csucList) {
        this.csucList = csucList;
    }

    /**
     * @return
     */
    public long getMaxWeight() {
        return maxWeight;
    }

    /**
     * @param maxWeight
     */
    public void setMaxWeight(long maxWeight) {
        this.maxWeight = maxWeight;
    }

    /**
     * @param maxsize
     */
    public void addMaxWeight(long maxsize) {
        this.maxWeight = this.maxWeight + maxsize;
    }

    /**
     * @param maxsize
     */
    public void removeMaxWeight(long maxsize) {
        this.maxWeight = this.maxWeight - maxsize;
    }

    /**
     * @return
     */
    public long getAveWeight() {
        return aveWeight;
    }

    /**
     * @param aveWeight
     */
    public void setAveWeight(long aveWeight) {
        this.aveWeight = aveWeight;
    }

    /**
     * @param avesize
     */
    public void addAveWeight(long avesize) {
        this.aveWeight = this.aveWeight + avesize;
    }

    /**
     * @param avesize
     */
    public void removeAveWeight(long avesize) {
        this.aveWeight = this.aveWeight - avesize;
    }

    /**
     * @return
     */
    public long getMinWeight() {
        return minWeight;
    }

    /**
     * @param minWeight
     */
    public void setMinWeight(long minWeight) {
        this.minWeight = minWeight;
    }

    /**
     * @param size
     */
    public void addMinWeight(long size) {
        this.minWeight = this.minWeight + size;
    }

    /**
     * @param size
     */
    public void removeMinWeight(long size) {
        this.minWeight = this.minWeight - size;
    }

    /**
     * @return
     */
    public Vector<Long> getIDVector() {
        return IDVector;
    }

    /**
     * @param IDVector
     */
    public void setIDVector(Vector<Long> IDVector) {
        this.IDVector = IDVector;
    }

    /**
     * @return
     */
    public long getBlevel() {
        return blevel;
    }

    /**
     * @param blevel
     */
    public void setBlevel(long blevel) {
        this.blevel = blevel;
    }

    /**
     * @return
     */
    public long getTlevel() {
        return tlevel;
    }

    /**
     * @param tlevel
     */
    public void setTlevel(long tlevel) {
        this.tlevel = tlevel;
    }

    /**
     * @return
     */
    public Vector<Long> getTpred() {
        return Tpred;
    }

    /**
     * @param tpred
     */
    public void setTpred(Vector<Long> tpred) {
        Tpred = tpred;
    }

    /**
     * @return
     */
    public Vector<Long> getBsuc() {
        return Bsuc;
    }

    /**
     * @param bsuc
     */
    public void setBsuc(Vector<Long> bsuc) {
        Bsuc = bsuc;
    }

    /**
     * @return
     */
    public long getCurrentID() {
        Long currentID = this.IDVector.lastElement();
        return currentID.longValue();
    }


    /**
     * @param dd
     */
    public boolean addDpred(DataDependence dd) {
        Vector<Long> in_fromID = dd.getFromID();
        Vector<Long> in_toID = dd.getToID();

        if(AplOperator.getInstance().isIDEqual(in_fromID,in_toID)){
            return false;
        }

        int size = this.dpredList.size();
        //Dpredリストを全てチェックするループ
        for (int i = 0; i < size; i++) {
            DataDependence tmp_dd = this.dpredList.get(i);
            Vector<Long> tmp_fromID = tmp_dd.getFromID();
            Vector<Long> tmp_toID = tmp_dd.getToID();

            //同じIDであれば追加せず，falseを返す．
            if ((AplOperator.getInstance().isIDEqual(tmp_fromID, in_fromID)) &&
                    (AplOperator.getInstance().isIDEqual(tmp_toID, in_toID))) {
                /*if(AplOperator.getInstance().isIDEqual(tmp_fromID,in_fromID)){ */
                return false;
            }
        }
        this.dpredList.add(dd);
        return true;
    }

    /**
     * @param dd
     */
    public boolean addDpredForce(DataDependence dd) {
        Vector<Long> in_fromID = dd.getFromID();
        Vector<Long> in_toID = dd.getToID();

        if(AplOperator.getInstance().isIDEqual(in_fromID,in_toID)){
            return false;
        }

        int size = this.dpredList.size();
        int foundIDX = -1;

        //Dpredリストを全てチェックするループ
        for (int i = 0; i < size; i++) {
            DataDependence tmp_dd = this.dpredList.get(i);
            Vector<Long> tmp_fromID = tmp_dd.getFromID();
            Vector<Long> tmp_toID = tmp_dd.getToID();

            //同じIDであれば追加せず，falseを返す．
            if ((AplOperator.getInstance().isIDEqual(tmp_fromID, in_fromID)) &&
                    (AplOperator.getInstance().isIDEqual(tmp_toID, in_toID))) {
            /*if(AplOperator.getInstance().isIDEqual(tmp_fromID,in_fromID)){ */
               // return false;
                foundIDX = i;
                break;
            }
        }
        if(foundIDX != -1){
            this.dpredList.remove(foundIDX);
        }
        this.dpredList.add(dd);
        return true;
    }


    public boolean AddDpredSimply(DataDependence dd){
        Vector<Long> in_fromID = dd.getFromID();
        Vector<Long> in_toID = dd.getToID();

        if(AplOperator.getInstance().isIDEqual(in_fromID,in_toID)){
            return false;
        }

        int size = this.dpredList.size();
        //Dpredリストを全てチェックするループ
        for (int i = 0; i < size; i++) {
            DataDependence tmp_dd = this.dpredList.get(i);
            Vector<Long> tmp_fromID = tmp_dd.getFromID();
            Vector<Long> tmp_toID = tmp_dd.getToID();

            //同じIDであれば追加せず，falseを返す．
            if (AplOperator.getInstance().isIDEqual(tmp_fromID, in_fromID)){
            /*if(AplOperator.getInstance().isIDEqual(tmp_fromID,in_fromID)){ */
                return false;
            }
        }
        this.dpredList.add(dd);
        return true;


    }

    /**
     * @param fromID
     * @param toID
     * @return
     */
    public boolean delDpred(Vector<Long> fromID, Vector<Long> toID) {
        int size = this.dpredList.size();
        //DpredListのリスト内チェックループ
        for (int i = 0; i < size; i++) {
            DataDependence tmp_dd = this.dpredList.get(i);
            //同じIDが見つかれば，それを削除する
            if ((AplOperator.getInstance().isIDEqual(tmp_dd.getFromID(), fromID)) &&
                    (AplOperator.getInstance().isIDEqual(tmp_dd.getToID(), toID))) {
                this.dpredList.remove(i);
                return true;
            }
        }
        return false;
    }


    public boolean delDpredSimply(Vector<Long> fromID) {
        int size = this.dpredList.size();
        //DpredListのリスト内チェックループ
        for (int i = 0; i < size; i++) {
            DataDependence tmp_dd = this.dpredList.get(i);
            //同じIDが見つかれば，それを削除する
            if ((AplOperator.getInstance().isIDEqual(tmp_dd.getFromID(),fromID))) {
                this.dpredList.remove(i);
                return true;
            }
        }
        return false;
    }


    /**
     * @param fromID
     * @param toID
     * @return
     */
    public DataDependence findDDFromDpredList(Vector<Long> fromID, Vector<Long> toID) {
        int size = this.dpredList.size();
        Iterator<DataDependence> ite = this.dpredList.iterator();
        //DpredListのリスト内チェックループ
        //for (int i = 0; i < size; i++) {
        while(ite.hasNext()){
           // DataDependence tmp_dd = this.dpredList.get(i);
            DataDependence tmp_dd = ite.next();
            //同じIDが見つかれば，それを取得する
            if ((AplOperator.getInstance().isIDEqual(tmp_dd.getFromID(), fromID)) &&
                    (AplOperator.getInstance().isIDEqual(tmp_dd.getToID(), toID))) {
                return tmp_dd;
            }
        }
        return null;
    }

    public DataDependence findDDFromDpredListSimply(Vector<Long> fromID) {
        int size = this.dpredList.size();
        //DpredListのリスト内チェックループ
        for (int i = 0; i < size; i++) {
            DataDependence tmp_dd = this.dpredList.get(i);
            //同じIDが見つかれば，それを削除する
            if (AplOperator.getInstance().isIDEqual(tmp_dd.getFromID(), fromID)) {
                return tmp_dd;
            }
        }
        return null;
    }


    /**
     * @return
     */
    public boolean updateDpred(DataDependence dd) {
        Vector<Long> in_fromID = dd.getFromID();
        Vector<Long> in_toID = dd.getToID();
        if(AplOperator.getInstance().isIDEqual(in_fromID,in_toID)){
            return false;
        }
        
        int size = this.dpredList.size();

        for (int i = 0; i < size; i++) {
            DataDependence tmp_dd = this.dpredList.get(i);
            Vector<Long> tmp_fromID = tmp_dd.getFromID();
            Vector<Long> tmp_toID = tmp_dd.getToID();

            if ((AplOperator.getInstance().isIDEqual(tmp_fromID, in_fromID)) &&
                    (AplOperator.getInstance().isIDEqual(tmp_toID, in_toID))) {
                this.dpredList.set(i, dd);
                return true;

            }
        }
        return false;
    }

    /**
     * @param dd
     * @return
     */
    public boolean addDsuc(DataDependence dd) {
        Vector<Long> in_fromID = dd.getFromID();
        Vector<Long> in_toID = dd.getToID();
        if(AplOperator.getInstance().isIDEqual(in_fromID,in_toID)){
            return false;
        }
        int size = this.dsucList.size();
        //Dsucリストを全てチェックするループ
        for (int i = 0; i < size; i++) {
            DataDependence tmp_dd = this.dsucList.get(i);
            Vector<Long> tmp_fromID = tmp_dd.getFromID();
            Vector<Long> tmp_toID = tmp_dd.getToID();

            //同じIDであれば追加せず，falseを返す．
            if ((AplOperator.getInstance().isIDEqual(tmp_fromID, in_fromID)) &&
                    (AplOperator.getInstance().isIDEqual(tmp_toID, in_toID))) {
                return false;
            }
        }


        if(this.maxData <= dd.getMaxDataSize()){
            this.maxData = dd.getMaxDataSize();
        }

        if(this.minData>=dd.getMaxDataSize()){
            this.minData = dd.getMaxDataSize();
        }
        this.dsucList.add(dd);
        return true;
    }

    public boolean addDsuc(DataDependence dd, BBTask apl) {
        Vector<Long> in_fromID = dd.getFromID();
        Vector<Long> in_toID = dd.getToID();
        if(AplOperator.getInstance().isIDEqual(in_fromID,in_toID)){
            return false;
        }
        int size = this.dsucList.size();
        //Dsucリストを全てチェックするループ
        for (int i = 0; i < size; i++) {
            DataDependence tmp_dd = this.dsucList.get(i);
            Vector<Long> tmp_fromID = tmp_dd.getFromID();
            Vector<Long> tmp_toID = tmp_dd.getToID();

            //同じIDであれば追加せず，falseを返す．
            if ((AplOperator.getInstance().isIDEqual(tmp_fromID, in_fromID)) &&
                    (AplOperator.getInstance().isIDEqual(tmp_toID, in_toID))) {
                return false;
            }
        }


        if(this.maxData <= dd.getMaxDataSize()){
            this.maxData = dd.getMaxDataSize();
        }

        if(this.minData>=dd.getMaxDataSize()){
            this.minData = dd.getMaxDataSize();
        }
        this.dsucList.add(dd);
        return true;
    }

    public boolean addDsucForce(DataDependence dd) {
        Vector<Long> in_fromID = dd.getFromID();
        Vector<Long> in_toID = dd.getToID();
        if(AplOperator.getInstance().isIDEqual(in_fromID,in_toID)){
            return false;
        }
        int size = this.dsucList.size();
        int foundIDX = -1;

        //Dsucリストを全てチェックするループ
        for (int i = 0; i < size; i++) {
            DataDependence tmp_dd = this.dsucList.get(i);
            Vector<Long> tmp_fromID = tmp_dd.getFromID();
            Vector<Long> tmp_toID = tmp_dd.getToID();

            //同じIDであれば上書きする．
            if ((AplOperator.getInstance().isIDEqual(tmp_fromID, in_fromID)) &&
                    (AplOperator.getInstance().isIDEqual(tmp_toID, in_toID))) {
                foundIDX = i;
                break;

               // return false;
            }
        }
        if(foundIDX != -1){
            this.dsucList.remove(foundIDX);
        }

        this.dsucList.add(dd);
        return true;
    }


    public boolean addDsucSimply(DataDependence dd){
        Vector<Long> in_fromID = dd.getFromID();
        Vector<Long> in_toID = dd.getToID();
        if(AplOperator.getInstance().isIDEqual(in_fromID,in_toID)){
            return false;
        }
        int size = this.dsucList.size();
        //Dsucリストを全てチェックするループ
        for (int i = 0; i < size; i++) {
            DataDependence tmp_dd = this.dsucList.get(i);
            Vector<Long> tmp_fromID = tmp_dd.getFromID();
            Vector<Long> tmp_toID = tmp_dd.getToID();

            //同じIDであれば追加せず，falseを返す．
            if (AplOperator.getInstance().isIDEqual(tmp_toID, in_toID)) {
                return false;
            }
        }
        this.dsucList.add(dd);
        return true;


    }

    /**
     * @param fromID
     * @param toID
     * @return
     */
    public boolean delDsuc(Vector<Long> fromID, Vector<Long> toID) {
        int size = this.dsucList.size();
        //DpredListのリスト内チェックループ
        for (int i = 0; i < size; i++) {
            DataDependence tmp_dd = this.dsucList.get(i);
            //同じIDが見つかれば，それを削除する
            if ((AplOperator.getInstance().isIDEqual(tmp_dd.getFromID(), fromID)) &&
                    (AplOperator.getInstance().isIDEqual(tmp_dd.getToID(), toID))) {
                this.dsucList.remove(i);
                return true;
            }
        }
        return false;
    }

    public boolean delDsucSimply(Vector<Long> toID){
        int size = this.dsucList.size();
        //DpredListのリスト内チェックループ
        for (int i = 0; i < size; i++) {
            DataDependence tmp_dd = this.dsucList.get(i);
            //同じIDが見つかれば，それを削除する
            if ((AplOperator.getInstance().isIDEqual(tmp_dd.getToID(),toID))){
                this.dsucList.remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * @param fromID
     * @param toID
     * @return
     */
    public DataDependence findDDFromDsucList(Vector<Long> fromID, Vector<Long> toID) {
        int size = this.dsucList.size();
        //DpredListのリスト内チェックループ
        for (int i = 0; i < size; i++) {
            DataDependence tmp_dd = this.dsucList.get(i);
            //同じIDが見つかれば，それを削除する
            if ((AplOperator.getInstance().isIDEqual(tmp_dd.getFromID(), fromID)) &&
                    (AplOperator.getInstance().isIDEqual(tmp_dd.getToID(), toID))) {
                return tmp_dd;
            }
        }
        return null;
    }

    public DataDependence findDDFromDsucListSimply(Vector<Long> toID) {
        int size = this.dsucList.size();
        //DpredListのリスト内チェックループ
        for (int i = 0; i < size; i++) {
            DataDependence tmp_dd = this.dsucList.get(i);
            //同じIDが見つかれば，それを削除する
            if (AplOperator.getInstance().isIDEqual(tmp_dd.getToID(), toID)) {
                return tmp_dd;
            }
        }
        return null;
    }


    /**
     * @return
     */
    public boolean updateDsuc(DataDependence dd) {
        Vector<Long> in_fromID = dd.getFromID();
        Vector<Long> in_toID = dd.getToID();
        int size = this.dsucList.size();
        if(AplOperator.getInstance().isIDEqual(in_fromID,in_toID)){
            return false;
        }


        for (int i = 0; i < size; i++) {
            DataDependence tmp_dd = this.dsucList.get(i);
            Vector<Long> tmp_fromID = tmp_dd.getFromID();
            Vector<Long> tmp_toID = tmp_dd.getToID();

            if ((AplOperator.getInstance().isIDEqual(tmp_fromID, in_fromID)) &&
                    (AplOperator.getInstance().isIDEqual(tmp_toID, in_toID))) {
                this.dsucList.set(i, dd);
                return true;

            }
        }
        return false;
    }

    /**
     * @param cd
     * @return
     */
    public boolean addCpred(ControlDependence cd) {
        Vector<Long> in_labelfromID = cd.getLabelFromID();
        //Vector<Long> in_labeltoID = cd.getLabelToID();
        Vector<Long> in_toID = cd.getCsucID();

        int size = this.cpredList.size();
        //Cpredリストを全てチェックするループ
        for (int i = 0; i < size; i++) {
            ControlDependence tmp_cd = this.cpredList.get(i);
            Vector<Long> tmp_labelfromID = tmp_cd.getLabelFromID();
            //Vector<Long> tmp_labeltoID = tmp_cd.getLabelToID();
            Vector<Long> tmp_labeltoID = tmp_cd.getCsucID();

            //同じIDであれば追加せず，falseを返す．
            if ((AplOperator.getInstance().isIDEqual(tmp_labelfromID, in_labelfromID)) &&
                    (AplOperator.getInstance().isIDEqual(tmp_labeltoID, in_toID))) {
                return false;
            }
        }
        this.cpredList.add(cd);
        return true;
    }

    /**
     * @param fromID
     * @param toID CsucID
     * @return
     */
    public boolean delCpred(Vector<Long> fromID, Vector<Long> toID) {
        int size = this.cpredList.size();
        //CpredListのリスト内チェックループ
        for (int i = 0; i < size; i++) {
            ControlDependence tmp_cd = this.cpredList.get(i);
            //同じIDが見つかれば，それを削除する
            if ((AplOperator.getInstance().isIDEqual(tmp_cd.getLabelFromID(), fromID)) &&
                    (AplOperator.getInstance().isIDEqual(tmp_cd.getCsucID(), toID))) {
                this.cpredList.remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * @param fromID
     * @param toID   CsucID
     * @return
     */
    public ControlDependence findCDFromCpredList(Vector<Long> fromID, Vector<Long> toID) {
        int size = this.cpredList.size();
        //DpredListのリスト内チェックループ
        for (int i = 0; i < size; i++) {
            ControlDependence tmp_cd = this.cpredList.get(i);
            //同じIDが見つかれば，それを削除する
            if ((AplOperator.getInstance().isIDEqual(tmp_cd.getLabelFromID(), fromID)) &&
                    (AplOperator.getInstance().isIDEqual(tmp_cd.getCsucID(), toID))) {
                return tmp_cd;
            }
        }
        return null;
    }

    /**
     * @param cd
     * @return
     */
    public boolean updateCpred(ControlDependence cd) {
        Vector<Long> in_fromID = cd.getLabelFromID();
        //Vector<Long> in_toID = cd.getLabelToID();
        Vector<Long> in_toID = cd.getCsucID();

        int size = this.cpredList.size();

        for (int i = 0; i < size; i++) {
            ControlDependence tmp_cd = this.cpredList.get(i);
            Vector<Long> tmp_fromID = tmp_cd.getLabelFromID();
            //Vector<Long> tmp_toID = tmp_cd.getLabelToID();
            Vector<Long> tmp_toID = tmp_cd.getCsucID();

            if ((AplOperator.getInstance().isIDEqual(tmp_fromID, in_fromID)) &&
                    (AplOperator.getInstance().isIDEqual(tmp_toID, in_toID))) {
                this.cpredList.set(i, cd);
                return true;

            }
        }
        return false;
    }

    /**
     * @param cd
     * @return
     */
    public boolean addCsuc(ControlDependence cd) {
        Vector<Long> in_labelfromID = cd.getLabelFromID();
        //Vector<Long> in_labeltoID = cd.getLabelToID();
        Vector<Long> in_toID = cd.getCsucID();
        int size = this.csucList.size();
        //Cpredリストを全てチェックするループ
        for (int i = 0; i < size; i++) {
            ControlDependence tmp_cd = this.csucList.get(i);
            Vector<Long> tmp_labelfromID = tmp_cd.getLabelFromID();
           // Vector<Long> tmp_labeltoID = tmp_cd.getLabelToID();
            Vector<Long> tmp_toID = tmp_cd.getCsucID();
            //同じIDであれば追加せず，falseを返す．
            if ((AplOperator.getInstance().isIDEqual(tmp_labelfromID, in_labelfromID)) &&
                    (AplOperator.getInstance().isIDEqual(tmp_toID, in_toID))) {
                return false;
            }
        }
        this.csucList.add(cd);
        return true;
    }

    /**
     * @param fromID
     * @param toID
     * @return
     */
    public boolean delCsuc(Vector<Long> fromID, Vector<Long> toID) {
        int size = this.csucList.size();
        //CpredListのリスト内チェックループ
        for (int i = 0; i < size; i++) {
            ControlDependence tmp_cd = this.csucList.get(i);
            //同じIDが見つかれば，それを削除する
            if ((AplOperator.getInstance().isIDEqual(tmp_cd.getLabelFromID(), fromID)) &&
                    (AplOperator.getInstance().isIDEqual(tmp_cd.getCsucID(), toID))) {
                this.csucList.remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * @param fromID
     * @param toID
     * @return
     */
    public ControlDependence findCDFromCsucList(Vector<Long> fromID, Vector<Long> toID) {
        int size = this.csucList.size();
        //DpredListのリスト内チェックループ
        for (int i = 0; i < size; i++) {
            ControlDependence tmp_cd = this.csucList.get(i);
            //同じIDが見つかれば，それを削除する
            if ((AplOperator.getInstance().isIDEqual(tmp_cd.getLabelFromID(), fromID)) &&
                    (AplOperator.getInstance().isIDEqual(tmp_cd.getCsucID(), toID))) {
                return tmp_cd;
            }
        }
        return null;
    }

    /**
     * @return
     */
    public boolean updateCsuc(ControlDependence cd) {
        Vector<Long> in_fromID = cd.getLabelFromID();
        Vector<Long> in_toID = cd.getCsucID();
        int size = this.csucList.size();

        for (int i = 0; i < size; i++) {
            ControlDependence tmp_cd = this.csucList.get(i);
            Vector<Long> tmp_fromID = tmp_cd.getLabelFromID();
            Vector<Long> tmp_toID = tmp_cd.getCsucID();

            if ((AplOperator.getInstance().isIDEqual(tmp_fromID, in_fromID)) &&
                    (AplOperator.getInstance().isIDEqual(tmp_toID, in_toID))) {
                this.csucList.set(i, cd);
                return true;

            }
        }
        return false;
    }


    /**
     * @param id
     * @return
     */
    public AbstractTask findTask(Vector<Long> id) {
        int size = this.IDVector.size();
        Vector<Long> tmpID = this.IDVector;

        if (size > id.size()) {
            return null;
        }

        //IDチェック
        for (int i = 0; i < size; i++) {
            if (tmpID.get(i).longValue() != id.get(i).longValue()) {
                return null;
            }
        }
        //AbstractTask recursiveTask = null;
        int retIDX = 0;
        //指定IDタスクが、このタスクの子要素に該当するときはその子要素を返す．
        if (size == id.size() - 1) {
            return this.findTaskByLastID(id.get(1));

        } else {
            //指定IDタスクが、このタスクの孫以降であれば、再帰呼び出しする。
            int tasknum = this.taskList.size();
            for (int i = 0; i < tasknum; i++) {
                AbstractTask task = this.getTaskList().get(i);
                if (AplOperator.getInstance().isIDContained(task.getIDVector(), id)) {
                    //再帰呼び出しのためにここでbreak
                    retIDX = i;
                    break;
                }
            }
            //再帰呼び出し
            return this.getTaskList().get(retIDX).findTask(id);

        }
    }


    /**
     * 子タスクを探す．あらかじめ，親タスクまでは同一だということを前提としていることに注意
     *
     * @param id
     * @return
     */
    public AbstractTask findTaskFromCurrentLayer(Vector<Long> id) {
        int tasknum = this.taskList.size();
        AbstractTask rettask = null;
        int idx = 0;
        boolean found = false;
        for (int i = 0; i < tasknum; i++) {
            AbstractTask task = this.taskList.get(i);
            Vector<Long> idlist = task.getIDVector();
            Long currentID = idlist.lastElement();

            if (currentID.longValue() == id.lastElement().longValue()) {
                idx = i;
                found = true;
                break;
            } else {
                continue;
            }
        }
        if (found) {
            return this.taskList.get(idx);
        } else {
            return null;
        }
    }

    public AbstractTask findTaskByLastID(Long id){

        return this.taskList.get(id);

    }

    /**
     * <p>子ノードを探します。
     *
     * @param lastid
     * @return
     */
    public AbstractTask findChildTask(Long lastid) {
        int size = this.taskList.size();
        int idx = 0;
        boolean found = false;
        for (int i = 0; i < size; i++) {
            AbstractTask tmptask = this.taskList.get(i);
            if (tmptask.getIDVector().lastElement().longValue() == lastid.longValue()) {
                idx = i;
                found = true;
                break;
            }
        }
        if (found) {
            return this.taskList.get(idx);
        } else {
            return null;
        }
    }

    /**
     * 既にID設定済みタスクをputする
     * @param task
     * @return
     */
    public AbstractTask addTaskSimply(AbstractTask task){
        task.setParentTask(this.getIDVector());
         this.taskList.put(task.getIDVector().get(1), task);

        return task;
    }

    /**
     * このタスクの子タスクとして、新規タスクを追加する。
     * 実際の反映は、AplOperator経由で呼ばれる。
     *
     * @param task
     * @return
     */
    public AbstractTask addTask(AbstractTask task) {
        long newid = this.createNewID(this.taskList);

        Vector<Long> pID = new Vector<Long>();

       // pID = (Vector<Long>)this.getIDVector().clone();
        pID.add(new Long(1));
        Long nID = new Long(newid);
        pID.add(nID);
        //System.out.println("new ID: "+newid);
        if(this.maxWorkload <= task.getMaxWeight()){
            this.maxWorkload = task.getMaxWeight();
        }

        if(this.minWorkload >= task.getMaxWeight()){
            this.minWorkload = task.getMaxWeight();
        }

        task.setIDVector(pID);
        task.setParentTask(this.getIDVector());
        this.taskList.put(nID, task);
        //System.out.println("new ID: "+nID+"taskID: "+this.taskList.get(new Long(nID)).getIDVector().get(1).longValue());

        return task;

    }

    public AbstractTask addTaskMulti(AbstractTask task, long startIDX) {
        //long newid = this.createNewID(this.taskList);
        int size = this.taskList.size();
        long id = 0;
        if(size < 1){
           id = startIDX;
        }else{
            id = startIDX + size ;
        }


        Vector<Long> pID = new Vector<Long>();

        // pID = (Vector<Long>)this.getIDVector().clone();
        pID.add(new Long(1));
        Long nID = new Long(id);
        pID.add(nID);
        //System.out.println("new ID: "+newid);
        if(this.maxWorkload <= task.getMaxWeight()){
            this.maxWorkload = task.getMaxWeight();
        }

        if(this.minWorkload >= task.getMaxWeight()){
            this.minWorkload = task.getMaxWeight();
        }

        task.setIDVector(pID);
        task.setParentTask(this.getIDVector());
        this.taskList.put(nID, task);
        //System.out.println("new ID: "+nID+"taskID: "+this.taskList.get(new Long(nID)).getIDVector().get(1).longValue());

        return task;

    }

    /**
     * IDを生成します。タスクリストの最後の要素ID+1とする。つまり、昇順になるようにする。
     *
     * @param tasklist
     * @return
     */
    public long createNewID(Hashtable<Long, AbstractTask> tasklist) {
        int size = tasklist.size();
        //System.out.println("サイズ: "+size);
        if (size < 1) {
            return (long) 1;
        }else{
            return (long)(size+1);
        }
    }

    /**
     * @param task
     * @return
     */
    public boolean updateTask(AbstractTask task) {
        Vector<Long> in_id = task.getIDVector();
        Long lastid = in_id.lastElement();

        int size = this.taskList.size();
        int idx = 0;
        boolean found = false;
        Collection<AbstractTask> taskCollection = this.taskList.values();
        Iterator<AbstractTask> ite = taskCollection.iterator();
        int i=0;



        while(ite.hasNext()){
            if(size == i){
                break;
            }

        //for (int i = 0; i < size; i++) {
            //AbstractTask tmptask = this.taskList.get(i);
            AbstractTask tmptask = ite.next();
            if (tmptask.getIDVector().lastElement().longValue() == lastid.longValue()) {
                idx = i;
                found = true;
                break;
            }
            i++;
        }


        if (found) {

            this.taskList.put(new Long(idx), task);

            return true;
        } else {
            return false;
        }

    }   


    /**
     * @param task
     * @return
     */
    public boolean removeTask(AbstractTask task) {
        Vector<Long> in_id = task.getIDVector();
        Long lastid = in_id.lastElement();

        int size = this.taskList.size();
        int idx = 0;
        boolean found = false;
        for (int i = 0; i < size; i++) {
            AbstractTask tmptask = this.taskList.get(i);
            if (tmptask.getIDVector().lastElement().longValue() == lastid.longValue()) {
                idx = i;
                found = true;
                break;
            }
        }
        if (found) {
            this.taskList.remove(idx);
            return true;
        } else {
            return false;
        }
    }


    public Vector<Long> getStartTask() {
        return startTask;
    }

    public void setStartTask(Vector<Long> startTask) {
        this.startTask = startTask;
    }                        

    public Vector<Long> getEndTask() {
        return endTask;
    }

    public void setEndTask(Vector<Long> endTask) {
        this.endTask = endTask;
    }


    public HashSet<Long> getAncestorIDList() {
        return ancestorIDList;
    }

    public void setAncestorIDList(HashSet<Long> ancestorIDList) {
        this.ancestorIDList = ancestorIDList;

    }

    public HashSet<Long> getDancestorIDList(){
        return this.dancestorIDList;
    }

    public void setDancestorIDList(HashSet<Long> list){
        this.dancestorIDList = list;
    }

    public long getPriorityTlevel() {
        return priorityTlevel;
    }

    public void setPriorityTlevel(long priorityTlevel) {
        this.priorityTlevel = priorityTlevel;
    }

    public long getPriorityBlevel() {
        return priorityBlevel;
    }

    public void setPriorityBlevel(long priorityBlevel) {
        this.priorityBlevel = priorityBlevel;
    }

    public long getScheduledTlevel() {
        return scheduledTlevel;
    }

    public void setScheduledTlevel(long scheduledTlevel) {
        this.scheduledTlevel = scheduledTlevel;
    }

    public long getScheduledBlevel() {
        return scheduledBlevel;
    }

    public void setScheduledBlevel(long scheduledBlevel) {
        this.scheduledBlevel = scheduledBlevel;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public CustomIDSet getParentIDSet() {
        return parentIDSet;
    }

    public void setParentIDSet(CustomIDSet parentIDSet) {
        this.parentIDSet = parentIDSet;
    }

    public CustomIDSet getClusterSet() {
        return clusterSet;
    }

    public void setClusterSet(CustomIDSet clusterSet) {
        this.clusterSet = clusterSet;
    }

    public long getCnt_max() {
        return cnt_max;
    }

    public void setCnt_max(long cnt_max) {
        this.cnt_max = cnt_max;
    }

    public long getCnt_min() {
        return cnt_min;
    }

    public void setCnt_min(long cnt_min) {
        this.cnt_min = cnt_min;
    }

    public long getAve_procTime() {
        return ave_procTime;
    }

    public void setAve_procTime(long ave_procTime) {
        this.ave_procTime = ave_procTime;
    }

    public long getAve_oct() {
        return ave_oct;
    }

    public void setAve_oct(long ave_oct) {
        this.ave_oct = ave_oct;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }
}


