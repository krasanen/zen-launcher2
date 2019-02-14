package fi.zmengames.zen;

import android.os.Bundle;
import android.view.Menu;

import androidx.fragment.app.FragmentActivity;
import fr.neamar.kiss.R;

public class AppGridActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.appgripscreen);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }
}
