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
     * 学生数を所属別にまとめたリスト
     */
    private LinkedHashMap<String, StudentCounter> studentCounters;

    // コンストラクタ
    /**
     * 空のシートを生成する
     */
    public AttendanceSheet() {
        subject = "";
        time = "";

        attendancesStudentNo = new LinkedHashMap<String, Attendance>();
        studentCounters = new LinkedHashMap<String, StudentCounter>();
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
        int attendanceNoIndex = -1;
        int classNameIndex = -1;
        int studentNoIndex = -1;
        int studentNameIndex = -1;
        int studentRubyIndex = -1;
        int statusIndex = -1;
        int timeStampIndex = -1;
        int latitudeIndex = -1;
        int longitudeIndex = -1;
        int accuracyIndex = -1;
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
                int attendanceNo = -1;
                if (attendanceNoIndex != -1) {
                    try {
                        attendanceNo = Integer.parseInt(values[attendanceNoIndex]);
                    }
                    catch (NumberFormatException e) {}
                }
                Attendance mAttendance = new Attendance(new Student(values[studentNoIndex], values[classNameIndex],
                                                                    values[studentNameIndex], values[studentRubyIndex],
                                                                    (String[])null),
                                                        attendanceNo, inResources);
                if (statusIndex != -1) {
                    // 出席データが格納されたCSVファイル
                    double latitude = -1.0;
                    double longitude = -1.0;
                    float accuracy = -1.0f;
                    if (latitudeIndex != -1 && values.length >= latitudeIndex + 1) {
                        try {
                            latitude = Double.parseDouble(values[latitudeIndex]);
                        }
                        catch (NumberFormatException e) {}
                    }
                    if (longitudeIndex != -1 && values.length >= longitudeIndex + 1) {
                        try {
                            longitude = Double.parseDouble(values[longitudeIndex]);
                        }
                        catch (NumberFormatException e) {}
                    }
                    if (accuracyIndex != -1 && values.length >= accuracyIndex + 1) {
                        try {
                            accuracy = Float.parseFloat(values[accuracyIndex]);
                        }
                        catch (NumberFormatException e) {}
                    }

                    AttendanceLocation mAttendanceLocation;
                    if (latitude != -1.0 || longitude != -1.0 || accuracy != -1.0f) {
                        mAttendanceLocation = new AttendanceLocation(latitude, longitude, accuracy);
                    }
                    else {
                        mAttendanceLocation = null;
                    }

                    if (values.length >= 11 && values[10].length() != 0) {
                        mAttendance.putExtra(Attendance.PHOTO_PATH, values[10]);
                    }
                    if (values.length >= 12 && values[11].length() != 0) {
                        mAttendance.putExtra(Attendance.MOVIE_PATH, values[11]);
                    }

                    if (values.length >= statusIndex + 1 && values[statusIndex].length() != 0) {
                        if (values[statusIndex].equals(inResources.getString(R.string.attendance))) {
                            mAttendance.setStatus(Attendance.ATTENDANCE, mAttendanceLocation);
                        }
                        else if (values[statusIndex].equals(inResources.getString(R.string.lateness))) {
                            mAttendance.setStatus(Attendance.LATENESS, mAttendanceLocation);
                        }
                        else if (values[statusIndex].equals(inResources.getString(R.string.leave_early))) {
                            mAttendance.setStatus(Attendance.LEAVE_EARLY, mAttendanceLocation);
                        }

                        try {
                            mAttendance.setTimeStamp(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse(values[timeStampIndex]).getTime());
                        }
                        catch (ParseException e) {
                            Log.e("AttendanceSheet", e.getMessage(), e);
                        }
                    }
                }
                add(mAttendance);
            }

            if (values[0].equals("科目")) {
                isSubjectRecord = true;
            }
            else if (values[1].equals("所属")) {
                isStudentRecord = true;
                attendanceNoIndex = 0;
                classNameIndex = 1;
                for (int i = 1; i < values.length; i++) {
                    if (values[i].equals("学籍番号")) {
                        studentNoIndex = i;
                    }
                    else if (values[i].equals("氏名")) {
                        studentNameIndex = i;
                    }
                    else if (values[i].equals("カナ")) {
                        studentRubyIndex = i;
                    }
                    else if (values[i].equals("出席種別")) {
                        statusIndex = i;
                    }
                    else if (values[i].equals("確認日時")) {
                        timeStampIndex = i;
                    }
                    else if (values[i].equals("緯度")) {
                        latitudeIndex = i;
                    }
                    else if (values[i].equals("経度")) {
                        longitudeIndex = i;
                    }
                    else if (values[i].equals("精度")) {
                        accuracyIndex = i;
                    }
                }
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
     * 出席確認者数を返す
     * @return 出席確認者数
     */
    public int getNumOfConfirmedStudents() {
        int retInt = 0;

        for (StudentCounter mCounter : studentCounters.values()) {
            retInt += mCounter.getNumOfConfirmedStudents();
        }

        return retInt;
    }

    /**
     * 出席者数をインクリメントする
     */
    protected void incNumOfAttendance(String className) {
        if (!studentCounters.containsKey(className)) {
            studentCounters.put(className, new StudentCounter(1));
        }
        studentCounters.get(className).incNumOfAttendance();
    }
    /**
     * 出席者数をデクリメントする
     */
    protected void decNumOfAttendance(String className) {
        if (studentCounters.containsKey(className)) {
            studentCounters.get(className).decNumOfAttendance();
        }
    }

    /**
     * 遅刻者数をインクリメントする
     */
    protected void incNumOfLateness(String className) {
        if (!studentCounters.containsKey(className)) {
            studentCounters.put(className, new StudentCounter(1));
        }
        studentCounters.get(className).incNumOfLateness();
    }
    /**
     * 遅刻者数をデクリメントする
     */
    protected void decNumOfLateness(String className) {
        if (studentCounters.containsKey(className)) {
            studentCounters.get(className).decNumOfLateness();
        }
    }

    /**
     * 早退者数をインクリメントする
     */
    protected void incNumOfLeaveEarly(String className) {
        if (!studentCounters.containsKey(className)) {
            studentCounters.put(className, new StudentCounter(1));
        }
        studentCounters.get(className).incNumOfLeaveEarly();
    }
    /**
     * 早退者数をデクリメントする
     */
    protected void decNumOfLeaveEarly(String className) {
        if (studentCounters.containsKey(className)) {
            studentCounters.get(className).decNumOfLeaveEarly();
        }
    }

    /**
     * 出席データをリストに追加する
     * @param inAttendance 出席データ
     */
    public void add(Attendance inAttendance) {
        attendancesStudentNo.put(inAttendance.getStudentNo(), inAttendance);
        inAttendance.setParentSheet(AttendanceSheet.this);

        String className = inAttendance.getClassName();
        if (studentCounters.containsKey(className)) {
            studentCounters.get(className).incNumOfStudents();
        }
        else {
            studentCounters.put(className, new StudentCounter(1));
        }
        switch (inAttendance.getStatus()) {
            case Attendance.ATTENDANCE: {
                incNumOfAttendance(className);

                break;
            }
            case Attendance.LATENESS: {
                incNumOfLateness(className);

                break;
            }
            case Attendance.LEAVE_EARLY: {
                incNumOfLeaveEarly(className);

                break;
            }
        }
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
     * 引数で渡された出席データが存在するかどうかを調べる
     * @param inAttendance 出席データ
     * @return 存在するならばtrue 存在しなければfalse
     */
    public boolean hasAttendance(Attendance inAttendance) {
        return attendancesStudentNo.containsValue(inAttendance);
    }

    /**
     * 出席データの表示用のリストを取得する
     * @return 出席データの表示用のリスト
     */
    public ArrayList<Attendance> getAttendanceList() {
        return new ArrayList<Attendance>(attendancesStudentNo.values());
    }

    /**
     * 現在扱っている所属のリストを取得する
     * @return 所属のリスト
     */
    public ArrayList<String> getClassNames() {
        return new ArrayList<String>(studentCounters.keySet());
    }

    /**
     * 引数で指定した所属の学生数を取得する
     * @param className 所属
     * @return 指定された所属の学生数を格納したインスタンス 指定された所属に対応するデータがなければnull
     */
    public StudentCounter getStudentCounter(String className) {
        return studentCounters.get(className);
    }

    /**
     * 出席データをCSV形式で保存する<br>
     * {@link #saveCsvFile(File, String, boolean, boolean, boolean)}の第3引数以降がfalseのものに同じ。
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
     * 出席データをCSV形式で保存する<br>
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
        ArrayList<String> labels = new ArrayList<String>();
        labels.add("出席番号");
        labels.add("所属");
        labels.add("学籍番号");
        labels.add("氏名");
        labels.add("カナ");
        labels.add("出席種別");
        labels.add("確認日時");
        if (isLatitudeEnabled) {
            labels.add("緯度");
        }
        if (isLongitudeEnabled) {
            labels.add("経度");
        }
        if (isAccuracyEnabled) {
            labels.add("精度");
        }
        writer.writeNext(labels.toArray(new String[labels.size()]));
        for (Attendance mAttendance : attendancesStudentNo.values()) {
            writer.writeNext(mAttendance.getAttendanceData(isLatitudeEnabled, isLongitudeEnabled, isAccuracyEnabled));
        }
        writer.flush();
        writer.close();
    }
}