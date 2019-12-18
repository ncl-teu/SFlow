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
package com.intel.jnfd.deamon.table.measurement;

import com.intel.jndn.forwarder.api.Strategy;
import com.intel.jnfd.deamon.table.EntryFilter;
import com.intel.jnfd.deamon.table.strategy.StrategyChoice;
import net.named_data.jndn.Name;

/**
 *
 * @author Haitao Zhang <zhtaoxiang@gmail.com>
 */
public class MeasurementAccessor {

	public MeasurementAccessor(Measurement measurement,
			StrategyChoice strategyChoice, Strategy strategy) {
		this.measurement = measurement;
		this.strategyChoice = strategyChoice;
		this.strategy = strategy;
	}

	public MeasurementEntry get(Name name) {
		return filter(measurement.get(name));
	}

	/**
	 * find or insert a Measurements entry for child's parent
	 *
	 * @param child
	 * @return null if child is the root entry
	 */
	public MeasurementEntry getParentMeasurementEntry(MeasurementEntry child) {
		return filter(measurement.getParentMeasurementEntry(child));
	}

	public MeasurementEntry findLongestPrefixMatch(Name name, EntryFilter filter) {
		return filter(measurement.findLongestPrefixMatch(name, filter));
	}

	public MeasurementEntry findLongestPrefixMatch(Name name) {
		return filter(measurement.findLongestPrefixMatch(name));
	}

	public MeasurementEntry findExactMatch(Name name) {
		return filter(measurement.findExactMatch(name));
	}

	public void extendLifetime(MeasurementEntry entry, long lifetime) {
		measurement.extendLifetime(entry, lifetime);
	}

	private MeasurementEntry filter(MeasurementEntry entry) {
		if (entry == null) {
			return null;
		}
		Strategy effectiveStrategy
				= strategyChoice.findEffectiveStrategy(entry.getName());
		// FIX: how to compare the strategy in a proper way
		if (effectiveStrategy.equals(strategy)) {
			return entry;
		}
		return new MeasurementEntry();
	}

	private Measurement measurement;
	private StrategyChoice strategyChoice;
	private Strategy strategy;
}
