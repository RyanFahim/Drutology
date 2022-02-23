package com.example.downloadspeedtester;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import static android.telephony.TelephonyManager.NETWORK_TYPE_EHRPD;
import static android.telephony.TelephonyManager.NETWORK_TYPE_EVDO_B;
import static android.telephony.TelephonyManager.NETWORK_TYPE_HSPAP;
import static android.telephony.TelephonyManager.NETWORK_TYPE_IDEN;
import static android.telephony.TelephonyManager.NETWORK_TYPE_LTE;

public class MainActivity2 extends AppCompatActivity implements LocationListener, View.OnClickListener {

    TextView textView_location;
    TextView network_info;
    LocationManager locationManager;
    private final OkHttpClient client = new OkHttpClient();
    private final String TAG = this.getClass().getSimpleName();
    private long startTime;
    private long endTime;
    private long fileSize;
    private double kilobytePerSec;
    DatabaseReference databaseReference;
    TextView downSpeed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        network_info = findViewById(R.id.network_info);
        downSpeed = findViewById(R.id.downSpeed);
        Button checkBtn = findViewById(R.id.checkBtn);
        databaseReference = FirebaseDatabase.getInstance().getReference("Ping");


        checkBtn.setOnClickListener(this);
        textView_location = findViewById(R.id.text_location);
        // button_location = findViewById(R.id.button_location);
        //Runtime permissions
        if (ContextCompat.checkSelfPermission(MainActivity2.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity2.this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, 100);
        }
    }




    private void getLocation() {

        try {
            locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
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
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5, MainActivity2.this);

        }catch (Exception e){
            e.printStackTrace();
        }

    }
    @Override
    public void onLocationChanged(Location location) {
        Toast.makeText(this, ""+location.getLatitude()+","+location.getLongitude(), Toast.LENGTH_SHORT).show();
        try {
            Geocoder geocoder = new Geocoder(MainActivity2.this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(),location.getLongitude(),1);
            String address = addresses.get(0).getAddressLine(0);

            textView_location.setText(address);

        }catch (Exception e){
            e.printStackTrace();
        }

    }
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onClick(View v) {
        insertData();
        getLocation();

        if(v.getId() == R.id.checkBtn )
        {
            //TextView imeiNum = findViewById(R.id.imeiNum);

            String type = isConnected(this);
            network_info.setText(type);

            // Finding phone's IMEI number
            //TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
//            imeiNum.setText("IMEI is " + telephonyManager);
            //Toast.makeText(this,"IMEI is: "+ telephonyManager, Toast.LENGTH_LONG).show();

            double result = downloadSpeed();


            downSpeed.setText(result/1024 + " mbps") ;
            Log.d("speed", String.valueOf(result));


            //measuring upload and download speed
            ///*TextView upSpeed = findViewById(R.id.upSpeed);
            TextView downSpeed = findViewById(R.id.downSpeed);

            ConnectivityManager manager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = manager.getActiveNetworkInfo();
            //NetworkCapabilities nc = manager.getNetworkCapabilities(manager.getActiveNetwork());
            NetworkCapabilities nc = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                nc = manager.getNetworkCapabilities(manager.getActiveNetwork());
            }

            float downloadSpeed = nc.getLinkDownstreamBandwidthKbps();
            float uploadSpeed = nc.getLinkUpstreamBandwidthKbps();

           /* upSpeed.setText("Upload: " + uploadSpeed/1024 + " mbps");
            downSpeed.setText("Download " + downloadSpeed/1024 + " mbps");*/






        }


    }
    private void insertData(){
        String speed = downSpeed.getText().toString();
        String location = textView_location.getText().toString();
        String network = network_info.getText().toString();
        Speed speed1 = new Speed(network,speed, location);


        databaseReference.push().setValue(speed1);
        Toast.makeText(this, "Data Inserted", Toast.LENGTH_SHORT).show();
    }

    public double downloadSpeed(){

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://square.github.io/okhttp/recipes/")
                .build();
        startTime = System.currentTimeMillis();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.isSuccessful()){
//                        ResponseBody responseBody = response.body();
//                        Log.d(TAG, "onResponse: "+ responseBody.string());

                    if(!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                    Headers responseHeaders = response.headers();
                    for (int i = 0, size = responseHeaders.size(); i < size; i++) {
                        Log.d(TAG, responseHeaders.name(i) + ": " + responseHeaders.value(i));
                    }

                    InputStream input = response.body().byteStream();

                    try {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];

                        while (input.read(buffer) != -1) {
                            bos.write(buffer);
                        }
                        byte[] docBuffer = bos.toByteArray();
                        fileSize = bos.size();

                    } finally {
                        input.close();
                    }

                    endTime = System.currentTimeMillis();

                    // calculate how long it took by subtracting endtime from starttime

                    double timeTakenMills = Math.floor(endTime - startTime);  // time taken in milliseconds
                    double timeTakenInSecs = timeTakenMills / 1000;  // divide by 1000 to get time in seconds
                    kilobytePerSec = (int) Math.round(1024 / timeTakenInSecs);
                    double speed = Math.round(fileSize / timeTakenMills);

//                        Log.d(TAG, "Time taken in secs: " + timeTakenInSecs);
//                        Log.d(TAG, "Kb per sec: " + kilobytePerSec);
//                        Log.d(TAG, "Download Speed: " + speed);
                        Log.d(TAG, "File size in kb: " + fileSize);

                    System.out.println("kb/s : " +kilobytePerSec);

//                    TextView upSpeed = findViewById(R.id.upSpeed);
//                    upSpeed.setText("speed"+ speed);


                }


            }
        });
        return kilobytePerSec;


    }


    public static String isConnected(Context context){
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();

        if ((info != null && info.isConnected())) {
            return checkNetSpeed(info.getType(), info.getSubtype());
        } else
            return "No NetWork Access";
    }

    public static String checkNetSpeed(int type, int subType){

        if( type == ConnectivityManager.TYPE_WIFI )
        {
            return "Connected to WIFI";
        }
        else if (type == ConnectivityManager.TYPE_MOBILE)
        {
            switch (subType) {
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                    return "NETWORK TYPE 1xRTT"; //- Speed: ~50 - 100 Kbps";
                case TelephonyManager.NETWORK_TYPE_CDMA:
                    return "NETWORK TYPE CDMA (3G)";// Speed: ~14-64 Kbps";
                case TelephonyManager.NETWORK_TYPE_EDGE:
                    return "NETWORK TYPE EDGE (2.75G)";// Speed": 100-120 Kbps";
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                    return "NETWORK TYPE EVDO_0";// Speed: ~400-1000 Kbps";
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                    return "NETWORK TYPE EVDO_A";// Speed: ~600-1400 Kbps";
                case TelephonyManager.NETWORK_TYPE_GPRS:
                    return "NETWORK TYPE GPRS (2.5G)";// Speed: ~100 Kbps";
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                    return "NETWORK TYPE HSDPA (4G)";// Speed: 2-14 Mbps";
                case TelephonyManager.NETWORK_TYPE_HSPA:
                    return "NETWORK TYPE HSPA (4G)";// Speed: 0.7-1.7 Mbps";
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                    return "NETWORK TYPE HSUPA (3G)";// Speed: 1-23 Mbps";
                case TelephonyManager.NETWORK_TYPE_UMTS:
                    return "NETWORK TYPE UMTS (3G)";// Speed: 0.4-7 Mbps";

                // API level 7 not supported this type
                case NETWORK_TYPE_EHRPD:
                    return "NETWORK TYPE EHRPD";// Speed: ~1-2 Mbps";
                case NETWORK_TYPE_EVDO_B:
                    return "NETWORK_TYPE_EVDO_B";// Speed: ~5 Mbps";
                case NETWORK_TYPE_HSPAP:
                    return "NETWORK TYPE HSPA+ (4G)";// Speed: 10-20 Mbps";
                case NETWORK_TYPE_IDEN:
                    return "NETWORK TYPE IDEN";// Speed: ~25 Kbps";
                case NETWORK_TYPE_LTE:
                    return "NETWORK TYPE LTE (4G)";// Speed: 10+ Mbps";

                // Unknown type
                case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                    return "NETWORK TYPE UNKNOWN";
                default:
                    return "";
            }
        } else {
            return "";
        }
    }
}
