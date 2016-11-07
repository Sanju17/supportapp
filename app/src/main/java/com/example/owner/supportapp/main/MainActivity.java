package com.example.owner.supportapp.main;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.owner.supportapp.DBHelper;
import com.example.owner.supportapp.MyCursorAdapter;
import com.example.owner.supportapp.R;
import com.example.owner.supportapp.api.HttpRequestHandler;
import com.example.owner.supportapp.consts.CommonConstants;
import com.example.owner.supportapp.dto.NotificationData;
import com.example.owner.supportapp.service.RegistrationService;
import com.example.owner.supportapp.utils.GlobalVariable;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.message.BasicNameValuePair;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, SwipeRefreshLayout.OnRefreshListener {

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;

    private SharedPreferences sharedPreferences;

    private LinearLayout layoutForFirstRun;
    private LinearLayout layoutForOtherRun;
    private EditText actCodeEditText;
    private Button registerDeviceBtn;

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private double latitude = 0;
    private double longitude = 0;
    private String deviceToken = "";
    private boolean isDeviceRegistered = false;
    private SwipeRefreshLayout swipeRefreshLayout;
    private List<NotificationData> notificationDatas;
    private MyCursorAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent i = new Intent(this, RegistrationService.class);
        startService(i);

        layoutForFirstRun = (LinearLayout) findViewById(R.id.layout_for_first_run);
        layoutForOtherRun = (LinearLayout) findViewById(R.id.layout_for_other_run);
        actCodeEditText = (EditText) findViewById(R.id.act_code_edit_text);
        registerDeviceBtn = (Button) findViewById(R.id.register_device_btn);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        registerDeviceBtn.setEnabled(true);
        swipeRefreshLayout.setOnRefreshListener(this);

        swipeRefreshLayout.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        swipeRefreshLayout.setRefreshing(true);
                                        getNotificationDatas();
//                    fetchMovies();
                                    }
                                }
        );

        sharedPreferences = getSharedPreferences(CommonConstants.MY_PREFREENCES, Context.MODE_PRIVATE);

        if (checkPlayServices()) {
            buildGoogleApiClient();
        }

        // getLocation();
        getLocation1();
        if (sharedPreferences.contains(CommonConstants.RED_FLAG)) {
            // Device has been already registered.
            // update device id
            layoutForFirstRun.setVisibility(View.GONE);
            layoutForOtherRun.setVisibility(View.VISIBLE);
            updateDevice();
            setUpListView();

        } else {
            // App run for the First time
            // register phone
            layoutForFirstRun.setVisibility(View.VISIBLE);
            layoutForOtherRun.setVisibility(View.GONE);
        }
        exportDB();
    }

    //in fragment
    private void setUpListView() {

        final ListView v = (ListView) findViewById(R.id.notification_list_view);
        final TextView noMessageTextView = (TextView) findViewById(R.id.no_message_text_view);

        notificationDatas = getNotificationDatas();
        if (adapter == null) {
            adapter = new MyCursorAdapter(this, notificationDatas);
        }

        if (notificationDatas.size() == 0) {
            v.setVisibility(View.GONE);
            noMessageTextView.setVisibility(View.VISIBLE);
        } else {
            v.setVisibility(View.VISIBLE);
            noMessageTextView.setVisibility(View.GONE);
            v.setAdapter(adapter);
        }
    }

    //in fragment
    @NonNull
    private List<NotificationData> getNotificationDatas() {
        DBHelper dbHelper = new DBHelper(this);

        dbHelper.deleteUnnecessaryData();

        List<NotificationData> notificationDatas = new ArrayList<>();

        // Here we query database
        final Cursor mCursor = dbHelper.getData();
//        exportDB();
        mCursor.moveToFirst();
        while (!mCursor.isAfterLast()) {
            NotificationData notificationData = new NotificationData();
            notificationData.setDate(mCursor.getString(mCursor.getColumnIndex(DBHelper.NOTIFICATION_COLUMN_DATE)));
            notificationData.setMessage(mCursor.getString(mCursor.getColumnIndex(DBHelper.NOTIFICATION_COLUMN_MESSAGE)));
            notificationDatas.add(notificationData);
            mCursor.moveToNext();
        }
        swipeRefreshLayout.setRefreshing(false);
        return notificationDatas;
    }

    private void getLocation() {
        // Create Inner Thread Class
        final Thread background = new Thread(new Runnable() {

            // Define the Handler that receives messages from the thread and update the progress
            private final Handler handler = new Handler() {

                @Override
                public void handleMessage(Message msg) {
                    longitude = msg.getData().getDouble("longitude");
                    latitude = msg.getData().getDouble("latitude");
                    registerDeviceBtn.setEnabled(true);
                }
            };

            @Override
            public void run() {
                // re-run thread until location is set

                try {
                    getLatAndLng();
                    if (mLastLocation != null) {
                        handler.removeCallbacks(this);
                        threadMsg(mLastLocation);
                    } else {
                        handler.postDelayed(this, 1000);
                    }

                } catch (Throwable t) {
                    // just end the background thread
                    Log.i("Animation", "Thread  exception " + t);
                }
            }

            private void threadMsg(Location msg) {
                Message msgObj = handler.obtainMessage();
                Bundle b = new Bundle();
                b.putDouble("latitude", msg.getLatitude());
                b.putDouble("longitude", msg.getLongitude());
                msgObj.setData(b);
                handler.sendMessage(msgObj);
            }
        });
        // Start Thread
        background.start();
    }

    private void updateDevice() {
        // Create Inner Thread Class
        final Thread background = new Thread(new Runnable() {

            // Define the Handler that receives messages from the thread and update the progress
            private final Handler handler = new Handler() {

                public void handleMessage(Message msg) {

                    getLatAndLng();

                    String aResponse = msg.getData().getString("message");

                    if ((null != aResponse)) {
                        if (isDeviceRegistered) {
                            new UpdateDevice().execute();
                        } else {
                            isDeviceRegistered = true;
                        }
                    }
                }
            };

            public void run() {
                // re-run thread until location is set

                try {
                    deviceToken = ((GlobalVariable) getApplicationContext()).getToken();
                    if (deviceToken != null && !deviceToken.equals("")) {
                        handler.removeCallbacks(this);
                        threadMsg(deviceToken);
                    } else {
                        handler.postDelayed(this, 1000);
                    }

                } catch (Throwable t) {
                    // just end the background thread
                    Log.i("Animation", "Thread  exception " + t);
                }
            }

            private void threadMsg(String msg) {
                Message msgObj = handler.obtainMessage();
                Bundle b = new Bundle();
                b.putString("message", msg);
                msgObj.setData(b);
                handler.sendMessage(msgObj);
            }
        });
        // Start Thread
        background.start();  //After call start method thread called run Method
    }

    private void getLatAndLng() {
        if (mLastLocation != null) {
            latitude = mLastLocation.getLatitude();
            longitude = mLastLocation.getLongitude();
//            Toast.makeText(getApplicationContext(), "Latitude: " + latitude + "\nLongitude: " + longitude, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainActivity.this, "(Couldn't get the location. Make sure location is enabled on the device)", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPlayServices();

        //in fragment
        if (notificationDatas != null) {
            notificationDatas = getNotificationDatas();
            if (notificationDatas.size() != 0) {
                adapter.swapItems(notificationDatas);
            }
        }
    }

    public void registerDevice(View view) {
        if (actCodeEditText.getText().toString().isEmpty()) {
            Toast.makeText(MainActivity.this, "Please, enter activation code.", Toast.LENGTH_SHORT).show();
        } else {
            final GlobalVariable globalVariable = (GlobalVariable) getApplicationContext();
            deviceToken = globalVariable.getToken();
            new RegisterDevice().execute();
        }
    }

    public void updateDevice(View view) {
//         new UpdateDevice().execute();
    }

    /**
     * Method to retrieve the location
     * */
    private void retrieveLocation() {

        LocationManager lm = (LocationManager) MainActivity.this.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
        }

        if (!gps_enabled && !network_enabled) {
            if (!sharedPreferences.contains(CommonConstants.IS_LOCATION_ALERT_SHOW)) {
                showAlert();
            }
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi
                .getLastLocation(mGoogleApiClient);
    }

    private void showAlert() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Enable Location")
                .setMessage("Your Locations Settings is set to 'Off'.\nPlease Enable Location to " +
                        "use this app")
                .setPositiveButton("Location Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(myIntent);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    }
                });
        dialog.show();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(CommonConstants.IS_LOCATION_ALERT_SHOW, "YES");
        editor.apply();
        editor.commit();
    }

    /**
     * Creating google api client object
     * */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();

        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    /**
     * Method to verify google play services on the device
     * */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        "This device is not supported.", Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            return false;
        }
        return true;
    }

    @Override
    public void onConnected(Bundle bundle) {
        retrieveLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i("Test", "Connection failed: ConnectionResult.getErrorCode() = "
                + connectionResult.getErrorCode());
    }

    //in fragment
    @Override
    public void onRefresh() {
        // fetchMovie()

        NotificationManager nmagr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nmagr.cancelAll();
        swipeRefreshLayout.setRefreshing(true);
        notificationDatas = getNotificationDatas();

        final ListView v = (ListView) findViewById(R.id.notification_list_view);
        final TextView noMessageTextView = (TextView) findViewById(R.id.no_message_text_view);

        setUpListView();
        if(notificationDatas.size() != 0) {
            adapter.swapItems(notificationDatas);
        }
    }

    class RegisterDevice extends AsyncTask<String, String, String> {

        String url = CommonConstants.BASE_URL + "SupportDevice/RegDeviceByActCodeTest";
        String actCode = "";
        ProgressDialog pb;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            deviceToken = ((GlobalVariable) MainActivity.this.getApplicationContext()).getToken();
            actCode = actCodeEditText.getText().toString();
            pb = new ProgressDialog(MainActivity.this);
            pb.setMessage("Registering Device...");
            pb.setIndeterminate(true);
            pb.setProgress(0);
            pb.show();
        }

        @Override
        protected String doInBackground(String... params) {
            List<NameValuePair> dataParams = new ArrayList<>();
            dataParams.add(new BasicNameValuePair("DeviceId", deviceToken));
            dataParams.add(new BasicNameValuePair("DeviceType", "AND"));
            dataParams.add(new BasicNameValuePair("GeoLat", String.valueOf(latitude)));
            dataParams.add(new BasicNameValuePair("GeoLng", String.valueOf(longitude)));
            dataParams.add(new BasicNameValuePair("ActCode", actCode));
            String response = HttpRequestHandler.putRequest(url, "", "", dataParams);
            return response;
        }

        @Override
        protected void onPostExecute(String s) {
            pb.dismiss();
            String userID = "";
            try {
                JSONObject jsonObject = new JSONObject(s);
                if (jsonObject.getBoolean("success")) {
                    JSONArray jArr = jsonObject.getJSONArray("data");
                    userID = (String) jArr.get(0);

                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(CommonConstants.RED_FLAG, "F");
                    editor.putString(CommonConstants.USER_ID, userID);
                    editor.apply();
                    editor.commit();

                    layoutForFirstRun.setVisibility(View.GONE);
                    layoutForOtherRun.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(getApplicationContext(), jsonObject.getString("message"), Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    class UpdateDevice extends AsyncTask<String, String, String> {
        String userId = "";
        String url = CommonConstants.BASE_URL + "SupportDevice/UpdateDeviceDetailsTest       ";

        @Override
        protected void onPreExecute() {
            userId = sharedPreferences.getString(CommonConstants.USER_ID, "");
        }

        @Override
        protected String doInBackground(String... params) {
            List<NameValuePair> dataParams = new ArrayList<>();
            dataParams.add(new BasicNameValuePair("DeviceId", deviceToken));
            dataParams.add(new BasicNameValuePair("DeviceType", "AND"));
            dataParams.add(new BasicNameValuePair("GeoLat", String.valueOf(latitude)));
            dataParams.add(new BasicNameValuePair("GeoLng", String.valueOf(longitude)));
            dataParams.add(new BasicNameValuePair("userId", userId));
            String response = HttpRequestHandler.putRequest(url, "", "", dataParams);
            return response;
        }

        @Override
        protected void onPostExecute(String s) {

        }
    }

    public void getLocation1() {
        LocationManager locationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new MyLocationListener();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
    }

    private void exportDB(){
        File sd = Environment.getExternalStorageDirectory();
        File data = Environment.getDataDirectory();
        FileChannel source=null;
        FileChannel destination=null;
        String kdjk = getDatabasePath(DBHelper.NOTIFICATION_TABLE_NAME).toString();
        String currentDBPath = "/data/"+ "com.example.owner.supportapp" +"/databases/"+DBHelper.DATABASE_NAME;
        String backupDBPath = DBHelper.NOTIFICATION_TABLE_NAME;
        File currentDB = new File(data, currentDBPath);
        File backupDB = new File(sd, backupDBPath);
        try {
            source = new FileInputStream(currentDB).getChannel();
            destination = new FileOutputStream(backupDB).getChannel();
            destination.transferFrom(source, 0, source.size());
            source.close();
            destination.close();
            Toast.makeText(this, "DB Exported!", Toast.LENGTH_LONG).show();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location loc) {
            longitude = loc.getLongitude();
            Log.v("dinesh", "" + longitude);
            latitude = loc.getLatitude();
            Log.v("dinesh", "" + latitude);

            registerDeviceBtn.setEnabled(true);
        }

        @Override
        public void onProviderDisabled(String provider) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    }
}