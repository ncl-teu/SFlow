package org.ncl.workflow.ccn.autoicnsfc;

import com.intel.jnfd.deamon.face.tcp.TcpFace;
import com.intel.jnfd.deamon.table.fib.FibEntry;
import com.intel.jnfd.deamon.table.fib.FibNextHop;
import net.gripps.cloud.CloudUtil;
import net.gripps.cloud.core.*;
import net.gripps.cloud.nfv.NFVEnvironment;
import net.gripps.cloud.nfv.NFVUtil;
import net.gripps.cloud.nfv.sfc.BaseVNFSchedulingAlgorithm;
import net.gripps.cloud.nfv.sfc.SFC;
import net.gripps.cloud.nfv.sfc.VNF;
import net.gripps.clustering.common.aplmodel.CustomIDSet;
import net.gripps.clustering.common.aplmodel.DataDependence;
import net.gripps.clustering.tool.Calc;
import net.named_data.jndn.Name;
import org.ncl.workflow.ccn.core.NclwNFDMgr;
import org.ncl.workflow.ccn.sfc.process.NFDTask;
import org.ncl.workflow.ccn.util.ResourceMgr;
import org.ncl.workflow.comm.NCLWData;
import org.ncl.workflow.comm.WorkflowJob;
import org.ncl.workflow.util.NCLWUtil;

import java.util.Iterator;
import java.util.PriorityQueue;

/**
 * Created by Hidehiro Kanemitsu on 2020/02/26
 */
public class AutoICNSFCScheduling extends BaseVNFSchedulingAlgorithm {


    NFVEnvironment env;
    /**
     * ここに渡す前にenvは設定済みである必要がある．
     * @param env
     * @param sfc
     */
    public AutoICNSFCScheduling(NFVEnvironment env, SFC sfc) {
        super(env);
        this.env = (NFVEnvironment)super.env;
        this.sfc = sfc;

    }


    public AutoICNSFCScheduling(NFVEnvironment env) {
        super(env);
        this.env = (NFVEnvironment)super.env;
        long[] arrBW = {this.aveBW, this.maxBW, this.minBW};
        this.setSpeedBW();
        long[] arrSpeed = {this.aveSpeed, this.maxSpeed, this.minSpeed};
        //レベル計算につかう処理速度，BWを決める．
        this.usedSpeed = 1000;
        this.usedBW = 125;

    }

    /**
     * @param vnf
     * @param set
     * @return
     */
    public double calcBlevelWST(VNF vnf, CustomIDSet set) {
        //先行VNFたちを取得する．
        Iterator<DataDependence> dsucIte = vnf.getDsucList().iterator();
        //END VNFであれば，blevelは処理時間
        if (vnf.getDsucList().isEmpty()) {
            double value = this.calcExecTime(vnf.getWorkLoad(), 1);
            vnf.setBlevel(value);
            set.add(vnf.getIDVector().get(1));
            return value;
        }

        double maxValue = -1;

        //以降はENDではない場合の処理
        while (dsucIte.hasNext()) {
            DataDependence dsuc = dsucIte.next();
            Long toID = dsuc.getToID().get(1);
            VNF toVNF = this.sfc.findVNFByLastID(toID);
            double tmpValue = 0.0d;
            long dataSize = dsuc.getMaxDataSize();
            if (set.contains(toID)) {
                tmpValue = this.calcExecTime(vnf.getWorkLoad(), 1) + this.calcComTime(dataSize, 1) + toVNF.getBlevelWST();
            } else {
                tmpValue = this.calcExecTime(vnf.getWorkLoad(), 1) + this.calcComTime(dataSize, 1) + this.calcBlevelWST(toVNF, set);
            }
            if (maxValue <= tmpValue) {
                maxValue = tmpValue;
                vnf.setDominantWSTSucID(toID);
                vnf.setBlevelWST(maxValue);
            }
        }

        return maxValue;
    }





    /**
     *  SFCに対して，仮想辺を追加する処理です．
     * @return
     */
    public SFC constructAutoSFC(){
        //まずは，処理速度とBWをそれぞれ1としてblevelWSTを計算する．
        //STARTVNF集合から，blevel_wstを求める．
        CustomIDSet startSet = sfc.getStartVNFSet();
        Iterator<Long> startIte0 = startSet.iterator();
        while (startIte0.hasNext()) {
            Long startID = startIte0.next();
            //Free集合へ追加しておく．
            //this.freeVNFSet.add(startID);
            VNF startVNF = this.sfc.findVNFByLastID(startID);
            this.calcBlevelWST(startVNF, new CustomIDSet());
        }

        Iterator<Long> startIte = this.sfc.getStartVNFSet().iterator();
        CustomIDSet set = new CustomIDSet();

        while(startIte.hasNext()){
            Long startID = startIte.next();
            VNF startVNF = this.sfc.findVNFByLastID(startID);
           // this.addVDependence(startVNF, set);

        }
        return this.sfc;
    }

    /**
     * 指定VNFの後続SFを見て，最大のBlevel+
     * @param vnf
     * @param set
     */
    public void addVDependence(VNF vnf, CustomIDSet set){
        Iterator<DataDependence> dsucIte = vnf.getDsucList().iterator();
        Iterator<VDependence> vdsucIte = vnf.getVDsucList().iterator();

        //すでにチェック済みであれば，終了させる．
        if(set.contains(vnf.getIDVector().get(1))){
            return;
        }
        //END VNFであれば終了
        if(vnf.getDsucList().isEmpty()){
            return;
        }
        //当該VNFをチェック済みとしてマークする．
        set.add(vnf.getIDVector().get(1));
        double maxValue = 0;
        VNF retVNF = null;
        while(dsucIte.hasNext()){
            DataDependence dsuc = dsucIte.next();
            VNF sucVNF = this.sfc.findVNFByLastID(dsuc.getToID().get(1));
            double totalLevel = this.calcComTime(dsuc.getMaxDataSize(), this.usedBW) + sucVNF.getBlevel();
            if(maxValue <= totalLevel){
                maxValue = totalLevel;
                retVNF = sucVNF;
            }
        }
        while(vdsucIte.hasNext()){
            VDependence vd = vdsucIte.next();
            VNF sucVNF = this.sfc.findVNFByLastID(vd.getToID());
            double totalLevel = sucVNF.getBlevel();
            if(maxValue <= totalLevel){
                maxValue = totalLevel;
                retVNF = sucVNF;
            }
        }
        //retVNFに対して，他の後続タスクからの辺を追加する．
        Iterator<DataDependence> dsucIte2 = vnf.getDsucList().iterator();
        while(dsucIte2.hasNext()){
            DataDependence dsuc = dsucIte2.next();
            VNF sucVNF = this.sfc.findVNFByLastID(dsuc.getToID().get(1));
            if(sucVNF.getIDVector().get(1).equals(retVNF.getIDVector().get(1))){
                continue;

            }else{
                //sucVNF->retVNFに対するVEdgeを追加．
                VDependence vd = new VDependence(sucVNF.getIDVector().get(1), retVNF.getIDVector().get(1), vnf.getIDVector().get(1));
                sucVNF.getVDsucList().add(vd);
                retVNF.getVDpredList().add(vd);
            }
        }

    }

    /**
     *
     * @param queue オリジナルVCPU内のキュー
     * @param vnf やってきたData内にあったVNF
     * @return
     */
    public boolean containsVNF(PriorityQueue<VNF> queue, VNF vnf){
        Iterator<VNF> vIte = queue.iterator();
        boolean ret = false;
        while(vIte.hasNext()){
            VNF srcVnf = vIte.next();
            if((srcVnf.getIDVector().get(0).equals(vnf.getIDVector().get(0)))&&
                    (srcVnf.getIDVector().get(1).equals(vnf.getIDVector().get(1)))){
                ret = true;
                break;
            }
        }
        return ret;
    }

    /**
     * 自身のEnvに対して，addEnvをマージする処理です．
     * その後，FIBのエントリも更新する必要がある．IP単位でのエントリなので，
     * VM単位での追加の場合に，FIBを追加する必要がある．
     * 具体的には，
     * @param addEnv
     * @return
     */
    public NFVEnvironment mergeEnv(NFVEnvironment addEnv){
        Iterator<VCPU> vIte = addEnv.getGlobal_vcpuMap().values().iterator();
        while(vIte.hasNext()){
            VCPU vcpu = vIte.next();
            if(this.env.getGlobal_vcpuMap().containsKey(vcpu.getPrefix())){
                VCPU orgVCPU = this.env.getGlobal_vcpuMap().get(vcpu.getPrefix());
                Iterator<VNF> addVIte = vcpu.getVnfQueue().iterator();
                PriorityQueue<VNF> queue = orgVCPU.getVnfQueue();
                while(addVIte.hasNext()){
                    VNF addVNF = addVIte.next();
                    if(this.containsVNF(queue, addVNF)){
                        //すでにあるVNFは，何もしない．
                        VNF orgVNF = this.sfc.findVNFByLastID(addVNF.getIDVector().get(1));
                        //VNFの割当先の更新
                        if(orgVNF.getvCPUID() == null){
                            if(addVNF.getvCPUID() != null){
                                orgVNF.setvCPUID(addVNF.getvCPUID());
                            }

                        }

                    }else{
                        //System.out.println("^^^^^^^^^^^^^^ADDED:"+addVNF.getIDVector().get(1));
                        queue.add(addVNF);
                    }


                }
            }else{
                //vcpuがなければ，新規にputする．
                this.env.getGlobal_vcpuMap().put(vcpu.getPrefix(), vcpu);
            }

        }


        Iterator<Cloud> cIte = addEnv.getDcMap().values().iterator();
        while(cIte.hasNext()){
            Cloud cloud = cIte.next();
            //もしあれば，何もしない．vCPU単位での割り当てなので，vCPU以外は何もしない．
            if(this.env.getDcMap().containsKey(cloud.getId())){

            }else{
                this.env.getDcMap().put(cloud.getId(), cloud);
            }
        }

        Iterator<ComputeHost> hostIte = addEnv.getGlobal_hostMap().values().iterator();
        while (hostIte.hasNext()) {
            ComputeHost host = hostIte.next();
            if(this.env.getGlobal_hostMap().containsKey(host.getPrefix())){

            }else{
                this.env.getGlobal_hostMap().put(host.getPrefix(), host);
            }
        }

        Iterator<CloudCPU> cpuIte = addEnv.getGlobal_cpuMap().values().iterator();
        while(cpuIte.hasNext()){
            CloudCPU cpu = cpuIte.next();
            if(this.env.getGlobal_cpuMap().containsKey(cpu.getPrefix())){

            }else{
                this.env.getGlobal_cpuMap().put(cpu.getPrefix(), cpu);
            }
        }

        Iterator<VM> vmIte = addEnv.getGlobal_vmMap().values().iterator();
        while(vmIte.hasNext()){
            VM vm = vmIte.next();
            if(this.env.getGlobal_vmMap().containsKey(vm.getVMID())){

            }else{
                this.env.getGlobal_vmMap().put(vm.getVMID(), vm);
                TcpFace toFace = NclwNFDMgr.getIns().createFace(vm.getIpAddr(), ResourceMgr.getIns().getOwnIPAddr());
                if(this.existFibEntryAtPrefix(new Name(NCLWUtil.NCLW_PREFIX), vm)){
                    //もし存在すれば何もしない．

                }else{
                    //FIBにも追加
                    //そして，FIBへ追加する．
                    NclwNFDMgr.getIns().getFib().insert(new Name(NCLWUtil.NCLW_PREFIX), toFace, 1);
                }



            }
        }

        return (NFVEnvironment)this.env;
    }

    public boolean existFibEntryAtPrefix(Name prefix, VM vm){
        boolean ret = false;
        FibEntry entry = NclwNFDMgr.getIns().getFib().findExactMatch(prefix);
        if(entry == null){
            return false;
        }
        Iterator<FibNextHop> hIte = entry.getNextHopList().iterator();
        while(hIte.hasNext()){
            FibNextHop hop = hIte.next();
            String ip = hop.getFace().getRemoteUri().getInet().getHostAddress();
            if(ip.equals(vm.getIpAddr())){
                ret = true;
                break;
            }
        }
        return ret;
    }


    public void updateOutSFSet(String vcpuPrefix){
        VCPU vcpu = this.env.getGlobal_vcpuMap().get(vcpuPrefix);
        Iterator<VNF> vIte = vcpu.getVnfQueue().iterator();
        while(vIte.hasNext()){
            VNF vnf = vIte.next();
            Iterator<DataDependence> dsucIte = vnf.getDsucList().iterator();
            boolean isOut = false;
            while(dsucIte.hasNext()){
                DataDependence dsuc = dsucIte.next();
                VNF sucSF = this.sfc.findVNFByLastID(dsuc.getToID().get(1));
                if(sucSF != null){
                    if(sucSF.getvCPUID() == null){
                        isOut = true;
                        break;
                    }
                    if(sucSF.getvCPUID().equals(vcpuPrefix)){

                    }else{
                        isOut = true;
                        break;
                    }
                }else{

                }

            }
            vcpu.getOutSFSet().remove(vnf.getIDVector().get(1));

            if(isOut){
                vcpu.getOutSFSet().add(vnf.getIDVector().get(1));
            }else{
            }
        }


    }

    /**
     * FIB内エントリ
     */
    public void updateUsedValues(FibEntry entry){
        Iterator<FibNextHop> fIte = entry.getNextHopList().iterator();
        int entryNum = 0;
        int vcpuNum = 0;
        long totalBW = 0;
        long totalMips = 0;

        while(fIte.hasNext()){
            FibNextHop hop = fIte.next();
            TcpFace face = (TcpFace)hop.getFace();
            if(face == null){
                continue;
            }

            //faceのIPから，VMを取得する
            VM vm = NCLWUtil.getIns().findVMbyIP((NFVEnvironment)this.env, face.getRemoteUri().getInet().getHostAddress());
            if(vm == null){
                continue;
            }
            entryNum++;
            ComputeHost host = this.env.getGlobal_hostMap().get(vm.getHostID());
            totalBW += host.getBw();

            //VMのvCPUに対するループ
            Iterator<VCPU> vcIte = vm.getvCPUMap().values().iterator();
            while(vcIte.hasNext()){
                vcpuNum++;
                VCPU vcpu = vcIte.next();
                totalMips += vcpu.getMips();
            }

        }
        this.usedBW = totalBW / entryNum;
        this.usedSpeed = totalMips / vcpuNum;


    }



    /**
     * 指定されたNCLWDataから，先行SFの割当先となるVCPUを決める．
     * このとき，FIBエントリの中から決めることになる．
     *
     * @param data
     * @return
     */
    public VCPU findTargetVCPU( NCLWData data, Name prefix){
        String ownIP = ResourceMgr.getIns().getOwnIPAddr();
    //    SFC sfc = data.getSfc();
        //FromTask, つまり当該先行タスクを取得する．
        //AutoICNSFCScheduling sched = AutoICNSFCMgr.getIns().getSched();
        if(this.getSfc() == null){
            this.setSfc(data.getSfc());
        }


        //ToTask，つまりInterestの送信元タスクを取得する．
        //NFVEnvironment srcEnv = data.getEnv();
        NFVEnvironment srcEnv = data.getEnv();


        //envを受け継ぐようにするか，どうするか．
        //Envの更新をする．
        //これに伴ってFIBも更新されるので，また再度，FIBからエントリを探し直す．
        //環境及びSFCのマージを行う．
        if(this.env == srcEnv){

        }else{
            NFVEnvironment env = this.mergeEnv(srcEnv);

        }
        SFC sfc = this.getSfc();
        VNF predSF = sfc.findVNFByLastID(data.getFromTaskID());
        if(predSF.getvCPUID() != null){
            return this.env.getGlobal_vcpuMap().get(predSF.getvCPUID());
        }


        VM ownVM = NCLWUtil.getIns().findVMbyIP((NFVEnvironment)this.env, ownIP);

        //まずは自身のvCPUにおける最小のblevelWSTを計算する．
        //つまり，predSFがこのVCPUが属するVMに割り当てられた場合．
        Iterator<VCPU> vIte = ownVM.getvCPUMap().values().iterator();
        VCPU localVCPU = null;
        double localMinWST = Long.MAX_VALUE;
        //System.out.println("ownVCPU NUM:"+ownVM.getvCPUMap().size());
        System.out.println("Pred:"+predSF.getIDVector().get(1) + "/To:"+data.getToTaskID());
        while(vIte.hasNext()){
            VCPU vcpu =  vIte.next();
            double blevelWST  = Long.MAX_VALUE;
            //END->nullの場合は，ENDの処理時間のみを考える．
            if(data.getToTaskID() == -1){
                blevelWST = this.calcExecTime(predSF.getWorkLoad(), vcpu);
            }else{
                //System.out.println("Sched:352");
                blevelWST = this.calcBlevelWST(predSF, vcpu);

            }
            if(blevelWST < localMinWST){
                localMinWST = blevelWST;
                localVCPU = vcpu;
            }
        }
        double fibMinWST = Long.MAX_VALUE;

        //次にFIBから選択する．
        FibEntry entry = NclwNFDMgr.getIns().getFib().findLongestPrefixMatch(prefix);
        //usedSpeed, usedMipsを更新する．
        this.updateUsedValues(entry);
        Iterator<FibNextHop> fnIte = entry.getNextHopList().iterator();

        //System.out.println("Entry NUM:"+entry.getNextHopList().size());
        //System.out.println("***Scheduling: LocalMinWST_1st:"+localMinWST);
//*************これ以降のどこかでおかしい
        //Fibのface単位に対するループ
        while(fnIte.hasNext()){
            FibNextHop hop = fnIte.next();
            TcpFace face = (TcpFace)hop.getFace();
            if(face == null){
                continue;
            }else{
            }

            //faceのIPから，VMを取得する
            VM vm = NCLWUtil.getIns().findVMbyIP((NFVEnvironment)env, face.getRemoteUri().getInet().getHostAddress());

            //System.out.print("**Host:"+face.getRemoteUri().getInet().getHostAddress());
            //System.out.println("**Scheduling381 IP: "+vm.getIpAddr());
            if(vm == null){

                continue;
            }
            //VMのvCPUに対するループ
            Iterator<VCPU> vcIte = vm.getvCPUMap().values().iterator();
            if(!vm.getIpAddr().equals(ownIP)){
                while(vcIte.hasNext()){
                    VCPU vcpu = vcIte.next();
                    //当該vcpuでのblevelWSTを計算する．
                    double tmpBlevelWST = Long.MAX_VALUE;
                    if(data.getToTaskID() == -1){
                        tmpBlevelWST = this.calcExecTime(predSF.getWorkLoad(), vcpu);
                    }else{
                        tmpBlevelWST = this.calcBlevelWST(predSF, vcpu);

                    }
                    if(tmpBlevelWST < localMinWST){
                        localMinWST = tmpBlevelWST;
                        localVCPU = vcpu;
                    }
                }
            }

        }
        if(localVCPU == null){
        }else{
            //キューにSFを入れておく．
           // predSF.setvCPUID(localVCPU.getPrefix());
           //localVCPU.getVnfQueue().add(predSF);
//System.out.println("***localVCPU SF#"+localVCPU.getVnfQueue().size());

        }
        //最後に，localVCPUにSFを入れておく．
        //localVCPU.getVnfQueue().add(predSF);

        return localVCPU;
    }

    public long  calcTotalWorkload(VCPU vcpu){
        long totalW = 0;
        Iterator<VNF> vIte = vcpu.getVnfQueue().iterator();
        while(vIte.hasNext()){
            VNF vnf = vIte.next();
            totalW += vnf.getWorkLoad();
        }

        return totalW;
    }

    /**
     * vnfのblevelWSTの計算に必要な情報
     * vcpuにおける，これから割り当てようとするvnfの先祖SF集合を取得する．
     * そして，
     *
     * @param vnf
     * @param vcpu vnfが割り当てられたとされるVCPU
     * @return
     */
    public double calcBlevelWST(VNF vnf, VCPU vcpu){
       // System.out.println("***ClacBlevelWST@VNF"+vnf.getIDVector().get(1)+"/VCPU:"+vcpu.getPrefix()+"START****");
        //まずはvcpu内のSFのトータル仕事量を求める．
        long currentTotalW = this.calcTotalWorkload(vcpu);
        //System.out.println("VNF#@vcpu"+vcpu.getPrefix()+":"+vcpu.getVnfQueue().size());
        //さらに，今回のvnfの仕事量を加算する．
        currentTotalW += vnf.getWorkLoad();
        //まずはvcpuにおけるvnfの先祖SF集合を取得する．
        CustomIDSet ansSet = this.getAncestorsInVNF(new CustomIDSet(), vnf, vcpu);

        Iterator<Long> aIte = ansSet.iterator();
        long ansTotalW = 0;
        while(aIte.hasNext()){
            Long id = aIte.next();
           // System.out.println("***Ans:"+id);

            VNF predVNF = this.sfc.findVNFByLastID(id);
            ansTotalW += predVNF.getWorkLoad();
        }

        long slackW = currentTotalW - ansTotalW;
        //次に，out SFにおいてblevelWSTの最大値を取得する．
        //まずは，当該vcpuにおけるoutのblevelの最大値を取得する．
        Iterator<Long> outIte = vcpu.getOutSFSet().iterator();
        double retBlevelWST = -1;

        while(outIte.hasNext()){
            VNF outVNF = this.sfc.findVNFByLastID(outIte.next());

            double blevelWST = outVNF.getBlevelWST();
            if(retBlevelWST <= blevelWST){
                retBlevelWST = blevelWST;
            }
        }
        double sameBlevelWST = this.calcExecTime(slackW, vcpu)+ retBlevelWST;

        //次に，当該vnfがoutであった場合のblevelの最大値を取得する．
        Iterator<DataDependence> dsucIte = vnf.getDsucList().iterator();
        double difBlevwlWST = -1;
        while(dsucIte.hasNext()){

            DataDependence dsuc = dsucIte.next();
            VNF sucSF = this.sfc.findVNFByLastID(dsuc.getToID().get(1));
            double tmpBlevelWST = -1;
            if(sucSF.getvCPUID() == null){
                VM vm = this.env.getGlobal_vmMap().get(vcpu.getVMID());
                 ComputeHost host = this.env.getGlobal_hostMap().get(vm.getHostID());

                tmpBlevelWST = this.calcExecTime(vnf.getWorkLoad(), vcpu) + this.calcComTime(dsuc.getMaxDataSize(),
                         Math.min(host.getBw(), this.usedBW)) + sucSF.getBlevelWST();

            }else{
                if(!sucSF.getvCPUID().equals(vcpu.getPrefix())){
                    VCPU sucVCPU = this.env.getGlobal_vcpuMap().get(sucSF.getvCPUID());
                    //このときに限ってblevelWSTを計算する．
                     tmpBlevelWST = this.calcExecTime(vnf.getWorkLoad(), vcpu) + this.calcComTime(dsuc.getMaxDataSize(), vcpu, sucVCPU)
                             +sucSF.getBlevel();
                }
            }

            if(difBlevwlWST <= tmpBlevelWST){
                difBlevwlWST = tmpBlevelWST;
            }

        }
       // System.out.println("***ClacBlevelWST@VNF"+vnf.getIDVector().get(1)+"/VCPU:"+vcpu.getPrefix()+"END****");

        //よって，vnfのblevelWSTはいかに求められる．
        return Math.max(sameBlevelWST, difBlevwlWST);
    }



    public CustomIDSet getAncestorsInVNF(CustomIDSet set, VNF vnf, VCPU vcpu) {
        Iterator<DataDependence> dpredIte = vnf.getDpredList().iterator();


        while (dpredIte.hasNext()) {
            DataDependence dpred = dpredIte.next();
            VNF predVNF = this.sfc.findVNFByLastID(dpred.getFromID().get(1));
            Long predID = predVNF.getIDVector().get(1);
            if (set.contains(predID)) {
                continue;
            } else {
                if ((predVNF.getvCPUID()!=null)&&(predVNF.getvCPUID()== vcpu.getPrefix())) {
                    set.add(predVNF.getIDVector().get(1));
                    set = this.getAncestorsInVNF(set, predVNF, vcpu);
                } else {
                    continue;
                }
            }

        }
        return set;
    }


    /**
     * 指定SFの割当先を決める．
     * 保持しているenvの中から選ぶ．
     * 時間スロットまでは関知しない．時間スロットについては，
     * Interestパケットが届いてから決める．もし当該タスクが
     * 時間スロットの余裕がない場合は，他ノードを探す，という戦略にする．
     * sucSF -> predSFへinterestを投げるので，
     * sucSFは割り当て済みで，predSFの割当先を決める問題．
     * @param
     */
    public void scheduleSF(VNF predSF, VNF sucSF){
        //要は，blevel_sched(predSF)が小さくなれば良い．

    }

    /**
     * END SFの割当先を決める．基本的には，blevelWST(v_end)を最小にするようなvCPUを選択する．
     * もしblevelWST(v_end)の最小値となるvCPUが複数あれば，当該vCPUが属するbwが最大のものを選択する．
     * それでも同じものがあれば（例えば同一ホストとか），ランダムに選ぶ．
     * @param sf
     */
    public void scheduleEndSF(VNF sf, WorkflowJob job){

        NFDTask endTask = job.getNfdTaskMap().get(sf.getIDVector().get(1));
        Name endPrefix = NclwNFDMgr.getIns().createPrefix(endTask, null);
        //まず，FIBからエントリを取得する．NFDTaskを生成しなければならない．
        //FIBから選ぶか？それともvCPUマップから選ぶか？どちらにしても，双方で同期が必要となる．
        //指定のprefixから選ぶ，という意味では，まずはFIBから取得する必要がある．
        //それから，vCPUマップから選ぶスタイル．
        try{
            Thread.sleep(3000);

        }catch(Exception e){
            e.printStackTrace();
        }
       FibEntry fEntry = NclwNFDMgr.getIns().getFib().findLongestPrefixMatch(endPrefix);
       synchronized (fEntry){
           Iterator<FibNextHop> fnhIte = fEntry.getNextHopList().iterator();
           double minBlevelWST = Long.MAX_VALUE;
           VCPU retVCPU = null;
           while(fnhIte.hasNext()){
               FibNextHop hop = fnhIte.next();
               VM vm = NCLWUtil.getIns().findVMbyIP((NFVEnvironment)this.env, hop.getFace().getRemoteUri().getInet().getHostAddress());
               //vm内のvCPUリストを取得する．
               Iterator<VCPU> vIte = vm.getvCPUMap().values().iterator();
               while(vIte.hasNext()){
                   VCPU vcpu = vIte.next();
                   //単に処理時間のみを計算．
                   double endBlevelWST = this.calcExecTime(sf.getWorkLoad(), vcpu);
                   if(endBlevelWST < minBlevelWST){
                       minBlevelWST = endBlevelWST;
                       retVCPU = vcpu;
                   }else if(endBlevelWST == minBlevelWST){
                       //BWを見る．
                       if(retVCPU != null){
                           ComputeHost host = env.getGlobal_hostMap().get(vm.getHostID());
                           VM retVM = env.getGlobal_vmMap().get(retVCPU.getVMID());
                           ComputeHost retHost = env.getGlobal_hostMap().get(retVM.getHostID());

                           if(host.getBw() > retHost.getBw()){
                               retVCPU = vcpu;
                           }
                       }
                   }
               }
               sf.setvCPUID(retVCPU.getPrefix());
           }
       }




    }

    @Override
    public NFVEnvironment getEnv() {
        return this.env;
    }

    /*if(sf.getDsucList().isEmpty()){
            //当該SFの
            VNF dominantPredSF = this.sfc.findVNFByLastID(sf.getDominantPredID());
            //tlevelを支配するところの通信時間 + SFの処理時間を最小にするノードを選択する．
            DataDependence dpred = sf.findDDFromDpredList(dominantPredSF.getIDVector(), sf.getIDVector());
            Iterator<VCPU> vcpuIte = this.env.getGlobal_vcpuMap().values().iterator();
            double minValue = 9999999999.9d;
            VCPU retVCPU = null;
            while(vcpuIte.hasNext()){
                VCPU vcpu = vcpuIte.next();
                VM vm = NCLWUtil.findVM((NFVEnvironment)this.env, vcpu.getPrefix());
                ComputeHost host = this.env.getGlobal_hostMap().get(vm.getHostID());
                double val = this.calcComTime(dpred.getMaxDataSize(), host.getBw()) + this.calcExecTime(sf.getWorkLoad(), vcpu.getMips());
                if(val <= minValue){
                    minValue = val;
                    retVCPU = vcpu;
                }

            }
            //END sfの割当先が決定した．
            sf.setvCPUID(retVCPU.getPrefix());


        }else{
            return;
        }
    }

         */

}
