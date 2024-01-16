package net.gripps.ccn.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by kanemih on 2018/11/02.
 */
public class PIT extends FIB{

    public PIT(HashMap<String, LinkedList<Face>> table) {
        super(table);
    }

    public PIT() {
        super();
    }

    /**
     * 指定のfaceを指定のprefixにおいて削除します
     * @param prefix
     * @param face
     * @return
     */
    public synchronized boolean removeFace(String prefix, Face face){
        if(!this.getTable().containsKey(prefix)){
            return false;
        }
        Iterator<Face> fIte = this.getTable().get(prefix).iterator();

        Face retFace = null;
       // int idx = 0;

        while(fIte.hasNext()){
            Face f = fIte.next();
            if((f.getPointerID().equals(face.getPointerID()))&&(f.getType() == face.getType())){
                retFace = f;
                break;
            }
          //  idx++;
        }
        LinkedList<Face> retList = this.getTable().get(prefix);
        retList.remove(retFace);
        //this.getTable().remove(prefix, retFace);
        return true;

    }

    public synchronized boolean removeFace(String prefix, Long id){
        if(this.getTable().containsKey(prefix)){
            LinkedList<Face> fList = this.getTable().get(prefix);
            Face retFace = null;
            Iterator<Face> fIte = fList.iterator();
            while(fIte.hasNext()){
                Face face = fIte.next();
                if(face.getPointerID().equals(id)){
                    retFace = face;
                    break;

                }
            }
            fList.remove(retFace);
            return true;

        }else{
            return false;
        }
    }
}
