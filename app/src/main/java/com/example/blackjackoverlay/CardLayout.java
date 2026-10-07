package com.example.blackjackoverlay;
/** Five adjustable rank-corner windows for each of seven screen-ordered seats. */
public final class CardLayout {
    public final float[][][] slots=new float[7][5][5]; // center x/y, width/height, OCR rotation
    public CardLayout(){
        float[] x={171,260,373,465,576,678,789},y={788,811,833,833,826,814,788};
        float[] dx={37,33,25,21,16,-15,-24},dy={-14,-18,-19,-20,-22,-20,-21},a={-25,-17,-7,0,10,22,34};
        for(int h=0;h<7;h++)for(int k=0;k<5;k++)slots[h][k]=new float[]{(x[h]+k*dx[h])/945f,(y[h]+k*dy[h])/2048f,48/945f,24/2048f,a[h]};
    }
    public CardLayout copy(){CardLayout c=new CardLayout();for(int h=0;h<7;h++)for(int k=0;k<5;k++)c.slots[h][k]=slots[h][k].clone();return c;}
    public float[] bounds(int h){float l=1,t=1,r=0,b=0;for(float[] s:slots[h]){l=Math.min(l,s[0]-s[2]/2);t=Math.min(t,s[1]-s[3]/2);r=Math.max(r,s[0]+s[2]/2);b=Math.max(b,s[1]+s[3]/2);}return new float[]{l,t,r,b};}
    public void transformHand(int h,float l,float t,float r,float b){float[] old=bounds(h);float xs=(r-l)/(old[2]-old[0]),ys=(b-t)/(old[3]-old[1]);for(float[] s:slots[h]){s[0]=l+(s[0]-old[0])*xs;s[1]=t+(s[1]-old[1])*ys;s[2]*=xs;s[3]*=ys;}}
    public float[] corners(int h,int k,float width,float height){float[] s=slots[h][k];float cx=s[0]*width,cy=s[1]*height,hw=s[2]*width/2,hh=s[3]*height/2;double a=Math.toRadians(-s[4]);float[] q={-hw,-hh,hw,-hh,hw,hh,-hw,hh};for(int n=0;n<8;n+=2){float x=q[n],y=q[n+1];q[n]=cx+(float)(x*Math.cos(a)-y*Math.sin(a));q[n+1]=cy+(float)(x*Math.sin(a)+y*Math.cos(a));}return q;}
    public void rotate(int h,int k,float degrees,float aspect){if(k>=0){slots[h][k][4]+=degrees;return;}float[] b=bounds(h);float cx=(b[0]+b[2])/2,cy=(b[1]+b[3])/2;double a=Math.toRadians(-degrees);for(float[] s:slots[h]){float x=s[0]-cx,y=(s[1]-cy)/aspect;s[0]=cx+(float)(x*Math.cos(a)-y*Math.sin(a));s[1]=cy+aspect*(float)(x*Math.sin(a)+y*Math.cos(a));s[4]+=degrees;}}
}
