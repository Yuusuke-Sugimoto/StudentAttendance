package jp.ddo.kingdragon.attendance.student;

import android.content.res.Resources;

import au.com.bytecode.opencsv.CSVParser;
import au.com.bytecode.opencsv.CSVWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

/**
 * 出席リストを管理するクラス
 * @author 杉本祐介
 */
public class AttendanceSheet implements Serializable {
    // 定数の宣言
    /**
     * シリアルバージョンUID
     */
    private static final long serialVersionUID = 3500974580905601302L;

    // 変数の宣言
    /**
     * 科目
     */
    private String subject;
    /**
     * 授業時間
     */
    private String time;

    // コレクションの宣言
    /**
     * 現在管理している出席データのリスト
     */
    private LinkedHashMap<String, Attendance> attendances;

    // コンストラクタ
    /**
     * 空のシートを生成する
     */
    public AttendanceSheet() {
        subject = "";
        time = "";
        attendances = new LinkedHashMap<String, Attendance>();
    }
    /**
     * CSVファイルからシートを生成する
     * @param csvFile CSVファイルのインスタンス
     * @param encode CSVファイルの文字コード
     * @param inResources アプリのリソース
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    public AttendanceSheet(File csvFile, String encode, Resources inResources) throws UnsupportedEncodingException, FileNotFoundException, IOException {
        this();

        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(csvFile), encode));
        CSVParser parser = new CSVParser();
        boolean isSubjectRecord = false;
        boolean isStudentRecord = false;
        String line;
        while ((line = br.readLine()) != null) {
            StringBuilder rawLine = new StringBuilder(line);
            while (rawLine.charAt(rawLine.length() - 1) == ',') {
                rawLine.deleteCharAt(rawLine.length() - 1);
            }
            String[] values = parser.parseLine(rawLine.toString());

            if (isSubjectRecord) {
                subject = values[0];
                time = values[1];
                isSubjectRecord = false;
            }
            else if (isStudentRecord) {
                String[] nfcIds;
                if (values.length == 6) {
                    // NFCのタグのIDが1つ
                    nfcIds = new String[] {values[5]};
                }
                else if (values.length > 6) {
                    // NFCタグのIDが複数セットされている場合は配列に直す
                    ArrayList<String> temp = new ArrayList<String>();
                    for (int i = 5; i < values.length; i++) {
                        temp.add(values[i]);
                    }
                    nfcIds = temp.toArray(new String[temp.size()]);
                }
                else {
                    // NFCのタグのIDが未登録
                    nfcIds = new String[0];
                }
                int num;
                if (values[0].length() != 0) {
                    // 連番が設定されている場合
                    try {
                        num = Integer.parseInt(values[0]);
                    }
                    catch (Exception ex) {
                        num = -1;
                    }
                }
                else {
                    num = -1;
                }
                Attendance mAttendance = new Attendance(new Student(values[2], num, values[1],
                                                                    values[3], values[4], nfcIds),
                                                        inResources);
                // ID1個ごとにリストに追加する
                if (nfcIds.length > 0) {
                    for (String id : nfcIds) {
                        attendances.put(id, mAttendance);
                    }
                }
                else {
                    attendances.put(values[2], mAttendance);
                }
            }

            if (values[0].equals("科目")) {
                isSubjectRecord = true;
            }
            else if (values[1].equals("所属")) {
                isStudentRecord = true;
            }
        }
        br.close();
    }

    // アクセッサ
    /**
     * 科目名をセットする
     * @param subject 科目名
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }
    /**
     * 科目名を取得する
     * @return 科目名
     */
    public String getSubject() {
        return subject;
    }
    /**
     * 授業時間をセットする
     * @param time 授業時間
     */
    public void setTime(String time) {
        this.time = time;
    }
    /**
     * 授業時間を取得する
     * @return 授業時間
     */
    public String getTime() {
        return time;
    }
    /**
     * 出席データのリストをセットする
     * @param attendanceData 出席データのリスト
     */
    public void setAttendanceData(LinkedHashMap<String, Attendance> attendanceData) {
        attendances = attendanceData;
    }
    /**
     * 出席データのリストを取得する
     * @return 出席データのリスト
     */
    public LinkedHashMap<String, Attendance> getAttendanceData() {
        return attendances;
    }
    /**
     * 出席データの表示用のリストを取得する
     * @return 出席データの表示用のリスト
     */
    public ArrayList<Attendance> getAttendanceDisplayData() {
        LinkedHashSet<Attendance> hashSet = new LinkedHashSet<Attendance>(attendances.values());

        return new ArrayList<Attendance>(hashSet);
    }

    /**
     * 引数で渡されたNFCタグを持つ出席データを取得する
     * @param id NFCタグのID
     * @return 出席データ
     */
    public Attendance get(String id) {
        return attendances.get(id);
    }

    /**
     * 現在の出席データの数を取得する
     * @return 現在の出席データの数
     */
    public int size() {
        LinkedHashSet<Attendance> hashSet = new LinkedHashSet<Attendance>(attendances.values());

        return hashSet.size();
    }

    /**
     * 引数で渡されたNFCタグをもつ出席データが存在するかどうかを調べる
     * @param id NFCタグのID
     * @return 存在したならばtrue、存在しなければfalse
     */
    public boolean hasNfcId(String id) {
        return attendances.containsKey(id);
    }

    /**
     * 出席データをCSV形式で保存する
     * @param csvFile 保存先のインスタンス
     * @param encode 書き込む際に使用する文字コード
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    public void saveCsvFile(File csvFile, String encode) throws UnsupportedEncodingException, FileNotFoundException, IOException {
        saveCsvFile(csvFile, encode, false, false, false, false);
    }

    /**
     * 出席データをCSV形式で保存する<br />
     * 位置情報を付加する。
     * @param csvFile 保存先のインスタンス
     * @param encode 書き込む際に使用する文字コード
     * @param isLatitudeEnabled 緯度を付加するかどうか
     * @param isLongitudeEnabled 経度を付加するかどうか
     * @param isAltitudeEnabled 高度を付加するかどうか
     * @param isAccuracyEnabled 精度を付加するかどうか
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    public void saveCsvFile(File csvFile, String encode, boolean isLatitudeEnabled, boolean isLongitudeEnabled, boolean isAltitudeEnabled,
                            boolean isAccuracyEnabled) throws UnsupportedEncodingException, FileNotFoundException, IOException {
        LinkedHashSet<Attendance> hashSet = new LinkedHashSet<Attendance>(attendances.values());
        CSVWriter writer = new CSVWriter(new OutputStreamWriter(new FileOutputStream(csvFile), encode));
        writer.writeNext(new String[] {"科目", "授業時間", "受講者数"});
        writer.writeNext(new String[] {subject, time, String.valueOf(hashSet.size())});
        writer.writeNext(new String[] {"", "所属", "学籍番号", "氏名", "カナ"});
        for (Attendance mAttendance : hashSet) {
            writer.writeNext(mAttendance.getAttendanceData(isLatitudeEnabled, isLongitudeEnabled, isAltitudeEnabled, isAccuracyEnabled));
        }
        writer.flush();
        writer.close();
    }
}