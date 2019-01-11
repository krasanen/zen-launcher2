package fi.zmengames.zlauncher;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Collection;
import java.util.List;

import fr.neamar.kiss.IconsHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.result.Result;

/**
 * Created by Arnab Chakraborty
 */
public class AppListAdapter extends ArrayAdapter<Pojo> {
    private final LayoutInflater mInflater;
    MainActivity mainActivity;
    public AppListAdapter(Context context) {
        super(context, android.R.layout.simple_list_item_2);
        mainActivity = KissApplication.getApplication(getContext()).getMainActivity();
        mInflater = LayoutInflater.from(context);
    }

    public void setData(List<Pojo> data) {
        clear();
        if (data != null) {
            addAll(data);
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void addAll(Collection<? extends Pojo> items) {
        //If the platform supports it, use addAll, otherwise add in loop
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            super.addAll(items);
        }else{
            for(Pojo item: items){
                super.add(item);
            }
        }
    }

    /**
     * Populate new items in the list.
     */
    @Override public View getView(int position, View convertView, ViewGroup parent) {
        View view;

        if (convertView == null) {
            view = mInflater.inflate(R.layout.list_item_icon_text, parent, false);
        } else {
            view = convertView;
        }

        Pojo item = getItem(position);

        Result result = Result.fromPojo(mainActivity, item);
        Drawable drawable = result.getDrawable(mainActivity);
        ((ImageView)view.findViewById(R.id.icon)).setImageDrawable(drawable);
        ((TextView)view.findViewById(R.id.text)).setText(result.toString());

        return view;
    }
}
