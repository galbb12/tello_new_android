package com.gal.tello;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;

import androidx.appcompat.app.AppCompatActivity;

import java.nio.ByteBuffer;

public class DecoderView extends SurfaceView {
    byte[] buffer;
    private MediaCodec codec;

    private boolean bConfigured;
    //pic mode sps
    private byte[] sps = new byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 1, (byte) 103, (byte) 77, (byte) 64, (byte) 40, (byte) 149, (byte) 160, (byte) 60, (byte) 5, (byte) 185};
    private boolean bWaitForKeyframe = true;

    //vid mode sps
    private byte[] vidSps = new byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 1, (byte) 103, (byte) 77, (byte) 64, (byte) 40, (byte) 149, (byte) 160, (byte) 20, (byte) 1, (byte) 110, (byte) 64};

    private byte[] pps = {(byte) 0, (byte) 0, (byte) 0, (byte) 1, (byte) 104, (byte) 238, (byte) 56, (byte) 128};
    private int decoderWidth = 960;
    private int decoderHeight = 720;


    public DecoderView(Context context, AttributeSet attributeSet) {
        super(context,attributeSet);
        Initialize();


    }


    public void test() {
        Log.d("test", "test");
    }

    private void Init() {
        if (sps.length == 14)
            decoderWidth = 1280;
        else
            decoderWidth = 960;

        MediaFormat videoFormat = MediaFormat.createVideoFormat("video/avc", decoderWidth, decoderHeight);
        videoFormat.setByteBuffer("csd-0", ByteBuffer.wrap(sps));
        videoFormat.setByteBuffer("csd-1", ByteBuffer.wrap(pps));

        String str = videoFormat.getString("mime");
        try {
            MediaCodec cdx = MediaCodec.createDecoderByType(str);
            cdx.configure(videoFormat, getHolder().getSurface(), (MediaCrypto) null, 0);
            cdx.start();

            codec = cdx;
            bWaitForKeyframe = true;
            bConfigured = true;
        } catch (Exception ex) {
            //handle
            ex.printStackTrace();
        }
        return;

    }


    public void decode() {
        Log.d("decode", "decode");
        byte[] array = MainActivity.lmessage;
        if (bConfigured == false) {
            Init();
        }

        int nalType = array[4] & 0x1f;
//Console.WriteLine("nal:" + nalType);
        if (nalType == 7) {
            //sps = array.ToArray();
            if (array.length != sps.length) {
                stop();
                sps = array;
                Init();
            }
            return;
        }
        if (nalType == 8) {
            //pps = array.ToArray();
            return;
        }
        if (bConfigured == false) {
            return;
        }

        //Make sure keyframe is first.
        if (nalType == 5) {
            bWaitForKeyframe = false;
            //pps = array.ToArray();
            //return;
        }
        if (bWaitForKeyframe)
            return;

        if (bConfigured) {
            try {
                ByteBuffer[] inputBuffers = codec.getInputBuffers();
                ByteBuffer[] outputBuffers = codec.getOutputBuffers();
                int dequeueInputBuffer = codec.dequeueInputBuffer(-1L);
                if (dequeueInputBuffer >= 0) {
                    //Send data to decoder.
                    ByteBuffer byteBuffer = inputBuffers[dequeueInputBuffer];
                    byteBuffer.clear();
                    byteBuffer.put(array);
                    codec.queueInputBuffer(dequeueInputBuffer, 0, array.length, 0L, 0);
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
                }
            } catch (Exception ex) {
                ex.printStackTrace();

                //attempt to recover.
                //codec.Release();
                //codec = null;
                //bConfigured = false;


                stop();
            }
        }
    }


    private void Initialize() {
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
    }


}
