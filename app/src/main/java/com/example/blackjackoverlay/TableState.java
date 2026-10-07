package com.example.blackjackoverlay;
import java.util.*;

/** Temporal rank multiplicity tracker, fed globally deduplicated, seat-grouped reads. */
public final class TableState {
    public static final String[] RANKS={"A","2","3","4","5","6","7","8","9","10","J","Q","K"};
    public final List<List<String>> hands=new ArrayList<>();
    private final int[][] accepted=new int[8][13],streak=new int[8][13];
    private final int[] seen=new int[13];
    private final int[][] proposed=new int[8][13];
    public int decks=8,running=0,observed=0,round=0,shoe=0;
    public boolean shoeKnown=false,uncertain=true,inRound=false,burning=false,cutSeen=false;
    public String reason="Joined mid-shoe · wait for new shoe",phase="WAIT";
    public int burnMin=0,burnMax=0;
    private int idleStreak=0,burnStreak=0,playStreak=0;
    private boolean awaitingNewShoe=false;
    private int visualDealStreak=0,visualEmptyStreak=0;private long emptySince=0;
    public TableState(){for(int i=0;i<8;i++)hands.add(new ArrayList<>());}
    public void flag(String why){uncertain=true;reason=why;}
    public void interrupted(){flag("Capture paused or interrupted · wait for next shoe");clearStreaks();}
    public void clearStreaks(){for(int[] a:streak)Arrays.fill(a,0);}
    public void startShoe(){
        burning=false;awaitingNewShoe=false;idleStreak=burnStreak=playStreak=0;phase="BETTING";
        shoe++;running=observed=0;Arrays.fill(seen,0);shoeKnown=true;uncertain=false;
        burnMin=1;burnMax=10;cutSeen=false;clearHands();reason="Burn quantity unknown (1–10)";
    }
    public void setBurns(int n){burnMin=burnMax=n;}
    public void clearHands(){visualDealStreak=visualEmptyStreak=0;emptySince=0;for(List<String> h:hands)h.clear();for(int[] a:accepted)Arrays.fill(a,0);clearStreaks();inRound=false;}
    public void status(String text){
        String s=text.toLowerCase(Locale.ROOT);
        boolean burn=s.contains("burn")&&s.contains("procedure");
        boolean idle=s.contains("play behind")||s.contains("take a seat")||s.contains("place your");
        boolean play=s.contains("round in progress");
        burnStreak=burn?burnStreak+1:0;idleStreak=idle?idleStreak+1:0;playStreak=play?playStreak+1:0;
        if(burnStreak>=3&&!burning){burning=true;awaitingNewShoe=true;phase="BURN";clearHands();}
        if(idleStreak>=3){
            if(awaitingNewShoe){startShoe();awaitingNewShoe=false;burning=false;}
            if(inRound)clearHands();phase="BETTING";
        }
        if(playStreak>=3&&!burning){
            if(!inRound){inRound=true;round++;}phase="PLAY";
        }
    }
    /** Card evidence can start play when the game's phase text is covered. Never clear on OCR loss alone. */
    public boolean scene(String text,List<List<String>> ranks,int groups,long now){
        boolean dealer=!ranks.get(0).isEmpty(),pair=false;
        for(int i=1;i<8;i++)if(ranks.get(i).size()>=2)pair=true;
        boolean empty=groups==0&&!dealer;
        if(empty){if(visualEmptyStreak++==0)emptySince=now;}else{visualEmptyStreak=0;emptySince=0;}
        boolean cleared=visualEmptyStreak>=4&&now-emptySince>=3000;
        if(cleared){clearHands();phase="BETTING";}
        // An idle label may remain on screen for observers during an active hand.
        String lower=text.toLowerCase(Locale.ROOT);
        if(empty||lower.contains("burn")||lower.contains("round in progress"))status(text);
        visualDealStreak=dealer&&pair&&groups>0?visualDealStreak+1:0;
        if(!inRound&&!burning&&visualDealStreak>=3){inRound=true;round++;phase="PLAY";}
        return cleared;
    }
    public List<String> observe(int seat,List<String> ranks){return observeRequired(seat,ranks,3);}
    public List<String> observeConfirmed(int seat,List<String> ranks){return observeRequired(seat,ranks,1);}
    private List<String> observeRequired(int seat,List<String> ranks,int required){
        List<String> added=new ArrayList<>();if(burning||seat<0||seat>=8)return added;
        int[] counts=new int[13];for(String r:ranks){int k=Arrays.asList(RANKS).indexOf(r);if(k>=0)counts[k]++;}
        for(int k=0;k<13;k++){
            int c=counts[k];
            if(c>accepted[seat][k]){
                if(proposed[seat][k]==c)streak[seat][k]++;else{proposed[seat][k]=c;streak[seat][k]=1;}
                if(streak[seat][k]>=required){
                    int delta=c-accepted[seat][k];
                    if(c>4*decks||hands.get(seat).size()+delta>21||seen[k]+delta>4*decks){flag("Implausible rank count in seat "+seat);continue;}
                    for(int j=0;j<delta;j++){String r=RANKS[k];hands.get(seat).add(r);added.add(r);seen[k]++;observed++;running+=CountEngine.value(r);}
                    accepted[seat][k]=c;
                }
            }else streak[seat][k]=0;
        }
        return added;
    }
    public void correctHand(int seat,List<String> ranks){
        if(seat<0||seat>=8)throw new IllegalArgumentException("Invalid seat");
        int[] corrected=new int[13];for(String r:ranks){int k=Arrays.asList(RANKS).indexOf(r);if(k<0)throw new IllegalArgumentException("Invalid rank");corrected[k]++;}
        // Preserve recognition watermarks so repeated prior reads cannot undo corrections.
        // Manual intervention marks the shoe uncertain; hidden/replaced cards still need review.
        for(int k=0;k<13;k++)accepted[seat][k]=Math.max(accepted[seat][k],corrected[k]);
        for(String r:hands.get(seat)){int k=Arrays.asList(RANKS).indexOf(r);seen[k]--;observed--;running-=CountEngine.value(r);}
        hands.get(seat).clear();Arrays.fill(streak[seat],0);
        for(String r:ranks){int k=Arrays.asList(RANKS).indexOf(r);seen[k]++;observed++;running+=CountEngine.value(r);hands.get(seat).add(r);}
        flag("Manual correction · verify shoe history");
    }
    public double decksRemaining(){return Math.max(.25,(decks*52-observed-(inRound&&hands.get(0).size()==1?1:0)-(burnMin+burnMax)/2.0)/52.0);}
    public double trueCount(){return running/decksRemaining();}
    public boolean qualified(){return shoeKnown&&!uncertain&&burnMin==burnMax;}
    public String quality(){return !shoeKnown?"PARTIAL SHOE":uncertain?"REVIEW REQUIRED":burnMin!=burnMax?"BURNS UNKNOWN":"TRACKING · UNVALIDATED";}
}
