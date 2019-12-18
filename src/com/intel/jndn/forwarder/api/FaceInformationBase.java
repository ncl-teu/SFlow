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

import com.intel.jndn.forwarder.api.callbacks.OnDataReceived;
import com.intel.jndn.forwarder.api.callbacks.OnInterestReceived;
import com.intel.jnfd.deamon.table.Pair;
import com.intel.jnfd.deamon.table.fib.FibEntry;
import java.util.Collection;
import net.named_data.jndn.Name;

/**
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public interface FaceInformationBase extends OnInterestReceived, OnDataReceived {

	public Pair<FibEntry> insert(Name prefix, Face face, int cost);

	public FibEntry remove(Name prefix);

	public Collection<FibEntry> list();

	public FibEntry findLongestPrefixMatch(Name prefix);

	public FibEntry findExactMatch(Name prefix);

	public void removeNextHopFromAllEntries(Face face);
}
