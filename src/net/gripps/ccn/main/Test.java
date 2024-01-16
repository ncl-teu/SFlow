package net.gripps.ccn.main;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

public class Test {
    public static void main(String[] args){
        try{
            //Runtime r = Runtime.getRuntime();
            //Process p = r.exec("route print");
            //コマンドのリスト
            //java -versionであれば，cmdList(0)が"java" cmdList(1)が"-version"
            List<String> cmdList = new LinkedList<String>();
            cmdList.add("route");
            cmdList.add("print");

            ProcessBuilder b = new ProcessBuilder(cmdList);
            Process p = b.start();
            //プロセスが終了するのを待つ．

            p.waitFor();
            int code = p.exitValue();
            System.out.println(code);
            //出力結果を得る．
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            p.getInputStream(),
                    Charset.defaultCharset()
                    )
            );
            BufferedReader reader2 = new BufferedReader(
                    new InputStreamReader(
                            p.getErrorStream(),
                            Charset.defaultCharset()
                    )
            );

            StringBuffer buf = new StringBuffer();
            String line;
            //出力結果を最終行まで読み込む
            while((line = reader.readLine()) != null){
                buf.append(line+"\n");
            }

            while((line = reader2.readLine())!= null){
                buf.append(line + "\n");
            }
            System.out.println(buf.toString());

        }catch(Exception e){
            e.printStackTrace();
        }


    }
}

