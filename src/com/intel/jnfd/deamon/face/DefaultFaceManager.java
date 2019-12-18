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
package com.intel.jnfd.deamon.face;

import com.intel.jndn.forwarder.api.Channel;
import com.intel.jndn.forwarder.api.Face;
import com.intel.jndn.forwarder.api.FaceManager;
import com.intel.jndn.forwarder.api.ProtocolFactory;
import com.intel.jndn.forwarder.api.callbacks.OnCompleted;
import com.intel.jndn.forwarder.api.callbacks.OnDataReceived;
import com.intel.jndn.forwarder.api.callbacks.OnFailed;
import com.intel.jndn.forwarder.api.callbacks.OnInterestReceived;
import com.intel.jnfd.deamon.face.tcp.TcpFactory;
import com.intel.jnfd.deamon.fw.ForwardingPipeline;
import com.intel.jnfd.util.NfdCommon;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;

/**
 *
 * @author Haitao Zhang <zhtaoxiang@gmail.com>
 */
public final class DefaultFaceManager implements Runnable, FaceManager {

	public DefaultFaceManager(ExecutorService pool, ForwardingPipeline pipeline) {
		this.pool = pool;
		this.pipeline = pipeline;

		registerProtocol(new TcpFactory(this.pool,
				onChannelCreated,
				onChannelCreationFailed,
				onChannelDestroyed,
				onChannelDestructionFailed,
				onFaceCreated,
				onFaceCreationFailed,
				onFaceDestroyed,
				onFaceDestructionFailed,
				onFaceDestroyedByPeer,
				onDataReceived,
				onInterestReceived));
		logger.setLevel(NfdCommon.LOG_LEVEL);
	}

	public DefaultFaceManager(ForwardingPipeline pipeline) {
		this(Executors.newCachedThreadPool(), pipeline);
	}

	@Override
	public void registerProtocol(ProtocolFactory protocolFactory) {
		if (!protocols.containsKey(protocolFactory.scheme())) {
			protocols.put(protocolFactory.scheme(), protocolFactory);
		}
	}



	@Override
	public Collection<ProtocolFactory> listProtocols() {
		return protocols.values();
	}

	@Override
	public Collection<String> listProtocolNames() {
		return protocols.keySet();
	}

	public ProtocolFactory findProtocol(String scheme) {
		if (!protocols.containsKey(scheme)) {
			throw new IllegalArgumentException("Unknown protocol scheme: " + scheme);
		} else {
			return protocols.get(scheme);
		}
	}

	@Override
	public void run() {
		while(true){
			try{
				Thread.sleep(10);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}

	@Override
	public void createChannelAndListen(FaceUri localUri) {
		ProtocolFactory protocol = findProtocol(localUri.getScheme());
		if (protocol == null) {
			onChannelCreationFailed.onFailed(new Exception("No factory found "
					+ "for " + localUri.getScheme()));
			return;
		}
		//Create the channel instance and start to listen
		Channel channel = protocol.createChannel(localUri);
		try {
			channel.open();
			onChannelCreated.onCompleted(channel);
		} catch (IOException ex) {
			logger.log(Level.SEVERE, null, ex);
			onChannelCreationFailed.onFailed(ex);
		}
	}

	@Override
	public void destroyChannel(FaceUri localUri) {
		ProtocolFactory protocol = protocols.get(localUri.getScheme());
		if (protocol == null) {
			return;
		}
		protocol.destroyChannel(localUri);
	}

	@Override
	public Collection<? extends Channel> listChannels() {
		Collection<Channel> result = new HashSet<>();
		for (ProtocolFactory one : protocols.values()) {
			result.addAll(one.listChannels());
		}
		return result;
	}

	@Override
	public Collection<? extends Channel> listChannels(String scheme) {
		if (!protocols.containsKey(scheme)) {
			return null;
		}
		return protocols.get(scheme).listChannels();
	}

	@Override
	public void createFaceAndConnect(FaceUri remoteUri) {
		ProtocolFactory protocol = protocols.get(remoteUri.getScheme());
		if (protocol == null) {
			onFaceCreationFailed.onFailed(new Exception("No such scheme found"
					+ remoteUri.getScheme()));
			return;
		}
		protocol.createFace(remoteUri);
	}

	@Override
	public void createFaceAndConnect(FaceUri remoteUri, OnCompleted<Face> onFaceCreated) {
		ProtocolFactory protocol = protocols.get(remoteUri.getScheme());
		if (protocol == null) {
			onFaceCreationFailed.onFailed(new Exception("No such scheme found"
					+ remoteUri.getScheme()));
			return;
		}
		protocol.createFace(remoteUri, onFaceCreated);
	}

//    @Override
//    public void createFaceAndConnect(FaceUri localUri, FaceUri remoteUri) {
//        ProtocolFactory protocol = protocols.get(localUri.getScheme());
//        if (protocol == null) {
//            onFaceCreationFailed.onFailed(new Exception("No such scheme found"
//                    + localUri.getScheme()));
//            return;
//        }
//        protocol.createFace(localUri, remoteUri, true);
//    }
	@Override
	public void destroyFace(Face face) {
		logger.log(Level.INFO, "destroyFace: {0}", face);
		ProtocolFactory protocol = protocols.get(face.getLocalUri().getScheme());
		if (protocol == null) {
			onFaceDestructionFailed.onFailed(new Exception("No such scheme found "
					+ face.getLocalUri().getScheme()));
			return;
		}
		protocol.destroyFace(face);
	}

	@Override
	public void destroyFace(FaceUri localFaceUri, FaceUri remoteFaceUri) {
		ProtocolFactory protocol = protocols.get(localFaceUri.getScheme());
		if (protocol == null) {
			onFaceDestructionFailed.onFailed(new Exception("No such face found "
					+ localFaceUri.getScheme()));
			return;
		}
		protocol.destroyFace(localFaceUri, remoteFaceUri);
	}

	@Override
	public Collection<? extends Face> listFaces() {
		Collection<Face> result = new HashSet<>();
		for (ProtocolFactory one : protocols.values()) {
			result.addAll(one.listFaces());
		}
		return result;
	}

	public ExecutorService getPool() {
		return pool;
	}

	@Override
	public Collection<? extends Face> listFaces(String scheme) {
		if (!protocols.containsKey(scheme)) {
			return null;
		}
		return protocols.get(scheme).listFaces();
	}

	private final Map<String, ProtocolFactory> protocols = new HashMap<>();
	private final ExecutorService pool;
	private final ForwardingPipeline pipeline;
	private static final Logger logger = Logger.getLogger(DefaultFaceManager.class.getName());

	// All the callbacks
	public final OnCompleted<Channel> onChannelCreated = new OnCompleted() {

		@Override
		public void onCompleted(Object result) {

		}

	};
	public final OnFailed onChannelCreationFailed = new OnFailed() {

		@Override
		public void onFailed(Throwable error) {

		}

	};
	public final OnCompleted<Channel> onChannelDestroyed = new OnCompleted() {

		@Override
		public void onCompleted(Object result) {

		}

	};
	private final OnFailed onChannelDestructionFailed = new OnFailed() {

		@Override
		public void onFailed(Throwable error) {

		}

	};
	public  final OnCompleted<Face> onFaceCreated = new OnCompleted() {

		@Override
		public void onCompleted(Object result) {
			if (result instanceof Face) {
				pipeline.addFace((Face) result);
			}
		}

	};
	private final OnFailed onFaceCreationFailed = new OnFailed() {

		@Override
		public void onFailed(Throwable error) {

		}

	};
	public  final OnCompleted<Face> onFaceDestroyed = new OnCompleted() {

		@Override
		public void onCompleted(Object result) {
			if (result instanceof Face) {
				pipeline.removeFace((Face) result, null);
			}
		}

	};
	private final OnFailed onFaceDestructionFailed = new OnFailed() {

		@Override
		public void onFailed(Throwable error) {

		}

	};
	public  final OnCompleted<Face> onFaceDestroyedByPeer = new OnCompleted() {
		@Override
		public void onCompleted(Object result) {
			if (result instanceof Face) {
				destroyFace((Face) result);
			}
		}
	};
	public  final OnDataReceived onDataReceived = new OnDataReceived() {

		@Override
		public void onData(Data data, Face incomingFace) {
			logger.info("OnData is called");
			pipeline.onData(incomingFace, data);
		}

	};
	public final OnInterestReceived onInterestReceived = new OnInterestReceived() {
		@Override
		public void onInterest(Interest interest, Face face) {
			logger.info("OnInterest is called");
			pipeline.onInterest(face, interest);
		}

	};

	public void processInterest(Interest interest, Face face){
		logger.info("OnInterest is called");
		pipeline.onInterest(face, interest);
	}

}
