package org.ncl.workflow.ccn.sfc.routing;

import com.intel.jndn.forwarder.api.Face;
import com.intel.jnfd.deamon.face.FaceUri;
import com.intel.jnfd.deamon.face.tcp.TcpFace;
import net.named_data.jndn.Name;

import java.net.InetSocketAddress;

/**
 * Created by Hidehiro Kanemitsu on 2019/11/19.
 */
public class NclwSFCNFDRouting extends BaseNFDRouting {

    public NclwSFCNFDRouting() {
    }


    @Override
    public  Face findFace(Name name) {
        try{
        /*    InetSocketAddress remoteSocket
                    = (InetSocketAddress) (asynchronousSocketChannel.getRemoteAddress());
            Face face = new TcpFace(new FaceUri("tcp", localSocket),
                    new FaceUri("tcp", remoteSocket),
                    asynchronousSocketChannel, false, false,
                    onFaceDestroyedByPeer,
                    onDataReceived, onInterestReceived);

         */
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;

    }

    @Override
    public  Face findFace(String destIP) {
        return null;
    }

    @Override
    public  void initializeFIB(String file) {

    }
}
