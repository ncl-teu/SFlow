package net.gripps.ccn.core;

import net.gripps.ccn.CCNUtil;
import net.gripps.ccn.Logger.CCNLog;
import net.gripps.ccn.icnsfc.AutoUtil;
import net.gripps.ccn.icnsfc.core.AutoEnvironment;
import net.gripps.ccn.icnsfc.core.AutoInfo;
import net.gripps.ccn.icnsfc.logger.ISLog;
import net.gripps.ccn.icnsfc.process.AutoSFCMgr;
import net.gripps.ccn.icnsfc.routing.AutoRouting;
import net.gripps.ccn.process.CCNMgr;
import net.gripps.cloud.CloudUtil;
import net.gripps.cloud.nfv.sfc.SFC;
import net.gripps.cloud.nfv.sfc.VNF;
import org.ncl.workflow.util.NCLWUtil;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

public class CCNDataReceiver implements Runnable{


    /**
     * コンテンツの領域．ルータ，ノードのcontentsQueueそのもの．
     *
     */
    protected LinkedBlockingQueue<CCNContents> contentsQueue;

    /**
     * このクラスで，同時に受信するデータのためのキュー．
     */
    protected LinkedBlockingQueue<CCNContentsInfo> tmpQueue;

    /**
     * 指定ノード・ルータのbw値．
     */
    protected double  bw;

    protected int state;

    public CCNDataReceiver(LinkedBlockingQueue<CCNContents> contentsQueue, double bw) {
        this.contentsQueue = contentsQueue;
        this.tmpQueue = new LinkedBlockingQueue<CCNContentsInfo>();
        this.bw = bw;
        this.state = CCNUtil.STATE_NODE_NONE;
    }

    @Override
    public void run() {
        //キューから取り出して，計算して，またaddする．
        while(true){
            try{
                Thread.sleep(CCNUtil.ccn_hop_per_delay);


                long maxConNum = 0;
                while(!this.tmpQueue.isEmpty()){

                    //1秒単位で処理する．
                    long currentSize = this.tmpQueue.size();
                    if(maxConNum <= currentSize){
                        maxConNum = currentSize;
                    }
                    //とりだす．
                    CCNContentsInfo info = this.tmpQueue.poll();
//                    ForwardHistory fh = info.getContents().getHistoryList().getLast();


                    long currentMinBW = info.getContents().getMinBW();
                    long realBW = (long)Math.min(this.bw/currentSize, currentMinBW);

                    //実際のBWを計算する．この場合はtmp中にあるデータ数分だけ按分される．
                    // double realBW = CCNUtil.getRoundedValue(minBW/currentSize);

                    //ルータからの転送であれば，帯域幅の計算をする．
               /* if(fh.getFromType() == CCNUtil.NODETYPE_ROUTER){
                    //現在の同時接続数を調べる．コネクション数分だけ，
                    //送信帯域は按分される．
                    info.getContents().setMinBW(realBW);
                    this.tmpQueue.clear();
                    break;
                }*/

                    long remSize = info.getRemainedSize() - (long)(realBW / 8);
                    Thread.sleep(1000);
                    //もし残りがなければ，contentsQueueへいれる．
                    if(remSize <= 0){

                        CCNContents c = info.getContents();
                        //リストを更新
                        LinkedList<ForwardHistory> fList = c.getHistoryList();

                        fList.getLast().setArrivalTime(System.currentTimeMillis());
                        this.contentsQueue.offer(info.getContents());
                        ForwardHistory last = c.getHistoryList().getLast();

                        //宛先がノードであれば，最終地点である．
                        if(last.getToType() == CCNUtil.NODETYPE_NODE){
                            String BC = "-";
                            if(c.isBC()){
                                BC = "o";
                            }

                            if(c.isCache()){
                                //キャッシュの場合
                                CCNLog.getIns().log(",3,"+c.getPrefix()+","+c.getSize()+","+fList.getFirst().getStartTime()+","+ fList.getLast().getArrivalTime()+","+
                                        (fList.getLast().getArrivalTime()-fList.getFirst().getStartTime())+","+
                                        last.getToID()+","+c.getCurrentOwnerID()+","+fList.size()+","+maxConNum+","+"o"+","+BC);
                                if(CCNMgr.getIns().isSFCMode()){

                                    long current = System.currentTimeMillis();
                                    long duration = current - c.getHistoryList().getFirst().getStartTime();
                                    String cap = null;
                                    //cに含まれている，宛先タスクの情報を取
                                    Long jobID = AutoSFCMgr.getIns().getJobID(c.getPrefix());
                                    Long toID = AutoSFCMgr.getIns().getSucVNFID(c.getPrefix());
                                    Long fromID = AutoSFCMgr.getIns().getPredVNFID(c.getPrefix());

                                   // SFC sfc = this.sfcMap.get(jobID);

                                    cap = "R";
                                    AutoEnvironment env = AutoSFCMgr.getIns().getEnv();
                                    //ここでは，転送先のルータIDを取得するのみ
                                  //  SFC sfc_int = (SFC) p.getAppParams().get(AutoUtil.SFC_NAME);
                                    //VNF predVNF = sfc_int.findVNFByLastID(fromID);

//System.out.println(sfc_int.getAplID() + ":Candidate VCPU for "+p.getPrefix() + ":"+vCPUID+"@"+this.getRouterID());
                                   // CCNRouter router = (CCNRouter) NCLWUtil.findVM(AutoSFCMgr.getIns().getEnv(), vCPUID);
                                    long proctime = 0;
                                    if(c.getHistoryList().getFirst().getCustomMap().containsKey("proctime")){
                                        proctime = ((Long)c.getHistoryList().getFirst().getCustomMap().get("proctime")).longValue();
                                    }
                                    ISLog.getIns().log(",Data.,1,"+c.getPrefix() + "," +proctime + ","+c.getAplID()+","+jobID+","+c.getPrefix()+","+fromID+",R"+last.getFromID()+"->,"+toID +"@N" +last.getToID() + ","+
                                            c.getHistoryList().size() +","+ duration + ","+c.getSize()+","+ "-" + ","+ "-" + ",-"+","+realBW + ","+maxConNum+","+current);
                                    AutoSFCMgr.getIns().saveFinishTime(c.getAplID(), jobID);

                                }
                            }else{
                                CCNLog.getIns().log(",2,"+c.getPrefix()+","+c.getSize()+","+fList.getFirst().getStartTime()+","+ fList.getLast().getArrivalTime()+","+
                                        (fList.getLast().getArrivalTime()-fList.getFirst().getStartTime())+","+
                                        last.getToID()+","+c.getOrgOwnerID()+","+fList.size()+","+maxConNum+","+"o"+","+BC);
                                if(CCNMgr.getIns().isSFCMode()){

                                    long current = System.currentTimeMillis();
                                    long duration = current - c.getHistoryList().getFirst().getStartTime();
                                    String cap = null;
                                    //cに含まれている，宛先タスクの情報を取
                                    Long jobID = AutoSFCMgr.getIns().getJobID(c.getPrefix());
                                    Long toID = AutoSFCMgr.getIns().getSucVNFID(c.getPrefix());
                                    Long fromID = AutoSFCMgr.getIns().getPredVNFID(c.getPrefix());

                                    // SFC sfc = this.sfcMap.get(jobID);

                                    cap = "R";
                                    AutoEnvironment env = AutoSFCMgr.getIns().getEnv();
                                    //ここでは，転送先のルータIDを取得するのみ
                                    //  SFC sfc_int = (SFC) p.getAppParams().get(AutoUtil.SFC_NAME);
                                    //VNF predVNF = sfc_int.findVNFByLastID(fromID);

//System.out.println(sfc_int.getAplID() + ":Candidate VCPU for "+p.getPrefix() + ":"+vCPUID+"@"+this.getRouterID());
                                    // CCNRouter router = (CCNRouter) NCLWUtil.findVM(AutoSFCMgr.getIns().getEnv(), vCPUID);

                                    long proctime = 0;
                                    if(c.getHistoryList().getFirst().getCustomMap().containsKey("proctime")){
                                        proctime = ((Long)c.getHistoryList().getFirst().getCustomMap().get("proctime")).longValue();
                                    }
                                    if(c.getAplID().longValue() == -1){
                                        System.out.println();
                                    }
                                    ISLog.getIns().log(",Data.,0,"+c.getAplID()+","+jobID+","+c.getPrefix()+","+proctime+","+fromID+",R"+last.getFromID()+"->,"+toID +"@N" +last.getToID() + ","+
                                            c.getHistoryList().size() +","+ duration +","+ c.getSize()+","+ "-" + ","+ "-" + ",-"+","+realBW + ","+maxConNum+","+current);
                                    AutoSFCMgr.getIns().saveFinishTime(c.getAplID(), jobID);



                                }

                            }


                        }
                        //break;


                    }else{
                        //まだなら，また最後へいれる．
                        info.setRemainedSize(remSize);
                        this.tmpQueue.offer(info);
                    }



                }
                if(this.state == CCNUtil.STATE_NODE_END){
                    break;
                }

            }catch(Exception e){
                e.printStackTrace();
            }
        }

    }

    public void addContents(CCNContents c){
        this.contentsQueue.offer(c);
    }

    public LinkedBlockingQueue<CCNContents> getContentsQueue() {
        return contentsQueue;
    }

    public void setContentsQueue(LinkedBlockingQueue<CCNContents> contentsQueue) {
        this.contentsQueue = contentsQueue;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
