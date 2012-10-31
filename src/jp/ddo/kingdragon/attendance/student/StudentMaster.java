package jp.ddo.kingdragon.attendance.student;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * 学生マスタを管理するクラス
 * @author 杉本祐介
 */
public class StudentMaster {
    // 変数の宣言
    /**
     * 学生マスタ格納フォルダ
     */
    private File masterDir;
    /**
     * 読み込みに使用する文字コード
     */
    private String characterCode;

    // コレクションの宣言
    /**
     * 学生マスタ格納フォルダから読み取った全てのシートを格納するリスト
     */
    private ArrayList<StudentSheet> studentSheets;

    // コンストラクタ
    /**
     * 学生マスタの位置と文字コードを指定してインスタンスを生成する
     * @param masterDir 学生マスタ格納フォルダ
     * @param characterCode 展開に使用する文字コード
     * @throws IOException
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    public StudentMaster(File masterDir, String characterCode) throws UnsupportedEncodingException, FileNotFoundException, IOException {
        this.masterDir = masterDir;
        this.characterCode = characterCode;

        refresh();
    }

    /**
     * 読み込んだリストの数を取得する
     * @return 読み込んだリストの数
     */
    public int size() {
        return studentSheets.size();
    }

    /**
     * 指定された添字のリストを取得する
     * @param index 添字
     * @return 指定された添字のリストのコピー
     */
    public StudentSheet getStudentSheet(int index) {
        return new StudentSheet(studentSheets.get(index));
    }

    /**
     * 指定された学籍番号を持つ学生データを取得する
     * @param studentNo 学籍番号
     * @return 学生データのコピー 該当するデータがなければnull
     */
    public Student getStudentByStudentNo(String studentNo) {
        Student retStudent = null;

        boolean isExisted = false;
        if (studentSheets.size() != 0) {
            for (int i = 0; !isExisted && i < studentSheets.size(); i++) {
                StudentSheet tempStudentSheet = studentSheets.get(i);
                if (tempStudentSheet.hasStudentNo(studentNo)) {
                    retStudent = new Student(tempStudentSheet.getByStudentNo(studentNo));
                    isExisted = true;
                }
            }
        }

        return retStudent;
    }

    /**
     * 指定されたNFCタグを持つ学生データを取得する
     * @param id NFCタグのID
     * @return 学生データのコピー 該当するデータがなければnull
     */
    public Student getStudentByNfcId(String id) {
        Student retStudent = null;

        boolean isExisted = false;
        if (studentSheets.size() != 0) {
            for (int i = 0; !isExisted && i < studentSheets.size(); i++) {
                StudentSheet tempStudentSheet = studentSheets.get(i);
                if (tempStudentSheet.hasNfcId(id)) {
                    retStudent = new Student(tempStudentSheet.getByNfcId(id));
                    isExisted = true;
                }
            }
        }

        return retStudent;
    }

    /**
     * 学生データにNFCタグを登録する
     * @param studentNo 学籍番号
     * @param id NFCタグ
     * @return 登録後の学生データのコピー 該当するデータがなければnull
     * @throws IOException
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    public Student registerNfcId(String studentNo, String id) throws UnsupportedEncodingException, FileNotFoundException, IOException {
        Student retStudent = null;

        boolean isExisted = false;
        if (studentSheets.size() != 0) {
            for (int i = 0; !isExisted && i < studentSheets.size(); i++) {
                StudentSheet tempStudentSheet = studentSheets.get(i);
                if (tempStudentSheet.hasStudentNo(studentNo)) {
                    Student mStudent = tempStudentSheet.getByStudentNo(studentNo);
                    mStudent.addNfcId(id);
                    tempStudentSheet.saveCsvFile(tempStudentSheet.getBaseFile(), characterCode);
                    retStudent = new Student(mStudent);
                    isExisted = true;
                }
            }
        }

        return retStudent;
    }

    /**
     * 学生マスタを読み込み直す
     * @throws IOException
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    public void refresh() throws UnsupportedEncodingException, FileNotFoundException, IOException {
        studentSheets = new ArrayList<StudentSheet>();
        ArrayList<File> csvFiles = new ArrayList<File>();
        for (File mFile : masterDir.listFiles()) {
            if (mFile.getName().endsWith(".csv")) {
                csvFiles.add(mFile);
            }
        }
        Comparator<File> mComparator = new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
            }
        };
        Collections.sort(csvFiles, mComparator);
        for (File csvFile : csvFiles) {
            studentSheets.add(new StudentSheet(csvFile, characterCode));
        }
    }
}