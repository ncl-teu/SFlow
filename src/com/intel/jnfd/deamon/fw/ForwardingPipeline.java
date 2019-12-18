/*
 * jndn-forwarder
 * Copyright (c) 2015, Intel Corporation.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the GNU Lesser General Public License,
 * version 3, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details.
 */
package com.intel.jnfd.deamon.fw;

import com.intel.jndn.forwarder.api.ContentStore;
import com.intel.jndn.forwarder.api.Face;
import com.intel.jndn.forwarder.api.FaceInformationBase;
import com.intel.jndn.forwarder.api.PendingInterestTable;
import com.intel.jndn.forwarder.api.Strategy;
import com.intel.jndn.forwarder.impl.RegisterPrefixCommand;
import com.intel.jnfd.deamon.table.cs.SearchCsCallback;
import com.intel.jnfd.deamon.table.cs.SortedSetCs;
import com.intel.jnfd.deamon.table.deadnonce.DeadNonce;
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
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;

/**
 *
 * @author Haitao Zhang <zhtaoxiang@gmail.com>
 */
public class ForwardingPipeline {

	public static  Name LOCALHOST_NAME = new Name("ndn:/localhost");

	public ForwardingPipeline(ScheduledExecutorService scheduler) {
		//TODO: find a right way to initialize the shcheduler
	/*	this.scheduler = scheduler;

		faceTable = new FaceTable(this);
		pit = new Pit();
		fib = new Fib();
		cs = new SortedSetCs();


		measurement = new Measurement();
		strategyChoice = new StrategyChoice(new BestRouteStrategy2(this));
		// install more strategies into strategyChoice here
		deadNonceList = new DeadNonceNaive(scheduler);
		prefixRegistration = new RegisterPrefixCommand();
		logger.setLevel(NfdCommon.LOG_LEVEL);

	 */
	}

	public FaceTable getFaceTable() {
		return faceTable;
	}

	public Face getFace(int faceId) {
		return faceTable.get(faceId);
	}

	public void addFace(Face face) {
		faceTable.add(face);
	}

	public void removeFace(Face face, String reason) {
		faceTable.remove(face, reason);
	}

	public void onInterest(Face face, Interest interest) {
		onIncomingInterest(face, interest);
	}

	public void onData(Face face, Data data) {
		onIncomingData(face, data);
	}

	public FaceInformationBase getFib() {
		return fib;
	}

	public void setFib(FaceInformationBase fib) {
		this.fib = fib;
	}

	public PendingInterestTable getPit() {
		return pit;
	}

	public void setPit(PendingInterestTable pit) {
		this.pit = pit;
	}

	public ContentStore getCs() {
		return cs;
	}

	public void setCs(ContentStore cs) {
		this.cs = cs;
	}

	public Measurement getMeasurement() {
		return measurement;
	}

	public StrategyChoice getStrategyChoice() {
		return strategyChoice;
	}

	public DeadNonce getDeadNonceList() {
		return deadNonceList;
	}

	/**
	 * incoming Interest pipeline
	 *
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
		ForwardingPipelineSearchCallback callback = new ForwardingPipelineSearchCallback(inFace, pitEntry);
		if (inRecords == null || inRecords.isEmpty()) {
			cs.find(interest, callback);
		} else {
			callback.onContentStoreMiss(interest);
		}
	}

	protected class ForwardingPipelineSearchCallback implements SearchCsCallback {

		protected  Face face;
		protected  PitEntry pitEntry;

		public ForwardingPipelineSearchCallback(Face face, PitEntry pitEntry) {
			this.face = face;
			this.pitEntry = pitEntry;
		}

		/**
		 * Callback to handle CS misses
		 *
		 * @param interest
		 */
		@Override
		public void onContentStoreMiss(Interest interest) {
			logger.info("onContentStoreMiss");
			// insert InRecord
			pitEntry.insertOrUpdateInRecord(face, interest);
			// set PIT unsatisfy timer
			setUnsatisfyTimer(pitEntry);
			// FIB lookup
			FibEntry fibEntry = fib.findLongestPrefixMatch(pitEntry.getName());
			if(fibEntry == null){
				fibEntry = new FibEntry(interest.getName());
			}
			////logger.log(Level.INFO, "find fib{0}", fibEntry.getNextHopList().get(0).getFace().toString());
			// dispatch to strategy
			Strategy effectiveStrategy
					= strategyChoice.findEffectiveStrategy(pitEntry.getName());
			effectiveStrategy.afterReceiveInterest(face, interest, fibEntry,
					pitEntry);
		}

		/**
		 * Callback to handle CS hits
		 *
		 * @param interest
		 * @param data
		 */
		@Override
		public void onContentStoreHit(Interest interest, Data data) {
			logger.info("content store hit");
			// TODO: in the c++ code, they set the incoming FaceId, but jndn does
			// not provide similiar function. Need to find a solution
			// data.setIncomingFaceId();

			// set PIT straggler timer
			setStragglerTimer(pitEntry, true,
					(long) Math.round(data.getMetaInfo().getFreshnessPeriod()));
			// goto outgoing Data pipeline
			onOutgoingData(data, face);
		}
	}

	/**
	 *
	 * Interest loop pipeline
	 *
	 * @param inFace
	 * @param interest
	 * @param pitEntry
	 */
	protected void onInterestLoop(Face inFace, Interest interest, PitEntry pitEntry) {
		logger.info("interest loop");
        // Do nothing

        // TODO: drop the interest. Since the c++ code hasn't implemented this 
		// method, we will also omit it here
	}

	/**
	 * outgoing Interest pipeline
	 *
	 * @param pitEntry
	 * @param outFace
	 * @param wantNewNonce
	 */
	public void onOutgoingInterest(PitEntry pitEntry, Face outFace,
			boolean wantNewNonce) {
		if (outFace.getFaceId() == FaceTable.INVALID_FACEID) {
			return;
		}

		// scope control
		if (pitEntry.violatesScope(outFace)) {
			return;
		}

		// pick Interest
		List<PitInRecord> inRecords = pitEntry.getInRecords();
		if (inRecords == null || inRecords.isEmpty()) {
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
			return;
		}
		Interest interest = pickedInRecord.getInterest();
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

		outFace.sendInterest(interest);
	}

	/**
	 * Interest reject pipeline
	 *
	 * @param pitEntry
	 */
	public void onInterestReject(PitEntry pitEntry) {

		if (pitEntry.hasUnexpiredOutRecords()) {
			return;
		}

		// cancel unsatisfy & straggler timer
		cancelUnsatisfyAndStragglerTimer(pitEntry);

		// set PIT straggler timer
		setStragglerTimer(pitEntry, false, -1);
	}

	/**
	 * Interest unsatisfied pipeline
	 *
	 * @param pitEntry
	 */
	protected void onInterestUnsatisfied(PitEntry pitEntry) {
		//logger.log(Level.INFO, "onInterestUnsatisfied for Interest {0}", pitEntry.getName().toUri());
		Strategy effectiveStrategy
				= strategyChoice.findEffectiveStrategy(pitEntry.getName());
		effectiveStrategy.beforeExpirePendingInterest(pitEntry);

		// goto Interest ize pipeline
		onInterestize(pitEntry, false, -1);
	}

	/**
	 * Interest ize pipeline
	 *
	 * @param pitEntry
	 * @param isSatisfied
	 * @param dataFreshnessPeriod
	 */
	protected void onInterestize(PitEntry pitEntry, boolean isSatisfied,
			long dataFreshnessPeriod) {
		//logger.log(Level.INFO, "onInterestize for Interest {0}", pitEntry.getName().toUri());
		// Dead Nonce List insert if necessary
		insertDeadNonceList(pitEntry, isSatisfied, dataFreshnessPeriod, null);

		// PIT delete
		cancelUnsatisfyAndStragglerTimer(pitEntry);
		pit.erase(pitEntry);
	}

	/**
	 * incoming Data pipeline
	 *
	 * @param inFace
	 * @param data
	 */
	protected void onIncomingData(Face inFace, Data data) {
// TODO: in the c++ code, they set the incoming FaceId, but jndn does
// not provide similiar function. Need to find a solution
// data.setIncomingFaceId();
//        data.setIncomingFaceId();
		//logger.log(Level.INFO, "on incomingdata {0}", data.getName().toUri());

		// /localhost scope control
		boolean isViolatingLocalhost = !inFace.isLocal()
				&& LOCALHOST_NAME.match(data.getName());
		if (isViolatingLocalhost) {
			// (drop)
			return;
		}

		// PIT match
		List<List<PitEntry>> pitMatches = pit.findAllMatches(data);
		if (pitMatches == null || pitMatches.isEmpty()) {
			// goto Data unsolicited pipeline
			onDataUnsolicited(inFace, data);
			return;
		}
		//logger.log(Level.INFO, "{0} PIT entry list(s) found for {1}",
				//new Object[]{pitMatches.size(), data.getName().toUri()});
		// CS insert
		cs.insert(data, false);

		//logger.log(Level.INFO, "Data inserting to CS succeed for {0}", data.getName().toUri());
		Set<Face> pendingDownstreams = new HashSet<>();
		// foreach PitEntry
		for (List<PitEntry> oneList : pitMatches) {
			//logger.log(Level.INFO, "{0} Pit entry(ies) found in the list for {1}",
					//new Object[]{oneList.size(), data.getName().toUri()});
			for (PitEntry onePitEntry : oneList) {
				// cancel unsatisfy & straggler timer
				cancelUnsatisfyAndStragglerTimer(onePitEntry);

				// remember pending downstreams
				List<PitInRecord> inRecords = onePitEntry.getInRecords();
				//logger.log(Level.INFO, "{0} Pit Inrecord(s) found in the list for {1}",
						//new Object[]{inRecords.size(), data.getName().toUri()});
				for (PitInRecord oneInRecord : inRecords) {
					//logger.log(Level.INFO, "The remaining lifetime for the inRecord is {0}",
						//	(oneInRecord.getExpiry() - System.currentTimeMillis()));
					if (oneInRecord.getExpiry() > System.currentTimeMillis()) {
						pendingDownstreams.add(oneInRecord.getFace());
					}
				}

				// invoke PIT satisfy callback
				Strategy effectiveStrategy
						= strategyChoice.findEffectiveStrategy(onePitEntry.getName());
				effectiveStrategy.beforeSatisfyInterest(onePitEntry, inFace, data);

				// Dead Nonce List insert if necessary (for OutRecord of inFace)
				insertDeadNonceList(onePitEntry, true,
						(long) Math.round(data.getMetaInfo().getFreshnessPeriod()),
						inFace);

				// mark PIT satisfied
				onePitEntry.deleteInRecords();
				onePitEntry.deleteOutRecord(inFace);

				// set PIT straggler timer
				setStragglerTimer(onePitEntry, true,
						(long) Math.round(data.getMetaInfo().getFreshnessPeriod()));
			}
		}

		//logger.log(Level.INFO, "Find {0} downstream(s) faces for {1}",
				//new Object[]{pendingDownstreams.size(), data.getName().toUri()});
		// foreach pending downstream
		for (Face one : pendingDownstreams) {
			//logger.log(Level.INFO, "downstream faces for {0} is {1}",
					//new Object[]{data.getName().toUri(), one.toString()});
			if (inFace.equals(one)) {
				continue;
			}
			// goto outgoing Data pipeline
			onOutgoingData(data, one);
		}
	}

	/**
	 * Data unsolicited pipeline
	 *
	 * @param inFace
	 * @param data
	 */
	protected void onDataUnsolicited(Face inFace, Data data) {
		//logger.log(Level.INFO, "onDataUnsolicited{0}", data.getName().toUri());
		// accept to cache?
		boolean acceptToCache = inFace.isLocal();
		if (acceptToCache) {
			// CS insert
			cs.insert(data, true);
		}
	}

	/**
	 * outgoing Data pipeline
	 *
	 * @param data
	 * @param outFace
	 */
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

	protected void setUnsatisfyTimer( PitEntry pitEntry) {
		//logger.log(Level.INFO, "setUnsatisfyTimer is called for {0}", pitEntry.getName().toUri());
		List<PitInRecord> inRecords = pitEntry.getInRecords();
		long lastExpiry = 0;
		for (PitInRecord one : inRecords) {
			if (lastExpiry < one.getExpiry()) {
				lastExpiry = one.getExpiry();
			}
		}
		long lastExpiryFromNow = lastExpiry - System.currentTimeMillis();
		if (lastExpiryFromNow < 0) {
            // TODO: this message is copied from c++ code
			// all InRecords are already expired; will this happen?
			onInterestUnsatisfied(pitEntry);
		}

		if (pitEntry.unsatisfyTimer != null) {
			pitEntry.unsatisfyTimer.cancel(false);
		}
		pitEntry.unsatisfyTimer = scheduler.schedule(new Runnable() {

			@Override
			public void run() {
                // TODO: make sure if we need to change the pointer pitEntry or 
				// not.
				// Since it is an inner class, we cannot change the pointer.
				onInterestUnsatisfied(pitEntry);
			}
		},
				lastExpiryFromNow, TimeUnit.MILLISECONDS);
	}

	protected void setStragglerTimer( PitEntry pitEntry,
			 boolean isSatisfied,  long dataFreshnessPeriod) {
		long stragglerTime = 100;
		if (pitEntry.stragglerTimer != null) {
			pitEntry.stragglerTimer.cancel(false);
		}
		pitEntry.stragglerTimer = scheduler.schedule(new Runnable() {

			@Override
			public void run() {
				onInterestize(pitEntry, isSatisfied, dataFreshnessPeriod);
			}

		},
				stragglerTime, TimeUnit.MILLISECONDS);
	}

	protected void cancelUnsatisfyAndStragglerTimer(PitEntry pitEntry) {
		if (pitEntry.unsatisfyTimer != null) {
			pitEntry.unsatisfyTimer.cancel(false);
		}
		if (pitEntry.stragglerTimer != null) {
			pitEntry.stragglerTimer.cancel(false);
		}
	}

	protected void insertDeadNonceList(PitEntry pitEntry, boolean isSatisfied,
			long dataFreshnessPeriod, Face upstream) {
		// need Dead Nonce List insert?
		boolean needDnl = false;
		if (isSatisfied) {
			boolean hasFreshnessPeriod = dataFreshnessPeriod >= 0;
			// Data never becomes stale if it doesn't have FreshnessPeriod field
			needDnl = pitEntry.getInterest().getMustBeFresh()
					&& (hasFreshnessPeriod
					&& dataFreshnessPeriod < deadNonceList.getLifetime());
		} else {
			needDnl = true;
		}

		if (!needDnl) {
			return;
		}

		// Dead Nonce List insert
		if (upstream == null) {
			// insert all outgoing Nonces
			List<PitOutRecord> outRecords = pitEntry.getOutRecords();
			for (PitOutRecord one : outRecords) {
				deadNonceList.add(pitEntry.getName(), one.getLastNonce());
			}
		} else {
			// insert outgoing Nonce of a specific face
			PitOutRecord outRecord = pitEntry.getOutRecord(upstream);
			if (outRecord != null) {
				deadNonceList.add(pitEntry.getName(), outRecord.getLastNonce());
			}
		}
	}

    //    protected void dispatchToStrategy(PitEntry pitEntry, Trigger trigger) {
	//        Strategy effectiveStrategy
	//                = strategyChoice.findEffectiveStrategy(pitEntry.getName());
	//        trigger.trigger(effectiveStrategy);
	//    }
	protected static  Logger logger = Logger.getLogger(ForwardingPipeline.class.getName());
	protected  FaceTable faceTable;
	protected FaceInformationBase fib;
	protected PendingInterestTable pit;
	protected ContentStore cs;
	protected  Measurement measurement;
	protected  StrategyChoice strategyChoice;
	protected  DeadNonce deadNonceList;
	protected  ScheduledExecutorService scheduler;
	protected  RegisterPrefixCommand prefixRegistration;
}
