package com.techyos.andronoid.command;

import android.app.ProgressDialog;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DICTIONARY_KEY_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_ERROR_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerArgumentDictionary;
import com.parrot.arsdk.arcontroller.ARControllerDictionary;
import com.parrot.arsdk.arcontroller.ARDeviceController;
import com.parrot.arsdk.arcontroller.ARDeviceControllerListener;
import com.parrot.arsdk.arcontroller.ARFeatureARDrone3;
import com.parrot.arsdk.arcontroller.ARFeatureCommon;
import com.parrot.arsdk.ardiscovery.ARDISCOVERY_PRODUCT_ENUM;
import com.parrot.arsdk.arsal.ARSALPrint;
import com.techyos.andronoid.DroneCommand;
import com.techyos.andronoid.R;
import com.techyos.andronoid.SimpleDrawingView;
import com.techyos.andronoid.base.FragmentBase;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.abs;
import static java.lang.Math.atan2;
import static java.lang.Math.sqrt;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CommandFragment extends FragmentBase implements View.OnClickListener, ARDeviceControllerListener {

    private static final String TAG = "YLE-CommandFragment";

    private Button takeOffToggle;
    private TextView textDroneStatus;
    private TextView textBatteryLevel;
    private boolean inFlight = false;
    private boolean changingState = false;
    private ARCONTROLLER_DEVICE_STATE_ENUM state = ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_STOPPED;
    private ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM flyingState = ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED;
    private ProgressDialog progressDialog;
    private Handler handler;
    private boolean exit = false;
    private ARDeviceController controller;

    private SimpleDrawingView mPathView;

    /**
     * Fragment lifeCycle
     */

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_command, container, false);

        handler = new Handler(getActivity().getMainLooper());
        controller = getRepository().getDeviceController();

        takeOffToggle = view.findViewById(R.id.buttonTakeOffToggle);
        takeOffToggle.setOnClickListener(this);
        view.findViewById(R.id.buttonExit).setOnClickListener(this);
        view.findViewById(R.id.buttonLandToggle).setOnClickListener(this);
        textDroneStatus = view.findViewById(R.id.textDroneStatus);
        textBatteryLevel = view.findViewById(R.id.textBatteryLevel);
        mPathView = view.findViewById(R.id.path);
        mPathView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {

                switch (e.getAction()) {
                    case MotionEvent.ACTION_UP:
                        List<PointF> path = simplifyPath(mPathView.getPoints());
                        debugPrintPath(path);

                        break;
                }

                return false;
            }
        });

        controller.addListener(this);
        updateButtonState();
        connect();
        return view;
    }


    private static final float MIN_DISTANCE = 50;

    private static List<PointF> simplifyPath(List<PointF> path) {
        List<PointF> simplified = new ArrayList<>();

        PointF current = new PointF(0,0);

        for (PointF pointF : path) {
            current.x += pointF.x;
            current.y += pointF.y;

            final double dist = sqrt(current.x * current.x + current.y * current.y);

            if (dist >= MIN_DISTANCE) {
                simplified.add(pointF);
                current.set(0, 0);
            }
        }

        return simplified;
    }

    private static final float SPEED_RATIO = 500f; // milliseconds/units
    private static final float ROTATION_RATIO = 100.0f; // milliseconds/radians
    private static final long IDLE_TIME = 1000;

    private static List<DroneCommand> buildCommands(final List<PointF> path) {
        List<DroneCommand> commands = new ArrayList<>();
        PointF current = new PointF(0,0);

        for (PointF pointF : path) {
            DroneCommand rotate = new DroneCommand();

            double signed_angle = atan2(pointF.y, pointF.x) - atan2(current.y, current.x);
            if (current.x * pointF.y - current.y * pointF.x < 0)
                rotate.mType = DroneCommand.Type.ROTATE_LEFT;
            else
                rotate.mType = DroneCommand.Type.ROTATE_RIGHT;
            rotate.mDuration = (long) (abs(signed_angle) * ROTATION_RATIO);

            DroneCommand forward = new DroneCommand();
            forward.mType = DroneCommand.Type.FORWARD;
            forward.mDuration = (long) (sqrt(pointF.x * pointF.x + pointF.y * pointF.y) * SPEED_RATIO);

            DroneCommand idle =  new DroneCommand();
            idle.mType = DroneCommand.Type.IDLE;
            idle.mDuration = IDLE_TIME;

            commands.add(rotate);
            commands.add(forward);
            commands.add(idle);
        }

        return commands;
    }

    private void debugPrintPath(final List<PointF> path) {
        StringBuilder stringBuilder = new StringBuilder();
        for (PointF pointF : path) {
            stringBuilder.append("{ " + pointF.x + ", " + pointF.y + " }, ");
        }
        Log.d(TAG, stringBuilder.toString());
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonTakeOffToggle:
                toggleTakeOffLanding();
                break;
            case R.id.buttonExit:
                disconnect();
                break;
            case R.id.buttonLandToggle:
                land();
                break;
        }
    }

    /**
     * Overridden methods
     */

    @Override
    public void onStateChanged(ARDeviceController deviceController, final ARCONTROLLER_DEVICE_STATE_ENUM newState, ARCONTROLLER_ERROR_ENUM error) {
        Log.d(TAG, "onStateChanged: newState = " + newState);
        state = newState;

        handler.post(new Runnable() {
            @Override
            public void run() {
                textDroneStatus.setText(newState.toString());
                checkState();
            }
        });
    }

    @Override
    public void onExtensionStateChanged(ARDeviceController deviceController, ARCONTROLLER_DEVICE_STATE_ENUM newState, ARDISCOVERY_PRODUCT_ENUM product, String name, ARCONTROLLER_ERROR_ENUM error) {

    }

    @Override
    public void onCommandReceived(ARDeviceController deviceController, ARCONTROLLER_DICTIONARY_KEY_ENUM commandKey, ARControllerDictionary elementDictionary) {
        Log.d(TAG, "onCommandReceived: commandKey = " + commandKey);
        if (elementDictionary != null) {
            // if the command received is a battery state changed
            switch (commandKey) {
                case ARCONTROLLER_DICTIONARY_KEY_COMMON_COMMONSTATE_BATTERYSTATECHANGED:
                    updateBatteryLevel(elementDictionary);
                    break;
                case ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED:
                    updateFlyingState(elementDictionary);
                    break;
            }
        } else {
            Log.e(TAG, "elementDictionary is null");
        }
    }

    /**
     * Control commands
     */

    private boolean connect() {
        showProgress("Connecting");

        boolean success = false;
        if (controller != null && ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_STOPPED.equals(state)) {
            ARCONTROLLER_ERROR_ENUM error = controller.start();
            if (error == ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK) {
                success = true;
            }
        }
        return success;
    }

    public boolean disconnect() {
        showProgress("Disconnecting");
        exit = true;

        boolean success = false;
        if (controller != null && ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING.equals(state)) {
            ARCONTROLLER_ERROR_ENUM error = controller.stop();
            hideProgress();
            if (error == ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK) {
                success = true;
            }
        }
        return success;
    }

    private void toggleTakeOffLanding() {
        Log.d(TAG, "toggleTakeOffLanding: ");
        if (inFlight) {
            land();
        } else {
            takeOff();
        }
    }

    private void takeOff() {
        takeOffToggle.setText(R.string.button_land);
        Log.d(TAG, "takeOff: controller extensionState = " + controller.getExtensionState().toString());

        if (controller != null &&
                state.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING)) {
            ARCONTROLLER_ERROR_ENUM error = controller.getFeatureMiniDrone().sendPilotingTakeOff();

            if (!error.equals(ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK)) {
                ARSALPrint.e(TAG, "Error while sending take off: " + error);
            }
        }
    }

    private void land() {
        takeOffToggle.setText(R.string.button_take_off);
        Log.d(TAG, "land: controller extensionState = " + controller.getExtensionState().toString());

        if (controller != null &&
                state.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING)) {
            ARCONTROLLER_ERROR_ENUM error = controller.getFeatureMiniDrone().sendPilotingLanding();

            if (!error.equals(ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK)) {
                ARSALPrint.e(TAG, "Error while sending land: " + error);
            }
        }
    }

    private void forward() {

        if (controller != null &&
                state.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING)) {
            ARCONTROLLER_ERROR_ENUM error = controller.getFeatureMiniDrone().sendPilotingLanding();

            if (!error.equals(ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK)) {
                ARSALPrint.e(TAG, "Error while sending land: " + error);
            }
        }
    }

    private ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM getPilotingState() {
        return flyingState;
    }

    private void updateBatteryLevel(ARControllerDictionary elementDictionary) {
        final Integer batValue = getIntValueFromDictionary(elementDictionary, ARFeatureCommon.ARCONTROLLER_DICTIONARY_KEY_COMMON_COMMONSTATE_BATTERYSTATECHANGED_PERCENT);
        handler.post(new Runnable() {
            @Override
            public void run() {
                String batteryLevel = getString(R.string.drone_battery_level);
                textBatteryLevel.setText(String.format(Locale.getDefault(), "%s: %d%s", batteryLevel, batValue, "%"));
            }
        });
    }

    private void updateFlyingState(ARControllerDictionary elementDictionary) {
        Integer flyingStateInt = getIntValueFromDictionary(elementDictionary, ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE);
        final ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM flyingState = ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.getFromValue(flyingStateInt);
        handler.post(new Runnable() {
            @Override
            public void run() {
                CommandFragment.this.flyingState = flyingState;
                textDroneStatus.setText(flyingState.toString());
                switch (flyingState) {
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_TAKINGOFF:
                        changingState = true;
                        break;
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED:
                        changingState = false;
                        inFlight = false;
                        break;
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDING:
                        changingState = true;
                        break;
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:
                        changingState = false;
                        inFlight = true;
                        break;
                }
                updateButtonState();
            }
        });
    }

    private Integer getIntValueFromDictionary(ARControllerDictionary elementDictionary, String key) {
        Integer value = -1;
        ARControllerArgumentDictionary<Object> args = elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);
        if (args != null) {
            value = (Integer) args.get(key);
        }
        return value;
    }

    private void updateButtonState() {
        takeOffToggle.setEnabled(!changingState);
        if (!changingState) {
            takeOffToggle.setText(inFlight ? R.string.button_land : R.string.button_take_off);
        }
    }

    private void showProgress(String message) {
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void hideProgress() {
        progressDialog.dismiss();
    }

    private void checkState() {
        switch (state) {
            case ARCONTROLLER_DEVICE_STATE_RUNNING:
                progressDialog.dismiss();
                break;
            case ARCONTROLLER_DEVICE_STATE_STOPPED:
                progressDialog.dismiss();
                if (exit) {
                    getActivity().finish();
                }
                break;
        }
    }




















    /**
     * Created by florien on 26/11/17.
     */

    public class CommandExecutor implements Runnable {

        private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        private List<DroneCommand> commands;
        private long timeLeft;
        private long previousRunningTime;

        private int idx;
        private DroneCommand current;


        @Override
        public void run() {
            long delta = System.nanoTime() / 1000 - previousRunningTime;
            timeLeft -= delta;
            if (timeLeft < 0) {
                current = commands.get(idx);
                idx ++;
                timeLeft = current.mDuration;

                switch (current.mType) {
                    case IDLE:
                        fullStop();
                        break;
                    case FORWARD:
                        forward();
                        break;
                    case ROTATE_LEFT:
                        rotateLeft();
                        break;
                    case ROTATE_RIGHT:
                        rotateRight();
                        break;
                }
            }
        }

        private void fullStop() {

        }

        public void start(List<DroneCommand> commands) {
            this.commands = commands;
            previousRunningTime = System.nanoTime() / 1000;
            idx = 0;
            current = null;
            timeLeft = 0;
            executor.scheduleAtFixedRate(this, 0, 10, TimeUnit.MILLISECONDS);
        }
    }




}
