package pattydiamond.whattodo;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.DialogInterface;
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
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;

public class MyActivity extends ActionBarActivity {

    EditText[] Text = new EditText[7];
    NotificationCompat.Builder mBuilder;
    NotificationManager mNotifyMgr;
    int mNotificationId = 001;
    String content, bullet = Html.fromHtml("&#8226").toString() + " ";
    boolean showNotification = true;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
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
        mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(content));
        Intent resultIntent = new Intent(this, MyActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MyActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent
                (0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        for (int i=0; i<Text.length; i++) {
            Text[i].addTextChangedListener(textChange);
        }
        mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        updateNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.statusbar));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setCancelable(true);
        if (item.getItemId() == R.id.clear) {
            alert.setMessage("Are you sure?");
            alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    for (int i=0; i<Text.length; i++) {
                        Text[i].setText("");
                    }
                    saveData();
                    mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(""));
                    mBuilder.setContentText(content);
                    if (showNotification) {
                        mNotifyMgr.notify(mNotificationId, mBuilder.build());
                    }
                    dialog.cancel();
                }
            });
            alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });
            alert.create().show();
            return true;
        }
        if (item.getItemId() == R.id.pause) {
            showNotification = !showNotification;
            if (showNotification) {
                updateNotification();
            } else {
                mNotifyMgr.cancel(mNotificationId);
            }
            return true;
        }
        return false;
    }

    private void loadSavedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        for (int i=0; i<Text.length; i++) {
            Text[i].setText(sharedPreferences.getString("string_text"+Integer.toString(i),""));
        }
    }

    private void saveText(String key, String value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public void saveData() {
        for(int i=0; i<Text.length; i++) {
            saveText("string_text" + Integer.toString(i), Text[i].getText().toString());
        }
    }

    public void updateNotification() {
        if (showNotification) {
            content="";
            for (int i=0; i<Text.length; i++) {
                if (!Text[i].getText().toString().equals("")) {
                    content += bullet + Text[i].getText().toString();
                }
            }
            String[] s = content.split(bullet);
            if (s.length > 1) {
                mBuilder.setContentText(s[1].toString());
            }
            content = "";
            for (int i=1; i<s.length-1; i++){
                s[i] = bullet + s[i] + "\n";
                content += s[i];
            }
            if (s.length > 2) {
                content += bullet + s[s.length - 1];
                mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(content));
            } else {
                content += s[s.length - 1];
                mBuilder.setContentText(content);
            }
            mNotifyMgr.notify(mNotificationId, mBuilder.build());
        }
    }

    private final TextWatcher textChange = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
        //Updates the notification and saves what's in the textboxs
        public void afterTextChanged(Editable s) {
            updateNotification();
            saveData();
        }
    };
}
