package jp.ddo.kingdragon.attendance;

import android.app.Application;

import jp.ddo.kingdragon.attendance.student.StudentMaster;

/**
 * 学生マスタ共有用のApplicationクラス
 * @author 杉本祐介
 */
public class CustomApplication extends Application {
    // 変数の宣言
    /** 学生マスタ */
    private StudentMaster master;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    /**
     * 学生マスタをセットする
     * @param master 学生マスタ
     */
    public void setStudentMaster(StudentMaster master) {
        this.master = master;
    }
    /**
     * 学生マスタを取得する
     * @return 学生マスタ
     */
    public StudentMaster getStudentMaster() {
        return master;
    }
}