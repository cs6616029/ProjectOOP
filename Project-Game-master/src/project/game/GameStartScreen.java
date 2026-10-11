package project.game;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.function.BiConsumer;
import javax.imageio.ImageIO;
import javax.swing.*;

public final class GameStartScreen extends JPanel {
    private static final int DESIGN_WIDTH = 800, DESIGN_HEIGHT = 600;
    private static final Color GOLD = new Color(241,197,113), WHITE = new Color(247,235,214);
    private static final Color MUTED = new Color(158,164,157), RED = new Color(191,69,59), PANEL = new Color(16,22,25,238);
    private static final Font PIXEL_FONT = loadFont();
    static Font pixelFont(float size) { return PIXEL_FONT.deriveFont(size); }
    private enum Page { START, PLAYER, MAP, DIFFICULTY }
    @FunctionalInterface
    public interface GameStartHandler {
        void start(Player.CharacterType character, String map, Difficulty difficulty);
    }
    private static final Player.CharacterType[] CHARACTERS = Player.CharacterType.values();
    private static final String[] HERO_NAMES = {"SWORDSMAN","IRON MAN","STRONG BOY"};
    private static final String[] HERO_ROLES = {"THE BLADE","THE ARSENAL","THE POWERHOUSE"};
    private static final String[] SKILLS = {"SWORD","LASER","AURAFARMING"};
    private static final MapOption[] MAPS = {
        new MapOption("Enchanted Forest","/project/game/Map/forest.jpg","GREEN WILDS"),
        new MapOption("Desert","/project/game/Map/desert.jpg","GOLDEN SANDS"),
        new MapOption("Night","/project/game/Map/night.jpg","AFTER DARK"),
        new MapOption("Lava","/project/game/Map/lava.jpg","ASH & EMBER")
    };
    private final GameStartHandler onGameStart;
    private final SpriteAnimation[] characterPreviews = new SpriteAnimation[CHARACTERS.length];
    private final BufferedImage[] mapPreviews = new BufferedImage[MAPS.length];
    private final Timer animationTimer;
    private Page page = Page.START;
    private int selectedCharacter, selectedMap, selectedDifficulty, hoveredIndex = -1;
    private boolean primaryButtonHovered, backButtonHovered, launching;

    public GameStartScreen(BiConsumer<Player.CharacterType,String> onGameStart) {
        this((character, map, difficulty) -> onGameStart.accept(character, map));
    }

    public GameStartScreen(GameStartHandler onGameStart) {
        this.onGameStart = onGameStart;
        setPreferredSize(new Dimension(900,675)); setFocusable(true);
        for(int i=0;i<CHARACTERS.length;i++) characterPreviews[i]=SpriteAnimation.load(CHARACTERS[i].getIdleSpriteResource(),8);
        for(int i=0;i<MAPS.length;i++) mapPreviews[i]=loadImage(MAPS[i].resource);
        MouseAdapter mouse=new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { if(e.getButton()==MouseEvent.BUTTON1) handleClick(toDesignX(e.getX()),toDesignY(e.getY())); }
            @Override public void mouseMoved(MouseEvent e) { updateHover(toDesignX(e.getX()),toDesignY(e.getY())); }
            @Override public void mouseExited(MouseEvent e) { updateHover(-1,-1); }
        };
        addMouseListener(mouse); addMouseMotionListener(mouse);
        bind("ENTER","continue",this::advance); bind("ESCAPE","back",this::back);
        bind("LEFT","previous",()->selectBy(-1)); bind("RIGHT","next",()->selectBy(1));
        bind("UP","up",()->selectBy(page==Page.MAP?-2:-1)); bind("DOWN","down",()->selectBy(page==Page.MAP?2:1));
        animationTimer=new Timer(100,e->repaint()); animationTimer.start();
    }
    @Override public void addNotify() { super.addNotify(); if(!launching) animationTimer.start(); }
    @Override public void removeNotify() { animationTimer.stop(); super.removeNotify(); }
    private void bind(String key,String name,Runnable action) {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key),name);
        getActionMap().put(name,new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { action.run(); } });
    }
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics); Graphics2D g=(Graphics2D)graphics.create();
        g.scale(getWidth()/800.0,getHeight()/600.0);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        drawBackground(g);
        if(page==Page.START) drawStartPage(g); else if(page==Page.PLAYER) drawPlayerPage(g); else if(page==Page.MAP) drawMapPage(g); else drawDifficultyPage(g);
        rightText(g,"ISEKAI SURVIVORS",10,MUTED,752,586); g.dispose();
    }
    private void drawBackground(Graphics2D g) {
        cover(g,mapPreviews[page==Page.MAP||page==Page.DIFFICULTY?selectedMap:0],0,0,800,600);
        g.setColor(new Color(5,12,14,190)); g.fillRect(0,0,800,600);
        g.setPaint(new GradientPaint(0,0,new Color(7,11,17,75),0,600,new Color(8,10,15,245))); g.fillRect(0,0,800,600);
        long phase=System.currentTimeMillis()/180;
        for(int i=0;i<24;i++) {
            int x=32+i*137%738, y=(int)((i*97+phase)%560)+20;
            g.setColor(new Color(241,197,113,30+i%4*14)); g.fillRect(x,y,i%3==0?3:2,2);
        }
        g.setColor(new Color(241,197,113,32)); g.drawRect(20,20,759,559);
        for(int x:new int[]{20,768}) for(int y:new int[]{20,568}) {
            g.setColor(GOLD);
            g.fillRect(x,y==20?y:y+10,12,2);
            g.fillRect(x==20?x:x+10,y,2,12);
        }
    }
    private void drawStartPage(Graphics2D g) {
        text(g,"VS / SURVIVAL",14,GOLD,48,56); rightText(g,"3 HEROES  /  4 WORLDS",12,MUTED,752,56);
        centered(g,"SURVIVE. EVOLVE. CONQUER.",14,WHITE,400,109);
        title(g,"ISEKAI",54,400,179,WHITE); title(g,"SURVIVORS",64,400,242,GOLD);
        g.setColor(RED); g.fillRect(360,260,80,3);
        for(int i=0;i<CHARACTERS.length;i++) {
            int cx=400+(i-1)*118; g.setColor(new Color(0,0,0,125)); g.fillOval(cx-34,348,68,12);
            hero(g,i,cx,290,i==1?138:128);
        }
        drawButton(g,primaryButton(),"START GAME  >",true,primaryButtonHovered);
        centered(g,"Pick your hero. Find your world.",13,WHITE,400,478);
        centered(g,"W A S D / ARROWS TO MOVE     |     AUTO ATTACK",12,MUTED,400,535);
        centered(g,"ENTER TO BEGIN",11,GOLD,400,556);
    }
    private void drawHeader(Graphics2D g,String title) {
        text(g,"ISEKAI SURVIVORS",12,GOLD,48,49); rightText(g,page==Page.PLAYER?"01 / HERO":page==Page.MAP?"02 / WORLD":"03 / DIFFICULTY",12,MUTED,752,49);
        text(g,title,34,WHITE,48,104);
        g.setColor(new Color(241,197,113,60)); g.fillRect(48,149,704,1);
    }
    private void drawPlayerPage(Graphics2D g) {
        drawHeader(g,"CHOOSE YOUR HERO");
        for(int i=0;i<CHARACTERS.length;i++) {
            Rectangle card=playerCard(i); boolean selected=selectedCharacter==i;
            drawCard(g,card,selected,hoveredIndex==i);
            text(g,"0"+(i+1),14,selected?GOLD:MUTED,card.x+16,card.y+24);
            rightText(g,HERO_ROLES[i],10,MUTED,card.x+card.width-16,card.y+24);
            centered(g,HERO_NAMES[i],22,WHITE,card.x+card.width/2,card.y+59);
            g.setColor(new Color(241,197,113,selected?30:12)); g.fillRect(card.x+48,card.y+80,card.width-96,104);
            g.setColor(new Color(0,0,0,130)); g.fillOval(card.x+72,card.y+175,76,12);
            hero(g,i,card.x+card.width/2,card.y+85,i==2?156:144);
            centered(g,"STARTING SKILL",10,MUTED,card.x+card.width/2,card.y+204);
            centered(g,SKILLS[i],17,GOLD,card.x+card.width/2,card.y+227);
            if(selected) { g.setColor(GOLD); g.fillRect(card.x+1,card.y+card.height-21,card.width-2,20); }
            centered(g,selected?"SELECTED":"CLICK TO SELECT",10,selected?PANEL:MUTED,card.x+card.width/2,card.y+card.height-7);
        }
        drawFooter(g,"CHOOSE WORLD  >",HERO_NAMES[selectedCharacter]+" / "+SKILLS[selectedCharacter]);
    }
    private void drawMapPage(Graphics2D g) {
        drawHeader(g,"CHOOSE YOUR WORLD");
        for(int i=0;i<MAPS.length;i++) {
            Rectangle card=mapCard(i); boolean selected=selectedMap==i;
            drawCard(g,card,selected,hoveredIndex==i);
            cover(g,mapPreviews[i],card.x+8,card.y+8,card.width-16,82);
            g.setColor(new Color(6,13,16,145)); g.fillRect(card.x+8,card.y+8,card.width-16,82);
            text(g,MAPS[i].tagline,11,WHITE,card.x+19,card.y+32); text(g,"0"+(i+1),24,GOLD,card.x+19,card.y+72);
            if(selected) {
                g.setColor(GOLD); g.fillRect(card.x+card.width-98,card.y+16,80,19);
                centered(g,"SELECTED",10,PANEL,card.x+card.width-58,card.y+30);
            }
            text(g,MAPS[i].name.toUpperCase(),18,WHITE,card.x+16,card.y+116);
        }
        drawFooter(g,"CHOOSE DIFFICULTY >",HERO_NAMES[selectedCharacter]+" / "+MAPS[selectedMap].name.toUpperCase());
    }
    private void drawDifficultyPage(Graphics2D g) {
        drawHeader(g,"CHOOSE DIFFICULTY");
        Difficulty[] options=Difficulty.values();
        for(int i=0;i<options.length;i++) {
            Rectangle card=playerCard(i); boolean selected=selectedDifficulty==i;
            Difficulty option=options[i]; drawCard(g,card,selected,hoveredIndex==i);
            centered(g,option.label,24,selected?GOLD:WHITE,card.x+card.width/2,card.y+61);
            for(int mark=0;mark<=i;mark++) {
                int cx=card.x+card.width/2+(int)((mark-i/2.0)*32);
                g.setColor(i==0?GOLD:RED);
                g.fillPolygon(new int[]{cx,cx+10,cx,cx-10},new int[]{card.y+96,card.y+110,card.y+124,card.y+110},4);
            }
            centered(g,option.description,12,WHITE,card.x+card.width/2,card.y+173);
            centered(g,option.detail,12,MUTED,card.x+card.width/2,card.y+202);
            if(selected) { g.setColor(GOLD); g.fillRect(card.x+1,card.y+card.height-21,card.width-2,20); }
            centered(g,selected?"SELECTED":"CLICK TO SELECT",10,selected?PANEL:MUTED,card.x+card.width/2,card.y+card.height-7);
        }
        drawFooter(g,"START ADVENTURE >",HERO_NAMES[selectedCharacter]+" / "+MAPS[selectedMap].name.toUpperCase()+" / "+options[selectedDifficulty].label);
    }

    private void drawFooter(Graphics2D g,String primary,String summary) {
        drawButton(g,backButton(),"< BACK",false,backButtonHovered); drawButton(g,primaryButton(),primary,true,primaryButtonHovered);
        text(g,summary,11,GOLD,48,506); centered(g,"ARROWS TO SELECT / ENTER TO CONTINUE",9,MUTED,374,549);
    }
    private void hero(Graphics2D g,int index,int centerX,int top,int height) {
        BufferedImage frame=characterPreviews[index].frameAt(System.currentTimeMillis(),160);
        int width=height*frame.getWidth()/frame.getHeight(); g.drawImage(frame,centerX-width/2,top-22,width,height,null);
    }
    private void drawCard(Graphics2D g,Rectangle r,boolean selected,boolean hover) {
        g.setColor(new Color(0,0,0,110)); g.fillRect(r.x+4,r.y+5,r.width,r.height);
        g.setColor(selected?new Color(34,34,28,245):PANEL); g.fillRect(r.x,r.y,r.width,r.height);
        g.setColor(selected?GOLD:hover?WHITE:new Color(68,78,75)); g.drawRect(r.x,r.y,r.width,r.height);
        if(selected) g.drawRect(r.x+2,r.y+2,r.width-4,r.height-4);
        g.setColor(selected?GOLD:new Color(91,101,94)); g.fillRect(r.x,r.y,20,3);
    }
    private void drawButton(Graphics2D g,Rectangle r,String label,boolean primary,boolean hover) {
        g.setColor(new Color(0,0,0,150)); g.fillRect(r.x,r.y+4,r.width,r.height);
        g.setColor(primary?(hover?new Color(255,219,150):GOLD):(hover?new Color(48,56,52):PANEL)); g.fillRect(r.x,r.y,r.width,r.height);
        g.setColor(primary?new Color(255,231,172):new Color(92,107,94)); g.drawRect(r.x+2,r.y+2,r.width-4,r.height-4);
        centered(g,label,primary&&page==Page.START?20:13,primary?PANEL:WHITE,r.x+r.width/2,r.y+r.height/2+5);
    }
    private Rectangle playerCard(int index) {
        int width=220,gap=24,startX=(800-(width*CHARACTERS.length+gap*(CHARACTERS.length-1)))/2;
        return new Rectangle(startX+index*(width+gap),185,width,280);
    }
    private Rectangle mapCard(int index) { return new Rectangle(64+index%2*346,170+index/2*165,326,155); }
    private Rectangle primaryButton() { return page==Page.START?new Rectangle(295,390,210,62):new Rectangle(570,520,182,48); }
    private Rectangle backButton() { return new Rectangle(48,520,130,48); }
    private void handleClick(int x,int y) {
        if(launching) return; requestFocusInWindow();
        if(primaryButton().contains(x,y)) advance();
        else if(page!=Page.START&&backButton().contains(x,y)) back();
        else if(page==Page.PLAYER) { for(int i=0;i<CHARACTERS.length;i++) if(playerCard(i).contains(x,y)) select(i); }
        else if(page==Page.MAP) { for(int i=0;i<MAPS.length;i++) if(mapCard(i).contains(x,y)) select(i); }
        else if(page==Page.DIFFICULTY) { for(int i=0;i<Difficulty.values().length;i++) if(playerCard(i).contains(x,y)) select(i); }
        updateHover(x,y);
    }
    private void advance() {
        if(launching) return; SoundEffects.play(SoundEffects.Effect.MENU_CLICK);
        if(page==Page.START) page=Page.PLAYER; else if(page==Page.PLAYER) page=Page.MAP;
        else if(page==Page.MAP) page=Page.DIFFICULTY;
        else { launching=true; animationTimer.stop(); onGameStart.start(CHARACTERS[selectedCharacter],MAPS[selectedMap].resource,Difficulty.values()[selectedDifficulty]); }
        resetHover(); repaint();
    }
    private void back() {
        if(launching||page==Page.START) return; page=page==Page.DIFFICULTY?Page.MAP:page==Page.MAP?Page.PLAYER:Page.START;
        SoundEffects.play(SoundEffects.Effect.MENU_CLICK); resetHover(); repaint();
    }
    private void selectBy(int delta) {
        if(page==Page.START||launching) return;
        int count=page==Page.PLAYER?CHARACTERS.length:page==Page.MAP?MAPS.length:Difficulty.values().length,
            current=page==Page.PLAYER?selectedCharacter:page==Page.MAP?selectedMap:selectedDifficulty;
        select(Math.floorMod(current+delta,count));
    }
    private void select(int index) {
        if(page==Page.PLAYER) selectedCharacter=index; else if(page==Page.MAP) selectedMap=index; else selectedDifficulty=index;
        SoundEffects.play(SoundEffects.Effect.SELECT); repaint();
    }
    private void resetHover() {
        hoveredIndex=-1; primaryButtonHovered=false; backButtonHovered=false; setCursor(Cursor.getDefaultCursor());
    }
    private void updateHover(int x,int y) {
        hoveredIndex=-1;
        if(page==Page.PLAYER) { for(int i=0;i<CHARACTERS.length;i++) if(playerCard(i).contains(x,y)) hoveredIndex=i; }
        else if(page==Page.MAP) { for(int i=0;i<MAPS.length;i++) if(mapCard(i).contains(x,y)) hoveredIndex=i; }
        else if(page==Page.DIFFICULTY) { for(int i=0;i<Difficulty.values().length;i++) if(playerCard(i).contains(x,y)) hoveredIndex=i; }
        primaryButtonHovered=primaryButton().contains(x,y); backButtonHovered=page!=Page.START&&backButton().contains(x,y);
        setCursor(Cursor.getPredefinedCursor(hoveredIndex>=0||primaryButtonHovered||backButtonHovered?Cursor.HAND_CURSOR:Cursor.DEFAULT_CURSOR)); repaint();
    }
    private int toDesignX(int x) { return x*800/Math.max(1,getWidth()); }
    private int toDesignY(int y) { return y*600/Math.max(1,getHeight()); }
    private static void text(Graphics2D g,String text,int size,Color color,int x,int y) {
        g.setFont(PIXEL_FONT.deriveFont((float)size)); g.setColor(color); g.drawString(text,x,y);
    }
    private static void centered(Graphics2D g,String text,int size,Color color,int x,int y) {
        g.setFont(PIXEL_FONT.deriveFont((float)size)); text(g,text,size,color,x-g.getFontMetrics().stringWidth(text)/2,y);
    }
    private static void rightText(Graphics2D g,String text,int size,Color color,int x,int y) {
        g.setFont(PIXEL_FONT.deriveFont((float)size)); text(g,text,size,color,x-g.getFontMetrics().stringWidth(text),y);
    }
    private static void title(Graphics2D g,String text,int size,int x,int y,Color color) {
        centered(g,text,size,new Color(0,0,0,170),x+4,y+5); centered(g,text,size,RED,x+2,y+2); centered(g,text,size,color,x,y);
    }
    private static void cover(Graphics2D g,BufferedImage image,int x,int y,int width,int height) {
        double scale=Math.max(width/(double)image.getWidth(),height/(double)image.getHeight());
        int sw=Math.min(image.getWidth(),(int)Math.round(width/scale)),sh=Math.min(image.getHeight(),(int)Math.round(height/scale));
        int sx=(image.getWidth()-sw)/2,sy=(image.getHeight()-sh)/2;
        g.drawImage(image,x,y,x+width,y+height,sx,sy,sx+sw,sy+sh,null);
    }
    private static Font loadFont() {
        try(var stream=GameStartScreen.class.getResourceAsStream("/project/game/fonts/boldpixels.ttf")) {
            if(stream!=null) return Font.createFont(Font.TRUETYPE_FONT,stream);
        } catch(IOException|FontFormatException e) { System.err.println("Unable to load BoldPixels: "+e.getMessage()); }
        return new Font("Monospaced",Font.BOLD,16);
    }
    private static BufferedImage loadImage(String path) {
        var resource=GameStartScreen.class.getResource(path); if(resource==null) throw new IllegalStateException("Missing menu image: "+path);
        try { BufferedImage image=ImageIO.read(resource); if(image==null) throw new IllegalStateException("Invalid menu image: "+path); return image; }
        catch(IOException e) { throw new IllegalStateException("Unable to load menu image: "+path,e); }
    }
    private static final class MapOption {
        private final String name,resource,tagline;
        private MapOption(String name,String resource,String tagline) {
            this.name=name; this.resource=resource; this.tagline=tagline;
        }
    }
}
