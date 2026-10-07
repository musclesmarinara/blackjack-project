package com.example.blackjackoverlay;
import java.util.*;

/** Chart policy, with a documented subset of published Hi-Lo shoe indices.
 * No claim of exact composition-dependent EV. Unknown rules stay explicit. */
public final class Strategy {
    public static class Advice {
        public String action, fallback, basis;
        Advice(String a,String f,String b){action=a;fallback=f;basis=b;}
        public String toString(){return action+" | fallback "+fallback+" | "+basis;}
    }
    public static int value(String r){return r.equals("A")?11:r.matches("[JQK]")?10:Integer.parseInt(r);}
    public static int total(List<String> cards){int n=0,a=0;for(String c:cards){n+=value(c);if(c.equals("A"))a++;}while(n>21&&a-->0)n-=10;return n;}
    public static boolean soft(List<String> cards){int n=0,a=0;for(String c:cards){n+=value(c);if(c.equals("A"))a++;}while(n>21&&a>0){n-=10;a--;}return a>0;}
    public static Advice advise(List<String> cards,String up,double tc,boolean useCount,boolean splitHand){
        if(cards.size()<2||up==null)return new Advice("WAIT","—","Need two cards and dealer upcard");
        int n=total(cards),d=value(up);boolean s=soft(cards),two=cards.size()==2;
        if(n>21)return new Advice("BUST","—","Observed hand");
        if(n==21)return new Advice("STAND","STAND","21");
        String fallback=hitStand(n,s,d,tc,useCount),a=fallback;
        if(two&& !splitHand && value(cards.get(0))==value(cards.get(1))){
            int p=value(cards.get(0));
            boolean split=p==11||p==8||p==9&&(d>=2&&d<=6||d==8||d==9)||p==7&&d<=7||p==6&&d<=6||p==4&&(d==5||d==6)||(p==2||p==3)&&d<=7;
            if(split)a="SPLIT";
        }
        if(!a.equals("SPLIT")&&two){
            boolean dbl=s?(n==13||n==14?d==5||d==6:n==15||n==16?d>=4&&d<=6:n==17||n==18?d>=3&&d<=6:false):n==9?d>=3&&d<=6:n==10?d<=9:n==11?d<=10:false;
            if(dbl)a="DOUBLE";
            if(useCount&&!s){
                if(n==9&&d==2)a=tc>=1?"DOUBLE":fallback;
                if(n==9&&d==7)a=tc>=3?"DOUBLE":fallback;
                if(n==10&&(d==10||d==11))a=tc>=4?"DOUBLE":fallback;
                if(n==11&&d==11)a=tc>=1?"DOUBLE":fallback;
            }
        }
        String basis=useCount?"Hi-Lo shoe indices (experimental)":"Basic strategy · count not qualified";
        if(a.equals("SPLIT")||a.equals("DOUBLE"))basis+=" · rule-dependent";
        if(d>=10)basis+=" · assumes dealer blackjack ruled out";
        return new Advice(a,fallback,basis);
    }
    private static String hitStand(int n,boolean soft,int d,double tc,boolean use){
        if(soft)return n>=19||n==18&&d<=8?"STAND":"HIT";
        boolean stand=n>=17||n>=13&&d<=6||n==12&&d>=4&&d<=6;
        if(use){
            Integer index=null;
            if(n==16&&d==10)index=0;if(n==15&&d==10)index=4;if(n==16&&d==9)index=5;
            if(n==12&&d==2)index=3;if(n==12&&d==3)index=2;if(n==12&&d==4)index=0;
            if(n==12&&d==5)index=-2;if(n==12&&d==6)index=-1;if(n==13&&d==2)index=-1;if(n==13&&d==3)index=-2;
            if(index!=null)stand=tc>=index;
        }
        return stand?"STAND":"HIT";
    }
}
