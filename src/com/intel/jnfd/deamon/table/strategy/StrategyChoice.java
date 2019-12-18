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

import com.intel.jndn.forwarder.api.StrategyChoiceTable;
import com.intel.jndn.forwarder.api.Strategy;
import com.intel.jnfd.deamon.table.HashMapRepo;
import com.intel.jnfd.deamon.table.Pair;
import java.util.Collection;
import com.intel.jnfd.deamon.table.pit.PitEntry;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import net.named_data.jndn.Name;

/**
 *
 * @author Haitao Zhang <zhtaoxiang@gmail.com>
 */
public class StrategyChoice implements StrategyChoiceTable {

	public StrategyChoice(Strategy defaultStrategy) {
		setDefaultStrategy(defaultStrategy);
	}

	@Override
	public int size() {
		return strategyChoice.size();
	}

	@Override
	public boolean hasStrategy(Name strategyName, boolean isExact) {
		if (isExact) {
			return strategyInstances.containsKey(strategyName);
		} else {
			return getStrategy(strategyName) != null;
		}
	}

	@Override
	public boolean install(Strategy strategy) {
		Name strategyName = strategy.getName();
		if (hasStrategy(strategyName, true)) {
			return false;
		}
		strategyInstances.put(strategyName, strategy);
		return true;
	}

	/*
	 * This method is the right code in the future. (After we implement 
	 * clearStrategyInfoInEffectiveSubTree)
	 * @param prefix
	 * @param strategyName
	 * @return
	 public boolean insert(Name prefix, Name strategyName) {
	 Strategy strategy = getStrategy(strategyName);
	 if (strategy == null) { // the strategyName does not exist
	 return false;
	 }
	 StrategyChoiceEntry entry = strategyChoice.findExactMatch(prefix);
	 Strategy oldStrategy = null;
	 if (entry != null) {
	 if (entry.getStrategy().getName().equals(strategy.getName())) {
	 return true; //no change
	 }
	 oldStrategy = entry.getStrategy();
	 } else {
	 oldStrategy = findEffectiveStrategy(prefix);
	 entry = new StrategyChoiceEntry(prefix);
	 strategyChoice.insert(prefix, entry);
	 }
	 clearStrategyInfoInEffectiveSubTree(entry, oldStrategy, strategy);
	 entry.setStrategy(strategy);
	 return true;
	 }*/
	@Override
	public boolean insert(Name prefix, Name strategyName) {
		Strategy strategy = getStrategy(strategyName);
		if (strategy == null) { // the strategyName does not exist
			return false;
		}
		// simply insert the new policy
		StrategyChoiceEntry entry = strategyChoice.findExactMatch(prefix);
		if (entry == null
				|| !entry.getStrategy().getName().equals(strategy.getName())) {
			strategyChoice.insert(prefix, new StrategyChoiceEntry(prefix, strategy));
		}
		return true;
	}

	/*
	 * When erase a strategy, the strategy info in the PITs of all the effective
	 * subtree nodes should also be removed, this is right for future code
	 * @param prefix 
     
	 public void erase(Name prefix) {
	 // The root strategy should not be erased.
	 if(prefix.size() == 0) {
	 return;
	 }
        
	 StrategyChoiceEntry strategyChoiceEntry 
	 = strategyChoice.findExactMatch(prefix);
	 if(strategyChoiceEntry == null) {
	 return;
	 }
	 Strategy oldStrategy = strategyChoiceEntry.getStrategy();
	 Strategy parentStrategy = findEffectiveStrategy(prefix.getPrefix(-1));
	 clearStrategyInfoInEffectiveSubTree(
	 strategyChoiceEntry, oldStrategy, parentStrategy);
	 strategyChoice.erase(prefix);
	 }*/
	@Override
	public void erase(Name prefix) {
		// The root strategy should not be erased.
		if (prefix.size() == 0) {
			return;
		}

		strategyChoice.erase(prefix);
	}

	@Override
	public Collection<StrategyChoiceEntry> list() {
		return strategyChoice.values();
	}

	/**
	 * get strategy Name of prefix.
	 *
	 * @param prefix
	 * @return true and strategyName at exact match, or false
	 */
	public Pair<Name> get(Name prefix) {
		StrategyChoiceEntry strategyChoiceEntry
				= strategyChoice.findExactMatch(prefix);
		if (strategyChoiceEntry == null) {
			return new Pair(new Name(), false);
		}
		return new Pair(strategyChoiceEntry.getStrategyName(), true);
	}

	@Override
	public Strategy findEffectiveStrategy(Name prefix) {
		StrategyChoiceEntry strategyChoiceEntry
				= strategyChoice.findLongestPrefixMatch(prefix);
		if (strategyChoiceEntry != null) {
			return strategyChoiceEntry.getStrategy();
		}
		return null;
	}

	private Strategy getStrategy(Name strategyName) {
		Strategy result = null;
		for (Entry<Name, Strategy> one : strategyInstances.entrySet()) {
			switch (one.getKey().size() - strategyName.size()) {
				case 0: // exact match
					return one.getValue();
				case 1: // unversioned strategyName matches versioned strategy
					result = one.getValue();
					break;
			}
		}
		return result;
	}

	private void setDefaultStrategy(Strategy strategy) {
		install(strategy);
		Name rootName = new Name();
		StrategyChoiceEntry entry = new StrategyChoiceEntry(rootName);
		entry.setStrategy(strategy);
		strategyChoice.insert(rootName, entry);
	}

	/**
	 * This is used to clear the strategy info in the PITs of all the effective
	 * subtree nodes. TODO: implement this in the future. Since to simplify the
	 * debug process, we do not store strategy info in the PIT.
	 *
	 * @param entry
	 * @param oldStrategy
	 * @param newStrategy
	 */
	private void clearStrategyInfoInEffectiveSubTree(StrategyChoiceEntry entry,
			Strategy oldStrategy, Strategy newStrategy) {

		if (oldStrategy.equals(newStrategy)) {
			return;
		}

	}

	private HashMapRepo<StrategyChoiceEntry> strategyChoice
			= new HashMapRepo<>();
	private Map<Name, Strategy> strategyInstances = new ConcurrentSkipListMap<>();

	public void findEffectiveStrategy(PitEntry pitEntry) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
}
