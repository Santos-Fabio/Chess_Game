package boardgame;

public class Piece{
    protected Position piecePosition;
    private Board board;

    public Piece(Board board){
        this.board = board;
        piecePosition = null;
    }

    protected Board getBoard(){
        return board;
    }
    
}

