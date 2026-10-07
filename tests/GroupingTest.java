import com.example.blackjackoverlay.*;
import java.util.*;
public class GroupingTest {
 static int checks;static void check(boolean v,String m){checks++;if(!v)throw new AssertionError(m);}
 static float[] box(float l,float t,float r,float b){return new float[]{l,t,r,t,r,b,l,b};}
 static float[][] boxes(){float[][] q=new float[8][];q[0]=box(0,0,100,50);for(int i=1;i<8;i++)q[i]=box(1000+i*100,1000,1090+i*100,1090);q[1]=box(30,60,180,190);q[2]=box(115,60,270,190);return q;}
 static void rect(int[] pixels,int w,int l,int t,int r,int b){for(int y=t;y<b;y++)for(int x=l;x<r;x++)pixels[y*w+x]=0xffffffff;}
 static List<List<String>> empty(){List<List<String>> v=new ArrayList<>();for(int i=0;i<8;i++)v.add(new ArrayList<>());return v;}
 public static void main(String[] args){
  int w=540,h=240;int[] p=new int[w*h];rect(p,w,85,110,126,146);rect(p,w,113,96,154,132);rect(p,w,190,105,220,150);
  HandGrouping g=new HandGrouping(p,w,h,boxes());check(g.groups.size()==2,"touching cards make one cluster; nearby separate hand stays separate");
  List<HandGrouping.Read> reads=Arrays.asList(new HandGrouping.Read("8",120,115,1),new HandGrouping.Read("8",120.5f,115,2),new HandGrouping.Read("8",93,136,1),new HandGrouping.Read("5",200,120,2));
  List<List<String>> r=g.assign(reads);check(r.get(1).equals(Arrays.asList("8","8")),"overlapping boxes deduplicated; distinct identical cards kept");check(r.get(2).equals(Arrays.asList("5")),"neighbor stays in own hand");check(g.duplicates==1,"duplicate diagnostic");
  HandGrouping conflict=new HandGrouping(p,w,h,boxes());r=conflict.assign(Arrays.asList(new HandGrouping.Read("8",120,115,1),new HandGrouping.Read("3",120,115,2)));check(r.get(1).isEmpty()&&conflict.ambiguous==1,"conflicting rank at same corner withheld");
  HandGrouping.Ownership locks=new HandGrouping.Ownership();locks.apply(g);float[][] shifted=boxes();shifted[1]=box(0,60,160,190);shifted[2]=box(100,60,200,190);HandGrouping moved=new HandGrouping(p,w,h,shifted);locks.apply(moved);r=moved.assign(reads);check(r.get(1).size()==2,"cluster ownership retained despite changed closest box");
  TableState state=new TableState();List<List<String>> ranks=empty();ranks.get(0).add("K");ranks.get(1).addAll(Arrays.asList("8","8"));
  for(int i=0;i<3;i++)state.scene("SEAT CALIBRATE",ranks,1,i*1000);check(state.inRound&&state.round==1,"two-card hand plus dealer starts round with hidden status");
  for(int i=0;i<3;i++)state.observe(1,ranks.get(1));check(state.observed==2,"initial pair counted");
  for(int i=0;i<5;i++){state.scene("Please take a seat",ranks,1,4000+i*1000);state.observe(1,ranks.get(1));}check(state.round==1&&state.observed==2,"observer idle label cannot reset visible hand");
  for(int i=0;i<6;i++)state.scene("",empty(),1,10000+i*1000);check(state.inRound,"OCR dropout with card faces cannot clear hand");
  state.scene("",empty(),0,18000);state.scene("",empty(),0,19000);state.scene("",ranks,1,20000);check(state.inRound,"brief obstruction cannot reset");
  for(int i=0;i<4;i++)state.scene("",empty(),0,21000+i*1000);check(!state.inRound&&state.observed==2,"confirmed empty table ends hand but retains shoe count");
  for(int i=0;i<3;i++)state.scene("",ranks,1,26000+i*1000);for(int i=0;i<3;i++)state.observe(1,ranks.get(1));check(state.round==2&&state.observed==4,"same pair next round counted once again");
  System.out.println("PASS: "+checks+" grouping/round checks");
 }
}
