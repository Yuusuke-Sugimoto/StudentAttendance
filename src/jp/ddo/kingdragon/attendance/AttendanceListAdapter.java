package jp.ddo.kingdragon.attendance;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * 出席データの一覧を表示するアダプタクラス
 * @author 杉本祐介
 */
public class AttendanceListAdapter extends ArrayAdapter<Attendance> {
    // 変数の宣言
    private LayoutInflater inflater;
    private ViewHolder holder;

    // コンストラクタ
    public AttendanceListAdapter(Context context, int textViewResourceId) {
        this(context, textViewResourceId, new ArrayList<Attendance>());
    }

    public AttendanceListAdapter(Context context, int textViewResourceId, List<Attendance> objects) {
        super(context, textViewResourceId, objects);

        inflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Attendance mAttendance = getItem(position);

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.attendance_view, null);

            holder = new ViewHolder();
            holder.studentNum    = (TextView)convertView.findViewById(R.id.student_num);
            holder.studentNo     = (TextView)convertView.findViewById(R.id.student_no);
            holder.studentName   = (TextView)convertView.findViewById(R.id.student_name);
            holder.studentStatus = (TextView)convertView.findViewById(R.id.student_status);

            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder)convertView.getTag();
        }

        holder.studentNum.setText(String.valueOf(mAttendance.getStudentNum()));
        holder.studentNo.setText(mAttendance.getStudentNo());
        holder.studentName.setText(mAttendance.getStudentName());
        holder.studentStatus.setText(mAttendance.getStatusString());

        /**
         * 選択中の行を常にハイライトする
         * 参考:ListViewのonItemClickイベントに関して - Android-SDK-Japan | Google グループ
         *      https://groups.google.com/group/android-sdk-japan/browse_thread/thread/d1b256728c759f01/f42670b4aa8326e2?show_docid=f42670b4aa8326e2&hl=ja
         */
        if (parent instanceof ListView) {
            ListView mListView = (ListView)parent;
            if (position == mListView.getCheckedItemPosition()) {
                convertView.setBackgroundResource(R.drawable.orange);
            }
            else {
                convertView.setBackgroundColor(Color.TRANSPARENT);
            }
        }

        return convertView;
    }

    /**
     * 作成済みのTextViewを保持するクラス
     * @author 杉本祐介
     */
    private static class ViewHolder {
        TextView studentNum;
        TextView studentNo;
        TextView studentName;
        TextView studentStatus;
    }
}