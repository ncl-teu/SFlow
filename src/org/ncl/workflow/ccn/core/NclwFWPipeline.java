package org.ncl.workflow.ccn.core;

import com.intel.jndn.forwarder.api.Face;
import com.intel.jndn.forwarder.api.Strategy;
import com.intel.jndn.forwarder.impl.RegisterPrefixCommand;
import com.intel.jnfd.deamon.fw.BestRouteStrategy2;
import com.intel.jnfd.deamon.fw.FaceTable;
import com.intel.jnfd.deamon.fw.ForwardingPipeline;
import com.intel.jnfd.deamon.table.cs.SearchCsCallback;
import com.intel.jnfd.deamon.table.cs.SortedSetCs;
import com.intel.jnfd.deamon.table.deadnonce.DeadNonceNaive;
import com.intel.jnfd.deamon.table.fib.Fib;
import com.intel.jnfd.deamon.table.fib.FibEntry;
import com.intel.jnfd.deamon.table.measurement.Measurement;
import com.intel.jnfd.deamon.table.pit.Pit;
import com.intel.jnfd.deamon.table.pit.PitEntry;
import com.intel.jnfd.deamon.table.pit.PitInRecord;
import com.intel.jnfd.deamon.table.strategy.StrategyChoice;
import com.intel.jnfd.util.NfdCommon;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;
import org.ncl.workflow.ccn.sfc.strategy.AutoICNSFCStrategy;
import org.ncl.workflow.ccn.sfc.strategy.BackTrackStrategy;
import org.ncl.workflow.util.NCLWUtil;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by Hidehiro Kanemitsu on 2019/11/15
 */
public class NclwFWPipeline extends ForwardingPipeline {

    public NclwFWPipeline(ScheduledExecutorService scheduler) {
        super(scheduler);
        //TODO: find a right way to initialize the shcheduler
        this.scheduler = scheduler;

        faceTable = new FaceTable(this);
        pit = new Pit();
        fib = new Fib();
        cs = new SortedSetCs();

        measurement = new Measurement();

        switch(NCLWUtil.nfd_strategy){
            case 0:
                strategyChoice = new StrategyChoice(new BestRouteStrategy2(this));

                break;
            case 1:
                strategyChoice = new StrategyChoice(new BackTrackStrategy(this));

                break;
            case 2:
                strategyChoice = new StrategyChoice(new AutoICNSFCStrategy(this));
                break;

            default:
                strategyChoice = new StrategyChoice(new BackTrackStrategy(this));

                break;
        }
        // install more strategies into strategyChoice here
        deadNonceList = new DeadNonceNaive(scheduler);
        prefixRegistration = new RegisterPrefixCommand();
        logger.setLevel(NfdCommon.LOG_LEVEL);
    }

    /**
     * Interestパケットが来た場合の処理です．
     * @param inFace
     * @param interest
     */
    protected void onIncomingInterest(Face inFace, Interest interest) {
// TODO: in the c++ code, they set the incoming FaceId, but jndn does
// not provide similiar function. Need to find a solution
// interest.setIncomingFaceId();
        //logger.log(Level.INFO, "onIncomingInterest: {0}", interest.toUri());
        // localhost scope control
        boolean isViolatingLocalhost = !inFace.isLocal()
                && LOCALHOST_NAME.match(interest.getName());
        if (isViolatingLocalhost) {
            return;
        }

        // handle registration commands; TODO this should be applied as an internal
        // face to the FIB but current interface does not allow this
        if (prefixRegistration.matches(interest.getName())) {
            prefixRegistration.call(interest, inFace, this);
            return;
        }

        // PIT insert
        PitEntry pitEntry = pit.insert(interest).getFirst();

        // detect duplicate Nonce
        int dnw = pitEntry.findNonce(interest.getNonce(), inFace);
        boolean hasDuplicateNonce = (dnw != PitEntry.DUPLICATE_NONCE_NONE)
                || deadNonceList.find(interest.getName(), interest.getNonce());
        if (hasDuplicateNonce) {
            // goto Interest loop pipeline
            onInterestLoop(inFace, interest, pitEntry);
            return;
        }

        // cancel unsatisfy & straggler timer
        cancelUnsatisfyAndStragglerTimer(pitEntry);

        // is pending?
        List<PitInRecord> inRecords = pitEntry.getInRecords();
        ForwardingPipelineSearchCallback callback = new NclwForwardingPipelineSearchCallback(inFace, pitEntry);
        if (inRecords == null || inRecords.isEmpty()) {
            cs.find(interest, callback);
        } else {
            callback.onContentStoreMiss(interest);
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
         * CSにヒットしなかったときに呼ばれる処理
         *
         *
         * @param interest
         */
        @Override
        public void onContentStoreMiss(Interest interest) {
            logger.info("onContentStoreMiss");
            // Pitへエントリを追加する．
            pitEntry.insertOrUpdateInRecord(face, interest);
            // set PIT unsatisfy timer
            setUnsatisfyTimer(pitEntry);
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
     * Interest->CSにヒットした後にdataを
     * 送り返す処理
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

        outFace.sendData(data);
    }
}
