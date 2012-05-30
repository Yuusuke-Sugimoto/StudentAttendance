package jp.ddo.kingdragon;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Set;

/***
 * 1つのシートを管理するクラス
 *
 * @author 杉本祐介
 */
public class Sheet {
    // 変数の宣言
    /***
     * 科目
     */
    private String subject;
    /***
     * 授業時間
     */
    private String time;
    /***
     * 現在管理している学生データのリスト
     */
    private LinkedHashMap<String, Student> students;

    // コンストラクタ
    /***
     * 空のシートを生成する
     */
    public Sheet() {
        subject = "";
        time = "";
        students = new LinkedHashMap<String, Student>();
    }
    /***
     * CSVファイルからシートを生成する
     *
     * @param csvFile
     *     CSVファイルのインスタンス
     * @param encode
     *     CSVファイルの文字コード
     */
    public Sheet(File csvFile, String encode) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(csvFile), encode));
            students = new LinkedHashMap<String, Student>();
            boolean isSubjectRecord = false;
            boolean isStudentRecord = false;
            String line;
            while((line = br.readLine()) != null) {
                String[] splittedLine = line.split(",");

                if(isSubjectRecord) {
                    subject = splittedLine[0];
                    time = splittedLine[1];
                    isSubjectRecord = false;
                }
                else if(isStudentRecord) {
                    String[] nfcIds;
                    if(splittedLine.length == 6) {
                        // NFCのタグのIDが1つ
                        nfcIds = new String[] {splittedLine[5]};
                    }
                    else if(splittedLine.length > 6) {
                        // NFCタグのIDが複数セットされている場合は配列に直す
                        ArrayList<String> temp = new ArrayList<String>();
                        for(int i = 5; i < splittedLine.length; i++) {
                            temp.add(splittedLine[i]);
                        }
                        nfcIds = temp.toArray(new String[0]);
                    }
                    else {
                        // NFCのタグのIDが未登録
                        nfcIds = new String[0];
                    }
                    int num;
                    if(splittedLine[0].length() != 0) {
                        // 連番が設定されている場合
                        try {
                            num = Integer.parseInt(splittedLine[0]);
                        }
                        catch(Exception ex) {
                            num = -1;
                        }
                    }
                    else {
                        num = -1;
                    }
                    students.put(splittedLine[2], new Student(splittedLine[2], num, splittedLine[1],
                                                              splittedLine[3], splittedLine[4], nfcIds));
                }

                if(splittedLine[0].equals("科目")) {
                    isSubjectRecord = true;
                }
                else if(splittedLine[1].equals("所属")) {
                    isStudentRecord = true;
                }
            }
            br.close();
        }
        catch(Exception e) {
            Log.e("openCsvFile", e.getMessage(), e);
        }
    }

    // アクセッサ
    /***
     * 科目名をセットする
     *
     * @param subject
     *     科目名
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }
    /***
     * 授業時間をセットする
     *
     * @param time
     *     授業時間
     */
    public void setTime(String time) {
        this.time = time;
    }

    /***
     * 学生データを追加する
     *
     * @param inStudent
     *     追加する学生データ
     */
    public void add(Student inStudent) {
        inStudent.setNum(students.size() + 1);
        students.put(inStudent.getStudentNo(), inStudent);
    }

    /***
     * 引数で渡された学籍番号を持つ学生データを取得する
     *
     * @param studentNo
     *     学籍番号
     * @return 学生データ
     */
    public Student get(String studentNo) {
        return(students.get(studentNo));
    }

    /***
     * 現在の学生データの数を返す
     *
     * @return 現在の学生データの数
     */
    public int size() {
        return(students.size());
    }

    /***
     * 引数で渡された学籍番号をもつ学生データが存在するかどうかを調べる
     *
     * @param studentNo
     *     学籍番号
     * @return 存在したならばtrue、存在しなければfalse
     */
    public boolean searchByStudentNo(String studentNo) {
        return(students.containsKey(studentNo));
    }

    /***
     * 学生データをCSV形式で保存する
     *
     * @param csvFile
     *     保存先のインスタンス
     * @param encode
     *     書き込む際に使用する文字コード
     */
    public void saveCsvFile(File csvFile, String encode) {
        try {
            OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(csvFile), encode);
            osw.write("科目,授業時間,受講者数\n");
            osw.write(subject + "," + time + "," + students.size() + "\n");
            osw.write(",所属,学籍番号,氏名,カナ\n");
            Set<String> keySet = students.keySet();
            for(String key : keySet) {
                osw.write(students.get(key).toCsvRecord() + "\n");
            }
            osw.flush();
            osw.close();
        }
        catch(Exception e) {
            Log.e("saveCsvFile", e.getMessage(), e);
        }
    }
}