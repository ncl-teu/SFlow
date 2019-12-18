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
package com.intel.jndn.forwarder.impl;

import com.intel.jndn.forwarder.api.callbacks.OnDataReceived;
import com.intel.jndn.forwarder.api.callbacks.OnInterestReceived;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.encoding.ElementListener;
import net.named_data.jndn.encoding.ElementReader;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.encoding.TlvWireFormat;
import net.named_data.jndn.encoding.tlv.Tlv;
import net.named_data.jndn.encoding.tlv.TlvDecoder;

/**
 * A packet collector to deserialize TLV packets and pass them up to the given
 * callbacks; assumes that {@link #add(java.nio.ByteBuffer)} will be called with
 * packets in order. Since this may be used inside an IO callback,
 * {@link #add(java.nio.ByteBuffer)} will return very quickly.
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class OrderedPacketReader {

	private static final Logger logger = Logger.getLogger(OrderedPacketReader.class.getName());
	private static int globalReaderCount = 0;
	private final Queue<OrderedPacket> packets = new ConcurrentLinkedQueue<>();
	private final BackgroundReader reader;
	private final Thread thread;
	private boolean stopThreadWhenQueueIsEmpty = false;

	public OrderedPacketReader(OnDataReceived onDataReceived, OnInterestReceived onInterestReceived) {
		this.reader = new BackgroundReader(onDataReceived, onInterestReceived);
		this.thread = new Thread(reader);
		startBackgroundThread(this.thread);
	}

	/**
	 * Helper method to start the background reader thread
	 *
	 * @param thread
	 */
	private void startBackgroundThread(Thread thread) {
		thread.setName("OrderedPacketReader-" + globalReaderCount);
		thread.start();
		globalReaderCount++;
	}

	/**
	 * Push a packet on to the queue
	 *
	 * @param packet
	 */
	public void add(OrderedPacket packet) {
		packets.offer(packet);
		synchronized (reader) {
			reader.notifyAll();
		}
	}

	/**
	 * Advise the packet reader to stop processing packets once the current
	 * queue is empty
	 */
	public void stopGracefully() {
		logger.log(Level.FINEST, "Stopping packet reader when queue is empty: {0}", thread.getName());
		stopThreadWhenQueueIsEmpty = true;
	}

	/**
	 * Background reader thread; pauses when no packets are available on the
	 * queue and starts again when notified of new packets
	 */
	private class BackgroundReader implements Runnable {

		private int numPackets = 0;
		private final ElementReader elementReader;

		private BackgroundReader(OnDataReceived onDataReceived, OnInterestReceived onInterestReceived) {
			this.elementReader = new ElementReader(new PacketDeserializer(onDataReceived, onInterestReceived));
		}

		@Override
		public void run() {
			logger.log(Level.FINEST, "Starting packet reader: {0}", thread.getName());
			
			while (true) {
				while (packets.isEmpty()) {
					waitForNotify();
				}

				while (!packets.isEmpty()) {
					if (packets.peek().filled) {
						process(packets.poll());
					}
				}

				if (stopThreadWhenQueueIsEmpty && packets.isEmpty()) {
					break;
				}
			}
			
			logger.log(Level.FINEST, "Stopping packet reader: {0}", thread.getName());
		}

		private void waitForNotify() {
			try {
				synchronized (this) {
					this.wait();
				}
			} catch (InterruptedException ex) {
			}
		}

		private void process(OrderedPacket attachment) {
			logger.log(Level.FINEST, "Decoding packet #{0}", numPackets);
			try {
				attachment.buffer.flip();
				elementReader.onReceivedData(attachment.buffer);
				numPackets++;
			} catch (EncodingException ex) {
				logger.log(Level.WARNING, "Failed to decode packet: {0}", ex);
			}
		}
	}

	/**
	 * Parse bytes into Interest and Data packets
	 */
	private class PacketDeserializer implements ElementListener {

		private final OnDataReceived onDataReceived;
		private final OnInterestReceived onInterestReceived;

		public PacketDeserializer(OnDataReceived onDataReceived, OnInterestReceived onInterestReceived) {
			this.onDataReceived = onDataReceived;
			this.onInterestReceived = onInterestReceived;
		}

		@Override
		public final void onReceivedElement(ByteBuffer element) throws EncodingException {
			logger.log(Level.INFO, "Received {0} bytes to deserialize", element.limit());
			if (element.get(0) == Tlv.Interest || element.get(0) == Tlv.Data) {
				TlvDecoder decoder = new TlvDecoder(element);
				if (decoder.peekType(Tlv.Interest, element.remaining())) {
					logger.finest("Decoding an interest packet.");
					Interest interest = new Interest();
					interest.wireDecode(element, TlvWireFormat.get());
					onInterestReceived.onInterest(interest, null); // TODO fix

				} else if (decoder.peekType(Tlv.Data, element.remaining())) {
					logger.finest("Decoding a data packet.");
					Data data = new Data();
					data.wireDecode(element, TlvWireFormat.get());
					onDataReceived.onData(data, null); // TODO fix
				}
			}
		}
	}
}
