package uk.org.ngo.squeezer.service;

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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.TimeUnit;

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

//    @Nullable private ISqueezeService mService = null;


    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();


        Log.d("mobile:squeezer", "ListenerService:onCreate");
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
            Log.d("mobile:squeezer", "Message path received on mobile is: " + messageEvent.getPath());
            Log.d("mobile:squeezer", "Message received on mobile is: " + message.toString());
            //SEND CURRENT SONG BACK

            JSONObject numberinfo = new JSONObject();
            try {
                numberinfo.put("title", "Take me to church");
                numberinfo.put("artist", "Hozier");
                numberinfo.put("album", "Hozier");
                numberinfo.put("status","play");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Log.d("mobile:squeezer", numberinfo.toString());
            //Requires a new thread to avoid blocking the UI
            new SendToDataLayerThread(DATA_CURRENT_SONG, numberinfo.toString()).start();

        }else if (messageEvent.getPath().equals("/squeezer_action__")) {
        } else if (messageEvent.getPath().equals("/squeezer_action/next")) {
            JSONObject numberinfo = new JSONObject();
            try {
                numberinfo.put("title", "VOLGENDE NUMMER");
                numberinfo.put("artist", "Dotan");
                numberinfo.put("album", "Hozier");
                numberinfo.put("status","play");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Log.d("mobile:squeezer", numberinfo.toString());
            //Requires a new thread to avoid blocking the UI
            new SendToDataLayerThread(DATA_CURRENT_SONG, numberinfo.toString()).start();
        }else if (messageEvent.getPath().equals("/squeezer_action/previous")) {
            JSONObject numberinfo = new JSONObject();
            try {
                numberinfo.put("title", "VORIGE NUMMER");
                numberinfo.put("artist", "Dotan");
                numberinfo.put("album", "Hozier");
                numberinfo.put("status","play");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Log.d("mobile:squeezer", numberinfo.toString());
            //Requires a new thread to avoid blocking the UI
            new SendToDataLayerThread(DATA_CURRENT_SONG, numberinfo.toString()).start();
        }else if (messageEvent.getPath().equals("/squeezer_action/current")) {
            JSONObject numberinfo = new JSONObject();
            try {
                numberinfo.put("title", "GET CURRENT");
                numberinfo.put("artist", "Hozier");
                numberinfo.put("album", "Hozier");
                numberinfo.put("status","play");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Log.d("mobile:squeezer", numberinfo.toString());
            //Requires a new thread to avoid blocking the UI
            new SendToDataLayerThread(DATA_CURRENT_SONG, numberinfo.toString()).start();
        }else if (messageEvent.getPath().equals("/squeezer_action/play")) {

            JSONObject numberinfo = new JSONObject();
            try {
                numberinfo.put("title", "GET CURRENT");
                numberinfo.put("artist", "Hozier");
                numberinfo.put("album", "Hozier");
                if(message == "play"){
                    numberinfo.put("status","stop");
                }else{
                    numberinfo.put("status","play");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Log.d("mobile:squeezer", numberinfo.toString());
            //Requires a new thread to avoid blocking the UI
            new SendToDataLayerThread(DATA_CURRENT_SONG, numberinfo.toString()).start();
        }

        Log.d("path", messageEvent.getPath());
        Log.d("mobile:squeezer-data", String.valueOf(messageEvent.getPath()));
        super.onMessageReceived(messageEvent);
    }

    @Override
    public void onPeerConnected(Node peer) {
        LOGD("mobile:squeezer", "onPeerConnected: " + peer);
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        LOGD("mobile:squeezer", "onPeerDisconnected: " + peer);
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
}
