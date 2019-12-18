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

import com.intel.jndn.forwarder.api.Face2;
import com.intel.jndn.forwarder.api.callbacks.OnCompleted;
import com.intel.jndn.forwarder.api.callbacks.OnDataReceived;
import com.intel.jndn.forwarder.api.callbacks.OnInterestReceived;
import com.intel.jndn.forwarder.impl.OrderedPacket;
import com.intel.jndn.forwarder.impl.OrderedPacketReader;
import com.intel.jnfd.deamon.face.FaceUri;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;

/**
 * Represent a connection to a remote TCP port; receives packets from the remote
 * and provides an API for sending {@link Interest}s and {@link Data}s back.
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class TcpFace2 implements Face2 {

	private static final Logger logger = Logger.getLogger(TcpFace.class.getName());
	private final FaceUri remoteUri;
	private final FaceUri localUri;
	private AsynchronousSocketChannel socket;
	private final ExecutorService pool;
	private final OrderedPacketReader packetReader;

	/**
	 * Build a TCP face
	 *
	 * @param localUri
	 * @param remoteUri
	 * @param pool
	 * @param socket
	 * @param onDataReceived
	 * @param onInterestReceived
	 */
	public TcpFace2(FaceUri localUri, FaceUri remoteUri,
			ExecutorService pool,
			AsynchronousSocketChannel socket,
			OnDataReceived onDataReceived,
			OnInterestReceived onInterestReceived) {

		this.localUri = localUri;
		this.remoteUri = remoteUri;
		this.socket = socket;
		this.packetReader = new OrderedPacketReader(onDataReceived, onInterestReceived);
		this.pool = pool;

		readNext(this.socket);
	}

	private void readNext(AsynchronousSocketChannel socket1) {
		OrderedPacket packet = new OrderedPacket();
		socket1.read(packet.buffer, packet, new ReceiveHandler());
		packetReader.add(packet);
	}

	/**
	 * Receive data and split it into elements (e.g. Data, Interest) using the a
	 * packet reader
	 */
	private class ReceiveHandler implements CompletionHandler<Integer, OrderedPacket> {

		@Override
		public void completed(Integer result, OrderedPacket attachment) {
			if (result != -1) {
				logger.log(Level.INFO, "Received {0} bytes from: {1}", new Object[]{result, remoteUri});
				attachment.filled = true;
				readNext(socket);
			} else {
				logger.log(Level.INFO, "Socket closed from remote, closing locally: {0}", remoteUri);
				close(DO_NOTHING_HANDLER, DO_NOTHING_HANDLER);
			}
		}

		@Override
		public void failed(Throwable exc, OrderedPacket attachment) {
			logger.log(Level.WARNING, "Failed to receive bytes on face: {0}", remoteUri);
		}
	}

	public static OnCompleted DO_NOTHING_HANDLER = new OnCompleted() {
		@Override
		public void onCompleted(Object result) {
			// do nothing
		}
	};

	@Override
	public void close(OnCompleted<Void> onClosed, OnCompleted<Throwable> onFailed) {
		if (socket != null && socket.isOpen()) {
			try {
				socket.close();
			} catch (IOException ex) {
				logger.log(Level.SEVERE, "Failed to close socket on: {0}", remoteUri);
				onFailed.onCompleted(ex);
			}
		}
		packetReader.stopGracefully();
	}

	@Override
	public void sendInterest(Interest interest) {
		ByteBuffer buffer = interest.wireEncode().buf();
		socket.write(buffer);
	}

	@Override
	public void sendData(Data data) {
		ByteBuffer buffer = data.wireEncode().buf();
		socket.write(buffer);
	}

	@Override
	public FaceUri localUri() {
		return localUri;
	}

	@Override
	public FaceUri remoteUri() {
		return remoteUri;
	}

	@Override
	public boolean isLocal() {
		return false; // TODO fix
	}

	@Override
	public boolean isMultiAccess() {
		return false;
	}
}
