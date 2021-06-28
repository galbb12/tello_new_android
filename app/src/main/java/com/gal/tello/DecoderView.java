package com.gal.tello;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;

public class DecoderView extends TextureView {
    private MediaCodec codec;
    private boolean bConfigured =false;
    //pic mode sps
    private byte[] sps = new byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 1, (byte) 103, (byte) 77, (byte) 64, (byte) 40, (byte) 149, (byte) 160, (byte) 60, (byte) 5, (byte) 185};



    private byte[] pps = new byte[] {(byte) 0, (byte) 0, (byte) 0, (byte) 1, (byte) 104, (byte) 238, (byte) 56, (byte) 128};
    private int decoderWidth = 960;
    private int decoderHeight = 720;
    Context CONTEXT;
    MainActivity mainActivity;
    SurfaceTexture surfaceTexture;
    Surface surface;
    TextureView textureView;


    public DecoderView(Context context, AttributeSet attributeSet) {
        super(context,attributeSet);
        CONTEXT=context;
        Initialize();



    }





    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void Init() {

        if(surface == null){
            textureView=this;
            surfaceTexture = textureView.getSurfaceTexture();
            surface= new Surface(surfaceTexture);}
        try {
            if (sps.length == 14){
                decoderWidth = 1280;}
            else{
                decoderWidth = 960;}

            MediaFormat videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, decoderWidth, decoderHeight);
            videoFormat.setByteBuffer("csd-0", ByteBuffer.wrap(sps));
            videoFormat.setByteBuffer("csd-1", ByteBuffer.wrap(pps));
         videoFormat.setFloat(MediaFormat.KEY_I_FRAME_INTERVAL,mainActivity.iFrameRate);
          videoFormat.setFloat(MediaFormat.KEY_BIT_RATE,  1.5f);
           videoFormat.setInteger(MediaFormat.KEY_HEIGHT,decoderHeight);
           videoFormat.setInteger(MediaFormat.KEY_WIDTH,decoderWidth);
           videoFormat.setInteger(MediaFormat.KEY_CAPTURE_RATE,30);
          // videoFormat.setInteger(videoFormat.KEY_COLOR_STANDARD,MediaFormat.COLOR_STANDARD_BT709);
          // videoFormat.setInteger(videoFormat.KEY_COLOR_RANGE,MediaFormat.COLOR_RANGE_LIMITED);
           videoFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);


            String str = videoFormat.getString("mime");
            if(codec!=null){
                codec.release();}
            MediaCodec cdx = MediaCodec.createDecoderByType(str);
            cdx.configure(videoFormat, surface, (MediaCrypto) null, 0);
            cdx.start();

            codec = cdx;
        mainActivity.runOnUiThread(new Runnable() {
            public void run() {
                //Code goes here
                int videoWidth = decoderWidth;
                int videoHeight = decoderHeight;
                float videoProportion = (float) videoWidth / (float) videoHeight;
                WindowManager windowManager = (WindowManager) CONTEXT.getSystemService(Context.WINDOW_SERVICE);

                // Get the width of the screen
                int screenWidth =  windowManager.getDefaultDisplay().getWidth();
                int screenHeight = windowManager.getDefaultDisplay().getHeight();
                float screenProportion = (float) screenWidth / (float) screenHeight;

                // Get the SurfaceView layout parameters
                android.view.ViewGroup.LayoutParams lp = textureView.getLayoutParams();
                if (videoProportion > screenProportion) {
                    lp.width = screenWidth;
                    lp.height = (int) ((float) screenWidth / videoProportion);
                } else {
                    lp.width = (int) (videoProportion * (float) screenHeight);
                    lp.height = screenHeight;
                }
                // Commit the layout parameters
                textureView.setLayoutParams(lp);
                textureView.invalidate();
                Log.d("Configured", "Configured");
                bConfigured = true;}});

        return;
        } catch (Exception ex) {
            //handle
            //bConfigured=false;
            // Init();
            ex.printStackTrace();
            //stop();
        }

    }

    public void setVideoData(byte[] array){

        int nalType = array[4] & 0x1f;
       if(nalType==8){
         if(array!=pps){
           pps=array;
           Init();}

       }
         if(nalType==7){

             if(array!=sps){
                 sps=array;
                 bConfigured=false;
             Init();}

        }


    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void decode(byte[] array) {
        Log.d("decode", "decode");
        if (!bConfigured) {
            Init();
        }

        int nalType = array[4] & 0x1f;
//Console.WriteLine("nal:" + nalType);











        if (bConfigured == false) {
            return;
        }

        //Make sure keyframe is first.
       //if (nalType == 5) {
       //    bWaitForKeyframe = false;

       //    //pps = array.ToArray();
       //    //return;
       //
       //}


        if (bConfigured) {
            try {
                int dequeueInputBuffer = codec.dequeueInputBuffer(-1L);
                ByteBuffer inputBuffer = codec.getInputBuffer(dequeueInputBuffer);
                if (dequeueInputBuffer >= 0) {
                    //Send data to decoder.
                    ByteBuffer byteBuffer = inputBuffer;
                    byteBuffer.clear();
                    byteBuffer.put(array);
                    codec.queueInputBuffer(dequeueInputBuffer, 0, array.length, 0L, 0);
                }

                //Show decoded frame
                MediaCodec.BufferInfo BufferInfo = new MediaCodec.BufferInfo();
                Log.d("tag", String.valueOf(BufferInfo.size));
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
        sps = new byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 1, (byte) 103, (byte) 77, (byte) 64, (byte) 40, (byte) 149, (byte) 160, (byte) 60, (byte) 5, (byte) 185};
        pps = new byte[] {(byte) 0, (byte) 0, (byte) 0, (byte) 1, (byte) 104, (byte) 238, (byte) 56, (byte) 128};
    }
}
