package org.ncl.workflow.main;

import org.ncl.workflow.comm.NCLWData;
import org.ncl.workflow.comm.SendThread;


/**
 * Created by Hidehiro Kanemitsu on 2019/05/03.
 */
public class Client {
    public static void main(String[] args) {
        final String HOST = "localhost"; // 接続先アドレス
        final int PORT = 8001;        // 接続先ポート番号

        //readFilePath: 送るファイルのpath, //相手側で受信後，入力ファイル生成path
        NCLWData data = new NCLWData("test.flv", "test1000.flv", 0, 0, "localhost",
               true, 8001 );

        NCLWData data2 = new NCLWData("test2.flv", "test5000.flv", 0, 0, "localhost",
                true, 8001 );

    /*    NCLWData data2 = new NCLWData("test.flv", "test1001.flv", 0, 0, "localhost",
                true, 8001 );
        NCLWData data3 = new NCLWData("test.flv", "test1002.flv", 0, 0, "localhost",
                true, 8001 );
                */

       /*
        NCLWData data2 = new NCLWData("majin.jpg", "majin3.jpg", 0, 0, "localhost",
                false, 8001 );

        */
        SendThread sender = new SendThread();
        sender.getDataQueue().add(data);

        sender.getDataQueue().add(data2);
    //    sender.getDataQueue().add(data2);
    //    sender.getDataQueue().add(data3);
        //sender.getDataQueue().add(data2);
        Thread t = new Thread(sender);
        t.start();




    }

}
