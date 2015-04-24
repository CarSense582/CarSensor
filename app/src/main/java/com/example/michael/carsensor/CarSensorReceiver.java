/**
 * Created by michael on 4/23/15.
 */
package com.example.michael.carsensor;

import android.content.Context;

import com.example.michael.dataserverlib.ContentManagerReceiver;

public class CarSensorReceiver extends ContentManagerReceiver<CarSensor> {
    @Override
    public CarSensor getSensor() {
        return new CarSensor();
    }
    @Override
    public String getServiceId(Context context) {
        return context.getResources().getString(R.string.service_id);
    }
}