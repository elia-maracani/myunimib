package it.communikein.myunimib.data.database;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;


@Dao
public interface UnimibDao {

    @Insert
    void addBookletEntry(BookletEntry entry);

    @Insert
    void addAvailableExam(AvailableExam entry);

    @Insert
    void addEnrolledExam(EnrolledExam entry);


    @Insert
    void bulkInsertBooklet(List<BookletEntry> entry);

    @Insert
    void bulkInsertAvailableExams(List<AvailableExam> entry);

    @Insert
    void bulkInsertEnrolledExams(List<EnrolledExam> entry);


    @Query("DELETE FROM booklet")
    void deleteBooklet();

    @Query("DELETE FROM available_exams")
    void deleteAvailableExams();

    @Query("DELETE FROM enrolled_exams")
    void deleteEnrolledExams();


    @Query("DELETE FROM available_exams WHERE adsceId = :adsceId AND appId = :appId " +
            "AND attDidEsaId = :attDidEsaId AND cdsEsaId = :cdsEsaId")
    void deleteAvailableExam(int adsceId, int appId, int attDidEsaId, int cdsEsaId);

    @Query("DELETE FROM enrolled_exams WHERE adsceId = :adsceId AND appId = :appId " +
            "AND attDidEsaId = :attDidEsaId AND cdsEsaId = :cdsEsaId")
    void deleteEnrolledExam(int adsceId, int appId, int attDidEsaId, int cdsEsaId);


    @Query("SELECT * FROM booklet")
    LiveData<List<BookletEntry>> getBooklet();

    @Query("SELECT * FROM available_exams")
    LiveData<List<AvailableExam>> getAvailableExams();

    @Query("SELECT * FROM enrolled_exams")
    LiveData<List<EnrolledExam>> getEnrolledExams();


    @Query("SELECT * FROM booklet WHERE adsceId = :adsceId")
    BookletEntry getBookletEntry(int adsceId);

    @Query("SELECT * FROM available_exams WHERE adsceId = :adsceId AND appId = :appId " +
            "AND attDidEsaId = :attDidEsaId AND cdsEsaId = :cdsEsaId")
    LiveData<AvailableExam> getObservableAvailableExam(int adsceId, int appId, int attDidEsaId, int cdsEsaId);

    @Query("SELECT * FROM available_exams WHERE adsceId = :adsceId AND appId = :appId " +
            "AND attDidEsaId = :attDidEsaId AND cdsEsaId = :cdsEsaId")
    AvailableExam getAvailableExam(int adsceId, int appId, int attDidEsaId, int cdsEsaId);

    @Query("SELECT * FROM enrolled_exams WHERE adsceId = :adsceId AND appId = :appId " +
            "AND attDidEsaId = :attDidEsaId AND cdsEsaId = :cdsEsaId")
    EnrolledExam getEnrolledExam(int adsceId, int appId, int attDidEsaId, int cdsEsaId);


    @Query("SELECT COUNT(adsceId) FROM booklet")
    int getBookletSize();

    @Query("SELECT COUNT(adsceId) FROM available_exams")
    int getAvailableExamsSize();

    @Query("SELECT COUNT(adsceId) FROM enrolled_exams")
    int getEnrolledExamsSize();


    @Query("SELECT UPPER(name) FROM booklet WHERE UPPER(name) LIKE UPPER(:name)")
    List<String> getCoursesNames(String name);


    @Update
    int updateBookletEntry(BookletEntry entry);

    @Update
    int updateAvailableExam(AvailableExam entry);

    @Update
    int updateEnrolledExam(EnrolledExam entry);
}