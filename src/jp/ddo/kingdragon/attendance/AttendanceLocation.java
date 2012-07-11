package jp.ddo.kingdragon.attendance;

/**
 * 出席データの座標を管理するクラス
 * @author 杉本祐介
 */
public class AttendanceLocation {
    // 変数の宣言
    /**
     * 緯度
     */
    private final double latitude;
    /**
     * 経度
     */
    private final double longitude;
    /**
     * 高度
     */
    private final double altitude;
    /**
     * 精度
     */
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
        this(latitude, longitude, -1.0, accuracy);
    }
    /**
     * 緯度、経度、高度、精度がセットされたインスタンスを生成する
     * @param latitude 緯度
     * @param longitude 経度
     * @param altitude 高度
     * @param accuracy 精度
     */
    public AttendanceLocation(double latitude, double longitude, double altitude, float accuracy) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.accuracy = accuracy;
    }

    // アクセッサ
    /**
     * 緯度を返す
     * @return 緯度
     */
    public double getLatitude() {
        return latitude;
    }
    /**
     * 経度を返す
     * @return 経度
     */
    public double getLongitude() {
        return longitude;
    }
    /**
     * 高度を返す
     * @return 高度 セットされていなければ-1.0
     */
    public double getAltitude() {
        return altitude;
    }
    /**
     * 精度を返す
     * @return 精度 セットされていなければ-1.0f
     */
    public float getAccuracy() {
        return accuracy;
    }

    /**
     * 座標データをCSV形式で出力する
     * @return CSV形式にした座標データ
     */
    public String toCsvRecord() {
        StringBuilder csvRecord = new StringBuilder("\"" + latitude + "\",\"" + longitude + "\"");

        if (accuracy != -1.0f) {
            csvRecord.append(",\"");
            if (altitude != -1.0) {
                csvRecord.append(altitude);
            }
            csvRecord.append("\",\"" + accuracy + "\"");
        }
        else {
            if (altitude != -1.0) {
                csvRecord.append(",\"" + altitude + "\"");
            }
        }

        return csvRecord.toString();
    }
}