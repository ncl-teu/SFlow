package net.gripps.test;

import java.util.Vector;
import java.util.Hashtable;

/**
 * Author: H. Kanemitsu
 * Date: 2010/12/17
 */
public class Node {

    private Hashtable<Long, Piece> piece_table ;

    public Hashtable<Long, Piece> getPiece_table() {
        return piece_table;
    }

    public void setPiece_table(Hashtable<Long, Piece> piece_table) {
        this.piece_table = piece_table;

    }

    public void addPiece(long piece_id, Piece piece){
        this.piece_table.put(new Long(piece_id), piece);
    }

    public void removePiece(long id){
        this.piece_table.remove(new Long(id));
    }



}
