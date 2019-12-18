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
package com.intel.jnfd.deamon.table;

import com.intel.jndn.forwarder.api.NameTable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.named_data.jndn.Name;

/**
 *
 * @author Haitao Zhang <zhtaoxiang@gmail.com>
 * @param <V>
 */
public class HashMapRepo<V> implements NameTable<V> {

	public HashMapRepo() {
		repo = new ConcurrentHashMap<>();
	}

	@Override
	public int size() {
		return repo.size();
	}

	@Override
	public V findLongestPrefixMatch(Name prefix) {
		V result = null;
		for (int i = prefix.size(); i >= 0; i--) {
			Name p = prefix.getPrefix(i);
			result = findExactMatch(p);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	public V findLongestPrefixMatch(Name prefix, EntryFilter filter) {
		V result = null;
		for (int i = prefix.size(); i >= 0; i--) {
			Name p = prefix.getPrefix(i);
			result = findExactMatch(p);
			if (result != null && filter.filt(result)) {
				return result;
			}
		}
		return null;
	}

	@Override
	public List<V> findAllMatch(Name prefix) {
		List<V> result = new ArrayList<>();
		for (int i = prefix.size(); i >= 0; i--) {
			Name p = prefix.getPrefix(i);
			V one = findExactMatch(p);
			if (one != null) {
				result.add(one);
			}
		}
		return result.isEmpty() ? null : result;
	}

	@Override
	public V findExactMatch(Name prefix) {
		return repo.get(prefix);
	}

	@Override
	public void insert(Name prefix, V value) {
		repo.put(prefix, value);
	}

	@Override
	public V erase(Name prefix) {
		return repo.remove(prefix);
	}

	@Override
	public boolean hasKey(Name key) {
		return repo.containsKey(key);
	}

	@Override
	public Set<Entry<Name, V>> EntrySet() {
		return repo.entrySet();
	}

	@Override
	public Collection<V> values() {
		return repo.values();
	}

	private final Map<Name, V> repo;
}
