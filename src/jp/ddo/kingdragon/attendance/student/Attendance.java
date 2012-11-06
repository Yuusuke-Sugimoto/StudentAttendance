package jp.ddo.kingdragon.attendance.student;

import android.content.res.Resources;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import jp.ddo.kingdragon.attendance.R;

/**
 * 学生データと出席データをまとめるクラス
 * @author 杉本祐介
 */
public class Attendance implements Serializable {
    // 定数の宣言
    /**
     * シリアルバージョンUID
     */
    private static final long serialVersionUID = 6607703008569368040L;
    // 出席種別
    public static final int ATTENDANCE  = 0;
    public static final int LATENESS    = 1;
    public static final int LEAVE_EARLY = 2;
    public static final int ABSENCE     = 3;
    // 各情報用
    public static final String PHOTO_PATH = "PhotoPath";
    public static final String MOVIE_PATH = "MoviePath";

    // 変数の宣言
    /**
     * 学生データ
     */
    private final Student mStudent;
    /**
     * 出席番号
     */
    private int attendanceNo;
    /**
     * 出席種別
     */
    private int status;
    /**
     * 更新した日時
     */
    private long timeStamp;
    /**
     * 座標
     */
    private AttendanceLocation mAttendanceLocation;
    /**
     * その他情報
     */
    private HashMap<String, String> extras;

    /**
     * "出席"の文字列表現
     */
    private String attendanceString;
    /**
     * "遅刻"の文字列表現
     */
    private String latenessString;
    /**
     * "早退"の文字列表現
     */
    private String leaveEarlyString;

    // コンストラクタ
    /**
     * 学生の出席データを管理するインスタンスを作成する
     * @param inStudent 学生データ
     * @param inResources アプリのリソース
     */
    public Attendance(Student inStudent, Resources inResources) {
        this(inStudent, -1, inResources);
    }
    /**
     * 出席番号を指定してインスタンスを作成する
     * @param inStudent 学生データ
     * @param attendanceNo 出席番号
     * @param inResources アプリのリソース
     */
    public Attendance(Student inStudent, int attendanceNo, Resources inResources) {
        mStudent = inStudent;
        this.attendanceNo = attendanceNo;
        status = Attendance.ABSENCE;
        timeStamp = -1;
        extras = new HashMap<String, String>();

        attendanceString = inResources.getString(R.string.attendance);
        latenessString   = inResources.getString(R.string.lateness);
        leaveEarlyString = inResources.getString(R.string.leave_early);
    }

    @Override
    public boolean equals(Object o) {
        boolean retBool = false;

        if (o instanceof Attendance) {
            Attendance a = (Attendance)o;
            retBool = mStudent.equals(a.mStudent);
        }

        return retBool;
    }

    @Override
    public int hashCode() {
        return mStudent.hashCode();
    }

    // アクセッサ
    /**
     * 学籍番号を取得する
     * @return 学籍番号
     */
    public String getStudentNo() {
        return mStudent.getStudentNo();
    }

    /**
     * 出席番号を変更する
     * @param attendanceNo 出席番号
     */
    public void setAttendanceNo(int attendanceNo) {
        this.attendanceNo = attendanceNo;
    }
    /**
     * 出席番号を取得する
     * @return 出席番号
     */
    public int getAttendanceNo() {
        return attendanceNo;
    }

    /**
     * 所属を取得する
     * @return 所属
     */
    public String getClassName() {
        return mStudent.getClassName();
    }

    /**
     * 氏名を取得する
     * @return 氏名
     */
    public String getStudentName() {
        return mStudent.getStudentName();
    }

    /**
     * カナを取得する
     * @return カナ
     */
    public String getStudentRuby() {
        return mStudent.getStudentRuby();
    }

    /**
     * 出席種別を変更する
     * @param status 出席種別
     */
    public void setStatus(int status) {
        setStatus(status, null);
    }
    /**
     * 出席種別を変更する
     * @param status 出席種別
     * @param inAttendanceLocation 座標
     */
    public void setStatus(int status, AttendanceLocation inAttendanceLocation) {
        if (status >= Attendance.ATTENDANCE && status <= Attendance.ABSENCE) {
            this.status = status;
            setTimeStamp(System.currentTimeMillis());
            mAttendanceLocation = inAttendanceLocation;
        }
        else {
            throw new IllegalArgumentException("setStatus : 引数の値が正しくありません。");
        }
    }
    /**
     * 出席種別を取得する
     * @return 出席種別
     */
    public int getStatus() {
        return status;
    }
    /**
     * 出席種別を文字列で取得する
     * @return 出席種別を文字列にしたもの
     */
    public String getStatusString() {
        String retStr = "";

        switch (status) {
            case Attendance.ATTENDANCE: {
                retStr = attendanceString;

                break;
            }
            case Attendance.LATENESS: {
                retStr = latenessString;

                break;
            }
            case Attendance.LEAVE_EARLY: {
                retStr = leaveEarlyString;

                break;
            }
        }

        return retStr;
    }

    /**
     * 更新日時をセットする
     * @param timeStamp 更新日時
     */
    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    /**
     * 更新日時を取得する
     * @return 更新日時
     */
    public long getTimeStamp() {
        return timeStamp;
    }

    /**
     * 緯度を取得する
     * @return 緯度
     */
    public double getLatitude() {
        double latitude = -1.0;
        if (mAttendanceLocation != null) {
            latitude = mAttendanceLocation.getLatitude();
        }

        return latitude;
    }

    /**
     * 経度を取得する
     * @return 経度
     */
    public double getLongitude() {
        double longitude = -1.0;
        if (mAttendanceLocation != null) {
            longitude = mAttendanceLocation.getLongitude();
        }

        return longitude;
    }

    /**
     * 精度を取得する
     * @return 精度
     */
    public float getAccuracy() {
        float accuracy = -1.0f;
        if (mAttendanceLocation != null) {
            accuracy = mAttendanceLocation.getAccuracy();
        }

        return accuracy;
    }

    /**
     * 情報をセットする
     * @param key キー
     * @param value 値
     */
    public void putExtra(String key, String value) {
        extras.put(key, value);
    }
    /**
     * 情報を取得する
     * @param key キー
     * @param defaultValue デフォルト値
     * @return キーに対応する値が存在すればその値 存在しなければデフォルト値
     */
    public String getExtra(String key, String defaultValue) {
        String retValue;
        if (extras.containsKey(key)) {
            retValue = extras.get(key);
        }
        else {
            retValue = defaultValue;
        }

        return retValue;
    }

    /**
     * 出席データの内容を配列で取得する
     * @return 出席データの内容を配列に格納したもの
     */
    public String[] getAttendanceData() {
        return getAttendanceData(false, false, false);
    }

    /**
     * 出席データの内容を配列で取得する<br />
     * 位置情報を付加する。
     * @param isLatitudeEnabled 緯度を付加するかどうか
     * @param isLongitudeEnabled 経度を付加するかどうか
     * @param isAccuracyEnabled 精度を付加するかどうか
     * @return 出席データの内容を配列に格納したもの
     */
    public String[] getAttendanceData(boolean isLatitudeEnabled, boolean isLongitudeEnabled, boolean isAccuracyEnabled) {
        ArrayList<String> attendanceData = new ArrayList<String>();
        if (getAttendanceNo() != -1) {
            attendanceData.add(String.valueOf(getAttendanceNo()));
        }
        else {
            attendanceData.add("");
        }
        attendanceData.add(getClassName());
        attendanceData.add(getStudentNo());
        attendanceData.add(getStudentName());
        attendanceData.add(getStudentRuby());
        if (status != Attendance.ABSENCE) {
            attendanceData.add(getStatusString());
            SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            attendanceData.add(format.format(new Date(timeStamp)));
            if (isLatitudeEnabled) {
                attendanceData.add(String.valueOf(mAttendanceLocation.getLatitude()));
            }
            if (isLongitudeEnabled) {
                attendanceData.add(String.valueOf(mAttendanceLocation.getLongitude()));
            }
            if (isAccuracyEnabled) {
                attendanceData.add(String.valueOf(mAttendanceLocation.getAccuracy()));
            }
            if (extras.containsKey(Attendance.PHOTO_PATH)) {
                attendanceData.add(extras.get(Attendance.PHOTO_PATH));
            }
            else if (extras.containsKey(Attendance.MOVIE_PATH)) {
                attendanceData.add("");
            }
            if (extras.containsKey(Attendance.MOVIE_PATH)) {
                attendanceData.add(extras.get(Attendance.MOVIE_PATH));
            }
        }

        return attendanceData.toArray(new String[attendanceData.size()]);
    }
}