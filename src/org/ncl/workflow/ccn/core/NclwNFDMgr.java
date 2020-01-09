package org.ncl.workflow.ccn.core;

import com.intel.jndn.forwarder.Forwarder;
import com.intel.jndn.forwarder.api.*;
import com.intel.jnfd.deamon.face.FaceUri;
import com.intel.jnfd.deamon.face.tcp.TcpChannel;
import com.intel.jnfd.deamon.face.tcp.TcpFace;
import com.intel.jnfd.deamon.face.tcp.TcpLocalFace;
import com.intel.jnfd.deamon.fw.FaceTable;
import com.intel.jnfd.deamon.fw.ForwardingPipeline;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;
import org.ncl.workflow.ccn.sfc.process.NFDTask;
import org.ncl.workflow.ccn.sfc.process.NclwSFCFWPipeline;
import org.ncl.workflow.ccn.util.NetInfo;
import org.ncl.workflow.comm.NCLWData;
import org.ncl.workflow.engine.Task;
import org.ncl.workflow.util.HostInfo;
import org.ncl.workflow.util.NCLWUtil;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;

/**
 * Created by Hidehiro Kanemitsu on 2019/11/13
 */
public class NclwNFDMgr {

    private static NclwNFDMgr own;

    private  NclwFaceManager mgr;

    private  Forwarder fw;

    private  TcpFace face;

    private String ownIP;

    private HashMap<String, String> ipMap;

    private long startTime;

    private long finishTime;


    /**
     * データの宛先
     * (JobID^宛先タスクのID, データ宛先Face)
     */
   // private HashMap<String, TcpFace> toFaceMap;

    /**
     * データがやってきたfrom
     * (JobID^先行タスクのID, データがきたFace)
     */
    //private HashMap<String, TcpFace> fromFaceMap;

    private TcpChannel channel;

    public NclwSFCFWPipeline getPipeline(){

        return (NclwSFCFWPipeline)this.getMgr().getPipeline();
    }

    public static NclwNFDMgr getIns(){
        if(NclwNFDMgr.own==null){
            NclwNFDMgr.own = new NclwNFDMgr();
        }else{

        }

        return NclwNFDMgr.own;
    }

    private NclwNFDMgr(){
        //this.toFaceMap = new HashMap<String, TcpFace>();
       // this.fromFaceMap = new HashMap<String, TcpFace>();
        this.ipMap = new HashMap<String, String>();


    }


    public TcpChannel getChannel() {
        return channel;
    }

    public void setChannel(TcpChannel channel) {
        this.channel = channel;
    }

   /* public HashMap<String, TcpFace> getToFaceMap() {
        return toFaceMap;
    }*/

    public boolean isLocalHost(String ip){
        boolean isFound = false;
        try{
            LinkedList<InetAddress> addList = NetInfo.getAllIP();
            Iterator<InetAddress> addIte = addList.listIterator();
            while(addIte.hasNext()){
                InetAddress ia = addIte.next();
                if(ip.equals(ia.getHostAddress())){
                    isFound = true;
                    break;
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return isFound;

    }

/*
    public void setToFaceMap(HashMap<String, TcpFace> toFaceMap) {
        this.toFaceMap = toFaceMap;
    }

    public HashMap<String, TcpFace> getFromFaceMap() {
        return fromFaceMap;
    }

    public void setFromFaceMap(HashMap<String, TcpFace> fromFaceMap) {
        this.fromFaceMap = fromFaceMap;
    }

    public String generateNclwPrefix(String cmd, String output){
        return null;
    }
*/
    public void initializeTables(String file){
        try{
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            // 最終行まで読み込む
            String line = "";
            //まずは一行読み

            //br.readLine();
            LinkedList<HostInfo> hostList = new LinkedList<HostInfo>();

            while ((line = br.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line, ",");
                int cnt = 0;
             //   while (st.hasMoreTokens()) {
                    //IPアドレスを取得する．
                    String ip_addr = st.nextToken();
                    this.ipMap.put(ip_addr,ip_addr);

            }
            //Delegatorも追加する．
            this.ipMap.put(NCLWUtil.delegator_ip, NCLWUtil.delegator_ip);

            //自身のIPを確定させる．
            this.getOwnIPAddr();
            Iterator<String> ipIte = ipMap.values().iterator();
            while(ipIte.hasNext()){
                String ip = ipIte.next();
                //生成+FaceTableへ反映させる．
               /// TcpFace face = this.createFace(ip, this.ownIP);
                this.getMgr().getPfactory().createFace(new FaceUri("tcp4", ip, NCLWUtil.NFD_PORT));

                //Fibにも追加しておく．
               // this.getFib().insert(new Name(NCLWUtil.NCLW_PREFIX), face, 1);
            }
        }catch(Exception e){
            e.printStackTrace();
        }

    }

    public void addFIBEntryCallBack(TcpFace face){
        this.getFib().insert(new Name(NCLWUtil.NCLW_PREFIX), face, 1);

    }
    /**
     * Interestパケットのapplicationパラメータから，
     * NCLWDataへ変換します．
     * @param interest
     * @return
     */
    public NCLWData fetchNCLWData(Interest interest){
        Blob bdata = interest.getApplicationParameters();
        NCLWData data = null;
        try{
            ByteArrayInputStream is = new ByteArrayInputStream(bdata.getImmutableArray());
            ObjectInputStream in = new ObjectInputStream(is);
            data = (NCLWData) in.readObject();

            return data;
        }catch(Exception e){
            e.printStackTrace();
        }
        return data;
    }

    public String  getOwnIPAddr(){
        try{
            InetAddress  addr = InetAddress.getLocalHost();
            String hostName = addr.getHostName();

            LinkedList<InetAddress> addList = NetInfo.getAllIP();
            Iterator<InetAddress> addIte = addList.listIterator();
            while(addIte.hasNext()){
                String ip = addIte.next().getHostAddress();
                if(this.ipMap.containsKey(ip)){
                    this.ownIP = ip;
                    break;
                }

            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return this.ownIP;

    }

    public NCLWData fetchNCLWData(Data nfdData){
        Blob bdata = nfdData.getContent();
        NCLWData data = null;
        try{
            ByteArrayInputStream is = new ByteArrayInputStream(bdata.getImmutableArray());
            ObjectInputStream in = new ObjectInputStream(is);
            data = (NCLWData) in.readObject();

            return data;
        }catch(Exception e){
            e.printStackTrace();
        }
        return data;
    }



    public TcpFace createFace(String remoteAddress, String localAddress){

        TcpFace face = this.channel.getFace(remoteAddress, NCLWUtil.NFD_PORT);
        if(face == null){
            face = this.createFace2(remoteAddress);
          /*  try{
                this.getMgr().getPfactory().createFace(new FaceUri("tcp4://"+remoteAddress + ":"+NCLWUtil.NFD_PORT));
                face = this.channel.getFace(remoteAddress, NCLWUtil.NFD_PORT);


            }catch(Exception e){
                e.printStackTrace();
            }*/
            InetSocketAddress remoteSocket = new InetSocketAddress(remoteAddress, NCLWUtil.NFD_PORT);

            this.channel.getFaceMap().put(remoteSocket, face);
        }else{
            if(remoteAddress.equals(localAddress)){
                if(!face.getLocalUri().getInet().getHostAddress().equals(face.getRemoteUri().getInet().getHostAddress())){
                    try{
                        face.setLocalUri(new FaceUri("tcp4://"+remoteAddress));

                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }else{

            }
        }



         return face;

    }

    /**
     * 新規Faceを作ります．
     * @param remoteAddress
     * @return
     */
    public TcpFace createFace2(String remoteAddress){
       // return this.channel.getFace(remoteAddress, NCLWUtil.NFD_PORT);
         TcpFace face = null;
       try{

          // this.getMgr().getPfactory().createFace(new FaceUri("tcp4://"+remoteAddress + ":"+NCLWUtil.NFD_PORT));
            AsynchronousChannelGroup  asynchronousChannelGroup = AsynchronousChannelGroup.withThreadPool(this.mgr.getPool());

            AsynchronousSocketChannel asynchronousSocket
                    = AsynchronousSocketChannel.open(asynchronousChannelGroup);
            asynchronousSocket.connect(new InetSocketAddress(remoteAddress, NCLWUtil.NFD_PORT));
            FaceUri localFaceUri = new FaceUri("tcp4://"+this.getOwnIPAddr()); // or new FaceUri("tcp6://[::]:6363")};

            face = new TcpFace(localFaceUri,
                    new FaceUri("tcp4://"+remoteAddress + ":"+NCLWUtil.NFD_PORT), asynchronousSocket
                    );
      /*     InetSocketAddress localSocket = new InetSocketAddress(this.getOwnIPAddr(), NCLWUtil.NFD_PORT);
           InetSocketAddress remoteSocket
                   = new InetSocketAddress(remoteAddress, NCLWUtil.NFD_PORT);
           face = new TcpLocalFace(new FaceUri("tcp", localSocket),
                   new FaceUri("tcp", remoteSocket),
                   asynchronousSocket, true, false,
                   this.channel.getOnFaceDestroyedByPeer(),
                   this.channel.getOnDataReceived(), this.channel.getOnInterestReceived());
           onFaceCreated.onCompleted(face);
           */
/*


           face = new TcpFace(new FaceUri("tcp", localSocket),
                   new FaceUri("tcp", remoteSocket),
                   asynchronousSocket, false, false,
                   this.channel.getOnFaceDestroyedByPeer(),
                   this.channel.getOnDataReceived(), this.channel.getOnInterestReceived());
           this.channel.getOnFaceCreated().onCompleted(face);
*/
            //当該faceを登録します．
            this.putFace(face);



        }catch(Exception e){
            e.printStackTrace();
        }


      return face;

    }

    /**
     *
     * @param name
     * @return
     */
    public NclwNameInfo getToTaskIDFromName(Name name){
        String prefix = name.toUri();

        StringTokenizer st = new StringTokenizer(prefix, "/");

        int cnt = 0;
        //while (st.hasMoreTokens()) {
            // 1行の各要素をタブ区切りで表示
            String NCLW = st.nextToken();
            String JobID = st.nextToken();
            String fromTask = st.nextToken();
            String toTask = st.nextToken();
          //  String toTaskID = st.nextToken();
      //  }

        NclwNameInfo info = new NclwNameInfo(Long.valueOf(JobID).longValue(), Long.valueOf(toTask).longValue());

        return info;


    }

    /**
     * Cmd以下のみの部分を取得する．
     * @param name
     * @return
     */
    public Name trimName(Name name){
        String prefix = name.toUri();

        StringTokenizer st = new StringTokenizer(prefix, "/");
        StringBuffer buf = new StringBuffer();

        int cnt = 0;
        String NCWL = st.nextToken();
        String JobID = st.nextToken();
        String fromTaskID = st.nextToken();
        String toTaskID = st.nextToken();
       // String toTaskID = st.nextToken();
        while (st.hasMoreTokens()) {
            // 1行の各要素をタブ区切りで表示
            buf.append(st.nextToken());
        }

        Name retName= new Name(buf.toString());

        return retName;
    }

    /**
     * Prefix生成する処理になります．
     * /NCLW固有/fromTaskのID/toTaskのID/fromTaskのCmd
     * @param
     * @return
     */
    public Name createPrefix(NFDTask fromTask, NFDTask toTask){
        long toID = -1;
        if(toTask != null){
            toID = toTask.getTaskID();
        }
        String renewCmd = fromTask.getCmd().replaceAll("^","vv");

        String val = NCLWUtil.NCLW_PREFIX+fromTask.getJobID() + "/"+fromTask.getTaskID()+"/"+toID+"/"+renewCmd;
        val = val.replaceAll(" ","");
        Name targetName = new Name(val);

        return targetName;

    }

    public FaceTable getFaceTable(){
        return this.mgr.getPipeline().getFaceTable();

    }


    public Face getFace(int faceID){
        return this.getFaceTable().get(faceID);

    }

    /**
     *
     * @param face
     * @return
     */
    public int  putFace(Face face){
        int id = this.getFaceTable().add(face);
        return id;
    }

    public ContentStore getCs(){
        return this.getPipeline().getCs();
    }

    public FaceInformationBase getFib(){
        return this.getPipeline().getFib();
    }

    public PendingInterestTable getPit(){
        return this.getPipeline().getPit();
    }


    public void setMgr(NclwFaceManager val){
        this.mgr = val;
    }

    public NclwFaceManager getMgr(){
        return this.mgr;
    }

    public Forwarder getFw() {
        return fw;
    }

    public void setFw(Forwarder fw) {
        this.fw = fw;
    }

    public TcpFace getFace() {
        return this.face;
    }

    public  void setFace(TcpFace face) {
        this.face = face;
    }


}
