package org.ncl.workflow.delegator;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.gripps.cloud.core.VM;
import net.gripps.cloud.nfv.NFVEnvironment;
import net.gripps.cloud.nfv.sfc.SFC;
import net.gripps.cloud.nfv.sfc.VNF;
import org.ncl.workflow.comm.WorkflowJob;
import org.ncl.workflow.util.NCLWUtil;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Iterator;

/**
 * Created by Hidehiro Kanemitsu on 2020/06/01
 * スケジューリング結果をJsonファイルへ書き出します．
 *
 */
public class SchedGenerator {


    private WorkflowJsonLoader jobLoader;

    /**
     * スケジューリング済みのSFC
     */
    protected SFC sfc;

    /**
     * Jsonファイル内のroot
     */
    protected JsonNode root;

    /**
     * 書き出し先のPath
     */
    protected String writePath;

    /**
     * Cloud環境
     */
    protected NFVEnvironment env;

    public SchedGenerator(WorkflowJsonLoader loader, SFC sfc, String writePath, NFVEnvironment env) {
        this.jobLoader = loader;
        this.root = this.jobLoader.getRoot();
        this.sfc = sfc;
        this.writePath = writePath;
        this.env = env;
    }

    private String getJSONFromObj(WorkflowJsonLoader  obj) {

        String json = null;
        ObjectMapper mapper = new ObjectMapper();
        try {
            json = mapper.writeValueAsString(obj.root);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return json;
    }

    public String process(){
        try{
            Writer out = new PrintWriter(this.writePath);
            JsonFactory jsonFactory = new JsonFactory();
            //createJsonGenerator->createGenerator
            JsonGenerator gen = jsonFactory.createGenerator(out);


            JsonNode task_list = this.root.get("task_list");
            Iterator<JsonNode> taskIte = task_list.iterator();
            while(taskIte.hasNext()){
                JsonNode task = taskIte.next();
                Long task_id = task.get("task_id").longValue();
                VNF vnf = this.sfc.findVNFByLastID(task_id);
                String  vcpuID = vnf.getvCPUID();
                VM vm = NCLWUtil.findVM(this.env, vcpuID);

                //taskに，割当先を追加する．
                ((ObjectNode)task).put("ip", vm.getIpAddr());
                ((ObjectNode)task).put("vm_id", vm.getVMID());

                ((ObjectNode)task).put("vcpu_id", vcpuID);


            }
            String json = getJSONFromObj(this.jobLoader);
            gen.writeString(json);
            gen.close();

        }catch(Exception  e){
            e.printStackTrace();
        }
        return this.writePath;


    }


    public WorkflowJsonLoader getJobLoader() {
        return jobLoader;
    }

    public void setJobLoader(WorkflowJsonLoader jobLoader) {
        this.jobLoader = jobLoader;
    }

    public SFC getSfc() {
        return sfc;
    }

    public void setSfc(SFC sfc) {
        this.sfc = sfc;
    }

    public JsonNode getRoot() {
        return root;
    }

    public void setRoot(JsonNode root) {
        this.root = root;
    }

    public String getWritePath() {
        return writePath;
    }

    public void setWritePath(String writePath) {
        this.writePath = writePath;
    }

    public NFVEnvironment getEnv() {
        return env;
    }

    public void setEnv(NFVEnvironment env) {
        this.env = env;
    }
}
