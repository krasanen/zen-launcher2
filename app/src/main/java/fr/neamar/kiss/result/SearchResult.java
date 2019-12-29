package fr.neamar.kiss.result;


import android.content.Context;
import android.content.Intent;

import android.graphics.PorterDuff;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.greenrobot.eventbus.EventBus;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import fi.zmengames.zen.AlarmUtils;
import fi.zmengames.zen.LauncherService;
import fi.zmengames.zen.ZEvent;
import fi.zmengames.zen.ZenProvider;
import fr.neamar.kiss.BuildConfig;
import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.pojo.SearchPojo;
import fr.neamar.kiss.pojo.ShortcutsPojo;
import fr.neamar.kiss.ui.ListPopup;
import fr.neamar.kiss.utils.ClipboardUtils;
import fr.neamar.kiss.utils.FuzzyScore;

import static fi.zmengames.zen.LauncherService.ALARM_ENTERED_TEXT;
import static fr.neamar.kiss.MainActivity.ALARM_IN_ACTION;
import static fr.neamar.kiss.MainActivity.DATE_TIME_PICKER;
import static fr.neamar.kiss.MainActivity.LOCK_IN;

public class SearchResult extends Result {
    private final SearchPojo searchPojo;
    private boolean zenQuery = false;
    private boolean urlQuery = false;
    SearchResult(SearchPojo searchPojo) {
        super(searchPojo);
        this.searchPojo = searchPojo;
    }

    @NonNull
    @Override
    public View display(Context context, int position, View v, @NonNull ViewGroup parent, FuzzyScore fuzzyScore) {
        if (v == null)
            v = inflateFromId(context, R.layout.item_search, parent);
        zenQuery = false;
        urlQuery = false;
        TextView searchText = v.findViewById(R.id.item_search_text);
        ImageView image = v.findViewById(R.id.item_search_icon);

        String text;
        int pos;
        int len;

        if (searchPojo.type == SearchPojo.URL_QUERY) {
            urlQuery = true;
            text = String.format(context.getString(R.string.ui_item_visit), this.pojo.getName());
            pos = text.indexOf(this.pojo.getName());
            len = this.pojo.getName().length();
            image.setImageResource(R.drawable.ic_public);
        } else if (searchPojo.type == SearchPojo.SEARCH_QUERY) {
            text = String.format(context.getString(R.string.ui_item_search), this.pojo.getName(), searchPojo.query);
            pos = text.indexOf(searchPojo.query);
            len = searchPojo.query.length();
            image.setImageResource(R.drawable.search);
        } else if (searchPojo.type == SearchPojo.CALCULATOR_QUERY) {
            text = searchPojo.query;
            pos = text.indexOf("=");
            len = text.length() - pos;
            image.setImageResource(R.drawable.ic_functions);
        } else if (searchPojo.type == SearchPojo.ZEN_QUERY) {
            zenQuery = true;
            text = searchPojo.query;
            pos = text.indexOf(searchPojo.query);
            len = searchPojo.query.length();
            if (searchPojo.url.contains(ZenProvider.mAlarm)) {
                image.setImageResource(R.drawable.ic_alarm_add_24px);
            } else {
                image.setImageResource(R.drawable.ic_lock_24px);
            }

        }else if (searchPojo.type == SearchPojo.ZEN_ALARM) {
            text = searchPojo.query;
            pos = text.indexOf(searchPojo.query);
            len = searchPojo.query.length();
            image.setImageResource(R.drawable.ic_alarm_add_24px);
            zenQuery = true;
        }

        else {
            throw new IllegalArgumentException();
        }

        displayHighlighted(text, Collections.singletonList(new Pair<>(pos, pos + len)), searchText, context);

        image.setColorFilter(getThemeFillColor(context), PorterDuff.Mode.SRC_IN);
        return v;
    }

    @Override
    public void doLaunch(Context context, View v) {
        switch (searchPojo.type) {
            case SearchPojo.URL_QUERY:
            case SearchPojo.SEARCH_QUERY:
                String query;
                try {
                    query = URLEncoder.encode(searchPojo.query, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    query = URLEncoder.encode(searchPojo.query);
                }
                String urlWithQuery = searchPojo.url.replaceAll("%s|\\{q\\}", query);
                Uri uri = Uri.parse(urlWithQuery);
                Intent search = new Intent(Intent.ACTION_VIEW, uri);
                search.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    context.startActivity(search);
                } catch (android.content.ActivityNotFoundException e) {
                    Log.w("SearchResult", "Unable to run search for url: " + searchPojo.url);
                }
                break;
            case SearchPojo.CALCULATOR_QUERY:
                ClipboardUtils.setClipboard(context, searchPojo.query.substring(searchPojo.query.indexOf("=") + 2));
                Toast.makeText(context, R.string.copy_confirmation, Toast.LENGTH_SHORT).show();
                break;
            case SearchPojo.ZEN_QUERY:

                if (searchPojo.url.contains(ZenProvider.mAlarm)) {
                    String minutesOrTime = searchPojo.url.substring(ZenProvider.mAlarm.length());
                    Intent alarmIntent = new Intent(context, LauncherService.class);
                    if (BuildConfig.DEBUG) Log.w("ZEN_QUERY", "minutesOrTime: " + minutesOrTime);
                    if (minutesOrTime.isEmpty()) {
                        ZEvent event = new ZEvent(ZEvent.State.INTERNAL_EVENT, DATE_TIME_PICKER+searchPojo.query);
                        EventBus.getDefault().post(event);
                        return;
                    }
                    else {
                        if (BuildConfig.DEBUG) Log.w("ZEN_QUERY", "minutesOrTime: " + minutesOrTime);
                        if (BuildConfig.DEBUG) Log.w("ZEN_QUERY", "id: " + searchPojo.id);
                        if (minutesOrTime.contains(":")) {

                            minutesOrTime = minutesOrTime.replace(" ", "");
                            Calendar rightNow = Calendar.getInstance();
                            int currentHourIn24Format = rightNow.get(Calendar.HOUR_OF_DAY); // return the hour in 24 hrs format (ranging from 0-23)

                            int currentMinutes = rightNow.get(Calendar.MINUTE); // return t

                            Date dt = null;
                            Date dt2 = null;
                            DateFormat dateFormat = new SimpleDateFormat("hh:mma",new Locale("en"));
                            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                            TimeZone tz = dateFormat.getTimeZone();
                            if (BuildConfig.DEBUG)  Log.w("ZEN_QUERY","timzenone:"+tz.getDisplayName(true, TimeZone.SHORT)+" Timezon id :: " +tz.getID());
                            long millis= 0;
                            try {
                                dt = dateFormat.parse(minutesOrTime);
                                dt2 = dateFormat.parse(""+currentHourIn24Format+":"+currentMinutes);
                                millis = dt.getTime()-dt2.getTime();
                                if (BuildConfig.DEBUG)  Log.w("ZEN_QUERY", "diff:"+millis);

                                if (millis<0){
                                    if (BuildConfig.DEBUG)  Log.w("ZEN_QUERY", "BEFORE");
                                    millis +=12*60*60*1000;
                                }
                            } catch (ParseException e) {

                                dateFormat = new SimpleDateFormat("HH:mm",new Locale("en"));
                                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                                try {
                                    dt = dateFormat.parse(minutesOrTime);
                                    dt2 = dateFormat.parse(""+currentHourIn24Format+":"+currentMinutes);
                                    millis = dt.getTime()-dt2.getTime();
                                    if (BuildConfig.DEBUG)  Log.w("ZEN_QUERY", "diff:"+millis);

                                    if (millis<0){
                                        if (BuildConfig.DEBUG)  Log.w("ZEN_QUERY", "BEFORE");
                                        millis +=24*60*60*1000;
                                    }
                                } catch (ParseException e2) {
                                    e2.printStackTrace();
                                    return;
                                }
                            }


                            if (BuildConfig.DEBUG)  Log.w("ZEN_QUERY", "millis: " + millis);
                            long mins = TimeUnit.MILLISECONDS.toMinutes(millis);

                            if (BuildConfig.DEBUG) Log.w("ZEN_QUERY", "mins: " + mins);

                            alarmIntent.putExtra(ZenProvider.mMinutes, mins);
                        } else {
                            alarmIntent.putExtra(ZenProvider.mMinutes, Long.valueOf(minutesOrTime));
                        }
                    }
                    alarmIntent.putExtra(ALARM_ENTERED_TEXT, searchPojo.query + "\n"+ searchPojo.id);
                    alarmIntent.setAction(ALARM_IN_ACTION);
                    KissApplication.startLaucherService(alarmIntent, context);
                } else if (searchPojo.url.contains(ZenProvider.mLockIn)) {
                    String minutes = searchPojo.url.substring(ZenProvider.mLockIn.length());
                    Intent lockin = new Intent(context, LauncherService.class);
                    try {
                        lockin.putExtra(ZenProvider.mMinutes, Integer.valueOf(minutes));
                    } catch (NumberFormatException e){
                        Toast.makeText(context, context.getString(R.string.minutes) + " or " +
                                context.getString(R.string.hours), Toast.LENGTH_SHORT).show();
                        break;
                    }
                    lockin.setAction(LOCK_IN);
                    KissApplication.startLaucherService(lockin, context);
                }
                break;
            case SearchPojo.ZEN_ALARM:
                break;
        }
    }

    @Override
    protected ListPopup buildPopupMenu(Context context, ArrayAdapter<ListPopup.Item> adapter, final RecordAdapter parent, View parentView) {
        adapter.add(new ListPopup.Item(context, R.string.share));
        if (zenQuery) {
            adapter.add(new ListPopup.Item(context, R.string.removeAlarm));
        }
        if (urlQuery){
            adapter.add(new ListPopup.Item(context, R.string.add_shortcut));
        }
        return inflatePopupMenu(adapter, context);
    }

    @Override
    protected boolean popupMenuClickHandler(Context context, RecordAdapter parent, int stringId, View parentView) {
        switch (stringId) {
            case R.string.share:
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_TEXT, searchPojo.query);
                shareIntent.setType("text/plain");
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(shareIntent);
                return true;
            case R.string.removeAlarm:
                AlarmUtils.cancelAlarm(context, Long.parseLong(searchPojo.url));
                parent.clear();
                return true;
            case R.string.add_shortcut:
                DataHandler dataHandler = KissApplication.getApplication(context).getDataHandler();
                ShortcutsPojo record = new ShortcutsPojo(ShortcutsPojo.SCHEME + searchPojo.url, searchPojo.url, searchPojo.url, searchPojo.url, null);
                record.setName(searchPojo.url);
                record.setTags(searchPojo.url);
                dataHandler.addShortcut(record);
                return true;

        }

        return super.popupMenuClickHandler(context, parent, stringId, parentView);
    }

}
