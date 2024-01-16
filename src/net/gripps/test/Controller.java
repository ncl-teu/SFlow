package net.gripps.test;

import java.util.Hashtable;

/**
 * Author: H. Kanemitsu
 * Date: 2010/12/17
 *
 * //how to call
 * Controller.getInstance().addNode or getNode
 */
public class Controller {
    static Controller ownInstance;
    Hashtable<Long, Node> nodeTable;

    public static Controller getInstance(){
        if(Controller.ownInstance == null){
            Controller.ownInstance = new Controller();
        }
        return Controller.ownInstance;

    }

    private Controller(){
        this.nodeTable = new Hashtable<Long,Node>();
    }

    public void addNode(Long nodeID, Node node){
        this.nodeTable.put(nodeID, node);

    }

    public Node getNode(long id){
        return this.nodeTable.get(new Long(id));
    }

    public void removeNode(long id){
         this.nodeTable.remove(new Long(id));
    }
}
