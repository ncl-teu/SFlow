package net.gripps.ccn.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by kanemih on 2018/11/02.
 */
public class FIB {
    /**
     * (Name属性, Faceリスト)の対応付けのMap
     */
    protected HashMap<String, LinkedList<Face>> table;


    public FIB(HashMap<String, LinkedList<Face>> table) {
        this.table = table;
    }

    public boolean lock;

    /**
     *
     */
    public FIB() {
        this.table = new HashMap<String, LinkedList<Face>>();
        this.lock = false;
    }

    public boolean isLock() {
        return lock;
    }

    public void setLock(boolean lock) {
        this.lock = lock;
    }

    /**
     * 新規にFaceを追加する．
     * @param prefix
     * @param f
     * @return
     */
    public boolean addFace(String prefix, Face f){


        if(this.table.containsKey(prefix)){
            //次に，リストから同一faceの物があるかを調べる．
            LinkedList<Face> fList = this.table.get(prefix);
            Iterator<Face> fIte = fList.iterator();
            while(fIte.hasNext()){
                Face face = fIte.next();
                if((face.getPointerID().longValue() == f.getPointerID().longValue()) &&
                        (face.getType() == f.getType())){
                    //同一faceがあれば，無視する．
                    return false;
                }
            }
            //後は，エントリに追加するだけ
            fList.add(f);
            return true;
        }else{
            LinkedList<Face> newList = new LinkedList<Face>();
            newList.add(f);
            this.table.put(prefix, newList);
            return true;
        }
    }

    /**
     *
     * @param prefix
     * @return
     */
    public boolean removeByKey(String prefix){
        this.table.remove(prefix);
        return true;
    }

    /**
     *
     * @param prefix
     * @param f
     * @return
     */
    public boolean removeFace(String prefix, Face f){
        if(this.table.containsKey(prefix)){
            //次に，リストから同一faceの物があるかを調べる．
            LinkedList<Face> fList = this.table.get(prefix);
            fList.remove(f);
            //さらにエントリが空になれば，キー毎削除する．
            if(fList.isEmpty()){
                this.table.remove(prefix);
            }
            return true;
        }else{
            return false;
        }
    }

    public HashMap<String, LinkedList<Face>> getTable() {
        return table;
    }

    public void setTable(HashMap<String, LinkedList<Face>> table) {
        this.table = table;
    }


}
