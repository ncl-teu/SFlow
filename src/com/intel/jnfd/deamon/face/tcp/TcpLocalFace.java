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
import com.intel.jnfd.deamon.face.FaceUri;
import java.nio.channels.AsynchronousSocketChannel;

/**
 *
 * @author Haitao Zhang <zhtaoxiang@gmail.com>
 */
public class TcpLocalFace extends TcpFace {

	public TcpLocalFace(FaceUri localUri, FaceUri remoteUri,
			AsynchronousSocketChannel asynchronousSocketChannel, boolean isLocal,
			boolean isMultiAccess, OnCompleted<Face> onFaceDestroyedByPeer,
			OnDataReceived onDataReceived, OnInterestReceived onInterestReceived) {
		super(localUri, remoteUri, asynchronousSocketChannel, isLocal, isMultiAccess,
				onFaceDestroyedByPeer, onDataReceived, onInterestReceived);
	}

}
