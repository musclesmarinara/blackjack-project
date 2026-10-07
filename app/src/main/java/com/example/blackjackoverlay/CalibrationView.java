package com.example.blackjackoverlay;
import android.content.*;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.util.function.Consumer;

/** Calibrate against a frozen frame; preview never changes the count. */
public final class CalibrationView extends LinearLayout {
    public interface Tester {void read(Regions config,Consumer<Vision.Sample> reply);}
    private final Bitmap picture;private final Regions regions;private final Tester tester;
    private final TextView heading,readout;private final ImageView crop;private final Board board;
    private final Button test;private Bitmap cropBitmap;private int selected=-1,revision=0;private boolean closed=false;
    private Vision.Sample result;private boolean move=true,zoom=false;
    public CalibrationView(Context c,Bitmap b,Regions r,Tester t,Runnable learnEmpty,Runnable finish){
        super(c);picture=b;regions=r;tester=t;setOrientation(VERTICAL);setPadding(8,8,8,8);setBackgroundColor(0xff0a121a);
        heading=text(16);addView(heading);
        LinearLayout row=row();button(row,"Portrait preset",()->{regions.stakePortrait();changed();});
        test=button(row,"Test frame",this::testFrame);
        button(row,"Learn empty",()->{learnEmpty.run();learned();});
        LinearLayout nav=row();button(nav,"Previous",()->{selected=Math.max(-1,selected-1);refresh();});button(nav,"Next",()->{selected=Math.min(8,selected+1);refresh();});
        button(nav,"−5°",()->rotate(-5));button(nav,"+5°",()->rotate(5));
        LinearLayout tools=row();
        Button mode=button(tools,"Move",()->{});mode.setOnClickListener(v->{move=!move;mode.setText(move?"Move":"Draw box");});button(tools,"Zoom",()->{zoom=!zoom;refresh();});
        LinearLayout sizes=row();button(sizes,"Width −",()->size(.9f,1));button(sizes,"Width +",()->size(1.1f,1));button(sizes,"Height −",()->size(1,.9f));button(sizes,"Height +",()->size(1,1.1f));
        board=new Board(c);addView(board,new LayoutParams(-1,0,1));
        crop=new ImageView(c);crop.setScaleType(ImageView.ScaleType.FIT_CENTER);crop.setBackgroundColor(0xff263746);addView(crop,new LayoutParams(-1,dp(72)));
        readout=text(13);readout.setMaxLines(5);addView(readout,new LayoutParams(-1,dp(96)));
        LinearLayout end=row();button(end,"Save calibration",()->{String error=regions.validationError();if(!error.isEmpty()){Toast.makeText(c,error,Toast.LENGTH_LONG).show();return;}regions.tableV7=true;regions.calibrated=true;regions.fullHands=true;regions.cards=null;regions.save(c);finish.run();});refresh();
    }
    private void learned(){changed();readout.setText("Empty reference requested. Use only with NO cards or hands on the table. Save, then Start.");}
    private int dp(int n){return (int)(getResources().getDisplayMetrics().density*n+.5f);}
    private TextView text(int size){TextView v=new TextView(getContext());v.setTextColor(Color.WHITE);v.setTextSize(size);return v;}
    private LinearLayout row(){LinearLayout r=new LinearLayout(getContext());addView(r);return r;}
    private Button button(LinearLayout row,String label,Runnable task){Button b=new Button(getContext());b.setText(label);b.setTextSize(11);b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(2,0,2,0);row.addView(b,new LayoutParams(0,dp(42),1));b.setOnClickListener(v->task.run());return b;}
    private String name(){return selected<0?"Table detection region":selected==0?"Dealer anchor":selected<=7?"Seat "+selected+" anchor · left to right":"Round status";}
    private RectF target(){if(selected<0)return new RectF(regions.table);if(selected==8)return new RectF(regions.boxes[8]);float[] a=regions.anchors[selected];return new RectF(a[0]-.008f,a[1]-.008f,a[0]+.008f,a[1]+.008f);}
    private void setTarget(RectF b){if(b.width()<.006f||b.height()<.004f||b.left<0||b.top<0||b.right>1||b.bottom>1)return;if(selected<0)regions.table=b;else if(selected==8)regions.boxes[8]=b;else{regions.anchors[selected][0]=b.centerX();regions.anchors[selected][1]=b.centerY();}}
    private void size(float x,float y){if(selected>=0&&selected<8)return;RectF b=target();float w=b.width()*x/2,h=b.height()*y/2;setTarget(new RectF(b.centerX()-w,b.centerY()-h,b.centerX()+w,b.centerY()+h));changed();}
    private void rotate(int degrees){if(selected>=0&&selected<8){regions.handAngles[selected]=(regions.handAngles[selected]+degrees+540)%360-180;changed();}}
    private void changed(){revision++;result=null;refresh();}
    private void testFrame(){
        final int version=revision;test.setEnabled(false);readout.setText("Reading frozen frame…");
        tester.read(regions.copy(),sample->{if(closed)return;test.setEnabled(true);if(version!=revision){readout.setText("Regions changed. Tap Test frame again.");return;}result=sample;refresh();});
    }
    private void refresh(){
        heading.setText(name()+(selected>=0&&selected<8?" · "+(int)regions.handAngles[Math.min(selected,7)]+"°":"")+"\nOne table box · move anchors to first-card positions");
        board.invalidate();crop.setImageDrawable(null);if(cropBitmap!=null){cropBitmap.recycle();cropBitmap=null;}
        cropBitmap=selected==8?regions.boxCrop(picture,8):regions.tableCrop(picture);crop.setImageBitmap(cropBitmap);
        String msg="Tap Test frame. Preview shows the exact crop.\nTable box: include dealer + all hands, exclude digital cards and chips. Move anchors; ±5° sets card tilt (clockwise positive).";
        if(result!=null){
            if(!result.error.isEmpty())msg="Read failed: "+result.error;
            else if(selected>=0&&selected<9){String raw=result.raw[selected];msg="Read: "+(raw==null||raw.isEmpty()?"(nothing)":raw.replace('\n',' '));if(selected<8)msg+="\nRanks: "+result.candidates.get(selected);String status=result.status.toLowerCase(java.util.Locale.ROOT);boolean matched=status.contains("round in progress")||status.contains("place your")||status.contains("take a seat")||status.contains("play behind")||(status.contains("burn")&&status.contains("procedure"));msg+="\nStatus: "+(matched?result.status.replace('\n',' '):"Hidden / unreadable — card-start detection available");}
            else msg="Candidate ranks: "+result.candidates+"\n"+result.warning;
        }if(result!=null)msg=result.diagnostic+"\n"+msg;readout.setText(msg);
    }
    @Override protected void onDetachedFromWindow(){closed=true;crop.setImageDrawable(null);if(cropBitmap!=null){cropBitmap.recycle();cropBitmap=null;}super.onDetachedFromWindow();}
    private final class Board extends View {
        private final Paint p=new Paint(3);private final RectF imageRect=new RectF();private float sx,sy;private boolean dragging;private RectF initial;
        Board(Context c){super(c);}
        private RectF absolute(RectF b){RectF g=regions.game;return new RectF(g.left+b.left*g.width(),g.top+b.top*g.height(),g.left+b.right*g.width(),g.top+b.bottom*g.height());}
        private RectF map(RectF b){return new RectF(imageRect.left+b.left*imageRect.width(),imageRect.top+b.top*imageRect.height(),imageRect.left+b.right*imageRect.width(),imageRect.top+b.bottom*imageRect.height());}
        @Override protected void onDraw(Canvas c){
            if(!dragging){float scale=Math.min(getWidth()/(float)picture.getWidth(),getHeight()/(float)picture.getHeight());float w=picture.getWidth()*scale,h=picture.getHeight()*scale;imageRect.set((getWidth()-w)/2,0,(getWidth()+w)/2,h);
            if(zoom){RectF focus=selected==8?absolute(regions.boxes[8]):target();focus.inset(-.035f,-.025f);float z=Math.min(getWidth()/(focus.width()*picture.getWidth()),getHeight()/(focus.height()*picture.getHeight()));w=picture.getWidth()*z;h=picture.getHeight()*z;imageRect.set(getWidth()/2f-focus.centerX()*w,getHeight()/2f-focus.centerY()*h,getWidth()/2f+(1-focus.centerX())*w,getHeight()/2f+(1-focus.centerY())*h);}}
            p.setStyle(Paint.Style.FILL);c.drawBitmap(picture,null,imageRect,p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(selected<0?Color.YELLOW:0xff78ebbb);c.drawRect(map(regions.table),p);
            p.setColor(selected==8?Color.YELLOW:0xff78ebbb);c.drawRect(map(absolute(regions.boxes[8])),p);
            if(result!=null){p.setColor(Color.CYAN);for(RectF box:result.boxes)c.drawRect(map(box),p);}
            for(int i=0;i<8;i++){
                float x=imageRect.left+regions.anchors[i][0]*imageRect.width(),y=imageRect.top+regions.anchors[i][1]*imageRect.height();
                p.setColor(i==selected?Color.YELLOW:0xff78ebbb);p.setStrokeWidth(i==selected?3:1);p.setStyle(Paint.Style.STROKE);c.drawCircle(x,y,dp(7),p);
                c.drawLine(x-dp(10),y,x+dp(10),y,p);c.drawLine(x,y-dp(10),x,y+dp(10),p);
                p.setStyle(Paint.Style.FILL);p.setTextSize(dp(12));c.drawText(i==0?"D":Integer.toString(i),x+dp(9),y,p);
            }
            p.setStyle(Paint.Style.FILL);
        }
        private float nx(float x){return Math.max(0,Math.min(1,(x-imageRect.left)/imageRect.width()));}
        private float ny(float y){return Math.max(0,Math.min(1,(y-imageRect.top)/imageRect.height()));}
        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getAction()==MotionEvent.ACTION_DOWN){if(imageRect.contains(e.getX(),e.getY())){sx=nx(e.getX());sy=ny(e.getY());initial=target();dragging=true;}return true;}
            if(dragging&&(e.getAction()==MotionEvent.ACTION_MOVE||e.getAction()==MotionEvent.ACTION_UP)){
                RectF b;
                if(move||(selected>=0&&selected<8)){b=new RectF(initial);float dx=nx(e.getX())-sx,dy=ny(e.getY())-sy;if(selected==8){dx/=regions.game.width();dy/=regions.game.height();}b.offset(dx,dy);setTarget(b);changed();}
                else{b=new RectF(Math.min(sx,nx(e.getX())),Math.min(sy,ny(e.getY())),Math.max(sx,nx(e.getX())),Math.max(sy,ny(e.getY())));
                    if(selected==8){RectF g=regions.game;if(b.intersect(g))b=new RectF((b.left-g.left)/g.width(),(b.top-g.top)/g.height(),(b.right-g.left)/g.width(),(b.bottom-g.top)/g.height());else return true;}
                    setTarget(b);changed();}
                if(e.getAction()==MotionEvent.ACTION_UP)dragging=false;return true;
            }return true;
        }
    }
}
