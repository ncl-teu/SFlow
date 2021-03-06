package org.ncl.workflow.ccn.sfc.process;

import com.intel.jndn.forwarder.api.Face;
import com.intel.jndn.forwarder.api.Strategy;
import com.intel.jndn.forwarder.impl.RegisterPrefixCommand;
import com.intel.jnfd.deamon.face.FaceUri;
import com.intel.jnfd.deamon.face.tcp.TcpFace;
import com.intel.jnfd.deamon.fw.BestRouteStrategy2;
import com.intel.jnfd.deamon.fw.FaceTable;
import com.intel.jnfd.deamon.fw.ForwardingPipeline;
import com.intel.jnfd.deamon.table.cs.SortedSetCs;
import com.intel.jnfd.deamon.table.deadnonce.DeadNonceNaive;
import com.intel.jnfd.deamon.table.fib.Fib;
import com.intel.jnfd.deamon.table.fib.FibEntry;
import com.intel.jnfd.deamon.table.measurement.Measurement;
import com.intel.jnfd.deamon.table.pit.Pit;
import com.intel.jnfd.deamon.table.pit.PitEntry;
import com.intel.jnfd.deamon.table.pit.PitInRecord;
import com.intel.jnfd.deamon.table.pit.PitOutRecord;
import com.intel.jnfd.deamon.table.strategy.StrategyChoice;
import com.intel.jnfd.util.NfdCommon;
import net.gripps.cloud.core.VM;
import net.gripps.cloud.nfv.NFVEnvironment;
import net.gripps.cloud.nfv.sfc.SFC;
import net.gripps.cloud.nfv.sfc.VNF;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.MetaInfo;
import net.named_data.jndn.Name;
import net.named_data.jndn.lp.LpPacket;
import net.named_data.jndn.util.Blob;
import org.ncl.workflow.ccn.core.NclwFWPipeline;
import org.ncl.workflow.ccn.core.NclwInterest;
import org.ncl.workflow.ccn.core.NclwNFDMgr;
import org.ncl.workflow.comm.NCLWData;
import org.ncl.workflow.comm.WorkflowJob;
import org.ncl.workflow.engine.Task;
import org.ncl.workflow.logger.NclwLog;
import org.ncl.workflow.util.NCLWUtil;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by Hidehiro Kanemitsu on 2019/11/17.
 */
    public class NclwSFCFWPipeline extends NclwFWPipeline {

     //   protected NclwNFDSendThread sender;

    public NclwSFCFWPipeline(ScheduledExecutorService scheduler) {
        super(scheduler);

    }

    /**
     * Interestパケットが来た場合の処理です．
     * @param inFace
     * @param interest
     */
    public  void onIncomingInterest(Face inFace, Interest interest) {
        NclwLog.getIns().log("-----onIncomingInterest START from "+inFace.getRemoteUri().getInet().getHostAddress());

        NCLWData data = NclwNFDMgr.getIns().fetchNCLWData(interest);

        if(interest.getApplicationParameters().isNull()){
        }else{
            //System.out.println("Param: "+ interest.getApplicationParameters().toString());
        }
        SFC sfc = data.getSfc();

        NFVEnvironment env = data.getEnv();
        VNF predVNF = sfc.findVNFByLastID(data.getFromTaskID());
        VNF toVNF = sfc.findVNFByLastID(data.getToTaskID());

        //System.out.println("***FIRST COME!!"+"Local:"+inFace.getLocalUri().getInet().getHostAddress()+"/Remote:"+inFace.getRemoteUri().getInet().getHostAddress());
        // handle registration commands; TODO this should be applied as an internal
        // face to the FIB but current interface does not allow this
        if (prefixRegistration.matches(interest.getName())) {
            if(inFace != null){
                prefixRegistration.call(interest, inFace, this);
                return;
            }
        }
        // PIT insert

        PitEntry pitEntry = pit.insert(interest).getFirst();
        NclwLog.getIns().log("PIT Add:"+pitEntry.getName());
        Iterator<PitInRecord> pIte = pitEntry.getInRecords().iterator();
        while(pIte.hasNext()){
            PitInRecord p = pIte.next();
        }
       // pitEntry.insertOrUpdateInRecord(inFace, interest);

        if(toVNF != null){
            VM predHost = NCLWUtil.findVM(env, predVNF.getvCPUID());
            VM toHost = NCLWUtil.findVM(env, toVNF.getvCPUID());

            //もしpred/toが同一ノード(VM含む）に割り当てられているなら，Interestは送信せずにカウンタのみを+するだけ．
            //そして，PITに追加する．
            if(predHost != null && toHost != null){
                if(predHost.getIpAddr().equals(toHost.getIpAddr())) {

                    WorkflowJob job = data.getJob();
                    String prefix = job.getJobID() + "^" + data.getFromTaskID();
                    HashMap<String, NFDTask> taskPool = NFDTaskEngine.getIns().getTaskPool();
                    NFDTask task;

                    if (taskPool.containsKey(prefix)) {
                        task = (NFDTask) NFDTaskEngine.getIns().getTaskPool().get(prefix);
                    } else {

                        task = (NFDTask) job.getNfdTaskMap().get(data.getFromTaskID());
                        NFDTaskEngine.getIns().getTaskPool().put(prefix, task);

                    }
                    //カウンタを+する．
                    task.addInterestCounter();

                }
            }else{


            }


        }
        this.processPit(inFace, interest, pitEntry);


    }

    public void processPit(Face inFace, Interest interest, PitEntry pitEntry){
        NclwLog.getIns().log("-----ProcessPit START-----");
        // detect duplicate Nonce
        if(inFace != null){
            int dnw = pitEntry.findNonce(interest.getNonce(), inFace);
            boolean hasDuplicateNonce = (dnw != PitEntry.DUPLICATE_NONCE_NONE)
                    || deadNonceList.find(interest.getName(), interest.getNonce());
            if (hasDuplicateNonce) {
                // goto Interest loop pipeline
                onInterestLoop(inFace, interest, pitEntry);
                //return;
            }
        }

        // cancel unsatisfy & straggler timer
        cancelUnsatisfyAndStragglerTimer(pitEntry);

        // is pending?
        List<PitInRecord> inRecords = pitEntry.getInRecords();
        ForwardingPipelineSearchCallback callback = new NclwSFCFWPipeline.NclwForwardingPipelineSearchCallback(inFace, pitEntry);
        //ForwardingPipelineSearchCallback callback = this.NclwForwardingPipelineSearchCallback(inFace, pitEntry);
        //Pitに無ければ，CSを見る．
        if (inRecords == null || inRecords.isEmpty()) {
            cs.find(interest, callback);
        } else {
            //Pitにあれば，CSには無いので，CSにない場合の処理へいく，
            callback.onContentStoreMiss(interest);

        }
    }

    /**
     * Interestパケット送信時のメソッドです．
     * pit中にあるレコードから，元タスク（子供）interestを取り出して,
     * prefixを親に変更してからそれを送信する．
     *
     *
     * @param pitEntry
     * @param outFace
     * @param wantNewNonce
     */
    @Override
    public void onOutgoingInterest(PitEntry pitEntry, Face outFace, boolean wantNewNonce) {
        super.onOutgoingInterest(pitEntry, outFace, wantNewNonce);
    }



    public void onOutgoingInterest( NFDTask fromTask, NFDTask toTask, NCLWData data, PitEntry pitEntry, Face outFace, boolean wantNewNonce) {
        //super.onOutgoingInterest(pitEntry, outFace, wantNewNonce);
        if (outFace.getFaceId() == FaceTable.INVALID_FACEID) {
            NclwLog.getIns().log("***ERROR: Invalid FaceID***");
            return;
        }


        // scope control
        if (pitEntry.violatesScope(outFace)) {
            NclwLog.getIns().log("***ERROR: Invalid Scope***");

            return;
        }

        // pick Interest

        List<PitInRecord> inRecords = pitEntry.getInRecords();
        if (inRecords == null || inRecords.isEmpty()) {
            NclwLog.getIns().log("***ERROR: inRecords Empty***");

            return;

        }
        long smallestLastRenewed = Long.MAX_VALUE;
        boolean smallestIsOutFace = true;
        PitInRecord pickedInRecord = null;
        for (PitInRecord one : inRecords) {
            boolean currentIsOutFace = one.getFace().equals(outFace);
            if (!smallestIsOutFace && currentIsOutFace) {
                continue;
            }
            if (smallestIsOutFace && !currentIsOutFace) {
                smallestLastRenewed = one.getLastRenewed();
                smallestIsOutFace = currentIsOutFace;
                pickedInRecord = one;
                continue;
            }
            if (smallestLastRenewed > one.getLastRenewed()) {
                smallestLastRenewed = one.getLastRenewed();
                smallestIsOutFace = currentIsOutFace;
                pickedInRecord = one;
            }
        }
        if (pickedInRecord == null) {
            NclwLog.getIns().log("***ERROR: PickedInRecord Null***");

            return;

        }
        Interest interest = pickedInRecord.getInterest();

        //Interest更新．
        //Name targetName = new Name(NCLWUtil.NCLW_PREFIX+targetTask.getCmd()/*+ Math.floor(Math.random() * 100000)*/);
        Name targetName = NclwNFDMgr.getIns().createPrefix(fromTask, toTask);
        Interest newInterest = new Interest(targetName);
        newInterest.setApplicationParameters(new Blob(data.getAllBytes()));
        NclwLog.getIns().log("INTEREST generated: SF"+toTask.getTaskID() + "->SF" + fromTask.getTaskID()  + "/Prefix:"+targetName.toUri());


        newInterest.setName(targetName);

        if (wantNewNonce) {
            //generate and set new nonce
            Random randomGenerator = new Random();
            byte bytes[] = new byte[4];
            randomGenerator.nextBytes(bytes);
            // notice that this is right, if we set the nonce first, the jndn
            // library will not change the nonce
            interest.setNonce(new Blob(bytes));
        }

        // insert OutRecord
        pitEntry.insertOrUpdateOutRecord(outFace, interest);

        NclwLog.getIns().log("-----INTEREST Go: From:"+outFace.getLocalUri()+"To/:"+outFace.getRemoteUri());
/*
        NclwNFDSendThread sender = new NclwNFDSendThread();
        Thread sendThread = new Thread(sender);
        sendThread.start();
*/
        if(NCLWUtil.ccn_comm_mode == 0){
            outFace.sendInterest(newInterest);

        }else{
            this.sendInterest(newInterest, (TcpFace)outFace);
        }
        //NclwNFDMgr.getIns().getChannel().

       /* try{

            String remoteAddr = outFace.getRemoteUri().getInet().getHostAddress();
            NclwNFDMgr.getIns().getMgr().getPfactory().destroyFace(outFace);
            //まだ受信まちする．
            NclwNFDMgr.getIns().getMgr().getPfactory().createFace(new FaceUri("tcp4", remoteAddr, NCLWUtil.NFD_PORT));

        }catch(Exception e){
            e.printStackTrace();
        }
*/



        //this.sender.getInterestDataQueue().add(data);


    }

    /**
     * データ到着時に呼ばれるメソッド
     *
     * Dataが来て，そこからNCLWDataを取り出す．
     * そして，TaskのindataMapにput
     * 処理をして，結果をDataとして送ります．
     * PITから出力先のFaceを取得して，Dataをそれに向けて送ります．
     * 最終的にはonOutgoingDataを呼び出して送信する．
     * @param inFace Dataがやってきたface
     * @param data
     */
    @Override
    protected void onIncomingData(Face inFace, Data data) {
System.out.println("***Data COME!!****");

        if(data.getName().toUri().startsWith(NCLWUtil.NCLW_PREFIX)){


            // /localhost scope control
            boolean isViolatingLocalhost = !inFace.isLocal()
                    && LOCALHOST_NAME.match(data.getName());
            if (isViolatingLocalhost) {
                // (drop)
                return;
            }


            NCLWData nData = NclwNFDMgr.getIns().fetchNCLWData(data);
            WorkflowJob job = nData.getJob();
            //NFDTask toTask = job.getNfdTaskMap().get(nData.getToTaskID());
            SFC sfc = nData.getSfc();
           // NclwNFDMgr.getIns().getFromFaceMap().put(job.getJobID()+"^"+nData.getFromTaskID(), (TcpFace)inFace);

            NFDTask toTask;
            String prefix = job.getJobID()+"^"+nData.getToTaskID();
            HashMap<String, NFDTask> taskPool = NFDTaskEngine.getIns().getTaskPool();
            if(taskPool.containsKey(prefix)){
                toTask = (NFDTask)NFDTaskEngine.getIns().getTaskPool().get(prefix);
            }else{
                toTask = (NFDTask)job.getNfdTaskMap().get(nData.getToTaskID());
                NFDTaskEngine.getIns().getTaskPool().put(prefix, toTask);
            }


            //System.out.println("Data arrived:"+data.getFromTaskID() + "->"+data.getToTaskID());
            Thread taskThread = new Thread(toTask);
            toTask.setVnf(sfc.findVNFByLastID(nData.getToTaskID()));
            toTask.setJobID(job.getJobID());
            toTask.setJob(job);
            toTask.setSfc(sfc);
            toTask.setEnv(toTask.getEnv());
            if(!toTask.isStarted()){
                taskThread.start();

            }
            //nData.setInFace(inFace);
            toTask.getInDataMap().put(nData.getFromTaskID(), nData);


        }else{
            super.onIncomingData(inFace, data);
        }


    }

    @Override
    public void addFace(Face face) {
        super.addFace(face);
        NclwNFDMgr.getIns().addFIBEntryCallBack((TcpFace)face);
    }

    public boolean addCSEntry(NCLWData  nData){
        //宛先タスクのIDを取り出す．
        SFC sfc = nData.getSfc();
        WorkflowJob job = nData.getJob();
        NFDTask fromTask = job.getNfdTaskMap().get(nData.getFromTaskID());
        NFDTask toTask = job.getNfdTaskMap().get(nData.getToTaskID());
        //Face inFace = nData.getInFace();

        //当該タスクのからNameを生成する．
        Name fromName = NclwNFDMgr.getIns().createPrefix(fromTask, toTask);
        //toIDを見て，toTaskからPrefixを生成する．
        //そのPrefixがPITにあるかどうか
        // PIT match
        Data data = new Data();
        data.setName(fromName);
        //NCLWDataをバイナリ変換してdataへセットする．
        byte[] bs = nData.getAllBytes();
        data.setContent(new Blob(bs));
        cs.insert(data, false);

        return true;
    }
    /**
     * Task処理後に，Dataを送り出す処理です．
     * 当該タスクのprefixがPITにあるかどうかをチェックし，
     * もしあれば，そのpitエントリ全部に対して結果を送る．
     *
     * nDataには，宛先タスクID, 宛先ホストIPが必要．
     * ipAddrは，宛先タスクが割り当てられたホストのIP
     *
     * @param nData
     */
    public void processSendData(NCLWData nData){

        //宛先タスクのIDを取り出す．
        SFC sfc = nData.getSfc();
        WorkflowJob job = nData.getJob();
        NFDTask fromTask = job.getNfdTaskMap().get(nData.getFromTaskID());
        NFDTask toTask = job.getNfdTaskMap().get(nData.getToTaskID());
        //Face inFace = nData.getInFace();

        //当該タスクのからNameを生成する．
        Name fromName = NclwNFDMgr.getIns().createPrefix(fromTask, toTask);
        //toIDを見て，toTaskからPrefixを生成する．
        //そのPrefixがPITにあるかどうか
        // PIT match
        Data data = new Data();
        data.setName(fromName);
        //NCLWDataをバイナリ変換してdataへセットする．
        byte[] bs = nData.getAllBytes();
        data.setContent(new Blob(bs));
System.out.println("**PIT size:"+pit.size());
        List<List<PitEntry>> pitMatches = pit.findAllMatches(data);

        if (pitMatches == null || pitMatches.isEmpty()) {
System.out.println("**PIT No Match!!**");
            // goto Data unsolicited pipeline
            if(fromTask.getInDataMap().isEmpty()){

            }else{
                if(!fromTask.getInDataMap().isEmpty()){

                    ///TcpFace inFace = NclwNFDMgr.getIns().getFromFaceMap().get(job.getJobID()+"^"+fromTask.getTaskID());
                    //onDataUnsolicited(inFace, data);
                }


            }
            if(NCLWUtil.ccn_routing == 0){
                return;
            }
        }
        Iterator<List<PitEntry>> pIte = pitMatches.iterator();
        while(pIte.hasNext()){
            List<PitEntry> pl = pIte.next();
            Iterator<PitEntry> plIte = pl.iterator();
            while(plIte.hasNext()){
                PitEntry pe = plIte.next();

                System.out.println("**Pit Name:"+pe.getName().toUri());
                Iterator<PitInRecord> pirIte = pe.getInRecords().iterator();
                while(pirIte.hasNext()){
                    PitInRecord pir = pirIte.next();

                    System.out.println("***Pit IN_Record:Name:"+pir.getInterest().getName().toUri()+"/LocalURI:"+pir.getFace().getLocalUri() +"/RemoteURI:"+ pir.getFace().getRemoteUri());
                }
                Iterator<PitOutRecord> pioIte = pe.getOutRecords().iterator();
                while(pioIte.hasNext()){
                    PitOutRecord pio = pioIte.next();
                    System.out.println("***Pit OUT_Record:LocalURI:"+pio.getFace().getLocalUri() +"/RemoteURI:"+ pio.getFace().getRemoteUri());

                }

            }

        }

        //logger.log(Level.INFO, "{0} PIT entry list(s) found for {1}",
        //new Object[]{pitMatches.size(), data.getName().toUri()});
        // CS insert
        //CSに入れるためだけに生成されるDataオブジェクト．
        //Nameにおいて，ヒットさせるために/nclw/job/from/toの部分
        //を取り除いておく．
        /*Data csData = new Data();
        csData.setContent(data.getContent());
        Name csName = NclwNFDMgr.getIns().trimName(fromName);
        csData.setName(csName);
*/
        cs.insert(data, false);

        //logger.log(Level.INFO, "Data inserting to CS succeed for {0}", data.getName().toUri());
        Set<Face> pendingDownstreams = new HashSet<>();
        // foreach PitEntry
        for (List<PitEntry> oneList : pitMatches) {

            for (PitEntry onePitEntry : oneList) {
                System.out.println("***Pit Matched to :"+onePitEntry.getName().toUri());
                // cancel unsatisfy & straggler timer
                cancelUnsatisfyAndStragglerTimer(onePitEntry);

                // remember pending downstreams
                List<PitInRecord> inRecords = onePitEntry.getInRecords();

                //logger.log(Level.INFO, "{0} Pit Inrecord(s) found in the list for {1}",
                //new Object[]{inRecords.size(), data.getName().toUri()});
                for (PitInRecord oneInRecord : inRecords) {

                    //logger.log(Level.INFO, "The remaining lifetime for the inRecord is {0}",
                    //	(oneInRecord.getExpiry() - System.currentTimeMillis()));
                    if(oneInRecord.getInterest().getName().toUri().startsWith(NCLWUtil.NCLW_PREFIX)){
                        pendingDownstreams.add(oneInRecord.getFace());

                    }else{
                        if (oneInRecord.getExpiry() > System.currentTimeMillis()) {
                            pendingDownstreams.add(oneInRecord.getFace());
                        }
                    }

                }

                // invoke PIT satisfy callback
                Strategy effectiveStrategy
                        = strategyChoice.findEffectiveStrategy(onePitEntry.getName());

                // Dead Nonce List insert if necessary (for OutRecord of inFace)


                // mark PIT satisfied
                NFVEnvironment env = nData.getEnv();
                VNF fromVNF = sfc.findVNFByLastID(fromTask.getTaskID());
                if(toTask != null){
                    VNF toVNF = sfc.findVNFByLastID(toTask.getTaskID());

                    VM toHost = NCLWUtil.findVM(env, toVNF.getvCPUID());
                    VM fromHost = NCLWUtil.findVM(env, fromVNF.getvCPUID());
                    if(toHost.getIpAddr().equals(fromHost.getIpAddr())){

                    }else{
                        onePitEntry.deleteInRecords();
                    }
                }else{
                    onePitEntry.deleteInRecords();

                }


                /*if(!fromTask.getInDataMap().isEmpty()){
                    //TcpFace inFace = NclwNFDMgr.getIns().getToFaceMap().get(job.getJobID()+"^"+fromTask.getTaskID());

                    onePitEntry.deleteOutRecord(inFace);

                }*/

                // set PIT straggler timer
                setStragglerTimer(onePitEntry, true,
                      (long) Math.round(data.getMetaInfo().getFreshnessPeriod()));
            }
        }

        for (Face one : pendingDownstreams) {
            System.out.println(one.getRemoteUri());
            // goto outgoing Data pipeline
           this.onOutgoingData(data, one);

        }


    }




    protected class NclwForwardingPipelineSearchCallback extends ForwardingPipelineSearchCallback {

        protected  Face face;
        protected  PitEntry pitEntry;

        public NclwForwardingPipelineSearchCallback(Face face, PitEntry pitEntry) {
            super(face, pitEntry);
            this.face = face;
            this.pitEntry = pitEntry;
        }

        /**
         * CSにヒットしたときに呼ばれる処理です．Dataを送ります．
         *      * 最終的にはonOutgoingDataを呼び出して送信する．
         * @param interest
         * @param data
         */
        @Override
        public void onContentStoreHit(Interest interest, Data data) {
            super.onContentStoreHit(interest, data);
        }

        public PitEntry getPitEntry() {
            return pitEntry;
        }

        public void setPitEntry(PitEntry pitEntry) {
            this.pitEntry = pitEntry;
        }

        /**
         * CSにヒットしなかったときに呼ばれる処理
         *
         *
         * @param interest
         */
        @Override
        public void onContentStoreMiss(Interest interest) {
            //logger.info("onContentStoreMiss");
            NclwLog.getIns().log("CS no Hit. Then add the prefix to PIT.");
            // Pitへエントリを追加する．
            NclwLog.getIns().log("PIT ADD:"+interest.getName().toUri());
            pitEntry.insertOrUpdateInRecord(face, interest);
            if(interest.getName().toUri().startsWith(NCLWUtil.NCLW_PREFIX)){

            }else{
                // set PIT unsatisfy timer
                setUnsatisfyTimer(pitEntry);
            }

                // set PIT unsatisfy timer
           // setUnsatisfyTimer(pitEntry);

            // FIBからエントリを探す．
            FibEntry fibEntry = fib.findLongestPrefixMatch(pitEntry.getName());
            if(fibEntry == null){
                //FIBにも無ければ，エントリを作る．
                fibEntry = new FibEntry(interest.getName());
            }

            ////logger.log(Level.INFO, "find fib{0}", fibEntry.getNextHopList().get(0).getFace().toString());
            // dispatch to strategy
            Strategy effectiveStrategy
                    = strategyChoice.findEffectiveStrategy(pitEntry.getName());

            effectiveStrategy.afterReceiveInterest(face, interest, fibEntry,
                    pitEntry);
        }
    }



    /**
     * dataを
     * 送り返す処理
     * CSにヒット = 入力＋ファンクション名が一致していること．
     * したがって，コンテンツ = 入力＋ファンクションというprefixなので，
     * データ = 入力＋ファンクション名による処理結果データ，ということ．
     *
     * @param data
     * @param outFace
     */
    @Override
    protected void onOutgoingData(Data data, Face outFace) {

        //logger.log(Level.INFO, "onOutgoingData {0}", data.getName().toUri());
        if (outFace.getFaceId() == FaceTable.INVALID_FACEID) {
            return;
        }

        // /localhost scope control
        boolean isViolatingLocalhost = !outFace.isLocal()
                && LOCALHOST_NAME.match(data.getName());
        if (isViolatingLocalhost) {
            // (drop)
            return;
        }
       // System.out.println("476");

        System.out.println("Resultant Data is Ready for Sending to :"+outFace.getRemoteUri().getInet().getHostAddress());
       // outFace.sendData(data);
        this.processOutData(data, (TcpFace)outFace);
    }

    /**
     * Interest送信プロセスです．
     * 毎回，socket生成して送信します．
     * @param interest
     * @param face
     */
    public void sendInterest(Interest interest, TcpFace face) {
        try {
            String ipAddr = face.getRemoteUri().getInet().getHostAddress();

            // ソケットの準備
            Socket socket = new Socket(ipAddr, NCLWUtil.NFD_PORT);
            ObjectOutputStream outStrm = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream inStrm = new ObjectInputStream(socket.getInputStream());
            NclwInterest newInterest = new NclwInterest(interest.getName().toUri(),
            interest.getApplicationParameters().getImmutableArray());
            outStrm.writeObject(newInterest);
            NclwLog.getIns().log("INTEREST is Sent successfully.");

            outStrm.flush();
        }catch(Exception e){
            e.printStackTrace();
        }
    }


    public void processOutData(Data inData, TcpFace face){

        NCLWData data = NclwNFDMgr.getIns().fetchNCLWData(inData);
        //PITのIPアドレスを設定する．
        data.setPitIPAddr(face.getRemoteUri().getInet().getHostAddress());
        boolean isFile = data.isFile();
        String readPath = data.getReadFilePath();
        String ipAddr;
        //今度はチェック
        if(data.getToTaskID() == -1){
            ipAddr = NCLWUtil.delegator_ip;
        }else{
             ipAddr = face.getRemoteUri().getInet().getHostAddress();

        }

        int port = NCLWUtil.port;

        NFVEnvironment env = data.getEnv();
        SFC sfc = data.getSfc();
        VNF srcVNF = sfc.findVNFByLastID(data.getFromTaskID());

        try{

            // ソケットの準備
            Socket socket = new Socket(ipAddr, port);
            ObjectOutputStream outStrm=new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream inStrm=new ObjectInputStream(socket.getInputStream());
            if(isFile){
                File file = new File(readPath);
                data.setFile(file);
                if(srcVNF != null){
                    VM srcVM = NCLWUtil.findVM(env, srcVNF.getvCPUID());
                    String srcIP = srcVM.getIpAddr();
                    if(srcIP.equals(data.getIpAddr())){
                        data.setIpAddr("localhost");
                    }
                }

                FileInputStream is = new FileInputStream(file);
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                while(true) {
                    int len = is.read(buffer);
                    if(len < 0) {
                        break;
                    }
                    bout.write(buffer, 0, len);

                }
                data.setBytes(bout.toByteArray());

                //return bout.toByteArray();


                // FileInputStream fis = new FileInputStream(file);
                //data.setFis(fis);
            }else{

            }
            outStrm.writeObject(data);

            outStrm.flush();
            // System.out.println("Sent data");
        }catch(Exception e){
            e.printStackTrace();
        }

    }
}
