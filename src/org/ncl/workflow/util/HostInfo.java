package org.ncl.workflow.util;

import java.io.Serializable;

/**
 * Created by Hidehiro Kanemitsu on 2019/08/05
 */
public class HostInfo implements Serializable {

    private String ipAddress;

    private String userName;

    private String password;

    private String Path;

    public HostInfo(String ipAddress, String userName, String password, String path) {
        this.ipAddress = ipAddress;
        this.userName = userName;
        this.password = password;
        Path = path;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPath() {
        return Path;
    }

    public void setPath(String path) {
        Path = path;
    }
}
