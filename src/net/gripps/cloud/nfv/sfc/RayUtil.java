package net.gripps.cloud.nfv.sfc;

import net.gripps.cloud.nfv.NFVUtil;

import java.io.FileInputStream;
import java.util.Properties;

public class RayUtil {

    private Properties prop;

    public static long size_per_pixel;
    public static int ray_quality;

    public static long ray_pixel_per_task;

    public static int ray_thread_min;

    public static int ray_thread_max;


    public static double ray_thread_mu;

    public static double  ray_intersection_workload_min;

    public static double ray_intersection_workload_max;

    public static double ray_lighting_workload_min;

    public static double ray_lighting_workload_max;

    public static double ray_lighting_probability_min;
    public static double ray_lighting_probability_max;


    public static double ray_reflection_workload_min;

    public static double ray_reflection_workload_max;

    public static double ray_reflection_probability_min;
    public static double ray_reflection_probability_max;
    public static double ray_glass_workload_min;

    public static double ray_glass_workload_max;

    public static double ray_glass_probability_min;
    public static double ray_glass_probability_max;




    private static RayUtil own;

    public static RayUtil getIns(){
        if(RayUtil.own == null){
            RayUtil.own = new RayUtil();
        }
        return RayUtil.own;
    }

    private RayUtil(){

    }

    public void initialize(String propName){
        try{
            this.prop = new Properties();
            this.prop.load(new FileInputStream(propName));
            RayUtil.ray_quality = Integer.valueOf(this.prop.getProperty("ray_quality")).intValue();

            RayUtil.size_per_pixel = Long.valueOf(this.prop.getProperty("size_per_pixel")).longValue();
            RayUtil.ray_pixel_per_task = Long.valueOf(this.prop.getProperty("ray_pixel_per_task")).longValue();
            RayUtil.ray_thread_min = Integer.valueOf(this.prop.getProperty("ray_thread_min")).intValue();
            RayUtil.ray_thread_max = Integer.valueOf(this.prop.getProperty("ray_thread_max")).intValue();
            RayUtil.ray_thread_mu = Double.valueOf(this.prop.getProperty("ray_thread_mu")).doubleValue();
            RayUtil.ray_intersection_workload_min = Double.valueOf(this.prop.getProperty("ray_intersection_workload_min")).doubleValue();
            RayUtil.ray_intersection_workload_max = Double.valueOf(this.prop.getProperty("ray_intersection_workload_max")).doubleValue();
            RayUtil.ray_lighting_workload_min = Double.valueOf(this.prop.getProperty("ray_lighting_workload_min")).doubleValue();
            RayUtil.ray_lighting_workload_max = Double.valueOf(this.prop.getProperty("ray_lighting_workload_max")).doubleValue();
            RayUtil.ray_lighting_probability_min = Double.valueOf(this.prop.getProperty("ray_lighting_probability_min")).doubleValue();
            RayUtil.ray_lighting_probability_max = Double.valueOf(this.prop.getProperty("ray_lighting_probability_max")).doubleValue();

            RayUtil.ray_reflection_workload_min = Double.valueOf(this.prop.getProperty("ray_reflection_workload_min")).doubleValue();
            RayUtil.ray_reflection_workload_max = Double.valueOf(this.prop.getProperty("ray_reflection_workload_max")).doubleValue();
            RayUtil.ray_reflection_probability_min = Double.valueOf(this.prop.getProperty("ray_reflection_probability_min")).doubleValue();
            RayUtil.ray_reflection_probability_max = Double.valueOf(this.prop.getProperty("ray_reflection_probability_max")).doubleValue();


            RayUtil.ray_glass_workload_min = Double.valueOf(this.prop.getProperty("ray_glass_workload_min")).doubleValue();
            RayUtil.ray_glass_workload_max = Double.valueOf(this.prop.getProperty("ray_glass_workload_max")).doubleValue();
            RayUtil.ray_glass_probability_min = Double.valueOf(this.prop.getProperty("ray_glass_probability_min")).doubleValue();
            RayUtil.ray_glass_probability_max = Double.valueOf(this.prop.getProperty("ray_glass_probability_max")).doubleValue();

        }catch(Exception e){
            e.printStackTrace();
        }

    }
}
