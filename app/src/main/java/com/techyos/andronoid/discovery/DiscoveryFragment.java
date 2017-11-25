package com.techyos.andronoid.discovery;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.parrot.arsdk.ardiscovery.ARDiscoveryDevice;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryException;
import com.parrot.arsdk.ardiscovery.ARDiscoveryService;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiver;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiverDelegate;
import com.techyos.andronoid.App;
import com.techyos.andronoid.R;
import com.techyos.andronoid.base.FragmentBase;

import java.util.List;


public class DiscoveryFragment extends FragmentBase implements DroneListItem.DroneListItemClickListener {

    private static final String TAG = "YLE-DiscoveryFragment";

    private ARDiscoveryService arDiscoveryService;
    private ServiceConnection arDiscoveryServiceConnection;
    private ARDiscoveryServicesDevicesListUpdatedReceiver receiver;

    private RecyclerView droneList;
    private DroneListAdapter adapter;
    private ProgressBar progressBar;
    private App application;
    private Handler handler;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        application = (App) getActivity().getApplication();
        handler = new Handler(getActivity().getMainLooper());

        View view = inflater.inflate(R.layout.fragment_discovery, container, false);

        progressBar = view.findViewById(R.id.progress);

        droneList = view.findViewById(R.id.droneList);
        droneList.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new DroneListAdapter(this);
        droneList.setAdapter(adapter);

        view.findViewById(R.id.buttonDiscover).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initDiscoveryService();
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: ");
        registerReceivers();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: ");
        unregisterReceivers();
    }

    private void initDiscoveryService() {
        Log.d(TAG, "initDiscoveryService: ");
        showProgress();

        // create the service connection
        if (arDiscoveryServiceConnection == null) {
            arDiscoveryServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    Log.d(TAG, "onServiceConnected: name = " + name.flattenToString());
                    arDiscoveryService = ((ARDiscoveryService.LocalBinder) service).getService();
                    startDiscovery();
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    Log.d(TAG, "onServiceDisconnected: ");
                    arDiscoveryService = null;
                }
            };
        }

        if (arDiscoveryService == null) {
            // if the discovery service doesn't exists, bind to it
            Intent i = new Intent(getActivity(), ARDiscoveryService.class);
            application.bindService(i, arDiscoveryServiceConnection, Context.BIND_AUTO_CREATE);
        } else {
            // if the discovery service already exists, start discovery
            startDiscovery();
        }
    }

    private void startDiscovery() {
        if (arDiscoveryService != null) {
            Log.d(TAG, "startDiscovery: starting");
            arDiscoveryService.start();
        } else {
            Log.d(TAG, "startDiscovery: already started");
        }
    }

    private void registerReceivers() {
        Log.d(TAG, "registerReceivers: ");
        receiver = new ARDiscoveryServicesDevicesListUpdatedReceiver(discoveryDelegate);
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(application);
        localBroadcastMgr.registerReceiver(receiver, new IntentFilter(ARDiscoveryService.kARDiscoveryServiceNotificationServicesDevicesListUpdated));
    }

    private final ARDiscoveryServicesDevicesListUpdatedReceiverDelegate discoveryDelegate =
            new ARDiscoveryServicesDevicesListUpdatedReceiverDelegate() {

                @Override
                public void onServicesDevicesListUpdated() {
                    if (arDiscoveryService != null) {
                        Log.d(TAG, "onServicesDevicesListUpdated: list updated");
                        final List<ARDiscoveryDeviceService> deviceList = arDiscoveryService.getDeviceServicesArray();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                adapter.setDrones(deviceList);
                                hideProgress();
                            }
                        });
                    } else {
                        Log.d(TAG, "onServicesDevicesListUpdated: discovery service is null");
                    }
                }
            };

    private ARDiscoveryDevice createDiscoveryDevice(@NonNull ARDiscoveryDeviceService service) {
        Log.d(TAG, "createDiscoveryDevice: ");
        ARDiscoveryDevice device = null;
        try {
            device = new ARDiscoveryDevice(getActivity(), service);
        } catch (ARDiscoveryException e) {
            Log.e(TAG, "Exception", e);
        }

        return device;
    }

    private void unregisterReceivers() {
        Log.d(TAG, "unregisterReceivers: ");
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(application);
        localBroadcastMgr.unregisterReceiver(receiver);
    }

    private void closeServices() {
        Log.d(TAG, "closeServices ...");

        if (arDiscoveryService != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    arDiscoveryService.stop();
                    if (arDiscoveryServiceConnection != null) {
                        application.unbindService(arDiscoveryServiceConnection);
                    }
                    arDiscoveryService = null;
                }
            }).start();
        }
    }

    private void showProgress() {
        progressBar.setVisibility(View.VISIBLE);
        droneList.setVisibility(View.GONE);
    }

    private void hideProgress() {
        progressBar.setVisibility(View.GONE);
        droneList.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(ARDiscoveryDeviceService service) {
        Log.d(TAG, "onClick: yeah \\o/");
        getRepository().storeDevice(createDiscoveryDevice(service));
        unregisterReceivers();
        closeServices();
        getNavListener().discoveryFinished();
    }

}
