package com.example.android.enda;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class WifiSelectAndOffload extends Activity implements
        LocationListener, GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    public final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    // Update frequency in seconds
    public static final int UPDATE_INTERVAL_IN_SECONDS = 5;
    // Milliseconds per second
    private static final int MILLISECONDS_PER_SECOND = 1000;
    // Update frequency in milliseconds
    private static final long UPDATE_INTERVAL =
            MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;
    // The fastest update frequency, in seconds
    private static final int FASTEST_INTERVAL_IN_SECONDS = 1;
    // A fast frequency ceiling in milliseconds
    private static final long FASTEST_INTERVAL =
            MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS;
    private static final String url = "http://lijiwei.tk:25004/facedetection/simulateoffloading";
    // Define an object that holds accuracy and frequency parameters
    LocationRequest mLocationRequest;
    Button.OnClickListener mRequestWifi = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {

            ConnectivityManager connMgr = (ConnectivityManager)
                    getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                Location lastestLocation = mLocationClient.getLastLocation();
                tempLocations.add(lastestLocation);
                mLocationClient.requestLocationUpdates(mLocationRequest, WifiSelectAndOffload.this);
            } else {
                mTextView.setText("No network connection available.");
            }

        }
    };
    SharedPreferences mPrefs;
    SharedPreferences.Editor mEditor;
    Button.OnClickListener mScanWifiOnClickListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            ScanWifiTask scanWifiTask = new ScanWifiTask(WifiSelectAndOffload.this);
            scanWifiTask.execute();
        }
    };
    Button.OnClickListener mRestoreWifiOnClickListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            RestoreWifiTask restoreWifiTask = new RestoreWifiTask(WifiSelectAndOffload.this);
            restoreWifiTask.execute();
        }
    };
    Button.OnClickListener mShowWifiConfigurationListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            WifiManager wifiMgr = (WifiManager) WifiSelectAndOffload.this.getSystemService(Context.WIFI_SERVICE);

            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();

            mTextView.setText(wifiInfo.toString() + "\n");

            List<WifiConfiguration> confs = wifiMgr.getConfiguredNetworks();

            List<WifiConfiguration> result = new ArrayList<WifiConfiguration>();

            for (final WifiConfiguration wc : confs) {
                if (wc.SSID.equals("\"PolyUWLAN\"")) result.add(wc);
            }


            Calendar c = Calendar.getInstance();
            int seconds = c.get(Calendar.SECOND);
            int hours = c.get(Calendar.HOUR_OF_DAY);

            mTextView.append(hours + ":" + seconds + " Connect to a specified BSSID \n" + result.toString());
        }
    };
    private LocationClient mLocationClient;
    private TextView mTextView;
    private ListView mListView;
    private NetworkReceiver receiver = new NetworkReceiver();
    private Calendar calendar;
    private Button scanWifi;
    private Button restoreWifi;
    private Button offload;
    private Button show;
    private Button scan;
    private Button request;
    private boolean mReceiveNetwork;
    private String offloadingFilePath;
    private int fixedDelay;
    private int maxCount;
    private String httpGetUrl;
    private float quality;
    Button.OnClickListener mScanListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            String scanSSID = "PolyUWLAN";

            WifiManager wifiMgr = (WifiManager) WifiSelectAndOffload.this.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
            Log.e("error", "Wifi Info Before Delete: " + wifiInfo);


            if (!wifiMgr.isWifiEnabled()) {
                wifiMgr.setWifiEnabled(true);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            wifiMgr.startScan();
            List<ScanResult> scanResults = wifiMgr.getScanResults();

            Collections.sort(scanResults, new Comparator<ScanResult>() {
                @Override
                public int compare(ScanResult o1, ScanResult o2) {
                    return o2.level - o1.level;
                }
            });

            Log.e("error", "WiFi scan succeeds.");

            List<String> polyuWlans = new ArrayList<String>();

            for (ScanResult scanResult : scanResults) {
                if (scanResult.SSID.equals(scanSSID) && scanResult.level > ((quality * 100 / 2) - 100))
                    polyuWlans.add(scanResult.BSSID + " " + scanResult.level);
            }
            Log.e("error", "Test scan:" + polyuWlans);


            ArrayAdapter<String> adapter = new ArrayAdapter<String>(WifiSelectAndOffload.this, android.R.layout.simple_list_item_1, polyuWlans);
            mListView.setAdapter(adapter);

            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    String item = ((TextView) view).getText().toString().substring(0, 17);

                    Toast.makeText(getBaseContext(), "Connecting to " + item, Toast.LENGTH_SHORT).show();

                    (new ConnectToWifiTask(WifiSelectAndOffload.this, item)).execute();


                }
            });
        }
    };
    private int serverDelay;
    Button.OnClickListener mOffloadListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {

            mTextView.setText(null);
            mListView.setAdapter(null);


            //Get battery statistics
            BatteryManager mBatteryManager =
                    new BatteryManager();
            Long energy =
                    mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

            mTextView.append("Remaining energy = " + energy + "%" + "\n");
            writeToFile("Remaining energy at start= " + energy + "%", "OffloadingLog.txt");
            //start offloading tasks using ScheduledExecutorService
            calendar = Calendar.getInstance();
            int hours = calendar.get(Calendar.HOUR_OF_DAY);

            int mins = calendar.get(Calendar.MINUTE);
            int seconds = calendar.get(Calendar.SECOND);


            mTextView.append(hours + ":" + mins + ":" + seconds + " The commencement of offloading, local delay: " + fixedDelay + ", server delay: " + serverDelay + ", maxCount: " +
                    maxCount + ", upload: " + offloadingFilePath.substring(23) + ", download: " + httpGetUrl.substring(52) + "\n");
            writeToFile(hours + ":" + mins + ":" + seconds + " The commencement of offloading, maxCount: " + maxCount + ", delay: " + fixedDelay, "OffloadingLog.txt");

            long startingTime = calendar.getTimeInMillis();

            (new ScheduleOffloadingTasks(startingTime)).offloadingTasks();
        }
    };
    private List<Location> tempLocations = new ArrayList<Location>();

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);


        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create();
        // Use high accuracy
        mLocationRequest.setPriority(
                LocationRequest.PRIORITY_HIGH_ACCURACY);
        // Set the update interval to 5 seconds
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        // Set the fastest update interval to 1 second
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

        mLocationClient = new LocationClient(this, this, this);

        mLocationClient.connect();


        mTextView = (TextView) findViewById(R.id.textView1);

        mListView = (ListView) findViewById(R.id.listview1);


        scanWifi = (Button) findViewById(R.id.scanWiFi);


        restoreWifi = (Button) findViewById(R.id.restoreWiFi);

        offload = (Button) findViewById(R.id.offload);

        show = (Button) findViewById(R.id.showWifiConfiguration);

        scan = (Button) findViewById(R.id.scanAndChoose);

        request = (Button) findViewById(R.id.requestWifiSelect);

        mPrefs = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
        mEditor = mPrefs.edit();

        mReceiveNetwork = false;

        calendar = Calendar.getInstance();

//        IntentFilter filter = new IntentFilter();
//        filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
//        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);


//        receiver = new NetworkReceiver();
//        this.registerReceiver(receiver, filter);


    }

    @Override
    public void onResume() {
        super.onResume();

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        offloadingFilePath = sharedPrefs.getString("listOffloadingFilePath", "/sdcard/Pictures/input_small.jpg");

        fixedDelay = Integer.parseInt(sharedPrefs.getString("listFixedDelay", "2"));

        maxCount = Integer.parseInt(sharedPrefs.getString("listCount", "10"));

        httpGetUrl = sharedPrefs.getString("listHttpGet", "http://lijiwei.tk:25004/facedetection/output/output_small.jpg");

        quality = Float.parseFloat(sharedPrefs.getString("listQuality", "0.4"));

        serverDelay = Integer.parseInt(sharedPrefs.getString("listServerDelay", "1"));


        scanWifi.setOnClickListener(mScanWifiOnClickListener);
        restoreWifi.setOnClickListener(mRestoreWifiOnClickListener);
        offload.setOnClickListener(mOffloadListener);
        show.setOnClickListener(mShowWifiConfigurationListener);
        scan.setOnClickListener(mScanListener);

        request.setOnClickListener(mRequestWifi);


        if (mPrefs.contains("KEY_RECEIVE_ON")) {
            mReceiveNetwork = mPrefs.getBoolean("KEY_RECEIVE_ON", false);
        } else {
            mEditor.putBoolean("KEY_RECEIVE_ON", false);
            mEditor.commit();
        }


        if (true) {


            mReceiveNetwork = true;

        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // Gets the user's network preference settings
//        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Retrieves a string value for the preferences. The second parameter
        // is the default value to use if a preference value is not found.
//        sPref = sharedPrefs.getString("listPref", "Wi-Fi");


    }

    @Override
    public void onPause() {

        mEditor.putBoolean("KEY_RECEIVE_ON", mReceiveNetwork);
        mEditor.commit();


        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

//        if (receiver != null) {
//            this.unregisterReceiver(receiver);
//        }
        mLocationClient.disconnect();

    }

    // Populates the activity's options menu.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return true;
    }

    // Handles the user's menu selection.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                Intent settingsActivity = new Intent(getBaseContext(), SettingsActivity.class);
                startActivity(settingsActivity);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Given a URL, establishes an HttpUrlConnection and retrieves
    // the web page content as a InputStream, which it returns as
    // a string.
    private String downloadUrl(String myurl) throws IOException {
        InputStream is = null;
        // Only display the first 500 characters of the retrieved
        // web page content.
        int len = 500;

        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            Log.e("error", "The response is: " + response);
            is = conn.getInputStream();

            // Convert the InputStream into a string
            String contentAsString = readIt(is, len);
            return contentAsString;

            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    // Reads an InputStream and converts it to a String.
    private String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
        Reader reader = null;
        reader = new InputStreamReader(stream, "UTF-8");
        char[] buffer = new char[len];
        reader.read(buffer);
        return new String(buffer);
    }

    private void writeToFile(String text, String filename) {


        boolean sdCardExist = Environment.getExternalStorageState()
                .equals(android.os.Environment.MEDIA_MOUNTED);
        if (sdCardExist) {

            File sdDir = Environment.getExternalStorageDirectory();

            FileWriter fw;
            try {
                fw = new FileWriter(sdDir.toString() + "/" + filename, true);

                fw.write(text);
                fw.write("\r\n");


                fw.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }


        } else
            Log.d("external storage", "failed");

    }

    @Override
    public void onConnected(Bundle dataBundle) {
        // Display the connection status
        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();


    }

    @Override
    public void onDisconnected() {
        // Display the connection status
        Toast.makeText(this, "Disconnected. Please re-connect.",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
//            showErrorDialog(connectionResult.getErrorCode());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        mTextView.setText(null);
        mTextView.append("Detecting Location.\n");
        Log.e("error", "Location changed.");
        tempLocations.add(location);
        Location[] tempLocationsArray = tempLocations.toArray(new Location[tempLocations.size()]);
        (new RequestWifiTask()).execute(tempLocationsArray);


        mLocationClient.removeLocationUpdates(WifiSelectAndOffload.this);

    }

    private class ScheduleOffloadingTasks {
        public int count;
        private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        private ScheduledFuture<?> offloadHandle;
        private long startingTime;

        public ScheduleOffloadingTasks(long startingTime) {
            this.count = 1;
            this.startingTime = startingTime;
        }

        public void offloadingTasks() {
            final Runnable offloadingTask = new Runnable() {
                @Override
                public void run() {
//                   (new UploadBitmap()).execute(url,mCurrentPhotoPath);
//                     (new RestoreWifiTask(WifiSelectAndOffload.this)).execute();
//                     (new UploadBitmap()).execute(url,offloadFilePath);
                    runOnUiThread(new Runnable() {
                        public void run() {

                            calendar = Calendar.getInstance();
                            int hours = calendar.get(Calendar.HOUR_OF_DAY);

                            int mins = calendar.get(Calendar.MINUTE);
                            int seconds = calendar.get(Calendar.SECOND);

                            Log.e("error", "Offloading starts");
                            mTextView.append(hours + ":" + mins + ":" + seconds + " Start upload." + "\n");
                            writeToFile(hours + ":" + mins + ":" + seconds + " Start upload.", "OffloadingLog.txt");
                        }
                    });


                    String result = null;


                    while (result == null) {


                        try {

                            result = (new UploadImage(url, "camera.jpg")).uploadBitmap(offloadingFilePath);

                        } catch (IOException e) {
                            result = null;
                            Log.e("error", "Attempt to resend.");
                        }

                    }

                    runOnUiThread(new Runnable() {
                        public void run() {

                            calendar = Calendar.getInstance();
                            int hours = calendar.get(Calendar.HOUR_OF_DAY);

                            int mins = calendar.get(Calendar.MINUTE);
                            int seconds = calendar.get(Calendar.SECOND);


                            mTextView.append(hours + ":" + mins + ":" + seconds + " Upload success." + "\n");
                            writeToFile(hours + ":" + mins + ":" + seconds + " Upload success.", "OffloadingLog.txt");
                        }
                    });


                    try {
                        Thread.sleep(serverDelay * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }


                    runOnUiThread(new Runnable() {
                        public void run() {

                            calendar = Calendar.getInstance();
                            int hours = calendar.get(Calendar.HOUR_OF_DAY);

                            int mins = calendar.get(Calendar.MINUTE);
                            int seconds = calendar.get(Calendar.SECOND);


                            mTextView.append(hours + ":" + mins + ":" + seconds + " Start Download." + "\n");
                            writeToFile(hours + ":" + mins + ":" + seconds + " Start Download.", "OffloadingLog.txt");
                        }
                    });


                    HttpClient httpclient = new DefaultHttpClient();
                    HttpGet httpget = new HttpGet(httpGetUrl);

                    HttpResponse response;

                    boolean flagSuccess = false;

                    while (!flagSuccess) {
                        try {
                            response = httpclient.execute(httpget);
                            if (response.getStatusLine().getStatusCode() == 200) {
                                // Get hold of the response entity
                                HttpEntity entity = response.getEntity();
                                if (entity != null) {
                                    InputStream instream = entity.getContent();
                                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
                                    String path = Environment.getExternalStorageDirectory().toString() + "/testOffloading/" + timeStamp + ".jpg";
                                    FileOutputStream output = new FileOutputStream(path);
                                    int bufferSize = 1024;
                                    byte[] buffer = new byte[bufferSize];
                                    int len = 0;
                                    while ((len = instream.read(buffer)) != -1) {
                                        output.write(buffer, 0, len);
                                    }
                                    output.close();
                                    flagSuccess = true;
                                    if (flagSuccess == false)
                                        Log.e("error", "Attempt to redownload.");
                                }
                            }
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (ClientProtocolException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    runOnUiThread(new Runnable() {
                        public void run() {

                            calendar = Calendar.getInstance();
                            int hours = calendar.get(Calendar.HOUR_OF_DAY);

                            int mins = calendar.get(Calendar.MINUTE);
                            int seconds = calendar.get(Calendar.SECOND);


                            mTextView.append(hours + ":" + mins + ":" + seconds + " Download success." + "\n");
                            writeToFile(hours + ":" + mins + ":" + seconds + " Download success.", "OffloadingLog.txt");
                        }
                    });


                    if (count >= maxCount) {
                        scheduler.schedule(new Runnable() {
                            @Override
                            public void run() {
                                offloadHandle.cancel(true);
                                runOnUiThread(new Runnable() {
                                    public void run() {

                                        //Get battery statistics
                                        BatteryManager mBatteryManager =
                                                new BatteryManager();
                                        Long energy =
                                                mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

                                        mTextView.append("Remaining energy = " + energy + "%" + "\n");
                                        writeToFile("Remaining energy at end= " + energy + "%", "OffloadingLog.txt");

                                        calendar = Calendar.getInstance();
                                        int hours = calendar.get(Calendar.HOUR_OF_DAY);

                                        int mins = calendar.get(Calendar.MINUTE);
                                        int seconds = calendar.get(Calendar.SECOND);


                                        long endingTime = calendar.getTimeInMillis();
                                        mTextView.append(hours + ":" + mins + ":" + seconds + " Offload completes. Overall time: " + (endingTime - startingTime) / 1000.0 + "\n");
                                        writeToFile(hours + ":" + mins + ":" + seconds + " Offload completes.", "OffloadingLog.txt");
                                        Log.e("error", "Offloading ends");


                                    }
                                });

                            }
                        }, 0, TimeUnit.SECONDS);
                    }

                    //schedule tasks stop conditions depend on this count;
                    count++;
                }
            };

            offloadHandle = scheduler.scheduleWithFixedDelay(offloadingTask, 0, fixedDelay, TimeUnit.SECONDS);


        }
    }

    private class ScanWifiTask extends AsyncTask<Void, Void, String> {

        public Context myCtx;

        public ScanWifiTask(Context ctx) {
            myCtx = ctx;
        }

        @Override
        protected String doInBackground(Void... args) {

            String scanSSID = "PolyUWLAN";


            WifiManager wifiMgr = (WifiManager) myCtx.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
            Log.e("error", "Wifi Info Before Delete: " + wifiInfo);


            if (!wifiMgr.isWifiEnabled()) {
                wifiMgr.setWifiEnabled(true);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }


            List<WifiConfiguration> confs = wifiMgr.getConfiguredNetworks();


            for (final WifiConfiguration config : confs) {

                if (config.SSID.equals("\"PolyUWLAN\"")) {

                    Log.e("error", "PolyUWLAN networkID is " + config.networkId);
                    wifiMgr.removeNetwork(config.networkId);
                    wifiMgr.saveConfiguration();

                    break;
                }
            }


            wifiInfo = wifiMgr.getConnectionInfo();
            Log.e("error", "Wifi Info After Delete: " + wifiInfo);


            List<ScanResult> polyuWlans = new ArrayList<ScanResult>();
            WifiConfiguration newConf = new WifiConfiguration();


//            while(wifiInfo.getNetworkId() == -1){


            wifiMgr.startScan();
            List<ScanResult> scanResults = wifiMgr.getScanResults();
            Log.e("error", "WiFi scan succeeds.");

            polyuWlans = new ArrayList<ScanResult>();

            for (ScanResult scanResult : scanResults) {
                if (scanResult.SSID.equals(scanSSID) && scanResult.level > ((quality * 100 / 2) - 100))
                    polyuWlans.add(scanResult);
            }


//            List<WifiConfiguration> confs = wifiMgr.getConfiguredNetworks();
//
//
//
//            for(final WifiConfiguration config : confs) {
//
//                if(config.SSID.equals("\"PolyUWLAN\"") ){
//
//                    Log.e("error","PolyUWLAN networkID is "+config.networkId);
//
//
//               break; }
//            }


            //select the polyuwlan with strongest RSSI

            if (polyuWlans.size() > 0) {
                Log.e("error", "Polyuwlan number is " + polyuWlans.size());


                int randomNum = (new Random()).nextInt((polyuWlans.size() - 1) + 1);


                ScanResult strongestPolyuWlan = polyuWlans.get(randomNum);
//                for (ScanResult polyuWlan : polyuWlans) {
//                    if (polyuWlan.level > strongestPolyuWlan.level) {
//
//                        strongestPolyuWlan = polyuWlan;
//                    }
//                }

                Log.e("error", "It chooses No. " + (randomNum + 1));
                newConf = new WifiConfiguration();
                newConf.SSID = "\"" + strongestPolyuWlan.SSID + "\"";
                newConf.BSSID = strongestPolyuWlan.BSSID;
//                newConf.BSSID = "18:64:72:19:8d:80";
                WifiEnterpriseConfig enterpriseConfig = new WifiEnterpriseConfig();
                newConf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
                newConf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.IEEE8021X);
                enterpriseConfig.setIdentity("12900263r");
                enterpriseConfig.setPassword("Zju3061001016");
                enterpriseConfig.setEapMethod(WifiEnterpriseConfig.Eap.PEAP);
                enterpriseConfig.setPhase2Method(WifiEnterpriseConfig.Phase2.MSCHAPV2);
                newConf.enterpriseConfig = enterpriseConfig;


                Log.e("error", "Wifi Info:" + wifiInfo.getSupplicantState());

                if (!(wifiInfo.getSupplicantState() == SupplicantState.ASSOCIATING ||
                        wifiInfo.getSupplicantState() == SupplicantState.AUTHENTICATING)) {
                    int networkID;
                    networkID = wifiMgr.addNetwork(newConf);
//                    Log.e("error", "New Network ID is " + networkID);


                    wifiMgr.enableNetwork(networkID, false);

                    boolean saveSuccess;
                    saveSuccess = wifiMgr.saveConfiguration();
                    Log.e("error", "Save success: " + saveSuccess);

                    wifiMgr.enableNetwork(networkID, true);


                }
            }


//            newConf.SSID = "\"hku\"";
//            newConf.BSSID = "18:64:72:19:8d:80";
//
//            newConf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
//
//            int networkID;
//                networkID = wifiMgr.addNetwork(newConf);
//                Log.e("error","NetworkID is "+ networkID);


//            confs = wifiMgr.getConfiguredNetworks();

            //give some time to connect to an AP and then check whether it is connected
//                try {
//                    Thread.sleep(5000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }


//                wifiInfo = wifiMgr.getConnectionInfo();
//                Log.e("error", "Wifi Info Network ID: " + wifiInfo.getNetworkId());
//            }


            Log.e("error", "Wifi scan task finishes, Wifi Info is:" + wifiInfo);
//            return wifiInfo.toString();


            confs = wifiMgr.getConfiguredNetworks();

            List<WifiConfiguration> result = new ArrayList<WifiConfiguration>();

            for (final WifiConfiguration wc : confs) {
                if (wc.SSID.equals("\"PolyUWLAN\"")) result.add(wc);
            }


            Calendar c = Calendar.getInstance();
            int seconds = c.get(Calendar.SECOND);
            int hours = c.get(Calendar.HOUR_OF_DAY);

            return hours + ":" + seconds + " Connect to a specified BSSID \n" + result.toString();

//            return confs.toString();

        }

        @Override
        protected void onPostExecute(String results) {

            mTextView.setText(results);

        }
    }

    private class RestoreWifiTask extends AsyncTask<Void, Void, String> {

        public Context myCtx;

        public RestoreWifiTask(Context ctx) {
            myCtx = ctx;
        }

        @Override
        protected String doInBackground(Void... args) {

            WifiManager wifiMgr = (WifiManager) myCtx.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
            Log.e("error", "Wifi Info Before: " + wifiInfo);


//            if(wifiMgr.startScan()) {
//                List<ScanResult> scanResults = wifiMgr.getScanResults();
//                Log.e("error", "WiFi scan succeeds.");
//                return scanResults.toString();
//            }
//            else
//            return null;
            List<WifiConfiguration> confs = wifiMgr.getConfiguredNetworks();

            WifiConfiguration newConf = new WifiConfiguration();

            for (final WifiConfiguration config : confs) {

                if (config.SSID.equals("\"PolyUWLAN\"")) {

                    Log.e("error", "PolyUWLAN networkID is " + config.networkId);


                    wifiMgr.removeNetwork(config.networkId);
                    wifiMgr.saveConfiguration();


                    break;
                }
            }


            newConf.SSID = "\"PolyUWLAN\"";
            WifiEnterpriseConfig enterpriseConfig = new WifiEnterpriseConfig();
            newConf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
            newConf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.IEEE8021X);
            enterpriseConfig.setIdentity("12900263r");
            enterpriseConfig.setPassword("Zju3061001016");
            enterpriseConfig.setEapMethod(WifiEnterpriseConfig.Eap.PEAP);
            enterpriseConfig.setPhase2Method(WifiEnterpriseConfig.Phase2.MSCHAPV2);
            newConf.enterpriseConfig = enterpriseConfig;


            int networkID;
            networkID = wifiMgr.addNetwork(newConf);
            Log.e("error", "New Network ID is " + networkID);


            wifiMgr.enableNetwork(networkID, false);

            boolean saveSuccess;
            saveSuccess = wifiMgr.saveConfiguration();
            Log.e("error", "Save success: " + saveSuccess);


            wifiMgr.enableNetwork(networkID, true);

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            wifiInfo = wifiMgr.getConnectionInfo();
            Log.e("error", "Wifi Info After: " + wifiInfo);


            if (wifiInfo.getNetworkId() == -1) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                wifiInfo = wifiMgr.getConnectionInfo();
                Log.e("error", "Wifi Info: " + wifiInfo);
            }


//            newConf.SSID = "\"hku\"";
//            newConf.BSSID = "18:64:72:19:8d:80";
//
//            newConf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
//
//            int networkID;
//                networkID = wifiMgr.addNetwork(newConf);
//                Log.e("error","NetworkID is "+ networkID);


//            confs = wifiMgr.getConfiguredNetworks();


            Log.e("error", "Restore Wifi succeeds.");

            confs = wifiMgr.getConfiguredNetworks();

            WifiConfiguration result = new WifiConfiguration();

            for (WifiConfiguration wc : confs) {
                if (wc.SSID.equals("\"PolyUWLAN\"")) result = wc;
            }


            Calendar c = Calendar.getInstance();
            int seconds = c.get(Calendar.SECOND);
            int hours = c.get(Calendar.HOUR_OF_DAY);

            return hours + ":" + seconds + "\n" + result.toString();


        }

        @Override
        protected void onPostExecute(String results) {

            mTextView.setText(results);

        }
    }

    private class ConnectToWifiTask extends AsyncTask<Void, Void, String> {

        public Context myCtx;
        private String BSSID;

        public ConnectToWifiTask(Context ctx, String BSSID) {
            myCtx = ctx;
            this.BSSID = BSSID;
        }

        @Override
        protected String doInBackground(Void... args) {

            WifiManager wifiMgr = (WifiManager) myCtx.getSystemService(Context.WIFI_SERVICE);
//            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
//            Log.e("error", "Wifi Info Before: "+ wifiInfo);


//            if(wifiMgr.startScan()) {
//                List<ScanResult> scanResults = wifiMgr.getScanResults();
//                Log.e("error", "WiFi scan succeeds.");
//                return scanResults.toString();
//            }
//            else
//            return null;
            List<WifiConfiguration> confs = wifiMgr.getConfiguredNetworks();

            WifiConfiguration newConf = new WifiConfiguration();

            for (final WifiConfiguration config : confs) {

                if (config.SSID.equals("\"PolyUWLAN\"")) {

                    Log.e("error", "PolyUWLAN networkID is " + config.networkId);


                    wifiMgr.removeNetwork(config.networkId);
                    wifiMgr.saveConfiguration();


                    break;
                }
            }


            newConf.SSID = "\"PolyUWLAN\"";
            newConf.BSSID = this.BSSID;
            WifiEnterpriseConfig enterpriseConfig = new WifiEnterpriseConfig();
            newConf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
            newConf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.IEEE8021X);
            enterpriseConfig.setIdentity("12900263r");
            enterpriseConfig.setPassword("Zju3061001016");
            enterpriseConfig.setEapMethod(WifiEnterpriseConfig.Eap.PEAP);
            enterpriseConfig.setPhase2Method(WifiEnterpriseConfig.Phase2.MSCHAPV2);
            newConf.enterpriseConfig = enterpriseConfig;


            int networkID;
            networkID = wifiMgr.addNetwork(newConf);
//            Log.e("error","New Network ID is "+ networkID);


            wifiMgr.enableNetwork(networkID, false);

            boolean saveSuccess;
            saveSuccess = wifiMgr.saveConfiguration();
//            Log.e("error","Save success: "+ saveSuccess);


            wifiMgr.enableNetwork(networkID, true);


            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
            Log.e("error", "Wifi Info After: " + wifiInfo);


            if (wifiInfo.getNetworkId() == -1) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                wifiInfo = wifiMgr.getConnectionInfo();
                Log.e("error", "Wifi Info: " + wifiInfo);
            }


            Log.e("error", "Restore Wifi succeeds.");

            confs = wifiMgr.getConfiguredNetworks();

            WifiConfiguration result = new WifiConfiguration();

            for (WifiConfiguration wc : confs) {
                if (wc.SSID.equals("\"PolyUWLAN\"")) result = wc;
            }


            Calendar c = Calendar.getInstance();
            int seconds = c.get(Calendar.SECOND);
            int hours = c.get(Calendar.HOUR_OF_DAY);

            return hours + ":" + seconds + " " + wifiInfo.toString() + "\n" + result.toString();


        }

        @Override
        protected void onPostExecute(String results) {

            mTextView.setText(results);

        }
    }

    private class RequestWifiTask extends AsyncTask<Location, Void, String> {
        @Override
        protected String doInBackground(Location... locations) {


            int num = locations.length;

            double lat = locations[num - 1].getLatitude();
            double lon = locations[num - 1].getLongitude();

            lat = RoundLocationToFiveDecimal.round(lat, 5);
            lon = RoundLocationToFiveDecimal.round(lon, 5);


            String urlStr = "http://lijiwei.tk:25004/wifiserver/wifiselect?x=" + lat + "&y=" + lon;

            Log.e("error", "URL: " + urlStr);

            try {
                return downloadUrl(urlStr);
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            }


        }

        @Override
        protected void onPostExecute(String result) {
            mTextView.append(result);

        }
    }

    public class NetworkReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
//               WifiManager wifiMgr = (WifiManager)context.getSystemService(WIFI_SERVICE);
//            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
//            Log.e("error","Backgound check wifi info supplicate:"+ wifiInfo.getSupplicantState());
            calendar = Calendar.getInstance();
            int hours = calendar.get(Calendar.HOUR_OF_DAY);

            int mins = calendar.get(Calendar.MINUTE);
            int seconds = calendar.get(Calendar.SECOND);

            if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
//                mTextView.append(hours+":"+mins+":"+seconds+" Detailed Network State: "+ networkInfo.getDetailedState()+"\n");
                writeToFile(hours + ":" + mins + ":" + seconds + " Detailed Network State: " + networkInfo.getDetailedState(), "WiFiConnectionLog.txt");
            } else if (intent.getAction().equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
                SupplicantState supplicantState = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
//                mTextView.append(hours+":"+mins+":"+seconds+" Supplicate State: "+ supplicantState + "\n");
                writeToFile(hours + ":" + mins + ":" + seconds + " Supplicate State: " + supplicantState, "WiFiConnectionLog.txt");
            }

//            Log.e("error","detected network condition change");


//            WifiInfo wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);

//            Log.e("error", "Test Coarse: "+networkInfo.getState());
//            Log.e("error", "Test Fine: "+networkInfo.getDetailedState());

//            mTextView.append("Supplicate State: "+ wifiInfo.getSupplicantState()+ "\n");

//            if (wifiInfo.getNetworkId() == -1) {
//
//
//
//                    executeScanWifiTask(context);
//
//
//
//            }
        }

        private void executeScanWifiTask(Context context) {
            ScanWifiTask scanWifiTask = new ScanWifiTask(context);
            scanWifiTask.execute();
            Log.e("error", "execute scan wifi task");

        }

    }


}