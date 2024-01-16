package net.gripps.clustering.common.aplmodel;

import java.util.Vector;

/**
 * LabelTo毎に一つ作られる．
 * Author: Hidehiro Kanemitsu<br>
 * Date: 2007/07/30
 */
public class CDMap {
    private Vector<Long> labelFrom;
    private Vector<CDLabelToBean> labelList;



    /**
     *
     * @param cd
     */
    public CDMap(ControlDependence cd,long max, long ave, long min){
        this.labelFrom = cd.getLabelFromID();
        CDLabelToBean bean = new CDLabelToBean(max,ave,min, cd.getLabelToID());
        this.setLabelList(new Vector<CDLabelToBean>());
        this.getLabelList().add(bean);


    }

    public Vector<Long> getLabelFrom() {
        return labelFrom;
    }

    public void setLabelFrom(Vector<Long> labelFrom) {
        this.labelFrom = labelFrom;
    }

    public void reflectLabelFromAndTo(ControlDependence cd, long max, long ave, long min){
        //まずは，labelToのBeanを探す．
        Vector<CDLabelToBean> toList = this.getLabelList();
        //labelToのサイズ
        int size = toList.size();
        boolean found = false;
        for(int i=0;i<size;i++){
            CDLabelToBean bean = toList.get(i);
            //labelToがすでに存在すれば，そいつの命令数を加算する．
            if(AplOperator.getInstance().isIDEqual(bean.getLabelToID(),cd.getLabelToID())){
                long tmpMax = bean.getMax();
                bean.setMax(tmpMax+max);

                long tmpAve = bean.getAve();
                bean.setAve(tmpAve+ave);

                long tmpMin = bean.getMin();
                bean.setMin(tmpMin + min);

                //beanを反映する。
                this.getLabelList().set(i,bean);
                found = true;
                break;
            }
        }

        if(found){

        }else{
            //labelToが無かった場合は，beanを新規生成してAddする．
            CDLabelToBean bean = new CDLabelToBean(max,ave,min,cd.getLabelToID());
            this.getLabelList().add(bean);
        }
    }


    public Vector<CDLabelToBean> getLabelList() {
        return labelList;
    }

    public void setLabelList(Vector<CDLabelToBean> labelList) {
        this.labelList = labelList;
    }
}
