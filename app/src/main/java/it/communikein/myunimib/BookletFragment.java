package it.communikein.myunimib;


import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.app.LoaderManager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import it.communikein.myunimib.data.ExamContract;
import it.communikein.myunimib.databinding.FragmentBookletBinding;
import it.communikein.myunimib.sync.booklet.SyncUtilsBooklet;
import it.communikein.myunimib.utilities.UserUtils;


/**
 * The {@link Fragment} responsible for showing the user's booklet.
 */
public class BookletFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>{

    /* The columns of data that we are interested in displaying. */
    private static final String[] BOOKLET_PROJECTION = {
            ExamContract.BookletEntry.COLUMN_COURSE_NAME,
            ExamContract.BookletEntry.COLUMN_MARK,
            ExamContract.BookletEntry.COLUMN_STATE
    };

    /*
     * The indices of the values in the array of Strings above.
     * If the order of the Strings above changes, these indices must be adjusted
     * to match the order of the Strings.
     */
    public static final int INDEX_BOOKLET_COURSE_NAME = 0;
    public static final int INDEX_BOOKLET_MARK = 1;
    public static final int INDEX_BOOKLET_STATE = 2;

    /* Booklet Loader unique identifier */
    private static final int ID_BOOKLET_LOADER = 44;

    /*  */
    private BookletAdapter mBookletAdapter;
    private int mPosition = RecyclerView.NO_POSITION;

    /*  */
    private FragmentBookletBinding mBinding;

    /* Required empty public constructor */
    public BookletFragment() {}


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        /* Inflate the layout for this fragment */
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_booklet, container, false);

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

        /* Create a new BookletAdapter. It will be responsible for displaying the list's items */
        if (getActivity() != null)
            mBookletAdapter = new BookletAdapter(getActivity());

        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setTitle();

        /* Setting the adapter attaches it to the RecyclerView in our layout. */
        mBinding.rvList.setAdapter(mBookletAdapter);

        /*
         * Ensures a loader is initialized and active and shows the loading view.
         * If the loader doesn't already exist, one is created and (if the activity/fragment is
         * currently started) starts the loader. Otherwise the last created loader is re-used.
         */
        if (!UserUtils.getUser(getActivity()).isFake()) {
            toggleLoading(true);
            if (getActivity() != null)
                getActivity().getSupportLoaderManager()
                        .initLoader(ID_BOOKLET_LOADER, null, this);

            SyncUtilsBooklet.initialize(getActivity());
        }
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
                actionBar.setTitle(R.string.title_booklet);
        }
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case ID_BOOKLET_LOADER:
                /* URI for all rows of booklet data in our booklet table */
                Uri bookletQueryUri = ExamContract.BookletEntry.CONTENT_URI;

                /*
                 * Create a new CursorLoader and pass in the URI for the data and the list
                 * of parameters that we are interested in.
                 */
                if (getActivity() != null)
                    return new CursorLoader(getActivity(),
                            bookletQueryUri,
                            BOOKLET_PROJECTION,
                            null,
                            null,
                            null);

            default:
                /* If any other Loader is required, fail.  */
                throw new RuntimeException("Loader Not Implemented: " + id);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        /* We've got new data to display, so we swap it into the adapter */
        mBookletAdapter.swapCursor(data);

        /* If no position is chosen, choose the first */
        if (mPosition == RecyclerView.NO_POSITION) mPosition = 0;
        /* Smoothly scroll the recyclerview to the chosen position */
        mBinding.rvList.smoothScrollToPosition(mPosition);

        /* Hide the loading view and show the data */
        toggleLoading(false);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        /*
         * Since this Loader's data is now invalid, we need to clear the Adapter that is
         * displaying the data.
         */
        mBookletAdapter.swapCursor(null);
    }

    /**
     * Show or hide the loading view and the data view.
     *
     * @param show If this is set to true, this method will show the loading view and hide the
     *             data view. If its set to false, it'll do the opposite.
     */
    private void toggleLoading(boolean show) {
        /* Hide the recyclerview since there is no data to show yet */
        mBinding.rvList.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
        /* Show the loading since we're trying to get the data to display */
        mBinding.pbLoadingIndicator.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }
}