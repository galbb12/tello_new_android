package com.gal.tello;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;




import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import io.github.controlwear.virtual.joystick.android.JoystickView;
//import nl.bravobit.ffmpeg.ExecuteBinaryResponseHandler;
//import nl.bravobit.ffmpeg.FFmpeg;
//import nl.bravobit.ffmpeg.FFtask;

import static java.lang.Math.max;


import java.lang.*;


public class MainActivity extends AppCompatActivity{

    public static final int TELLO_CAM_LISTEN_PORT = 11111;
    String output;
    Button streamon;
    Button takeoff;
    Button land;
    Button stopffmpegtask;
    TextureView textureView;

    // DecoderView decoderView = new DecoderView();
    private byte[] sps = new byte[] {(byte) 0, (byte)0,(byte) 0,(byte) 1,(byte) 103,(byte) 77, (byte)64,(byte) 40,(byte) 149, (byte)160, (byte)60,(byte) 5,(byte) 185 };
    private boolean bWaitForKeyframe = true;

    //vid mode sps
    private byte[] vidSps = new byte[] { (byte)0,(byte) 0, (byte)0, (byte)1, (byte)103, (byte)77,(byte) 64,(byte) 40,(byte) 149, (byte)160,(byte) 20,(byte) 1, (byte)110, (byte)64 };

    private byte[] pps = { (byte) 0, (byte) 0, (byte) 0, (byte) 1, (byte) 104, (byte) 238, (byte) 56, (byte) 128};
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
    DecoderView decoderView;


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
    byte[] add (byte[] dst, byte[] source){
        byte[] new_a;
        if(dst!=null){
    new_a = new byte[dst.length+source.length];
            System.arraycopy(dst, 0, new_a, 0, dst.length); // copy a
            System.arraycopy(source, 0, new_a, source.length, dst.length); // copy b after a
            }
        else {new_a = new byte[source.length];

          new_a=source;
        }



    return new_a;
    }


    byte[] trim(byte[] bytes)
    {
        int i = bytes.length - 1;
        while (i >= 0 && bytes[i] == 0)
        {
            --i;
        }

        return Arrays.copyOf(bytes, i + 1);
    }

    public static byte[] addAll(final byte[] array1, byte[] array2) {
        if(array1==null){
       byte[] joinedArray=array2;
            return joinedArray;}
        else if(array2==null){
            byte[] joinedArray=array1;
            return joinedArray;
        }else{   byte[] joinedArray = Arrays.copyOf(array1, array1.length + array2.length);
            System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
            return joinedArray;}
    }

    private class VideoDatagramReceiver extends Thread {
        private boolean bKeepRunning = true;
        private String lastMessage = "";
        public byte[] lmessage = new byte[1460];
        byte[] videoFrame;

        @Override
        public void run() {
            Log.d("video start", "start");
            DatagramPacket packet = new DatagramPacket(lmessage, lmessage.length);

            try {


                while(bKeepRunning) {

                    socketStreamOnServer.receive(packet);
                    byte[] data = new byte[packet.getLength()];
                    System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());

                    Log.d("video length", String.valueOf(data.length));

                    try{
                        videoFrame=addAll(videoFrame,data);
                        if(data.length!=1460&&videoFrame!=null){
                         DecoderView imageView = findViewById(R.id.decoderView);
                         imageView.decode(videoFrame);
                         videoFrame=new byte[1460*100];
                            videoFrame=null;}









                    }catch (RuntimeException e){Log.e("error",e.toString());
                   }




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

//   private class displayimage extends Thread {
//       private boolean bKeepRunning = true;


//       @Override
//       public void run() {





//           final Handler handler = new Handler();
//           Runnable runnable =  new Runnable() {
//               @Override
//               public void run() {

//                   File f = new File(output);
//                   if(f.isFile())

//                   {
//                       imageView.setImageBitmap(BitmapFactory.decodeFile(output));
//                   }
//                   if(bKeepRunning==true){
//                       handler.postDelayed(this, 5);
//                   }
//               }



//           };
//           handler.postDelayed(runnable, 5);

//       }

//       public void kill() {
//           bKeepRunning = false;
//       }
//   }


//   //  void startffmpegstream(){
//   //      if (FFmpeg.getInstance(this).isSupported()) {
//   //          File directory = getFilesDir();
//   //          output = directory + "/tello.bmp";
///
///
//   //          Log.v("MainActivity", "The storage path is: " + output);
//   //          String[] cmd = {"-y","-i", "udp://127.0.0.1:11111","-r", "5/1","-update","1",output};
//   //          FFmpeg ffmpeg = FFmpeg.getInstance(this);
///
///
//   //          // to execute "ffmpeg -version" command you just need to pass "-version"
//   //          fftask = ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {
///
//   //              @Override
//   //              public void onStart() {
///
//   //                  displayimage displayimage=new displayimage();
//   //                  displayimage.run();
//   //              }
///
//   //              @Override
//   //              public void onProgress(String message) {
///
//   //                 //while (true){
//   //                 //    File f = new File(output);
//   //                 //    if(f.isFile()){
//   //                 //  imageView.setImageBitmap(BitmapFactory.decodeFile(output));}
//   //                 //}
//   //              }
///
//   //              @Override
//   //              public void onFailure(String message) {
//   //                  Log.v("TEST", "FFMPEG streaming command failure: " + message);
///
//   //              }
///
//   //              @Override
//   //              public void onSuccess(String message) {
///
//   //              }
///
//   //              @Override
//   //              public void onFinish() {}
///
//   //          });
///
//   //      }
//   //  }





}




 // create a destination array that is the size of the two arrays
    /*
 add (a,b => a)
    byte[] new_a = new byte[length (a)+length(b)];
System.arraycopy(a, 0, new_a, 0, a.length()); // copy a
System.arraycopy(b, 0, new_a, a.length(), b.length()); // copy b after a
retrun new_a*/