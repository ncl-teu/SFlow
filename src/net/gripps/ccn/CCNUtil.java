package net.gripps.ccn;

import net.gripps.cloud.CloudUtil;
import org.apache.commons.math.random.RandomDataImpl;

import java.io.FileInputStream;
import java.util.Properties;

/**
 * Created by kanemih on 2018/11/13.
 */
public class CCNUtil extends CloudUtil {


    public static int TYPE_MOVIE = 1;

    public static int TYPE_DOCUMENT = 2;

    public static int TYPE_DATA = 3;

    public static int NODETYPE_NODE=0;

    public static int NODETYPE_ROUTER=1;

    public static long TIME_MS = 1000;

    /**
     * ヒマな状態
     */
    public static int STATE_NODE_NONE = 0;

    /**
     * Interestパケット送信済みだが，応答がまだ
     */
    public static int STATE_NODE_REQUESTING = 1;

    /**
     * データが到着した
     */
    public static int STATE_NODE_DATA_ARRIVED = 2;

    /**
     * Interestパケットが到着した．
     */
    public static int STATE_NODE_DATA_REQUESTED = 3;

    public static int STATE_NODE_END = 100;


    // CCNルータ数
    public static long ccn_router_num;
    //CCNノード数
    public static long ccn_node_num;
    //要求数@CCNノード
    public static int ccn_node_request_num;

    // Face数の最小値
    public static int ccn_node_face_num_min;
    // Face数の最大値
    public static int ccn_node_face_num_max;
    // コンテンツサイズの最小値(MB)
    public static long ccn_contents_size_min;
    // コンテンツサイズの最大値(MB)
    public static long ccn_contents_size_max;
    // PIT min
    public static int ccn_pit_entry_min;
    // PIT max
    public static int ccn_pit_entry_max;
    // FIB min
    public static int ccn_fib_entry_min;
    // FIB max
    public static int ccn_fib_entry_max;
    // CS min
    public static int ccn_cs_entry_min;
    // CS max
    public static int ccn_cs_entry_max;

    //0: uniform dist. 1: normal dist.
    public static int dist_num_contents;
    // muの値.
    public static double dist_num_contents_mu;

    // ノードがもつコンテンツの最小数
    public static int ccn_node_num_contents_min;
    //ノードがもつコンテンツの最大数
    public static int ccn_node_num_contents_max;
    //個別の呼発生は，指数分布に従うものとする．
    // 1秒あたりの要求発生確率（最小値）
    public static double ccn_request_exp_dist_lambda_min;
    // 1秒あたりの要求発生確率（最大値）
    public static double ccn_request_exp_dist_lambda_max;

    public static CCNUtil own;

    public static int ccn_dist_bw;
    public static double ccn_dist_bw_mu;
    public static long ccn_bw_min;
    public static long ccn_bw_max;

    public static int ccn_node_routernum;

    public static int  ccn_max_pow;

    public static long ccn_maxID;

    public static int ccn_routing_no;
    public static int ccn_routing_allnum;

    public static long MINUS_VAUE = -1;

    public static long ccn_hop_per_delay;

    public static double ccn_actual_data_rate;

    public static int ccn_caching_no;
    public static int ccn_caching_allnum;

    public static int ccn_node_duplicate_interest_num_min;
    public static int ccn_node_duplicate_interest_num_max;

    public static long ccn_interest_ttl;

    public static int ccn_bc_enable;

    public static long ccn_bc_timeout;
    public static int ccn_bc_allnum;

    public static int ccn_churn_enable;
    public static double ccn_join_exp_dist_lambda;

    public static double ccn_leave_exp_dist_lambda;

    public static int ccn_churn_allnum;

    public static int  dist_duplicate_interest;

    public static double dist_duplicate_interest_mu;

    public static int ccn_prefix_delimiter_num_min;

    public static int ccn_prefix_delimiter_num_max;

    public static int ccn_prefix_degree_min;

    public static int ccn_prefix_degree_max;

    public static int ccn_fib_duplicate_num_min;

    public static int ccn_fib_duplicate_num_max;

    public static double dist_fib_duplicate_mu;

    public static  long ccn_chord_pit_threshold;

    public static long ccn_hop_per_delay_min;

    public static long ccn_hop_per_delay_max;

    public static double ccn_hop_per_delay_mu;





    private CCNUtil() {
        CloudUtil.rDataGen = new RandomDataImpl();

    }


    public static CCNUtil getIns() {
        if (CCNUtil.own == null) {
            CCNUtil.own = new CCNUtil();
        } else {

        }
        return CCNUtil.own;
    }

    public void initialize(String fileName) {
        try {
            CCNUtil.prop = new Properties();
            CCNUtil.prop.load(new FileInputStream(fileName));
            // CCNルータ数
            CCNUtil.ccn_router_num = Long.valueOf(CCNUtil.prop.getProperty("ccn_router_num"));
            //CCNノード数
            CCNUtil.ccn_node_num = Long.valueOf(CCNUtil.prop.getProperty("ccn_node_num"));

            CCNUtil.ccn_node_request_num = Integer.valueOf(CCNUtil.prop.getProperty("ccn_node_request_num"));

            // Face数の最小値
            CCNUtil.ccn_node_face_num_min = Integer.valueOf(CCNUtil.prop.getProperty("ccn_node_face_num_min"));
            // Face数の最大値
            CCNUtil.ccn_node_face_num_max = Integer.valueOf(CCNUtil.prop.getProperty("ccn_node_face_num_max"));
            // コンテンツサイズの最小値(MB)
            CCNUtil.ccn_contents_size_min = Long.valueOf(CCNUtil.prop.getProperty("ccn_contents_size_min"));
            // コンテンツサイズの最大値(MB)
            CCNUtil.ccn_contents_size_max = Long.valueOf(CCNUtil.prop.getProperty("ccn_contents_size_max"));
            // PIT min
            CCNUtil.ccn_pit_entry_min = Integer.valueOf(CCNUtil.prop.getProperty("ccn_pit_entry_min"));
            // PIT max
            CCNUtil.ccn_pit_entry_max = Integer.valueOf(CCNUtil.prop.getProperty("ccn_pit_entry_max"));
            // FIB min
            CCNUtil.ccn_fib_entry_min = Integer.valueOf(CCNUtil.prop.getProperty("ccn_fib_entry_min"));
            // FIB max
            CCNUtil.ccn_fib_entry_max = Integer.valueOf(CCNUtil.prop.getProperty("ccn_fib_entry_max"));
            // CS min
            CCNUtil.ccn_cs_entry_min = Integer.valueOf(CCNUtil.prop.getProperty("ccn_cs_entry_min"));
            // CS max
            CCNUtil.ccn_cs_entry_max = Integer.valueOf(CCNUtil.prop.getProperty("ccn_cs_entry_max"));
            //0: uniform dist. 1: normal dist.
            CCNUtil.dist_num_contents = Integer.valueOf(CCNUtil.prop.getProperty("dist_num_contents"));
            // muの値.
            CCNUtil.dist_num_contents_mu = Double.valueOf(CCNUtil.prop.getProperty("dist_num_contents_mu")).doubleValue();
            // ノードがもつコンテンツの最小数
            CCNUtil.ccn_node_num_contents_min = Integer.valueOf(CCNUtil.prop.getProperty("ccn_node_num_contents_min"));
            //ノードがもつコンテンツの最大数
            CCNUtil.ccn_node_num_contents_max = Integer.valueOf(CCNUtil.prop.getProperty("ccn_node_num_contents_max"));
            //個別の呼発生は，指数分布に従うものとする．
            // 1秒あたりの要求発生確率（最小値）
            CCNUtil.ccn_request_exp_dist_lambda_min = Double.valueOf(CCNUtil.prop.getProperty("ccn_request_exp_dist_lambda_min")).doubleValue();
            // 1秒あたりの要求発生確率（最大値）
            CCNUtil.ccn_request_exp_dist_lambda_max = Double.valueOf(CCNUtil.prop.getProperty("ccn_request_exp_dist_lambda_max")).doubleValue();
            CCNUtil.ccn_dist_bw = Integer.valueOf(CCNUtil.prop.getProperty("ccn_dist_bw"));
            CCNUtil.ccn_dist_bw_mu = Double.valueOf(CCNUtil.prop.getProperty("ccn_dist_bw_mu")).doubleValue();

            CCNUtil.ccn_bw_min = Long.valueOf(CCNUtil.prop.getProperty("ccn_bw_min"));
            CCNUtil.ccn_bw_max = Long.valueOf(CCNUtil.prop.getProperty("ccn_bw_max"));

            CCNUtil.ccn_max_pow = Integer.valueOf(CCNUtil.prop.getProperty("ccn_max_pow"));
            CCNUtil.ccn_maxID = (long)Math.pow(2, CCNUtil.ccn_max_pow)-1;

            CCNUtil.ccn_node_routernum = Integer.valueOf(CCNUtil.prop.getProperty("ccn_node_routernum"));
            CCNUtil.ccn_routing_no = Integer.valueOf(CCNUtil.prop.getProperty("ccn_routing_no"));
            CCNUtil.ccn_hop_per_delay = Long.valueOf(CCNUtil.prop.getProperty("ccn_hop_per_delay"));

            CCNUtil.ccn_hop_per_delay_min = Long.valueOf(CCNUtil.prop.getProperty("ccn_hop_per_delay_min"));
            CCNUtil.ccn_hop_per_delay_max = Long.valueOf(CCNUtil.prop.getProperty("ccn_hop_per_delay_max"));
            CCNUtil.ccn_hop_per_delay_mu = Double.valueOf(CCNUtil.prop.getProperty("ccn_hop_per_delay_mu")).doubleValue();


            CCNUtil.ccn_actual_data_rate = Double.valueOf(CCNUtil.prop.getProperty("ccn_actual_data_rate")).doubleValue();

            CCNUtil.ccn_caching_no =  Integer.valueOf(CCNUtil.prop.getProperty("ccn_caching_no"));

            CCNUtil.ccn_routing_allnum =  Integer.valueOf(CCNUtil.prop.getProperty("ccn_routing_allnum"));

            CCNUtil.ccn_caching_allnum =  Integer.valueOf(CCNUtil.prop.getProperty("ccn_caching_allnum"));
            CCNUtil.ccn_node_duplicate_interest_num_min =  Integer.valueOf(CCNUtil.prop.getProperty("ccn_node_duplicate_interest_num_min"));
            CCNUtil.ccn_node_duplicate_interest_num_max =  Integer.valueOf(CCNUtil.prop.getProperty("ccn_node_duplicate_interest_num_max"));

            CCNUtil.ccn_interest_ttl = Long.valueOf(CCNUtil.prop.getProperty("ccn_interest_ttl"));
            CCNUtil.ccn_bc_enable = Integer.valueOf(CCNUtil.prop.getProperty("ccn_bc_enable"));
            CCNUtil.ccn_bc_timeout = Long.valueOf(CCNUtil.prop.getProperty("ccn_bc_timeout"));
            CCNUtil.ccn_bc_allnum =  Integer.valueOf(CCNUtil.prop.getProperty("ccn_bc_allnum"));
            CCNUtil.ccn_churn_enable =   Integer.valueOf(CCNUtil.prop.getProperty("ccn_churn_enable"));
            CCNUtil.ccn_join_exp_dist_lambda = Double.valueOf(CCNUtil.prop.getProperty("ccn_join_exp_dist_lambda")).doubleValue();
            CCNUtil.ccn_leave_exp_dist_lambda = Double.valueOf(CCNUtil.prop.getProperty("ccn_leave_exp_dist_lambda")).doubleValue();
            CCNUtil.ccn_churn_allnum =  Integer.valueOf(CCNUtil.prop.getProperty("ccn_churn_allnum"));

            CCNUtil.dist_duplicate_interest = Integer.valueOf(CCNUtil.prop.getProperty("dist_duplicate_interest"));
            CCNUtil.dist_duplicate_interest_mu =  Double.valueOf(CCNUtil.prop.getProperty("dist_duplicate_interest_mu")).doubleValue();

            CCNUtil.ccn_prefix_delimiter_num_min = Integer.valueOf(CCNUtil.prop.getProperty("ccn_prefix_delimiter_num_min"));
             CCNUtil.ccn_prefix_delimiter_num_max = Integer.valueOf(CCNUtil.prop.getProperty("ccn_prefix_delimiter_num_max"));

             CCNUtil.ccn_prefix_degree_min = Integer.valueOf(CCNUtil.prop.getProperty("ccn_prefix_delimiter_num_min"));
            CCNUtil.ccn_prefix_degree_max = Integer.valueOf(CCNUtil.prop.getProperty("ccn_prefix_delimiter_num_max"));


            CCNUtil.ccn_fib_duplicate_num_min = Integer.valueOf(CCNUtil.prop.getProperty("ccn_fib_duplicate_num_min"));
            CCNUtil.ccn_fib_duplicate_num_max = Integer.valueOf(CCNUtil.prop.getProperty("ccn_fib_duplicate_num_max"));

            CCNUtil.dist_fib_duplicate_mu = Double.valueOf(CCNUtil.prop.getProperty("dist_fib_duplicate_mu")).doubleValue();

            CCNUtil.ccn_chord_pit_threshold = Long.valueOf(CCNUtil.prop.getProperty("ccn_chord_pit_threshold"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
