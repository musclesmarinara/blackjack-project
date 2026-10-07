package com.example.blackjackoverlay;
import android.content.*;
import android.graphics.*;
import org.json.*;

public final class Regions {
    public RectF game=new RectF(0,0,1,1);
    // Table-wide detector: seats are anchors, not overlapping OCR windows.
    public RectF table=new RectF(.10f,.30f,.90f,.43f);
    public float[][] anchors={{.52f,.345f},{.20f,.388f},{.30f,.400f},{.41f,.412f},{.51f,.416f},{.62f,.412f},{.72f,.400f},{.81f,.382f}};
    public float[] handAngles={0,32,18,8,0,-8,-18,-32};
    public boolean tableV7=true;
    public float[] tableCorners(int w,int h){return new float[]{table.left*w,table.top*h,table.right*w,table.top*h,table.right*w,table.bottom*h,table.left*w,table.bottom*h};}
    public Bitmap tableCrop(Bitmap b){return Bitmap.createBitmap(b,(int)(table.left*b.getWidth()),(int)(table.top*b.getHeight()),Math.max(1,(int)(table.width()*b.getWidth())),Math.max(1,(int)(table.height()*b.getHeight())));}

    // Relative to the calibrated game rectangle. Whole-hand search boxes may overlap.
    public RectF[] boxes=new RectF[10];
    public float[] angles=new float[]{0,32,18,8,0,-8,-18,-32,0,0};
    public CardLayout cards;public boolean fullHands=false;
    public boolean calibrated=false;public boolean portraitPreset=false;
    public Regions(){
        boxes[0]=new RectF(.012f,.14f,.47f,.29f); // dealer digital cards only
        float[] x={.14f,.29f,.42f,.50f,.61f,.72f,.80f};
        float[] y={.55f,.59f,.60f,.61f,.60f,.59f,.55f};
        for(int i=1;i<=7;i++)boxes[i]=new RectF(x[i-1]-.03f,y[i-1]-.09f,x[i-1]+.03f,y[i-1]+.045f);
        boxes[8]=new RectF(.05f,.935f,.96f,1f); // phase text
        boxes[9]=new RectF(.10f,.45f,.94f,.65f); // physical dealer/shoe marker strip, review only
    }
    public String validationError(){
        if(!Float.isFinite(table.left)||!Float.isFinite(table.top)||!Float.isFinite(table.right)||!Float.isFinite(table.bottom)||table.left<0||table.top<0||table.right>1||table.bottom>1||table.width()<.05f||table.height()<.025f)return "Draw a valid table region inside the screen";
        for(int i=0;i<8;i++){
            if(!Float.isFinite(anchors[i][0])||!Float.isFinite(anchors[i][1])||!Float.isFinite(handAngles[i])||!table.contains(anchors[i][0],anchors[i][1]))return "Move every anchor inside the table region";
            if(i>1&&anchors[i][0]<=anchors[i-1][0])return "Seats 1 to 7 must be ordered left to right";
        }return "";
    }
    public Regions copy(){Regions r=new Regions();r.game=new RectF(game);r.table=new RectF(table);r.tableV7=tableV7;for(int i=0;i<8;i++){r.anchors[i]=anchors[i].clone();r.handAngles[i]=handAngles[i];}r.calibrated=calibrated;r.cards=cards==null?null:cards.copy();r.fullHands=fullHands;r.portraitPreset=portraitPreset;for(int i=0;i<10;i++){r.boxes[i]=new RectF(boxes[i]);r.angles[i]=angles[i];}return r;}
    /** Screen-normalized starting points from the user's portrait recording, excluding recorder footer. */
    public void stakePortrait(){
        game=new RectF(0,0,1,1);portraitPreset=true;Regions preset=new Regions();table=preset.table;anchors=preset.anchors;handAngles=preset.handAngles;tableV7=true;
        boxes[0]=new RectF(.017f,.183f,.47f,.2075f);
        cards=new CardLayout();toHandBoxes();
        boxes[8]=new RectF(.22f,.829f,.80f,.876f);
        boxes[9]=new RectF(.10f,.29f,.96f,.37f);
        angles[0]=angles[8]=angles[9]=0;calibrated=false;
    }
    public void updateEnvelope(int h){float[] b=cards.bounds(h);boxes[h+1]=new RectF(b[0],b[1],b[2],b[3]);}
    public float[] corners(int h,int k,int w,int height){float[] q=cards.corners(h,k,w*game.width(),height*game.height());for(int n=0;n<8;n+=2){q[n]+=game.left*w;q[n+1]+=game.top*height;}return q;}
    public Bitmap cardCrop(Bitmap source,int h,int k,int w,int height){Bitmap b=Bitmap.createBitmap(w,height,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.drawColor(Color.BLACK);Matrix m=new Matrix();m.setPolyToPoly(corners(h,k,source.getWidth(),source.getHeight()),0,new float[]{0,0,w,0,w,height,0,height},0,4);c.drawBitmap(source,m,new Paint(3));return b;}
    public void toHandBoxes(){
        if(cards==null)return;
        for(int h=0;h<7;h++){
            float angle=cards.slots[h][0][4];double a=Math.toRadians(angle);float l=Float.MAX_VALUE,t=l,r=-l,b=-l;
            for(int k=0;k<5;k++){float[] slot=cards.slots[h][k];float x=slot[0]*945,y=slot[1]*2048;
                float rx=(float)(x*Math.cos(a)-y*Math.sin(a)),ry=(float)(x*Math.sin(a)+y*Math.cos(a));
                l=Math.min(l,rx-28);r=Math.max(r,rx+55);t=Math.min(t,ry-20);b=Math.max(b,ry+40);
            }
            float cx=(l+r)/2,cy=(t+b)/2;float x=(float)(cx*Math.cos(a)+cy*Math.sin(a)),y=(float)(-cx*Math.sin(a)+cy*Math.cos(a));
            boxes[h+1]=new RectF((x-(r-l)/2)/945,(y-(b-t)/2)/2048,(x+(r-l)/2)/945,(y+(b-t)/2)/2048);angles[h+1]=angle;
        }cards=null;fullHands=true;
    }
    public float[] boxCorners(int i,int w,int h){RectF b=boxes[i];float cx=(game.left+b.centerX()*game.width())*w,cy=(game.top+b.centerY()*game.height())*h;
        float hw=b.width()*game.width()*w/2,hh=b.height()*game.height()*h/2;float[] q={-hw,-hh,hw,-hh,hw,hh,-hw,hh};double a=Math.toRadians(-angles[i]);
        for(int n=0;n<8;n+=2){float x=q[n],y=q[n+1];q[n]=cx+(float)(x*Math.cos(a)-y*Math.sin(a));q[n+1]=cy+(float)(x*Math.sin(a)+y*Math.cos(a));}return q;
    }
    public Bitmap boxCrop(Bitmap source,int i){RectF b=boxes[i];float sw=b.width()*game.width()*source.getWidth(),sh=b.height()*game.height()*source.getHeight();float scale=Math.min(3,960f/Math.max(sw,sh));int w=Math.max(8,(int)(sw*scale)),h=Math.max(8,(int)(sh*scale));
        Bitmap image=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(image);c.drawColor(Color.BLACK);Matrix m=new Matrix();m.setPolyToPoly(boxCorners(i,source.getWidth(),source.getHeight()),0,new float[]{0,0,w,0,w,h,0,h},0,4);c.drawBitmap(source,m,new Paint(3));return image;
    }
    public Rect pixels(int i,int w,int h){RectF r=boxes[i];return new Rect(
        Math.max(0,(int)((game.left+r.left*game.width())*w)),Math.max(0,(int)((game.top+r.top*game.height())*h)),
        Math.min(w,(int)((game.left+r.right*game.width())*w)),Math.min(h,(int)((game.top+r.bottom*game.height())*h)));}
    public void save(Context c){try{JSONObject o=new JSONObject();o.put("tableV7",true);o.put("table",arr(table));JSONArray aa=new JSONArray();for(float[] anchor:anchors)aa.put(new JSONArray(anchor));o.put("anchors",aa);o.put("handAngles",new JSONArray(handAngles));o.put("game",arr(game));JSONArray b=new JSONArray(),a=new JSONArray();for(int i=0;i<10;i++){b.put(arr(boxes[i]));a.put(angles[i]);}o.put("boxes",b);o.put("angles",a);o.put("calibrated",calibrated);o.put("portraitPreset",portraitPreset);o.put("fullHands",fullHands);if(cards!=null){JSONArray cc=new JSONArray();for(int h=0;h<7;h++)for(int k=0;k<5;k++)cc.put(new JSONArray(cards.slots[h][k]));o.put("cardsV3",cc);}c.getSharedPreferences("config",0).edit().putString("regions",o.toString()).apply();}catch(Exception ignored){}}
    public static Regions load(Context c){Regions r=new Regions();try{JSONObject o=new JSONObject(c.getSharedPreferences("config",0).getString("regions","{}"));r.game=rect(o.getJSONArray("game"));for(int i=0;i<10;i++){r.boxes[i]=rect(o.getJSONArray("boxes").getJSONArray(i));r.angles[i]=(float)o.getJSONArray("angles").getDouble(i);}r.tableV7=o.optBoolean("tableV7",false);if(r.tableV7){r.table=rect(o.getJSONArray("table"));for(int i=0;i<8;i++){JSONArray a=o.getJSONArray("anchors").getJSONArray(i);r.anchors[i]=new float[]{(float)a.getDouble(0),(float)a.getDouble(1)};r.handAngles[i]=(float)o.getJSONArray("handAngles").getDouble(i);}}r.calibrated=r.tableV7&&o.optBoolean("calibrated");r.portraitPreset=o.optBoolean("portraitPreset");r.fullHands=o.optBoolean("fullHands");if(o.has("cardsV3")){r.cards=new CardLayout();for(int h=0;h<7;h++){for(int k=0;k<5;k++){JSONArray a=o.getJSONArray("cardsV3").getJSONArray(h*5+k);for(int n=0;n<5;n++)r.cards.slots[h][k][n]=(float)a.getDouble(n);}r.updateEnvelope(h);}}}catch(Exception ignored){r.cards=null;r.calibrated=false;}if(!r.fullHands){r.toHandBoxes();r.calibrated=false;}if(!r.validationError().isEmpty())r.calibrated=false;return r;}
    private JSONArray arr(RectF r)throws JSONException{return new JSONArray(new float[]{r.left,r.top,r.right,r.bottom});}
    private static RectF rect(JSONArray a)throws JSONException{return new RectF((float)a.getDouble(0),(float)a.getDouble(1),(float)a.getDouble(2),(float)a.getDouble(3));}
}
