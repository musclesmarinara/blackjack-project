package com.example.blackjackoverlay;
import java.util.*;

/** Global rank-corner tracks. Counting ownership never changes during a round.
 * Unmatched detections are held if a confirmed corner disappears from that seat:
 * it may have moved rather than being a newly dealt card. No guessed replacement.
 */
public final class TableTracker {
 public static final class Detection {
  public final String rank; public final float x,y; public final int seat,group;
  public Detection(String rank,float x,float y,int seat,int group){this.rank=rank;this.x=x;this.y=y;this.seat=seat;this.group=group;}
 }
 private static final class Track {long id;float x,y;int seat,group,last,streak;String candidate,rank;}
 public static final class Result {
  public final List<List<String>> counted=new ArrayList<>(),visible=new ArrayList<>();
  public final Map<Integer,List<String>> hands=new TreeMap<>();
  public final Map<Integer,Integer> owners=new HashMap<>();
  public final Set<Integer> unsafeSeats=new HashSet<>();
  public boolean uncertain;public String reason="";public String tracks="";
  Result(){for(int i=0;i<8;i++){counted.add(new ArrayList<>());visible.add(new ArrayList<>());}}
 }
 private final List<Track> tracks=new ArrayList<>();private int frame;private long next=1;
 public void clear(){tracks.clear();frame=0;}
 public Result observe(List<Detection> input,int width){
  frame++;Result out=new Result();List<Detection> reads=new ArrayList<>();
  for(Detection d:input){
   if(d.seat<0||d.seat>7||CountEngine.parseRank(d.rank)==null||!Float.isFinite(d.x)||!Float.isFinite(d.y))continue;
   boolean duplicate=false;for(Detection p:reads)if(Math.hypot(p.x-d.x,p.y-d.y)<width*.010){duplicate=true;if(!p.rank.equals(d.rank)){out.unsafeSeats.add(d.seat);out.unsafeSeats.add(p.seat);}}
   if(!duplicate)reads.add(d);
  }
  Set<Track> used=new HashSet<>();Set<Integer> consumed=new HashSet<>();
  // Global nearest-neighbour matching; never transfer a confirmed card between seats.
  while(true){Track best=null;int index=-1;double distance=width*.024;
   for(int i=0;i<reads.size();i++)if(!consumed.contains(i))for(Track t:tracks)if(!used.contains(t)){
    Detection d=reads.get(i);double dd=Math.hypot(t.x-d.x,t.y-d.y);
    if(dd<distance){distance=dd;best=t;index=i;}
   }
   if(best==null)break;Detection d=reads.get(index);used.add(best);consumed.add(index);
   if(best.seat!=d.seat||(best.rank!=null&&!best.rank.equals(d.rank))){out.unsafeSeats.add(best.seat);out.unsafeSeats.add(d.seat);best.last=frame;continue;}
   update(best,d);
  }
  Set<Integer> missing=new HashSet<>();for(Track t:tracks)if(t.rank!=null&&!used.contains(t))missing.add(t.seat);
  out.unsafeSeats.addAll(missing);
  for(int i=0;i<reads.size();i++)if(!consumed.contains(i)){
   Detection d=reads.get(i);if(missing.contains(d.seat)||out.unsafeSeats.contains(d.seat))continue;
   Track t=new Track();t.id=next++;t.seat=d.seat;tracks.add(t);update(t,d);
  }
  // Confirmation is consecutive and suppressed for ambiguous seats. Was streak>=3;
  // at the real-world ~0.8s/cycle this measured on a busy multi-hand table, that
  // was ~2.4s a card had to stay unmoved and unobstructed before it ever counted,
  // which is long enough that quickly-covered cards never got confirmed at all.
  // Dropping to 2 still requires one independent repeat (rules out a single bad
  // OCR frame) while roughly halving that dwell time.
  for(Track t:tracks)if(t.last==frame&&t.rank==null&&t.streak>=2&&!out.unsafeSeats.contains(t.seat))t.rank=t.candidate;
  tracks.removeIf(t->t.rank==null&&frame-t.last>3);
  StringBuilder log=new StringBuilder();
  for(Track t:tracks){
   if(t.rank!=null){out.counted.get(t.seat).add(t.rank);log.append(t.id).append(':').append(t.seat).append(':').append(t.rank).append('@').append(Math.round(t.x)).append(',').append(Math.round(t.y)).append(';');}
   // Was t.last==frame: required a confirmed card to be freshly re-OCR'd in this
   // EXACT frame to count as "visible" -- which is what dealerUp/advice is built
   // from. That's a much stricter bar than staying in `tracks` (confirmed cards
   // persist for up to 3 missed frames). Measured directly: the dealer's card was
   // confirmed in 173/529 samples one real session but was "visible" in ZERO of
   // them, so dealerUp could never be set and advice never fired. A confirmed
   // card should stay visible for as long as it's tracked, not just the instant
   // it's re-read.
   if(t.rank!=null&&!out.unsafeSeats.contains(t.seat)){
    out.visible.get(t.seat).add(t.rank);out.hands.computeIfAbsent(t.group,k->new ArrayList<>()).add(t.rank);out.owners.put(t.group,t.seat);
   }
  }
  out.uncertain=!out.unsafeSeats.isEmpty();
  // Any provisional or rejected rank blocks advice for its seat, not other seats.
  for(Detection d:reads){int total=0;for(Track t:tracks)if(t.seat==d.seat&&t.last==frame&&t.rank!=null)total++;
   long expected=reads.stream().filter(p->p.seat==d.seat).count();if(total!=expected)out.unsafeSeats.add(d.seat);
  }
  if(out.uncertain)out.reason="Moved, hidden or conflicting cards at seats "+out.unsafeSeats;
  out.tracks=log.toString();return out;
 }
 private void update(Track t,Detection d){
  t.streak=d.rank.equals(t.candidate)&&t.last==frame-1?t.streak+1:1;
  t.candidate=d.rank;t.x=d.x;t.y=d.y;t.group=d.group;t.last=frame;
 }
}
