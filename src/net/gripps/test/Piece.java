package net.gripps.test;

/**
 * Author: H. Kanemitsu
 * Date: 2010/12/17
 */
public class Piece {
    
    private long pieceID;


    public long getPieceID() {
        return pieceID;
    }

    public void setPieceID(long pieceID) {
        this.pieceID = pieceID;
    }

    public Piece(long pieceID) {
        this.pieceID = pieceID;
    }
}
