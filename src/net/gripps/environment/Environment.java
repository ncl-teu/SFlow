package net.gripps.environment;

import net.gripps.clustering.common.Constants;
import net.gripps.clustering.common.aplmodel.AplOperator;
import net.gripps.clustering.tool.Calc;
import org.apache.commons.math.random.RandomDataImpl;

import java.io.*;
import java.util.*;

/**
 * Author: H. Kanemitsu
 * マシン環境を取り扱うクラスです．マシン間のデータ転送速度を定義<br>
 * します．<br>
 * Date: 2008/10/06
 */
public class Environment implements Serializable, Cloneable {

    /**
     * シングルトンオブジェクトです．
     *
     */
   // private static Environment singleton;

     /**
     *
     */
    protected int size;

    /**
     *
     */
    protected Hashtable<Long, CPU> cpuList;



    /**
     *
     */
    protected Hashtable<Long, Machine> machineList;



    /**
     *
     */
    protected long[][] linkMatrix;


    /**
     *
     */
    protected long speedMax;

    /**
     *
     */
    protected long speedMin;

    /**
     *
     */
    protected long linkMax;

    /**
     *
     */
    protected long linkMin;

    protected boolean isMultiCore;

    protected int core_min;
    protected int core_max;

    protected int distType;

    protected double cpu_mu;

    protected double cpu_link_mu;

    protected int virtualmode;

    protected long aveLink;

    protected long setupTime;

    private RandomDataImpl rData = new RandomDataImpl();

    protected long minLinkSpeed;

    protected long maxLinkSpeed;

    protected long maxSpeed;

    protected long minSpeed;

    protected long averageSpeed;

    protected long averageBW;






    /**
     * 自オブジェクト返し
     * @return
     */
   /* public static Environment getInstance(String fileName, int size){
        if(Environment.singleton == null){

            Environment.singleton = new Environment(fileName, size);
        }

        return Environment.singleton;
    } */

    /**
     * 自オブジェクト返し with チェック機能なし
     * @return
     */
   /* public static Environment getInstance(){
        return Environment.singleton;
    } */

    /**
     *
     * @param min
     * @param max
     * @return
     */
    public long generateLongValue(long min, long max) {
        return min + (long) (Math.random() * (max - min + 1));
    }

    /**
     *
     */
    public Environment(){

    }

    public long generateCPUSpeed(long min, long max){
           if(this.distType == 1){
               return min + (long) (Math.random() * (max - min + 1));

           }else{

               double meanValue2 = min + (max-min)* this.cpu_mu;
               double ave = (max + min)/2;

               double sig = (double)(Math.max((meanValue2-min), (max-meanValue2)))/3;
               double ran2 = this.rData.nextGaussian(meanValue2,sig);
               if(ran2 < min){
                   ran2 =(double) min;
               }

               if(ran2 > max){
                   ran2 = (double)max;
               }

               return (long) ran2;

           }

       }

    public void initialize(){

    }

    public long generateLink(long min, long max){
              if(this.distType == 1){
                  return min + (long) (Math.random() * (max - min + 1));

              }else{

                  double meanValue2 = min + (max-min)* this.cpu_link_mu;
                  double ave = (max + min)/2;

                  double sig = (double)(Math.max((meanValue2-min), (max-meanValue2)))/3;
                  double ran2 = this.rData.nextGaussian(meanValue2,sig);
                  if(ran2 < min){
                      ran2 =(double) min;
                  }

                  if(ran2 > max){
                      ran2 = (double)max;
                  }

                  return (long) ran2;

              }

          }


    /**
     *
     * @param fileName
     */
    public  Environment(String fileName, int clusterSize){
        try{
            Properties prop = new Properties();
            prop.load(new FileInputStream(fileName));
            //マシンのサイズの格納
            //this.size = Integer.valueOf(prop.getProperty("cpu.num")).intValue();
            this.size = clusterSize;

            //マシンの処理速度
            this.speedMax = Long.valueOf(prop.getProperty("cpu.speed.max")).longValue();
            this.speedMin = Long.valueOf(prop.getProperty("cpu.speed.min")).longValue();

            //マシン間の転送速度
            this.linkMax = Long.valueOf(prop.getProperty("cpu.link.max")).longValue();
            this.linkMin = Long.valueOf(prop.getProperty("cpu.link.min")).longValue();

            this.distType =  Integer.valueOf(prop.getProperty("cpu.distribution")).intValue();
            this.cpu_mu = Double.valueOf(prop.getProperty("cpu.mu")).doubleValue();
            this.cpu_link_mu = Double.valueOf(prop.getProperty("cpu.link.mu")).doubleValue();
            this.virtualmode = Integer.valueOf(prop.getProperty("mwsl.virtual.condition")).intValue();
            this.setupTime = Long.valueOf(prop.getProperty("env.setuptime")).longValue();

            this.minLinkSpeed = 99999999;
            this.maxLinkSpeed = 0;

            this.maxSpeed = 0;

            this.minSpeed = 99999999;

            this.averageBW = 0;
            this.averageSpeed = 0;


            //マルチコアがOnかどうか
            if(Integer.valueOf(prop.getProperty("cpu.multicore")).intValue() > 0){
                this.isMultiCore = true;
                this.core_min = Integer.valueOf(prop.getProperty("cpu.core.min")).intValue();
                this.core_max = Integer.valueOf(prop.getProperty("cpu.core.max")).intValue();

            }else{
                this.isMultiCore = false;
            }

            if(this.isMultiCore){
                //コア行列のサイズ定義
                this.linkMatrix = new long[this.size][this.size];
                this.cpuList = new Hashtable<Long, CPU>();
                this.machineList = new Hashtable<Long, Machine>();


                //初期化処理（クラスタサイズ=コア数）に関するループ処理
                for(int i=0;i<size;i++){
                    //マシン速度の初期化処理
                    long speedValue = 0;
                    if(this.distType == 1){
                        speedValue = AplOperator.getInstance().generateLongValue(this.speedMin,  this.speedMax);
                    }else{
                        speedValue = this.generateCPUSpeed(this.speedMin, this.speedMax);
                    }
                    Long cpuID = Long.valueOf(String.valueOf(i));
                    CPU CPU = new CPU(cpuID, speedValue, new Vector<Long>(), new Vector<Long>());
                    CPU.setVirtual(false);
                    if(this.maxSpeed <= CPU.getSpeed()){
                        this.maxSpeed = CPU.getSpeed();
                    }

                    if(this.minSpeed >= CPU.getSpeed()){
                        this.minSpeed = CPU.getSpeed();
                    }


                    //当該CPU（コア）が属するマシンIDを生成する．とりあえず，コア数の半分だけの個数を用意する．
                    //long machineID = (long)Math.random()*size/2;
                    //マシンにコアを追加するためのループ．
                    //もしマシンのコア数が上限に達していれば，次のコアへと追加する．
                    boolean isCoreAdded = false;
                    int tmp_idx = 0;
                    while(!isCoreAdded){
                        long machineID = (i+tmp_idx)/this.core_max;
                        //すでにマシンが生成済みであれば，追加する．
                        if(this.machineList.containsKey(machineID)){
                           //もし目的マシンにコアを追加できれば問題なし
                           if(this.machineList.get(machineID).addCore(CPU)){
                               //CPUリストに登録
                               this.cpuList.put(cpuID, CPU);
                               CPU.setMachineID(machineID);
                               break;
                           }else{
                               //目的マシンにコアを追加できない場合は，次の候補マシンを探す．
                               tmp_idx++;
                               continue;
                           }

                        }else{
                           //マシンリストにマシンがない場合の処理
                            //新規にマシンを追加させる．
                            //Machine m = new Machine(machineID, new TreeMap<Long, CPU>(),this.core_max);
                            Machine m = new Machine(machineID, new TreeMap<Long, CPU>(),this.core_min + (int) (Math.random() *
                                    (this.core_max - this.core_min + 1)));
                            //帯域幅の設定
                            long actualBW = 0;
                            if(this.distType==1){
                                actualBW = AplOperator.getInstance().generateLongValue(this.linkMin, this.linkMax);
                                m.setBw(actualBW);

                            }else{
                                actualBW = this.generateLink(this.linkMin, this.linkMax);
                                m.setBw(actualBW);

                            }
                            m.addCore(CPU);
                            CPU.setBw(actualBW);
                            //CPUリストに登録
                            this.cpuList.put(cpuID, CPU);
                            //マシンリストに追加させる．
                            this.machineList.put(machineID, m);
                            break;
                       }
                    }
                }

                for(int i=0;i<size;i++) {
                    CPU cpu_i = this.cpuList.get(new Long(i));
                    //コア間の転送速度の定義
                    for (int j = 0; j < size; j++) {
                        //CPUの属する先マシンのIDを見て，互いに一致すれば無視する．
                        //CPU_jを見る．
                        CPU cpu_j = this.cpuList.get(new Long(j));
                        if (cpu_j.getMachineID() == cpu_i.getMachineID()) {

                            //もし互いにマシンが同じであれば，無限大の値の帯域幅を設定する．
                            this.linkMatrix[i][j] = Constants.INFINITY;
                        } else {
                            //もし異なるマシン同士であれば，通常通り帯域幅を設定する．
                            Machine m_i = this.machineList.get(cpu_i.getMachineID());
                            Machine m_j = this.machineList.get(cpu_j.getMachineID());
                            long minL = Math.min(m_i.getBw(), m_j.getBw());
                            this.linkMatrix[i][j] = minL;


                            //this.linkMatrix[i][j] = AplOperator.getInstance().generateLongValue(this.linkMin, this.linkMax);

                        }


                    }
                }
            }else{
                //マシン行列のサイズ定義
                this.linkMatrix = new long[this.size][this.size];
                this.cpuList = new Hashtable<Long, CPU>();


                //初期化処理
                for(int i=0;i<size;i++){
                    //マシン速度の初期化処理
                    long speedValue = AplOperator.getInstance().generateLongValue(this.speedMin,  this.speedMax);
                    Long machineID = Long.valueOf(String.valueOf(i));
                    CPU CPU = new CPU(machineID, speedValue, new Vector<Long>(), new Vector<Long>());
                    //マシンオブジェクトをリストへ格納する．
                    //this.cpuList.add(CPU);
                    this.cpuList.put(machineID, CPU);
                    if(this.maxSpeed <= CPU.getSpeed()){
                        this.maxSpeed = CPU.getSpeed();
                    }

                    if(this.minSpeed >= CPU.getSpeed()){
                        this.minSpeed = CPU.getSpeed();
                    }

                    //マシン間の転送速度の定義
                    for(int j=0; j<size;j++){
                        this.linkMatrix[i][j] = AplOperator.getInstance().generateLongValue(this.linkMin,  this.linkMax);

                    }
                }
                //マシンへ，入力リンク速度，出力リンク速度をセットする．
                Iterator<CPU> mIte = this.cpuList.values().iterator();
                while(mIte.hasNext()){
                    CPU m = mIte.next();
                    long id = m.getCpuID().longValue();
                    //リンクマトリックスのループ

                }
            }

            long[][] linkMX = this.getLinkMatrix();
            long len = linkMX[0].length;
            int cnt  = 0;
            long totalBW = 0;
            for(int i=0;i<len;i++){
                for(int j=i+1;j<len;j++){
                    if(linkMX[i][j] == -1) {
                        continue;
                    }else if(!this.cpuList.containsKey(new Long(i))|| (!this.cpuList.containsKey(new Long(j)))){
                   // }else if(!this.cpuTable.contains(i) || (!this.cpuTable.contains(j))){
                        continue;
                    }else{
                         totalBW+=linkMX[i][j];
                        if(this.minLinkSpeed >= linkMX[i][j]){
                            this.minLinkSpeed = linkMX[i][j];
                        }

                        if(this.maxLinkSpeed <= linkMX[i][j]){
                            this.maxLinkSpeed = linkMX[i][j];
                        }
                        cnt++;
                    }
                }
            }
            this.aveLink = totalBW/cnt;


        }catch(Exception e){
            e.printStackTrace();

        }


    }

    public void setMinSpeed(long minSpeed) {
        this.minSpeed = minSpeed;
    }

    public long getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(long maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public long getMinLinkSpeed() {
        return minLinkSpeed;
    }

    public void setMinLinkSpeed(long minLinkSpeed) {
        this.minLinkSpeed = minLinkSpeed;
    }

    public long getMaxLinkSpeed() {
        return maxLinkSpeed;
    }

    public void setMaxLinkSpeed(long maxLinkSpeed) {
        this.maxLinkSpeed = maxLinkSpeed;
    }

    /**
     * 
     * @param mID
     * @return
     */
    public long[] getLinkRow(int mID){
        return this.linkMatrix[mID];

    }

    public long[] getLinkColumn(int mID){
        /*int len =this.cpuList.size();
        long[] retLinks = new long[this.cpuList.size()];
        List Arrays.asList(retLinks);
        
        for (int i=0;i<)
        */
        return null;
    }


    public long getSetupTime() {
        return setupTime;
    }

    public void setSetupTime(long setupTime) {
        this.setupTime = setupTime;
    }

    public CPU findCPU(Long id){
        return this.cpuList.get(id);
    }

    /**
     *
     * @param cpu
     * @return
     */
    public long getBWFromCPU(CPU cpu){
        if(cpu.isVirtual()){
            return this.linkMax;
        }else{
            Long mID = cpu.getMachineID();
            Machine m = this.getMachineList().get(mID);
            return m.getBw();
        }

    }


    /**
     *
     * @return
     */
    public boolean isMachineHetero(){
            //均一マシン環境かどうかを判定する．
            if((this.speedMin == this.speedMax) &&(this.linkMin == this.linkMax)){
                return false;
            }else{
                //ヘテロマシン環境の場合は，マッピング処理が必要．
                return true;
            }
    }

    /**
     * マシンリストから，マシン速度が最小となるマシンのインデックス値を返す．
     * @return
     */
    public int getMinSpeedMachineIndex(){
       // Iterator<CPU> ite = this.cpuList.iterator();
        Iterator<CPU> ite = this.cpuList.values().iterator();
        int idx = 0;
        long tmpValue = 999999;
        long retValue = 0;
        int retIDX = 0;

        //各マシンに対するループ処理
        while(ite.hasNext()){
            CPU m = ite.next();
            if(m.getSpeed() < tmpValue){
                retValue = m.getSpeed();
                retIDX = idx;

            }
            idx++;
        }

        return retIDX;

    }

    /**
     * 最小のマシン速度を返します．
     * @return
     */
    public long getMinSpeed(){
        Iterator<CPU> ite = this.cpuList.values().iterator();
        long tmpValue = 999999;
        long retValue = 0;

        //各マシンに対するループ処理
        while(ite.hasNext()){
            CPU m = ite.next();
            if(m.getSpeed() < tmpValue){
                retValue = m.getSpeed();

            }
            tmpValue = m.getSpeed();
        }

        return retValue;

    }

    /**
     * 指定CPUの最小帯域幅を取得します．
     * @param cpuID
     * @return
     */
    public long getMinLink(Long cpuID){
        int len = this.size;
        int id = cpuID.intValue();
        long retLink = 10000000;

        //出力側の最小値算出
        for(int i=0;i<len;i++){
            if(i == id){
                continue;
            }else{
                long value = this.linkMatrix[id][i];
                if(value < 0)continue;
                if(retLink >= value){
                    retLink = value;
                }
            }
        }
        for(int j=0;j<len;j++){
            if(j==id){
                continue;
            }else{
                long value = this.linkMatrix[j][id];
                if(value < 0)continue;
                if(retLink >= value){
                    retLink = value;
                }
            }
        }
        return retLink;
    }

     /**
     * 最小のマシン速度を返します．
     * @return
     */
    public long getMinLink(){
        int len = this.size;
        long tmpValue = 9999999;
        long retValue = 0;

        for(int i=0;i<len;i++){
            for(int j=0;j<len;j++){
                long value = this.linkMatrix[i][j];
                if(value < tmpValue){
                    retValue = value;
                }
                tmpValue = value;
            }
        }

        return retValue;
    }


    /**
     *
     * @return
     */
    public Hashtable<Long, CPU> getCpuList() {
        return cpuList;
    }

    /**
     *
     * @param cpuList
     */
    public void setCpuList(Hashtable<Long, CPU> cpuList) {
        this.cpuList = cpuList;
    }

    /**
     *
     * @return
     */
    public int getSize() {
        return size;
    }

    /**
     *
     * @param size
     */
    public void setSize(int size) {
        this.size = size;
    }

    /**
     * 指定行，列のところにリンク速度をセットします．
     * @param value
     * @param row
     * @param column
     */
    public void setLink(long value, int row, int column){
        this.linkMatrix[row][column] = value;
    }

    /**
     * 指定行，列に該当するリンク速度を返します．
     * @param row
     * @param column
     * @return
     */
    public long getNWLink(long row, long column){
        try{
            long fromBW = 0;
            long toBW = 0;
            if(row == -1){
                if(this.virtualmode == 0){
                    fromBW = this.linkMax;
                }else{
                    fromBW = this.aveLink;
                }

            }else{
                CPU rowCPU = this.findCPU(new Long(row));
                fromBW = this.getBWFromCPU(rowCPU);
            }

            if(column == -1){
                if(this.virtualmode == 0){
                    toBW = this.linkMax;
                }else{
                    toBW = this.aveLink;
                }

            }else{
                CPU columnCPU = this.findCPU(new Long(column));
                toBW = this.getBWFromCPU(columnCPU);
            }


           // return this.linkMatrix[row][column];
            return Math.min(fromBW, toBW);

        }catch(ArrayIndexOutOfBoundsException e){
            //System.out.println("来たよ");
            e.printStackTrace();
            return 0;
        }

    }

    public long getLink(int row, int column){
            try{


              return this.linkMatrix[row][column];

            }catch(ArrayIndexOutOfBoundsException e){
                //System.out.println("来たよ");
                e.printStackTrace();
                return 0;
            }

        }


    public long getMaxInLink(int mID){
        int len = this.linkMatrix[mID].length;

        long ret = 0;
      //  long[] columnData = new long[]
        for(int i=0;i<len;i++){
            long data = this.linkMatrix[i][mID];
            if(data >= ret){
                ret = data;
            }
        }
        return ret;
    }

    public long getMaxOutLink(int mID){
         int len = this.linkMatrix[mID].length;

        long ret = 0;
      //  long[] columnData = new long[]
        for(int i=0;i<len;i++){
            long data = this.linkMatrix[mID][i];
            if(data >= ret){
                ret = data;
            }
        }
        return ret;
    }

    public double getAveEndToEndLink(int mID){
        int len = this.linkMatrix[mID].length;
        long totalValue = 0;
        for(int i=0;i<len;i++){
            long data1 = this.linkMatrix[mID][i];
            long data2 = this.linkMatrix[i][mID];
            totalValue +=Math.min(data1,data2);
        }


        return Calc.getRoundedValue((double)totalValue/(double)len);
    }

    public long getAveLink(int mID){
        int len = this.linkMatrix[mID].length;
        long totalValue = 0;
        for(int i=0;i<len;i++){
            long data = this.linkMatrix[mID][i];
            totalValue += data;
        }

        for(int i=0;i<len;i++){
            long data = this.linkMatrix[i][mID];
            totalValue += data;
        }
        return totalValue/(len*2);
    }

    public CPU getCPU(Long id){
        return this.cpuList.get(id);
    }

    /**
     *
     * @return
     */
    public long[][] getLinkMatrix() {
        return linkMatrix;
    }

    /**
     *
     * @param linkMatrix
     */
    public void setLinkMatrix(long[][] linkMatrix) {
        this.linkMatrix = linkMatrix;
    }

    public Hashtable<Long, Machine> getMachineList() {
        return machineList;
    }

    public void setMachineList(Hashtable<Long, Machine> machineList) {
        this.machineList = machineList;
    }

    /**
     *
     * @return
     */
    public Serializable deepCopy(){
        System.gc();
        try{
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bout);
            out.writeObject(this);
            out.close();
            byte[] bytes = bout.toByteArray();
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
            Object newObject = in.readObject();
            in.close();
            return (Serializable) newObject;
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }
}
