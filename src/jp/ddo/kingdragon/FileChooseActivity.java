package jp.ddo.kingdragon;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import java.io.File;
import java.io.IOException;
import java.util.Stack;

/***
 * ファイルの選択を行うアクティビティ
 * @author 杉本祐介
 */
public class FileChooseActivity extends Activity {
    // 定数の宣言
    // ダイアログのID
    public static final int DIALOG_NEW_FILE            = 0;
    public static final int DIALOG_FILE_ALREADY_EXISTS = 1;
    public static final int DIALOG_FILE_CREATE_FAILED  = 2;
    public static final int DIALOG_ILLEGAL_FILE_NAME   = 3;
    public static final int DIALOG_FILE_NAME_IS_NULL   = 4;

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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.file_choose_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retBool = false;

        switch(item.getItemId()) {
        case R.id.menu_new_file:
            showDialog(FileChooseActivity.DIALOG_NEW_FILE);

            retBool = true;

            break;
        }

        return retBool;
    }

    @Override
    public Dialog onCreateDialog(int id) {
        Dialog retDialog = null;

        AlertDialog.Builder builder;
        final EditText mEditText;

        switch(id) {
        case FileChooseActivity.DIALOG_NEW_FILE:
            builder = new AlertDialog.Builder(FileChooseActivity.this);
            builder.setTitle(R.string.menu_new_file);
            mEditText = new EditText(FileChooseActivity.this);
            mEditText.setInputType(InputType.TYPE_CLASS_TEXT);
            mEditText.setMaxLines(1);
            mEditText.setHint(R.string.dialog_new_file_hint);
            builder.setView(mEditText);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    StringBuilder fileNameTemp = new StringBuilder(mEditText.getEditableText().toString());
                    if(fileNameTemp.length() != 0) {
                        // 円記号(U+00A5)をバックスラッシュ(U+005C)に置換
                        int pos;
                        while((pos = fileNameTemp.indexOf("\u00a5")) != -1) {
                            fileNameTemp.replace(pos, pos + 1, "\\");
                        }
                        String fileName = fileNameTemp.toString();
                        if(!fileName.matches(".*(<|>|:|\\*|\\?|\"|/|\\\\|\\|).*")) {
                            // 使用不可能な文字列(< > : * ? " / \ |)が含まれていなければファイルを作成
                            File mFile = new File(currentDir, fileName);
                            try {
                                if(mFile.createNewFile()) {
                                    showFileList(currentDir);
                                }
                                else {
                                    showDialog(FileChooseActivity.DIALOG_FILE_ALREADY_EXISTS);
                                }
                            }
                            catch (IOException e) {
                                showDialog(FileChooseActivity.DIALOG_FILE_CREATE_FAILED);
                                Log.e("onCreateDialog", e.getMessage(), e);
                            }
                        }
                        else {
                            showDialog(FileChooseActivity.DIALOG_ILLEGAL_FILE_NAME);
                        }
                    }
                    else {
                        showDialog(FileChooseActivity.DIALOG_FILE_NAME_IS_NULL);
                    }
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.setCancelable(true);
            retDialog = builder.create();

            break;
        case FileChooseActivity.DIALOG_FILE_ALREADY_EXISTS:
        case FileChooseActivity.DIALOG_FILE_CREATE_FAILED:
        case FileChooseActivity.DIALOG_ILLEGAL_FILE_NAME:
        case FileChooseActivity.DIALOG_FILE_NAME_IS_NULL:
            builder = new AlertDialog.Builder(FileChooseActivity.this);
            builder.setTitle(R.string.error);
            builder.setMessage("");
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setCancelable(true);
            retDialog = builder.create();
            retDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    showDialog(FileChooseActivity.DIALOG_NEW_FILE);
                }
            });

            break;
        }

        return retDialog;
    }

    @Override
    public void onPrepareDialog(int id, Dialog dialog) {
        AlertDialog mAlertDialog = null;
        if(dialog instanceof AlertDialog) {
            mAlertDialog = (AlertDialog)dialog;
        }

        switch(id) {
        case FileChooseActivity.DIALOG_FILE_ALREADY_EXISTS:
            mAlertDialog.setMessage(getString(R.string.error_file_already_exists));

            break;
        case FileChooseActivity.DIALOG_FILE_CREATE_FAILED:
            mAlertDialog.setMessage(getString(R.string.error_file_create_failed));

            break;
        case FileChooseActivity.DIALOG_ILLEGAL_FILE_NAME:
            mAlertDialog.setMessage(getString(R.string.error_illegal_file_name));

            break;
        case FileChooseActivity.DIALOG_FILE_NAME_IS_NULL:
            mAlertDialog.setMessage(getString(R.string.error_file_name_is_null));

            break;
        }
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

    /***
     * ファイルの一覧をListViewに表示する
     * @param dir 対象となるディレクトリ
     */
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