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
import android.text.Html;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MyActivity extends ActionBarActivity {

    EditText[] Text = new EditText[3];
    NotificationCompat.Builder mBuilder;
    NotificationManager mNotifyMgr;
    int mNotificationId = 001;
    String content, bullet = Html.fromHtml("&#8226").toString() + " ";

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        findMaxLength();
        Text[0] = (EditText) findViewById(R.id.box_1);
        Text[1] = (EditText) findViewById(R.id.box_2);
        Text[2] = (EditText) findViewById(R.id.box_3);
        loadSavedPreferences();
        content = bullet + Text[0].getText().toString();
        mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle("What To Do?")
                .setContentText(Text[0].getText().toString())
                .setOngoing(true)
                .setShowWhen(false)
                .setPriority(2);
        for (int i=1; i<Text.length; i++){
            content = content + "\n" + bullet + Text[i].getText().toString();
        }
        mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(content));
        Intent resultIntent = new Intent(this, MyActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MyActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent
                (0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Text[0].addTextChangedListener(textChange);
        Text[1].addTextChangedListener(textChange);
        Text[2].addTextChangedListener(textChange);
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
    }

    private void findMaxLength() {

    }

    private boolean isTooLarge (TextView text, String newText) {
        float textWidth = text.getPaint().measureText(newText);
        return (textWidth >= text.getMeasuredWidth ());
    }

    private void loadSavedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Text[0].setText(sharedPreferences.getString("string_text1", ""));
        Text[1].setText(sharedPreferences.getString("string_text2", ""));
        Text[2].setText(sharedPreferences.getString("string_text3", ""));
    }

    private void savePreferences(String key, String value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.commit();
    }
    public void saveData() {
        savePreferences("string_text1", Text[0].getText().toString());
        savePreferences("string_text2", Text[1].getText().toString());
        savePreferences("string_text3", Text[2].getText().toString());
    }

    private final TextWatcher textChange = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }


        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        public void afterTextChanged(Editable s) {
            content = bullet + Text[0].getText().toString();
            mBuilder.setContentText(content);
            for (int i=1; i<Text.length; i++){
                content = content + "\n" + bullet + Text[i].getText().toString();
            }
            mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(content));
            mNotifyMgr.notify(mNotificationId, mBuilder.build());
            saveData();
        }
    };
}
