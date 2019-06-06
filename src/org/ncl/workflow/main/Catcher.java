package org.ncl.workflow.main;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;

/**
 * Created by Hidehiro Kanemitsu on 2019/05/10
 */
public class Catcher extends Thread {
    Reader in;
    StringWriter out = new StringWriter();
    public Catcher(Reader in) {
        this.in = in;
    }

    public void run() {
        int c;
        try {
            while ((c = in.read()) != -1) {
                out.write((char)c);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
