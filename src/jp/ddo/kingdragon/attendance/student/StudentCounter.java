package jp.ddo.kingdragon.attendance.student;

import java.io.Serializable;

/**
 * 学生数を管理するクラス
 * @author 杉本祐介
 */
public class StudentCounter implements Serializable {
    // 定数の宣言
    /**
     * シリアルバージョンUID
     */
    private static final long serialVersionUID = 7273000972747048791L;

    // 変数の宣言
    /**
     * 学生数
     */
    private int numOfStudents;
    /**
     * 出席者数
     */
    private int numOfAttendance;
    /**
     * 遅刻者数
     */
    private int numOfLateness;
    /**
     * 早退者数
     */
    private int numOfLeaveEarly;

    // コンストラクタ
    /**
     * 学生数を指定してインスタンスを生成する<br />
     * {@link #StudentCounter(int, int, int, int)}の第2引数以降が0のものに同じ。
     * @param numOfStudents 学生数
     */
    public StudentCounter(int numOfStudents) {
        this(numOfStudents, 0, 0, 0);
    }
    /**
     * 学生数、出席者数、遅刻者数および早退者数を指定してインスタンスを生成する
     * @param numOfStudents 学生数
     * @param numOfAttendance 出席者数
     * @param numOfLateness 遅刻者数
     * @param numOfLeaveEarly 早退者数
     */
    public StudentCounter(int numOfStudents, int numOfAttendance, int numOfLateness, int numOfLeaveEarly) {
        this.numOfStudents   = numOfStudents;
        this.numOfAttendance = numOfAttendance;
        this.numOfLateness   = numOfLateness;
        this.numOfLeaveEarly = numOfLeaveEarly;
    }

    // アクセッサ
    /**
     * 学生数をインクリメントする
     */
    protected void incNumOfStudents() {
        numOfStudents++;
    }
    /**
     * 学生数を取得する
     * @return 学生数
     */
    public int getNumOfStudents() {
        return numOfStudents;
    }

    /**
     * 出席確認者数を返す
     * @return 出席確認者数
     */
    public int getNumOfConfirmedStudents() {
        return numOfAttendance + numOfLateness + numOfLeaveEarly;
    }

    /**
     * 出席者数をインクリメントする
     */
    protected void incNumOfAttendance() {
        numOfAttendance++;
    }
    /**
     * 出席者数をデクリメントする
     */
    protected void decNumOfAttendance() {
        numOfAttendance--;
    }
    /**
     * 出席者数を取得する
     * @return 出席者数
     */
    public int getNumOfAttendance() {
        return numOfAttendance;
    }

    /**
     * 遅刻者数をインクリメントする
     */
    protected void incNumOfLateness() {
        numOfLateness++;
    }
    /**
     * 遅刻者数をデクリメントする
     */
    protected void decNumOfLateness() {
        numOfLateness--;
    }
    /**
     * 遅刻者数を取得する
     * @return 遅刻者数
     */
    public int getNumOfLateness() {
        return numOfLateness;
    }

    /**
     * 早退者数をインクリメントする
     */
    protected void incNumOfLeaveEarly() {
        numOfLeaveEarly++;
    }
    /**
     * 早退者数をデクリメントする
     */
    protected void decNumOfLeaveEarly() {
        numOfLeaveEarly--;
    }
    /**
     * 早退者数を取得する
     * @return 早退者数
     */
    public int getNumOfLeaveEarly() {
        return numOfLeaveEarly;
    }
}