package jp.ddo.kingdragon;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * 学生の一覧を表示するアダプタクラス
 * @author 杉本祐介
 */
public class StudentListAdapter extends ArrayAdapter<Student> {
    // 変数の宣言
    private LayoutInflater inflater;
    private ViewHolder holder;

    // コンストラクタ
    public StudentListAdapter(Context context, int textViewResourceId) {
        this(context, textViewResourceId, new ArrayList<Student>());
    }

    public StudentListAdapter(Context context, int textViewResourceId, List<Student> objects) {
        super(context, textViewResourceId, objects);

        inflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Student mStudent = getItem(position);

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.student_view, null);

            holder = new ViewHolder();
            holder.studentNum  = (TextView)convertView.findViewById(R.id.student_num);
            holder.studentNo   = (TextView)convertView.findViewById(R.id.student_no);
            holder.studentName = (TextView)convertView.findViewById(R.id.student_name);
            holder.numOfNfcId  = (TextView)convertView.findViewById(R.id.num_of_nfc_id);

            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder)convertView.getTag();
        }

        holder.studentNum.setText(String.valueOf(mStudent.getStudentNum()));
        holder.studentNo.setText(mStudent.getStudentNo());
        holder.studentName.setText(mStudent.getStudentName());
        holder.numOfNfcId.setText(String.valueOf(mStudent.getNumOfNfcId()));

        return convertView;
    }

    @Override
    public void add(Student inStudent) {
        if (getPosition(inStudent) == -1) {
            super.add(inStudent);
        }
    }

    /**
     * 作成済みのTextViewを保持するクラス
     * @author 杉本祐介
     */
    private static class ViewHolder {
        TextView studentNum;
        TextView studentNo;
        TextView studentName;
        TextView numOfNfcId;
    }
}