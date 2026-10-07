package com.example.blackjackoverlay;
/** A stop or restart invalidates any OCR result already in flight. */
public final class ScanControl {
    private boolean running;private int generation;
    public synchronized int start(){running=true;return ++generation;}
    public synchronized void stop(){running=false;generation++;}
    public synchronized boolean running(){return running;}
    public synchronized int token(){return generation;}
    public synchronized boolean accept(int token){return running&&generation==token;}
}
