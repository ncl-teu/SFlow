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

import com.intel.jnfd.deamon.table.cs.SearchCsCallback;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;

/**
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public interface ContentStore {

	/**
	 * @param data the data to insert
	 * @param isUnsolicited true if the data was not solicited by an interest;
	 * content stores may use this information in their eviction policies
	 * @return true if the data was added to the content store, false otherwise;
	 * this is necessary because of different caching policies
	 */
	public boolean insert(Data data, boolean isUnsolicited);

	/**
	 * @param exactName the exact name of the data to erase
	 * @return the data erased
	 */
	public Data erase(Name exactName);

	/**
	 * Find a data packet asynchronously; necessary due to future disk-based
	 * content stores
	 *
	 * @param interest the interest to search with
	 * @param searchCsCallback the class called when the operation completes
	 */
	public void find(Interest interest, SearchCsCallback searchCsCallback);

	/**
	 * @return the number of data packets in the content store
	 */
	public int size();

	/**
	 * @return the maximum number of data packets allowed in the content store
	 */
	public int limit();

	/**
	 * Change the maximum number of data packets allowed
	 *
	 * @param maxNumberOfDataPackets the new maximum limit
	 */
	public void limit(int maxNumberOfDataPackets);
}
