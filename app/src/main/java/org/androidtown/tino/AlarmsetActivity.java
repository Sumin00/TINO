package org.androidtown.tino;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.androidtown.tino.MultiAlarm.database.DataBaseManager;
import org.androidtown.tino.MultiAlarm.model.Alarm;
import org.androidtown.tino.MultiAlarm.receiver.AlarmReceiver;
import org.androidtown.tino.MultiAlarm.ultil.Constants;
import org.androidtown.tino.MultiAlarm.view.AlarmAdapter;

import java.util.ArrayList;
import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;


public class AlarmsetActivity extends AppCompatActivity implements AlarmAdapter.CallBack {

    @BindView(R.id.alarmView)
    RecyclerView recyclerView;
    // this to manage data base
    private DataBaseManager dataBaseManager;
    // this to manage Alarm adapter like ArrayList
    private AlarmAdapter alarmAdapter;
    Button checkOk;
    TextView checkInputHour, checkInputMinute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarmset);
        checkInputHour = findViewById(R.id.checkInputHour);
        checkInputMinute = findViewById(R.id.checkInputMinute);
        ButterKnife.bind(this);
        initView();
        SharedPreferences sf = getSharedPreferences("sTime",MODE_PRIVATE);
        int hour=sf.getInt("hour",0);
        int min=sf.getInt("min",0);
        Log.d("time3","hour:"+hour+"min:"+min);


        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        int lazy = pref.getInt("lazy", 0);
        Log.d("Lazy","lax"+lazy);
        selectAll(hour,min,lazy);

        SQLiteDatabase db2;
        String sql2;

        final BmDB bookmark = new BmDB(this);
        db2 = bookmark.getReadableDatabase();
        sql2 = "select d_hour, d_min from bookmark;";

        Cursor cursor2 = db2.rawQuery(sql2, null);
        final int count2 = cursor2.getCount();
        try {
            if (cursor2!=null){
                for (int i = 0; i < count2; i++) {
                    cursor2.moveToNext();
                    String d_hour = cursor2.getString(cursor2.getColumnIndex("d_hour"));
                    String d_min = cursor2.getString(cursor2.getColumnIndex("d_min"));
                    checkInputHour.setText(d_hour);
                    checkInputMinute.setText(d_min);
                }
            }
        } finally {
            db2.close();
            cursor2.close();
        }

        checkOk = findViewById(R.id.checkOk);
        checkOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AlarmsetActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    // TODO: this initialize view for activity
    private void initView() {
        // set layout for recycle view
        //hasFixedSize true if adapter changes cannot affect the size of the RecyclerView
        recyclerView.setHasFixedSize(true);
        // this layout can be vertical or horizontal by change the second param
        // of LinearLayoutManager, and display up to down by set the third param false
        LinearLayoutManager layoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);

        recyclerView.setLayoutManager(layoutManager);
        importData();
        // set adapter for recycle view
        recyclerView.setAdapter(alarmAdapter);
    }

    public void AddAlarm(int h,int m,String t){
        Intent intent = new Intent(getApplicationContext(), AddAlarmActivity.class);
        intent.putExtra("Hour",h);
        intent.putExtra("Min",m);
        intent.putExtra("Task",t);
        startActivityForResult(intent, Constants.REQUEST_ADD);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //TODO: Receive return data form Add or Edit activity to process add or edit alarm
        Alarm alarm;
        // check if request code and result code the same sent and return result
        if (requestCode == Constants.REQUEST_ADD && resultCode == RESULT_OK) {
            // get data and set a new alarm by function setAlarm
            alarm = (Alarm) data.getSerializableExtra("Alarm");
            // this check if the setting time already exist
            boolean containAlarm = checkAlarm(alarm);

            if (!containAlarm) {
                // add alarm to adapter
                alarmAdapter.add(alarm);
                // refresh adapter
                alarmAdapter.notifyDataSetChanged();
                // add it to database
                dataBaseManager.insert(alarm);
                // set new PendingIntent
                setAlarm(alarm, 0);
            }


        } else if (requestCode == Constants.REQUEST_EDIT && resultCode == RESULT_OK) {
            // get alarm object from AddAlarmActivity
            alarm = (Alarm) data.getSerializableExtra("Alarm");
            // this check if the setting time already exist
            boolean containAlarm = checkAlarm(alarm);

            if (!containAlarm) {
                // get alarm's position
                int position = data.getExtras().getInt("position");
                // update alarm at position
                alarmAdapter.updateAlarm(alarm, position);
                // this help refresh display
                alarmAdapter.notifyDataSetChanged();
                // update alarm to database
                dataBaseManager.update(alarm);
                // if alarm.getOnOff ==1 set alarm else not
                if (alarm.getOnOff() == 1) {
                    // get data and set a new alarm by function setAlarm with flag update current because
                    // this PendingIntent has already existed
                    setAlarm(alarm, PendingIntent.FLAG_UPDATE_CURRENT);
                }
            }

        }
    }




    @Override
    public void onMenuAction(Alarm alarm, MenuItem item, int position) {
        // TODO: this function is a function of callBack interface which was created in alarm adapter
        //TODO: process edit or delete based on user option
        switch (item.getItemId()) {

            case R.id.delete:
                // when user click edit remove alarm
                alarmAdapter.removeAlarm(position);
                // refresh
                alarmAdapter.notifyDataSetChanged();
                // get alarm id to delete alarm in database
                int alarmId = (int) alarm.getId();
                // delete alarm from database
                dataBaseManager.delete(alarmId);
                // cancel pendingIntent
                deleteCancel(alarm);
                break;
        }

    }


    @Override
    public void startAlarm(Alarm alarm) {
        //TODO: Xử lý truyền thông tin giờ hẹn cho AlarmReceiver
        // when toggle button click on set alarm on
        alarm.setOnOff(1);
        // update database
        dataBaseManager.update(alarm);
        // set PendingIntent for this alarm
        setAlarm(alarm, 0);

    }


    @Override
    public void cancelAlarm(Alarm timeItem) {
        //TODO: Gửi thông tin giờ hẹn cần hủy sang cho AlarmReceiver
        // when user click cancel toggle button
        // set alarm off
        timeItem.setOnOff(0);
        // update database
        dataBaseManager.update(timeItem);
        // cancel this Alarm PendingIntent
        deleteCancel(timeItem);
        // if alarm is triggered and ringing, send this alarm detail to AlarmReceiver
        // then AlarmReceiver send detail to service to stop music
        sendIntent(timeItem, Constants.OFF_INTENT);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    // TODO: this check if Alarm have already existed
    private boolean checkAlarm(Alarm alarm) {
        boolean contain = false;
        for (Alarm alar : alarmAdapter.getmAlarms()) {
            if (alar.getHour_x() == alarm.getHour_x() && alar.getMinute_x() == alarm.getMinute_x())
                contain = true;
        }
        if (contain) {
            Toast.makeText(this, "You have already set this Alarm", Toast.LENGTH_SHORT).show();
        }
        return contain;
    }

    // TODO: import data from dataBase and create AlarmAdapter
    private void importData() {
        // if alarmAdapter null it's means data have not imported, yet or database is empty
        if (alarmAdapter == null) {
            // initialize database manager
            dataBaseManager = new DataBaseManager(this);
            // get Alarm ArrayList from database
            ArrayList<Alarm> arrayList = dataBaseManager.getAlarmList();
            // create Alarm adapter to display detail through RecyclerView
            alarmAdapter = new AlarmAdapter(arrayList, this);

        }
    }

    // TODO: this sends intent to AlarmReceiver
    private void sendIntent(Alarm alarm, String intentType) {
        // intent1 to send to AlarmReceiver
        Intent intent1 = new Intent(AlarmsetActivity.this, AlarmReceiver.class);
        // put intent type Constants.ADD_INTENT or Constants.OFF_INTENT
        intent1.putExtra("intentType", intentType);
        // put alarm'id to compare with pendingIntent'id in AlarmService
        intent1.putExtra("AlarmId", (int) alarm.getId());
        intent1.putExtra("task", alarm.getAlarm_Name());
        // this sent broadCast right a way
        sendBroadcast(intent1);
    }

    // TODO: this sets pendingIntent for alarm
    private void setAlarm(Alarm alarm, int flags) {
        // this set alarm based on TimePicker so we need to set Calendar like the
        // trigger time
        // get instant of Calendar
        Calendar myCalendar = Calendar.getInstance();
        Calendar calendar = (Calendar) myCalendar.clone();
        // set current hour for calendar
        calendar.set(Calendar.HOUR_OF_DAY, alarm.getHour_x());
        // set current minute
        calendar.set(Calendar.MINUTE, alarm.getMinute_x());
        // set current second for calendar
        calendar.set(Calendar.SECOND, 0);
        // plus one day if the time set less than the the Calendar current time
        if (calendar.compareTo(myCalendar) <= 0) {
            calendar.add(Calendar.DATE, 1);
        }
        // get id of alarm and set for PendingIntent to multiply multiple PendingIntent for cancel
        // time, this also put into PendingIntent to compare with the cancel Alarm's id=
        int alarmId = (int) alarm.getId();
        // make intent to broadCast
        Intent intent = new Intent(AlarmsetActivity.this, AlarmReceiver.class);
        // put intent type to check which intent trigger add or cancel
        intent.putExtra("intentType", Constants.ADD_INTENT);
        // put id to intent
        intent.putExtra("PendingId", alarmId);
        // this pendingIntent include alarm id  to manage
        PendingIntent alarmIntent = PendingIntent.getBroadcast(AlarmsetActivity.this, alarmId,
                intent, flags);
        // create alarm manager ALARM_SERVICE
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        // Set alarm, at the trigger time "calandar.getTimeInMillis" this pendingIntent will be
        // sent to AlarmReceiver and then sent to alarm service to play music
        // this "AlarmManager.INTERVAL_DAY" mean this will set one new alarm at the trigger time
        // setInExactRepeating this may set alarm again and again also this may be not
        // trigger at the right time( at the first second start) but this will save the battery.
        // "AlarmManager.RTC_WAKEUP" allow this app wake device from idle time and the time
        // based on device time

        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, alarmIntent);

    }

    // TODO:  this cancel pendingIntent of the alarm
    private void deleteCancel(Alarm alarm) {
        // if user click delete or cancel alarm the pendingIntent also to be canceled by AlarmManager
        // this PendingIntent is canceled based on alarm's ID was set for it, the pendingIntent is
        // going to be canceled must be same with the one was made based on it'id and intent also
        // where the context is.
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        // get alarm id
        int alarmId = (int) alarm.getId();
        // create intent
        Intent intent = new Intent(AlarmsetActivity.this, AlarmReceiver.class);
        // this retrieve the pendingIntent was set
        PendingIntent alarmIntent = PendingIntent.getBroadcast(AlarmsetActivity.this, alarmId, intent, 0);
        // cancel this pendingIntent
        alarmManager.cancel(alarmIntent);
    }

    @Override
    protected void onStart() {
        //reset all alarms
        super.onStart();
        alarmAdapter.reset();
    }

    public int[] compute_time(int hour,int min, int time){
        int[] result_time={0,0};

        if(min>=time){
            result_time[0]=hour;
            result_time[1]=min-time;
        }
        else{
            if(hour>0){
                result_time[0]=hour-1;
                result_time[1]=min+60-time;
            }
            else{
                result_time[0]=hour+23;
                result_time[1]=min+60-time;
            }
        }

        return result_time;
    }

    // 모든 Data 읽어서 알람 설정
    public void selectAll(int hour, int min,int lazy) {

        SQLiteDatabase db;
        String sql;
        TinoDB helper =new TinoDB(this);
        db = helper.getReadableDatabase();
        sql = "Select * from tino where time>=1 and do =1 ORDER BY id DESC;";
        Cursor cursor = db.rawQuery(sql, null);
        int count = cursor.getCount();
        Log.d("test", "count: " +count);
        int r_h = hour;
        int r_m = min;
        int re_time[];

        re_time=compute_time(hour,min,lazy);
        AddAlarm(re_time[0],re_time[1],"출발");

        try {
            if (cursor != null) {
                for (int i = 0; i < count; i++) {
                    cursor.moveToNext();
                    String task = cursor.getString(cursor.getColumnIndex("name"));
                    int time = cursor.getInt(cursor.getColumnIndex("time"));
                    re_time=compute_time(re_time[0],re_time[1],time);
                    r_h=re_time[0];
                    r_m=re_time[1];
                    AddAlarm(r_h,r_m,task);

                    Log.d("sls", "name: " + task + ", time: " + time);
                }
            }
            re_time=compute_time(r_h,r_m,lazy);
            r_h=re_time[0];
            r_m=re_time[1];
            AddAlarm(r_h,r_m,"기상");
        } finally {
            db.close();
            cursor.close();
        }

//
//        Intent intent = new Intent(getApplicationContext(),MainActivity.class);
//        startActivity(intent);

    }


}