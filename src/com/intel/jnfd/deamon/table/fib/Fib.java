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
package com.intel.jnfd.deamon.table.fib;

import com.intel.jndn.forwarder.api.Face;
import com.intel.jndn.forwarder.api.FaceInformationBase;
import com.intel.jnfd.deamon.table.HashMapRepo;
import com.intel.jnfd.deamon.table.Pair;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;

/**
 *
 * @author Haitao Zhang <zhtaoxiang@gmail.com>
 */
public class Fib implements FaceInformationBase {

	public int size() {
		return fib.size();
	}

	@Override
	public FibEntry findLongestPrefixMatch(Name prefix) {
		return fib.findLongestPrefixMatch(prefix);
	}

	@Override
	public FibEntry findExactMatch(Name prefix) {
		return fib.findExactMatch(prefix);
	}

	public void erase(Name prefix) {
		fib.erase(prefix);
	}

	public void erase(FibEntry entry) {
		fib.erase(entry.getPrefix());
	}

	@Override
	public Pair<FibEntry> insert(Name prefix, Face face, int cost) {
		FibEntry fibEntry = fib.findExactMatch(prefix);
		if (fibEntry != null) {
			FibNextHop nextHop = fibEntry.findNextHop(face);
			if (nextHop != null) {
				nextHop.setCost(cost);
				return new Pair(fibEntry, false);
			}
			fibEntry.addNextHop(face, cost);
			return new Pair(fibEntry, true);
		}
		fibEntry = new FibEntry(prefix);
		fibEntry.addNextHop(face, cost);
		fib.insert(prefix, fibEntry);
		return new Pair(fibEntry, true);
	}

	@Override
	public FibEntry remove(Name prefix) {
		return fib.erase(prefix);
	}

	@Override
	public Collection<FibEntry> list() {
		return fib.values();
	}

	public void removeNextHop(Name prefix, Face face) {
		FibEntry entry = fib.findExactMatch(prefix);
		if (entry == null) {
			return;
		}
		List<FibNextHop> nextHopList = entry.getNextHopList();
		if (nextHopList == null || nextHopList.isEmpty()) {
			fib.erase(prefix);
			return;
		}
		nextHopList.remove(face);
		if (nextHopList.isEmpty()) {
			fib.erase(prefix);
		}
	}

	@Override
	public void removeNextHopFromAllEntries(Face face) {
		Set<Map.Entry<Name, FibEntry>> entrySet = fib.EntrySet();
		for (Map.Entry<Name, FibEntry> one : entrySet) {
			FibEntry fibEntry = one.getValue();
			if (fibEntry == null) {
				fib.erase(one.getKey());
				continue;
			}
			List<FibNextHop> nextHopList = fibEntry.getNextHopList();
			if (nextHopList == null || nextHopList.isEmpty()) {
				fib.erase(one.getKey());
				continue;
			}
			nextHopList.remove(face);
			if (nextHopList.isEmpty()) {
				fib.erase(one.getKey());
			}
		}
	}

	private final HashMapRepo<FibEntry> fib = new HashMapRepo<>();

	@Override
	public void onInterest(Interest interest, Face face) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void onData(Data data, Face incomingFace) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

}
