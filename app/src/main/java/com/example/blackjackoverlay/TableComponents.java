package com.example.blackjackoverlay;
import java.util.*;

/** Connected light-face candidates inside one table ROI. Heuristic, not a trained card detector. */
public final class TableComponents {
 public static final class Read {
  public final String rank; public final float x,y; public final int window;
  public Read(String r,float x,float y,int w){rank=r;this.x=x;this.y=y;window=w;}
 }
 public static final class Group {
  public int id,area,left,top,right,bottom; public int owner; boolean hasRank;
  public String toString(){return "seat="+owner+" bounds="+left+","+top+","+right+","+bottom+" area="+area;}
  float cx(){return (left+right)/2f;} float cy(){return (top+bottom)/2f;}
 }
 public final List<Group> groups=new ArrayList<>();
 private final int width,height; private final int[] labels; 
 public int ambiguous=0,duplicates=0,rejectedSmall=0;public boolean unresolvedFaces=false;
 public final List<Read> assigned=new ArrayList<>();
 public TableComponents(int[] pixels,int w,int h,float[] table,float[][] anchors){
  this(pixels,w,h,table,anchors,null);
 }
 public TableComponents(int[] pixels,int w,int h,float[] table,float[][] anchors,int[] background){
  if(background!=null&&background.length!=pixels.length)throw new IllegalArgumentException("Reference size differs");
  width=w;height=h;
  int x0=w,y0=h,x1=0,y1=0;for(int i=0;i<8;i+=2){x0=Math.min(x0,(int)table[i]);y0=Math.min(y0,(int)table[i+1]);x1=Math.max(x1,(int)Math.ceil(table[i]));y1=Math.max(y1,(int)Math.ceil(table[i+1]));}
  x0=Math.max(0,x0);y0=Math.max(0,y0);x1=Math.min(w,x1);y1=Math.min(h,y1);
  labels=new int[w*h];boolean[] mask=new boolean[w*h];
  for(int y=y0;y<y1;y++)for(int x=x0;x<x1;x++){
   boolean inside=contains(table,x,y);
   if(!inside)continue;int c=pixels[y*w+x],r=(c>>16)&255,g=(c>>8)&255,b=c&255;
   mask[y*w+x]=Math.min(r,Math.min(g,b))>(background==null?155:165)&&Math.max(r,Math.max(g,b))-Math.min(r,Math.min(g,b))<65;
  }
  // Remove thin table lettering that can otherwise bridge two nearby hands.
  int opening=Math.max(1,Math.round(w/540f));boolean[] core=new boolean[w*h],opened=new boolean[w*h];
  for(int y=Math.max(opening,y0-4);y<Math.min(h-opening,y1+4);y++)for(int x=Math.max(opening,x0-4);x<Math.min(w-opening,x1+4);x++){
   boolean all=true;for(int dy=-opening;dy<=opening&&all;dy++)for(int dx=-opening;dx<=opening;dx++)if(!mask[(y+dy)*w+x+dx]){all=false;break;}core[y*w+x]=all;
  }
  for(int y=Math.max(opening,y0-4);y<Math.min(h-opening,y1+4);y++)for(int x=Math.max(opening,x0-4);x<Math.min(w-opening,x1+4);x++){
   boolean any=false;for(int dy=-opening;dy<=opening&&!any;dy++)for(int dx=-opening;dx<=opening;dx++)if(core[(y+dy)*w+x+dx]){any=true;break;}opened[y*w+x]=any;
  }mask=opened;
  // Close small printed-rank/suit gaps without joining hands across table felt.
  int radius=Math.max(1,Math.round(w/270f));boolean[] dilated=new boolean[w*h],closed=new boolean[w*h];
  for(int y=Math.max(radius,y0-4);y<Math.min(h-radius,y1+4);y++)for(int x=Math.max(radius,x0-4);x<Math.min(w-radius,x1+4);x++){
   boolean any=false;for(int dy=-radius;dy<=radius&&!any;dy++)for(int dx=-radius;dx<=radius;dx++)if(mask[(y+dy)*w+x+dx]){any=true;break;}dilated[y*w+x]=any;
  }
  for(int y=Math.max(radius,y0-4);y<Math.min(h-radius,y1+4);y++)for(int x=Math.max(radius,x0-4);x<Math.min(w-radius,x1+4);x++){
   boolean all=true;for(int dy=-radius;dy<=radius&&all;dy++)for(int dx=-radius;dx<=radius;dx++)if(!dilated[(y+dy)*w+x+dx]){all=false;break;}closed[y*w+x]=all;
  }
  int[] queue=new int[w*h];int id=0;
  for(int p=0;p<closed.length;p++)if(closed[p]&&labels[p]==0){
   Group group=new Group();group.id=++id;group.left=w;group.top=h;int head=0,tail=0;queue[tail++]=p;labels[p]=id;
   while(head<tail){int n=queue[head++],x=n%w,y=n/w;group.area++;group.left=Math.min(group.left,x);group.right=Math.max(group.right,x);group.top=Math.min(group.top,y);group.bottom=Math.max(group.bottom,y);
    for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){int xx=x+dx,yy=y+dy;if(xx<0||xx>=w||yy<0||yy>=h)continue;int k=yy*w+xx;if(closed[k]&&labels[k]==0){labels[k]=id;queue[tail++]=k;}}
   }
   int bw=group.right-group.left+1,bh=group.bottom-group.top+1;
   // Compare complete faces instead of cutting white pixels out of new cards.
   if(background!=null){int changedPixels=0;
    for(int n=0;n<tail;n++){int k=queue[n],c=pixels[k],ref=background[k];
     int delta=Math.max(Math.abs(((c>>16)&255)-((ref>>16)&255)),Math.max(Math.abs(((c>>8)&255)-((ref>>8)&255)),Math.abs((c&255)-(ref&255))));if(delta>=35)changedPixels++;
    }if(changedPixels<group.area*.18f)continue;
   }
   if(group.area<w*w*(background==null?.00065f:.00020f)||bw<w*(background==null?.025f:.015f)||bh<w*(background==null?.018f:.008f)||group.area<(bw*bh)*.32f){rejectedSmall++;if(group.area>w*w*.00015f&&bw>w*.012f&&bh>w*.012f)unresolvedFaces=true;continue;}
   if(bw>w*.24f||bh>w*.23f){ambiguous++;continue;}
   int enclosed=0;for(float[] a:anchors)if(a[0]>=group.left&&a[0]<=group.right&&a[1]>=group.top&&a[1]<=group.bottom)enclosed++;
   if(enclosed>1){ambiguous++;continue;}
   float best=Float.MAX_VALUE,second=best;int owner=-1;
   for(int i=0;i<8;i++){
    float d=(float)Math.hypot(anchors[i][0]-group.cx(),anchors[i][1]-group.cy());
    if(d<best){second=best;best=d;owner=i;}else second=Math.min(second,d);
   }
   if(background!=null&&best>w*.10f)continue;
   if(owner<0||best>w*.16f||second-best<w*.012f){ambiguous++;continue;}
   if(owner==0&&Math.abs(group.cy()-anchors[0][1])>w*.045f)continue;
   group.owner=owner;groups.add(group);
  }
 }
 public boolean belongs(Group g,float x,float y){
  int radius=Math.max(2,Math.round(width*.006f));
  for(int yy=Math.max(0,(int)y-radius);yy<Math.min(height,(int)y+radius+1);yy++)for(int xx=Math.max(0,(int)x-radius);xx<Math.min(width,(int)x+radius+1);xx++)if(labels[yy*width+xx]==g.id)return true;
  return false;
 }
 public static boolean contains(float[] q,float x,float y){
  return HandGrouping.contains(q,x,y);
 }
}
