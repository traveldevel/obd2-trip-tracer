package ro.traveldevel.obdtriptracerapp;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.control.VinCommand;
import com.github.pires.obd.commands.engine.LoadCommand;
import com.github.pires.obd.commands.engine.OilTempCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.engine.ThrottlePositionCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.commands.temperature.EngineCoolantTemperatureCommand;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import ro.traveldevel.config.Constants;
import ro.traveldevel.db.DateHelper;
import ro.traveldevel.db.entity.OBD2Record;
import ro.traveldevel.db.entity.OBD2Trip;
import ro.traveldevel.db.repo.RecordRepository;
import ro.traveldevel.db.repo.TripRepository;
import ro.traveldevel.obd.BluetoothDeviceConnector;
import ro.traveldevel.util.FileHelper;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();
    private static final FileHelper Log = new FileHelper();

    // controls
    CheckBox orientSensorCheckbox;
    CheckBox accelerometerSensorCheckbox;
    CheckBox buletoothOnCheckbox;
    CheckBox obd2IsPairedCheckbox;
    CheckBox receivingObd2DataCheckbox;
    CheckBox gpsIsEnabledCheckbox;

    EditText postToUrlEditText;
    EditText manualOdometerEditText;

    EditText receivedNumeric;
    EditText sentNumeric;
    EditText latitudeNumeric;
    EditText longitudeNumeric;
    EditText altitudeNumeric;
    EditText bearingNumeric;

    Button startReadingButton;
    Button stopReadingButton;

    // everything else
    public RecordRepository recordRepository;
    public TripRepository tripRepository;

    private SensorManager sensorManager;
    private PowerManager powerManager;

    boolean mGpsIsEnabled = false;
    boolean mGpsIsStarted = false;
    private LocationManager mLocService;
    private LocationProvider mLocProvider;
    private Location mLastLocation;

    private Sensor orientSensor = null;
    private String mCurrentDir = "";
    private float mCurrentHeading = 0;

    private Sensor accelerometerSensor = null;
    private ConnectivityManager connectivityManager = null;

    private boolean bluetoothIsOn = false;
    private BluetoothAdapter mBtAdapter = null;
    private BluetoothDevice mBtDevice = null;

    private PowerManager.WakeLock wakeLock;

    AsyncObd2DataReader dataReader = null;
    AsyncSendDataToBackend dataTransmiter = null;

    float mLastAccelerationX = 0;
    float mLastAccelerationY = 0;
    float mLastAccelerationZ = 0;
    double mLastAccelerationTotal = 0;

    long tripStartUTCTicks = 0;
    int tripManualStartOdometer = 0;
    double tripEstimatedDistance = 0;

    OBD2Trip currentTrip = null;
    String currentCarVin = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        orientSensorCheckbox = (CheckBox) findViewById(R.id.orientSensorCheckbox);
        accelerometerSensorCheckbox = (CheckBox) findViewById(R.id.accelerometerSensorCheckbox);
        gpsIsEnabledCheckbox = (CheckBox) findViewById(R.id.gpsIsEnabledCheckbox);
        buletoothOnCheckbox = (CheckBox) findViewById(R.id.buletoothOnCheckbox);
        obd2IsPairedCheckbox = (CheckBox) findViewById(R.id.obd2IsPairedCheckbox);
        receivingObd2DataCheckbox = (CheckBox) findViewById(R.id.receivingObd2DataCheckbox);

        postToUrlEditText =  (EditText) findViewById(R.id.postToUrlEditText);
        manualOdometerEditText =  (EditText) findViewById(R.id.manualOdometerEditText);

        receivedNumeric = (EditText) findViewById(R.id.receivedNumeric);
        sentNumeric = (EditText) findViewById(R.id.sentNumeric);
        latitudeNumeric = (EditText) findViewById(R.id.latitudeNumeric);
        longitudeNumeric = (EditText) findViewById(R.id.longitudeNumeric);
        altitudeNumeric = (EditText) findViewById(R.id.altitudeNumeric);
        bearingNumeric = (EditText) findViewById(R.id.bearingNumeric);

        startReadingButton = (Button) findViewById(R.id.startReadingButton);
        startReadingButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

            String odoReading = manualOdometerEditText.getText().toString();

            if(odoReading.length() == 0 || odoReading.compareTo("0") == 0) {
                Toast.makeText(getApplicationContext(), "Odometer Reading Missing !", Toast.LENGTH_LONG).show();
                return;
            }

            currentTrip = new OBD2Trip();

            currentTrip.manualStartOdometer = Integer.parseInt(odoReading);
            currentTrip.startUTCTicks = new Date().getTime();

            dataReader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, odoReading);
            dataReader.readData = true;

            startReadingButton.setVisibility(View.GONE);
            stopReadingButton.setVisibility(View.VISIBLE);
            }
        });

        stopReadingButton = (Button) findViewById(R.id.stopReadingButton);
        stopReadingButton.setVisibility(View.GONE);

        stopReadingButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());

                builder.setTitle("Confirm");
                builder.setMessage("Are you sure?");

                builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();

                        dataReader.cancel(true);
                        dataReader = new AsyncObd2DataReader();
                        dataReader.readData = false;

                        currentTrip.obdVin = currentCarVin;
                        currentTrip.endUTCTicks = new Date().getTime();
                        currentTrip.estimatedDistance = tripEstimatedDistance / 1000;
                        currentTrip.estimatedEndOdometer =  currentTrip.manualStartOdometer + (int)(currentTrip.estimatedDistance);

                        long diffTicks = currentTrip.endUTCTicks - currentTrip.startUTCTicks;
                        currentTrip.totalMinutes = diffTicks / 1000 / 60f;

                        if(currentTrip.totalMinutes > 0) {
                            currentTrip.averageSpeed = currentTrip.estimatedDistance / (currentTrip.totalMinutes / 60.00f);
                        }
                        else
                        {
                            currentTrip.averageSpeed = 0;
                        }

                        if(currentTrip.obdVin.length() > 0) {
                            AsyncSaveTrip saveTripTask = new AsyncSaveTrip();
                            saveTripTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, currentTrip);
                        }
                        else
                        {
                            Toast.makeText(getApplicationContext(), "Trip dropped : NO VIN !", Toast.LENGTH_SHORT).show();
                        }

                        startReadingButton.setVisibility(View.VISIBLE);
                        stopReadingButton.setVisibility(View.GONE);
                    }
                });

                builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        // Do nothing
                        dialog.dismiss();
                    }
                });

                AlertDialog alert = builder.create();
                alert.show();
            }
        });

        AppInit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        releaseWakeLockIfHeld();

        final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        btAdapter.disable();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.debug(TAG, "Entered onStart...");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.debug(TAG, "Pausing..");
        releaseWakeLockIfHeld();
    }

    // custom code for Main Activity

    private void AppInit(){

        // records reporsitory create
        recordRepository = new RecordRepository(getApplicationContext());
        tripRepository = new TripRepository(getApplicationContext());

        // screen won't turn off until wakeLock.release()
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

        if(powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "ObdReader");
        }

        if(wakeLock != null) {
            wakeLock.acquire();
        }

        // enable bluetooth
        bluetoothStart();

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        if(mBtAdapter != null && mBtAdapter.isEnabled()) {

            mBtAdapter = BluetoothAdapter.getDefaultAdapter();
            buletoothOnCheckbox.setChecked(true);

            Set<BluetoothDevice> pairedDevices = getBluetoothPairedDevices();

            if(mBtDevice == null){
                // here automated pairing
            }
        }

        // init gps
        gpsInit();

        // take last location
        if(mLocService != null) {
            Location lastKnownLocation = mLocService.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnownLocation != null) {
                mLastLocation = lastKnownLocation;
            }
        }

        // get Orientation sensor

        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        orientSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

        if(accelerometerSensor != null){

            accelerometerSensorCheckbox.setChecked(true);

            sensorManager.registerListener(new SensorEventListener() {

                @Override
                public void onSensorChanged(SensorEvent event) {

                    mLastAccelerationX = event.values[0];
                    mLastAccelerationY = event.values[1];
                    mLastAccelerationZ = event.values[2];
                    mLastAccelerationTotal = Math.sqrt(mLastAccelerationX * mLastAccelerationX + mLastAccelerationY * mLastAccelerationY + mLastAccelerationZ * mLastAccelerationZ);
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }

            }, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
        }

        if(orientSensor != null){
            orientSensorCheckbox.setChecked(true);

            sensorManager.registerListener(new SensorEventListener() {

                @Override
                public void onSensorChanged(SensorEvent event) {
                    float x = event.values[0];
                    String dir = "";
                    if (x >= 337.5 || x < 22.5) {
                        dir = "N";
                    } else if (x >= 22.5 && x < 67.5) {
                        dir = "NE";
                    } else if (x >= 67.5 && x < 112.5) {
                        dir = "E";
                    } else if (x >= 112.5 && x < 157.5) {
                        dir = "SE";
                    } else if (x >= 157.5 && x < 202.5) {
                        dir = "S";
                    } else if (x >= 202.5 && x < 247.5) {
                        dir = "SW";
                    } else if (x >= 247.5 && x < 292.5) {
                        dir = "W";
                    } else if (x >= 292.5 && x < 337.5) {
                        dir = "NW";
                    }

                    mCurrentDir = dir;
                    mCurrentHeading = x;

                    DecimalFormat df = new DecimalFormat("#");
                    bearingNumeric.setText(df.format(x));
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }

            }, orientSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        // check network connection
        if(haveNetworkConnection()){
            new AsyncCheckForGoogleConnection().execute("");
        }

        // launch background tasks
        dataReader = new AsyncObd2DataReader();
        //dataReader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        String postUrl = postToUrlEditText.getText().toString();
        dataTransmiter = new AsyncSendDataToBackend();
        dataTransmiter.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, postUrl);
    }

    private Set<BluetoothDevice> getBluetoothPairedDevices(){
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                String devName = device.getName();
                if(devName.equals("OBDII")) {
                    mBtDevice = device;
                    obd2IsPairedCheckbox.setChecked(true);
                    break;
                }
            }
        }

        return pairedDevices;
    }

    private void releaseWakeLockIfHeld() {
        if (wakeLock.isHeld())
            wakeLock.release();
    }

    private boolean gpsInit() {

        checkGps();

        if(mGpsIsEnabled == false) {
            return false;
        }

        gpsStart();

        mLocService = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (mLocService != null) {

            LocationListener locationListener = new LocationListener() {

                @Override
                public void onLocationChanged(Location location) {

                    mLastLocation = location;
                    updateGpsLocationUI();
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}

                @Override
                public void onProviderEnabled(String provider) {}

                @Override
                public void onProviderDisabled(String provider) {}
            };

            mLocService.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, Constants.GPS_UPDATE_PERIOD, Constants.GPS_UPDATE_DISTANCE, locationListener);
            mLocService.requestLocationUpdates(LocationManager.GPS_PROVIDER, Constants.GPS_UPDATE_PERIOD, Constants.GPS_UPDATE_DISTANCE, locationListener);

            mLocProvider = mLocService.getProvider(LocationManager.GPS_PROVIDER);

            if (mLocProvider != null) {
                if (mLocService.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void checkGps() {
        LocationManager mLocService = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (mLocService != null) {
            LocationProvider mLocProvider = mLocService.getProvider(LocationManager.GPS_PROVIDER);
            if (mLocProvider == null) {
                mGpsIsEnabled = false;
                gpsIsEnabledCheckbox.setChecked(false);
            }
            else
            {
                boolean isEnabled = mLocService.isProviderEnabled(LocationManager.GPS_PROVIDER);
                if(isEnabled) {
                    mGpsIsEnabled = true;
                    gpsIsEnabledCheckbox.setChecked(true);
                }
                else{
                    mGpsIsEnabled = false;
                    gpsIsEnabledCheckbox.setChecked(false);
                }
            }
        }
    }

    private synchronized void gpsStart() {
        if (!mGpsIsStarted && mLocProvider != null && mLocService != null && mLocService.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mGpsIsStarted = true;
        } else {
            Log.debug(TAG, "NO GPS SUPPORT");
        }
    }

    private synchronized void bluetoothStart(){
        final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

        bluetoothIsOn = btAdapter != null && btAdapter.isEnabled();
        if (!bluetoothIsOn) {
            bluetoothIsOn = btAdapter != null && btAdapter.enable();
        }
    }

    private synchronized void gpsStop() {
        if (mGpsIsStarted) {
            mGpsIsStarted = false;
        }
    }

    private void updateGpsLocationUI(){

        DecimalFormat df = new DecimalFormat("#.######");
        DecimalFormat df2 = new DecimalFormat("#");

        if(mLastLocation != null) {
            latitudeNumeric.setText(df.format(mLastLocation.getLatitude()));
            longitudeNumeric.setText(df.format(mLastLocation.getLongitude()));
            altitudeNumeric.setText(df2.format(mLastLocation.getAltitude()));
        }
    }

    private boolean haveNetworkConnection() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;
        boolean online = false;

        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo[] netInfo = cm.getAllNetworkInfo();
            for (NetworkInfo ni : netInfo) {
                if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                    if (ni.isConnected())
                        haveConnectedWifi = true;
                if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                    if (ni.isConnected())
                        haveConnectedMobile = true;
            }
        }
        catch(Exception err){
            Log.debug(TAG, err.getMessage());
        }

        return (haveConnectedWifi || haveConnectedMobile);
    }

    // async tasks start here
    private class AsyncObd2DataReader extends AsyncTask<String, Integer, Boolean> {

        public boolean readData;

        private Location mLastDistanceLocation = null;

        private Exception exception;

        private double roundToDecimals(double value, int precision) {
            int scale = (int) Math.pow(10, precision);
            return (double) Math.round(value * scale) / scale;
        }

        private double distance_between(Location l1, Location l2)
        {
            double lat1=l1.getLatitude();
            double lon1=l1.getLongitude();
            double lat2=l2.getLatitude();
            double lon2=l2.getLongitude();
            double R = 6371; // km
            double dLat = (lat2-lat1)*Math.PI/180;
            double dLon = (lon2-lon1)*Math.PI/180;
            lat1 = lat1*Math.PI/180;
            lat2 = lat2*Math.PI/180;

            double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                    Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(lat1) * Math.cos(lat2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
            double d = R * c * 1000;

            Log.debug(TAG, "private double distance_between : " +
                    d + " " +
                    l1.getLatitude()+ " " +
                    l1.getLongitude() + " " +
                    l2.getLatitude() + " " +
                    l2.getLongitude()
            );

            return d;
        }

        @Override
        protected Boolean doInBackground(String... params) {

//            DUMMY DATA DEBUG
//            int x = 0;
//            while (!Thread.currentThread().isInterrupted()) {
//                OBD2Record record2 = new OBD2Record();
//                recordRepository.createRecord(record2);
//
//                try {
//                    Thread.sleep(Constants.OBD_UPDATE_PERIOD);
//                } catch (InterruptedException e) {
//                    Log.debug(TAG, "Error: " + e.getMessage());
//                }
//
//                x+=1;
//                publishProgress(x);
//            }
//
//            return true;

            int odoValue = 0;

            try
            {
                odoValue = Integer.parseInt(params[0]);
            }
            catch (Exception e) {
                Log.debug(TAG, "Bad Odometer Value : " + e.getMessage());
            }

            BluetoothSocket socket = null;

            // open bluetooth socket
            try
            {
                socket = BluetoothDeviceConnector.connect(mBtDevice);
            }
            catch (Exception e2) {

                Log.debug(TAG, "There was an error while establishing Bluetooth connection. Stopping app..");
                Log.debug(TAG, e2.getMessage());

                if (socket != null) {
                    try
                    {
                        socket.close();
                    }
                    catch (Exception e) {
                        Log.debug(TAG, e.getMessage());
                        socket = null;
                    }
                }
            }

            // initialize OBD Commands
            try
            {
                if(socket != null) {
                    new EchoOffCommand().run(socket.getInputStream(), socket.getOutputStream());
                    new LineFeedOffCommand().run(socket.getInputStream(), socket.getOutputStream());
                    new TimeoutCommand(125).run(socket.getInputStream(), socket.getOutputStream());
                    new EchoOffCommand().run(socket.getInputStream(), socket.getOutputStream());
                }
            } catch (Exception e) {
                Log.debug(TAG, e.getMessage());
            }

            // read data commands defined
            EngineCoolantTemperatureCommand engineCoolantTempCommand = new EngineCoolantTemperatureCommand();
            RPMCommand engineRpmCommand = new RPMCommand();
            SpeedCommand speedCommand = new SpeedCommand();
            OilTempCommand oilTempCommand = new OilTempCommand();
            ThrottlePositionCommand throtlePositionCommand = new ThrottlePositionCommand();
            LoadCommand engineLoadCommand = new LoadCommand();
            VinCommand vinCommand = new VinCommand();

            // read obd data loop
            while (!Thread.currentThread().isInterrupted() && this.readData)
            {
                if(isCancelled())
                {
                    this.readData = false;
                    break;
                }

                Log.debug(TAG, "START OBD READ CYCLE...");

                try
                {
                    socket = BluetoothDeviceConnector.connect(mBtDevice);
                }
                catch (Exception berr) {
                    Log.debug(TAG, "SOCKET CONNECT ERROR : " + berr.getMessage());
                }

                // take car vin if none present
                if(currentCarVin.length() == 0) {

                    Log.debug(TAG, "GET CAR VIN...");

                    String vinNo = "";

                    try {
                        vinCommand.run(socket.getInputStream(), socket.getOutputStream());
                        vinNo = vinCommand.getResult();

                        currentCarVin = vinNo;

                        Log.debug(TAG, "getResult() : " + vinCommand.getResult());
                        Log.debug(TAG, "getFormattedResult() : " + vinCommand.getFormattedResult());
                        Log.debug(TAG, "getCalculatedResult() : " + vinCommand.getCalculatedResult());

                        socket.close();
                    } catch (Exception e) {
                        // handle errors
                        Log.debug(TAG, "OBD2 VIN Error : " + e.getMessage());
                    }
                }

                try
                {
                    OBD2Record record = new OBD2Record();

                    record.UTCTicks = DateHelper.getUTCTicks(new Date());
                    record.obdVin = currentCarVin;
                    record.manualOdometer = odoValue;

                    Location lastKnownLocation = mLocService.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if(lastKnownLocation != null) {

                        mLastLocation = lastKnownLocation;

                        if(mLastDistanceLocation != null) {

                            double distance = distance_between(lastKnownLocation, mLastDistanceLocation);
                            distance = roundToDecimals(distance, 1);

                            float accuracy = lastKnownLocation.getAccuracy();

                            Log.debug(TAG, "Distance : " + String.valueOf(distance) + " , Accuracy : " + String.valueOf(accuracy));

                            if(distance > accuracy){
                                tripEstimatedDistance += distance;
                                mLastDistanceLocation = lastKnownLocation;
                            }
                        }
                        else
                        {
                            mLastDistanceLocation = lastKnownLocation;
                        }
                    }

                    if(mLastLocation != null) {
                        record.gpsLatitude = mLastLocation.getLatitude();
                        record.gpsLongitude = mLastLocation.getLongitude();
                        record.gpsAltitude = (int) Math.round(mLastLocation.getAltitude());
                        record.gpsAccuracy = (int) Math.round(mLastLocation.getAccuracy());
                        record.gpsBearing = (int) mLastLocation.getBearing();
                        record.gpsSpeed = (int) Math.round(mLastLocation.getSpeed());
                    }
                    else
                    {
                        Log.debug(TAG, "GPS Last Location is null !");
                    }

                    record.orientDir = mCurrentDir;
                    record.accelerationX = mLastAccelerationX;
                    record.accelerationY = mLastAccelerationY;
                    record.accelerationZ = mLastAccelerationZ;
                    record.accelerationTotal = mLastAccelerationTotal;

                    if(socket != null) {
                        try {
                            engineRpmCommand.run(socket.getInputStream(), socket.getOutputStream());
                            record.obdRpm = engineRpmCommand.getRPM();
                        }
                        catch(Exception err){
                            Log.debug(TAG, "OBD2 RPM Error : " + err.getMessage());
                        }

                        try
                        {
                            speedCommand.run(socket.getInputStream(), socket.getOutputStream());
                            record.obdSpeed = speedCommand.getMetricSpeed();
                        }
                        catch(Exception err){
                            Log.debug(TAG, "OBD2 Speed Error : " + err.getMessage());
                        }

//                        try
//                        {
//                            oilTempCommand.run(socket.getInputStream(), socket.getOutputStream());
//                            record.obdOilTemp = (int) Math.round(oilTempCommand.getTemperature());
//                        }
//                        catch(Exception err){
//                            Log.debug(TAG, "OBD2 Oil Temp Error : " + err.getMessage());
//                        }

                        try
                        {
                            engineCoolantTempCommand.run(socket.getInputStream(), socket.getOutputStream());
                            record.obdCoolantTemp = (int) Math.round(engineCoolantTempCommand.getTemperature());
                        }
                        catch(Exception err){
                            Log.debug(TAG, "OBD2 Coolant Temp Error : " + err.getMessage());
                        }

                        try
                        {
                            throtlePositionCommand.run(socket.getInputStream(), socket.getOutputStream());
                            record.obdThrotlePosition = (int) Math.round(throtlePositionCommand.getPercentage());
                        }
                        catch(Exception err){
                            Log.debug(TAG, "OBD2 Throttle Pos Error : " + err.getMessage());
                        }

                        try
                        {
                            engineLoadCommand.run(socket.getInputStream(), socket.getOutputStream());
                            record.obdEngineLoad = (int) Math.round(engineLoadCommand.getPercentage());
                        }
                        catch(Exception err){
                            Log.debug(TAG, "OBD2 Engine Load Error : " + err.getMessage());
                        }

                        Log.debug(TAG, "RPM : " + String.valueOf(record.obdRpm));
                        Log.debug(TAG, "Speed : " +  String.valueOf(record.obdSpeed));
                        Log.debug(TAG, "Oil temp : " +  String.valueOf(record.obdOilTemp));
                        Log.debug(TAG, "Coolant temp : " +  String.valueOf(record.obdCoolantTemp));
                        Log.debug(TAG, "Throtle pos : " + String.valueOf(record.obdThrotlePosition));
                        Log.debug(TAG, "Engine load : " + String.valueOf(record.obdEngineLoad));
                    }
                    else {
                        Log.debug(TAG, "BT socket is null !");
                    }

                    if(socket != null){
                        socket.close();
                    }

                    if(record.obdVin.length() > 0) {
                        recordRepository.createRecord(record);
                    }
                    else
                    {
                        Log.debug(TAG, "Drop record, NO VIN !");
                        publishProgress(-1);
                    }

                    try {
                        Thread.sleep(Constants.OBD_UPDATE_PERIOD);
                    } catch (InterruptedException e) {
                        Log.debug(TAG, "Error Thread Waiting : " +  e.getMessage());
                    }

                    publishProgress(1);

                    Log.debug(TAG, "END READ CYCLE");
                    Log.debug(TAG, "--------------------------------------------------------");
                }
                catch(Exception err){
                    exception = err;
                }
            }

            return true;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {

            super.onProgressUpdate();

            try
            {
                CheckBox receivingObd2DataCheckbox = (CheckBox) findViewById(R.id.receivingObd2DataCheckbox);
                receivingObd2DataCheckbox.setChecked(true);

                int old = Integer.parseInt(receivedNumeric.getText().toString());
                if(values[0] > 0) {
                    receivedNumeric.setText(String.valueOf(old + values[0]));
                }
                else
                {
                    Toast.makeText(getApplicationContext(), "Record dropped : NO VIN !", Toast.LENGTH_SHORT).show();
                }
            }
            catch (Exception e) {
                Log.debug(TAG, "Error: " +  e.getMessage());
            }

            updateGpsLocationUI();
        }

        @Override
        protected void onPostExecute(Boolean result) {

            super.onPostExecute(result);

            try
            {
                CheckBox receivingObd2DataCheckbox = (CheckBox)findViewById(R.id.receivingObd2DataCheckbox);
                receivingObd2DataCheckbox.setChecked(false);
            }
            catch (Exception e) {
                Log.debug(TAG, "Error: " +  e.getMessage());
            }
        }

        @Override
        protected void onCancelled(){
            super.onCancelled();
            Log.debug(TAG,"Cancel asyncTask obd read");
            Toast.makeText(getApplicationContext(), "Read Stopped", Toast.LENGTH_SHORT).show();
        }
    }

    private class AsyncCheckForGoogleConnection extends AsyncTask<String, Void, Boolean> {

        private Exception exception;

        private boolean hostAvailable(String host, int port) {
            try
            {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), 2000);
                return true;
            } catch (IOException e) {
                System.out.println(e);
                this.exception = e;
                return false;
            }
            catch (Exception e) {
                System.out.println(e);
                this.exception = e;
                return false;
            }
        }

        protected Boolean doInBackground(String...param) {
            Boolean online = hostAvailable("www.google.com", 80);
            return online;
        }

        protected void onPostExecute(Boolean online) {
            if(online){
                CheckBox internetCheckbox = (CheckBox)findViewById(R.id.internetConnectedCheckbox);
                internetCheckbox.setChecked(true);
            }
        }
    }

    private class AsyncSendDataToBackend extends AsyncTask<String, Integer, Integer>{

        private Exception exception;

        @Override
        protected Integer doInBackground(String... params) {

            int x = 0;
            while (!Thread.currentThread().isInterrupted()) {

                if(isCancelled())
                {
                    break;
                }

                recordRepository.refreshRepository();
                tripRepository.refreshRepository();

                // get records to be sent
                List<OBD2Record> records = recordRepository.getRecordsToBeSent();

                int size = records.size();

                if(size > 0) {
                    Log.debug(TAG, String.valueOf(size) + " records have to be sent...");
                }

                if(size > 0) {

                    int packages = size / Constants.SEND_MAX_RECORDS_COUNT + 1;

                    for(int i = 0; i < packages; i++) {

                        List<OBD2Record> recordsToSend = new ArrayList<OBD2Record>();

                        int max = (i + 1) * Constants.SEND_MAX_RECORDS_COUNT;
                        if (max > size) {
                            max = size;
                        }

                        // send records
                        Log.debug(TAG, "Trying to send data...");

                        boolean sent = false;
                        try {
                            String url = params[0];
                            URL object = new URL(url);

                            HttpURLConnection conn = (HttpURLConnection) object.openConnection();
                            conn.setDoOutput(true);
                            conn.setDoInput(true);
                            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                            conn.setRequestMethod("POST");

                            Gson gson = new Gson();

                            recordsToSend = records.subList(i * Constants.SEND_MAX_RECORDS_COUNT, max);
                            String json = gson.toJson(recordsToSend);

                            OutputStream os = conn.getOutputStream();
                            os.write(json.getBytes("UTF-8"));
                            os.close();

                            String response = conn.getResponseMessage();
                            int respCode = conn.getResponseCode();

                            if (respCode == 200) {
                                sent = true;
                            } else {
                                sent = false;
                                String respMessage = conn.getResponseMessage();

                                Log.debug(TAG, "Send error : " + respMessage);
                            }
                        } catch (Exception err) {
                            sent = false;
                            Log.debug(TAG, "Send error : " + err.getMessage());
                        }

                        // delete records
//                        if(sent) {
//                            for (int j = 0; j < recordsToSend.size(); j++) {
//                                OBD2Record rec = recordsToSend.get(i);
//                                recordRepository.deleteRecord(rec.id);
//                            }
//
//                            x += recordsToSend.size();
//                            publishProgress(x);
//                        }

                        // update the sent flag and keep in history
                        if (sent) {
                            for (int j = 0; j < recordsToSend.size(); j++) {
                                OBD2Record rec = recordsToSend.get(j);
                                rec.sent = 1;
                                recordRepository.updateRecord(rec);
                            }

                            Log.debug(TAG, "Updated sent records.");

                            x += recordsToSend.size();
                            publishProgress(x);
                        }
                    }
                }

                //send trips also
                List<OBD2Trip> trips = tripRepository.getTripsToBeSent();

                int size2 = trips.size();

                if(size2 > 0) {
                    Log.debug(TAG, String.valueOf(size2) + " trips have to be sent...");
                }

                if(size2 > 0) {
                    boolean sent = false;
                    try {
                        String url = params[0];
                        URL object = new URL(url.replace("postObd", "postTrips"));

                        HttpURLConnection conn = (HttpURLConnection) object.openConnection();
                        conn.setDoOutput(true);
                        conn.setDoInput(true);
                        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                        conn.setRequestMethod("POST");

                        Gson gson = new Gson();
                        String json = gson.toJson(trips);

                        OutputStream os = conn.getOutputStream();
                        os.write(json.getBytes("UTF-8"));
                        os.close();

                        String response = conn.getResponseMessage();
                        if(conn.getResponseCode() == 200) {
                            sent = true;
                        }
                        else
                        {
                            sent = false;
                        }
                    }
                    catch(Exception err){
                        sent = false;
                        Log.debug(TAG, "Send trips error : " + err.getMessage());
                    }

                    if(sent) {
                        for (int i = 0; i < size2; i++) {
                            OBD2Trip trip = trips.get(i);
                            trip.sent = 1;
                            tripRepository.updateTrip(trip);
                        }

                        Log.debug(TAG, "Updated sent trips.");
                    }

                }

                // wait
                try {
                    Thread.sleep(Constants.SEND_DATA_PERIOD);
                } catch (InterruptedException e) {
                    Log.debug(TAG, "Send Thread Error: " + e.getMessage());
                }
            }

            return 0;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {

            super.onProgressUpdate();

            try
            {
                sentNumeric.setText(String.valueOf(values[0]));
            }
            catch (Exception e) {
                Log.debug(TAG, "Error: " +  e.getMessage());
            }

            updateGpsLocationUI();
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
        }
    }

    private class AsyncSaveTrip extends AsyncTask<OBD2Trip, Void, Boolean> {

        private Exception exception;

        @Override
        protected Boolean doInBackground(OBD2Trip...param) {

            try
            {
                OBD2Trip trip = param[0];
                tripRepository.createTrip(trip);
            }
            catch(Exception err){
                exception = err;
                Log.debug(TAG, "Error save trip : " + err.getMessage());
                return false;
            }

            return true;
        }
    }
}
