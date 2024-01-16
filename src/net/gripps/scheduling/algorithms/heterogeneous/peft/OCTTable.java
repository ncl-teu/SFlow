package net.gripps.scheduling.algorithms.heterogeneous.peft;

import java.util.Hashtable;

/**
 * Author: H. Kanemitsu
 * Date: 14/09/24
 */
public class OCTTable {

    private Hashtable<Long, TaskCPUInfo> table;

    public Hashtable<Long, TaskCPUInfo> getTable() {
        return table;
    }

    public void setTable(Hashtable<Long, TaskCPUInfo> table) {
        this.table = table;
    }

    public OCTTable(Hashtable<Long, TaskCPUInfo> table) {

        this.table = table;
    }

    public long get(long taskID, long cpuID){
        TaskCPUInfo info = this.table.get(new Long(taskID));
        return info.getCpuTable().get(new Long(cpuID));
    }

    public void add(Long taskID, Long cpuID, long value){
        if(this.table.containsKey(taskID)){
            TaskCPUInfo info = this.table.get(taskID);
            info.getCpuTable().put(cpuID, new Long(value));

        }else{
            TaskCPUInfo info = new TaskCPUInfo( new Hashtable<Long, Long>());
            info.getCpuTable().put(cpuID, new Long(value));
            this.table.put(taskID, info);


        }
    }

    public boolean isExist(Long taskID, Long cpuID){
        boolean ret = true;
        if(this.table.containsKey(taskID)){
            TaskCPUInfo info = this.table.get(taskID);
            if(info.getCpuTable().containsKey(cpuID)){

            }else{
                ret = false;
            }

        }else{
            ret = false;
        }

        return ret;
    }
}
