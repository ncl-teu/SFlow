package org.ncl.workflow.util;

import ch.ethz.ssh2.Session;

import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Created by Hidehiro Kanemitsu on 2019/08/06.
 */
public class SshCommandExecute {
    private Session session;
    private InputStreamBuffering stdout;
    private InputStreamBuffering stderr;
    private OutputStreamWriter stdin;

    public SshCommandExecute(Session _session) {
        session = _session;
    }

    public void exec(String command) throws IOException {
        session.execCommand(command);

        stdout = new InputStreamBuffering(session.getStdout());
        stderr = new InputStreamBuffering(session.getStderr());
        stdin = new OutputStreamWriter(session.getStdin());
    }

    public void exec(String command, long timeout) throws IOException, InterruptedException {
        exec(command);
        stdout.join(timeout);
    }

    public void join(long timeout) throws InterruptedException {
        stdout.join(timeout);
    }

    public OutputStreamWriter getStdin() {
        return stdin;
    }

    public InputStreamBuffering getStdout() {
        return stdout;
    }

    public InputStreamBuffering getStderr() {
        return stderr;
    }

    public int getExitStatus() {
        return session.getExitStatus().intValue();
    }

    public void close() {
        session.close();
    }
}
