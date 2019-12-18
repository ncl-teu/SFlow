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
package com.intel.jnfd.deamon.table.pit;

import com.intel.jndn.forwarder.api.Face;
import com.intel.jnfd.deamon.table.strategy.StrategyInfoHost;
import com.intel.jnfd.util.NfdCommon;
import net.named_data.jndn.Interest;
import net.named_data.jndn.util.Blob;

/**
 *
 * @author Haitao Zhang <zhtaoxiang@gmail.com>
 */
public class PitFaceRecord extends StrategyInfoHost {

	public PitFaceRecord(Face face) {
		this.face = face;
		lastNonce = new Blob();
		lastRenewed = 0;
		expiry = 0;
	}

	public Face getFace() {
		return face;
	}

	public Blob getLastNonce() {
		return lastNonce;
	}

	public long getLastRenewed() {
		return lastRenewed;
	}

	/**
	 * gives the time point this record expires getLastRenewed() +
	 * InterestLifetime
	 *
	 * @return
	 */
	public long getExpiry() {
		return expiry;
	}

	/**
	 * updates lastNonce, lastRenewed, expiry fields
	 *
	 * @param interest
	 */
	public void update(Interest interest) {

		lastNonce = interest.getNonce();
		lastRenewed = System.currentTimeMillis();
		long lifeTime = (long) (interest.getInterestLifetimeMilliseconds());
		if (lifeTime < 0) {
			lifeTime = NfdCommon.DEFAULT_INTEREST_LIFETIME;
		}
		expiry = lastRenewed + lifeTime;
	}

	private Face face;
	private Blob lastNonce;
	private long lastRenewed;
	private long expiry;
}
