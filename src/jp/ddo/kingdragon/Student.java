package jp.ddo.kingdragon;

import java.util.ArrayList;

/***
 * 学生1人分のデータを管理するクラス
 *
 * @author 杉本祐介
 */
public class Student {
    // 変数の宣言
    /***
     * 学籍番号
     */
    private String studentNo;
    /***
     * 連番
     */
    private int num;
    /***
     * 所属
     */
    private String className;
    /***
     * 氏名
     */
    private String name;
    /***
     * カナ
     */
    private String ruby;

    // リストの宣言
    /***
     * NFCタグのIDのリスト
     */
    private ArrayList<String> nfcIds;

    // コンストラクタ
    /***
     * 学籍番号およびNFCタグのIDが未設定のインスタンスを生成する
     */
    public Student() {
        this("");
    }
    /***
     * 学籍番号がセットされたインスタンスを生成する
     *
     * @param studentNo
     *     学籍番号
     */
    public Student(String studentNo) {
        this(studentNo, new String[0]);
    }
    /***
     * 学籍番号とNFCタグのIDがセットされたインスタンスを生成する
     *
     * @param studentNo
     *     学籍番号
     * @param nfcIds
     *     NFCタグのID(複数可)
     */
    public Student(String studentNo, String... nfcIds) {
        this(studentNo, -1, "", "", "", nfcIds);
    }
    /***
     * 学籍番号、連番、所属、氏名、カナ、NFCタグのIDがセットされたインスタンスを生成する
     *
     * @param studentNo
     *     学籍番号
     * @param num
     *     連番
     * @param className
     *     所属
     * @param name
     *     氏名
     * @param ruby
     *     カナ
     * @param nfcIds
     *     NFCタグのID(複数可)
     */
    public Student(String studentNo, int num, String className, String name, String ruby, String... nfcIds) {
        this.studentNo = studentNo;
        this.num = num;
        this.className = className;
        this.name = name;
        this.ruby = ruby;
        this.nfcIds = new ArrayList<String>();
        if(nfcIds.length != 0) {
            for(String nfcId : nfcIds) {
                this.nfcIds.add(nfcId);
            }
        }
    }

    // アクセッサ
    /***
     * 学籍番号をセットする
     *
     * @param studentNo
     *     学籍番号
     */
    public void setStudentNo(String studentNo) {
        this.studentNo = studentNo;
    }
    /***
     * 学籍番号を返す
     *
     * @return 学籍番号
     */
    public String getStudentNo() {
        return(studentNo);
    }
    /***
     * 連番をセットする
     *
     * @param num
     *     連番
     */
    public void setNum(int num) {
        this.num = num;
    }
    /***
     * NFCタグのIDを追加する
     *
     * @param id
     *     NFCタグのID
     */
    public void addNfcId(String id) {
        if(nfcIds.indexOf(id) == -1) {
            // 同じIDが追加されていなければ追加する
            nfcIds.add(id);
        }
    }
    /***
     * NFCタグのIDの配列を返す
     *
     * @return NFCタグの配列
     */
    public String[] getNfcIds() {
        return(nfcIds.toArray(new String[0]));
    }
    /***
     * 学籍番号とNFCタグのIDが揃ったかどうかを調べる
     *
     * @return 揃っていればtrue、揃っていなければfalseを返す
     */
    public boolean isDataPrepared() {
        return(studentNo.length() != 0 && !nfcIds.isEmpty());
    }

    /***
     * 学生データをCSV形式で出力する
     *
     * @return CSV形式にした学生データ
     */
    public String toCsvRecord() {
        StringBuilder csvRecord = new StringBuilder();

        if(num != -1) {
            csvRecord.append(num);
        }
        csvRecord.append("," + className + "," + studentNo + "," + name + "," + ruby);
        for(String nfcId : nfcIds) {
            csvRecord.append("," + nfcId);
        }

        return(csvRecord.toString());
    }
}