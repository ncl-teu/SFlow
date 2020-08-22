package org.ncl.workflow.logger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by Hidehiro Kanemitsu on 2020/08/11
 */
public class NclwLog {

    //getLoggerの引数はロガー名を指定する。
    //log4j2では、ロガー名の指定が省略可能になった。
    private static Logger logger;

    public static NclwLog own;

    public static NclwLog getIns(){
        if(NclwLog.own == null){
            NclwLog.own = new NclwLog();
        }
        return NclwLog.own;
    }

    private  NclwLog(){
        NclwLog.logger = LogManager.getLogger();
    }

    /**
     * ログ出力する．
     * @param m
     */
    public void log(String m){
        logger.info(m);
    }
}
