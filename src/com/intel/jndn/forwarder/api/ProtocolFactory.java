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
 * @author Haitao Zhang <zhtaoxiang@gmail.com>
 */
public interface ProtocolFactory {

	/**
	 * @return the scheme (e.g. 'tcp', 'udp', etc.) of the protocol; use this to
	 * match factories to URIs
	 */
	public String scheme();

	/**
	 * @return the local URI to be used for creating a default channel
	 */
	public FaceUri defaultLocalUri();

	/**
	 *
	 * @param faceUri
	 * @return
	 */
	public Channel createChannel(FaceUri faceUri);

	/**
	 * @return a list of currently open channels
	 */
	public Collection<? extends Channel> listChannels();

	/**
	 * Destroy the channel
	 *
	 * @param faceUri the URI of the channel
	 */
	public void destroyChannel(FaceUri faceUri);

	public void createFace(FaceUri remoteFaceUri);

	public void createFace(FaceUri remoteFaceUri, OnCompleted<Face> onFaceCreated);

	public Collection<? extends Face> listFaces();

	public void destroyFace(Face face);

	public void destroyFace(FaceUri localFaceUri, FaceUri remoteFaceUri);
}
