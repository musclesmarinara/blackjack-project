package com.example.blackjackoverlay;
import java.util.*;

/** Persistent rank-corner identities between confirmed table clears. */
public final class CardIdentity {
 private static final class Track {float x,y;int seat,streak,lastFrame;String candidate,rank;}
 private final List<Track> tracks=new ArrayList<>();private int frame;
 public boolean conflict;
 public void clear(){tracks.clear();frame=0;conflict=false;}
 public List<List<String>> observe(List<HandGrouping.Read> reads,int width){
  frame++;conflict=false;Set<Track> used=new HashSet<>();Set<Integer> consumed=new HashSet<>();
  // Closest pairs first keeps neighboring identical-rank cards distinct.
  while(true){Track best=null;int index=-1;double distance=width*.023;
   for(int i=0;i<reads.size();i++)if(!consumed.contains(i)){HandGrouping.Read r=reads.get(i);
    for(Track t:tracks)if(!used.contains(t)&&(r.window==0)==(t.seat==0)){
     double d=Math.hypot(r.x-t.x,r.y-t.y);if(d<distance){distance=d;best=t;index=i;}
    }
   }
   if(best==null)break;update(best,reads.get(index));used.add(best);consumed.add(index);
  }
  for(int i=0;i<reads.size();i++)if(!consumed.contains(i)){
   HandGrouping.Read r=reads.get(i);Track t=new Track();t.seat=r.window;t.candidate=r.rank;t.x=r.x;t.y=r.y;tracks.add(t);update(t,r);
  }
  tracks.removeIf(t->t.rank==null&&frame-t.lastFrame>4);
  List<List<String>> result=new ArrayList<>();for(int i=0;i<8;i++)result.add(new ArrayList<>());
  for(Track t:tracks)if(t.rank!=null)result.get(t.seat).add(t.rank);return result;
 }
 private void update(Track t,HandGrouping.Read r){
  if(t.rank!=null&&!t.rank.equals(r.rank)){conflict=true;t.lastFrame=frame;return;}
  if(t.candidate.equals(r.rank)&&t.lastFrame==frame-1)t.streak++;else{t.candidate=r.rank;t.streak=1;}
  t.x=r.x;t.y=r.y;t.lastFrame=frame;if(t.streak>=3)t.rank=t.candidate;
 }
}
