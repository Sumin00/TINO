package org.androidtown.tino;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.kwabenaberko.openweathermaplib.constants.Lang;
import com.kwabenaberko.openweathermaplib.constants.Units;
import com.kwabenaberko.openweathermaplib.implementation.OpenWeatherMapHelper;
import com.kwabenaberko.openweathermaplib.implementation.callbacks.CurrentWeatherCallback;
import com.kwabenaberko.openweathermaplib.models.currentweather.CurrentWeather;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;
    long mNow;
    Date mDate;
    SimpleDateFormat mFormat2 = new SimpleDateFormat("yyyy 년 MM 월 dd 일");
    String end;
    TextView textDate;
    TextView textnowtime;
    TextView textTime;
    TextView Remaintime;
    TextView textView0;
    ProgressBar bar;
    ProgressHandler handler;
    boolean isRunning = false;
    Button btnNew;
    Button btnExist;


    public static final String TAG = "GPSListener";
    TextView weatherText;
    Button btn;
    double latitude, longitude;
    ImageView weatherIcon;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        weatherText = findViewById(R.id.weatherText);
        weatherIcon = findViewById(R.id.weatherIcon);
        final LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    0);
        } else {
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            longitude = location.getLongitude();
            latitude = location.getLatitude();

            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    1000,
                    1,
                    gpsLocationListener);
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    1000,
                    1,
                    gpsLocationListener);
        }

        //Instantiate Class With Your ApiKey As The Parameter
        OpenWeatherMapHelper helper = new OpenWeatherMapHelper("9d0ecaac6ad18c1a3b072f7c87ee845c");

        //Set Units
        helper.setUnits(Units.IMPERIAL);

        //Set lang
        helper.setLang(Lang.ENGLISH);

        helper.getCurrentWeatherByGeoCoordinates(latitude, longitude, new CurrentWeatherCallback() {
            @Override
            public void onSuccess(CurrentWeather currentWeather) {
                String description = currentWeather.getWeather().get(0).getDescription();
                int temperature = (int)((currentWeather.getMain().getTempMax() - 32.0) / 1.8);
                Log.d(TAG, "Coordinates: " + currentWeather.getCoord().getLat() + ", "+currentWeather.getCoord().getLon() +"\n"
                        +"Weather Description: " + currentWeather.getWeather().get(0).getDescription() + "\n"
                        +"Temperature: " + ((currentWeather.getMain().getTempMax() - 32.0) / 1.8) +"\n"
                        +"City, Country: " + currentWeather.getName() + ", " + currentWeather.getSys().getCountry()
                );
                setting(description, temperature);
            }

            @Override
            public void onFailure(Throwable throwable) {
                Log.v(TAG, throwable.getMessage());
            }
        });
        textDate = (TextView)findViewById(R.id.textDate);
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        String getNow = mFormat2.format(date);
        textDate.setText(getNow);

        textTime = (TextView)findViewById(R.id.textTime);
        Remaintime = (TextView)findViewById(R.id.textRemaintime);
        String destHour=null;
        String destMin=null;

        SQLiteDatabase db;
        String sql;
        final BmDB helper2 = new BmDB(this);
        db = helper2.getReadableDatabase();
        sql = "Select hour, min from bookmark;";

        Cursor cursor = db.rawQuery(sql,null);
        final int last = cursor.getCount();
        Log.d("last", String.format("%s",last));
        try{
            if(cursor != null || last>0) {
                for (int i = 0; i < last; i++) {
                    cursor.moveToNext();
                    destHour = cursor.getString(cursor.getColumnIndex("hour"));
                    Log.d("hour", destHour);
                    destMin = cursor.getString(cursor.getColumnIndex("min"));
                }
            }
            if(last == 0){
                Remaintime.setText("");
                textTime.setText("아직 스케쥴이 없네!");
            }
        }finally{
            db.close();
            cursor.close();
        }
        end = destHour + ":" + destMin + ":" + "00";

        textView0 = (TextView)findViewById(R.id.textView0);
        bar = (ProgressBar)findViewById(R.id.bar);
        bar.setScaleY(8f);

        handler = new ProgressHandler();

        btnNew = (Button)findViewById(R.id.btnNew);
        btnExist = (Button)findViewById(R.id.btnExist);

        btnNew.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(MainActivity.this, AddScheduleActivity.class);
                startActivity(intent);
            }
        });

        btnExist.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(MainActivity.this, BookmarkActivity.class);
                startActivity(intent);

            }
        });

        bottomNavigationView = findViewById(R.id.bottomNavigation);
        final LinearLayout linear1 = findViewById(R.id.linear1);
        final LinearLayout linear2 = findViewById(R.id.linear2);

        bottomNavigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

                        switch(menuItem.getItemId()) {
                            case R.id.action_home:
                                linear1.setVisibility(View.INVISIBLE);
                                linear2.setVisibility(View.VISIBLE);
                                break;
                            case R.id.action_schedule:
                                linear2.setVisibility(View.INVISIBLE);
                                linear1.setVisibility(View.VISIBLE);
                                replaceFragment(ScheduleFragment.newInstance());
                                break;
                            case R.id.action_check:
                                linear2.setVisibility(View.INVISIBLE);
                                linear1.setVisibility(View.VISIBLE);
                                replaceFragment(CheckFragment.newInstance());
                                break;
                            case R.id.action_more:
                                linear2.setVisibility(View.INVISIBLE);
                                linear1.setVisibility(View.VISIBLE);
                                replaceFragment(MoreFragment.newInstance());
                                break;
                        }
                        return true;
                    }
                }
        );
    }
    public void update(){



    }

    @Override
    protected void onStart(){
        super.onStart();
        bar.setProgress(0);
        Thread thread1 = new Thread(new Runnable() {
            public void run() {
                try{
                    for(int i=0; i<100 && isRunning; i++){
                        Thread.sleep(100);
                        Message msg = handler.obtainMessage();
                        handler.sendMessage(msg);
                    }}
                catch(Exception ex){
                    Log.e("MainActivity","Exception in processing message.",ex);
                }}
        });
        isRunning = true;
        thread1.start();

        Thread thread2 = new Thread(new Runnable() {
            public void run() {
                SimpleDateFormat mFormat  = new SimpleDateFormat("HH:mm:ss");
                String start = mFormat.format(new Date()) ;
                end = mFormat.format(end);
                Date startDate = null;
                Date endDate =null;

                try {
                    startDate = mFormat.parse(start);
                    endDate = mFormat.parse(end);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                try{
                    long diff = endDate.getTime() - startDate.getTime();

                    if(diff >0){
                        int seconds = (int) (diff / 1000) % 60 ;
                        int minutes = (int) ((diff / (1000*60)) % 60);
                        int hours   = (int) ((diff / (1000*60*60)) % 24);
                        textTime.setText(String.format("%d 시간 %d 분 %d초야",hours,minutes,seconds));
                    }
                    if(diff < 0) {
                        diff += 24 * 60 * 60 * 1000;
                        int seconds = (int) (diff / 1000) % 60;
                        int minutes = (int) ((diff / (1000 * 60)) % 60);
                        int hours = (int) ((diff / (1000 * 60 * 60)) % 24);
                        textTime.setText(String.format("%d 시간 %d 분 %d초야.", hours, minutes, seconds));
                    }
                    while(true){
                        Thread.sleep(100);
                        Message msg = handler.obtainMessage();
                        handler.sendMessage(msg);
                    }}
                catch(Exception ex){
                    Log.e("MainActivity","Exception in processing message.",ex);
                }}
        });
        thread2.start();
    }

    @Override
    protected void onStop(){
        super.onStop();
        isRunning = false;
    }
    public class ProgressHandler extends Handler {
        public void handleMessage(Message msg){
            bar.incrementProgressBy(1);

            if(bar.getProgress()==bar.getMax()){
                textView0.setText("약속 시간이 되었어요 !");
            }else{
                textView0.setText(String.format("%s", bar.getProgress()));
            }
        }
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.linear1, fragment).commit();
    }

    final LocationListener gpsLocationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            longitude = location.getLongitude();
            latitude = location.getLatitude();
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }
    };


    public void setting(String description, int temperature) {
        weatherText.setText(String.format("%s℃", temperature));

        if(description.equalsIgnoreCase("clear sky")) {
            weatherIcon.setImageResource(R.drawable.clearsky);
        }

        else if(description.equalsIgnoreCase("few clouds")) {
            weatherIcon.setImageResource(R.drawable.fewclouds);
        }

        else if(description.equalsIgnoreCase("scattered clouds")) {
            weatherIcon.setImageResource(R.drawable.scatteredclouds);
        }

        else if(description.equalsIgnoreCase("broken clouds")) {
            weatherIcon.setImageResource(R.drawable.brokenclouds);
        }

        else if(description.equalsIgnoreCase("shower rain")) {
            weatherIcon.setImageResource(R.drawable.rain);
        }

        else if(description.equalsIgnoreCase("rain")) {
            weatherIcon.setImageResource(R.drawable.rain);
        }

        else if(description.equalsIgnoreCase("thunderstorm")) {
            weatherIcon.setImageResource(R.drawable.thunderstorm);
        }

        else if(description.equalsIgnoreCase("Snow")) {
            weatherIcon.setImageResource(R.drawable.snow);
        }

        else if(description.equalsIgnoreCase("mist")) {
            weatherIcon.setImageResource(R.drawable.mist);
        }

        else if(description.equalsIgnoreCase("overcast clouds")) {
            weatherIcon.setImageResource(R.drawable.brokenclouds);
        }

        else if(description.equalsIgnoreCase("light rain")) {
            weatherIcon.setImageResource(R.drawable.lightrain);
        }

        else if(description.equalsIgnoreCase("light intensity drizzle rain")) {
            weatherIcon.setImageResource(R.drawable.rain);
        }

        else if(description.equalsIgnoreCase("proximity thunderstorm")) {
            weatherIcon.setImageResource(R.drawable.prothunderstorm);
        }

        else if(description.equalsIgnoreCase("haze")) {
            weatherIcon.setImageResource(R.drawable.mist);
        }
    }
}