package fr.neamar.kiss.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.EditText;

import fr.neamar.kiss.BuildConfig;

public class SearchEditText extends EditText {
    private static final String TAG = SearchEditText.class.getSimpleName();
    private OnEditorActionListener mEditorListener;

    public SearchEditText(Context context) {
        super(context);
    }

    public SearchEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SearchEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setOnEditorActionListener(OnEditorActionListener listener) {
        if (BuildConfig.DEBUG) Log.d(TAG, "setOnEditorActionListener");
        mEditorListener = listener;
        super.setOnEditorActionListener(listener);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (BuildConfig.DEBUG) Log.d(TAG, "onKeyPreIme");
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP)
            if (mEditorListener != null && mEditorListener.onEditorAction(this, android.R.id.closeButton, event))
                return true;
        return super.onKeyPreIme(keyCode, event);
    }
}
