package com.gal.tello;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;

import android.media.MediaCodec;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;

public class DecoderView extends TextureView implements TextureView.SurfaceTextureListener {
    private MediaCodec codec;
    Boolean bConfigured =false;
    //pic mode sps
    private byte[] sps;// = new byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 1, (byte) 103, (byte) 77, (byte) 64, (byte) 40, (byte) 149, (byte) 160, (byte) 60, (byte) 5, (byte) 185};
    private byte[] pps;// = new byte[] {(byte) 0, (byte) 0, (byte) 0, (byte) 1, (byte) 104, (byte) 238, (byte) 56, (byte) 128};
    private int decoderWidth ;
    private int decoderHeight;
     boolean bWaitForKeyframe = true;
    Context CONTEXT;
    MainActivity mainActivity;
    SurfaceTexture surfaceTexture;
    Surface surface;
    TextureView textureView;
    MediaFormat videoFormat;
    boolean recivedsps,recivedpps=false;


    public DecoderView(Context context, AttributeSet attributeSet) {
        super(context,attributeSet);
        CONTEXT=context;
        Initialize();



    }





    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void Init() {

        if(sps!=null) {
            if (surface == null) {
                textureView = this;
                surfaceTexture = textureView.getSurfaceTexture();
                surface = new Surface(surfaceTexture);
            }
            try {
                if (sps.length == 14) {
                    decoderWidth = 1280;
                    decoderHeight = 720;
                } else {
                    decoderWidth = 960;
                    decoderHeight = 720;
                }


            videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, decoderWidth, decoderHeight);
            if (sps != null) {
                videoFormat.setByteBuffer("csd-0", ByteBuffer.wrap(sps));
                videoFormat.setByteBuffer("csd-1", ByteBuffer.wrap(pps));
            }
            videoFormat.setInteger(MediaFormat.KEY_PRIORITY,0);
            //  videoFormat.setInteger(MediaFormat.KEY_COLOR_TRANSFER,MediaFormat.COLOR_TRANSFER_SDR_VIDEO);
            //videoFormat.setFloat(MediaFormat.KEY_I_FRAME_INTERVAL,(1000/mainActivity.iFrameRate)/1000);
           // videoFormat.setInteger(MediaFormat.KEY_HEIGHT,decoderHeight);
           //    videoFormat.setInteger(MediaFormat.KEY_WIDTH,decoderWidth);
            //   videoFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0);
            //  videoFormat.setInteger(MediaFormat.KEY_CAPTURE_RATE,30);
            //    videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE,25);


            String str = videoFormat.getString("mime");
            if (codec != null) {
                codec.release();
            } else {
                mainActivity.requestIframe();
            }
            MediaCodec cdx = MediaCodec.createDecoderByType(str);
            cdx.configure(videoFormat, surface, (MediaCrypto) null, 0);
            cdx.start();

            codec = cdx;

            //Code goes here
            int videoWidth = decoderWidth;
            int videoHeight = decoderHeight;
            float videoProportion = (float) videoWidth / (float) videoHeight;
            WindowManager windowManager = (WindowManager) mainActivity.activity.getSystemService(Context.WINDOW_SERVICE);

            // Get the width of the screen
            int screenWidth = windowManager.getDefaultDisplay().getWidth();
            int screenHeight = windowManager.getDefaultDisplay().getHeight();
            float screenProportion = (float) screenWidth / (float) screenHeight;

            // Get the SurfaceView layout parameters
            ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) this.getLayoutParams();
            if (videoProportion > screenProportion) {
                lp.width = screenWidth;
                lp.height = (int) ((float) screenWidth / videoProportion);
            } else {
                lp.width = (int) (videoProportion * (float) screenHeight);
                lp.height = screenHeight;
            }
            // Commit the layout parameters
            this.setLayoutParams(lp);
            Log.d("Configured", "Configured");
            bConfigured = true;// This is your code
        }



     //   return;
         catch (Exception exception) {
            //handle
            //bConfigured=false;
            // Init();
            exception.printStackTrace();
            //stop();
        }}

    }

    void ReinitzializeDecoderParmeters(){

        if(videoFormat.getByteBuffer("csd-0")!=ByteBuffer.wrap(sps)||videoFormat.getByteBuffer("csd-1")!=ByteBuffer.wrap(pps))
            if(recivedsps){
        videoFormat.setByteBuffer("csd-0", ByteBuffer.wrap(sps));}
        if(recivedpps){
        videoFormat.setByteBuffer("csd-1", ByteBuffer.wrap(pps));}

        codec.configure(videoFormat, surface, (MediaCrypto) null, 0);
    }

    public void setVideoData(byte[] array) throws IOException {

        int nalType = array[4] & 0x1f;

       if(nalType==8){
           pps=array;
           recivedpps=true;

           ReinitzializeDecoderParmeters();


       }
         if(nalType==7){
             recivedsps=true;
                 sps=array;
                 ReinitzializeDecoderParmeters();
}

    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void decode(byte[] array) throws IOException {
        Log.d("decode", "decode");
        if (!bConfigured) {
            if(pps!=null){
            Init();}
        }

        int nalType = array[4] & 0x1f;






        if (nalType == 5) {
           // if(setvideoparmeters){
           //     setvideoparmeters=false;
           //     ReinitzializeDecoderParmeters();}



                //recivedsps=false;
                //recivedpps=false;
                bWaitForKeyframe = false;



        }
        //if (nalType == 1) {
        //    if(recivedpps&&recivedsps){
        //        ReinitzializeDecoderParmeters();
        //        recivedsps=false;
        //        recivedpps=false;
        //}
        //}
        //else if(nalType==1){
        //    if(setvideoparmeters){
        //   setvideoparmeters=false;
        //   ReinitzializeDecoderParmeters();}
        //}
        if (bWaitForKeyframe){
            return;}




        if (bConfigured == false) {
            return;
        }



        if (bConfigured||nalType==8||nalType==7) {
            try {
                int dequeueInputBuffer = codec.dequeueInputBuffer(-1L);
                ByteBuffer inputBuffer = codec.getInputBuffer(dequeueInputBuffer);
                if (dequeueInputBuffer >= 0) {
                    //Send data to decoder.
                    ByteBuffer byteBuffer = inputBuffer;
                    byteBuffer.clear();
                    if(nalType==5){
                       // byteBuffer.put(sps);
                       // byteBuffer.put(pps);
                        byteBuffer.put(array);
                        codec.queueInputBuffer(dequeueInputBuffer, 0, array.length, -1L,MediaCodec.BUFFER_FLAG_KEY_FRAME);}
                    else if(nalType==1){
                        byteBuffer.put(array);
                        codec.queueInputBuffer(dequeueInputBuffer, 0, array.length, -1L,MediaCodec.BUFFER_FLAG_PARTIAL_FRAME);
                    }
                    else if(nalType==8||nalType==7){
                        if(bConfigured){
                        byteBuffer.put(array);
                        codec.queueInputBuffer(dequeueInputBuffer, 0, array.length, -1L,MediaCodec.BUFFER_FLAG_CODEC_CONFIG);}
                        if(nalType==8){
                            pps=array;
                            recivedpps=true;

                          //  ReinitzializeDecoderParmeters();


                        }
                        if(nalType==7){
                            recivedsps=true;
                            sps=array;
                          //  ReinitzializeDecoderParmeters();
                        }
                    }

                }

                //Show decoded frame
                MediaCodec.BufferInfo BufferInfo = new MediaCodec.BufferInfo();

                int i = codec.dequeueOutputBuffer(BufferInfo, 0L);
                while (i >= 0) {
                        /*if (picSurface == null)//Only if not using display surface.
                        {
                            ByteBuffer byteBuffer2 = outputBuffers[i];
                            if (buffer == null || buffer.Length != BufferInfo.Size)
                            {
                                buffer = new byte[BufferInfo.Size];
                            }
                            byteBuffer2.Get(buffer);
                            //do something with raw frame in buffer.
                        }*/







                        codec.releaseOutputBuffer(i, true);
                        codec.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT);
                        i = codec.dequeueOutputBuffer(BufferInfo, 0L);

                        //  ByteBuffer buf = codec.getOutputBuffer(-1);
                        //  byte[] imageBytes= new byte[buf.remaining()];
                        //  buf.get(imageBytes);
                        //  Bitmap bitmap= BitmapFactory.decodeByteArray(imageBytes,0,imageBytes.length);
                        //  FileOutputStream out = new FileOutputStream("");
                        //  bitmap.compress(Bitmap.CompressFormat.PNG,100,)

                        //Image image = codec.getOutputImage(0);
                        //  YuvImage yuvImage = new YuvImage(YUV_420_888toNV21(image), ImageFormat.NV21, decoderWidth,decoderHeight, null);
                        //  ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        //  yuvImage.compressToJpeg(new Rect(0, 0, decoderWidth, decoderHeight), 80, stream);
                        //  Bitmap bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
                        //  try {
                        //      stream.close();
                        //  } catch (IOException e) {
                        //      e.printStackTrace();
                        //  }
                    }

            } catch (Exception ex) {
                ex.printStackTrace();

                //attempt to recover.
                //codec.Release();
                //codec = null;
                //bConfigured = false;


                stop();
            }
        }else{stop();}
    }



    private void Initialize() {
        mainActivity= new MainActivity();
    }

    public void stop() {
        bConfigured = false;
        if (codec != null) {
            try {
                //codec.Stop();
                codec.release();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        codec = null;
        //sps = new byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 1, (byte) 103, (byte) 77, (byte) 64, (byte) 40, (byte) 149, (byte) 160, (byte) 60, (byte) 5, (byte) 185};
        //pps = new byte[] {(byte) 0, (byte) 0, (byte) 0, (byte) 1, (byte) 104, (byte) 238, (byte) 56, (byte) 128};
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        Init();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
       stop();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        stop();
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
      stop();
    }
}
