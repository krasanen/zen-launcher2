package fi.zmengames.zlauncher.loader;

import android.content.Context;
import android.graphics.BitmapFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import fi.zmengames.zlauncher.KissApplication;
import fi.zmengames.zlauncher.TagsHandler;
import fi.zmengames.zlauncher.db.DBHelper;
import fi.zmengames.zlauncher.db.ShortcutRecord;
import fi.zmengames.zlauncher.pojo.ShortcutsPojo;

public class LoadShortcutsPojos extends LoadPojos<ShortcutsPojo> {

    private final TagsHandler tagsHandler;

    public LoadShortcutsPojos(Context context) {
        super(context, ShortcutsPojo.SCHEME);
        tagsHandler = KissApplication.getApplication(context).getDataHandler().getTagsHandler();
    }

    @Override
    protected ArrayList<ShortcutsPojo> doInBackground(Void... arg0) {
        ArrayList<ShortcutsPojo> pojos = new ArrayList<>();

        if(context.get() == null) {
            return pojos;
        }
        List<ShortcutRecord> records = DBHelper.getShortcuts(context.get());
        for (ShortcutRecord shortcutRecord : records) {
            ShortcutsPojo pojo = createPojo(shortcutRecord.name);

            pojo.packageName = shortcutRecord.packageName;
            pojo.resourceName = shortcutRecord.iconResource;
            pojo.intentUri = shortcutRecord.intentUri;
            if (shortcutRecord.icon_blob != null) {
                pojo.icon = BitmapFactory.decodeByteArray(shortcutRecord.icon_blob, 0, shortcutRecord.icon_blob.length);
            }

            pojo.setTags(tagsHandler.getTags(pojo.id));
            pojos.add(pojo);
        }

        return pojos;
    }

    private ShortcutsPojo createPojo(String name) {
        ShortcutsPojo pojo = new ShortcutsPojo();

        pojo.id = ShortcutsPojo.SCHEME + name.toLowerCase(Locale.ROOT);
        pojo.setName(name);

        return pojo;
    }

}
