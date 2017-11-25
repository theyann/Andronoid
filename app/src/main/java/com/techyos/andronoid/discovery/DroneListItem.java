package com.techyos.andronoid.discovery;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.techyos.andronoid.R;


public class DroneListItem extends RecyclerView.ViewHolder {

    private ARDiscoveryDeviceService service;
    private TextView textDroneId;

    public DroneListItem(ViewGroup parent, final DroneListItemClickListener listener) {
        super(LayoutInflater.from(parent.getContext()).inflate(R.layout.drone_list_item, parent, false));

        textDroneId = itemView.findViewById(R.id.textDroneId);
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onClick(service);
                }
            }
        });
    }

    public void setDrone(ARDiscoveryDeviceService service) {
        this.service = service;
        textDroneId.setText(service.getName());
    }

    public interface DroneListItemClickListener {
        void onClick(ARDiscoveryDeviceService service);
    }
}
