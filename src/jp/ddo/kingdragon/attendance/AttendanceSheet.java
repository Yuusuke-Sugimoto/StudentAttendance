package jp.ddo.kingdragon.attendance;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import android.content.res.Resources;

/**
 * 出席リストを管理するクラス
 * @author 杉本祐介
 */
public class AttendanceSheet {
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
     * CSVファイルからシートを生成する
     * @param csvFile CSVファイルのインスタンス
     * @param encode CSVファイルの文字コード
     * @param inResources アプリのリソース
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    public AttendanceSheet(File csvFile, String encode, Resources inResources) throws UnsupportedEncodingException, FileNotFoundException, IOException {
        subject = "";
        time = "";
        attendances = new LinkedHashMap<String, Attendance>();

        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(csvFile), encode));
        boolean isSubjectRecord = false;
        boolean isStudentRecord = false;
        String line;
        while ((line = br.readLine()) != null) {
            String[] splittedLine = line.replace("\"", "").split(",");

            if (isSubjectRecord) {
                subject = splittedLine[0];
                time = splittedLine[1];
                isSubjectRecord = false;
            }
            else if (isStudentRecord) {
                String[] nfcIds;
                if (splittedLine.length == 6) {
                    // NFCのタグのIDが1つ
                    nfcIds = new String[] {splittedLine[5]};
                }
                else if (splittedLine.length > 6) {
                    // NFCタグのIDが複数セットされている場合は配列に直す
                    ArrayList<String> temp = new ArrayList<String>();
                    for (int i = 5; i < splittedLine.length; i++) {
                        temp.add(splittedLine[i]);
                    }
                    nfcIds = temp.toArray(new String[temp.size()]);
                }
                else {
                    // NFCのタグのIDが未登録
                    nfcIds = new String[0];
                }
                int num;
                if (splittedLine[0].length() != 0) {
                    // 連番が設定されている場合
                    try {
                        num = Integer.parseInt(splittedLine[0]);
                    }
                    catch (Exception ex) {
                        num = -1;
                    }
                }
                else {
                    num = -1;
                }
                Attendance mAttendance = new Attendance(new Student(splittedLine[2], num, splittedLine[1],
                                                                    splittedLine[3], splittedLine[4], nfcIds),
                                                        inResources);
                // ID1個ごとにリストに追加する
                if(nfcIds.length > 0) {
                    for(String id : nfcIds) {
                        attendances.put(id, mAttendance);
                    }
                }
                else {
                    attendances.put(splittedLine[2], mAttendance);
                }
            }

            if (splittedLine[0].equals("科目")) {
                isSubjectRecord = true;
            }
            else if (splittedLine[1].equals("所属")) {
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
     * 科目名を返す
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
     * 授業時間を返す
     * @return 授業時間
     */
    public String getTime() {
        return time;
    }
    /**
     * 出席データのリストを返す
     * @return 出席データのリスト
     */
    public ArrayList<Attendance> getAttendanceList() {
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
     * 現在の出席データの数を返す
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
        LinkedHashSet<Attendance> hashSet = new LinkedHashSet<Attendance>(attendances.values());
        OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(csvFile), encode);
        osw.write("\"科目\",\"授業時間\",\"受講者数\"\n");
        osw.write("\"" + subject + "\",\"" + time + "\",\"" + hashSet.size() + "\"\n");
        osw.write("\"\",\"所属\",\"学籍番号\",\"氏名\",\"カナ\"\n");
        for (Attendance mAttendance : hashSet) {
            osw.write(mAttendance.toCsvRecord() + "\n");
        }
        osw.flush();
        osw.close();
    }
}