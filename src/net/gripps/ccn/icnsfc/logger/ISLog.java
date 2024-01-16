package net.gripps.ccn.icnsfc.logger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Created by Hidehiro Kanemitsu on 2020/08/27
 */
public class ISLog implements Runnable{
    //getLoggerの引数はロガー名を指定する。
    //log4j2では、ロガー名の指定が省略可能になった。
    private static Logger logger;

    private PriorityBlockingQueue<String> msgQueue;

    public static ISLog own;

    public static ISLog getIns(){
        if(ISLog.own == null){
            ISLog.own = new ISLog();
        }
        return ISLog.own;
    }

    private  ISLog(){
        ISLog.logger = LogManager.getLogger();
        this.msgQueue = new PriorityBlockingQueue<String>();
        Thread t = new Thread(this);
        t.start();
    }

    /**
     * ログ出力する．
     * @param m
     */
    public void log(String m){
        //logger.info(m);
        this.msgQueue.offer(m);
    }

    @Override
    public void run() {
        while(true){
            if(!this.msgQueue.isEmpty()){
                String msg = this.msgQueue.poll();
                logger.info(msg);
            }
        }
    }

    /**
     public void runSample() {

     logger.trace("Start"); //2017/01/21 06:02:17.154 [main] TRACE  test1.Sample Start

     int a = 1;
     int b = 2;
     String c = null;

     logger.info("this is a "+a);
     logger.debug("debug"); //2017/01/21 06:02:17.157 [main] DEBUG  test1.Sample debug
     logger.info("info={}",a); //2017/01/21 06:02:17.159 [main] INFO   test1.Sample info=1
     logger.warn("warn={},={}" ,a,b); //2017/01/21 06:02:17.159 [main] WARN   test1.Sample warn=1,=2
     logger.error("error={}",c); //2017/01/21 06:02:17.171 [main] ERROR  test1.Sample error=null

     logger.trace("End"); //2017/01/21 06:02:17.172 [main] TRACE  test1.Sample End
     }
     **/
}
