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

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * All the constructors only accept IP address (either as String or
 * InetAddress). If host names are used, please use FaceUri.getAddressesByName
 * to resolve and choose the proper IP address first.
 *
 * @author Haitao Zhang <zhtaoxiang@gmail.com>
 */
public class FaceUri {

	/**
	 * resolve the host name and return all the possible IP addresses.
	 *
	 * @param name
	 * @return
	 */
	public static InetAddress[] getAddressesByName(String name) {
		try {
			return InetAddress.getAllByName(name);
		} catch (UnknownHostException ex) {
			Logger.getLogger(FaceUri.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}

	public FaceUri(String uri) throws ParseFaceUriException, UnknownHostException {
		Pattern pattern = Pattern.compile(faceUriRegex);
		Matcher matcher = pattern.matcher(uri);
		if (matcher.matches()) {
			scheme = matcher.group("Scheme");
			String ip = matcher.group("Host");
			inet = InetAddress.getByName(ip);
			isV6 = inet instanceof Inet6Address;
			if (matcher.group("Port") != null) {
				port = Integer.parseInt(matcher.group("Port"));
			}
		} else {
			throw new ParseFaceUriException("The format of the provided face URI is wrong!");
		}
	}

	public FaceUri(String scheme, String ip, int port) throws ParseFaceUriException, UnknownHostException {
		Pattern schemePattern = Pattern.compile(schemeRegex);
		Matcher schemeMatcher = schemePattern.matcher(scheme);
		if (schemeMatcher.matches()) {
			this.scheme = scheme;
		} else {
			throw new ParseFaceUriException("The format of the provided face URI is wrong!");
		}
		inet = InetAddress.getByName(ip);
		isV6 = inet instanceof Inet6Address;
		if (65536 > port && 0 <= port) {
			this.port = port;
		} else {
			throw new ParseFaceUriException("The format of the provided face URI is wrong!");
		}
	}

	public FaceUri(String scheme, InetAddress inet, int port) throws ParseFaceUriException {
		this.scheme = scheme;
		this.port = port;
		this.inet = inet;
		isV6 = inet instanceof Inet6Address;
		if (!isConsistency()) {
			throw new ParseFaceUriException("The format of the provided face URI is wrong!");
		}
	}

	public FaceUri(String scheme, InetSocketAddress inetSocketAddress) throws ParseFaceUriException {
		port = inetSocketAddress.getPort();
		inet = inetSocketAddress.getAddress();
		isV6 = inet instanceof Inet6Address;
		this.scheme = scheme;
		if (!isConsistency()) {
			throw new ParseFaceUriException("The format of the provided face URI is wrong!");
		}
	}

	/**
	 * Used by constructors Judge if the inetSocketAddress and scheme are
	 * consistent or not, and make the scheme completed.
	 *
	 * @return
	 */
	private boolean isConsistency() {
		if (isV6) {
			if (scheme.endsWith("4")) {
				return false;
			}
			if (!scheme.endsWith("6")) {
				scheme = scheme + "6";
			}
			return true;
		} else {
			if (scheme.endsWith("6")) {
				return false;
			}
			if (!scheme.endsWith("4")) {
				scheme = scheme + "4";
			}
			return true;
		}
	}

	public String getScheme() {
		return scheme;
	}

	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public boolean getIsV6() {
		return isV6;
	}

	public void setIsV6(boolean isV6) {
		this.isV6 = isV6;
	}

	public InetAddress getInet() {
		return inet;
	}

	public void setInet(InetAddress inet) {
		this.inet = inet;
	}

	/**
	 * When the scheme, host and port fields are equal, then the two FaceUris
	 * are equal.
	 *
	 * @param o
	 * @return
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof FaceUri) {
			FaceUri other = ((FaceUri) o);
			if (scheme.equals(other.getScheme())
					&& inet.equals(other.getInet())
					&& other.getPort() == port
					&& isV6 == other.getIsV6()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 79 * hash + Objects.hashCode(this.scheme);
		hash = 79 * hash + this.port;
		hash = 79 * hash + (this.isV6 ? 1 : 0);
		hash = 79 * hash + Objects.hashCode(this.inet);
		return hash;
	}

	@Override
	public String toString() {
		return String.format("%s://%s:%s", scheme, inet.getHostAddress(), port);
	}

	private String scheme;
	private int port;
	private boolean isV6;
	private InetAddress inet;

//    private static final String faceUriRegex = "(?<Scheme>(tcp|udp)[46]?):\\/\\/" //the scheme part
//            // between this is the host part
//            + "(?<Host>localhost|" //local host
//            + "((2[0-4]\\d|25[0-5]|[01]?\\d\\d?)\\.){3}(2[0-4]\\d|25[0-5]|[01]?\\d\\d?)|" //ipv4 address
//            + "(\\[[\\dA-Fa-f:]*\\])|" //basic check of ipv6 address
//            + "(([\\w-]+\\.)+\\w{2,6}))" //host name
//            //
//            + "(:(?<Port>[0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5]))?"; //the port part
	private static final String faceUriRegex = "(?<Scheme>(tcp|udp)[46]?):\\/\\/" //the scheme part
			// between this is the host part
			+ "(?<Host>"
			+ "((2[0-4]\\d|25[0-5]|[01]?\\d\\d?)\\.){3}(2[0-4]\\d|25[0-5]|[01]?\\d\\d?)|" //ipv4 address
			+ "(\\[[\\dA-Fa-f:]*\\]))" //basic check of ipv6 address
			//
			+ "(:(?<Port>[0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5]))?"; //the port part

	private static final String schemeRegex = "(tcp|udp)[46]?";
}
