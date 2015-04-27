package com.itad230.lwtech.usingshaders;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;


public class UsingShadersActivity extends ActionBarActivity {
    // ...1st, add these two member variables...
    private GLSurfaceView glSurfaceView;
    private boolean rendererSet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ...2nd, add this call...
        glSurfaceView = new GLSurfaceView(this);

        // ...3rd, see if device supports OpenGL ES 2.0...
        final ActivityManager activityManager =
                (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        final ConfigurationInfo configurationInfo =
                activityManager.getDeviceConfigurationInfo();

        // ...4th, only problem is, this doesn't work with the emulator
        //final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;

        // ...4th (continued), use a method that the emulator can handle (rumor
        // has it that the activity manager function calls won't work on the emulator
        final boolean supportsEs2 =
                configurationInfo.reqGlEsVersion >= 0x20000
                        || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
                        && (Build.FINGERPRINT.startsWith("generic")
                        || Build.FINGERPRINT.startsWith("unknown")
                        || Build.MODEL.contains("google_sdk")
                        || Build.MODEL.contains("Emulator")
                        || Build.MODEL.contains("Android SDK built for x86")));

        // ...5th, if the device supports ES 2...
        if (supportsEs2) {
            // get an OpenGL ES 2 compatible context
            glSurfaceView.setEGLContextClientVersion(2);

            // Assign a renderer
            glSurfaceView.setRenderer(new UsingShadersRenderer(this));
            rendererSet = true;
        } else {
            Toast.makeText(this, "This device does not support OpenGL ES 2.0.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // ...6th, replace this call...
        // setContentView(R.layout.activity_week3);
        // ...with this
        setContentView(glSurfaceView);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_using_shaders, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // ...7th, add these two lifecycle event handlers so the surface view
    // can pause and resume the background rendering thread, as well as release
    // and renew the OpenGL context
    @Override
    protected void onPause() {
        super.onPause();
        if(rendererSet) {
            glSurfaceView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (rendererSet) {
            glSurfaceView.onResume();
        }
    }
}
