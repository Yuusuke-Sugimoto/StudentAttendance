package jp.ddo.kingdragon.attendance;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Stack;

/**
 * ファイルの選択を行うアクティビティ<br />
 * [パラメータ]<br />
 * initDirPath:初期ディレクトリ<br />
 * filter:一覧に表示するファイル名の正規表現<br />
 * extension:一覧に表示する拡張子の正規表現<br />
 * [戻り値]<br />
 * fileName:ファイル名<br />
 * filePath:ファイルの絶対パス
 * @author 杉本祐介
 */
public class FileChooseActivity extends Activity {
    // 定数の宣言
    // ダイアログのID
    private static final int DIALOG_NEW_FILE            = 0;
    private static final int DIALOG_FILE_ALREADY_EXISTS = 1;
    private static final int DIALOG_FILE_CREATE_FAILED  = 2;
    private static final int DIALOG_ILLEGAL_FILE_NAME   = 3;
    private static final int DIALOG_FILE_NAME_IS_NULL   = 4;

    // 変数の宣言
    /**
     * 隠しファイルを表示するかどうか
     */
    private boolean isShowingInvisibleFile;

    /**
     * 現在のディレクトリ
     */
    private File currentDir;
    /**
     * 一覧に表示するファイル名の正規表現
     */
    private String filter;
    /**
     * 一覧に表示する拡張子の正規表現
     */
    private String extension;
    /**
     * ファイルの一覧を表示するビュー
     */
    private ListView fileListView;
    /**
     * ファイルの一覧を表示するアダプタ
     */
    private FileListAdapter mFileListAdapter;
    /**
     * 新規ファイル作成ダイアログに適用するレイアウト
     */
    private LinearLayout layoutForNewFile;
    /**
     * ファイル名用のEditText
     */
    private EditText editTextForFileName;
    /**
     * 拡張子用のTextView
     */
    private TextView textViewForExtension;

    // コレクションの宣言
    /**
     * 辿った履歴を格納するスタック
     */
    private Stack<File> history;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_choose);

        Intent mIntent = getIntent();
        String initDirPath = mIntent.getStringExtra("initDirPath");
        if (initDirPath == null) {
            initDirPath = "/sdcard";
        }
        currentDir = new File(initDirPath);

        filter = mIntent.getStringExtra("filter");
        if (filter == null) {
            filter = ".*";
        }

        extension = mIntent.getStringExtra("extension");
        if (extension == null) {
            extension = ".*";
        }

        fileListView = (ListView)findViewById(R.id.file_list);
        fileListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File selectedFile = (File)parent.getItemAtPosition(position);
                if (selectedFile.isFile()) {
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.getItem(0);
        if (isShowingInvisibleFile) {
            item.setTitle(R.string.menu_hide_invisible_file);
        }
        else {
            item.setTitle(R.string.menu_show_invisible_file);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_show_invisible_file:
            isShowingInvisibleFile = !isShowingInvisibleFile;
            showFileList(currentDir);

            break;
        case R.id.menu_new_file:
            showDialog(FileChooseActivity.DIALOG_NEW_FILE);

            break;
        }

        return true;
    }

    @Override
    public Dialog onCreateDialog(int id) {
        Dialog retDialog = null;

        AlertDialog.Builder builder;

        switch (id) {
        case FileChooseActivity.DIALOG_NEW_FILE:
            builder = new AlertDialog.Builder(FileChooseActivity.this);
            builder.setTitle(R.string.dialog_new_file_title);

            LayoutInflater inflater = LayoutInflater.from(FileChooseActivity.this);
            layoutForNewFile = (LinearLayout)inflater.inflate(R.layout.dialog_new_file, null);
            editTextForFileName = (EditText)layoutForNewFile.findViewById(R.id.dialog_file_name);
            textViewForExtension = (TextView)layoutForNewFile.findViewById(R.id.dialog_file_extension);

            builder.setView(layoutForNewFile);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    StringBuilder fileNameBuilder = new StringBuilder(editTextForFileName.getEditableText().toString());
                    if (fileNameBuilder.length() != 0) {
                        if (extension.matches("[A-Za-z0-9]*")) {
                            // 拡張子が1つだけ設定されている時はその拡張子を付加する
                            fileNameBuilder.append("." + extension);
                        }
                        String fileName = fileNameBuilder.toString();
                        if (!fileName.matches(".*(<|>|:|\\*|\\?|\"|/|\\\\|\\||\u00a5).*")) {
                            // 使用不可能な文字列(< > : * ? " / \ |)が含まれていなければファイルを作成
                            File mFile = new File(currentDir, fileName);
                            try {
                                if (mFile.createNewFile()) {
                                    showFileList(currentDir);
                                    Toast.makeText(FileChooseActivity.this, fileName + getString(R.string.notice_file_created), Toast.LENGTH_SHORT).show();
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
        if (dialog instanceof AlertDialog) {
            mAlertDialog = (AlertDialog)dialog;
        }

        switch (id) {
        case FileChooseActivity.DIALOG_NEW_FILE:
            editTextForFileName.setText("");
            layoutForNewFile.removeView(textViewForExtension);
            if (extension.matches("[A-Za-z0-9]*")) {
                // 拡張子が1つだけ設定されている時はその拡張子を付加する
                textViewForExtension.setText("." + extension);
                layoutForNewFile.addView(textViewForExtension);
            }

            break;
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
            mAlertDialog.setMessage(getString(R.string.error_file_name_null));

            break;
        }
    }

    @Override
    public void onBackPressed() {
        if (!history.isEmpty()) {
            currentDir = history.pop();
            showFileList(currentDir);
        }
        else {
            super.onBackPressed();
        }
    }

    /**
     * ファイルの一覧をListViewに表示する
     * @param dir 対象となるディレクトリ
     */
    public void showFileList(File dir) {
        mFileListAdapter = new FileListAdapter(FileChooseActivity.this, 0);
        fileListView.setAdapter(mFileListAdapter);

        if (dir.getParentFile() != null) {
            mFileListAdapter.add(dir.getParentFile(), true);
        }
        if (dir.listFiles() != null) {
            ArrayList<File> directories = new ArrayList<File>();
            ArrayList<File> files = new ArrayList<File>();
            for (File mFile : dir.listFiles()) {
                if (mFile.isDirectory()) {
                    if (isShowingInvisibleFile || !mFile.getName().startsWith(".")) {
                        directories.add(mFile);
                    }
                }
                else if (mFile.getName().toLowerCase().matches("(" + filter + ")\\.(" + extension + ")")) {
                    if (isShowingInvisibleFile || !mFile.getName().startsWith(".")) {
                        files.add(mFile);
                    }
                }
            }
            Comparator<File> mComparator = new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
                }
            };
            Collections.sort(directories, mComparator);
            Collections.sort(files, mComparator);
            for (int i = 0; i < directories.size(); i++) {
                mFileListAdapter.add(directories.get(i), false);
            }
            for (int i = 0; i < files.size(); i++) {
                mFileListAdapter.add(files.get(i), false);
            }
        }
        setTitle(dir.getAbsolutePath());
    }
}