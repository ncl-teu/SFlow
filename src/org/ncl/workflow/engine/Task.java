package org.ncl.workflow.engine;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.ConnectionInfo;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.Session;
import net.gripps.cloud.core.VM;
import net.gripps.cloud.nfv.NFVEnvironment;
import net.gripps.cloud.nfv.sfc.SFC;
import net.gripps.cloud.nfv.sfc.VNF;
import net.gripps.clustering.common.aplmodel.CustomIDSet;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.ncl.workflow.ccn.autoicnsfc.AutoICNSFCMgr;
import org.ncl.workflow.comm.*;
import org.ncl.workflow.util.NCLWUtil;
// import com.amihaiemil.docker.Images;
// import com.amihaiemil.docker.Docker;

import java.io.*;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;

/**
 * Created by Hidehiro Kanemitsu on 2019/05/04.
 * Receives the input data from a receiveThread.
 */
public class Task implements Serializable, Runnable {

    /**
     * ID of the job including this task.
     */
    protected long jobID;

    /**
     * ID of this task.
     */
    protected long taskID;

    /**
     * String of the command.
     */
    protected String cmd;

    /**
     * Additional arguments which is determined depending on
     * the dynamics of the system.
     */
    protected String[] args;

    /**
     * Output file path of the executable command.
     */
    protected LinkedList<FileSendInfo> outFileInfoList;

    /**
     * Output message(i.e., values) of the command.
     */
    protected DataSendInfo outDataInfo;

    /**
     * Immediate predecessor task IDs.
     */
    // protected LinkedList<Long> fromTaskList;

    /**
     * Immediate successor task IDs.
     */
    //protected LinkedList<Long> toTaskList;

    /**
     * Actual start time of the task.
     */
    protected long startTime;

    /**
     * Actual completion time of the task.
     */
    protected long completionTime;

    /**
     * assignment target ID.
     * vCPU level ID.
     */
    protected String targetID;

    protected HashMap<Long, NCLWData> inDataMap;

    protected HashMap<Long, NCLWData> outDataMap;

    protected CustomIDSet portSet;

    /**
     * Referencial VNF to monitor the number of
     * input data/ outpu t data.
     */
    protected VNF vnf;

    protected SFC sfc;

    protected NFVEnvironment env;

    protected WorkflowJob job;

    protected boolean isStarted;

    protected boolean isFinished;

    protected String cd;

    protected String docker_tar;

    protected String docker_image;




    //protected int arrivedCnt;


    public Task(long jobID, long taskID, String cmd,
                LinkedList<FileSendInfo> oFIL, DataSendInfo oDSI, LinkedList<Long> fromTaskList, LinkedList<Long> toTaskList, String targetID) {
        this.jobID = jobID;
        this.taskID = taskID;
        this.cmd = cmd;
        this.outFileInfoList = oFIL;
        this.outDataInfo = oDSI;
        // this.fromTaskList = fromTaskList;
        //   this.toTaskList = toTaskList;
        this.targetID = targetID;
        this.inDataMap = new HashMap<Long, NCLWData>();
        this.outDataMap = new HashMap<Long, NCLWData>();
        this.portSet = new CustomIDSet();
        this.isStarted = false;
        this.isFinished = false;
        this.cd =  new File(".").getAbsoluteFile().getParent();

        //this.arrivedCnt = 0;
        //  this.sfc = in_sfc;
        //  this.env = in_env;


    }


    /**
     * Add new input data.
     * @param data
     * @return
     */
    /*public boolean putInData(NCLWData data){
        if(this.fromTaskList.contains(data.getFromTaskID())){
            this.inDataMap.put(data.getFromTaskID(), data);
            return true;
        }else{
            return false;
        }
    }*/

    /**
     * Analyze the command with variables and returns the actual command.
     * Format: "java -class $F^12 $V^34 $F^12 $F^33 $V^45 poliy";
     *
     * @param cmd
     * @return
     */
    public LinkedList<String> createCompleteCmd(String cmd) {
        System.out.println("PreCmd:"+cmd);
        cmd  = cmd.replaceAll("\\./",  this.cd+"/");

        System.out.println("AfterCmd:"+cmd);
        StringTokenizer st1 = new StringTokenizer(cmd, " ");
        LinkedList<String> retCmd = new LinkedList<String>();

        while (st1.hasMoreTokens()) {
            String str = st1.nextToken();
            if (str.length() < 3) {
                retCmd.add(str);
                continue;
            }
            String headStr = str.substring(0, 3);

            if (headStr.equals("$F^")) {
                long taskID = Long.valueOf(str.substring(3));
                NCLWData inData = this.inDataMap.get(taskID);
                //outFile is specified.
                String inFile = inData.getWriteFilePath();
                inFile = this.convertToAbsPathAll(inFile);

                cmd = cmd.replace(str, inFile);

                retCmd.add(inFile);


            } else if (headStr.equals("$V^")) {
                long taskID = Long.valueOf(str.substring(3));
                NCLWData inData = this.inDataMap.get(taskID);
                //outFile is specified.
                String inValue = inData.getMsg();
                cmd = cmd.replace(str, inValue);
                retCmd.add(inValue);
            } else if (headStr.equals("$R^")) {
                //Obtain file from FTP server.

                String prefilePath = str.substring(3);

                cmd = cmd.replace(str, prefilePath);
                File file = new File(prefilePath);
                if(!file.exists()){
                    System.out.println("File GET Mode");
                    if(NCLWUtil.input_file_transfer_protocol.equals("scp")){

                        this.getFIleBySCP(prefilePath);
                    }else{

                        //Get the file via FTP.
                        this.getFileByFTP(prefilePath);
                    }

                }else{
                }
                retCmd.add(prefilePath);



            } else {
                retCmd.add(str);
            }
        }

        return retCmd;
    }

    public String  nclwExec(LinkedList<String> cmdList){
        try{
            ProcessBuilder builder;
            Process process;

            //try to load image from tar file.
            builder = new ProcessBuilder(cmdList);
            //builder.inheritIO();
            //Execute the task.
            process = builder.start();
            int code =  process.waitFor();
            InputStream stream = process.getErrorStream();

            BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.defaultCharset()));
            BufferedReader r2 = new BufferedReader(new InputStreamReader(stream, Charset.defaultCharset()));
            StringBuffer retBuf = new StringBuffer();
            process.waitFor();

            String line_normal;
            String line_error;
            while ((line_normal = r.readLine()) != null) {
                retBuf.append(line_normal);
            }

            while ((line_error = r2.readLine()) != null) {
                retBuf.append(line_error);
            }


            if (!process.isAlive()) {
                return retBuf.toString();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;

    }






    public LinkedList<String>  loadDockerBySCP(LinkedList<String> orgCmd){
        LinkedList<String> dockerCmd = new LinkedList<String>();

        try{
            //Try to save docker tar file.
            Connection conn = new Connection(NCLWUtil.docker_repository_ip);
            ConnectionInfo info = conn.connect();
            boolean result = conn.authenticateWithPassword(NCLWUtil.docker_repository_userid,
                    NCLWUtil.docker_repository_password);
            if (result) {
                SCPClient scp = conn.createSCPClient();
                String localPath = this.cd + "/"+NCLWUtil.docker_localdir+"/";
                File dir = new File(this.cd + "/"+NCLWUtil.docker_localdir+"/");
                if(!dir.exists()){
                    dir.mkdirs();
                }

                scp.get(NCLWUtil.docker_repository_home+"/"+this.docker_tar, localPath);

                conn.close();
            }else{
                System.out.println("SCP Connection Failed...");
            }
            conn.close();
            ProcessBuilder builder;
            Process process;
            LinkedList<String> loadCmd = new LinkedList<String>();
            if(NCLWUtil.isWindows()){
                loadCmd.add("cmd");
                loadCmd.add("/c");
            }
            loadCmd.add("docker");
            loadCmd.add("load");
            loadCmd.add("-i");
            loadCmd.add(this.cd + "/"+ NCLWUtil.docker_localdir + "/"+this.docker_tar);


            //try to load image from tar file.
            builder = new ProcessBuilder(loadCmd);
            //builder.inheritIO();
            //Execute the task.
            process = builder.start();
            int code =  process.waitFor();
            InputStream stream = process.getErrorStream();

            BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.defaultCharset()));
            BufferedReader r2 = new BufferedReader(new InputStreamReader(stream, Charset.defaultCharset()));
            StringBuffer retBuf = new StringBuffer();
            process.waitFor();

            String line_normal;
            String line_error;
            while ((line_normal = r.readLine()) != null) {
                retBuf.append(line_normal);
            }

            while ((line_error = r2.readLine()) != null) {
                retBuf.append(line_error);
            }
System.out.println("LOAD RESULT:"+retBuf.toString());

            if (!process.isAlive()) {
                //After loading the docker, run it.
                // docker run -it -v `pwd`:/home/kanemih/  cvt
                dockerCmd = this.generateDockerRunCmd(orgCmd);


            }

            return dockerCmd;

        }catch(Exception e){

        }
        return dockerCmd;


    }

    public LinkedList<String> generateDockerRunCmd(LinkedList<String> orgCmd){
        LinkedList<String> dockerCmd = new LinkedList<String>();
        dockerCmd.add("docker");
        dockerCmd.add("run");
        dockerCmd.add("--rm");
        /*dockerCmd.add("-v");
        dockerCmd.add("/etc/group:/etc/group:ro");
        dockerCmd.add("-v");
        dockerCmd.add("/etc/passwd:/etc/passwd:ro");
        dockerCmd.add("-u");
        dockerCmd.add("$(id -u $USER):$(id -g $USER)");
*/
        // dockerCmd.add("-it");
        dockerCmd.add("-v");
        dockerCmd.add(this.cd + "/"+":"+this.cd + "/");
        dockerCmd.add(this.docker_image);
        orgCmd.removeFirst();
        Iterator<String> orgIte = orgCmd.listIterator();
        while(orgIte.hasNext()){
            dockerCmd.add(orgIte.next());
        }
        return dockerCmd;
    }


    public boolean loadDockerByFTP(String filePath){
        FileOutputStream os = null;
        //まずはtarを取得する．
        File  file = new File(filePath);


        FTPClient fp = new FTPClient();
        FileInputStream is = null;
        try {

            fp.connect(NCLWUtil.ftp_server_ip);
            if (!FTPReply.isPositiveCompletion(fp.getReplyCode())) { // コネクトできたか？
                System.out.println("connection failed");
                System.exit(1); // 異常終了
            }

            if (fp.login(NCLWUtil.ftp_server_id, NCLWUtil.ftp_server_pass) == false) { // ログインできたか？
                System.out.println("***FTP login failed****");
                System.exit(1); // 異常終了
            }
            fp.setFileType(FTP.BINARY_FILE_TYPE);
// ファイル受信
            if(!(file.getParent() == null)){
                File dir = new File(file.getParent());
                if(!dir.exists()){
                    dir.mkdirs();
                }
            }

            os = new FileOutputStream(filePath);// Client side
            String serverPath = "/"+NCLWUtil.ftp_server_homedirName + "/"+this.getJobID() + "/"+file.getName();
System.out.println("ServerPath:"+serverPath);

            fp.retrieveFile(serverPath, os);// サーバー側
            os.close();
            System.out.println("FTP GET COMPLETED");



        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try{
                fp.disconnect();

                os.close();
            }catch(Exception e){
                e.printStackTrace();
            }


        }
        return true;
    }

    public  void getFIleBySCP(String filePath){
        try {
            //Try to save docker tar file.
            Connection conn = new Connection(NCLWUtil.ftp_server_ip);
            ConnectionInfo info = conn.connect();
            boolean result = conn.authenticateWithPassword(NCLWUtil.ftp_server_id,
                    NCLWUtil.ftp_server_pass);
            if (result) {
                SCPClient scp = conn.createSCPClient();
               // String localPath =  NCLWUtil.ftp_server_homedirName + "/";
                //File dir = new File(filePath);
                File  file = new File(filePath);
                if(!(file.getParent() == null)){
                    File dir = new File(file.getParent());
                    if(!dir.exists()){
                        dir.mkdirs();
                    }
                }
                //String newPath = filePath.replace(filePath, file.getName());
                //String newPath = filePath;
                String newPath = file.getParent();




                String serverPath = NCLWUtil.ftp_server_homedirName + "/"+this.getJobID() + "/"+file.getName();
                System.out.println("newPath:"+newPath);
                scp.get(serverPath, newPath);
                System.out.println("OK:"+newPath);

                conn.close();
            } else {
                System.out.println("SCP Connection Failed...");
            }
            conn.close();
        }catch(Exception e){
            e.printStackTrace();
            StackTraceElement elem[] = e.getStackTrace();
            for(int i=0;i<elem.length;i++){
                System.out.println(elem[i]);
            }
        }
    }
    public void getFileByFTP(String filePath) {
        FileOutputStream os = null;
        File  file = new File(filePath);

        String tmp = "/"+NCLWUtil.ftp_server_homedirName + "/"+this.getJobID() + "/"+file.getName();
        System.out.println("serverPath:"+tmp);

        FTPClient fp = new FTPClient();
        System.out.println("484");
        FileInputStream is = null;
        try {
            System.out.println("487");

            fp.connect(NCLWUtil.ftp_server_ip);

            if (!FTPReply.isPositiveCompletion(fp.getReplyCode())) { // コネクトできたか？
                System.out.println("connection failed");
                System.exit(1); // 異常終了
            }
            System.out.println("494");

            if (fp.login(NCLWUtil.ftp_server_id, NCLWUtil.ftp_server_pass) == false) { // ログインできたか？
                System.out.println("***FTP login failed****");
                System.exit(1); // 異常終了
            }
            fp.setFileType(FTP.BINARY_FILE_TYPE);
            System.out.println("501");

// ファイル受信
            if(!(file.getParent() == null)){
                File dir = new File(file.getParent());
                if(!dir.exists()){
                    dir.mkdirs();
                }
            }
            System.out.println("510");

            os = new FileOutputStream(filePath);// Client side
            String serverPath = "/"+NCLWUtil.ftp_server_homedirName + "/"+this.getJobID() + "/"+file.getName();

            fp.retrieveFile(serverPath, os);// サーバー側
            os.close();
            System.out.println("FTP GET COMPLETED");



        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try{
                fp.disconnect();

                os.close();
            }catch(Exception e){
                e.printStackTrace();
            }


        }
    }

    public String convertToAbsPath(String path){
        return path.replace("\\./", this.cd+"/");
    }

    public String convertToAbsPathAll(String path){
        return path.replaceAll("\\./", this.cd+"/");
    }

    public boolean isDockerExecutable(LinkedList<String> in_cmd, String imageName){


        try{
            Runtime r = Runtime.getRuntime();
            String[] hashcmd = new String[]{in_cmd.get(0), in_cmd.get(1)};

            Process p = r.exec(hashcmd);

            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = null;
            StringBuffer strBuf = new StringBuffer();
            while ((line = br.readLine()) != null) {
                strBuf.append(line);
            }
            String ret = strBuf.toString();

            //nullもしくはno command形式なら，ダメということ．
            if(ret.indexOf(imageName)==-1){
                return false;
            }else{
                return true;
            }

        }catch(Exception e){
            e.printStackTrace();
        }
        return true;


    }


    public boolean isExecutable(LinkedList<String> in_cmd){


        try{
            Runtime r = Runtime.getRuntime();
            String[] hashcmd = new String[]{in_cmd.get(0), in_cmd.get(1)};

            Process p = r.exec(hashcmd);

            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = null;
            StringBuffer strBuf = new StringBuffer();
            while ((line = br.readLine()) != null) {
                strBuf.append(line);
            }
            String ret = strBuf.toString();

            //nullもしくはno command形式なら，ダメということ．
            if(ret.equals("")||(ret.indexOf("no "+in_cmd.get(0) + " in")!=-1)){
                return false;
            }else{
                return true;
            }

        }catch(Exception e){
            e.printStackTrace();
        }
        return true;


    }

    public SendThread  processSend(){
        SendThread sender = new SendThread();
        Thread sendThread = new Thread(sender);
        //NCLWEngine.getIns().getExec().submit(sender);
        sendThread.start();

        return sender;
    }



    @Override
    public void run() {
        System.out.println("Task:" + this.getTaskID() + "started");
        this.setStarted(true);
        int inDegree = this.vnf.getDpredList().size();
        //System.out.println("InDegree:"+ inDegree);
        if(NCLWUtil.nfd_strategy == 2){
           if( this.sfc ==null) {
               AutoICNSFCMgr.getIns().getSched().getSfc();
           }

        }
        try {
            while (true) {
                Thread.sleep(100);
                //Thread.sleep(3000);
                if (this.inDataMap.size() == inDegree) {
               // if(this.arrivedCnt == inDegree){
                    System.out.println("All input data arrived @ Task" + this.getTaskID());
                    //obtain all input data from the hashMap.
                    //command format "xxxxxx  $F^23 $V^23 $F^12 xxxx"
                    //$[File Flag][From Task ID] or $[Value Flag][From Task ID]
                    //analyze the command.
                    LinkedList<String> actualCmd = this.createCompleteCmd(this.cmd);
                    System.out.println("cmd:" + actualCmd + "@" + this.getTaskID());

                    LinkedList<String> hashCmd = new LinkedList<String>();
                    if(NCLWUtil.isWindows()){
                        hashCmd.add("cmd");
                        hashCmd.add("/c");
                        hashCmd.add("echo");
                        hashCmd.add("%PATH%");
                        hashCmd.add("|");
                        hashCmd.add("grep");
                        hashCmd.add(actualCmd.get(0));
                    }else{
                        hashCmd.add("which");
                        hashCmd.add(actualCmd.get(0));
                    }

                    ProcessBuilder builder;
                    Process process;
                    if(this.isExecutable(hashCmd)){
                        System.out.println("****OK! Executable!!!****");
                        builder = new ProcessBuilder(actualCmd);

                        process = builder.start();
                    }else{
                        //Try to execute through docker.
                        //search the image name from docker image list.
                        LinkedList<String> dockerExistCmd = new LinkedList<String>();
                        if(NCLWUtil.isWindows()){
                            dockerExistCmd.add("cmd");
                            dockerExistCmd.add("/c");
                        }
                        dockerExistCmd.add("docker");
                        dockerExistCmd.add("images");
                        dockerExistCmd.add("|");
                        dockerExistCmd.add("grep");
                        dockerExistCmd.add("-w");
                        dockerExistCmd.add(this.docker_image);

                        LinkedList<String> dockerCmd = new LinkedList<String>();
                        if(this.isDockerExecutable(dockerExistCmd, this.docker_image)){
                            System.out.println("****OK! Executable2!!!****");

                            //execute by docker.
                            dockerCmd = this.generateDockerRunCmd(actualCmd);
                        }else{
                            System.out.println("****NG!! NOT Executable!!! Trying to load Docker.****");
                            dockerCmd = loadDockerBySCP(actualCmd);
                            System.out.println("DockerCmd:"+dockerCmd);
                        }
                        builder = new ProcessBuilder(dockerCmd);
                        process = builder.start();

                    }

                        int code =  process.waitFor();
                    //process.destroy();
                    //Obtain the results regarding normal / error.
                    InputStream stream = process.getErrorStream();

                    BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.defaultCharset()));
                    BufferedReader r2 = new BufferedReader(new InputStreamReader(stream, Charset.defaultCharset()));
                    StringBuffer retBuf = new StringBuffer();
                    process.waitFor();

                    String line_normal;
                    String line_error;
                    while ((line_normal = r.readLine()) != null) {
                        retBuf.append(line_normal);
                    }

                    while ((line_error = r2.readLine()) != null) {
                        retBuf.append(line_error);
                    }
System.out.println("Exec RESULT:"+retBuf.toString());
                    //int result = process.exitValue();
                    // if (result == 0) {

                    if (!process.isAlive()) {

                        int result = process.exitValue();
                        System.out.println("cmd:"+actualCmd + "code:"+result);

                        //Normal Termination. Thus, the process tries to
                        //get retultant data.
                        //File transfer process.
                        if (this.outFileInfoList.size() > 0) {
                            Iterator<FileSendInfo> fIte = this.outFileInfoList.iterator();

                            //per file loop
                            while (fIte.hasNext()) {
                                FileSendInfo fsi = fIte.next();


                                File file = new File(fsi.getPath());
                                System.out.println("FileName:"+file.getPath() + "/Size:"+file.length());
                                while(!file.exists()|| file.length()==0){
                                    Thread.sleep(100);
                                }
                                process.destroyForcibly();
                                if (this.isEndVNF()) {
                                    NCLWData data = new NCLWData(fsi.getPath(), fsi.getWritePath(), this.getTaskID(), -1, NCLWUtil.delegator_ip,
                                            true, NCLWUtil.port);
                                    data.setSfc(this.sfc);
                                    data.setJob(this.job);
                                    data.setEnv(this.env);
                                    if (file.exists()) {
                                        data.setFile(file);

                                    }

                                    SendThread sender = this.processSend();

                                   /* SendThread sender = new SendThread();
                                    Thread sendThread = new Thread(sender);
                                    //NCLWEngine.getIns().getExec().submit(sender);
                                    sendThread.start();

                                    */
                                    sender.getDataQueue().add(data);
                                    System.out.println("outFile Sent : task" + this.vnf.getIDVector().get(1) + "@" + this.vnf.getvCPUID() + "->@delegator");
                                    break;
                                }
                                //per target task loop
                                Iterator<Long> fsiIte = fsi.getTargetTaskIDList().iterator();
                                while (fsiIte.hasNext()) {
                                    Long targetID = fsiIte.next();
                                    int portNumber = NCLWUtil.port;
                                    this.portSet.add(Long.valueOf(portNumber));
                                    //Get the IP address of the target node.


                                    VNF sucVNF = this.sfc.findVNFByLastID(targetID);
                                    String sucVCPUID = sucVNF.getvCPUID();
                                    VM host = NCLWUtil.findVM(this.env, sucVCPUID);
                                    if(this.env == null){
                                        System.out.println("ENV is null");
                                    }
                                    if(sucVCPUID == null){
                                        System.out.println("vcpuid is null");
                                    }
                                    if(this.sfc== null){
                                        System.out.println("sfc is null");
                                    }
                                    System.out.println("***FSI.getpath:"+fsi.getPath());
                                    System.out.println("***FSI.getWPath:"+fsi.getWritePath());
                                    System.out.println("***takID:"+this.getTaskID());
                                    System.out.println("**targetID:"+targetID);
                                    System.out.println("***host:"+host.getIpAddr());

                                    //String ipAddr =;
                                    NCLWData data = new NCLWData(fsi.getPath(), fsi.getWritePath(), this.getTaskID(), targetID, host.getIpAddr(),
                                            true, portNumber);
                                    data.setSfc(this.sfc);
                                    data.setJob(this.job);
                                    data.setEnv(this.env);
                                    if (file.exists()) {
                                        data.setFile(file);

                                    }

                                    SendThread sender = this.processSend();

                                   /* SendThread sender = new SendThread();
                                    Thread sendThread = new Thread(sender);
                                    //NCLWEngine.getIns().getExec().submit(sender);
                                    sendThread.start();

                                    */
                                    sender.getDataQueue().add(data);
                                    System.out.println("outFile Sent : task" + this.vnf.getIDVector().get(1) + "@" + this.vnf.getvCPUID() + "->" + "task" + targetID + "@" + sucVCPUID);


                                }


                            }

                        }

                        //Data transfer process
                        if (this.outDataInfo != null) {

                            //prepare for sending the output message
                            String msg = this.outDataInfo.getMsg();
                            Iterator<Long> targetIDIte = this.outDataInfo.getTargetIDList().iterator();
                            while (targetIDIte.hasNext()) {
                                Long targetTaskID = targetIDIte.next();
                                int portNumber = NCLWUtil.port;
                                this.portSet.add(Long.valueOf(portNumber));
                                //Get the IP address of the target node.

                                VNF sucVNF = this.sfc.findVNFByLastID(targetTaskID);
                                String sucVCPUID = sucVNF.getvCPUID();
                                VM host = NCLWUtil.findVM(this.env, sucVCPUID);

                                //String ipAddr =;
                                NCLWData data = new NCLWData(null, null, this.getTaskID(), targetTaskID, host.getIpAddr(),
                                        true, portNumber);
                                data.setSfc(this.sfc);
                                data.setJob(this.job);
                                data.setEnv(this.env);
                                data.setMsg(msg);
                                data.setFile(false);
                                SendThread sender = this.processSend();

                                   /* SendThread sender = new SendThread();
                                    Thread sendThread = new Thread(sender);
                                    //NCLWEngine.getIns().getExec().submit(sender);
                                    sendThread.start();

                                    */
                                sender.getDataQueue().add(data);
                                System.out.println("outMSG Sent : task" + this.vnf.getIDVector().get(1) + "@" + this.vnf.getvCPUID() + "->" + "task" + targetTaskID + "@" + sucVCPUID);


                            }

                        }


                    } else {

                    }
                    //System.out.print("TaskID:"+this.getTaskID());
                    // System.out.printf(" result=%d%n", result);

                    break;
                } else {

                }
            }
            this.isFinished = true;
            //remove own task in taskpool
            NCLWEngine.getIns().getTaskPool().remove(this.getJobID() + "^"+this.getTaskID());

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public long getJobID() {
        return jobID;
    }

    public void setJobID(long jobID) {
        this.jobID = jobID;
    }

    public long getTaskID() {
        return taskID;
    }

    public void setTaskID(long taskID) {
        this.taskID = taskID;
    }

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public String[] getArgs() {
        return args;
    }

    public void setArgs(String[] args) {
        this.args = args;
    }

    public LinkedList<FileSendInfo> getOutFileInfoList() {
        return outFileInfoList;
    }

    public void setOutFileInfoList(LinkedList<FileSendInfo> outFileInfoList) {
        this.outFileInfoList = outFileInfoList;
    }

    public DataSendInfo getOutDataInfo() {
        return outDataInfo;
    }

    public void setOutDataInfo(DataSendInfo outDataInfo) {
        this.outDataInfo = outDataInfo;
    }


    public boolean isEndVNF() {
        return this.vnf.getDsucList().isEmpty();
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(long completionTime) {
        this.completionTime = completionTime;
    }

    public String getTargetID() {
        return targetID;
    }

    public void setTargetID(String targetID) {
        this.targetID = targetID;
    }

    public HashMap<Long, NCLWData> getInDataMap() {
        return inDataMap;
    }

    public void setInDataMap(HashMap<Long, NCLWData> inDataMap) {
        this.inDataMap = inDataMap;
    }

    public HashMap<Long, NCLWData> getOutDataMap() {
        return outDataMap;
    }

    public void setOutDataMap(HashMap<Long, NCLWData> outDataMap) {
        this.outDataMap = outDataMap;
    }

    public CustomIDSet getPortSet() {
        return portSet;
    }

    public void setPortSet(CustomIDSet portSet) {
        this.portSet = portSet;
    }

    public VNF getVnf() {
        return vnf;
    }

    public void setVnf(VNF vnf) {
        this.vnf = vnf;
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

    public boolean isStarted() {
        return isStarted;
    }

    public void setStarted(boolean started) {
        isStarted = started;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean finished) {
        isFinished = finished;
    }

    public String getDocker_tar() {
        return docker_tar;
    }

    public void setDocker_tar(String docker_tar) {
        this.docker_tar = docker_tar;
    }

    public String getDocker_image() {
        return docker_image;
    }

    public void setDocker_image(String docker_image) {
        this.docker_image = docker_image;
    }


}
