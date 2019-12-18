package org.ncl.workflow.util;

import net.gripps.cloud.core.VM;
import net.gripps.cloud.nfv.NFVEnvironment;
import org.apache.commons.math.random.RandomDataImpl;

import java.io.FileInputStream;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Properties;

public class NCLWUtil {

    public static RandomDataImpl rDataGen;
    /**
     * property file
     */
    public static Properties prop;

    /**
     * Singleton Instance
     */
    protected static NCLWUtil own;

    /**
     * # of thread in the threadpool for sending/receiving data.
     */
    public  static int rmgr_send_thread_num;

    public  static int rmgr_recv_thread_num;

    public static long total_cnt_cpuusage;

    public static int port;

    public static String delegator_ip;

    public static String ftp_server_ip;

    public static String ftp_server_id;

    public static String ftp_server_pass;

    public static String ftp_server_homedirName;

    public static String docker_repository_home;

    public static String docker_repository_ip;

    public static String docker_repository_userid;

    public static String docker_repository_password;

    public static String docker_localdir;

    public static int sched_algorithm;

    public static String input_file_transfer_protocol;

    public static int nfd_strategy;



    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();

    /**
     * NFD用のポート番号
     */
    public  static int NFD_PORT = 6363;

    public static String NCLW_PREFIX="/nclw/";


    /**
     * Constructor
     */
    private  NCLWUtil() {
        NCLWUtil.rDataGen = new RandomDataImpl();

    }

    public static boolean isLinux(){
        return OS_NAME.startsWith("linux");
    }

    public static boolean isWindows(){
        return OS_NAME.startsWith("windows");
    }

    public static boolean isMac(){
        return OS_NAME.startsWith("mac");
    }

    /**
     * Factory Method
     * @return
     */
    public static NCLWUtil getIns() {
        if (NCLWUtil.own == null) {
            NCLWUtil.own = new NCLWUtil();
        }

        return NCLWUtil.own;
    }

    /**
     * Initialization Procedure
     * @param propName
     */
    public void initialize(String propName){
        try{
            //Read
            NCLWUtil.prop = new Properties();
            NCLWUtil.prop.load(new FileInputStream(propName));
            NCLWUtil.rmgr_send_thread_num = Integer.valueOf( NCLWUtil.prop.getProperty("rmgr_send_thread_num"));
            NCLWUtil.rmgr_recv_thread_num = Integer.valueOf( NCLWUtil.prop.getProperty("rmgr_recv_thread_num"));
            NCLWUtil.total_cnt_cpuusage = Long.valueOf(NCLWUtil.prop.getProperty("total_cnt_cpuusage"));
            NCLWUtil.port =Integer.valueOf( NCLWUtil.prop.getProperty("port_number"));
            NCLWUtil.delegator_ip =  NCLWUtil.prop.getProperty("delegator_ip");
            NCLWUtil.ftp_server_ip = NCLWUtil.prop.getProperty("ftp_server_ip");
            NCLWUtil.ftp_server_id = NCLWUtil.prop.getProperty("ftp_server_id");
            NCLWUtil.ftp_server_pass = NCLWUtil.prop.getProperty("ftp_server_pass");
            NCLWUtil.ftp_server_homedirName = NCLWUtil.prop.getProperty("ftp_server_homedirName");
            NCLWUtil.docker_repository_home = NCLWUtil.prop.getProperty("docker_repository_home");
            NCLWUtil.docker_repository_ip = NCLWUtil.prop.getProperty("docker_repository_ip");

            NCLWUtil.docker_repository_userid =  NCLWUtil.prop.getProperty("docker_repository_userid");
            NCLWUtil.docker_repository_password = NCLWUtil.prop.getProperty("docker_repository_password");
            NCLWUtil.docker_localdir = NCLWUtil.prop.getProperty("docker_localdir");
            NCLWUtil.sched_algorithm = Integer.valueOf(NCLWUtil.prop.getProperty("sched_algorithm"));
            NCLWUtil.input_file_transfer_protocol = NCLWUtil.prop.getProperty("input_file_transfer_protocol");
            NCLWUtil.nfd_strategy = Integer.valueOf(NCLWUtil.prop.getProperty("nfd.strategy"));




        }catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     *
     * @param x
     * @return
     */
    public static double log2(double x) {

        if (Double.isNaN(x) || x < 0.0) return Double.NaN;
        if (x == Double.POSITIVE_INFINITY) return Double.POSITIVE_INFINITY;
        if (x == 0.0) return Double.NEGATIVE_INFINITY;
        int k = Math.getExponent(x);
        if (k < Double.MIN_EXPONENT) {

            k = Math.getExponent(x * 0x1.0p52) - 52;
        }
        if (k < 0) {
            k++;
        }
        double s = Math.scalb(x, -k);
        final double LOG2_E = 1.4426950408889634;
        return k + LOG2_E * Math.log(s);
    }


    public static double genDouble(double min, double max) {
        return NCLWUtil.getRoundedValue(min + (Math.random() * (max - min + 1)));

    }

    public static long genLong(long min, long max) {

        return min + (long) (Math.random() * (max - min + 1));

    }

    public static int genInt(int min, int max) {

        return min + (int) (Math.random() * (max - min + 1));

    }

    public static double getRoundedValue(double value1) {
        //  try{
        BigDecimal value2 = new BigDecimal(String.valueOf(value1));
        double retValue = value2.setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
        return retValue;

    }


    /**
     *
     *
     * @param min
     * @param max
     * @param dist
     * @param mu
     * @return
     */
    public static int genInt2(int min, int max, int dist, double mu) {
        if (min == max) {
            return min;
        }
        if (dist == 0) {
            //テストです．
            return min + (int) (Math.random() * (max - min + 1));

        } else {
            //豁｣隕丞・蟶・
            double meanValue2 = min + (max - min) * mu;
            double sig = Math.max((meanValue2 - min), (max - meanValue2)) / 3;
            double ran2 = NCLWUtil.rDataGen.nextGaussian(meanValue2, sig);


            if (ran2 < min) {
                ran2 = min;
            }

            if (ran2 > max) {
                ran2 = max;
            }

            return (int) ran2;
        }

    }

    public static VM findVM(NFVEnvironment env, String vcpuID){
        Iterator<VM> vmIte = env.getGlobal_vmMap().values().iterator();
        VM retVM = null;
        while(vmIte.hasNext()){
            VM vm = vmIte.next();
            if(vm.getvCPUMap().containsKey(vcpuID)){
                retVM = vm;
                break;
            }
        }
        return retVM;
        /*
        StringTokenizer st = new StringTokenizer(vcpuID, CloudUtil.DELIMITER);
        long val = -1;
        String str = null;
        StringBuffer buf = new StringBuffer();
        for(int i=0;i<3;i++){
            str = st.nextToken();
            buf.append(str);
            if(i <2){
                buf.append(CloudUtil.DELIMITER);

            }
        }
        VM vm = env.getGlobal_vmMap().get(buf.toString());

        return vm;

         */
    }

    /**
     * Doubl
     *
     * @param min
     * @param max
     * @param dist
     * @param mu
     * @return
     */
    public static double genDouble2(double min, double max, int dist, double mu) {
        if (min == max) {
            return min;
        }
        if (dist == 0) {
            //荳讒伜・蟶・
            return min + (Math.random() * (max - min + 1));

        } else {
            //豁｣隕丞・蟶・
            double meanValue2 = min + (max - min) * mu;
            double sig = Math.max((meanValue2 - min), (max - meanValue2)) / 3;
            double ran2 = NCLWUtil.getRoundedValue(NCLWUtil.rDataGen.nextGaussian(meanValue2, sig));


            if (ran2 < min) {
                ran2 = min;
            }

            if (ran2 > max) {
                ran2 = max;
            }

            return ran2;
        }

    }


    /**
     * Long蝙九・荳讒倥・豁｣隕丞・蟶・・蜉幃未謨ｰ
     *
     * @param min
     * @param max
     * @param dist
     * @param mu
     * @return
     */
    public static long genLong2(long min, long max, int dist, double mu) {
        if (min == max) {
            return min;
        }
        if (dist == 0) {
            //荳讒伜・蟶・
            return min + (long) (Math.random() * (max - min + 1));

        } else {
            //豁｣隕丞・蟶・
            double meanValue2 = min + (max - min) * mu;
            double sig = Math.max((meanValue2 - min), (max - meanValue2)) / 3;
            double ran2 = NCLWUtil.rDataGen.nextGaussian(meanValue2, sig);


            if (ran2 < min) {
                ran2 = (double) min;
            }

            if (ran2 > max) {
                ran2 = (double) max;
            }

            return (long) ran2;
        }

    }
}
