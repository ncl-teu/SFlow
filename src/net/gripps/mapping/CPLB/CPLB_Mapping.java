package net.gripps.mapping.CPLB;

import net.gripps.mapping.AbstractMapping;
import net.gripps.clustering.common.aplmodel.BBTask;
import net.gripps.clustering.common.aplmodel.TaskCluster;
import net.gripps.clustering.common.aplmodel.CustomIDSet;
import net.gripps.environment.Environment;

/**
 * CPLBマッピングです．
 * Critical Path上のクラスタのうちで，最もデータサイズ＋クラスタサイズの大きなものを選択する．
 * そして，Partial Scheduleしてみて，最も応答時間の抑えられるマシンを選択する．
 *
 * Author: H. Kanemitsu
 * Date: 2009/05/26
 */
public class CPLB_Mapping extends AbstractMapping {

    /**
     *
     * @param task
     * @param file
     */
    public CPLB_Mapping(BBTask task, String file) {
        super(task, file);    //To change body of overridden methods use File | Settings | File Templates.
    }

    /**
     *
     * @param apl
     * @param file
     * @param env
     */
    public CPLB_Mapping(BBTask apl, String file, Environment env) {
        super(apl, file, env);    //To change body of overridden methods use File | Settings | File Templates.
    }



    /**
     *
     * @return
     */
    public TaskCluster selectTaskCluster(){
        return null;

        

    }

    /**
     *
     * @return
     */
    public CustomIDSet getDSClusterSet(){
        return null;
    }

    /**
     * CriticalPathを求める．
     * @return
     */
    public BBTask mapping(){
        return null;
    }

}
