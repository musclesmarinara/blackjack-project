package com.example.blackjackoverlay;
import android.graphics.*;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.android.gms.tasks.*;
import java.util.*;

/** One table ROI; OCR only on connected light-face candidates, never seat windows. */
public final class Vision {
 int timeoutSeconds=3; // Extended only by the software-emulator replay runner.
 private int[] reference;private int referenceW,referenceH;
 public void learnEmpty(Bitmap source){
  float scale=Math.min(1,720f/source.getWidth());referenceW=Math.round(source.getWidth()*scale);referenceH=Math.round(source.getHeight()*scale);
  Bitmap b=Bitmap.createScaledBitmap(source,referenceW,referenceH,true);reference=new int[referenceW*referenceH];b.getPixels(reference,0,referenceW,0,0,referenceW,referenceH);if(b!=source)b.recycle();resetOwnership();
 }
 public void useReferenceFrom(Vision other){reference=other.reference;referenceW=other.referenceW;referenceH=other.referenceH;}
 /** False until Learn empty has been tapped on an empty table this session; read() does nothing until then. */
 public boolean hasReference(){return reference!=null;}
 private final TableTracker tracker=new TableTracker();
 private final CornerSelector cornerSelector=new CornerSelector();
 private static final class OrientedRead {
  final TableTracker.Detection detection;final int orientation;
  OrientedRead(TableTracker.Detection d,int o){detection=d;orientation=o;}
 }
 private final TextRecognizer ocr=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
 public void resetOwnership(){tracker.clear();cornerSelector.clear();}
 public static final class Sample {
  public List<List<String>> candidates=new ArrayList<>(),trackedRanks=new ArrayList<>(),ranks=new ArrayList<>(),freshRanks=new ArrayList<>(),selectedHands=new ArrayList<>();
  public String diagnostic="",componentLog="",tokenLog="";public int tokens=0,rejectedRank=0,rejectedAngle=0,rejectedFace=0,rejectedPosition=0;public List<RectF> boxes=new ArrayList<>();
  public String status="",score="",error="",warning="",trackLog="";public boolean marker=false,obscured=false,selectedSafe=false;
  public int groups=0,duplicates=0;public String[] raw=new String[9];public String[][] slotText=new String[7][5];
  Sample(){for(int i=0;i<8;i++){candidates.add(new ArrayList<>());ranks.add(new ArrayList<>());freshRanks.add(new ArrayList<>());trackedRanks.add(new ArrayList<>());}}
 }
 public Sample read(Bitmap source,Regions regions,int seat)throws Exception{return read(source,regions,seat,null);}
 public Sample read(Bitmap source,Regions r,int seat,RectF overlay)throws Exception{
  Sample s=new Sample();RectF table=new RectF(r.table.left*source.getWidth(),r.table.top*source.getHeight(),r.table.right*source.getWidth(),r.table.bottom*source.getHeight());
  if(overlay!=null&&RectF.intersects(table,overlay)){s.obscured=true;s.warning="Move overlay outside the table region";s.diagnostic=s.warning;return s;}
  float scale=Math.min(1,720f/source.getWidth());int w=Math.round(source.getWidth()*scale),h=Math.round(source.getHeight()*scale);
  if(reference==null||referenceW!=w||referenceH!=h){s.obscured=true;s.warning="Calibrate on an empty table, then tap Learn empty";s.diagnostic=s.warning;return s;}
  Bitmap small=Bitmap.createScaledBitmap(source,w,h,true);int[] pixels=new int[w*h];small.getPixels(pixels,0,w,0,0,w,h);if(small!=source)small.recycle();
  float[][] anchors=new float[8][2];for(int i=0;i<8;i++){anchors[i][0]=r.anchors[i][0]*w;anchors[i][1]=r.anchors[i][1]*h;}
  TableComponents components=new TableComponents(pixels,w,h,r.tableCorners(w,h),anchors,reference);
  s.componentLog="regions="+components.groups+" ambiguous="+components.ambiguous+" small="+components.rejectedSmall+" unresolved="+components.unresolvedFaces;
  // Stable left-to-right component ordering for split-hand selection in this frame.
  components.groups.sort(Comparator.comparingDouble(g->g.cx()));
  List<OrientedRead> readings=new ArrayList<>();List<TableTracker.Detection> detections=new ArrayList<>();int groupId=0;Set<Integer> unreadableSeats=new HashSet<>();
  // Phase 1: build every region's crop and DISPATCH its OCR call without waiting.
  // ML Kit's TextRecognizer client supports concurrent in-flight process() calls
  // against one shared client -- each call returns independently, backed by the
  // client's own executor. Doing this sequentially (crop, await, crop, await...)
  // was the main driver of real-world cycle time on a busy multi-hand table: 9
  // regions meant 9 full round-trips end to end instead of 9 running at once.
  final class Pending{int id;TableComponents.Group g;Bitmap crop;Matrix inverse;Task<Text> task;}
  List<Pending> pending=new ArrayList<>();
  for(TableComponents.Group g:components.groups){
   int id=++groupId;s.boxes.add(new RectF(g.left/(float)w,g.top/(float)h,(g.right+1f)/w,(g.bottom+1f)/h));
   Rect box=new Rect(Math.max(0,(int)(g.left/scale)-4),Math.max(0,(int)(g.top/scale)-4),Math.min(source.getWidth(),(int)((g.right+1)/scale)+4),Math.min(source.getHeight(),(int)((g.bottom+1)/scale)+4));
   float stretch=2.0f;
   // Undo table foreshortening before subtracting the calibrated card tilt.
   float angle=(float)Math.toDegrees(Math.atan2(Math.sin(Math.toRadians(r.handAngles[g.owner]))*stretch,Math.cos(Math.toRadians(r.handAngles[g.owner]))));
   Matrix transform=new Matrix();transform.setScale(1,stretch);transform.postRotate(-angle);
   RectF rotated=new RectF(box);transform.mapRect(rotated);
   transform.postTranslate(-rotated.left,-rotated.top);float zoom=Math.min(6,960f/Math.max(rotated.width(),rotated.height()));transform.postScale(zoom,zoom);
   Bitmap crop=Bitmap.createBitmap(Math.max(8,(int)Math.ceil(rotated.width()*zoom)),Math.max(8,(int)Math.ceil(rotated.height()*zoom)),Bitmap.Config.ARGB_8888);
   Canvas canvas=new Canvas(crop);canvas.drawColor(Color.BLACK);canvas.save();canvas.concat(transform);canvas.clipRect(box);canvas.drawBitmap(source,0,0,new Paint(3));canvas.restore();
   Matrix inverse=new Matrix();transform.invert(inverse);
   Pending p=new Pending();p.id=id;p.g=g;p.crop=crop;p.inverse=inverse;p.task=ocr.process(InputImage.fromBitmap(crop,0));
   pending.add(p);
  }
  // Phase 2: await each dispatched call (already running) and process its result.
  for(Pending p:pending){
   int before=readings.size();
   try{
    Text text=Tasks.await(p.task,timeoutSeconds,java.util.concurrent.TimeUnit.SECONDS);
    s.raw[p.g.owner]=(s.raw[p.g.owner]==null?"":s.raw[p.g.owner]+" | ")+text.getText();
    for(Text.TextBlock block:text.getTextBlocks())for(Text.Line line:block.getLines())for(Text.Element e:line.getElements()){
     s.tokens++;s.tokenLog+="seat="+p.g.owner+" text="+e.getText()+" angle="+e.getAngle()+" confidence="+e.getConfidence()+" bounds="+e.getBoundingBox()+";";
     if(CountEngine.parseRank(e.getText())!=null){
      addRank(s,readings,e.getText(),e.getBoundingBox(),e.getAngle(),e.getConfidence(),p.crop,p.inverse,components,p.g,scale,p.id);
     }else if(e.getText()!=null&&e.getText().trim().length()<=2){
      // Short unmatched text (a rank glyph glued to a misread suit icon, e.g. "7♣")
      // is still worth trying symbol-by-symbol. 3+ real characters that don't form
      // one exact rank are almost always two distinct cards whose corners merged
      // into one OCR line on a crowded table (e.g. "6A4") -- splitting THAT into
      // separate symbols manufactures phantom cards rather than reading a real one,
      // so we leave it alone instead of guessing which characters are real.
      for(Text.Symbol symbol:e.getSymbols())addRank(s,readings,symbol.getText(),symbol.getBoundingBox(),symbol.getAngle(),symbol.getConfidence(),p.crop,p.inverse,components,p.g,scale,p.id);
     }
    }
   }finally{p.crop.recycle();}
   if(readings.size()==before)unreadableSeats.add(p.g.owner);
  }
  // Hold one printed-corner orientation per seat until a confirmed table clear.
  // This is an orientation filter, not proof of physical-card identity.
  for(int owner=0;owner<8;owner++){
   int up=0,down=0;for(OrientedRead read:readings)if(read.detection.seat==owner){if(read.orientation==1)up++;else down++;}
   int mode=cornerSelector.choose(owner,up,down),kept=0;
   for(OrientedRead read:readings)if(read.detection.seat==owner&&read.orientation==mode){detections.add(read.detection);kept++;}
   if(up+down>0&&kept==0)unreadableSeats.add(owner);
  }
  s.tokenLog+=" cornerModes="+cornerSelector;
  for(TableTracker.Detection d:detections)s.candidates.get(d.seat).add(d.rank);
  TableTracker.Result result=tracker.observe(detections,w);s.trackedRanks=result.counted;s.ranks=result.visible;s.freshRanks=result.fresh;s.trackLog=result.tracks;
  s.groups=components.ambiguous>0||components.unresolvedFaces?Math.max(1,components.groups.size()):components.groups.size();
  for(Map.Entry<Integer,List<String>> e:result.hands.entrySet())if(result.owners.get(e.getKey())==seat)s.selectedHands.add(new ArrayList<>(e.getValue()));
  s.selectedSafe=!result.unsafeSeats.contains(seat)&&!result.unsafeSeats.contains(0)&&components.ambiguous==0&&!components.unresolvedFaces&&!unreadableSeats.contains(seat)&&!unreadableSeats.contains(0);
  if(result.uncertain)s.warning=result.reason;
  // Was: fired if ANY of up to 7 other seats had a noisy/unreadable read this
  // cycle, regardless of whether that affected your seat's own advice. On a
  // busy multi-hand table that's nearly always true somewhere -- measured at
  // 86% of samples in one real session -- so it drowned out the status line
  // almost every cycle even when your own hand was perfectly clean. Scoped to
  // match s.selectedSafe above: only warn when it's actually the dealer or
  // your selected seat that's ambiguous/unreadable.
  if(components.ambiguous>0||components.unresolvedFaces||unreadableSeats.contains(seat)||unreadableSeats.contains(0))s.warning="Unresolved card regions: review required; advice withheld";
  Rect status=r.pixels(8,source.getWidth(),source.getHeight());
  if(status.width()>0&&status.height()>0&&(overlay==null||!RectF.intersects(new RectF(status),overlay))){
   Bitmap b=r.boxCrop(source,8);try{s.status=Tasks.await(ocr.process(InputImage.fromBitmap(b,0)),timeoutSeconds,java.util.concurrent.TimeUnit.SECONDS).getText();s.raw[8]=s.status;}finally{b.recycle();}
  }
  int visible=0;for(List<String> hand:s.ranks)visible+=hand.size();
  s.diagnostic="Regions "+components.groups.size()+" · OCR "+s.tokens+" · ranks "+detections.size()+" · stable "+visible;
  if(components.groups.isEmpty())s.diagnostic+=" · No card regions";
  else if(detections.isEmpty())s.diagnostic+=" · Ranks unreadable";
  else if(s.ranks.get(0).isEmpty())s.diagnostic+=" · Waiting for dealer rank";
  return s;
 }
 private void addRank(Sample s,List<OrientedRead> readings,String raw,Rect bounds,float angle,float confidence,Bitmap crop,Matrix inverse,TableComponents components,TableComponents.Group g,float scale,int id){
  String rank=CountEngine.parseRank(raw);
  // NOTE: with the bundled com.google.mlkit:text-recognition:16.0.1 artifact,
  // Text.Element/Symbol#getConfidence() is not populated (reports 0) — it is not
  // a usable signal here, and gating acceptance on it rejected every legitimate
  // read. parseRank() above already requires an exact rank-string match, and the
  // orientation/cardFace/position checks below still filter out noise. confidence
  // is kept in the method signature and tokenLog purely for diagnostic visibility.
  if(rank==null||bounds==null){s.rejectedRank++;return;}
  int orientation=RankFilter.orientation(angle);if(orientation==0){s.rejectedAngle++;return;}
  if(!cardFace(crop,bounds)){s.rejectedFace++;return;}
  float[] p={bounds.exactCenterX(),bounds.exactCenterY()};inverse.mapPoints(p);
  if(!components.belongs(g,p[0]*scale,p[1]*scale)){s.rejectedPosition++;return;}
  readings.add(new OrientedRead(new TableTracker.Detection(rank,p[0]*scale,p[1]*scale,g.owner,id),orientation));
 }
 private boolean cardFace(Bitmap b,Rect r){int l=Math.max(0,r.left-5),t=Math.max(0,r.top-5),right=Math.min(b.getWidth(),r.right+7),bottom=Math.min(b.getHeight(),r.bottom+5),white=0,n=0;
  for(int y=t;y<bottom;y+=2)for(int x=l;x<right;x+=2){int c=b.getPixel(x,y);if(Color.red(c)>165&&Color.green(c)>165&&Color.blue(c)>165)white++;n++;}return n>0&&white>n*.30;
 }
 public void close(){ocr.close();}
}
