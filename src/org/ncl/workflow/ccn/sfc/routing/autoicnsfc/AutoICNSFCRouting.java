//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.ncl.workflow.ccn.sfc.routing.autoicnsfc;

import com.intel.jndn.forwarder.api.Face;
import com.intel.jndn.forwarder.api.callbacks.OnCompleted;
import com.intel.jnfd.deamon.face.FaceUri;
import com.intel.jnfd.deamon.face.tcp.TcpFace;
import java.util.Iterator;
import net.gripps.cloud.core.VM;
import net.gripps.cloud.nfv.NFVEnvironment;
import net.named_data.jndn.Name;
import org.ncl.workflow.ccn.autoicnsfc.AutoICNSFCMgr;
import org.ncl.workflow.ccn.autoicnsfc.AutoICNSFCScheduling;
import org.ncl.workflow.ccn.core.NclwNFDMgr;
import org.ncl.workflow.ccn.sfc.routing.BaseNFDRouting;
import org.ncl.workflow.ccn.util.ResourceMgr;
import org.ncl.workflow.util.NCLWUtil;

public class AutoICNSFCRouting extends BaseNFDRouting {
    AutoICNSFCSender sender;
    AutoICNSFCReceiver receiver;

    public AutoICNSFCRouting() {
    }

    public Face findFace(Name name) {
        return null;
    }

    public Face findFace(String destIP) {
        return null;
    }

    public void initializeFIB(String file) {
        try {
            AutoICNSFCScheduling sched = AutoICNSFCMgr.getIns().getSched();
            NFVEnvironment env = (NFVEnvironment)sched.getEnv();
            int maxFaces = NCLWUtil.ccn_fib_maxfaces_entry;
            Iterator<VM> vIte = env.getGlobal_vmMap().values().iterator();
            String ownIP = ResourceMgr.getIns().getOwnIPAddr();
            int vmLen = env.getGlobal_vmMap().size();
            if (maxFaces >= vmLen) {
            }

            System.out.println("VM #" + env.getGlobal_vmMap().size());

            while(vIte.hasNext()) {
                VM vm = (VM)vIte.next();
             //   if (!ownIP.equals(vm.getIpAddr())) {
                    Name name = new Name(NCLWUtil.NCLW_PREFIX);
                    boolean ret = false;
                    while(!ret){
                        TcpFace face = NclwNFDMgr.getIns().getChannel().getFace(vm.getIpAddr(), NCLWUtil.NFD_PORT);
                        if(face == null){
                            //TcpFace oFace = NclwNFDMgr.getIns().createFace(vm.getIpAddr(), ownIP);

                            NclwNFDMgr.getIns().getMgr().getPfactory().createFace(new FaceUri("tcp4", vm.getIpAddr(), NCLWUtil.NFD_PORT));
                            Thread.sleep(300);
                        }else{
                            ret = true;
                            break;
                        }

                    }
                   // TcpFace face = NclwNFDMgr.getIns().createFace(vm.getIpAddr(), ownIP);
                  //  OnCompleted<Face> onFace =  NclwNFDMgr.getIns().getMgr().getPfactory().getOnFaceCreated();

                    //NclwNFDMgr.getIns().getFib().insert(name, (TcpFace)onFace, 1);
               // }
            }
        } catch (Exception var11) {
            var11.printStackTrace();
        }

    }


}
