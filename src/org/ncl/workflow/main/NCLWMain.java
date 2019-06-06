package org.ncl.workflow.main;

import org.ncl.workflow.comm.NCLWData;
import org.ncl.workflow.engine.NCLWEngine;
import org.ncl.workflow.resource.ResourceMgr;
import org.ncl.workflow.util.NCLWUtil;

import java.io.*;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.StringTokenizer;


/**
 * Created by Hidehiro Kanemitsu on 2019/04/26.
 */
public class NCLWMain {
    public static void main(String[] args){

    try {

            if (args.length <= 1) {
                System.out.println("Please input 2 arguments [node ID] [property file name] ");
                System.exit(-1);
            }
            long nodeID = Long.valueOf(args[0]);

            //property file name
            String propName = args[1];


            //Set parameters
            NCLWUtil.getIns().initialize(propName);
            NCLWEngine.getIns().setNodeID(nodeID);

            Thread mgmtThread = new Thread(NCLWEngine.getIns());
            mgmtThread.start();
   /*
        for(int i=0;i<10000;i++){
            double load = ResourceMgr.getIns().getAveVCPULoad();

            System.out.println("load:"+load);
            Thread.sleep(1000);
        }

    */
        }catch(Exception e){
            System.out.println("Please input correct arguments [node ID] [property file name] ");
            System.exit(-1);
        }

    }


}
