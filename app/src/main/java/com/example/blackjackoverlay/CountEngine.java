package com.example.blackjackoverlay;

import java.util.*;

/** One calibrated region per physical card rank. Latches last until nextHand(). */
public final class CountEngine {
    private static final class Slot {
        String candidate, accepted;
        int streak;
        boolean suppressed;
    }
    private static final class Event {
        final int slot; final String rank; final long hand;
        Event(int slot, String rank, long hand) { this.slot=slot; this.rank=rank; this.hand=hand; }
    }
    private final Map<Integer, Slot> slots = new HashMap<>();
    private final Deque<Event> history = new ArrayDeque<>();
    private int running, cards;
    private long hand;
    public static String parseRank(String raw) {
        if (raw == null) return null;
        String s=raw.toUpperCase(Locale.ROOT).replaceAll("[\\s♠♥♦♣]", "");
        return s.matches("(?:[2-9]|10|J|Q|K|A)") ? s : null;
    }
    public static int value(String rank) {
        if (rank.matches("[2-6]")) return 1;
        return rank.matches("[7-9]") ? 0 : -1;
    }
    /** Called once per successful OCR sampling cycle, including null readings. */
    public boolean observe(int id, String raw) {
        Slot s=slots.computeIfAbsent(id, k -> new Slot());
        if(s.accepted != null || s.suppressed) return false;
        String rank=parseRank(raw);
        if(rank==null) { s.candidate=null; s.streak=0; return false; }
        if(rank.equals(s.candidate)) s.streak++; else { s.candidate=rank; s.streak=1; }
        if(s.streak<3) return false;
        s.accepted=rank; running+=value(rank); cards++;
        history.push(new Event(id,rank,hand)); return true;
    }
    public String accepted(int id) { Slot s=slots.get(id); return s==null?null:s.accepted; }
    public void breakCandidates() { for(Slot s:slots.values()){s.candidate=null;s.streak=0;} }
    public void nextHand() { slots.clear(); hand++; }
    public void resetShoe() { slots.clear(); history.clear(); running=0; cards=0; hand++; }
    public boolean undo() {
        if(history.isEmpty()) return false;
        Event e=history.pop(); running-=value(e.rank); cards--;
        if(e.hand==hand) {
            Slot s=slots.get(e.slot);
            if(s!=null) {s.accepted=null;s.suppressed=true;}
        }
        return true;
    }
    public int running() { return running; }
    public int cards() { return cards; }
    public String last() { return history.isEmpty()?"—":history.peek().rank; }
}
