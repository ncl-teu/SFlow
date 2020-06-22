//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.ncl.workflow.ccn.sfc.strategy;

import com.intel.jnfd.deamon.face.tcp.TcpFace;
import net.named_data.jndn.Name;
import org.ncl.workflow.ccn.core.NclwNFDMgr;

public class AutoICNFaceThread implements Runnable {
    private String remoteAddress;
    private String localAddress;
    private Name prefix;

    public AutoICNFaceThread(String rIP, String lIP, Name pre) {
        this.remoteAddress = rIP;
        this.localAddress = lIP;
        this.prefix = pre;
    }

    public void run() {
        TcpFace face = NclwNFDMgr.getIns().createFace2(this.remoteAddress);
        NclwNFDMgr.getIns().getFib().insert(this.prefix, face, 1);
    }
}
