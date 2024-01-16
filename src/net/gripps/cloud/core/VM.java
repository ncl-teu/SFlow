package net.gripps.cloud.core;

import net.gripps.ccn.icnsfc.process.AutoSFCMgr;
import net.gripps.cloud.CloudUtil;
import net.gripps.cloud.nfv.sfc.SFC;
import net.gripps.cloud.nfv.sfc.VNF;
import net.gripps.clustering.common.aplmodel.CustomIDSet;
import net.gripps.clustering.common.aplmodel.DataDependence;
import net.gripps.environment.CPU;
import net.gripps.environment.Machine;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * Created by kanemih on 2018/11/01.
 */
public class VM /*extends Machine*/ implements Serializable {
    /**
     * vCPUのリスト
     */
    protected HashMap<String, VCPU> vCPUMap;

    /**
     * メモリのサイズ(MB)
     */
    protected long ramSize;

    /**
     * このVMのID(文字列）
     */
    protected String VMID;

    /**
     * このVMが属するホストID
     */
    protected String hostID;

    /**
     * オリジナルVMのID
     * もしこのVMが，元々のVMに対してインスタンス生成されたものであれば，
     * オリジナルのものと同じはず．
     */
    protected String  orgVMID;

    protected String ipAddr;

    /**
     * 自身が把握しているSFC情報(割当について把握している部分を反映）
     */
    protected HashMap<Long, SFC> sfcMap;

    protected String name;

    /**
     * DLしたVNFイメージのタイプ集合
     */
    protected CustomIDSet dlSet;

    private HashSet<Integer> typeSet;



    public VM(){
        this.sfcMap = new HashMap<Long, SFC>();
        this.dlSet = new CustomIDSet();

    }

    public VM(String  ID, String in_hostID,   HashMap<String, VCPU> vCPUMap, long ramSize, String orgVMID) {
        //super(machineID, cpuMap, num);

        this.VMID = ID;
        this.hostID = in_hostID;
        this.vCPUMap = vCPUMap;
        this.ramSize = ramSize;
        this.orgVMID = orgVMID;
        this.ipAddr = null;
        this.sfcMap = new HashMap<Long, SFC>();
        this.name = null;
        this.dlSet = new CustomIDSet();
        this.typeSet = new HashSet<Integer>();

    }

    public HashSet<Integer> getTypeSet() {
        return typeSet;
    }

    public void setTypeSet(HashSet<Integer> typeSet) {
        this.typeSet = typeSet;
    }


    public CustomIDSet getDlSet() {
        return dlSet;
    }

    public void setDlSet(CustomIDSet dlSet) {
        this.dlSet = dlSet;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HashMap<Long, SFC> getSfcMap() {
        return sfcMap;
    }

    public void setSfcMap(HashMap<Long, SFC> sfcMap) {
        this.sfcMap = sfcMap;
    }

    public boolean isSFCExists(SFC sfc){
        return this.sfcMap.containsKey(sfc.getSfcID());
    }

    public boolean putSFC(SFC sfc){
        if(this.isSFCExists(sfc)){
            //mergeする．自身が持っているsfcの割当先が優先
            SFC ownSFC = this.sfcMap.get(sfc.getSfcID());
            Iterator<VNF> vIte = sfc.getVnfMap().values().iterator();
            while(vIte.hasNext()){
                VNF vnf = vIte.next();
                VNF ownVNF = ownSFC.findVNFByLastID(vnf.getIDVector().get(1));
                if((vnf.getvCPUID() != null)&&(ownVNF.getvCPUID()==null)){
                    ownVNF.setvCPUID(vnf.getvCPUID());
                }
            }
        }else{
            this.sfcMap.put(sfc.getSfcID(), sfc);
        }
        return true;
    }

    public boolean containsType(int type){
        return this.typeSet.contains(new Integer(type));
    }

    public String findAllocatedVCPU(SFC sfc, Long vnfID){
        if(this.isSFCExists(sfc)){
            SFC ownSFC = this.sfcMap.get(sfc.getSfcID());
            VNF ownVNF = ownSFC.findVNFByLastID(vnfID);
            return ownVNF.getvCPUID();
        }else{
            return null;
        }
    }

    public String getIpAddr() {
        return ipAddr;
    }

    public void setIpAddr(String ipAddr) {
        this.ipAddr = ipAddr;
    }

    public String getVMID() {
        return VMID;
    }

    public void setVMID(String VMID) {
        this.VMID = VMID;
    }

    public String getHostID() {
        return hostID;
    }

    public void setHostID(String hostID) {
        this.hostID = hostID;
    }

    public String getOrgVMID() {
        return orgVMID;
    }

    public void setOrgVMID(String orgVMID) {
        this.orgVMID = orgVMID;
    }

    public long getMIPS(){
        if(this.getvCPUMap().isEmpty()){
            return -1;
        }else{
            Iterator<VCPU> vIte = this.getvCPUMap().values().iterator();
            VCPU vcpu = vIte.next();
            return vcpu.getMips();
        }
    }



    /**
     * 入力となるVMと同一VMを複製する．
     * @return
     */
    public VM replicate(){
        System.gc();
        try{
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bout);
            out.writeObject(this);
            out.close();
            byte[] bytes = bout.toByteArray();
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
            Object newObject = in.readObject();
            in.close();
            VM newVM = (VM) newObject;

            return newVM;

        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }


    /**
     * このVM自体のCPU使用率を計算します．
     * @return
     */
    public double getCPUUsage(){
        int vCPUNum = this.vCPUMap.size();
        Iterator<VCPU> vIte = this.vCPUMap.values().iterator();
        long totalMips = 0;
        long totalUsedMips =0 ;
        while(vIte.hasNext()){
            VCPU vCPU = vIte.next();
            totalMips += vCPU.getMips();
            totalUsedMips += vCPU.getUsedMips();
        }
        return CloudUtil.getRoundedValue((double)(100*totalUsedMips/totalMips));

    }

    public HashMap<String, VCPU> getvCPUMap() {
        return vCPUMap;
    }

    public void setvCPUMap(HashMap<String, VCPU> vCPUMap) {
        this.vCPUMap = vCPUMap;
    }

    public long getRamSize() {
        return ramSize;
    }

    public void setRamSize(long ramSize) {
        this.ramSize = ramSize;
    }
}
