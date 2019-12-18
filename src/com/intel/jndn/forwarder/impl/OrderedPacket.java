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

import java.nio.ByteBuffer;
import net.named_data.jndn.util.Common;

/**
 * Wrap buffers so we can notate when they are filled; this allows us to put
 * buffers in the reader queue before they are filled (and thus are still in
 * order). The IO callback must set {@link #filled} to true once it knows the
 * buffer now has data.
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class OrderedPacket {

	public final ByteBuffer buffer;
	public boolean filled = false;

	private OrderedPacket(ByteBuffer buffer) {
		this.buffer = buffer;
	}

	/**
	 * Build an NDN-sized, direct-memory packet
	 */
	public OrderedPacket() {
		this(ByteBuffer.allocateDirect(Common.MAX_NDN_PACKET_SIZE));
	}

	/**
	 * @param buffer a filled buffer
	 * @return a filled packet
	 */
	public static OrderedPacket fromFilledBuffer(ByteBuffer buffer) {
		OrderedPacket packet = new OrderedPacket(buffer);
		packet.filled = true;
		return packet;
	}
}
