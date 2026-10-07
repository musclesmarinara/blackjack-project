import com.example.blackjackoverlay.*;import java.util.*;
public class IdentityTest {
 static int n;static void check(boolean b,String m){n++;if(!b)throw new AssertionError(m);}
 static HandGrouping.Read r(String rank,float x,int seat){return new HandGrouping.Read(rank,x,120,seat);}
 public static void main(String[] args){
  CardIdentity tracker=new CardIdentity();List<List<String>> out=null;
  for(int i=0;i<3;i++)out=tracker.observe(Arrays.asList(r("8",100,1),r("8",120,1)),540);
  check(out.get(1).size()==2,"identical physical cards distinct");
  for(int i=0;i<5;i++)out=tracker.observe(Arrays.asList(r("8",101,1),r("8",121,1)),540);
  check(out.get(1).size()==2,"jitter and repeats not extra cards");
  for(int i=0;i<3;i++)out=tracker.observe(Arrays.asList(r("8",121,1),r("8",145,1)),540);
  check(out.get(1).size()==3,"new identical card counts while old card occluded");
  out=tracker.observe(Arrays.asList(r("3",101,1)),540);check(tracker.conflict&&out.get(1).size()==3,"changed OCR rank cannot create extra card");
  for(int i=0;i<4;i++)out=tracker.observe(Arrays.asList(r("8",101,2)),540);
  check(out.get(1).size()==3&&out.get(2).isEmpty(),"same physical corner reassigned by overlapping box not recounted");
  tracker.clear();for(int i=0;i<3;i++)out=tracker.observe(Arrays.asList(r("8",100,2)),540);check(out.get(2).size()==1,"new round resets identities");
  tracker.clear();tracker.observe(Arrays.asList(r("K",100,1)),540);tracker.observe(Collections.emptyList(),540);out=tracker.observe(Arrays.asList(r("K",100,1)),540);check(out.get(1).isEmpty(),"intermittent one-frame read not confirmed");
  System.out.println("PASS: "+n+" identity checks");
 }
}
