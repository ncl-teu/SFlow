package net.gripps.ccn.fibrouting.nlsr;

import net.gripps.ccn.core.CCNNode;
import net.gripps.ccn.core.CCNRouter;
import net.gripps.ccn.core.Face;
import net.gripps.ccn.core.InterestPacket;
import net.gripps.ccn.fibrouting.BaseRouting;

import java.util.HashMap;

public class NLSRRouting extends BaseRouting {

    public NLSRRouting() {
    }

    public NLSRRouting(HashMap<Long, CCNNode> nodeMap, HashMap<Long, CCNRouter> routerMap) {
        super(nodeMap, routerMap);
    }

    @Override
    public long calcID(long index) {
        return 0;
    }

    @Override
    public long getNextID(long id) {
        return 0;
    }

    @Override
    public void buildFIBProcess() {

    }

    @Override
    public void buildFaces() {

    }

    @Override
    public CCNRouter selectRouter(HashMap<Long, CCNRouter> rMap, InterestPacket packet) {
        return null;
    }

    @Override
    public Long addFaceToFIBAsNewEntry(String prefix, CCNRouter router) {
        return null;
    }

    @Override
    public long getNextRouterIDIfInterestLoop(InterestPacket p, Face f, CCNRouter router) {
        return 0;
    }
}
