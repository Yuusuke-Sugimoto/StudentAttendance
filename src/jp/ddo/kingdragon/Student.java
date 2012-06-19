package jp.ddo.kingdragon;

import java.util.ArrayList;

/**
 * 学生1人分のデータを管理するクラス
 * @author 杉本祐介
 */
public class Student {
    // 変数の宣言
    /**
     * 学籍番号
     */
    private String studentNo;
    /**
     * 連番
     */
    private int studentNum;
    /**
     * 所属
     */
    private String className;
    /**
     * 氏名
     */
    private String studentName;
    /**
     * カナ
     */
    private String studentRuby;

    // コレクションの宣言
    /**
     * NFCタグのIDのリスト
     */
    private ArrayList<String> nfcIds;

    // コンストラクタ
    /**
     * 学籍番号およびNFCタグのIDが未設定のインスタンスを生成する
     */
    public Student() {
        this("");
    }
    /**
     * 学籍番号がセットされたインスタンスを生成する
     * @param studentNo 学籍番号
     */
    public Student(String studentNo) {
        this(studentNo, new String[0]);
    }
    /**
     * 学籍番号とNFCタグのIDがセットされたインスタンスを生成する
     * @param studentNo 学籍番号
     * @param nfcIds NFCタグのID(複数可)
     */
    public Student(String studentNo, String... nfcIds) {
        this(studentNo, -1, "", "", "", nfcIds);
    }
    /**
     * 学籍番号、連番、所属、氏名、カナ、NFCタグのIDがセットされたインスタンスを生成する
     * @param studentNo 学籍番号
     * @param num 連番
     * @param className 所属
     * @param name 氏名
     * @param ruby カナ
     * @param nfcIds NFCタグのID(複数可)
     */
    public Student(String studentNo, int num, String className, String name, String ruby, String... nfcIds) {
        this.studentNo = studentNo;
        this.studentNum = num;
        this.className = className;
        this.studentName = name;
        this.studentRuby = ruby;
        this.nfcIds = new ArrayList<String>();
        if (nfcIds != null && nfcIds.length != 0) {
            for (String nfcId : nfcIds) {
                this.nfcIds.add(nfcId);
            }
        }
    }

    // アクセッサ
    /**
     * 学籍番号をセットする
     * @param studentNo 学籍番号
     */
    public void setStudentNo(String studentNo) {
        this.studentNo = studentNo;
    }
    /**
     * 学籍番号を返す
     * @return 学籍番号
     */
    public String getStudentNo() {
        return studentNo;
    }
    /**
     * 連番をセットする
     * @param studentNum 連番
     */
    public void setStudentNum(int studentNum) {
        this.studentNum = studentNum;
    }
    /**
     * 連番を返す
     * @return 連番
     */
    public int getStudentNum() {
        return studentNum;
    }
    /**
     * 所属をセットする
     * @param className 所属
     */
    public void setClassName(String className) {
        this.className = className;
    }
    /**
     * 所属を返す
     * @return 所属
     */
    public String getClassName() {
        return className;
    }
    /**
     * 氏名をセットする
     * @param studentName 氏名
     */
    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }
    /**
     * 氏名を返す
     * @return 氏名
     */
    public String getStudentName() {
        return studentName;
    }
    /**
     * カナをセットする
     * @param studentRuby カナ
     */
    public void setStudentRuby(String studentRuby) {
        this.studentRuby = studentRuby;
    }
    /**
     * カナを返す
     * @return カナ
     */
    public String getStudentRuby() {
        return studentRuby;
    }
    /**
     * NFCタグのIDを追加する<br />
     * 既に同じタグが追加されている場合は追加しない。
     * @param id 追加するID
     */
    public void addNfcId(String id) {
        if (nfcIds.indexOf(id) == -1) {
            // 同じIDが追加されていなければ追加する
            nfcIds.add(id);
        }
    }
    /**
     * NFCタグのIDを削除する
     * @param id 削除するID
     */
    public void removeNfcId(String id) {
        nfcIds.remove(id);
    }
    /**
     * NFCタグのIDの配列を返す
     * @return NFCタグの配列
     */
    public String[] getNfcIds() {
        return nfcIds.toArray(new String[nfcIds.size()]);
    }
    /**
     * NFCタグのIDの数を返す
     * @return NFCタグの数
     */
    public int getNumOfNfcId() {
        return nfcIds.size();
    }
    /***
     * NFCタグを全て削除する
     */
    public void removeAllNfcIds() {
        nfcIds = new ArrayList<String>();
    }
    /**
     * NFCタグのIDの添字を返す
     * @param id 検索するID
     * @return NFCタグのIDの添字 存在しなければ-1
     */
    public int indexOfNfcId(String id) {
        return nfcIds.indexOf(id);
    }

    /**
     * 学籍番号とNFCタグのIDが揃ったかどうかを調べる
     * @return 揃っていればtrue、揃っていなければfalseを返す
     */
    public boolean isDataPrepared() {
        return studentNo.length() != 0 && !nfcIds.isEmpty();
    }

    /**
     * 学生データをCSV形式で出力する
     * @return CSV形式にした学生データ
     */
    public String toCsvRecord() {
        StringBuilder csvRecord = new StringBuilder();

        if (studentNum != -1) {
            csvRecord.append(studentNum);
        }
        csvRecord.append("," + className + "," + studentNo + "," + studentName + "," + studentRuby);
        for (String nfcId : nfcIds) {
            csvRecord.append("," + nfcId);
        }

        return csvRecord.toString();
    }
}