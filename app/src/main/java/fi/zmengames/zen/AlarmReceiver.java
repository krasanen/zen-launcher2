package fi.zmengames.zen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent k2) {
        // TODO Auto-generated method stub
        Toast.makeText(context, "Alarm received!", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(context, AlarmActivity.class);
        intent.putExtras(k2);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);

    }
}
