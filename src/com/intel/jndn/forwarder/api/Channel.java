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

import com.intel.jnfd.deamon.face.AbstractFace;
import com.intel.jnfd.deamon.face.FaceUri;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

/**
 * Channels are abstractions of local interfaces; they perform the necessary
 * functionality to connect a local interface to a remote interface (i.e. a
 * {@link AbstractFace}). When a channel is opened, it accepts incoming traffic
 * from remote faces; when it is closed, it rejects incoming traffic and closes
 * all faces connected to it.
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public interface Channel {

	/**
	 * A channel may correspond to more than one local FaceUri, for example, a
	 * tcp channel corresponds to a IPv4 and a IPv6 local FaceUri.
	 *
	 * @return
	 */
	public Set<FaceUri> localUri();

	public void open() throws IOException;

	public void close() throws IOException;

	public Collection<? extends Face> listFaces();

	public void destroyFace(FaceUri faceUri);
}
