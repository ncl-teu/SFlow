package net.gripps.clustering.common.aplmodel;

import java.util.Vector;

/**
 * Author: Hidehiro Kanemitsu<br>
 * Date: 2007/07/30
 */
public class CDMapList {
    private Vector<CDMap> mapList;

    public CDMapList(){
        this.setMapList(new Vector<CDMap>());
    }

    /**
     *
     * @param cd
     * @param max
     * @param ave
     * @param min
     */
    public void addCDMap(ControlDependence cd, long max, long ave, long min){
        //もし，LabelFromに同じものがなければ足す．
        int idx = this.findLabelFrom(cd.getLabelFromID());
        //もし見つかれば，そのインデックスに反映する．
        if(idx != -1){
            CDMap org_map = this.getMapList().get(idx);
            this.getMapList().set(idx,this.reflectMap(org_map,cd, max, ave, min));
        }else{
            //labelFromが無ければ，新規追加する．
            //CDMapを新規生成するということは，labelFrom, labelToが新たに作られるということである．
            CDMap map = new CDMap(cd, max,ave,min);
            this.getMapList().add(map);
        }
    }

    /**
     *
     * @param org_map
     * @param cd
     * @param max
     * @param ave
     * @param min
     * @return
     */
    private CDMap reflectMap(CDMap org_map, ControlDependence cd, long max, long ave, long min){
        org_map.reflectLabelFromAndTo(cd,max,ave,min);
        //labelTo毎に分かれているので，
        return org_map;

    }

    /**
     * 指定インデックス(labelFrom)における，Maxの命令数を決定する．
     * @param idx
     * @param mode
     * @return
     */
    public long generateInstrucationSum(int idx, int mode){
        //指定Fromのマップを取得する
        CDMap map = this.getMapList().get(idx);
        //Toのリストを取得する
        Vector<CDLabelToBean> toList = map.getLabelList();
        int size = toList.size();
        long value =0;
        switch(mode){
            case 1://Max
                long max = 0;
                for(int i=0;i<size;i++){
                    CDLabelToBean bean = toList.get(i);
                    long tmpMax = bean.getMax();
                    if(max < tmpMax){
                        max = tmpMax;
                    }

                }
                value = max;
                break;
            case 2: //Average
                long ave = 0;
                for(int i=0;i<size;i++){
                    CDLabelToBean bean = toList.get(i);
                    ave += bean.getAve();
                }
                value = ave/size;
                break;
            case 3: //Min
                long min = toList.get(0).getMin();
                for(int i=0;i<size;i++){
                    CDLabelToBean bean = toList.get(i);
                    long tmpMin = bean.getMin();
                    if(min > tmpMin){
                        min = tmpMin;
                    }

                }
                value = min;
                break;

        }

        return value;

    }
    /**
     *
     * @param fromID
     * @return
     */
    public int findLabelFrom(Vector<Long> fromID){
        int size = getMapList().size();
        int idx = 0;
        boolean found = false;
        for(int i=0;i<size;i++){
            CDMap map = this.getMapList().get(i);
            if(AplOperator.getInstance().isIDEqual(fromID, map.getLabelFrom())){
                found = true;
                idx = i;
                break;
            }
        }
        if(found){
            return idx;
        }else{
            return -1;
        }


    }

    public Vector<CDMap> getMapList() {
        return mapList;
    }

    public void setMapList(Vector<CDMap> mapList) {
        this.mapList = mapList;
    }
}
