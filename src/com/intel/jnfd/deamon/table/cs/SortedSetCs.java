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
package com.intel.jnfd.deamon.table.cs;

import com.intel.jndn.forwarder.api.ContentStore;
import com.intel.jnfd.util.NameUtil;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import org.ncl.workflow.util.NCLWUtil;

/**
 *
 * @author Haitao Zhang <zhtaoxiang@gmail.com>
 */
public class SortedSetCs implements ContentStore {

	public SortedSetCs(int maxNumberOfDataPackets) {
		limit(maxNumberOfDataPackets);
	}

	public SortedSetCs() {
	}

	@Override
	public boolean insert(Data data, boolean isUnsolicited) {
		// TODO: recognize CachingPolicy
		// this part should be added after the jNDN library add
		// "LocalControlHeader" attribute

		if (limit() == 0) {
			return false;
		}

		if (dataCache.size() >= limit()) {
			evictOldestData();
		}

		CsEntry newEntry = new CsEntry(data, isUnsolicited);
		fifoQueue.add(newEntry);
		dataCache.put(data.getName(), newEntry);

		return true;
	}

	private void evictOldestData() {
		// evict oldest first; note: that queue may contain records already erased
		boolean evicted = false;
		while (!evicted) {
			CsEntry oldEntry = fifoQueue.poll();
			if (oldEntry != null) {
				evicted = dataCache.remove(oldEntry.getName()) != null;
			}
		}
	}

	/**
	 *
	 * @param interest the interest to search with
	 * @param searchCsCallback the class called when the operation completes
	 */
	@Override
	public void find(Interest interest, SearchCsCallback searchCsCallback) {
		Name prefix = interest.getName();
		boolean isRightMost = (interest.getChildSelector() == 1);
		CsEntry match = null;
		//Interest in = null;
		if(interest.getName().toUri().startsWith(NCLWUtil.NCLW_PREFIX)){

		}else{

		}


			if (isRightMost) {
			match = findRightMost(interest, prefix, NameUtil.getNameSuccessor(prefix));
		} else {
			match = findLeftMost(interest, prefix, NameUtil.getNameSuccessor(prefix));
		}
		if (match == null) {
			searchCsCallback.onContentStoreMiss(interest);
			return;
		}
		searchCsCallback.onContentStoreHit(interest, match.getData());
	}

	@Override
	public Data erase(Name exactName) {
		CsEntry removed = dataCache.remove(exactName);
		return removed == null ? null : removed.getData();
	}

	private CsEntry findRightMost(Interest interest, Name first, Name last) {
		// Each loop visits a sub-namespace under a prefix one component longer than Interest Name.
		// If there is a match in that sub-namespace, the leftmost match is returned;
		// otherwise, loop continues.
		int interestNameLength = interest.getName().size();

		for (Name right = last; !right.equals(first);) {
			Name prev = dataCache.lowerKey(right);

			// special case: [first,prev] have exact Names
			if (prev.size() == interestNameLength) {
				return findRightmostAmongExact(interest, first, right);
			}

			Name prefix = prev.getPrefix(interestNameLength + 1);
			Name left = dataCache.ceilingKey(prefix);
			// normal case: [left,right) are under one-component-longer prefix
			CsEntry match = findLeftMost(interest, left, right);
			if (match != null) {
				return match;
			}

			right = left;
		}
		return null;
	}

	private CsEntry findRightmostAmongExact(Interest interest, Name first, Name last) {
		ConcurrentNavigableMap<Name, CsEntry> subMap
				= dataCache.subMap(first, true, last, false);
		if (subMap == null) {
			return null;
		}
		Iterator<Name> descendingIterator = subMap.keySet().descendingIterator();
		while (descendingIterator.hasNext()) {
			Name dataName = descendingIterator.next();
			if (dataCache.get(dataName).canSatisfy(interest)) {
				return dataCache.get(dataName);
			}
		}
		return null;
	}

	private CsEntry findLeftMost(Interest interest, Name first, Name last) {
		ConcurrentNavigableMap<Name, CsEntry> subMap = null;
		try {
			subMap = dataCache.subMap(first, true, last, false);
		} catch (Exception e) {
			System.out.println("Error: " + interest.toUri() + ", " + first.toUri() + ", " + last.toUri());
			e.printStackTrace();
		}

		if (subMap == null) {
			return null;
		}
		Iterator<Name> iterator = subMap.keySet().iterator();
		while (iterator.hasNext()) {
			Name dataName = iterator.next();
			if (dataCache.get(dataName).canSatisfy(interest)) {
				return dataCache.get(dataName);
			}
		}
		return null;
	}

	@Override
	public int size() {
		return dataCache.size();
	}

	@Override
	public int limit() {
		return maxNumberOfDataPackets;
	}

	@Override
	public void limit(int maxNumberOfDataPackets) {
		this.maxNumberOfDataPackets = maxNumberOfDataPackets;
	}

	private final ConcurrentSkipListMap<Name, CsEntry> dataCache = new ConcurrentSkipListMap<>();
	private final Queue<CsEntry> fifoQueue = new ConcurrentLinkedQueue();
	private int maxNumberOfDataPackets = Integer.MAX_VALUE;
}
