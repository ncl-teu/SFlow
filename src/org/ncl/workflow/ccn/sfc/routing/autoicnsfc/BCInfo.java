package org.ncl.workflow.ccn.sfc.routing.autoicnsfc;

import net.gripps.cloud.core.VM;

import java.io.Serializable;

/**
 * Created by Hidehiro Kanemitsu on 2020/03/01.
 * 初期設定時から，送る情報です．
 * 自身の性能情報，位置情報を知らせるために必要です．
 */
public class BCInfo implements Serializable {

    /**
     * 自身をVMとして，
     * - 所属する物理ホストのIPアドレス（ローカリティを考慮するため）
     *   -> これ，わかんの？
     * - vCPU(番号づけする）情報: MIPS,
     * - RAM: 容量
     * - BW: 帯域幅
     * をBroadCastする．
     */
    private VM own;

 
}
