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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;

/**
 *
 * @author Haitao Zhang <zhtaoxiang@gmail.com>
 */
public class DeadNonceNaive implements DeadNonce {

	public static long DEFAULT_LIFETIME = TimeUnit.SECONDS.toMillis(6);

	private final Set<Entry> nonceCache
			= Collections.synchronizedSet(new LinkedHashSet<Entry>());

	public DeadNonceNaive(ScheduledExecutorService scheduler) {
		lifetime = DEFAULT_LIFETIME;
		this.scheduler = scheduler;
	}

	public DeadNonceNaive(ScheduledExecutorService scheduler, long lifetime) {
		this.lifetime = lifetime;
		this.scheduler = scheduler;
		evictTimer = this.scheduler.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				evictStaleEntries();
			}

		},
				1000, this.lifetime, TimeUnit.MILLISECONDS);
	}

	@Override
	public void add(Name name, Blob nonce) {
		nonceCache.add(new Entry(name, nonce));
	}

	@Override
	public void evictStaleEntries() {
		Iterator<Entry> iterator = nonceCache.iterator();
		long currentTimeMillis = System.currentTimeMillis();
		while (iterator.hasNext()) {
			Entry next = iterator.next();
			if (next.getStaleTime() < currentTimeMillis) {
				nonceCache.remove(next);
                // Because we remove the last one, so we need to 
				// get the iterator again.
				iterator = nonceCache.iterator();
			} else {
				break;
			}
		}
	}

	@Override
	public boolean find(Name name, Blob nonce) {
		return nonceCache.contains(new Entry(name, nonce));
	}

	@Override
	public int size() {
		return nonceCache.size();
	}

	private class Entry {

		public Entry(Name name, Blob nonce) {
			this.name = name;
			this.nonce = nonce;
			staleTime = System.currentTimeMillis() + DEFAULT_LIFETIME;
		}

		public Name getName() {
			return name;
		}

		public Blob getNonce() {
			return nonce;
		}

		public long getStaleTime() {
			return staleTime;
		}

		private Name name;
		private Blob nonce;
		private long staleTime;

		@Override
		public final boolean equals(Object o) {
			return name.equals(((Entry) o).getName())
					&& nonce.equals(((Entry) o).getNonce());
		}

	}

	@Override
	public long getLifetime() {
		return lifetime;
	}

	private long lifetime;
	private ScheduledExecutorService scheduler;
	public ScheduledFuture<?> evictTimer;

	public static void main(String[] args) {
		Set<Integer> test
				= Collections.synchronizedSet(new LinkedHashSet<Integer>());
		for (int i = 0; i <= 10; i++) {
			test.add(i);
		}
		Iterator<Integer> iterator = test.iterator();
		while (iterator.hasNext()) {
			Integer next = iterator.next();
			System.out.println(next);
			if (next < 5) {
				test.remove(next);
				iterator = test.iterator();
			}
		}
		iterator = test.iterator();
		while (iterator.hasNext()) {
			System.out.println(iterator.next());
		}
	}

}
