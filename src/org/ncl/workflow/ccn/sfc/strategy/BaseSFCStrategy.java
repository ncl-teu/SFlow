package org.ncl.workflow.ccn.sfc.strategy;

import com.intel.jndn.forwarder.api.Face;
import com.intel.jnfd.deamon.fw.BestRouteStrategy2;
import com.intel.jnfd.deamon.fw.ForwardingPipeline;
import com.intel.jnfd.deamon.fw.RetxSuppression;
import com.intel.jnfd.deamon.table.fib.FibEntry;
import com.intel.jnfd.deamon.table.fib.FibNextHop;
import com.intel.jnfd.deamon.table.pit.PitEntry;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;

import java.util.List;

/**
 * Created by Hidehiro Kanemitsu on 2019/11/15
 */
public class BaseSFCStrategy extends BestRouteStrategy2 {

    public static final Name STRATEGY_NAME
            = new Name("ndn:/localhost/nfd/strategy/best-route/%SFC%00");

    public BaseSFCStrategy(ForwardingPipeline forwarder, Name name) {
        super(forwarder, name);
    }

    public BaseSFCStrategy(ForwardingPipeline forwarder) {
        super(forwarder);
    }





}
