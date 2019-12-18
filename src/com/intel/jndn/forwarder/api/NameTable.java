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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.named_data.jndn.Name;

/**
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 * @param <V>
 */
public interface NameTable<V> {

	public Set<Map.Entry<Name, V>> EntrySet();

	public Collection<V> values();

	public void insert(Name prefix, V value);

	public V erase(Name prefix);

	public V findExactMatch(Name prefix);

	public V findLongestPrefixMatch(Name prefix);

	public List<V> findAllMatch(Name prefix);

	public boolean hasKey(Name key);

	public int size();
}
