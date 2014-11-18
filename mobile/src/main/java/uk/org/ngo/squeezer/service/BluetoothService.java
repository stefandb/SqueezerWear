package uk.org.ngo.squeezer.service;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.WearableListenerService;

import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import uk.org.ngo.squeezer.DisconnectedActivity;
import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.SettingsActivity;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.dialog.AuthenticationDialog;
import uk.org.ngo.squeezer.framework.BaseActivity;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.PlayerState;
import uk.org.ngo.squeezer.model.Song;

/**
 * Created by Stefan on 16-11-2014.
 */
public class BluetoothService extends WearableListenerService {

    private static final String TAG = "DataLayerListenerServic";
    private static final String START_ACTIVITY_PATH = "/squeezer_current";
    private static final String DATA_ITEM_RECEIVED_PATH = "/data-item-received";
    public static final String DATA_CURRENT_SONG = "/squeezer_current";
    public static final String DATA_ACTION = "/squeezer_action";
    public static final String COUNT_PATH = "/count";
    public static final String IMAGE_PATH = "/image";
    public static final String IMAGE_KEY = "photo";
    private static final String COUNT_KEY = "count";
    private static final int MAX_LOG_TAG_LENGTH = 23;

    GoogleApiClient mGoogleApiClient;

    @Nullable private ISqueezeService mService = null;
    private BluetoothService mActivity;
    private final static int UPDATE_TIME = 1;
    private int secondsTotal;
    private int secondsIn;
    private boolean mRegisteredCallbacks;

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();


        //service voor squeezer
        mActivity = this;
        // Set up a server connection, if it is not present

        mActivity.bindService(new Intent(mActivity, SqueezeService.class), serviceConnection,
                Context.BIND_AUTO_CREATE);
        Log.d(TAG, "did bindService; serviceStub = " + mService);

        Log.d("mobile:squeezer", "ListenerService:onCreate");
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mService != null) {
            if (serviceConnection != null) {
                mActivity.unbindService(serviceConnection);
            }
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d("mobile:squeezer", "ListenerService:onDataChanged");


        LOGD(TAG, "onDataChanged: " + dataEvents);
        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        dataEvents.close();
        if(!mGoogleApiClient.isConnected()) {
            ConnectionResult connectionResult = mGoogleApiClient
                    .blockingConnect(30, TimeUnit.SECONDS);
            if (!connectionResult.isSuccess()) {
                Log.e("mobile:squeezer", "DataLayerListenerService failed to connect to GoogleApiClient.");
                return;
            }
        }

        // Loop through the events and send a message back to the node that created the data item.
        for (DataEvent event : events) {
            Uri uri = event.getDataItem().getUri();
            String path = uri.getPath();
            if (COUNT_PATH.equals(path)) {
                // Get the node id of the node that created the data item from the host portion of
                // the uri.
                String nodeId = uri.getHost();
                // Set the data of the message to be the bytes of the Uri.
                byte[] payload = uri.toString().getBytes();

                // Send the rpc
                Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, DATA_ITEM_RECEIVED_PATH,
                        payload);
            }else{
                Log.d("mobile:squeezer-data", String.valueOf(event.getType()));
            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        LOGD("mobile:squeezer", "onMessageReceived: " + messageEvent);
        Log.d("squeezer message", messageEvent.getData().toString());
        final String message = new String(messageEvent.getData());
        Log.d("mobile:squeezer-message-service", "Message path received on watch is: " + messageEvent.getPath());
        Log.d("mobile:squeezer-message-service", "Message received on watch is: " + message);

        // Check to see if the message is to start an activity

//        if (messageEvent.getPath().equals(START_ACTIVITY_PATH)) {
//            Intent messageIntent = new Intent();
//            messageIntent.setAction(Intent.ACTION_SEND);
//            messageIntent.putExtra("message", message);
//            LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
//        }

        if (messageEvent.getPath().equals("/squeezer_current")) {
//            Log.d("mobile:squeezer", "Message path received on mobile is: " + messageEvent.getPath());
//            Log.d("mobile:squeezer", "Message received on mobile is: " + message.toString());
//            //SEND CURRENT SONG BACK
//
//            JSONObject numberinfo = new JSONObject();
//            try {
//                numberinfo.put("title", "Take me to church");
//                numberinfo.put("artist", "Hozier");
//                numberinfo.put("album", "Hozier");
//                numberinfo.put("status","play");
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//
//            Log.d("mobile:squeezer", numberinfo.toString());
//            //Requires a new thread to avoid blocking the UI
//            new SendToDataLayerThread(DATA_CURRENT_SONG, numberinfo.toString()).start();

            // TODO call update song

        } else if (messageEvent.getPath().equals("/squeezer_action/next")) {
            if (mService == null) {
                return;
            }
            mService.nextTrack();
//            JSONObject numberinfo = new JSONObject();
//            try {
//                numberinfo.put("title", "VOLGENDE NUMMER");
//                numberinfo.put("artist", "Dotan");
//                numberinfo.put("album", "Hozier");
//                numberinfo.put("status","play");
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//
//            Log.d("mobile:squeezer", numberinfo.toString());
//            //Requires a new thread to avoid blocking the UI
//            new SendToDataLayerThread(DATA_CURRENT_SONG, numberinfo.toString()).start();
        } else if (messageEvent.getPath().equals("/squeezer_action/previous")) {
            if (mService == null) {
                return;
            }
            mService.previousTrack();
//            JSONObject numberinfo = new JSONObject();
//            try {
//                numberinfo.put("title", "VORIGE NUMMER");
//                numberinfo.put("artist", "Dotan");
//                numberinfo.put("album", "Hozier");
//                numberinfo.put("status","play");
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//
//            Log.d("mobile:squeezer", numberinfo.toString());
//            //Requires a new thread to avoid blocking the UI
//            new SendToDataLayerThread(DATA_CURRENT_SONG, numberinfo.toString()).start();
        } else if (messageEvent.getPath().equals("/squeezer_action/current")) {
            // TODO call update song

//            JSONObject numberinfo = new JSONObject();
//            try {
//                numberinfo.put("title", "GET CURRENT");
//                numberinfo.put("artist", "Hozier");
//                numberinfo.put("album", "Hozier");
//                numberinfo.put("status","play");
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//
//            Log.d("mobile:squeezer", numberinfo.toString());
//            //Requires a new thread to avoid blocking the UI
//            new SendToDataLayerThread(DATA_CURRENT_SONG, numberinfo.toString()).start();
        } else if (messageEvent.getPath().equals("/squeezer_action/play")) {
            if (mService == null) {
                return;
            }
            if (isConnected()) {
                Log.v(TAG, "Pause...");
                mService.togglePausePlay();
            } else {
                // When we're not connected, the play/pause
                // button turns into a green connect button.
//                onUserInitiatesConnect();
            }
//            JSONObject numberinfo = new JSONObject();
//            try {
//                numberinfo.put("title", "GET CURRENT");
//                numberinfo.put("artist", "Hozier");
//                numberinfo.put("album", "Hozier");
//                if(message == "play"){
//                    numberinfo.put("status","stop");
//                }else{
//                    numberinfo.put("status","play");
//                }
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//            Log.d("mobile:squeezer", numberinfo.toString());
//            //Requires a new thread to avoid blocking the UI
//            new SendToDataLayerThread(DATA_CURRENT_SONG, numberinfo.toString()).start();
//        }
//
//        Log.d("path", messageEvent.getPath());
//        Log.d("mobile:squeezer-data", String.valueOf(messageEvent.getPath()));
//        super.onMessageReceived(messageEvent);
        }
    }

    public static void LOGD(final String tag, String message) {
//        if (Log.isLoggable(tag, Log.DEBUG)) {
        Log.d(tag, message);
//        }
    }

    class SendToDataLayerThread extends Thread {
        String path;
        String message;

        // Constructor to send a message to the data layer
        SendToDataLayerThread(String p, String msg) {
            path = p;
            message = msg;
        }

        public void run() {
            NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
            Log.d("mobile:squeezer", nodes.getNodes().toString());
            for (Node node : nodes.getNodes()) {
                Log.d("mobile:squeezer-node", node.toString());
                MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), path, message.getBytes()).await();

                if (result.getStatus().isSuccess()) {
                    Log.d("mobile:squeezer-run", "Message: {" + message + "} sent to: " + node.getDisplayName());
                }
                else {
                    // Log an error
                    Log.d("mobile:squeezer-run", "ERROR: failed to send Message");
                }
            }
        }
    }



    /* alles voor de service coonectie naar squeezer */

    // TODO GEBLEVEN
    public void startVisibleConnection() {
        Log.v(TAG, "startVisibleConnection");
        if (mService == null) {
            return;
        }

        uiThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                Preferences preferences = new Preferences(mActivity);
                String ipPort = preferences.getServerAddress();
                if (ipPort == null) {
                    return;
                }

                if (isConnectInProgress()) {
                    Log.v(TAG, "Connection is already in progress, connecting aborted");
                    return;
                }
                try {
                    mService.startConnect(ipPort, preferences.getUserName("test"),
                            preferences.getPassword("test1"));
                } catch (IllegalStateException e) {
                    Log.i(TAG, "ProgressDialog.show() was not allowed, connecting aborted: " + e);
                }
            }
        });
    }



    // TODO aanroepen START
    // TODO GEBLEVEN
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.v(TAG, "ServiceConnection.onServiceConnected()");
            BluetoothService.this.onServiceConnected((ISqueezeService) binder);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    protected void onServiceConnected(@NonNull ISqueezeService service) {
        Log.v(TAG, "Service bound");
        mService = service;

        maybeRegisterCallbacks(mService);
        uiThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                updateUIFromServiceState();
            }
        });

        // Assume they want to connect (unless manually disconnected).

        // && !isManualDisconnect()
        if (!isConnected()) {
            startVisibleConnection();
        }
    }

    private boolean isConnected() {
        if (mService == null) {
            return false;
        }
        return mService.isConnected();
    }

    private void updateUIFromServiceState() {
        // Update the UI to reflect connection state. Basically just for
        // the initial display, as changing the prev/next buttons to empty
        // doesn't seem to work in onCreate. (LayoutInflator still running?)
        Log.d(TAG, "updateUIFromServiceState");
        boolean connected = isConnected();
        setConnected(connected, false, false);
        if (connected) {
            PlayerState playerState = getPlayerState();
            updateSongInfo(playerState.getCurrentSong(), playerState.getPlayStatus(), playerState.getShuffleStatus(), playerState.getRepeatStatus());
            updateTimeDisplayTo(playerState.getCurrentTimeSecond(),
                    playerState.getCurrentSongDuration());

            // TODO ONDERSTAANDE functies samencoegen met updatesonginfo
            updateShuffleStatus(playerState.getShuffleStatus());
            updateRepeatStatus(playerState.getRepeatStatus());
        }
    }

    private PlayerState getPlayerState() {
        if (mService == null) {
            return null;
        }
        return mService.getPlayerState();
    }

    private void setConnected(boolean connected, boolean postConnect, boolean loginFailure) {
        Log.v(TAG, "setConnected(" + connected + ", " + postConnect + ", " + loginFailure + ")");

        if (postConnect) {
            if (!connected) {
                // TODO: Make this a dialog? Allow the user to correct the
                // server settings here?
            }
        }
//        if (loginFailure) {
//            new AuthenticationDialog().show(mActivity.getSupportFragmentManager(), "AuthenticationDialog");
//        }

        if (!connected) {
            updateSongInfo(null, null, null, null);
        }
    }

    private void updateSongInfo(Song song, PlayerState.PlayStatus playStatus, PlayerState.ShuffleStatus shuffleStatus, PlayerState.RepeatStatus repeatStatus) {
        Log.v(TAG, "updateSongInfo " + song);
        if (song != null) {
            JSONObject numberinfo = new JSONObject();
            try {
                numberinfo.put("title", song.getName());
                numberinfo.put("artist", song.getArtist());
                numberinfo.put("album", song.getAlbumName());
                if(playStatus == PlayerState.PlayStatus.play && playStatus != null){
                    //pauze
                    numberinfo.put("status","pause");
                }else if(playStatus != PlayerState.PlayStatus.play && playStatus != null){
                    //play
                    numberinfo.put("status","play");
                }else{
                    numberinfo.put("status","null");
                }

                numberinfo.put("repeat",repeatStatus.getId());
                numberinfo.put("shuffle", shuffleStatus.getId());

                numberinfo.put("currentTime","--:--");
                numberinfo.put("totalTime","--:--");

                numberinfo.put("btnnext", (song.isRemote()) ? false : true);
                numberinfo.put("btnprevious", (song.isRemote()) ? false : true);
            } catch (JSONException e) {
//                e.printStackTrace();
                Log.d("nowplaying:squeezer", e.toString());
                Log.d("nowplaying:squeezer", e.getMessage());
            }

//            shuffleButton.setEnabled(connected);
//            repeatButton.setEnabled(connected);
            Log.d("nowplaying:squeezer-json", numberinfo.toString());

            //Requires a new thread to avoid blocking the UI
            new SendToDataLayerThread(BluetoothService.DATA_CURRENT_SONG, numberinfo.toString()).start();
//            new SendMesage(BluetoothService.DATA_CURRENT_SONG, numberinfo.toString()).execute();
            Log.d("nowplaying:squeezer-", "na post");
        } else {
            JSONObject numberinfo = new JSONObject();
            try {
                numberinfo.put("title", getText(R.string.disconnected_text));
                numberinfo.put("artist", getText(R.string.disconnected_text));
                numberinfo.put("album", getText(R.string.disconnected_text));
                numberinfo.put("status","null");
                numberinfo.put("currentTime","--:--");
                numberinfo.put("totalTime","--:--");
                numberinfo.put("btnnext", false);
                numberinfo.put("btnprevious", false);
                numberinfo.put("repeat",null);
                numberinfo.put("shuffle", null);

            } catch (JSONException e) {
//                e.printStackTrace();
                Log.d("nowplaying:squeezer", e.toString());
                Log.d("nowplaying:squeezer", e.getMessage());
            }

            Log.d("nowplaying:squeezer-json", numberinfo.toString());
            //Requires a new thread to avoid blocking the UI
//            new SendMesage(BluetoothService.DATA_CURRENT_SONG, numberinfo.toString()).execute();
            new SendToDataLayerThread(BluetoothService.DATA_CURRENT_SONG, numberinfo.toString()).start();
            Log.d("nowplaying:squeezer-", "na post");
        }
//        updateAlbumArt(song);
    }

    private void maybeRegisterCallbacks(@NonNull ISqueezeService service) {
        if (!mRegisteredCallbacks) {
            service.registerCallback(serviceCallback);
            service.registerConnectionCallback(connectionCallback);
//            service.registerHandshakeCallback(handshakeCallback);
            service.registerMusicChangedCallback(musicChangedCallback);
//            service.registerPlayersCallback(playersCallback);
//            service.registerVolumeCallback(volumeCallback);
            mRegisteredCallbacks = true;
        }
    }

//    private final IServiceVolumeCallback volumeCallback = new IServiceVolumeCallback() {
//        @Override
//        public void onVolumeChanged(final int newVolume, final Player player) {
//            if (!ignoreVolumeChange) {
//                mVolumePanel.postVolumeChanged(newVolume, player == null ? "" : player.getName());
//            }
//        }
//
//        @Override
//        public Object getClient() {
//            return BluetoothService.this;
//        }
//
//        @Override
//        public boolean wantAllPlayers() {
//            return false;
//        }
//    };

//    private final IServicePlayersCallback playersCallback = new IServicePlayersCallback() {
//        @Override
//        public void onPlayersChanged(final List<Player> players, final Player activePlayer) {
//            uiThreadHandler.post(new Runnable() {
//                @Override
//                public void run() {
//                    updatePlayerDropDown(players, activePlayer);
//                }
//            });
//        }
//
//        @Override
//        public Object getClient() {
//            return BluetoothService.this;
//        }
//    };

    private final IServiceMusicChangedCallback musicChangedCallback
            = new IServiceMusicChangedCallback() {
        @Override
        public void onMusicChanged(final PlayerState playerState) {
            uiThreadHandler.post(new Runnable() {
                public void run() {
                    updateSongInfo(playerState.getCurrentSong(), playerState.getPlayStatus(), playerState.getShuffleStatus(), playerState.getRepeatStatus());
                }
            });
        }

        @Override
        public Object getClient() {
            return BluetoothService.this;
        }
    };

    private final IServiceConnectionCallback connectionCallback = new IServiceConnectionCallback() {
        @Override
        public void onConnectionChanged(final boolean isConnected,
                                        final boolean postConnect,
                                        final boolean loginFailed) {
            Log.v(TAG, "Connected == " + isConnected + " (postConnect==" + postConnect + ")");
            uiThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    setConnected(isConnected, postConnect, loginFailed);
                }
            });
        }

        @Override
        public Object getClient() {
            return BluetoothService.this;
        }
    };

    private final IServiceCallback serviceCallback = new IServiceCallback() {
        @Override
        public void onPlayStatusChanged(final String playStatusName) {
            uiThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    // TODO testen of call nodgi is
//                    updatePlayPauseIcon(PlayerState.PlayStatus.valueOf(playStatusName));
                }
            });
        }

        @Override
        public void onShuffleStatusChanged(final boolean initial, final int shuffleStatusId) {
            uiThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    PlayerState.ShuffleStatus shuffleStatus = PlayerState.ShuffleStatus.valueOf(shuffleStatusId);
                    updateShuffleStatus(shuffleStatus);
                }
            });
        }

        @Override
        public void onRepeatStatusChanged(final boolean initial, final int repeatStatusId) {
            uiThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    PlayerState.RepeatStatus repeatStatus = PlayerState.RepeatStatus.valueOf(repeatStatusId);
                    updateRepeatStatus(repeatStatus);
                }
            });
        }

        @Override
        public void onTimeInSongChange(final int secondsIn, final int secondsTotal) {
            BluetoothService.this.secondsIn = secondsIn;
            BluetoothService.this.secondsTotal = secondsTotal;
            uiThreadHandler.sendEmptyMessage(UPDATE_TIME);
        }

        @Override
        public void onPowerStatusChanged(final boolean canPowerOn, final boolean canPowerOff) {
            uiThreadHandler.post(new Runnable() {
                public void run() {
//                    updatePowerMenuItems(canPowerOn, canPowerOff);
                }
            });
        }

        @Override
        public Object getClient() {
            return BluetoothService.this;
        }
    };

    private void updateRepeatStatus(PlayerState.RepeatStatus repeatStatus) {
        if (repeatStatus != null) {

            // TODO json call met status en testen
            JSONObject numberinfo = new JSONObject();
            try {
                numberinfo.put("status", repeatStatus.getId());
            } catch (JSONException e) {
//                e.printStackTrace();
                Log.d("nowplaying:squeezer", e.toString());
                Log.d("nowplaying:squeezer", e.getMessage());
            }

            Log.d("nowplaying:squeezer-json", numberinfo.toString());
            //Requires a new thread to avoid blocking the UI
//            new SendMesage(BluetoothService.DATA_CURRENT_SONG, numberinfo.toString()).execute();
            new SendToDataLayerThread(BluetoothService.DATA_CURRENT_SONG + "/status/repeat", numberinfo.toString()).start();
            Log.d("nowplaying:squeezer-", "na post");
        }
    }

    private void updateShuffleStatus(PlayerState.ShuffleStatus shuffleStatus) {
        if (shuffleStatus != null) {

            // TODO json call met status en testen
            JSONObject numberinfo = new JSONObject();
            try {
                numberinfo.put("status", shuffleStatus.getId());
            } catch (JSONException e) {
//                e.printStackTrace();
                Log.d("nowplaying:squeezer", e.toString());
                Log.d("nowplaying:squeezer", e.getMessage());
            }

            Log.d("nowplaying:squeezer-json", numberinfo.toString());
            //Requires a new thread to avoid blocking the UI
//            new SendMesage(BluetoothService.DATA_CURRENT_SONG, numberinfo.toString()).execute();
            new SendToDataLayerThread(BluetoothService.DATA_CURRENT_SONG + "/status/shuffle", numberinfo.toString()).start();
            Log.d("nowplaying:squeezer-", "na post");
        }
    }

    private final Handler uiThreadHandler = new UiThreadHandler(this);

    private final static class UiThreadHandler extends Handler {

        final WeakReference<BluetoothService> mFragment;

        public UiThreadHandler(BluetoothService fragment) {
            mFragment = new WeakReference<BluetoothService>(fragment);
        }

        // Normally I'm lazy and just post Runnables to the uiThreadHandler
        // but time updating is special enough (it happens every second) to
        // take care not to allocate so much memory which forces Dalvik to GC
        // all the time.
        @Override
        public void handleMessage(Message message) {
            if (message.what == UPDATE_TIME) {
                mFragment.get().updateTimeDisplayTo(mFragment.get().secondsIn,
                        mFragment.get().secondsTotal);
            }
        }
    }

    private boolean isConnectInProgress() {
        if (mService == null) {
            return false;
        }
        return mService.isConnectInProgress();
    }

    private void updateTimeDisplayTo(int secondsIn, int secondsTotal) {
//        if (mFullHeightLayout) {
//            if (updateSeekBar) {
//                if (seekBar.getMax() != secondsTotal) {
//                    seekBar.setMax(secondsTotal);
//                    totalTime.setText(Util.formatElapsedTime(secondsTotal));
//                }
//                seekBar.setProgress(secondsIn);
//                currentTime.setText(Util.formatElapsedTime(secondsIn));
//            }
//        } else {
//            if (mProgressBar.getMax() != secondsTotal) {
//                mProgressBar.setMax(secondsTotal);
//            }
//            mProgressBar.setProgress(secondsIn);
//        }
    }
}
