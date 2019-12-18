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
package com.intel.jnfd.deamon.face.tcp;

import java.io.IOException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.util.Blob;

/**
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class Producer implements OnInterestCallback {

	private static final Logger logger = Logger.getLogger(Producer.class.getName());
	private Integer count = 0;
	private Blob blob = buildRandom(100);

	@Override
	public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {
		// synchronized (count) {
		try {
			System.out.println("Interest received: " + interest.toUri());
			Data data = new Data(new Name(interest.getName()).appendSegment(count));
			data.setContent(blob);
			data.getMetaInfo().setFreshnessPeriod(1000);
			face.putData(data);
			//count++;
		} catch (IOException ex) {
			logger.log(Level.SEVERE, "Data write failed.", ex);
		}
		// }
	}

	private static Blob buildRandom(int size) {
		byte[] bytes = new byte[size];
		Random random = new Random();
		random.nextBytes(bytes);
		return new Blob(bytes);
	}
}
