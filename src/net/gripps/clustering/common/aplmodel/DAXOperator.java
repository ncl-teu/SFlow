package net.gripps.clustering.common.aplmodel;

import net.gripps.clustering.common.Constants;
import net.gripps.clustering.tool.Calc;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import java.io.FileInputStream;
import java.util.*;
import java.io.File;

/**
 * Created by kanemih on 2016/01/12.
 */
public class DAXOperator extends AplOperator {
    private String fileName;
    private String directory = "dax/";

    private Document daxDoc;

    protected static DAXOperator singleton;

    protected Hashtable<String, Long> idTable;

    protected Hashtable<String, FileInfo> fileTable;

    /**
     * Map from task name to task.
     */
    protected Map<String, AbstractTask> mName2Task;
    /**
     * Map from task name to task runtime.
     */
    protected Map<String, Double> mName2Runtime;
    /**
     * Map from file name (data) to its size.
     */
    protected Map<String, Double> mName2Size;

    protected long taskNum;

    protected long totalTaskSize;

    protected double aveTasksize;

    protected long edgeNum;

    protected long totalDataSize;

    protected double aveDataSize;



    protected double defCCR;

    protected Hashtable<Long, JobInfo> multiList;

    protected String jobName;








    private DAXOperator() {
        super();
        this.idTable = new Hashtable<String, Long>();
        this.fileTable = new Hashtable<String, FileInfo>();
        this.taskNum = 0;
        this.totalTaskSize = 0;
        this.aveTasksize = 0;
        this.edgeNum = 0;
        this.totalDataSize = 0;
        this.aveDataSize = 0;
        this.defCCR = -1;
        this.multiList = new Hashtable<Long, JobInfo>();



    }

    private void  clear(){
        this.idTable.clear();
        this.fileTable.clear();
        this.taskNum = 0;
        this.totalTaskSize = 0;
        this.aveTasksize = 0;
        this.edgeNum = 0;
        this.totalDataSize = 0;
        this.aveDataSize = 0;
        this.defCCR = -1;
        //this.multiList = new LinkedList<BBTask>();


    }

    public Hashtable<Long, JobInfo> getMultiList() {
        return multiList;
    }

    public void setMultiList(Hashtable<Long, JobInfo> multiList) {
        this.multiList = multiList;
    }

    /**
     *
     * @param name
     * @param dataSize
     * @param fromID  output
     * @param toID  input
     */
    public void addFileInfo(String name, long dataSize, Long fromID, Long toID){
        if(this.fileTable.containsKey(name)){
            //もし含まれていれば，既存のものに追加する．
            FileInfo fInfo = this.fileTable.get(name);
            if(fromID == null){
                //toIDを追加する．
                fInfo.getToIDSet().add(toID);
            }
            if(toID == null){
                //fromIDを追加する．
                fInfo.getFromIDSet().add(fromID);
            }
        }else{
            //新規作成する．
            FileInfo newInfo = new FileInfo(name, dataSize);
            if(fromID == null){
                //toIDを追加する．
                newInfo.getToIDSet().add(toID);
            }
            if(toID == null){
                //fromIDを追加する．
                newInfo.getFromIDSet().add(fromID);
            }
            this.fileTable.put(name, newInfo);

        }

    }

    /**
        * @param filename
        */
       public void constructTask(String filename) {
           try {
               Properties prop = new Properties();
               //create input stream from file

               prop.load(new FileInputStream(filename));
               //レイヤの範囲
               this.setNumOfLayer_min(Long.valueOf(prop.getProperty("task.NumOfLayer.min")).longValue());
               this.setNumOfLayer_max(Long.valueOf(prop.getProperty("task.NumOfLayer.max")).longValue());
               this.defCCR = Double.valueOf(prop.getProperty("dax.ccr")).doubleValue();

               this.fileName = prop.getProperty("dax.filename");
             //  this.minTaskSize = Long.valueOf(prop.getProperty("dax.task.minsize")).longValue();
            //   this.minDataSize = Long.valueOf(prop.getProperty("dax.data.minsize")).longValue();

               //最上位レイヤ(第一層)におけるタスク数
               long tasknum = this.generateLongValue(this.getNumOfTask_min(), this.getNumOfTask_max());

               //APLを生成して，シングルトンにセットする．
               BBTask apl = new BBTask(Constants.TYPE_BASIC_BLOCK, 0, 0, 0);
               Vector<Long> id = new Vector<Long>();
               id.add(new Long(1));
               apl.setIDVector(id);
               DAXOperator.getInstance().setApl(apl);

               super.load(filename);
               //this.constructTask(filename);

               //スケールから，レイヤ数を算出する．
               //レイヤ数 == Log(スケール)
       /*        double log_layer = Integer.valueOf(this.scale).doubleValue();
               this.layer = (int)(Math.log(log_layer)/Math.log(2));

               //タスクを生成する
               for(int i=1 ; i<= this.layer;i++){
                   for(int j = 1; j <= this.scale; j++){
                       AbstractTask updatedTask = this.buildChildTask(true);
                       //Aplへタスクを追加する．このとき，タスクIDは付与されている．
                       FFTRealOperator.getInstance().getApl().addTask(updatedTask);

                   }
               }

               //最後に，ENDタスクを作る．
               AbstractTask updatedTask = this.buildChildTask(true);
                //Aplへタスクを追加する．このとき，タスクIDは付与されている．
               FFTRealOperator.getInstance().getApl().addTask(updatedTask);
*/

           } catch (Exception e) {
               e.printStackTrace();
           }
       }



    /**
     * The Factory Method as Singleton
     *
     * @return
     */
    public static DAXOperator getInstance() {

        if (DAXOperator.singleton == null) {
            DAXOperator.singleton = new DAXOperator();
        }
        return DAXOperator.singleton;
    }


    /**
     * DAXフォルダ内の，100コまでのタスクを持つファイルをランダムに読み込む．
     * そして，IDをふってMultipleDAGOperatorに渡す．
     * @param filename
     * @return
     */
    public BBTask generateMultipleDAGs(String filename){
        try{

            this.clear();
            Properties prop = new Properties();
            //create input stream from file
            prop.load(new FileInputStream(filename));
            //DAG数
            this.multipleNum =  Integer.valueOf(prop.getProperty("dax.multiple.num")).intValue();
            //CCR
            this.multipleCCRMin = Double.valueOf(prop.getProperty("dax.multiple.ccr.min")).doubleValue();

            this.multipleCCRMax = Double.valueOf(prop.getProperty("dax.multiple.ccr.max")).doubleValue();

            this.jobName = prop.getProperty("task.multiple.jobname");

            this.load(filename);
            long idx = 2;
            //this.multiplecurrentidx = 2;
            BBTask retApl = null;
            //一通りのタスクをセットしたら，次はセットする．
            MultipleDAGOperator multiple = new MultipleDAGOperator();
            //daxディレクトリを走査する．
            File file = new File("dax");
            File files[] = file.listFiles();
            //1000や997を含むファイルは削除する．
            int len = files.length;

            //複数DAG生成のためのループ
            for(int i=0;i<this.multipleNum;i++){
                int fileIndex = this.generateIntValue(0, len-1);
                double ccr = this.generateDoubleValue(this.multipleCCRMin, this.multipleCCRMax);

                //正常なファイルであれば，読み込む
                while(files[fileIndex].getName().indexOf("1000")!=-1 || files[fileIndex].getName().indexOf("997")!=-1 ){
                    fileIndex = this.generateIntValue(0, len-1);
                }
                this.multiplecurrentidx = idx;
                //一旦nullにする．

                DAXOperator.getInstance().setApl(null);

                BBTask apl = new BBTask(Constants.TYPE_BASIC_BLOCK, 0, 0, 0);
                Vector<Long> id = new Vector<Long>();
                id.add(new Long(1));
                apl.setIDVector(id);
                DAXOperator.getInstance().setApl(apl);


                //System.out.println(this.multiplecurrentidx);
               BBTask newDAG =  this.loadDAX(filename, files[fileIndex].getName(), this.multiplecurrentidx,ccr);
             //   BBTask newDAG2 =  this.loadDAX(filename, files[fileIndex].getName(), 1,ccr);

                Iterator<AbstractTask> tIte = newDAG.taskIerator();

                //BBTask orgDAG = this.loadDAX(filename, files[fileIndex].getName(), 1,ccr);
                //System.out.println("タスク数:"+newDAG.getTaskList().size());

/*
                System.out.println("---NEW START----");
                Iterator<AbstractTask> newIte = newDAG.taskIerator();
                while(newIte.hasNext()){
                    AbstractTask t = newIte.next();
                    System.out.println(t.getIDVector().get(1));
                }
                System.out.println("-----NEW END-------");

                System.out.println("-----SINGELE START-------");
                BBTask orgDAG = null;
                orgDAG = (BBTask)newDAG.deepCopy();

                Iterator<AbstractTask> newIte2 = orgDAG.taskIerator();
                while(newIte2.hasNext()){
                    AbstractTask t = newIte2.next();
                    System.out.println(t.getIDVector().get(1));
                }
                System.out.println("-----SINGELE END-------");
*/

                // this.multiList.put(idx, orgDAG);
               // BBTask orgDAG = null;
               // orgDAG = (BBTask)newDAG.deepCopy();
               // orgDAG = this.generateOrgDAG(orgDAG);
                String fName = files[fileIndex].getName();
                JobInfo info = new JobInfo(ccr, fName);

               this.multiList.put(newDAG.getEndTask().get(1), info);


                retApl = multiple.addDAG(newDAG);


                idx = newDAG.getEndTask().get(1)+1;

                //idx++;
                this.clear();

            }
            AplOperator.getInstance().setApl(retApl);

            return AplOperator.getInstance().getApl();

        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public BBTask generateOrgDAG(BBTask dag){
        //Startタスク集合を取得する．１
        Iterator<Long> startIte = dag.getStartTaskSet().iterator();
        long minID = Constants.MAXValue;
        long start_id = -1;
        Iterator<AbstractTask> tIte = dag.getTaskList().values().iterator();
        long minID_org = Constants.MAXValue;
     /*
        while (tIte.hasNext()){
            AbstractTask t = tIte.next();
            long sid = t.getIDVector().get(1).longValue();
            if(sid < minID_org){
                minID_org = sid;
            }

        }
        */
        CustomIDSet startSet = new CustomIDSet();

        while(startIte.hasNext()){
            long startID = startIte.next().longValue();
            startSet.add(new Long(startID));
            if(startID < minID){
                start_id = startID;
                minID = startID;
            }
        }
       // long minusValue = start_id - 1;
        long minusValue = start_id-1;
       // System.out.println(minusValue);
        //以降は，全てIDを更新する
        Iterator<AbstractTask> taskIte = dag.getTaskList().values().iterator();
        Hashtable<Long, AbstractTask> newTaskMap = new Hashtable<Long, AbstractTask>();

        while(taskIte.hasNext()){
            AbstractTask t = taskIte.next();
            long oldID = t.getIDVector().get(1);
            Long newID = new Long(oldID-minusValue);
           // System.out.println("old:"+oldID);
            t.getIDVector().set(1, newID);
            //先行，後続も更新する．
            Iterator<DataDependence> dpredIte = t.getDpredList().iterator();
            while(dpredIte.hasNext()){
                DataDependence dpred = dpredIte.next();
                long oldfromID = dpred.getFromID().get(1).longValue();
                dpred.getFromID().set(1, new Long(oldfromID-minusValue));
                long oldtoID = dpred.getToID().get(1).longValue();
                //System.out.println("id:"+oldID+"/toID:"+oldtoID);

                dpred.getToID().set(1, newID);
            }

            Iterator<DataDependence> dsucIte = t.getDsucList().iterator();
            while(dsucIte.hasNext()){
                DataDependence dsuc = dsucIte.next();
                long oldfromID = dsuc.getFromID().get(1).longValue();

                dsuc.getFromID().set(1, newID);
                long oldtoID = dsuc.getToID().get(1).longValue();
                dsuc.getToID().set(1, new Long(oldtoID-minusValue));
            }
            newTaskMap.put(newID, t);
        }

        dag.setStartTaskSet(new CustomIDSet());

        Iterator<Long> startTasks = startSet.iterator();
        while(startTasks.hasNext()){
            long sID = startTasks.next().longValue();
            dag.getStartTaskSet().add(new Long(sID-minusValue));


        }
        long oldEndID = dag.getEndTask().get(1);
        long newEndID = oldEndID - minusValue;
        AbstractTask eTask = dag.findTaskByLastID(oldEndID);

        //AbstractTask eTask = dag.getEndTask();
        //eTask.getIDVector().set(1, new Long(newEndID));
        //newTaskMap.remove(new Long(oldEndID));
        newTaskMap.put(newEndID, eTask);
        dag.setTaskList(null);
        dag.setTaskList(newTaskMap);
        dag.getEndTask().set(1, new Long(newEndID));



        //this.multiList.put(new Long(oldEndID), dag);
        return dag;

    }

    public BBTask loadDAX( String conf) {
        this.constructTask(conf);

        return this.loadDAX(conf, this.fileName, 1,0);


    }

    public BBTask loadDAXFromFile(String daxFile, String conf, double CCRValue){
        DAXOperator.getInstance().setApl(new BBTask(Constants.TYPE_BASIC_BLOCK, 0, 0, 0));
        this.clear();
        this.constructTask(conf);

        return this.loadDAXSingle(conf, daxFile, 1,CCRValue);

    }

    /**
     *
     * @param conf
     * @return
     */
    public BBTask loadDAXSingle( String conf, String daxfile, long startIDX, double setCCR) {
        //super();
        // this.load(conf);
        //まだ読み込んでいなければ読み込む
        //System.out.println(startIDX);
        if(this.fileName==null){
            this.constructTask(conf);
        }

        BBTask upperTask = DAXOperator.getInstance().getApl();
        Hashtable<Long, AbstractTask> tmpTaskList = upperTask.getTaskList();

        int tasknum = tmpTaskList.size();
        CustomIDSet startSet = new CustomIDSet();
        try {

            SAXBuilder builder = new SAXBuilder();
            Document dom = builder.build(new File(directory + daxfile));
            Element root = dom.getRootElement();
            List list = root.getChildren();
            int idIndex = 1;

            long taskID = startIDX;
            //STARTタスクを生成する．
            Vector<Long> startIDVector = new Vector<Long>();
            startIDVector.add(new Long(1));
            Long sID = new Long(taskID);
            startIDVector.add(sID);
            //タスク生成
            AbstractTask startTask = new BBTask(Constants.TYPE_BASIC_BLOCK, 0, 0, 0);
            //IDをセットする．
            startTask.setIDVector(startIDVector);
            DAXOperator.getInstance().getApl().addTaskSimply(startTask);
            DAXOperator.getInstance().getApl().getStartTaskSet().add(sID);


            for (Iterator it = list.iterator(); it.hasNext(); ) {
                Element node = (Element) it.next();

                //ノードがタスクである場合である場合
                if (node.getName().toLowerCase().equals("job")) {
                    this.taskNum++;
                    taskID++;
                    Long taskIDWrapper = new Long(taskID);

                    //ID設定部
                    Vector<Long> pID = new Vector<Long>();
                    pID.add(new Long(1));
                    Long nID = new Long(taskID);

                    pID.add(nID);
                    double runtime = 0.0;
                    long length = 0;
                    if (node.getAttributeValue("runtime") != null) {
                        String nodeTime = node.getAttributeValue("runtime");
                        runtime = 1000 * Double.parseDouble(nodeTime);
                        length = (long) runtime;
                    } else {
                        //Log.printLine("Cannot find runtime for " + nodeName + ",set it to be 0");
                    }

                    //タスク生成
                    AbstractTask newTask = new BBTask(Constants.TYPE_BASIC_BLOCK, length, length, length);
                    this.totalTaskSize+=length;
                    //IDをセットする．
                    newTask.setIDVector(pID);



                    //データ依存関係の前処理．この段階で，全ての辺の関係を保存しておく．
                    List<Element> jobChildren = node.getChildren();
                    Iterator<Element> jobCIte = jobChildren.iterator();
                    while(jobCIte.hasNext()){
                        Element e = jobCIte.next();
                        if(e.getName().toLowerCase().equals("uses")){
                            //file名を取得する．
                            String fname = e.getAttributeValue("file");
                            long datasize =Math.abs(Long.valueOf(e.getAttributeValue("size")).longValue());
                            String type = e.getAttributeValue("link");
                            //Toの場合
                            if(type.toLowerCase().equals("input")){
                                this.addFileInfo(fname, datasize, null, taskID);
                            }else{
                                //Fromの場合
                                this.addFileInfo(fname, datasize, taskID, null);
                            }

                        }
                    }
                    //ジョブにタスクをputする．
                    DAXOperator.getInstance().getApl().addTaskSimply(newTask);


                    // DAXOperator.getInstance().getApl().addTask(updatedTask);
                    //  System.out.println("ID:"+updatedTask.getIDVector().get(1));
                    String nodeName = node.getAttributeValue("id");
                    String nodeType = node.getAttributeValue("name");

                    this.idTable.put(nodeName, taskIDWrapper);

                    //制御フロー情報の追加（データサイズが0のデータ依存辺のこと）
                }else if (node.getName().toLowerCase().equals("child")) {
                    List pList = node.getChildren();
                    //１つのchildを取得する．
                    String childName = node.getAttributeValue("ref");
                    Long childID = this.idTable.get(childName);
                    //System.out.println(childID);
                    //childタスクを取得する．
                    AbstractTask childTask = DAXOperator.getInstance().getApl().findTaskByLastID(childID);

                    for (Iterator itc = pList.iterator(); itc.hasNext();) {
                        Element parent = (Element) itc.next();
                        //親のIDを取得する．
                        String parentName = parent.getAttributeValue("ref");
                        if (this.idTable.containsKey(parentName)) {
                            Long parentID = this.idTable.get(parentName);
                            //親タスクを取得する．
                            AbstractTask parentTask = DAXOperator.getInstance().getApl().findTaskByLastID(parentID);
                            DataDependence dd = new DataDependence(parentTask.getIDVector(), childTask.getIDVector(), 0, 0, 0);

                            parentTask.addDsucForce(dd);
                            childTask.addDpredForce(dd);
                        }

                    }


                }
            }
            Vector<Long> endIDVector = new Vector<Long>();
            endIDVector.add(new Long(1));
            Long eID = new Long(taskID+1);
            endIDVector.add(eID);
            //タスク生成
            AbstractTask endTask = new BBTask(Constants.TYPE_BASIC_BLOCK, 0, 0, 0);
            //IDをセットする．
            endTask.setIDVector(endIDVector);
            DAXOperator.getInstance().getApl().addTaskSimply(endTask);
            DAXOperator.getInstance().getApl().setEndTask(endTask.getIDVector());

            //依存関係を構築する．
            this.assignDD(startIDX);
            //BBTask task = DAXOperator.getInstance().getApl();
            //  System.out.println("END");

            //平均値を出す．
            this.aveTasksize = Calc.getRoundedValue(this.totalTaskSize/(double)this.taskNum);
            this.aveDataSize = Calc.getRoundedValue(this.totalDataSize/(double)this.edgeNum);
            double realCCR = Calc.getRoundedValue(this.aveDataSize/(double)this.aveTasksize);
            double usedCCR = 0;
            if(setCCR == 0){
                usedCCR = this.defCCR;
            }else{
                usedCCR = setCCR;
            }

            double CCR_rate = Calc.getRoundedValue(realCCR/(double)usedCCR);
            Iterator<AbstractTask> taskIte = DAXOperator.getInstance().getApl().getTaskList().values().iterator();
            while(taskIte.hasNext()){
                AbstractTask t = taskIte.next();
                long taskSize = t.getMaxWeight();
                long newTaskSize = Double.valueOf(taskSize*CCR_rate).longValue();
                t.setMaxWeight(newTaskSize);
                t.setAveWeight(newTaskSize);
                t.setMinWeight(newTaskSize);

            }
            long newMaxWeight = Double.valueOf(this.totalTaskSize*CCR_rate).longValue();
            DAXOperator.getInstance().getApl().setMaxWeight(newMaxWeight);
            double newaverw = Calc.getRoundedValue(newMaxWeight/(double)this.taskNum);




            return  DAXOperator.getInstance().getApl();

            //System.out.println("test");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return  DAXOperator.getInstance().getApl();
    }
        /**
         *
         * @param conf
         * @return
         */
    public BBTask loadDAX( String conf, String daxfile, long startIDX, double setCCR) {
        //super();
       // this.load(conf);
        //まだ読み込んでいなければ読み込む
        //System.out.println(startIDX);
        if(this.fileName==null){
            this.constructTask(conf);
        }

        BBTask upperTask = DAXOperator.getInstance().getApl();
        Hashtable<Long, AbstractTask> tmpTaskList = upperTask.getTaskList();

        int tasknum = tmpTaskList.size();
        CustomIDSet startSet = new CustomIDSet();
        try {

            SAXBuilder builder = new SAXBuilder();
            Document dom = builder.build(new File(directory + daxfile));
            Element root = dom.getRootElement();
            List list = root.getChildren();
            int idIndex = 1;

            long taskID = startIDX;
            //STARTタスクを生成する．
            Vector<Long> startIDVector = new Vector<Long>();
            startIDVector.add(new Long(1));
            Long sID = new Long(taskID);
            startIDVector.add(sID);
            //タスク生成
            AbstractTask startTask = new BBTask(Constants.TYPE_BASIC_BLOCK, 0, 0, 0);
            //IDをセットする．
            startTask.setIDVector(startIDVector);
            DAXOperator.getInstance().getApl().addTaskSimply(startTask);
            DAXOperator.getInstance().getApl().getStartTaskSet().add(sID);


            for (Iterator it = list.iterator(); it.hasNext(); ) {
                Element node = (Element) it.next();

                //ノードがタスクである場合である場合
                if (node.getName().toLowerCase().equals("job")) {
                    this.taskNum++;
                    taskID++;
                    Long taskIDWrapper = new Long(taskID);

                    //ID設定部
                    Vector<Long> pID = new Vector<Long>();
                    pID.add(new Long(1));
                    Long nID = new Long(taskID);

                    pID.add(nID);
                    double runtime = 0.0;
                    long length = 0;
                    if (node.getAttributeValue("runtime") != null) {
                        String nodeTime = node.getAttributeValue("runtime");
                        runtime = 1000 * Double.parseDouble(nodeTime);
                        length = (long) runtime;
                    } else {
                        //Log.printLine("Cannot find runtime for " + nodeName + ",set it to be 0");
                    }

                    //タスク生成
                    AbstractTask newTask = new BBTask(Constants.TYPE_BASIC_BLOCK, length, length, length);
                    this.totalTaskSize+=length;
                    //IDをセットする．
                    newTask.setIDVector(pID);



                    //データ依存関係の前処理．この段階で，全ての辺の関係を保存しておく．
                    List<Element> jobChildren = node.getChildren();
                    Iterator<Element> jobCIte = jobChildren.iterator();
                    while(jobCIte.hasNext()){
                        Element e = jobCIte.next();
                        if(e.getName().toLowerCase().equals("uses")){
                            //file名を取得する．
                            String fname = e.getAttributeValue("file");
                            long datasize =Math.abs(Long.valueOf(e.getAttributeValue("size")).longValue());
                            String type = e.getAttributeValue("link");
                            //Toの場合
                            if(type.toLowerCase().equals("input")){
                                this.addFileInfo(fname, datasize, null, taskID);
                            }else{
                                //Fromの場合
                                this.addFileInfo(fname, datasize, taskID, null);
                            }

                        }
                    }
                    //ジョブにタスクをputする．
                    DAXOperator.getInstance().getApl().addTaskSimply(newTask);


                   // DAXOperator.getInstance().getApl().addTask(updatedTask);
                  //  System.out.println("ID:"+updatedTask.getIDVector().get(1));
                    String nodeName = node.getAttributeValue("id");
                    String nodeType = node.getAttributeValue("name");

                    this.idTable.put(nodeName, taskIDWrapper);

                 //制御フロー情報の追加（データサイズが0のデータ依存辺のこと）
                }else if (node.getName().toLowerCase().equals("child")) {
                    List pList = node.getChildren();
                    //１つのchildを取得する．
                    String childName = node.getAttributeValue("ref");
                    Long childID = this.idTable.get(childName);
                    //System.out.println(childID);
                    //childタスクを取得する．
                    AbstractTask childTask = DAXOperator.getInstance().getApl().findTaskByLastID(childID);

                    for (Iterator itc = pList.iterator(); itc.hasNext();) {
                           Element parent = (Element) itc.next();
                           //親のIDを取得する．
                           String parentName = parent.getAttributeValue("ref");
                           if (this.idTable.containsKey(parentName)) {
                               Long parentID = this.idTable.get(parentName);
                               //親タスクを取得する．
                               AbstractTask parentTask = DAXOperator.getInstance().getApl().findTaskByLastID(parentID);
                               DataDependence dd = new DataDependence(parentTask.getIDVector(), childTask.getIDVector(), 0, 0, 0);

                               parentTask.addDsucForce(dd);
                               childTask.addDpredForce(dd);
                           }

                       }


                }
            }
            Vector<Long> endIDVector = new Vector<Long>();
            endIDVector.add(new Long(1));
            Long eID = new Long(taskID+1);
            endIDVector.add(eID);
            //タスク生成
            AbstractTask endTask = new BBTask(Constants.TYPE_BASIC_BLOCK, 0, 0, 0);
            //IDをセットする．
            endTask.setIDVector(endIDVector);
            DAXOperator.getInstance().getApl().addTaskSimply(endTask);
            DAXOperator.getInstance().getApl().setEndTask(endTask.getIDVector());

            //依存関係を構築する．
            this.assignDD(startIDX);
            //BBTask task = DAXOperator.getInstance().getApl();
          //  System.out.println("END");

            //平均値を出す．
            this.aveTasksize = Calc.getRoundedValue(this.totalTaskSize/(double)this.taskNum);
            this.aveDataSize = Calc.getRoundedValue(this.totalDataSize/(double)this.edgeNum);
            double realCCR = Calc.getRoundedValue(this.aveDataSize/(double)this.aveTasksize);
            double usedCCR = 0;
            if(setCCR == 0){
                usedCCR = this.defCCR;
            }else{
                usedCCR = setCCR;
            }

            double CCR_rate = Calc.getRoundedValue(realCCR/(double)usedCCR);
            Iterator<AbstractTask> taskIte = DAXOperator.getInstance().getApl().getTaskList().values().iterator();
            while(taskIte.hasNext()){
                AbstractTask t = taskIte.next();
                long taskSize = t.getMaxWeight();
                long newTaskSize = Double.valueOf(taskSize*CCR_rate).longValue();
                t.setMaxWeight(newTaskSize);
                t.setAveWeight(newTaskSize);
                t.setMinWeight(newTaskSize);

            }
            long newMaxWeight = Double.valueOf(this.totalTaskSize*CCR_rate).longValue();
            DAXOperator.getInstance().getApl().setMaxWeight(newMaxWeight);
            double newaverw = Calc.getRoundedValue(newMaxWeight/(double)this.taskNum);




            return  DAXOperator.getInstance().getApl();

            //System.out.println("test");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return  DAXOperator.getInstance().getApl();
    }

    /**
     * 実際のデータ依存を構築する．
     * fileTable<ファイルID, FileInfo>の形式であり，さらにFileInfoには
     * fromリスト，toListが含まれている．つまり，
     * ファイルIDというキーのリストのイテレーションを行い，
     * その中で
     */
    public void assignDD(long idx){
        //aplを保存しておく．
        BBTask apl = DAXOperator.getInstance().getApl();
        Enumeration<String> fIDEnum = this.fileTable.keys();
        Vector<Long> startVec = new Vector<Long>();
        startVec.add(new Long(1));
        startVec.add(new Long(idx));

        //1ファイル単位のループ
        while(fIDEnum.hasMoreElements()){
            String id = fIDEnum.nextElement();
            FileInfo info = this.fileTable.get(id);
            //output(From)は，0 or 1である．
            if(info.getFromIDSet().isEmpty()){
                //もしFromがないのであれば，STARTタスクをFROMにする．
                Iterator<Long> toIte = info.getToIDSet().iterator();
                while(toIte.hasNext()){
                    Long toID = toIte.next();

                    Vector<Long> toVec = new Vector<Long>();
                    toVec.add(new Long(1));
                    toVec.add(toID);
                    DataDependence dd = new DataDependence(
                            startVec,toVec, info.getDataSize(), info.getDataSize(), info.getDataSize());
                    AbstractTask startTask = apl.findTaskByLastID(startVec.get(1));
                    AbstractTask toTask = apl.findTaskByLastID(toID);
                    this.edgeNum++;
                    this.totalDataSize+=info.getDataSize();

                    startTask.addDsucForce(dd);
                    toTask.addDpredForce(dd);


                }
            }else{
                //以降は，From(output)があることが前提で，ただ1つ．
                Long fromID = info.getFromIDSet().getList().getFirst();
                Vector<Long> fromVec = new Vector<Long>();
                fromVec.add(new Long(1));
                fromVec.add(fromID);
                //to(input)が空であれば，これはFrom -> ENDタスクというようにする．
                if(info.getToIDSet().isEmpty()){
                    Vector<Long> endVec = apl.getEndTask();
                    AbstractTask fromTask = apl.findTaskByLastID(fromID);
                    AbstractTask endTask = apl.findTaskByLastID(endVec.get(1));
                    DataDependence dd = new DataDependence(fromVec, endVec, info.getDataSize(), info.getDataSize(), info.getDataSize());
                    fromTask.addDsucForce(dd);
                    endTask.addDpredForce(dd);
                    this.edgeNum++;
                    this.totalDataSize+=info.getDataSize();


                }else{
                    //空でなければ，そのまま通常どおりデータ依存辺をセットする．
                    Iterator<Long> toIDIte = info.getToIDSet().iterator();
                    while(toIDIte.hasNext()){
                        Long toID = toIDIte.next();
                        Vector<Long> toVec = new Vector<Long>();
                        toVec.add(new Long(1));
                        toVec.add(toID);

                        AbstractTask fromTask = apl.findTaskByLastID(fromID);
                        AbstractTask toTask = apl.findTaskByLastID(toID);

                        DataDependence dd = new DataDependence(fromVec, toVec, info.getDataSize(), info.getDataSize(), info.getDataSize());
                        fromTask.addDsucForce(dd);
                        toTask.addDpredForce(dd);
                        this.edgeNum++;
                        this.totalDataSize+=info.getDataSize();

                    }
                }
            }
            //System.out.println("ID:"+id+"/ToNum:"+info.getToIDSet().getList().size());
        }

    }

    public void constractDAGFromDAX() {
        // this.daxDoc.get
    }


    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public Document getDaxDoc() {
        return daxDoc;
    }

    public void setDaxDoc(Document daxDoc) {
        this.daxDoc = daxDoc;
    }
}
