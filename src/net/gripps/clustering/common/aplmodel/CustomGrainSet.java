package net.gripps.clustering.common.aplmodel;

import net.gripps.clustering.algorithms.mwsl_delta.TaskGrain;
import net.gripps.clustering.algorithms.mwsl_delta.TaskGrainComparator;

import java.util.TreeSet;
import java.util.Iterator;

/**
 * Author: H. Kanemitsu
 * Date: 2009/12/14
 */
public class CustomGrainSet{


   // private TreeSet<TaskGrain> grainSet;
    //private Hashtable<Long, TaskGrain> grainSet;
    private TreeSet<TaskGrain> grainSet;

    /*public CustomGrainSet(Hashtable<Long, TaskGrain> grainSet) {
       // super();
        this.grainSet = grainSet;

    }

    public Hashtable<Long, TaskGrain> getGrainSet() {
        return grainSet;
    }

    public void setGrainSet(Hashtable<Long, TaskGrain> grainSet) {
        this.grainSet = grainSet;
    }
    */
   /* public CustomGrainSet(TreeSet<TaskGrain> set){
        this.grainSet = grainSet;
    }
    */

    public CustomGrainSet(){
        this.grainSet = new TreeSet<TaskGrain>(new TaskGrainComparator());
    }


    public void addGrain(TaskGrain g){
       // this.grainSet.put(g.getId(), g);
        this.grainSet.add(g);
        
    }

    /**
     *
     * @return
     */
    /*public LinkedList<TaskGrain> getGrainList(){

        this.grainSet.
        LinkedList<TaskGrain> list = new LinkedList<TaskGrain>();
        Iterator<TaskGrain> gIte = this.grainSet.values().iterator();
        while(gIte.hasNext()){
            TaskGrain grain = gIte.next();
            list.add(grain);
        }

        return list;
    }
    */

    public TreeSet<TaskGrain> getGrainSet() {
        return grainSet;
    }

    public void setGrainSet(TreeSet<TaskGrain> grainSet) {
        this.grainSet = grainSet;
    }

    /*
    public void remove(Long id){
        this.grainSet.remove(id);
    }
    */

    public void remove(TaskGrain grain){
        this.grainSet.remove(grain);
    }


    public TaskGrain searchGrain(Long id){
        Iterator<TaskGrain> gIte = this.grainSet.iterator();
        TaskGrain retG = null;
        while(gIte.hasNext()){
            TaskGrain grain = gIte.next();
            Long tId = grain.getId();
            if(tId.longValue() == id.longValue()){
                retG = grain;
                break;
            }
        }

        return retG;

    }

    /**
     *
     * @param id
     * @return
     */
    public TaskGrain remove(Long id){
        TaskGrain retG = this.searchGrain(id);
        if(retG != null){
            this.grainSet.remove(retG);
            return retG;
        }else{
            return null;

        }
    }

}
