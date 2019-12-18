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
package com.intel.jnfd.deamon.table.deadnonce;

import java.util.concurrent.TimeUnit;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;

/**
 *
 * @author Haitao Zhang <zhtaoxiang@gmail.com>
 */
public class DeadNonceStandard implements DeadNonce {

	public static long DEFAULT_LIFETIME = TimeUnit.SECONDS.toMillis(6);
	public static long MIN_LIFETIME = TimeUnit.MILLISECONDS.toMillis(1);
	public static int INITIAL_CAPACITY = (1 << 7);
	public static int MIN_CAPACITY = (1 << 3);
	public static int MAX_CAPACITY = (1 << 24);
//const DeadNonceList::Entry DeadNonceList::MARK = 0;
	public static int EXPECTED_MARK_COUNT = 5;
	public static double CAPACITY_UP = 1.2;
	public static double CAPACITY_DOWN = 0.9;
	public static int EVICT_LIMIT = (1 << 6);

	/**
	 * the unit of the lifetime is millisecond
	 *
	 * @param lifetime
	 */
	public DeadNonceStandard(long lifetime/*millisecond*/) {
		this.lifetime = lifetime;
		capacity = INITIAL_CAPACITY;
	}

	@Override
	public boolean find(Name name, Blob nonce) {
		return false;
	}

	@Override
	public void add(Name name, Blob nonce) {

	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public long getLifetime() {
		return lifetime;
	}

	private int countMarks() {
		return 0;

	}

	private void mark() {

	}

	private void adjustCapacity() {

	}

	private void evictEntries() {

	}

	private long makeEntry(Name name, long nonce) {
		return 0;
	}
	private long lifetime;
	private int capacity;

	@Override
	public void evictStaleEntries() {
		throw new UnsupportedOperationException("Not supported yet.");
//To change body of generated methods, choose Tools | Templates.
	}
}
