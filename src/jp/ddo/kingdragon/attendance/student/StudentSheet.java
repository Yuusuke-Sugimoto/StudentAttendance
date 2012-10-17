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
    /**
     * 元のファイル
     */
    private File baseFile;

    // コレクションの宣言
    /**
     * 現在管理している学生データを学籍番号をキーとして格納したリスト
     */
    private LinkedHashMap<String, Student> studentsStudentNo;
    /**
     * 現在管理している学生データをNFCタグをキーとして格納したリスト
     */
    private LinkedHashMap<String, Student> studentsNfcId;

    // コンストラクタ
    /**
     * 空のシートを生成する
     */
    public StudentSheet() {
        subject = "";
        time = "";
        baseFile = null;
        studentsStudentNo = new LinkedHashMap<String, Student>();
        studentsNfcId = new LinkedHashMap<String, Student>();
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

        baseFile = csvFile;
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
                Student mStudent = new Student(values[2], num, values[1], values[3], values[4], nfcIds);
                studentsStudentNo.put(values[2], mStudent);
                for (String nfcId : nfcIds) {
                    studentsNfcId.put(nfcId, mStudent);
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
     * 元のファイルを取得する
     * @return 元のファイル 展開も保存も行われていない場合はnull
     */
    public File getBaseFile() {
        return baseFile;
    }

    /**
     * 学生データを追加する<br />
     * 既に追加されている場合は追加しない。
     * @param inStudent 学生データ
     */
    public void add(Student inStudent) {
        if (!studentsStudentNo.containsKey(inStudent.getStudentNo())) {
            inStudent.setStudentNum(studentsStudentNo.size() + 1);
            studentsStudentNo.put(inStudent.getStudentNo(), inStudent);
            if (inStudent.getNumOfNfcId() != 0) {
                for (String nfcId : inStudent.getNfcIds()) {
                    studentsNfcId.put(nfcId, inStudent);
                }
            }
        }
    }

    /**
     * NFCタグを登録する<br />
     * 既に登録されている場合は追加しない。
     * @param nfcId NFCタグ
     * @param inStudent 学生データ
     */
    public void addNfcId(String nfcId, Student inStudent) {
        if (!studentsNfcId.containsKey(nfcId)) {
            studentsNfcId.put(nfcId, inStudent);
        }
    }

    /***
     * 学生データを削除する
     * @param inStudent 学生データ
     */
    public void remove(Student inStudent) {
        studentsStudentNo.remove(inStudent.getStudentNo());
        for (String nfcId : inStudent.getNfcIds()) {
            studentsNfcId.remove(nfcId);
        }
    }

    /**
     * NFCタグの登録を削除する
     * @param nfcId NFCタグ
     */
    public void removeNfcId(String nfcId) {
        studentsNfcId.remove(nfcId);
    }

    /**
     * 引数で渡された学籍番号を持つ学生データを取得する
     * @param studentNo 学籍番号
     * @return 学生データ
     */
    public Student getByStudentNo(String studentNo) {
        return studentsStudentNo.get(studentNo);
    }

    /**
     * 引数で渡されたNFCタグを持つ学生データを取得する
     * @param id NFCタグのID
     * @return 学生データ
     */
    public Student getByNfcId(String id) {
        return studentsNfcId.get(id);
    }

    /**
     * 現在の学生データの数を取得する
     * @return 現在の学生データの数
     */
    public int size() {
        return studentsStudentNo.size();
    }

    /**
     * 引数で渡された学籍番号をもつ学生データが存在するかどうかを調べる
     * @param studentNo 学籍番号
     * @return 存在するならばtrue 存在しなければfalse
     */
    public boolean hasStudentNo(String studentNo) {
        return studentsStudentNo.containsKey(studentNo);
    }

    /**
     * 引数で渡されたNFCタグをもつ学生データが存在するかどうかを調べる
     * @param id NFCタグのID
     * @return 存在するならばtrue 存在しなければfalse
     */
    public boolean hasNfcId(String id) {
        return studentsNfcId.containsKey(id);
    }

    /**
     * 学生データの表示用のリストを取得する
     * @return 学生データの表示用のリスト
     */
    public ArrayList<Student> getStudentList() {
        return new ArrayList<Student>(studentsStudentNo.values());
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
        baseFile = csvFile;
        CSVWriter writer = new CSVWriter(new OutputStreamWriter(new FileOutputStream(csvFile), encode));
        writer.writeNext(new String[] {"科目", "授業時間", "受講者数"});
        writer.writeNext(new String[] {subject, time, String.valueOf(studentsStudentNo.size())});
        writer.writeNext(new String[] {"", "所属", "学籍番号", "氏名", "カナ"});
        for (String key : studentsStudentNo.keySet()) {
            writer.writeNext(studentsStudentNo.get(key).getStudentData());
        }
        writer.flush();
        writer.close();
    }
}