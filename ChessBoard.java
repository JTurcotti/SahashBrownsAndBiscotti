import java.util.List;
import java.util.LinkedList;

public class ChessBoard {
    // Default pieces for Chess with unique values so names can be referenced 
    // in the bitboard
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

    /**
     *Default constructor 
     *Initializes the default board layout
     */    

    public ChessBoard(){
	bbWhite = new long[6];
	bbBlack = new long[6];
    }

    public void setup() {
	bbWhite[PAWN] = ((1<<8)-1)<<8;
	bbWhite[BISHOP] = (1<<2) + (1<<5);
	bbWhite[KNIGHT] = (1<<1) + (1<<6);
	bbWhite[ROOK] = (1<<0) + (1<<7);
	bbWhite[QUEEN] = (1<<4);
	bbWhite[KING] = (1<<3);
	for (int i=0; i<bbBlack.length; i++) bbBlack[i]=Long.reverse(bbWhite[i]);

    }
	
    /**
     *toString method that returns the chess board 
     *@return String Visual representation of the present chessboard. The lowercase letter preceeding the uppercase letter indicates color. K = King, Q = Queen, R = Rook, B = Bishop, N = Knight, P = Pawn
     */    
    
    public String toString() {
	return toString(0L);
    }

    public String toString(long mask) {
	String out = "";
	for (int i=63; i>=0; i--) {
	    String s = "  ";
	    for (int j=0; j<bbWhite.length; j++)
		if (((1L << i) & bbWhite[j]) != 0) s = "w" + names[j];
	    for (int j=0; j<bbBlack.length; j++)
		if (((1L << i) & bbBlack[j]) != 0) s = "b" + names[j];

	    out += "\033[" + ((((1L<<i) & mask) != 0)? 45:((i/8+i%8)%2==0? 40: 47)) + ";" + ((((1L << i) & getWhite()) != 0)? 34: 31) + "m" + s + "\033[0m";
	    if (i%8==0) out +="\n";
	    
	}
	return out;
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

    /**
     *This method is used to get a bitboard containg all of the pieces 
     *@return long A bitboard containing all present pieces
     */

    public long getAll() {
	return getWhite() | getBlack();
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
    
    public void makeMove(ChessMove move) {
	int type = typeAtPosition(move.start);
	switch (colorAtPosition(move.start)) {
	case WHITE:
	    bbWhite[type] |= (1L << move.end); //add white piece at end
	    bbWhite[type] &= -1 *((1L << move.start)+1L);//remove white piece at start
	    bbBlack[type] &= -1 *((1L << move.end)+1L);//remove black piece at end
	    break;
	case BLACK:
	    bbBlack[type] |= (1L << move.end);//add black piece at end
	    bbBlack[type] &= -1 *((1L << move.start)+1L);//remove black piece at start
	    bbBlack[type] &= -1 *((1L << move.end)+1L);//remove white piece at end
	    break;
	    }
    }
    public void makeMove(String start, String end) {
	makeMove(new ChessMove(start, end));
    }

    //for debugging only
    public void place(int type, int color, String pos) {
	if (color==WHITE) {
	    bbWhite[type] |= (1L<<ChessMove.toIndex(pos));
	} else {
	    bbBlack[type] |= (1L<<ChessMove.toIndex(pos));
	}
    }
    
    public List<ChessMove> toMoves(int start, long ends, boolean capture) {
	List<ChessMove> moves = new LinkedList<>();
	for (int i=0; i<64; i++)
	    if (((1L<<i) & ends) != 0) moves.add(new ChessMove(start, i, capture));
	return moves;
    }

    public long moveMask(List<ChessMove> moves) {
	long moveMask = 0L;
	for (ChessMove move: moves) {
	    if (!move.capture) moveMask += (1L<<move.end);
	}
	return moveMask;
    }
    public long captureMask(List<ChessMove> moves) {
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
	long opp = color==WHITE? getBlack(): getWhite();
	long all = getAll();
	long moveMask = 0L;
	long captureMask = 0L;
	switch (typeAtPosition(pos)) {
	case PAWN:
	    moveMask = Chess.pawnMasks[pos][1-color] & ~all;
	    captureMask = Chess.pawnMasks[pos][2-color] & opp;
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
	    captureMask = queenMask & ~all;
	    break;
	case KING:
	    moveMask = Chess.kingMasks[pos] & ~all;
	    captureMask = Chess.kingMasks[pos] & opp;
	    break;
	}
	moves.addAll(toMoves(pos, moveMask, false));
	moves.addAll(toMoves(pos, captureMask, true));
	
	return moves;
    }

    public List<ChessMove> pieceMoves(String pos) {
	return pieceMoves(ChessMove.toIndex(pos));
    }
    

    public boolean validMove(ChessMove move) {
	List<ChessMove> valid = pieceMoves(move.start);
	return valid.contains(move);// || valid.contains(move.opposite());
    }

    public void playerMoveCycle() {
	System.out.println(this);
	while(true) {
	    System.out.print("enter move start: ");
	    String start = System.console().readLine();
	    System.out.print("enter move end: ");
	    String end = System.console().readLine();
	    try {
		ChessMove move = new ChessMove(start, end);
		if (validMove(move)) {
		    if (((1L<<move.end) & getAll())!=0) System.out.println("Capture made!");
		    makeMove(move);
		    System.out.println(this.toString(0L));
		} else {
		    System.out.println("invalid move for given piece, try:");
		    for (ChessMove valid: pieceMoves(start)) System.out.print(valid + " ");
		    System.out.print("\n");
		}
	    } catch (Exception e) {
		System.out.println("invalid move syntax");
	    }
	}
    }
		    
    
    public static void main(String[] a) {
	ChessBoard b = new ChessBoard();
	//b.setup();
	/*b.place(QUEEN, WHITE, "f3");
	pr(b.toString(64L));
	*/
	b.playerMoveCycle();
	/*b.makeMove("g7", "g3");
	pr(b);
	for (ChessMove cm: b.pieceMoves("e1")) pr(cm);*/
    }

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
