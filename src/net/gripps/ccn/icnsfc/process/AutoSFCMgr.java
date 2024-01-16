package net.gripps.ccn.icnsfc.process;

import net.gripps.ccn.CCNUtil;
import net.gripps.ccn.core.CCNRouter;
import net.gripps.ccn.core.InterestPacket;
import net.gripps.ccn.icnsfc.AutoUtil;
import net.gripps.ccn.icnsfc.core.AutoEnvironment;
import net.gripps.ccn.icnsfc.core.AutoInfo;
import net.gripps.ccn.process.CCNMgr;
import net.gripps.cloud.CloudUtil;
import net.gripps.cloud.core.Cloud;
import net.gripps.cloud.core.ComputeHost;
import net.gripps.cloud.core.VCPU;
import net.gripps.cloud.core.VM;
import net.gripps.cloud.nfv.NFVUtil;
import net.gripps.cloud.nfv.sfc.SFC;
import net.gripps.cloud.nfv.sfc.SFCGenerator;
import net.gripps.cloud.nfv.sfc.VNF;
import net.gripps.clustering.common.aplmodel.DataDependence;
import net.gripps.clustering.tool.Calc;

import org.ncl.workflow.ccn.sfc.process.NFDTask;
import org.ncl.workflow.util.NCLWUtil;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by Hidehiro Kanemitsu on 2020/09/16
 */
public class AutoSFCMgr implements Serializable {

    /**
     * Singleton
     */
    private static AutoSFCMgr own;

    /**
     * Total # of SFCs
     */
    private long SFCNum;

    private LinkedBlockingDeque<SFC> sfcQueue;

    private AutoEnvironment env;

    /**
     * <VNF ID, Size>のマップ
     */
    protected HashMap<String, Long> imgMap;

    protected HashMap<String, AutoInfo> infoMap;

    protected int finishCount;

    protected AutoInfo resultInfo;

    protected Date currentTime;

    protected long globalCnt;

    protected boolean isSFC;

    protected HashMap<Long, SFC> sfcMap;



    public static AutoSFCMgr getIns(){
        if(AutoSFCMgr.own == null){
            AutoSFCMgr.own = new AutoSFCMgr();
        }

        return AutoSFCMgr.own;
    }

    private AutoSFCMgr(){
        this.sfcMap = new HashMap<Long, SFC>();
        this.SFCNum = 0;
        this.sfcQueue = new LinkedBlockingDeque<SFC>();
        this.imgMap = new HashMap<String, Long>();
        this.infoMap = new HashMap<String, AutoInfo>();
        this.finishCount = 0;
        this.resultInfo = new AutoInfo("result");
        //Calendarクラスのオブジェクトを生成する
        Calendar cl = Calendar.getInstance();

        //SimpleDateFormatクラスでフォーマットパターンを設定する
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        this.currentTime = cl.getTime();
        this.globalCnt = 0;
        this.isSFC = false;


    }

    public HashMap<Long, SFC> getSfcMap() {
        return sfcMap;
    }

    public void setSfcMap(HashMap<Long, SFC> sfcMap) {
        this.sfcMap = sfcMap;
    }

    public boolean isSFC() {
        return isSFC;
    }

    public void setSFC(boolean SFC) {
        isSFC = SFC;
    }

    public int getFinishCount() {
        return finishCount;
    }

    public void setFinishCount(int finishCount) {
        this.finishCount = finishCount;
    }
    public void addFinishCount(){
        this.finishCount++;


    }

    /**
     * CCRを計算します．
     * @param sfc
     * @return
     */
    public  double calcCCR(SFC sfc){
        if(!CCNMgr.getIns().isSFCMode()){
            return -1d;
        }
        Iterator<VNF> vnfIte = sfc.getVnfMap().values().iterator();
        long totalWorkload = 0;
        long totalDataSize = 0;
        long totalEdgeNum = 0;
        while(vnfIte.hasNext()){
            VNF vnf = vnfIte.next();
            totalWorkload += vnf.getWorkLoad();
            totalEdgeNum += vnf.getDsucList().size();
            Iterator<DataDependence> dsucIte = vnf.getDsucList().iterator();
            while(dsucIte.hasNext()) {
                DataDependence dd = dsucIte.next();
                totalDataSize += dd.getMaxDataSize();
            }
        }
        //次に，環境．
        long totalSpeed = 0;
        long totalBW = 0;
        long hostNum = this.env.getGlobal_hostMap().size();
        Iterator<ComputeHost> cIte = env.getGlobal_hostMap().values().iterator();
        while(cIte.hasNext()){
            ComputeHost host = cIte.next();
            totalBW += host.getBw();
        }
        double ave_bw = NFVUtil.getRoundedValue((double)totalBW / (double)hostNum);

        Iterator<VCPU> vcpuIte = env.getGlobal_vcpuMap().values().iterator();
        long vcpuNum = env.getGlobal_vcpuMap().size();
        while(vcpuIte.hasNext()){
            VCPU vcpu = vcpuIte.next();
            totalSpeed += vcpu.getMips();
        }
        double ave_speed = NFVUtil.getRoundedValue((double)totalSpeed/(double)vcpuNum);

        double ave_workload = NFVUtil.getRoundedValue((double)totalWorkload / (double) sfc.getVnfMap().size());
        double ave_datasize = NFVUtil.getRoundedValue((double)totalDataSize / (double)totalEdgeNum);

        double ave_comTime = NFVUtil.getRoundedValue((double)ave_datasize / (double) ave_bw);
        double ave_procTime = NFVUtil.getRoundedValue((double)ave_workload / (double)ave_speed);
        double CCR = NFVUtil.getRoundedValue((double)ave_comTime / (double)ave_procTime);

        return CCR;
    }

    public String genAutoID(SFC sfc){
        if(!CCNMgr.getIns().isSFCMode()){
            return null;
        }
        Long aplID = sfc.getAplID();
        Long sfcID = sfc.getSfcID();
        return this.genAutoID(aplID, sfcID);

    }

    public String genAutoID(Long aplID, Long sfcID){
        StringBuffer buf = new StringBuffer(String.valueOf(aplID));
        buf.append("^");
        buf.append(String.valueOf(sfcID));

        return buf.toString();

    }

    public boolean containsVNF(VM vm, VNF vnf){

        Iterator<VCPU> vIte = vm.getvCPUMap().values().iterator();
        boolean ret = false;
        if(vm.getDlSet().contains((long)vnf.getType())){
            ret = true;

        /*while(vIte.hasNext()){
            VCPU vcpu = vIte.next();
            Iterator<VNF> vnfIte = vcpu.getVnfQueue().iterator();
            while(vnfIte.hasNext()){
                VNF invnf = vnfIte.next();
                if(invnf.getType() == vnf.getType()){
                    ret = true;
                    break;
                }

            }

         */
            /*if(!vcpu.getHavingVNFSet().isEmpty()){
                Iterator<VNF> vIte2 = vcpu.getHavingVNFSet().iterator();
                while(vIte2.hasNext()){
                    VNF v = vIte2.next();
                    if(v.getType()==vnf.getType()){
                        ret = true;
                        break;
                    }
                }

            }*/

        }


        return ret;
    }

    public HashMap<String, AutoInfo> getInfoMap() {
        return infoMap;
    }

    public void setInfoMap(HashMap<String, AutoInfo> infoMap) {
        this.infoMap = infoMap;
    }

    public HashMap<String, Long> getImgMap() {
        return imgMap;
    }

    public void setImgMap(HashMap<String, Long> imgMap) {
        this.imgMap = imgMap;
    }


    public AutoEnvironment getEnv() {
        return env;
    }

    public void setEnv(AutoEnvironment env) {
        this.env = env;
    }


    public double calcImageDLTime(VNF vnf, VCPU vcpu){

        VM vm = this.env.getGlobal_vmMap().get(vcpu.getVMID());
        return this.calcImageDLTime(vnf, vm);

    }
    public double calcImageDLTime(VNF vnf, VM vm){
        if(vm == null){
            return 0.0;
        }
        double imageSize = vnf.getImageSize();

        ComputeHost host = env.getGlobal_hostMap().get(vm.getHostID());
        if(host == null){
            return -1d;
        }
        if(this.containsVNF(vm, vnf)){
            return 0.0d;
        }else{
            double dlTime = Calc.getRoundedValue((double)((double)imageSize/(double)host.getBw()));
            vm.getDlSet().add((long)vnf.getType());
            return dlTime;
        }

    }

    public AutoInfo getAutoInfo(String autoID){
        return this.getInfoMap().get(autoID);
    }

    /**
     * 新規にSFCを生成する．
     * @return
     */
    public SFC createNewSFC(){
        //SFC sfc = SFCGenerator.getIns().multipleSFCProcess();
        NFVUtil.sfc_vnf_num = CCNUtil.genLong(AutoUtil.sfc_vnf_num_min, AutoUtil.sfc_vnf_num_max);
        SFC sfc = SFCGenerator.getIns().autoSFCProcess();

        //IDを付与する．
        this.SFCNum ++;
        sfc.setSfcID(this.SFCNum);
        this.sfcMap.put(sfc.getSfcID(), sfc);


        //あとは，各タスクのIDを変更する．
        Iterator<VNF> vIte = sfc.getVnfMap().values().iterator();
        while(vIte.hasNext()){
            VNF vnf = vIte.next();
            vnf.getIDVector().set(0, new Long(this.SFCNum));

            //imgサイズを設定する．
        }
        this.sfcQueue.add(sfc);
        //System.out.println("Num:"+sfc.getVnfMap().size());
        return sfc;

    }

    /**
     * 既存SFCのコピーを生成する．
     * IDも同一．
     * @param orgSFC
     * @return
     */
    public SFC replicateSFC(SFC orgSFC){
        SFC newSFC = (SFC)orgSFC.deepCopy();
        this.sfcQueue.add(newSFC);

        this.SFCNum++;
        return newSFC;
    }

    public void saveStartTime(InterestPacket p, SFC sfc){
        if(!CCNMgr.getIns().isSFCMode()){
            return;
        }
        if(p.getHistoryList().size() == 1){
            //初期状態
            long startTime = System.currentTimeMillis();
            String id = this.genAutoID(sfc);

            AutoInfo info = AutoSFCMgr.getIns().getAutoInfo(id);
            info.setStartTime(startTime);
        }else{
            return;
        }

    }

    public String genSFInsID(VNF vnf, VM vm){
        if(vm == null){
            return null;
        }
        Integer vnfID = Integer.valueOf(vnf.getType());
        StringBuffer buf = new StringBuffer(String.valueOf(vnfID));
        buf.append("^");
        buf.append(vm.getVMID());
        return buf.toString();

    }


    public void saveUpdatedSFInsNum(SFC sfc, VNF vnf, VM vm){
        if(!CCNMgr.getIns().isSFCMode()){
            return;
        }
        String id = this.genAutoID(sfc);
        AutoInfo info = AutoSFCMgr.getIns().getAutoInfo(id);
        HashSet<String> insSet = info.getSfInsSet();

        String insID = this.genSFInsID(vnf, vm);
        insSet.add(insID);

    }

    public void saveUpdatedCacheHitNum(SFC sfc, long cnt) {
        if(!CCNMgr.getIns().isSFCMode()){
            return;
        }
        String id = this.genAutoID(sfc);
        AutoInfo info = AutoSFCMgr.getIns().getAutoInfo(id);
        long currentNum = info.getCacheHitNum();
        currentNum += cnt;
        info.setCacheHitNum(currentNum);
    }




    public void saveFinishTime(SFC sfc){
        if(!CCNMgr.getIns().isSFCMode()){
            return;
        }
        long finishTime = System.currentTimeMillis();
        String id = this.genAutoID(sfc);

        AutoInfo info = AutoSFCMgr.getIns().getAutoInfo(id);
        info.setFinishTime(finishTime);
        long makeSpan = finishTime - info.getStartTime();
        info.setMakeSpan(makeSpan);
        this.addFinishCount();

        if(AutoSFCMgr.getIns().getFinishCount() >= AutoSFCMgr.getIns().getSFCNum()){
            HashMap<String, AutoInfo> infoMap = AutoSFCMgr.getIns().getInfoMap();
            System.out.println("Simulation Completed....");
            System.exit(1);
        }

    }

    public void saveFinishTime(Long aplID, Long sfcID){
        if(!CCNMgr.getIns().isSFCMode()){
            return;
        }
        long finishTime = System.currentTimeMillis();


        String id = this.genAutoID(aplID, sfcID);

        AutoInfo info = AutoSFCMgr.getIns().getAutoInfo(id);
        info.setFinishTime(finishTime);
        long makeSpan = finishTime - info.getStartTime();
        info.setMakeSpan(makeSpan);
        this.addFinishCount();

        /*if(AutoSFCMgr.getIns().getFinishCount() >= AutoSFCMgr.getIns().getSFCNum()){
            HashMap<String, AutoInfo> infoMap = AutoSFCMgr.getIns().getInfoMap();
            System.out.println("Simulation Completed....");
            this.outputResult();
        }*/
       // this.outputResult();
        this.outputOneResult(info);

    }




    public void saveUpdatedVCPU(SFC sfc, VCPU vcpu){
        if(!CCNMgr.getIns().isSFCMode()){
            return;
        }
        String id = this.genAutoID(sfc);
        AutoInfo info = AutoSFCMgr.getIns().getAutoInfo(id);
        HashSet<String> vSet = info.getvCPUSet();
        vSet.add(vcpu.getPrefix());
        String hostPrefix = CloudUtil.getInstance().getHostPrefix(vcpu.getPrefix());
        info.getHostSet().add(hostPrefix);

    }

    public void saveUpdatedVM(SFC sfc, VM vm){
        if(!CCNMgr.getIns().isSFCMode()){
            return;
        }
        if(vm == null){
            return;
        }
        String id = this.genAutoID(sfc);
        AutoInfo info = AutoSFCMgr.getIns().getAutoInfo(id);
        HashSet<String> vSet = info.getVmSet();
        vSet.add(vm.getVMID());

    }

    public void outputOneResult(AutoInfo info){
        Iterator<AutoInfo> aIte = this.infoMap.values().iterator();


        //SimpleDateFormatクラスでフォーマットパターンを設定する
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String fileName = "./is/"+sdf.format(this.currentTime) + ".csv";
        //String fileName = "./is/result_m"+AutoUtil.ccn_sfc_mode+"_r"+CCNUtil.ccn_routing_no + ".csv";
        double totalCCR = 0.0d;
        double totalMakeSpan = 0.0d;
        long totalHitNum = 0;
        long totalSFInsNum = 0;
        try{
            FileWriter fw = new FileWriter(fileName, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter pw = new PrintWriter(bw);
            if(this.globalCnt == 0){
                pw.println("0:Each/1:Total,Site#, Host#, VM#, vCPU#, MaxMips, MinMips, MaxBW, MinBW,Apl#, SFC#, CCR, SF#, SFIns#, MakeSpan,Host#, VM#, vCPU#, CacheHit#");

            }

            String aplJobID = info.getAplJobID();
            String newAutoID = aplJobID.replace("^", ",");
            StringBuffer buf = new StringBuffer("0,");
            buf.append(NFVUtil.num_dc+",");
            buf.append(this.env.getGlobal_hostMap().size()+",");
            buf.append(this.env.getGlobal_vmMap().size() + ",");
            buf.append(this.env.getGlobal_vcpuMap().size() + ",");
            buf.append(NFVUtil.host_mips_max + ","+NFVUtil.host_mips_min + ",");
            buf.append(NFVUtil.host_bw_max + ","+NFVUtil.host_bw_min+ ",");
            buf.append(newAutoID + ",");
            buf.append(info.getCCR()+",");
            totalCCR += info.getCCR();

            buf.append(info.getSfNum() + "," + info.getSfInsSet().size() + ",");
            this.resultInfo.setSfNum(this.resultInfo.getSfNum() + info.getSfNum());
            this.resultInfo.getSfInsSet().addAll(info.getSfInsSet());
            totalSFInsNum += info.getSfInsSet().size();

            buf.append(info.getMakeSpan()+ ",");
            totalMakeSpan += info.getMakeSpan();

            buf.append(info.getVmSet().size() + ",");
            buf.append(info.getHostSet().size() + ",");
            this.resultInfo.getVmSet().addAll(info.getVmSet());
            this.resultInfo.getHostSet().addAll(info.getHostSet());

            buf.append(info.getvCPUSet().size() + ","+info.getCacheHitNum());
            this.resultInfo.getvCPUSet().addAll(info.getvCPUSet());
            totalHitNum += info.getCacheHitNum();
            pw.println(buf.toString());



            this.resultInfo.setCCR(Calc.getRoundedValue(totalCCR/(double)this.infoMap.size()));
            this.resultInfo.setMakeSpan(((long)totalMakeSpan )/this.infoMap.size());
            this.resultInfo.setCacheHitNum(totalHitNum);

           // pw.println(buf.toString());
            pw.close();
            bw.close();
            fw.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        this.globalCnt++;
    }
    public void outputResult(){
        Iterator<AutoInfo> aIte = this.infoMap.values().iterator();


        //SimpleDateFormatクラスでフォーマットパターンを設定する
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String fileName = "./is/"+sdf.format(this.currentTime) + ".csv";
        //String fileName = "./is/result_m"+AutoUtil.ccn_sfc_mode+"_r"+CCNUtil.ccn_routing_no + ".csv";
        double totalCCR = 0.0d;
        double totalMakeSpan = 0.0d;
        long totalHitNum = 0;
        long totalSFInsNum = 0;
        try{
            FileWriter fw = new FileWriter(fileName, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter pw = new PrintWriter(bw);
            pw.println("0:Each/1:Total,Site#, Host#, VM#, vCPU#, MaxMips, MinMips, MaxBW, MinBW,Apl#, SFC#, CCR, SF#, SFIns#, MakeSpan,Host#, VM#, vCPU#, CacheHit#");
            while(aIte.hasNext()){
                AutoInfo info = aIte.next();
                String aplJobID = info.getAplJobID();
                String newAutoID = aplJobID.replace("^", ",");
                StringBuffer buf = new StringBuffer("0,");
                buf.append(NFVUtil.num_dc+",");
                buf.append(this.env.getGlobal_hostMap().size()+",");
                buf.append(this.env.getGlobal_vmMap().size() + ",");
                buf.append(this.env.getGlobal_vcpuMap().size() + ",");
                buf.append(NFVUtil.host_mips_max + ","+NFVUtil.host_mips_min + ",");
                buf.append(NFVUtil.host_bw_max + ","+NFVUtil.host_bw_min+ ",");
                buf.append(newAutoID + ",");
                buf.append(info.getCCR()+",");
                totalCCR += info.getCCR();

                buf.append(info.getSfNum() + "," + info.getSfInsSet().size() + ",");
                this.resultInfo.setSfNum(this.resultInfo.getSfNum() + info.getSfNum());
                this.resultInfo.getSfInsSet().addAll(info.getSfInsSet());
                totalSFInsNum += info.getSfInsSet().size();

                buf.append(info.getMakeSpan()+ ",");
                totalMakeSpan += info.getMakeSpan();

                buf.append(info.getVmSet().size() + ",");
                buf.append(info.getHostSet().size() + ",");
                this.resultInfo.getVmSet().addAll(info.getVmSet());
                this.resultInfo.getHostSet().addAll(info.getHostSet());

                buf.append(info.getvCPUSet().size() + ","+info.getCacheHitNum());
                this.resultInfo.getvCPUSet().addAll(info.getvCPUSet());
                totalHitNum += info.getCacheHitNum();
                pw.println(buf.toString());


            }
            this.resultInfo.setCCR(Calc.getRoundedValue(totalCCR/(double)this.infoMap.size()));
            this.resultInfo.setMakeSpan(((long)totalMakeSpan )/this.infoMap.size());
            this.resultInfo.setCacheHitNum(totalHitNum);
            StringBuffer buf = new StringBuffer("1,"+NFVUtil.num_dc);
            buf.append(",");
            buf.append(this.env.getGlobal_hostMap().size()+",");
            buf.append(this.env.getGlobal_vmMap().size() + ",");
            buf.append(this.env.getGlobal_vcpuMap().size() + ",");
            buf.append(NFVUtil.host_mips_max + ","+NFVUtil.host_mips_min + ",");
            buf.append(NFVUtil.host_bw_max + ","+NFVUtil.host_bw_min+ ",");

            buf.append(this.resultInfo.getAplJobID() + ",result,");
            buf.append(this.resultInfo.getCCR()+",");
            buf.append(this.resultInfo.getSfNum() + "," + totalSFInsNum + ",");

            buf.append(this.resultInfo.getMakeSpan()+ ",");
            buf.append(this.resultInfo.getHostSet().size() + ",");

            buf.append(this.resultInfo.getVmSet().size() + ",");

            buf.append(this.resultInfo.getvCPUSet().size() + ","+this.resultInfo.getCacheHitNum());
            pw.println(buf.toString());
            pw.close();
            bw.close();
            fw.close();
        }catch(Exception e){
            e.printStackTrace();
        }


    }

    /**
     * Prefix生成する処理になります．
     * /JobID/fromTaskのID/toTaskのID/fromTaskのCmd
     *
     * @param
     * @return
     */
    public String  createPrefix(VNF  fromTask, VNF toTask) {
        long toID = -1;
        if (toTask != null) {
            toID = toTask.getIDVector().get(1);
        }
        Long jobID = fromTask.getIDVector().get(0);
        String val = "/"+fromTask.getIDVector().get(0) + "/" + fromTask.getIDVector().get(1) + "/" + toID + "/";
        val = val.replaceAll(" ", "");
        return val;
    }

    public String  createEndPrefix(VNF endTask) {
        String toID = "-1";

        Long jobID = endTask.getIDVector().get(0);
        String val = "/"+endTask.getIDVector().get(0) + "/" + endTask.getIDVector().get(1) + "/" + toID + "/";
        val = val.replaceAll(" ", "");
        return val;
    }



    public double calcExecTime(long w, VCPU vcpu){
        return CloudUtil.getRoundedValue((double) w / (double) vcpu.getMips());


    }


    public double calcComTime(long dataSize, VCPU fromVCPU, VCPU toVCPU) {
        //DCの情報．
        Long fromDCID = CloudUtil.getInstance().getDCID(fromVCPU.getPrefix());
        Long toDCID = CloudUtil.getInstance().getDCID(toVCPU.getPrefix());
        long dcBW = NFVUtil.MAXValue;
        Cloud fromCloud = env.getDcMap().get(fromDCID);
        Cloud toCloud = env.getDcMap().get(toDCID);

        //同一クラウド内であれば，DC間の通信は考慮しなくて良い．
        if (fromDCID.longValue() == toDCID.longValue()) {
        } else {
            //DCが異なれば，DC間の通信も考慮スべき．
            dcBW = Math.min(fromCloud.getBw(), toCloud.getBw());

        }
        Long fromHostID = CloudUtil.getInstance().getHostID(fromVCPU.getPrefix());
        Long toHostID = CloudUtil.getInstance().getHostID(toVCPU.getPrefix());

        //後は，ホスト間での通信
        ComputeHost fromHost = fromCloud.getComputeHostMap().get(fromHostID);
        ComputeHost toHost = toCloud.getComputeHostMap().get(toHostID);
        long hostBW = NFVUtil.MAXValue;
        if (fromHost.getDcID() == toHost.getMachineID()) {
            //同一ホストなら，0を返す．
            return 0;
        } else {
            hostBW = Math.min(fromHost.getBw(), toHost.getBw());
        }

        long realBW = Math.min(dcBW, hostBW);

        double time = CloudUtil.getRoundedValue((double) dataSize / (double) realBW);

        return time;

    }

    public long getJobID(String prefix){
        StringTokenizer st = new StringTokenizer(prefix, "/");
        long val = -1;
        String str = null;
        for(int i=0;i<1;i++){
            str = st.nextToken();

        }

        return Long.valueOf(str).longValue();
    }


    public long getPredVNFID(String prefix){
        StringTokenizer st = new StringTokenizer(prefix, "/");
        long val = -1;
        String str = null;
        for(int i=0;i<2;i++){
            str = st.nextToken();

        }

        return Long.valueOf(str).longValue();
    }

    public long getSucVNFID(String prefix){
        StringTokenizer st = new StringTokenizer(prefix, "/");
        long val = -1;
        String str = null;
        for(int i=0;i<3;i++){
            str = st.nextToken();

        }

        return Long.valueOf(str).longValue();
    }

    public long getSFCNum() {
        return SFCNum;
    }

    public void setSFCNum(long SFCNum) {
        this.SFCNum = SFCNum;
    }

    public LinkedBlockingDeque<SFC> getSfcQueue() {
        return sfcQueue;
    }

    public void setSfcQueue(LinkedBlockingDeque<SFC> sfcQueue) {
        this.sfcQueue = sfcQueue;
    }

    public AutoInfo getResultInfo() {
        return resultInfo;
    }

    public void setResultInfo(AutoInfo resultInfo) {
        this.resultInfo = resultInfo;
    }
}
