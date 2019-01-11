package fi.zmengames.zlauncher;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.GridView;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.Pojo;

public class AppsGridFragment extends GridFragment {

    AppListAdapter mAdapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setEmptyText("No Applications");

        mAdapter = new AppListAdapter(getActivity());
        setGridAdapter(mAdapter);
        mAdapter.setData(KissApplication.getApplication(getContext()).getDataHandler().getApplications());
        // till the data is loaded display a spinner
        setGridShown(true);

    }



    @Override
    public void onGridItemClick(GridView g, View v, int position, long id) {
        AppPojo app = (AppPojo) getGridAdapter().getItem(position);
        if (app != null) {
            Intent intent = getActivity().getPackageManager().getLaunchIntentForPackage(app.packageName);

            if (intent != null) {
                startActivity(intent);
            }
        }
    }
}
