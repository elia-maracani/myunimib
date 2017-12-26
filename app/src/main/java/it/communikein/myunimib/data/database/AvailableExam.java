package it.communikein.myunimib.data.database;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Date;

@Entity(tableName = "available_exams", primaryKeys = {"adsceId", "appId", "attDidEsaId", "cdsEsaId"})
public class AvailableExam extends Exam {

    private static final String ARG_BEGIN_ENROLLMENT = "ARG_BEGIN_ENROLLMENT";
    private static final String ARG_END_ENROLLMENT = "ARG_END_ENROLLMENT";

    private Date beginEnrollment;
    private Date endEnrollment;

    public AvailableExam(int cdsEsaId, int attDidEsaId, int appId, int adsceId, String name,
                         Date date, String description, Date beginEnrollment, Date endEnrollment) {
        super(cdsEsaId, attDidEsaId, appId, adsceId, name, date, description);

        setBeginEnrollment(beginEnrollment);
        setEndEnrollment(endEnrollment);
    }

    @Ignore
    public AvailableExam(ExamID examID, String name, Date date,
                         String description, Date begin_enrollment, Date end_enrollment) {
        super(examID, name, date, description);

        setBeginEnrollment(begin_enrollment);
        setEndEnrollment(end_enrollment);
    }

    @Ignore
    public AvailableExam(JSONObject obj) throws JSONException, NullPointerException, ParseException {
        super(obj);

        if (obj.has(ARG_BEGIN_ENROLLMENT))
            setBeginEnrollment(obj.getLong(ARG_BEGIN_ENROLLMENT));
        if (obj.has(ARG_END_ENROLLMENT))
            setEndEnrollment(obj.getLong(ARG_END_ENROLLMENT));
    }


    public Date getBeginEnrollment() {
        return beginEnrollment;
    }

    @Ignore
    public long getBeginMillis() {
        if (getBeginEnrollment() == null) return -1;
        else return getBeginEnrollment().getTime();
    }

    @Ignore
    public long getEndMillis() {
        if (getEndEnrollment() == null) return -1;
        else return getEndEnrollment().getTime();
    }

    public Date getEndEnrollment() {
        return endEnrollment;
    }

    private void setBeginEnrollment(Date beginEnrollment) {
        this.beginEnrollment = beginEnrollment;
    }

    private void setEndEnrollment(Date endEnrollment) {
        this.endEnrollment = endEnrollment;
    }

    @Ignore
    private void setBeginEnrollment(long millis) {
        if (millis < 0) setBeginEnrollment(null);
        else setBeginEnrollment(new Date(millis));
    }

    @Ignore
    private void setEndEnrollment(long millis) {
        if (millis < 0) setEndEnrollment(null);
        else setEndEnrollment(new Date(millis));
    }


    @Ignore
    public JSONObject toJSON() {
        JSONObject obj;

        try {
            obj = super.toJSON();

            obj.put(ARG_BEGIN_ENROLLMENT, getBeginMillis());
            obj.put(ARG_END_ENROLLMENT, getEndMillis());
        } catch (JSONException e){
            obj = new JSONObject();
        }

        return obj;
    }


    @Ignore
    @Override
    public String toString() {
        return toJSON().toString();
    }

    @Override
    public boolean isIdentic(Object obj) {
        if (! (obj instanceof AvailableExam)) return false;

        AvailableExam exam = (AvailableExam) obj;
        return super.isIdentic(exam) &&
                exam.getBeginMillis() == getBeginMillis() &&
                exam.getEndMillis() == getEndMillis();
    }

}