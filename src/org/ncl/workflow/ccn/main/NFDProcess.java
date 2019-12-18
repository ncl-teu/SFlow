package org.ncl.workflow.ccn.main;

import com.intel.jndn.forwarder.Forwarder;
import com.intel.jndn.forwarder.api.FaceManager;
import com.intel.jnfd.deamon.fw.ForwardingPipeline;
import org.ncl.workflow.ccn.util.Setup;



import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by Hidehiro Kanemitsu on 2019/11/12.
 */
public class NFDProcess {
    public static void main(String[] args) {
        try {

            final ScheduledExecutorService pool;
            final ForwardingPipeline pipeline;
            final FaceManager faceManager;

            Forwarder forwarder = new Forwarder();

//テスト用にデータを準備して，テーブルへ入れておく処理
            Setup setup = new Setup();
            setup.prepare();
            forwarder.run();

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }


    }

}
