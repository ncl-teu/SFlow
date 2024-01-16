package net.gripps.cloud.nfv.sfc;

import net.gripps.cloud.nfv.NFVUtil;
import net.gripps.cloud.nfv.clustering.HUtil;
import net.gripps.clustering.common.aplmodel.CustomIDSet;
import net.gripps.clustering.common.aplmodel.DataDependence;

import java.util.*;

/**
 * Ray Tracingのワークフロー生成クラスです
 * 並列度を決めて，各列でのタスク数を決めます．
 * このとき，
 * - 最初は交差点の計算（最も重たい）
 * - 他の処理をいくつか
 * という感じです．
 */
public class RayTracingSFCGenerator extends SFCGenerator {
    /**
     * シングルトンオブジェクト
     */
    protected static RayTracingSFCGenerator own;

    private RayTracingSFCGenerator() {
        super();
    }

    public static RayTracingSFCGenerator getIns() {
        if (RayTracingSFCGenerator.own == null) {
            RayTracingSFCGenerator.own = new RayTracingSFCGenerator();
        }
        return RayTracingSFCGenerator.own;
    }


    public SFC singleSFCProcess() {

        this.constructFunction();
        //this.assignDependencyProcess();
        //完成したSFCを取得
        SFC sfc = this.getSfc();
        this.sfcList.add(sfc);
        Iterator<VNFCluster> cIte = sfc.getVNFClusterMap().values().iterator();
        while(cIte.hasNext()){
            VNFCluster c = cIte.next();
            c.configureVNFCluster();
        }
        this.setSfc(sfc);



        return this.sfc;
    }

    /**
     *
     * @return
     */
    public VNF buildChildVNF(long pixelNum) {
        double w_min = 0;
        double w_max = 0;
        double w_mu = 0.5;

        //(交差判定+確率x透過処理+確率x反射処理+色塗り)*pixel数
        double w_kousa = NFVUtil.genDouble2(RayUtil.ray_intersection_workload_min, RayUtil.ray_intersection_workload_max, 1, 0.5);
        double w_touka = NFVUtil.genDouble2(RayUtil.ray_lighting_workload_min, RayUtil.ray_lighting_workload_max, 1, 0.5);
        double w_hansya = NFVUtil.genDouble2(RayUtil.ray_reflection_workload_min, RayUtil.ray_reflection_workload_max, 1, 0.5);
        double prob_lighting = NFVUtil.genDouble2(RayUtil.ray_lighting_probability_min, RayUtil.ray_lighting_probability_max, 1, 0.5);
        double prob_reflect = NFVUtil.genDouble2(RayUtil.ray_reflection_probability_min, RayUtil.ray_reflection_probability_max, 1, 0.5);
        double prob_glass = NFVUtil.genDouble2(RayUtil.ray_glass_probability_min, RayUtil.ray_glass_probability_max, 1, 0.5);
        double w_iro = NFVUtil.genDouble2(RayUtil.ray_glass_workload_min, RayUtil.ray_glass_workload_max, 1, 0.5);
        double t_w = pixelNum * (w_kousa+prob_lighting*w_touka + prob_reflect*w_hansya +
                prob_glass * w_iro);

        //使用率を求める．
        int usage = NFVUtil.genInt2(NFVUtil.vnf_usage_min, NFVUtil.vnf_usage_max, NFVUtil.dist_vnf_usage, NFVUtil.dist_vnf_usage_mu);
        int type = NFVUtil.genInt(1, NFVUtil.vnf_type_max);

        VNF newVNF = new VNF(type, (long)t_w, -1, -1, -1, null, usage);


        return newVNF;
    }

    public void createVNFCluster(VNF vnf, SFC sfc){
        CustomIDSet vnfSet = new CustomIDSet();
        vnfSet.add(vnf.getIDVector().get(1));

        VNFCluster cluster = new VNFCluster(vnf.getIDVector().get(1), null, vnfSet, vnf.getWorkLoad());
        vnf.setClusterID(cluster.getClusterID());
        sfc.getVNFClusterMap().put(cluster.getClusterID(), cluster);
        cluster.setClusterSize(vnf.getWorkLoad());
        if (vnf.getDpredList().isEmpty()) {
            sfc.getStartVNFSet().add(vnf.getIDVector().get(1));
        }

        if (vnf.getDsucList().isEmpty()) {
            sfc.getEndVNFSet().add(vnf.getIDVector().get(1));
        }
        if(vnf.getIDVector().get(1) == sfc.getVnfMap().size()){
            Iterator<DataDependence> dpredIte = vnf.getDpredList().iterator();
            while (dpredIte.hasNext()) {
                DataDependence dd = dpredIte.next();
                VNF dpredTask = sfc.findVNFByLastID(dd.getFromID().get(1));
                CustomIDSet ansSet = new CustomIDSet();
                this.updateAncestor(ansSet, dpredTask);
            }
        }


        //全体のPGの命令数を集計する．
        //VNF task = (BBTask)AplOperator.getInstance().calculateInstructions(AplOperator.getInstance().getApl());
        //SFCGenerator.getIns().setSfc(sfc);



    }

    @Override
    public void constructFunction() {
        try {
            //最上位レイヤ(第一層)におけるファンクション数
            //long tasknum = NFVUtil.sfc_vnf_num;            //APLを生成して，シングルトンにセットする．
            SFC apl = new SFC(-1, -1, -1, -1, -1, -1,
                    -1, null, new HashMap<Long, VNF>(), new HashMap<Long, VNFCluster>(), new Long(1), -1, -1);
            Vector<Long> id = new Vector<Long>();
            id.add(new Long(1));
            apl.setIDVector(id);
            RayTracingSFCGenerator.getIns().setSfc(apl);
            //long level = HUtil.hclustering_level_num;
            // for (int i = 0; i < tasknum; i++) {
            long predNum = 0;
            long currentNum = 0;
            long predMaxWorkload = 0;
            long pixelNum = 0;
            long taskNum = 0;
            //各タスクがもつデータサイズを決定する．
            double dataSize = (double)RayUtil.size_per_pixel * RayUtil.ray_pixel_per_task / (1024*1024);
            //まずは，タスク数を決める．
            switch(RayUtil.ray_quality){
                //HD
                case 1:
                    pixelNum = 1280 * 720;
                    break;
                case 2:
                    pixelNum = 1920 * 1080;
                    break;
                case 3:
                    pixelNum = 3840 * 2160;
                    break;
                default:
                    pixelNum = 3840 * 2160;
                    break;
            }
            //トータルのタスク数
            taskNum = (long)Math.ceil(pixelNum / RayUtil.ray_pixel_per_task);
            //残りの数
            long remainNum = taskNum;
            long parallelism = 0;
            //まずはstartタスク
            //1スレッド内のタスク数を決める．
            SFC sfc = RayTracingSFCGenerator.getIns().getSfc();

            //STARTタスク(ID=1)を生成する．
            VNF startVNF = this.buildChildVNF();
            sfc.addVNF(startVNF);
            sfc.getStartVNFSet().add(startVNF.getIDVector().get(1));
            this.createVNFCluster(startVNF, sfc);


            VNF endVNF = this.buildChildVNF();
            Vector<Long> endVec = new Vector();
            endVec.add(new Long(1));
            endVec.add(new Long(taskNum+2));
            endVNF.setIDVector(endVec);
            long ldsize = (long)dataSize;
            long taskNumPerThred = NFVUtil.genLong2(RayUtil.ray_thread_min, RayUtil.ray_thread_max, 1, RayUtil.ray_thread_mu);

            // long taskNumPerThred =
            while(remainNum > 0){
                //1スレッド内のタスク数を決める．
                //そのスレッド内のタスク数分だけ，タスクを生成する．
                for(int i=0;i<taskNumPerThred;i++){

                    VNF vnf = this.buildChildVNF(RayUtil.ray_pixel_per_task);
                    sfc.addVNF(vnf);
                    //i=0のときは，startVNFと依存関係を持たせる．
                    if(i==0){
                        DataDependence dd = new DataDependence(startVNF.getIDVector(), vnf.getIDVector(), ldsize, ldsize, ldsize);
                        startVNF.addDsuc(dd);
                        vnf.addDpred(dd);
                        if (sfc.getMaxData() <= dd.getMaxDataSize()) {
                            sfc.setMaxData(dd.getMaxDataSize());
                        }
                        if (sfc.getMinData() >= dd.getMaxDataSize()) {
                            sfc.setMinData(dd.getMaxDataSize());
                        }
                    }else if(i == taskNumPerThred-1 || remainNum == 1){
                        //ENDとの依存関係
                        DataDependence dd = new DataDependence(vnf.getIDVector(), endVNF.getIDVector(), ldsize, ldsize, ldsize);
                        vnf.addDsuc(dd);
                        endVNF.addDpred(dd);
                        if (sfc.getMaxData() <= dd.getMaxDataSize()) {
                            sfc.setMaxData(dd.getMaxDataSize());
                        }
                        if (sfc.getMinData() >= dd.getMaxDataSize()) {
                            sfc.setMinData(dd.getMaxDataSize());
                        }
                    }else{
                        long predID = vnf.getIDVector().get(1) -1;
                        VNF predVNF = sfc.findVNFByLastID(new Long(predID));
                        DataDependence dd = new DataDependence(predVNF.getIDVector(), vnf.getIDVector(), ldsize, ldsize, ldsize);
                        predVNF.addDsuc(dd);
                        vnf.addDpred(dd);
                        if (sfc.getMaxData() <= dd.getMaxDataSize()) {
                            sfc.setMaxData(dd.getMaxDataSize());
                        }
                        if (sfc.getMinData() >= dd.getMaxDataSize()) {
                            sfc.setMinData(dd.getMaxDataSize());
                        }
                    }
                    this.createVNFCluster(vnf, sfc);
                    remainNum--;
                    if(remainNum < 1){
                        break;
                    }
                }

            }
            sfc.addVNF(endVNF);
            sfc.getEndVNFSet().add(endVNF.getIDVector().get(1));
            this.createVNFCluster(endVNF, sfc);

            Long endID = endVNF.getIDVector().get(1);



        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    /*
    public VNF buildChildVNF(int level, long w) {
        long weight = 0;
        if (level == 0) {
            weight = NFVUtil.vnf_weight_max;
        } else {
            double w_rate = HUtil.genDouble2(HUtil.hclustering_vnf_workload_rate_min, HUtil.hclustering_vnf_workload_rate_max,
                    HUtil.dist_hclustering_vnf_workload_rate, HUtil.dist_hclustering_vnf_workload_rate_mu);
            weight = (long) Math.ceil(w * w_rate);
        }        //使用率を求める．
        int usage = NFVUtil.genInt2(NFVUtil.vnf_usage_min, NFVUtil.vnf_usage_max, NFVUtil.dist_vnf_usage, NFVUtil.dist_vnf_usage_mu);
        int type = NFVUtil.genInt(1, NFVUtil.vnf_type_max);
        VNF newVNF = new VNF(type, weight, -1, -1, -1, null, usage);
        return newVNF;
    }
*/
    public VNFCluster createCluster(VNF vnf) {        //クラスタを生成しておく．
        //VNFクラスタも作っておく．
        CustomIDSet vnfSet = new CustomIDSet();
        vnfSet.add(vnf.getIDVector().get(1));
        VNFCluster cluster = new VNFCluster(vnf.getIDVector().get(1), null, vnfSet, vnf.getWorkLoad());
        vnf.setClusterID(cluster.getClusterID());
        sfc.getVNFClusterMap().put(cluster.getClusterID(), cluster);
        if (vnf.getDpredList().isEmpty()) {
            sfc.getStartVNFSet().add(vnf.getIDVector().get(1));
        }
        if (vnf.getDsucList().isEmpty()) {
            sfc.getEndVNFSet().add(vnf.getIDVector().get(1));
        }
        cluster.configureVNFCluster();
        return cluster;
    }
}

