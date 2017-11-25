package com.techyos.andronoid;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.parrot.arsdk.ardiscovery.ARDiscoveryDevice;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryException;
import com.parrot.arsdk.ardiscovery.ARDiscoveryService;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiver;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiverDelegate;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private ARDiscoveryService arDiscoveryService;
    private ServiceConnection arDiscoveryServiceConnection;
    private ARDiscoveryServicesDevicesListUpdatedReceiver receiver;

    private RecyclerView droneList;
    private DroneListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        droneList = findViewById(R.id.droneList);
        droneList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DroneListAdapter();
        droneList.setAdapter(adapter);

        findViewById(R.id.buttonDiscover).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initDiscoveryService();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceivers();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceivers();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeServices();
    }

    private void initDiscoveryService() {
        // create the service connection
        if (arDiscoveryServiceConnection == null) {
            arDiscoveryServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    arDiscoveryService = ((ARDiscoveryService.LocalBinder) service).getService();

                    startDiscovery();
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    arDiscoveryService = null;
                }
            };
        }

        if (arDiscoveryService == null) {
            // if the discovery service doesn't exists, bind to it
            Intent i = new Intent(getApplicationContext(), ARDiscoveryService.class);
            getApplicationContext().bindService(i, arDiscoveryServiceConnection, Context.BIND_AUTO_CREATE);
        } else {
            // if the discovery service already exists, start discovery
            startDiscovery();
        }
    }

    private void startDiscovery() {
        if (arDiscoveryService != null) {
            arDiscoveryService.start();
        }
    }

    private void registerReceivers() {
        receiver = new ARDiscoveryServicesDevicesListUpdatedReceiver(discoveryDelegate);
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastMgr.registerReceiver(receiver, new IntentFilter(ARDiscoveryService.kARDiscoveryServiceNotificationServicesDevicesListUpdated));
    }

    private final ARDiscoveryServicesDevicesListUpdatedReceiverDelegate discoveryDelegate =
            new ARDiscoveryServicesDevicesListUpdatedReceiverDelegate() {

                @Override
                public void onServicesDevicesListUpdated() {
                    if (arDiscoveryService != null) {
                        List<ARDiscoveryDeviceService> deviceList = arDiscoveryService.getDeviceServicesArray();
                        adapter.setDrones(deviceList);
                    }
                }
            };

    private ARDiscoveryDevice createDiscoveryDevice(@NonNull ARDiscoveryDeviceService service) {
        ARDiscoveryDevice device = null;
        try {
            device = new ARDiscoveryDevice(this, service);
        } catch (ARDiscoveryException e) {
            Log.e(TAG, "Exception", e);
        }

        return device;
    }

    private void unregisterReceivers() {
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastMgr.unregisterReceiver(receiver);
    }

    private void closeServices() {
        Log.d(TAG, "closeServices ...");

        if (arDiscoveryService != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    arDiscoveryService.stop();

                    getApplicationContext().unbindService(arDiscoveryServiceConnection);
                    arDiscoveryService = null;
                }
            }).start();
        }
    }


}
