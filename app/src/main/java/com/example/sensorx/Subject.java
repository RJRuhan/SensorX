package com.example.sensorx;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.ArrayList;

class Subject {
    public int age, gender;
    public float weight , height ;

    @NonNull
    @Override
    public String toString() {
        return "Subject{" +
                "age=" + age +
                ", gender=" + gender +
                ", height=" + height +
                ", weight=" + weight +
                '}';
    }

    public ArrayList<Walk> walks = new ArrayList<>();
}

class Walk {
    public long startTime;
    public int track;
    public int device_position;
    public int time;
    public int walk_type;
    public String phone_model;
    public Accelerometer mAccl;
    public Gyroscope mGyro;
    public StepDetector mStep;
    public RotationVectorSensor mRot;

    public Walk() {
        mAccl = new Accelerometer();
        mGyro = new Gyroscope();
        mStep = new StepDetector();
        mRot = new RotationVectorSensor();
    }

    @Override
    public String toString() {
        return "Walk{" +
                "track=" + track +
                ", device_position=" + device_position +
                ", time=" + time +
                ", walk_type=" + walk_type +
                ", phone_model='" + phone_model + '\'' +
                '}';
    }
}

class SensorInfo {
    public ArrayList<Datum> data = new ArrayList<>();
    public String sensor_model;
    public float avg_delay, avg_sample_rate, max_delay;

}

class Accelerometer extends SensorInfo {
    public float accl_net_avg, accl_net_dev, accl_x_avg, accl_y_avg, accl_z_avg,
            accl_x_dev, accl_y_dev, accl_z_dev;
}

class Gyroscope extends SensorInfo {
    public float gyro_net_avg, gyro_net_dev, gyro_x_avg, gyro_y_avg, gyro_z_avg,
            gyro_x_dev, gyro_y_dev, gyro_z_dev;
}

class RotationVectorSensor extends SensorInfo {

}


class StepDetector extends SensorInfo{
    public long totalSteps;
}





abstract class Datum {
    public float timestamp;
}

class AcclDatum extends Datum {
    public float accl_x, accl_y, accl_z;

    public AcclDatum(float accl_x, float accl_y, float accl_z) {
        this.accl_x = accl_x;
        this.accl_y = accl_y;
        this.accl_z = accl_z;
    }

}

class GyroDatum extends Datum {
    public float gyro_x, gyro_y, gyro_z;

    public GyroDatum(float gyro_x, float gyro_y, float gyro_z) {
        this.gyro_x = gyro_x;
        this.gyro_y = gyro_y;
        this.gyro_z = gyro_z;
    }
}

class RotVecDatum extends Datum {
    public float rot_x, rot_y, rot_z;

    public RotVecDatum(float rot_x, float rot_y, float rot_z) {
        this.rot_x = rot_x;
        this.rot_y = rot_y;
        this.rot_z = rot_z;
    }
}

class StepDetectorDatum extends Datum {

}
