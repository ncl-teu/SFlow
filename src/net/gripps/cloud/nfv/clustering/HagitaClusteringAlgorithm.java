package net.gripps.cloud.nfv.clustering;

import net.gripps.cloud.core.CloudEnvironment;
import net.gripps.cloud.nfv.sfc.SFC;
import net.gripps.cloud.nfv.sfc.VNFCluster;

public class HagitaClusteringAlgorithm extends AbstractVNFClusteringAlgorithm {

    public HagitaClusteringAlgorithm(CloudEnvironment env, SFC sfc) {
        super(env, sfc);
    }

    @Override
    public VNFCluster selectVNFCluster() {
        return null;
    }

    @Override
    public VNFCluster processVNFCluster(VNFCluster cluster) {
        return null;
    }

    public void mainProcess() {
        //未スケジュールなVNFが残っている間，行うループ
        while (!this.UEXClusterSet.isEmpty()) {
            VNFCluster cls = this.selectVNFCluster();
            //クラスタリングの対象となるクラスタを選び，そして
            //指定のvcpuへと割り当てる処理をよぶ．
            this.processVNFCluster(cls);

        }
    }
}