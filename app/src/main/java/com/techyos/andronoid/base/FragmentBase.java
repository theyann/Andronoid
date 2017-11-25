package com.techyos.andronoid.base;

import android.support.v4.app.Fragment;

import com.techyos.andronoid.DroneRepository;
import com.techyos.andronoid.NavigationListener;


public class FragmentBase extends Fragment {

    private DroneRepository repository;
    private NavigationListener navListener;

    public DroneRepository getRepository() {
        return repository;
    }

    public void setRepository(DroneRepository repository) {
        this.repository = repository;
    }

    public NavigationListener getNavListener() {
        return navListener;
    }

    public void setNavListener(NavigationListener navListener) {
        this.navListener = navListener;
    }
}
