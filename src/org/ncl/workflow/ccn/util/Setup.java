package org.ncl.workflow.ccn.util;

import com.intel.jndn.forwarder.api.Face;
import com.intel.jnfd.deamon.face.AbstractFace;
import com.intel.jnfd.deamon.face.FaceUri;
import com.intel.jnfd.deamon.face.tcp.TcpFace;
import com.intel.jnfd.deamon.table.cs.SortedSetCs;
import com.intel.jnfd.deamon.table.fib.FibNextHop;
import net.named_data.jndn.Data;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;
import org.ncl.workflow.ccn.core.NclwNFDMgr;

import java.net.InetSocketAddress;

/**
 * Created by Hidehiro Kanemitsu on 2019/11/15
 */
public class Setup {
    public void prepare(){
        //Kanemitsu
     /*   Data data = new Data(new Name("/example/testApp/randomData/"));
        data.setContent(new Blob("HELLWOrld"));
        SortedSetCs cs = (SortedSetCs) NclwNFDMgr.getIns().getCs();
        cs.insert(data, true);

      */


        try{

        }catch(Exception e){
            e.printStackTrace();
        }


        //localuril




        //Face face = new AbstractFace("test");

    }
}
