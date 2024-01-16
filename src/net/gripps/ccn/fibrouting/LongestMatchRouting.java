package net.gripps.ccn.fibrouting;

import net.gripps.ccn.CCNUtil;
import net.gripps.ccn.core.*;
import net.gripps.ccn.process.CCNMgr;
import net.gripps.clustering.tool.Calc;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by Hidehiro Kanemitsu on 2020/06/17.
 */
public class LongestMatchRouting extends BaseRouting {

    public LongestMatchRouting() {
        super();
    }

    public LongestMatchRouting(HashMap<Long, CCNNode> nodeMap, HashMap<Long, CCNRouter> routerMap) {
        super(nodeMap, routerMap);
    }

    @Override
    /**
     * index値を返すようにする．
     */
    public long calcID(long index) {
        //long rIDX = CCNUtil.ccn_node_num + index;
        return index;
    }

    @Override
    public long getNextID(long id) {
        return 0;
    }

    /**
     * - 各ノードが持つコンテンツについて，そのprefixをランダムに選ばれたルータのFIBへ
     * 登録する．
     * -
     */
    @Override
    public void buildFIBProcess() {
        Iterator<CCNNode> nIte = this.nodeMap.values().iterator();
        HashMap<String, Integer> cMap = new HashMap<String, Integer>();
        //まずはルータどうしのFIBへの登録（default prefix)
        Iterator<CCNRouter> aRIte = this.routerMap.values().iterator();
        while(aRIte.hasNext()){
            CCNRouter ar = aRIte.next();
            Iterator<Face> afIte = ar.getFace_routerMap().values().iterator();
            while(afIte.hasNext()){
                Face af = afIte.next();
                ar.getFIBEntry().addFace("/", af);
                //ar.getFace_routerMap().put(af.getPointerID(), af);
            }
        }

        //特定のprefix登録.
        //ノード<->ルータのFIB登録
        while(nIte.hasNext()){
            CCNNode node = nIte.next();
            int  rLen = node.getRouterMap().size();
            Object[] routers = node.getRouterMap().values().toArray();

            Iterator<String> prefixIte = node.getOwnContentsMap().keySet().iterator();
            //同一prefixの次数: ランダムで決定
            //一つ上の階層までに区切って，その同一prefixの次数: ランダムで決定．

            //FIBの冗長度を決める．
            int fib_dup_num = CCNUtil.genInt2(CCNUtil.ccn_fib_duplicate_num_min,
                    CCNUtil.ccn_prefix_degree_min, 1,
                    CCNUtil.ccn_prefix_degree_max);
            while(prefixIte.hasNext()){
                //当該ノードが持つ1つのコンテンツprefix．
                String prefix = prefixIte.next();
                String subPrefix = prefix;

                //まだ決めていない場合のprefixであれば処理続行
                //まずはフルのprefixを登録する．
                //Min(ルータ数, FIBの冗長度)の数だけ, ルータを選択する．
                int num = Math.min(node.getRouterMap().size(), fib_dup_num);
                Iterator<CCNRouter> rIte = node.getRouterMap().values().iterator();
                int cnt = 0;
                HashMap<Long, CCNRouter> firstMap = new HashMap<Long, CCNRouter>();
                //デリミタ数を取得する．
                int delimiter_num = prefix.length() - prefix.replaceAll("\\/", "").length();
                while(rIte.hasNext()){
                    if(cnt >= num){
                        break;
                    }
                    CCNRouter router = rIte.next();
                    //ルータのFIBに追加する．
                    //転送先となるルータのIDを見つかったので，Faceを作成
                    Face f = new Face(null, node.getNodeID(), CCNUtil.NODETYPE_NODE);
                    //ルータのFIBに追加する．
                    router.getFIBEntry().addFace(prefix, f);
                    //ルータのnodeMapに追加する．
                    router.getFace_nodeMap().put(node.getNodeID(), f);
                    firstMap.put(router.getRouterID(), router);
                    //以降は，substringのprefixでのFIB登録処理．
                    //デリミタの数だけ行う．
                    for(int k = 0;/*!subPrefix.equals("/")*/k<delimiter_num;k++){

                        // kだけとる．
                        int lastIDX = subPrefix.lastIndexOf("/");
                        if(lastIDX <= 0){
                            break;
                           // subPrefix = "/";
                        }else{
                            subPrefix = subPrefix.substring(0, lastIDX);

                        }

                        //取得したものを1台のルータのFIBへ登録する．
                        //routerの隣接ルータへ登録する．
                        Iterator<Face> fIte = router.getFace_routerMap().values().iterator();
                        int fidx = 0;
                        int randomIDX = CCNUtil.genInt(0, router.getFace_routerMap().size()-1);
                        CCNRouter nextRouter = null;
                        while(fIte.hasNext()){
                            Face face = fIte.next();
                            if(fidx == randomIDX){
                                //ここでbreakする．
                                nextRouter = this.getRouterMap().get(face.getPointerID());
                                break;

                            }

                            fidx++;
                        }

                        Face nextFace = new Face(null, router.getRouterID(), CCNUtil.NODETYPE_ROUTER);
                        nextRouter.getFIBEntry().addFace(subPrefix, nextFace);
                        nextRouter.getFace_routerMap().put(router.getRouterID(), nextFace);


                        router = nextRouter;


                    }
                    subPrefix = prefix;
                    cnt++;

                }

            }

        }



    }



    @Override
    public void buildFaces() {

    }

    @Override
    /**
     * CCNNode->CCNRouterへのInterestパケット転送先の決定．
     * ここは，ランダムに選択する．
     */
    public CCNRouter selectRouter(HashMap<Long, CCNRouter> rMap, InterestPacket packet) {
        Object[] arrID = rMap.keySet().toArray();
        int len = arrID.length;
        int idx = CCNUtil.genInt(0, len - 1);
        Long destID = (Long) arrID[idx];
        CCNRouter r = rMap.get(destID);
        return r;
    }

    @Override
    public Long addFaceToFIBAsNewEntry(String prefix, CCNRouter router) {

            //LongestMatchによって指定のprefixに最も近いprefixを選択する．
            Iterator<String> pIte  = router.getFIBEntry().getTable().keySet().iterator();
            int maxCnt = 0;
            String retPrefix = null;
            while(pIte.hasNext()){
                String fibPrefix = pIte.next();
                //まずは，startwithであることが必要．
                int cnt = -1;
                if(prefix.startsWith(fibPrefix)){
                    cnt = this.longestMatch(prefix, fibPrefix);
                }


                if(cnt >= maxCnt){
                    maxCnt = cnt;
                    retPrefix = fibPrefix;
                }

            }

            //対象prerixが決まったので，後はどのfaceにするか．
            //とりあえず，ランダム．
           LinkedList<Face> retFList = router.getFIBEntry().getTable().get(retPrefix);
            int ran = CCNUtil.genInt(0, retFList.size()-1);
            Face f = retFList.get(ran);


            return f.getPointerID();



    }

    //static int LCSubStr(char X[], char Y[], int m, int n)
    public int longestMatch(String x, String y) {
        char[] X = x.toCharArray();
        char[] Y = y.toCharArray();
        int m = x.length();
        int n = y.length();

        // Create a table to store lengths of longest common suffixes of
        // substrings. Note that LCSuff[i][j] contains length of longest
        // common suffix of X[0..i-1] and Y[0..j-1]. The first row and
        // first column entries have no logical meaning, they are used only
        // for simplicity of program
        int LCStuff[][] = new int[m + 1][n + 1];
        int result = 0;  // To store length of the longest common substring

        // Following steps build LCSuff[m+1][n+1] in bottom up fashion
        for (int i = 0; i <= m; i++)
        {
            for (int j = 0; j <= n; j++)
            {
                if (i == 0 || j == 0)
                    LCStuff[i][j] = 0;
                else if (X[i - 1] == Y[j - 1])
                {
                    LCStuff[i][j] = LCStuff[i - 1][j - 1] + 1;
                    result = Integer.max(result, LCStuff[i][j]);
                }
                else
                    LCStuff[i][j] = 0;
            }
        }
        return result;
    }

    @Override
    public long getNextRouterIDIfInterestLoop(InterestPacket p, Face f, CCNRouter router) {
        return 0;
    }
}
