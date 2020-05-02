package in.wizelab.timecapsule;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.List;
import java.util.Locale;

public class TCService extends Service
{
    private final IBinder mBinder = new MyBinder();

    private static final String TAG = TCService.class.getSimpleName();
    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 1000;
    private static final float LOCATION_DISTANCE = (float) 0.1;
    private Context appContext;

    Location loc;
    protected List<Location> locationList;

    protected String locality;
    protected String mlastLocality;
    protected String area;
    protected String country;
    protected String featureName;

    protected long mLastOnlineQuery;



    private class LocationListener implements android.location.LocationListener{
        Location mLastLocation;
        public LocationListener(String provider)
        {
            Log.d(TAG, "LocationListener " + provider);
        }

        @Override
        public void onLocationChanged(Location location) {
            try {
                Geocoder geo = new Geocoder(appContext, Locale.getDefault());
                List<Address> addresses = geo.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                if (addresses.isEmpty()) {
                    Log.d(TAG, "Waiting for Location");
                } else {
                    if (addresses.size() > 0) {
                        featureName = addresses.get(0).getFeatureName();
                        locality = addresses.get(0).getLocality();
                        area = addresses.get(0).getAdminArea();
                        country = addresses.get(0).getCountryName();
                        Log.d(TAG, featureName + ", " + locality + ", " + area + ", " + country);

                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            loc = location;
            Log.d(TAG, "onLocationChanged: " + location);

            if (locality != null) {
                if (mlastLocality == null||!mlastLocality.equals(locality)||System.currentTimeMillis()>mLastOnlineQuery) {
                    mlastLocality = locality;
                    onlineParseQuery(locality);
                    localParseQuery(location);
                } else {
                    sendNotification("New Location: " + featureName + ", " + locality + ", " + area);
                    if (mlastLocality.equals(locality)) {
                        localParseQuery(location);
                    } else {
                        onlineParseQuery(locality);
                    }
                }
                mLastLocation=location;
            }
        }

        void localParseQuery(Location location){
            Log.d(TAG,"Starting offline background query");
            ParseGeoPoint parseGeoPoint = new ParseGeoPoint(location.getLatitude(),location.getLongitude());
            ParseQuery<ParseObject> query = ParseQuery.getQuery(ParseConstants.CLASS_MESSAGES);
            query.fromLocalDatastore();
            query.whereWithinMiles(ParseConstants.KEY_LOCATION_POINT,parseGeoPoint,0.1);
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if (e == null) {
                        if(!objects.isEmpty()){
                            Log.d(TAG,"OFFLINE: New Location, capsules discovered");
                            sendNotification("Time Capsules Discovered: "+objects.size());
                            // Iterating over the results
                            for (int i = 0; i < objects.size(); i++) {
                                // Now get Latitude and Longitude of the object
                                double queryLatitude = objects.get(i).getParseGeoPoint(ParseConstants.KEY_LOCATION_POINT).getLatitude();
                                double queryLongitude = objects.get(i).getParseGeoPoint(ParseConstants.KEY_LOCATION_POINT).getLongitude();
                                Log.d(TAG, queryLatitude + ", " + queryLongitude);
                            }
                        }else{
                            //cancelNotification(appContext,0);
                        }
                    } else {
                        Log.d(TAG,"Error");
                    }
                }
            });
        }

        void onlineParseQuery(String locality){
            mLastOnlineQuery=System.currentTimeMillis()+1*60*60*1000;
            Log.d(TAG,"Online background query");
            ParseQuery<ParseObject> query = ParseQuery.getQuery(ParseConstants.CLASS_MESSAGES);
            query.whereContains(ParseConstants.KEY_LOCATION_LOCALITY,locality);
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if (e == null) {
                        if(!objects.isEmpty()){
                            ParseObject.pinAllInBackground(objects);
                        }else{
                            cancelNotification(appContext,0);
                        }
                    } else {
                        Log.d(TAG,"Error");
                    }
                }
            });
        }

        @Override
        public void onProviderDisabled(String provider)
        {
            Log.d(TAG, "onProviderDisabled: " + provider);
        }
        @Override
        public void onProviderEnabled(String provider)
        {
            Log.d(TAG, "onProviderEnabled: " + provider);
            sendNotification(provider+" enabled");
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            Log.d(TAG, "onStatusChanged: " + provider);
        }
    }
    LocationListener[] mLocationListeners = new LocationListener[] {
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    public Location getLocation(){
        return loc;
    }

    public List<Location> getLocations(){
        return locationList;
    }
    @Override
    public IBinder onBind(Intent arg0)
    {
        return mBinder;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        appContext=getBaseContext();//Get the context here

        Log.d(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }
    @Override
    public void onCreate()
    {
        Log.d(TAG, "onCreate");
        initializeLocationManager();
        /*
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[1]);
        } catch (java.lang.SecurityException ex) {
            Log.d(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }
        */
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[0]);
        } catch (java.lang.SecurityException ex) {
            Log.d(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }

    }
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        Intent broadcastIntent = new Intent(RestartTCService.class.getSimpleName());
        //sendBroadcast(broadcastIntent);
        Log.d(TAG, "onDestroy");
        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listners, ignore", ex);
                }
            }
        }
    }

    private void initializeLocationManager() {
        if (mLocationManager == null) {
            Log.d(TAG, "initializeLocationManager");
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }
    public class MyBinder extends Binder {
        TCService getService() {
            return TCService.this;
        }
    }

    void showToast(final String message){
        if(null !=appContext){
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run()
                {
                    Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show();
                }
            });

        }
    }

    private void sendNotification(String message){

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

        Notification n  = new Notification.Builder(this)
                .setContentTitle("Time Capsules")
                .setContentText(message)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pIntent)
                .setAutoCancel(true).getNotification();
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0, n);
    }

    public static void cancelNotification(Context ctx, int notifyId) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nMgr = (NotificationManager) ctx.getSystemService(ns);
        nMgr.cancel(notifyId);
    }
}