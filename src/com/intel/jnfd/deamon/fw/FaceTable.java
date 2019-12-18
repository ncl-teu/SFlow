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
package com.intel.jnfd.deamon.fw;

import com.intel.jndn.forwarder.api.Face;
import com.intel.jnfd.util.NfdCommon;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Haitao Zhang <zhtaoxiang@gmail.com>
 */
public class FaceTable {

	/// indicates an invalid FaceId
	public static final int INVALID_FACEID = -1;
	/// identifies the InternalFace used in management
	public static final int FACEID_INTERNAL_FACE = 1;
	/// identifies a packet comes from the ContentStore, in LocalControlHeader incomingFaceId
	public static final int FACEID_CONTENT_STORE = 254;
	/// identifies the NullFace that drops every packet
	public static final int FACEID_NULL = 255;
	/// upper bound of reserved FaceIds
	public static final int FACEID_RESERVED_MAX = 255;

	public FaceTable(ForwardingPipeline forwarder) {
		this.forwarder = forwarder;
		lastFaceId = FACEID_RESERVED_MAX;
		logger.setLevel(NfdCommon.LOG_LEVEL);
	}

	//TODO: question, where does the initial faceId come from?
	public int  add(Face face) {
		if (face.getFaceId() != INVALID_FACEID && faces.get(face.getFaceId()) != null) {
			return -1;
		}
		int faceId = ++this.lastFaceId;
		if (faceId < FACEID_RESERVED_MAX) {
			return -1;
		}
		Iterator<Face> fIte = this.faces.values().iterator();
		boolean found = false;
		while(fIte.hasNext()){
			Face f = fIte.next();
			String remoteAddr = f.getRemoteUri().getInet().getHostAddress();
			String orgAddr = face.getRemoteUri().getInet().getHostAddress();
			if(orgAddr.equals(remoteAddr)){
				found = true;
				break;
			}
		}
		if(found){
			return -1;
		}else{
			addImp(face, faceId);

		}
		return faceId;
	}


	public void addImp(Face face, int faceId) {
		face.setFaceId(faceId);
		faces.put(faceId, face);
		logger.log(Level.INFO, "Add face to FaceTable: {0}", face);
	}

	// add a special Face with a reserved FaceId
	public void addReserved(Face face, int faceId) {
		if (face.getFaceId() != INVALID_FACEID
				|| faces.get(face.getFaceId()) != null
				|| faceId > FACEID_RESERVED_MAX) {
			return;
		}
		addImp(face, faceId);
	}

	public Face get(int faceId) {
		return faces.get(faceId);
	}

	public int size() {
		return faces.size();
	}

    // remove is private because it's a handler of face.onFail signal.
	// face->close() closes the face and triggers .remove()
	public void remove(Face face, String reason) {
		int faceId = face.getFaceId();
		faces.remove(faceId);
		logger.log(Level.INFO, "Remove face from faceTable: {0}", face);
		face.setFaceId(INVALID_FACEID);

		forwarder.getFib().removeNextHopFromAllEntries(face);
	}

	private final ForwardingPipeline forwarder;
	private int lastFaceId;
	private final Map<Integer, Face> faces = new HashMap<>();
	private static final Logger logger = Logger.getLogger(FaceTable.class.getName());
}
