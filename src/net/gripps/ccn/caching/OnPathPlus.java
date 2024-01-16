package net.gripps.ccn.caching;

import net.gripps.ccn.CCNUtil;
import net.gripps.ccn.core.CCNContents;
import net.gripps.ccn.core.CCNRouter;
import net.gripps.ccn.core.Face;
import net.gripps.ccn.process.CCNMgr;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by Hidehiro Kanemitsu on 2020/07/24.
 */
public class OnPathPlus extends OnPathCaching{

    @Override
    public boolean chachingProcessIfNoPITEntry(CCNContents c, CCNRouter r) {
        return super.chachingProcessIfNoPITEntry(c, r);
    }

    @Override
    public boolean cachingProcess(CCNContents c, CCNRouter r) {
         super.cachingProcess(c, r);
         //さらに，rのFIBの/エントリ全てに対してキャッシュする．
        LinkedList<Face> fList = r.getFIBEntry().getTable().get("/");
        Iterator<Face> fIte = fList.iterator();
        while(fIte.hasNext()){
            Face face = fIte.next();
            if(face.getType() == CCNUtil.NODETYPE_ROUTER){
                Long id = face.getPointerID();
                CCNRouter router = CCNMgr.getIns().getRouterMap().get(id);
                router.getCSEntry().getCacheMap().put(c.getPrefix(), c);
            }
        }
         return true;
    }
}
