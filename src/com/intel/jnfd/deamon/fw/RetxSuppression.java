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
package com.intel.jnfd.deamon.fw;

import com.intel.jndn.forwarder.api.Face;
import com.intel.jnfd.deamon.table.pit.PitEntry;
import com.intel.jnfd.deamon.table.pit.PitOutRecord;
import java.util.List;
import net.named_data.jndn.Interest;

/**
 *
 * @author Haitao Zhang <zhtaoxiang@gmail.com>
 */
public abstract class RetxSuppression {

	public enum Result {

		/**
		 * Interest is new (not a retransmission)
		 */
		NEW,
		/**
		 * Interest is retransmission and should be forwarded
		 */
		FORWARD,
		/**
		 * Interest is retransmission and should be suppressed
		 */
		SUPPRESS
	}

	public abstract Result decide(Face inFace, Interest interest,
			PitEntry pitEntry);

	protected long getLastOutgoing(PitEntry pitEntry) {
		List<PitOutRecord> outRecords = pitEntry.getOutRecords();
		long result = 0;
		for (PitOutRecord one : outRecords) {
			if (result < one.getLastRenewed()) {
				result = one.getLastRenewed();
			}
		}
		return result;
	}
}
