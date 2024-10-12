package com.example.sensorx;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;


public class SensorService extends Service {

    private boolean isTimerRunning;
    private int wakeLockCount = 0;
    private Subject subject;
    private Walk currWalk;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer, mGyroscope, mRotVector, mStepDetector;

    private long prevTime_accl = 0;
    private long prevTime_gyro = 0;
    private long prevTime_rot = 0;

    private long totalAcclEvent = 0;
    private long totalGyroEvent = 0;
    private long totalStepEvent = 0;
    private long totalRotEvent = 0;
    private DataService dataService = new DataService(BuildConfig.URL);

    private HandlerThread acclSensorHandlerThread;
    private Handler acclSensorHandler;
    private HandlerThread gyroSensorHandlerThread;
    private Handler gyroSensorHandler;
    private HandlerThread rotVecSensorHandlerThread;

    private Handler rotVecSensorHandler;

    private HandlerThread stepDetectorHandlerThread;
    private Handler stepDetectorHandler;

    public static final String SERVICE_ACTION = "SERVICE_ACTION";
    public static final String START = "START";
    public static final String STOP = "STOP";
    public static final String SET_SUBJECT = "SET_SUBJECT";
    public static final String SET_WALK = "SET_WALK";
    public static final String RESET_SUBJECT = "RESET_SUBJECT";
    public static final String RESET_WALK = "RESET_WALK";

    public static final String TEST_CONN = "TEST_CONN";
    public static final String SEND_DATA = "SEND_DATA";
    public static final String DELETE_WALK = "DELETE_WALK";
    public static final String MOVE_TO_FOREGROUND = "MOVE_TO_FOREGROUND";
    public static final String MOVE_TO_BACKGROUND = "MOVE_TO_BACKGROUND";

    // Intent Actions
    public static final String TIMER_STOPPED = "TIMER_STOPPED ";
    public static final String WALK_CANCELLED = "WALK_CANCELLED";
    public static final String TIMER_TICK = "TIMER_TICK";
    public static final String TIME_ELAPSED = "TIME_ELAPSED";
    public static final String API_CONN = "API_CONN";
    public static final String SENSOR_DATA = "SENSOR_DATA";
    public static final String SENSOR_TYPE = "SENSOR_TYPE";


    private static final String CHANNEL_ID = "ForegroundServiceChannel";

    private final String TAG = "Service";
    private int timeElapsed;
    private Timer countDownTimer;
    private NotificationManager notificationManager;
    private Timer updateTimer;
    private PowerManager.WakeLock wakeLock;
    private long startTime;

    private String[] sensorAccuracy = {"Unreliable","LOW","MEDIUM","HIGH"};
    private boolean setup_sensors(){

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (mSensorManager == null) {
            return false;
        }

        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mRotVector = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mStepDetector = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        return mAccelerometer != null && mGyroscope != null && mRotVector != null && mStepDetector != null;
    }



    @Override
    public void onCreate() {
        super.onCreate();
        setup_sensors();
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "SensorX::MyWakelockTag");
    }

    public boolean register_sensors(){

        startTime = SystemClock.elapsedRealtimeNanos();
        currWalk.startTime = System.currentTimeMillis();

        acclSensorHandlerThread = new HandlerThread("AcclThread");
        acclSensorHandlerThread.start();
        acclSensorHandler = new Handler(acclSensorHandlerThread.getLooper()) {};

        gyroSensorHandlerThread = new HandlerThread("GyroThread");
        gyroSensorHandlerThread.start();
        gyroSensorHandler = new Handler(gyroSensorHandlerThread.getLooper()) {};

        rotVecSensorHandlerThread = new HandlerThread("RotVecThread");
        rotVecSensorHandlerThread.start();
        rotVecSensorHandler = new Handler(rotVecSensorHandlerThread.getLooper()) {};

        stepDetectorHandlerThread = new HandlerThread("StepDetectorThread");
        stepDetectorHandlerThread.start();
        stepDetectorHandler = new Handler(stepDetectorHandlerThread.getLooper()) {};


        boolean success = mSensorManager.registerListener(acclSensorEventListener, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST, acclSensorHandler);
//                Log.d(TAG, String.valueOf(success));
        if (!success) {
            success = mSensorManager.registerListener(acclSensorEventListener, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL, acclSensorHandler);
            if (!success){
                return false;
            }
        }


        success = mSensorManager.registerListener(gyroSensorEventListener, mGyroscope, SensorManager.SENSOR_DELAY_FASTEST, gyroSensorHandler);

        if (!success) {
            success = mSensorManager.registerListener(gyroSensorEventListener, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL, gyroSensorHandler);
                if( !success )
                    return false;
        }

        success = mSensorManager.registerListener(rotVecSensorListener, mRotVector, SensorManager.SENSOR_DELAY_FASTEST, rotVecSensorHandler);

        if (!success) {
            success = mSensorManager.registerListener(rotVecSensorListener, mRotVector, SensorManager.SENSOR_DELAY_NORMAL, rotVecSensorHandler);
            if( !success )
                return false;
        }

        success = mSensorManager.registerListener(stepDetectorListener, mStepDetector, SensorManager.SENSOR_DELAY_FASTEST, stepDetectorHandler);

        if (!success) {
            success = mSensorManager.registerListener(stepDetectorListener, mStepDetector, SensorManager.SENSOR_DELAY_NORMAL, stepDetectorHandler);
            if( !success )
                return false;
        }

        return true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createChannel();
        getNotificationManager();
        Intent notificationIntent = new Intent(this, MainActivity.class);

        String action = intent.getStringExtra(SERVICE_ACTION);

        Log.d("Timer", "onStartCommand Action: " + action);

        switch (action) {
            case START:
                startTimer();
                break;
            case STOP:
                stopTimer(true);
                break;
            case SET_SUBJECT:
                setSubject(intent);
                break;
            case RESET_SUBJECT:
                resetSubject();
                break;
            case SET_WALK:
                setWalk(intent);
                break;
            case RESET_WALK:
                resetWalk();
                break;
            case TEST_CONN:
                testConn();
                break;
            case SEND_DATA:
                sendData();
                break;
            case DELETE_WALK:
                deleteWalk(intent);
                break;
            case MOVE_TO_FOREGROUND:
                moveToForeground();
                break;
            case MOVE_TO_BACKGROUND:
                moveToBackground();
                break;
        }


        return START_NOT_STICKY;
    }

    private void releaseWakeLock() {

        if (wakeLock != null && wakeLock.isHeld()) {
            if (wakeLockCount <= 0) {
                stopSelf();
            }
            wakeLock.release();
            wakeLockCount--;
        }
    }

    private void unregisterSensors(){
        mSensorManager.unregisterListener(acclSensorEventListener);
        mSensorManager.unregisterListener(gyroSensorEventListener);
        mSensorManager.unregisterListener(rotVecSensorListener);
        mSensorManager.unregisterListener(stepDetectorListener);
        acclSensorHandlerThread.quitSafely();
        gyroSensorHandlerThread.quitSafely();
        rotVecSensorHandlerThread.quitSafely();
        stepDetectorHandlerThread.quitSafely();
    }
    @Override
    public void onDestroy() {
        unregisterSensors();
        while (wakeLockCount > 0)
            releaseWakeLock();
        super.onDestroy();
    }



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // Here we do not use a binder to interact with the foreground service.
        return null;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "TIMER",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationChannel.setSound(null, null);
            notificationChannel.setShowBadge(true);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }
    }


    private void getNotificationManager() {
            notificationManager = (NotificationManager) ContextCompat.getSystemService(
                this,
                NotificationManager.class
        );
    }

    private void deleteWalk(Intent intent) {
        int indx = intent.getIntExtra("INDEX",-1);
        subject.walks.remove(indx);
    }

    private void sendDataUploadResult(boolean success,String msg){
        Intent statusIntent = new Intent();
        statusIntent.setAction(API_CONN);
        statusIntent.putExtra("TYPE", SEND_DATA);
        statusIntent.putExtra("SUCCESS", success);
        statusIntent.putExtra("MESSAGE", msg);
        sendBroadcast(statusIntent);
    }

    private void sendData() {
        if (subject == null) {
            sendDataUploadResult(false,"Error: Nothing to send.Take Data First!!!");
            return;
        } else if (subject.walks.size() == 0) {
            sendDataUploadResult(false,"Error: Nothing to send.Take Data First!!!");
            return;
        }

        String data = new Gson().toJson(subject);
        String filename = System.currentTimeMillis() + ".txt";
        FileIO.writeToExternalStorage( filename, data);
        String state = Environment.getExternalStorageState();
        try {
            if(Environment.MEDIA_MOUNTED.equals(state)){
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "/" + filename);
                if(file.isFile()){
                    sendDataUploadResult(true,"Sending Data");
                    dataService.sendFile(file, new DataService.ResponseListener<String>() {
                        @Override
                        public void onFailure(String value) {
                            if(value.equals("request timed out 408"))
                                sendDataUploadResult(false,"timeout");
                            else
                                sendDataUploadResult(false,value.substring(0,12));
                        }

                        @Override
                        public void onResponse(String value) {
                            sendDataUploadResult(true,"SUCCESS");
//                            MainActivity.this.runOnUiThread(new Runnable() {
//                                public void run() {
//                                    connStatText.setText("success");
//                                }
//                            });
                        }
                    });
                }else{
                    throw new Exception("not a file");
                }
            }else{
                throw new Exception("not mounted");
            }
        }catch (Exception e){
//            Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            sendDataUploadResult(false,"Error : file write failed");
        }
    }

    private void sendTestConnResult(boolean success){
        Intent statusIntent = new Intent();
        statusIntent.setAction(API_CONN);
        statusIntent.putExtra("TYPE", TEST_CONN);
        statusIntent.putExtra("SUCCESS", success);
        sendBroadcast(statusIntent);
    }

    private void testConn() {
        dataService.testConn(new DataService.ResponseListener<String>() {
            @Override
            public void onFailure(String value) {
                sendTestConnResult(false);
            }
            @Override
            public void onResponse(String value) {
                sendTestConnResult(true);
            }
        });
    }

    private void resetWalk() {
        currWalk = null;
    }

    private void setWalk(Intent intent) {
        currWalk = new Walk();
        currWalk.time = intent.getIntExtra("TIME",0);
        currWalk.track = intent.getIntExtra("TRACK",0);
        currWalk.device_position = intent.getIntExtra("DEVICE_POSITION",0);
        currWalk.walk_type = intent.getIntExtra("WALK_TYPE",0);

        currWalk.phone_model = Device.getDeviceName();
        currWalk.mAccl.sensor_model = mAccelerometer.getVendor() + " " + mAccelerometer.getName();
        currWalk.mGyro.sensor_model = mGyroscope.getVendor() + " " + mGyroscope.getName();

        Log.d(TAG,currWalk.toString());
    }

    private void resetSubject() {
        subject = null;
        currWalk = null;
    }

    private void setSubject(Intent intent) {
        subject = new Subject();
        subject.age = intent.getIntExtra("AGE",0);
        subject.weight = intent.getFloatExtra("WEIGHT",0);
        subject.height = intent.getFloatExtra("HEIGHT",0);
        subject.gender = intent.getIntExtra("GENDER",0);

//        Log.d(TAG,subject.toString());
    }

    private void sendTimerStoppedStatus(boolean cancelled) {
        Intent statusIntent = new Intent();
        statusIntent.setAction(TIMER_STOPPED);
        statusIntent.putExtra(WALK_CANCELLED, cancelled);
        if(!cancelled){
            statusIntent.putExtra("WALK_TYPE", currWalk.walk_type);
            statusIntent.putExtra("WALK_NO", subject.walks.size());
            statusIntent.putExtra("TRACK_TYPE", currWalk.track);
            statusIntent.putExtra("DEVICE_POSITION", currWalk.device_position);
            statusIntent.putExtra("TIME", currWalk.time);

        }

        sendBroadcast(statusIntent);
    }

    private void startTimer() {

        totalAcclEvent = 0;
        totalGyroEvent = 0;
        totalStepEvent = 0;
        totalRotEvent = 0;
        timeElapsed = 0;

        if(!register_sensors()){
            return;
        }

        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire(10*60*1000L /*10 minutes*/);
            wakeLockCount++;
        }else{
            return;
        }


        countDownTimer = new Timer();
        countDownTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Intent timerIntent = new Intent();
                timerIntent.setAction(TIMER_TICK);

                timeElapsed++;

                timerIntent.putExtra(TIME_ELAPSED, timeElapsed);
                sendBroadcast(timerIntent);

                if(timeElapsed >= currWalk.time){
                    stopTimer(false);
                }
            }
        }, 1000, 1000);

        isTimerRunning = true;
    }

    private void stopTimer(boolean walkCancelled) {

        if(!isTimerRunning)
            return;

        isTimerRunning = false;

        unregisterSensors();

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        if (!walkCancelled) {
            if (totalAcclEvent > 1)
                currWalk.mAccl.avg_delay /= (totalAcclEvent - 1);
            if (totalGyroEvent > 1)
                currWalk.mGyro.avg_delay /= (totalGyroEvent - 1);
            if( totalRotEvent > 1 )
                currWalk.mRot.avg_delay /= (totalRotEvent - 1);

            currWalk.mAccl.avg_sample_rate = (float) totalAcclEvent / currWalk.time;
            currWalk.mGyro.avg_sample_rate = (float) totalGyroEvent / currWalk.time;

            currWalk.mStep.totalSteps = totalStepEvent;

            subject.walks.add(currWalk);

        }

        //Log.d(TAG, "accl max delay : " + currWalk.mAccl.max_delay);
        //Log.d(TAG, "gyro max delay : " + currWalk.mGyro.max_delay);

//        Gson gson = new Gson();
//        try{
//            String json = gson.toJson(subject);
//            Log.d(TAG, json);
//            Log.d(TAG, "size : " + json.length());
//        }catch (Exception e){
//            Log.d(TAG,e.getMessage());
//        }

        sendTimerStoppedStatus(walkCancelled);
        releaseWakeLock();

    }


    /*
     * This function is responsible for building and returning a Notification with the current
     * state of the stopwatch along with the timeElapsed
     */
    private Notification buildNotification() {
        String title = isTimerRunning ? "Timer is running!" : "Timer is paused!";

        int hours = (int) (timeElapsed / 3600);
        int minutes = (int) ((timeElapsed % 3600) / 60);
        int seconds = (int) (timeElapsed % 60);

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setOngoing(true)
                .setContentText(String.format("%02d:%02d:%02d", hours, minutes, seconds))
                .setColorized(true)
                .setColor(Color.parseColor("#BEAEE2"))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOnlyAlertOnce(true)
                .setContentIntent(pIntent)
                .setAutoCancel(true)
                .build();
    }

    /*
     * This function uses the notificationManager to update the existing notification with the new notification
     */
    private void updateNotification() {
        notificationManager.notify(1, buildNotification());
    }

    private void moveToForeground() {
        if (isTimerRunning) {
            startForeground(1, buildNotification());

            updateTimer = new Timer();

            updateTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    updateNotification();
                }
            }, 0, 1000);
        }
    }

    private void moveToBackground() {
        if(updateTimer != null)
            updateTimer.cancel();
        stopForeground(true);
    }

    private final SensorEventListener gyroSensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {

                float timestamp;
                float delay;

                timestamp = (float) ((event.timestamp - startTime) * 1e-9);

                if(totalAcclEvent == 0){
                    delay = (float) ((event.timestamp - startTime) * 1e-6);
                }else
                    delay = (float) ((event.timestamp - prevTime_gyro) * 1e-6);

                currWalk.mGyro.avg_delay += delay;


                if( totalGyroEvent%40 == 0)
                    sendSensorData(1,event,timestamp);


//                Log.d(TAG,"gyro : timestamp : " + timestamp  + " delay : " + delay + " x = " + event.values[0] + " y = " +
//                        event.values[1] + " z = " + event.values[2] );
                prevTime_gyro = event.timestamp;

                GyroDatum newData = new GyroDatum(event.values[0], event.values[1], event.values[2]);
                newData.timestamp = timestamp * 1000;
                currWalk.mGyro.data.add(newData);

                if (currWalk.mGyro.max_delay < delay)
                    currWalk.mGyro.max_delay = delay;

                totalGyroEvent++;

            }

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Handle accuracy changes if needed
            Log.d(TAG,"Gyro accuracy : " + sensorAccuracy[accuracy]);
        }
    };

    private final SensorEventListener acclSensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {

                float timestamp;
                float delay;

                timestamp = (float) ((event.timestamp - startTime) * 1e-9);

                if(totalAcclEvent == 0){
                    delay = (float) ((event.timestamp - startTime) * 1e-6);
                }else
                    delay = (float) ((event.timestamp - prevTime_accl) * 1e-6);

                currWalk.mAccl.avg_delay += delay;


                if( totalAcclEvent%40 == 0)
                    sendSensorData(0,event,timestamp);

                Log.d(TAG, "accl : timestamp : " + timestamp + " delay : " + delay + " x = " + event.values[0] + " y = " +
                        event.values[1] + " z = " + event.values[2]);
                prevTime_accl = event.timestamp;

                AcclDatum newData = new AcclDatum(event.values[0], event.values[1], event.values[2]);
                newData.timestamp = timestamp * 1000;
                currWalk.mAccl.data.add(newData);

                if (currWalk.mAccl.max_delay < delay)
                    currWalk.mAccl.max_delay = delay;
                totalAcclEvent++;

            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Handle accuracy changes if needed
        }
    };

    private final SensorEventListener rotVecSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float timestamp;
            float delay;

            timestamp = (float) ((event.timestamp - startTime) * 1e-9);

            if(totalRotEvent == 0){
                delay = (float) ((event.timestamp - startTime) * 1e-6);
            }else
                delay = (float) ((event.timestamp - prevTime_rot) * 1e-6);

            currWalk.mRot.avg_delay += delay;

//            Log.d(TAG, " x = " + event.values[0] + " y = " +
//                    event.values[1] + " z = " + event.values[2]);

            prevTime_rot = event.timestamp;

            RotVecDatum newData = new RotVecDatum(event.values[0], event.values[1], event.values[2]);
            newData.timestamp = timestamp * 1000;
            currWalk.mRot.data.add(newData);

            if (currWalk.mRot.max_delay < delay)
                currWalk.mRot.max_delay = delay;

            totalRotEvent++;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Handle accuracy changes if needed
        }

    };

    private final SensorEventListener stepDetectorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float timestamp;
            timestamp = (float) ((event.timestamp - startTime) * 1e-9);

            StepDetectorDatum newData = new StepDetectorDatum();
            newData.timestamp = timestamp * 1000;
            currWalk.mStep.data.add(newData);
            totalStepEvent++;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Handle accuracy changes if needed
        }
    };

    private void sendSensorData(int type, SensorEvent event, float timestamp) {
        Intent statusIntent = new Intent();
        statusIntent.setAction(SENSOR_DATA);
        statusIntent.putExtra(SENSOR_TYPE, type);
        statusIntent.putExtra("VALUE1", event.values[0]);
        statusIntent.putExtra("VALUE2", event.values[1]);
        statusIntent.putExtra("VALUE3", event.values[2]);
        statusIntent.putExtra("TIMESTAMP", timestamp);
        sendBroadcast(statusIntent);
    }

}
