package com.example.sensorx;

import static com.example.sensorx.Chart.addEntry;
import static com.example.sensorx.Chart.clearGraph;
import static com.example.sensorx.Chart.init_chart;
import static com.example.sensorx.Chart.enableTouch;


import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import com.github.mikephil.charting.charts.LineChart;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputLayout;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MAIN";
    private static final String[] WALK_TYPE = {"Walk","Run"};
    private static final String[] TRACK_TYPE = {"Flat","Slope","Stairs"};
    private static final String[] DEVICE_POSITION = {"FrontPocket","BackPocket","OnHand","HandBag",
            "LowerThigh","Knee","Shin","Ankle","Foot"};

    private BroadcastReceiver statusReceiver;
    private BroadcastReceiver timeReceiver;
    private BroadcastReceiver connReceiver;
    private BroadcastReceiver sensorDataReceiver;
    private LineChart chart_accl_x;
    private LineChart chart_accl_y;
    private LineChart chart_accl_z;

    private LineChart chart_gyro_x;
    private LineChart chart_gyro_y;
    private LineChart chart_gyro_z;

    private SwitchMaterial startSwitch;
    private TextView timerText,connStatText;
    private Spinner gender_dropDown;
    private Spinner track_dropdown;
    private Spinner sensor_pos_dropdown;
    private Spinner walk_type_dropdown;

    private Button setSubjectButton, resetSubjectButton, setWalkButton, resetWalkButton, stopTimerButton;

    private TextInputLayout ageInput, weightInput, heightInput, timerInput;
    private List<ListItem> walkList;
    private ListAdapter adapter;
    private RecyclerView recyclerView;
    private View sendDataButton;

    private void clearData() {

        clearGraph(chart_accl_x);
        clearGraph(chart_accl_y);
        clearGraph(chart_accl_z);

        clearGraph(chart_gyro_x);
        clearGraph(chart_gyro_y);
        clearGraph(chart_gyro_z);

    }

    private void addWalk(String text) {
        walkList.add(new ListItem(text));
        adapter.notifyItemInserted(walkList.size() - 1);
    }

    private void init(){
        walkList = new ArrayList<>();

        // Setting up the RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setLayoutParams(new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT));
        adapter = new ListAdapter(walkList,MainActivity.this);
        recyclerView.setAdapter(adapter);

        connStatText = findViewById(R.id.connection_stat);
        setSubjectButton = findViewById(R.id.set_btn);
        resetSubjectButton = findViewById(R.id.reset_btn);
        setWalkButton = findViewById(R.id.set_btn2);
        resetWalkButton = findViewById(R.id.reset_btn2);
        stopTimerButton = findViewById(R.id.stop_timer_btn);
        ageInput = findViewById(R.id.age_input);
        heightInput = findViewById(R.id.height_input);
        weightInput = findViewById(R.id.weight_input);
        timerInput = findViewById(R.id.timer_input);
        gender_dropDown = findViewById(R.id.gender_spinner);
        String[] items = new String[]{"Male", "Female"};
        gender_dropDown.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items));

        track_dropdown = findViewById(R.id.track_spinner);
        items = new String[]{"Flat Track", "Slope Track", "Stairs"};
        track_dropdown.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items));

        sensor_pos_dropdown = findViewById(R.id.sensor_pos_spinner);
        items = new String[]{"Front Pocket", "Back Pocket", "On Hand", "Hand Bag", "Lower Thigh", "Knee", "Shin", "Ankle", "Foot"};
        sensor_pos_dropdown.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items));

        walk_type_dropdown = findViewById(R.id.walk_type_spinner);
        items = new String[]{"walk", "run"};
        walk_type_dropdown.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items));

        startSwitch = findViewById(R.id.material_switch);
        timerText = findViewById(R.id.timer);

        chart_accl_x = findViewById(R.id.chart_accl_x);
        chart_accl_y = findViewById(R.id.chart_accl_y);
        chart_accl_z = findViewById(R.id.chart_accl_z);

        chart_gyro_x = findViewById(R.id.chart_gyro_x);
        chart_gyro_y = findViewById(R.id.chart_gyro_y);
        chart_gyro_z = findViewById(R.id.chart_gyro_z);

        startSwitch.setEnabled(false);
        setWalkButton.setEnabled(false);
        resetWalkButton.setEnabled(false);
        timerInput.setEnabled(false);
        sensor_pos_dropdown.setEnabled(false);
        track_dropdown.setEnabled(false);
        walk_type_dropdown.setEnabled(false);
        stopTimerButton.setEnabled(false);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        myToolbar.setBackgroundColor(Color.rgb(103, 58, 183));
        myToolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(myToolbar);
    }


    private void registerReceivers() {

        IntentFilter statusFilter = new IntentFilter();
        statusFilter.addAction(SensorService.TIMER_STOPPED);
        statusReceiver = new BroadcastReceiver() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean walkCancelled = intent.getBooleanExtra(SensorService.WALK_CANCELLED, false);
                int walk_type = intent.getIntExtra("WALK_TYPE",0);
                int walkNo = intent.getIntExtra("WALK_NO",0);
                int track = intent.getIntExtra("TRACK_TYPE",0);
                int dev_pos = intent.getIntExtra("DEVICE_POSITION",0);
                int time = intent.getIntExtra("TIME",0);

                updateUI_timerStopped(walkCancelled,walk_type,walkNo,track,dev_pos,time);
            }
        };
        registerReceiver(statusReceiver, statusFilter);

        // Receiving time values from service
        IntentFilter timeFilter = new IntentFilter();
        timeFilter.addAction(SensorService.TIMER_TICK);
        timeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int timeElapsed = intent.getIntExtra(SensorService.TIME_ELAPSED, 0);
                updateTimerValue(timeElapsed);
            }
        };
        registerReceiver(timeReceiver, timeFilter);

        IntentFilter connFilter = new IntentFilter();
        connFilter.addAction(SensorService.API_CONN);
        connReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String connType = intent.getStringExtra("TYPE");
                boolean success = intent.getBooleanExtra("SUCCESS",false);
                assert connType != null;
                if(connType.equals(SensorService.TEST_CONN)){
                   if(success){
                       Toast.makeText(MainActivity.this, "Server is online", Toast.LENGTH_LONG).show();
                   }else{
                       Toast.makeText(MainActivity.this, "Error: Can't Connect to server", Toast.LENGTH_LONG).show();
                   }
                }else if (connType.equals(SensorService.SEND_DATA)){
                    String msg = intent.getStringExtra("MESSAGE");
                    if(success){
                        connStatText.setText(msg);
                    }else{
                        connStatText.setText(msg);
                    }
                }
            }
        };
        registerReceiver(connReceiver, connFilter);

        IntentFilter sensorDataFilter = new IntentFilter();
        sensorDataFilter.addAction(SensorService.SENSOR_DATA);
        sensorDataReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int sensorType = intent.getIntExtra(SensorService.SENSOR_TYPE,0);
                float[] values = {0,0,0};
                values[0] = intent.getFloatExtra("VALUE1",0);
                values[1] = intent.getFloatExtra("VALUE2",0);
                values[2] = intent.getFloatExtra("VALUE3",0);
                float timestamp = intent.getFloatExtra("TIMESTAMP",-1);
                if(timestamp != -1){
                    if(sensorType == 0){
                        addEntry_accl(values,timestamp);
                    }else{
                        addEntry_gyro(values,timestamp);
                    }
                }

            }
        };
        registerReceiver(sensorDataReceiver, sensorDataFilter);


    }

    private boolean setup_sensors(){
        SensorManager mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (mSensorManager == null) {
            return false;
        }

        Sensor mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        Sensor mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        return mAccelerometer != null && mGyroscope != null;
    }

    private void printSensorInfo(Sensor deviceSensor){
        Log.d(TAG,"Type : " + deviceSensor.getType());
        Log.d(TAG, deviceSensor.toString());
        Log.d(TAG,"Max Delay : " + deviceSensor.getMaxDelay() + "Min Delay : " + deviceSensor.getMinDelay());
        Log.d(TAG,"Power : " + deviceSensor.getPower());
        Log.d(TAG,"Res : " + deviceSensor.getResolution());
        Log.d(TAG,"Max Range : " + deviceSensor.getMaximumRange());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG,"HighestDirectReportRateLevel :"  + deviceSensor.getHighestDirectReportRateLevel());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.d(TAG,"Dynamic : " + deviceSensor.isDynamicSensor());
        }
        Log.d(TAG,"wake up : " + deviceSensor.isWakeUpSensor());
    }

    private void test2(int type){
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor deviceSensor = sensorManager.getDefaultSensor(type);
        if(deviceSensor != null){
            printSensorInfo(deviceSensor);
        }else{
            Log.d(TAG,"No sensor");
        }

    }

    private void test(){
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        List<Sensor> deviceSensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
        for(Sensor s : deviceSensors){
            Log.d(TAG,"Type : " + s.getType());
            Log.d(TAG, s.toString());
            Log.d(TAG,"Max Delay : " + s.getMaxDelay() + "Min Delay : " + s.getMinDelay());
            Log.d(TAG,"Power : " + s.getPower());
            Log.d(TAG,"Res : " + s.getResolution());
            Log.d(TAG,"Max Range : " + s.getMaximumRange());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(TAG,"HighestDirectReportRateLevel :"  + s.getHighestDirectReportRateLevel());
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Log.d(TAG,"Dynamic : " + s.isDynamicSensor());
            }
            Log.d(TAG,"wake up : " + s.isWakeUpSensor());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        test2(Sensor.TYPE_HEART_RATE);
//        test2(Sensor.TYPE_ROTATION_VECTOR);

        init();
        if(!setup_sensors()){
            Toast.makeText(this, "Error : no Sensors", Toast.LENGTH_LONG).show();
            setSubjectButton.setEnabled(false);
            return;
        }

        registerReceivers();

        setSubjectButton.setOnClickListener((view) -> setSubject());
        resetSubjectButton.setOnClickListener((v -> resetSubject()));
        setWalkButton.setOnClickListener((v -> setWalk()));
        resetWalkButton.setOnClickListener((v -> resetWalk()));
        
        Button testConnButton = findViewById(R.id.testConnButton);
        testConnButton.setOnClickListener((v)->testConn());
        sendDataButton = findViewById(R.id.send_data_btn);
        sendDataButton.setOnClickListener((v -> sendData()));

        startSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                startTimer();
            }
        });

        stopTimerButton.setOnClickListener((view) -> stopTimer());

        testConnButton.performClick();
    }



    private void sendData() {
        Intent service = new Intent(this, SensorService.class);
        service.putExtra(SensorService.SERVICE_ACTION, SensorService.SEND_DATA);
        startService(service);
    }

    private void testConn() {
        Intent service = new Intent(this, SensorService.class);
        service.putExtra(SensorService.SERVICE_ACTION, SensorService.TEST_CONN);
        startService(service);
    }

    private void resetWalk() {

        Intent service = new Intent(this, SensorService.class);
        service.putExtra(SensorService.SERVICE_ACTION, SensorService.RESET_WALK);
        startService(service);

        Objects.requireNonNull(timerInput.getEditText()).setText("");
        setWalkButton.setEnabled(true);
        timerInput.setEnabled(true);
        track_dropdown.setEnabled(true);
        sensor_pos_dropdown.setEnabled(true);
        walk_type_dropdown.setEnabled(true);
        track_dropdown.setSelection(0);
        sensor_pos_dropdown.setSelection(0);
        walk_type_dropdown.setSelection(0);
        startSwitch.setEnabled(false);
        connStatText.setText("");
        timerText.setText("");

    }

    private void setWalk() {
        try {
            Walk currWalk = new Walk();
            String input = Objects.requireNonNull(timerInput.getEditText()).getText().toString();
            currWalk.time = (Integer.parseInt(input))*60 ;
//            Log.d(TAG,"time =======" + currWalk.time);
            currWalk.track = track_dropdown.getSelectedItemPosition();
            currWalk.device_position = sensor_pos_dropdown.getSelectedItemPosition();
            currWalk.walk_type = walk_type_dropdown.getSelectedItemPosition();

            Intent service = new Intent(this, SensorService.class);
            service.putExtra(SensorService.SERVICE_ACTION, SensorService.SET_WALK);
            service.putExtra("TIME",currWalk.time);
            service.putExtra("TRACK",currWalk.track);
            service.putExtra("DEVICE_POSITION",currWalk.device_position);
            service.putExtra("WALK_TYPE",currWalk.walk_type);
            startService(service);

            setWalkButton.setEnabled(false);
            timerInput.setEnabled(false);
            track_dropdown.setEnabled(false);
            sensor_pos_dropdown.setEnabled(false);
            walk_type_dropdown.setEnabled(false);
            startSwitch.setEnabled(true);
            connStatText.setText("");
            timerText.setText(String.valueOf(currWalk.time));

        } catch (NumberFormatException ne) {
            Toast.makeText(MainActivity.this, "invalid input", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "unknown error!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Receiving stopwatch status from service

    }


    @Override
    protected void onStart() {
        super.onStart();
        moveToBackground();
    }

    @Override
    protected void onPause() {
        super.onPause();
//        unregisterReceiver(statusReceiver);
//        unregisterReceiver(timeReceiver);

        // Moving the service to foreground when the app is in background / not visible
        moveToForeground();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        moveToBackground();
        try{
            unregisterReceiver(statusReceiver);
            unregisterReceiver(timeReceiver);
            unregisterReceiver(connReceiver);
            unregisterReceiver(sensorDataReceiver);
        }catch (Exception e){
            Log.d(TAG,"already unregistered");
        }
        stopService();
    }

    public void stopService() {
        Intent serviceIntent = new Intent(this, SensorService.class);
        stopService(serviceIntent);
    }

    private void addEntry_accl(float[] values, float timestamp) {

        addEntry(chart_accl_x, timestamp, values[0]);
        addEntry(chart_accl_y, timestamp, values[1]);
        addEntry(chart_accl_z, timestamp, values[2]);

    }

    private void addEntry_gyro(float[] values, float timestamp) {

        addEntry(chart_gyro_x, timestamp, values[0]);
        addEntry(chart_gyro_y, timestamp, values[1]);
        addEntry(chart_gyro_z, timestamp, values[2]);

    }

    private void updateTimerValue(int timeElapsed) {
        int time = Integer.parseInt(Objects.requireNonNull(timerInput.getEditText()).getText().toString())*60;
        timerText.setText( String.valueOf(time - timeElapsed));
    }

    private void updateUI_timerStopped(boolean walkCancelled, int walk_type, int walkNo, int track, int device_position, int time) {

        enableTouch(chart_accl_x);
        enableTouch(chart_accl_y);
        enableTouch(chart_accl_z);
        enableTouch(chart_gyro_x);
        enableTouch(chart_gyro_y);
        enableTouch(chart_gyro_z);
        timerText.setText("");
        resetWalkButton.performClick();

        resetSubjectButton.setEnabled(true);
        resetWalkButton.setEnabled(true);
        stopTimerButton.setEnabled(false);
        sendDataButton.setEnabled(true);

        startSwitch.setChecked(false);
        if(walkCancelled){
            Toast.makeText(this, "Walk Cancelled", Toast.LENGTH_LONG).show();
        }else{
            addWalk( WALK_TYPE[walk_type] + (walkNo+1) + ": track=" + TRACK_TYPE[track]
                    + " dev_pos=" + DEVICE_POSITION[device_position] + " time=" + time/60);
        }

        adapter.enableButton();

    }


    private void startTimer() {

        stopTimerButton.setEnabled(true);
        resetSubjectButton.setEnabled(false);
        resetWalkButton.setEnabled(false);
        startSwitch.setEnabled(false);
        sendDataButton.setEnabled(false);
        adapter.disableButton();


        // The switch is checked.
        clearData();
        init_chart(chart_accl_x, "Real Time Accl-X Data Plot");
        init_chart(chart_accl_y, "Real Time Accl-Y Data Plot");
        init_chart(chart_accl_z, "Real Time Accl-Z Data Plot");

        init_chart(chart_gyro_x, "Real Time GYRO-X Data Plot");
        init_chart(chart_gyro_y, "Real Time GYRO-Y Data Plot");
        init_chart(chart_gyro_z, "Real Time GYRO-Z Data Plot");

        Intent service = new Intent(this, SensorService.class);
        service.putExtra(SensorService.SERVICE_ACTION, SensorService.START);
        startService(service);

    }

    private void stopTimer(){
        Intent service = new Intent(this, SensorService.class);
        service.putExtra(SensorService.SERVICE_ACTION, SensorService.STOP);
        startService(service);
    }

    private void setSubject(){
        try {
            Subject subject = new Subject();
            String input = Objects.requireNonNull(ageInput.getEditText()).getText().toString();
            subject.age = Integer.parseInt(input);
            input = Objects.requireNonNull(weightInput.getEditText()).getText().toString();
            if (!input.equals("")) {
                subject.weight = Float.parseFloat(input);
            }
            input = Objects.requireNonNull(heightInput.getEditText()).getText().toString();
            if (!input.equals("")) {
                subject.height = Float.parseFloat(input);
            }
            subject.gender = gender_dropDown.getSelectedItemPosition();

            Intent service = new Intent(this, SensorService.class);
            service.putExtra(SensorService.SERVICE_ACTION, SensorService.SET_SUBJECT);
            service.putExtra("AGE",subject.age);
            service.putExtra("HEIGHT",subject.weight);
            service.putExtra("WEIGHT",subject.height);
            service.putExtra("GENDER",subject.gender);
            startService(service);

            setSubjectButton.setEnabled(false);
            ageInput.setEnabled(false);
            weightInput.setEnabled(false);
            heightInput.setEnabled(false);
            gender_dropDown.setEnabled(false);

            setWalkButton.setEnabled(true);
            resetWalkButton.setEnabled(true);
            timerInput.setEnabled(true);
            sensor_pos_dropdown.setEnabled(true);
            walk_type_dropdown.setEnabled(true);
            track_dropdown.setEnabled(true);
            connStatText.setText("");

        } catch (NumberFormatException ne) {
            Toast.makeText(MainActivity.this, "invalid input", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "unknown error!", Toast.LENGTH_LONG).show();
        }
    }

    private void resetSubject() {
        Intent service = new Intent(this, SensorService.class);
        service.putExtra(SensorService.SERVICE_ACTION, SensorService.RESET_SUBJECT);
        startService(service);
        Objects.requireNonNull(ageInput.getEditText()).setText("");
        Objects.requireNonNull(weightInput.getEditText()).setText("");
        Objects.requireNonNull(heightInput.getEditText()).setText("");
        Objects.requireNonNull(timerInput.getEditText()).setText("");

        setSubjectButton.setEnabled(true);
        ageInput.setEnabled(true);
        weightInput.setEnabled(true);
        heightInput.setEnabled(true);
        gender_dropDown.setEnabled(true);
        gender_dropDown.setSelection(0);

        track_dropdown.setEnabled(false);
        track_dropdown.setSelection(0);
        sensor_pos_dropdown.setEnabled(false);
        sensor_pos_dropdown.setSelection(0);
        walk_type_dropdown.setEnabled(false);
        walk_type_dropdown.setSelection(0);
        setWalkButton.setEnabled(false);
        resetWalkButton.setEnabled(false);
        startSwitch.setEnabled(false);
        timerInput.setEnabled(false);
        connStatText.setText("");
        adapter.clear();
        timerText.setText("");
    }

    private void moveToForeground() {
        Intent service = new Intent(this, SensorService.class);
        service.putExtra(SensorService.SERVICE_ACTION, SensorService.MOVE_TO_FOREGROUND);
        startService(service);
    }

    private void moveToBackground() {
        Intent service = new Intent(this, SensorService.class);
        service.putExtra(SensorService.SERVICE_ACTION, SensorService.MOVE_TO_BACKGROUND);
        startService(service);
    }

    public void sendDeleteCommand(int indx) {
        Intent service = new Intent(this, SensorService.class);
        service.putExtra(SensorService.SERVICE_ACTION, SensorService.DELETE_WALK);
        service.putExtra("INDEX", indx);
        startService(service);
    }
}