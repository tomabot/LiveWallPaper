package com.itad230.lwtech.livewallpaper;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;
import android.widget.Toast;

/**
 * Created by tomabot on 4/29/15.
 */
public class GLWallpaperService extends WallpaperService {
    @Override
    public Engine onCreateEngine() {
        return new GLEngine();
    }

    /**
     * When WallpaperService is created by the system, it will create its own
     * rendering surface for the application to draw on. It will then initialize
     * the wallpaper engine via a call to onCreate() and pass this surface in as
     * a SurfaceHolder, which is an abstract interface to the surface.
     */
    public class GLEngine extends Engine {
        private WallpaperGLSurfaceView glSurfaceView;
        private boolean rendererSet;

        class WallpaperGLSurfaceView extends GLSurfaceView {
            WallpaperGLSurfaceView(Context context) {
                super(context);
            }

            @Override
            public SurfaceHolder getHolder() {
                return getSurfaceHolder();
            }

            public void onWallpaperDestroy() {
                super.onDetachedFromWindow();
            }
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            glSurfaceView = new WallpaperGLSurfaceView(GLWallpaperService.this);

            // Check if the system supports OpenGL ES 2.0.
            ActivityManager activityManager =
                    (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            ConfigurationInfo configurationInfo = activityManager
                    .getDeviceConfigurationInfo();

            final boolean supportsEs2 =
                    configurationInfo.reqGlEsVersion >= 0x20000
                            || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
                            && (Build.FINGERPRINT.startsWith("generic")
                            || Build.FINGERPRINT.startsWith("unknown")
                            || Build.MODEL.contains("google_sdk")
                            || Build.MODEL.contains("Emulator")
                            || Build.MODEL.contains("Android SDK built for x86")));

            final LiveWallpaperRenderer liveWallPaperRenderer =
                    new LiveWallpaperRenderer(GLWallpaperService.this);

            if (supportsEs2) {
                glSurfaceView.setEGLContextClientVersion(2);
                glSurfaceView.setRenderer(liveWallPaperRenderer);
                rendererSet = true;
            } else {
                Toast.makeText(GLWallpaperService.this,
                        "This device does not support OpenGL ES 2.0.",
                        Toast.LENGTH_LONG).show();
                return;
            }

        }

        /**
         * Called whenever the live wallpaper becomes visible or hidden
         */
        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if(rendererSet) {
                if (visible) {
                    glSurfaceView.onResume();
                } else {
                    glSurfaceView.onPause();
                }
            }
        }

        /**
         * Called when the live wallpaper is destroyed
         */
        @Override
        public void onDestroy() {
            super.onDestroy();
            glSurfaceView.onWallpaperDestroy();
        }
    }

}
