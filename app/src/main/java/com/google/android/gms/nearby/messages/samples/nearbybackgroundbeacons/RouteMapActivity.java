package com.google.android.gms.nearby.messages.samples.nearbybackgroundbeacons;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.TransitMode;
import com.akexorcist.googledirection.constant.TransportMode;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Leg;
import com.akexorcist.googledirection.model.Route;
import com.akexorcist.googledirection.model.Step;
import com.akexorcist.googledirection.util.DirectionConverter;
import com.example.bmilos.library.SlidingUpPanelLayout;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.nearby.messages.samples.nearbybackgroundbeacons.Services.CountdownService;

import java.util.ArrayList;
import java.util.List;


public class RouteMapActivity extends AppCompatActivity implements OnMapReadyCallback, View.OnClickListener, DirectionCallback {
    private GoogleMap googleMap;
    private String serverKey = "AIzaSyAuOW532SyK0k2E_ARd4jB_YsASoUpGK6c";
    private LatLng destination = new LatLng(44.9067210, 20.2888280);
    public TextView textView;
    public Button followButton;
    public View slider;
    public View wrapper_slide;
    public TextView textSeconds;
    public TextView textMinutes;
    SlidingUpPanelLayout mLayout;
    private Handler mHandler;
    private int mInterval = 9000;
    private boolean isFirstTime;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_map);
        mHandler = new Handler();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }

        ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);

        mLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        mLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                Log.i("info", "onPanelSlide, offset " + slideOffset);
            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                Log.i("info", "onPanelStateChanged " + newState);
            }
        });
        mLayout.setFadeOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            }
        });

        textView = (TextView) findViewById(R.id.name);
        textView.setText(Html.fromHtml(getString(R.string.hello)));
        followButton = (Button) findViewById(R.id.follow);
        followButton.setText(Html.fromHtml(getString(R.string.follow)));
        followButton.setMovementMethod(LinkMovementMethod.getInstance());
        followButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("http://www.twitter.com/umanoapp"));
                startActivity(i);
            }
        });
        slider = findViewById(R.id.slider_wrapper);
        textView.setVisibility(View.GONE);
        followButton.setVisibility(View.GONE);
        slider.setVisibility(View.GONE);
        wrapper_slide = findViewById(R.id.activity_coupon);
        wrapper_slide.setVisibility(View.GONE);

        textSeconds = (TextView) findViewById(R.id.textSeconds);
        textMinutes = (TextView) findViewById(R.id.textMinutes);
        startRepeatingTask();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        googleMap.setMyLocationEnabled(true);
        isFirstTime = true;
        googleMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {

            @Override
            public void onMyLocationChange(Location loc) {
                // TODO Auto-generated method stub
                requestDirection(loc);
            }
        });

    }

    public void requestDirection(Location origin) {
        GoogleDirection.withServerKey(serverKey)
                .from(new LatLng(origin.getLatitude(), origin.getLongitude()))
                .to(destination)
                .transportMode(TransportMode.TRANSIT)
                .execute(this);

        if (this.isFirstTime) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(origin.getLatitude(), origin.getLongitude()), 14));
            this.isFirstTime = false;
        }
    }

    @Override
    public void onDirectionSuccess(Direction direction, String rawBody) {
        if (direction.isOK()) {
            googleMap.clear();
            ArrayList<LatLng> sectionPositionList = direction.getRouteList().get(0).getLegList().get(0).getSectionPoint();
            googleMap.addMarker(new MarkerOptions().position(sectionPositionList.get(1)));
            Route route = direction.getRouteList().get(0);
            Leg leg = route.getLegList().get(0);
            ArrayList<LatLng> pointList = leg.getDirectionPoint();

            List<Step> stepList = direction.getRouteList().get(0).getLegList().get(0).getStepList();
            ArrayList<PolylineOptions> polylineOptionList = DirectionConverter.createTransitPolyline(this, stepList, 5, Color.RED, 4, Color.BLUE);
            for (PolylineOptions polylineOption : polylineOptionList) {
//                pl = polylineOption.add(pointList.get(i));
                googleMap.addPolyline(polylineOption.geodesic(false));
//                if (polylineOption.getColor() == Color.RED){
//                    googleMap.addPolyline(polylineOption);
//                    continue;
//                }
//
//                for (LatLng ltln : polylineOption.getPoints()){
//                    googleMap.addCircle(new CircleOptions()
//                            .center(ltln)
//                            .radius(3)
//                            .strokeColor(Color.BLUE)
//                            .fillColor(Color.BLUE));
//                }
            }

        }
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(br);
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(br, new IntentFilter(CountdownService.COUNTDOWN_BR));
    }

    @Override
    public void onDirectionFailure(Throwable t) {
    }

    @Override
    public void onClick(View v) {

    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                Activity act = RouteMapActivity.this;
                act.runOnUiThread(new Runnable(){
                    @Override
                    public void run() {
                        wrapper_slide.setVisibility(View.VISIBLE);
                        slider.setVisibility(View.VISIBLE);
                        followButton.setVisibility(View.VISIBLE);
                        textView.setVisibility(View.VISIBLE);

                    } });
                startService(new Intent(RouteMapActivity.this, CountdownService.class));
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                mHandler.postDelayed(mStatusChecker, mInterval);
            }
        }
    };

    void startRepeatingTask() {
        mStatusChecker.run();
    }

    // COUTDOWN TIMER
    private BroadcastReceiver br = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateGUI(intent); // or whatever method used to update your GUI fields
        }
    };

    private void updateGUI(Intent intent) {
        if (intent.getExtras() != null) {
            int minutes = intent.getIntExtra("minutes", 0);
            int seconds = intent.getIntExtra("seconds", 0);
            textMinutes.setText(String.valueOf(minutes));
            textSeconds.setText(String.valueOf(seconds));
        }
    }

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }
}

