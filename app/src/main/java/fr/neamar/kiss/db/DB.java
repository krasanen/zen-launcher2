package fr.neamar.kiss.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import fr.neamar.kiss.BuildConfig;

class DB extends SQLiteOpenHelper {

    private final static String DB_NAME = "kiss.s3db";
    private final static int DB_VERSION = 8;
    private static final String TAG = DB.class.getSimpleName();
    DB(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE history ( _id INTEGER PRIMARY KEY AUTOINCREMENT, query TEXT, record TEXT NOT NULL)");
        database.execSQL("CREATE TABLE shortcuts ( _id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, package TEXT,"
                + "icon TEXT, intent_uri TEXT NOT NULL, icon_blob BLOB)");
        createTags(database);
        createBadges(database);
        addTimeStamps(database);
        addAppsTable(database);
    }

    private void createTags(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE tags ( _id INTEGER PRIMARY KEY AUTOINCREMENT, tag TEXT NOT NULL, record TEXT NOT NULL)");
        database.execSQL("CREATE INDEX idx_tags_record ON tags(record);");
    }
    private void createBadges(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE badges ( _id INTEGER PRIMARY KEY AUTOINCREMENT, package INT NOT NULL, badge_count INT NOT NULL)");
    }

    private void addTimeStamps(SQLiteDatabase database) {
        database.execSQL("ALTER TABLE history ADD COLUMN timeStamp INTEGER DEFAULT 0  NOT NULL");
    }

    private void addAppsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE custom_apps ( _id INTEGER PRIMARY KEY AUTOINCREMENT, custom_flags INTEGER DEFAULT 0, component_name TEXT NOT NULL UNIQUE, name TEXT NOT NULL DEFAULT '' )");
        db.execSQL("CREATE INDEX index_component ON custom_apps(component_name);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        if(BuildConfig.DEBUG) Log.w("onUpgrade", "Updating database from version " + oldVersion + " to version " + newVersion);
        // See
        // http://www.drdobbs.com/database/using-sqlite-on-android/232900584
        if (oldVersion < newVersion) {
            switch (oldVersion) {
                case 1:
                case 2:
                case 3:
                    database.execSQL("CREATE TABLE shortcuts ( _id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, package TEXT,"
                            + "icon TEXT, intent_uri TEXT NOT NULL, icon_blob BLOB)");
                    // fall through
                case 4:
                    createTags(database);
                    // fall through
                case 5:
                    createBadges(database);
                    // fall through
                case 6:
                    // DB version FU fix
                    if(!isFieldExist(database, "timeStamp")) {
                        if (BuildConfig.DEBUG) Log.d(TAG,"adding timeStamps");
                        addTimeStamps(database);
                    } else {
                        if (BuildConfig.DEBUG) Log.d(TAG,"timeStamps already exist");
                    }
                    // fall through
                case 7:
                    addAppsTable(database);
                    // fall through
                default:
                    break;
            }
        }
    }

    // This method will check if column exists in your table
    public boolean isFieldExist(SQLiteDatabase db, String fieldName)
    {
        boolean isExist = false;
        Cursor res = db.rawQuery("PRAGMA table_info(history)",null);
        res.moveToFirst();
        do {
            String currentColumn = res.getString(1);
            if (currentColumn.equals(fieldName)) {
                isExist = true;
            }
        } while (res.moveToNext());
        return isExist;
    }
}