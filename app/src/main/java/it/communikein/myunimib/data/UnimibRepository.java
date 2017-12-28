package it.communikein.myunimib.data;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.util.Log;

import java.util.List;

import it.communikein.myunimib.AppExecutors;
import it.communikein.myunimib.data.database.AvailableExam;
import it.communikein.myunimib.data.database.BookletEntry;
import it.communikein.myunimib.data.database.EnrolledExam;
import it.communikein.myunimib.data.database.ExamID;
import it.communikein.myunimib.data.database.UnimibDao;
import it.communikein.myunimib.data.network.UnimibNetworkDataSource;

public class UnimibRepository {

    private static final String LOG_TAG = UnimibRepository.class.getSimpleName();

    // For Singleton instantiation
    private static final Object LOCK = new Object();
    private static UnimibRepository sInstance;
    private final UnimibDao mUnimibDao;
    private final UnimibNetworkDataSource mUnimibNetworkDataSource;
    private final AppExecutors mExecutors;
    private boolean mInitialized = false;

    private MutableLiveData<Integer> mModifiedBookletEntriesCount;
    private MutableLiveData<Integer> mModifiedAvailableExamsCount;
    private MutableLiveData<Integer> mModifiedEnrolledExamsCount;



    private UnimibRepository(UnimibDao unimibDao,
                             UnimibNetworkDataSource unimibNetworkDataSource,
                               AppExecutors executors) {
        mUnimibDao = unimibDao;
        mUnimibNetworkDataSource = unimibNetworkDataSource;
        mExecutors = executors;

        mModifiedBookletEntriesCount = new MutableLiveData<>();
        mModifiedAvailableExamsCount = new MutableLiveData<>();
        mModifiedEnrolledExamsCount = new MutableLiveData<>();

        LiveData<List<BookletEntry>> bookletOnline =
                mUnimibNetworkDataSource.getOnlineBooklet();
        bookletOnline.observeForever(newOnlineData -> mExecutors.diskIO().execute(() -> {
            if (newOnlineData != null)
                Log.d(LOG_TAG, "Repository observer notified. Found " + newOnlineData.size() + " elements.");
            else
                Log.d(LOG_TAG, "Repository observer notified. Found NULL elements.");

            int newExams = 0;
            if (isBookletEmpty() && newOnlineData != null) {
                mUnimibDao.bulkInsertBooklet(newOnlineData);
                newExams = newOnlineData.size();

                Log.d(LOG_TAG, "New values inserted.");
            }
            else if (newOnlineData != null) {
                for (BookletEntry entry : newOnlineData) {
                    BookletEntry oldEntry = mUnimibDao.getBookletEntry(entry.getAdsceId());

                    if (oldEntry == null) {
                        mUnimibDao.addBookletEntry(entry);
                        Log.d(LOG_TAG, "New value inserted.");
                        newExams++;
                    }
                    else if (!entry.isIdentic(oldEntry)) {
                        mUnimibDao.updateBookletEntry(entry);
                        Log.d(LOG_TAG, "Value updated.");
                        newExams++;
                    }
                }
            }

            mModifiedBookletEntriesCount.postValue(newExams);
        }));

        LiveData<List<AvailableExam>> availableExamsOnline =
                mUnimibNetworkDataSource.getOnlineAvailableExams();
        availableExamsOnline.observeForever(newOnlineData -> mExecutors.diskIO().execute(() -> {
            if (newOnlineData != null)
                Log.d(LOG_TAG, "Repository observer notified. Found " + newOnlineData.size() + " elements.");
            else
                Log.d(LOG_TAG, "Repository observer notified. Found NULL elements.");

            int newExams = 0;
            if (isAvailableExamsEmpty() && newOnlineData != null) {
                mUnimibDao.bulkInsertAvailableExams(newOnlineData);
                newExams = newOnlineData.size();

                Log.d(LOG_TAG, "New values inserted.");
            }
            else if (newOnlineData != null) {
                for (AvailableExam entry : newOnlineData) {
                    AvailableExam oldEntry = mUnimibDao.getAvailableExam(
                            entry.getAdsceId(),
                            entry.getAppId(),
                            entry.getAttDidEsaId(),
                            entry.getCdsEsaId());

                    if (oldEntry == null) {
                        mUnimibDao.addAvailableExam(entry);
                        Log.d(LOG_TAG, "New value inserted.");
                        newExams++;
                    }
                    else if (!entry.isIdentic(oldEntry)) {
                        mUnimibDao.updateAvailableExam(entry);
                        Log.d(LOG_TAG, "Value updated.");
                        newExams++;
                    }
                }
            }

            mModifiedAvailableExamsCount.postValue(newExams);
        }));

        LiveData<List<EnrolledExam>> enrolledExamsOnline =
                mUnimibNetworkDataSource.getOnlineEnrolledExams();
        enrolledExamsOnline.observeForever(newOnlineData -> mExecutors.diskIO().execute(() -> {
            if (newOnlineData != null)
                Log.d(LOG_TAG, "Repository observer notified. Found " + newOnlineData.size() + " elements.");
            else
                Log.d(LOG_TAG, "Repository observer notified. Found NULL elements.");

            int newExams = 0;
            if (isEnrolledExamsEmpty() && newOnlineData != null) {
                mUnimibDao.bulkInsertEnrolledExams(newOnlineData);
                newExams = newOnlineData.size();

                Log.d(LOG_TAG, "New values inserted.");
            }
            else if (newOnlineData != null) {
                for (EnrolledExam entry : newOnlineData) {
                    EnrolledExam oldEntry = mUnimibDao.getEnrolledExam(
                            entry.getAdsceId(),
                            entry.getAppId(),
                            entry.getAttDidEsaId(),
                            entry.getCdsEsaId());

                    if (oldEntry == null) {
                        mUnimibDao.addEnrolledExam(entry);
                        Log.d(LOG_TAG, "New value inserted.");
                        newExams++;
                    }
                    else if (!entry.isIdentic(oldEntry)) {
                        mUnimibDao.updateEnrolledExam(entry);
                        Log.d(LOG_TAG, "Value updated.");
                        newExams++;
                    }
                }
            }

            mModifiedEnrolledExamsCount.postValue(newExams);
        }));
    }

    public synchronized static UnimibRepository getInstance(
            UnimibDao unimibDao, UnimibNetworkDataSource unimibNetworkDataSource,
            AppExecutors executors) {
        Log.d(LOG_TAG, "Getting the repository");
        if (sInstance == null) {
            synchronized (LOCK) {
                sInstance = new UnimibRepository(unimibDao, unimibNetworkDataSource,
                        executors);
                Log.d(LOG_TAG, "Made new repository");
            }
        }
        return sInstance;
    }

    /**
     * Creates periodic sync tasks and checks to see if an immediate sync is required. If an
     * immediate sync is required, this method will take care of making sure that sync occurs.
     */
    private synchronized void initializeData() {

        // Only perform initialization once per app lifetime. If initialization has already been
        // performed, we have nothing to do in this method.
        if (mInitialized) return;
        mInitialized = true;

        mUnimibNetworkDataSource.scheduleRecurringFetchBookletSync();
        mUnimibNetworkDataSource.scheduleRecurringFetchAvailableExamsSync();
        mUnimibNetworkDataSource.scheduleRecurringFetchEnrolledExamsSync();

        mExecutors.diskIO().execute(() ->{
            startFetchBookletService();
            startFetchAvailableExamsService();
            startFetchEnrolledExamsService();
        });
    }


    public MutableLiveData<Integer> getModifiedBookletEntriesCount() {
        initializeData();

        return mModifiedBookletEntriesCount;
    }

    public MutableLiveData<Integer> getModifiedEnrolledExamsCount() {
        initializeData();

        return mModifiedEnrolledExamsCount;
    }

    public MutableLiveData<Integer> getModifiedAvailableExamsCount() {
        initializeData();

        return mModifiedAvailableExamsCount;
    }


    public LiveData<List<BookletEntry>> getCurrentBooklet() {
        initializeData();

        return mUnimibDao.getBooklet();
    }

    public LiveData<List<AvailableExam>> getCurrentAvailableExams() {
        initializeData();

        return mUnimibDao.getAvailableExams();
    }

    public LiveData<List<EnrolledExam>> getCurrentEnrolledExams() {
        initializeData();

        return mUnimibDao.getEnrolledExams();
    }


    public LiveData<Boolean> getBookletLoading() {
        return mUnimibNetworkDataSource.getBookletLoading();
    }

    public LiveData<Boolean> getAvailableExamsLoading() {
        return mUnimibNetworkDataSource.getAvailableExamsLoading();
    }

    public LiveData<Boolean> getEnrolledExamsLoading() {
        return mUnimibNetworkDataSource.getEnrolledExamsLoading();
    }


    public BookletEntry getBookletEntry(int adsceId) {
        initializeData();

        return mUnimibDao.getBookletEntry(adsceId);
    }

    public LiveData<EnrolledExam> getEnrolledExam(ExamID examID) {
        initializeData();

        return mUnimibDao.getObservableEnrolledExam(examID.getAdsceId(), examID.getAppId(),
                examID.getAttDidEsaId(), examID.getCdsEsaId());
    }

    public LiveData<AvailableExam> getAvailableExam(ExamID examID) {
        initializeData();

        return mUnimibDao.getObservableAvailableExam(examID.getAdsceId(), examID.getAppId(),
                examID.getAttDidEsaId(), examID.getCdsEsaId());
    }


    /* */
    private boolean isBookletEmpty() {
        return mUnimibDao.getBookletSize() == 0;
    }

    private boolean isAvailableExamsEmpty() {
        return mUnimibDao.getAvailableExamsSize() == 0;
    }

    private boolean isEnrolledExamsEmpty() {
        return mUnimibDao.getEnrolledExamsSize() == 0;
    }


    /**
     * Deletes old weather data because we don't need to keep multiple days' data
     */
    private void deleteBooklet() {
        mUnimibDao.deleteBooklet();
    }

    private void deleteAvailableExams() {
        mUnimibDao.deleteAvailableExams();
    }

    private void deleteEnrolledExams() {
        mUnimibDao.deleteEnrolledExams();
    }


    /** */
    public void deleteAvailableExam(ExamID examId) {
        mUnimibDao.deleteAvailableExam(examId.getAdsceId(), examId.getAppId(),
                examId.getAttDidEsaId(), examId.getCdsEsaId());
    }

    public void deleteEnrolledExam(ExamID examId) {
        mUnimibDao.deleteAvailableExam(examId.getAdsceId(), examId.getAppId(),
                examId.getAttDidEsaId(), examId.getCdsEsaId());
    }

    /**
     * Network related operation
     */
    public void startFetchBookletService() {
        mUnimibNetworkDataSource.startFetchBookletService();
    }

    public void startFetchAvailableExamsService() {
        mUnimibNetworkDataSource.startFetchAvailableExamsService();
    }

    public void startFetchEnrolledExamsService() {
        mUnimibNetworkDataSource.startFetchEnrolledExamsService();
    }

}
