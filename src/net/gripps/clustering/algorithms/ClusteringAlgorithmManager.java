package net.gripps.clustering.algorithms;

///import net.gripps.clustering.algorithms.lbm.LBM_Algorithm;
//import net.gripps.merging.algorithms.workprofiling.WP_Algorithm;
//import net.gripps.merging.algorithms.Random.Random_MergingAlgorithm;
//import net.gripps.clustering.algorithms.ClusteringAlgorithmManager;

import net.gripps.clustering.algorithms.dsc.DSC_Algorithm;
import net.gripps.clustering.algorithms.dsc.DSC_WPAlgorithm;
import net.gripps.clustering.algorithms.cassII.CASSII_Algorithm;
import net.gripps.clustering.algorithms.cassII.CASSII_LBAlgorithm;
import net.gripps.clustering.algorithms.loadbacancing.LB_Algorithm;
//import net.gripps.clustering.algorithms.random.Random_Algorithm;
//import net.gripps.clustering.algorithms.loadbacancing.CASSII_Algorithm;
import net.gripps.clustering.algorithms.mwsl_delta.MWSL_delta;
import net.gripps.clustering.common.aplmodel.AplOperator;
import net.gripps.clustering.common.aplmodel.BBTask;
//import net.gripps.clustering.algorithms.mwsl_delta.LBC_BestAlgorithm;

import java.util.LinkedList;
import java.util.Properties;
import java.io.FileInputStream;

/**
 * Author: Hidehiro Kanemitsu<br>
 * Date: 2007/08/06
 */
public class ClusteringAlgorithmManager {
    private static ClusteringAlgorithmManager ownInstance;

    /**
     * Merging Algorithmのリスト
     */
    private LinkedList<AbstractClusteringAlgorithm> algorithmList;


    /**
     * @return
     */
    public static ClusteringAlgorithmManager getInstance() {
        if (ClusteringAlgorithmManager.ownInstance == null) {
            ClusteringAlgorithmManager.ownInstance = new ClusteringAlgorithmManager();
        }

        return ClusteringAlgorithmManager.ownInstance;
    }

    private ClusteringAlgorithmManager() {
        algorithmList = new LinkedList<AbstractClusteringAlgorithm>();

    }

    /**
     * マージングアルゴリズムたちを初期化及び実行します．
     *
     * @return
     */
    public LinkedList<BBTask> process(String filename) {
        try {
              LinkedList<BBTask> aplList = new LinkedList<BBTask>();
            Properties prop = new Properties();
            //create input stream from file
            prop.load(new FileInputStream(filename));
            //実行モード
            int mode = Integer.valueOf(prop.getProperty("algorithm.clustering.using")).intValue();

            switch (mode) {
                /*case 0:
                    BBTask lbcbest_apl = AplOperator.getInstance().getApl();
                    LBC_BestAlgorithm lbcbest = new LBC_BestAlgorithm(lbcbest_apl, filename);
                    this.addAlgorithm(lbcbest);
                    break;
              */
                //LBC(Level-Based Clustering)のみの場合
                case 1:
                    BBTask lbc_apl = AplOperator.getInstance().getApl();
                    MWSL_delta SIRTDelta = new MWSL_delta(lbc_apl, filename);
                    this.addAlgorithm(SIRTDelta);
                    break;
                //CASS-II
                case 2:
                    BBTask cassII_apl = AplOperator.getInstance().getApl();
                    CASSII_Algorithm CASSII = new CASSII_Algorithm(cassII_apl, filename);
                    this.addAlgorithm(CASSII);
                    break;
                //CASS-II + LB
                case 3:
                    BBTask cassIILB_apl = AplOperator.getInstance().getApl();
                    CASSII_LBAlgorithm CASSIILB = new CASSII_LBAlgorithm(cassIILB_apl, filename);
                    this.addAlgorithm(CASSIILB);
                    break;
                //DSC
                case 4:
                    BBTask dsc_apl = AplOperator.getInstance().getApl();
                    DSC_Algorithm dsc = new DSC_Algorithm(dsc_apl, filename);
                    this.addAlgorithm(dsc);
                    break;
                case 5:
                    BBTask dsc_apllb = AplOperator.getInstance().getApl();
                    DSC_WPAlgorithm dsclb = new DSC_WPAlgorithm(dsc_apllb, filename);
                    this.addAlgorithm(dsclb);
                    break;

                case 6:
                    BBTask lb_apl = AplOperator.getInstance().getApl();
                    LB_Algorithm lb = new LB_Algorithm(lb_apl, filename);
                    this.addAlgorithm(lb);

                case 7:
                    BBTask lbc_apl5 = AplOperator.getInstance().getApl();
                    MWSL_delta lbc5 = new MWSL_delta(lbc_apl5, filename);
                    this.addAlgorithm(lbc5);

                    BBTask cassIIlb_apl2 = AplOperator.getInstance().getApl();
                    CASSII_Algorithm cassIIlb2 = new CASSII_Algorithm(cassIIlb_apl2, filename);
                    this.addAlgorithm(cassIIlb2);

                    BBTask dsc_apllb2 = AplOperator.getInstance().getApl();
                    DSC_Algorithm dsclb2 = new DSC_Algorithm(dsc_apllb2, filename);
                    this.addAlgorithm(dsclb2);

                    break;

                default:
                    return null;
            }
            AplOperator.getInstance().setApl(null);


            int size = this.getAlgorithmList().size();
            //Iterator<AbstractMergingAlgorithm> ite = this.getAlgorithmList().iterator();
            //while(ite.hasNext()){

           long optThreshold = 0;
            for (int idx = 0; idx < size; idx++) {
                //格納されているタスク
                AbstractClusteringAlgorithm al = this.getAlgorithmList().get(idx);
                //AbstractMergingAlgorithm al = ite.next();
                //以降は，すべてスーパークラスにて処理は隠蔽される．
                //マージングアルゴリズムの実行
                
                //System.out.println("No."+(idx+1));

                BBTask retApl = null;

                if(idx == 0){
                    retApl = al.process();
                    optThreshold = al.getThreshold();
                }else{
                    al.setThreshold(optThreshold);
                    retApl = al.process();

                }

                aplList.add(retApl);
               // System.out.println(al.getCnt_1()+":"+al.getCnt_2()+":"+ al.getCnt_3());

                //結果の出力
                al.println();
                //System.out.println();
            }
            this.getAlgorithmList().clear();
            return aplList;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;



    }


    public LinkedList<AbstractClusteringAlgorithm> getAlgorithmList() {
        return this.algorithmList;
    }

    public void setAlgorithmList(LinkedList<AbstractClusteringAlgorithm> algorithmList) {
        this.algorithmList = algorithmList;
    }

    public void addAlgorithm(AbstractClusteringAlgorithm algorithm) {
        this.getAlgorithmList().add(algorithm);
    }
}
