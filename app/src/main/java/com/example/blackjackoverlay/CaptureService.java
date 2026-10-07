package com.example.blackjackoverlay;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.hardware.display.*;
import android.media.*;
import android.media.projection.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.util.*;

public final class CaptureService extends Service {
    public static volatile boolean active=false;
    private final Handler main=new Handler(Looper.getMainLooper());
    private HandlerThread thread;private Handler worker;private MediaProjection projection;private VirtualDisplay display;private ImageReader reader;
    private WindowManager wm;private LinearLayout panel,controls;private TextView title,detail,runLight;private Button runButton;private final ScanControl scan=new ScanControl();private WindowManager.LayoutParams lp;
    private volatile RectF overlayBounds;
    private CalibrationView calibration;private Bitmap calibrationBitmap,latest;
    private Regions regions;private TableState state;private Vision vision;private ClipRecorder recorder;
    private PrintWriter log;private volatile boolean paused=true,closed=false;private boolean splitHand=false,recording=false;
    private List<String> adviceHand=new ArrayList<>();private List<List<String>> seatHands=new ArrayList<>();private int handIndex=0;private boolean adviceSafe=false;
    private volatile int seat=4;private int width,height,epoch=0,markerStreak=0,mismatch=0;private long lastFrame=0,lastAnalysis=0,lastGood=0,lastValidHand=0;
    private volatile String caption="Waiting for capture",recordError="";private String dealerUp=null,scanDiagnostic="Calibrate the table and press Start";private boolean experimentallyUseCount=false;
    private File clipFile;private int oldRound=0,oldShoe=0;
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public void onCreate(){
        super.onCreate();thread=new HandlerThread("capture-and-recognition");thread.start();worker=new Handler(thread.getLooper());wm=getSystemService(WindowManager.class);
        regions=Regions.load(this);state=new TableState();state.decks=getSharedPreferences("config",0).getInt("decks",8);seat=getSharedPreferences("config",0).getInt("seat",4);vision=new Vision();
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent!=null&&"STOP".equals(intent.getAction())){stopSelf();return START_NOT_STICKY;}
        if(active)return START_NOT_STICKY;
        if(intent==null||!intent.hasExtra("data")){stopSelf();return START_NOT_STICKY;}
        NotificationManager nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel("capture","Observation capture",NotificationManager.IMPORTANCE_LOW));
        PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,CaptureService.class).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent home=PendingIntent.getActivity(this,2,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification=new Notification.Builder(this,"capture").setSmallIcon(android.R.drawable.ic_menu_view).setContentTitle("Blackjack Project v.01").setContentText("Screen capture active · tap Stop to end").setContentIntent(home).addAction(android.R.drawable.ic_media_pause,"Stop",stop).setOngoing(true).build();
        startForeground(1,notification);active=true;
        try{
            Intent data=intent.getParcelableExtra("data");projection=getSystemService(MediaProjectionManager.class).getMediaProjection(intent.getIntExtra("code",Activity.RESULT_OK),data);
            android.util.DisplayMetrics metrics=new android.util.DisplayMetrics();wm.getDefaultDisplay().getRealMetrics(metrics);
            // Preserve native pixels for rank OCR; segmentation alone is downscaled.
            width=metrics.widthPixels;height=metrics.heightPixels;
            projection.registerCallback(new MediaProjection.Callback(){
                @Override public void onStop(){main.post(()->stopSelf());}
                @Override public void onCapturedContentResize(int w,int h){if(Math.abs((double)w/h-(double)width/height)>.05)main.post(()->{toast("Display changed: stop and recalibrate");stopSelf();});}
            },main);
            reader=ImageReader.newInstance(width,height,PixelFormat.RGBA_8888,2);reader.setOnImageAvailableListener(this::frame,worker);
            display=projection.createVirtualDisplay("Observation",width,height,metrics.densityDpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader.getSurface(),null,worker);
            File logs=new File(getFilesDir(),"sessions");logs.mkdirs();String stamp=Long.toString(System.currentTimeMillis());
            log=new PrintWriter(new File(logs,stamp+".jsonl"));clipFile=new File(logs,stamp+".mp4");
            getSharedPreferences("config",0).edit().putString("lastLog",new File(logs,stamp+".jsonl").toString()).apply();
            event("start","New capture session; scanner stopped; count history not restored");showPanel();worker.post(this::renderCaption);
        }catch(Exception e){toast("Capture failed: "+e.getMessage());stopSelf();}
        return START_NOT_STICKY;
    }
    private void frame(ImageReader source){
        if(closed)return;
        Image image=null;Bitmap frame=null;
        try{
            image=source.acquireLatestImage();if(image==null)return;long now=SystemClock.elapsedRealtime();if(now-lastFrame<200)return;
            if(lastFrame>0&&now-lastFrame>6000)state.flag("Capture gap detected");lastFrame=now;
            Image.Plane p=image.getPlanes()[0];int padded=p.getRowStride()/p.getPixelStride();
            Bitmap raw=Bitmap.createBitmap(padded,height,Bitmap.Config.ARGB_8888);raw.copyPixelsFromBuffer(p.getBuffer());
            frame=Bitmap.createBitmap(raw,0,0,width,height);if(frame!=raw)raw.recycle();image.close();image=null;
            if(latest!=null)latest.recycle();latest=frame;frame=null;
            if(scan.running()&&!paused&&regions.calibrated&&now-lastAnalysis>=650){
                lastAnalysis=now;
                try{int token=scan.token(),sampleSeat=seat;Vision.Sample sample=vision.read(latest,regions,sampleSeat,overlayBounds);synchronized(scan){if(scan.accept(token)&&seat==sampleSeat&&!paused&&!closed){accept(sample);lastGood=SystemClock.elapsedRealtime();}}}
                catch(Exception e){adviceSafe=false;lastValidHand=0;scanDiagnostic="OCR error: "+e.getClass().getSimpleName();state.flag(scanDiagnostic);event("recognition_error",e.toString());}
            }
            renderCaption();
            if(recording&&recorder!=null){try{recorder.add(latest,caption,SystemClock.elapsedRealtime());}catch(Exception e){recordError="Recording failed: "+e.getMessage();stopRecording();toast(recordError);}}
        }catch(Exception e){state.flag("Frame processing failed");event("frame_error",e.toString());}
        finally{if(image!=null)image.close();if(frame!=null)frame.recycle();}
    }
    private void accept(Vision.Sample sample){
        scanDiagnostic=sample.diagnostic;
        if(!sample.warning.isEmpty())state.flag(sample.warning);
        if(sample.obscured){adviceSafe=false;lastValidHand=0;event("obscured",sample.warning);return;}
        boolean wasPlaying=state.inRound;
        // Round/empty-table detection needs evidence from THIS frame (freshRanks), not the persistent view used for advice.
        boolean cleared=state.scene(sample.status,sample.freshRanks,sample.groups,SystemClock.elapsedRealtime());
        boolean boundary=cleared||(wasPlaying&&!state.inRound)||state.shoe!=oldShoe;
        if(boundary){vision.resetOwnership();event("table_clear","Confirmed empty table; cleared visual identities, retained shoe count");}
        if(cleared||state.shoe!=oldShoe)dealerUp=null;
        if(state.round!=oldRound||state.shoe!=oldShoe){splitHand=false;handIndex=0;adviceHand.clear();adviceSafe=false;mismatch=0;event("transition",state.phase);oldRound=state.round;oldShoe=state.shoe;}
        
        if(dealerUp==null&&sample.ranks.get(0).size()==1)dealerUp=sample.ranks.get(0).get(0);
        if(!boundary)for(int i=0;i<8;i++){List<String> added=state.observeConfirmed(i,sample.trackedRanks.get(i));if(!added.isEmpty())event("cards",i+":"+added);}
        markerStreak=sample.marker?markerStreak+1:0;if(markerStreak==3){state.cutSeen=true;event("marker_candidate","Barcode-like texture; not sufficient to reset");}
        seatHands=sample.selectedHands;
        if(handIndex>=seatHands.size())handIndex=0;
        adviceHand=seatHands.isEmpty()?new ArrayList<>():new ArrayList<>(seatHands.get(handIndex));
        splitHand=seatHands.size()>1;
        adviceSafe=!boundary&&sample.selectedSafe&&adviceHand.size()>=2&&dealerUp!=null&&state.inRound;
        lastValidHand=adviceSafe?SystemClock.elapsedRealtime():0;
        event("sample",sample.diagnostic+" | components="+sample.componentLog+" | tokens="+sample.tokenLog+" | rejected="+sample.rejectedRank+","+sample.rejectedAngle+","+sample.rejectedFace+","+sample.rejectedPosition+" | tracks="+sample.trackLog+" | hands="+sample.selectedHands+" | warning="+sample.warning+" | confirmed="+sample.trackedRanks+" | groups="+sample.groups+" duplicates="+sample.duplicates+" | "+sample.status+" | score="+sample.score+" | ranks="+sample.ranks+" | raw="+Arrays.toString(sample.raw));
    }
    private void renderCaption(){
        boolean stale=SystemClock.elapsedRealtime()-lastValidHand>5000;
        boolean useCount=state.qualified()&&experimentallyUseCount;
        Strategy.Advice a=Strategy.advise(adviceHand,dealerUp,state.trueCount(),useCount,splitHand);
        if(!scan.running()||paused||!adviceSafe||stale||!regions.calibrated||!state.inRound||mismatch>=4)a=new Strategy.Advice("WAIT","—",!regions.calibrated?"Calibrate table and seat anchors":!vision.hasReference()?"Calibrate on an empty table and tap Learn empty":!scan.running()||paused?"Stopped — press Start to scan":mismatch>=4?"Correct hand / regions":"Waiting for readable active hand");
        String tc=state.shoeKnown?String.format(Locale.US,"%+.1f",state.trueCount()):"—";
        caption=(scan.running()?"RUNNING":"STOPPED")+" · Seat "+seat+" · "+state.phase+(recording?" · REC":"")+" · EXPERIMENTAL\n"+
            "RC "+String.format(Locale.US,"%+d",state.running)+" | TC "+tc+" | seen "+state.observed+"\n"+
            "Hand "+(handIndex+1)+" "+adviceHand+" vs "+(dealerUp==null?"?":dealerUp)+" → "+a.action+"\n"+
            "Fallback: "+a.fallback+" | "+state.quality()+"\n"+a.basis+"\n"+state.reason+"\n"+scanDiagnostic;
        final String out=caption;main.post(()->{if(detail!=null){String[] lines=out.split("\n");detail.setText(!scan.running()?"Scanning stopped.\n"+scanDiagnostic:controls.getVisibility()==View.GONE&&lines.length>=4?lines[1]+"\n"+lines[2]+"\n"+lines[lines.length-1]:out);updateRunUi();title.setText("♠ Blackjack Project v.01 · Seat "+seat+" · "+(recording?"REC":"Test")+" · drag / tap");}});
    }
    private void event(String type,String msg){if(log==null)return;try{JSONObject o=new JSONObject();o.put("time_ms",System.currentTimeMillis());o.put("event",type);o.put("app_version","v.01");o.put("scanner_running",scan.running());o.put("detail",msg);o.put("running",state.running);o.put("observed",state.observed);o.put("shoe",state.shoe);o.put("round",state.round);o.put("seat",seat);o.put("quality",state.quality());o.put("caption",caption);log.println(o);log.flush();}catch(Exception ignored){}}
    private void showPanel(){
        panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(12,8,12,8);panel.setBackgroundColor(0xF0182530);
        title=new TextView(this);title.setText("♠ Observation · drag here · tap to collapse");title.setTextSize(12);title.setTextColor(0xff78ebbb);panel.addView(title);
        LinearLayout runRow=new LinearLayout(this);panel.addView(runRow);runLight=new TextView(this);runLight.setTextSize(15);runRow.addView(runLight,new LinearLayout.LayoutParams(0,dp(42),1));runButton=new Button(this);runButton.setTextSize(12);runRow.addView(runButton,new LinearLayout.LayoutParams(dp(90),dp(42)));runButton.setOnClickListener(v->toggleScan());updateRunUi();
        detail=new TextView(this);detail.setTextColor(Color.WHITE);detail.setTextSize(12);detail.setText("Open table, then Calibrate");panel.addView(detail);
        controls=new LinearLayout(this);controls.setOrientation(LinearLayout.VERTICAL);panel.addView(controls);
        LinearLayout r1=row();button(r1,"Seat",this::chooseSeat);button(r1,"Hand",this::chooseHand);button(r1,"Calibrate",this::calibrate);
        LinearLayout r2=row();button(r2,"Record",()->worker.post(()->{if(recording)stopRecording();else startRecording();renderCaption();}));button(r2,"Review",this::review);button(r2,"Close",()->{scan.stop();paused=true;updateRunUi();stopSelf();});
        lp=new WindowManager.LayoutParams(dp(300),WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);lp.gravity=Gravity.TOP|Gravity.LEFT;lp.x=12;lp.y=(int)(getResources().getDisplayMetrics().heightPixels*.62f);
        wm.addView(panel,lp);
        panel.getViewTreeObserver().addOnGlobalLayoutListener(this::updateOverlayBounds);
        title.setOnTouchListener(new View.OnTouchListener(){float x,y;int bx,by;boolean moved;public boolean onTouch(View v,MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN){x=e.getRawX();y=e.getRawY();bx=lp.x;by=lp.y;moved=false;return true;}if(e.getAction()==MotionEvent.ACTION_MOVE){float dx=e.getRawX()-x,dy=e.getRawY()-y;if(Math.abs(dx)+Math.abs(dy)>10)moved=true;lp.x=Math.max(0,bx+(int)dx);lp.y=Math.max(0,by+(int)dy);wm.updateViewLayout(panel,lp);panel.post(CaptureService.this::updateOverlayBounds);return true;}if(e.getAction()==MotionEvent.ACTION_UP){if(!moved){boolean expand=controls.getVisibility()!=View.VISIBLE;controls.setVisibility(expand?View.VISIBLE:View.GONE);lp.width=dp(expand?300:220);wm.updateViewLayout(panel,lp);worker.post(()->renderCaption());}return true;}return false;}});
    }
    private void updateOverlayBounds(){
        if(panel==null||panel.getVisibility()!=View.VISIBLE){overlayBounds=null;return;}
        int[] xy=new int[2];panel.getLocationOnScreen(xy);android.util.DisplayMetrics m=new android.util.DisplayMetrics();wm.getDefaultDisplay().getRealMetrics(m);
        float scale=width/(float)m.widthPixels;overlayBounds=new RectF(xy[0]*scale,xy[1]*scale,(xy[0]+panel.getWidth())*scale,(xy[1]+panel.getHeight())*scale);
    }
    private LinearLayout row(){LinearLayout r=new LinearLayout(this);controls.addView(r);return r;}
    private void button(LinearLayout row,String text,Runnable task){Button b=new Button(this);b.setText(text);b.setTextSize(10);b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(3,0,3,0);row.addView(b,new LinearLayout.LayoutParams(0,dp(38),1));b.setOnClickListener(v->task.run());}
    private void updateRunUi(){if(runLight==null)return;boolean on=scan.running()&&!paused&&!closed;runLight.setText(on?"● RUNNING":"● STOPPED");runLight.setTextColor(on?0xff43e06b:0xffff6262);runButton.setText(on?"Stop":"Start");}
    private void toggleScan(){
        if(scan.running()){
            scan.stop();paused=true;updateRunUi();detail.setText("Scanning stopped. Press Start to continue.");
            worker.post(()->{lastValidHand=0;state.interrupted();if(recording)stopRecording();event("scanner_stop","User stopped scanner");renderCaption();});
        }else{
            if(!regions.calibrated){toast("Calibrate and save the table region first");return;}
            final int requestedGeneration=scan.token();
            worker.post(()->{
                synchronized(scan){
                    if(closed||scan.token()!=requestedGeneration)return;
                    if(!vision.hasReference()){
                        scanDiagnostic="Open Calibrate on an empty table, tap Learn empty, then Save and Start";
                        toast(scanDiagnostic);renderCaption();return;
                    }
                    scan.start();paused=false;lastValidHand=0;lastAnalysis=0;
                    state.interrupted();scanDiagnostic="Scanning for visible cards";
                    event("scanner_start","Continuous scanning requested");renderCaption();
                }
            });
        }
    }
    private void chooseSeat(){
        String[] names={"1 · far left","2 · second from left","3 · third from left","4 · middle","5 · fifth from left","6 · sixth from left","7 · far right"};
        show(dialog().setTitle("Seats 1–7 · left to right").setSingleChoiceItems(names,seat-1,(d,n)->{
            seat=n+1;getSharedPreferences("config",0).edit().putInt("seat",seat).apply();
            worker.post(()->{mismatch=0;handIndex=0;adviceSafe=false;adviceHand.clear();seatHands.clear();lastValidHand=0;event("seat","Selected "+seat+" left to right");renderCaption();});d.dismiss();
        }).setNegativeButton("Cancel",null).create());
    }
    private void chooseHand(){worker.post(()->{
        if(seatHands.size()<2){toast("One or no separated hand detected for this seat");return;}
        handIndex=(handIndex+1)%seatHands.size();adviceSafe=false;lastValidHand=0;
        adviceHand=new ArrayList<>(seatHands.get(handIndex));event("selected_hand","Seat "+seat+" hand "+(handIndex+1));renderCaption();
    });}
    private void calibrate(){scan.stop();paused=true;updateRunUi();worker.post(()->{
        if(latest==null){toast("Wait for a captured frame");return;}paused=true;state.interrupted();if(recording)stopRecording();calibrationBitmap=latest.copy(Bitmap.Config.ARGB_8888,false);
        main.post(()->{panel.setVisibility(View.GONE);calibration=new CalibrationView(this,calibrationBitmap,regions,(config,reply)->{
            Bitmap frame=calibrationBitmap.copy(Bitmap.Config.ARGB_8888,false);
            worker.post(()->{Vision preview=new Vision();preview.useReferenceFrom(vision);try{Vision.Sample result=preview.read(frame,config,seat);main.post(()->reply.accept(result));}catch(Exception e){Vision.Sample result=new Vision.Sample();result.error=e.toString();main.post(()->reply.accept(result));}finally{preview.close();frame.recycle();}});
        },()->{
            Bitmap empty=calibrationBitmap.copy(Bitmap.Config.ARGB_8888,false);
            worker.post(()->{try{vision.learnEmpty(empty);adviceSafe=false;lastValidHand=0;state.clearHands();state.interrupted();scanDiagnostic="Empty reference learned · Save, then Start";event("empty_reference","User selected frozen empty table; reference is session-only");toast("Empty reference learned");}finally{empty.recycle();}});
        },()->{
            wm.removeView(calibration);calibration=null;calibrationBitmap.recycle();calibrationBitmap=null;panel.setVisibility(View.VISIBLE);
            worker.post(()->{paused=true;lastValidHand=0;dealerUp=null;vision.resetOwnership();state.flag("Calibration changed · verify tracking");event("calibration","table="+regions.table+" anchors="+Arrays.deepToString(regions.anchors)+" game="+regions.game+" boxes="+Arrays.toString(regions.boxes)+" angles="+Arrays.toString(regions.handAngles)+" cardSlots="+(regions.cards==null?"legacy":Arrays.deepToString(regions.cards.slots)));renderCaption();});
        });WindowManager.LayoutParams p=new WindowManager.LayoutParams(-1,-1,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.OPAQUE);wm.addView(calibration,p);});
    });}
    private AlertDialog.Builder dialog(){return new AlertDialog.Builder(new ContextThemeWrapper(this,android.R.style.Theme_Material_Dialog_Alert));}
    private void show(AlertDialog d){d.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);d.show();}
    private void review(){
        String[] items={"Correct selected hand","Set burned-card quantity","Confirm NEW shoe (clears count)","Enable/disable experimental count advice","Cycle detected split hand","Confirm reviewed tracking","Manual next hand (old cards cleared)"};
        AlertDialog d=dialog().setTitle("Review · seat "+seat).setItems(items,(di,n)->{
            if(n==0)edit("Ranks separated by spaces",false);
            if(n==1)edit("Burned cards (1–10)",true);
            if(n==2)show(dialog().setTitle("Confirm new shoe?").setMessage("Only confirm after replacement, before dealing. Previous count is cleared.").setPositiveButton("New shoe",(a,b)->worker.post(()->{state.startShoe();vision.resetOwnership();dealerUp=null;event("manual_shoe","User confirmed");renderCaption();})).setNegativeButton("Cancel",null).create());
            if(n==3)worker.post(()->{experimentallyUseCount=!experimentallyUseCount;toast(experimentallyUseCount?"Count advice enabled when shoe and burns are qualified":"Basic strategy only");renderCaption();});
            if(n==4)chooseHand();
            if(n==5)show(dialog().setTitle("Tracking reviewed?").setMessage("Only confirm after checking all counted cards since this shoe began. This cannot recover missed cards.").setPositiveButton("Confirm",(a,b)->worker.post(()->{state.uncertain=false;state.reason="User reviewed; recognition remains unvalidated";event("review","User cleared uncertainty flag");renderCaption();})).setNegativeButton("Cancel",null).create());
            if(n==6)worker.post(()->{state.clearHands();vision.resetOwnership();state.inRound=true;state.round++;dealerUp=null;state.flag("Manual hand boundary");renderCaption();});
        }).setNegativeButton("Close",null).create();show(d);
    }
    private void edit(String hint,boolean burn){
        EditText input=new EditText(new ContextThemeWrapper(this,android.R.style.Theme_Material_Dialog_Alert));input.setHint(hint);
        AlertDialog d=dialog().setTitle(hint).setView(input).setPositiveButton("Apply",(a,b)->{
            String text=input.getText().toString().trim().toUpperCase(Locale.ROOT);
            if(burn){try{int n=Integer.parseInt(text);if(n<1||n>10)throw new Exception();worker.post(()->{state.setBurns(n);event("burn_quantity","User confirmed "+n);renderCaption();});}catch(Exception e){toast("Enter 1–10");}}
            else{List<String> cards=new ArrayList<>();for(String token:text.split("[ ,]+")){String r=CountEngine.parseRank(token);if(r==null){toast("Use A, 2–10, J, Q, K");return;}cards.add(r);}if(cards.size()>12){toast("Too many cards");return;}worker.post(()->{state.correctHand(seat,cards);mismatch=0;event("correction",cards.toString());renderCaption();});}
        }).setNegativeButton("Cancel",null).create();show(d);
    }
    private void startRecording(){try{recorder=new ClipRecorder(clipFile,width,height);recording=true;getSharedPreferences("config",0).edit().putString("lastClip",clipFile.toString()).apply();event("record_start",clipFile.getName());toast("Recording started. Use Record again to finish.");}catch(Exception e){recording=false;toast("Recording unavailable: "+e.getMessage());}}
    private void stopRecording(){if(recorder!=null){recorder.close();recorder=null;}if(recording){recording=false;event("record_stop","Saved locally");toast("Recording finished. Open app to export.");}clipFile=new File(clipFile.getParentFile(),System.currentTimeMillis()+".mp4");}
    private void toast(String s){main.post(()->Toast.makeText(this,s,Toast.LENGTH_LONG).show());}
    private int dp(int n){return (int)(getResources().getDisplayMetrics().density*n+.5f);}
    @Override public void onConfigurationChanged(Configuration c){super.onConfigurationChanged(c);toast("Orientation changed: restart capture and recalibrate");stopSelf();}
    @Override public void onDestroy(){
        closed=true;scan.stop();paused=true;
        if(panel!=null){wm.removeView(panel);panel=null;}if(calibration!=null){wm.removeView(calibration);calibration=null;}
        if(display!=null)display.release();if(projection!=null)projection.stop();
        worker.post(()->{if(reader!=null)reader.close();if(recorder!=null)stopRecording();if(latest!=null)latest.recycle();vision.close();event("stop","Capture stopped");if(log!=null)log.close();active=false;thread.quitSafely();});
        stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();
    }
}
