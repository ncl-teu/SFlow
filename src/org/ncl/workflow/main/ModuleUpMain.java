package org.ncl.workflow.main;

import ch.ethz.ssh2.*;
import net.gripps.environment.CPU;
import net.gripps.environment.Machine;
import org.ncl.workflow.util.HostInfo;
import org.ncl.workflow.util.NCLWUtil;
import org.ncl.workflow.util.SshCommandExecute;
import sun.awt.image.ImageWatched;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Created by Hidehiro Kanemitsu on 2019/08/05.
 */
public class ModuleUpMain {
    public static void main(String[] args){

        //hostsファイル取得
        String hostsFile = args[0];
        String user = args[1];

        if(args.length == 0){
            System.out.println("Please input hosts file...");
            System.exit(-1);
        }

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
                    SCPClient scp = conn.createSCPClient();
                    //scp.put("/home/kanemih/gripps/", h.getPath(), "0755");

                    ModuleUpMain.putDir(conn, "/home/"+user+"/gripps/", h.getPath(), "0777");

                    SshCommandExecute ssh1 = new SshCommandExecute(conn.openSession());
                    ssh1.exec("ls -l");

                    SshCommandExecute ssh2 = new SshCommandExecute(conn.openSession());
                    ssh2.exec("chmod -R 777 " + h.getPath(), 10000);
                    System.out.println(ssh2.getStdout());
                    ssh2.close();

                    ssh1.join(10000);
                    System.out.println(ssh1.getStdout());
                    ssh1.close();

                    conn.close();

                }else{
                    System.out.println("SCP Connection Failed...");
                }
            /**    JSch jsch;
                Session session = null;
                ChannelExec channel = null;
                BufferedInputStream bin = null;
                //接続
                jsch = new JSch();
                session = jsch.getSession(h.getUserName(), h.getIpAddress(), 22);
                //known_hostsのチェックをスキップ
                session.setConfig("StrictHostKeyChecking", "no");
                session.setPassword(h.getPassword());
                session.connect();

                channel = (ChannelExec) session.openChannel("exec");
                String command = orgCmd +"/home/kanemih/gripps/ "+h.getUserName()+"@"+h.getIpAddress()
                        +":"+h.getPath();
                channel.setCommand(command );
                System.out.println("CMD:"+command);
                channel.connect();

                //コマンド実行
                bin = new BufferedInputStream(channel.getInputStream());
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                byte[] buf = new byte[1024];
                int length;
                while (true) {
                    length = bin.read(buf);
                    if (length == -1) {
                        break;
                    }
                    bout.write(buf, 0, length);
                }
                //標準出力
                System.out.format("Result=%1$s", new String(bout.toByteArray(), StandardCharsets.UTF_8));
             **/
            }


        }catch(Exception e){
            e.printStackTrace();
        }



    }

    public  static void putDir( Connection conn, String localDirectory, String remoteTargetDirectory, String mode) throws IOException {
        File curDir = new File(localDirectory);
        final String[] fileList = curDir.list();
        for (String file : fileList) {
            final String fullFileName = localDirectory + "/" + file;
            if (new File(fullFileName).isDirectory()) {
                final String subDir = remoteTargetDirectory + "/" + file;
                Session sess = conn.openSession();
                sess.execCommand("mkdir " + subDir);
                sess.waitForCondition(ChannelCondition.EOF, 0);
                ModuleUpMain.putDir(conn, fullFileName, subDir, mode);
            }
            else {
                SCPClient scpc = conn.createSCPClient();
                scpc.put(fullFileName, remoteTargetDirectory, mode);

            }
        }
    }
}
