package fi.zmengames.zlauncher;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
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
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void addAll(Collection<? extends Pojo> items) {
        //If the platform supports it, use addAll, otherwise add in loop
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            super.addAll(items);
        } else {
            for (Pojo item : items) {
                super.add(item);
            }
        }
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
            holder.icon = (ImageView) view.findViewById(R.id.icon);
            holder.icon.setVisibility(View.GONE);
            holder.text = (TextView) view.findViewById(R.id.text);
            holder.text.setVisibility(View.GONE);
            holder.context = getContext();
            // initialize views
            view.setTag(holder);  // set tag on view
        } else {
            view = convertView;
            holder = (ViewHolder) convertView.getTag();
        }

        if (holder != null) {
            new ImageLoadTask(position).execute(holder);
        }

        return view;
    }

    static class ViewHolder {
        TextView text;
        ImageView icon;
        Context context;
    }

    private class ImageLoadTask extends AsyncTask<ViewHolder, Void, Drawable> {
        private final int mPosition;
        private ViewHolder v;
        private String text;
        private ImageLoadTask(int position) {
            this.mPosition = position;

        }

        @Override
        protected Drawable doInBackground(ViewHolder... params) {
            v = params[0];
            Pojo item = getItem(mPosition);
            final Result result = Result.fromPojo(null, item);
            this.text = item.getName();
            return result.getDrawable(v.context);
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

        }
    }

}
