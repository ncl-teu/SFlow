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
import java.util.logging.Level;
import java.util.logging.Logger;
import net.named_data.jndn.Interest;

/**
 *
 * @author Haitao Zhang <zhtaoxiang@gmail.com>
 */
public class RetxSuppressionExponential extends RetxSuppression {

	public RetxSuppressionExponential(long initialInterval, double multiplier,
			long maxInterval) {
		this.initialInterval = initialInterval;
		this.multiplier = multiplier;
		this.maxInterval = maxInterval;
	}

	public RetxSuppressionExponential() {
		this.initialInterval = DEFAULT_INITIAL_INTERVAL;
		this.multiplier = DEFAULT_MULTIPLIER;
		this.maxInterval = DEFAULT_MAX_INTERVAL;
	}

	/**
	 * this method must be further overrided
	 *
	 * @param inFace
	 * @param interest
	 * @param pitEntry
	 * @return
	 */
	@Override
	public Result decide(Face inFace, Interest interest, PitEntry pitEntry) {
		boolean isNewPitEntry = !pitEntry.hasUnexpiredOutRecords();
		if (isNewPitEntry) {
			return Result.NEW;
		}

		long lastOutgoing = getLastOutgoing(pitEntry);
		long currentTime = System.currentTimeMillis();
		long sinceLastOutgoing = lastOutgoing - currentTime;

		PitInfo candidatePitInfo = new PitInfo(initialInterval);
		try {
			PitInfo pitInfo
					= (PitInfo) pitEntry.getOrCreateStrategyInfo(candidatePitInfo);
			boolean shouldSuppress
					= sinceLastOutgoing < pitInfo.suppressionInterval;

			if (shouldSuppress) {
				return Result.SUPPRESS;
			}
			pitInfo.suppressionInterval = Math.min(maxInterval,
					(long) (pitInfo.suppressionInterval * multiplier));
			return Result.FORWARD;
		} catch (InstantiationException ex) {
			Logger.getLogger(RetxSuppressionExponential.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IllegalAccessException ex) {
			Logger.getLogger(RetxSuppressionExponential.class.getName()).log(Level.SEVERE, null, ex);
		}

		//
		return null;
	}

	public class PitInfo extends StrategyInfo {

		public PitInfo(long suppressionInterval) {
			this.suppressionInterval = suppressionInterval;
		}

		@Override
		public int getTypeId() {
			return 1020;
		}

		/**
		 * if last transmission occurred within suppressionInterval,
		 * retransmission will be suppressed
		 */
		public long suppressionInterval;

	}

	public static final long DEFAULT_INITIAL_INTERVAL = 1; // milliseconds
	public static final double DEFAULT_MULTIPLIER = 2.0;
	public static final long DEFAULT_MAX_INTERVAL = 250; // milliseconds

	private final long initialInterval;
	private final double multiplier;
	private final long maxInterval;
}
