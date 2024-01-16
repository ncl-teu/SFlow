package net.gripps.ccn.fibrouting;

import net.gripps.ccn.CCNUtil;
import net.gripps.ccn.core.CCNNode;
import net.gripps.ccn.core.CCNRouter;
import net.gripps.ccn.core.Face;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by Hidehiro Kanemitsu on 2019/10/30
 * BA (Barabasi-Albertモデル）において，Chordのルーティングを動作
 * させます．
 * - NextIDは変更なし（ID的に隣り合うものは追加しておく）
 * - buildFacesにおいて，2の累乗離れたものを設定するのではなく，
 *   確率に従って配備する．
 */
public class ChordOnBARouting extends ChordDHTRouting {



    public ChordOnBARouting(HashMap<Long, CCNNode> nodeMap, HashMap<Long, CCNRouter> routerMap) {
        super(nodeMap, routerMap);
    }

    public ChordOnBARouting() {
        super();
    }

    @Override
    public void buildFaces() {
       super.buildFaces();
        Iterator<Long> idIte = this.idSet.iterator();
        Iterator<Long> idIte2 = this.idSet.iterator();


        long totalDegree = 0;
        //現状のトータルの次数を計算する．
        while(idIte.hasNext()){
            Long id = idIte.next();
            CCNRouter r = this.routerMap.get(id);
            totalDegree += r.getFace_routerMap().size();
        }

        //実際にFaceを確率で設定する．
        while(idIte2.hasNext()){
            Long id = idIte2.next();
            Iterator<Long> anotherIdIte = this.idSet.iterator();
            //当該ルータ
            CCNRouter r = this.routerMap.get(id);
            while(anotherIdIte.hasNext()){
                Long id2 = anotherIdIte.next();
                CCNRouter r2 = this.routerMap.get(id2);
                //確率を計算する．
                double p = CCNUtil.getRoundedValue((double)r2.getFace_routerMap().size() / (double)totalDegree) * 100;
                double random = Math.random() * 100;
                if(random <= p){
                    //設定する場合
                    Face f1 = new Face(null, r2.getRouterID(), CCNUtil.NODETYPE_ROUTER);
                    r.addFace(f1, r.getFace_routerMap());

                    Face f2 = new Face(null, r.getRouterID(), CCNUtil.NODETYPE_ROUTER);
                    r2.addFace(f2, r2.getFace_routerMap());

                    totalDegree ++;
                }else{
                    //設定しない場合

                }
            }




        }




    }
}
