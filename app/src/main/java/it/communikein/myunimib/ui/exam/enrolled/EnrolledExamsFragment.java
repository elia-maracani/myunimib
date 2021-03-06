package it.communikein.myunimib.ui.exam.enrolled;


import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import it.communikein.myunimib.R;
import it.communikein.myunimib.data.model.EnrolledExam;
import it.communikein.myunimib.data.model.ExamID;
import it.communikein.myunimib.data.network.UnimibNetworkDataSource;
import it.communikein.myunimib.databinding.FragmentEnrolledExamsBinding;
import it.communikein.myunimib.ui.MainActivity;
import it.communikein.myunimib.viewmodel.MainActivityViewModel;


/**
 * The {@link Fragment} responsible for showing the user's Enrolled Exams.
 */
public class EnrolledExamsFragment extends Fragment implements
        EnrolledExamAdapter.ExamClickCallback, SwipeRefreshLayout.OnRefreshListener {

    public static final String TAG = EnrolledExamsFragment.class.getSimpleName();

    /*  */
    private FragmentEnrolledExamsBinding mBinding;

    /* Required empty public constructor */
    public EnrolledExamsFragment() {}

    public MainActivityViewModel getViewModel() {
        return ((MainActivity) getActivity()).getViewModel();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        /* Inflate the layout for this fragment */
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_enrolled_exams, container, false);

        /*
         * A LinearLayoutManager is responsible for measuring and positioning item views within a
         * RecyclerView into a linear list. This means that it can produce either a horizontal or
         * vertical list.
         *
         * The third parameter (reverseLayout) should be true if you want to reverse your
         * layout. Generally, this is only true with horizontal lists that need to support a
         * right-to-left layout.
         */
        LinearLayoutManager layoutManager = new LinearLayoutManager(
                getActivity(),
                LinearLayoutManager.VERTICAL,
                false);
        mBinding.rvList.setLayoutManager(layoutManager);

        /*
         * Use this setting to improve performance if you know that changes in content do not
         * change the child layout size in the RecyclerView
         */
        mBinding.rvList.setHasFixedSize(true);

        /* Show data downloading */
        mBinding.swipeRefresh.setOnRefreshListener(this);

        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setTitle();

        /* Create a new EnrolledExamAdapter. It will be responsible for displaying the list's items */
        final EnrolledExamAdapter mExamsAdapter = new EnrolledExamAdapter(this);

        getViewModel().getEnrolledExamsLoading().observe(this, loading -> {
            if (loading != null)
                mBinding.swipeRefresh.setRefreshing(loading);
        });

        getViewModel().getEnrolledExams().observe(this, list -> {
            if (list != null) {
                mExamsAdapter.setList((ArrayList<EnrolledExam>) list);
            }
        });

        /* Setting the adapter attaches it to the RecyclerView in our layout. */
        mBinding.rvList.setAdapter(mExamsAdapter);
    }


    @Override
    public void onRefresh() {
        getViewModel().refreshEnrolledExams();
    }

    /**
     * Change the Activity's ActionBar title.
     */
    private void setTitle() {
        if (getActivity() != null) {
            /* Get a reference to the MainActivity ActionBar */
            ActionBar actionBar = ((MainActivity) getActivity()).getSupportActionBar();
            /* If there is an ActionBar, set it's title */
            if (actionBar != null)
                actionBar.setTitle(R.string.title_exams_enrolled);
        }
    }


    @Override
    public void onListItemClick(ExamID examID) {
        Intent intent = new Intent(getActivity(), EnrolledExamDetailActivity.class);
        intent.putExtra(UnimibNetworkDataSource.ADSCE_ID, examID.getAdsceId());
        intent.putExtra(UnimibNetworkDataSource.APP_ID, examID.getAppId());
        intent.putExtra(UnimibNetworkDataSource.ATT_DID_ESA_ID, examID.getAttDidEsaId());
        intent.putExtra(UnimibNetworkDataSource.CDS_ESA_ID, examID.getCdsEsaId());
        startActivity(intent);
    }
}
