package net.gripps.ccn.icnsfc.routing;

import com.intel.jnfd.deamon.face.tcp.TcpFace;
import com.intel.jnfd.deamon.table.fib.FibEntry;
import com.intel.jnfd.deamon.table.fib.FibNextHop;
import net.gripps.ccn.CCNUtil;
import net.gripps.ccn.core.*;
import net.gripps.ccn.fibrouting.LongestMatchRouting;
import net.gripps.ccn.icnsfc.AutoUtil;
import net.gripps.ccn.icnsfc.core.AutoEnvironment;
import net.gripps.ccn.icnsfc.process.AutoSFCMgr;
import net.gripps.ccn.process.CCNMgr;
import net.gripps.cloud.CloudUtil;
import net.gripps.cloud.core.Cloud;
import net.gripps.cloud.core.ComputeHost;
import net.gripps.cloud.core.VCPU;
import net.gripps.cloud.core.VM;
import net.gripps.cloud.nfv.NFVEnvironment;
import net.gripps.cloud.nfv.NFVUtil;
import net.gripps.cloud.nfv.sfc.SFC;
import net.gripps.cloud.nfv.sfc.VNF;
import net.gripps.clustering.common.aplmodel.CustomIDSet;
import net.gripps.clustering.common.aplmodel.DataDependence;
import net.gripps.clustering.tool.Calc;

import org.ncl.workflow.ccn.core.NclwNFDMgr;

import org.ncl.workflow.util.NCLWUtil;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;

/**
 * Created by Hidehiro Kanemitsu on 2020/09/17
 * AutoICNSchedulingのアルゴリズム実装です．
 *
 */
public class AutoRouting extends LongestMatchRouting {

    protected AutoEnvironment env;

    protected long  usedBW;

    protected long  usedSpeed;


    public AutoRouting() {
        this.env = AutoSFCMgr.getIns().getEnv();
    }

    public AutoRouting(HashMap<Long, CCNNode> nodeMap, HashMap<Long, CCNRouter> routerMap) {
        super(nodeMap, routerMap);
        this.env = AutoSFCMgr.getIns().getEnv();

    }

    @Override
    /**
     * CCNNodeにおいて，Interstパケットの転送先を決める
     * 要実装．
     * @param rMap: 検索対象のルータ集合
     * @param packet: これから送信するInterestパケット．この中に，SFCが入っている．
     *
     *
     */
    public CCNRouter selectRouter(HashMap<Long, CCNRouter> rMap, InterestPacket packet) {
        AutoEnvironment env = AutoSFCMgr.getIns().getEnv();
        SFC sfc = (SFC)packet.getAppParams().get(AutoUtil.SFC_NAME);


        VNF endTask = sfc.findVNFByLastID((long)sfc.getVnfMap().size());
        //Name endPrefix = NclwNFDMgr.getIns().createPrefix(endTask, null);
        //まず，FIBからエントリを取得する．NFDTaskを生成しなければならない．
        //FIBから選ぶか？それともvCPUマップから選ぶか？どちらにしても，双方で同期が必要となる．
        //指定のprefixから選ぶ，という意味では，まずはFIBから取得する必要がある．
        //それから，vCPUマップから選ぶスタイル．

        Iterator<CCNRouter> rIte = rMap.values().iterator();
           // Iterator<FibNextHop> fnhIte = fEntry.getNextHopList().iterator();
            double minBlevelWST = Long.MAX_VALUE;
            VCPU retVCPU = null;
            CCNRouter retRouter = null;
            while(rIte.hasNext()){
                CCNRouter r = rIte.next();
                //ルータのvcpuを取得する．
                Iterator<VCPU> vIte = r.getvCPUMap().values().iterator();
                while(vIte.hasNext()){
                    VCPU vcpu = vIte.next();
                    //単に処理時間のみを計算．
                    double endBlevelWST = AutoSFCMgr.getIns().calcExecTime(endTask.getWorkLoad(), vcpu);

                    if(endBlevelWST < minBlevelWST){
                        minBlevelWST = endBlevelWST;
                        retVCPU = vcpu;
                        retRouter = r;
                    }else if(endBlevelWST == minBlevelWST){
                        //BWを見る．
                        if(retVCPU != null){
                            ComputeHost host = env.getGlobal_hostMap().get(r.getHostID());
                            VM retVM = env.getGlobal_vmMap().get(retVCPU.getVMID());
                            ComputeHost retHost = env.getGlobal_hostMap().get(retVM.getHostID());

                            if(host.getBw() > retHost.getBw()){
                                retVCPU = vcpu;
                                retRouter = r;
                            }
                        }
                    }

                 // endTask.setvCPUID(retVCPU.getPrefix());
                }
            }
            return retRouter;



    }



    public CustomIDSet getAncestorsInVNF(CustomIDSet set, VNF vnf, VCPU vcpu, SFC sfc) {
        Iterator<DataDependence> dpredIte = vnf.getDpredList().iterator();


        while (dpredIte.hasNext()) {
            DataDependence dpred = dpredIte.next();
            VNF predVNF = sfc.findVNFByLastID(dpred.getFromID().get(1));
            Long predID = predVNF.getIDVector().get(1);
            if (set.contains(predID)) {
                continue;
            } else {
                if ((predVNF.getvCPUID()!=null)&&(predVNF.getvCPUID()== vcpu.getPrefix())) {
                    set.add(predVNF.getIDVector().get(1));
                    set = this.getAncestorsInVNF(set, predVNF, vcpu, sfc);
                } else {
                    continue;
                }
            }

        }
        return set;
    }

    public  VM findVM(AutoEnvironment env, String vcpuID){
        Iterator<VM> vmIte = env.getGlobal_vmMap().values().iterator();
        VM retVM = null;
        while(vmIte.hasNext()){
            VM vm = vmIte.next();
            if(vm.getvCPUMap().containsKey(vcpuID)){
                retVM = vm;
                break;
            }
        }
        return retVM;
    }

    public double calcExecTime(long workLoad, VCPU vcpu) {
        return CloudUtil.getRoundedValue((double) workLoad / (double) vcpu.getMips());
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
     * Interestパケットが到着して，そして次の転送先をFIBから選択する．
     * 特定基準を満たすものがFIBにあれば，転送し，見つからなければ自分で実行する．
     * ちなみに，履歴リストにて，過去に自分に転送されていないことを保証する必要あり．
     * pがやってきた時点で，自分が含まれていないことを保証する．
     * 要実装．
     * @param p
     * @param r
     * @return
     */
    @Override
    public String findNextRouter(InterestPacket p, CCNRouter r) {


       // String ownIP = ResourceMgr.getIns().getOwnIPAddr();
        //InterestからのSFC
        String prefix = p.getPrefix();
        SFC sfc_int = (SFC)p.getAppParams().get(AutoUtil.SFC_NAME);
        SFC sfc_own = null;
        //routerが持つSFC
        if(r.isSFCExists(sfc_int)){
            sfc_own = r.getSfcMap().get(sfc_int);
        }else{
            //初めてのSFCなら，登録する．
            r.getSfcMap().put(sfc_int.getSfcID(), (SFC)sfc_int.deepCopy());
            //r.getSfcMap().put(sfc_int.getSfcID(), (SFC)sfc_int);
            sfc_own = r.getSfcMap().get(sfc_int.getSfcID());

        }
        //Interestパケット内のSFCを更新する．

        //sfc_intとsfc_ownに差分がある場合，どうするか．
        //自分から転送するsfcでは，あくまで自身がもつものである．よって，すべてが最新である必要がある．
        //双方に割当先が記載されていれば，あくまで自分のものを優先させるべきえある．
        Iterator<VNF> vIte = sfc_int.getVnfMap().values().iterator();
        while(vIte.hasNext()){
            VNF vnf = vIte.next();
            String vPrefix = vnf.getvCPUID();
            if(vPrefix != null){
                VNF ownVNF = sfc_own.findVNFByLastID(vnf.getIDVector().get(1));
                if(ownVNF.getvCPUID() == null){
                    //もし自身のprefixがなければ設定してあげる．
                    ownVNF.setvCPUID(vPrefix);
                }else{
                    //既に設定済みであれば何もしない．
                }
            }
        }
        //そしてInterestパケットの中身のSFCを更新する．
        sfc_int = sfc_own;
        //次に，後続タスクを取得
        SFC sfc = sfc_own;
        Long predID = AutoSFCMgr.getIns().getPredVNFID(p.getPrefix());
        VNF predSF = sfc.findVNFByLastID(predID);
        if(predSF.getvCPUID() != null){
            //もし既に割り当て済みなら，それを送る．
            CCNRouter target = (CCNRouter)this.findVM(AutoSFCMgr.getIns().getEnv(), predSF.getvCPUID());
            //return target.getRouterID();
            return predSF.getvCPUID();

        }

        //他のルータIDであれば，実行しようとする．
        Iterator<ForwardHistory> fIte = p.getHistoryList().iterator();
        boolean isForwarded = false;
        while(fIte.hasNext()) {
            ForwardHistory history = fIte.next();
            if (history.getToID().equals(r.getRouterID()) && (history.getToType() == CCNUtil.NODETYPE_ROUTER)) {
                //過去に自分へのinterest転送がなされているかどうか
                //もし転送済みであれば，自身のIDを返す．
                isForwarded = true;
                break;
            }
        }
        //転送済みなら，ここで自分のIDをreturnして終わり．
        /*if(isForwarded){
            return r.getRouterID();
        }*/
            //以降の処理は，自身が転送されていない前提での処理．
        //まずは自身のvCPUにおける最小のblevelWSTを計算する．
        //つまり，predSFがこのVCPUが属するVMに割り当てられた場合．
        Iterator<VCPU> vIte2= r.getvCPUMap().values().iterator();
        VCPU localVCPU = null;
        double localMinWST = Long.MAX_VALUE;

        Long sucID = AutoSFCMgr.getIns().getSucVNFID(p.getPrefix());

        while(vIte2.hasNext()){
            VCPU  vcpu =  vIte2.next();
            double blevelWST  = Long.MAX_VALUE;
            //END->nullの場合は，ENDの処理時間のみを考える．
            if(sucID == -1){
                blevelWST = AutoSFCMgr.getIns().calcExecTime(predSF.getWorkLoad(), vcpu);
            }else{
                //ENDでなければ，普通に計算する．
                blevelWST = this.calcBlevelWST(predSF, vcpu, sfc);
            }
            if(blevelWST < localMinWST){
                localMinWST = blevelWST;
                localVCPU = vcpu;
            }
        }
        double fibMinWST = Long.MAX_VALUE;
        if(isForwarded){
            return localVCPU.getPrefix();
        }
        //次にFIBから選択する．
        //prefixに最もマッチするレコードを取得する．
        LinkedList<Face> fList = this.findLongestMatch(r, prefix);
        Iterator<Face> fnIte = fList.iterator();
        Long retID = r.getRouterID();
        long imageSize = (long)predSF.getImageSize();

        //Fibのface単位に対するループ
        while(fnIte.hasNext()){
            Face face = fnIte.next();
            if(face.getType() == CCNUtil.NODETYPE_ROUTER){
                //ルータを取得する．
                CCNRouter vm = CCNMgr.getIns().getRouterMap().get(face.getPointerID());

                ComputeHost host = env.getGlobal_hostMap().get(vm.getHostID());
                if(host == null){
                    continue;
                }
                //double dlTime = Calc.getRoundedValue((double)((double)imageSize/(double)host.getBw()));
                //同一タイプのSFが割り当てられているかどうかによってDL時間が変わる．
                double dlTime = AutoSFCMgr.getIns().calcImageDLTime(predSF, vm);
                if(dlTime == -1d){
                    continue;
                }


                //VMのvCPUに対するループ
                Iterator<VCPU> vcIte = vm.getvCPUMap().values().iterator();
                //自分以外に対するループ
                if(face.getPointerID() != r.getRouterID()){
                    while(vcIte.hasNext()){
                        VCPU vcpu = vcIte.next();
                        //当該vcpuでのblevelWSTを計算する．
                        double comTime = -1;

                        double tmpBlevelWST = Long.MAX_VALUE;
                        if(AutoSFCMgr.getIns().getSucVNFID(prefix) == -1){
                            tmpBlevelWST = dlTime + this.calcExecTime(predSF.getWorkLoad(), vcpu);
                        }else{
                            tmpBlevelWST = dlTime + this.calcBlevelWST(predSF, vcpu, sfc);

                        }
                        if(tmpBlevelWST < localMinWST){
                            localMinWST = tmpBlevelWST;
                            localVCPU = vcpu;
                            retID = face.getPointerID();
                        }
                    }
                }

            }
        }


        return localVCPU.getPrefix();

    }



    public LinkedList<Face> findLongestMatch(CCNRouter router, String prefix){
        //LongestMatchによって指定のprefixに最も近いprefixを選択する．
        Iterator<String> pIte  = router.getFIBEntry().getTable().keySet().iterator();
        int maxCnt = 0;
        LinkedList<Face> retList = null;
        String retPrefix = null;
        while(pIte.hasNext()){
            String fibPrefix = pIte.next();
            //まずは，startwithであることが必要．
            int cnt = -1;
            if(prefix.startsWith(fibPrefix)){
                cnt = this.longestMatch(prefix, fibPrefix);
            }

            if(cnt >= maxCnt){
                maxCnt = cnt;
                retPrefix = fibPrefix;
                retList = router.getFIBEntry().getTable().get(fibPrefix);

            }

        }
        return retList;

        //対象prerixが決まったので，後はどのfaceにするか．
        //とりあえず，ランダム．
        /*LinkedList<Face> retFList = router.getFIBEntry().getTable().get(retPrefix);
        int ran = CCNUtil.genInt(0, retFList.size()-1);
        Face f = retFList.get(ran);

         */


    }

    public double calcExecTime(long workLoad, long speed) {
        return CloudUtil.getRoundedValue((double) workLoad / (double) speed);

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
        //ComputeHost fromHost = fromCloud.getComputeHostMap().get(fromHostID);
        //ComputeHost toHost = toCloud.getComputeHostMap().get(toHostID);
        ComputeHost fromHost = env.getGlobal_hostMap().get(fromDCID+CloudUtil.DELIMITER + fromHostID);
        ComputeHost toHost = env.getGlobal_hostMap().get(toDCID+CloudUtil.DELIMITER + toHostID);

        if((fromHost == null)||(toHost == null)){
            System.out.println("fjdkla");
        }
        long hostBW = NFVUtil.MAXValue;
        if ((fromDCID.longValue() == toDCID.longValue())&&(fromHostID.longValue() == toHostID.longValue())) {
            //同一ホストなら，0を返す．
            return 0;
        } else {
            hostBW = Math.min(fromHost.getBw(), toHost.getBw());
        }

        long realBW = Math.min(dcBW, hostBW);

        double time = CloudUtil.getRoundedValue((double) dataSize / (double) realBW);

        return time;

    }

    /**
     * 指定の帯域幅のもとでの通信時間を求める
     *
     * @param dataSize
     * @param bw
     * @return
     */
    public double calcComTime(long dataSize, long bw) {

        double time = CloudUtil.getRoundedValue((double) dataSize / (double) bw);
        return time;
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

    public double calcBlevelWST(VNF vnf, VCPU vcpu, SFC sfc){
        //まずはvcpu内のSFのトータル仕事量を求める．
        long currentTotalW = this.calcTotalWorkload(vcpu);
        //System.out.println("VNF#@vcpu"+vcpu.getPrefix()+":"+vcpu.getVnfQueue().size());
        //さらに，今回のvnfの仕事量を加算する．
        currentTotalW += vnf.getWorkLoad();
        //まずはvcpuにおけるvnfの先祖SF集合を取得する．
        CustomIDSet ansSet = this.getAncestorsInVNF(new CustomIDSet(), vnf, vcpu, sfc);

        Iterator<Long> aIte = ansSet.iterator();
        long ansTotalW = 0;
        while(aIte.hasNext()){
            Long id = aIte.next();
            // System.out.println("***Ans:"+id);

            VNF predVNF = sfc.findVNFByLastID(id);
            ansTotalW += predVNF.getWorkLoad();
        }

        long slackW = currentTotalW - ansTotalW;
        //次に，out SFにおいてblevelWSTの最大値を取得する．
        //まずは，当該vcpuにおけるoutのblevelの最大値を取得する．
        Iterator<Long> outIte = vcpu.getOutSFSet().iterator();
        double retBlevelWST = -1;

        while(outIte.hasNext()){
            VNF outVNF = sfc.findVNFByLastID(outIte.next());

            double blevelWST = outVNF.getBlevelWST();
            //0524 START
            if(blevelWST < 0){
                VCPU outVCPU = this.env.getGlobal_vcpuMap().get(outVNF.getvCPUID());
                blevelWST = this.calcBlevelWST(outVNF, outVCPU, sfc);
            }
            //0524 END
            if(retBlevelWST <= blevelWST){
                retBlevelWST = blevelWST;
            }
        }
        double sameBlevelWST = this.calcExecTime(slackW, vcpu)+ retBlevelWST;
        this.env = AutoSFCMgr.getIns().getEnv();
        //次に，当該vnfがoutであった場合のblevelの最大値を取得する．
        Iterator<DataDependence> dsucIte = vnf.getDsucList().iterator();
        double difBlevwlWST = -1;
        while(dsucIte.hasNext()){

            DataDependence dsuc = dsucIte.next();
            VNF sucSF = sfc.findVNFByLastID(dsuc.getToID().get(1));
            double tmpBlevelWST = -1;
            if(sucSF.getvCPUID() == null){
                VM vm = this.env.getGlobal_vmMap().get(vcpu.getVMID());
                ComputeHost host = this.env.getGlobal_hostMap().get(vm.getHostID());

                tmpBlevelWST = this.calcExecTime(vnf.getWorkLoad(), vcpu) + this.calcComTime(dsuc.getMaxDataSize(),
                        host.getBw()) + sucSF.getBlevelWST();

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


    public double calcBlevelWST(VNF vnf, CustomIDSet set, SFC sfc) {
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
            VNF toVNF = sfc.findVNFByLastID(toID);
            double tmpValue = 0.0d;
            long dataSize = dsuc.getMaxDataSize();
            if (set.contains(toID)) {
                tmpValue = this.calcExecTime(vnf.getWorkLoad(), 1) + this.calcComTime(dataSize, 1) + toVNF.getBlevelWST();
            } else {
                tmpValue = this.calcExecTime(vnf.getWorkLoad(), 1) + this.calcComTime(dataSize, 1) + this.calcBlevelWST(toVNF, set, sfc);
            }
            if (maxValue <= tmpValue) {
                maxValue = tmpValue;
                vnf.setDominantWSTSucID(toID);
                vnf.setBlevelWST(maxValue);
            }
        }

        return maxValue;
    }

}
