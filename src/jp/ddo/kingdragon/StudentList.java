package jp.ddo.kingdragon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/***
 * 学生のリストを管理するクラス
 *
 * @author 杉本祐介
 */
public class StudentList implements Iterable<Student> {
    private ArrayList<Student> students;
    private HashMap<String, Integer> hashTable;

    // コンストラクタ
    public StudentList() {
        students = new ArrayList<Student>();
        hashTable = new HashMap<String, Integer>();
    }

    @Override
    public Iterator<Student> iterator() {
        return(students.iterator());
    }

    /***
     * 学生データを追加する
     *
     * @param inStudent
     *     学生データ
     */
    public void add(Student inStudent) {
        inStudent.setNum(students.size() + 1);
        students.add(inStudent);
        hashTable.put(inStudent.getStudentNo(), students.indexOf(inStudent));
    }

    /***
     * 引数で渡された添字の学生データを取得する
     *
     * @param index
     *     添字
     * @return 添字の位置の学生データ
     */
    public Student get(int index) {
        return(students.get(index));
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
     * 引数で渡された学籍番号をもつ学生データの添字を調べる
     *
     * @param studentNo
     *     学籍番号
     * @return 存在したならばその学生データの添字、存在しなければ-1を返す
     */
    public int searchByStudentNo(String studentNo) {
        int retInt = -1;

        Integer temp = hashTable.get(studentNo);
        if(temp != null) {
            retInt = temp.intValue();
        }

        return(retInt);
    }
}