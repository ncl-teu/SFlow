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
package com.intel.jndn.forwarder;

import com.intel.jndn.forwarder.api.FaceInformationBase;
import com.intel.jndn.forwarder.api.PendingInterestTable;
import com.intel.jndn.forwarder.api.ContentStore;
import com.intel.jndn.forwarder.api.Face;
import com.intel.jndn.forwarder.api.FaceManager;
import com.intel.jnfd.deamon.face.FaceUri;
import com.intel.jndn.forwarder.api.ProtocolFactory;
import com.intel.jndn.forwarder.api.Strategy;
import com.intel.jndn.forwarder.api.callbacks.OnCompleted;
import com.intel.jndn.forwarder.api.callbacks.OnDataReceived;
import com.intel.jndn.forwarder.api.callbacks.OnInterestReceived;
import com.intel.jnfd.deamon.fw.ForwardingPipeline;
import com.intel.jnfd.deamon.table.Pair;
import com.intel.jnfd.deamon.table.fib.FibEntry;
import com.intel.jnfd.deamon.table.pit.PitEntry;
import com.intel.jnfd.deamon.table.strategy.StrategyChoiceEntry;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import org.ncl.workflow.ccn.core.NclwFaceManager;
import org.ncl.workflow.ccn.core.NclwNFDMgr;
import org.ncl.workflow.ccn.sfc.process.NclwSFCFWPipeline;

/**
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class Forwarder implements Runnable, OnDataReceived, OnInterestReceived {

	protected  ScheduledExecutorService pool;
	//protected  ForwardingPipeline pipeline;
	protected ForwardingPipeline pipeline;

	protected  FaceManager faceManager;
	protected static final Logger logger = Logger.getLogger(Forwarder.class.getName());

	public Forwarder() {
		pool = Executors.newScheduledThreadPool(4);
		//ここを変える．
		pipeline = new NclwSFCFWPipeline(pool);
		//faceManager = new DefaultFaceManager(pool, pipeline);
		faceManager = new NclwFaceManager(pool, pipeline);
		NclwNFDMgr.getIns().setFw(this);
		NclwNFDMgr.getIns().setMgr((NclwFaceManager)faceManager);
	}


	/**
	 * If new {@link ScheduledExecutorService}, {@link PendingInterestTable},
	 * {@link FaceInformationBase} and {@link ContentStore} need to be tested,
	 * use this constructor.
	 *
	 * @param pool
	 * @param pit
	 * @param fib
	 * @param cs
	 */
	public Forwarder(ScheduledExecutorService pool, PendingInterestTable pit,
			FaceInformationBase fib, ContentStore cs) {
		this.pool = pool;
		pipeline = new NclwSFCFWPipeline(pool);
		faceManager = new NclwFaceManager(pool, pipeline);

		//faceManager = new DefaultFaceManager(pool, pipeline);
		pipeline.setPit(pit);
		pipeline.setFib(fib);
		pipeline.setCs(cs);
		NclwNFDMgr.getIns().setFw(this);
		NclwNFDMgr.getIns().setMgr((NclwFaceManager)faceManager);
		//FaceMgrSingleton.getInst().setFw(this);
		//FaceMgrSingleton.getInst().setMgr((DefaultFaceManager)faceManager);
	}

	/**
	 * New {@link protocolFactory} can be dynamically registered using this
	 * method.
	 *
	 * @param protocolFactory
	 */
	public void registerProtocol(ProtocolFactory protocolFactory) {
		faceManager.registerProtocol(protocolFactory);
	}

	/**
	 * New {@link Strategy} can be dynamically registered using this method.
	 *
	 * @param strategy
	 */
	public void installStrategies(Strategy strategy) {
		pipeline.getStrategyChoice().install(strategy);
	}

	@Override
	public void run() {
		while (true) {

		}
	}

	public void stop() {
		pool.shutdownNow();
	}

	public void addNextHop(final Name prefix, FaceUri uri, final int cost,
			final OnCompleted<FibEntry> onCompleted) {
		createFace(uri, new OnCompleted<Face>() {

			@Override
			public void onCompleted(Face result) {
				if (result instanceof Face) {
					pipeline.addFace((Face) result);
					Pair<FibEntry> fibEntry = pipeline.getFib().insert(prefix, result, cost);
					onCompleted.onCompleted(fibEntry.getFirst());
				}
			}


		});
	}

	public void removeNextHop(Name name, FaceUri uri) {
		pipeline.getFib().remove(name);
	}

	public Collection<FibEntry> listNextHops() {
		return pipeline.getFib().list();
	}

	/**
	 * set the Strategy used by a specific name prefix. Notice, the Strategy
	 * should be installed first.
	 *
	 * @param prefix
	 * @param strategy
	 */
	public void setStrategy(Name prefix, Name strategy) {
		pipeline.getStrategyChoice().insert(prefix, strategy);
	}

	public void unsetStrategy(Name prefix) {
		pipeline.getStrategyChoice().erase(prefix);
	}

	public Collection<StrategyChoiceEntry> listStrategies() {
		return pipeline.getStrategyChoice().list();
	}

	/**
	 * the reference to the face will be returned in the onCompleted
	 *
	 * @param remoteUri
	 * @param onCompleted
	 */
	public void createFace(FaceUri remoteUri, OnCompleted<Face> onFaceCreated) {
		faceManager.createFaceAndConnect(remoteUri, onFaceCreated);
	}

	/**
	 * the default createFace, the reference to the face will not be returned.
	 *
	 * @param remoteUri
	 */
	public void createFace(FaceUri remoteUri) {
		faceManager.createFaceAndConnect(remoteUri);
	}

	public void destroyFace(Face face) {
		faceManager.destroyFace(face);
	}

	public Collection<? extends Face> listFaces() {
		return faceManager.listFaces();
	}

	@Override
	public void onData(Data data, Face incomingFace) {
		List<List<PitEntry>> matches = pipeline.getPit().findAllMatches(data);
		for (List<PitEntry> entry : matches) {
			for (PitEntry one : entry) {

				// TODO: satisfy interests here
			}
		}

		pipeline.getCs().insert(data, matches.isEmpty());
	}

	@Override
	public void onInterest(Interest interest, final Face face) {
		logger.info("OnInterest is called");
		pipeline.onInterest(face, interest);
	}
}
