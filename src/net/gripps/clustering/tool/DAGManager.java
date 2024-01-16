package net.gripps.clustering.tool;

import net.gripps.clustering.common.aplmodel.BBTask;
import net.gripps.clustering.common.aplmodel.AbstractTask;
import net.gripps.clustering.common.aplmodel.DataDependence;
import net.gripps.clustering.common.Constants;

import java.io.*;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Author: H. Kanemitsu
 * Date: 2008/10/26
 */
public class DAGManager {
    private static DAGManager singleton;



    private DAGManager(){

    }

    public static DAGManager getInstance(){
        if(DAGManager.singleton == null){
            DAGManager.singleton = new DAGManager();
        }
        return DAGManager.singleton;
    }

    /**
     * APLをファイルに書き込みます．
     * @param apl
     */
    public void writeDAG(BBTask apl, String fileName){
        try{
            File file = new File(fileName);
            FileOutputStream stream = new FileOutputStream(file);
            OutputStreamWriter osr = new OutputStreamWriter(stream);
            BufferedWriter bw =  new BufferedWriter(osr);

            //まずはタスク数を取得
            int taskNum = apl.getTaskList().size();
            String taskNumStr = String.valueOf(taskNum);

            //一行目に，タスク数を書き込む
            bw.write(taskNumStr, 0, taskNumStr.length());
            bw.newLine();

            Iterator<AbstractTask> taskIte = apl.taskIerator();

            //各タスクに対するループ
            while(taskIte.hasNext()){
                AbstractTask task = taskIte.next();
                //タスクID
                long taskID = task.getIDVector().get(1).longValue();
                ///そして書き込む
                String TaskStr = taskID+" "+ task.getMaxWeight()+" "+"0";
                bw.write(TaskStr, 0, TaskStr.length());
                //改行
                bw.newLine();
                //先行タスク，後続タスクを取得
                StringBuffer strBuf = new StringBuffer();
                //先行タスクたちに対するループ
                Iterator<DataDependence> dpredIte = task.getDpredList().iterator();
                while(dpredIte.hasNext()){
                    DataDependence dpred = dpredIte.next();
                    strBuf.append(dpred.getFromID().get(1).longValue()+" "+dpred.getMaxDataSize()+" ");
                }

                strBuf.append("-1");
                bw.write(strBuf.toString(), 0, strBuf.toString().length());
                bw.newLine();

                StringBuffer strBuf2 = new StringBuffer();
                //後続タスクたちを取得
                Iterator<DataDependence> dsucIte = task.getDsucList().iterator();
                while(dsucIte.hasNext()){
                    DataDependence dsuc = dsucIte.next();
                    strBuf2.append(dsuc.getToID().get(1).longValue()+" "+dsuc.getMaxDataSize()+" ");
                }
                //そして-1を書き込む
                strBuf2.append("-1");
                bw.write(strBuf2.toString(), 0, strBuf2.toString().length());
                //改行
                bw.newLine();



                
            }
            //bw.newLine();
            //bw.write("-1");
            bw.close();

        }catch(Exception e){
            e.printStackTrace();

        }
    }

    /**
     * ファイルから，DAGをロードします．
     * @param fileName
     * @return
     */
    public BBTask readDAG(String fileName){
        try{
            File file = new File(fileName);
            FileInputStream fs = new FileInputStream(file);
            BufferedReader bf = new BufferedReader(new InputStreamReader(fs));
            //一行目を読んで，タスク数を取得
            String firstStr = bf.readLine();
            int taskNum = Integer.valueOf(firstStr);

            BBTask apl = new BBTask(Constants.TYPE_BASIC_BLOCK, 0, 0, 0);
            Vector<Long> id = new Vector<Long>();
            id.add(new Long(1));
            apl.setIDVector(id);

            String line;
            while((line = bf.readLine()) != null){
                StringTokenizer tk = new StringTokenizer(line, " ");
                //一行目
                String value = tk.nextToken();
                if(value.equals("-1")){
                    break;
                }
                //タスクID
                Vector<Long> idVector = new Vector();
                idVector.add(new Long(1));
                idVector.add(Long.valueOf(value));

                //タスクの重み
                value = tk.nextToken();
                long taskWeight = Long.valueOf(value).longValue();
                BBTask newTask = new BBTask(Constants.TYPE_BASIC_BLOCK, taskWeight, taskWeight, taskWeight);
                newTask.setIDVector(idVector);
                //2行目へ行く．
                line = bf.readLine();

                //先行タスクたち
                StringTokenizer tk2 = new StringTokenizer(line, " ");
                while(tk2.hasMoreElements()){
                    //先行タスクID
                    String str_predTaskID = tk2.nextToken();
                    if((str_predTaskID.equals("-1")) || (str_predTaskID == null)){
                        break;

                    }
                    Vector<Long> predTaskID = new Vector<Long>();
                    predTaskID.add(new Long(1));
                    predTaskID.add(Long.valueOf(str_predTaskID));

                    //次へ行く(先行タスクIDがあれば，次の値もあるはず)．
                    //辺の重み
                    long  predEdgeWeight = Long.valueOf(tk2.nextToken());
                    DataDependence dpred = new DataDependence(predTaskID, newTask.getIDVector(), predEdgeWeight,predEdgeWeight,predEdgeWeight);
                    newTask.addDpred(dpred);
                }

                //3行目へ行く．
                line = bf.readLine();
                StringTokenizer tk3 = new StringTokenizer(line, " ");
                while(tk3.hasMoreElements()){
                    //後続タスクID
                    String str_sucTaskID = tk3.nextToken();
                    if((str_sucTaskID.equals("-1"))||(str_sucTaskID == null)){
                        break;
                    }
                    Vector<Long> sucTaskID = new Vector<Long>();
                    sucTaskID.add(new Long(1));
                    sucTaskID.add(Long.valueOf(str_sucTaskID));
                    long sucEdgeWeight = Long.valueOf(tk3.nextToken());
                    DataDependence dsuc = new DataDependence(sucTaskID, newTask.getIDVector(), sucEdgeWeight,sucEdgeWeight,sucEdgeWeight);
                    newTask.addDsuc(dsuc);
                }
                newTask.setParentTask(id);
                apl.getTaskList().put(newTask.getIDVector().get(1), newTask);
            }
            return apl;


        }catch(Exception e){
            e.printStackTrace();
            return null;
        }

    }
}
