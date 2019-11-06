package com.ihm.myapp;

import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
public class Helper {
    private static final String DIRECTION_API = "https://maps.googleapis.com/maps/api/directions/json?origin=";
    public static final int MY_SOCKET_TIMEOUT_MS = 5000;
    public static String getUrl(String originLat, String originLon,
                                String destinationLat, String destinationLon,
                                final String API_KEY){
        return Helper.DIRECTION_API + originLat+","+originLon+"&destination="+destinationLat+","+destinationLon+"&key="+API_KEY;
    }
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}