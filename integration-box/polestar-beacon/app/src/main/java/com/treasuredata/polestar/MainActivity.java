package com.treasuredata.polestar;

import androidx.appcompat.app.AppCompatActivity;

import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.treasuredata.android.TreasureData;
import com.treasuredata.polestar.naosdk.NaoGeofencing;
import com.treasuredata.polestar.naosdk.NaoLocation;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    final private static String TD_WRITE_KEY = "1/234567890abcdefghijklmnopqrstuvwxyz";
    final private static String TD_ENCRYPTION_KEY = "1234567890";
    final private static String TD_DATABASE = "database_name";
    final private static String TD_TABLE = "table_name";

    final private static String NAO_API_KEY = "emulator";

    final private static LatLng MAP_CENTER_POSITION = new LatLng(37.4187416, -122.0999732);
    final private static boolean MAP_CAMERA_FIXED = true;
    final private static float MAP_ZOOM = 18.0f;

    private TreasureData td;
    private NaoLocation naoLocation;
    private NaoGeofencing naoGeofencing;

    private GoogleMap map;
    private Marker marker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initTreasureData();

        initNao();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        TreasureData.startSession(this);
        naoLocation.startService();
        naoGeofencing.startService();
    }

    @Override
    protected void onStop() {
        super.onStop();
        TreasureData.endSession(this);
        td.uploadEvents();
        naoLocation.stopService();
        // NOT stop geofencing since it needs to send notification on background
        // naoGeofencing.stopService();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        Marker marker = map.addMarker(new MarkerOptions()
                .position(MAP_CENTER_POSITION)
                .title("Marker"));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(MAP_CENTER_POSITION, MAP_ZOOM));
        this.map = map;
        this.marker = marker;
    }

    public void onLocationChanged(Location location) {
        double lat = location.getLatitude();
        double lng = location.getLongitude();

        LatLng position = new LatLng(lat, lng);
        marker.setPosition(position);
        if (!MAP_CAMERA_FIXED) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, MAP_ZOOM));
        }

        Map<String, Object> event = new HashMap<>();
        event.put("td_latitude", lat);
        event.put("td_longitude", lng);
        td.addEvent(TD_TABLE, event);
    }

    private void initTreasureData() {
        // @see https://docs.treasuredata.com/articles/android-sdk
        TreasureData.initializeApiEndpoint("https://in.treasuredata.com");
        TreasureData.initializeEncryptionKey(TD_ENCRYPTION_KEY);
        TreasureData.disableLogging();
        TreasureData.initializeSharedInstance(this, TD_WRITE_KEY);

        TreasureData td = TreasureData.sharedInstance();

        td.setDefaultDatabase(TD_DATABASE);
        td.enableAutoAppendUniqId();
        td.enableAutoAppendModelInformation();
        td.enableAutoAppendAppInformation();
        td.enableAutoAppendLocaleInformation();

        this.td = td;
    }

    private void initNao() {
        NaoLocation naoLocation = new NaoLocation();
        naoLocation.setContext(this);
        naoLocation.setApiKey(NAO_API_KEY);
        this.naoLocation = naoLocation;

        NaoGeofencing naoGeofencing = new NaoGeofencing();
        naoGeofencing.setContext(this);
        naoGeofencing.setApiKey(NAO_API_KEY);
        this.naoGeofencing = naoGeofencing;
    }
}
