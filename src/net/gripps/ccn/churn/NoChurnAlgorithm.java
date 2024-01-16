package net.gripps.ccn.churn;

import net.gripps.ccn.core.CCNRouter;
import net.gripps.ccn.fibrouting.BaseRouting;

import java.util.HashMap;

/**
 * Created by Hidehiro Kanemitsu on 2018/12/18.
 */
public class NoChurnAlgorithm extends BaseChurnResilienceAlgorithm {


    public NoChurnAlgorithm(BaseRouting routing) {
        super(routing);
    }

    @Override
    public boolean ccnJoin(CCNRouter r, HashMap<Long, CCNRouter> routerMap) {
        return false;
    }

    @Override
    public boolean ccnLeave(CCNRouter r, HashMap<Long, CCNRouter> routerMap) {
        return false;
    }
}
