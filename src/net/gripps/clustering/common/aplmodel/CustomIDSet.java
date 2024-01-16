package net.gripps.clustering.common.aplmodel;

import java.util.*;
import java.io.*;
/*
* IDといったキーのみを格納する集合です．キーをソートして格納した場合，
* 途中で値が変わるかもしれず，結果として順序が変わってしまう可能性があるために，キーのみを格納
* するようにしています．
* Author: Hidehiro Kanemitsu<br>
* Date: 2007/08/06
*/
public class CustomIDSet implements Cloneable, Serializable{

    /**
     * 
     */
    private TreeSet<Long> objSet;

    /**
     *
     */
    private int order_mode;


    public CustomIDSet(){
        this.setObjSet(new TreeSet<Long>());
        //this.order_mode = mode;

    }

    /**
     *
     * @return
     */
    public Serializable deepCopy(){
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

    /**
     *
     * @return
     */
    public Object clone() {
        try {
            return (super.clone());
        } catch (CloneNotSupportedException e) {
            throw (new InternalError(e.getMessage()));
        }
    }

    /**
     * 
     */
    public void initializeTaskSet(){
        //this.setObjSet(new TreeSet<Long>());
        this.objSet.clear();
    }

    public boolean isEmpty(){
        return this.objSet.isEmpty();
    }

    public Iterator<Long> iterator(){

        return this.objSet.iterator();
    }

    /**
     *
     * @param id
     * @return
     */
    public boolean contains(Long id){

        return this.getObjSet().contains(id);
    }

    /**
     *
     * @param id
     * @return
     */
    public boolean remove(Long id){

        return this.getObjSet().remove(id);
    }

    /**
     * 
     * @return
     */
    public LinkedList<Long> getList(){
        LinkedList<Long> list = new LinkedList<Long>();

        Iterator<Long> ite = this.objSet.iterator();
        while(ite.hasNext()){
            list.add((ite.next()));
        }

        return list;
    }

    public void addAll(CustomIDSet set){
        this.objSet.addAll(set.getObjSet());

    }


    /**
     * 重複を許さないようにaddを行う．
     * @param id
     */
    public boolean add(Long id){
         //this.getObjSet().put(id,value);
        return this.getObjSet().add(id);
    }


    public TreeSet<Long> getObjSet() {
        return this.objSet;
    }

    public void setObjSet(TreeSet<Long> objSet) {
        this.objSet = objSet;
    }

    public void orderByDecreasing(){

    }

    public void orderByIncreasing(){
        
    }
}
