import com.example.blackjackoverlay.*;import java.util.*;
public class TableTrackerTest {
 static int n;static void check(boolean b,String message){n++;if(!b)throw new AssertionError(message);}
 static TableTracker.Detection d(String r,float x,int seat,int group){return new TableTracker.Detection(r,x,100,seat,group);}
 static TableTracker.Result repeat(TableTracker t,TableTracker.Detection... reads){TableTracker.Result r=null;for(int i=0;i<3;i++)r=t.observe(Arrays.asList(reads),1000);return r;}
 public static void main(String[] args){
  TableTracker t=new TableTracker();TableTracker.Result r=repeat(t,d("8",100,1,1),d("8",140,1,1),d("A",500,4,4));
  check(r.counted.get(1).size()==2,"two equal cards remain distinct");check(r.counted.get(4).size()==1,"empty seats do not renumber seat 4");
  r=repeat(t,d("8",101,1,1),d("8",140,1,1),d("A",500,4,4));check(r.counted.get(1).size()==2,"jitter not recounted");
  r=repeat(t,d("8",101,1,1),d("8",140,1,2),d("A",500,4,4),d("3",180,1,2));
  check(r.counted.get(1).size()==3,"one new hit counted once");check(r.hands.get(2).size()==2,"split groups separated for advice");
  r=repeat(t,d("8",101,1,1),d("3",180,1,2),d("A",500,4,4));check(r.counted.get(1).size()==3&&r.unsafeSeats.contains(1),"occlusion retains history, blocks seat advice");
  r=repeat(t,d("8",101,1,1),d("8",250,1,2),d("3",180,1,2),d("A",500,4,4));check(r.counted.get(1).size()==3,"large movement held instead of duplicate count");
  r=repeat(t,d("8",101,2,1),d("8",140,1,2),d("3",180,1,2),d("A",500,4,4));check(r.counted.get(2).isEmpty()&&r.unsafeSeats.contains(2),"seat boundary cannot recount card");
  t.clear();r=repeat(t,d("K",100,1,1),d("K",102,1,1));check(r.counted.get(1).size()==1,"duplicate observations deduplicated globally");
  r=repeat(t,d("Q",100,1,1));check(r.counted.get(1).equals(Arrays.asList("K"))&&r.uncertain,"conflict not accepted");
  t.clear();r=repeat(t,d("Q",100,1,1));check(r.counted.get(1).equals(Arrays.asList("Q")),"new round clears identity");
  t.clear();t.observe(Arrays.asList(d("2",100,1,1)),1000);t.observe(Collections.emptyList(),1000);r=t.observe(Arrays.asList(d("2",100,1,1)),1000);check(r.counted.get(1).isEmpty(),"consecutive confirmation required");
  check(!r.uncertain&&r.unsafeSeats.contains(1),"normal confirmation delay blocks advice without marking count corrupt");
  System.out.println("PASS: "+n+" table tracker checks");
 }
}
