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
package com.intel.jndn.forwarder.impl;

import java.util.ArrayList;
import java.util.List;

/**
 * Combines SPI-loaded implementations with a manual registration process
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class ImplementationLoader {

	/**
	 *
	 * @param <T>
	 * @param type
	 * @return
	 */
	public static <T> List<T> load(Class<T> type) {
		java.util.ServiceLoader<T> load = java.util.ServiceLoader.load(type);
		List<T> implementations = new ArrayList<>();

		// add SPI-loaded implementations
		for (T implementation : load) {
			implementations.add(implementation);
		}

		// add manually registered implementations
		for (Object implementation : manuallyRegisteredImplementations) {
			if (type.isInstance(implementation)) {
				implementations.add((T) implementation);
			}
		}

		return implementations;
	}

	/**
	 *
	 * @param implementation
	 */
	public static void register(Object implementation) {
		manuallyRegisteredImplementations.add(implementation);
	}

	/**
	 *
	 */
	public static void clear() {
		manuallyRegisteredImplementations.clear();
	}

	private static final List manuallyRegisteredImplementations = new ArrayList();
}
