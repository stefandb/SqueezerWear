/*
 * Copyright (c) 2011 Kurt Aaholst <kaaholst@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.ngo.squeezer.framework;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.widget.Toast;

import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.menu.BaseMenuFragment;
import uk.org.ngo.squeezer.menu.MenuFragment;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.ServerString;
import uk.org.ngo.squeezer.service.SqueezeService;
import uk.org.ngo.squeezer.util.SqueezePlayer;

/**
 * Common base class for all activities in the squeezer
 *
 * @author Kurt Aaholst
 */
public abstract class BaseActivity extends ActionBarActivity implements HasUiThread {

    @Nullable private ISqueezeService mService = null;

    /**
     * Keep track of whether callbacks have been registered
     */
    private boolean mRegisteredCallbacks;

    private final Handler uiThreadHandler = new Handler() {
    };

    private SqueezePlayer squeezePlayer;

    protected String getTag() {
        return getClass().getSimpleName();
    }

    /**
     * @return The squeezeservice, or null if not bound
     */
    @Nullable
    public ISqueezeService getService() {
        return mService;
    }

    /**
     * Use this to post Runnables to work off thread
     */
    @Override
    public Handler getUIThreadHandler() {
        return uiThreadHandler;
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mService = (ISqueezeService) binder;
            BaseActivity.this.onServiceConnected(mService);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();

        actionBar.setIcon(R.drawable.ic_launcher);
        actionBar.setHomeButtonEnabled(true);
        bindService(new Intent(this, SqueezeService.class), serviceConnection,
                Context.BIND_AUTO_CREATE);
        Log.d(getTag(), "did bindService; serviceStub = " + getService());

        BaseMenuFragment.add(this, MenuFragment.class);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mService != null) {
            maybeRegisterCallbacks(mService);
        }

        // If SqueezePlayer is installed, start it
        if (SqueezePlayer.hasSqueezePlayer(this) && new Preferences(this).controlSqueezePlayer()) {
            squeezePlayer = new SqueezePlayer(this);
        }
    }

    @Override
    public void onPause() {
        if (squeezePlayer != null) {
            squeezePlayer.stopControllingSqueezePlayer();
            squeezePlayer = null;
        }
        if (mRegisteredCallbacks) {
            // If we are not bound to the service, it's process is no longer
            // running, so the callbacks are already cleaned up.
            if (getService() != null) {
                unregisterCallback();
            }
            mRegisteredCallbacks = false;
        }

        super.onPause();
    }

    /** Fix for https://code.google.com/p/android/issues/detail?id=63570. */
    private boolean mIsRestoredToTop;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if ((intent.getFlags() | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT) > 0) {
            mIsRestoredToTop = true;
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void finish() {
        super.finish();
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT && !isTaskRoot() && mIsRestoredToTop) {
            // 4.4.2 platform issues for FLAG_ACTIVITY_REORDER_TO_FRONT,
            // reordered activity back press will go to home unexpectedly,
            // Workaround: move reordered activity current task to front when it's finished.
            ActivityManager tasksManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            tasksManager.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_NO_USER_ACTION);
        }
    }

    /**
     * Performs any actions necessary after the service has been connected. Sub-classes must
     * call through to this implementation.
     *
     * <ul>
     *     <li>Invalidates the options menu so that menu items can be adjusted based on
     *     the state of the service connection.</li>
     *     <li>Ensures that callbacks are registered.</li>
     * </ul>
     *
     * @param service The connection to the bound service.
     */
    protected void onServiceConnected(@NonNull ISqueezeService service) {
        supportInvalidateOptionsMenu();
        maybeRegisterCallbacks(service);
    }

    /**
     * Registers any callbacks with the bound service. The default implementation does nothing,
     * sub-classes should override this as appropriate.
     *
     * @param service The connection to the bound service.
     */
    protected void registerCallback(@NonNull ISqueezeService service) {
    }

    /**
     * This is called when the service is disconnected.
     * <p/>
     * Normally you do not need to override this.
     */
    protected void unregisterCallback() {
        if (mService == null) return;

        mService.cancelItemListRequests(this);
        mService.cancelSubscriptions(this);
    }

    /**
     * Conditionally registers callbacks.
     * <p/>
     * Callback registration can happen in {@link #onResume()} and
     * {@link #onServiceConnected(uk.org.ngo.squeezer.service.ISqueezeService)}, this ensures
     * that it only happens once.
     *
     * @param service The connection to the bound service.
     */
    private void maybeRegisterCallbacks(@NonNull ISqueezeService service) {
        if (!mRegisteredCallbacks) {
            registerCallback(service);
            mRegisteredCallbacks = true;
        }
    }

    /**
     * Block searches, when we are not connected.
     */
    @Override
    public boolean onSearchRequested() {
        if (!isConnected()) {
            return false;
        }
        return super.onSearchRequested();
    }

    /*
     * Intercept hardware volume control keys to control Squeezeserver
     * volume.
     *
     * Change the volume when the key is depressed.  Suppress the keyUp
     * event, otherwise you get a notification beep as well as the volume
     * changing.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                return changeVolumeBy(+5);
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                return changeVolumeBy(-5);
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    private boolean changeVolumeBy(int delta) {
        ISqueezeService service = getService();
        if (service == null) {
            return false;
        }
        Log.v(getTag(), "Adjust volume by: " + delta);
        service.adjustVolumeBy(delta);
        return true;
    }

    // Safe accessors

    public boolean canDownload() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD);
    }

    public boolean isConnected() {
        if (mService == null) {
            return false;
        }
        return mService.isConnected();
    }

    public String getIconUrl(String icon) {
        if (mService == null || icon == null) {
            return null;
        }
        return mService.getIconUrl(icon);
    }

    public String getServerString(ServerString stringToken) {
        return ServerString.values()[stringToken.ordinal()].getLocalizedString();
    }

    // This section is just an easier way to call squeeze service

    public void play(PlaylistItem item) {
        playlistControl(PlaylistControlCmd.load, item, R.string.ITEM_PLAYING);
    }

    public void add(PlaylistItem item) {
        playlistControl(PlaylistControlCmd.add, item, R.string.ITEM_ADDED);
    }

    public void insert(PlaylistItem item) {
        playlistControl(PlaylistControlCmd.insert, item, R.string.ITEM_INSERTED);
    }

    private void playlistControl(PlaylistControlCmd cmd, PlaylistItem item, int resId)
            {
        if (mService == null) {
            return;
        }

        mService.playlistControl(cmd.name(), item);
        Toast.makeText(this, getString(resId, item.getName()), Toast.LENGTH_SHORT).show();
    }

    /**
     * Initiate download of songs for the supplied item.
     *
     * @param item Song or item with songs to download
     * @see ISqueezeService#downloadItem(uk.org.ngo.squeezer.framework.FilterItem)
     */
    public void downloadItem(FilterItem item) {
        if (canDownload())
            mService.downloadItem(item);
        else
            Toast.makeText(this, R.string.DOWNLOAD_MANAGER_NEEDED, Toast.LENGTH_LONG).show();
    }

    private enum PlaylistControlCmd {
        load,
        add,
        insert
    }

}
