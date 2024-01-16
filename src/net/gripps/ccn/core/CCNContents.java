package net.gripps.ccn.core;

import net.gripps.ccn.CCNUtil;

import java.io.*;
import java.util.LinkedList;
import java.util.UUID;

/**
 * Created by Hidehiro Kaneimtsu  on 2018/11/13.
 */
public class CCNContents implements Serializable, Cloneable{

    boolean isCache;
    /**
     * コンテンツID(UUID)
     */
    private String  contentID;

    /**
     * カスタマイズされたID
     */
    private long customID;

    /**
     * コンテンツ名(パス区切り）
     */
    private String prefix;

    /**
     * 履歴リスト
     */
    LinkedList<ForwardHistory> historyList;

    /**
     * データサイズ
     */
    private long size;

    /**
     * このコンテンツを持っているオリジナルノードID
     */
    private Long orgOwnerID;

    /**
     * 現在の所持ノードID
     */
    private Long currentOwnerID;

    /**
     * 分割された場合用．オリジナルのコンテンツIDを保持する領域．
     */
    private String orgContentID;

    /**
     * このコンテンツのタイプ
     */
    private int type;

    /**
     * このコンテンツのシーケンス番号．分割用．
     */
    //private long sequenceID;

    /**
     * 分割された場合，何個に分かれているのかを示す値．
     */
    private long maxCount;

    /**
     * 単位時間あたりの最新アクセス数．
     */
    private long currentAccessNum;

    /**
     * 単位時間あたりの最大アクセス数
     */
    private long maxAccessNum;

    /**
     * 単位時間あたりの最小アクセス数
     */
    private long  minAccessRate;

    /**
     * 単位時間あたりの平均アクセス数
     * これだけは浮動小数点を扱います．
     */
    private double  aveAccessRate;

    /**
     * 生成された時刻@オリジナルコンテンツ
     */
    private Long  generatedTimeAtSrc;

    /**
     * 生成された時刻＠キャッシュ
     */
    private long generatedTimeAtCache;

    /**
     * 経由したノード（ルータ）での最小のBW．
     */
    private Long  minBW;

    /**
     * BCによって発見されたかどうかのフラグ
     */
    private Boolean isBC;

    /**
     * CCN関連の処理が失敗して，最終的にIPネットワーク上で
     * 転送されれば，trueに設定される．
     */
    private Boolean  isIPUsed;

    private Long aplID;

    private boolean isFNJ;


    /**
     *
     * @param size
     * @param inName
     * @param orgOwnerID
     * @param type
     */
    public CCNContents(long size, String inName,  Long orgOwnerID,  int type, long genTimeSrc, long genTimeCache, boolean isCache) {
        this.isCache = false;
        this.size = size;
        this.prefix = inName;
        this.orgOwnerID = orgOwnerID;
        this.orgContentID = orgContentID;
        this.type = type;
        this.isFNJ = false;
        //このコンテンツにUIDを設定する．
        String  cID = UUID.randomUUID().toString();
        this.setContentID(cID);
        //オリジナルコンテンツIDをセットする．setメソッドをあえて用意しない．
        this.orgContentID = cID;
        //this.setSequenceID(0);
        this.setMaxCount(1);
        this.currentOwnerID = orgOwnerID;
        this.currentAccessNum = 0;
        this.maxAccessNum = 0;
        this.minAccessRate = 0;
        this.aveAccessRate = 0;
        this.customID = 0;
        this.historyList = new LinkedList<ForwardHistory>();
        this.generatedTimeAtSrc = genTimeSrc;
        this.generatedTimeAtCache = genTimeCache;
        this.minBW = CCNUtil.MAXValue;
        this.isBC = false;

        this.isIPUsed = false;
        this.aplID = -1L;
    }

    public boolean isFNJ() {
        return isFNJ;
    }

    public void setFNJ(boolean FNJ) {
        isFNJ = FNJ;
    }

    public Long getAplID() {
        return aplID;
    }

    public void setAplID(Long aplID) {
        this.aplID = aplID;
    }

    public boolean isIPUsed() {
        return isIPUsed;
    }

    public void setIPUsed(boolean IPUsed) {
        isIPUsed = IPUsed;
    }

    public boolean isBC() {
        return isBC;
    }

    public void setBC(boolean BC) {
        isBC = BC;
    }

    /**
     * 生成されてからの時間@オリジナルコンテンツを計算します．（寿命）
     * @return
     */
    public long getLifeTimeAtSrc(){
        long currentTime = System.currentTimeMillis();
        return currentTime - this.generatedTimeAtSrc;
    }

    /**
     * 生成されてからの時間@本キャッシュを計算します．（寿命）
     * @return
     */
    public long getLifeTimeAtCache(){
        long currentTime = System.currentTimeMillis();
        return currentTime - this.generatedTimeAtCache;
    }

    public long getCustomID() {
        return customID;
    }

    public void setCustomID(long customID) {
        this.customID = customID;
    }


    public long getGeneratedTimeAtSrc() {
        return generatedTimeAtSrc;
    }

    public void setGeneratedTimeAtSrc(long generatedTimeAtSrc) {
        this.generatedTimeAtSrc = generatedTimeAtSrc;
    }

    public long getGeneratedTimeAtCache() {
        return generatedTimeAtCache;
    }

    public void setGeneratedTimeAtCache(long generatedTimeAtCache) {
        this.generatedTimeAtCache = generatedTimeAtCache;
    }

    public boolean isdivided(){
        if(this.maxCount == 1){
            return false;
        }else{
            return true;
        }
    }


    public LinkedList<ForwardHistory> getHistoryList() {
        return historyList;
    }

    public void setHistoryList(LinkedList<ForwardHistory> historyList) {
        this.historyList = historyList;
    }

    public void setOrgContentID(String orgContentID) {
        this.orgContentID = orgContentID;
    }

    public long getCurrentAccessNum() {
        return currentAccessNum;
    }

    public void setCurrentAccessNum(long currentAccessNum) {
        this.currentAccessNum = currentAccessNum;
    }

    public long getMaxAccessNum() {
        return maxAccessNum;
    }

    public void setMaxAccessNum(long maxAccessNum) {
        this.maxAccessNum = maxAccessNum;
    }

    public long getMinAccessRate() {
        return minAccessRate;
    }

    public void setMinAccessRate(long minAccessRate) {
        this.minAccessRate = minAccessRate;
    }

    public double getAveAccessRate() {
        return aveAccessRate;
    }

    public void setAveAccessRate(double aveAccessRate) {
        this.aveAccessRate = aveAccessRate;
    }

    /**
     * 指定したサイズごとに分割するメソッドです．
     * @param unitSize
     * @return
     */
    /**
    private LinkedList<CCNContents> divide(long unitSize){
        LinkedList<CCNContents> retContents = new LinkedList<CCNContents>();

        //既に分割済みなら，nullを返す．
        if(this.isdivided()){

        }else{
           long remainedSize = this.size;
            remainedSize -= unitSize;
            //CCNContents c = new CCNContents(unitSize, )
            //まずは最大数を出す．
            long divCount = 0;
            if(this.size % unitSize != 0){
               divCount = this.size / unitSize + 1;
            }else{
                divCount = this.size / unitSize;
            }
            for(long i = 0;remainedSize >= unitSize;i++){
                Long seqID = new Long(i);
                CCNContents c = new CCNContents(unitSize, this.prefix, this.orgOwnerID, this.type);
                //シーケンスIDをセットする．
                //c.setSequenceID(i);
                c.setMaxCount(divCount);
                c.setSize(unitSize);
                retContents.add(c);
                remainedSize -= unitSize;

            }

            if(remainedSize >0){
                CCNContents c = new CCNContents(unitSize, this.prefix, this.orgOwnerID, this.type);
                //シーケンスIDをセットする．
               // c.setSequenceID(divCount-1);
                c.setMaxCount(divCount);
                c.setSize(remainedSize);
                retContents.add(c);
            }
        }
        return retContents;


    }
**/
    private CCNContents generateCache(){
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
            CCNContents cache = (CCNContents) newObject;
            //新しいUIDをセットする．
            UUID uid = UUID.randomUUID();
            cache.setContentID(uid.toString());
            cache.setCache(true);

            return cache;

        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
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


    public String getPrefix() {
        return this.prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getContentID() {
        return contentID;
    }

    public void setContentID(String contentID) {
        this.contentID = contentID;
    }

    public Long getOrgOwnerID() {
        return orgOwnerID;
    }

    public void setOrgOwnerID(Long orgOwnerID) {
        this.orgOwnerID = orgOwnerID;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getOrgContentID() {
        return orgContentID;
    }



    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }



    public long getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(long maxCount) {
        this.maxCount = maxCount;
    }

    public boolean isCache() {
        return isCache;
    }

    public void setCache(boolean isCache) {
        this.isCache = isCache;
    }

    public Long getCurrentOwnerID() {
        return currentOwnerID;
    }

    public void setCurrentOwnerID(Long currentOwnerID) {
        this.currentOwnerID = currentOwnerID;
    }

    public long getMinBW() {
        return minBW;
    }

    public void setMinBW(long minBW) {
        this.minBW = minBW;
    }
}
