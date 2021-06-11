package com.gal.tello;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.Application;
import android.app.usage.ExternalStorageStats;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.text.format.Formatter;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;


import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;

import java.io.Console;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import io.github.controlwear.virtual.joystick.android.JoystickView;
//import nl.bravobit.ffmpeg.ExecuteBinaryResponseHandler;
//import nl.bravobit.ffmpeg.FFmpeg;
//import nl.bravobit.ffmpeg.FFtask;

import static java.lang.Math.max;


import java.lang.*;


public class MainActivity extends AppCompatActivity {
    VideoDatagramReceiver videoDatagramReceiver;
    public static final int TELLO_CAM_LISTEN_PORT = 11111;
    Boolean VideoStarted = false;
    Button streamon;
    connectionlistener connectionlistener;
    boolean isstreamon = false;
    Boolean connected = false;
    Button takeoff;
    Button land;
    public boolean isPaused = false;
    private static int sequence = 1;
    private MediaCodec m_codec;// Media decoder
    static DatagramSocket socketMainSending;
    static InetAddress inetAddressMainSending;
    public static final int portMainSending = 8889;
    public static final String addressMainSending = "192.168.10.1";
    int picMode;
    DatagramSocket socketStatusServer;
    DatagramSocket socketStreamOnServer;
    static Activity activity;
    int speed = 1;
    static ControllerState controllerState;
    static JoystickView joystickr;
    static JoystickView joystickl;
    int iFrameRate = 10;
    HeartBeatStreamon heartBeatStreamon;
    HeartBeatJoystick heartBeatJoystick;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        activity = (Activity) MainActivity.this;
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        joystickr = (JoystickView) findViewById(R.id.joystickView);
        joystickl = (JoystickView) findViewById(R.id.joystickView1);
        takeoff = findViewById(R.id.TakeOff);
        land = findViewById(R.id.Land);
        streamon = findViewById(R.id.StremOn);
        //textureView=findViewById(R.id.textureView);
        controllerState = new ControllerState();
        BroadcastReceiver broadcastReceiver = new WifiBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        intentFilter.addAction(WifiManager.EXTRA_SUPPLICANT_CONNECTED);
        registerReceiver(broadcastReceiver, intentFilter);
        connectionlistener = new connectionlistener();
        heartBeatStreamon = new HeartBeatStreamon();
        heartBeatJoystick = new HeartBeatJoystick();

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
        StartDroneConnection();
        videoDatagramReceiver = new VideoDatagramReceiver();
    }

    void StartDroneConnection() {
        // final Handler ha = new Handler();
        // ha.postDelayed(new Runnable() {
//
        //     @Override
        //     public void run() {
        //         //call function
        //         if (connected == false) {
        //             if (lastreplay != null) {
        //                 Log.d("connected", "connected");
        //                 connected = true;
        //                 setAttAngle(25.0f);
        //                 StartHeartBeatJoystick();
        //                 streamon();
        //             } else {
        //                 Initialize();
        //                 connect();
//
        //             }
//
//
        //             ha.postDelayed(this, 1000);
        //         }
        //     }
//
        // }, 0);

        try {
            if (!connectionlistener.isAlive()) {
                connectionlistener.join();
            connectionlistener=new connectionlistener();}
                connectionlistener.start();//start listening for tello

        }catch (RuntimeException | InterruptedException e){
           e.printStackTrace();
        }


    }

    public class WifiBroadcastReceiver extends BroadcastReceiver {//reconnect after disconnection

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("Wifi Action", action);



                connected = false;
                StartDroneConnection();

        }

        /**
         * Detect you are connected to a specific network.
         */
        private boolean checkConnectedToDesiredWifi() {
            boolean wificonnected = false;

            String desiredMacAddress = "router mac address";

            WifiManager wifiManager =
                    (WifiManager) MainActivity.this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

            WifiInfo wifi = wifiManager.getConnectionInfo();
            if (wifi != null) {
                // get current router Mac address
                //String bssid = wifi.getBSSID();
                //connected = desiredMacAddress.equals(bssid);
                wificonnected = true;
            }

            return wificonnected;
        }
    }

    class connectionlistener extends Thread {

        @Override
        public void run() {
            Log.d("connecting", "connecting to tello");
            while (!connected) {
                try {
                WifiManager wifiManager = (WifiManager) getApplication().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if(mWifi.isConnected()){
               String ip = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
               if (ip.startsWith("192.168.10.")) {
                    if (connect().contains("ack")) {
                        Log.d("connected", "connected");
                        connected = true;
                        setAttAngle(25.0f);
                        StartHeartBeatJoystick();
                        streamon();
                    }
                }}


                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //public class SendOneCommandwithoutreplay extends AsyncTask {
//
//
    //    @Override
    //    protected Object doInBackground(Object[] objects) {
    //        if(connected==false){
    //            DatagramPacket packet = new DatagramPacket(buf, buf.length, inetAddressMainSending, portMainSending);
    //        }
    //        return null;
    //    }
    //}


    @RequiresApi(api = Build.VERSION_CODES.O)
    static void setcontrolleraxis() {
        float deadBand = 0.15f;
        DecimalFormat df = new DecimalFormat("#.#");
        float[] r = ellipticalDiscToSquare((((float) joystickr.getNormalizedX() - 50.0f) / 50), (((float) joystickr.getNormalizedY() - 50.0f) / 50));
        float[] l = ellipticalDiscToSquare((((float) joystickl.getNormalizedX() - 50.0f) / 50), (((float) joystickl.getNormalizedY() - 50.0f) / 50));
        float rx = Float.parseFloat(df.format(Math.abs(r[0]) < deadBand ? 0.0f : r[0]));//(((float)joystickr.getNormalizedX()-50.0f)/50);
        float ry = Float.parseFloat(df.format(Math.abs(r[1]) < deadBand ? 0.0f : r[1]));//(((float)joystickr.getNormalizedY()-50.0f)/50);
        float lx = Float.parseFloat(df.format(Math.abs(l[0]) < deadBand ? 0.0f : l[0]));//(((float)joystickl.getNormalizedX()-50.0f)/50);
        float ly = Float.parseFloat(df.format(Math.abs(l[1]) < deadBand ? 0.0f : l[1]));//(((float)joystickl.getNormalizedY()-50.0f)/50);
        // Log.d("joystick", "rx: " + rx + " " + "ry: " + ry + " " + "lx: " + lx + " " + "ly: " + ly);
        //d=dx;
        //c=dy;
        //Log.d("strength joystic2", String.valueOf(strength));
        //Log.d("dx joystic2", String.valueOf(dx));
        //Log.d("dy joystic2", String.valueOf(dx));
        //rc();
        controllerState.setSpeedMode(1);
        controllerState.setAxis(lx, -ly, rx, -ry);
        sendControllerUpdate();
    }

    public static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }


    public static float[] ellipticalDiscToSquare(double u, double v) {
        float[] ans = new float[2];
        double u2 = u * u;
        double v2 = v * v;
        double twosqrt2 = 2.0 * Math.sqrt(2.0);
        double subtermx = 2.0 + u2 - v2;
        double subtermy = 2.0 - u2 + v2;
        double termx1 = subtermx + u * twosqrt2;
        double termx2 = subtermx - u * twosqrt2;
        double termy1 = subtermy + v * twosqrt2;
        double termy2 = subtermy - v * twosqrt2;
        ans[0] = (float) (0.5 * Math.sqrt(termx1) - 0.5 * Math.sqrt(termx2));
        ans[1] = (float) (0.5 * Math.sqrt(termy1) - 0.5 * Math.sqrt(termy2));
        return ans;


    }

    public void Initialize() {

        try {
            socketMainSending = new DatagramSocket();
            inetAddressMainSending = getInetAddressByName(addressMainSending);
            if (inetAddressMainSending == null) {

            } else {

            }


        } catch (IOException e) {
            Log.e("IOException", e.toString());
        }


    }

    public static boolean isPortOpen(final String ip, final int port, final int timeout) {

        try {

            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(8890), timeout);
            socket.close();
            return true;
        } catch (ConnectException ce) {
            ce.printStackTrace();
            return false;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
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
                Log.e("IOException", e.getMessage());

            } catch (Exception e) {
                Log.e("Exception", e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
        }
    }

    void setVideoBitRate(int rate) {
        //                                          crc    typ  cmdL  cmdH  seqL  seqH  rateL  crc   crc
        byte[] packet = new byte[]{(byte) 0xcc, 0x60, 0x00, 0x27, 0x68, 0x20, 0x00, 0x09, 0x00, 0x00, 0x5b, (byte) 0xc5};

        //payload
        packet[9] = (byte) rate;

        setPacketSequence(packet);
        setPacketCRCs(packet);

        SendOneBytePacketWithoutReplay sendOneBytePacketWithoutReplay = new SendOneBytePacketWithoutReplay();
        sendOneBytePacketWithoutReplay.execute(packet);
    }

    void setVideoDynRate(int rate) {
        //                                          crc    typ  cmdL  cmdH  seqL  seqH  rateL  crc   crc
        byte[] packet = new byte[]{(byte) 0xcc, 0x60, 0x00, 0x27, 0x68, 0x21, 0x00, 0x09, 0x00, 0x00, 0x5b, (byte) 0xc5};

        //payload
        packet[9] = (byte) rate;

        setPacketSequence(packet);
        setPacketCRCs(packet);

        SendOneBytePacketWithoutReplay sendOneBytePacketWithoutReplay = new SendOneBytePacketWithoutReplay();
        sendOneBytePacketWithoutReplay.execute(packet);
    }

    void setVideoRecord(int n) {
        //                                          crc    typ  cmdL  cmdH  seqL  seqH  nL  crc   crc
        byte[] packet = new byte[]{(byte) 0xcc, 0x60, 0x00, 0x27, 0x68, 0x32, 0x00, 0x09, 0x00, 0x00, 0x5b, (byte) 0xc5};

        //payload
        packet[9] = (byte) n;

        setPacketSequence(packet);
        setPacketCRCs(packet);

        SendOneBytePacketWithoutReplay sendOneBytePacketWithoutReplay = new SendOneBytePacketWithoutReplay();
        sendOneBytePacketWithoutReplay.execute(packet);
    }

    void queryAttAngle() {
        byte[] packet = new byte[]{(byte) 0xcc, 0x58, 0x00, 0x7c, 0x48, 0x59, 0x10, 0x06, 0x00, (byte) 0xe9, (byte) 0xb3};
        setPacketSequence(packet);
        setPacketCRCs(packet);
        SendOneBytePacketWithoutReplay sendOneBytePacketWithoutReplay = new SendOneBytePacketWithoutReplay();
        sendOneBytePacketWithoutReplay.execute(packet);
    }

    void setAttAngle(float angle) {
        //                                          crc    typ  cmdL  cmdH  seqL  seqH  ang1  ang2 ang3  ang4  crc   crc
        byte[] packet = new byte[]{(byte) 0xcc, 0x78, 0x00, 0x27, 0x68, 0x58, 0x10, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x5b, (byte) 0xc5};

        //payload
        byte[] bytes = ByteBuffer.allocate(8).putFloat(angle).array();
        packet[9] = bytes[0];
        packet[10] = bytes[1];
        packet[11] = bytes[2];
        packet[12] = bytes[3];

        setPacketSequence(packet);
        setPacketCRCs(packet);

        SendOneBytePacketWithoutReplay sendOneBytePacketWithoutReplay = new SendOneBytePacketWithoutReplay();
        sendOneBytePacketWithoutReplay.execute(packet);

        queryAttAngle();//refresh
    }

    /*TELLO_CMD_SWITCH_PICTURE_VIDEO
    49 0x31
    0x68
    switching video stream mode
    data: u8 (1=video, 0=photo)
    */
    void setPicVidMode(int mode) {
        //                                          crc    typ  cmdL  cmdH  seqL  seqH  modL  crc   crc
        byte[] packet = new byte[]{(byte) 0xcc, 0x60, 0x00, 0x27, 0x68, 0x31, 0x00, 0x00, 0x00, 0x00, 0x5b, (byte) 0xc5};

        picMode = mode;

        //payload
        packet[9] = (byte) (mode & 0xff);

        setPacketSequence(packet);
        setPacketCRCs(packet);

        SendOneBytePacketWithoutReplay sendOneBytePacketWithoutReplay = new SendOneBytePacketWithoutReplay();
        sendOneBytePacketWithoutReplay.execute(packet);
    }

    void setEV(int ev) {
        //                                          crc    typ  cmdL  cmdH  seqL  seqH  evL  crc   crc
        byte[] packet = new byte[]{(byte) 0xcc, 0x60, 0x00, 0x27, 0x68, 0x34, 0x00, 0x09, 0x00, 0x00, 0x5b, (byte) 0xc5};

        byte evb = (byte) (ev - 9);//Exposure goes from -9 to +9
        //payload
        packet[9] = evb;

        setPacketSequence(packet);
        setPacketCRCs(packet);

        SendOneBytePacketWithoutReplay sendOneBytePacketWithoutReplay = new SendOneBytePacketWithoutReplay();
        sendOneBytePacketWithoutReplay.execute(packet);
    }

    void requestIframe() {
        byte[] iframePacket = new byte[]{(byte) 0xcc, 0x58, 0x00, 0x7c, 0x60, 0x25, 0x00, 0x00, 0x00, 0x6c, (byte) 0x95};
        SendOneBytePacketWithoutReplay sendOneBytePacketWithoutReplay = new SendOneBytePacketWithoutReplay();
        sendOneBytePacketWithoutReplay.execute(iframePacket);

    }

    public static class SendOneBytePacketWithoutReplay extends AsyncTask<byte[], String, String> {


        @Override
        public String doInBackground(byte[]... bytes) {
            byte[] buf = bytes[0];
            DatagramPacket packet = new DatagramPacket(buf, buf.length, inetAddressMainSending, portMainSending);
            try {
                socketMainSending.send(packet);

            } catch (IOException e) {
                Log.e("IOException", e.getMessage());

            } catch (Exception e) {
                Log.e("Exception", e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
        }
    }

    public class SendOneBytePacket extends AsyncTask<byte[], String, String> {


        @Override
        protected String doInBackground(byte[]... bytes) {
            byte[] buf = bytes[0];
            DatagramPacket packet = new DatagramPacket(buf, buf.length, inetAddressMainSending, portMainSending);
            String doneText = "";
            try {
                socketMainSending.send(packet);
                buf = new byte[500];
                packet = new DatagramPacket(buf, buf.length);
                socketMainSending.setSoTimeout(500);
                socketMainSending.receive(packet);
                if (packet.getLength() != 0) {
                    doneText = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                    Log.d("message", doneText);
                }


            } catch (IOException e) {
                Log.e("IOException", e.getMessage());

            } catch (Exception e) {
                Log.e("Exception", e.getMessage());
            }
            return doneText;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
        }
    }

    public String connect() {

        String replay = "";
        try {
            Initialize();
            //InetSocketAddress addressStatus = new InetSocketAddress(8890);
            //socketMainSending.connect(addressStatus);
            // if(!socketMainSending.isConnected()){
            // socketMainSending.connect(inetAddressMainSending,3000);}

            String connectstring = "conn_req:lh";

            byte[] connectPacket = connectstring.getBytes(StandardCharsets.UTF_8);
            connectPacket[9] = (byte) (6038 & 0x96);
            connectPacket[10] = 6038 >> 8;
            Log.d("connect packet", new String(connectPacket, StandardCharsets.UTF_8));
            SendOneBytePacket sendOneBytePacket = new SendOneBytePacket();
            replay = sendOneBytePacket.execute(connectPacket).get();
            Log.d("connect replay", replay);


        } catch (Exception e) {
            e.printStackTrace();
        }
        return replay;
    }

    public void takeoff() {


        try {


            //   SendOneCommandwithoutreplay sendOneCommand = new SendOneCommandwithoutreplay();
            //   sendOneCommand.doInBackground("takeoff");
            byte[] packet = new byte[]{(byte) 0xcc, 0x58, 0x00, 0x7c, 0x68, 0x54, 0x00, (byte) 0xe4, 0x01, (byte) 0xc2, 0x16};
            setPacketSequence(packet);
            setPacketCRCs(packet);
            SendOneBytePacketWithoutReplay sendOneBytePacketWithoutReplay = new SendOneBytePacketWithoutReplay();
            sendOneBytePacketWithoutReplay.execute(packet);


        } catch (Exception e) {
        }
    }

    private static void setPacketSequence(byte[] packet) {
        packet[7] = (byte) (sequence & 0xff);
        packet[8] = (byte) ((sequence >> 8) & 0xff);
        sequence++;
    }

    private static void setPacketCRCs(byte[] packet) {
        CRC.calcUCRC(packet, 4);
        CRC.calcCrc(packet, packet.length);
    }


    public static float Clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    static void sendControllerUpdate() {
        //if (!connected)
        //    return;

        float boost = 0.0f;
        if (controllerState.speed > 0)
            boost = 1.0f;

        //var limit = 1.0f;//Slow down while testing.
        //rx = rx * limit;
        //ry = ry * limit;
        float rx = controllerState.rx;
        float ry = controllerState.ry;
        float lx = controllerState.lx;
        float ly = controllerState.ly;
        // if (true)//Combine autopilot sticks.
        // {
        //     rx = Clamp(rx + autoPilotControllerState.rx, -1.0f, 1.0f);
        //     ry = Clamp(ry + autoPilotControllerState.ry, -1.0f, 1.0f);
        //     lx = Clamp(lx + autoPilotControllerState.lx, -1.0f, 1.0f);
        //     ly = Clamp(ly + autoPilotControllerState.ly, -1.0f, 1.0f);
        // }
        //Console.WriteLine(controllerState.rx + " " + controllerState.ry + " " + controllerState.lx + " " + controllerState.ly + " SP:"+boost);
        byte[] packet = createJoyPacket(rx, ry, lx, ly, boost);
        try {
            SendOneBytePacketWithoutReplay sendOneBytePacketWithoutReplay = new SendOneBytePacketWithoutReplay();
            sendOneBytePacketWithoutReplay.execute(packet);
        } catch (Exception ex) {

        }
    }

    public class ControllerState {
        public float rx, ry, lx, ly;
        public int speed;
        public double deadBand = 0.15;

        public void setAxis(float lx, float ly, float rx, float ry) {
            //var deadBand = 0.15f;
            //this.rx = Math.Abs(rx) < deadBand ? 0.0f : rx;
            //this.ry = Math.Abs(ry) < deadBand ? 0.0f : ry;
            //this.lx = Math.Abs(lx) < deadBand ? 0.0f : lx;
            //this.ly = Math.Abs(ly) < deadBand ? 0.0f : ly;

            this.rx = rx;
            this.ry = ry;
            this.lx = lx;
            this.ly = ly;

            //Console.WriteLine(rx + " " + ry + " " + lx + " " + ly + " SP:" + speed);
        }

        public void setSpeedMode(int mode) {
            speed = mode;

            //Console.WriteLine(rx + " " + ry + " " + lx + " " + ly + " SP:" + speed);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    static byte[] createJoyPacket(float fRx, float fRy, float fLx, float fLy, float speed) {
        //template joy packet.
        byte[] packet = new byte[]{(byte) 0xcc, (byte) 0xb0, 0x00, 0x7f, 0x60, 0x50, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x12, 0x16, 0x01, 0x0e, 0x00, 0x25, 0x54};

        short axis1 = (short) (660.0F * fRx + 1024.0F);//RightX center=1024 left =364 right =-364
        short axis2 = (short) (660.0F * fRy + 1024.0F);//RightY down =364 up =-364
        short axis3 = (short) (660.0F * fLy + 1024.0F);//LeftY down =364 up =-364
        short axis4 = (short) (660.0F * fLx + 1024.0F);//LeftX left =364 right =-364
        short axis5 = (short) (660.0F * speed + 1024.0F);//Speed.

        if (speed > 0.1f)
            axis5 = 0x7fff;

        long packedAxis = ((long) axis1 & 0x7FF) | (((long) axis2 & 0x7FF) << 11) | ((0x7FF & (long) axis3) << 22) | ((0x7FF & (long) axis4) << 33) | ((long) axis5 << 44);
        packet[9] = ((byte) (int) (0xFF & packedAxis));
        packet[10] = ((byte) (int) (packedAxis >> 8 & 0xFF));
        packet[11] = ((byte) (int) (packedAxis >> 16 & 0xFF));
        packet[12] = ((byte) (int) (packedAxis >> 24 & 0xFF));
        packet[13] = ((byte) (int) (packedAxis >> 32 & 0xFF));
        packet[14] = ((byte) (int) (packedAxis >> 40 & 0xFF));

        //Add time info.
        LocalDateTime now = LocalDateTime.now();
        packet[15] = (byte) now.getHour();
        packet[16] = (byte) now.getMinute();
        packet[17] = (byte) now.getSecond();
        packet[18] = (byte) (now.getNano() & 0xff);
        packet[19] = (byte) ((now.getNano() * 1000000) >> 8);

        CRC.calcUCRC(packet, 4);//Not really needed.

        //calc crc for packet.
        CRC.calcCrc(packet, packet.length);

        return packet;
    }


    void StartRecivingVideoStream() {
        try {
            if (isstreamon == false) {
                socketStreamOnServer = new DatagramSocket(null);
                InetSocketAddress addressVideo = new InetSocketAddress(6038);
                socketStreamOnServer.bind(addressVideo);
                if (!videoDatagramReceiver.isAlive()) {
                    try {
                        videoDatagramReceiver.start();//start listening for tello
                    }catch (RuntimeException e){

                    }
                }
                isstreamon = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void streamon() {


        try {


            //  SendOneCommandwithoutreplay sendOneCommand = new SendOneCommandwithoutreplay();
            //  sendOneCommand.doInBackground("streamon");
            // setPicVidMode(0);
            // setVideoBitRate(2);
            // setVideoDynRate(1);
            // setEV(0);

            // SendOneCommandwithoutreplay sendOneCommandwithoutreplay = new SendOneCommandwithoutreplay();
            // sendOneCommandwithoutreplay.execute("streamon");
            StartRecivingVideoStream();
            StartHeartBeatStreamOn();


            //  requestIframe();


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

    public static byte[] addAll(final byte[] array1, byte[] array2) {
        if (array1 == null) {
            byte[] joinedArray = array2;
            return joinedArray;
        } else if (array2 == null) {
            byte[] joinedArray = array1;
            return joinedArray;
        } else {
            byte[] joinedArray = Arrays.copyOf(array1, array1.length + array2.length);
            System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
            return joinedArray;
        }
    }

    private class VideoDatagramReceiver extends Thread {
        private boolean bKeepRunning = true;
        public byte[] lmessage = new byte[1460];
        byte[] videoFrame = new byte[100 * 1024];
        int videoOffset = 0;


        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void run() {
            Log.d("video start", "start");
            DatagramPacket packet = new DatagramPacket(lmessage, lmessage.length);

            try {


                while (bKeepRunning) {

                    socketStreamOnServer.receive(packet);
                    byte[] data = new byte[packet.getLength()];
                    System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());


                    try {
                        if (data[2] == 0 && data[3] == 0 && data[4] == 0 && data[5] == 1) {

                            if (videoOffset > 0) {
                                if (!isPaused) {
                                    sendControllerUpdate();
                                    DecoderView imageView = findViewById(R.id.decoderView);
                                    byte[] videoFramenew = new byte[videoOffset];
                                    System.arraycopy(videoFrame, 0, videoFramenew, 0, videoOffset);
                                    imageView.decode(videoFramenew);
                                    // b= imageView.getBitmap();
                                    // b.compress(Bitmap.CompressFormat.PNG,0, new FileOutputStream(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString()+System.nanoTime()+"/tello.png") );
                                    videoOffset = 0;
                                    // videoFrame = new byte[1460 * 100];
                                }
                            }
                        }
                        System.arraycopy(data, 2, videoFrame, videoOffset, data.length - 2);
                        videoOffset += (data.length - 2);

                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }


                }

                if (socketStreamOnServer == null) {
                    socketStreamOnServer.close();
                }

            } catch (IOException ioe) {

            }

        }

        public void kill() {
            bKeepRunning = false;
        }
    }


    class HeartBeatStreamon extends Thread {

        boolean bKeepRunning = true;

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void run() {
            int tick = 0;

            while (bKeepRunning) {
                tick++;
                if ((tick % iFrameRate) == 0) {
                    requestIframe();
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


            }


        }

        public void kill() {
            bKeepRunning = false;
        }

    }

    void StartHeartBeatStreamOn() {
        if (!heartBeatStreamon.isAlive()) {
            try {
                heartBeatStreamon.start();//start listening for tello
            }catch (RuntimeException e){

            }
        }


    }


    class HeartBeatJoystick extends Thread {

        boolean bKeepRunning = true;

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void run() {

            while (bKeepRunning) {
                if (!isPaused) {
                    setcontrolleraxis();
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


            }


        }

        public void kill() {
            bKeepRunning = false;
        }

    }

    void StartHeartBeatJoystick() {
        if (!heartBeatJoystick.isAlive()) {
            try {
                heartBeatJoystick.start();//start listening for tello
            }catch (RuntimeException e){

            }
        }


    }


    // public void rc() {

    //    /* if(a==70&&b==-70&&c==-70&&d==-70){
    //     Log.d("startmotors","motorstart");}
    //     else{*/
    //     SendOneCommandwithoutreplay sendOneCommand = new SendOneCommandwithoutreplay();
    //     sendOneCommand.doInBackground("rc " + a + " " + b + " " + c + " " + d);
    //     // }
    // }

    private class StatusDatagramReceiver extends Thread {
        private boolean bKeepRunning = true;
        private String lastMessage = "";

        @Override
        public void run() {
            String message;
            byte[] lmessage = new byte[500];
            DatagramPacket packet = new DatagramPacket(lmessage, lmessage.length);

            try {

                while (bKeepRunning) {
                    socketStatusServer.receive(packet);
                    message = new String(lmessage, 0, packet.getLength());
                    lastMessage = message;
                    Log.d("message", message);
                }

                if (socketStatusServer == null) {
                    socketStatusServer.close();
                }

            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

        }

        public void kill() {
            bKeepRunning = false;
        }
    }

    private class MessageDatagramReceiver extends Thread {
        private boolean bKeepRunning = true;
        private String lastMessage = "";

        //@Override
        //public void run() {
        //    String message;
        //    byte[] lmessage = new byte[500];
        //    DatagramPacket packet = new DatagramPacket(lmessage, lmessage.length);
//
        //    try {
//
        //        while(bKeepRunning) {
        //            socketStatusServer.receive(packet);
        //            message = new String(lmessage, 0, packet.getLength());
        //            lastMessage = message;
        //            Log.d("message",message);
        //            Thread.sleep(200);
//
        //        }
//
        //        if (socketStatusServer == null) {
        //            socketStatusServer.close();
        //        }
//
        //    } catch (IOException | InterruptedException ioe){
//
        //    }
//
        //}
        @Override
        public void run() {
            Log.d("video start", "start");
            byte[] lmessage = new byte[500];
            DatagramPacket packet = new DatagramPacket(lmessage, lmessage.length);
            String message = "";
            try {
                while (bKeepRunning) {
                    socketStatusServer.receive(packet);
                    try {
                        socketStatusServer.receive(packet);
                        message = new String(lmessage, 0, packet.getLength());
                        lastMessage = message;
                        Log.d("message", message);

                    } catch (RuntimeException e) {
                        Log.e("error", e.toString());
                    }
                }
                if (socketStatusServer == null) {
                    socketStatusServer.close();
                }
            } catch (IOException ioe) {
            }
        }

        public void kill() {
            bKeepRunning = false;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        DecoderView decoderView = findViewById(R.id.decoderView);
        decoderView.stop();
        isPaused = true;
        controllerState.setAxis(0, 0, 0, 0);
        //    try {
        //        VideoDatagramReceiver videoDatagramReceiver = new VideoDatagramReceiver();
        //        videoDatagramReceiver.kill();}catch (Exception e){
        //        e.printStackTrace();}
    }

    @Override
    public void onResume() {
        super.onResume();
        isPaused = false;
        // StartRecivingVideoStream();


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