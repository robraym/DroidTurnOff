package com.droid.ray.droidturnoff;

import android.app.Activity;
import android.os.Bundle;

public class DroidTurnOffActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DroidCommon.turnOffScreen(getBaseContext());
        finish();
    }
}
