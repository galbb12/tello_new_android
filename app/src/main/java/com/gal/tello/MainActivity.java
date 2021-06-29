package com.gal.tello;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PointF;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.text.format.Formatter;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;


import io.github.controlwear.virtual.joystick.android.JoystickView;
import nl.bravobit.ffmpeg.ExecuteBinaryResponseHandler;
import nl.bravobit.ffmpeg.FFmpeg;


import static java.lang.Math.max;


import java.lang.*;


public class MainActivity extends AppCompatActivity {
    byte[] picbuffer = new byte[3000 * 1024];
    boolean[] picPieceState;
    File h264FilePath;
    File videoFilePath;
    VideoDatagramReceiver videoDatagramReceiver;
    StatusDatagramReceiver statusDatagramReceiver;
    connectionlistener connectionlistener;
    boolean isstreamon = false;
    Boolean connected = false;
    Button takeoff;
    Button connect;
    Button returntohome;
    Button takepicture;
    Button button_record;
    boolean[] picChunkState;
    TextView textViewBattery;
    TextView textViewTemp;
    TextView textViewRotation;
    TextView textViewPosition;
    int picBytesRecived;
    int height;
    int northSpeed;
    int eastSpeed;
    int flySpeed;
    int verticalSpeed;
    int flyTime;
    int picBytesExpected;
    int maxPieceNum = 0;
    int picExtraPackets;
    boolean picDownloading = false;
    boolean imuState;
    boolean pressureState;
    boolean downVisualState;
    boolean powerState;
    boolean batteryState;
    boolean gravityState;
    boolean windState;
    int imuCalibrationState;
    int batteryPercentage;
    int droneFlyTimeLeft;
    int droneBatteryLeft;
    boolean flying;
    boolean onGround;
    boolean eMOpen;
    boolean droneHover;
    boolean outageRecording;
    boolean batteryLow;
    boolean batteryLower;
    boolean factoryMode;
    int flyMode;
    int throwFlyTimer;
    int cameraState;
    int electricalMachineryState;
    boolean frontIn;
    boolean frontOut;
    boolean frontLSC;
    int temperatureHeight;
    int wifiStrength;
    public boolean isPaused = false;
    private static int sequence = 1;
    static DatagramSocket socketMainSending;
    static InetAddress inetAddressMainSending;
    public static final int portMainSending = 8889;
    public static final String addressMainSending = "192.168.10.1";
    int picMode;
    DatagramSocket socketStreamOnServer;
    static Activity activity;
    int bitrate = 2;
    static ControllerState controllerState;
    static ControllerState AutoPilotControllerState;
    static JoystickView joystickr;
    static JoystickView joystickl;
    View joystickviewlocatorL;
    View joystickviewlocatorR;

    float iFrameRate = 4f;
    Float posX=0.0f;
    Float posY=0.0f;
    Float posZ=0.0f;
    Float posUncertainty=0.0f;
    double[] eular;
    HeartBeatStreamon heartBeatStreamon;
    HeartBeatJoystick heartBeatJoystick;
    BitConverter bitConverter;
    Boolean record=false;
    FileOutputStream fos;
    boolean bHomePointSet=true;
    boolean bAutopilot=false;
    boolean bLookAtTargetSet =false;
    boolean bLookAt=false;
    PointF lookAtTarget= new PointF(0.0f,0.0f);
    PointF autopilotTarget= new PointF(0.0f,0.0f);
    DecoderView decoderView;
    WifiManager          wifiManager;
    ConnectivityManager connManager ;
    NetworkInfo         mWifi       ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
       // getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        activity = (Activity) this;
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        decoderView=findViewById(R.id.decoderView);
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        joystickviewlocatorL = findViewById(R.id.joystickgotoviewL);
        joystickviewlocatorR     = findViewById(R.id.joystickgotoviewR);
        decoderView.setSystemUiVisibility(uiOptions);
        joystickr = (JoystickView) findViewById(R.id.joystickView);
        joystickl = (JoystickView) findViewById(R.id.joystickView1);
        takeoff = findViewById(R.id.TakeOff);
        button_record = findViewById(R.id.record);
        //connect = findViewById(R.id.connect);
        returntohome = findViewById(R.id.ReturnToHome);
        takepicture = findViewById(R.id.takepicture);
        textViewBattery = findViewById(R.id.textViewbattery);
        textViewTemp = findViewById(R.id.textViewTemp);
        textViewRotation = findViewById(R.id.textViewRotation);
        textViewPosition = findViewById(R.id.textViewPosition);
        controllerState = new ControllerState();
        AutoPilotControllerState = new ControllerState();
        BroadcastReceiver broadcastReceiver = new WifiBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        intentFilter.addAction(WifiManager.EXTRA_SUPPLICANT_CONNECTED);
        registerReceiver(broadcastReceiver, intentFilter);
        connectionlistener = new connectionlistener();
        heartBeatStreamon = new HeartBeatStreamon();
        heartBeatJoystick = new HeartBeatJoystick();
        statusDatagramReceiver = new StatusDatagramReceiver();
        videoDatagramReceiver = new VideoDatagramReceiver();
        bitConverter = new BitConverter();
        wifiManager= (WifiManager) getApplication().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        View.OnTouchListener touchListenerL = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                // save the X,Y coordinates
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    joystickl.setX(v.getX()-v.getWidth()/2+event.getX());
                    joystickl.setY(v.getY()-v.getHeight()/2+event.getY());
                }

                // let the touch event pass on to whoever needs it
                return false;
            }
        };

        View.OnTouchListener joystickmoverL= new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                // save the X,Y coordinates
                if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                    v.setX(joystickviewlocatorL.getX());
                    v.setY(joystickviewlocatorL.getY());
                }

                // let the touch event pass on to whoever needs it
                return false;
            }
        };
        joystickl.setOnTouchListener(joystickmoverL);
        View.OnTouchListener joystickmoverR= new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                // save the X,Y coordinates
                if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                    v.setX(joystickviewlocatorR.getX());
                    v.setY(joystickviewlocatorR.getY());
                }

                // let the touch event pass on to whoever needs it
                return false;
            }
        };
        joystickr.setOnTouchListener(joystickmoverR);
        joystickviewlocatorL.setOnTouchListener(touchListenerL);
        View.OnTouchListener touchListenerR = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                // save the X,Y coordinates
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    joystickr.setX(v.getX()-v.getWidth()/2+event.getX());
                    joystickr.setY(v.getY()-v.getHeight()/2+event.getY());
                }
                if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                    joystickr.setX(v.getX()-v.getWidth()/2);
                    joystickr.setY(v.getY()-v.getHeight()/2);
                }

                // let the touch event pass on to whoever needs it
                return false;
            }
        };
        joystickviewlocatorR.setOnTouchListener(touchListenerR);

        takeoff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takeoff();
            }
        });
        //connect.setOnClickListener(new View.OnClickListener() {
        //    @Override
        //    public void onClick(View view) {
        //        StartDroneConnection();
        //    }
        //});
        StartDroneConnection();
        returntohome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bAutopilot = true;

            }
        });
        takepicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!picDownloading) {
                    takePicture();
                }
            }
        });
        button_record.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View view) {

                if (record == false) {
                    if (connected) {
                        File filechache = new File(Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DCIM), "tello+/cache");
                        if (!filechache.mkdirs()) {

                        }
                        File filevideo = new File(Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DCIM), "tello+/vid");
                        if (!filevideo.mkdirs()) {
                        }


                        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-dd-M--HH-mm-ss");
                        h264FilePath = new File(filechache.getPath() + "/" + LocalDateTime.now().format(format) + ".h264");
                        videoFilePath = new File(filevideo.getPath() + "/" + LocalDateTime.now().format(format) + ".mp4");
                        button_record.setText("Stop recording");
                        fos=null;
                        record = true;
                        Toast.makeText(activity, "Recording to: " + videoFilePath.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                    } }else {

                    ConvertRecording(h264FilePath.getAbsolutePath(), videoFilePath.getAbsolutePath());



                }
            }
        });
    }


    void ConvertRecording(String h264input , String mp4output){

        //    if (FFmpeg.getInstance(this).isSupported()) {
        //        // ffmpeg is supported
        //    } else {
        //        // ffmpeg is not supported
        //    }

        // FFmpeg.doInBackgroundAsync("-y -i "+h264input+" -vcodec copy "+mp4output, new doInBackgroundCallback() {

        //    @Override
        //    public void apply(final long executionId, final int returnCode) {
        //        if (returnCode == RETURN_CODE_SUCCESS) {
        //            Toast.makeText(activity,"File saved to: "+mp4output,Toast.LENGTH_LONG).show();
        //        } else if (returnCode == RETURN_CODE_CANCEL) {
        //            Log.i(Config.TAG, "Async command execution cancelled by user.");
        //        } else {
        //            Log.i(Config.TAG, String.format("Async command execution failed with returnCode=%d.", returnCode));
        //        }
        //    }
        //});

        if (FFmpeg.getInstance(this).isSupported()) {
            FFmpeg ffmpeg = FFmpeg.getInstance(this);
            String[] cmd= {"-y","-i",h264input, "-vcodec","copy" ,mp4output};
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                    ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {

                        @Override
                        public void onStart() {}

                        @Override
                        public void onProgress(String message) {}

                        @Override
                        public void onFailure(String message) {
                            Log.e("ffmpeg error",message);
                        }

                        @Override
                        public void onSuccess(String message) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.activity,"Video saved to: "+mp4output,Toast.LENGTH_LONG).show();
                                }
                            });
                        }

                        @Override
                        public void onFinish() {}

                    });}catch (Exception e){e.printStackTrace();}
                }};
            thread.run();
        } else {
            // ffmpeg is not supported
        }
        record = false;
        button_record.setText("Record");
    }




    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }
    public void setLookAtTarget(PointF target)
    {
        if (flying)
        {
            lookAtTarget = target;
        }
    }
    public void setAutopilotTarget(PointF target)
    {
        if (flying)
        {
            lookAtTarget = target;
        }
    }
    public void cancelAutopilot()
    {
        if(bAutopilot)
            AutoPilotControllerState.setAxis(0, 0, 0, 0);
        sendControllerUpdate();
        bAutopilot = false;

    }
    private void handleAutopilot()
    {

        if (!bHomePointSet)
        {
            if(posUncertainty>0.03)
            {
                //set new home point
                setAutopilotTarget(new PointF(posX, posY));
                bHomePointSet = true;
            }
        }

        if (!bLookAtTargetSet)
        {
            if(posUncertainty>0.03)
            {
                //set new home point
                setLookAtTarget(new PointF(posX, posY));
                bLookAtTargetSet = true;
            }
        }

        double lx = 0, ly = 0, rx = 0, ry = 0;
        boolean updated = false;
        if (bLookAt && bLookAtTargetSet)
        {
            float yaw = (float) eular[2];

            float deltaPosX = lookAtTarget.x - posX;
            float deltaPosY = lookAtTarget.y - posY;
            float dist = (float) Math.sqrt(deltaPosX * deltaPosX + deltaPosY * deltaPosY);
            float normalizedX = deltaPosX / dist;
            float normalizedY = deltaPosY / dist;

            float targetYaw = (float) Math.atan2(normalizedY, normalizedX);

            double deltaYaw = 0.0;
            if (Math.abs(targetYaw - yaw) < Math.PI)
                deltaYaw = targetYaw - yaw;
            else if (targetYaw > yaw)
                deltaYaw = targetYaw - yaw - Math.PI * 2.0f;
            else
                deltaYaw = targetYaw - yaw + Math.PI * 2.0f;


            float minYaw = 0.1f;//Radians
            if (Math.abs(deltaYaw) > minYaw)
            {
                lx = Math.min(1.0, deltaYaw * 1.0);
                updated = true;
            }
            else if (deltaYaw < -minYaw)
            {
                lx = -Math.min(1.0, deltaYaw * 1.0);
                updated = true;
            }
        }
        if (bAutopilot && bHomePointSet)
        {
            double yaw = eular[2];

            Float deltaPosX = autopilotTarget.x - posX;
            Float deltaPosY = autopilotTarget.y- posY;
            Float dist = Float.valueOf((float) Math.sqrt(deltaPosX * deltaPosX + deltaPosY * deltaPosY));
            Float normalizedX = deltaPosX / dist;
            Float normalizedY = deltaPosY / dist;

            Float targetYaw = (float) Math.atan2(normalizedY, normalizedX);
            Float deltaYaw = (Float.valueOf((float) (targetYaw - yaw)));

            Float minDist = 3f;//Meters (I think)

            if (dist > minDist)
            {
                Float speed = Math.min(0.45f, dist*2);//0.2 limits max throttle for safety.
                rx = speed * Math.sin(deltaYaw);
                ry = speed * Math.cos(deltaYaw);
                updated = true;
            }
            else
            {
                cancelAutopilot();//arrived
                updated = true;
            }
        }
        if (updated)
        {
            AutoPilotControllerState.setAxis((float)lx, (float)ly, (float)rx, (float)ry);
            sendControllerUpdate();
        }
    }

    void StartDroneConnection() {
        startStatus();
        try {
            if (!connectionlistener.isAlive()) {
                connectionlistener.start();
            }else{
                connectionlistener.kill();
                connectionlistener = new connectionlistener();
                connectionlistener.start();
            }

        } catch ( RuntimeException e) {
            e.printStackTrace();
            connectionlistener.kill();
            connectionlistener = new connectionlistener();
            connectionlistener.start();
        }


    }

    public class WifiBroadcastReceiver extends BroadcastReceiver {//reconnect after disconnection

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("Wifi Action", action);
            if(connected){
                connected = false;
                StartDroneConnection();
                decoderView.stop();
                }

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
        Boolean BKeepRunning = true;
        @Override
        public void run() {
            BKeepRunning = true;
            while (!connected&&BKeepRunning) {
                try {
                    if (mWifi.isConnected()) {
                        String ip = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
                        if (ip.startsWith("192.168.10.")) {
                            connect();
                        }
                    }


                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        public void kill(){
            BKeepRunning=false;
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
    void setcontrolleraxis() {
        float deadBand = 0.15f;
        DecimalFormat df = new DecimalFormat("#.#");

        float[] r = ellipticalDiscToSquare((((float) joystickr.getNormalizedX() - 50.0f) / 50), (((float) joystickr.getNormalizedY() - 50.0f) / 50));
        float[] l = ellipticalDiscToSquare((((float) joystickl.getNormalizedX() - 50.0f) / 50), (((float) joystickl.getNormalizedY() - 50.0f) / 50));
        float rx = Float.parseFloat(df.format(Math.abs(r[0]) < deadBand ? 0.0f : r[0]));//(((float)joystickr.getNormalizedX()-50.0f)/50);
        float ry = Float.parseFloat(df.format(Math.abs(r[1]) < deadBand ? 0.0f : r[1]));//(((float)joystickr.getNormalizedY()-50.0f)/50);
        float lx = Float.parseFloat(df.format(Math.abs(l[0]) < deadBand ? 0.0f : l[0]));//(((float)joystickl.getNormalizedX()-50.0f)/50);
        float ly = Float.parseFloat(df.format(Math.abs(l[1]) < deadBand ? 0.0f : l[1]));//(((float)joystickl.getNormalizedY()-50.0f)/50);
        //Log.d("joystick", "rx: " + rx + " " + "ry: " + ry + " " + "lx: " + lx + " " + "ly: " + ly+" " + "ly: " + ly);
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
            if(socketMainSending==null|| socketMainSending.isClosed()){

                socketMainSending = new DatagramSocket();
                inetAddressMainSending = getInetAddressByName(addressMainSending);}
            if(socketStreamOnServer==null || socketStreamOnServer.isClosed()){
                socketStreamOnServer = new DatagramSocket(null);
                InetSocketAddress addressVideo = new InetSocketAddress(6038);
                socketStreamOnServer.bind(addressVideo);}
        } catch (SocketException socketException) {
            socketException.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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
e.printStackTrace();

            } catch (Exception e) {
e.printStackTrace();
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
        sendOneBytePacketWithoutReplay.doInBackground(packet);
    }
    void setBatteryLowLevel(int percentage) {
        //                                          crc    typ  cmdL  cmdH  seqL  seqH  rateL  crc   crc
        byte[] packet = new byte[]{(byte) 0xcc, 0x60, 0x00, 0x27, 0x68, (byte) 0x1055, 0x00, 0x09, 0x00, 0x00, 0x5b, (byte) 0xc5};

        //payload
        packet[9] = (byte) percentage;

        setPacketSequence(packet);
        setPacketCRCs(packet);

        SendOneBytePacketWithoutReplay sendOneBytePacketWithoutReplay = new SendOneBytePacketWithoutReplay();
        sendOneBytePacketWithoutReplay.doInBackground(packet);
    }
    void setAttitude(int attitude) {
        //                                          crc    typ  cmdL  cmdH  seqL  seqH  rateL  crc   crc
        byte[] packet = new byte[]{(byte) 0xcc, 0x60, 0x00, 0x27, 0x68, (byte) 0x1059, 0x00, 0x09, 0x00, 0x00, 0x5b, (byte) 0xc5};

        //payload
        packet[9] = (byte) attitude;

        setPacketSequence(packet);
        setPacketCRCs(packet);

        SendOneBytePacketWithoutReplay sendOneBytePacketWithoutReplay = new SendOneBytePacketWithoutReplay();
        sendOneBytePacketWithoutReplay.doInBackground(packet);
    }

    void setVideoDynRate(int rate) {
        //                                          crc    typ  cmdL  cmdH  seqL  seqH  rateL  crc   crc
        byte[] packet = new byte[]{(byte) 0xcc, 0x60, 0x00, 0x27, 0x68, 0x21, 0x00, 0x09, 0x00, 0x00, 0x5b, (byte) 0xc5};

        //payload
        packet[9] = (byte) rate;

        setPacketSequence(packet);
        setPacketCRCs(packet);

        SendOneBytePacketWithoutReplay sendOneBytePacketWithoutReplay = new SendOneBytePacketWithoutReplay();
        sendOneBytePacketWithoutReplay.doInBackground(packet);
    }

    void setVideoRecord(int n) {
        //                                          crc    typ  cmdL  cmdH  seqL  seqH  nL  crc   crc
        byte[] packet = new byte[]{(byte) 0xcc, 0x60, 0x00, 0x27, 0x68, 0x32, 0x00, 0x09, 0x00, 0x00, 0x5b, (byte) 0xc5};

        //payload
        packet[9] = (byte) n;

        setPacketSequence(packet);
        setPacketCRCs(packet);

        SendOneBytePacketWithoutReplay sendOneBytePacketWithoutReplay = new SendOneBytePacketWithoutReplay();
        sendOneBytePacketWithoutReplay.doInBackground(packet);
    }

    void queryAttAngle() {
        byte[] packet = new byte[]{(byte) 0xcc, 0x58, 0x00, 0x7c, 0x48, 0x59, 0x10, 0x06, 0x00, (byte) 0xe9, (byte) 0xb3};
        setPacketSequence(packet);
        setPacketCRCs(packet);
        SendOneBytePacketWithoutReplay sendOneBytePacketWithoutReplay = new SendOneBytePacketWithoutReplay();
        sendOneBytePacketWithoutReplay.doInBackground(packet);
    }

    void setAttAngle(float angle) {
        //                                          crc    typ  cmdL  cmdH  seqL  seqH  ang1  ang2 ang3  ang4  crc   crc
        byte[] packet = new byte[]{(byte) 0xcc, 0x78, 0x00, 0x27, 0x68, 0x58, 0x10, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x5b, (byte) 0xc5};

        //payload
        byte[] bytes = BitConverter.getBytes(angle);
        packet[9] = bytes[0];
        packet[10] = bytes[1];
        packet[11] = bytes[2];
        packet[12] = bytes[3];

        setPacketSequence(packet);
        setPacketCRCs(packet);

        SendOneBytePacketWithoutReplay sendOneBytePacketWithoutReplay = new SendOneBytePacketWithoutReplay();
        sendOneBytePacketWithoutReplay.doInBackground(packet);

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
        sendOneBytePacketWithoutReplay.doInBackground(packet);
        // DecoderView decoderView = findViewById(R.id.decoderView);
         decoderView.stop();

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
        sendOneBytePacketWithoutReplay.doInBackground(packet);
    }

    public void requestIframe() {
        byte[] iframePacket = new byte[]{(byte) 0xcc, 0x58, 0x00, 0x7c, 0x60, 0x25, 0x00, 0x00, 0x00, 0x6c, (byte) 0x95};
        SendOneBytePacketWithoutReplay sendOneBytePacketWithoutReplay = new SendOneBytePacketWithoutReplay();
        sendOneBytePacketWithoutReplay.doInBackground(iframePacket);

    }

    public class SendOneBytePacketWithoutReplay extends AsyncTask<byte[], byte[], Void> {


        @Override
        protected Void doInBackground(byte[]... bytes) {
            try {
                mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

                if (mWifi.isConnected()) {
                    String ip = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
                    if (ip.startsWith("192.168.10.")) {
                        byte[] buf = bytes[0];
                        DatagramPacket packet = new DatagramPacket(buf, buf.length, inetAddressMainSending, portMainSending);

                        socketMainSending.send(packet);

                    }}


            }catch (Exception e){//e.printStackTrace();
                 }


            return null;
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
e.printStackTrace();

            } catch (Exception e) {
e.printStackTrace();
            }
            return doneText;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
        }
    }
    public void setEis(int value)
    {
        //                                          crc    typ  cmdL  cmdH  seqL  seqH  valL  crc   crc
        byte[] packet = new byte[] {(byte) 0xcc, 0x60, 0x00, 0x27, 0x68, 0x24, 0x00, 0x09, 0x00, 0x00, 0x5b, (byte) 0xc5};

        //payload
        packet[9] = (byte)(value & 0xff);

        setPacketSequence(packet);
        setPacketCRCs(packet);

        SendOneBytePacketWithoutReplay sendOneBytePacketWithoutReplay =new SendOneBytePacketWithoutReplay();
        sendOneBytePacketWithoutReplay.doInBackground(packet);
    }

    public void connect() {

        try {
            String connectstring = "conn_req:lh";
            byte[] connectPacket = connectstring.getBytes(StandardCharsets.UTF_8);
            connectPacket[9] = (byte) (6038 & 0x96);
            connectPacket[10] = 6038 >> 8;
            Log.d("connectPacket",new String(connectPacket, StandardCharsets.UTF_8));
            SendOneBytePacketWithoutReplay sendOneBytePacketWithoutReplay = new SendOneBytePacketWithoutReplay();
            sendOneBytePacketWithoutReplay.doInBackground(connectPacket);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void takeoff() {


        try {


            //   SendOneCommandwithoutreplay sendOneCommand = new SendOneCommandwithoutreplay();
            //   sendOneCommand.doInBackground("takeoff");
            byte[] packet = new byte[]{(byte) 0xcc, 0x58, 0x00, 0x7c, 0x68, 0x54, 0x00, (byte) 0xe4, 0x01, (byte) 0xc2, 0x16};
            setPacketSequence(packet);
            setPacketCRCs(packet);
            SendOneBytePacketWithoutReplay sendOneBytePacketWithoutReplay = new SendOneBytePacketWithoutReplay();
            sendOneBytePacketWithoutReplay.doInBackground(packet);


        } catch (Exception e) {
        }
    }


    void takePicture()
    {
        //                                          crc    typ  cmdL  cmdH  seqL  seqH  crc   crc

        byte[] packet = new byte[] {(byte) 0xcc, 0x58, 0x00, 0x7c, 0x68, 0x30, 0x00, 0x06, 0x00, (byte) 0xe9, (byte) 0xb3};
        setPacketSequence(packet);
        setPacketCRCs(packet);
        SendOneBytePacketWithoutReplay sendOneBytePacketWithoutReplay = new SendOneBytePacketWithoutReplay();
        sendOneBytePacketWithoutReplay.doInBackground(packet);
        Log.d("takepicture","takepicture");
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
    void sendControllerUpdate() {
        //if (!connected)
        //    return;

        float boost = 0.0f;
        if (controllerState.speed > 0)
            boost = 0.3f;

        //var limit = 1.0f;//Slow down while testing.
        //rx = rx * limit;
        //ry = ry * limit;
        float rx = Clamp(controllerState.rx + AutoPilotControllerState.rx, -1.0f, 1.0f);
        float ry = Clamp(controllerState.ry + AutoPilotControllerState.ry, -1.0f, 1.0f);
        float lx = Clamp(controllerState.lx + AutoPilotControllerState.lx, -1.0f, 1.0f);
        float ly = Clamp(controllerState.ly + AutoPilotControllerState.ly, -1.0f, 1.0f);
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
            sendOneBytePacketWithoutReplay.doInBackground(packet);
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
            if (!videoDatagramReceiver.isAlive()) {
                videoDatagramReceiver.start();//start listening for tello
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
            // sendOneCommandwithoutreplay.doInBackground("streamon");
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
        public byte[] lmessage = new byte[4380];
        byte[] videoFrame = new byte[100 * 4380];
        int videoOffset = 0;
        Boolean started =false;

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void run() {

            float speed=0.0f;
            float time=0.0f;
            int SequenceNumber=0;
            int SubSequenceNumber=0;
            int nalType=0;
            int packetlen=0;
            ArrayList<byte[]> PacketsArray = new ArrayList<byte[]>();
            boolean showframe = false;


            Log.d("video start", "start");
            DatagramPacket packet = new DatagramPacket(lmessage, lmessage.length);


            while (bKeepRunning) {

                try {
                    socketStreamOnServer.receive(packet);
                } catch (IOException ioException) {


                }
                packetlen++;

                byte[] data = new byte[packet.getLength()];
                //Log.d("Video Length", String.valueOf(data.length));
                System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());
                if (data[2] == 0 && data[3] == 0 && data[4] == 0 && data[5] == 1&&!started)//Wait for first NAL
                {
                    int nal = (data[6] & 0x1f);
                    //if (nal != 0x01 && nal!=0x07 && nal != 0x08 && nal != 0x05)
                    //    Console.WriteLine("NAL type:" +nal);
                    started = true;
                }
                if(started){





                    try {
                            //   if(speed<0.3&&speed>0.01){
                            //       requestIframe();
                            //   }

                            nalType = data[6] & 0x1f;
                            // Log.d("videoOffset", String.valueOf(videoOffset));
                            if (showframe) {
                                if (!isPaused) {
                                    byte[] videoFramenew = new byte[videoOffset];
                                    System.arraycopy(videoFrame, 0, videoFramenew, 0, videoOffset);
                                    try {
                                        decoderView.decode(videoFrame);
                                    } catch (Exception e) {
                                  //      decoderView.stop();
                                    }
                                    videoOffset = 0;
                                   videoFrame = new byte[100 * 1024];
                                    showframe = false;
                                    packetlen=0;

                                }
                            }


                            //System.arraycopy(data, 2, videoFrame, videoOffset, data.length - 2);


                        Log.d("SequenceNumber", String.valueOf(data[0]));
                        Log.d("SubSequenceNumber", String.valueOf(data[1]));
                        Log.d("len", String.valueOf(data.length));
                        Log.d("nal", String.valueOf(data[6] & 0x1f));



                        if (data[1] == -128) {
                            decoderView.setVideoData(data);
                            //showframe=true;
                            videoOffset = 0;
                            videoFrame = new byte[100 * 1024];
                            showframe = false;
                        }
                       // else if(data[1]==-124) {
                       //     System.arraycopy(data, 2, videoFrame, videoOffset, data.length - 2);
                       //     videoOffset += data.length - 2;
                       //     Log.d("video frame len", String.valueOf(videoOffset));
                       //     if (Math.abs(9 - (packetlen)) >= Math.abs(data[1] + 120)) {
                       //         showframe = true;
                       //     } else {
                       //         requestIframe();
//
                       //     }}
                            //videoOffset = 0;
                            //videoFrame = new byte[100 * 1024];
                            //packetlen=0;
                        //else if(data[1]==-124){
                        //    System.arraycopy(data, 2, videoFrame, videoOffset, data.length - 2);
                        //    videoOffset += data.length - 2;
                        //    //if(!(packetlen>=3)) {
                        //    //    requestIframe();
                        //    //}else{
                        //    Log.d("video frame len", String.valueOf(videoOffset));
                        //    showframe=true;//}
                        //}
                       // else if(data[1]==-123){
                       //     System.arraycopy(data, 2, videoFrame, videoOffset, data.length - 2);
                       //     videoOffset += data.length - 2;
                       //     if(!(packetlen>=5)) {
                       //         requestIframe();
                       //     }else{
                       //     Log.d("video frame len", String.valueOf(videoOffset));
                       //     showframe=true;}
                       // }
                        else if(data[1]<=-100) {
                            System.arraycopy(data, 2, videoFrame, videoOffset, data.length - 2);
                            videoOffset += data.length - 2;
                            Log.d("video frame len", String.valueOf(videoOffset));

                            showframe = true;
                            //videoOffset = 0;
                            //videoFrame = new byte[100 * 1024];
                            //packetlen=0;
                        }


                     else if(data.length!=1460){
                            videoOffset = 0;
                            videoFrame = new byte[100 * 1024];
                            packetlen=0;
                        }
                        else if(data[1]<0){
                            videoOffset = 0;
                            videoFrame = new byte[100 * 1024];
                            packetlen=0;
                        }
                        else {
                            System.arraycopy(data, 2, videoFrame, videoOffset, data.length - 2);
                            videoOffset += data.length - 2;
                        }







                    }catch (Exception e){}


                }}

            if (socketStreamOnServer == null) {
                socketStreamOnServer.close();
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

            while (bKeepRunning) {
                    requestIframe();
                try {
                    Thread.sleep((long) (1000/iFrameRate));
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
            } catch (RuntimeException e) {

            }
        }}




    void startStatus() {
        try {
            if (!statusDatagramReceiver.isAlive()) {
                Initialize();
                statusDatagramReceiver.start();
            }

        } catch (RuntimeException e) {
            e.printStackTrace();
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
                    Thread.sleep(8);
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
            } catch (RuntimeException e) {

            }
        }


    }

//old form the normal sdk
    // public void rc() {

    //    /* if(a==70&&b==-70&&c==-70&&d==-70){
    //     Log.d("startmotors","motorstart");}
    //     else{*/
    //     SendOneCommandwithoutreplay sendOneCommand = new SendOneCommandwithoutreplay();
    //     sendOneCommand.doInBackground("rc " + a + " " + b + " " + c + " " + d);
    //     // }
    // }


    public void set(byte[] data)
    {
        try {
            int index = 0;
            height = (short)(data[index] | (data[index + 1] << 8)); index += 2;
            northSpeed = (short)(data[index] | (data[index + 1] << 8)); index += 2;
            eastSpeed = (short)(data[index] | (data[index + 1] << 8)); index += 2;
            flySpeed = ((int)Math.sqrt(Math.pow(northSpeed, 2.0D) + Math.pow(eastSpeed, 2.0D)));
            verticalSpeed = (int) (data[index] | (data[index + 1] << 8)); index += 2;// ah.a(paramArrayOfByte[6], paramArrayOfByte[7]);
            flyTime = data[index] | (data[index + 1] << 8); index += 2;// ah.a(paramArrayOfByte[8], paramArrayOfByte[9]);

            imuState = (data[index] >> 0 & 0x1) == 1 ? true : false;
            pressureState = (data[index] >> 1 & 0x1) == 1 ? true : false;
            downVisualState = (data[index] >> 2 & 0x1) == 1 ? true : false;
            powerState = (data[index] >> 3 & 0x1) == 1 ? true : false;
            batteryState = (data[index] >> 4 & 0x1) == 1 ? true : false;
            gravityState = (data[index] >> 5 & 0x1) == 1 ? true : false;
            windState = (data[index] >> 7 & 0x1) == 1 ? true : false;
            index += 1;

            //if (paramArrayOfByte.length < 19) { }
            imuCalibrationState = data[index]; index += 1;
            batteryPercentage = data[index]; index += 1;
            textViewBattery.setText("battery:"+ String.valueOf(batteryPercentage));
            droneFlyTimeLeft = data[index] | (data[index + 1] << 8); index += 2;
            droneBatteryLeft = data[index] | (data[index + 1] << 8); index += 2;

            //index 17
            flying = (data[index] >> 0 & 0x1)==1?true:false;
            onGround = (data[index] >> 1 & 0x1) == 1 ? true : false;
            eMOpen = (data[index] >> 2 & 0x1) == 1 ? true : false;
            droneHover = (data[index] >> 3 & 0x1) == 1 ? true : false;
            outageRecording = (data[index] >> 4 & 0x1) == 1 ? true : false;
            batteryLow = (data[index] >> 5 & 0x1) == 1 ? true : false;
            batteryLower = (data[index] >> 6 & 0x1) == 1 ? true : false;
            factoryMode = (data[index] >> 7 & 0x1) == 1 ? true : false;
            index += 1;

            flyMode = data[index]; index += 1;
            throwFlyTimer = data[index]; index += 1;
            cameraState = data[index]; index += 1;

            //if (paramArrayOfByte.length >= 22)
            electricalMachineryState = data[index]; index += 1; //(paramArrayOfByte[21] & 0xFF);

            //if (paramArrayOfByte.length >= 23)
            frontIn = (data[index] >> 0 & 0x1) == 1 ? true : false;//22
            frontOut = (data[index] >> 1 & 0x1) == 1 ? true : false;
            frontLSC = (data[index] >> 2 & 0x1) == 1 ? true : false;
            index += 1;
            temperatureHeight = (int)(data[index] >> 0 & 0x1);//23
            textViewTemp.setText("temp: "+ String.valueOf(temperatureHeight));}catch (Exception e){}
    }

    public double[] toEuler(float quatX,float quatY,float quatZ,float quatW)
    {
        float qX = quatX;
        float qY = quatY;
        float qZ = quatZ;
        float qW = quatW;

        double sqW = qW * qW;
        double sqX = qX * qX;
        double sqY = qY * qY;
        double sqZ = qZ * qZ;
        double yaw;
        double roll;
        double pitch;
        double[] retv = new double[3];
        double unit = sqX + sqY + sqZ + sqW; // if normalised is one, otherwise
        // is correction factor
        double test = qW * qX + qY * qZ;
        Log.d("test", String.valueOf(test));
        if (test > 0.499 * unit)
        { // singularity at north pole
            yaw = 2 * Math.atan2(qY, qW);
            pitch = Math.PI / 2;
            roll = 0;
        }
        else if (test < -0.499 * unit)
        { // singularity at south pole
            yaw = -2 * Math.atan2(qY, qW);
            pitch = -Math.PI / 2;
            roll = 0;
        }
        else
        {
            yaw = Math.atan2(2.0 * (qW * qZ - qX * qY),
                    1.0 - 2.0 * (sqZ + sqX));
            roll = Math.asin(2.0 * test / unit);
            pitch = Math.atan2(2.0 * (qW * qY - qX * qZ),
                    1.0 - 2.0 * (sqY + sqX));
        }
        retv[0] = pitch;
        retv[1] = roll;
        retv[2] = yaw;
        return retv;
    }

    //Parse some of the interesting info from the tello log stream
    public void parseLog(byte[] data) throws Exception {
        int pos = 0;

        //A packet can contain more than one record.
        while (pos < data.length-2)//-2 for CRC bytes at end of packet.
        {

            if (data[pos] != 85)//Check magic byte
            {
                //Console.WriteLine("PARSE ERROR!!!");
                break;
            }
            //Log.d("magic byte", String.valueOf(data[pos]));
            if (data[pos + 2] != 0)//Should always be zero (so far)
            {
                //Console.WriteLine("SIZE OVERFLOW!!!");
                break;
            }
            byte len = (byte) Math.abs(data[pos + 1]);
            Log.d("len", String.valueOf(len));
            int crc = data[pos + 3];
            int id = BitConverter.toUint16(data, pos + 4);
            byte[] xorBuf = new byte[256];
            byte xorValue = data[pos + 6];
            switch (id) {
                case 0x1d://29 new_mvo
                    for (int i = 0; i < len; i++) {//Decrypt payload.
                        xorBuf[i] = (byte) (data[pos + i] ^ xorValue);
                    }
                    int index = 10;//start of the velocity and pos data.
                    int observationCount = BitConverter.toUint16(xorBuf, index);
                    index += 2;
                    int velX = BitConverter.toUint16(xorBuf, index);
                    index += 2;
                    int velY = BitConverter.toUint16(xorBuf, index);
                    index += 2;
                    int velZ = BitConverter.toUint16(xorBuf, index);
                    index += 2;
                    posX = BitConverter.toSingle(xorBuf, index);
                    index += 4;
                    posY = BitConverter.toSingle(xorBuf, index);
                    index += 4;
                    posZ = -BitConverter.toSingle(xorBuf, index);
                    index += 4;
                    posUncertainty = BitConverter.toSingle(xorBuf, index) * 10000.0f;
                    index += 4;
                    Log.d("pos", observationCount + " " + posX + " " + posY + " " + posZ);
                    Log.d("vel", observationCount + " " + velX + " " + velY + " " + velZ);
                    textViewPosition.setText(" x:" + posX + " y:" + posY + " z:" + posZ);
                    break;
                case 0x0800://2048 imu
                    for (int i = 0; i < len; i++) {//Decrypt payload.
                        xorBuf[i] = (byte) (data[pos + i] ^ xorValue);
                    }
                    int index2 = 10 + 48;//44 is the start of the quat data.
                    float quatW = BitConverter.toSingle(xorBuf, index2);
                    index2 += 4;
                    float quatX = BitConverter.toSingle(xorBuf, index2);
                    index2 += 4;
                    float quatY = BitConverter.toSingle(xorBuf, index2);
                    index2 += 4;
                    float quatZ = BitConverter.toSingle(xorBuf, index2);
                    index2 += 4;


                    eular = toEuler(quatX, quatY, quatZ, quatW);
                    Log.d("eular", " Pitch:" + eular[0] * (180 / 3.141592) + " Roll:" + eular[1] * (180 / 3.141592) + " Yaw:" + eular[2] * (180 / 3.141592));
                    textViewRotation.setText(" Pitch:" + eular[0] * (180 / 3.141592) + " Roll:" + eular[1] * (180 / 3.141592) + " Yaw:" + eular[2] * (180 / 3.141592));
                    // Log.d("eular", "quatW:" + quatW + " quatX:" + quatX + " quatY:" + quatY + " quatZ:" + quatZ);
                    index2 = 10 + 76;//Start of relative velocity
                    float velN = BitConverter.toSingle(xorBuf, index2);
                    index2 += 4;
                    float velE = BitConverter.toSingle(xorBuf, index2);
                    index2 += 4;
                    float velD = BitConverter.toSingle(xorBuf, index2);
                    index2 += 4;


                    //Console.WriteLine(vN + " " + vE + " " + vD);

                    break;

            }
            pos += len;
        }
    }
    public void sendAckFilePiece(byte endFlag,int fileId, int pieceId)
    {
        //                                          crc    typ  cmdL  cmdH  seqL  seqH  byte  nL    nH    n2L                     crc   crc
        byte[] packet = new byte[] {(byte) 0xcc, (byte) 0x90, 0x00, 0x27, 0x50, 0x63, 0x00, (byte) 0xf0, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x5b, (byte) 0xc5};

        packet[9] = endFlag;
        packet[10] = (byte)(fileId & 0xff);
        packet[11] = (byte)((fileId >> 8) & 0xff);

        packet[12] = ((byte)(int)(0xFF & pieceId));
        packet[13] = ((byte)(int)(pieceId >> 8 & 0xFF));
        packet[14] = ((byte)(int)(pieceId >> 16 & 0xFF));
        packet[15] = ((byte)(int)(pieceId >> 24 & 0xFF));

        setPacketSequence(packet);
        setPacketCRCs(packet);
        //var dataStr = BitConverter.ToString(packet).Replace("-", " ");
        //Console.WriteLine(dataStr);

        SendOneBytePacketWithoutReplay sendOneBytePacketWithoutReplay = new SendOneBytePacketWithoutReplay();
        sendOneBytePacketWithoutReplay.doInBackground(packet);
    }
    void sendAckFileSize()
    {
        //                                          crc    typ  cmdL  cmdH  seqL  seqH  modL  crc   crc
        byte[] packet = new byte[] {(byte) 0xcc, 0x60, 0x00, 0x27, 0x50, 0x62, 0x00, 0x00, 0x00, 0x00, 0x5b, (byte) 0xc5};
        setPacketSequence(packet);
        setPacketCRCs(packet);

        SendOneBytePacketWithoutReplay sendOneBytePacketWithoutReplay = new SendOneBytePacketWithoutReplay();
        sendOneBytePacketWithoutReplay.doInBackground(packet);
    }
    public void sendAckFileDone(int size)
    {
        //                                          crc    typ  cmdL  cmdH  seqL  seqH  fidL  fidH  size  size  size  size  crc   crc
        byte[] packet = new byte[] {(byte) 0xcc, (byte) 0x88, 0x00, 0x24, 0x48, 0x64, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x5b, (byte) 0xc5};

        //packet[9] = (byte)(fileid & 0xff);
        //packet[10] = (byte)((fileid >> 8) & 0xff);

        packet[11] = ((byte)(int)(0xFF & size));
        packet[12] = ((byte)(int)(size >> 8 & 0xFF));
        packet[13] = ((byte)(int)(size >> 16 & 0xFF));
        packet[14] = ((byte)(int)(size >> 24 & 0xFF));
        setPacketSequence(packet);
        setPacketCRCs(packet);

        SendOneBytePacketWithoutReplay sendOneBytePacketWithoutReplay = new SendOneBytePacketWithoutReplay();
        sendOneBytePacketWithoutReplay.doInBackground(packet);
    }
    void sendAckLog(short cmd,short id)
    {
        //                                          crc    typ  cmdL  cmdH  seqL  seqH  unk   idL   idH   crc   crc
        byte[] packet = new byte[] {(byte) 0xcc, 0x70, 0x00, 0x27, 0x50, 0x50, 0x10, 0x00, 0x00, 0x00, 0x00, 0x00, 0x5b, (byte) 0xc5};

        packet[5] = (byte)(cmd & 0xff);
        packet[6] = (byte)((cmd >> 8) & 0xff);


        packet[10] = (byte)(id & 0xff);
        packet[11] = (byte)((id >> 8) & 0xff);


        setPacketSequence(packet);
        setPacketCRCs(packet);

        SendOneBytePacketWithoutReplay sendOneBytePacketWithoutReplay = new SendOneBytePacketWithoutReplay();
        sendOneBytePacketWithoutReplay.doInBackground(packet);
    }

    private class StatusDatagramReceiver extends Thread {
        private boolean bKeepRunning = true;

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void run() {

            try {
                socketMainSending.setSoTimeout(1000);
            } catch (SocketException e) {
            }
            byte[] lmessage = new byte[2048];
            DatagramPacket packet = new DatagramPacket(lmessage, lmessage.length);
            WifiManager  wifiManager = (WifiManager) getApplication().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            try {

                while (bKeepRunning) {
                    if (mWifi.isConnected()) {
                        String ip = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
                        if (ip.startsWith("192.168.10.")) {
                    if(socketMainSending!=null){
                        try {
                            socketMainSending.receive(packet);
                            byte[] recived = new byte[packet.getLength()];
                            System.arraycopy(packet.getData(), 0, recived, 0, packet.getLength());
                            int cmdId = ((int) recived[5] | ((int) recived[6] << 8));
                            Log.d("Cmd id", String.valueOf(cmdId));
                            String dataString= new String(packet.getData(), StandardCharsets.UTF_8);
                            if(dataString.startsWith("conn_ack")&&connected==false){
                                connected = true;
                                setPicVidMode(0);
                                streamon();
                                setEis(0);
                                requestIframe();
                                controllerState.setSpeedMode(0);
                                setAttAngle(25.0f);
                                queryAttAngle();
                                StartHeartBeatJoystick();
                                activity = (Activity) MainActivity.this;
                                setVideoBitRate(bitrate);
                                setBatteryLowLevel(10);
                                setAttitude(30);
                                decoderView.Init();

                            }
                            if (cmdId >= 74 && cmdId < 80) {
                                //Console.WriteLine("XXXXXXXXCMD:" + cmdId);
                            }
                            if (cmdId == 86)//state command
                            {
                                //update
                                set(Arrays.copyOfRange(recived, 9, recived.length));

                            }
                            if (cmdId == 4176)//log header
                            {
                                //just ack.
                                int id =  BitConverter.toUint16(recived, 9);
                                sendAckLog((short) cmdId,  (short) id);
                                Log.d("id", String.valueOf(id));
                            }
                            if (cmdId == 4177)//log data
                            {
                                try {
                                    parseLog(Arrays.copyOfRange(recived, 10,recived.length));
                                } catch (Exception pex) {
                                    // Console.WriteLine("parseLog error:" + pex.Message);
                                    pex.printStackTrace();
                                }
                            }
                            if (cmdId == 4178)//log config
                            {
                                //todo. this doesnt seem to be working.

                                //var id = BitConverter.ToUInt16(received.bytes, 9);
                                //var n2 = BitConverter.ToInt32(received.bytes, 11);
                                //sendAckLogConfig((short)cmdId, id,n2);

                                //var dataStr = BitConverter.ToString(received.bytes.Skip(14).Take(10).ToArray()).Replace("-", " ")/*+"  "+pos*/;


                                //Console.WriteLine(dataStr);
                            }
                            if (cmdId == 4185)//att angle response
                            {
                                byte[] array = Arrays.copyOfRange(recived,10,14);
                                float f = BitConverter.toSingle(array, 0);
                                Log.d("att angle response", String.valueOf(f));
                            }
                            if (cmdId == 4182)//max hei response
                            {
                                //var array = received.bytes.Skip(9).Take(4).Reverse().ToArray();
                                //float f = BitConverter.ToSingle(array, 0);
                                //Console.WriteLine(f);
                                // if (received.bytes[10] != 10)
                                // {
//
                                // }
                            }
                            if (cmdId == 26)//wifi str command
                            {
                                wifiStrength = recived[9];
                                if (recived[10] != 0)//Disturb?
                                {
                                }
                            }
                            if (cmdId == 53)//light str command
                            {
                            }
                            if (cmdId == 98)//start jpeg.
                            {
                                //picFilePath = picPath + DateTime.Now.ToString("yyyy-dd-M--HH-mm-ss") + ".jpg";

                                int start = 9;
                                int ftype = recived[start];
                                start += 1;
                                picBytesExpected = BitConverter.toUint32(recived, start);
                                Log.d("picBytesExpected", String.valueOf(picBytesExpected));
                                if(picBytesExpected>picbuffer.length)
                                {
                                    picbuffer = new byte[picBytesExpected];
                                }
                                picBytesRecived = 0;
                                picChunkState = new boolean[Math.abs(picBytesExpected/1024)+1]; //calc based on size.
                                picPieceState = new boolean[(picChunkState.length / 8)+1];
                                picExtraPackets = 0;//for debugging.
                                picDownloading = true;

                                sendAckFileSize();
                            }
                            if (cmdId == 99)//jpeg
                            {
                                DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-dd-M--HH-mm-ss");
                                //var dataStr = BitConverter.ToString(received.bytes.Skip(0).Take(30).ToArray()).Replace("-", " ");
                                File file = new File(Environment.getExternalStoragePublicDirectory(
                                        Environment.DIRECTORY_DCIM), "tello+/pic");
                                if (!file.mkdirs()) {

                                }
                                File picFilePath = new File(file.getPath()+"/" + LocalDateTime.now().format(format).toString() + ".jpg");
                                Log.d("picpath",picFilePath.getAbsolutePath());
                                int start = 9;
                                int fileNum = BitConverter.toUint16(recived,start);
                                start += 2;
                                int pieceNum = BitConverter.toUint32(recived, start);
                                start += 4;
                                int seqNum = BitConverter.toUint32(recived, start);
                                start += 4;
                                int size = BitConverter.toUint16(recived, start);
                                start += 2;


                                maxPieceNum = Math.max((int) pieceNum, maxPieceNum);
                                if (!picChunkState[seqNum]) {
                                    System.arraycopy(recived, start, picbuffer, seqNum * 1024, size);
                                    picBytesRecived += size;
                                    picChunkState[seqNum] = true;

                                    for (int p = 0; p < picChunkState.length / 8; p++) {
                                        Boolean done = true;
                                        for (int s = 0; s < 8; s++) {
                                            if (!picChunkState[(p * 8) + s]) {
                                                done = false;
                                                break;
                                            }
                                        }
                                        if (done && !picPieceState[p]) {
                                            picPieceState[p] = true;
                                            sendAckFilePiece((byte) 0, fileNum, (int) p);
                                            //Console.WriteLine("\nACK PN:" + p + " " + seqNum);
                                        }
                                    }
                                    if (picFilePath != null && picBytesRecived >= picBytesExpected) {
                                        picDownloading = false;
                                        runOnUiThread(new Runnable() {
                                            public void run() {
                                                Toast.makeText(getApplicationContext(),"Saved photo to: "+picFilePath.getAbsolutePath(),Toast.LENGTH_SHORT).show();
                                            }
                                        });


                                        sendAckFilePiece((byte) 1, 0, (int) maxPieceNum);//todo. Double check this. finalize

                                        sendAckFileDone((int) picBytesExpected);

                                        //HACK.
                                        //Send file done cmdId to the update listener so it knows the picture is done.
                                        //hack.
                                        //onUpdate(100);
                                        //hack.
                                        //This is a hack because it is faking a message. And not a very good fake.
                                        //HACK.

                                        ///Console.WriteLine("\nDONE PN:" + pieceNum + " max: " + maxPieceNum);

                                        //Save raw data minus sequence.
                                        FileOutputStream fos = new FileOutputStream(picFilePath);
                                        fos.write(picbuffer, 0, (int)picBytesExpected);
                                    }
                                } else {
                                    picExtraPackets++;//for debugging.

                                    //if(picBytesRecived >= picBytesExpected)
                                    //    Console.WriteLine("\nEXTRA PN:"+pieceNum+" max "+ maxPieceNum);
                                }


                            }
                            if (cmdId == 100) {

                            }
                            handleAutopilot();

                            //send command to listeners.
                            try {
                                //fire update event.
                                //  onUpdate(cmdId);
                            } catch (Exception ex) {
                                //Fixed. Update errors do not cause disconnect.
                                ex.printStackTrace();
                                //break;
                            }


                        }catch (Exception e){e.printStackTrace();}}}}}

            } catch (Exception e) {
                e.printStackTrace();
            }


            if (socketMainSending == null) {
                socketMainSending.close();
            }
        }

        public void kill() {
            bKeepRunning = false;
        }
    }


    @Override
    public void onPause() {
        super.onPause();
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
    @Override
    public void onStart() {
        super.onStart();
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
//   //          // to doInBackground "ffmpeg -version" command you just need to pass "-version"
//   //          fftask = ffmpeg.doInBackground(cmd, new doInBackgroundBinaryResponseHandler() {
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