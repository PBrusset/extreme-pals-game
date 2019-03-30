package logic;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import intermediary.Settings;

public class NPC {
	
    /* **************** */
    /* Image properties */
    /* **************** */

    // Current frame in the animation
    private BufferedImage currentFrame;

    // All the bufferedImages used in the character's animation
    private BufferedImage   idle_R;
    private BufferedImage   idle_L;
    private BufferedImage[] run_R;
    private BufferedImage[] run_L;

    // Determines the currentFrame to be used in a run animation
    private int currentFrameNumber = 0;

    // The direction that the character is currently facing
    // Used to tell which way facing animations to use
    private int facingDirection;
    
    /* **************************** */
    /* Position and size properties */
    /* **************************** */

    // The initial position of the character
    public static final int NPC_START_X = 20 * Settings.TILE_SIZE;
    public static final int NPC_START_Y = 4 * Settings.TILE_SIZE;
    // The current position of the character
    private int currentX;
    private int currentY;
    
 // The boundingBox (sometimes called hit box) is a rectangle around the
    // character
    // It defines the space occupied by the character at the specific moment
    // Used for detecting collisions
    private Rectangle boundingBox;

    // Dimensions of the main character (used to set the boundingBox)
    private final int NPC_HEIGHT = 64;
    private final int NPC_WIDTH  = 32;
    
    private final static int DISPLACEMENT = 2;
    
    // MOVE_COUNTER_THRESH is explained in the setFrameNumber function's comment
    private static final int MOVE_COUNTER_THRESH = 5;
    
 // moveCounter is explained in the setFrameNumber function's comment
    private int moveCounter = 0;

    // True when the character is falling
    private boolean falling;

    // Idle is 'true' if the character is not moving, false otherwise
    private boolean idle;
    
    public void resetPosition() {
        currentX = NPC_START_X;
        currentY = NPC_START_Y;

        boundingBox = new Rectangle(NPC_START_X, currentY, NPC_WIDTH, NPC_HEIGHT);

        // Initially, the NPC is standing still with his head turned left
        facingDirection = KeyEvent.VK_LEFT;
        currentFrame = idle_R;
        falling = false;

        idle = true;
    }
    
    public NPC() {
        // Initialise the buffers that will store the run sprites
        run_L = new BufferedImage[Settings.BOY_RUN_FRAMES];
        run_R = new BufferedImage[Settings.BOY_RUN_FRAMES];

        // Load all the sprites needed to animate the character
        try {
            BufferedImage spritesheet = ImageIO.read(getClass().getResource(Settings.playerSpritesheet));

            idle_R = spritesheet.getSubimage(0,
                                             0,
                                             Settings.BOY_SPRITE_WIDTH,
                                             Settings.BOY_SPRITE_HEIGHT);

            idle_L = spritesheet.getSubimage(0,
                                             Settings.BOY_SPRITE_HEIGHT,
                                             Settings.BOY_SPRITE_WIDTH,
                                             Settings.BOY_SPRITE_HEIGHT);

            for (int i = 0; i < Settings.BOY_RUN_FRAMES; i++) {
                run_R[i] = spritesheet.getSubimage((i + 1) * Settings.BOY_SPRITE_WIDTH,
                                                   0,
                                                   Settings.BOY_SPRITE_WIDTH, 
                                                   Settings.BOY_SPRITE_HEIGHT);

                run_L[i] = spritesheet.getSubimage((i + 1) * Settings.BOY_SPRITE_WIDTH,
                                                   Settings.BOY_SPRITE_HEIGHT,
                                                   Settings.BOY_SPRITE_WIDTH,
                                                   Settings.BOY_SPRITE_HEIGHT);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        resetPosition();
    }
    
    private void setFrameNumber() {
        currentFrameNumber = moveCounter / MOVE_COUNTER_THRESH;
        currentFrameNumber %= Settings.BOY_RUN_FRAMES;
        moveCounter %= MOVE_COUNTER_THRESH * Settings.BOY_RUN_FRAMES;
    }
    
    /* ****************** */
    /* Movement functions */
    /* ****************** */
    public void change_direction() {
    	if(facingDirection == KeyEvent.VK_LEFT) {
    		facingDirection = KeyEvent.VK_RIGHT;
    	}
    	else {
    		facingDirection = KeyEvent.VK_LEFT;
    	}
    }
    
    public void move() {
    	if(facingDirection == KeyEvent.VK_LEFT) {
    		moveleft();
    	}
    	else {
    		moveright();
    	}
    }
    
    public void moveleft() {
        idle = false;
        // Attempt to move left by DISPLACEMENT amount
        currentX = (int) checkMove(currentX, currentX - DISPLACEMENTPLUS);
        boundingBox.setLocation(currentX, currentY);
    }
    
    public void moveright() {
        idle = false;
        // Attempt to move left by DISPLACEMENT amount
        currentX = (int) checkMove(currentX, currentX + DISPLACEMENTPLUS);
        boundingBox.setLocation(currentX, currentY);
    }
    
    double DISPLACEMENTPLUS = 5;


    // Check whether the location the player wants to move into
    // Is not out of bounds and does not contain a block
    // If so, return the new position
    // Otherwise, return the old one
    private double checkMove(int oldX, double newX) {
        if (newX <= 0) {
        	change_direction();
            return 5;
        }
        if (newX >1240) {
        	change_direction();
        	return 1200;
        }

        if (newX >= (Settings.WINDOW_WIDTH - NPC_WIDTH)) {
            return (Settings.WINDOW_WIDTH - NPC_WIDTH);
        }

        boundingBox.setLocation((int)newX, currentY);

        // Get the tile position (in the tiled map)
        // Relative to the tile in front of the character
        int footCol;

        int footX = (int) boundingBox.getMaxX();
        footCol = (footX / Settings.TILE_SIZE) - 1;

        // The character is at the edge of the map and the tile in front of it
        // Would be out of bounds, so skip checking it
        if (footCol < 0 || footCol >= World.cols) {
            return newX;
        }

        int footY = (int) (boundingBox.getMaxY());
        int footRow = ((footY - 1) / Settings.TILE_SIZE);

        Block tileInFrontOfFoot = World.map[footRow][footCol];

        if (!tileInFrontOfFoot.empty() && tileInFrontOfFoot.intersects(boundingBox)) {
            return oldX;
        }
        return newX;
    }


    // Checks and handles possible collisions with static blocks (Block class)
    private void checkBlockCollisions() {
        // If the character is jumping, his head must not touch a block;
        // If it touches a block, stop the ascending phase of the jump (start
        // falling)

        // Row position of the cell above the character's head (in the tiled
        // map)
        int upRow = (int) ((boundingBox.getMinY() - 1) / Settings.TILE_SIZE);

        // Tile position relative to the upper-left corner of the character's
        // bounding box
        int upLeftCornerCol = (int) (boundingBox.getMinX() / Settings.TILE_SIZE);
        Block leftCornerBlock = null;

        if (upRow >= 0 && upLeftCornerCol >= 0) {
            leftCornerBlock = World.map[upRow][upLeftCornerCol];
        }

        // Tile position relative to the upper-right corner of the character's
        // bounding box
        int upRightCornerCol = (int) ((boundingBox.getMaxX()) / Settings.TILE_SIZE);
        Block rightCornerBlock = null;

        if (upRow >= 0 && upRightCornerCol < World.cols) {
            rightCornerBlock = World.map[upRow][upRightCornerCol];
        }

        if ((leftCornerBlock != null && !leftCornerBlock.empty() && leftCornerBlock.intersects(boundingBox))
            || (rightCornerBlock != null && !rightCornerBlock.empty() && rightCornerBlock.intersects(boundingBox))) {
            // If an upper corner is intersecting a block, stop the jumping
            // phase
            // And start the falling phase, setting the jump_count to 0
            falling = true;
        }
    }

    // Sets an idle position as current frame
    public void stop() {
        // If the last direction was right, set the idle-right position
        // As the current frame
        if (facingDirection == KeyEvent.VK_RIGHT) {
            currentFrame = idle_R;
            // Otherwise set the idle-left position
        } else {
            currentFrame = idle_L;
        }

        idle = true;
    }

    public void handleFalling() {
        // Skip falling altogether if the character is jumping

        int currentRow = currentY / Settings.TILE_SIZE;

        // Since the character is wider than one tile but less wide than two
        // Check the two tiles below the character
        int lowLeftX = (int) boundingBox.getMinX() + 1;
        int lowRightX = (int) boundingBox.getMaxX() - 1;
        int lowLeftCol = lowLeftX / Settings.TILE_SIZE;
        int lowRightCol = lowRightX / Settings.TILE_SIZE;

        Block lowLeftBlock = null;

        if (lowLeftCol < World.cols) {
            lowLeftBlock = World.map[currentRow + 1][lowLeftCol];
        }

        Block lowRightBlock = null;

        if (lowRightCol < World.cols) {
            lowRightBlock = World.map[currentRow + 1][lowRightCol];
        }

        // If both of the tiles below the character are thin air or beyond map
        // edge
        // Make the character fall down DISPLACEMENT units
        if ((lowLeftBlock == null || lowLeftBlock.empty())
            && (lowRightBlock == null || lowRightBlock.empty())) {
            falling = true;
            currentY += DISPLACEMENT;
            boundingBox.setLocation(currentX, currentY);
        } else {
            falling = false;
        }
    }

    /* ******* */
    /* Getters */
    /* ******* */

    public BufferedImage getCurrentFrame() {
        return currentFrame;
    }

    public int getCurrentX() {
        return currentX;
    }

    public int getCurrentY() {
        return currentY;
    }

    public Rectangle getBoundingBox() {
        return boundingBox;
    }

    public boolean getFalling() {
        return falling;
    }

    public boolean outOfBounds() {
        return (currentX >= Settings.WINDOW_WIDTH);
    }
    
    public boolean intersects_npc(Rectangle BoyboundingBox) {
    	//System.out.println("boy" + BoyboundingBox);
    	//System.out.println("npc" + this.boundingBox);
        return this.boundingBox.intersects(BoyboundingBox);
    }
}
