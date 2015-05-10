package pattydiamond.whattodo;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

public class MyActivity extends ActionBarActivity {

    EditText text;
    NotificationCompat.Builder mBuilder;
    NotificationManager mNotifyMgr;
    int mNotificationId = 001;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        text = (EditText) findViewById(R.id.edit_message);
        loadSavedPreferences();
        mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle("What To Do")
                .setContentText(text.getText().toString())
                .setOngoing(true);
        Intent resultIntent = new Intent(this, MyActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MyActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent
                (0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        text.addTextChangedListener(textChange);
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
    }

    private void loadSavedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        text.setText(sharedPreferences.getString("string_text", ""));
    }

    private void savePreferences(String key, String value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.commit();
    }
    public void saveData(){
        savePreferences("string_text", text.getText().toString());
    }
    @Override
    public void onBackPressed() {
        saveData();
        super.onBackPressed();
    }




    private final TextWatcher textChange = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        public void afterTextChanged(Editable s) {
            mBuilder.setContentText(s.toString());
            mNotifyMgr.notify(mNotificationId, mBuilder.build());
        }
    };
}
