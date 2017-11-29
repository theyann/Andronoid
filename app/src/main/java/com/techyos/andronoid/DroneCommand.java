package com.techyos.andronoid;

/**
 * Created by philippesimons on 25/11/2017.
 */

public class DroneCommand {

    public enum Type {
        IDLE,
        ROTATE_LEFT,
        ROTATE_RIGHT,
        FORWARD
    }

    public Type mType;
    public long mDuration;
}
