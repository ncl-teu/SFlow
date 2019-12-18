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

import com.intel.jndn.forwarder.api.Face;
import com.intel.jnfd.deamon.fw.ForwardingPipeline;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.named_data.jndn.ControlParameters;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.encoding.TlvWireFormat;
import net.named_data.jndn.encoding.WireFormat;
import net.named_data.jndn.util.Blob;

/**
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class RegisterPrefixCommand {

	private static final Logger logger = Logger.getLogger(RegisterPrefixCommand.class.getName());
	public static final Name PREFIX = new Name("/localhost/nfd/rib/register");

	/**
	 * @param name the name of an incoming interest
	 * @return true if the given name begins with the command {@link #PREFIX}
	 */
	public boolean matches(Name name) {
		return PREFIX.match(name);
	}

	/**
	 * Parse a register prefix command, adding a new routing entry into the FIB;
	 * this minimal implementation does not yet validate signatures or handle
	 * any faces other than the incoming face
	 *
	 * @param command the command interest, see
	 * http://redmine.named-data.net/projects/nfd/wiki/ControlCommand#ControlParameters
	 * @param incomingFace the face on which the interest has been read
	 * @param pipeline the current forwarding pipeline
	 */
	public void call(Interest command, Face incomingFace, ForwardingPipeline pipeline) {
		try {
			logger.info(command.toUri());
			ControlParameters controlParameters = findParameters(command);
			pipeline.getFib().insert(controlParameters.getName(), incomingFace, controlParameters.getCost());

			Data response = buildResponse(command, 200, "OK");
			incomingFace.sendData(response);
		} catch (EncodingException ex) {
			logger.log(Level.WARNING, "Incorrect command encoding.", ex);

			// see http://redmine.named-data.net/projects/nfd/wiki/ControlCommand#StatusCode
			Data response = buildResponse(command, 400, "ControlParameters is incorrect");
			incomingFace.sendData(response);
		}
	}

	/**
	 * Parse parameters from the given interest command
	 *
	 * @param command the signed interest command
	 * @return a valid control parameters object
	 * @throws EncodingException if the interest is malformed
	 */
	private ControlParameters findParameters(Interest command) throws EncodingException {
		if (command.getName().size() < PREFIX.size() + 1) {
			throw new EncodingException("Expected control parameters after prefix: " + PREFIX.toUri());
		}
		Name.Component component = command.getName().get(PREFIX.size());
		ControlParameters parameters = new ControlParameters();
		Blob value = component.getValue();
// FIX: the commented code should be right, but for some reason, the inner code
//        WireFormat.getDefaultWireFormat() will return null;
//        parameters.wireDecode(value);
		parameters.wireDecode(value, new TlvWireFormat());
		return parameters;
	}

	/**
	 * Build a data in control response format
	 *
	 * @param interest the incoming interest
	 * @param code the status code (e.g. 200, 400, 404, etc.)
	 * @param text the status text
	 * @return a control response data, see see
	 * http://redmine.named-data.net/projects/nfd/wiki/ControlCommand#StatusCode
	 */
	private Data buildResponse(Interest interest, int code, String text) {
		ControlResponse response = new ControlResponse();
		response.setStatusCode(code);
		response.setStatusText(text);

		Data data = new Data(interest.getName());
		data.setContent(response.wireEncode());
		data.getMetaInfo().setFreshnessPeriod(0);

		return data;
	}
}
