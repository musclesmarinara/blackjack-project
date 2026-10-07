import com.example.blackjackoverlay.*;
public class CornerSelectorTest {
 static void check(boolean b,String msg){if(!b)throw new AssertionError(msg);}
 public static void main(String[] args){
  CornerSelector c=new CornerSelector();check(c.choose(1,0,0)==0,"no evidence cannot select a mode");
  check(c.choose(1,0,2)==-1,"inverted readable corners accepted");
  check(c.choose(1,3,1)==-1,"cannot switch corner ends during a hand");
  check(c.choose(2,2,2)==1,"a tie selects only one end");
  c.clear();check(c.choose(1,2,0)==1,"confirmed boundary releases orientation");
  check(RankFilter.orientation(175)==-1&&RankFilter.orientation(-180)==-1,"inverted classification");
  check(RankFilter.orientation(90)==0&&RankFilter.orientation(Float.NaN)==0,"sideways and invalid rejected");
  System.out.println("PASS: 7 corner-orientation regressions");
 }
}
