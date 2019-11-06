package com.ihm.myapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.ihm.myapp.entity.DirectionObject;
import com.ihm.myapp.entity.LegsObject;
import com.ihm.myapp.entity.PolylineObject;
import com.ihm.myapp.entity.RouteObject;
import com.ihm.myapp.entity.StepsObject;
import com.ihm.myapp.network.GsonRequest;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, LocationListener {
    private static final String TAG = MapsActivity.class.getSimpleName();
    private GoogleMap mMap;
    private List<LatLng> latLngList;
    private TextView distanceValue;
    private TextView durationValue;

    /* GPS */
    private final int LOCATION_GPS_PERMISSIONS_REQUEST = 1;
    private LocationManager locationManager;
    private Location currentLocation;

    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        distanceValue = (TextView) findViewById(R.id.distance_value);
        durationValue = (TextView) findViewById(R.id.duration_value);
        latLngList = new ArrayList<LatLng>();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        /* GPS */
        grantLocationPermissions();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapClickListener(this);
    }

    @Override
    public void onMapClick(LatLng latLng) {
//        if (latLngList.size() > 1) {
//            refreshMap(mMap);
//            latLngList.clear();
//            distanceValue.setText("");
//            durationValue.setText("");
//        }
//        latLngList.add(latLng);
//        Log.d(TAG, "Marker number " + latLngList.size());
//        createMarker(latLng, latLngList.size());
//        if (latLngList.size() == 2) {
//            LatLng origin = latLngList.get(0);
//            LatLng destination = latLngList.get(1);
//            //use Google Direction API to get the route between these Locations
//            String directionApiPath = Helper.getUrl(
//                    String.valueOf(origin.latitude), String.valueOf(origin.longitude),
//                    String.valueOf(destination.latitude), String.valueOf(destination.longitude),
//                    getString(R.string.google_maps_key));
//            Log.d(TAG, "Path " + directionApiPath);
//            getDirectionFromDirectionApiServer(directionApiPath);
//        }

        if (this.currentLocation == null)
            return;

        refreshMap(mMap);
        latLngList.clear();
        distanceValue.setText("");
        durationValue.setText("");
        LatLng destination = latLng;
        createMarker(new LatLng(this.currentLocation.getLatitude(), this.currentLocation.getLongitude()), 1);
        createMarker(destination, 2);
        //use Google Direction API to get the route between these Locations
        String directionApiPath = Helper.getUrl(
                String.valueOf(this.currentLocation.getLatitude()), String.valueOf(this.currentLocation.getLongitude()),
                String.valueOf(destination.latitude), String.valueOf(destination.longitude),
                getString(R.string.google_maps_key));
        Log.d(TAG, "Path " + directionApiPath);
        getDirectionFromDirectionApiServer(directionApiPath);
    }

    private void refreshMap(GoogleMap mapInstance) {
        mapInstance.clear();
    }

    private void createMarker(LatLng latLng, int position) {
        MarkerOptions mOptions = new MarkerOptions().position(latLng);
        if (position == 1) {
            mOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
            mOptions.title("You're here!");
        } else {
            mOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        }
//        addCameraToMap(latLng);
        mMap.addMarker(mOptions);
    }

    private void addCameraToMap(LatLng latLng) {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(latLng)
                .zoom(8)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private void getDirectionFromDirectionApiServer(String url) {
        GsonRequest<DirectionObject> serverRequest = new GsonRequest<DirectionObject>(
                Request.Method.GET,
                url,
                DirectionObject.class,
                createRequestSuccessListener(),
                createRequestErrorListener());
        serverRequest.setRetryPolicy(new DefaultRetryPolicy(
                Helper.MY_SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(serverRequest);
    }

    private Response.Listener<DirectionObject> createRequestSuccessListener() {
        return new Response.Listener<DirectionObject>() {
            @Override
            public void onResponse(DirectionObject response) {
                try {
                    Log.d("JSON Response", response.toString());
                    if (response.getStatus().equals("OK")) {
                        List<LatLng> mDirections = getDirectionPolylines(response.getRoutes());
                        drawRouteOnMap(mMap, mDirections);
                    } else {
                        Toast.makeText(MapsActivity.this, R.string.server_error, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            ;
        };
    }

    private void setRouteDistanceAndDuration(String distance, String duration) {
        distanceValue.setText(distance);
        durationValue.setText(duration);
    }

    private List<LatLng> getDirectionPolylines(List<RouteObject> routes) {
        List<LatLng> directionList = new ArrayList<LatLng>();
        for (RouteObject route : routes) {
            List<LegsObject> legs = route.getLegs();
            for (LegsObject leg : legs) {
                String routeDistance = leg.getDistance().getText();
                String routeDuration = leg.getDuration().getText();
                setRouteDistanceAndDuration(routeDistance, routeDuration);
                List<StepsObject> steps = leg.getSteps();
                for (StepsObject step : steps) {
                    PolylineObject polyline = step.getPolyline();
                    String points = polyline.getPoints();
                    List<LatLng> singlePolyline = decodePoly(points);
                    for (LatLng direction : singlePolyline) {
                        directionList.add(direction);
                    }
                }
            }
        }
        return directionList;
    }

    private Response.ErrorListener createRequestErrorListener() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        };
    }

    private void drawRouteOnMap(GoogleMap map, List<LatLng> positions) {
        PolylineOptions options = new PolylineOptions().width(5).color(Color.RED).geodesic(true);
        options.addAll(positions);
        Polyline polyline = map.addPolyline(options);
    }

    /**
     * Method to decode polyline points
     * Courtesy : http://jeffreysambells.com/2010/05/27/decoding-polylines-from-google-maps-direction-api-with-java
     */
    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }
        return poly;
    }

    /* GPS */
    private void grantLocationPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)
                || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            // Show an explanation to the user *asynchronously* -- don't block
            // this thread waiting for the user's response! After the user
            // sees the explanation, try again to request the permission.
        } else {
            // No explanation needed; request the permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_GPS_PERMISSIONS_REQUEST);

            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
            // app-defined int constant. The callback method gets the
            // result of the request.
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case LOCATION_GPS_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    this.initLocationManager();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                break;
            }
        }
    }

    private void initLocationManager() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    Activity#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for Activity#requestPermissions for more details.
            return;
        }
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // time in milliseconds, distance in meters
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, this);
//        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, null);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            currentLocation = location;
                        }
                    }
                });
    }

    @Override
    public void onLocationChanged(Location location) {
        this.currentLocation = location;
        Log.d("LOCATION::Coordinates", "Latitude: " + location.getLatitude() + ", Longitude:" + location.getLongitude());
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("LOCATION::provider", "disable");
        Toast.makeText(this, "LOCATION Is disabled", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("LOCATION::provider", "enable");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("LOCATION::Status", String.valueOf(status));
    }
}

