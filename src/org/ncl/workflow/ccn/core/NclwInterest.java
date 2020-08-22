package org.ncl.workflow.ccn.core;

import net.named_data.jndn.Name;

import java.io.Serializable;

/**
 * Created by Hidehiro Kanemitsu on 2020/08/10.
 */
public class NclwInterest implements Serializable {

    private String  name;

    private byte[] applicationParameters;

    public NclwInterest(String  name, byte[] applicationParameters) {
        this.name = name;
        this.applicationParameters = applicationParameters;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getApplicationParameters() {
        return applicationParameters;
    }

    public void setApplicationParameters(byte[] applicationParameters) {
        this.applicationParameters = applicationParameters;
    }
}
