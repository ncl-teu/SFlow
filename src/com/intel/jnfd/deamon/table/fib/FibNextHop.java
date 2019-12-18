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
package com.intel.jnfd.deamon.table.fib;

import com.intel.jndn.forwarder.api.Face;

/**
 *
 * @author Haitao Zhang <zhtaoxiang@gmail.com>
 *     Face, cost (long)から構成される．
 */
public class FibNextHop implements Comparable {

	public FibNextHop(Face face) {
		this.face = face;
	}

	public FibNextHop(Face face, long cost) {
		this.face = face;
		this.cost = cost;
	}

	public Face getFace() {
		return face;
	}

	public long getCost() {
		return cost;
	}

	public void setCost(long cost) {
		this.cost = cost;
	}

	private Face face;
	private long cost;

	@Override
	public int compareTo(Object o) {
		if (cost < ((FibNextHop) o).getCost()) {
			return -1;
		}
		if (cost > ((FibNextHop) o).getCost()) {
			return 1;
		}
		return 0;
	}
}
