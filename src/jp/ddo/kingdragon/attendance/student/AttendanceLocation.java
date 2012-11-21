package jp.ddo.kingdragon.attendance.student;

import java.io.Serializable;

/**
 * 出席データの位置情報を管理するクラス
 * @author 杉本祐介
 */
public class AttendanceLocation implements Serializable {
    // 定数の宣言
    /** シリアルバージョンUID */
    private static final long serialVersionUID = -5939174616940873761L;

    // 変数の宣言
    /** 緯度 */
    private final double latitude;
    /** 経度 */
    private final double longitude;
    /** 精度 */
    private final float accuracy;

    // コンストラクタ
    /**
     * 緯度、経度がセットされたインスタンスを生成する
     * @param latitude 緯度
     * @param longitude 経度
     */
    public AttendanceLocation(double latitude, double longitude) {
        this(latitude, longitude, -1.0f);
    }
    /**
     * 緯度、経度、精度がセットされたインスタンスを生成する
     * @param latitude 緯度
     * @param longitude 経度
     * @param accuracy 精度
     */
    public AttendanceLocation(double latitude, double longitude, float accuracy) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
    }

    // アクセッサ
    /**
     * 緯度を取得する
     * @return 緯度
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * 経度を取得する
     * @return 経度
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * 精度を取得する
     * @return 精度 セットされていなければ-1.0f
     */
    public float getAccuracy() {
        return accuracy;
    }
}