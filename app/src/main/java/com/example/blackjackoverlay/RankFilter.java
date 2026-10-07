package com.example.blackjackoverlay;
/** Reject opposite-end card ranks after deskewing a whole-hand image. */
public final class RankFilter {
    public static int orientation(float angle){if(!Float.isFinite(angle))return 0;if(Math.abs(angle)<=55)return 1;if(Math.abs(Math.abs(angle)-180)<=55)return -1;return 0;}
    public static boolean upright(float angle){return !Float.isNaN(angle)&&!Float.isInfinite(angle)&&Math.abs(angle)<=55;}
}
