package fi.zmengames.zen;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Collections;
import java.util.List;

import fr.neamar.kiss.BuildConfig;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.PojoComparator;
import fr.neamar.kiss.result.Result;
import fr.neamar.kiss.ui.ListPopup;

public class AppsGridFragment extends GridFragment {

    AppListAdapter mAdapter;
    private GridView gridView;
    private static final String TAG = AppsGridFragment.class.getSimpleName();
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (BuildConfig.DEBUG) Log.d(TAG,"onActivityCreated");

        setEmptyText("No Applications");

        mAdapter = new AppListAdapter(getActivity());
        setGridAdapter(mAdapter);

        gridView = getGridView(); // Assuming you have a method to get the GridView
        gridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (firstVisibleItem == 0 && gridView.getChildAt(0) != null && gridView.getChildAt(0).getTop() == 0) {
                    AppGridActivity.onTop = true;
                } else {
                    AppGridActivity.onTop = false;
                }
            }
        });

        List<Pojo> apps = KissApplication.getApplication(getContext()).getDataHandler().getApplications();
        try {
            Collections.sort(apps, new PojoComparator());
        } catch (Exception e){
            Log.d(TAG, "AppsGridFragment, sort exception:" +e);
        }

        mAdapter.setData(apps);
        // till the data is loaded display a spinner
        setGridShown(true);

        EventBus.getDefault().register(this);
    }
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ZEvent event) {
        if (BuildConfig.DEBUG) Log.i(TAG, "Got message from service: " + event.getState());

        switch (event.getState()) {
            case LOAD_OVER:
                Log.d(TAG, "LOAD_OVER");
                List<Pojo> apps = KissApplication.getApplication(getContext()).getDataHandler().getApplications();
                try {
                    Collections.sort(apps, new PojoComparator());
                } catch (Exception e){
                    Log.d(TAG, "AppsGridFragment, sort exception:" +e);
                }
                mAdapter.setData(apps);
                mAdapter.notifyDataSetInvalidated();
                break;
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
    }
    @Override
    public boolean onGridItemLongClick(GridView g, View v, int position, long id) {
        AppPojo app = (AppPojo) getGridAdapter().getItem(position);
        Result result = Result.fromPojo(null,app);
        MainActivity mainActivity = KissApplication.getApplication(v.getContext()).getMainActivity();
        if (mainActivity!=null) {
            ListPopup menu = result.getPopupMenu(v.getContext(), mainActivity.getAdapter(), v);
            //check if menu contains elements and if yes show it
            if (menu.getAdapter().getCount() > 0) {
                KissApplication.getApplication(v.getContext()).getMainActivity().registerPopup(menu);
                menu.show(v);
            }
        } else {
            Log.d(TAG,"not able to show menu, mainActivity is null");
        }
        return true;
    }

    @Override
    public void onGridItemClick(GridView g, View v, int position, long id) {
        AppPojo app = (AppPojo) getGridAdapter().getItem(position);
        if (app != null) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setComponent(ComponentName.unflattenFromString(app.getComponentName()));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                intent.setSourceBounds(v.getClipBounds());
            }
            try {
                this.startActivity(intent);
            } catch (Exception e){
                Toast.makeText(getContext(), R.string.application_not_found, Toast.LENGTH_LONG).show();
            }
        }

    }
}
