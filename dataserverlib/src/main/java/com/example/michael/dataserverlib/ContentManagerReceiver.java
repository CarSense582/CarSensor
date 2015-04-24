/**
 * Created by michael on 4/22/15.
 */

package com.example.michael.dataserverlib;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.example.michael.dataserverlib.SensorData;

import java.util.ArrayList;

abstract public class ContentManagerReceiver<T extends SensorData> extends BroadcastReceiver {
    abstract public T getSensor();
    abstract public String getServiceId(Context c);
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction() == DataServerLibConstants.CM_BROADCAST_ID) {
            Bundle results = getResultExtras(true);
            //Add this service id to list
            ArrayList<String> otherServices = results.getStringArrayList(DataServerLibConstants.CM_BROADCAST_SERVICES);
            if (otherServices == null) {
                otherServices = new ArrayList<String>();
            }
            String serviceId = getServiceId(context);
            otherServices.add(serviceId);
            results.putStringArrayList(DataServerLibConstants.CM_BROADCAST_SERVICES, otherServices);
            //Add service dependent map
            results.putSerializable(serviceId, getSensor().getFields());
        }
    }

}