import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Transparent, original typography overlays for the gameplay edit. */
public final class TrailerTitles {
    private static final Color GOLD=new Color(237,194,104),TEXT=new Color(237,243,242),TEAL=new Color(94,211,185);
    private static Font font;
    private static void centered(Graphics2D g,String text,int y,float size,Color color) {
        g.setFont(font.deriveFont(size));g.setColor(color);g.drawString(text,(1920-g.getFontMetrics().stringWidth(text))/2,y);
    }
    public static void main(String[] args) throws Exception {
        Path root=Path.of(args.length>0?args[0]:"build/marketing");Files.createDirectories(root.resolve("overlays"));
        font=Font.createFont(Font.TRUETYPE_FONT,Path.of("resources/outpost/interface.ttf").toFile());
        var lines=Files.readAllLines(root.resolve("shots.tsv"));
        for(String line:lines.subList(1,lines.size())) {
            String[] s=line.split("\t");BufferedImage image=new BufferedImage(1920,1080,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            boolean title=s[0].startsWith("01") || s[0].startsWith("16");
            if(title) {
                g.setPaint(new GradientPaint(0,0,new Color(7,16,22,150),0,1080,new Color(7,16,22,220)));g.fillRect(0,0,1920,1080);
                centered(g,"T H E   L A S T   E M B E R",330,32,TEAL);centered(g,"OUTPOST",555,196,GOLD);
                g.setColor(GOLD);g.fillRect(760,606,400,2);centered(g,s[3],680,38,TEXT);
                if(s[0].startsWith("16")) {
                    centered(g,"github.com/Chris020606/JavaGame",770,35,TEAL);
                    centered(g,"Latest build and all-gear playtest • See description",825,27,TEXT);
                } else centered(g,"An extended gameplay trailer • Captured in the development build",840,25,TEXT);
            } else {
                g.setPaint(new GradientPaint(0,805,new Color(7,16,22,0),0,1080,new Color(7,16,22,240)));g.fillRect(0,805,1920,275);
                g.setColor(TEAL);g.fillRect(94,907,5,102);g.setFont(font.deriveFont(50f));g.setColor(GOLD);g.drawString(s[2],125,951);
                g.setFont(font.deriveFont(29f));g.setColor(TEXT);g.drawString(s[3],125,1001);
                g.setFont(font.deriveFont(20f));g.setColor(new Color(177,198,200));g.drawString("OUTPOST  /  ACTUAL GAMEPLAY  /  DEVELOPMENT BUILD",125,1049);
            }
            g.dispose();ImageIO.write(image,"png",root.resolve("overlays/"+s[0]+".png").toFile());
        }
        System.out.println("Created "+(lines.size()-1)+" trailer overlays");
    }
}
