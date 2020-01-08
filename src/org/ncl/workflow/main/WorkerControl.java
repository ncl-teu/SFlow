package org.ncl.workflow.main;

import ch.ethz.ssh2.*;
import org.ncl.workflow.util.HostInfo;
import org.ncl.workflow.util.SshCommandExecute;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;

/**
 * Created by Hidehiro Kanemitsu on 2019/08/23.
 */
public class WorkerControl {
    public static void main(String[] args){

        //hostsファイル取得

        if(args.length < 2){
            System.out.println("Please input hosts file...");
            System.exit(-1);
        }
        String option = args[0];
        String hostsFile = args[1];



         try{
             BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(hostsFile), "UTF-8"));
             // 最終行まで読み込む
             String line = "";
             //まずは一行読み

             //br.readLine();
             LinkedList hostList = new LinkedList<HostInfo>();
             String orgCmd = "scp -r ";

             while ((line = br.readLine()) != null) {
                 StringTokenizer st = new StringTokenizer(line, ",");
                 int cnt = 0;
                 while (st.hasMoreTokens()) {
                     // 1行の各要素をタブ区切りで表示
                     String ip_addr = st.nextToken();

                     String userName = st.nextToken();

                     String password = st.nextToken();

                     String dir = st.nextToken();
                     HostInfo info = new HostInfo(ip_addr,userName, password, dir);
                     hostList.add(info);


                 }
             }

             Iterator<HostInfo> hostIte = hostList.listIterator();
             String collectLog;
             FileOutputStream fos = null;
             if(args.length>3 &&(option.equals("collectlog"))){
                  collectLog = args[3];
                 fos = new FileOutputStream(new File(collectLog), false);


             }


             //BufferedWriter writer = Files.newBufferedWriter(path);


             while(hostIte.hasNext()){
                 HostInfo h = hostIte.next();
                 Connection conn = new Connection(h.getIpAddress());
                 ConnectionInfo info = conn.connect();
                 boolean result = conn.authenticateWithPassword(h.getUserName(),
                         h.getPassword());
                 if (result) {

                     Session sess= conn.openSession();
                     if(option.equals("start")){
                         if(args.length>=3){
                             String shFile = args[2];
                             sess.execCommand("cd " + h.getPath()+" && "+ shFile);
                         }

                     }else if(option.equals("collectlog")) {
                         if(args.length>=4){
                             String logFile = args[2];

                             //Logの内容を取得する．
                             sess.execCommand("cat " + h.getPath()+ logFile);
                             InputStream is = sess.getStdout();
                             //InputStreamReader ir = new InputStreamReader(is, "UTF-8");
                             int data;
                             int b = 0;

                             byte [] buffer = new byte[1024];
                             while(true) {
                                 int len = is.read(buffer);

                                 if(len < 0) {
                                     break;
                                 }
                                 fos.write(buffer);

                                 //bout.write(buffer, 0, len);
                                // bout.writeTo(bos);



                             }

                             is.close();
                         }


                     }else {
                         sess.execCommand("pkill -KILL -f nclw");

                     }

                     sess.close();

                 }else{
                     System.out.println("SCP Connection Failed...");
                 }

             }
             if(args.length>3 &&(option.equals("collectlog"))) {
                 fos.close();
             }



         }catch(Exception e){
             e.printStackTrace();
         }





    }


}
