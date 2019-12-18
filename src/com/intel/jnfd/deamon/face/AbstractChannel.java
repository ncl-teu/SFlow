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
package com.intel.jnfd.deamon.face;

import com.intel.jndn.forwarder.api.Channel;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Haitao Zhang <zhtaoxiang@gmail.com>
 */
public abstract class AbstractChannel implements Channel {

	@Override
	public Set<FaceUri> localUri() {
		return localUris;
	}

	public void addLocalUri(FaceUri localUri) {
		localUris.add(localUri);
	}

	public void removeLocalUri(FaceUri localUri) {
		localUris.remove(localUri);
	}

	private final Set<FaceUri> localUris = new HashSet<>();
}
