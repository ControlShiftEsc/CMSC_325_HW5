/*
    Name: Michael Wood Jr
    Course: CMSC-335-7380 
    Assignment: Homework 5
    Date: May 9, 2023
    
    Description: This Game is a recreation of the connect four game.
    This is a fully functional connect four game that allow two players 
    to be on two different teams, red and yellow. The game has a HUD that
    shows the title of the game, current players turn and Tips on how to play.
    The game has a custom camera that prevents the user from looking upside down
    and also reallocates some of the camera controls. The game uses gravity along
    with collision detection to determine who's turn it is.
*/

package mygame;

import com.jme3.app.FlyCamAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import static com.jme3.bullet.PhysicsSpace.getPhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.font.BitmapText;
import com.jme3.input.CameraInput;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.jme3.util.SkyFactory;

public class Main extends SimpleApplication implements AnalogListener, ActionListener, PhysicsCollisionListener{
    
    private BulletAppState bulletAppState;
    private Material boxMat;
    private Material redDiscMat;
    private Material yellowDiscMat;
    private int turn = 0;
    private Geometry disc, newDisc;  
    private String color = "Red";
    
   
    private Box selectionBox;
    private Geometry selectColumn;
    private Vector3f initialUpVec, selectColumnPos;
    private final int xPos[] = {-51,-33,-16,0,16,33,51};
    private int pos = 3;
    private final int gridHeight = 6, gridWidth = 7;
    private final String[][] grid = new String[gridHeight][gridWidth];
    
    private final boolean enabled = true;
    private boolean turnComplete = true;
    private final float rotationSpeed = 1f;
    
    private BitmapText scoreboard;
    
    final private static String[] mappings = new String[]{
        "LeftSelect",
        "RightSelect",
        "UpSelect",
        "DownSelect",
        "Drop",
        
        CameraInput.FLYCAM_LEFT,
        CameraInput.FLYCAM_RIGHT,
        CameraInput.FLYCAM_UP,
        CameraInput.FLYCAM_DOWN,

        CameraInput.FLYCAM_STRAFELEFT,
        CameraInput.FLYCAM_STRAFERIGHT,
        CameraInput.FLYCAM_FORWARD,
        CameraInput.FLYCAM_BACKWARD,

        CameraInput.FLYCAM_RISE,
        CameraInput.FLYCAM_LOWER
    };
    
    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        // Removes fly cam from App State and hides cursor
        stateManager.detach(stateManager.getState(FlyCamAppState.class));
        inputManager.setCursorVisible(false);
        
        setupKeys();
        
        // Set cam location
        cam.setLocation(new Vector3f(0,75,125));
        initialUpVec = cam.getUp().clone();
        
        // Initializes Variables
        selectColumnPos = new Vector3f(xPos[pos],125,-50);
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        
        // Creates and Adds floor
        Node sceneNode = new Node("Scene Node");
        Spatial sceneModel = assetManager.loadModel("Models/ConnectFourBoard/ConnectFourBoard2.j3o");
        CollisionShape sceneShape = CollisionShapeFactory.createMeshShape(sceneModel);
        RigidBodyControl landscape = new RigidBodyControl(sceneShape, 0);   
        sceneModel.addControl(landscape);        
        sceneNode.attachChild(sceneModel);
        
        // Creates Geometry Shapes and Set colors
        disc = (Geometry) assetManager.loadModel("Models/RedDisc/Disc.obj");
        boxMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        redDiscMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        redDiscMat.setColor("Color", ColorRGBA.Red);
        yellowDiscMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        yellowDiscMat.setColor("Color", ColorRGBA.Yellow);
        
        // Add Scene to rootNode
        rootNode.attachChild(sceneNode);
        bulletAppState.getPhysicsSpace().add(landscape);
        bulletAppState.getPhysicsSpace().setMaxSubSteps(8);
        
        // Adds Texture to Skybox
        Texture west = assetManager.loadTexture("Textures/wall3.jpg");
        Texture east = assetManager.loadTexture("Textures/wall2.jpg");
        Texture north = assetManager.loadTexture("Textures/wall3.jpg");
        Texture south = assetManager.loadTexture("Textures/wall3.jpg");
        Texture ceiling = assetManager.loadTexture("Textures/ceiling.jpg");
        Texture floor = assetManager.loadTexture("Textures/floor.jpg");
        
        Spatial skyBox = SkyFactory.createSky(assetManager, west, east, north, south, ceiling, floor);
        sceneNode.attachChild(skyBox);
        
        // Creates column selection box
        columnSelect();
        
        // Adds HUD
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        Node hudNode = new Node("Heads-up Display Node");        
        
        // Displays Title of game
        BitmapText title = new BitmapText(guiFont, false);
        title.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        title.setText("Connect Four"); 
        title.setLocalTranslation(
                settings.getWidth() - settings.getWidth()/5 - guiFont.getCharSet().getRenderedSize() / 3 * 2,
                settings.getHeight() - (settings.getHeight()/10), 0);
        hudNode.attachChild(title);
        
        // Displays current players turn
        scoreboard = new BitmapText(guiFont, false);
        scoreboard.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        scoreboard.setText(color + "'s turn.");
        scoreboard.setLocalTranslation(
                title.getLocalTranslation().x,
                title.getLocalTranslation().y - title.getLineHeight(), 0);
        hudNode.attachChild(scoreboard);
        
        // Displays How to Play in HUD
        BitmapText tips = new BitmapText(guiFont, false);
        tips.setSize(guiFont.getCharSet().getRenderedSize());
        tips.setText(
                "\n How to play: \n" +
                "  - Players must alternate turns, and only one\n" +
                "    disc can be dropped in each turn.\n" +
                "  - On your turn, drop one of your colored discs from\n" +
                "    the top into any of the seven slots.\n" +
                "  - The game ends when there is a 4-in-a-row \n" + 
                "    (either vertically, horizontally, or diagonally)\n" +
                "    or a stalemate.\n"); 
        tips.setLocalTranslation(
                scoreboard.getLocalTranslation().x,
                scoreboard.getLocalTranslation().y - scoreboard.getLineHeight(), 0);
        hudNode.attachChild(tips);
        // Adds Hud to GUI Node
        guiNode.attachChild(hudNode);
        
        // Adds background to HUD
        Geometry background = new Geometry("background", new Quad(tips.getLineWidth(), -title.getLineWidth()*2));
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        ColorRGBA bgColor = new ColorRGBA(0f, 0f, 0f, .8f);
        mat.setColor("Color", bgColor);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        background.setMaterial(mat);
        background.setLocalTranslation(title.getLocalTranslation().x, title.getLocalTranslation().y, -1);
        getGuiNode().attachChild(background);
    }

    @Override
    public void simpleUpdate(float tpf) {
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }
    
    private void setupKeys() {
        // Sets Controls
        inputManager.addMapping("LeftSelect", new KeyTrigger(KeyInput.KEY_A), new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("RightSelect", new KeyTrigger(KeyInput.KEY_D), new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping("UpSelect", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("DownSelect", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Drop", new KeyTrigger(KeyInput.KEY_SPACE), new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping(CameraInput.FLYCAM_LEFT, new MouseAxisTrigger(MouseInput.AXIS_X, true));
        inputManager.addMapping(CameraInput.FLYCAM_RIGHT, new MouseAxisTrigger(MouseInput.AXIS_X, false));
        inputManager.addMapping(CameraInput.FLYCAM_UP, new MouseAxisTrigger(MouseInput.AXIS_Y, false),
                new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping(CameraInput.FLYCAM_DOWN, new MouseAxisTrigger(MouseInput.AXIS_Y, true),
                new KeyTrigger(KeyInput.KEY_DOWN));     
        inputManager.addMapping(CameraInput.FLYCAM_STRAFELEFT, new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping(CameraInput.FLYCAM_STRAFERIGHT, new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping(CameraInput.FLYCAM_FORWARD, new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping(CameraInput.FLYCAM_BACKWARD, new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping(CameraInput.FLYCAM_RISE, new KeyTrigger(KeyInput.KEY_Q));
        inputManager.addMapping(CameraInput.FLYCAM_LOWER, new KeyTrigger(KeyInput.KEY_Z));   
        inputManager.addListener(this, mappings);
        
    }
    
    public void columnSelect(){
        // Sets selector color
        boxMat.setColor("Color", ColorRGBA.Red);
        
        // Creates box as shape of selector
        selectionBox = new Box(1, 1, 1);
        selectColumn = new Geometry("Box", selectionBox);
        selectColumn.setMaterial(boxMat);
        
        // Adds selector to root node
        rootNode.attachChild(selectColumn);
        
        // Sets column position of selector and moves it
        selectColumnPos.x = xPos[pos];
        selectColumn.move(selectColumnPos);
    }
    
    public void makeDisc(String color){
        // Set turn Complete to false
        turnComplete = false;
        // Makes disc and sets name of disc to include color and turn it was created.
        newDisc = disc.clone();
        newDisc.setName(color + "Disc-" + turn);
        
        // Sets disc to players color
        if(turn%2==1){
            newDisc.setMaterial(redDiscMat);
        }else{
            newDisc.setMaterial(yellowDiscMat);
        }
        
        // Add new disc to rootnode
        rootNode.attachChild(newDisc);
        
        // Rotates disc and Drops it in selected position
        newDisc.rotate((float) (-Math.PI / 2.0), 0, 0);
        newDisc.move(xPos[pos], 100, -50);
                
        // Adds Physics to Disc
        RigidBodyControl discPhysics = new RigidBodyControl(2f);
        newDisc.addControl(discPhysics);
        bulletAppState.getPhysicsSpace().add(newDisc);
        // Adds Collision Listener to disc
        getPhysicsSpace().addCollisionListener(this);
        
        // Calculates the expected disc drop location.
        for(int i = 0; i<gridHeight;i++){
            if(grid[i][pos] == null){
                grid[i][pos] = newDisc.getName();
                break;
            }
        }
        
    }

    @Override
    public void onAction(String binding, boolean isPressed, float tpf) {
        // Adds player control
        if(("LeftSelect".equals(binding) || "UpSelect".equals(binding)) && isPressed){
            // Moves selection to the left if it's position is greater than -50 in the x direction.
            if(pos>0){
                pos--;
            }
        }else if(("RightSelect".equals(binding) || "DownSelect".equals(binding)) && isPressed){
            // Moves selection to the right if it's position is less than 50 in the x direction.
            if(pos<xPos.length-1){
                pos++;
            }
        }else if("Drop".equals(binding) && isPressed && turnComplete == true){
            // Increase the turn counter makes make disc and drops it in column
            turn++;
            makeDisc(color);
            // Resets pos to the center position
            pos = 3;
            // Changes selector color to the next players color
            if(turn%2==1){
                boxMat.setColor("Color", ColorRGBA.Yellow);
            }else{
                boxMat.setColor("Color", ColorRGBA.Red);
            }
        }
        // Sets selector pos and move to that column
        selectColumnPos.x = xPos[pos];
        selectColumn.setLocalTranslation(selectColumnPos);
    }
    
    @Override
    public void onAnalog(String name, float value, float tpf) {
        /* This function was taken directly from the default flyCam class with a 
        few modifications. */
        if (!enabled)
            return;

        if (name.equals(CameraInput.FLYCAM_LEFT)) {
            rotateCamera(value, initialUpVec);
        } else if (name.equals(CameraInput.FLYCAM_RIGHT)) {
            rotateCamera(-value, initialUpVec);
        } else if (name.equals(CameraInput.FLYCAM_UP) && cam.getUp().y >= .7f) {
            rotateCamera(-value, cam.getLeft());
        } else if (name.equals(CameraInput.FLYCAM_DOWN) && cam.getUp().y >= .7f) {
            rotateCamera(value, cam.getLeft());
        }
    }
    
    protected void rotateCamera(float value, Vector3f axis) {
        /* This function was taken directly from the default flyCam class with a 
        few modifications. */
        Matrix3f mat = new Matrix3f();
        mat.fromAngleNormalAxis(rotationSpeed * value, axis);

        Vector3f up = cam.getUp();
        Vector3f left = cam.getLeft();
        Vector3f dir = cam.getDirection();

        mat.mult(up, up);
        mat.mult(left, left);
        mat.mult(dir, dir);
        
        // This modification prevents the camera from flipping upside down.
        if (up.getY() < 0.7f) {
            return;
        }
        
        Quaternion q = new Quaternion();
        q.fromAxes(left, up, dir);
        q.normalizeLocal();

        cam.setAxes(q);
    }

    @Override
    public void collision(PhysicsCollisionEvent pce) {
        // Gets x and y coordinates of collision
        float xVal = newDisc.getLocalTranslation().x;
        float yVal = newDisc.getLocalTranslation().y;
        // Gets approximate grid location of collision
        int xGrid = Math.round((xVal/16))+3;
        int yGrid = Math.round((yVal-20)/14);
        // Checks if collision occurs in the expected final drop location
        if(xGrid >= 0 && yGrid >= 0 && xGrid < 7 && yGrid < 6 && grid[yGrid][xGrid] != null && grid[yGrid][xGrid].equals(newDisc.getName())){
            // Checks if there was a winner
            if(winner(color)==true){
                // Inform users who won.
                scoreboard.setText(color + " wins.");
                if(color.equals("Red")){
                    boxMat.setColor("Color", ColorRGBA.Red);
                }else{
                    boxMat.setColor("Color", ColorRGBA.Yellow);
                }
            }else{
                // Checks who's turn it is and sets appropiate color values.
                if(turn%2==1){
                    color = "Yellow";
                }else{
                    color = "Red";
                }
                
                // Checks if the is a stalemate.
                if(turn == gridWidth * gridHeight){
                    // Declare Stalemate
                    turnComplete = false;
                    scoreboard.setText("No Winner - Stalemate.");
                }else{
                    // End turn and Update current user's turn in HUD.
                    turnComplete = true;
                    scoreboard.setText(color + "'s turn.");
                }
            }
        }
    }
    
    public boolean winner(String color){        
        // Horizontal Win Check 
        for (int j = 0; j<gridWidth-3 ; j++ ){
            for (int i = 0; i<gridHeight; i++){
                if(grid[i][j] != null && grid[i][j+1] != null && grid[i][j+2] != null && grid[i][j+3] != null){
                    if (grid[i][j].contains(color) && grid[i][j+1].contains(color) && grid[i][j+2].contains(color) && grid[i][j+3].contains(color)){
                        return true;
                    }       
                }        
            }
        }
        // Vertical Win Check
        for (int i = 0; i<gridHeight-3 ; i++ ){
            for (int j = 0; j<gridWidth; j++){
                if(grid[i][j] != null && grid[i+1][j] != null && grid[i+2][j] != null && grid[i+3][j] != null){
                    if (grid[i][j].contains(color) && grid[i+1][j].contains(color) && grid[i+2][j].contains(color) && grid[i+3][j].contains(color)){
                        return true;
                    }           
                }
            }
        }
        // ascendingDiagonalCheck 
        for (int i=3; i<gridHeight; i++){
            for (int j=0; j<gridWidth-3; j++){
                if(grid[i][j] != null && grid[i-1][j+1] != null && grid[i-2][j+2] != null && grid[i-3][j+3] != null){
                    if (grid[i][j].contains(color) && grid[i-1][j+1].contains(color) && grid[i-2][j+2].contains(color) && grid[i-3][j+3].contains(color)){
                        return true;
                    }
                }
            }
        }
        // descendingDiagonalCheck
        for (int i=3; i<gridHeight; i++){
            for (int j=3; j<gridWidth; j++){
                if(grid[i][j] != null && grid[i-1][j-1] != null && grid[i-2][j-2] != null && grid[i-3][j-3] != null){
                    if (grid[i][j].contains(color) && grid[i-1][j-1].contains(color) && grid[i-2][j-2].contains(color) && grid[i-3][j-3].contains(color)){
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
