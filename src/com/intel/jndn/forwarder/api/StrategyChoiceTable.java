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

import com.intel.jndn.forwarder.api.Strategy;
import com.intel.jnfd.deamon.table.Pair;
import com.intel.jnfd.deamon.table.strategy.StrategyChoiceEntry;
import java.util.Collection;
import net.named_data.jndn.Name;

/**
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public interface StrategyChoiceTable {

	public boolean install(Strategy strategy);

	public boolean insert(Name prefix, Name strategyName);

	public boolean hasStrategy(Name strategyName, boolean isExact);

	public void erase(Name prefix);

	public Strategy findEffectiveStrategy(Name prefix);

	public Collection<StrategyChoiceEntry> list();

	public int size();

}
