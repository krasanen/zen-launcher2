package fr.neamar.kiss.pojo;


import androidx.annotation.DrawableRes;

public class SettingsPojo extends Pojo {
    public final String settingName;
    public final String packageName;
    public final @DrawableRes
    int icon;

    public SettingsPojo(String id, String settingName, @DrawableRes int icon) {
    	super(id);

        this.settingName = settingName;
        this.packageName = "";
        this.icon = icon;
    }

    public SettingsPojo(String id, String settingName, String packageName, @DrawableRes int icon) {
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
            SettingsPojo settingsPojo = (SettingsPojo) object;
            if (this.id.hashCode() == (settingsPojo.id.hashCode()))  {
                result = true;
            }
        }
        return result;
    }
}
