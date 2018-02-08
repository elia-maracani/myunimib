package it.communikein.myunimib.data.network.loaders;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;

import it.communikein.myunimib.R;
import it.communikein.myunimib.data.model.AvailableExam;
import it.communikein.myunimib.data.model.EnrolledExam;
import it.communikein.myunimib.data.model.Exam;
import it.communikein.myunimib.data.model.UserAuthentication;
import it.communikein.myunimib.utilities.Utils;
import it.communikein.myunimib.data.model.User;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;


public class S3Helper {

    private static final String TAG = S3Helper.class.getSimpleName();

    public static final String URL_HOME =
            "https://s3w.si.unimib.it/esse3/Home.do;";
    public static final String URL_LIBRETTO =
            "https://s3w.si.unimib.it/esse3/auth/studente/Libretto/LibrettoHome.do;";
    public static final String URL_AVAILABLE_EXAMS =
            "https://s3w.si.unimib.it/esse3/auth/studente/Appelli/Appelli.do;";
    public static final String URL_ENROLLED_EXAMS =
            "https://s3w.si.unimib.it/esse3/auth/studente/Appelli/BachecaPrenotazioni.do;";
    public static final String URL_ENROLLED_EXAM_CERTIFICATE =
            "https://s3w.si.unimib.it/esse3/auth/studente/Appelli/StampaStatinoPDF.do?";
    public static final String URL_ENROLL_TO =
            "https://s3w.si.unimib.it/esse3/auth/studente/Appelli/EffettuaPrenotazioneAppello.do;";
    public static final String URL_UNENROLL_FROM =
            "https://s3w.si.unimib.it/esse3/auth/studente/Appelli/CancellaAppello.do;";
    public static final String URL_PROFILE_PICTURE =
            "https://s3w.si.unimib.it/esse3/auth/AddressBook/DownloadFoto.do;";
    public static final String URL_CAREER_BASE =
            "https://s3w.si.unimib.it/esse3/auth/studente/SceltaCarrieraStudente.do;";

    public static final int ERROR_GENERIC = -1;
    public static final int ERROR_S3_NOT_AVAILABLE = -2;
    public static final int ERROR_WRONG_PASSWORD = -3;
    public static final int ERROR_CONNECTION_TIMEOUT = -4;
    public static final int ERROR_FACULTY_TO_CHOOSE = -5;
    public static final int ERROR_CAREER_OVER = -6;
    public static final int ERROR_RESPONSE_NULL = -7;

    public static final int OK_LOGGED_IN = 1;
    public static final int OK_LOGGED_OUT = 2;
    public static final int OK_UPDATED = 3;

    public interface NewSessionIdListener {
        void onNewSessionID(String sessionID);
    }
    public interface SaveUserListener {
        void onSaveUser(User user);
    }

    private static URL buildUrl(UserAuthentication user, String url, String query, boolean queryOperator) {
        String url_string = url + "JSESSIONID=" + user.getSessionID();
        if (user.isFacultyChosen())
            url_string += "?stu_id=" + user.getSelectedFaculty();
        if (query != null) {
            if (queryOperator) url_string += "?";
            else url_string += "&";
            url_string += query;
        }

        try {
            return new URL(url_string);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    @Nullable
    public static SSLSocketFactory getSocketFactory(Context context) {

        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate ca;
            try (InputStream caInput = context.getResources().openRawResource(R.raw.terenasslca3)) {
                ca = cf.generateCertificate(caInput);
                Log.e("CERT", "ca=" + ((X509Certificate) ca).getSubjectDN());
            }

            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            HostnameVerifier hostnameVerifier = (hostname, session) -> {
                Log.e("CipherUsed", session.getCipherSuite());
                return hostname.compareTo("s3w.si.unimib.it") == 0;
            };
            HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

            return sslContext.getSocketFactory();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return  null;
    }


    public static HttpsURLConnection getPage(UserAuthentication user, String url, Context context,
                                      NewSessionIdListener listener) throws IOException {
        return getPage(user, url, null, false, context, listener);
    }

    public static HttpsURLConnection getPage(UserAuthentication user, String url, String query,
                                             boolean queryOperator, Context context,
                                             NewSessionIdListener listener) throws IOException {
        String USER_AGENT = System.getProperty("http.agent");

        URL url_target = buildUrl(user, url, query, queryOperator);
        if (url_target == null) return null;
        HttpsURLConnection con = (HttpsURLConnection) url_target.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Accept", "text/html");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Host", "s3w.si.unimib.it");
        con.setRequestProperty("Connection", "keep-alive");
        if (user.getAuthToken() != null)
            con.setRequestProperty("Authorization", "Basic " + user.getAuthToken());
        if (user.getSessionID() != null)
            con.setRequestProperty("Cookie", "JSESSIONID=" + user.getSessionID());
        con.setConnectTimeout(5000);

        /* For devices running Nougat (API 24) or above, use the network_security_config.xml */
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
            con.setSSLSocketFactory(getSocketFactory(context));

        String cookie = con.getHeaderField("Set-Cookie");
        String sessionID = parseSessionID(cookie);
        if (sessionID != null) {
            user.setSessionID(sessionID);
            listener.onNewSessionID(sessionID);
            con = getPage(user, url, query, queryOperator, context, listener);
        }

        return con;
    }

    public static String getHTML(InputStream instream) throws IOException {
        BufferedReader rd;
        String ris;
        rd = new BufferedReader(new InputStreamReader(instream));

        StringBuilder result = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        rd.close();
        ris = result.toString();

        return ris;
    }

    public static String parseSessionID(String cookies) {
        if (cookies != null && !cookies.isEmpty() && cookies.contains("JSESSIONID")) {
            cookies = cookies.substring(cookies.indexOf("JSESSIONID=") + 11);
            cookies = cookies.substring(0, cookies.indexOf(";"));

            return cookies;
        }

        return null;
    }



    public static boolean isCareerOver(UserAuthentication user, Document document) {
        if (user.isFake()) return false;

        Element el = document.getElementById("gu-textStatusStudente");

        return el.text().toLowerCase().contains("cessato");
    }

    public static User downloadUserData(UserAuthentication user, Context context,
                                        NewSessionIdListener listener) throws IOException {
        User userData = (User) user;
        if (userData.isFake()) {
            userData.setMatricola("293640");
            userData.setName("Pippo Pluto");
            userData.setTotalCFU(42);
            userData.setAverageMark(24);

            return userData;
        }

        try {
            HttpURLConnection response = getPage(user, S3Helper.URL_LIBRETTO, context, listener);
            Document doc;
            String result = S3Helper.getHTML(response.getInputStream());

            if (response.getResponseCode() == HttpURLConnection.HTTP_OK){
                doc = Jsoup.parse(result);
                Element el2 = doc.select("#esse3old > table:has(div.titolopagina)").first();

                if (el2 != null) {
                    String matricola = el2.text();
                    matricola = matricola.substring(matricola.indexOf("MAT. ") + 5);
                    matricola = matricola.substring(0, 6);

                    userData.setMatricola(matricola);
                    Log.d("LOGIN_USER_DATA", "Matricola: " + matricola);
                }
            }

            response = getPage(user, S3Helper.URL_HOME, context, listener);
            result = S3Helper.getHTML(response.getInputStream());
            doc = Jsoup.parse(result);
            Element el = doc.select("div#sottotitolo-menu-principale").first();

            String name = el.text();
            userData.setName(name);
            Log.d("LOGIN_USER_DATA", "Name: " + name);

            Elements els = doc.select("div#gu-boxRiepilogoEsami dl.record-riga dd");
            String averageTmp = els.get(2).text();
            averageTmp = averageTmp.substring(0, averageTmp.length()-3);
            String cfuTmp = els.get(3).text();
            cfuTmp = cfuTmp.substring(0, cfuTmp.indexOf("/"));
            float averageMark = Float.parseFloat(averageTmp);
            int totalCfu = Integer.parseInt(cfuTmp);
            userData.setTotalCFU(totalCfu);
            userData.setAverageMark(averageMark);

            Log.d("LOGIN_USER_DATA", "Average mark: " + averageTmp);
            Log.d("LOGIN_USER_DATA", "CFU: " + cfuTmp);

            return userData;
        } catch (SocketTimeoutException e) {
            throw e;
        } catch (IOException e){
            Utils.saveBugReport(e, TAG);

            throw e;
        }
    }

    public static SparseArray<String> hasMultiFaculty(UserAuthentication user, Document document) {
        if (user.isFake()) return downloadFacultiesList(user, null);

        Element el1 = document.select("#titolo-menu-principale").first();
        boolean hasMultiFaculty = el1.text().toLowerCase().equals("registrato");

        if (hasMultiFaculty)
            return downloadFacultiesList(user, document);
        else
            return null;
    }

    public static SparseArray<String> downloadFacultiesList(UserAuthentication user, Document document) {
        SparseArray<String> courses = new SparseArray<>();

        if (user.isFake()) {
            courses.put(34829, "Faculty of IT");
            courses.put(19347, "Faculty of Science");
            courses.put(58240, "Faculty of Chemistry");

            return courses;
        }

        Elements els = document.select("table#gu_table_sceltacarriera tbody tr");
        if (!els.isEmpty()) {
            for (Element el : els) {
                String name = el.child(1).text() + " in " + el.child(2).text();
                String relativeUrl = el.select("#gu_toolbar_sceltacarriera a")
                        .attr("href");
                relativeUrl = relativeUrl.substring(relativeUrl.indexOf("?stu_id=") + 8);
                int stu_id = Integer.parseInt(relativeUrl);

                courses.put(stu_id, name);
            }
        }

        return courses;
    }

    public static boolean downloadCertificate(UserAuthentication user, Exam exam, Context context,
                                        NewSessionIdListener listener) throws IOException {
        String urlParameters = exam.examIdToUrl();
        HttpsURLConnection connection = getPage(user, URL_ENROLLED_EXAM_CERTIFICATE,
                urlParameters, false, context, listener);
        InputStream ris = null;

        if (connection != null)
            ris = connection.getInputStream();

        if (ris != null) {
            try {
                File file = exam.getCertificatePath();
                FileOutputStream f = new FileOutputStream(file);

                byte[] buffer = new byte[1024];
                int len;

                while ((len = ris.read(buffer)) > 0)
                    f.write(buffer, 0, len);
                f.close();

                return true;
            } catch (IOException e) {
                return false;
            }
        }

        return false;
    }



    /**************
     * LOADERS ****
     **************/

    public static LoginLoader createLoginLoader(UserAuthentication userToLogin, Activity activity) {
        return new LoginLoader(activity, userToLogin);
    }

    public static EnrollLoader createEnrollLoader(Exam exam, Activity activity,
                                                  EnrollLoader.EnrollCompleteListener enrollCompleteListener,
                                                  EnrollLoader.EnrollUpdatesListener enrollUpdatesListener) {
        return new EnrollLoader(activity, exam)
                .setEnrollCompleteListener(enrollCompleteListener)
                .setEnrollUpdatesListener(enrollUpdatesListener);
    }

    public static UserDataLoader createUserDataLoader(Activity activity) {
        return new UserDataLoader(activity);
    }

    public static CertificateLoader createCertificateLoader(EnrolledExam exam, Activity activity) {
        return new CertificateLoader(activity, exam);
    }
}