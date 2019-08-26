package org.ncl.workflow.main;


import org.ncl.workflow.util.NCLWUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.rmi.Remote;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by Hidehiro Kanemitsu on 2019/06/08.
 * - 遠隔からdockerイメージをDLする．docker
 * - DLしたイメージをロードする．
 * - cmdで定義したコマンドを実行する．
 * - インスタンス = Dockerイメージ
 *
 * docker build -t 名前 .
 * 1. dockerイメージをtar保存する．
 * step1. docker save イメージ名 -o 名前.tar
 * ~~~~~~~~~~ここから↓は，実行時~~~~~~~
 * step2. step1で保存したtarファイルをとってくる．
 * step2. ロードする．
 *  docker load -i busybox.tar
 *  step2. 実行する．
 *  docker run -t busybox /bin/echo "Hello World"
 *  [成功例]
 *  docker run --rm -v `pwd`:/home/kanemih/docker2 cvt /home/kanemih/docker2/ret.jpg /home/kanemih/docker2/out.jpg
 *  docker run -it -v `pwd`:/home/kanemih/ cvt /home/kanemih/docker2/ret.jpg /home/kanemih/docker2/out.jpg
 *  [コマンドがあるかどうか]
 *  1. which [コマンド]
 *  ubuntu, jetsonの場合，無ければ，null
 *  centOSの場合，無ければ** no [コマンド] in **** という書式
 *
 *  2. type [コマンド]
 *  jetsonの場合, ** not found**
 *  ubuntu: 見つかりません
 *  centos: 見つかりません
 *
 *  3. hash [コマンド]
 *  ubuntu, jetson, centOS: あればnull, なければ文字列あり．
 *
 * Windowsの場合, %PATH%に，コマンドが含まれているかどうかになるかと思われる．
 */
public class DockerTest {
    public static void main(String[] args){
        try{
            String os = System.getProperty("os.name").toLowerCase();
            System.out.println("OS:"+os);
            String test = "./test/ret.jpg";
            String ret = test.replaceAll("\\.\\/", "kanemih/");
            LinkedList<String> hashCmd = new LinkedList<String>();
            if(NCLWUtil.isWindows()){
                hashCmd.add("echo");
                hashCmd.add("%PATH%");
                hashCmd.add("|");
                hashCmd.add("grep");
                hashCmd.add("java");
            }else{
                hashCmd.add("which");
                hashCmd.add("java");
            }


            LinkedList<String> cmd = new LinkedList<String>();
            cmd.add("docker");
            cmd.add("run");
            cmd.add("--rm");
            cmd.add("-v");
            StringBuffer buf = new StringBuffer("`pwd`:");
            String a = new File(".").getAbsoluteFile().getParent();
            // "/"は含まず．
            buf.append(new File(".").getAbsoluteFile().getParent());
            //cmd.add("")
            ProcessBuilder builder = new ProcessBuilder(cmd);
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
                int result = process.exitValue();
           //     System.out.println("cmd:" + actualCmd + "code:" + result);

                       /* try{
                            Thread.sleep(3000);
                        }catch(Exception e){
                            e.printStackTrace();
                        }

                        */
                //Docker
                // Docker docker = new LocalDocker()
       /*     Docker docker = new LocalDocker( new File("/var/run/docker.sock"));


            //Docker rd = new RemoteDocker();
            Containers containers = docker.containers();
/*
            for(final Container ctn : containers) {//iterate over the running containers, with the default filters.
                System.out.println(ctn);//Container implements JsonObject and contains the Json returned by List-Containers operation
            }


            Iterator<Container> cIte = containers.all();
            while(cIte.hasNext()){
            //for(final Container ctn : containers.all()) {//iterate over all the containers (not only the running ones), with the default filters.
                Container ctn = cIte.next();
                System.out.println(ctn);//Container implements JsonObject and contains the Json returned by List-Containers operation
            }

            Container created = containers.create("debian");//overloaded, so you can also pass the name and/or JsonObject config
            System.out.println(created);//created JsonObject contains just the ``Id`` and ``Warnings`` JsonObject config for the created container.
*/
            }
        }catch(Exception e){
            e.printStackTrace();
        }

    }

}
