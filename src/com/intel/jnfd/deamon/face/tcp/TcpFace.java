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
import com.intel.jndn.forwarder.api.callbacks.OnInterestReceived;
import com.intel.jnfd.deamon.face.AbstractFace;
import com.intel.jnfd.deamon.face.FaceUri;
import com.intel.jnfd.util.NfdCommon;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
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
import net.named_data.jndn.encoding.WireFormat;
import net.named_data.jndn.encoding.tlv.Tlv;
import net.named_data.jndn.encoding.tlv.TlvDecoder;
import net.named_data.jndn.util.Blob;
import net.named_data.jndn.util.Common;
import org.ncl.workflow.ccn.core.NclwNFDMgr;
import org.ncl.workflow.util.NCLWUtil;

/**
 *
 * @author Haitao Zhang <zhtaoxiang@gmail.com>
 */
public class TcpFace extends AbstractFace implements Serializable {


	public TcpFace(FaceUri localUri, FaceUri remoteUri,
			AsynchronousSocketChannel asynchronousSocketChannel,
			boolean isLocal, boolean isMultiAccess,
			OnCompleted<Face> onFaceDestroyedByPeer,
			OnDataReceived onDataReceived,
			OnInterestReceived onInterestReceived) {
		super(localUri, remoteUri, isLocal, isMultiAccess);
		this.asynchronousSocketChannel = asynchronousSocketChannel;

		// callbacks
		this.onFaceDestroyedByPeer = onFaceDestroyedByPeer;
		this.onInterestReceived = onInterestReceived;
		this.onDataReceived = onDataReceived;
		Deserializer  des = new Deserializer(onDataReceived, onInterestReceived);
		this.elementReader = new ElementReader(des);

		ReceiveHandler receiveHandler = new ReceiveHandler();
		receiveHandler.setParentFace(this);

		ReceiveAttachment newAttachment = new ReceiveAttachment();
		receiveQueue.add(newAttachment);
		this.asynchronousSocketChannel.read(newAttachment.inputBuffer, newAttachment, receiveHandler);
		logger.setLevel(NfdCommon.LOG_LEVEL);
//		NclwNFDMgr.getIns().setFace(this);
	}

	public TcpFace(FaceUri localUri, FaceUri remoteUri, AsynchronousSocketChannel asynchronousSocketChannel){
		super(localUri, remoteUri, false, false);
		this.asynchronousSocketChannel = asynchronousSocketChannel;
		this.onFaceDestroyedByPeer =null;
		this.onInterestReceived = null;
		this.onDataReceived = null;
		/*
		this.onFaceDestroyedByPeer =NclwNFDMgr.getIns().getMgr().onFaceDestroyedByPeer;
		this.onInterestReceived = NclwNFDMgr.getIns().getMgr().onInterestReceived;
		this.onDataReceived = NclwNFDMgr.getIns().getMgr().onDataReceived;

		 */
		//this.elementReader = new ElementReader(new Deserializer(onDataReceived, onInterestReceived));
		Deserializer  des = new Deserializer(onDataReceived, onInterestReceived);
		this.elementReader = new ElementReader(des);
		ReceiveHandler receiveHandler = new ReceiveHandler();
		receiveHandler.setParentFace(this);

////Added by Kanemitsu////
		ReceiveAttachment newAttachment = new ReceiveAttachment();
		receiveQueue.add(newAttachment);
		//this.asynchronousSocketChannel.read(newAttachment.inputBuffer, newAttachment, receiveHandler);
//		NclwNFDMgr.getIns().setFace(this);
////Added by Kanemitsu////

	}


	public AsynchronousSocketChannel getAsynchronousSocketChannel() {
		return asynchronousSocketChannel;
	}

	public void setAsynchronousSocketChannel(AsynchronousSocketChannel asynchronousSocketChannel) {
		this.asynchronousSocketChannel = asynchronousSocketChannel;
	}

	@Override
	public void sendInterest(Interest interest) {
		boolean wasQueueEmpty = sendQueue.isEmpty();
		sendQueue.add(interest.wireEncode(wireFormat));
		if (wasQueueEmpty) {
				sendFromQueue();
		}
	}

	/**
	 * This method is created for test purpose. It should not be invoked by the
	 * formal method.
	 *
	 * @param str
	 */
	public void send(String str) {
		boolean wasQueueEmpty = sendQueue.isEmpty();
		sendQueue.add(new Blob(str));
		if (wasQueueEmpty) {
			sendFromQueue();
		}
	}

	@Override
	public void sendData(Data data) {
		boolean wasQueueEmpty = sendQueue.isEmpty();
		sendQueue.add(data.wireEncode(wireFormat));
		if (wasQueueEmpty) {
			sendFromQueue();
		}
	}

	/**
	 * Check the sendQueue and send data out.
	 */
	protected synchronized void sendFromQueue() {
		if (!sendQueue.isEmpty()) {
			try{
				System.out.println("Remote ADDR:"+asynchronousSocketChannel.getRemoteAddress().toString());
			}catch(Exception e){
				e.printStackTrace();
			}


			try{
				ByteBuffer  bdata = sendQueue.poll().buf();

				asynchronousSocketChannel.write(bdata, null, sendHandler);
				//TcpChannel channel = NclwNFDMgr.getIns().getMgr().getPfactory().getChannelMap().get(NCLWUtil.NFD_PORT);
				//channel.acceptAgain();

			}catch(Exception e){
				StackTraceElement[] elem = e.getStackTrace();
				for(int i=0;i<elem.length;i++){
					System.out.println(elem[i]);
				}
			}

		}
	}

	protected synchronized void receiveFromQueue() {
		while (!receiveQueue.isEmpty()) {
			logger.info("try to process the incoming queue");
			System.out.println("***Received NANIKA***");
			//ReceiveAttachment head = receiveQueue.peek();
			ReceiveAttachment head = receiveQueue.poll();

			if (head.haveReadFromChannle) {
				System.out.println("****NANIKA179");
				////receiveQueue.poll();
				logger.info("decode packet");
				try {

					System.out.println("****NANIKA183***");
					elementReader.onReceivedData(head.inputBuffer.slice());
				} catch (EncodingException ex) {
					System.out.println("**Encoding Exception ");

					logger.log(Level.WARNING, "Failed to decode bytes on face.", ex);
				}
			} else {
				System.out.println("**ReceiveQueue NG");
				return;
			}
		}
	}

	@Override
	public void close() throws IOException {
		if (!asynchronousSocketChannel.isOpen()) {
			return;
		}

		asynchronousSocketChannel.close();
		sendQueue.clear();
		receiveQueue.clear();
	}

	private class ReceiveAttachment {

		public ByteBuffer inputBuffer = ByteBuffer.allocate(Common.MAX_NDN_PACKET_SIZE);
		public boolean haveReadFromChannle = false;
	}

	/**
	 * Receive data and split it into elements (e.g. Data, Interest) using the
	 * ElementReader
	 */
	private class ReceiveHandler implements CompletionHandler<Integer, ReceiveAttachment> {

		private TcpFace parentFace;

		public TcpFace getParentFace() {
			return parentFace;
		}

		public void setParentFace(TcpFace parentFace) {
			this.parentFace = parentFace;
		}

		@Override
		public void completed(Integer result, ReceiveAttachment attachment) {
			System.out.println("******Recv Handler_Completed with Result"+result +"@"+ this.parentFace.getRemoteUri().getInet().getHostAddress() );
//System.out.println("MAX Packet SIZE: "+Common.MAX_NDN_PACKET_SIZE);

			if (result != -1) {
				attachment.inputBuffer.flip();
				attachment.haveReadFromChannle = true;
				receiveFromQueue();
				ReceiveAttachment newAttachment = new ReceiveAttachment();
				receiveQueue.add(newAttachment);
				asynchronousSocketChannel.read(newAttachment.inputBuffer, newAttachment, this);

			} else {
				logger.info("NO DATA RECEIVED...");
				System.out.println("***No Data Received***");

				onFaceDestroyedByPeer.onCompleted((Face) TcpFace.this);
			}
		}

		@Override
		public void failed(Throwable exc, ReceiveAttachment attachment) {
			logger.log(Level.WARNING, "Failed to receive bytes on face.");
			System.out.println("***Failed to receive bytes on face.");
		}
	}

	/**
	 * Parse bytes into Interest and Data packets
	 */
	private class Deserializer implements ElementListener {

		private final OnDataReceived onDataReceived;
		private final OnInterestReceived onInterestReceived;

		public Deserializer(OnDataReceived onDataReceived, OnInterestReceived onInterestReceived) {
			this.onDataReceived = onDataReceived;
			this.onInterestReceived = onInterestReceived;
		}



		@Override
		public final void onReceivedElement(ByteBuffer element) throws EncodingException {
			//logger.info("onReceivedElement is called");
			if (element.get(0) == Tlv.Interest || element.get(0) == Tlv.Data) {
				logger.info("receive Data or Interest packet");
				TlvDecoder decoder = new TlvDecoder(element);
				if (decoder.peekType(Tlv.Interest, element.remaining())) {
					System.out.println("*********Received Interest***********");

					logger.info("receive Interest packet");
					Interest interest = new Interest();
					interest.wireDecode(element.slice(), TlvWireFormat.get());
System.out.println("***INTEREST ARRIVED@"+TcpFace.this.getLocalUri().getInet().getHostAddress());
System.out.println("***LOCAL:"+TcpFace.this.getLocalUri().getInet().getHostAddress() + "/REMOTE:"+TcpFace.this.getRemoteUri().getInet().getHostAddress());
					this.onInterestReceived.onInterest(interest, TcpFace.this);
				} else if (decoder.peekType(Tlv.Data, element.remaining())) {
					//logger.info("receive Data packet");
System.out.println("***DATA ARRIVED@"+TcpFace.this.getLocalUri().getInet().getHostAddress());
					Data data = new Data();
					data.wireDecode(element, TlvWireFormat.get());
					onDataReceived.onData(data, TcpFace.this);
				}
			}
		}

	}

	/**
	 * This class is used to handle the send data. The Void is used to pass the
	 * parameters.
	 */
	private class SendHandler implements CompletionHandler<Integer, Void> {

		@Override
		public void completed(Integer result, Void attachment) {
			System.out.println("***Send Handler is called***");
			if (result != -1) {
				//logger.log(Level.INFO, "bytes sent: {0}", result);
				if (!sendQueue.isEmpty()) {
					sendFromQueue();
				}
			} else {
				System.out.println("****NO DATE SENT...");
				logger.info("NO DATA SENT...");
				onFaceDestroyedByPeer.onCompleted((Face) this);
			}
		}

		@Override
		public void failed(Throwable exc, Void attachment) {
			//TODO: add actions in the future;
			//logger.log(Level.WARNING, "Failed to send bytes on face.");
			System.out.println("****SENDING FAILED...****");
			System.out.println(exc.getMessage());
			StackTraceElement[]  elem = exc.getStackTrace();
			for(int i=0;i<elem.length;i++){
				System.out.println(elem[i].toString());
			}
			exc.printStackTrace();

		}

	}

	private static final Logger logger = Logger.getLogger(TcpFace.class.getName());
	protected AsynchronousSocketChannel asynchronousSocketChannel;
    private final ByteBuffer inputBuffer = ByteBuffer.allocate(Common.MAX_NDN_PACKET_SIZE);
	private final Queue<Blob> sendQueue = new ConcurrentLinkedQueue<>();
	private final Queue<ReceiveAttachment> receiveQueue = new ConcurrentLinkedQueue<>();
	private final ElementReader elementReader;
	private final OnCompleted<Face> onFaceDestroyedByPeer;
	private final OnInterestReceived onInterestReceived;
	private final OnDataReceived onDataReceived;
	private final WireFormat wireFormat = new TlvWireFormat();
	private final CompletionHandler<Integer, Void> sendHandler = new SendHandler();
	//vm prefix
	protected String nodePrefix;

	public String getNodePrefix() {
		return nodePrefix;
	}

	public void setNodePrefix(String nodePrefix) {
		this.nodePrefix = nodePrefix;
	}
}
