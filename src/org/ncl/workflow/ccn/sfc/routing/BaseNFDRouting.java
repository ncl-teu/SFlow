package org.ncl.workflow.ccn.sfc.routing;

import com.intel.jndn.forwarder.api.Face;
import net.named_data.jndn.Name;

/**
 * Created by Hidehiro Kanemitsu on 2019/11/19
 */
public abstract class BaseNFDRouting  {

    protected RIB Rib;

    /**
     * 指定したNameで，適切なFaceを返す．
     * @param name
     * @return
     */
    protected abstract Face findFace(Name name);

    /**
     * 指定した宛先IPに対応するFaceを返す．
     * @param destIP
     * @return
     */
    protected abstract Face findFace(String destIP);



}
