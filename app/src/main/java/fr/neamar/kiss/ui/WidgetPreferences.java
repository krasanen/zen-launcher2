package fr.neamar.kiss.ui;


import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import fi.zmengames.zen.LauncherAppWidgetHostView;
import fr.neamar.kiss.BuildConfig;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;

import static fr.neamar.kiss.ui.WidgetLayout.LayoutParams.POSITION_MIDDLE;


/**
 * Created by TBog on 5/10/2018.
 */
public class WidgetPreferences implements Serializable {
    static int MASK_GRAVITY_VERTICAL = Gravity.FILL_VERTICAL | Gravity.CLIP_VERTICAL;
    static int MASK_GRAVITY_HORIZONTAL = Gravity.FILL_HORIZONTAL | Gravity.CLIP_HORIZONTAL;
    private static final String TAG = WidgetPreferences.class.getSimpleName();
    public int position = WidgetLayout.LayoutParams.POSITION_MIDDLE;
    private static final long serialVersionUID = 882755531094416368L;
    public int width = 0;
    public int height = 0;
    public int offsetVertical = 0;
    public int offsetHorizontal = 0;
    public int gravity = Gravity.TOP | Gravity.START;
    public byte[] appWidgetProviderInfo;
    public byte[] appWidgetOptions;


    private boolean isValid() {
        return width > 0 && height > 0;
    }

    public void apply(WidgetLayout.LayoutParams layoutParams) {
        layoutParams.position = this.position;
        layoutParams.width = this.width;
        layoutParams.height = this.height;
        layoutParams.topMargin = this.offsetVertical;
        layoutParams.leftMargin = this.offsetHorizontal;
        layoutParams.gravity = this.gravity;
     }


    public void load(WidgetPreferences wp, byte[] appWidgetProviderInfo, byte[] appWidgetOptions) {
        this.position = wp.position;
        this.width = wp.width;
        this.height = wp.height;
        this.offsetVertical = wp.offsetVertical;
        this.offsetHorizontal = wp.offsetHorizontal;
        this.gravity = wp.gravity;
        this.appWidgetProviderInfo = appWidgetProviderInfo;
        this.appWidgetOptions = appWidgetOptions;
    }

    public void load(WidgetLayout.LayoutParams layoutParams, byte[] appWidgetProviderInfo, byte[] appWidgetOptions) {
        this.position = layoutParams.position;
        this.width = layoutParams.width;
        this.height = layoutParams.height;
        this.offsetVertical = layoutParams.topMargin;
        this.offsetHorizontal = layoutParams.leftMargin;
        this.gravity = layoutParams.gravity;
        this.appWidgetProviderInfo = appWidgetProviderInfo;
        this.appWidgetOptions = appWidgetOptions;
    }


    public void load(LauncherAppWidgetHostView hostView) {
        WidgetLayout.LayoutParams layoutParams = (WidgetLayout.LayoutParams) hostView.getLayoutParams();
        this.position = layoutParams.position;
        this.width = hostView.getMeasuredWidth();
        this.height = hostView.getMeasuredHeight();
        this.offsetVertical = layoutParams.topMargin;
        this.gravity = layoutParams.gravity;
    }


    public void showEditMenu(MainActivity mainActivity, SharedPreferences widgetPrefs, LauncherAppWidgetHostView hostView) {
        if (!isValid())
            load(hostView);
        Menu menu = new Menu(mainActivity, widgetPrefs);
        Point windowSize = new Point();
        mainActivity.getWindowManager()
                .getDefaultDisplay()
                .getSize(windowSize);
        menu.setScreenSize(windowSize);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            menu.setBackgroundDrawable(mainActivity.getDrawable(R.drawable.main_bg_navigation));
        }
        menu.show(mainActivity, this, hostView);
        mainActivity.registerPopup(menu);
    }

    public static String serialize(WidgetPreferences o) {
        if (BuildConfig.DEBUG) Log.i(TAG, "serialize");

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream;
        try {
            objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(o);
            objectOutputStream.close();
        } catch (IOException e) {
            Log.e(TAG, "Serialize WidgetPreferences", e);
        }
        try {
            return byteArrayOutputStream.toString("ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    public static WidgetPreferences unserialize(String data) {
        if (BuildConfig.DEBUG) Log.i(TAG, "unserialize");
        if (data == null || data.isEmpty())
            return null;
        ObjectInputStream objectInputStream;
        try {
            objectInputStream = new ObjectInputStream(new ByteArrayInputStream(data.getBytes(StandardCharsets.ISO_8859_1)));
            WidgetPreferences wp = (WidgetPreferences) objectInputStream.readObject();
            return wp;
        } catch (Exception e) {
            Log.e(TAG, "UnSerialize WidgetPreferences", e);
            return null;
        }
    }


    static class Menu extends ListPopup {
        private final Point mWindowSize = new Point(1, 1);
        private final SharedPreferences prefs;
        private boolean shown;

        public Menu(Context context, SharedPreferences widgetPrefs) {
            super(context);
            prefs = widgetPrefs;
            ScrollView scrollView = new ScrollView(context);

            LayoutInflater.from(context).inflate(R.layout.widget_customize, scrollView);
            setContentView(scrollView);

            setWidth(LinearLayout.LayoutParams.WRAP_CONTENT);
            setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);
        }

        private void updateWidget(){
            SeekBar seek;

            //Width
            seek = contentView.findViewById(R.id.seek_width);
            widgetPreferences.width = seek.getProgress() + info.minWidth;
            //seek.setProgress(info.minWidth);
            //Height
            seek = contentView.findViewById(R.id.seek_height);
            widgetPreferences.height = seek.getProgress() + info.minHeight;
            //seek.setProgress(info.minHeight);
            //Offset top
            seek = contentView.findViewById(R.id.seek_top);
            widgetPreferences.offsetVertical = seek.getProgress();

            seek = contentView.findViewById(R.id.seek_horiz);
            widgetPreferences.offsetHorizontal = seek.getProgress();

            widgetPreferences.gravity = Gravity.TOP | Gravity.START;
            //Position
            //dropDown = contentView.findViewById(R.id.value_pos);
            widgetPreferences.position = POSITION_MIDDLE; //((SpinnerItem) dropDown.getSelectedItem()).value;

            int appWidgetId = hostView.getAppWidgetId();
            prefs.edit().putString(String.valueOf(appWidgetId), serialize(widgetPreferences)).apply();
            mainActivity.refreshWidget(appWidgetId);
        }
        View contentView;
        AppWidgetProviderInfo info;
        WidgetPreferences widgetPreferences;
        MainActivity mainActivity;
        LauncherAppWidgetHostView hostView;
        public void show(MainActivity mainActivityIn, WidgetPreferences widgetPreferencesIn, LauncherAppWidgetHostView hostViewIn) {
            contentView = getContentView();
            info = hostViewIn.getAppWidgetInfo();
            widgetPreferences = widgetPreferencesIn;
            mainActivity = mainActivityIn;
            hostView = hostViewIn;
            contentView.findViewById(R.id.btn_apply).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();

                }
            });
            contentView.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                    updateWidget();
                }
            });

            String label;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                label = hostView.getAppWidgetInfo().label;
            } else {
                label = hostView.getAppWidgetInfo().loadLabel(mainActivity.getPackageManager());
            }
            TextView text = contentView.findViewById(R.id.title);
            text.setText(label);
            if (BuildConfig.DEBUG)
                text.setText(text.getText().toString() + " id:" + hostView.getAppWidgetId());

            SeekBar seek;
            TextViewSync textSync;

            //Width
            text = contentView.findViewById(R.id.value_width);
            seek = contentView.findViewById(R.id.seek_width);
            seek.setMax(mWindowSize.x - info.minWidth);
            seek.setOnSeekBarChangeListener(new SeekBarSync(text, info.minWidth));
            seek.setProgress(widgetPreferences.width - info.minWidth);
            textSync = new TextViewSync(seek, info.minWidth);
            text.addTextChangedListener(textSync);
            text.setOnFocusChangeListener(textSync);
            text.setText(String.valueOf(widgetPreferences.width));

            //Height
            text = contentView.findViewById(R.id.value_height);
            seek = contentView.findViewById(R.id.seek_height);
            seek.setMax(mWindowSize.y - info.minHeight);
            seek.setOnSeekBarChangeListener(new SeekBarSync(text, info.minHeight));
            seek.setProgress(widgetPreferences.height - info.minHeight);
            textSync = new TextViewSync(seek, info.minHeight);
            text.addTextChangedListener(textSync);
            text.setOnFocusChangeListener(textSync);
            text.setText(String.valueOf(widgetPreferences.height));

            //Offset top
            text = contentView.findViewById(R.id.value_top);
            seek = contentView.findViewById(R.id.seek_top);
            seek.setMax(mWindowSize.y - info.minHeight);
            seek.setOnSeekBarChangeListener(new SeekBarSync(text, 0));
            seek.setProgress(widgetPreferences.offsetVertical);
            textSync = new TextViewSync(seek, 0);
            text.addTextChangedListener(textSync);
            text.setOnFocusChangeListener(textSync);
            text.setText(String.valueOf(widgetPreferences.offsetVertical));

            //Offset horiz
            text = contentView.findViewById(R.id.value_horiz);
            seek = contentView.findViewById(R.id.seek_horiz);
            seek.setMax(mWindowSize.y - info.minHeight);
            seek.setOnSeekBarChangeListener(new SeekBarSync(text, 0));
            seek.setProgress(widgetPreferences.offsetHorizontal);
            textSync = new TextViewSync(seek, 0);
            text.addTextChangedListener(textSync);
            text.setOnFocusChangeListener(textSync);
            text.setText(String.valueOf(widgetPreferences.offsetHorizontal));

            //Position
           /* dropDown = contentView.findViewById(R.id.value_pos);
            dropDownItems = new ArrayList<>(3);
            dropDownItems.add(new SpinnerItem(WidgetLayout.LayoutParams.POSITION_LEFT, "Left"));
            dropDownItems.add(new SpinnerItem(WidgetLayout.LayoutParams.POSITION_MIDDLE, "Middle"));
            dropDownItems.add(new SpinnerItem(WidgetLayout.LayoutParams.POSITION_RIGHT, "Right"));
            dropDown.setAdapter(new ArrayAdapter<>(mainActivity, android.R.layout.simple_spinner_dropdown_item, dropDownItems));
            dropDown.setSelection(dropDownItems.indexOf(new SpinnerItem(widgetPreferences.position)));
*/
            setFocusable(true);
            showAtLocation(mainActivity.emptyListView, Gravity.CENTER, 0, 0);
            shown = true;
        }

        void setScreenSize(Point size) {
            mWindowSize.x = size.x;
            mWindowSize.y = size.y;
        }

        class SeekBarSync implements SeekBar.OnSeekBarChangeListener {
            final TextView mTextView;
            final int mMin;

            SeekBarSync(TextView text, int min) {
                mTextView = text;
                mMin = min;
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int textProgress;
                try {
                    textProgress = Integer.parseInt(mTextView.getText().toString()) - mMin;
                } catch (NumberFormatException e) {
                    textProgress = 0;
                }
                if (textProgress != progress)
                    mTextView.setText(String.valueOf(progress + mMin));
                if (shown) updateWidget();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        }

        class TextViewSync implements TextWatcher, View.OnFocusChangeListener {
            final SeekBar mSeekBar;
            final int mMin;

            TextViewSync(SeekBar seekBar, int min) {
                mSeekBar = seekBar;
                mMin = min;
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                int progress;
                try {
                    progress = Integer.parseInt(s.toString()) - mMin;
                } catch (NumberFormatException e) {
                    progress = mSeekBar.getProgress();
                }

                    mSeekBar.setProgress(progress);
                if (shown) updateWidget();
            }

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    return;
                }
                TextView textView = (TextView) v;
                int textProgress;
                try {
                    textProgress = Integer.parseInt(textView.getText().toString()) - mMin;
                } catch (NumberFormatException e) {
                    textProgress = -1;
                }
                if (textProgress != mSeekBar.getProgress())
                    textView.setText(String.valueOf(mSeekBar.getProgress() + mMin));
            }
        }

        static class SpinnerItem {
            final int value;
            final String name;

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                SpinnerItem that = (SpinnerItem) o;
                return value == that.value;
            }

            @Override
            public int hashCode() {
                return value;
            }

            SpinnerItem(int value) {
                this.value = value;
                this.name = null;
            }

            SpinnerItem(int value, String name) {
                this.value = value;
                this.name = name;
            }

            @Override
            public String toString() {
                return name;
            }
        }
    }
}
