package net.gripps.p2p;

import net.gripps.environment.Environment;

/**
 * Author: H. Kanemitsu
 * Date: 2010/06/01
 * ピアグループ生成を管理するためのクラスです．
 * 指定数のピアをEnvoronmentクラスを元に作成し，さらにこのクラスでは，隣接ピアも管理します．
 * 
 */
public class PeerGroupingManager {
    /**
     * Singletonオブジェクト
     */
    private static PeerGroupingManager singleton;

    /**
     *
     */
    private Environment env;

    public PeerGroupingManager getInstance(){
        if(PeerGroupingManager.singleton == null){
            PeerGroupingManager.singleton = new PeerGroupingManager();
        }
        return PeerGroupingManager.singleton;
    }

    private PeerGroupingManager(){

    }
}
