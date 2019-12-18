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
package com.intel.jndn.forwarder.api.callbacks;

/**
 * Similar interface to {@link java.nio.channels.CompletionHandler} to allow
 * consistent pattern for completing actions;
 * {@link java.nio.channels.CompletionHandler} is not used in case a
 * lower/different version of Java is used (CompletionHandler is published in
 * 1.7).
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 * @param <R> the result type of a successful action
 */
public interface OnCompleted<R> {

	void onCompleted(R result);
}
