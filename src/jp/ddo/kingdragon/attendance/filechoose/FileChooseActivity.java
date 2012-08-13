package jp.ddo.kingdragon.attendance.filechoose;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.Button;
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

import jp.ddo.kingdragon.attendance.R;

/**
 * ファイルの選択を行うアクティビティ<br />
 * [パラメータ]<br />
 * initDirPath:初期ディレクトリ[String]<br />
 * filter:一覧に表示するファイル名の正規表現[String]<br />
 * extension:一覧に表示する拡張子の正規表現[String]<br />
 * dirMode:選択対象をディレクトリにするかどうか[boolean]<br />
 * [戻り値]<br />
 * fileName:ファイル名[String]<br />
 * filePath:ファイルの絶対パス[String]
 * @author 杉本祐介
 */
public class FileChooseActivity extends Activity {
    // 定数の宣言
    // ダイアログのID
    private static final int DIALOG_CREATE_FILE             = 0;
    private static final int DIALOG_FILE_ALREADY_EXISTS     = 1;
    private static final int DIALOG_FILE_CREATE_FAILED      = 2;
    private static final int DIALOG_ILLEGAL_FILE_NAME       = 3;
    private static final int DIALOG_FILE_NAME_IS_NULL       = 4;
    private static final int DIALOG_CREATE_DIRECTORY        = 5;
    private static final int DIALOG_DIRECTORY_CREATE_FAILED = 6;
    private static final int DIALOG_ILLEGAL_DIRECTORY_NAME  = 7;
    private static final int DIALOG_DIRECTORY_NAME_IS_NULL  = 8;

    // 変数の宣言
    /**
     * 選択対象がディレクトリかどうか
     */
    private boolean isDirModeEnabled;
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
    private LinearLayout layoutForCreateFile;
    /**
     * ファイル名用のEditText
     */
    private EditText editTextForFileName;
    /**
     * 拡張子用のTextView
     */
    private TextView textViewForExtension;
    /**
     * フォルダ名用のEditText
     */
    private EditText editTextForDirectoryName;

    // コレクションの宣言
    /**
     * 辿った履歴を格納するスタック
     */
    private Stack<File> history;

    @Override
    @SuppressWarnings("unchecked")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_choose);

        Intent mIntent = getIntent();
        String initDirPath = mIntent.getStringExtra("initDirPath");
        if (initDirPath == null) {
            initDirPath = Environment.getExternalStorageDirectory().getAbsolutePath();
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

        isDirModeEnabled = mIntent.getBooleanExtra("dirMode", false);

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

        LinearLayout buttonLayout = (LinearLayout)findViewById(R.id.file_button_layout);
        if (isDirModeEnabled) {
            Button okButton = new Button(FileChooseActivity.this);
            okButton.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1));
            okButton.setText(android.R.string.ok);
            okButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent mIntent = new Intent();
                    mIntent.putExtra("fileName", currentDir.getName());
                    mIntent.putExtra("filePath", currentDir.getAbsolutePath());
                    setResult(Activity.RESULT_OK, mIntent);
                    finish();
                }
            });
            buttonLayout.addView(okButton);
        }
        Button cancelButton = new Button(FileChooseActivity.this);
        cancelButton.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1));
        cancelButton.setText(android.R.string.cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(Activity.RESULT_CANCELED, null);
                finish();
            }
        });
        buttonLayout.addView(cancelButton);

        history = new Stack<File>();

        // アクティビティ再生成前のデータがあれば復元する
        if (savedInstanceState != null) {
            isShowingInvisibleFile = savedInstanceState.getBoolean("IsShowingInvisibleFile");
            currentDir = (File)savedInstanceState.getSerializable("CurrentDir");
            history = (Stack<File>)savedInstanceState.getSerializable("History");
        }

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

        if (isDirModeEnabled) {
            menu.removeItem(R.id.menu_create_file);
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
        case R.id.menu_create_file:
            showDialog(FileChooseActivity.DIALOG_CREATE_FILE);

            break;
        case R.id.menu_create_directory:
            showDialog(FileChooseActivity.DIALOG_CREATE_DIRECTORY);

            break;
        }

        return true;
    }

    @Override
    public Dialog onCreateDialog(int id) {
        Dialog retDialog = null;

        AlertDialog.Builder builder;

        switch (id) {
        case FileChooseActivity.DIALOG_CREATE_FILE:
            builder = new AlertDialog.Builder(FileChooseActivity.this);
            builder.setTitle(R.string.dialog_create_file_title);

            LayoutInflater inflater = LayoutInflater.from(FileChooseActivity.this);
            layoutForCreateFile = (LinearLayout)inflater.inflate(R.layout.dialog_create_file, null);
            editTextForFileName = (EditText)layoutForCreateFile.findViewById(R.id.dialog_file_name);
            textViewForExtension = (TextView)layoutForCreateFile.findViewById(R.id.dialog_file_extension);

            builder.setView(layoutForCreateFile);
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
                                    editTextForFileName.setText("");
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
                    showDialog(FileChooseActivity.DIALOG_CREATE_FILE);
                }
            });

            break;
        case FileChooseActivity.DIALOG_CREATE_DIRECTORY:
            builder = new AlertDialog.Builder(FileChooseActivity.this);
            builder.setTitle(R.string.dialog_create_directory_title);
            editTextForDirectoryName = new EditText(FileChooseActivity.this);
            editTextForDirectoryName.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            builder.setView(editTextForDirectoryName);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String directoryName = editTextForDirectoryName.getEditableText().toString();
                    if (directoryName.length() != 0) {
                        if (!directoryName.matches(".*(<|>|:|\\*|\\?|\"|/|\\\\|\\||\u00a5).*")) {
                            // 使用不可能な文字列(< > : * ? " / \ |)が含まれていなければフォルダを作成
                            File mDir = new File(currentDir, directoryName);
                            if (mDir.mkdir()) {
                                showFileList(currentDir);
                                editTextForDirectoryName.setText("");
                                Toast.makeText(FileChooseActivity.this, directoryName + getString(R.string.notice_file_created), Toast.LENGTH_SHORT).show();
                            }
                            else {
                                showDialog(FileChooseActivity.DIALOG_DIRECTORY_CREATE_FAILED);
                            }
                        }
                        else {
                            showDialog(FileChooseActivity.DIALOG_ILLEGAL_DIRECTORY_NAME);
                        }
                    }
                    else {
                        showDialog(FileChooseActivity.DIALOG_DIRECTORY_NAME_IS_NULL);
                    }
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.setCancelable(true);
            retDialog = builder.create();

            break;
        case FileChooseActivity.DIALOG_DIRECTORY_CREATE_FAILED:
        case FileChooseActivity.DIALOG_ILLEGAL_DIRECTORY_NAME:
        case FileChooseActivity.DIALOG_DIRECTORY_NAME_IS_NULL:
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
                    showDialog(FileChooseActivity.DIALOG_CREATE_DIRECTORY);
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
        case FileChooseActivity.DIALOG_CREATE_FILE:
            layoutForCreateFile.removeView(textViewForExtension);
            if (extension.matches("[A-Za-z0-9]*")) {
                // 拡張子が1つだけ設定されている時はその拡張子を付加する
                textViewForExtension.setText("." + extension);
                layoutForCreateFile.addView(textViewForExtension);
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
        case FileChooseActivity.DIALOG_DIRECTORY_CREATE_FAILED:
            mAlertDialog.setMessage(getString(R.string.error_directory_create_failed));

            break;
        case FileChooseActivity.DIALOG_ILLEGAL_DIRECTORY_NAME:
            mAlertDialog.setMessage(getString(R.string.error_illegal_directory_name));

            break;
        case FileChooseActivity.DIALOG_DIRECTORY_NAME_IS_NULL:
            mAlertDialog.setMessage(getString(R.string.error_directory_name_null));

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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("IsShowingInvisibleFile", isShowingInvisibleFile);
        outState.putSerializable("CurrentDir", currentDir);
        outState.putSerializable("History", history);
    }

    /**
     * ファイルの一覧をListViewに表示する
     * @param dir 対象となるディレクトリ
     */
    public void showFileList(File dir) {
        File limitDir = Environment.getExternalStorageDirectory();
        mFileListAdapter = new FileListAdapter(FileChooseActivity.this, 0);
        fileListView.setAdapter(mFileListAdapter);

        // 外部SDカードより上の階層には移動できないようにする
        if (dir.getParentFile() != null && !dir.equals(limitDir)) {
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
                else if (!isDirModeEnabled && mFile.getName().toLowerCase().matches("(" + filter + ")\\.(" + extension + ")")) {
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