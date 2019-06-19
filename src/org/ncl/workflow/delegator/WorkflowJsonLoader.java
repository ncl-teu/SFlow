package org.ncl.workflow.delegator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.gripps.cloud.nfv.sfc.SFC;
import net.gripps.cloud.nfv.sfc.SFCGenerator;
import net.gripps.cloud.nfv.sfc.VNF;
import net.gripps.cloud.nfv.sfc.VNFCluster;
import net.gripps.clustering.common.aplmodel.CustomIDSet;
import net.gripps.clustering.common.aplmodel.DataDependence;
import org.ncl.workflow.comm.DataSendInfo;
import org.ncl.workflow.comm.FileSendInfo;
import org.ncl.workflow.comm.WorkflowJob;
import org.ncl.workflow.engine.Task;

import java.io.File;
import java.util.*;

/**
 * Created by Hidehiro Kanemitsu on 2019/05/17
 * Load each task as "Task" and "VNF" in the SFC.
 * Then both, i.e., "Task" and "VNF" is separately
 * managed in each Map object in the same ID for each
 * element.
 */
public class WorkflowJsonLoader {
    /**
     * Managed during only in the simulation process.
     */
    protected SFC sfc;

    protected WorkflowJob job;

    protected  JsonNode root;


    /**
     * Load job from the path.
     * @param path
     * @return
     */
    public SFC loadJob(String path){
        try {

            ObjectMapper job_mapper = new ObjectMapper();
            this.root = job_mapper.readTree(new File(path));
            HashMap<Long, JsonNode> jTaskMap = new HashMap<Long, JsonNode>();

            //# of tasks in the job.
            long tasknum = this.root.get("task_list").size();

            //APLを生成して，シングルトンにセットする．
            SFC apl = new SFC(-1, -1, -1, -1, -1, -1,
                    -1, null, new HashMap<Long, VNF>(), new HashMap<Long, VNFCluster>(), this.root.get("job_id").longValue(),
                    -1, -1);

            Vector<Long> id = new Vector<Long>();
            id.add(new Long(this.root.get("job_id").longValue()));
            apl.setIDVector(id);
            SFCGenerator.getIns().setSfc(apl);
            Iterator<JsonNode> nodeIte = this.root.get("task_list").iterator();
            this.job = new WorkflowJob(this.root.get("job_id").longValue(), new HashMap<Long, Task>());

            //VNF数分だけ，VNFインスタンスを生成する．
            //for (int i = 0; i < tasknum; i++) {
            while(nodeIte.hasNext()){
                JsonNode jvnf = nodeIte.next();
                long taskid = jvnf.get("task_id").longValue();
                long weight = jvnf.get("workload").longValue();
                int usage = jvnf.get("usage").intValue();
                int type = jvnf.get("type").intValue();
                String docker_tar = null;
                String docker_image = null;
                if(jvnf.has("docker-tarname")&&jvnf.has("docker-imagename")){
                    docker_tar = jvnf.get("docker-tarname").asText();
                     docker_image = jvnf.get("docker-imagename").asText();

                }

                VNF vnf = new VNF(type, weight, -1, -1, -1, null, usage);
                vnf =apl.addVNF(vnf, taskid);
                Task task = new Task(this.root.get("job_id").longValue(),taskid,
                        null,null,null,null,null,null );
                task.setDocker_image(docker_image);
                task.setDocker_tar(docker_tar);

                this.job.getTaskMap().put(taskid, task);
                jTaskMap.put(taskid, jvnf);

            }

            Iterator<JsonNode> nodeIte2 = jTaskMap.values().iterator();
            //for(int i=0;i<tasknum;i++){
            while(nodeIte2.hasNext()){
                JsonNode jvnf = nodeIte2.next();
                VNF vnf = apl.findVNFByLastID(jvnf.get("task_id").longValue());
                Task task = this.job.getTaskMap().get(vnf.getIDVector().get(1));

                LinkedList<Long> dpredList = new LinkedList<Long>();
                LinkedList<Long> dsucList = new LinkedList<Long>();

                // Following is the task construction
                String cmd = jvnf.get("cmd").asText();
                task.setCmd(cmd);
                LinkedList<FileSendInfo> fsiList = new LinkedList<FileSendInfo>();

                if(jvnf.has("out_file_list")){
                    int ofilelen = jvnf.get("out_file_list").size();
                    for(int j=0;j<ofilelen;j++){
                        JsonNode felement = jvnf.get("out_file_list").get(j);
                        int targetLen = felement.get("dest_task_id").size();
                        long size = felement.get("size").longValue();
                        LinkedList<Long> destList = new LinkedList<Long>();

                        for(int k=0;k<targetLen;k++){
                            long destid = felement.get("dest_task_id").get(k).longValue();
                            destList.add(destid);
                            dsucList.add(destid);
                            VNF sucVNF = apl.findVNFByLastID(destid);
                            DataDependence dd = new DataDependence(vnf.getIDVector(), sucVNF.getIDVector(), size,size,size);

                            sucVNF.addDpred(dd);
                            vnf.addDsuc(dd);
                            //Generate FileSendInfo.
                        }
                        FileSendInfo info = new FileSendInfo(felement.get("path").asText(),destList, felement.get("path").asText());
                        fsiList.add(info);
                    }

                }
                task.setOutFileInfoList(fsiList);

                DataSendInfo dinfo = new DataSendInfo(null, new LinkedList<Long>());

                if(jvnf.has("out_value")){
                    JsonNode valNode = jvnf.get("out_value");
                    int destIDLen = valNode.get("dest_task_id").size();
                    for(int m=0;m<destIDLen;m++){
                        long destID = valNode.get("dest_task_id").get(m).longValue();
                        long size = (long)valNode.get("size").doubleValue();
                        dinfo.setMsg(valNode.get("val").asText());
                        VNF sucVNF = apl.findVNFByLastID(destID);
                        DataDependence dd = new DataDependence(vnf.getIDVector(), sucVNF.getIDVector(), size,size,size);
                        sucVNF.addDpred(dd);
                        vnf.addDsuc(dd);
                        dinfo.getTargetIDList().add(destID);

                    }
                }
                task.setOutDataInfo(dinfo);

                if(vnf.getDpredList().isEmpty()){
                    apl.getStartVNFSet().add(vnf.getIDVector().get(1));
                }
                if(vnf.getDsucList().isEmpty()){
                    apl.getEndVNFSet().add(vnf.getIDVector().get(1));
                }


                CustomIDSet vnfSet = new CustomIDSet();
                vnfSet.add(vnf.getIDVector().get(1));

                VNFCluster cluster = new VNFCluster(vnf.getIDVector().get(1), null, vnfSet, vnf.getWorkLoad());
                vnf.setClusterID(cluster.getClusterID());


                apl.getVNFClusterMap().put(cluster.getClusterID(), cluster);
                if (vnf.getDpredList().isEmpty()) {
                    apl.getStartVNFSet().add(vnf.getIDVector().get(1));

                }

                if (vnf.getDsucList().isEmpty()) {
                    apl.getEndVNFSet().add(vnf.getIDVector().get(1));
                }
                task.setVnf(vnf);
            }
            this.sfc = apl;
            //this.assignDependencyProcess();

            return this.sfc;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return this.sfc;
    }

    public SFC getSfc() {
        return sfc;
    }

    public void setSfc(SFC sfc) {
        this.sfc = sfc;
    }

    public WorkflowJob getJob() {
        return this.job;
    }

    public void setJob(WorkflowJob job) {
        this.job = job;
    }

    public JsonNode getRoot() {
        return root;
    }

    public void setRoot(JsonNode root) {
        this.root = root;
    }
}
