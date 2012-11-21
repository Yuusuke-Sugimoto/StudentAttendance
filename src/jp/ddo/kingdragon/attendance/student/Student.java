package jp.ddo.kingdragon.attendance.student;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * 学生1人分のデータを管理するクラス
 * @author 杉本祐介
 */
public class Student implements Serializable {
    // 定数の宣言
    /** シリアルバージョンUID */
    private static final long serialVersionUID = -2959850874518622078L;

    // 変数の宣言
    /** 学籍番号 */
    private String studentNo;
    /** 所属 */
    private String className;
    /** 氏名 */
    private String studentName;
    /** カナ */
    private String studentRuby;

    // コレクションの宣言
    /** NFCタグのIDのリスト */
    private ArrayList<String> nfcIds;

    // コンストラクタ
    /** 学籍番号およびNFCタグのIDが未設定のインスタンスを生成する */
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
        this(studentNo, "", "", "", nfcIds);
    }
    /**
     * 学籍番号、所属、氏名、カナ、NFCタグのIDがセットされたインスタンスを生成する
     * @param studentNo 学籍番号
     * @param className 所属
     * @param name 氏名
     * @param ruby カナ
     * @param nfcIds NFCタグのID(複数可)
     */
    public Student(String studentNo, String className, String name, String ruby, String... nfcIds) {
        this.studentNo = studentNo;
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
    /**
     * コピーコンストラクタ
     * @param inStudent コピーする学生データ
     */
    public Student(Student inStudent) {
        studentNo = inStudent.studentNo;
        className = inStudent.className;
        studentName = inStudent.studentName;
        studentRuby = inStudent.studentRuby;
        nfcIds = new ArrayList<String>();
        for (String nfcId : inStudent.nfcIds) {
            nfcIds.add(nfcId);
        }
    }

    @Override
    public boolean equals(Object o) {
        boolean retBool = false;

        if (o instanceof Student) {
            Student s = (Student)o;
            if (studentNo.equals(s.studentNo)
                && className.equals(s.className)
                && studentName.equals(s.studentName)
                && studentRuby.equals(s.studentRuby)) {
                retBool = true;
            }
        }

        return retBool;
    }

    @Override
    public int hashCode() {
        int hash = studentNo.hashCode() + className.hashCode() + studentName.hashCode() + studentRuby.hashCode();

        return hash;
    }

    // アクセッサ
    /**
     * 学籍番号を変更する
     * @param studentNo 学籍番号
     */
    public void setStudentNo(String studentNo) {
        this.studentNo = studentNo;
    }
    /**
     * 学籍番号を取得する
     * @return 学籍番号
     */
    public String getStudentNo() {
        return studentNo;
    }

    /**
     * 所属を変更する
     * @param className 所属
     */
    public void setClassName(String className) {
        this.className = className;
    }
    /**
     * 所属を取得する
     * @return 所属
     */
    public String getClassName() {
        return className;
    }

    /**
     * 氏名を変更する
     * @param studentName 氏名
     */
    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }
    /**
     * 氏名を取得する
     * @return 氏名
     */
    public String getStudentName() {
        return studentName;
    }

    /**
     * カナを変更する
     * @param studentRuby カナ
     */
    public void setStudentRuby(String studentRuby) {
        this.studentRuby = studentRuby;
    }
    /**
     * カナを取得する
     * @return カナ
     */
    public String getStudentRuby() {
        return studentRuby;
    }

    /**
     * NFCタグのIDを追加する<br>
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
     * NFCタグのIDの配列を取得する
     * @return NFCタグの配列
     */
    public String[] getNfcIds() {
        return nfcIds.toArray(new String[nfcIds.size()]);
    }

    /**
     * NFCタグのIDの数を取得する
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
     * NFCタグが登録されているかどうかを取得する
     * @param id 検索するID
     * @return 登録されていればtrue そうでなければfalse
     */
    public boolean hasNfcId(String id) {
        return nfcIds.contains(id);
    }

    /**
     * 学生データの内容を配列で取得する
     * @return 学生データの内容を配列に格納したもの
     */
    public String[] getStudentData() {
        ArrayList<String> studentData = new ArrayList<String>();

        studentData.add(studentNo);
        studentData.add(studentName);
        studentData.add(studentRuby);
        for (String nfcId : nfcIds) {
            studentData.add(nfcId);
        }

        return studentData.toArray(new String[studentData.size()]);
    }
}