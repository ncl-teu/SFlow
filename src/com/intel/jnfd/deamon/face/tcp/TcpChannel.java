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

import com.intel.jndn.forwarder.api.Face;
import com.intel.jndn.forwarder.api.callbacks.OnCompleted;
import com.intel.jndn.forwarder.api.callbacks.OnDataReceived;
import com.intel.jnfd.deamon.face.AbstractChannel;
import com.intel.jndn.forwarder.api.callbacks.OnFailed;
import com.intel.jndn.forwarder.api.callbacks.OnInterestReceived;
import com.intel.jnfd.deamon.face.FaceUri;
import com.intel.jnfd.deamon.face.ParseFaceUriException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Each channel is corresponded to a local port, so two different local FaceUris
 * will correspond to a channel (one for IPv4 0.0.0.0 and the other one for IPv6
 * [::]). If IPv4 is enabled on this channel, face relied on IPv4 can be
 * created, If IPv6 is enabled on this channel, face relied on IPv6 can be
 * created,
 *
 * @author Haitao Zhang <zhtaoxiang@gmail.com>
 */
public class TcpChannel extends AbstractChannel {

	TcpChannel(FaceUri uri, AsynchronousChannelGroup asynchronousChannelGroup,
			OnCompleted<Face> onFaceCreated,
			OnFailed onFaceCreationFailed,
			OnCompleted<Face> onFaceDestroyed,
			OnFailed onFaceDestructionFailed,
			OnCompleted<Face> onFaceDestroyedByPeer,
			OnInterestReceived onInterestReceived,
			OnDataReceived onDataReceived) {
		addLocalUri(uri);
		if (uri.getIsV6()) {
			enableV6 = true;
		} else {
			enableV4 = true;
		}

		mAddr = new InetSocketAddress(uri.getInet(), uri.getPort());
		this.asynchronousChannelGroup = asynchronousChannelGroup;

		// all the callbacks
		this.onFaceCreated = onFaceCreated;
		this.onFaceCreationFailed = onFaceCreationFailed;
		this.onFaceDestroyed = onFaceDestroyed;
		this.onFaceDestructionFailed = onFaceDestructionFailed;
		this.onFaceDestroyedByPeer = onFaceDestroyedByPeer;
		this.onDataReceived = onDataReceived;
		this.onInterestReceived = onInterestReceived;
	}

	public int size() {
		return faceMap.size();
	}



	void connect(FaceUri remoteFaceUri, OnCompleted<Face> onFaceCreated)
			throws IOException {
		connect(remoteFaceUri, TimeUnit.SECONDS.toSeconds(4), onFaceCreated);
	}

	public void connect(FaceUri faceUri) throws IOException {
		connect(faceUri, TimeUnit.SECONDS.toSeconds(4), onFaceCreated);
	}

	public void connect(FaceUri faceUri, long timeout, OnCompleted<Face> onFaceCreated)
			throws IOException {
		InetSocketAddress remoteAddr
				= new InetSocketAddress(faceUri.getInet(), faceUri.getPort());
		Face face = faceMap.get(remoteAddr);
		if (face != null) {
			onFaceCreated.onCompleted(face);
			return;
		}

		AsynchronousSocketChannel asynchronousSocket
				= AsynchronousSocketChannel.open(asynchronousChannelGroup);
		ConnectAttachment connectAttachment = new ConnectAttachment();
		connectAttachment.onFaceCreated = onFaceCreated;
		connectAttachment.asynchronousSocketChannel = asynchronousSocket;
		asynchronousSocket.connect(remoteAddr, connectAttachment, new ConnectHandler());
	}

	/**
	 * Open the AsynchronousServerSocket to prepare to accept incoming
	 * connections. This method only needs to be called once.
	 *
	 * @throws java.io.IOException
	 */
	@Override
	public void open() throws IOException {
		if (asynchronousServerSocket == null) {
			// start to listen
			asynchronousServerSocket
					= AsynchronousServerSocketChannel.open(asynchronousChannelGroup);
			asynchronousServerSocket.setOption(StandardSocketOptions.SO_REUSEADDR, true);
			asynchronousServerSocket.bind(mAddr);
			asynchronousServerSocket.accept(null, new AcceptHandler());
		}
	}

	public TcpFace getFace(String remoteIP, int remotePort) {
		InetSocketAddress remoteSocket = new InetSocketAddress(remoteIP, remotePort);
		return faceMap.get(remoteSocket);
	}

	@Override
	public void destroyFace(FaceUri faceUri) {
		InetSocketAddress remoteAddr
				= new InetSocketAddress(faceUri.getInet(), faceUri.getPort());
		TcpFace face = faceMap.remove(remoteAddr);
		try {
			face.close();
			onFaceDestroyed.onCompleted(face);
		} catch (IOException ex) {
			logger.log(Level.SEVERE, null, ex);
			onFaceDestructionFailed.onFailed(ex);
		}
	}

	@Override
	public void close() throws IOException {
		// close all the Faces first
		for (Face one : faceMap.values()) {
			try {
				one.close();
				onFaceDestroyed.onCompleted(one);
			} catch (IOException ex) {
				logger.log(Level.SEVERE, null, ex);
				onFaceDestructionFailed.onFailed(ex);
			}
		}
		// close the socket for this channel
		asynchronousServerSocket.close();
	}

	/**
	 * Only close one of IPv4 and IPv6 on the channel. After closing, check if
	 * enableV4 and enableV6 are both disabled, if yes, close the socket for
	 * this channel
	 *
	 * @param localFaceUri
	 * @throws java.io.IOException
	 */
	public void close(FaceUri localFaceUri) throws IOException {
		if (localFaceUri.getIsV6()) {
			// If both IPv4 and IPv6 need to be disabled, simply close the channel
			if (enableV4 == false) {
				close();
			} else {
				enableV6 = false;
				removeLocalUri(localFaceUri);
				// close all the IPv6 Faces
				for (Face one : faceMap.values()) {
					if (one.getLocalUri().getIsV6()) {
						try {
							one.close();
							onFaceDestroyed.onCompleted(one);
						} catch (IOException ex) {
							logger.log(Level.SEVERE, null, ex);
							onFaceDestructionFailed.onFailed(ex);
						}
					}
				}
			}
		} else {
			// If both IPv4 and IPv6 need to be disabled, simply close the channel
			if (enableV6 == false) {
				close();
			} else {
				enableV4 = false;
				removeLocalUri(localFaceUri);
				// close all the IPv4 Faces
				for (Face one : faceMap.values()) {
					if (!one.getLocalUri().getIsV6()) {
						try {
							one.close();
							onFaceDestroyed.onCompleted(one);
						} catch (IOException ex) {
							logger.log(Level.SEVERE, null, ex);
							onFaceDestructionFailed.onFailed(ex);
						}
					}
				}
			}
		}
	}

	@Override
	public Collection<? extends Face> listFaces() {
		return faceMap.values();
	}

	/**
	 * This AcceptAttachment class is used to pass the parameters needed by the
	 * AcceptHandler.
	 */
	private class AcceptAttachment {

	}

	/**
	 * This is the AcceptHandler used to accept incoming connections.
	 */
	private class AcceptHandler implements
			CompletionHandler<AsynchronousSocketChannel, AcceptAttachment> {

		/**
		 *
		 * @param result it is the AsynchronousSocketChannel created by the
		 * AsynchronousSereverSocketChannel.
		 * @param attachment it is the parameter need by the
		 * AsynchronousSocketChannel.
		 */
		@Override
		public void completed(AsynchronousSocketChannel result,
				AcceptAttachment attachment) {
			// accept the next connection
			asynchronousServerSocket.accept(attachment, this);
			try {
				// handle this connection
				createFace(result, onFaceCreated);
			} catch (IOException ex) {
				logger.log(Level.SEVERE, null, ex);
				onFaceCreationFailed.onFailed(ex);
			}
		}

		@Override
		public void failed(Throwable exc, AcceptAttachment attachment) {
			//TODO: fix this in the future;
			logger.log(Level.INFO, "connetion is closed");
		}

	}

	private class ConnectAttachment {

		public OnCompleted<Face> onFaceCreated;
		public AsynchronousSocketChannel asynchronousSocketChannel;
	}

	private class ConnectHandler implements CompletionHandler<Void, ConnectAttachment> {

		@Override
		public void completed(Void result, ConnectAttachment attachment) {
			try {
				createFace(attachment.asynchronousSocketChannel, attachment.onFaceCreated);
			} catch (IOException ex) {
				logger.log(Level.SEVERE, null, ex);
				onFaceCreationFailed.onFailed(ex);
			}
		}

		@Override
		public void failed(Throwable exc, ConnectAttachment attachment) {
			//TODO: fix this in the future;
			logger.log(Level.INFO, "connetion is closed");
		}
	}

	/**
	 * Create face for data sending and receiving.
	 *
	 * @param asynchronousSocketChannel
	 * @throws IOException
	 */
	private void createFace(AsynchronousSocketChannel asynchronousSocketChannel,
			OnCompleted<Face> onFaceCreated)
			throws IOException {
		InetSocketAddress remoteSocket
				= (InetSocketAddress) (asynchronousSocketChannel.getRemoteAddress());
		TcpFace face = null;
		if ((face = faceMap.get(remoteSocket)) == null) {
			InetSocketAddress localSocket
					= (InetSocketAddress) (asynchronousSocketChannel.getLocalAddress());
			if (remoteSocket.getAddress().isLoopbackAddress()
					&& localSocket.getAddress().isLoopbackAddress()) {
				try {
					face = new TcpLocalFace(new FaceUri("tcp", localSocket),
							new FaceUri("tcp", remoteSocket),
							asynchronousSocketChannel, true, false,
							onFaceDestroyedByPeer,
							onDataReceived, onInterestReceived);
					onFaceCreated.onCompleted(face);
				} catch (ParseFaceUriException ex) {
					logger.log(Level.SEVERE, null, ex);
					onFaceCreationFailed.onFailed(ex);
				}
			} else {
				try {
					face = new TcpFace(new FaceUri("tcp", localSocket),
							new FaceUri("tcp", remoteSocket),
							asynchronousSocketChannel, false, false,
							onFaceDestroyedByPeer,
							onDataReceived, onInterestReceived);
					onFaceCreated.onCompleted(face);
				} catch (ParseFaceUriException ex) {
					logger.log(Level.SEVERE, null, ex);
					onFaceCreationFailed.onFailed(ex);
				}
			}
			faceMap.put(remoteSocket, face);
		} else {
			try {
				// we already have a face for this endpoint, just reuse it
				asynchronousSocketChannel.close();
			} catch (IOException ex) {
				logger.log(Level.SEVERE, null, ex);
			}
		}
	}

	public boolean isEnabledV4() {
		return enableV4;
	}

	public void enableV4() {
		this.enableV4 = true;
	}

	public void disableV4() {
		this.enableV4 = false;
	}

	public boolean isEnabledV6() {
		return enableV6;
	}

	public void enableV6() {
		this.enableV6 = true;
	}

	public void disableV6() {
		this.enableV6 = false;
	}

	public Map<InetSocketAddress, TcpFace> getFaceMap() {
		return faceMap;
	}

	public AsynchronousServerSocketChannel getAsynchronousServerSocket() {
		return asynchronousServerSocket;
	}

	public void setAsynchronousServerSocket(AsynchronousServerSocketChannel asynchronousServerSocket) {
		this.asynchronousServerSocket = asynchronousServerSocket;
	}

	public AsynchronousChannelGroup getAsynchronousChannelGroup() {
		return asynchronousChannelGroup;
	}

	public void setAsynchronousChannelGroup(AsynchronousChannelGroup asynchronousChannelGroup) {
		this.asynchronousChannelGroup = asynchronousChannelGroup;
	}




	private InetSocketAddress mAddr = null;
	private final Map<InetSocketAddress, TcpFace> faceMap = new HashMap<>();
	// server socket used for incoming connection
	private AsynchronousServerSocketChannel asynchronousServerSocket = null;
	// socket used for outgoing connection
	private AsynchronousChannelGroup asynchronousChannelGroup = null;
	private boolean enableV4 = false;
	private boolean enableV6 = false;

	private static final Logger logger = Logger.getLogger(TcpChannel.class.getName());
	private final OnCompleted<Face> onFaceCreated;
	private final OnFailed onFaceCreationFailed;
	private final OnCompleted<Face> onFaceDestroyed;
	private final OnFailed onFaceDestructionFailed;
	private final OnCompleted<Face> onFaceDestroyedByPeer;
	private final OnInterestReceived onInterestReceived;
	private final OnDataReceived onDataReceived;
}
