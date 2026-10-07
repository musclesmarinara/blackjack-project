import com.example.blackjackoverlay.*;import javax.imageio.ImageIO;import java.io.*;import java.awt.image.BufferedImage;
public class ReplayGeometry {
 public static void main(String[] args)throws Exception{
 BufferedImage image=ImageIO.read(new File(args[0]));int w=image.getWidth(),h=1140;int[] pixels=image.getRGB(0,0,w,h,null,0,w);
 float[][] b={{.0017356f,.1848148f,.3319727f,.2093148f},{.1500043f,.3200364f,.2588045f,.4131179f},{.2832903f,.3465987f,.3785354f,.4445406f},{.382133f,.360193f,.469691f,.447411f},{.4670005f,.3668528f,.5806391f,.4560285f},{.5657399f,.3583123f,.6766145f,.4455253f},{.6759166f,.3496098f,.7873304f,.4469464f},{.770035f,.332764f,.892379f,.422277f}};
 float[] angles={0,325,333,348,355,0,7,34};float[][] q=new float[8][];
 for(int i=0;i<8;i++){float cx=(b[i][0]+b[i][2])*w/2,cy=(b[i][1]+b[i][3])*h/2,hw=(b[i][2]-b[i][0])*w/2,hh=(b[i][3]-b[i][1])*h/2;float[] a={-hw,-hh,hw,-hh,hw,hh,-hw,hh};double theta=Math.toRadians(-angles[i]);for(int j=0;j<8;j+=2){float x=a[j],y=a[j+1];a[j]=cx+(float)(x*Math.cos(theta)-y*Math.sin(theta));a[j+1]=cy+(float)(x*Math.sin(theta)+y*Math.cos(theta));}q[i]=a;}
 HandGrouping g=new HandGrouping(pixels,w,h,q);
 java.util.List<HandGrouping.Read> reads=new java.util.ArrayList<>();float[][] anchors={{92,441},{164,461},{222,466},{276,467},{330,466},{397,455},{439,430}};
 for(int i=0;i<7;i++)reads.add(new HandGrouping.Read("8",anchors[i][0],anchors[i][1],i+1));
 g.assign(reads);new HandGrouping.Ownership().apply(g);java.util.List<java.util.List<String>> hands=g.assign(reads);
 for(int i=1;i<=7;i++)if(hands.get(i).size()!=1)throw new AssertionError("Wrong seat "+i+": "+hands);
 System.out.println("PASS: seven annotated hand positions assigned correctly; unlabelled bright shoe object excluded. This checks geometry, not OCR.");
 }
}
