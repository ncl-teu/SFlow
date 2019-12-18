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
package com.intel.jnfd.deamon.face.tcp;

import com.intel.jndn.forwarder.api.Channel;
import com.intel.jndn.forwarder.api.Face;
import com.intel.jnfd.deamon.face.FaceUri;
import com.intel.jndn.forwarder.api.ProtocolFactory;
import com.intel.jndn.forwarder.api.callbacks.OnCompleted;
import com.intel.jndn.forwarder.api.callbacks.OnDataReceived;
import com.intel.jndn.forwarder.api.callbacks.OnFailed;
import com.intel.jndn.forwarder.api.callbacks.OnInterestReceived;
import com.intel.jnfd.deamon.face.ParseFaceUriException;
import org.ncl.workflow.ccn.core.NclwFaceManager;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Haitao Zhang <zhtaoxiang@gmail.com>
 */
public final class TcpFactory implements ProtocolFactory {

	public TcpFactory(ExecutorService pool,
			OnCompleted<Channel> onChannelCreated,
			OnFailed onChannelCreationFailed,
			OnCompleted<Channel> onChannelDestroyed,
			OnFailed onChannelDestructionFailed,
			OnCompleted<Face> onFaceCreated,
			OnFailed onFaceCreationFailed,
			OnCompleted<Face> onFaceDestroyed,
			OnFailed onFaceDestructionFailed,
			OnCompleted<Face> onFaceDestroyedByPeer,
			OnDataReceived onDataReceived,
			OnInterestReceived onInterestReceived) {
		this.onChannelCreated = onChannelCreated;
		this.onChannelCreationFailed = onChannelCreationFailed;
		this.onChannelDestroyed = onChannelDestroyed;
		this.onChannelDestructionFailed = onChannelDestructionFailed;
		this.onFaceCreated = onFaceCreated;
		this.onFaceCreationFailed = onFaceCreationFailed;
		this.onFaceDestroyed = onFaceDestroyed;
		this.onFaceDestructionFailed = onFaceDestructionFailed;
		this.onFaceDestroyedByPeer = onFaceDestroyedByPeer;
		this.onDataReceived = onDataReceived;
		this.onInterestReceived = onInterestReceived;
		try {
			this.asynchronousChannelGroup
					= AsynchronousChannelGroup.withThreadPool(pool);
			LOCAL_ADDRESS = new InetAddress[]{InetAddress.getByName("0.0.0.0"),
				InetAddress.getByName("::")};
			DEFAULT_URI = new FaceUri("tcp4://0.0.0.0:6363"); // or new FaceUri("tcp6://[::]:6363")};
		} catch (ParseFaceUriException ex) {
			logger.log(Level.SEVERE, null, ex);
		} catch (UnknownHostException ex) {
			logger.log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			logger.log(Level.SEVERE, null, ex);
		}
		// create default channels and start to listen
		Channel channel = createChannel(defaultLocalUri());
		if (channel != null) {
			try {
				channel.open();
				onChannelCreated.onCompleted(channel);

			} catch (IOException ex) {
				logger.log(Level.SEVERE, null, ex);
				onChannelCreationFailed.onFailed(ex);
			}
		}
	}

	public Map<Integer, TcpChannel> getChannelMap() {
		return channelMap;
	}

	@Override
	public Channel createChannel(FaceUri faceUri) {
		// Check if the FaceUri used to create channel is right or not.
		boolean isLocalFaceUri = true;
		for (InetAddress one : LOCAL_ADDRESS) {
			if (faceUri.getInet().equals(one)) {
				isLocalFaceUri = false;
				break;
			}
		}
		if (isLocalFaceUri) {
			onChannelDestructionFailed.onFailed(
					new IllegalArgumentException("The give FaceUri: " + faceUri
							+ " is not a local host uri"));
			return null;
		}

		TcpChannel channel = null;
		if (channelMap.containsKey(faceUri.getPort())) {
			channel = channelMap.get(faceUri.getPort());
			if (!channel.localUri().contains(faceUri)) {
				channel.addLocalUri(faceUri);
				if (faceUri.getIsV6()) {
					channel.enableV6();
				} else {
					channel.enableV4();
				}
			}
			onChannelCreated.onCompleted(channel);
		} else {
			channel = new TcpChannel(faceUri,
					asynchronousChannelGroup,
					onFaceCreated,
					onFaceCreationFailed,
					onFaceDestroyed,
					onFaceDestructionFailed,
					onFaceDestroyedByPeer,
					onInterestReceived,
					onDataReceived);
			channelMap.put(faceUri.getPort(), channel);
			onChannelCreated.onCompleted(channel);
		}
		return channel;
	}

	protected TcpChannel findChannel(FaceUri uri) {
		TcpChannel channel = channelMap.get(uri.getPort());
		if (channel == null) {
			return null;
		}
		if (channel.isEnabledV4() && !uri.getIsV6()
				|| channel.isEnabledV6() && uri.getIsV6()) {
			return channel;
		}
		return null;
	}

	@Override
	public Collection<? extends Channel> listChannels() {
		return channelMap.values();
	}

	@Override
	public void destroyChannel(FaceUri faceUri) {
		TcpChannel channel = channelMap.get(faceUri.getPort());
		try {
			// close Faces and Channel
			channel.close(faceUri);
			// remove it
			channelMap.remove(faceUri.getPort());
			onChannelDestroyed.onCompleted(channel);
		} catch (IOException ex) {
			logger.log(Level.SEVERE, null, ex);
			onChannelDestructionFailed.onFailed(ex);
		}
	}

	@Override
	public void createFace(FaceUri remoteFaceUri) {
		for (Map.Entry<Integer, TcpChannel> entry : channelMap.entrySet()) {
			TcpChannel channel = entry.getValue();
			if ((channel.isEnabledV4() && (!remoteFaceUri.getIsV6()))
					|| channel.isEnabledV6() && remoteFaceUri.getIsV6()) {
				try {
					channel.connect(remoteFaceUri);
				} catch (IOException ex) {
					logger.log(Level.SEVERE, null, ex);
					onFaceCreationFailed.onFailed(ex);
				}
				return;
			}
		}
		onFaceCreationFailed.onFailed(new IOException(
				"No channels available to connect to for " + remoteFaceUri));
	}

	@Override
	public void createFace(FaceUri remoteFaceUri, OnCompleted<Face> onFaceCreated) {
		for (Map.Entry<Integer, TcpChannel> entry : channelMap.entrySet()) {
			TcpChannel channel = entry.getValue();
			if ((channel.isEnabledV4() && (!remoteFaceUri.getIsV6()))
					|| channel.isEnabledV6() && remoteFaceUri.getIsV6()) {
				try {
					channel.connect(remoteFaceUri, onFaceCreated);
				} catch (IOException ex) {
					logger.log(Level.SEVERE, null, ex);
					onFaceCreationFailed.onFailed(ex);
				}
				return;
			}
		}
		onFaceCreationFailed.onFailed(new IOException(
				"No channels available to connect to for " + remoteFaceUri));
	}

	@Override
	public void destroyFace(Face face) {
		logger.log(Level.INFO, "destroyFace: {0}", face);
		destroyFace(face.getLocalUri(), face.getRemoteUri());
	}

	@Override
	public void destroyFace(FaceUri localFaceUri, FaceUri remoteFaceUri) {
		Channel channel = findChannel(localFaceUri);
		if (channel == null) {
			return;
		}
		channel.destroyFace(remoteFaceUri);
	}

	@Override
	public Collection<? extends Face> listFaces() {
		Collection<Face> result = new HashSet<>();
		for (TcpChannel one : channelMap.values()) {
			result.addAll(one.listFaces());
		}
		return result;
	}

	@Override
	public String scheme() {
		return SCHEME_NAME;
	}

	public int getDefaultPort() {
		return DEFAULT_PORT;
	}

	@Override
	public FaceUri defaultLocalUri() {
		return DEFAULT_URI;
	}

	// All the callbacks
	private final OnCompleted<Channel> onChannelCreated;
	private final OnFailed onChannelCreationFailed;
	private final OnCompleted<Channel> onChannelDestroyed;
	private final OnFailed onChannelDestructionFailed;
	private final OnCompleted<Face> onFaceCreated;
	private final OnFailed onFaceCreationFailed;
	private final OnCompleted<Face> onFaceDestroyed;
	private final OnFailed onFaceDestructionFailed;
	private final OnCompleted<Face> onFaceDestroyedByPeer;
	private final OnDataReceived onDataReceived;
	private final OnInterestReceived onInterestReceived;

	private static InetAddress LOCAL_ADDRESS[];
	private static final int DEFAULT_PORT = 6363;
	private static final String SCHEME_NAME = "tcp";
	private static final String DEFAULT_HOST = "::";
	private static FaceUri DEFAULT_URI = null;

	private static final Logger logger = Logger.getLogger(TcpFactory.class.getName());
	// The port # is the entry for the channel, since IPv4 and IPv6 sockes can
	// not be actived seperately
	private final Map<Integer, TcpChannel> channelMap = new HashMap<>();
	private AsynchronousChannelGroup asynchronousChannelGroup = null;

    //    private Set<Node> prohibitedNodes = new HashSet<Node>();
	//    TODO: if the prohibition function is necessary, we can implement this funtion 
	//    in the future;
}
