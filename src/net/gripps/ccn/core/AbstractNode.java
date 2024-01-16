package net.gripps.ccn.core;

import net.gripps.ccn.CCNUtil;
import net.gripps.ccn.fibrouting.BaseRouting;
import net.gripps.ccn.fibrouting.ChordDHTRouting;
import net.gripps.ccn.fibrouting.ChordOnBARouting;
import net.gripps.ccn.fibrouting.LongestMatchRouting;
import net.gripps.ccn.icnsfc.routing.AutoRouting;
import net.gripps.ccn.icnsfc.routing.MWRouting;
import net.gripps.cloud.core.ComputeHost;
import net.gripps.cloud.core.VCPU;
import net.gripps.cloud.core.VM;
import net.gripps.cloud.nfv.sfc.SFC;
import net.gripps.environment.CPU;

import java.io.Serializable;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Hidehiro Kanemitsu on 2018/11/12.
 */
public class AbstractNode  extends VM  implements Runnable, Serializable {


    protected int type;
    /**
     * Interestパケットを受信するためのキュー
     */
    protected LinkedBlockingQueue<InterestPacket> interestQueue;

    /**
     * 帯域幅(MBps)
     */
    protected long bw;

    protected  BaseRouting[] routings;

    protected  BaseRouting usedRouting;

    protected  LinkedBlockingQueue<CCNContents> contentsQueue;

    protected CCNDataReceiver receiver;
    /*
    このノードの処理遅延
     */
    protected long ccn_hop_per_delay;



    public AbstractNode (String  ID, String in_hostID, HashMap<String, VCPU> vCPUMap, long ramSize, String orgVMID) {
        //super(machineID, cpuMap, num);
        super(ID, in_hostID, vCPUMap, ramSize, orgVMID);

        this.VMID = ID;
        this.hostID = in_hostID;
        this.vCPUMap = vCPUMap;
        this.ramSize = ramSize;
        this.orgVMID = orgVMID;
        this.ipAddr = null;

    }

    /**
     * 最大の保持容量
     */
    //protected  long maxCapacity;

    public AbstractNode(LinkedBlockingQueue<InterestPacket> interestQueue) {
        //super(-1, new TreeMap<Long, CPU>(), 0, new HashMap<String, VM>(), 0L, null, 0);


        this.interestQueue = interestQueue;
        this.bw = CCNUtil.genLong2(CCNUtil.ccn_bw_min, CCNUtil.ccn_bw_max, CCNUtil.ccn_dist_bw, CCNUtil.ccn_dist_bw_mu);
        this.routings = new BaseRouting[CCNUtil.ccn_routing_allnum];
        this.routings[0] = new ChordDHTRouting();
        this.routings[1] = new ChordOnBARouting();
        this.routings[2] = new LongestMatchRouting();
        this.routings[3] = new AutoRouting();
        this.routings[4] = new MWRouting();
        this.usedRouting = this.routings[CCNUtil.ccn_routing_no];
        this.contentsQueue = new LinkedBlockingQueue<CCNContents>();
        this.receiver = new CCNDataReceiver(this.contentsQueue, this.bw*CCNUtil.ccn_actual_data_rate);
        this.sfcMap = new HashMap<Long, SFC>();
        //遅延を計算する．
        this.ccn_hop_per_delay = CCNUtil.genLong2(CCNUtil.ccn_hop_per_delay_min,
                CCNUtil.ccn_hop_per_delay_max, 1, CCNUtil.ccn_hop_per_delay_mu);

      //  this.maxCapacity = maxCapacity;
    }

    public AbstractNode(){
        //super(-1, new TreeMap<Long, CPU>(), 0, new HashMap<String, VM>(), 0L, null, 0);

        this.interestQueue = new LinkedBlockingQueue<InterestPacket>();
        this.bw = CCNUtil.genLong2(CCNUtil.ccn_bw_min, CCNUtil.ccn_bw_max, CCNUtil.ccn_dist_bw, CCNUtil.ccn_dist_bw_mu);
        this.routings = new BaseRouting[CCNUtil.ccn_routing_allnum];
        this.routings[0] = new ChordDHTRouting();
        this.routings[1] = new ChordOnBARouting();
        this.routings[2] = new LongestMatchRouting();
        this.routings[3] = new AutoRouting();
        this.routings[4] = new MWRouting();

        this.usedRouting = this.routings[CCNUtil.ccn_routing_no];
        this.contentsQueue = new LinkedBlockingQueue<CCNContents>();
        this.receiver = new CCNDataReceiver(this.contentsQueue, this.bw*CCNUtil.ccn_actual_data_rate);

    }

    /**
    public AbstractNode(long machineID, TreeMap<Long, CPU> cpuMap, int num, LinkedBlockingQueue<InterestPacket> interestQueue, long maxCapacity) {
     //   super(machineID, cpuMap, num);
        this.interestQueue = interestQueue;
        this.maxCapacity = maxCapacity;
    }
**/

    public void run() {

    }


    public LinkedBlockingQueue<CCNContents> getContentsQueue() {
        return contentsQueue;
    }

    public void setContentsQueue(LinkedBlockingQueue<CCNContents> contentsQueue) {
        this.contentsQueue = contentsQueue;
    }

    public long getBw() {
        return bw;
    }

    public void setBw(long bw) {
        this.bw = bw;
    }

    public LinkedBlockingQueue<InterestPacket> getInterestQueue() {
        return interestQueue;
    }

    public void setInterestQueue(LinkedBlockingQueue<InterestPacket> interestQueue) {
        this.interestQueue = interestQueue;
    }

    public BaseRouting[] getRoutings() {
        return routings;
    }

    public void setRoutings(BaseRouting[] routings) {
        this.routings = routings;
    }

    public BaseRouting getUsedRouting() {
        return usedRouting;
    }

    public void setUsedRouting(BaseRouting usedRouting) {
        this.usedRouting = usedRouting;
    }

    public long getCcn_hop_per_delay() {
        return ccn_hop_per_delay;
    }

    public void setCcn_hop_per_delay(long ccn_hop_per_delay) {
        this.ccn_hop_per_delay = ccn_hop_per_delay;
    }

    /**
     * キューに追加する処理．
     * データを送信する場合は，必ずこの処理を呼び出してください．
     * tmpQueueにoffer→帯域幅により，指定時間後にcontentsQueueへ追加される．
     * このクラスが，コンテンツのあて先になっている．
     * @param c
     */
    public void forwardData(CCNContents c){

        //this.contentsQueue.offer(c);
        if(this.type == CCNUtil.NODETYPE_NODE){
            //ノードのときだけ帯域幅共有を考慮する．
            CCNContentsInfo info = new CCNContentsInfo(c, c.getSize(), c.getSize());
            this.receiver.tmpQueue.offer(info);
        }else{
            //System.out.println("***OKIKI***");
            try{
                Thread.sleep(this.ccn_hop_per_delay);
                int currentSize = this.contentsQueue.size()+1;
                long minBW = Math.min(this.getBw()/currentSize, c.getMinBW());
                c.setMinBW(minBW);
                if(c.isCache){

                }
                this.contentsQueue.offer(c);
               //long minBW = c.getMinBW();
               /*if(minBW >= this.getBw()){
                    c.setMinBW(this.getBw());
                }
            */
            }catch(Exception e){
                e.printStackTrace();
            }
        }


    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    /*
    public long getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(long maxCapacity) {
        this.maxCapacity = maxCapacity;
    }
    */
}
