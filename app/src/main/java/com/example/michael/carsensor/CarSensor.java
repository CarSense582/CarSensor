/**
 * Created by michael on 4/23/15.
 */
package com.example.michael.carsensor;

import com.example.michael.dataserverlib.SensorData;

public class CarSensor extends SensorData {
    public int throttle, rpm, speed, load;
    CarSensor() {
        throttle = rpm = speed = load = 0;
    }
}