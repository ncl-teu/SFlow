package net.gripps.clustering.algorithms.mwsl_delta;

/**
 * Author: H. Kanemitsu
 * Date: 2009/12/11
 */
public class TaskGrain {
    private Long id;

    private double preGrain;

    private double sucGrain;

    /**
     * 各粒度は，タスク間の関係で決まるので，固定値である．
     * これに，マシン情報を乗算することにより，最終的な粒度が決まる．
     * 
     * @param id
     * @param preGrain　先行タスクに対する粒度
     * @param sucGrain　後続タスクに対する粒度
     */
    public TaskGrain(Long id, double preGrain, double sucGrain) {
        this.id = id;
        this.preGrain = preGrain;
        this.sucGrain = sucGrain;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public double getPreGrain() {
        return preGrain;
    }

    public void setPreGrain(double preGrain) {
        this.preGrain = preGrain;
    }

    public double getSucGrain() {
        return sucGrain;
    }

    public void setSucGrain(double sucGrain) {
        this.sucGrain = sucGrain;
    }

    public double getGmax(){
       /* double tmpMax = Math.max(preGrain, sucGrain);
        BigDecimal tmpMax2 = new BigDecimal(String.valueOf(tmpMax));
        double retGMax = tmpMax2.setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
        */

        return Math.max(this.preGrain, this.sucGrain);
    }
}


