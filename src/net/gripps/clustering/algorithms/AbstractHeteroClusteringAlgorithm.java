package net.gripps.clustering.algorithms;

import net.gripps.clustering.common.aplmodel.AbstractTask;
import net.gripps.clustering.common.aplmodel.BBTask;
import net.gripps.clustering.common.aplmodel.DataDependence;
import net.gripps.clustering.common.aplmodel.TaskCluster;
import net.gripps.clustering.tool.Calc;
import net.gripps.environment.P2PEnvironment;

import java.io.FileInputStream;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Vector;

/**
 * Author: H. Kanemitsu
 * Date: 11/06/16
 */
public class AbstractHeteroClusteringAlgorithm extends AbstractClusteringAlgorithm {

    /**
     *
     */
    private P2PEnvironment p2penv;

    /**
     *
     */
    private boolean hopuse;

    public AbstractHeteroClusteringAlgorithm(String file, BBTask apl) {
        super(file, apl);
    }

    public AbstractHeteroClusteringAlgorithm(BBTask task) {
        super(task);
    }

    public AbstractHeteroClusteringAlgorithm(BBTask task, String file, int algorithm) {
        super(task, file, algorithm);
    }

    /**
     * @param apl
     * @param file
     * @param env
     */
    public AbstractHeteroClusteringAlgorithm(BBTask apl, String file, P2PEnvironment env) {
        super(file, apl);
        this.p2penv = env;

        try {
            Properties prop = new Properties();
            //create input stream from file
            prop.load(new FileInputStream(file));
            int hopuse_tmp = Integer.valueOf(prop.getProperty("network.hop.use")).intValue();
            if (hopuse_tmp == 1) {
                this.hopuse = true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param fromTaskID
     * @param toTaskID
     * @param data
     * @param link
     * @param hop
     * @return
     */
    public double getNWTime(Long fromTaskID, Long toTaskID, long data, long link, long hop) {
        AbstractTask fromTask = this.retApl.findTaskByLastID(fromTaskID);
        AbstractTask toTask = this.retApl.findTaskByLastID(toTaskID);

        //もし双方のタスクが同じクラスタに属していれば，データ転送時間は0となる．
        if (fromTask.getClusterID().longValue() == toTask.getClusterID().longValue()) {
            return 0;
        } else {
            if(this.hopuse){
                return Calc.getRoundedValue((double)data*hop/link);
            }else{
                 return Calc.getRoundedValue((double)data/link);
            }

        }

    }

}
