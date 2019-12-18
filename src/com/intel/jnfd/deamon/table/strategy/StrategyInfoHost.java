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
package com.intel.jnfd.deamon.table.strategy;

import com.intel.jnfd.deamon.fw.StrategyInfo;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Haitao Zhang <zhtaoxiang@gmail.com>
 */
public class StrategyInfoHost {

	public StrategyInfo getStrategyInfo(int typeId) {
		return items.get(typeId);
	}

	public void setStrategyInfo(StrategyInfo item) {
		if (item == null) {
			items.remove(item.getTypeId());
		} else {
			items.put(item.getTypeId(), item);
		}
	}

	public <T extends StrategyInfo> StrategyInfo getOrCreateStrategyInfo(
			T newItem)
			throws InstantiationException, IllegalAccessException {
		StrategyInfo oldItem = items.get(newItem.getTypeId());
		if (oldItem != null) {
			return oldItem;
		}
		items.put(newItem.getTypeId(), newItem);
		return newItem;
	}

	public void clearStrategyInfo() {
		items.clear();
	}

	private Map<Integer, StrategyInfo> items = new ConcurrentHashMap<>();
}
