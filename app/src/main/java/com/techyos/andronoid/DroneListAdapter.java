package com.techyos.andronoid;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;

import java.util.ArrayList;
import java.util.List;

public class DroneListAdapter extends RecyclerView.Adapter<DroneListItem> {

    private List<ARDiscoveryDeviceService> drones = new ArrayList<>();

    public void setDrones(List<ARDiscoveryDeviceService> drones) {
        this.drones = drones;
        notifyDataSetChanged();
    }

    @Override
    public DroneListItem onCreateViewHolder(ViewGroup parent, int viewType) {
        return new DroneListItem(parent);
    }

    @Override
    public void onBindViewHolder(DroneListItem holder, int position) {
        holder.setDrone(drones.get(position));
    }

    @Override
    public int getItemCount() {
        return drones.size();
    }
}
