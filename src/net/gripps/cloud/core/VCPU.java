package net.gripps.cloud.core;

import net.gripps.ccn.core.CCNRouter;
import net.gripps.ccn.icnsfc.process.AutoSFCMgr;
import net.gripps.cloud.CloudUtil;
import net.gripps.cloud.mapreduce.core.IMapReduce;
import net.gripps.cloud.mapreduce.datamodel.*;
import net.gripps.cloud.nfv.sfc.StartTimeComparator;
import net.gripps.cloud.nfv.sfc.VNF;
import net.gripps.clustering.common.aplmodel.CustomIDSet;
import net.gripps.environment.CPU;
import org.ncl.workflow.util.NCLWUtil;

import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by kanemih on 2018/11/01.
 * CPUを継承するクラスです．
 * CPUクラスに，さらに所属するVMを表す機能が追加されます．
 */
public class VCPU extends CPU  implements Runnable, IMapReduce {

    /**
     * vCPUのIDです．dc_id^host_id^cpu_id^core_id^number
     * から構成されます．
     */
    private String prefix;

    /**
     * コアのPrefix
     */
    private String corePrefix;

    /**
     *
     */
    private HashMap<String, Long> prefixMap;

    /**
     * このvCPUが所属するVMのID
     */
    private String VMID;

    /**
     * MIPSの定義
     */
    private long mips;


    /**
     * 占有されているMIPS
     */
    private long usedMips;

    /**
     * Out SF集合
     */
    private CustomIDSet outSFSet;

    private double voltage;

    /**
     * DLしたVNFイメージリスト
     */
    private HashMap<Long, VNF> dlVNFMap;

    /**
     * VNFQueueのうち，Readyになったものがキューイングされるリスト
     */
    private HashMap<String, VNF> execQueue;

    /**
     * VNFのコンテナイメージのDL順番を
     */
    private LinkedList<VNF> dlQueue;

    /**
     * Imageのdl完了時刻の最大値
     * 最後のimageのDL完了時刻，ということ．
     * dlalgorithm
     */
    private double lastImageDLTime;




    /**
     * VNFのリスト．最初から持っている場合もあれば，あとで追加される場合もある．
     */
   // private HashMap<Long, VNF> vnfMap;


    /**
     * 割り当てられたVNFのキュー
     */
    private  PriorityQueue<VNF> vnfQueue;


    private double finishTimeAtClusteringPhase;

    private  long  remainedCapacity;

    private boolean isFake;

    private boolean realMode;


    private LinkedBlockingDeque<VNF> inputQueue;

    private VNF currentVNF;

    //private LinkedBlockingDeque<>

    public VCPU(){
        this.isFake = true;
        this.realMode = false;
        this.execQueue = new HashMap<String, VNF>();

    }



    public boolean isRealMode() {
        return realMode;
    }

    public void setRealMode(boolean realMode) {
        this.realMode = realMode;

    }

    public String genKey(VNF vnf){
        StringBuffer buf = new StringBuffer(String.valueOf(vnf.getIDVector().get(0)));
        buf.append(String.valueOf(vnf.getIDVector().get(1)));
        return buf.toString();
    }
    public void exec(VNF vnf){
        while(this.currentVNF != null){
            try{
                Thread.sleep(10);
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        this.execQueue.put(this.genKey(vnf), vnf);
        this.currentVNF = vnf;
    }

    @Override
    public void run() {
        if(this.realMode){
            while(true){
                try{
                    //実際には，入力データがそろってから処理をする．
                    Thread.sleep(100);
                    if(this.currentVNF == null){
                        continue;
                    }else{
                        //何かVNFに入っている場合
                        VNF vnf = this.execQueue.get(this.genKey(this.currentVNF));

                        double time = AutoSFCMgr.getIns().calcExecTime(vnf.getWorkLoad(), this);
                        //DL timeを計測．
                        double dlTime = AutoSFCMgr.getIns().calcImageDLTime(vnf, this);
                        time += dlTime;


                        long start = System.currentTimeMillis();
                        Thread.sleep((long)(time*1000));
                        long end = System.currentTimeMillis();
                        long duration = end - start;
                        this.execQueue.remove(this.genKey(this.currentVNF));
                        this.currentVNF = null;
                        CCNRouter router = (CCNRouter) NCLWUtil.findVM(AutoSFCMgr.getIns().getEnv(), this.getPrefix());
//System.out.println("Exec END@SFC:"+vnf.getIDVector().get(0) + "VNF:"+vnf.getIDVector().get((1)) + " with Time:"+time + "@Router"+router.getRouterID());

                        //終わったら，ルータへデータ返送の手続きを行う．
                        //割り当て済みだったSFを削除する．
                        this.vnfQueue.remove(vnf);


                        router.sendResultantData(vnf, (long)(time*1000));


                    }
                }catch(Exception  e){
                    e.printStackTrace();
                }


            }
        }else{


        }

    }


    public VCPU(Long id, long speed, Vector<Long> assignedTaskList, Vector<Long> scheduledTaskList,
                String prefix, String cPrefix, HashMap<String, Long> prefixMap, String  vmID, long mips, long usedMips) {
        super(id, speed, assignedTaskList, scheduledTaskList);
        this.realMode = false;

        this.prefix = prefix;
        this.corePrefix = cPrefix;
        this.prefixMap = prefixMap;
        VMID = vmID;
        this.mips = mips;
        this.usedMips = usedMips;
       // this.vnfMap = vnfMap;
        this.setSpeed(mips);
        //this.assignedVNFSet = new CustomIDSet();
        this.vnfQueue = new PriorityQueue<VNF>(5, new StartTimeComparator());
        this.finishTimeAtClusteringPhase = 0d;

        this.remainedCapacity = mips * 10000;
        this.isFake = false;

        this.outSFSet = new CustomIDSet();

        this.voltage = 0;
        this.dlVNFMap = new HashMap<Long, VNF>();
        this.execQueue = new HashMap<String, VNF> ();

        this.dlQueue = new LinkedList<VNF>();
        this.lastImageDLTime = 0.0d;

    }

    public double getLastImageDLTime() {
        return lastImageDLTime;
    }

    public void setLastImageDLTime(double lastImageDLTime) {
        this.lastImageDLTime = lastImageDLTime;
    }

    public LinkedList<VNF> getDlQueue() {
        return dlQueue;
    }

    public void setDlQueue(LinkedList<VNF> dlQueue) {
        this.dlQueue = dlQueue;
    }

    public VCPU(String prefix, String cPrefix, HashMap<String, Long> prefixMap, String  vmID, long mips, long usedMips) {
        this.realMode = false;

        this.prefix = prefix;
        this.corePrefix = cPrefix;
        this.prefixMap = prefixMap;
        VMID = vmID;
        this.mips = mips;
        this.setSpeed(mips);
        this.usedMips = usedMips;
       // this.vnfMap = vnfMap;
      //  this.assignedVNFSet = new CustomIDSet();
        this.vnfQueue = new PriorityQueue<VNF>(5, new StartTimeComparator());
        this.finishTimeAtClusteringPhase = 0d;
        this.isFake = false;
        this.outSFSet = new CustomIDSet();
        this.dlVNFMap = new HashMap<Long, VNF>();
        this.execQueue = new HashMap<String, VNF> ();
        this.dlQueue = new LinkedList<VNF>();



    }

    public boolean contiansVNF(VNF vnf){
        Iterator<VNF> vIte = this.vnfQueue.iterator();
        boolean ret = false;
        while(vIte.hasNext()){
            VNF v = vIte.next();
            if(vnf.getIDVector().get(0).equals( v.getIDVector().get(0))){
                if(vnf.getIDVector().get(1).equals(v.getIDVector().get(1))){
                    ret = true;
                    break;
                }
            }
        }
        return ret;
    }

    public HashMap<String, VNF>  getExecQueue() {
        return execQueue;
    }

    public void setExecQueue(HashMap<String, VNF>  execQueue) {
        this.execQueue = execQueue;
    }

    public HashMap<Long, VNF> getDlVNFMap() {
        return dlVNFMap;
    }

    public void setDlVNFMap(HashMap<Long, VNF> dlVNFMap) {
        this.dlVNFMap = dlVNFMap;
    }

    public double getVoltage() {
        return voltage;
    }

    public void setVoltage(double voltage) {
        this.voltage = voltage;
    }

    @Override
    public OutputSplit mapProcess(InputSplit is) {
        return null;
    }

    @Override
    public MergedSplit collectProcess(OutputSplit os) {
        return null;
    }

    @Override
    public SpillSplit spillProcess(MergedSplit ms) {
        return null;
    }

    @Override
    public MergedFileSplit mergeProcess(SpillSplit ss) {
        return null;
    }

    @Override
    public void shuffleSendProcess(MergedFileSplit mfs) {

    }

    @Override
    public ShuffleFileSplit shuffleReceiveProcess() {
        return null;
    }

    @Override
    public ReduceOutputFile reduceProcess(ShuffleFileSplit sfs) {
        return null;
    }

    @Override
    public boolean sendReduceOutputFile(ReduceOutputFile rof) {
        return false;
    }

    public boolean isFake() {
        return isFake;
    }

    public void setFake(boolean fake) {
        isFake = fake;
    }

    public String getCorePrefix() {
        return corePrefix;
    }

    public void setCorePrefix(String corePrefix) {
        this.corePrefix = corePrefix;
    }

    public synchronized PriorityQueue<VNF> getVnfQueue() {
        return vnfQueue;
    }

    public synchronized void setVnfQueue(PriorityQueue<VNF> vnfQueue) {
        this.vnfQueue = vnfQueue;
    }

    public HashMap<String, Long> getPrefixMap() {
        return prefixMap;
    }

    public void setPrefixMap(HashMap<String, Long> prefixMap) {
        this.prefixMap = prefixMap;
    }

    public long getUsedMips() {
        return usedMips;
    }

    public void setUsedMips(long usedMips) {
        this.usedMips = usedMips;
    }

    public String getVMID() {

        return VMID;
    }

    public Long getVCPUID(){
        return this.prefixMap.get(CloudUtil.ID_VCPU);
    }


    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String vCPUID) {
        this.prefix = vCPUID;
    }

    public void setVMID(String VMID) {
        this.VMID = VMID;
    }

    public long getMips() {
        return mips;
    }

    public void setMips(long mips) {
        this.mips = mips;
    }

    public double getFinishTimeAtClusteringPhase() {
        return finishTimeAtClusteringPhase;
    }

    public void setFinishTimeAtClusteringPhase(double finishTimeAtClusteringPhase) {
        this.finishTimeAtClusteringPhase = finishTimeAtClusteringPhase;
    }

    public long getRemainedCapacity() {
        return remainedCapacity;
    }

    public void setRemainedCapacity(long remainedCapacity) {
        this.remainedCapacity = remainedCapacity;
    }

    public CustomIDSet getOutSFSet() {
        return outSFSet;
    }

    public void setOutSFSet(CustomIDSet outSFSet) {
        this.outSFSet = outSFSet;
    }

    /**
    public HashMap<Long, VNF> getVnfMap() {
        return vnfMap;
    }

    public void setVnfMap(HashMap<Long, VNF> vnfMap) {
        this.vnfMap = vnfMap;
    }
 **/
}
