package chess;
import boardgame.Board;
import chessPieces.*;

public class ChessMatch {
    private Board board;

    public ChessMatch(){
        board = new Board(8,8);
        intialSetup();
    }

    public ChessPiece[][] getPieces(){
        ChessPiece[][] matrix = new ChessPiece[board.getRows()][board.getColumns()];
        for(int i=0;i<board.getRows();i++){
            for (int j = 0; j < board.getColumns(); j++) {
                matrix[i][j] = (ChessPiece) board.piece(i,j);
            }
        }
        return matrix;
    }

    private void placeNewPiece(char column, int row, ChessPiece piece){
        board.placePiece(piece, new ChessPosition(column, row).toPosition());
    }

    private void intialSetup(){
        placeNewPiece('a',1,new Rook(board,Color.WHITE));
        placeNewPiece('h',1,new Rook(board,Color.WHITE));
        placeNewPiece('a',8,new Rook(board,Color.BLACK));
        placeNewPiece('h',8,new Rook(board,Color.BLACK));
        placeNewPiece('e',1,new King(board,Color.WHITE));
        placeNewPiece('e',8,new King(board,Color.BLACK));
    }
     
}