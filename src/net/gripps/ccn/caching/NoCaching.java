package net.gripps.ccn.caching;

import net.gripps.ccn.core.CCNContents;
import net.gripps.ccn.core.CCNRouter;

public class NoCaching extends BaseCachingAlgorithm {

    @Override
    public boolean cachingProcess(CCNContents c, CCNRouter r) {
        return false;
    }

    @Override
    public boolean chachingProcessIfNoPITEntry(CCNContents c, CCNRouter r) {
        return false;
    }

    @Override
    public boolean chachingIFCSFULL(CCNContents c, CCNRouter r) {
        return false;
    }
}
