//Code implemented/developed from https://proghammer.wordpress.com/2010/08/10/chess01-dragging-game-pieces/

import java.awt.Graphics;
import java.awt.Image;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * all x and y coordinates point to the upper left position of a component all
 * lists are treated as 0 being the bottom and size-1 being the top piece
 * 
 */
public class ChessGui extends JPanel {
    
    private static final long serialVersionUID = 3114147670071466558L;
    
    private static final int WHITE = 0;
    private static final int BLACK = 1;
    
    private static final int ROOK = 1;
    private static final int KNIGHT = 2;
    private static final int BISHOP = 3;
    private static final int QUEEN = 4;
    private static final int KING = 5;
    private static final int PAWN = 6;
    
    private static final int BOARD_START_X = 301;
    private static final int BOARD_START_Y = 51;
    
    private static final int TILE_OFFSET_X = 50;
    private static final int TILE_OFFSET_Y = 50;
    
    private Image imgBackground;
    
    // 0 = bottom, size-1 = top
    private List<Piece> pieces = new ArrayList<Piece>();
    
    public ChessGui() {
	// load and set background image
	URL urlBackgroundImg = getClass().getResource("/img/board.png");
	this.imgBackground = new ImageIcon(urlBackgroundImg).getImage();

	// create application frame and set visible
	//
	JFrame f = new JFrame();
	f.setVisible(true);
	f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	f.add(this);
	f.setResizable(false);
	f.setSize(this.imgBackground.getWidth(null), this.imgBackground.getHeight(null));
    }
    
    /**
     * load image for given color and type. This method translates the color
     * and type information into a filename and loads that particular file.
     * 
     * @param color color constant
     * @param type type constant
     * @return image
     */
    private Image getImageForPiece(int color, int type) {
	String filename = "";
	
	filename += (color == WHITE ? "w" : "b");
	switch (type) {
	case BISHOP:
	    filename += "b";
	    break;
	case KING:
	    filename += "k";
	    break;
	case KNIGHT:
	    filename += "n";
	    break;
	case PAWN:
	    filename += "p";
	    break;
	case QUEEN:
	    filename += "q";
	    break;
	case ROOK:
	    filename += "r";
	    break;
	}
	filename += ".png";
	
	URL urlPieceImg = getClass().getResource("/img/" + filename);
	return new ImageIcon(urlPieceImg).getImage();
    }
    
    
    @Override
    protected void paintComponent(Graphics g) {
	g.drawImage(this.imgBackground, 0, 0, null);
	for (Piece piece : this.pieces) {
	    g.drawImage(piece.getImage(), piece.getX(), piece.getY(), null);
	}
    }
    
    public static void main(String[] args) {
	new ChessGui();
    }    
}
