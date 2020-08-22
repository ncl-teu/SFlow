package org.ncl.workflow.ccn.core;

import com.intel.jndn.forwarder.api.*;
import com.intel.jndn.forwarder.api.callbacks.OnCompleted;
import com.intel.jndn.forwarder.api.callbacks.OnDataReceived;
import com.intel.jndn.forwarder.api.callbacks.OnFailed;
import com.intel.jndn.forwarder.api.callbacks.OnInterestReceived;
import com.intel.jnfd.deamon.face.FaceUri;
import com.intel.jnfd.deamon.face.tcp.TcpChannel;
import com.intel.jnfd.deamon.face.tcp.TcpFace;
import com.intel.jnfd.deamon.face.tcp.TcpFactory;
import com.intel.jnfd.deamon.fw.ForwardingPipeline;
import com.intel.jnfd.util.NfdCommon;
import net.gripps.ccn.CCNUtil;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import org.ncl.workflow.util.NCLWUtil;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Hidehiro Kanemitsu on 2019/11/13.
 */
public class NclwFaceManager implements Runnable, FaceManager {
    /**
     * プロトコル集合
     */
    private  Map<String, ProtocolFactory> protocols = new HashMap<>();

    /**
     *
     */
    private  ExecutorService pool;

    /**
     *
     */
    private  ForwardingPipeline pipeline;

    private TcpFactory  pfactory;

    /**
     *
     */
    private static  Logger logger = Logger.getLogger(NclwFaceManager.class.getName());

    public NclwFaceManager(ExecutorService pool, ForwardingPipeline pipeline) {
        this.pool = pool;
        this.pipeline = pipeline;
        if(NCLWUtil.ccn_comm_mode == 0) {
            this.pfactory = new TcpFactory(this.pool,
                    onChannelCreated1,
                    onChannelCreationFailed,
                    onChannelDestroyed,
                    onChannelDestructionFailed,
                    onFaceCreated,
                    onFaceCreationFailed,
                    onFaceDestroyed,
                    onFaceDestructionFailed,
                    onFaceDestroyedByPeer,
                    onDataReceived,
                    onInterestReceived);


            registerProtocol(pfactory);
            //チャンネル登録
            NclwNFDMgr.getIns().setChannel(pfactory.getChannelMap().get(NCLWUtil.NFD_PORT));
            logger.setLevel(NfdCommon.LOG_LEVEL);
        }
    }

    public ExecutorService getPool() {
        return pool;
    }

    public ForwardingPipeline getPipeline() {
        return pipeline;
    }


    public NclwFaceManager(ForwardingPipeline pipeline) {
        this(Executors.newCachedThreadPool(), pipeline);
    }

    @Override
    public void registerProtocol(ProtocolFactory protocolFactory) {
        if (!protocols.containsKey(protocolFactory.scheme())) {
            protocols.put(protocolFactory.scheme(), protocolFactory);
        }
    }

    @Override
    public Collection<ProtocolFactory> listProtocols() {
        return protocols.values();
    }

    @Override
    public Collection<String> listProtocolNames() {
        return protocols.keySet();
    }

    public ProtocolFactory findProtocol(String scheme) {
        if (!protocols.containsKey(scheme)) {
            throw new IllegalArgumentException("Unknown protocol scheme: " + scheme);
        } else {
            return protocols.get(scheme);
        }
    }

    public TcpFactory getPfactory() {
        return pfactory;
    }

    public void setPfactory(TcpFactory pfactory) {
        this.pfactory = pfactory;

    }

    @Override
    public void run() {
        while(true){
            try{
                Thread.sleep(10);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    public void processInterest(Interest interest, TcpFace face){
        logger.info("***Arrived Interest!!****");
        pipeline.onInterest(face, interest);

    }

    @Override
    public void createChannelAndListen(FaceUri localUri) {
        ProtocolFactory protocol = findProtocol(localUri.getScheme());
        if (protocol == null) {
            onChannelCreationFailed.onFailed(new Exception("No factory found "
                    + "for " + localUri.getScheme()));
            return;
        }
        //Create the channel instance and start to listen
        Channel channel = protocol.createChannel(localUri);
        try {
            channel.open();
            onChannelCreated1.onCompleted(channel);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
            onChannelCreationFailed.onFailed(ex);
        }
    }

    @Override
    public void destroyChannel(FaceUri localUri) {
        ProtocolFactory protocol = protocols.get(localUri.getScheme());
        if (protocol == null) {
            return;
        }
        protocol.destroyChannel(localUri);
    }

    @Override
    public Collection<? extends Channel> listChannels() {
        Collection<Channel> result = new HashSet<>();
        for (ProtocolFactory one : protocols.values()) {
            result.addAll(one.listChannels());
        }
        return result;
    }

    @Override
    public Collection<? extends Channel> listChannels(String scheme) {
        if (!protocols.containsKey(scheme)) {
            return null;
        }
        return protocols.get(scheme).listChannels();
    }

    @Override
    public void createFaceAndConnect(FaceUri remoteUri) {
        ProtocolFactory protocol = protocols.get(remoteUri.getScheme());
        if (protocol == null) {
            onFaceCreationFailed.onFailed(new Exception("No such scheme found"
                    + remoteUri.getScheme()));
            return;
        }
        protocol.createFace(remoteUri);
    }

    @Override
    public void createFaceAndConnect(FaceUri remoteUri, OnCompleted<Face> onFaceCreated) {
        ProtocolFactory protocol = protocols.get(remoteUri.getScheme());
        if (protocol == null) {
            onFaceCreationFailed.onFailed(new Exception("No such scheme found"
                    + remoteUri.getScheme()));
            return;
        }
        protocol.createFace(remoteUri, onFaceCreated);
    }

    //    @Override
//    public void createFaceAndConnect(FaceUri localUri, FaceUri remoteUri) {
//        ProtocolFactory protocol = protocols.get(localUri.getScheme());
//        if (protocol == null) {
//            onFaceCreationFailed.onFailed(new Exception("No such scheme found"
//                    + localUri.getScheme()));
//            return;
//        }
//        protocol.createFace(localUri, remoteUri, true);
//    }
    @Override
    public void destroyFace(Face face) {
        logger.log(Level.INFO, "destroyFace: {0}", face);
        ProtocolFactory protocol = protocols.get(face.getLocalUri().getScheme());
        if (protocol == null) {
            onFaceDestructionFailed.onFailed(new Exception("No such scheme found "
                    + face.getLocalUri().getScheme()));
            return;
        }
        protocol.destroyFace(face);
    }

    @Override
    public void destroyFace(FaceUri localFaceUri, FaceUri remoteFaceUri) {
        ProtocolFactory protocol = protocols.get(localFaceUri.getScheme());
        if (protocol == null) {
            onFaceDestructionFailed.onFailed(new Exception("No such face found "
                    + localFaceUri.getScheme()));
            return;
        }
        protocol.destroyFace(localFaceUri, remoteFaceUri);
    }

    @Override
    public Collection<? extends Face> listFaces() {
        Collection<Face> result = new HashSet<>();
        for (ProtocolFactory one : protocols.values()) {
            result.addAll(one.listFaces());
        }
        return result;
    }

    @Override
    public Collection<? extends Face> listFaces(String scheme) {
        if (!protocols.containsKey(scheme)) {
            return null;
        }
        return protocols.get(scheme).listFaces();
    }



    // All the callbacks
    public  OnCompleted<Channel> onChannelCreated1 = new OnCompleted() {

        @Override
        public void onCompleted(Object result) {

        }

    };
    public  OnFailed onChannelCreationFailed = new OnFailed() {

        @Override
        public void onFailed(Throwable error) {

        }

    };
    public  OnCompleted<Channel> onChannelDestroyed = new OnCompleted() {

        @Override
        public void onCompleted(Object result) {

        }

    };
    private  OnFailed onChannelDestructionFailed = new OnFailed() {

        @Override
        public void onFailed(Throwable error) {

        }

    };
    public   OnCompleted<Face> onFaceCreated = new OnCompleted() {

        @Override
        public void onCompleted(Object result) {
            if (result instanceof Face) {
                pipeline.addFace((Face) result);
                NclwNFDMgr.getIns().getFib().insert(new Name(NCLWUtil.NCLW_PREFIX), (TcpFace)result, 1);

            }
        }

    };
    private  OnFailed onFaceCreationFailed = new OnFailed() {

        @Override
        public void onFailed(Throwable error) {

        }

    };
    public   OnCompleted<Face> onFaceDestroyed = new OnCompleted() {

        @Override
        public void onCompleted(Object result) {
            if (result instanceof Face) {
                pipeline.removeFace((Face) result, null);
            }
        }

    };
    private  OnFailed onFaceDestructionFailed = new OnFailed() {

        @Override
        public void onFailed(Throwable error) {

        }

    };
    public   OnCompleted<Face> onFaceDestroyedByPeer = new OnCompleted() {
        @Override
        public void onCompleted(Object result) {
            if (result instanceof Face) {
                destroyFace((Face) result);
            }
        }
    };
    public   OnDataReceived onDataReceived = new OnDataReceived() {

        @Override
        public void onData(Data data, Face incomingFace) {
            logger.info("OnData is called");
            pipeline.onData(incomingFace, data);
        }

    };
    public  OnInterestReceived onInterestReceived = new OnInterestReceived() {
        @Override
        public void onInterest(Interest interest, Face face) {
            logger.info("OnInterest is called");
            System.out.println("****ON INTEREST RECV Called");
            pipeline.onInterest(face, interest);
        }

    };


    
}
