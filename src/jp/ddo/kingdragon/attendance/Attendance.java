package jp.ddo.kingdragon.attendance;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.content.res.Resources;

/**
 * 学生データと出席データをまとめるクラス
 * @author 杉本祐介
 */
public class Attendance {
    // 定数の宣言
    private static final int ATTENDANCE  = 0;
    private static final int LATENESS    = 1;
    private static final int LEAVE_EARLY = 2;
    private static final int ABSENCE     = 3;

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
     * strings.xmlから文字列を取得するために使用
     */
    private Resources mResources;

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
        mResources = inResources;
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
            retStr = mResources.getString(R.string.attendance);

            break;
        case Attendance.LATENESS:
            retStr = mResources.getString(R.string.lateness);

            break;
        case Attendance.LEAVE_EARLY:
            retStr = mResources.getString(R.string.leave_early);

            break;
        }

        return retStr;
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