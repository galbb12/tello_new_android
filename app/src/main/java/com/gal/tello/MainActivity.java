package com.gal.tello;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;


import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.common.AndroidUtil;
import org.jcodec.common.JCodecUtil;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

import io.github.controlwear.virtual.joystick.android.JoystickView;
//import nl.bravobit.ffmpeg.ExecuteBinaryResponseHandler;
//import nl.bravobit.ffmpeg.FFmpeg;
//import nl.bravobit.ffmpeg.FFtask;

import static java.lang.Math.max;

import java.lang.*;


public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener{
    private H264Decoder decoder;
    public static final int TELLO_CAM_LISTEN_PORT = 11111;
    String output;
    Button streamon;
    Button takeoff;
    Button land;
    Button stopffmpegtask;
    TextureView textureView;
    ImageView imageView;

    //FFtask fftask;
    private MediaCodec m_codec;// Media decoder
    private DatagramSocket socketMainSending;
    private InetAddress inetAddressMainSending;
    public static final int portMainSending = 8889;
    public static final String addressMainSending = "192.168.10.1";

    DatagramSocket socketStatusServer;
    DatagramSocket socketStreamOnServer;
    private static final String SAMPLE_URL = "udp://@0.0.0.0:11111";
    public static final int portMainVideo = 11111;
    double a=0, b=0, c=0, d=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        final JoystickView joystick = (JoystickView) findViewById(R.id.joystickView);
        final JoystickView joystick1 = (JoystickView) findViewById(R.id.joystickView1);
        takeoff=findViewById(R.id.TakeOff);
        land=findViewById(R.id.Land);
        streamon=findViewById(R.id.StremOn);
        //textureView=findViewById(R.id.textureView);
        imageView=findViewById(R.id.player_view);
        stopffmpegtask=findViewById(R.id.stop_ffmpeg_task);
        joystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
               // double theta = angle * PI / 180;
               // int dx = (int) (strength*cos(theta));
               // int dy = (int) (strength * sin(theta));
                a = (joystick.getNormalizedX() - 50) * 2;
                b = -(joystick.getNormalizedY() - 50) * 2;
               // a=dx;
               // b=dy;
               // Log.d("dx joystic1", String.valueOf(dx));
               // Log.d("dy joystic1", String.valueOf(dx));
               d = (joystick1.getNormalizedX() - 50) * 2;
               c = -(joystick1.getNormalizedY() - 50) * 2;
                rc();
            }
        });
        joystick1.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                a = (joystick.getNormalizedX() - 50) * 2;
                b = -(joystick.getNormalizedY() - 50) * 2;
                //double theta = angle * PI /180;
                //int dx = (int) (100*cos(theta));
                //int dy = (int) (100 * sin(theta));
                d = (joystick1.getNormalizedX() - 50) * 2;
                c = -(joystick1.getNormalizedY() - 50) * 2;
                //d=dx;
                //c=dy;
                //Log.d("strength joystic2", String.valueOf(strength));
                //Log.d("dx joystic2", String.valueOf(dx));
                //Log.d("dy joystic2", String.valueOf(dx));
                rc();

            }
        });

        takeoff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takeoff();
            }
        });
        land.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connect();
            }
        });
        streamon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                streamon();
            }
        });
        stopffmpegtask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            //    if(fftask!=null){
            //    fftask.sendQuitSignal();}
            }
        });
        Initialize();
    }

    public void Initialize() {

        try {
            socketMainSending = new DatagramSocket();
            inetAddressMainSending = getInetAddressByName(addressMainSending);
            if (inetAddressMainSending == null) {

            } else {

            }
            socketStatusServer = new DatagramSocket(null);
            InetSocketAddress addressStatus = new InetSocketAddress("0.0.0.0", 8890);
            socketStatusServer.bind(addressStatus);


        } catch (IOException e) {
            Log.e("IOException",e.toString());
        }

    }

    public static InetAddress getInetAddressByName(String name) {
        AsyncTask<String, Void, InetAddress> task = new AsyncTask<String, Void, InetAddress>() {

            @Override
            protected InetAddress doInBackground(String... params) {
                try {
                    return InetAddress.getByName(params[0]);
                } catch (UnknownHostException e) {
                    return null;
                }
            }
        };
        try {
            return task.execute(name).get();
        } catch (InterruptedException e) {
            return null;
        } catch (ExecutionException e) {
            return null;
        }

    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {

    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

    }

    public class SendOneCommand extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... strings) {
            String command = strings[0];
            byte[] buf = strings[0].getBytes();
            DatagramPacket packet = new DatagramPacket(buf, buf.length, inetAddressMainSending, portMainSending);
            try {
                socketMainSending.send(packet);
                buf = new byte[500];
                packet = new DatagramPacket(buf, buf.length);
                socketMainSending.receive(packet);
                String doneText = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                Log.d("done text", doneText);

            } catch (IOException e) {
                Log.e("IOException",e.getMessage());

            } catch (Exception e) {
                Log.e("Exception",e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
        }
    }


    public class SendOneCommandwithoutreplay extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... strings) {
            String command = strings[0];
            byte[] buf = strings[0].getBytes();
            DatagramPacket packet = new DatagramPacket(buf, buf.length, inetAddressMainSending, portMainSending);
            try {
                socketMainSending.send(packet);

            } catch (IOException e) {
                Log.e("IOException",e.getMessage());

            } catch (Exception e) {
                Log.e("Exception",e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
        }
    }

    public class SendBytesWithoutReplay extends AsyncTask<byte[], Byte[], String> {

        @Override
        protected String doInBackground(byte[]... bytes) {

            byte[] buf = bytes[0];
            DatagramPacket packet = new DatagramPacket(buf, buf.length, inetAddressMainSending, portMainSending);
            try {
                socketMainSending.send(packet);

            } catch (IOException e) {
                Log.e("IOException",e.getMessage());

            } catch (Exception e) {
                Log.e("Exception",e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
        }


    }



    public void connect() {


        try {



            SendOneCommandwithoutreplay sendOneCommand = new SendOneCommandwithoutreplay();
            sendOneCommand.doInBackground("command");


        } catch (Exception e) {
        }
    }

    public void takeoff() {


        try {



            SendOneCommandwithoutreplay sendOneCommand = new SendOneCommandwithoutreplay();
            sendOneCommand.doInBackground("takeoff");


        } catch (Exception e) {
        }
    }

   void handle(ByteBuffer message) {
        try {
            ByteBuffer bb =message; // Your frame data is stored in this buffer
            H264Decoder decoder = new H264Decoder();
            Picture out = Picture.create(1920, 1088, ColorSpace.YUV420); // Allocate output frame of max size
            Picture real = decoder.decodeFrame(bb, out.getData());
            Bitmap bi = AndroidUtil.toBitmap(real); // If you prefere AWT image
            imageView.setImageBitmap(bi);
             }catch (Exception e){e.printStackTrace();}
   }

    public void streamon() {


        try {



            //startffmpegstream();
            SendOneCommandwithoutreplay sendOneCommand = new SendOneCommandwithoutreplay();
            sendOneCommand.doInBackground("streamon");
           socketStreamOnServer = new DatagramSocket(null);
           InetSocketAddress addressVideo = new InetSocketAddress("0.0.0.0", 11111);
           socketStreamOnServer.bind(addressVideo);
          VideoDatagramReceiver videoDatagramReceiver = new VideoDatagramReceiver();
          videoDatagramReceiver.start();

           //VideoServer videoServer= new VideoServer() {
           //    @Override
           //    protected void handle(byte[] message) {
           //        Log.d("decode","DECODE");
           //        Picture out = Picture.create(1920, 1088, ColorSpace.YUV420); // Allocate output frame of max size
           //        Picture real = decoder.decodeFrame(ByteBuffer.wrap(message), out.getData());
           //        Log.d("imagesize",real.getWidth() +  " : " + real.getHeight());
           //    }
           //};
           //videoServer.Server(TELLO_CAM_LISTEN_PORT, 2048);
           //videoServer.run();


        } catch (Exception e) {
        }
    }
    private class VideoDatagramReceiver extends Thread {
        private boolean bKeepRunning = true;
        private String lastMessage = "";

        @Override
        public void run() {
                Log.d("video start", "start");
                byte[] lmessage = new byte[2048];
                 DatagramPacket packet = new DatagramPacket(lmessage, lmessage.length);

            try {


                while(bKeepRunning) {
                    socketStreamOnServer.receive(packet);
                    Log.d("video length", String.valueOf(new String(lmessage, 0, packet.getLength()).trim().length()));

                    try{
                        SendOneCommandwithoutreplay sendOneCommand = new SendOneCommandwithoutreplay();
                        sendOneCommand.doInBackground("streamon");
                    handle(ByteBuffer.wrap(packet.getData()));

}catch (RuntimeException e){Log.e("error",e.toString());}





                }

                if (socketStatusServer == null) {
                    socketStatusServer.close();
                }

            } catch (IOException ioe){

            }

        }

        public void kill() {
            bKeepRunning = false;
        }
    }

    public void rc() {

       /* if(a==70&&b==-70&&c==-70&&d==-70){
        Log.d("startmotors","motorstart");}
        else{*/
            SendOneCommandwithoutreplay sendOneCommand = new SendOneCommandwithoutreplay();
            sendOneCommand .doInBackground("rc " + a + " " + b + " " + c + " " + d);
       // }
    }

    private class MessageDatagramReceiver extends Thread {
        private boolean bKeepRunning = true;
        private String lastMessage = "";

        @Override
        public void run() {
            String message;
            byte[] lmessage = new byte[500];
            DatagramPacket packet = new DatagramPacket(lmessage, lmessage.length);

            try {

                while(bKeepRunning) {
                    socketStatusServer.receive(packet);
                    message = new String(lmessage, 0, packet.getLength());
                    lastMessage = message;

                }

                if (socketStatusServer == null) {
                    socketStatusServer.close();
                }

            } catch (IOException ioe){

            }

        }

        public void kill() {
            bKeepRunning = false;
        }
    }

    private class displayimage extends Thread {
        private boolean bKeepRunning = true;


        @Override
        public void run() {





            final Handler handler = new Handler();
                Runnable runnable =  new Runnable() {
                    @Override
                    public void run() {

                        File f = new File(output);
                        if(f.isFile())

                        {
                            imageView.setImageBitmap(BitmapFactory.decodeFile(output));
                        }
                        if(bKeepRunning==true){
                            handler.postDelayed(this, 5);
                        }
                    }



            };
            handler.postDelayed(runnable, 5);

        }

        public void kill() {
            bKeepRunning = false;
        }
    }


  //  void startffmpegstream(){
  //      if (FFmpeg.getInstance(this).isSupported()) {
  //          File directory = getFilesDir();
  //          output = directory + "/tello.bmp";
//
//
  //          Log.v("MainActivity", "The storage path is: " + output);
  //          String[] cmd = {"-y","-i", "udp://127.0.0.1:11111","-r", "5/1","-update","1",output};
  //          FFmpeg ffmpeg = FFmpeg.getInstance(this);
//
//
  //          // to execute "ffmpeg -version" command you just need to pass "-version"
  //          fftask = ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {
//
  //              @Override
  //              public void onStart() {
//
  //                  displayimage displayimage=new displayimage();
  //                  displayimage.run();
  //              }
//
  //              @Override
  //              public void onProgress(String message) {
//
  //                 //while (true){
  //                 //    File f = new File(output);
  //                 //    if(f.isFile()){
  //                 //  imageView.setImageBitmap(BitmapFactory.decodeFile(output));}
  //                 //}
  //              }
//
  //              @Override
  //              public void onFailure(String message) {
  //                  Log.v("TEST", "FFMPEG streaming command failure: " + message);
//
  //              }
//
  //              @Override
  //              public void onSuccess(String message) {
//
  //              }
//
  //              @Override
  //              public void onFinish() {}
//
  //          });
//
  //      }
  //  }





}


