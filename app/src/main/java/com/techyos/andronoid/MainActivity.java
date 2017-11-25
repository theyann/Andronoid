package com.techyos.andronoid;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.techyos.andronoid.base.FragmentBase;
import com.techyos.andronoid.command.CommandFragment;
import com.techyos.andronoid.discovery.DiscoveryFragment;

public class MainActivity extends AppCompatActivity implements NavigationListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_LOCATION = 666;

    private App application;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        application = (App) getApplication();

        setContentView(R.layout.activity_main);

        changeFragment(new DiscoveryFragment());
        checkPermission();
    }

    private void checkPermission() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Check Permissions Now
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION);
        }
    }

    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_LOCATION) {
            Log.d(TAG, "onRequestPermissionsResult: we've got permission!");
//            if(grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                // We can now safely use the API we requested access to
//            } else {
//                // Permission was denied or request was cancelled
//            }
        }
    }

    @Override
    public void discoveryFinished() {
        changeFragment(new CommandFragment());
    }

    private void changeFragment(FragmentBase fragment) {
        fragment.setRepository(application.getRepository());
        fragment.setNavListener(this);
        FragmentTransaction trx = getSupportFragmentManager().beginTransaction();
        trx.replace(R.id.container, fragment);
        trx.commit();
    }
}
