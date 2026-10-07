package com.example.blackjackoverlay;
import java.util.*;

/** Screen-space grouping. Boxes are search windows, never card identities. */
public final class HandGrouping {
 public static final class Read {
  public final String rank; public final float x,y; public final int window;
  public Read(String r,float x,float y,int w){rank=r;this.x=x;this.y=y;window=w;}
 }
 public static final class Group {
  int id,area,left,top,right,bottom; public int owner; boolean hasRank;
  public String toString(){return "seat="+owner+" bounds="+left+","+top+","+right+","+bottom+" area="+area;}
  float cx(){return (left+right)/2f;} float cy(){return (top+bottom)/2f;}
 }
 public final List<Group> groups=new ArrayList<>();
 private final int width,height; private final int[] labels; private final float[][] boxes;
 public int ambiguous=0,duplicates=0;
 public final List<Read> assigned=new ArrayList<>();
 public HandGrouping(int[] pixels,int w,int h,float[][] quads){
  width=w;height=h;boxes=quads;labels=new int[w*h];boolean[] mask=new boolean[w*h];
  for(int y=0;y<h;y++)for(int x=0;x<w;x++){
   boolean inside=false;for(int i=1;i<=7;i++)if(contains(quads[i],x,y)){inside=true;break;}
   if(!inside)continue;int c=pixels[y*w+x],r=(c>>16)&255,g=(c>>8)&255,b=c&255;
   mask[y*w+x]=Math.min(r,Math.min(g,b))>155&&Math.max(r,Math.max(g,b))-Math.min(r,Math.min(g,b))<65;
  }
  // Remove thin table lettering that can otherwise bridge two nearby hands.
  int opening=Math.max(1,Math.round(w/540f));boolean[] core=new boolean[w*h],opened=new boolean[w*h];
  for(int y=opening;y<h-opening;y++)for(int x=opening;x<w-opening;x++){
   boolean all=true;for(int dy=-opening;dy<=opening&&all;dy++)for(int dx=-opening;dx<=opening;dx++)if(!mask[(y+dy)*w+x+dx]){all=false;break;}core[y*w+x]=all;
  }
  for(int y=opening;y<h-opening;y++)for(int x=opening;x<w-opening;x++){
   boolean any=false;for(int dy=-opening;dy<=opening&&!any;dy++)for(int dx=-opening;dx<=opening;dx++)if(core[(y+dy)*w+x+dx]){any=true;break;}opened[y*w+x]=any;
  }mask=opened;
  // Close small printed-rank/suit gaps without joining hands across table felt.
  int radius=Math.max(1,Math.round(w/270f));boolean[] dilated=new boolean[w*h],closed=new boolean[w*h];
  for(int y=radius;y<h-radius;y++)for(int x=radius;x<w-radius;x++){
   boolean any=false;for(int dy=-radius;dy<=radius&&!any;dy++)for(int dx=-radius;dx<=radius;dx++)if(mask[(y+dy)*w+x+dx]){any=true;break;}dilated[y*w+x]=any;
  }
  for(int y=radius;y<h-radius;y++)for(int x=radius;x<w-radius;x++){
   boolean all=true;for(int dy=-radius;dy<=radius&&all;dy++)for(int dx=-radius;dx<=radius;dx++)if(!dilated[(y+dy)*w+x+dx]){all=false;break;}closed[y*w+x]=all;
  }
  int[] queue=new int[w*h];int id=0;
  for(int p=0;p<closed.length;p++)if(closed[p]&&labels[p]==0){
   Group group=new Group();group.id=++id;group.left=w;group.top=h;int head=0,tail=0;queue[tail++]=p;labels[p]=id;
   while(head<tail){int n=queue[head++],x=n%w,y=n/w;group.area++;group.left=Math.min(group.left,x);group.right=Math.max(group.right,x);group.top=Math.min(group.top,y);group.bottom=Math.max(group.bottom,y);
    for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){int xx=x+dx,yy=y+dy;if(xx<0||xx>=w||yy<0||yy>=h)continue;int k=yy*w+xx;if(closed[k]&&labels[k]==0){labels[k]=id;queue[tail++]=k;}}
   }
   int bw=group.right-group.left+1,bh=group.bottom-group.top+1;
   if(group.area<w*w*.00065f||bw<w*.025f||bh<w*.018f||group.area<(bw*bh)*.32f)continue;
   if(bw>w*.24f||bh>w*.23f){ambiguous++;continue;}
   float best=Float.MAX_VALUE,second=best;int owner=0;
   for(int i=1;i<=7;i++){
    float[] q=quads[i];float cx=(q[0]+q[2]+q[4]+q[6])/4,cy=(q[1]+q[3]+q[5]+q[7])/4;
    if(!contains(q,group.cx(),group.cy()))continue;
    float d=(float)Math.hypot(cx-group.cx(),cy-group.cy());if(d<best){second=best;best=d;owner=i;}else second=Math.min(second,d);
   }
   if(owner==0||second-best<w*.008f){ambiguous++;continue;}group.owner=owner;groups.add(group);
  }
 }
 /** Locks a visible cluster to its seat until the confirmed table-clear boundary. */
 public static final class Ownership {
  private final List<Group> history=new ArrayList<>();
  public void clear(){history.clear();}
  public void apply(HandGrouping frame){
   for(Iterator<Group> it=frame.groups.iterator();it.hasNext();){Group g=it.next();Group match=null;Set<Integer> owners=new HashSet<>();
    for(Group old:history){
     int iw=Math.max(0,Math.min(g.right,old.right)-Math.max(g.left,old.left)+1),ih=Math.max(0,Math.min(g.bottom,old.bottom)-Math.max(g.top,old.top)+1);
     float fraction=(iw*ih)/(float)Math.min((g.right-g.left+1)*(g.bottom-g.top+1),(old.right-old.left+1)*(old.bottom-old.top+1));
     if(fraction>.45f){owners.add(old.owner);match=old;}
    }
    if(owners.size()>1){frame.ambiguous++;it.remove();continue;}
    if(match==null&&!g.hasRank){it.remove();continue;}
    if(match!=null){g.owner=match.owner;history.remove(match);}history.add(g);
   }
  }
 }
 public static boolean contains(float[] q,float x,float y){
  boolean pos=false,neg=false;for(int i=0;i<8;i+=2){int j=(i+2)%8;float c=(q[j]-q[i])*(y-q[i+1])-(q[j+1]-q[i+1])*(x-q[i]);pos|=c>0;neg|=c<0;}return !(pos&&neg);
 }
 private Group at(float x,float y){
  Group best=null;double distance=Double.MAX_VALUE;int radius=Math.max(3,Math.round(width*.015f));
  for(int yy=Math.max(0,(int)y-radius);yy<Math.min(height,(int)y+radius+1);yy++)for(int xx=Math.max(0,(int)x-radius);xx<Math.min(width,(int)x+radius+1);xx++){
   int id=labels[yy*width+xx];if(id==0)continue;double d=Math.hypot(xx-x,yy-y);if(d>=distance)continue;
   for(Group g:groups)if(g.id==id){best=g;distance=d;break;}
  }return best;
 }
 public List<List<String>> assign(List<Read> reads){
  assigned.clear();
  List<List<String>> result=new ArrayList<>();for(int i=0;i<8;i++)result.add(new ArrayList<>());
  boolean[] used=new boolean[reads.size()];
  for(int i=0;i<reads.size();i++){
   if(used[i])continue;Read r=reads.get(i);used[i]=true;boolean conflict=false;
   for(int j=i+1;j<reads.size();j++){Read s=reads.get(j);if((r.window==0)!=(s.window==0))continue;
    if(Math.hypot(r.x-s.x,r.y-s.y)<width*.012f){used[j]=true;duplicates++;if(!r.rank.equals(s.rank))conflict=true;}
   }
   if(conflict){ambiguous++;continue;}
   if(r.window==0){result.get(0).add(r.rank);assigned.add(r);continue;}
   Group g=at(r.x,r.y);if(g!=null){g.hasRank=true;result.get(g.owner).add(r.rank);assigned.add(new Read(r.rank,r.x,r.y,g.owner));}
  }return result;
 }
}
