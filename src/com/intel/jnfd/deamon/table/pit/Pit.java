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

import com.intel.jndn.forwarder.api.PendingInterestTable;
import com.intel.jnfd.deamon.table.HashMapRepo;
import com.intel.jnfd.deamon.table.Pair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import org.ncl.workflow.util.NCLWUtil;

/**
 *
 * @author Haitao Zhang <zhtaoxiang@gmail.com>
 */
public class Pit implements PendingInterestTable {

	@Override
	public int size() {
		return pit.size();
	}

	/**
	 * inserts a PIT entry for Interest If an entry for exact same name and
	 * selectors exists, that entry is returned.
	 *
	 * @param interest
	 * @return the entry, and true for new entry, false for existing entry
	 */
	@Override
	public Pair<PitEntry> insert(Interest interest) {
		List<PitEntry> pitEntries = pit.findExactMatch(interest.getName());

		if (pitEntries == null) {
			pitEntries = new ArrayList<>();
			PitEntry pitEntry = new PitEntry(interest);

			pitEntries.add(pitEntry);
			pit.insert(interest.getName(), pitEntries);
			return new Pair(pitEntry, true);
		}
		for (PitEntry one : pitEntries) {
			if (one.getInterest().getChildSelector() == interest.getChildSelector()
					&& one.getName().equals(interest.getName())) {

				return new Pair(one, false);
			}
		}
		PitEntry pitEntry = new PitEntry(interest);
		pitEntries.add(pitEntry);
		return new Pair(pitEntry, true);
	}

	@Override
	public List<PitEntry> findLongestPrefixMatches(Data data) {
		List<PitEntry> matches = pit.findLongestPrefixMatch(data.getName());
		return matches == null ? Collections.EMPTY_LIST : matches;
	}

	@Override
	public List<List<PitEntry>> findAllMatches(Data data) {
		List<List<PitEntry>> matches = pit.findAllMatch(data.getName());
		return matches == null ? Collections.EMPTY_LIST : matches;
	}

	@Override
	public void erase(PitEntry pitEntry) {
		List<PitEntry> pitEntries = pit.findExactMatch(pitEntry.getName());
		if (pitEntries == null) {
			return;
		}
		pitEntries.remove(pitEntry);
		if (pitEntries.isEmpty()) {

			if(pitEntry.getName().toUri().startsWith(NCLWUtil.NCLW_PREFIX)){


			}else{
				System.out.println("*****PIT DEL!!!!!!!***");
				pit.erase(pitEntry.getName());
			}


		}
	}

	private final HashMapRepo<List<PitEntry>> pit = new HashMapRepo<>();
}
