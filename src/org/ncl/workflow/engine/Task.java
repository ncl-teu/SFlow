package org.ncl.workflow.engine;

import net.gripps.cloud.core.VM;
import net.gripps.cloud.nfv.NFVEnvironment;
import net.gripps.cloud.nfv.sfc.SFC;
import net.gripps.cloud.nfv.sfc.VNF;
import net.gripps.clustering.common.aplmodel.CustomIDSet;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.ncl.workflow.comm.*;
import org.ncl.workflow.util.NCLWUtil;

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
    private long jobID;

    /**
     * ID of this task.
     */
    private long taskID;

    /**
     * String of the command.
     */
    private String cmd;

    /**
     * Additional arguments which is determined depending on
     * the dynamics of the system.
     */
    private String[] args;

    /**
     * Output file path of the executable command.
     */
    private LinkedList<FileSendInfo> outFileInfoList;

    /**
     * Output message(i.e., values) of the command.
     */
    private DataSendInfo outDataInfo;

    /**
     * Immediate predecessor task IDs.
     */
    // private LinkedList<Long> fromTaskList;

    /**
     * Immediate successor task IDs.
     */
    //private LinkedList<Long> toTaskList;

    /**
     * Actual start time of the task.
     */
    private long startTime;

    /**
     * Actual completion time of the task.
     */
    private long completionTime;

    /**
     * assignment target ID.
     * vCPU level ID.
     */
    private String targetID;

    private HashMap<Long, NCLWData> inDataMap;

    private HashMap<Long, NCLWData> outDataMap;

    private CustomIDSet portSet;

    /**
     * Referencial VNF to monitor the number of
     * input data/ outpu t data.
     */
    private VNF vnf;

    private SFC sfc;

    private NFVEnvironment env;

    private WorkflowJob job;

    private boolean isStarted;

    private boolean isFinished;

    //private int arrivedCnt;


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
                String filePath = str.substring(3);
                cmd = cmd.replace(str, filePath);
                File file = new File(filePath);
                if(!file.exists()){
                    //Get the file via FTP.
                    this.getFileByFTP(filePath);
                }else{
                }
                retCmd.add(filePath);



            } else {
                retCmd.add(str);
            }
        }

        return retCmd;
    }

    public void getFileByFTP(String filePath) {
        FileOutputStream os = null;
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

    @Override
    public void run() {
        System.out.println("Task:" + this.getTaskID() + "started");
        this.setStarted(true);
        int inDegree = this.vnf.getDpredList().size();
        //System.out.println("InDegree:"+ inDegree);

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

                    // Runtime.getRuntime().exec(actualCmd);
                    ProcessBuilder builder = new ProcessBuilder(actualCmd);
                    //builder.inheritIO();
                    //Execute the task.
                    Process process = builder.start();

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

                    //int result = process.exitValue();
                    // if (result == 0) {

                    if (!process.isAlive()) {


                       /* try{
                            Thread.sleep(3000);
                        }catch(Exception e){
                            e.printStackTrace();
                        }

                        */

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

                                    SendThread sender = new SendThread();
                                    Thread sendThread = new Thread(sender);
                                    //NCLWEngine.getIns().getExec().submit(sender);
                                    sendThread.start();
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


                                    //String ipAddr =;
                                    NCLWData data = new NCLWData(fsi.getPath(), fsi.getWritePath(), this.getTaskID(), targetID, host.getIpAddr(),
                                            true, portNumber);
                                    data.setSfc(this.sfc);
                                    data.setJob(this.job);
                                    data.setEnv(this.env);
                                    if (file.exists()) {
                                        data.setFile(file);

                                    }

                                    SendThread sender = new SendThread();
                                    Thread sendThread = new Thread(sender);
                                    //NCLWEngine.getIns().getExec().submit(sender);
                                    sendThread.start();
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
                                SendThread sender = new SendThread();
                                Thread sendThread = new Thread(sender);
                                sendThread.start();
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

    /*
        public LinkedList<Long> getFromTaskList() {
            return fromTaskList;
        }

        public void setFromTaskList(LinkedList<Long> fromTaskList) {
            this.fromTaskList = fromTaskList;
        }

        public LinkedList<Long> getToTaskList() {
            return toTaskList;
        }

        public void setToTaskList(LinkedList<Long> toTaskList) {
            this.toTaskList = toTaskList;
        }
    */

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
}
