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
package com.intel.jndn.forwarder.api;

import com.intel.jndn.forwarder.api.callbacks.OnCompleted;
import com.intel.jnfd.deamon.face.FaceUri;
import java.util.Collection;

/**
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public interface FaceManager {

	public void registerProtocol(ProtocolFactory protocolFactory);

	public Collection<ProtocolFactory> listProtocols();

	public Collection<String> listProtocolNames();

	public void createChannelAndListen(FaceUri localUri);

	public Collection<? extends Channel> listChannels();

	public Collection<? extends Channel> listChannels(String scheme);

	public void destroyChannel(FaceUri localUri);

	public void createFaceAndConnect(FaceUri remoteUri);

	public void createFaceAndConnect(FaceUri remoteUri, OnCompleted<Face> onFaceCreated);

//	public void createFaceAndConnect(FaceUri localUri, FaceUri remoteUri);
	public Collection<? extends Face> listFaces();

	public Collection<? extends Face> listFaces(String scheme);

	public void destroyFace(Face face);

	public void destroyFace(FaceUri localFaceUri, FaceUri remoteFaceUri);

}
