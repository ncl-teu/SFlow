package org.ncl.workflow.main;

import ch.ethz.ssh2.*;
import org.ncl.workflow.util.HostInfo;
import org.ncl.workflow.util.SshCommandExecute;

import java.io.*;
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
             while(hostIte.hasNext()){
                 HostInfo h = hostIte.next();
                 Connection conn = new Connection(h.getIpAddress());
                 ConnectionInfo info = conn.connect();
                 boolean result = conn.authenticateWithPassword(h.getUserName(),
                         h.getPassword());
                 if (result) {

                     Session sess= conn.openSession();
                     if(option.equals("start")){
                         sess.execCommand("cd " + h.getPath()+" && "+ "./nclw_worker.sh");
                     }else{
                         sess.execCommand("pkill -KILL -f nclw");

                     }

                     sess.close();

                 }else{
                     System.out.println("SCP Connection Failed...");
                 }

             }


         }catch(Exception e){
             e.printStackTrace();
         }





    }


}
