package net.gripps.clustering.common.aplmodel;

/**
 * Created by kanemih on 2016/04/22.
 */
public class FileInfo {

    private String fileName;

    private long dataSize;

    /**
     * inputに当たるID集合
     */
    private CustomIDSet toIDSet;

    /**
     * outputにあたるID集合
     */
    private CustomIDSet fromIDSet;

    public FileInfo(String fileName, long dataSize) {
        this.fileName = fileName;
        this.dataSize = dataSize;
        this.toIDSet = new CustomIDSet();
        this.fromIDSet = new CustomIDSet();

    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getDataSize() {
        return dataSize;
    }

    public void setDataSize(long dataSize) {
        this.dataSize = dataSize;
    }

    public CustomIDSet getToIDSet() {
        return toIDSet;
    }

    public void setToIDSet(CustomIDSet toIDSet) {
        this.toIDSet = toIDSet;
    }

    public CustomIDSet getFromIDSet() {
        return fromIDSet;
    }

    public void setFromIDSet(CustomIDSet fromIDSet) {
        this.fromIDSet = fromIDSet;
    }
}
