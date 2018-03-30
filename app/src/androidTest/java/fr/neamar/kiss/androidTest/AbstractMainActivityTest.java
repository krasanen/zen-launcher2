package fi.zmengames.zlauncher.androidTest;

import android.os.Build;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.test.ActivityInstrumentationTestCase2;
import android.view.WindowManager;

import org.junit.Before;

import fi.zmengames.zlauncher.KissApplication;
import fi.zmengames.zlauncher.MainActivity;
import fi.zmengames.zlauncher.R;

abstract class AbstractMainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {

    AbstractMainActivityTest() {
        super(MainActivity.class);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getInstrumentation().getUiAutomation().executeShellCommand(
                    "pm grant " + getActivity().getPackageName()
                            + " android.permission.READ_CONTACTS");
        }

        getActivity();

        // Initialize to default preferences
        KissApplication.getApplication(getActivity()).getDataHandler().clearHistory();
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().clear().apply();
        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, true);

        // Remove lock screen
        Runnable wakeUpDevice = new Runnable() {
            public void run() {
                getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        };
        getActivity().runOnUiThread(wakeUpDevice);
    }
}
