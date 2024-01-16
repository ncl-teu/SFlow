package net.gripps.ccn.breadcrumbs;

import net.gripps.ccn.core.CCNContents;
import net.gripps.ccn.core.CCNRouter;
import net.gripps.ccn.core.InterestPacket;

public class NoBreadCrumbsAlgorithm extends BaseBreadCrumbsAlgorithm {

    @Override
    public boolean createBC(CCNRouter r, CCNContents c) {
        return false;
    }

    @Override
    public boolean forwardRequestByBC(CCNRouter r, InterestPacket p) {
        return false;
    }

    @Override
    public boolean forwardBCData(CCNRouter r, CCNContents c) {
        return false;
    }
}
