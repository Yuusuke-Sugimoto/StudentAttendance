package jp.ddo.kingdragon.attendance.student;

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

/**
 * 学生リストを管理するクラス
 * @author 杉本祐介
 */
public class StudentSheet implements Serializable {
    // 定数の宣言
    /**
     * シリアルバージョンUID
     */
    private static final long serialVersionUID = -6227987707663359448L;

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
     * 読み取り済みのNFCタグのリスト
     */
    private ArrayList<String> readedNfcIds;
    /**
     * 現在管理している学生データのリスト
     */
    private LinkedHashMap<String, Student> students;

    // コンストラクタ
    /**
     * 空のシートを生成する
     */
    public StudentSheet() {
        subject = "";
        time = "";
        readedNfcIds = new ArrayList<String>();
        students = new LinkedHashMap<String, Student>();
    }
    /**
     * CSVファイルからシートを生成する
     * @param csvFile CSVファイルのインスタンス
     * @param encode CSVファイルの文字コード
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    public StudentSheet(File csvFile, String encode) throws UnsupportedEncodingException, FileNotFoundException, IOException {
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
                    ArrayList<String> tempNfcIds = new ArrayList<String>();
                    for (int i = 5; i < values.length; i++) {
                        tempNfcIds.add(values[i]);
                    }
                    nfcIds = tempNfcIds.toArray(new String[tempNfcIds.size()]);
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
                students.put(values[2], new Student(values[2], num, values[1],
                                                          values[3], values[4], nfcIds));

                // NFCタグを読み取り済みとして追加
                for (String nfcId : nfcIds) {
                    readedNfcIds.add(nfcId);
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
     * 学生データのリストをセットする
     * @param studentData 学生データのリスト
     */
    public void setStudentData(LinkedHashMap<String, Student> studentData) {
        students = studentData;
    }
    /**
     * 学生データのリストを取得する
     * @return 学生データのリスト
     */
    public LinkedHashMap<String, Student> getStudentData() {
        return students;
    }
    /**
     * 読み取り済みのNFCタグのリストをセットする
     * @param readedNfcIds 読み取り済みのNFCタグのリスト
     */
    public void setReadedNfcIds(ArrayList<String> readedNfcIds) {
        this.readedNfcIds = readedNfcIds;
    }
    /**
     * 読み取り済みのNFCタグのリストを取得する
     * @return 読み取り済みのNFCタグのリスト
     */
    public ArrayList<String> getReadedNfcIds() {
        return readedNfcIds;
    }
    /**
     * 学生データの表示用のリストを取得する
     * @return 学生データの表示用のリスト
     */
    public ArrayList<Student> getStudentDisplayData() {
        return new ArrayList<Student>(students.values());
    }

    /**
     * 読み取り済みのNFCタグを追加する
     * @param id 読み取り済みのNFCタグのID
     */
    public void addReadedNfcId(String id) {
        readedNfcIds.add(id);
    }

    /**
     * 読み取り済みのNFCタグを削除する
     * @param id 読み取り済みのNFCタグのID
     */
    public void removeReadedNfcId(String id) {
        readedNfcIds.remove(id);
    }

    /**
     * NFCタグが読み取り済みかどうかを調べる
     * @param id 検索するID
     * @return 読み取り済みであればtrue そうでなければfalse
     */
    public boolean isNfcIdReaded(String id) {
        return readedNfcIds.contains(id);
    }

    /**
     * 学生データを追加する<br />
     * 既に追加されている場合は追加しない。
     * @param inStudent 追加する学生データ
     */
    public void add(Student inStudent) {
        if (!students.containsKey(inStudent.getStudentNo())) {
            inStudent.setStudentNum(students.size() + 1);
            students.put(inStudent.getStudentNo(), inStudent);
        }
    }

    /***
     * 学生データを削除する
     * @param inStudent 削除する学生データ
     */
    public void remove(Student inStudent) {
        students.remove(inStudent.getStudentNo());
    }

    /**
     * 引数で渡された学籍番号を持つ学生データを取得する
     * @param studentNo 学籍番号
     * @return 学生データ
     */
    public Student get(String studentNo) {
        return students.get(studentNo);
    }

    /**
     * 現在の学生データの数を取得する
     * @return 現在の学生データの数
     */
    public int size() {
        return students.size();
    }

    /**
     * 引数で渡された学籍番号をもつ学生データが存在するかどうかを調べる
     * @param studentNo 学籍番号
     * @return 存在したならばtrue 存在しなければfalse
     */
    public boolean hasStudentNo(String studentNo) {
        return students.containsKey(studentNo);
    }

    /**
     * 学生データをCSV形式で保存する
     * @param csvFile 保存先のインスタンス
     * @param encode 書き込む際に使用する文字コード
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    public void saveCsvFile(File csvFile, String encode) throws UnsupportedEncodingException, FileNotFoundException, IOException {
        CSVWriter writer = new CSVWriter(new OutputStreamWriter(new FileOutputStream(csvFile), encode));
        writer.writeNext(new String[] {"科目", "授業時間", "受講者数"});
        writer.writeNext(new String[] {subject, time, String.valueOf(students.size())});
        writer.writeNext(new String[] {"", "所属", "学籍番号", "氏名", "カナ"});
        for (String key : students.keySet()) {
            writer.writeNext(students.get(key).getStudentData());
        }
        writer.flush();
        writer.close();
    }
}