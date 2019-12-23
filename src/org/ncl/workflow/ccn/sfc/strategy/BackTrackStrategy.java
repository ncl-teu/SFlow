package org.ncl.workflow.ccn.sfc.strategy;

import com.intel.jndn.forwarder.api.Face;
import com.intel.jnfd.deamon.face.FaceUri;
import com.intel.jnfd.deamon.face.tcp.TcpFace;
import com.intel.jnfd.deamon.fw.ForwardingPipeline;
import com.intel.jnfd.deamon.fw.RetxSuppression;
import com.intel.jnfd.deamon.table.fib.FibEntry;
import com.intel.jnfd.deamon.table.fib.FibNextHop;
import com.intel.jnfd.deamon.table.pit.PitEntry;
import net.gripps.cloud.core.VM;
import net.gripps.cloud.nfv.NFVEnvironment;
import net.gripps.cloud.nfv.sfc.SFC;
import net.gripps.cloud.nfv.sfc.VNF;
import net.gripps.clustering.common.aplmodel.DataDependence;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;
import org.ncl.workflow.ccn.core.NclwNFDMgr;
import org.ncl.workflow.ccn.sfc.process.NFDTask;
import org.ncl.workflow.ccn.sfc.process.NFDTaskEngine;
import org.ncl.workflow.ccn.sfc.process.NclwNFDSendThread;
import org.ncl.workflow.ccn.sfc.process.NclwSFCFWPipeline;
import org.ncl.workflow.ccn.util.NetInfo;
import org.ncl.workflow.comm.NCLWData;
import org.ncl.workflow.comm.WorkflowJob;
import org.ncl.workflow.engine.NCLWEngine;
import org.ncl.workflow.engine.Task;
import org.ncl.workflow.util.NCLWUtil;
import org.ncl.workflow.util.ProcessMgr;

import java.io.*;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Hidehiro Kanemitsu on 2019/11/16.
 * WorkflowをENDからトレースする際に使われるstrategyクラスです．
 *
 */
public class BackTrackStrategy extends BaseSFCStrategy {

    protected LinkedBlockingQueue<NCLWData> recvDataQueue;


    public static final Name STRATEGY_NAME
            = new Name("ndn:/localhost/nfd/strategy/best-route/%SFC%01");

    public BackTrackStrategy(ForwardingPipeline forwarder, Name name) {
        super(forwarder, name);
        this.recvDataQueue = new LinkedBlockingQueue<NCLWData>();
    }

    public BackTrackStrategy(ForwardingPipeline forwarder) {
        super(forwarder);
        this.recvDataQueue = new LinkedBlockingQueue<NCLWData>();

    }



    public LinkedBlockingQueue<NCLWData> getRecvDataQueue() {
        return recvDataQueue;
    }

    public void setRecvDataQueue(LinkedBlockingQueue<NCLWData> recvDataQueue) {
        this.recvDataQueue = recvDataQueue;
    }

    /**
     * START VNRを実行して結果を渡す処理
     * NCLWDataを受信して，タスクへと渡す．
     */
    public void processStartVNF(NCLWData data) {
        byte[] buffer = new byte[512]; // ファイル受信時のバッファ
        String msg = null;
        try {
/*
            ServerSocket listen_socket = new ServerSocket(this.port);
            Socket client = listen_socket.accept();
*/

            //System.out.println("ip:" + data.getIpAddr() + "/readpath:" + data.getReadFilePath() + "/writePath:" + data.getWriteFilePath());
            //もしFromTaskIDがnullなら，startTask
            WorkflowJob job = data.getJob();
            SFC sfc = data.getSfc();
            NFDTask task;
            //START VNFがSTARTかつENDであるばあい
            if(data.getToTaskID() == -1){
                if(data.isFile()){
                    File file = data.getFile();
                    String path = file.getPath();
                    String dir = file.getParent();
                    if(!(dir == null)){
                        File dir2 = new File(file.getParent());
                        if(!dir2.exists()){
                            dir2.mkdirs();
                        }
                    }

                    // String outPath = "/"+file.getParent()+ "/"+file.getName();
                    //FileInputStream fis = new FileInputStream(file);
                    //結果ファイルを取得する．
                    ByteArrayInputStream bis = new ByteArrayInputStream(data.getBytes());
                    //FileInputStream fis = data.getFis();
                    FileOutputStream fos = new FileOutputStream(data.getWriteFilePath());
                    int fileLength;
                    while ((fileLength = bis.read(buffer)) > 0) {
                        //while ((fileLength = fis.read(buffer)) > 0) {
                        fos.write(buffer, 0, fileLength);
                    }
                    fos.flush();
                    fos.close();
                    //  fis.close();
                    bis.close();
                    System.out.println("Obtained File:"+file.getPath());
                }
                System.out.println("Obtained Msg:"+data.getMsg());
                ProcessMgr.getIns().setFinishTime(System.currentTimeMillis());
                System.out.println("Elappsed time:"+ NCLWUtil.getRoundedValue((ProcessMgr.getIns().getFinishTime() - ProcessMgr.getIns().getStartTime())/(double)1000) + "(sec)");
                System.exit(0);

            }
            //以降は，taskがENDではない場合の処理
            String prefix = job.getJobID()+"^"+data.getFromTaskID();
            HashMap<String, NFDTask> taskPool = NFDTaskEngine.getIns().getTaskPool();
            if(taskPool.containsKey(prefix)){
                task = (NFDTask)NFDTaskEngine.getIns().getTaskPool().get(prefix);
            }else{
                task = (NFDTask)job.getNfdTaskMap().get(data.getFromTaskID());
                NFDTaskEngine.getIns().getTaskPool().put(prefix, task);
            }


            System.out.println("Data arrived:"+data.getFromTaskID() + "->"+data.getToTaskID());

            Thread taskThread = new Thread(task);
            task.setVnf(sfc.findVNFByLastID(data.getFromTaskID()));
            task.setJobID(job.getJobID());
            task.setJob(job);
            task.setSfc(sfc);
            task.setEnv(data.getEnv());
            if(!task.isStarted()){
                taskThread.start();

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void processTest(NCLWData data){
        NclwNFDSendThread sender = new NclwNFDSendThread();
        Thread sendThread = new Thread(sender);
        //NCLWEngine.getIns().getExec().submit(sender);
        sendThread.start();

        sender.getDataQueue().add(data);



    }

    /**
     *  PITに追加して，CSにエントリがなかったときの処理
     * FIBにあればFIBへ転送する．FIBになければ転送しないので，
     * これを，appparameterに書いてある先行ノードへ転送して，
     * FIBへ追加するようにする．
     * @param inFace
     * @param interest
     * @param fibEntry
     * @param pitEntry
     */
    @Override
    public void afterReceiveInterest(Face inFace, Interest interest,
                                     FibEntry fibEntry, PitEntry pitEntry) {

        //通常はLogestマッチでFIBを見るので，変更する．
        //NCLWのSFCの場合のみ，完全一致のFIBを見つけようとする．
        //完全一致しなければ，data部のipを見る．fromを見る．
        //PrefixがNCLWUtil.NCLW_PREFIXである場合にのみ，下記の処理とする．

        if(interest.getName().toUri().startsWith(NCLWUtil.NCLW_PREFIX)){
            NCLWData sfcData = NclwNFDMgr.getIns().fetchNCLWData(interest);
            String targetIP = sfcData.getIpAddr();
            System.out.println("**InterestArrived: Task:"+sfcData.getFromTaskID() + "@"+sfcData.getIpAddr());

            InetAddress addr;
            String ownIP = null;
            boolean isFound = false;
            try{
                addr = InetAddress.getLocalHost();
                String hostName = addr.getHostName();

                LinkedList<InetAddress> addList = NetInfo.getAllIP();
                Iterator<InetAddress> addIte = addList.listIterator();
                while(addIte.hasNext()){
                    InetAddress ia = addIte.next();
                    if(targetIP.equals(ia.getHostAddress())){
                        isFound = true;
                        break;
                    }
                }
                /*
                InetAddress []adrs = InetAddress.getAllByName(hostName);
System.out.println("AddrNum:"+adrs.length);
                for(int i=0;i<adrs.length;i++){
 System.out.println("IP:"+adrs[i].getHostAddress());

                    if(adrs[i].getHostAddress()==targetIP){
                        isFound = true;
                        break;
                    }
                }
*/
                //ownIP = addr.getHostAddress();
            }catch(Exception e){
                e.printStackTrace();
            }
            SFC sfc = sfcData.getSfc();
            NFVEnvironment env = sfcData.getEnv();
            NFDTask task = null;
            if(isFound){
                //もしIPが一致すれば，Taskを起動する．
                //そして，Taskの先行タスクたちのvCPUへInterestを送る．
                String prefix = sfcData.getJob().getJobID() + "^"+sfcData.getFromTaskID();

                if(NFDTaskEngine.getIns().getTaskPool().containsKey(prefix)){
                    task = (NFDTask)NFDTaskEngine.getIns().getTaskPool().get(prefix);
                }else{
                    task = sfcData.getJob().getNfdTaskMap().get(sfcData.getFromTaskID());
                    NFDTaskEngine.getIns().getTaskPool().put(prefix, task);
                }

                VNF taskVnf = sfc.findVNFByLastID(task.getTaskID());

                long toID = -1;

                //ToFaceに，interstがやってきたFaceを登録する（ToTask, ToFace)．
                //NclwNFDMgr.getIns().getToFaceMap().put(sfcData.getJob()+"^"+sfcData.getToTaskID(), (TcpFace)inFace);
//テスト用コード：必ずstartとしてデータを送信する．
 //processTest(sfcData);

                //カウントを増やす．
                task.addInterestCounter();
                //もし，ここでまだinterest数が足りなければ何もしない．
                if(task.getInterestCounter() < taskVnf.getDsucList().size()){
                    System.out.println("***<<<<Interest"+"IntCount:"+task.getInterestCounter()+"/Max:"+taskVnf.getDsucList().size());

                    return;
                }
                //以降は，後続タスクすべてからinterestが届いた場合の処理．

                //もしSTART VNFならば，実行して結果をDataパケットとして渡す．
                if(task.getVnf().getDpredList().isEmpty()){
                    //sfcData.setToFace(inFace);
                    this.processStartVNF(sfcData);


                }else{
                    //以降は，STARTでないタスクにinterestが届いた場合の処理．
                    //先行タスク数分だけNclwNFDSendThreadを新規に生成して，prefixをセットして，from/toをセット
                    //して，interstを送る．
                    //taskがfromTask扱いになる．
                    Iterator<DataDependence> dpredIte = task.getVnf().getDpredList().iterator();

                    while(dpredIte.hasNext()){
                        DataDependence dpred = dpredIte.next();
                        VNF predVNF = sfc.findVNFByLastID(dpred.getFromID().get(1));
                        VM host = NCLWUtil.findVM(env, predVNF.getvCPUID());
                        VNF toVNF = sfc.findVNFByLastID(task.getTaskID());

                        VM toHost = NCLWUtil.findVM(env, toVNF.getvCPUID());
                        NCLWData data = new NCLWData(dpred.getFromID().get(1), dpred.getToID().get(1), /*"localhost"/*/
                                host.getIpAddr(), NCLWUtil.NFD_PORT, sfc, env, sfcData.getJob());

                        data.setFile(false);
                     //   data.setToFace(inFace);

                        NFDTask  predTask = data.getJob().getNfdTaskMap().get(dpred.getFromID().get(1));
                        NFDTask toTask = data.getJob().getNfdTaskMap().get(task.getTaskID());
                        if(host.getIpAddr().equals(toHost.getIpAddr())){
                            System.out.println("***288FACE Structure::"+"Remote:"+host.getIpAddr() + "/Local:"+toHost.getIpAddr());

                            //疑似Interestを作成．
                            Interest predInterest = new Interest();
                            predInterest.setName(NclwNFDMgr.getIns().createPrefix(predTask, toTask));
                            predInterest.setApplicationParameters(new Blob(data.getAllBytes()));

                            TcpFace predFace = NclwNFDMgr.getIns().createFace(host.getIpAddr(), toHost.getIpAddr());
                            NclwNFDMgr.getIns().getPipeline().onIncomingInterest(predFace, predInterest);


                            //PipelineのonInterestComeを呼ぶ．

                        }else{
                            FibEntry fibE = NclwNFDMgr.getIns().getFib().findExactMatch(interest.getName());
                            TcpFace oFace = null;
                            //もしFIBにちょうどのprefixが無ければ，先行VNFの割当先へとinterestを転送する．
                            //通常はこっち
                            //先行タスクの保持ホストのIPが設定される．
                            if(fibE == null){
                                //System.out.println("**NEW FACE!!**");
                                oFace = NclwNFDMgr.getIns().createFace(host.getIpAddr(), toHost.getIpAddr());
                                System.out.println("***FACE Structure::"+"Remote:"+host.getIpAddr() + "/Local:"+toHost.getIpAddr());
                                //NclwNFDMgr.getIns().getFib().insert(interest.getName(), oFace, 1);
                            }else{
                                 System.out.println("**FACE XXX!!**");

                                //FIBにあれば，そのfaceの宛先にinterestを送る．
                                List<FibNextHop> nList  = fibE.getNextHopList();
                                //System.out.println("****FIB SIZE: "+ nList.size());

                                Iterator<FibNextHop> fIte = nList.iterator();
                                long minCost = 10000000;
                                //Fibエントリのfaceのうち最低コストのFaceを決めるためのループ
                                while(fIte.hasNext()){
                                    FibNextHop nextHop = fIte.next();
                                    long cost = nextHop.getCost();
                                    if(cost <= minCost){
                                        minCost = cost;
                                        oFace = (TcpFace)nextHop.getFace();
                                    }
                                }
                            }

                            //Interest送信
                            //Interestのfrom: 先行タスク to: 現在のタスクとしたInterestパケットを生成する．
                        /*NclwNFDSendThread sender = new NclwNFDSendThread();
                        Thread sendThread = new Thread(sender);
                        sendThread.start();
                        //当該データを，startタスクのノードへ送る．
                        sender.getInterestDataQueue().add(data);
                         */
                            //TcpFace側でInterestを送信する．
                            //そして，Data到着は，Pipeline->onIncomingDataで呼ばれる．
                            //sendInterest(pitEntry, oFace, false);

                            NclwNFDMgr.getIns().getPipeline().onOutgoingInterest( (NFDTask)predTask, (NFDTask)task, data, pitEntry, oFace, false);
                        }

                    }
                }

            }else{
                //割り当て先IPが自分ではない場合でinterestが来た時の処理
                //違うのであれば，targetIpのfaceへInterst送信する．
                //TcpFace oFace = NclwNFDMgr.getIns().createFace(targetIP);
                //sendInterest(pitEntry, oFace, false);
                //Interestを破棄する??
                System.out.println("**Error: wrong Target IP");
            }

        }else{

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
