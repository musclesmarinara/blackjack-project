import com.example.blackjackoverlay.*;
import java.util.*;import java.nio.file.*;
public class RoundRegressionTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static List<List<String>> empty(){List<List<String>> h=new ArrayList<>();for(int i=0;i<8;i++)h.add(new ArrayList<>());return h;}
 static List<List<String>> hands(){List<List<String>> h=empty();h.get(0).add("A");h.get(2).addAll(Arrays.asList("A","8"));return h;}
 public static void main(String[] args)throws Exception{
  TableState early=new TableState();early.observeConfirmed(3,Arrays.asList("K"));check(early.observed==1&&early.running==-1,"confirmed card counts before dealer or round text");
  for(int i=0;i<3;i++)early.scene("round in progress",hands(),3,1000*i);early.observeConfirmed(3,Arrays.asList("K"));check(early.observed==1,"round start does not recount early card");
  TableState visual=new TableState();visual.observeConfirmed(2,Arrays.asList("A","8"));for(int i=0;i<3;i++)visual.scene("",hands(),3,1000*i);visual.observeConfirmed(2,Arrays.asList("A","8"));check(visual.observed==2,"visual round start retains ledger");
  TableState manual=new TableState();manual.observeConfirmed(2,Arrays.asList("K"));manual.correctHand(2,Arrays.asList("A"));manual.observeConfirmed(2,Arrays.asList("K"));check(manual.observed==1&&manual.hands.get(2).equals(Arrays.asList("A")),"stale OCR cannot undo correction");manual.observeConfirmed(2,Arrays.asList("K","3"));check(manual.observed==2&&manual.running==0,"different-rank hit remains countable after correction");
  TableState shoe=new TableState();for(int i=0;i<3;i++)shoe.status("burn procedure");shoe.startShoe();shoe.observeConfirmed(1,Arrays.asList("2"));check(shoe.observed==1&&!shoe.burning,"manual shoe exits burn phase");for(int i=0;i<3;i++)shoe.status("place your bets");check(shoe.shoe==1,"manual shoe not reset twice");
  TableState t=new TableState();for(int i=0;i<3;i++){t.flag("Ambiguous detections skipped");t.scene("",hands(),3,1000*i);}
  check(t.inRound&&t.round==1,"valid hand starts despite unrelated warning");t.observeConfirmed(2,Arrays.asList("A","8"));check(t.observed==2,"confirmed cards counted");
  TableState waiting=new TableState();boolean reset=false;for(int i=0;i<4;i++)reset=waiting.scene("",empty(),0,1000*i);check(reset,"empty table resets identities before first detected round");check(waiting.observed==0&&waiting.round==0,"clear cannot invent count or round");
  TableState oldGate=new TableState();for(int i=0;i<5;i++)oldGate.scene("",hands(),-1,1000*i);check(!oldGate.inRound,"reproduced old global-warning deadlock");
  if(args.length>0){TableState replay=new TableState();int clears=0;for(String line:Files.readAllLines(Paths.get(args[0]))){
   String[] fields=line.split("\\t",-1);int groups=Integer.parseInt(fields[1]);List<List<String>> cards=new ArrayList<>();for(int i=2;i<10;i++)cards.add(fields[i].isEmpty()?new ArrayList<>():new ArrayList<>(Arrays.asList(fields[i].split(","))));
   if(replay.scene("",cards,groups,Long.parseLong(fields[0])))clears++;
  }check(replay.round>0,"uploaded diagnostics no longer stuck on round zero");System.out.println("Uploaded rank-stream replay: "+replay.round+" round starts; "+clears+" empty-table confirmations. Not a card-count accuracy test.");}
  System.out.println("PASS: round-block and pre-round identity-reset regressions");
 }
}
