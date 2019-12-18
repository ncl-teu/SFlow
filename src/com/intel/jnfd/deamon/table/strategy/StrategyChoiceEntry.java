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

import com.intel.jndn.forwarder.api.Strategy;
import net.named_data.jndn.Name;

/**
 *
 * @author Haitao Zhang <zhtaoxiang@gmail.com>
 */
public class StrategyChoiceEntry {

	public StrategyChoiceEntry(Name prefix) {
		this.prefix = prefix;
		strategy = null;
	}

	public StrategyChoiceEntry(Name prefix, Strategy strategy) {
		this.prefix = prefix;
		this.strategy = strategy;
	}

	public Name getPrefix() {
		return prefix;
	}

	public void setPrefix(Name prefix) {
		this.prefix = prefix;
	}

	public Strategy getStrategy() {
		return strategy;
	}

	public void setStrategy(Strategy strategy) {
		this.strategy = strategy;
	}

	public Name getStrategyName() {
		return strategy.getName();
	}

	private Name prefix;
	private Strategy strategy;
}
