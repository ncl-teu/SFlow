package org.ncl.workflow.delegator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.gripps.cloud.CloudUtil;
import net.gripps.cloud.core.*;
import net.gripps.cloud.nfv.NFVEnvironment;
import net.gripps.cloud.nfv.NFVUtil;
import net.gripps.environment.CPU;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Hidehiro Kanemitsu on 2019/05/17
 */
public class EnvJsonLoader {
    private NFVEnvironment env;

    private JsonNode root;

    public EnvJsonLoader() {
        //Initialize the cloud environment.
        this.env = new NFVEnvironment();

    }

    public NFVEnvironment loadEnv(String path){
        try{
            ObjectMapper env_mapper = new ObjectMapper();
            this.root = env_mapper.readTree(new File(path));

            //dc_list;
            Iterator<JsonNode> dcIte = this.root.get("dc_list").iterator();
            int dcNum = this.root.get("dc_list").size();
            HashMap<Long, Cloud> retMap = new HashMap<Long, Cloud>();
            //Cloud loop
            for(int i = 0; i< dcNum; i++){

                //while(dcIte.hasNext()) {
                JsonNode n = dcIte.next();

               //System.out.println(n.get("dc_id").longValue());
                Cloud dc = new Cloud();
                dc.setId(n.get("dc_id").longValue());

                long dc_bw = n.get("bw").longValue();
                dc.setBw(dc_bw);
                //ホスト数の生成
                long hostNum = n.get("host_list").size();

                HashMap<Long, ComputeHost> hostMap = new HashMap<Long, ComputeHost>();

                //ホスト数分だけのループ
                for (int j = 0; j < hostNum; j++) {
                    //Host
                    JsonNode host = n.get("host_list").get(j);
                    //ComputeHostの生成
                    //CPUソケット数
                    int cpuNum = host.get("cpu_list").size();
                    //帯域幅
                    long bw = host.get("bw").longValue();
                    TreeMap<Long, CPU> cpuMap = new TreeMap<Long, CPU>();
                    LinkedBlockingQueue<VCPU> vQueue = new LinkedBlockingQueue<VCPU>();
                    boolean isMoreVM = true;

                    //CPUソケット数だけのループ
                    for (int k = 0; k < cpuNum; k++) {
                        //CPU:
                        JsonNode cpu = host.get("cpu_list").get(k);
                        //コア数
                        int coreNum = cpu.get("core_list").size();

                        long mips = cpu.get("mips").longValue();
                        HashMap<Long, Core> coreMap = new HashMap<Long, Core>();

                        //コア数分だけのループ
                        for (int l = 0; l < coreNum; l++) {
                            JsonNode core = cpu.get("core_list").get(l);
                            double rate = 1.0;
                            HashMap<Long, VCPU> vcpuMap = new HashMap<Long, VCPU>();
                            String corePrefix = core.get("core_id").asText();
                            int vCPUNum = core.get("vcpu_list").size();
                            //VCPUを生成．
                            for (int m = 0; m < vCPUNum; m++) {
                                JsonNode vCPU = core.get("vcpu_list").get(m);
                                String prefix = vCPU.get("vcpu_id").asText();
                                HashMap<String, Long> pMap = new HashMap<String, Long>();
                                pMap.put(CloudUtil.ID_DC, new Long(i));
                                pMap.put(CloudUtil.ID_HOST, new Long(j));
                                pMap.put(CloudUtil.ID_CPU, new Long(k));
                                pMap.put(CloudUtil.ID_CORE, new Long(l));
                                pMap.put(CloudUtil.ID_VCPU, new Long(m));
                                //  String cPrefix = i + CloudUtil.DELIMITER + j + CloudUtil.DELIMITER + k + CloudUtil.DELIMITER + l;
                                VCPU vcpu = new VCPU(prefix, corePrefix, pMap, null, (long) (mips * rate), 0);
                                vcpuMap.put(new Long(m), vcpu);
                                String id = vcpu.getPrefix();
                                vQueue.offer(vcpu);
                                this.env.getGlobal_vcpuMap().put(prefix, vcpu);

                        /*    System.out.println("vCPU_Prefix:"+vcpu.getPrefix()+"/DataCenterID:"+CloudUtil.getInstance().getDCID(id)+"/HostID:"+CloudUtil.getInstance().getHostID(id)+
                                    "/CPUSocketID:"+CloudUtil.getInstance().getCPUID(id)+"/CoreID:"+CloudUtil.getInstance().getCoreID(id)+"/vCPUID:"+CloudUtil.getInstance().getVCPUID(vcpu.getPrefix()));
                     */
                            }

                            //System.out.println("core:"+corePrefix);
                            HashMap<String, Long> pMap = new HashMap<String, Long>();
                            pMap.put(CloudUtil.ID_DC, new Long(i));
                            pMap.put(CloudUtil.ID_HOST, new Long(j));
                            pMap.put(CloudUtil.ID_CPU, new Long(k));
                            pMap.put(CloudUtil.ID_CORE, new Long(l));
                            //コアの利用率上限値を設定する．
                            int maxUsage = core.get("maxusage").intValue();
                            Core c = new Core(corePrefix, vCPUNum, (long) (mips * rate), new Long(l), vcpuMap, maxUsage);
                            c.setPrefixMap(pMap);
                            coreMap.put(c.getCoreID(), c);
                            this.env.getGlobal_coreMap().put(corePrefix, c);


                        }


                        String cpuPrefix = cpu.get("cpu_id").asText();
                        HashMap<String, Long> pMap = new HashMap<String, Long>();
                        pMap.put(CloudUtil.ID_DC, new Long(i));
                        pMap.put(CloudUtil.ID_HOST, new Long(j));
                        pMap.put(CloudUtil.ID_CPU, new Long(k));

                        CloudCPU ccpu = new CloudCPU(new Long(k), mips, new Vector(), new Vector(),
                                mips, coreMap, cpuPrefix, pMap);

                        cpuMap.put(new Long(k), ccpu);
                        this.env.getGlobal_cpuMap().put(cpuPrefix, ccpu);
                    }

                    String hostPrefix = i + CloudUtil.DELIMITER + j;

                    ComputeHost chost = new ComputeHost(new Long(j), cpuMap, cpuNum, new HashMap<String, VM>(), new Long(i), hostPrefix, bw);
                    chost.setIpAddr(host.get("ip").asText());
                    hostMap.put(new Long(j), chost);
                    dc.setComputeHostMap(hostMap);
                    retMap.put(new Long(i), dc);
                    this.env.getGlobal_hostMap().put(hostPrefix, chost);


                    //次に，VM数分だけのループ
                    int vm_num = host.get("vm_list").size();
                    //System.out.println("vmnum:"+vm_num);
                    //HashMap<String, VM> vmMap = new HashMap<String, VM>();
                    for (int v = 0; v < vm_num; v++) {
                      /*  if (!isMoreVM) {
                            break;
                        }

                       */
                        JsonNode jvm = host.get("vm_list").get(v);
                        //IDを生成．
                        String vmPrefix = jvm.get("vm_id").asText();
                        //String hostPrefix =  i + CloudUtil.DELIMITER + j;
                        long ramSize = jvm.get("ram").longValue();
                        int vcpu_num = jvm.get("vcpu_list").size();
                        //     VM vm  = new VM(vmPrefix, hostPrefix,  new HashMap<String, VCPU>(), ramSize, vmPrefix);
                        HashMap<String, VCPU> vMap = new HashMap<String, VCPU>();

                        int realLen = Math.min(vcpu_num, vQueue.size());
                        for (int q = 0; q < vcpu_num; q++) {
                            JsonNode jvcpu = jvm.get("vcpu_list").get(q);
                            String jvpuid = jvcpu.get("vcpu_id").asText();

                            /*if (vQueue.isEmpty()) {
                                isMoreVM = false;
                                break;
                            }*/
                            //先程追加したキューから，vcpuを取り出して入れる．
                            //VCPU takenVCPU = vQueue.poll();
                            VCPU takenVCPU = this.env.getGlobal_vcpuMap().get(jvpuid);
                            takenVCPU.setVMID(vmPrefix);
                            vMap.put(takenVCPU.getPrefix(), takenVCPU);

                        }
                        VM vm = new VM(vmPrefix, hostPrefix, vMap, ramSize, vmPrefix);
                        vm.setIpAddr(jvm.get("ip").asText());
                        //VMを，ホストへ追加する．
                        chost.getVmMap().put(vmPrefix, vm);
                        this.env.getGlobal_vmMap().put(vmPrefix, vm);
                    }
                }
            }
            this.env.setDcMap(retMap);
            return this.env;
        }catch(Exception e){
            e.printStackTrace();
        }

        return this.env;



    }
}
