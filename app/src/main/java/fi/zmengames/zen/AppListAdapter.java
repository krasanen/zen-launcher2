package fi.zmengames.zen;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Collection;
import java.util.List;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
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
    public void addAll(Collection<? extends Pojo> items) {
        //If the platform supports it, use addAll, otherwise add in loop
        super.addAll(items);
    }


    /**
     * Populate new items in the list.
     */

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final View view;
        final ViewHolder holder;

        if (convertView == null) {
            view = mInflater.inflate(R.layout.list_item_icon_text, parent, false);
            holder = new ViewHolder();
            holder.icon = view.findViewById(R.id.icon);
            holder.icon.setVisibility(View.GONE);
            holder.text = view.findViewById(R.id.text);
            holder.text.setVisibility(View.GONE);
            holder.context = getContext();
            holder.badge = view.findViewById(R.id.zen_badge_count);
            holder.notificationCount = view.findViewById(R.id.zen_item_notification_count);
            // initialize views
            view.setTag(holder);  // set tag on view
        } else {
            view = convertView;
            holder = (ViewHolder) convertView.getTag();
        }

        if (holder != null) {
            new ImageLoadTask(position).execute(holder);
            int textColor = getColorBasedOnTheme(getContext(), R.attr.resultColor);
            holder.text.setTextColor(textColor);
        }

        return view;
    }

    private int getColorBasedOnTheme(Context context, int attr) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    static class ViewHolder {
        TextView text;
        ImageView icon;
        TextView badge;
        TextView notificationCount;
        Context context;
    }

    private class ImageLoadTask extends AsyncTask<ViewHolder, Void, Drawable> {
        private final int mPosition;
        private ViewHolder v;
        private String text;
        private String badgeText;
        private int notificationCount;
        private ImageLoadTask(int position) {
            this.mPosition = position;

        }

        @Override
        protected Drawable doInBackground(ViewHolder... params) {
            v = params[0];
            if (mPosition<getCount()) {
                Pojo item = getItem(mPosition);
                if (item==null)
                    return new ColorDrawable(Color.TRANSPARENT);
                this.notificationCount = item.getNotificationCount();
                final Result result = Result.fromPojo(null, item);
                this.text = item.getName();

                if (item.getBadgeCount() > 0) {
                    this.badgeText = item.getBadgeText();
                }

                return result.getDrawable(v.context);
            } else {
                return new ColorDrawable(Color.TRANSPARENT);
            }
        }

        @Override
        protected void onPostExecute(Drawable result) {
            super.onPostExecute(result);
            // If this item hasn't been recycled already, hide the
            // progress and set and show the image
            v.text.setText(text);
            v.text.setVisibility(View.VISIBLE);
            v.icon.setVisibility(View.VISIBLE);
            v.icon.setImageDrawable(result);
            if (badgeText !=null) {
                v.badge.setText(badgeText);
                v.badge.setVisibility(View.VISIBLE);
            }
            v.notificationCount.setVisibility(notificationCount>0 ? View.VISIBLE : View.GONE);
        }
    }
}
