/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.nearby.messages.samples.nearbybackgroundbeacons;

import android.Manifest;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.Messages;
import com.google.android.gms.nearby.messages.MessagesOptions;
import com.google.android.gms.nearby.messages.NearbyMessagesStatusCodes;
import com.google.android.gms.nearby.messages.NearbyPermissions;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeOptions;
import com.google.android.gms.nearby.messages.samples.nearbybackgroundbeacons.dialog.Dialog;
import com.google.android.gms.nearby.messages.samples.nearbybackgroundbeacons.geoloc.GeofenceErrorMessages;
import com.google.android.gms.nearby.messages.samples.nearbybackgroundbeacons.geoloc.GeofenceTransitionsIntentService;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static android.R.attr.data;


public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, SharedPreferences.OnSharedPreferenceChangeListener, ResultCallback<Status> {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        execute();

        if (savedInstanceState != null) {
            mSubscribed = savedInstanceState.getBoolean(KEY_SUBSCRIBED, false);
        }

    }

    private void execute() {
        if (!locationServicesEnabled() && isFirstTime()) {
            showDialog();
            return;
        }

        continueOnCreate();
    }

    /**
     * Show entrance dialog.
     */
    private void showDialog() {
        new Dialog(MainActivity.this, new Dialog.OnLocationEnabled() {

            @Override
            public void onLocationEnabled() {
                execute();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        startService(new Intent(this, GeofenceTransitionsIntentService.class)); // add this line
    }

    /**
     * On create method.
     */
    private void continueOnCreate() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mContainer = (RelativeLayout) findViewById(R.id.main_activity_container);
        final List<String> cachedMessages = Utils.getCachedMessages(this);
        if (cachedMessages != null) {
            mNearbyMessagesList.addAll(cachedMessages);
        }

//		nearbyMessagesListView = (ListView) findViewById(R.id.nearby_messages_list_view);
//		mNearbyMessagesArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mNearbyMessagesList);
//		if (nearbyMessagesListView != null) {
//			nearbyMessagesListView.setAdapter(mNearbyMessagesArrayAdapter);
//		}
//
//		if (!havePermissions()) {
//			Log.i(TAG, "Requesting permissions needed for this app.");
//			requestPermissions();
//		}
//
//        // nearbyMessagesListView Item Click Listener
//        nearbyMessagesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view,
//                                    int position, long id) {
//
//                // ListView Clicked item index
//                int itemPosition     = position;
//
//                // ListView Clicked item value
//                String  itemValue    = (String) nearbyMessagesListView.getItemAtPosition(position);
//
//                // Show Alert
//                Toast.makeText(getApplicationContext(),
//                        "Position :"+itemPosition+"  ListItem : " +itemValue , Toast.LENGTH_LONG)
//                        .show();
//                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
//                startActivity(intent);
//
//            }
//
//        });

        // Empty list for storing geofences.
        mGeofenceList = new ArrayList<Geofence>();

        // Initially set the PendingIntent used in addGeofences() and removeGeofences() to null.
        mGeofencePendingIntent = null;

        // Retrieve an instance of the SharedPreferences object.
        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME,
                MODE_PRIVATE);

        // Get the value of mGeofencesAdded from SharedPreferences. Set to false as a default.
        mGeofencesAdded = mSharedPreferences.getBoolean(Constants.GEOFENCES_ADDED_KEY, false);
//        setButtonsEnabledState();

        // Get the geofences used. Geofence data is hard coded in this sample.
        populateGeofenceList();

        // Kick off the request to build GoogleApiClient.
        buildGoogleApiClient();

    }

    /**
     * Checks if location services are enabled.
     *
     * @return True if location services are enabled, false otherwise.
     */
    private boolean locationServicesEnabled() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = false;
        boolean networkEnabled = false;

        try {
            gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }
        try {
            networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
        }

        return gpsEnabled && networkEnabled;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (BackgroundSubscribeIntentService.IsNotificationOn) {
            Intent intent = new Intent(MainActivity.this, RouteMapActivity.class);
            startActivity(intent);
        }

        getSharedPreferences(getApplicationContext().getPackageName(), Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(this);

        // As part of the permissions workflow, check permissions in case the user has gone to
        // Settings and enabled location there. If permissions are adequate, kick off a subscription
        // process by building GoogleApiClient.
        if (havePermissions()) {
            buildGoogleApiClient();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != PERMISSIONS_REQUEST_CODE) {
            return;
        }
        for (int i = 0; i < permissions.length; i++) {
            String permission = permissions[i];
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                // There are states to watch when a user denies permission when presented with
                // the Nearby permission dialog: 1) When the user pressed "Deny", but does not
                // check the "Never ask again" option. In this case, we display a Snackbar which
                // lets the user kick off the permissions flow again. 2) When the user pressed
                // "Deny" and also checked the "Never ask again" option. In this case, the
                // permission dialog will no longer be presented to the user. The user may still
                // want to authorize location and use the app, and we present a Snackbar that
                // directs them to go to Settings where they can grant the location permission.
                if (shouldShowRequestPermissionRationale(permission)) {
                    Log.i(TAG, "Permission denied without 'NEVER ASK AGAIN': " + permission);
                    showRequestPermissionsSnackbar();
                } else {
                    Log.i(TAG, "Permission denied with 'NEVER ASK AGAIN': " + permission);
                    showLinkToSettingsSnackbar();
                }
            } else {
                Log.i(TAG, "Permission granted, building GoogleApiClient");
                buildGoogleApiClient();
            }
        }
    }

    /**
     * Builds {@link GoogleApiClient}, enabling automatic lifecycle management using
     * {@link GoogleApiClient.Builder#enableAutoManage(FragmentActivity,
     * int, GoogleApiClient.OnConnectionFailedListener)}. I.e., GoogleApiClient connects in
     * {@link AppCompatActivity#onStart} -- or if onStart() has already happened -- it connects
     * immediately, and disconnects automatically in {@link AppCompatActivity#onStop}.
     */
    private synchronized void buildGoogleApiClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(Nearby.MESSAGES_API, new MessagesOptions.Builder().setPermissions(NearbyPermissions.BLE).build()).addConnectionCallbacks(this).enableAutoManage(this, this).build();
        }
        if (geoGoogleApiClient == null) {
            geoGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    @Override
    protected void onPause() {
        getSharedPreferences(getApplicationContext().getPackageName(), Context.MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (mContainer != null) {
            Snackbar.make(mContainer, "Exception while connecting to Google Play services: " + connectionResult.getErrorMessage(), Snackbar.LENGTH_INDEFINITE).show();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.w(TAG, "Connection suspended. Error code: " + i);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "GoogleApiClient connected");
        // Nearby.Messages.subscribe(...) requires a connected GoogleApiClient. For that reason,
        // we subscribe only once we have confirmation that GoogleApiClient is connected.
        if (mGoogleApiClient.isConnected()) {
            subscribe();
        }
        if (mGoogleApiClient.isConnected()) {
            addGeofences();
        }

        if (BackgroundSubscribeIntentService.IsNotificationOn || false) {
            Intent intent = new Intent(MainActivity.this, RouteMapActivity.class);
            startActivity(intent);
        }

        Log.i(TAG, "Connected to GoogleApiClient");
        SharedPreferences sharedPrefs = MainActivity.this.getSharedPreferences("GEO_PREFS", Context.MODE_PRIVATE);
        String geofencesExist = sharedPrefs.getString("Geofences added", null);

        if (geofencesExist == null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            LocationServices.GeofencingApi.addGeofences(
                    geoGoogleApiClient,
                    // The GeofenceRequest object.
                    getGeofencingRequest(),
                    // A pending intent that that is reused when calling removeGeofences(). This
                    // pending intent is used to generate an intent when a matched geofence
                    // transition is observed.
                    getGeofencePendingIntent()
            ).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    if (status.isSuccess()) {
                        SharedPreferences sharedPrefs = MainActivity.this.getSharedPreferences("GEO_PREFS", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPrefs.edit();
                        editor.putString("Geofences added", "1");
                        editor.commit();
                    }
                }
            });
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (TextUtils.equals(key, Utils.KEY_CACHED_MESSAGES)) {
            mNearbyMessagesList.clear();
            mNearbyMessagesList.addAll(Utils.getCachedMessages(this));
            mNearbyMessagesArrayAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_SUBSCRIBED, mSubscribed);
    }

    private boolean havePermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, PERMISSIONS_REQUEST_CODE);
    }

    /**
     * Calls {@link Messages#subscribe(GoogleApiClient, MessageListener, SubscribeOptions)},
     * using a {@link Strategy} for BLE scanning. Attaches a {@link ResultCallback} to monitor
     * whether the call to {@code subscribe()} succeeded or failed.
     */
    private void subscribe() {
        // In this sample, we subscribe when the activity is launched, but not on device orientation
        // change.
        if (mSubscribed) {
            Log.i(TAG, "Already subscribed.");
            return;
        }

        SubscribeOptions options = new SubscribeOptions.Builder().setStrategy(Strategy.BLE_ONLY).build();

        Nearby.Messages.subscribe(mGoogleApiClient, getPendingIntent(), options).setResultCallback(new ResultCallback<Status>() {

            @Override
            public void onResult(@NonNull Status status) {
                if (status.isSuccess()) {
                    Log.i(TAG, "Subscribed successfully.");
                    startService(getBackgroundSubscribeServiceIntent());

                } else {
                    Log.e(TAG, "Operation failed. Error: " + NearbyMessagesStatusCodes.getStatusCodeString(status.getStatusCode()));
                }
            }
        });
    }

    private PendingIntent getPendingIntent() {
        return PendingIntent.getService(this, 0, getBackgroundSubscribeServiceIntent(), PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private Intent getBackgroundSubscribeServiceIntent() {
        return new Intent(this, BackgroundSubscribeIntentService.class);
    }

    /**
     * Displays {@link Snackbar} instructing user to visit Settings to grant permissions required
     * by
     * this application.
     */
    private void showLinkToSettingsSnackbar() {
        if (mContainer == null) {
            return;
        }
        Snackbar.make(mContainer, R.string.permission_denied_explanation, Snackbar.LENGTH_INDEFINITE).setAction(R.string.settings, new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                // Build intent that displays the App settings screen.
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
                intent.setData(uri);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        }).show();
    }

    /**
     * Displays {@link Snackbar} with button for the user to re-initiate the permission workflow.
     */
    private void showRequestPermissionsSnackbar() {
        if (mContainer == null) {
            return;
        }

        Snackbar.make(mContainer, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok, new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                // Request permission.
                ActivityCompat.requestPermissions(MainActivity.this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, PERMISSIONS_REQUEST_CODE);
            }
        }).show();
    }

    /**
     * Checks if the user has entered in the app for the first time and enabled the location
     * services.
     *
     * @return True if user has enabled and also for the first time in the app, false otherwise.
     */
    protected boolean isFirstTime() {

        final String FIRST_TIME = "first_time";

        if (firstTime == null) {
            final SharedPreferences mPreferences = this.getSharedPreferences(FIRST_TIME, Context.MODE_PRIVATE);
            firstTime = mPreferences.getBoolean("firstTime", true);
            if (firstTime) {
                final SharedPreferences.Editor editor = mPreferences.edit();

                if (locationServicesEnabled()) {
                    editor.putBoolean("firstTime", false);
                    editor.commit();
                }
            }
        }

        return firstTime;
    }

    // GEOFENCE PART
    /**
     * This sample hard codes geofence data. A real app might dynamically create geofences based on
     * the user's location.
     */
    public void populateGeofenceList() {
        for (Map.Entry<String, LatLng> entry : Constants.BAY_AREA_LANDMARKS.entrySet()) {

            mGeofenceList.add(new Geofence.Builder()
                    // Set the request ID of the geofence. This is a string to identify this
                    // geofence.
                    .setRequestId(entry.getKey())

                    // Set the circular region of this geofence.
                    .setCircularRegion(
                            entry.getValue().latitude,
                            entry.getValue().longitude,
                            Constants.GEOFENCE_RADIUS_IN_METERS
                    )

                    // Set the expiration duration of the geofence. This geofence gets automatically
                    // removed after this period of time.
                    .setExpirationDuration(Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)

                    // Set the transition types of interest. Alerts are only generated for these
                    // transition. We track entry and exit transitions in this sample.
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                            Geofence.GEOFENCE_TRANSITION_EXIT)

                    // Create the geofence.
                    .build());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        geoGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    /**
     * Runs when the result of calling addGeofences() and removeGeofences() becomes available.
     * Either method can complete successfully or with an error.
     *
     * Since this activity implements the {@link ResultCallback} interface, we are required to
     * define this method.
     *
     * @param status The Status returned through a PendingIntent when addGeofences() or
     *               removeGeofences() get called.
     */
    public void onResult(Status status) {
        if (status.isSuccess() && isFirstTime()) {
            // Update state and save in shared preferences.
            mGeofencesAdded = !mGeofencesAdded;
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putBoolean(Constants.GEOFENCES_ADDED_KEY, mGeofencesAdded);
            editor.apply();

            Toast.makeText(
                    this,
                    getString(mGeofencesAdded ? R.string.geofences_added :
                            R.string.geofences_removed),
                    Toast.LENGTH_SHORT
            ).show();

        } else {
            // Get the status code for the error and log it using a user-friendly message.
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    status.getStatusCode());
            Log.e(TAG, errorMessage);
        }
    }

    /**
     * Adds geofences, which sets alerts to be notified when the device enters or exits one of the
     * specified geofences. Handles the success or failure results returned by addGeofences().
     */
    public void addGeofences() {
        if (!geoGoogleApiClient.isConnected()) {
            Toast.makeText(this, getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            LocationServices.GeofencingApi.addGeofences(
                    geoGoogleApiClient,
                    // The GeofenceRequest object.
                    getGeofencingRequest(),
                    // A pending intent that that is reused when calling removeGeofences(). This
                    // pending intent is used to generate an intent when a matched geofence
                    // transition is observed.
                    getGeofencePendingIntent()
            ).setResultCallback(this); // Result processed in onResult().
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            logSecurityException(securityException);
        }
    }


    /**
     * Builds and returns a GeofencingRequest. Specifies the list of geofences to be monitored.
     * Also specifies how the geofence notifications are initially triggered.
     */
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);

        // Add the geofences to be monitored by geofencing service.
        builder.addGeofences(mGeofenceList);

        // Return a GeofencingRequest.
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void logSecurityException(SecurityException securityException) {
        Log.e(TAG, "Invalid location permission. " +
                "You need to use ACCESS_FINE_LOCATION with geofences", securityException);
    }

    protected Boolean firstTime = null;

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int PERMISSIONS_REQUEST_CODE = 1111;

    private static final String KEY_SUBSCRIBED = "subscribed";

    /**
     * The entry point to Google Play Services.
     */
    private GoogleApiClient geoGoogleApiClient;

    /**
     * The container {@link android.view.ViewGroup} for the minimal UI associated with this sample.
     */
    private RelativeLayout mContainer;

    /**
     * Tracks subscription state. Set to true when a call to
     * {@link Messages#subscribe(GoogleApiClient, MessageListener)} succeeds.
     */
    private boolean mSubscribed = false;

    /**
     * Adapter for working with messages from nearby beacons.
     */
    private ArrayAdapter<String> mNearbyMessagesArrayAdapter;

    /**
     * Backing data structure for {@code mNearbyMessagesArrayAdapter}.
     */
    private List<String> mNearbyMessagesList = new ArrayList<>();

    private ListView nearbyMessagesListView = null;

    // GEFENCE
    protected GoogleApiClient mGoogleApiClient;

    /**
     * The list of geofences used in this sample.
     */
    protected ArrayList<Geofence> mGeofenceList;

    /**
     * Used to keep track of whether geofences were added.
     */
    private boolean mGeofencesAdded;

    /**
     * Used when requesting to add or remove geofences.
     */
    private PendingIntent mGeofencePendingIntent;

    /**
     * Used to persist application state about whether geofences were added.
     */
    private SharedPreferences mSharedPreferences;

}