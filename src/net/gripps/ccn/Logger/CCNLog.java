package net.gripps.ccn.Logger;

import net.gripps.ccn.process.CCNMgr;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.PriorityBlockingQueue;

public class CCNLog implements Runnable{
        //getLoggerの引数はロガー名を指定する。
        //log4j2では、ロガー名の指定が省略可能になった。
        private static Logger logger;

        public static CCNLog own;

        public static boolean isSFCMode;

        private PriorityBlockingQueue<String> msgQueue;

        public static CCNLog getIns(){
                if(CCNLog.own == null){
                        CCNLog.own = new CCNLog();
                }
                return CCNLog.own;
        }

        private  CCNLog(){
                 CCNLog.logger = LogManager.getLogger();
                 this.msgQueue = new PriorityBlockingQueue<String>();
                 Thread t = new Thread(this);
                 t.start();
        }

        @Override
        public void run() {
                while(true){
                        if(!this.msgQueue.isEmpty()){
                                String msg = this.msgQueue.poll();
                                if(CCNLog.isIsSFCMode()){
                                        return;
                                }else{
                                        logger.info(msg);

                                }
                        }
                }
        }

        public static boolean isIsSFCMode() {
                return isSFCMode;
        }

        public static void setIsSFCMode(boolean isSFCMode) {
                CCNLog.isSFCMode = isSFCMode;
        }

        /**
         * ログ出力する．
         * @param m
         */
        public void log(String m){
                this.msgQueue.offer(m);

               /* if(CCNLog.isIsSFCMode()){
                        return;
                }else{
                        logger.info(m);

                }*/
                /*
                if(CCNMgr.getIns().isSFCMode()){

                }else{
                        logger.info(m);

                }

                 */
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



