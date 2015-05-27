package pattydiamond.whattodo;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.NotificationCompat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.NumberPicker;
import android.widget.TimePicker;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

public class MyActivity extends AppCompatActivity {

    EditText[] Text = new EditText[7];
    NotificationCompat.Builder mBuilder;
    NotificationManager mNotifyMgr;
    int mNotificationId = 16, checked = 0, before = -1;
    String content, bullet = Html.fromHtml("&#8226").toString() + " ";
    boolean showNotification = true;
    ArrayList mSelectedItems;
    private AlarmManager alarmMgr;
    private PendingIntent alarmIntent;

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
        for (EditText aText : Text) {
            aText.addTextChangedListener(textChange);
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
            alert.setMessage("You will be removing all tasks.");
            alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    for (EditText aText : Text) {
                        aText.setText("");
                    }
                    saveData();
                    mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(""));
                    mBuilder.setContentText("");
                    if (showNotification) {
                        mNotifyMgr.notify(mNotificationId, mBuilder.build());
                    }
                    dialog.cancel();
                }
            });
            alert.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });
            alert.create().show();
            return true;
        } else if (item.getItemId() == R.id.pause) {
            showNotification = !showNotification;
            if (showNotification) {
                updateNotification();
            } else {
                mNotifyMgr.cancel(mNotificationId);
            }
            return true;
        } else if (item.getItemId() == R.id.swap){
            int hasTexts= 0;
            String[] s = new String[Text.length];
            mSelectedItems = new ArrayList();
            for (EditText aText : Text) {
                if (!aText.getText().toString().equals("")) {
                    s[hasTexts] = aText.getText().toString();
                    hasTexts++;
                }
            }
            String[] tasks = Arrays.copyOf(s, hasTexts);
            alert.setTitle("Swap which?")
                .setMultiChoiceItems(tasks, null, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        if (isChecked) {
                            mSelectedItems.add(which);
                            if (before == -1) before = which;
                            checked++;
                        } else if (mSelectedItems.contains(which)) {
                            mSelectedItems.remove(Integer.valueOf(which));
                            checked--;
                            before = -1;
                        }
                        if (checked == 2) {
                            Editable text0 = Text[before].getText();
                            Text[before].setText(Text[which].getText());
                            Text[which].setText(text0);
                            checked = 0;
                            before = -1;
                            //Data is saved in the text changed listener
                            dialog.cancel();
                        }
                    }
                });
            alert.create().show();
            return true;
        } else if (item.getItemId() == R.id.remove) {
            int hasTexts = 0;
            String[] s = new String[Text.length];
            for (EditText aText : Text) {
                if (!aText.getText().toString().equals("")) {
                    s[hasTexts] = aText.getText().toString();
                    hasTexts++;
                }
            }
            String[] tasks = Arrays.copyOf(s, hasTexts);
            alert.setTitle("Select which task.")
                .setItems(tasks, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dateDialog(which);
                }
            });
            if (showNotification) {
                updateNotification();
            }
            alert.create().show();
            return true;
        }
        return false;
    }

    private void dateDialog(int which) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        mSelectedItems = new ArrayList();
        String[] items = {"Time", "Date"};
        alert.setTitle("Select time and/or date.")
            .setMultiChoiceItems(items, null, new DialogInterface.OnMultiChoiceClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    if (isChecked && which==0) {
                        mSelectedItems.add(which);
                        DialogFragment newFragment = new TimePickerFragment();
                        newFragment.show(getSupportFragmentManager(), "timePicker");
                    } else if (isChecked && which==1) {
                        mSelectedItems.add(which);
                        DialogFragment newFragment = new DatePickerFragment();
                        newFragment.show(getSupportFragmentManager(), "datePicker");
                    } else if (mSelectedItems.contains(which)) {
                        mSelectedItems.remove(Integer.valueOf(which));
                    }
                }
            })
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {

                }
            })
            .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {

                }
            });
        alert.create().show();
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
        editor.apply();
    }

    private void saveData() {
        for(int i=0; i<Text.length; i++) {
            saveText("string_text" + Integer.toString(i), Text[i].getText().toString());
        }
    }

    private void updateNotification() {
        if (showNotification) {
            content="";
            for (EditText aText : Text) {
                if (!aText.getText().toString().equals("")) {
                    content += bullet + aText.getText().toString();
                }
            }
            String[] s = content.split(bullet);
            if (s.length > 1) {
                mBuilder.setContentText(s[1]);
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

    public static class DatePickerFragment extends DialogFragment
            implements DatePickerDialog.OnDateSetListener {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {

        }
    }

    public static class TimePickerFragment extends DialogFragment
            implements TimePickerDialog.OnTimeSetListener {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current time as the default values for the picker
            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);
            return new TimePickerDialog(getActivity(), this, hour, minute,
                    DateFormat.is24HourFormat(getActivity()));
        }

        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

        }
    }
}
