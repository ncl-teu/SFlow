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

import com.intel.jnfd.deamon.table.Pair;
import com.intel.jnfd.deamon.table.pit.PitEntry;
import java.util.List;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;

/**
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public interface PendingInterestTable {

	public Pair<PitEntry> insert(Interest interest);

	public void erase(PitEntry pitEntry);

	public List<List<PitEntry>> findAllMatches(Data data);

	public List<PitEntry> findLongestPrefixMatches(Data data);

	public int size();
}
