package jp.ddo.kingdragon;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * ファイルの一覧を表示するアダプタクラス
 * @author 杉本祐介
 */
public class FileListAdapter extends ArrayAdapter<File> {
    // 変数の宣言
    private LayoutInflater inflater;
    private ViewHolder holder;
    private String parentName;
    private File parentDir;

    // コンストラクタ
    public FileListAdapter(Context context, int textViewResourceId) {
        this(context, textViewResourceId, new ArrayList<File>());
    }

    public FileListAdapter(Context context, int textViewResourceId, List<File> objects) {
        super(context, textViewResourceId, objects);

        inflater = LayoutInflater.from(context);
        parentName = context.getString(R.string.file_choose_parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        File mFile = getItem(position);

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.file_view, null);

            holder = new ViewHolder();
            holder.fileName = (TextView)convertView.findViewById(R.id.file_name);

            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder)convertView.getTag();
        }

        if (mFile != parentDir) {
            if (mFile.isFile()) {
                holder.fileName.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_compose, 0, 0, 0);
            }
            else {
                holder.fileName.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_archive, 0, 0, 0);
            }
            holder.fileName.setText(mFile.getName());
        }
        else {
            holder.fileName.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_arrow_up, 0, 0, 0);
            holder.fileName.setText(parentName);
        }

        return convertView;
    }

    @Override
    public void add(File inFile) {}

    public void add(File inFile, boolean isParent) {
        if (getPosition(inFile) == -1) {
            if (isParent) {
                parentDir = inFile;
            }
            super.add(inFile);
        }
    }

    /**
     * 作成済みのTextViewを保持するクラス
     * @author 杉本祐介
     */
    private static class ViewHolder {
        TextView fileName;
    }
}