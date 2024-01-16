package net.gripps.ccn.icnsfc.routing;

import net.gripps.ccn.core.CCNNode;
import net.gripps.ccn.core.CCNRouter;
import net.gripps.ccn.core.InterestPacket;
import net.gripps.ccn.icnsfc.AutoUtil;
import net.gripps.ccn.icnsfc.core.AutoEnvironment;
import net.gripps.ccn.icnsfc.process.AutoSFCMgr;
import net.gripps.cloud.core.ComputeHost;
import net.gripps.cloud.core.VCPU;
import net.gripps.cloud.core.VM;
import net.gripps.cloud.nfv.sfc.SFC;
import net.gripps.cloud.nfv.sfc.VNF;
import org.ncl.workflow.util.NCLWUtil;

import java.util.HashMap;
import java.util.Iterator;

public class MWRouting extends AutoRouting{
    public MWRouting() {
    }

    public MWRouting(HashMap<Long, CCNNode> nodeMap, HashMap<Long, CCNRouter> routerMap) {
        super(nodeMap, routerMap);
    }

    @Override
    /**
     * CCNNodeにおいて，Interstパケットの転送先を決める
     * 要実装．
     * @param rMap: 検索対象のルータ集合
     * @param packet: これから送信するInterestパケット．この中に，SFCが入っている．
     *
     *
     */
    public CCNRouter selectRouter(HashMap<Long, CCNRouter> rMap, InterestPacket packet) {
        AutoEnvironment env = AutoSFCMgr.getIns().getEnv();
        SFC sfc = (SFC)packet.getAppParams().get(AutoUtil.SFC_NAME);


        VNF endTask = sfc.findVNFByLastID((long)sfc.getVnfMap().size());
        //Name endPrefix = NclwNFDMgr.getIns().createPrefix(endTask, null);
        //まず，FIBからエントリを取得する．NFDTaskを生成しなければならない．
        //FIBから選ぶか？それともvCPUマップから選ぶか？どちらにしても，双方で同期が必要となる．
        //指定のprefixから選ぶ，という意味では，まずはFIBから取得する必要がある．
        //それから，vCPUマップから選ぶスタイル．
        CCNRouter retRouter = (CCNRouter)this.findVM(AutoSFCMgr.getIns().getEnv(), endTask.getvCPUID());
        if(retRouter == null){
            System.out.println();
        }

        return retRouter;



    }


}
