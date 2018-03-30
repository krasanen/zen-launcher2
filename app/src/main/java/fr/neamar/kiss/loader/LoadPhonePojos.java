package fi.zmengames.zlauncher.loader;

import android.content.Context;

import java.util.ArrayList;

import fi.zmengames.zlauncher.dataprovider.PhoneProvider;
import fi.zmengames.zlauncher.pojo.PhonePojo;

public class LoadPhonePojos extends LoadPojos<PhonePojo> {

    public LoadPhonePojos(Context context) {
        super(context, PhoneProvider.PHONE_SCHEME);
    }

    @Override
    protected ArrayList<PhonePojo> doInBackground(Void... params) {
        return null;
    }
}
