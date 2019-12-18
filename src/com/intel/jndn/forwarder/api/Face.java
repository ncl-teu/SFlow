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

import com.intel.jnfd.deamon.face.FaceUri;
import java.io.IOException;
import java.io.Serializable;

import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;

/**
 * Faces are abstractions of remote interfaces (see {@link Channel}).
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public interface Face  {

	public void sendInterest(Interest interest);

	public void sendData(Data data);

	public void close() throws IOException;

	public int getFaceId();

	public void setFaceId(int id);

	public FaceUri getLocalUri();

	public FaceUri getRemoteUri();

	public boolean isLocal();

	public boolean isMultiAccess();

	@Override
	public String toString();
}
