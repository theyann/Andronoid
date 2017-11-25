package com.techyos.andronoid;

import android.util.Log;

import com.parrot.arsdk.arcontroller.ARControllerException;
import com.parrot.arsdk.arcontroller.ARDeviceController;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDevice;

public class DroneRepository {

    private static final String TAG = "YLE-DroneRepository";

    private ARDeviceController controller;

    public void storeDevice(ARDiscoveryDevice device) {
        createController(device);
    }

    public ARDeviceController getDeviceController() {
        return controller;
    }

    private void createController(ARDiscoveryDevice device) {
        if (device != null) {
            try {
                Log.d(TAG, "createController: ");
                controller = new ARDeviceController(device);
                device.dispose();
                Log.d(TAG, "createController: controller = " + controller.toString());
            } catch (ARControllerException e) {
                Log.e(TAG, "createController: something went really wrong! " + e.getError().toString(), e);
            }
        }
    }
}
