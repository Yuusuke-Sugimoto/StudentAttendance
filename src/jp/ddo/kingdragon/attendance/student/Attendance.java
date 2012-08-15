package jp.ddo.kingdragon.attendance.student;

import android.content.res.Resources;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

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
    private static final long serialVersionUID = -139773677012287047L;
    /**
     * 出席
     */
    public static final int ATTENDANCE  = 0;
    /**
     * 遅刻
     */
    public static final int LATENESS    = 1;
    /**
     * 早退
     */
    public static final int LEAVE_EARLY = 2;
    /**
     * 欠席
     */
    public static final int ABSENCE     = 3;

    // 変数の宣言
    /**
     * 学生データ
     */
    private final Student mStudent;
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
        mStudent = inStudent;
        status = Attendance.ABSENCE;
        timeStamp = -1;

        attendanceString = inResources.getString(R.string.attendance);
        latenessString   = inResources.getString(R.string.lateness);
        leaveEarlyString = inResources.getString(R.string.leave_early);
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
     * 連番を変更する
     * @param studentNum 連番
     */
    public void setStudentNum(int studentNum) {
        mStudent.setStudentNum(studentNum);
    }
    /**
     * 連番を取得する
     * @return 連番
     */
    public int getStudentNum() {
        return mStudent.getStudentNum();
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
     * NFCタグのIDの配列を取得する
     * @return NFCタグの配列
     */
    public String[] getNfcIds() {
        return mStudent.getNfcIds();
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
            timeStamp = System.currentTimeMillis();
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
        case Attendance.ATTENDANCE:
            retStr = attendanceString;

            break;
        case Attendance.LATENESS:
            retStr = latenessString;

            break;
        case Attendance.LEAVE_EARLY:
            retStr = leaveEarlyString;

            break;
        }

        return retStr;
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
     * 高度を取得する
     * @return 高度
     */
    public double getAltitude() {
        double altitude = -1.0;
        if (mAttendanceLocation != null) {
            altitude = mAttendanceLocation.getAltitude();
        }

        return altitude;
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
     * 出席データの内容を配列で取得する
     * @return 出席データの内容を配列に格納したもの
     */
    public String[] getAttendanceData() {
        return getAttendanceData(false, false, false, false);
    }

    /**
     * 出席データの内容を配列で取得する<br />
     * 位置情報を付加する。
     * @return 出席データの内容を配列に格納したもの
     */
    public String[] getAttendanceData(boolean isLatitudeEnabled, boolean isLongitudeEnabled, boolean isAltitudeEnabled, boolean isAccuracyEnabled) {
        ArrayList<String> attendanceData = new ArrayList<String>();
        if (getStudentNum() != -1) {
            attendanceData.add(String.valueOf(getStudentNum()));
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
            if (isAltitudeEnabled) {
                attendanceData.add(String.valueOf(mAttendanceLocation.getAltitude()));
            }
            if (isAccuracyEnabled) {
                attendanceData.add(String.valueOf(mAttendanceLocation.getAccuracy()));
            }
        }

        return attendanceData.toArray(new String[attendanceData.size()]);
    }
}