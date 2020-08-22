package org.ncl.workflow.ccn.util;

import net.gripps.cloud.core.Cloud;
import net.gripps.cloud.core.CloudEnvironment;
import net.gripps.cloud.core.VCPU;
import net.gripps.cloud.nfv.NFVEnvironment;
import net.gripps.clustering.tool.Calc;
import net.gripps.environment.CPU;
import org.ncl.workflow.util.NCLWUtil;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.NetworkParams;
import oshi.software.os.OperatingSystem;

import java.io.Serializable;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

public class ResourceMgr implements Serializable {

    private static ResourceMgr own;

    private NFVEnvironment env;

    private String ownIPAddr;



    public  static ResourceMgr getIns() {
        if (ResourceMgr.own == null) {
            ResourceMgr.own = new ResourceMgr();
        }

        return ResourceMgr.own;

    }

    private ResourceMgr(){

    }

    public NFVEnvironment getEnv() {
        return env;
    }

    public void setEnv(NFVEnvironment env) {
        this.env = env;
    }

    public CloudEnvironment createCloud(){
        Cloud dc =  new Cloud();
        //dc.setId()
        return null;
    }

    public long createID(String orgID){
        String  val = orgID.replaceAll(" ", "");
        return Math.abs((long)val.hashCode());
    }

    public void initResource() {
        try{
            SystemInfo info = new SystemInfo();
            HardwareAbstractionLayer hware = info.getHardware();

            //ProcessorのIDを取得する．これをホストIDに紐付ける．
            //String str_hostID = hware.getProcessor().getProcessorIdentifier().getIdentifier();
            String str_hostID = hware.getProcessor().getProcessorIdentifier().getProcessorID();

        //    System.out.println("CPUID:"+ hware.getProcessor().getProcessorIdentifier().getProcessorID());
        //    System.out.println("CPUID2:"+ hware.getProcessor().getProcessorIdentifier().getIdentifier());


            //ホストID: CPUのID
            long hostID = this.createID(str_hostID);
            //DCのID: IPのデフォルトゲートウェイアドレス
            long cloudID = this.createID(info.getOperatingSystem().getNetworkParams().getIpv4DefaultGateway());
//System.out.println("DC: "+cloudID + "/HostID:"+hostID);
            //これでクラウドを作る．
            Cloud dc = new Cloud();
            dc.setId(cloudID);
            //ホスト側のBW
            long bw = 0;

            NetworkIF[] ifs = hware.getNetworkIFs();
            //指定のネットワークアドレスに対応したNICの帯域幅を取得する．
            String org_nwAddr =  NCLWUtil.ccn_networkaddress;
            //定義済みNWアドレスを2進に変換する．
            byte[] bIP = InetAddress.getByName(org_nwAddr).getAddress();
            String ip_binary = new BigInteger(1, bIP).toString(2);
          // System.out.println(info.getOperatingSystem().getNetworkParams().getIpv4DefaultGateway());

            for(int i=0;i<ifs.length;i++){
                if(ifs[i].getIPv4addr().length>0){
                   // System.out.println(i+":"+ifs[i].getIPv4addr()[0] +"/"+ifs[i].getName() + "/"+ifs[i].getSpeed());

                    byte a = ifs[i].getSubnetMasks()[0].byteValue();
                    String networkAddress = this.calcNetworkAddress(ifs[i].getIPv4addr()[0], ifs[i].getSubnetMasks()[0]);
                    //System.out.println("MAC:"+ifs[i].getMacaddr() + "BW:"+ifs[i].getSpeed()/(1000000*8));

                    if(ip_binary.equals(networkAddress)){
                        //指定のネットワークに一致すれば，その帯域幅を取得する．
                        //親の帯域幅はわかる？
                        bw = (long)Calc.getRoundedValue(ifs[i].getSpeed()/(1000000*8));
                        break;
                    }
                }
            }
            dc.setBw(bw);
            //ホストのBWも,bwに設定する．

            long CPUID = this.createID(hware.getProcessor().getProcessorIdentifier().getProcessorID());
            int llen = hware.getProcessor().getLogicalProcessorCount();
            TreeMap<Long, CPU> cpuMap = new TreeMap<Long, CPU>();
            System.out.println("LCPU_count:"+hware.getProcessor().getLogicalProcessorCount());
            System.out.println("LCPU_length:"+hware.getProcessor().getLogicalProcessors().length);
            System.out.println("Speed:"+hware.getProcessor().getCurrentFreq()[0]);
            int vCPULen = hware.getProcessor().getLogicalProcessorCount();
            System.out.println("PCPU_cnt:"+hware.getProcessor().getPhysicalProcessorCount());
            System.out.println("CPUID:"+hware.getProcessor().getProcessorIdentifier().getProcessorID());
            for(int i=0;i<llen;i++){
                System.out.println("Group:"+hware.getProcessor().getLogicalProcessors()[i].getNumaNode());
                //たぶん，CPUソケット番号
                System.out.println("CPU#:"+hware.getProcessor().getLogicalProcessors()[i].getProcessorGroup());
                //Core番号
                System.out.println("Core#:"+hware.getProcessor().getLogicalProcessors()[i].getPhysicalProcessorNumber());
                System.out.println("Name:"+hware.getProcessor().getProcessorIdentifier().getName());
                System.out.println("ID:"+hware.getProcessor().getProcessorIdentifier().getProcessorID());
                System.out.println("Model:"+hware.getProcessor().getProcessorIdentifier().getModel());
                System.out.println("family:"+hware.getProcessor().getProcessorIdentifier().getFamily());
                System.out.println();
            }
        }catch(Exception e){
            e.printStackTrace();
        }


    }

    public String getOwnIPAddr() {
        if(this.ownIPAddr == null){
            this.ownIPAddr = this.calcOwnIPAddr();
        }
        return this.ownIPAddr;
    }



    /**
     *
     * @return
     */
    private  String calcOwnIPAddr(){
        String retAddr = null;
            try{
                SystemInfo info = new SystemInfo();
                HardwareAbstractionLayer hware = info.getHardware();
                NetworkIF[] ifs = hware.getNetworkIFs();
                //指定のネットワークアドレスに対応したNICの帯域幅を取得する．
                String org_nwAddr =  NCLWUtil.ccn_networkaddress;
                //定義済みNWアドレスを2進に変換する．
                byte[] bIP = InetAddress.getByName(org_nwAddr).getAddress();
                String ip_binary = new BigInteger(1, bIP).toString(2);
                //System.out.println(info.getOperatingSystem().getNetworkParams().getIpv4DefaultGateway());

                for(int i=0;i<ifs.length;i++){
                    if(ifs[i].getIPv4addr().length>0){
                        byte a = ifs[i].getSubnetMasks()[0].byteValue();
                        String networkAddress = this.calcNetworkAddress(ifs[i].getIPv4addr()[0], ifs[i].getSubnetMasks()[0]);
                        if(ip_binary.equals(networkAddress)){
                            //指定のネットワークに一致すれば，その帯域幅を取得する．
                            //親の帯域幅はわかる？
                            retAddr = ifs[i].getIPv4addr()[0];
                            break;
                        }
                    }
                }
                //ホストのBWも,bwに設定する．
                return retAddr;

            }catch(Exception e){
                e.printStackTrace();
            }
            return retAddr;
    }

    public String calcNetworkAddress(String ip, int mask){
        try{
            byte[] bIP = InetAddress.getByName(ip).getAddress();
            String ip_binary = new BigInteger(1, bIP).toString(2);

            StringBuffer buf = new StringBuffer();
            for(int j=0;j<32;j++){

                if(j<mask){
                    buf.append("1");
                }else{
                    buf.append("0");
                }
                if((j% 8 == 7)&&(j<mask)){
                    //buf.append(".");
                }
            }

           // byte[] bytes2 = InetAddress.getByName(buf.toString()).getAddress();
           // String subnetMask_binary = new BigInteger(1, bytes2).toString(2);
            String subIP = ip_binary.substring(0,mask);
            int remained = 32 - mask;
            StringBuffer ipBuf = new StringBuffer(subIP);
            for(int i=0;i<remained;i++){
                ipBuf.append("0");
            }
            return ipBuf.toString();
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }




}
