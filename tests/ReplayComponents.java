import com.example.blackjackoverlay.*;
import javax.imageio.ImageIO;import java.awt.image.BufferedImage;import java.awt.*;import java.io.*;
public class ReplayComponents{
 static int W=720,H=1560;
 static int[] pixels(String p)throws Exception{BufferedImage b=ImageIO.read(new File(p)),r=new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);Graphics2D g=r.createGraphics();g.drawImage(b,0,0,W,H,0,0,b.getWidth(),b.getHeight()-140,null);g.dispose();return r.getRGB(0,0,W,H,null,0,W);}
 public static void main(String[] args)throws Exception{
 float[] roi={.0032199f*W,.24678445f*H,.97121996f*W,.24678445f*H,.97121996f*W,.3729233f*H,.0032199f*W,.3729233f*H};
 float[][] anchors={{.5241048f,.2687695f},{.20645034f,.31939247f},{.3220933f,.33256534f},{.42114142f,.3334239f},{.5322827f,.33976948f},{.6364189f,.32873285f},{.7352461f,.32552865f},{.83931947f,.30576953f}};
 for(float[] a:anchors){a[0]*=W;a[1]*=H;}
 int[] ref=args[0].equals("none")?null:pixels(args[0]);
 for(int i=1;i<args.length;i++){int[] p=pixels(args[i]);TableComponents c=new TableComponents(p,W,H,roi,anchors,ref);System.out.println(args[i]+" groups="+c.groups.size()+" ambiguous="+c.ambiguous+" unresolved="+c.unresolvedFaces+" small="+c.rejectedSmall+" "+c.groups);
 BufferedImage b=new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);b.setRGB(0,0,W,H,p,0,W);Graphics2D g=b.createGraphics();g.setColor(Color.GREEN);for(TableComponents.Group v:c.groups){g.drawRect(v.left,v.top,v.right-v.left,v.bottom-v.top);g.drawString(""+v.owner,v.left,v.top-3);}g.dispose();ImageIO.write(b,"png",new File(args[i]+".boxes.png"));}
 }
}
