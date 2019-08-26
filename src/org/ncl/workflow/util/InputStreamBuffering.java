package org.ncl.workflow.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * Created by Hidehiro Kanemitsu on 2019/08/06.
 */
public class InputStreamBuffering extends Thread {
    private InputStream input;
    private ByteArrayOutputStream output = new ByteArrayOutputStream();
    private Exception exception = null;
    private static final int BUFFER_SIZE = 4096;

    public InputStreamBuffering(InputStream _input) {
        input = _input;
        start();
    }

    public void run() {
        try {
            byte[] buffer = new byte[BUFFER_SIZE];
            int length;
            while ((length = input.read(buffer)) > 0) synchronized (output) {
                output.write(buffer, 0, length);
            }
        } catch (Exception ex) {
            exception = ex;
        }
    }

    public Exception getException() {
        return exception;
    }

    public byte[] getByteArray() {
        return output.toByteArray();
    }

    public String toString() {
        synchronized(output) { try{return output.toString();} finally {output.reset();} }
    }

    public String toString(String encoding) throws UnsupportedEncodingException {
        synchronized(output) { try{return output.toString(encoding);} finally {output.reset();} }
    }
}

