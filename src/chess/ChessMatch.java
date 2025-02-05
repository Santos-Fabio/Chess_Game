package chess;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import boardgame.Board;
import boardgame.Piece;
import boardgame.Position;
import chessPieces.*;

public class ChessMatch {
    private Board board;
    private int turn;
    private Color currentPlayer;
    private boolean check;
    private boolean checkMate;
    private ChessPiece enPassantVulnerable;
    private ChessPiece promoted;

    private List<Piece> piecesOnTheBoard = new ArrayList<>();
    private List<Piece> capturedPieces = new ArrayList<>(); 

    public int getTurn(){
        return turn;
    }

    public Color getCurrentPlayer(){
        return currentPlayer;
    }

    public boolean getCheck(){
        return check;
    }

    public boolean getCheckmate(){
        return checkMate;
    }

    public ChessPiece getEnPassantVulnerable(){
        return enPassantVulnerable;
    }

    public ChessPiece getPromoted(){
        return promoted;
    }

    public ChessMatch(String mode){
        board = new Board(8,8);
        turn = 1;
        currentPlayer = Color.WHITE;
        if(mode.equals("Y")){
            randomSetup();
        }else{
            intialSetup();
        }
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

    public boolean[][] possibleMoves(ChessPosition sourcePosition){
        Position position = sourcePosition.toPosition();
        validateSourcePosition(position);
        return board.piece(position).possibleMoves();
    }

    public ChessPiece peformChessMove(ChessPosition sourcePosition, ChessPosition targetPosition){
        Position source = sourcePosition.toPosition();
        Position target = targetPosition.toPosition(); 
        validateSourcePosition(source);
        validateTargetPosition(source, target);
        Piece capturedPiece = makeMove(source, target);

        if(testCheck(currentPlayer)){
            undoMove(source, target, capturedPiece);
            throw new ChessException("You can't put yourself in check");
        }

        ChessPiece movedPiece = (ChessPiece)board.piece(target);

        
        //#Special move Promotion
        promoted = null;
        if(movedPiece instanceof Pawn){
            if((movedPiece.getColor() == Color.WHITE && target.getRow()==0 ) || (movedPiece.getColor() == Color.BLACK && target.getRow()==7)){
                promoted = (ChessPiece)board.piece(target);
                promoted = replacePromotedPiece("Q");
            }
        }
        
        check = (testCheck(opponent(currentPlayer))) ? true : false;

        if(testCheckMate(opponent(currentPlayer))){
            checkMate =true;
        }else{
            nextTurn();
        }

        //#Special Move En Passant
        if(movedPiece instanceof Pawn && (target.getRow()==source.getRow()-2 || target.getRow()==source.getRow()+2)){
            enPassantVulnerable = movedPiece;
        }else{
            enPassantVulnerable = null;
        }

        return (ChessPiece)capturedPiece;
    }

    public ChessPiece replacePromotedPiece(String type){
        if(promoted == null){
            throw new IllegalStateException("There is no piece to be promoted");
        }
        if(!type.equals("B") && !type.equals("N") && !type.equals("R") && !type.equals("Q")){
            return promoted;
        }
        
        Position pos = promoted.getChessPosition().toPosition();
        Piece p = board.removePiece(pos);
        piecesOnTheBoard.remove(p);

        ChessPiece newPiece = newPiece(type, promoted.getColor());
        board.placePiece(newPiece, pos);
        piecesOnTheBoard.add(newPiece);

        return newPiece;

    }

    private ChessPiece newPiece(String type, Color color){
        if(type.equals("B")) return new Bishop(board, color);
        if(type.equals("N")) return new Knight(board, color);
        if(type.equals("R")) return new Rook(board, color);
        return new Queen(board, color);
    }

    private Piece makeMove(Position source, Position target){
        ChessPiece p = (ChessPiece)board.removePiece(source);
        p.increaseMoveCount();
        Piece capturedPiece = board.removePiece(target);
        board.placePiece(p, target);
        if(capturedPiece!=null){
            piecesOnTheBoard.remove(capturedPiece);
            capturedPieces.add(capturedPiece);
        }

        //#Special move King side rook
        if(p instanceof King && target.getColumn() == source.getColumn()+2){
            Position sourceT = new Position(source.getRow(), source.getColumn()+3);
            Position targetT = new Position(source.getRow(), source.getColumn()+1);
            ChessPiece rook = (ChessPiece)board.removePiece(sourceT);
            board.placePiece(rook, targetT);
            rook.increaseMoveCount();
        }
        
        //#Special move Queen side rook
        if(p instanceof King && target.getColumn() == source.getColumn()-2){
            Position sourceT = new Position(source.getRow(), source.getColumn()-4);
            Position targetT = new Position(source.getRow(), source.getColumn()-1);
            ChessPiece rook = (ChessPiece)board.removePiece(sourceT);
            board.placePiece(rook, targetT);
            rook.increaseMoveCount();
        }

        //#Special move en passant
        if(p instanceof Pawn){
            if(source.getColumn()!=target.getColumn() && capturedPiece == null){
                Position pawnPosition;
                if(p.getColor() == Color.WHITE){
                    pawnPosition = new Position(target.getRow()+1, target.getColumn());
                }else{
                    pawnPosition = new Position(target.getRow()-1, target.getColumn());
                }
                capturedPiece = board.removePiece(pawnPosition);
                capturedPieces.add(capturedPiece);
                piecesOnTheBoard.remove(capturedPiece);
            }
        }

        return capturedPiece;
    }

    private void undoMove(Position source, Position target, Piece capturedPiece){
        ChessPiece p = (ChessPiece)board.removePiece(target);
        p.decreaseMoveCount();
        board.placePiece(p, source);
        if(capturedPiece != null){
            board.placePiece(capturedPiece, target);
            capturedPieces.remove(capturedPiece);
            piecesOnTheBoard.add(capturedPiece);
        }

        //#Special move King side rook
        if(p instanceof King && target.getColumn() == source.getColumn()+2){
            Position sourceT = new Position(source.getRow(), source.getColumn()+3);
            Position targetT = new Position(source.getRow(), source.getColumn()+1);
            ChessPiece rook = (ChessPiece)board.removePiece(targetT);
            board.placePiece(rook, sourceT);
            rook.decreaseMoveCount();
        }
        
        //#Special move Queen side rook
        if(p instanceof King && target.getColumn() == source.getColumn()-2){
            Position sourceT = new Position(source.getRow(), source.getColumn()-4);
            Position targetT = new Position(source.getRow(), source.getColumn()-1);
            ChessPiece rook = (ChessPiece)board.removePiece(targetT);
            board.placePiece(rook, sourceT);
            rook.decreaseMoveCount();
        }

        //#Special move en passant
        if(p instanceof Pawn){
            if(source.getColumn()!=target.getColumn() && capturedPiece == enPassantVulnerable){
                ChessPiece pawn = (ChessPiece)board.removePiece(target);
                Position pawnPosition;
                if(p.getColor() == Color.WHITE){
                    pawnPosition = new Position(3, target.getColumn());
                }else{
                    pawnPosition = new Position(4, target.getColumn());
                }
                board.placePiece(pawn, pawnPosition);
            }
        }

    }

    private void validateSourcePosition(Position position){
        if(!board.thereIsAPiece(position)){
            throw new ChessException("There is no piece on source position");
        }
        if(currentPlayer!=((ChessPiece)board.piece(position)).getColor()){
            throw new ChessException("The chosen piece is not yours");
        }
        if(!board.piece(position).isThereAnyPossibleMove()){
            throw new ChessException("There is no possible moves for the chosen piece.");
        }
    }

    private void validateTargetPosition(Position source, Position target){
        if(!board.piece(source).possibleMove(target)){
            throw new ChessException("The chosen piece can't move to target position");
        }
    }

    private void nextTurn(){
        turn++;
        currentPlayer = (currentPlayer == Color.WHITE) ? Color.BLACK : Color.WHITE;
    }

    private Color opponent(Color color){
        return (color == Color.WHITE) ? Color.BLACK : Color.WHITE;
    }

    private ChessPiece king(Color color){
        List<Piece> list = piecesOnTheBoard.stream().filter(x ->((ChessPiece)x).getColor()==color).collect(Collectors.toList());
        for (Piece p : list) {
            if(p instanceof King){
                return (ChessPiece)p;
            }
        }
        throw new IllegalStateException("There is no " + color + "King on the board");

    }

    private boolean testCheck(Color color){
        Position kingPosition = king(color).getChessPosition().toPosition();
        List<Piece> opponentPieces = piecesOnTheBoard.stream().filter(x ->((ChessPiece)x).getColor()==opponent(color)).collect(Collectors.toList());
        for (Piece p : opponentPieces) {
            boolean[][] mat = p.possibleMoves();
            if(mat[kingPosition.getRow()][kingPosition.getColumn()]){
                return true;
            }
        }
        return false;
    }

    private boolean testCheckMate(Color color){
        if(!testCheck(color)){
            return false;
        }
        List<Piece> list = piecesOnTheBoard.stream().filter(x ->((ChessPiece)x).getColor()==color).collect(Collectors.toList());
        for (Piece p : list) {
            boolean[][] mat = p.possibleMoves();
            for (int i = 0; i < board.getRows(); i++) {
                for (int j = 0; j < board.getColumns(); j++) {
                    if(mat[i][j]){
                        Position source = ((ChessPiece)p).getChessPosition().toPosition();
                        Position target = new Position(i, j);
                        Piece capturedPiece = makeMove(source, target);
                        boolean testCheck = testCheck(color);
                        undoMove(source, target, capturedPiece);
                        if(!testCheck){
                            return false;
                        }
                    }
                }
            }
        }
        return true;
        
    }

    private void placeNewPiece(char column, int row, ChessPiece piece){
        board.placePiece(piece, new ChessPosition(column, row).toPosition());
        piecesOnTheBoard.add(piece);
    }

    private void intialSetup(){
        //Pawns
        placeNewPiece('a', 2,new Pawn(board,Color.WHITE,this) );
        placeNewPiece('b', 2,new Pawn(board,Color.WHITE,this) );
        placeNewPiece('c', 2,new Pawn(board,Color.WHITE,this) );
        placeNewPiece('d', 2,new Pawn(board,Color.WHITE,this) );
        placeNewPiece('e', 2,new Pawn(board,Color.WHITE,this) );
        placeNewPiece('f', 2,new Pawn(board,Color.WHITE,this) );
        placeNewPiece('g', 2,new Pawn(board,Color.WHITE,this) );
        placeNewPiece('h', 2,new Pawn(board,Color.WHITE,this) );

        placeNewPiece('a', 7,new Pawn(board,Color.BLACK,this));
        placeNewPiece('b', 7,new Pawn(board,Color.BLACK,this));
        placeNewPiece('c', 7,new Pawn(board,Color.BLACK,this));
        placeNewPiece('d', 7,new Pawn(board,Color.BLACK,this));
        placeNewPiece('e', 7,new Pawn(board,Color.BLACK,this));
        placeNewPiece('f', 7,new Pawn(board,Color.BLACK,this));
        placeNewPiece('g', 7,new Pawn(board,Color.BLACK,this));
        placeNewPiece('h', 7,new Pawn(board,Color.BLACK,this));

        //Knights
        placeNewPiece('b', 1, new Knight(board, Color.WHITE));
        placeNewPiece('g', 1, new Knight(board, Color.WHITE));
        placeNewPiece('b', 8, new Knight(board, Color.BLACK));
        placeNewPiece('g', 8, new Knight(board, Color.BLACK));

        //Bishops
        placeNewPiece('c', 1, new Bishop(board, Color.WHITE));
        placeNewPiece('f', 1, new Bishop(board, Color.WHITE));
        placeNewPiece('c', 8, new Bishop(board, Color.BLACK));
        placeNewPiece('f', 8, new Bishop(board, Color.BLACK));

        //Rooks
        placeNewPiece('a',1,new Rook(board,Color.WHITE));
        placeNewPiece('h',1,new Rook(board,Color.WHITE));
        placeNewPiece('a',8,new Rook(board,Color.BLACK));
        placeNewPiece('h',8,new Rook(board,Color.BLACK));

        //Queens
        placeNewPiece('d',1,new Queen(board,Color.WHITE));
        placeNewPiece('d',8,new Queen(board,Color.BLACK));

        //Kings
        placeNewPiece('e',1,new King(board,Color.WHITE,this));
        placeNewPiece('e',8,new King(board,Color.BLACK,this));
    }

    private void randomSetup(){
        //Pawns
        placeNewPiece('a', 2,new Pawn(board,Color.WHITE,this) );
        placeNewPiece('b', 2,new Pawn(board,Color.WHITE,this) );
        placeNewPiece('c', 2,new Pawn(board,Color.WHITE,this) );
        placeNewPiece('d', 2,new Pawn(board,Color.WHITE,this) );
        placeNewPiece('e', 2,new Pawn(board,Color.WHITE,this) );
        placeNewPiece('f', 2,new Pawn(board,Color.WHITE,this) );
        placeNewPiece('g', 2,new Pawn(board,Color.WHITE,this) );
        placeNewPiece('h', 2,new Pawn(board,Color.WHITE,this) );

        placeNewPiece('a', 7,new Pawn(board,Color.BLACK,this));
        placeNewPiece('b', 7,new Pawn(board,Color.BLACK,this));
        placeNewPiece('c', 7,new Pawn(board,Color.BLACK,this));
        placeNewPiece('d', 7,new Pawn(board,Color.BLACK,this));
        placeNewPiece('e', 7,new Pawn(board,Color.BLACK,this));
        placeNewPiece('f', 7,new Pawn(board,Color.BLACK,this));
        placeNewPiece('g', 7,new Pawn(board,Color.BLACK,this));
        placeNewPiece('h', 7,new Pawn(board,Color.BLACK,this));

    
        List<Integer> positions = new ArrayList<>();

        //Add bishops and making sure there are 2 biushops in differents color squares
        int bishop1 = (int) (Math.random()*4)*2;
        int bishop2 = (int) (Math.random()*4)*2 + 1;
        positions.add(bishop1);
        positions.add(bishop2);
        
        List<Integer> remainingPositions = new ArrayList<>();

        for (int i = 0; i < 8; i++) {
            if(i!=bishop1 && i!=bishop2){
                remainingPositions.add(i);
            }
        }
        Collections.shuffle(remainingPositions);
        
        //Add Queen position
        positions.add(remainingPositions.remove(0));
        
        //Add Knights position
        positions.add(remainingPositions.remove(0));
        positions.add(remainingPositions.remove(0));

        //Add Rooks and King and ensure king is between rooks
        Collections.sort(remainingPositions);
        
        positions.add(remainingPositions.get(0));
        positions.add(remainingPositions.get(2));
        positions.add(remainingPositions.get(1));

        //Bishops
        placeNewPiece((char)('a'+ positions.get(0)), 1, new Bishop(board,Color.WHITE));
        placeNewPiece((char)('a'+ positions.get(1)), 1, new Bishop(board,Color.WHITE));
        placeNewPiece((char)('a'+ positions.get(0)), 8, new Bishop(board,Color.BLACK));
        placeNewPiece((char)('a'+ positions.get(1)), 8, new Bishop(board,Color.BLACK));

        //Queens
        placeNewPiece((char)('a'+ positions.get(2)), 1, new Queen(board,Color.WHITE));
        placeNewPiece((char)('a'+ positions.get(2)), 8, new Queen(board,Color.BLACK));

        //Knights
        placeNewPiece((char)('a'+ positions.get(3)), 1, new Knight(board,Color.WHITE));
        placeNewPiece((char)('a'+ positions.get(4)), 1, new Knight(board,Color.WHITE));
        placeNewPiece((char)('a'+ positions.get(3)), 8, new Knight(board,Color.BLACK));
        placeNewPiece((char)('a'+ positions.get(4)), 8, new Knight(board,Color.BLACK));
        
        //Rooks and Kings
        placeNewPiece((char)('a'+ positions.get(5)), 1, new Rook(board,Color.WHITE));
        placeNewPiece((char)('a'+ positions.get(6)), 1, new Rook(board,Color.WHITE));
        placeNewPiece((char)('a'+ positions.get(7)), 1, new King(board,Color.WHITE,this));
        placeNewPiece((char)('a'+ positions.get(5)), 8, new Rook(board,Color.BLACK));
        placeNewPiece((char)('a'+ positions.get(6)), 8, new Rook(board,Color.BLACK));
        placeNewPiece((char)('a'+ positions.get(7)), 8, new King(board,Color.BLACK,this));

    }
     
}