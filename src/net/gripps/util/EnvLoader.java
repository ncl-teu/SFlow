package net.gripps.util;

import net.gripps.clustering.common.Constants;
import net.gripps.clustering.common.aplmodel.AplOperator;
import net.gripps.environment.CPU;
import net.gripps.environment.Environment;
import net.gripps.environment.Machine;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Created by kanemih on 2015/11/20.
 */
public class EnvLoader extends Environment {

    String fileName;
    String path;

    Hashtable<Long, CPU> cpuMap;

    public EnvLoader(Hashtable<Long, Machine> machineList, long[][] linkMatrix, String path) {
        this.machineList = machineList;
        this.linkMatrix = linkMatrix;
        this.path = path;
    }

    public EnvLoader(String path, String fileName) {
        this.path = path;
        this.fileName = fileName;
        this.initialize(this.fileName);
        this.calcMachineList(this.path);

    }

    public EnvLoader(String fileName, Hashtable<Long, CPU> cpuMap_in){
        this.fileName = fileName;
        this.cpuMap = cpuMap_in;
        this.initialize(fileName);
        this.calcMachineListFromMap();

    }

    public void initialize(String fileName) {
        try {
            Properties prop = new Properties();
            prop.load(new FileInputStream(fileName));
            //マシンのサイズの格納
            //this.size = Integer.valueOf(prop.getProperty("cpu.num")).intValue();
            // this.size = clusterSize;

            //マシンの処理速度
            this.speedMax = Long.valueOf(prop.getProperty("cpu.speed.max")).longValue();
            this.speedMin = Long.valueOf(prop.getProperty("cpu.speed.min")).longValue();

            //マシン間の転送速度
            this.linkMax = Long.valueOf(prop.getProperty("cpu.link.max")).longValue();
            this.linkMin = Long.valueOf(prop.getProperty("cpu.link.min")).longValue();

            this.distType = Integer.valueOf(prop.getProperty("cpu.distribution")).intValue();
            this.cpu_mu = Double.valueOf(prop.getProperty("cpu.mu")).doubleValue();
            this.cpu_link_mu = Double.valueOf(prop.getProperty("cpu.link.mu")).doubleValue();
            this.virtualmode = Integer.valueOf(prop.getProperty("mwsl.virtual.condition")).intValue();
            this.setupTime = Long.valueOf(prop.getProperty("env.setuptime")).longValue();

            this.minLinkSpeed = Constants.MAXValue;
            this.maxLinkSpeed = 0;

            this.maxSpeed = 0;

            this.minSpeed = Constants.MAXValue;


            //マルチコアがOnかどうか
            if (Integer.valueOf(prop.getProperty("cpu.multicore")).intValue() > 0) {
                this.isMultiCore = true;
                this.core_min = Integer.valueOf(prop.getProperty("cpu.core.min")).intValue();
                this.core_max = Integer.valueOf(prop.getProperty("cpu.core.max")).intValue();

            } else {
                this.isMultiCore = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Hashtable<Long, Machine> calcMachineList(String path) {
        this.machineList = new Hashtable<Long, Machine>();
        try {
            //コア行列のサイズ定義
          //  this.linkMatrix = new long[this.size][this.size];
            this.cpuList = new Hashtable<Long, CPU>();
            this.machineList = new Hashtable<Long, Machine>();
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF-8"));
            // 最終行まで読み込む
            String line = "";
            //まずは一行読み
            br.readLine();
            int size_1 = 0;
            while ((line = br.readLine()) != null) {
                size_1++;
                StringTokenizer st = new StringTokenizer(line, ",");
                int cnt = 0;
                while (st.hasMoreTokens()) {
                    // 1行の各要素をタブ区切りで表示
                    String val = st.nextToken();
                    if (cnt == 0) {
                        //処理速度
                        long speedValue = Long.valueOf(val).longValue();
                        Long cpuID = Long.valueOf(String.valueOf(size_1-1));
                        CPU CPU = new CPU(cpuID, speedValue, new Vector<Long>(), new Vector<Long>());
                        CPU.setVirtual(false);
                        long machineID = size_1;
                        Machine m = new Machine(machineID, new TreeMap<Long, CPU>(), 1);
                        m.addCore(CPU);

                        //CPUリストに登録
                        this.cpuList.put(cpuID, CPU);
                        //マシンリストに追加させる．
                        this.machineList.put(machineID, m);

                    } else {
                        //帯域幅
                        Machine m = this.machineList.get(new Long(size_1));
                        m.setBw(Long.valueOf(val).longValue());
                    }
                    cnt++;
                }
            }
            this.size = size_1;
            this.linkMatrix = new long[this.size][this.size];


            for (int i = 0; i < this.size; i++) {
                CPU cpu_i = this.cpuList.get(new Long(i));
                //コア間の転送速度の定義
                for (int j = 0; j < this.size; j++) {
                    //CPUの属する先マシンのIDを見て，互いに一致すれば無視する．
                    //CPU_jを見る．
                    CPU cpu_j = this.cpuList.get(new Long(j));
                    if(cpu_i == null || cpu_j==null){
                         System.out.println("test");
                    }
                    if (cpu_j.getMachineID() == cpu_i.getMachineID()) {
                        //もし互いにマシンが同じであれば，無限大の値の帯域幅を設定する．
                        this.linkMatrix[i][j] = Constants.INFINITY;
                    } else {
                        //もし異なるマシン同士であれば，通常通り帯域幅を設定する．
                        Machine m_i = this.machineList.get(cpu_i.getMachineID());
                        Machine m_j = this.machineList.get(cpu_j.getMachineID());
                        this.linkMatrix[i][j] = Math.min(m_i.getBw(), m_j.getBw());
                        cpu_i.setBw(m_i.getBw());
                        cpu_j.setBw(m_j.getBw());

                        //this.linkMatrix[i][j] = AplOperator.getInstance().generateLongValue(this.linkMin, this.linkMax);

                    }


                }
            }


            long[][] linkMX = this.getLinkMatrix();
            long len = linkMX[0].length;
            int cnt = 0;
            long totalBW = 0;
            for (int i = 0; i < len; i++) {
                for (int j = i + 1; j < len; j++) {
                    if (linkMX[i][j] == -1) {
                        continue;
                    } else if (!this.cpuList.containsKey(new Long(i)) || (!this.cpuList.containsKey(new Long(j)))) {
                        // }else if(!this.cpuTable.contains(i) || (!this.cpuTable.contains(j))){
                        continue;
                    } else {
                        totalBW += linkMX[i][j];
                        cnt++;
                    }
                }
            }
            this.aveLink = totalBW / cnt;


        } catch (Exception e) {
            e.printStackTrace();

        }

        return null;
    }

    public Hashtable<Long, CPU> calcMachineListFromMap() {

        this.machineList = new Hashtable<Long, Machine>();
        this.cpuList = new Hashtable<Long, CPU>();

        Iterator<CPU> cpuIte = this.cpuMap.values().iterator();
        while(cpuIte.hasNext()){
            CPU cpu = cpuIte.next();
            if(this.maxSpeed<= cpu.getSpeed()){
                this.maxSpeed = cpu.getSpeed();
            }
            if(this.minSpeed>=cpu.getSpeed()){
                this.minSpeed = cpu.getSpeed();
            }
            if(this.machineList.containsKey(cpu.getMachineID())){
                Machine m = this.machineList.get(cpu.getMachineID());
                m.addCore(cpu);
            }else{
                Machine m = new Machine(cpu.getMachineID(), new TreeMap<Long, CPU>(), 1);
                m.addCore(cpu);
                m.setBw(cpu.getBw());

                //マシンリストに追加させる．
                this.machineList.put(m.getMachineID(), m);
            }
            //CPUリストに登録
            this.cpuList.put(cpu.getCpuID(), cpu);
        }

        this.size = this.cpuMap.size();
        this.linkMatrix = new long[this.size][this.size];

        for (int i = 0; i < this.size; i++) {
            CPU cpu_i = this.cpuList.get(new Long(i));
            //コア間の転送速度の定義
            for (int j = 0; j < this.size; j++) {
                //CPUの属する先マシンのIDを見て，互いに一致すれば無視する．
                //CPU_jを見る．
                CPU cpu_j = this.cpuList.get(new Long(j));
                if (cpu_i == null || cpu_j == null) {
                    System.out.println("test");
                    //continue;
                }

                if (cpu_j.getMachineID() == cpu_i.getMachineID()) {
                    //もし互いにマシンが同じであれば，無限大の値の帯域幅を設定する．
                    this.linkMatrix[i][j] = Constants.INFINITY;
                } else {
                    //もし異なるマシン同士であれば，通常通り帯域幅を設定する．
                    Machine m_i = this.machineList.get(cpu_i.getMachineID());
                    Machine m_j = this.machineList.get(cpu_j.getMachineID());
                    long linkMin = Math.min(m_i.getBw(), m_j.getBw());
                    this.linkMatrix[i][j] = linkMin;
                    if(this.minLinkSpeed>=linkMin){
                        this.minLinkSpeed = linkMin;

                    }

                    if(this.maxLinkSpeed<=linkMin){
                        this.maxLinkSpeed = linkMin;
                    }
                }
            }
        }

        long[][] linkMX = this.linkMatrix;
        long len = linkMX[0].length;
        int cnt = 1;
        long totalBW = 0;
        for (int i = 0; i < len; i++) {
            for (int j = i + 1; j < len; j++) {
                if (linkMX[i][j] == -1) {
                    continue;

                } else if (!this.cpuList.containsKey(new Long(i)) || (!this.cpuList.containsKey(new Long(j)))) {
                    // }else if(!this.cpuTable.contains(i) || (!this.cpuTable.contains(j))){
                    continue;
                } else {
                    totalBW += linkMX[i][j];
                    cnt++;
                }
            }
        }
        this.aveLink = totalBW / cnt;
        return this.cpuMap;

    }
}
