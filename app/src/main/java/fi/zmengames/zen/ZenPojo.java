package fi.zmengames.zen;


import androidx.annotation.DrawableRes;

import fr.neamar.kiss.pojo.Pojo;

public class ZenPojo extends Pojo {
    public final String settingName;
    public final String packageName;
    public final @DrawableRes
    int icon;

    public ZenPojo(String id, String settingName, @DrawableRes int icon) {
        super(id);

        this.settingName = settingName;
        this.packageName = "";
        this.icon = icon;
    }

    public ZenPojo(String id, String settingName, String packageName, @DrawableRes int icon) {
        super(id);

        this.settingName = settingName;
        this.packageName = packageName;
        this.icon = icon;
    }
    @Override
    @SuppressWarnings("EqualsHashCode")
    public boolean equals (Object object) {
        boolean result = false;
        if (object == null || object.getClass() != getClass()) {
        } else {
            ZenPojo settingsPojo = (ZenPojo) object;
            if (this.id.hashCode() == (settingsPojo.id.hashCode()))  {
                result = true;
            }
        }
        return result;
    }
}
