package fi.zmengames.zen;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import java.util.Collections;
import java.util.List;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.PojoComparator;
import fr.neamar.kiss.result.Result;
import fr.neamar.kiss.ui.ListPopup;

public class AppsGridFragment extends GridFragment {

    AppListAdapter mAdapter;
    private static final String TAG = AppsGridFragment.class.getSimpleName();
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setEmptyText("No Applications");

        mAdapter = new AppListAdapter(getActivity());
        setGridAdapter(mAdapter);

        List<Pojo> apps = KissApplication.getApplication(getContext()).getDataHandler().getApplications();
        try {
            Collections.sort(apps, new PojoComparator());
        } catch (Exception e){
            Log.d(TAG, "AppsGridFragment, sort exception:" +e);
        }

        mAdapter.setData(apps);
        // till the data is loaded display a spinner
        setGridShown(true);

    }


    @Override
    public boolean onGridItemLongClick(GridView g, View v, int position, long id) {
        AppPojo app = (AppPojo) getGridAdapter().getItem(position);
        Result result = Result.fromPojo(null,app);
        ListPopup menu = result.getPopupMenu(getContext(), KissApplication.getApplication(getContext()).getMainActivity().getAdapter(), v);

        //check if menu contains elements and if yes show it
        if (menu.getAdapter().getCount() > 0) {
            KissApplication.getApplication(getContext()).getMainActivity().registerPopup(menu);
            menu.show(v);
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
