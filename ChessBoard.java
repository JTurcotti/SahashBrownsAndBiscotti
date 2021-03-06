import java.util.List;
import java.util.LinkedList;
import java.util.stream.Collectors;

public class ChessBoard {
    // Default pieces for Chess with unique values so names can be referenced 
    // in the bitboard
    public static final int EMPTY = -1;
    public static final int PAWN = 0;
    public static final int BISHOP = 1;
    public static final int KNIGHT = 2;
    public static final int ROOK = 3;
    public static final int QUEEN = 4;
    public static final int KING = 5;

    public static final int WHITE = 1;
    public static final int BLACK = -1;

    // String Array containing abbreviations for the chess pieces
    public static final String[] names = new String[] {"P", "B", "H", "R", "Q", "K"};


    // Two bitboard arrays for each color, storing piece location
    public final long[] bbWhite;
    public final long[] bbBlack;
    public long bbPieces(int color, int type) {
	return (color==WHITE)? bbWhite[type]: bbBlack[type];
    }

    public boolean blackKingMoved;
    public boolean whiteKingMoved;

  // index of enpassantable square if exists, -1 otherwise
    public int passant;

    private void resetPassant() {passant = -1;}

    public final List<Integer> capturedWhitePieces;
    public final List<Integer> capturedBlackPieces;
    public List<Integer> capturedPieces(int color) {
	if (color == WHITE) return capturedWhitePieces;
	if (color == BLACK) return capturedBlackPieces;
	return new LinkedList<>();
    }
    // 	capturedWhitePieces, capturedBlackPieces};

    public ChessMoveHistory history;

    public void initHistory() {history = new ChessMoveHistory(this);}
    /**
     *Default constructor 
     *Initializes the default board layout
     */    

    public ChessBoard(){
	bbWhite = new long[6];
	bbBlack = new long[6];
	capturedWhitePieces = new LinkedList<>();
	capturedBlackPieces = new LinkedList<>();
	resetPassant();
	initHistory();
    }

    public ChessBoard(ChessBoard other) {
	this();
	System.arraycopy(other.bbWhite, 0, bbWhite, 0, 6);
	System.arraycopy(other.bbBlack, 0, bbBlack, 0, 6);
	initHistory();
    }

    public ChessBoard clone() {
	return new ChessBoard(this);
    }

    public void setup() {
	bbWhite[PAWN] = ((1<<8)-1)<<8;
	bbWhite[BISHOP] = (1<<2) + (1<<5);
	bbWhite[KNIGHT] = (1<<1) + (1<<6);
	bbWhite[ROOK] = (1<<0) + (1<<7);
	bbWhite[QUEEN] = (1<<4);
	bbWhite[KING] = (1<<3);
	for (int i=0; i<bbBlack.length; i++) bbBlack[i]=Long.reverse(bbWhite[i]);
	long queens = bbBlack[QUEEN];
	bbBlack[QUEEN] = bbBlack[KING];
	bbBlack[KING] = queens;
	whiteKingMoved=blackKingMoved=false;
	initHistory();

    }
	
    /**
     *toString method that returns the chess board 
     *@return String Visual representation of the present chessboard. The lowercase letter preceeding the uppercase letter indicates color. K = King, Q = Queen, R = Rook, B = Bishop, N = Knight, P = Pawn
     */    
    
    public String toString(long mask) {
	String out = "  a b c d e f g h\n8";
	for (int i=63; i>=0; i--) {
	    String s = "  ";
	    for (int j=0; j<bbWhite.length; j++)
		if (((1L << i) & bbWhite[j]) != 0) s = "w" + names[j];
	    for (int j=0; j<bbBlack.length; j++)
		if (((1L << i) & bbBlack[j]) != 0) s = "b" + names[j];

	    out += "\033[" + ((((1L<<i) & mask) != 0)? 43:((i/8+i%8)%2==0? 47: 40)) + ";" + ((((1L << i) & getWhite()) != 0)? 34: 31) + "m" + s + "\033[0m";
	    if (i%8==0) out +=(i/8+1) + "\n" + (i>0? (i/8):"");
	    
	}
	return out + " a b c d e f g h";
    }
    
    public String toString() {
	return toString(0L);
    }
    //mainly for debugging, ineffecient
    public String toString(String pos) {
	try {
	    List<ChessMove> pieceMoves = pieceMoves(ChessMove.toIndex(pos));
	    long mask = moveMask(pieceMoves) | captureMask(pieceMoves);
	    return toString(mask);
	} catch (Exception e) {return toString();}
    }
	    
    
    /**
     *This method is used to get a bitboard that contains only white pieces 
     *@return long A bitboard containing present white pieces only
     */    

    public long getWhite() {
	long out = 0L;
	for (int i=0; i<6; i++) out |= bbWhite[i];
	return out;
    }

    /**
     *This method is used to get a bitboard that contains only black pieces 
     *@return long A bitboard containing present black pieces only
     */

    public long getBlack() {
	long out = 0L;
	for (int i=0; i<6; i++) out |= bbBlack[i];
	return out;
    }

    public long getByColor(int color) {
	return (color==WHITE)? getWhite(): getBlack();
    }
    
    /**
     *This method is used to get a bitboard containg all of the pieces 
     *@return long A bitboard containing all present pieces
     */

    public long getAll() {
      return getWhite() | getBlack();
    } 


    /**
     *Method that will execute the attack
     *@param int Position of desired piece
     *@param int Color of the desired piece
     *@return long mask of all squares containing pieces of the given color that ar attacking the specific square at position pos
     */
  public int getKingIndex(int color) {
    int index = 0;  
    long king = bbPieces(color, KING);
    while ((king>>=1)>0) index++;
    return index;
  }
    //return a mask of all pieces of a given color attacking given square

    public long attacking(int pos, int color) {
	long pawns, knights, kings, bishopQueens, rookQueens;
	pawns = bbPieces(color, PAWN);
	knights = bbPieces(color, KNIGHT);
	kings = bbPieces(color, KING);
	bishopQueens = rookQueens = bbPieces(color, QUEEN);
	bishopQueens |= bbPieces(color, BISHOP);
	rookQueens |= bbPieces(color, ROOK);
	return(pawns & Chess.pawnMasks[pos][2+color])
	    | (knights & Chess.knightMasks[pos])
	    | (kings & Chess.kingMasks[pos])
	    | (bishopQueens & Chess.bishopMask(getAll(), pos))
	    | (rookQueens & Chess.rookMask(getAll(), pos));
    }

    /**
     *Method that checks if the other side is inCheck after every move is made. Runs the attacking method on the king of the color and the opposite color's pieces
     *@param int Color of king being checked
     *@return long A mask that reveals the positions the defending king's pieces can move to block the check
     */

   //returns a filter condition on moves that would get the king out of check if in check, otherwise just a lot of 1s

    public long inCheckFilter(int color) {
	int pos = getKingIndex(color);
	long attacking = attacking(pos, -color);
	if (attacking==0L) return -1L;

	long filter = (attacking & bbPieces(-color, KNIGHT))|(attacking & bbPieces(-color, PAWN));
	for (Integer i: toIndices(attacking & (bbPieces(-color, QUEEN)|bbPieces(-color, ROOK)|bbPieces(-color, BISHOP)))) {
	    filter |= Chess.rayMask(pos, i);
	}
	return filter;
    }
	
    
    
    // piece number for presence of either color, -1 for blank
    public int typeAtPosition(int i) {
	for (int j=0; j<6; j++)
	    if (((1L << i) & (bbBlack[j] | bbWhite[j])) != 0) return j;
	return -1;
    }

    // 0 for empty, 1 for white, -1 for black
    public int colorAtPosition(int i) {
	if (((1L << i) & getWhite()) != 0) return WHITE;
	if (((1L << i) & getBlack()) != 0) return BLACK;
	return 0;
    }

    public void promotePawn(int position, int type) {
	if (typeAtPosition(position) != PAWN) return;
	if (type < 0 || type > 4) return;
	int color = colorAtPosition(position);
	switch (color) {
	case WHITE:
	    bbWhite[type] |= (1L << position);
	    bbWhite[PAWN] &= -1*((1L << position) + 1L);
	    break;
	case BLACK:
	    bbBlack[type] |= (1L << position);
	    bbBlack[PAWN] &= -1*((1L << position) + 1L);
	}
    }
	    
    //move piece
    public void movePiece(int color, int startType, int endType, int start, int end) {
	switch (color) { //could technically be a conditional, but this leaves room to change null move conditions
	case WHITE:
	    bbWhite[startType] |= (1L << end); //add white piece at end
	    bbWhite[startType] &= -1*((1L << start)+1L);//remove white piece at start
	    if (endType!=-1) bbBlack[endType] &= -1 *((1L << end)+1L);//remove black piece at end
	    break;
	case BLACK:
	    bbBlack[startType] |= (1L << end);//add black piece at end
	    bbBlack[startType] &= -1*((1L << start)+1L);//remove black piece at start
	    if (endType!=-1) bbWhite[endType] &= -1 *((1L << end)+1L);//remove white piece at end
	    break;
	}
    }
    
    //moves piece then does all associated weird stuff
    public void makeMove(ChessMove move) {
	int startType = typeAtPosition(move.start);
	if (startType==-1) return;
	int endType = typeAtPosition(move.end);
	int color = colorAtPosition(move.start);
	movePiece(color, startType, endType, move.start, move.end);

	//all the following come after the actual move making in case of unchecked exceptions during bitboard manipulation

	//pawn promotion!
	if (startType==PAWN && (move.end/8)==(color==WHITE? 7: 0)) promotePawn(move.end, QUEEN);
	
	//if a capture, then add to the enemy's captured list this type of capture piece
	if (move.capture) capturedPieces((color + 1)/2).add(endType);

	//if a pawn just moved in front of a passantable square, kill the pawn there
	if (startType==PAWN && move.end-8*color==passant) {
	    switch (color) {
	    case WHITE:
		bbBlack[PAWN] &= -1*((1L << passant)+1L);
		break;
	    case BLACK:
		bbWhite[PAWN] &= -1*((1L << passant)+1L);
		break;
	    }
	}
	
	resetPassant();
	//if you double push a pawn, set its endpoint to be the enpassant square
	if (startType==PAWN && Math.abs(move.start/8-move.end/8)==2) passant = move.end;


	//when you move a king, update the castling variables
	if (startType==KING)
	    if (color==WHITE) {
		whiteKingMoved = true;
	    } else {
		blackKingMoved = true;
	    }

	//if king castled, then move the rook to accompany
	if (startType==KING && Math.abs(move.start%8 - move.end%8)>1) {
	    int position = (color==WHITE? 3: 59);
	    if (move.start > move.end) {
		movePiece(color, ROOK, EMPTY, position-3, position-1);
	    } else {
		movePiece(color, ROOK, EMPTY, position+4, position+1);
	    }
	}
	    
    }
    public void makeMove(String start, String end) {
	makeMove(new ChessMove(start, end));
    }

    //for debugging only
    public void place(int color, int type, int pos) {
	int t = typeAtPosition(pos);
	if (color==WHITE) {
	    if (t!=-1) bbBlack[t] &= -1 *((1L << pos)+1L);
	} else {
	    if (t!=-1) bbWhite[t] &= -1 *((1L << pos)+1L);
	}
	if (type==-1) return;
 	if (color==WHITE) {
	    bbWhite[type] |= (1L<<pos);
	} else {
	    bbBlack[type] |= (1L<<pos);
	}
    }
    public void place(int color, int type, String pos) {
	place(color, type, ChessMove.toIndex(pos));
    }
    //i tested this a lot, pretty sure its the fastest way....
    public static List<Integer> toIndices(long l) {
	List<Integer> ints = new LinkedList<>();
	for(int i=0; i<64; i++) {
	    if ((1 & l)==1) ints.add(i);
	    l>>=1;
	}
	return ints;
    }

    //the following four methods deal with conversion between move lists and bit masks
    public static List<ChessMove> toMoves(int start, long ends, boolean capture) {
	List<ChessMove> moves = new LinkedList<>();
	for (Integer end: toIndices(ends))
	    moves.add(new ChessMove(start, end, capture));
	return moves;
    }

    public static long toMask(List<ChessMove> moves) {
	long mask = 0L;
	for (ChessMove move: moves) {
	    mask += (1L<<move.end);
	}
	return mask;
    }
    
    public static long moveMask(List<ChessMove> moves) {
	long moveMask = 0L;
	for (ChessMove move: moves) {
	    if (!move.capture) moveMask += (1L<<move.end);
	}
	return moveMask;
    }
    public static long captureMask(List<ChessMove> moves) {
	long captureMask = 0L;
	for (ChessMove move: moves) {
	    if (move.capture) captureMask += (1L<<move.end);
	}
	return captureMask;
    }

    //it may seem convoluted to generate masks, turn them into lists of moves, then convert them back to masks, but its more effecient to store the list because they maintain origin and capture imformation, and regeneration to resotre that information is extremely costly
    public List<ChessMove>  pieceMoves(int pos) {
	List<ChessMove> moves = new LinkedList<>();
	int color = colorAtPosition(pos);
	if (color == 0) return moves;
	long opp = getByColor(-color);
	long all = getAll();
	long moveMask = 0L;
	long captureMask = 0L;
	int type = typeAtPosition(pos);
	switch (type) {
	case PAWN:
	    moveMask = Chess.pawnMasks[pos][1-color] & ~all;
	    captureMask = Chess.pawnMasks[pos][2-color] & opp;
	    captureMask |= Chess.passantMask(color, passant, pos);
	    break;
	case KNIGHT:
	    moveMask = Chess.knightMasks[pos] & ~all;
	    captureMask = Chess.knightMasks[pos] & opp;
	    break;
	case BISHOP:
	    long bishopMask = Chess.bishopMask(all, pos);
	    moveMask = bishopMask & ~all;
	    captureMask = bishopMask & opp;
	    break;
	case ROOK:
	    long rookMask = Chess.rookMask(all, pos);
	    moveMask = rookMask & ~all;
	    captureMask = rookMask & opp;
	    break;
	case QUEEN:
	    long queenMask = Chess.queenMask(all, pos);
	    moveMask = queenMask & ~all;
	    captureMask = queenMask & opp;
	    break;
	case KING:
	    moveMask = Chess.kingMasks[pos] & ~all;
	    moveMask |= Chess.castleMask(color, this);
	    captureMask = Chess.kingMasks[pos] & opp;
	    break;
	}
	moves.addAll(toMoves(pos, moveMask, false));
	moves.addAll(toMoves(pos, captureMask, true));
	return filterSafe(moves, color);
	/*if (type==KING) {
	    return filterSafe(moves, color);
	} else {
	    return moves;
	    }*/
	
	
    }

    public List<ChessMove> pieceMoves(String pos) {
	return pieceMoves(ChessMove.toIndex(pos));
    }

    //returns
    public static List<ChessMove> applyMask(List<ChessMove> moves, long startMask, long endMask) {
	List<Integer> starts = toIndices(startMask);
	List<Integer> ends = toIndices(endMask);
	return  moves.stream()
	    .filter(cm -> (starts.contains(cm.start) & ends.contains(cm.end)))
	    .collect(Collectors.toList());
    }
    //return only moves safe for pieces of given color
    public List<ChessMove> filterSafe(List<ChessMove> moves, int color) {
	return moves.stream()
	    .filter(cm -> {
		    ChessBoard c = clone();
		    c.makeMove(cm);
		    return c.attacking(c.getKingIndex(color), -color)==0L;})
	    .collect(Collectors.toList());
    }
	
    public static void main(String[] a) {
	ChessBoard b = new ChessBoard();
	b.place(WHITE, PAWN, "d7");
	b.makeMove("d7", "d8");
	pr(b.toString());
    }


    //debuggin prints
    static void pr(Object s) {
	System.out.println(s);
    }
    static void prl(long l) {
	pr(Long.toBinaryString(l));
    }
    static void prll(long l) {
	pr(Chess.longToString(l));
    }
}
