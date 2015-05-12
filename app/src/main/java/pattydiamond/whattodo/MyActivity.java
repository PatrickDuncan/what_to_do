package pattydiamond.whattodo;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
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

import android.view.Menu;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

public class MyActivity extends ActionBarActivity {

    EditText[] Text = new EditText[7];
    NotificationCompat.Builder mBuilder;
    NotificationManager mNotifyMgr;
    int mNotificationId = 001;
    String content="", bullet = Html.fromHtml("&#8226").toString() + " ";

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        for(int i=0; i<Text.length; i++) {
            int editTextId = getResources().getIdentifier("box_"+Integer.toString(i), "id", getPackageName());
            Text[i] = (EditText)findViewById(editTextId);
        }
        loadSavedPreferences();
        mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle("What To Do?")
                .setContentText(Text[0].getText().toString())
                .setOngoing(true)
                .setShowWhen(false)
                .setPriority(2);
        for (int i=0; i<Text.length; i++) {
            if (!Text[i].getText().toString().equals("")) {
                content += bullet + Text[i].getText().toString() + "\n";
            }
        }
        //removes the extra new line.
        if (content.lastIndexOf("\n") == content.length()-1){
            content = content.substring(0,content.length()-1);
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
        for(int i=0; i<7; i++) {
            Text[i].addTextChangedListener(textChange);
        }
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.statusbar_text));
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_my, menu);
        return true;
    }

    private void loadSavedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        for (int i=0; i<Text.length; i++) {
            Text[i].setText(sharedPreferences.getString("string_text"+Integer.toString(i),""));
        }
    }

    private void savePreferences(String key, String value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.commit();
    }
    public void saveData() {
        for(int i=0; i<Text.length; i++) {
            savePreferences("string_text" + Integer.toString(i), Text[i].getText().toString());
        }
    }

    private final TextWatcher textChange = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
        //Updates the notification and saves what's in the textboxs
        public void afterTextChanged(Editable s) {
            content = "";
            mBuilder.setContentText(Text[0].getText().toString());
            for (int i=0; i<Text.length; i++) {
                if (!Text[i].getText().toString().equals("")) {
                    content += bullet + Text[i].getText().toString() + "\n";
                }
            }
            if (content.lastIndexOf("\n") == content.length()-1){
                content = content.substring(0,content.length()-1);
            }
            mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(content));
            mNotifyMgr.notify(mNotificationId, mBuilder.build());
            saveData();
        }
    };
}
