package fr.neamar.kiss.pojo;

import android.graphics.drawable.Drawable;

public class SettingsPojo extends Pojo {
    public String settingName;
    public String packageName = "";
    public Drawable icon;

    @Override
    public boolean equals (Object object) {
        boolean result = false;
        if (object == null || object.getClass() != getClass()) {
            result = false;
        } else {
            SettingsPojo settingsPojo = (SettingsPojo) object;
            if (this.id.equals(settingsPojo.id))  {
                result = true;
            }
        }
        return result;
    }
}

