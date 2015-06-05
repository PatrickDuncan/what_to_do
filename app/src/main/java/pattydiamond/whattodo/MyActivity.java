package pattydiamond.whattodo;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.NotificationCompat;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

public class MyActivity extends AppCompatActivity {

    EditText[] Text = new EditText[7];
    NotificationCompat.Builder mBuilder;
    NotificationManager mNotifyMgr;
    private int mNotificationId = 16, checked = 0, before = -1;
    String content, bullet = Html.fromHtml("&#8226").toString() + " ";
    boolean showNotification;
    ArrayList<Integer> mSelectedItems;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        for (int i=0; i<Text.length; i++) {
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
        if (showNotification){
            updateNotification();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
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
        switch (item.getItemId()) {
            case (R.id.clear): {
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
            }
            case (R.id.pause): {
                showNotification = !showNotification;
                if (showNotification) {
                    updateNotification();
                } else {
                    mNotifyMgr.cancel(mNotificationId);
                }
                savePref("pause", Boolean.toString(showNotification));
                return true;
            }
            case (R.id.swap): {
                int hasTexts = 0;
                String[] s = new String[Text.length];
                mSelectedItems = new ArrayList<>();
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
            }
            case (R.id.remove): {
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
            case (R.id.theme): {
                String[] themes = {"Default", "Blue", "Brown", "Green", "Grey", "Orange", "Pink", "Purple", "Red", "Teal"};
                alert.setTitle("Select a theme")
                        .setItems(themes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                colorCase(which);
                            }
                        });
                alert.create().show();
                return true;
            }
        }
        return false;
    }

    private void colorCase(int which) {
        ActionBar bar = getSupportActionBar();
        assert bar != null;
        savePref("theme", Integer.toString(which));
        switch (which) {
            case (0):
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setStatusBarColor(getResources().getColor(R.color.statusbar));
                }
                bar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.actionbar_background)));
                break;
            case (1):
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setStatusBarColor(getResources().getColor(R.color.blue700));
                }
                bar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.blue500)));
                break;
            case (2):
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setStatusBarColor(getResources().getColor(R.color.brown700));
                }
                bar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.brown500)));
                break;
            case (3):
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setStatusBarColor(getResources().getColor(R.color.green700));
                }
                bar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.green500)));
                break;
            case (4):
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setStatusBarColor(getResources().getColor(R.color.grey700));
                }
                bar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.grey500)));
                break;
            case (5):
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setStatusBarColor(getResources().getColor(R.color.orange700));
                }
                bar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.orange500)));
                break;
            case (6):
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setStatusBarColor(getResources().getColor(R.color.pink700));
                }
                bar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.pink500)));
                break;
            case (7):
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setStatusBarColor(getResources().getColor(R.color.purple700));
                }
                bar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.purple500)));
                break;
            case (8):
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setStatusBarColor(getResources().getColor(R.color.red700));
                }
                bar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.red500)));
                break;
            case (9):
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setStatusBarColor(getResources().getColor(R.color.teal700));
                }
                bar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.teal500)));
                break;
        }
    }

    private void dateDialog(int which) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        mSelectedItems = new ArrayList<>();
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
        for (int i = 0; i < Text.length; i++) {
            Text[i].setText(sharedPreferences.getString("string_text" + Integer.toString(i), ""));
        }
        if (sharedPreferences.contains("theme")) {
            colorCase(Integer.parseInt(sharedPreferences.getString("theme", "")));
        } else {
            colorCase(0);
        }
        showNotification = !sharedPreferences.contains("pause") || Boolean.parseBoolean(sharedPreferences.getString("pause", ""));
    }

    private void savePref(String key, String value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    private void saveData() {
        for(int i=0; i<Text.length; i++) {
            savePref("string_text" + Integer.toString(i), Text[i].getText().toString());
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
                mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(content));
                mBuilder.setContentText(content);
            }
            System.out.println(content);
            mNotifyMgr.notify(mNotificationId, mBuilder.build());
        }
    }

    private void updateOrder() {
        int position = -1;
        boolean noText = true;
        for (int i=0;i<Text.length;i++) {
            if (Text[i].getText().toString().equals("")){
                position = i;
                break;
            }
        }
        //Base cases
        if (position == -1) return;
        for (int i=position+1;i<Text.length;i++) {
            if (!Text[i].getText().toString().equals("")){
                Text[position].setText(Text[i].getText());
                Text[i].setText("");
                noText = false;
                break;
            }
        }
        if (!noText) updateOrder();
    }

    private final TextWatcher textChange = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
        //Updates the notification and saves what's in the textboxes
        public void afterTextChanged(Editable s) {
            System.out.println("The text is " + s.toString());
            updateNotification();
            saveData();
            if (s.toString().equals("")) updateOrder();
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
