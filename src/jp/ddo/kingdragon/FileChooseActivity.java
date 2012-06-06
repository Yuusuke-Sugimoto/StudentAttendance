package jp.ddo.kingdragon;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import java.io.File;
import java.util.Stack;

/***
 * ファイルの選択を行うアクティビティ
 * @author 杉本祐介
 */
public class FileChooseActivity extends Activity {
    // 変数の宣言
    /***
     * 現在のディレクトリ
     */
    private File currentDir;
    /***
     * 一覧に表示するファイル名のパターン
     */
    private String filter;
    /***
     * ファイルの一覧を表示するビュー
     */
    private ListView fileListView;
    /***
     * ファイルの一覧を表示するアダプタ
     */
    private FileListAdapter mFileListAdapter;

    // コレクションの宣言
    /***
     * 辿った履歴を格納するスタック
     */
    private Stack<File> history;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_choose);

        Intent mIntent = getIntent();
        String initDirPath = mIntent.getStringExtra("initDirPath");
        if(initDirPath == null) {
            initDirPath = "/sdcard";
        }
        currentDir = new File(initDirPath);

        filter = mIntent.getStringExtra("filter");
        if(filter == null) {
            filter = ".*";
        }

        fileListView = (ListView)findViewById(R.id.file_list);
        fileListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File selectedFile = (File)parent.getItemAtPosition(position);
                if(selectedFile.isFile()) {
                    Intent mIntent = new Intent();
                    mIntent.putExtra("fileName", selectedFile.getName());
                    mIntent.putExtra("filePath", selectedFile.getAbsolutePath());
                    setResult(Activity.RESULT_OK, mIntent);
                    finish();
                }
                else {
                    history.push(currentDir);
                    currentDir = selectedFile;
                    showFileList(currentDir);
                }
            }
        });

        history = new Stack<File>();

        showFileList(currentDir);
    }

    @Override
    public void onBackPressed() {
        if(!history.isEmpty()) {
            currentDir = history.pop();
            showFileList(currentDir);
        }
        else {
            super.onBackPressed();
        }
    }

    public void showFileList(File dir) {
        mFileListAdapter = new FileListAdapter(FileChooseActivity.this, 0);
        fileListView.setAdapter(mFileListAdapter);

        if(dir.getParentFile() != null) {
            mFileListAdapter.add(dir.getParentFile(), true);
        }
        if(dir.listFiles() != null) {
            for(File mFile : dir.listFiles()) {
                if(mFile.isDirectory() || mFile.getName().toLowerCase().matches(filter)) {
                    mFileListAdapter.add(mFile, false);
                }
            }
        }
        setTitle(dir.getAbsolutePath());
    }
}