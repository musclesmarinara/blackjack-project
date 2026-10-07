package com.example.blackjackoverlay;
import java.util.Arrays;
/** Select one printed-corner orientation per seat; reset only with visual identity. */
public final class CornerSelector {
 private final int[] modes=new int[8];
 public int choose(int seat,int upright,int inverted){
  if(seat<0||seat>=8||upright<0||inverted<0)throw new IllegalArgumentException();
  if(modes[seat]==0&&upright+inverted>0)modes[seat]=upright>=inverted?1:-1;
  return modes[seat];
 }
 public void clear(){Arrays.fill(modes,0);}
 @Override public String toString(){return Arrays.toString(modes);}
}
