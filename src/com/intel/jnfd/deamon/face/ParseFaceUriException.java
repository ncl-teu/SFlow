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
package com.intel.jnfd.deamon.face;

/**
 *
 * @author Haitao Zhang <zhtaoxiang@gmail.com>
 */
public class ParseFaceUriException extends Exception {

	public ParseFaceUriException() {
		super();
	}

	/**
	 * Constructs a new exception with the specified detail message. The cause
	 * is not initialized, and may subsequently be initialized by a call to
	 * {@link #initCause}.
	 *
	 * @param message the detail message. The detail message is saved for later
	 * retrieval by the {@link #getMessage()} method.
	 */
	public ParseFaceUriException(String message) {
		super(message);
	}

	/**
	 * Constructs a new exception with the specified detail message and cause.
	 * <p>
	 * Note that the detail message associated with {@code cause} is <i>not</i>
	 * automatically incorporated in this exception's detail message.
	 *
	 * @param message the detail message (which is saved for later retrieval by
	 * the {@link #getMessage()} method).
	 * @param cause the cause (which is saved for later retrieval by the
	 * {@link #getCause()} method). (A <tt>null</tt> value is permitted, and
	 * indicates that the cause is nonexistent or unknown.)
	 * @since 1.4
	 */
	public ParseFaceUriException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructs a new exception with the specified cause and a detail message
	 * of <tt>(cause==null ? null : cause.toString())</tt> (which typically
	 * contains the class and detail message of <tt>cause</tt>). This
	 * constructor is useful for exceptions that are little more than wrappers
	 * for other throwables (for example, {@link
	 * java.security.PrivilegedActionException}).
	 *
	 * @param cause the cause (which is saved for later retrieval by the
	 * {@link #getCause()} method). (A <tt>null</tt> value is permitted, and
	 * indicates that the cause is nonexistent or unknown.)
	 * @since 1.4
	 */
	public ParseFaceUriException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructs a new exception with the specified detail message, cause,
	 * suppression enabled or disabled, and writable stack trace enabled or
	 * disabled.
	 *
	 * @param message the detail message.
	 * @param cause the cause. (A {@code null} value is permitted, and indicates
	 * that the cause is nonexistent or unknown.)
	 * @param enableSuppression whether or not suppression is enabled or
	 * disabled
	 * @param writableStackTrace whether or not the stack trace should be
	 * writable
	 * @since 1.7
	 */
	protected ParseFaceUriException(String message, Throwable cause,
			boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
