package jp.ddo.kingdragon.attendance.student;

import android.content.res.Resources;
import android.util.Log;

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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import au.com.bytecode.opencsv.CSVParser;
import au.com.bytecode.opencsv.CSVWriter;

import jp.ddo.kingdragon.attendance.R;

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
     * 現在管理している出席データを学籍番号をキーとして格納したリスト
     */
    private LinkedHashMap<String, Attendance> attendancesStudentNo;
    /**
     * 現在管理している出席データをNFCタグをキーとして格納したリスト
     */
    private LinkedHashMap<String, Attendance> attendancesNfcId;

    // コンストラクタ
    /**
     * 空のシートを生成する
     */
    public AttendanceSheet() {
        subject = "";
        time = "";
        attendancesStudentNo = new LinkedHashMap<String, Attendance>();
        attendancesNfcId = new LinkedHashMap<String, Attendance>();
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
                Attendance mAttendance;
                int num = -1;
                if (values[0].length() != 0) {
                    // 連番が設定されている場合
                    try {
                        num = Integer.parseInt(values[0]);
                    }
                    catch (NumberFormatException e) {}
                }

                if (values.length >= 6 && !values[5].matches("[A-Za-z0-9]+")) {
                    // values[5]が正規表現にマッチしなければ出席データが格納されたCSVファイル
                    mAttendance = new Attendance(new Student(values[2], num, values[1],
                                                             values[3], values[4], (String[])null),
                                                 inResources);
                    AttendanceLocation mAttendanceLocation = null;
                    if (values.length >= 8) {
                        double latitude = -1.0;
                        double longitude = -1.0;
                        float accuracy = -1.0f;

                        try {
                            latitude = Double.parseDouble(values[7]);
                        }
                        catch (NumberFormatException e) {}
                        if (values.length >= 9) {
                            try {
                                longitude = Double.parseDouble(values[8]);
                            }
                            catch (NumberFormatException e) {}
                            if (values.length >= 10) {
                                try {
                                    accuracy = Float.parseFloat(values[9]);
                                }
                                catch (NumberFormatException e) {}
                                if (values.length >= 11) {
                                    if (values[10].length() != 0) {
                                        mAttendance.putExtra(Attendance.PHOTO_PATH, values[10]);
                                    }
                                    if (values.length >= 12) {
                                        if (values[11].length() != 0) {
                                            mAttendance.putExtra(Attendance.MOVIE_PATH, values[11]);
                                        }
                                    }
                                }
                            }
                        }
                        mAttendanceLocation = new AttendanceLocation(latitude, longitude, accuracy);
                    }
                    if (values[5].length() != 0) {
                        if (values[5].equals(inResources.getString(R.string.attendance))) {
                            if (mAttendanceLocation != null) {
                                mAttendance.setStatus(Attendance.ATTENDANCE, mAttendanceLocation);
                            }
                            else {
                                mAttendance.setStatus(Attendance.ATTENDANCE);
                            }
                        }
                        else if (values[5].equals(inResources.getString(R.string.lateness))) {
                            if (mAttendanceLocation != null) {
                                mAttendance.setStatus(Attendance.LATENESS, mAttendanceLocation);
                            }
                            else {
                                mAttendance.setStatus(Attendance.LATENESS);
                            }
                        }
                        else if (values[5].equals(inResources.getString(R.string.leave_early))) {
                            if (mAttendanceLocation != null) {
                                mAttendance.setStatus(Attendance.LEAVE_EARLY, mAttendanceLocation);
                            }
                            else {
                                mAttendance.setStatus(Attendance.LEAVE_EARLY);
                            }
                        }

                        try {
                            mAttendance.setTimeStamp(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse(values[6]).getTime());
                        }
                        catch (ParseException e) {
                            Log.e("AttendanceSheet", e.getMessage(), e);
                        }
                    }
                }
                else {
                    String[] nfcIds;
                    if (values.length == 6) {
                        // NFCのタグのIDが1つ
                        if (values[5].length() != 0) {
                            nfcIds = new String[] {values[5]};
                        }
                        else {
                            nfcIds = new String[0];
                        }
                    }
                    else if (values.length > 6) {
                        // NFCタグのIDが複数セットされている場合は配列に直す
                        ArrayList<String> tempNfcIds = new ArrayList<String>();
                        for (int i = 5; i < values.length; i++) {
                            if (values[i].length() != 0) {
                                tempNfcIds.add(values[i]);
                            }
                        }
                        nfcIds = tempNfcIds.toArray(new String[tempNfcIds.size()]);
                    }
                    else {
                        // NFCのタグのIDが未登録
                        nfcIds = new String[0];
                    }
                    mAttendance = new Attendance(new Student(values[2], num, values[1],
                                                             values[3], values[4], nfcIds),
                                                 inResources);
                    // ID1個ごとにリストに追加する
                    if (nfcIds.length > 0) {
                        for (String id : nfcIds) {
                            attendancesNfcId.put(id, mAttendance);
                        }
                    }
                    else {
                        attendancesNfcId.put(values[2], mAttendance);
                    }
                }
                attendancesStudentNo.put(values[2], mAttendance);
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
     * 出席データをリストに追加する
     * @param nfcId NFCタグのID
     * @param mAttendance 出席データ
     */
    public void add(String nfcId, Attendance mAttendance) {
        attendancesStudentNo.put(mAttendance.getStudentNo(), mAttendance);
        attendancesNfcId.put(nfcId, mAttendance);
    }

    /**
     * 引数で渡された学籍番号を持つ出席データを取得する
     * @param studentNo 学籍番号
     * @return 出席データ
     */
    public Attendance getByStudentNo(String studentNo) {
        return attendancesStudentNo.get(studentNo);
    }

    /**
     * 引数で渡されたNFCタグを持つ出席データを取得する
     * @param id NFCタグのID
     * @return 出席データ
     */
    public Attendance getByNfcId(String id) {
        return attendancesNfcId.get(id);
    }

    /**
     * 現在の出席データの数を取得する
     * @return 現在の出席データの数
     */
    public int size() {
        return attendancesStudentNo.size();
    }

    /**
     * 引数で渡された学籍番号をもつ出席データが存在するかどうかを調べる
     * @param studentNo 学籍番号
     * @return 存在するならばtrue 存在しなければfalse
     */
    public boolean hasStudentNo(String studentNo) {
        return attendancesStudentNo.containsKey(studentNo);
    }

    /**
     * 引数で渡されたNFCタグをもつ出席データが存在するかどうかを調べる
     * @param id NFCタグのID
     * @return 存在するならばtrue 存在しなければfalse
     */
    public boolean hasNfcId(String id) {
        return attendancesNfcId.containsKey(id);
    }

    /**
     * 引数で渡された出席データが存在するかどうかを調べる
     * @param mAttendance 出席データ
     * @return 存在するならばtrue 存在しなければfalse
     */
    public boolean hasAttendance(Attendance mAttendance) {
        return attendancesNfcId.containsValue(mAttendance);
    }

    /**
     * 出席データの表示用のリストを取得する
     * @return 出席データの表示用のリスト
     */
    public ArrayList<Attendance> getAttendanceList() {
        return new ArrayList<Attendance>(attendancesStudentNo.values());
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
        saveCsvFile(csvFile, encode, false, false, false);
    }

    /**
     * 出席データをCSV形式で保存する<br />
     * 位置情報を付加する。
     * @param csvFile 保存先のインスタンス
     * @param encode 書き込む際に使用する文字コード
     * @param isLatitudeEnabled 緯度を付加するかどうか
     * @param isLongitudeEnabled 経度を付加するかどうか
     * @param isAccuracyEnabled 精度を付加するかどうか
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    public void saveCsvFile(File csvFile, String encode, boolean isLatitudeEnabled, boolean isLongitudeEnabled,
                            boolean isAccuracyEnabled) throws UnsupportedEncodingException, FileNotFoundException, IOException {
        CSVWriter writer = new CSVWriter(new OutputStreamWriter(new FileOutputStream(csvFile), encode));
        writer.writeNext(new String[] {"科目", "授業時間", "受講者数"});
        writer.writeNext(new String[] {subject, time, String.valueOf(attendancesStudentNo.size())});
        writer.writeNext(new String[] {"", "所属", "学籍番号", "氏名", "カナ"});
        for (Attendance mAttendance : attendancesStudentNo.values()) {
            writer.writeNext(mAttendance.getAttendanceData(isLatitudeEnabled, isLongitudeEnabled, isAccuracyEnabled));
        }
        writer.flush();
        writer.close();
    }
}