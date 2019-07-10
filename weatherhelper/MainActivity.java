package com.example.weatherhelper;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {


    /**
     * TODO: Make app run in foreground so i can have it send a timed notification about weather
     * https://stackoverflow.com/questions/7279916/how-to-set-time-for-alarm-for-next-day-in-android-app
     */
    TextView textView_temp, textView_city, textView_date, textView_description;
    TimePicker timePicker;
    Calendar calendar;

    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    LocationCallback locationCallback;


    AlarmManager alarmManager;
    PendingIntent pendingIntent;
    BroadcastReceiver broadcastReceiver;

    String umbrellaMessage = "wut happened?";

    int requestCode = 0;
    int timePickerHour;
    int timePickerMinute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        calendar = Calendar.getInstance();

        textView_temp = findViewById(R.id.text1);
        textView_city = findViewById(R.id.text2);
        textView_date = findViewById(R.id.text3);
        textView_description = findViewById(R.id.text4);
        timePicker = findViewById(R.id.time_picker);

        //createNotificationChannel();
        TimePickerManager();

        calendar.set(Calendar.HOUR_OF_DAY, timePickerHour);
        calendar.set(Calendar.MINUTE, timePickerMinute);

        RegisterAlarmBroadcast();

        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
        //alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 60 * 1, pendingIntent);

        if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},0);
        }
        else {
            locationRequestBuilder();
            locationCallBackBuilder();

            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
                return;
            }
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        }

        //Toast.makeText(MainActivity.this, "coords are " + string, Toast.LENGTH_LONG).show();
        //find_weather();
    }

    private void locationRequestBuilder() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for(Location location:locationResult.getLocations()) {
                    //string = location.getLatitude();
                    //Toast.makeText(MainActivity.this, location.getLatitude() + "-----" + location.getLongitude(), Toast.LENGTH_SHORT).show();
                    find_weather(location.getLatitude(), location.getLongitude());
                }
            }
        };
    }

    private void locationCallBackBuilder() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000);
        locationRequest.setSmallestDisplacement(10);
    }

    public void find_weather(double lat, double lon) {
        //String example = "https://samples.openweathermap.org/data/2.5/weather?q=London,uk&appid=b6907d289e10d714a6e88b30761fae22";
        // MY KEY = fbf309724d68dd14e47ce6f19760c91f
        //String url = "https://api.openweathermap.org/data/2.5/weather?q=";
        //String city_name = "New York";
        //String key = ",us&appid=fbf309724d68dd14e47ce6f19760c91f";
        String key = "&appid=fbf309724d68dd14e47ce6f19760c91f";

        String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + lat +  "&" + "lon=" + lon + "" +  key;
        //String URL = url + city_name + key;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONObject main_object = response.getJSONObject("main");
                    JSONArray array = response.getJSONArray("weather");
                    JSONObject object = array.getJSONObject(0);
                    String temp = String.valueOf(main_object.getDouble("temp"));
                    String description = object.getString("description");
                    String city = response.getString("name");

                    textView_temp.setText(temp);
                    textView_city.setText(city);
                    textView_description.setText(description);

                    if(description.toLowerCase().contains("rain"))
                        umbrellaMessage = "Your gonna need an umbrella";
                    else
                        umbrellaMessage = "Your not gonna need an umbrella";

                    Toast.makeText(MainActivity.this, umbrellaMessage, Toast.LENGTH_LONG).show();

                    Calendar calendar = Calendar.getInstance();
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEEE-MM-dd");
                    String formatted_date = simpleDateFormat.format(calendar.getTime());

                    textView_date.setText(formatted_date);


                    // Kelvin to Fahrenheit
                    double kelvin = Double.parseDouble(temp);
                    double fahrenheit = (kelvin - 273.15) * 9/5 + 32;
                    textView_temp.setText(String.valueOf(fahrenheit));

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(jsonObjectRequest);
    }

    private void RegisterAlarmBroadcast() {
        broadcastReceiver = new BroadcastReceiver() {
          @Override
          public void onReceive(Context context, Intent intent) {
              sendNotification();
              Toast.makeText(context, "IN ALARM" , Toast.LENGTH_LONG).show();
              alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
              //alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 5000, pendingIntent);
          }
        };
        registerReceiver(broadcastReceiver, new IntentFilter("sample"));
        pendingIntent = PendingIntent.getBroadcast(this, requestCode++, new Intent("sample"), 0);
        alarmManager = (AlarmManager)(this.getSystemService(Context.ALARM_SERVICE));
    }

    public void UnregisterAlarmBroadcast() {
        alarmManager.cancel(pendingIntent);
        getBaseContext().unregisterReceiver(broadcastReceiver);
    }

    private void sendNotification() {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_rain_cloud)
                        .setContentTitle("Umbrella Notifier")
                        .setContentText(umbrellaMessage)
                        .setChannelId("id");

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);

        // Add as notification
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(1, builder.build());
    }

    private void TimePickerManager() {
        timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                timePickerHour = hourOfDay;
                timePickerMinute = minute;
            }
        });
    }
}
