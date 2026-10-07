import com.example.blackjackoverlay.*;
import java.util.*;
public class CoreTest {
 static int checks=0;static void check(boolean v,String msg){checks++;if(!v)throw new AssertionError(msg);}
 static List<String> c(String...s){return Arrays.asList(s);}
 static void observe(TableState t,int seat,String...s){for(int i=0;i<3;i++)t.observe(seat,c(s));}
 public static void main(String[]args){
  check(CountEngine.parseRank("Q♠").equals("Q"),"suit stripped");check(CountEngine.parseRank("20")==null,"total rejected");
  check(Strategy.total(c("A","A","9"))==21,"multiple aces");check(Strategy.soft(c("A","6")),"soft17");check(!Strategy.soft(c("A","6","K")),"hard17");
  check(Strategy.advise(c("8","8"),"K",0,false,false).action.equals("SPLIT"),"8s split");
  check(Strategy.advise(c("8","8"),"K",2,true,false).fallback.equals("STAND"),"pair fallback uses index");
  check(Strategy.advise(c("A","7"),"4",0,false,false).fallback.equals("STAND"),"soft18 double fallback");
  check(Strategy.advise(c("A","7"),"4",0,false,false).action.equals("DOUBLE"),"soft18 double");
  check(Strategy.advise(c("10","6"),"10",-.1,true,false).action.equals("HIT"),"16 below0");
  check(Strategy.advise(c("10","6"),"10",0,true,false).action.equals("STAND"),"16 boundary0");
  check(Strategy.advise(c("10","5"),"10",3.99,true,false).action.equals("HIT"),"15 below4");
  check(Strategy.advise(c("10","5"),"10",4,true,false).action.equals("STAND"),"15 boundary4");
  check(Strategy.advise(c("7","5"),"5",-3,true,false).action.equals("HIT"),"negative count reversal");
  check(Strategy.advise(c("2","3","6"),"6",9,true,false).action.equals("HIT"),"no 3-card double");
  check(Strategy.advise(c("K","K"),"6",9,true,false).action.equals("STAND"),"10 splitting intentionally not implemented");
  TableState t=new TableState();check(!t.qualified(),"midshoe unqualified");
  for(int i=0;i<3;i++)t.status("Burn cards procedure");check(t.burning,"burn phase");
  for(int i=0;i<3;i++)t.status("Play behind other participants");check(t.shoe==1&&t.shoeKnown,"new shoe");check(!t.qualified(),"unknown burn");t.setBurns(4);check(t.qualified(),"known burn");
  for(int i=0;i<3;i++)t.status("Round in progress");
  observe(t,1,"8","8");check(t.observed==2,"two identical cards preserved");observe(t,1,"8","8");check(t.observed==2,"no repeated-frame duplicates");
  observe(t,1);observe(t,1,"8","8");check(t.observed==2,"occlusion no recount");
  observe(t,1,"8","8","2");check(t.observed==3&&t.running==1,"new dealt rank");
  observe(t,2,"2","A");check(t.observed==5&&t.running==1,"separate seats");
  for(int i=0;i<3;i++)t.status("Play behind other participants");for(int i=0;i<3;i++)t.status("Round in progress");observe(t,1,"8","8");check(t.observed==7,"next hand same cards");
  t.interrupted();check(!t.qualified(),"capture gap disables count advice");
  t.correctHand(1,c("3","A"));check(t.observed==7&&t.running==1,"correction reverses old values");
  TableState noise=new TableState();noise.inRound=true;noise.observe(1,c("K"));noise.observe(1,c("K"));noise.observe(1,c());noise.observe(1,c("K"));check(noise.observed==0,"unstable read rejected");
  t.status("Burn cards procedure");check(t.shoe==1,"one burn frame cannot reset");
  System.out.println("PASS: "+checks+" core checks");
 }
}
