package net.gripps.grouping.CEFT;

import net.gripps.clustering.common.aplmodel.AbstractTask;
import net.gripps.clustering.common.aplmodel.BBTask;
import net.gripps.clustering.common.aplmodel.DataDependence;
import net.gripps.clustering.tool.Calc;
import net.gripps.environment.CPU;
import net.gripps.environment.Environment;
import java.util.Hashtable;
import java.util.*;
import net.gripps.grouping.HEFT.HEFTCandidateNodeSelectionManager;
import net.gripps.grouping.IndexInfo;

/**
 * Created by kanemih on 2016/01/23.
 */
public class CEFTCandidateNodeSelectionManager extends HEFTCandidateNodeSelectionManager {

    public CEFTCandidateNodeSelectionManager(BBTask apl, String file, Environment env) {
        super(apl, file, env);
    }

    /**
     *
     * @return
     */
    public Hashtable<Long, CPU> deriveCaindidateCPUMapByCEFT2(){

        CPU firstCPU = this.configureTaskPair();

        this.diffBlevel = 9999999;
        Iterator<AbstractTask> taskIte = this.apl.taskIerator();

        while(taskIte.hasNext()){
            AbstractTask task = taskIte.next();
            long maxBlevel = 0;

            if(task.getDsucList().size()>=2){
                AbstractTask maxTask = this.apl.findTaskByLastID(task.getBsuc().get(1));
                Iterator<DataDependence> dsucIte = task.getDsucList().iterator();
                AbstractTask nextTask = null;
                while(dsucIte.hasNext()){
                    DataDependence dsuc = dsucIte.next();
                    if(dsuc.getToID().get(1).longValue() == maxTask.getIDVector().get(1).longValue()){
                        continue;
                    } else{
                        AbstractTask tmpTask = this.apl.findTaskByLastID(dsuc.getToID().get(1));
                        long tmpBlevel = dsuc.getAve_comTime() +  tmpTask.getAve_procTime();
                        if(tmpBlevel >= maxBlevel){
                            maxBlevel = tmpBlevel;
                            nextTask = tmpTask;

                        }
                    }
                }
              //  if((this.CPSet.contains(maxTask.getIDVector().get(1)))&&(!this.CPSet.contains(nextTask.getIDVector().get(1)))){
                    long diftask = maxTask.getBlevelTotalTaskSize() - nextTask.getBlevelTotalTaskSize();
                    long difdata = maxTask.getBlevelTotalDataSize() - nextTask.getBlevelTotalDataSize();
                if(diftask == 0){
                    diftask = 1;
                }
                    double leftvalue = (-1)*Calc.getRoundedValue(difdata/(double)diftask);
                    double dblevel = this.beta_alpha - leftvalue;
                  // if (diftask * difdata < 0) {
                       // long dblevel = maxTask.getBlevel() - nextTask.getBlevel();
                        if((Math.abs(dblevel) <= this.diffBlevel) ){
                            this.diffBlevel = Math.abs(dblevel);
                            if(diftask < 0){
                                this.isDeltaRankWPositive = false;
                            }else{
                                this.isDeltaRankWPositive = true;
                            }

                            //this.task_k = maxTask;
                            // this.task_s = nextTask;
                            this.totalDifBlevelDataSize = difdata;
                            this.totalDifBlevelTaskSize = diftask;
                        }

              //     }
            //    }
                /*
                if(nextTask != null){
                    double maxDuration = this.calcDuratoinTime(maxTask);
                    double nextDuration = this.calcDuratoinTime(nextTask);
                    double dif = nextDuration - maxDuration;
                    if((dif >0)&&(dif >= this.diffBlevel)){
                        this.diffBlevel = dif;
                        this.task_k = maxTask;
                        this.task_s = nextTask;
                    }else{

                    }
                }
                */
            }else{
                continue;
            }
        }

     //   this.candidateCPUmap = this.mergeProcess();
        if(this.totalDifBlevelTaskSize==0){
            this.candidateCPUmap = this.deriveCandidateCPUMap();
        }else{
            this.candidateCPUmap = this.deriveCandidateCPUMapForScheduling();

        }
        return this.candidateCPUmap;

    }
    /**
     * 各タスクについて，2つの後続タスクA->B, A->CのBとCを逆転させる．
     * Bの入力通信時間＋実行時間とCの入力通信時間＋実行時間を比べて，C - Bの差分が
     * 最大の箇所をtask_k, task_sとする．
     * @return
     */
    public Hashtable<Long, CPU> deriveCaindidateCPUMapByCEFT(){
        Iterator<AbstractTask> taskIte = this.apl.taskIerator();

        while(taskIte.hasNext()){
            AbstractTask task = taskIte.next();
            long maxBlevel = 0;

            if(task.getDsucList().size()>=2){
                AbstractTask maxTask = this.apl.findTaskByLastID(task.getBsuc().get(1));
                Iterator<DataDependence> dsucIte = task.getDsucList().iterator();
                AbstractTask nextTask = null;
                while(dsucIte.hasNext()){
                    DataDependence dsuc = dsucIte.next();
                    if(dsuc.getToID().get(1).longValue() == maxTask.getIDVector().get(1).longValue()){
                        continue;

                    } else{
                         AbstractTask tmpTask = this.apl.findTaskByLastID(dsuc.getToID().get(1));
                        long tmpBlevel = dsuc.getAve_comTime() +  tmpTask.getAve_procTime();
                        if(tmpBlevel >= maxBlevel){
                            maxBlevel = tmpBlevel;
                            nextTask = tmpTask;

                        }
                    }
                }
                if(nextTask != null){
                    double maxDuration = this.calcDuratoinTime(maxTask);
                    double nextDuration = this.calcDuratoinTime(nextTask);
                    double dif = nextDuration - maxDuration;
                    if((dif >0)&&(dif >= this.diffBlevel)){
                        this.diffBlevel = dif;
                        this.task_k = maxTask;
                        this.task_s = nextTask;
                    }else{

                    }
                }

            }else{
                continue;
            }
        }

        this.candidateCPUmap = this.mergeProcess();
        return this.candidateCPUmap;



    }


}
