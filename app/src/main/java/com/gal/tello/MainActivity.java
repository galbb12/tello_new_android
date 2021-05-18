package com.gal.tello;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;




import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import io.github.controlwear.virtual.joystick.android.JoystickView;
//import nl.bravobit.ffmpeg.ExecuteBinaryResponseHandler;
//import nl.bravobit.ffmpeg.FFmpeg;
//import nl.bravobit.ffmpeg.FFtask;

import static java.lang.Math.max;


import java.lang.*;


public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener{
    public static byte[] lmessage = new byte[2048];
    public static final int TELLO_CAM_LISTEN_PORT = 11111;
    String output;
    Button streamon;
    Button takeoff;
    Button land;
    Button stopffmpegtask;
    TextureView textureView;
    static JoystickView joystick;
    static JoystickView joystick1;
    private static int sequence = 1;

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
        joystick = (JoystickView) findViewById(R.id.joystickView);
        joystick1 = (JoystickView) findViewById(R.id.joystickView1);
        takeoff=findViewById(R.id.TakeOff);
        land=findViewById(R.id.Land);
        streamon=findViewById(R.id.StremOn);
        //textureView=findViewById(R.id.textureView);
        stopffmpegtask=findViewById(R.id.stop_ffmpeg_task);
        joystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onMove(int angle, int strength) {
                // double theta = angle * PI / 180;
                // int dx = (int) (strength*cos(theta));
                // int dy = (int) (strength * sin(theta));
          //     a = (joystick.getNormalizedX() - 50) * 2;
          //     b = -(joystick.getNormalizedY() - 50) * 2;
          //     // a=dx;
          //     // b=dy;
          //     // Log.d("dx joystic1", String.valueOf(dx));
          //     // Log.d("dy joystic1", String.valueOf(dx));
          //     d = (joystick1.getNormalizedX() - 50) * 2;
          //     c = -(joystick1.getNormalizedY() - 50) * 2;
          //     rc();
                sendControllerUpdate();
            }
        });
        joystick1.setOnMoveListener(new JoystickView.OnMoveListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onMove(int angle, int strength) {
             //   a = (joystick.getNormalizedX() - 50) * 2;
             //   b = -(joystick.getNormalizedY() - 50) * 2;
             //   //double theta = angle * PI /180;
             //   //int dx = (int) (100*cos(theta));
             //   //int dy = (int) (100 * sin(theta));
             //   d = (joystick1.getNormalizedX() - 50) * 2;
             //   c = -(joystick1.getNormalizedY() - 50) * 2;
                sendControllerUpdate();
                //d=dx;
                //c=dy;
                //Log.d("strength joystic2", String.valueOf(strength));
                //Log.d("dx joystic2", String.valueOf(dx));
                //Log.d("dy joystic2", String.valueOf(dx));
               // rc();

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
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void sendControllerUpdate()
    {

        float boost = 0.0f;


        //var limit = 1.0f;//Slow down while testing.
        //rx = rx * limit;
        //ry = ry * limit;

        float rx =(joystick.getNormalizedX() - 50) * 2;
        float ry =-(joystick.getNormalizedY() - 50) * 2;
        float lx =(joystick1.getNormalizedX() - 50) * 2;
        float ly = -(joystick1.getNormalizedY() - 50) * 2;

        //Console.WriteLine(controllerState.rx + " " + controllerState.ry + " " + controllerState.lx + " " + controllerState.ly + " SP:"+boost);
        byte[] packet = createJoyPacket(rx, ry, lx, ly, boost);

            SendOneBytePacketWithoutReplay sendOneBytePacketWithoutReplay=new SendOneBytePacketWithoutReplay();
            sendOneBytePacketWithoutReplay.execute(packet);





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


    public class SendOneBytePacketWithoutReplay extends AsyncTask<byte[], String, String> {



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
            String connectstring= "conn_req:\\x00\\x00";

            byte[] connectPacket =connectstring.getBytes(StandardCharsets.UTF_8);
            connectPacket[connectPacket.length - 2] = (byte) 0x96;
            connectPacket[connectPacket.length - 1] = (byte) 0x17;
            SendOneCommandwithoutreplay sendOneCommand = new SendOneCommandwithoutreplay();
            sendOneCommand.doInBackground("command");


        } catch (Exception e) {
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private static byte[] createJoyPacket(float fRx, float fRy, float fLx, float fLy, float speed)
    {
        //template joy packet.
       byte[] packet = new byte[] {(byte) 0xcc, (byte) 0xb0, 0x00, 0x7f, 0x60, 0x50, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x12, 0x16, 0x01, 0x0e, 0x00, 0x25, 0x54 };

        short axis1 = (short)(660.0F * fRx + 1024.0F);//RightX center=1024 left =364 right =-364
        short axis2 = (short)(660.0F * fRy + 1024.0F);//RightY down =364 up =-364
        short axis3 = (short)(660.0F * fLy + 1024.0F);//LeftY down =364 up =-364
        short axis4 = (short)(660.0F * fLx + 1024.0F);//LeftX left =364 right =-364
        short axis5 = (short)(660.0F * speed + 1024.0F);//Speed.

        if (speed > 0.1f)
            axis5 = 0x7fff;

        long packedAxis = ((long)axis1 & 0x7FF) | (((long)axis2 & 0x7FF) << 11) | ((0x7FF & (long)axis3) << 22) | ((0x7FF & (long)axis4) << 33) | ((long)axis5 << 44);
        packet[9] = ((byte)(int)(0xFF & packedAxis));
        packet[10] = ((byte)(int)(packedAxis >> 8 & 0xFF));
        packet[11] = ((byte)(int)(packedAxis >> 16 & 0xFF));
        packet[12] = ((byte)(int)(packedAxis >> 24 & 0xFF));
        packet[13] = ((byte)(int)(packedAxis >> 32 & 0xFF));
        packet[14] = ((byte)(int)(packedAxis >> 40 & 0xFF));

        //Add time info.

        packet[15] = (byte)Calendar.getInstance().get(Calendar.HOUR);;
        packet[16] = (byte)Calendar.getInstance().get(Calendar.MINUTE);
        packet[17] = (byte)Calendar.getInstance().get(Calendar.SECOND);
        packet[18] = (byte)(Calendar.getInstance().getTimeInMillis() & 0xff);
        packet[19] = (byte)(Calendar.getInstance().getTimeInMillis() >> 8);

        CRC.calcUCRC(packet, 4);//Not really needed.

        //calc crc for packet.
        CRC.calcCrc(packet, packet.length);

        return packet;
    }

    private static void setPacketSequence(byte[] packet)
    {
        packet[7] = (byte)(sequence & 0xff);
        packet[8] = (byte)((sequence >> 8) & 0xff);
        sequence++;
    }
    private static void setPacketCRCs(byte[] packet)
    {
        CRC.calcUCRC(packet, 4);
        CRC.calcCrc(packet, packet.length);
    }
    public void takeoff() {


        try {



         //   SendOneCommandwithoutreplay sendOneCommand = new SendOneCommandwithoutreplay();
         //   sendOneCommand.doInBackground("takeoff");
            byte[] packet = new byte[] {(byte) 0xcc, 0x58, 0x00, 0x7c, 0x68, 0x54, 0x00, (byte) 0xe4, 0x01, (byte) 0xc2, 0x16 };
            setPacketSequence(packet);
            setPacketCRCs(packet);
            SendOneBytePacketWithoutReplay sendOneBytePacketWithoutReplay = new SendOneBytePacketWithoutReplay();
            sendOneBytePacketWithoutReplay.execute(packet);


        } catch (Exception e) {
        }
    }

    void handle(byte[] message) {
        try {
            //   ByteBuffer bb =message; // Your frame data is stored in this buffer
            //   Picture real = decoder.decodeFrame(bb, out.getData());
            //   Bitmap bi = AndroidUtil.toBitmap(real); // If you prefere AWT image
            //   imageView.setImageBitmap(bi);
            DecoderView imageView=findViewById(R.id.decoderView);
            decoderView.decode();
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
            DatagramPacket packet = new DatagramPacket(lmessage, lmessage.length);

            try {


                while(bKeepRunning) {
                    socketStreamOnServer.receive(packet);
                    Log.d("video length", String.valueOf(new String(lmessage, 0, packet.getLength()).trim().length()));

                    try{
                        if(packet.getData()!=null){
                                    DecoderView imageView=findViewById(R.id.decoderView);
                                    imageView.decode();
                        }else{
                            Log.d("video packet","video packet is null");
                        }

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


