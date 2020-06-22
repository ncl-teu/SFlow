package org.ncl.workflow.ccn.sfc.strategy;

import com.intel.jndn.forwarder.api.Face;
import com.intel.jnfd.deamon.face.tcp.TcpFace;
import com.intel.jnfd.deamon.fw.ForwardingPipeline;
import com.intel.jnfd.deamon.fw.RetxSuppression;
import com.intel.jnfd.deamon.table.fib.FibEntry;
import com.intel.jnfd.deamon.table.fib.FibNextHop;
import com.intel.jnfd.deamon.table.pit.PitEntry;
import net.gripps.cloud.core.CloudEnvironment;
import net.gripps.cloud.core.VCPU;
import net.gripps.cloud.core.VM;
import net.gripps.cloud.nfv.NFVEnvironment;
import net.gripps.cloud.nfv.sfc.SFC;
import net.gripps.cloud.nfv.sfc.VNF;
import net.gripps.clustering.common.aplmodel.DataDependence;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;
import org.ncl.workflow.ccn.autoicnsfc.AutoICNSFCMgr;
import org.ncl.workflow.ccn.autoicnsfc.AutoICNSFCScheduling;
import org.ncl.workflow.ccn.autoicnsfc.BWSTComparator;
import org.ncl.workflow.ccn.core.NclwNFDMgr;
import org.ncl.workflow.ccn.sfc.process.NFDTask;
import org.ncl.workflow.ccn.sfc.process.NFDTaskEngine;
import org.ncl.workflow.ccn.sfc.process.NclwNFDSendThread;
import org.ncl.workflow.ccn.util.ResourceMgr;
import org.ncl.workflow.comm.InterestHopInfo;
import org.ncl.workflow.comm.NCLWData;
import org.ncl.workflow.comm.WorkflowJob;
import org.ncl.workflow.util.NCLWUtil;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Created by Hidehiro Kanemitsu on 2020/01/17
 * Autnomous Workflow Scheduling for ICN-based Service Function Chaining (AutoICN-SFC)アルゴリズム
 * 用のstrategyです．
 */
public class AutoICNSFCStrategy extends BackTrackStrategy {


    public static final Name STRATEGY_NAME
            = new Name("ndn:/localhost/nfd/strategy/best-route/%SFCA%01");


    public AutoICNSFCStrategy(ForwardingPipeline forwarder, Name name) {
        super(forwarder, name);
    }

    public AutoICNSFCStrategy(ForwardingPipeline forwarder) {
        super(forwarder);

    }


    /**
     * 当該ノードでSFを起動させて，findTargetによって先行SFの転送先を決める．
     * そしてinterestを送信する．
     *
     * @param sfcData
     * @param interest
     * @param pitEntry
     */
    public void processNCLWInterest(NCLWData sfcData, Interest interest, PitEntry pitEntry) {
        SFC sfc = AutoICNSFCMgr.getIns().getSched().getSfc();
        //NFVEnvironment env = sfcData.getEnv();
        //自身のenvを取得する．したがって，NCLWDataにはenvは含めない．
        NFVEnvironment env = ResourceMgr.getIns().getEnv();
        NFDTask task = null;
        WorkflowJob job = sfcData.getJob();
        //VNFの割り当て先の設定

        //vCPUのVNFキューへの追加

        //以降は，タスクを取得する処理．つまり，これ以上はinterestを転送しないことがわかった場合の処理．
        String prefix = sfcData.getJob().getJobID() + "^" + sfcData.getFromTaskID();

        if (NFDTaskEngine.getIns().getTaskPool().containsKey(prefix)) {
            task = NFDTaskEngine.getIns().getTaskPool().get(prefix);
        } else {
            task = sfcData.getJob().getNfdTaskMap().get(sfcData.getFromTaskID());
            NFDTaskEngine.getIns().getTaskPool().put(prefix, task);
        }
        task.setEnv(env);
        task.setSfc(sfc);


        VNF taskVnf = sfc.findVNFByLastID(task.getTaskID());

        long toID = -1;
       /*VNF opredVNF = sfc.findVNFByLastID(sfcData.getFromTaskID());
        VM predHost = NCLWUtil.findVM(env, opredVNF.getvCPUID());
        VNF otoVNF = sfc.findVNFByLastID(sfcData.getToTaskID());

        VM toHost = NCLWUtil.findVM(env, otoVNF.getvCPUID());
*/

      //  if(!predHost.getIpAddr().equals(toHost.getIpAddr())){
            task.addInterestCounter();

      //  }

        //Interestパケット受信の同期はとらない．
//System.out.println("*****92****");
//System.out.println("Counter:"+task.getInterestCounter());

        //もしSTART VNFならば，実行して結果をDataパケットとして渡す．
        if (task.getVnf().getDpredList().isEmpty()) {

            VNF vnf = task.getVnf();
            //if(task.getInterestCounter() >= vnf.getDsucList().size()){
                this.processStartVNF(sfcData);
           // }
            //sfcData.setToFace(inFace);


        } else {
            //以降は，STARTでないタスクにinterestが届いた場合の処理．
            //先行タスク数分だけNclwNFDSendThreadを新規に生成して，prefixをセットして，from/toをセット
            //して，interstを送る．
            //taskがfromTask扱いになる．
            Iterator<DataDependence> preIte = task.getVnf().getDpredList().iterator();
            SFC orgSFC = AutoICNSFCMgr.getIns().getSched().getSfc();
            AutoICNSFCScheduling sched = AutoICNSFCMgr.getIns().getSched();
            PriorityQueue<VNF> vnfQueue = new PriorityQueue<VNF>(5, new BWSTComparator());

            //sfcとenvは，前のループのもの＋
            //BlevelWSTの順にソートする処理．
            while (preIte.hasNext()) {
                DataDependence dpred = preIte.next();
                VNF predVNF = orgSFC.findVNFByLastID(dpred.getFromID().get(1));
                NFDTask predNFDTask = sfcData.getJob().getNfdTaskMap().get(dpred.getFromID().get(1));
                NFDTask toTask = sfcData.getJob().getNfdTaskMap().get(taskVnf.getIDVector().get(1));

                Name PredPrefix = NclwNFDMgr.getIns().createPrefix(predNFDTask, toTask);
                String rel = sfcData.getJob().getJobID() + "^" + predVNF.getIDVector().get(1) + "^" + taskVnf.getIDVector().get(1);

                //既にinterestを送ったかどうかを確認する．

                System.out.println("BEFORE MapSIZE:"+AutoICNSFCMgr.getIns().getInterestSentMap().size());
                // if(predVNF.isInterestSent()){
                Iterator<String> sIte = AutoICNSFCMgr.getIns().getInterestSentMap().keySet().iterator();

                System.out.println("****TARGET URL:" + PredPrefix.toUri());
                if (AutoICNSFCMgr.getIns().isKeyExist(rel)) {
                     System.out.println("***"+predVNF.getIDVector().get(1) + " is Already Sent Interest***");
                } else {
                      System.out.println("***"+predVNF.getIDVector().get(1) + " is Not Found***");

                    vnfQueue.offer(predVNF);
                    //System.out.println("***VNF Queue Size:"+vnfQueue.size() + "VNF ID:"+predVNF.getIDVector().get(1));
                }

            }
            NFDTask toTask = sfcData.getJob().getNfdTaskMap().get(taskVnf.getIDVector().get(1));
            //BlevelWSTの大きい順に取り出す．
            int len = vnfQueue.size();
            for (int i = 0; i < len; i++) {
                VNF vnf = vnfQueue.poll();
                NFVEnvironment nenv =  sched.getEnv();
                WorkflowJob job2 = this.updateTaskInJob(sfcData.getJob(), sched.getSfc(), sched.getEnv());

                NCLWData data = new NCLWData(vnf.getIDVector().get(1), taskVnf.getIDVector().get(1), /*"localhost"/*/
                        null, NCLWUtil.NFD_PORT, orgSFC, nenv, job2);
                data.setFile(false);
                NFDTask predNFDTask = sfcData.getJob().getNfdTaskMap().get(vnf.getIDVector().get(1));
                Name PredPrefix = NclwNFDMgr.getIns().createPrefix(predNFDTask, toTask);
                data.getHopInfo().setPrefix(PredPrefix.toUri());
                data.getHopInfo().addIp(ResourceMgr.getIns().getOwnIPAddr());

                System.out.println("Prefix:" + PredPrefix.toUri());
                String rel = sfcData.getJob().getJobID() + "^" + vnf.getIDVector().get(1) + "^" + taskVnf.getIDVector().get(1);
                System.out.println("**putted REL:"+rel);

                AutoICNSFCMgr.getIns().getInterestSentMap().put(rel, 1);
                System.out.println("AFTER MapSIZE:"+AutoICNSFCMgr.getIns().getInterestSentMap().size());


                VCPU vcpu = sched.findTargetVCPU(data, PredPrefix);
                System.out.println("***Target VCPU:" + vcpu.getPrefix() + "# of VNFs:" + vcpu.getVnfQueue().size() + "@" + vnf.getIDVector().get(1));

                VM host = sched.getEnv().getGlobal_vmMap().get(vcpu.getVMID());
               // data.setEnv((NFVEnvironment)sched.getEnv().deepCopy());
                data.setIpAddr(host.getIpAddr());
                //同一ノードへの転送であれば，
                System.out.println("***FACE Structure::" + "Remote:" + host.getIpAddr() + "/Local:" + ResourceMgr.getIns().getOwnIPAddr());
System.out.println("***NCLWData:"+data.getIpAddr() + "/"+data.getFromTaskID() + "/"+data.getToTaskID());
                if (host.getIpAddr().equals(ResourceMgr.getIns().getOwnIPAddr())) {

                    //疑似Interestを作成．
                    Interest predInterest = new Interest();
                    predInterest.setName(PredPrefix);
                    predInterest.setApplicationParameters(new Blob(data.getAllBytes()));

                    TcpFace predFace = NclwNFDMgr.getIns().createFace(host.getIpAddr(), ResourceMgr.getIns().getOwnIPAddr());
                    //Interestの転送をせずに，PipelineのonIncomingInterestを呼ぶ．
                    NclwNFDMgr.getIns().getPipeline().onIncomingInterest(predFace, predInterest);
                } else {


                    //異なるノードへの転送であれば，そうする．
                    //異なるVMであれば，vmへ転送する．
                    TcpFace oFace = NclwNFDMgr.getIns().createFace(host.getIpAddr(), ResourceMgr.getIns().getOwnIPAddr());
                    NFDTask predTask = data.getJob().getNfdTaskMap().get(data.getFromTaskID());

                    task = data.getJob().getNfdTaskMap().get(data.getToTaskID());
                    System.out.println("**INTEEST HOP SEND2: FROM:" + ResourceMgr.getIns().getOwnIPAddr() + "/To:" + host.getIpAddr());
                    System.out.println("**PredTask:"+predTask.getTaskID() + "/**ToTask:"+task.getTaskID());

                    //oFace.sendInterest();
                    NclwNFDMgr.getIns().getPipeline().onOutgoingInterest(
                            predTask,
                            task,
                            data,
                            pitEntry,
                            oFace,
                            false);


                }
            }
        }
    }

    public HashMap<String, HashMap<String, Integer>>
    addIpMap(HashMap<String, HashMap<String, Integer>> ipMap_src, String prefix, String ip) {
        HashMap<String, Integer> map = null;
        HashMap<String, HashMap<String, Integer>> data;
        if (ipMap_src.containsKey(prefix)) {
            map = ipMap_src.get(prefix);
            map.put(ip, 1);

        } else {
            map = new HashMap<String, Integer>();
            map.put(ip, 1);
            ipMap_src.put(prefix, map);
        }
        return ipMap_src;


    }

    /**
     *
     * @param cenv
     * @return
     */
    public NFVEnvironment convertToNFVEnv(CloudEnvironment cenv){

        return null;
    }

    public WorkflowJob  updateTaskInJob(WorkflowJob job, SFC sfc, NFVEnvironment env){
        Iterator<NFDTask> tIte = job.getNfdTaskMap().values().iterator();
        while(tIte.hasNext()){
            NFDTask task = tIte.next();
            task.setSfc(sfc);
            task.setEnv(env);
        }
        return job;
    }

    /**
     * Interestパケット受信後，PITにエントリが無かった場合の処理．
     * 当該タスクの仮想先行タスクを見る．
     * もし仮想先行タスクがあれば，その親～当該タスクのblevelが支配的である．
     * なので，当該タスクから親タスクの割り当て先を決める．
     * 決まったら仮想先行タスクに対し，親タスクの割り当て先の通知を行う．
     * その後に，親タスクへinterestを投げる．
     * Interestパケットの種類:
     * /nclw/: 通常の割当先決定のためのInterestパケット
     * /nclw_vctl/仮想辺をたどって，割当先追加のためのパケット．データ返送として，自身の割当先ノード情報を返す．
     *
     * @param inFace
     * @param interest
     * @param fibEntry
     * @param pitEntry
     */
    @Override
    public void afterReceiveInterest(Face inFace, Interest interest, FibEntry fibEntry, PitEntry pitEntry) {

        if (interest.getName().toUri().startsWith(NCLWUtil.NCLW_PREFIX)) {

            //Interestから，NCLWDataを取り出す．
            NCLWData sfcData = NclwNFDMgr.getIns().fetchNCLWData(interest);
            String targetIP = sfcData.getIpAddr();

            InetAddress addr;
            //自身のIPアドレスを取得する．
            String ownIP = ResourceMgr.getIns().getOwnIPAddr();
            boolean isFound = false;
            //自身宛かどうかのチェック．これもチェックする．
            //IPの同一性ではなく，求められる性能の同一性が大事．
            //FIBには，性能に関する整列が求められる．
            //FIBの初期配置問題ということにもなる．

            //FIB内で，前ホップから得られたblevel_sched(predSF)を最小にするものが無ければそれでおしまい．
            //そうすると，永遠に同一マシンへ転送されてしまうので，ある一定数超えると遠隔に行くしかけが
            //必要となる．
            //つまり，blevel_sched(当該SF@自分) vs blevel_sched(当該SF@FIB内の一つ）
            //FIBはIPアドレス単位なので，そのノードのvCPUを見て，一つでも自分よりも良いblevel_schedがあれば
            //Trueとなる．
            //また，1つのvCPUに対して一定数のinterestパケットが来たら，拒否するようなしかけが必要である．
            //そのためのテーブルが必要．このテーブルはノード(=VM)で一つということになる．当該VMがいくつのvCPU
            //を持っているかを認識しておく必要がある．
            //ownIPから，vCPUを取り出す方法は？->NCLWUtilでVM取得できる．
            //blevel_sched =
            //predの実行時間@ターゲットノード + com + blevel(後続SF)@元ノード : ターゲットノード≠元ノード
            //predの実行時間@ターゲットノード + blevel_sched(後続SF)@元ノード: ターゲットノード＝元ノード

            SFC sfc = sfcData.getSfc();
            //NFVEnvironment env = sfcData.getEnv();
            //自身のenvを取得する．したがって，NCLWDataにはenvは含めない．
            NFVEnvironment env = ResourceMgr.getIns().getEnv();


            if (ownIP.equals(targetIP)) {
                isFound = true;
            }


            NFDTask task = null;
            if (isFound) {

                //srcから，FIB@srcから決まったblevelWST値として
                //interest転送されてくる．しかし，これが正確な値かどうかは不明．
                //よって，転送先であるこちらでも，再度，blevelWST値を計算する必要がある．
                //すなわち，srcで決まったblevelWST値はもはや不要であり，単にinterest
                //転送に使われただけのものとなる．
                //まずは，当該ノード内のvCPU達において，
                //blevelWSTの最小値となるものを探す．
                //これをsrcのblevel_WSTと比べる．
                //もしこちらのほうが低ければ，更に転送する．
                //もしIPが自身と一致すれば，Taskを起動する（自分自身が最も良いため）
                //そして，Taskの先行タスクたちのvCPUへInterestを送る．
                //自身のVM内のvcpuリストのblevelWST vs FIB内のvcpuListのblevelWSTを比較する．
                //そして，自分で打ち止めにするかどうかを判断する．
                //戻り値は，転送先のIPアドレスと
                AutoICNSFCScheduling sched = AutoICNSFCMgr.getIns().getSched();

                //自身のIPとenv, predSF, toSF(こちらからのinterest送信）を渡して，vCPUをもらう．
                //つまり，IP+env + sfcData
                VCPU retVCPU = sched.findTargetVCPU(sfcData, interest.getName());

                //IPアドレス取得
                VM vm = env.getGlobal_vmMap().get(retVCPU.getVMID());

                //HashMap<String, HashMap<String, Integer>> ipMap = sfcData.getIpMap();

                System.out.println("***Target VM: " + vm.getIpAddr());
                boolean fwdFlg = false;
                if (sfcData.getHopInfo().isExists(ownIP)) {
                    fwdFlg = true;

                } else {

                }

                if (sched.getSfc() == null) {
                    sched.setSfc(sfcData.getSfc());
                }

                if (vm.getIpAddr().equals(ownIP) || fwdFlg) {
                    VNF predSF = sched.getSfc().findVNFByLastID(sfcData.getFromTaskID());
                    System.out.println("***Pred SF Preparation Mode: vCPU:" + retVCPU.getPrefix() + "@VNF" + predSF.getIDVector().get(1));

                    //割当先が決まったときだけ，設定する．
                    predSF.setvCPUID(retVCPU.getPrefix());
                    //vCPUでのキュー追加
                    if (sched.containsVNF(retVCPU.getVnfQueue(), predSF)) {

                    } else {
                        retVCPU.getVnfQueue().add(predSF);

                    }
                    sched.updateOutSFSet(retVCPU.getPrefix());

                    //同一VMであれば，ここで転送終了．また，循環を防ぐために，既に含まれている
                    //IPであれば，ここで終了．
                    //Taskを起動する．
                    //あとは先行SFへのInterest処理

                    this.processNCLWInterest(sfcData, interest, pitEntry);
                } else {
                    System.out.println("***Interest Forwarding Mode***");
                    SFC frdSFC = null;

                    WorkflowJob job = this.updateTaskInJob(sfcData.getJob(), sched.getSfc(), sched.getEnv());


                    //ToDo: job内のすべてのtaskにおいて，sfc, envをsched.sfc, sched.envへ統一させる．
                    //異なるVMであれば，vmへ転送する．

                    NCLWData data = new NCLWData(sfcData.getFromTaskID(), sfcData.getToTaskID(), /*"localhost"/*/
                            vm.getIpAddr(), NCLWUtil.NFD_PORT, sched.getSfc(), sched.getEnv(), job);
                    data.setFile(false);

                    //this.addIpMap(sfcData.getIpMap(), interest.getName().toUri(), ownIP );
                    InterestHopInfo info = sfcData.getHopInfo();
                    info.addIp(ownIP);
                    data.setHopInfo(info);

                    //.setIpMap(this.addIpMap(sfcData.getIpMap(), interest.getName().toUri(), ownIP));
                    TcpFace oFace = NclwNFDMgr.getIns().createFace(vm.getIpAddr(), ownIP);
                    NFDTask predTask = data.getJob().getNfdTaskMap().get(sfcData.getFromTaskID());

                    task = data.getJob().getNfdTaskMap().get(sfcData.getToTaskID());
                    System.out.println("**INTEEST HOP SEND: FROM:" + ResourceMgr.getIns().getOwnIPAddr() + "/To:" + vm.getIpAddr());


                    NclwNFDMgr.getIns().getPipeline().onOutgoingInterest(
                            predTask,
                            task,
                            data,
                            pitEntry,
                            oFace,
                            false);
                }


            } else {

                List<FibNextHop> nextHopList = fibEntry.getNextHopList();
                FibNextHop nextHop = null;

                RetxSuppression.Result suppression
                        = retxSuppression.decide(inFace, interest, pitEntry);

                if (suppression == RetxSuppression.Result.NEW) {
                    // forward to nexthop with lowest cost except downstream
                    if (nextHopList != null) {
                        for (FibNextHop one : nextHopList) {
                            if (predicate_NextHop_eligible(pitEntry, one, inFace.getFaceId(),
                                    false, 0)) {
                                nextHop = one;
                                break;
                            }
                        }
                    }
                    //nextHopになければ，そのまま何もせずリターンする．
                    if (nextHop == null) {
                        rejectPendingInterest(pitEntry);
                        return;
                    }
                    //以降は，転送先を決定させる．longestマッチするものとする．
                    Face outFace = nextHop.getFace();
                    sendInterest(pitEntry, outFace, false);
                    return;
                }

                if (suppression == RetxSuppression.Result.SUPPRESS) {
                    return;
                }

                // find an unused upstream with lowest cost except downstream
                for (FibNextHop one : nextHopList) {
                    if (predicate_NextHop_eligible(pitEntry, one, inFace.getFaceId(),
                            true, System.currentTimeMillis())) {
                        nextHop = one;
                        break;
                    }
                }
                if (nextHop != null) {
                    Face outFace = nextHop.getFace();

                    sendInterest(pitEntry, outFace, false);
                    return;
                }

                // find an eligible upstream that is used earliest
                nextHop = findEligibleNextHopWithEarliestOutRecord(
                        pitEntry, nextHopList, inFace.getFaceId());
                if (nextHop != null) {
                    Face outFace = nextHop.getFace();
                    sendInterest(pitEntry, outFace, false);
                }
            }
        } else {

            List<FibNextHop> nextHopList = fibEntry.getNextHopList();
            FibNextHop nextHop = null;

            RetxSuppression.Result suppression
                    = retxSuppression.decide(inFace, interest, pitEntry);

            if (suppression == RetxSuppression.Result.NEW) {
                // forward to nexthop with lowest cost except downstream
                if (nextHopList != null) {
                    for (FibNextHop one : nextHopList) {
                        if (predicate_NextHop_eligible(pitEntry, one, inFace.getFaceId(),
                                false, 0)) {
                            nextHop = one;
                            break;
                        }
                    }
                }
                //nextHopになければ，そのまま何もせずリターンする．
                if (nextHop == null) {
                    rejectPendingInterest(pitEntry);
                    return;
                }

                Face outFace = nextHop.getFace();
                sendInterest(pitEntry, outFace, false);
                return;
            }

            if (suppression == RetxSuppression.Result.SUPPRESS) {
                return;
            }

            // find an unused upstream with lowest cost except downstream
            for (FibNextHop one : nextHopList) {
                if (predicate_NextHop_eligible(pitEntry, one, inFace.getFaceId(),
                        true, System.currentTimeMillis())) {
                    nextHop = one;
                    break;
                }
            }
            if (nextHop != null) {
                Face outFace = nextHop.getFace();

                sendInterest(pitEntry, outFace, false);
                return;
            }

            // find an eligible upstream that is used earliest
            nextHop = findEligibleNextHopWithEarliestOutRecord(
                    pitEntry, nextHopList, inFace.getFaceId());
            if (nextHop != null) {
                Face outFace = nextHop.getFace();
                sendInterest(pitEntry, outFace, false);
            }
        }
    }
}
