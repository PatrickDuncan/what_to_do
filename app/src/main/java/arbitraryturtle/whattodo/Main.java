package arbitraryturtle.whattodo;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.Arrays;


public class Main extends AppCompatActivity {

    private int mNotificationId = 16;
    private int checked = 0;
    private int before = -1;
    private boolean showNotification, clearing, undoing, undoAble, ordering;
    private String content, bullet = Html.fromHtml("&#8226").toString() + " ";

    private EditText[] Text = new EditText[40];
    private NotificationCompat.Builder mBuilder;
    private NotificationManager mNotifyMgr;
    private ArrayList<Integer> mSelectedItems;
    private Menu menu;

    /**
     * Initially invoked method
     *
     * @param savedInstanceState
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        clearing = undoing = undoAble = ordering = false;
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        for (int i = 0; i < Text.length; i++) {
            int editTextId = getResources().getIdentifier("box_" + Integer.toString(i), "id", getPackageName());
            Text[i] = (EditText) findViewById(editTextId);
        }

        Bitmap large = BitmapFactory.decodeResource(getResources(), R.drawable.notification_iconlarge);
        mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle("What to Do?")
                .setContentText(Text[0].getText().toString())
                .setOngoing(true)
                .setShowWhen(false)
                .setPriority(2);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            mBuilder.setLargeIcon(large);
        mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(content));

        Intent resultIntent = new Intent(this, Main.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(Main.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);

        loadSavedPreferences();

        for (EditText aText : Text)
            aText.addTextChangedListener(textChange);
        mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (showNotification) updateNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        // If all the text boxes are empty, then cancel the notification
        if (noTasks()) {
            mNotifyMgr.cancel(mNotificationId);
            savePref("pause", Boolean.toString(showNotification));
        }
    }

    /**
     * Invoked when the options menu is to be created on startup
     *
     * @param menu The menu object
     * @return If it has been created
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Height calculation cannot be in onCreate
        fixHeight();
        getMenuInflater().inflate(R.menu.menu_my, menu);
        this.menu = menu;
        menu.findItem((R.id.pause)).setTitle(showNotification ? R.string.pause : R.string.unpause);
        return true;
    }

    /**
     * Creates the options menu and has the options' implementations
     *
     * @param item the menu item selected by the end user
     * @return if the options menu did its job
     */
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
                    clearing = true;
                    saveUndo();
                    for (EditText aText : Text)
                        aText.setText("");
                    saveData();
                    clearing = false;
                    Text[0].requestFocus();     // Set the focus back to the top of the app
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
                    menu.findItem((R.id.pause)).setTitle(R.string.pause);
                } else {
                    mNotifyMgr.cancel(mNotificationId);
                    menu.findItem((R.id.pause)).setTitle(R.string.unpause);
                }
                savePref("pause", Boolean.toString(showNotification));
                return true;
            }
            case (R.id.swap): {
                int hasTexts = 0, i = 0;
                int[] indexes = new int[Text.length];
                String[] s = new String[Text.length];
                mSelectedItems = new ArrayList<>();
                for (EditText aText : Text) {
                    if (!aText.getText().toString().equals("")) {
                        s[hasTexts] = aText.getText().toString();
                        indexes[hasTexts] = i;
                        hasTexts++;
                    }
                    i++;
                }
                String[] tasks = Arrays.copyOf(s, hasTexts);
                // No need to prompt for selection if there's only 2 tasks to be swapped.
                if (tasks.length == 2) {
                    Editable text0 = Text[indexes[0]].getText();
                    Text[indexes[0]].setText(Text[indexes[1]].getText());
                    Text[indexes[1]].setText(text0);
                }
                // Can't swap 0 or 1 tasks
                else if (tasks.length != 0 && tasks.length != 1) {
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
                                    System.out.println(Text[before].getText() + " " + Text[which].getText());
                                    Editable text0 = Text[before].getText();
                                    Text[before].setText(Text[which].getText());
                                    Text[which].setText(text0);
                                    System.out.println(Text[before].getText() + " " + Text[which].getText());
                                    checked = 0;
                                    before = -1;
                                    //Data is saved in the text changed listener
                                    dialog.cancel();
                                }
                            }
                        });
                    alert.create().show();
                }
                return true;
            }
            case (R.id.theme): {
                String[] themes = {"Default", "Blue", "Brown", "Green", "Grey", "Orange", "Pink", "Purple", "Red", "Teal"};
                alert.setTitle("Select a theme")
                    .setItems(themes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            colorSelect(which);
                        }
                    });
                alert.create().show();
                return true;
            }
            case (R.id.undo): {
                if (undoAble) {
                    loadUndo();
                    saveData();
                    updateNotification();
                }
                return true;
            }
        }
        return false;
    }

    /**
     * @return if there are currently no tasks
     */
    private boolean noTasks() {
        for (EditText aText : Text)
            if (!aText.getText().toString().equals(""))
                return false;
        return true;
    }

    /**
     * Change the colour of the app
     */
    private void colorSelect(int which) {
        ActionBar bar = getSupportActionBar();
        assert bar != null;
        savePref("theme", Integer.toString(which));
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        ActivityManager.TaskDescription taskDesc = null;
        String s = getString(R.string.app_name1);
        switch (which) {
            case (0):
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setStatusBarColor(getResources().getColor(R.color.statusbar));
                    mBuilder.setColor(getResources().getColor(R.color.actionbar_background));
                    taskDesc = new ActivityManager.TaskDescription(s, bm, getResources().getColor(R.color.recentapps));
                }
                bar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.actionbar_background)));
                break;
            case (1):
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setStatusBarColor(getResources().getColor(R.color.blue700));
                    mBuilder.setColor(getResources().getColor(R.color.blue500));
                    taskDesc = new ActivityManager.TaskDescription(s, bm, getResources().getColor(R.color.blue600));
                }
                bar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.blue500)));
                break;
            case (2):
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setStatusBarColor(getResources().getColor(R.color.brown700));
                    mBuilder.setColor(getResources().getColor(R.color.brown500));
                    taskDesc = new ActivityManager.TaskDescription(s, bm, getResources().getColor(R.color.brown600));

                }
                bar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.brown500)));
                break;
            case (3):
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setStatusBarColor(getResources().getColor(R.color.green700));
                    mBuilder.setColor(getResources().getColor(R.color.green500));
                    taskDesc = new ActivityManager.TaskDescription(s, bm, getResources().getColor(R.color.green600));
                }
                bar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.green500)));
                break;
            case (4):
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setStatusBarColor(getResources().getColor(R.color.grey700));
                    mBuilder.setColor(getResources().getColor(R.color.grey500));
                    taskDesc = new ActivityManager.TaskDescription(s, bm, getResources().getColor(R.color.grey600));
                }
                bar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.grey500)));
                break;
            case (5):
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setStatusBarColor(getResources().getColor(R.color.orange700));
                    mBuilder.setColor(getResources().getColor(R.color.orange500));
                    taskDesc = new ActivityManager.TaskDescription(s, bm, getResources().getColor(R.color.orange600));
                }
                bar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.orange500)));
                break;
            case (6):
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setStatusBarColor(getResources().getColor(R.color.pink700));
                    mBuilder.setColor(getResources().getColor(R.color.pink500));
                    taskDesc = new ActivityManager.TaskDescription(s, bm, getResources().getColor(R.color.pink600));
                }
                bar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.pink500)));
                break;
            case (7):
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setStatusBarColor(getResources().getColor(R.color.purple700));
                    mBuilder.setColor(getResources().getColor(R.color.purple500));
                    taskDesc = new ActivityManager.TaskDescription(s, bm, getResources().getColor(R.color.purple600));
                }
                bar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.purple500)));
                break;
            case (8):
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setStatusBarColor(getResources().getColor(R.color.red700));
                    mBuilder.setColor(getResources().getColor(R.color.red500));
                    taskDesc = new ActivityManager.TaskDescription(s, bm, getResources().getColor(R.color.red600));
                }
                bar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.red500)));
                break;
            case (9):
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setStatusBarColor(getResources().getColor(R.color.teal700));
                    mBuilder.setColor(getResources().getColor(R.color.teal500));
                    taskDesc = new ActivityManager.TaskDescription(s, bm, getResources().getColor(R.color.teal600));
                }
                bar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.teal500)));
                break;
        }
        assert taskDesc != null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            this.setTaskDescription(taskDesc);
        if (showNotification && !noTasks())
            mNotifyMgr.notify(mNotificationId, mBuilder.build());
    }

    /**
     * Corrects the size of each text box
     */
    private void fixHeight() {
        int height = matchParent();
        if (isTablet()) height /= 10;
        else height /= 7;
        for (EditText aText : Text)
            aText.setHeight(height);
    }

    /**
     * http://stackoverflow.com/a/24701063/4401223
     * Checks whether the current device is has a tablet screen size
     */
    private boolean isTablet() {
        DisplayMetrics metrics = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        float yInches = metrics.heightPixels / metrics.ydpi;
        float xInches = metrics.widthPixels / metrics.xdpi;
        double diagonalInches = Math.sqrt(xInches * xInches + yInches * yInches);
        return diagonalInches >= 6.5;
    }

    /**
     * Loads the saved content, ie the text content, theme colour and if the notification is paused
     */
    private void loadSavedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        for (int i = 0; i < Text.length; i++) {
            Text[i].setText(sharedPreferences.getString("string_text" + Integer.toString(i), ""));
        }
        if (sharedPreferences.contains("theme")) {
            colorSelect(Integer.parseInt(sharedPreferences.getString("theme", "")));
        } else colorSelect(0);
        showNotification = !sharedPreferences.contains("pause") || Boolean.parseBoolean(sharedPreferences.getString("pause", ""));
    }

    /**
     * Restore the content that they cleared.
     */
    private void loadUndo() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        undoing = true;
        for (int i = 0; i < Text.length; i++)
            Text[i].setText(sharedPreferences.getString("undo" + Integer.toString(i), ""));
        undoing = false;
        undoAble = false;
    }

    /**
     * This gets the int of match_parent in the xml.
     */
    private int matchParent() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        final int actionbar, height, resourceId, result, screenHeight = size.y, statusBarHeight;
        ActionBar bar = getSupportActionBar();
        assert bar != null;
        actionbar = bar.getHeight();
        resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        result = getResources().getDimensionPixelSize(resourceId);
        statusBarHeight = result;
        height = screenHeight - (statusBarHeight + actionbar);
        return height;
    }

    /**
     * Save the current parameters
     */
    private void savePref(String key, String value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    /**
     * Save text contents before removing, so that the user can recover their tasks
     */
    public void saveUndo() {
        for (int i = 0; i < Text.length; i++)
            savePref("undo" + Integer.toString(i), Text[i].getText().toString());
        undoAble = true;
    }

    /**
     * Save the text boxes content
     */
    private void saveData() {
        for (int i = 0; i < Text.length; i++)
            savePref("string_text" + Integer.toString(i), Text[i].getText().toString());
    }

    /**
     * Functions that trigger when a text box changes in any way
     */
    private final TextWatcher textChange = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        /**
         * This method is called to notify you that, within s, the count characters beginning at
         * start have just replaced old text that had length before.
         *
         * @param s what was in the edittext
         * @param start the starting index of the substring within s
         * @param before the length of the string that has been replaced
         * @param count the number of characters that have replaced the old text
         */
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // When you type a single character count-before is 1
            if (count - before == 1 && !clearing && !ordering)
                undoAble = false;
        }

        /**
         * This method is called to notify you that, somewhere within s, the text has been changed
         * Updates the notification and saves what's in the text boxes
         * @param s the edittext that was changed
         */
        public void afterTextChanged(Editable s) {
            if (!undoing) {
                updateNotification();
                saveData();
                if (s.toString().equals("") && !clearing && !ordering) {
                    ordering = true;
                    updateOrder();
                }
            }
        }
    };

    /**
     * Fixes the appearance of a bullet point in the notification
     */
    private String textFix(String s) {
        String fixed = s;
        int index = 0, count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == ' ') {
                count = 0;
                index = i;
            } else
                count++;
            // more than 16 characters might cause a new line glitch
            if (count == 17) {
                fixed = s.substring(0, index + count) + "\n" + s.substring(index + count);
                count = 0;
            }
        }
        return fixed;
    }

    /**
     * Update the notification after a content change
     */
    private void updateNotification() {
        if (showNotification) {
            content = "";
            for (EditText aText : Text) {
                if (!aText.getText().toString().equals("")) {
                    String text = aText.getText().toString();
                    if (aText.length() > 17)
                        content += bullet + textFix(text);
                    else
                        content += bullet + text;
                }
            }
            String[] s = content.split(bullet);
            if (s.length > 1)
                mBuilder.setContentText(s[1]);
            content = "";
            for (int i = 1; i < s.length - 1; i++) {
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
            if (!noTasks())
                mNotifyMgr.notify(mNotificationId, mBuilder.build());
            else {
                // Do not create a notification if there's nothing to show
                mNotifyMgr.cancel(mNotificationId);
                savePref("pause", Boolean.toString(showNotification));
            }
        }
    }

    /**
     * If there's empty text boxes above content, more the content to the top.
     */
    private void updateOrder() {
        int position = -1;
        boolean noText = true;
        for (int i = 0; i < Text.length; i++) {
            if (Text[i].getText().toString().equals("")) {
                position = i;
                break;
            }
        }
        //Base case -> There's content, backtrack
        if (position == -1) {
            ordering = false;
            return;
        }
        for (int i = position + 1; i < Text.length; i++) {
            if (!Text[i].getText().toString().equals("")) {
                Text[position].setText(Text[i].getText());
                Text[i].setText("");
                Text[position].setSelection(Text[position].length());
                noText = false;
                break;
            }
        }
        if (!noText)
            updateOrder();
        ordering = false;
    }
}