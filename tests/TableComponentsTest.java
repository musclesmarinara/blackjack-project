import com.example.blackjackoverlay.*;import java.util.*;
public class TableComponentsTest {
 static int n;static void check(boolean b,String m){n++;if(!b)throw new AssertionError(m);}
 static final int W=540,H=360;static final float[] ROI={0,40,540,40,540,300,0,300};
 static final float[][] A={{270,80},{50,240},{125,240},{200,240},{275,240},{350,240},{425,240},{500,240}};
 static void rect(int[] p,int l,int t,int r,int b){for(int y=t;y<b;y++)for(int x=l;x<r;x++)p[y*W+x]=0xffffffff;}
 public static void main(String[] args){
  int[] p=new int[W*H];rect(p,255,60,285,100);rect(p,110,220,140,260);rect(p,410,220,440,260);
  TableComponents c=new TableComponents(p,W,H,ROI,A);Set<Integer> owners=new HashSet<>();for(TableComponents.Group g:c.groups)owners.add(g.owner);
  check(owners.equals(new HashSet<>(Arrays.asList(0,2,6))),"physical dealer and fixed seat anchors from one ROI");
  rect(p,100,10,140,30);c=new TableComponents(p,W,H,ROI,A);check(c.groups.size()==3,"digital cards outside table excluded");
  rect(p,20,150,510,151);c=new TableComponents(p,W,H,ROI,A);check(c.groups.size()==3,"thin table lettering removed");
  p=new int[W*H];rect(p,100,220,220,260);c=new TableComponents(p,W,H,ROI,A);check(c.groups.isEmpty()&&c.ambiguous>0,"merged face across two seat anchors withheld");
  p=new int[W*H];rect(p,72,220,103,260);c=new TableComponents(p,W,H,ROI,A);check(c.groups.isEmpty()&&c.ambiguous>0,"equidistant seat ownership withheld");
  p=new int[W*H];rect(p,119,230,131,242);c=new TableComponents(p,W,H,ROI,A);check(c.groups.isEmpty()&&c.unresolvedFaces,"small unresolved face cannot establish an empty table");
  int[] empty=new int[W*H];rect(empty,240,150,290,158); // stationary felt lettering
  c=new TableComponents(empty,W,H,ROI,A,empty.clone());check(c.groups.isEmpty()&&!c.unresolvedFaces,"reference removes unchanged markings");
  int[] dealt=empty.clone();rect(dealt,258,75,282,84); // small foreshortened dealer face
  c=new TableComponents(dealt,W,H,ROI,A,empty);check(c.groups.size()==1&&c.groups.get(0).owner==0,"small dealer face survives reference pipeline");
  c=new TableComponents(empty,W,H,ROI,A,empty);check(c.groups.isEmpty(),"dealer disappearance permits clean-table evidence");
  boolean rejected=false;try{new TableComponents(empty,W,H,ROI,A,new int[1]);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"mismatched reference dimensions rejected");
  int[] tiny=empty.clone();rect(tiny,119,230,126,238);c=new TableComponents(tiny,W,H,ROI,A,empty);check(c.unresolvedFaces,"unreadable small face still prevents false empty");
  int[] partial=empty.clone();rect(partial,258,75,282,84);int[] patterned=empty.clone();rect(patterned,265,75,273,84);
  c=new TableComponents(partial,W,H,ROI,A,patterned);check(c.groups.size()==1&&c.groups.get(0).right-c.groups.get(0).left>=22,"unchanged white pixels do not fragment new card");
  int[] sleeve=empty.clone();rect(sleeve,258,42,282,50);c=new TableComponents(sleeve,W,H,ROI,A,empty);check(c.groups.isEmpty(),"clothing above dealer anchor excluded");
  System.out.println("PASS: "+n+" table region checks");
 }
}
