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

import com.intel.jnfd.deamon.table.EntryFilter;
import com.intel.jnfd.deamon.table.HashMapRepo;
import java.util.concurrent.TimeUnit;
import net.named_data.jndn.Name;

/**
 *
 * @author Haitao Zhang <zhtaoxiang@gmail.com>
 */
public class Measurement {

	public Measurement() {
		this.measurement = new HashMapRepo<>();
	}

	public MeasurementEntry get(Name name) {
		MeasurementEntry entry = measurement.findExactMatch(name);
		if (entry != null) {
			return entry;
		}

		entry = new MeasurementEntry(name);
		measurement.insert(name, entry);
		entry.setExpiry(System.currentTimeMillis() + getInitialLifetime());
		//FIX: set clean up here

		return entry;
	}

	/**
	 * find or insert a Measurements entry for child's parent TODO: the logic is
	 * different from the logic of c++ NFD, may need to change
	 *
	 * @param child
	 * @return null if child is the root entry
	 */
	public MeasurementEntry getParentMeasurementEntry(MeasurementEntry child) {
		return getParentMeasurementEntry(child.getName());
	}

	/**
	 * find or insert a Measurements entry for child's parent TODO: the logic is
	 * different from the logic of c++ NFD, may need to change
	 *
	 * @param name
	 * @return null if child is the root entry
	 */
	public MeasurementEntry getParentMeasurementEntry(Name name) {
		if (name.size() == 0) {
			return null;
		}

		MeasurementEntry measurementEntry = measurement.findLongestPrefixMatch(name.getPrefix(-1));
		if (measurementEntry != null) {
			return measurementEntry;
		}

		measurementEntry = new MeasurementEntry(name);
        // Since longest prefix match is used here, so if no result, the root 
		// node have been reached
		measurement.insert(new Name(), measurementEntry);
		measurementEntry.setExpiry(System.currentTimeMillis() + getInitialLifetime());
		//FIX: set clean up here
		return measurementEntry;
	}

	public MeasurementEntry findLongestPrefixMatch(Name name, EntryFilter filter) {
		return measurement.findLongestPrefixMatch(name, filter);
	}

	public MeasurementEntry findLongestPrefixMatch(Name name) {
		return measurement.findLongestPrefixMatch(name);
	}

	public MeasurementEntry findExactMatch(Name name) {
		return measurement.findExactMatch(name);
	}

	public static long getInitialLifetime() {
		return TimeUnit.SECONDS.toMillis(4);
	}

	/**
	 * This method is more rigorous than the current one, but is it necessary?
	 *
	 * @param entry
	 * @param lifetime
	 */
	public void extendLifetime2(MeasurementEntry entry, long lifetime) {
		MeasurementEntry existingEntry = measurement.findExactMatch(entry.getName());
		if (existingEntry == null) {
			return;
		}

		long expiry = System.currentTimeMillis() + lifetime;
		if (existingEntry.getExpiry() > expiry) {
			return; // has longer lifetime, not extending
		}

		//FIX: cancel the current scheduled cleanup
		existingEntry.setExpiry(expiry);
		//FIX: set up new scheduled cleanup
	}

	public void extendLifetime(MeasurementEntry entry, long lifetime) {
		long expiry = System.currentTimeMillis() + lifetime;
		if (entry.getExpiry() > expiry) {
			return; // has longer lifetime, not extending
		}

		//FIX: cancel the current scheduled cleanup
		entry.setExpiry(expiry);
		//FIX: set up new scheduled cleanup
	}

	public int size() {
		return measurement.size();
	}

	private void cleanup(MeasurementEntry entry) {
		measurement.erase(entry.getName());
	}

	private HashMapRepo<MeasurementEntry> measurement = new HashMapRepo<>();
}
