package org.ncl.workflow.comm;

import net.gripps.cloud.nfv.NFVEnvironment;
import net.gripps.cloud.nfv.sfc.SFC;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;

/**
 * Created by Hidehiro Kanemitsu on 2019/05/01.
 */
public class NCLWData implements Serializable {
    /**
     * read path of the file to be sent.
     */
    private String readFilePath;

    /**
     * write path of the arrived input file.
     */
    private String writeFilePath;

    /**
     * Src taskID
     */
    private long fromTaskID;

    /**
     * Dest. task ID
     */
    private long toTaskID;

    /**
     * IP address of the target node.
     */
    private String ipAddr;

    /**
     * If the data to be transfered is file, true/ otherwise, false.
     */
    private boolean isFile;

    /**
     * target port number.
     */
    private int portNumber;

    /**
     * Output File
     */
    private File file;

    /**
     * Output message
     */
    private String msg;

    /**
     * SFC object  after scheduling.
     */
    private SFC sfc;

    /**
     * Environment after scheduling
     */
    private NFVEnvironment env;

    /**
     * Whole workflow job.
     */
    private WorkflowJob job;

    //private FileInputStream fis;

    private byte[] bytes;


    public NCLWData(long fromTaskID, long toTaskID, String ipAddr, int portNumber, SFC sfc, NFVEnvironment env, WorkflowJob job) {
        this.fromTaskID = fromTaskID;
        this.toTaskID = toTaskID;
        this.ipAddr = ipAddr;
        this.portNumber = portNumber;
        this.sfc = sfc;
        this.env = env;
        this.job = job;
    }

    public NCLWData(String readFilePath, String writeFilePath, long fromTaskID, long toTaskID,
                    String ipAddr, boolean isFile, int portNumber) {
        this.readFilePath = readFilePath;
        this.writeFilePath = writeFilePath;
        this.fromTaskID = fromTaskID;
        this.toTaskID = toTaskID;
        this.ipAddr = ipAddr;
        this.isFile = isFile;
        this.portNumber = portNumber;
        this.file = file;
        this.msg = msg;
        this.sfc = null;
        this.job = null;
    }

    public NCLWData(String readFilePath, String writeFilePath,
                    long fromTaskID, long toTaskID, String ipAddr, boolean isFile, int portNumber, SFC sfc, NFVEnvironment env, WorkflowJob job) {
        this.readFilePath = readFilePath;
        this.writeFilePath = writeFilePath;
        this.fromTaskID = fromTaskID;
        this.toTaskID = toTaskID;
        this.ipAddr = ipAddr;
        this.isFile = isFile;
        this.portNumber = portNumber;
        this.sfc = sfc;
        this.env = env;
        this.job = job;
    }

    public String getReadFilePath() {
        return readFilePath;
    }

    public void setReadFilePath(String readFilePath) {
        this.readFilePath = readFilePath;
    }

    public String getWriteFilePath() {
        return writeFilePath;
    }

    public void setWriteFilePath(String writeFilePath) {
        this.writeFilePath = writeFilePath;
    }

    public long getFromTaskID() {
        return fromTaskID;
    }

    public void setFromTaskID(long fromTaskID) {
        this.fromTaskID = fromTaskID;
    }

    public long getToTaskID() {
        return toTaskID;
    }

    public void setToTaskID(long toTaskID) {
        this.toTaskID = toTaskID;
    }

    public String getIpAddr() {
        return ipAddr;
    }

    public void setIpAddr(String ipAddr) {
        this.ipAddr = ipAddr;
    }

    public boolean isFile() {
        return isFile;
    }

    public void setFile(boolean file) {
        isFile = file;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(int portNumber) {
        this.portNumber = portNumber;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public SFC getSfc() {
        return sfc;
    }

    public void setSfc(SFC sfc) {
        this.sfc = sfc;
    }

    public NFVEnvironment getEnv() {
        return env;
    }

    public void setEnv(NFVEnvironment env) {
        this.env = env;
    }

    public WorkflowJob getJob() {
        return job;
    }

    public void setJob(WorkflowJob job) {
        this.job = job;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }
}
