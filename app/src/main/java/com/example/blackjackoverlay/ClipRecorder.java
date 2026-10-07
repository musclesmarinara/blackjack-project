package com.example.blackjackoverlay;
import android.graphics.*;
import android.media.*;
import java.io.*;
import java.nio.*;

/** Encodes on the capture worker; no second MediaProjection session is used. */
public final class ClipRecorder {
    private MediaCodec codec; private MediaMuxer muxer; private int track=-1;
    private boolean muxStarted=false; private long start=-1; private int width,height;
    private Bitmap frame; private Canvas canvas; private Paint paint=new Paint(3);
    public ClipRecorder(File output,int sourceWidth,int sourceHeight)throws IOException{
        width=720;height=((int)(720.0*sourceHeight/sourceWidth)+180+1)/2*2;

        MediaFormat f=MediaFormat.createVideoFormat("video/avc",width,height);
        f.setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        f.setInteger(MediaFormat.KEY_BIT_RATE,3500000);f.setInteger(MediaFormat.KEY_FRAME_RATE,5);f.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,2);
        codec=MediaCodec.createEncoderByType("video/avc");codec.configure(f,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);codec.start();
        muxer=new MediaMuxer(output.toString(),MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        frame=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);canvas=new Canvas(frame);
    }
    public void add(Bitmap screen,String caption,long now)throws IOException{
        if(start<0)start=now;
        canvas.drawColor(Color.BLACK);canvas.drawBitmap(screen,null,new Rect(0,0,width,height-180),paint);
        paint.setColor(Color.rgb(107,237,186));paint.setTextSize(17);
        int y=height-157;for(String line:caption.split("\n")){canvas.drawText(line,10,y,paint);y+=22;if(y>height-5)break;}
        int idx=codec.dequeueInputBuffer(0);
        if(idx>=0){
            Image in=codec.getInputImage(idx);if(in==null)throw new IOException("Encoder has no YUV input image");
            Image.Plane[] planes=in.getPlanes();int[] pixels=new int[width*height];frame.getPixels(pixels,0,width,0,0,width,height);
            for(int py=0;py<height;py++)for(int px=0;px<width;px++){
                int c=pixels[py*width+px],r=(c>>16)&255,g=(c>>8)&255,b=c&255;
                put(planes[0],px,py,clamp(((66*r+129*g+25*b+128)>>8)+16));
                if((px&1)==0&&(py&1)==0){put(planes[1],px/2,py/2,clamp(((-38*r-74*g+112*b+128)>>8)+128));put(planes[2],px/2,py/2,clamp(((112*r-94*g-18*b+128)>>8)+128));}
            }
            codec.queueInputBuffer(idx,0,width*height*3/2,(now-start)*1000,0);
        }
        drain(false);
    }
    private int clamp(int n){return Math.max(0,Math.min(255,n));}
    private void put(Image.Plane p,int x,int y,int value){p.getBuffer().put(y*p.getRowStride()+x*p.getPixelStride(),(byte)value);}
    private void drain(boolean end){
        MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();int tries=0;
        while(true){int n=codec.dequeueOutputBuffer(info,end?10000:0);
            if(n==MediaCodec.INFO_TRY_AGAIN_LATER){if(!end||++tries>50)break;continue;}
            if(n==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){track=muxer.addTrack(codec.getOutputFormat());muxer.start();muxStarted=true;continue;}
            if(n>=0){ByteBuffer b=codec.getOutputBuffer(n);if((info.flags&MediaCodec.BUFFER_FLAG_CODEC_CONFIG)!=0)info.size=0;
                if(info.size>0&&muxStarted){b.position(info.offset);b.limit(info.offset+info.size);muxer.writeSampleData(track,b,info);}
                codec.releaseOutputBuffer(n,false);if((info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0)break;}
        }
    }
    public void close(){
        try{int n=codec.dequeueInputBuffer(100000);if(n>=0){codec.queueInputBuffer(n,0,0,Math.max(0,android.os.SystemClock.elapsedRealtime()-start)*1000,MediaCodec.BUFFER_FLAG_END_OF_STREAM);drain(true);}}catch(Exception ignored){}
        try{codec.stop();}catch(Exception ignored){}codec.release();
        try{if(muxStarted)muxer.stop();}catch(Exception ignored){}muxer.release();frame.recycle();
    }
}
