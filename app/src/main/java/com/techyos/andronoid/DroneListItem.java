package com.techyos.andronoid;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;


public class DroneListItem extends RecyclerView.ViewHolder {

    private TextView textDroneId;

    public DroneListItem(ViewGroup parent) {
        super(LayoutInflater.from(parent.getContext()).inflate(R.layout.drone_list_item, parent, false));
        textDroneId = itemView.findViewById(R.id.textDroneId);
    }

    public void setDrone(ARDiscoveryDeviceService device) {
        textDroneId.setText(device.getName());
    }
}
