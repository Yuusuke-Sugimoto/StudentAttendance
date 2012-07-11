package jp.ddo.kingdragon.attendance;

import java.text.SimpleDateFormat;
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
     * 学籍番号を返す
     * @return 学籍番号
     */
    public String getStudentNo() {
        return mStudent.getStudentNo();
    }
    /**
     * 連番を返す
     * @return 連番
     */
    public int getStudentNum() {
        return mStudent.getStudentNum();
    }
    /**
     * 所属を返す
     * @return 所属
     */
    public String getClassName() {
        return mStudent.getClassName();
    }
    /**
     * 氏名を返す
     * @return 氏名
     */
    public String getStudentName() {
        return mStudent.getStudentName();
    }
    /**
     * カナを返す
     * @return カナ
     */
    public String getStudentRuby() {
        return mStudent.getStudentRuby();
    }
    /**
     * NFCタグのIDの配列を返す
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
     * 出席種別を返す
     * @return 出席種別
     */
    public int getStatus() {
        return status;
    }
    /**
     * 出席種別を文字列で返す
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
     * 出席データをCSV形式で出力する
     * @return CSV形式にした出席データ
     */
    public String toCsvRecord() {
        StringBuilder csvRecord = new StringBuilder("\"");

        if (getStudentNum() != -1) {
            csvRecord.append(getStudentNum());
        }
        csvRecord.append("\",\"" + getClassName() + "\",\"" + getStudentNo() + "\",\"" + getStudentName() + "\",\"" + getStudentRuby() + "\"");
        if (status != Attendance.ABSENCE) {
            csvRecord.append(",\"" + getStatusString() + "\"");
            SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            csvRecord.append(",\"" + format.format(new Date(timeStamp)) + "\"");
            if (mAttendanceLocation != null) {
                csvRecord.append("," + mAttendanceLocation.toCsvRecord());
            }
        }

        return csvRecord.toString();
    }
}