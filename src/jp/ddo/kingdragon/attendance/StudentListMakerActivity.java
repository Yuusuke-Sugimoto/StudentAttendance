package jp.ddo.kingdragon.attendance;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnDismissListener;
import android.nfc.NfcAdapter;
import android.nfc.tech.TagTechnology;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import jp.ddo.kingdragon.attendance.filechoose.FileChooseActivity;
import jp.ddo.kingdragon.attendance.student.Student;
import jp.ddo.kingdragon.attendance.student.StudentListAdapter;
import jp.ddo.kingdragon.attendance.student.StudentMaster;
import jp.ddo.kingdragon.attendance.student.StudentSheet;
import jp.ddo.kingdragon.attendance.util.PreferenceUtil;
import jp.ddo.kingdragon.attendance.util.Util;

/**
 * リスト作成画面
 * @author 杉本祐介
 */
public class StudentListMakerActivity extends Activity {
    // 定数の宣言
    // リクエストコード
    private static final int REQUEST_CHOOSE_OPEN_FILE = 0;
    private static final int REQUEST_CHOOSE_SAVE_FILE = 1;
    // ダイアログのID
    private static final int DIALOG_ASK_EXIT_WITHOUT_SAVING       = 0;
    private static final int DIALOG_ASK_REMOVE_WITHOUT_SAVING     = 1;
    private static final int DIALOG_ASK_OPEN_WITHOUT_SAVING       = 2;
    private static final int DIALOG_ASK_ADD_STUDENT_BY_STUDENT_NO = 3;
    private static final int DIALOG_STUDENT_MENU                  = 4;
    private static final int DIALOG_EDIT_STUDENT                  = 5;
    private static final int DIALOG_ASK_REMOVE_NFC_ID             = 6;
    private static final int DIALOG_ASK_REMOVE_STUDENT            = 7;
    private static final int DIALOG_EDIT_CLASS_NAME               = 8;
    private static final int DIALOG_ADD_STUDENT_MENU              = 9;
    private static final int DIALOG_CSV_FILE_LIST                 = 10;
    private static final int DIALOG_STUDENT_LIST                  = 11;
    private static final int DIALOG_SEARCH_STUDENT_NO             = 12;
    private static final int DIALOG_INPUT_STUDENT_INFO            = 13;
    private static final int DIALOG_ASK_OVERWRITE                 = 14;
    private static final int DIALOG_REFRESHING_MASTER_FILE        = 15;
    /**
     * 使用する文字コード
     */
    private static final String CHARACTER_CODE = "Shift_JIS";

    // 変数の宣言
    /**
     * 他スレッドからのUIの更新に使用
     */
    private Handler mHandler;

    /**
     * 保存済みかどうか
     */
    private boolean isSaved;

    /**
     * ベースフォルダ
     */
    private File baseDir;
    /**
     * マスタフォルダ
     */
    private File masterDir;
    /**
     * 保存先のファイル
     */
    private File destFile;
    /**
     * キーボード(バーコードリーダ)から入力された内容
     */
    private StringBuilder inputBuffer;
    /**
     * 追加待ちの学籍番号
     */
    private String readStudentNo;
    /**
     * 現在編集しているシート
     */
    private StudentSheet mStudentSheet;
    /**
     * 現在扱っている学生データ
     */
    private Student currentStudent;
    /**
     * リストから追加する際に選択されたシート
     */
    private StudentSheet selectedSheet;
    /**
     * 学生マスタ
     */
    private StudentMaster master;
    /**
     * 学生の一覧を表示するビュー
     */
    private ListView studentListView;
    /**
     * 学生の一覧を表示するアダプタ
     */
    private StudentListAdapter mStudentListAdapter;
    /**
     * 学籍番号用のEditText
     */
    private EditText editTextForStudentNo;
    /**
     * 所属用のEditText
     */
    private EditText editTextForClassName;
    /**
     * 氏名用のEditText
     */
    private EditText editTextForStudentName;
    /**
     * カナ用のEditText
     */
    private EditText editTextForStudentRuby;

    /**
     * 設定内容の読み取り/変更に使用
     */
    private PreferenceUtil mPreferenceUtil;

    /**
     * NFCタグの読み取りに使用
     */
    private NfcAdapter mNfcAdapter;
    /**
     * NFCタグの読み取りに使用
     */
    private PendingIntent mPendingIntent;

    // 配列の宣言
    /**
     * 対応するインテントの種類
     */
    private IntentFilter[] filters;
    /**
     * 対応させるタグの一覧
     */
    private String[][] techs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.student_list_maker);

        mHandler = new Handler();

        isSaved = true;

        mPreferenceUtil = new PreferenceUtil(StudentListMakerActivity.this);

        // 各フォルダの作成
        baseDir = new File(Environment.getExternalStorageDirectory(), "StudentAttendance");
        masterDir = new File(baseDir, "StudentMaster");
        if (!masterDir.exists() && !masterDir.mkdirs()) {
            Toast.makeText(StudentListMakerActivity.this, R.string.error_make_master_directory_failed, Toast.LENGTH_SHORT).show();

            finish();
        }

        inputBuffer = new StringBuilder();
        try {
            master = new StudentMaster(masterDir, StudentListMakerActivity.CHARACTER_CODE);
        }
        catch (IOException e) {
            Toast.makeText(StudentListMakerActivity.this, R.string.error_master_file_open_failed, Toast.LENGTH_SHORT).show();
            Log.e("onCreate", e.getMessage(), e);

            finish();
        }

        if (savedInstanceState != null) {
            // アクティビティ再生成前のデータがあれば復元する
            isSaved = savedInstanceState.getBoolean("IsSaved");
            readStudentNo = savedInstanceState.getString("ReadStudentNo");
            currentStudent = (Student)savedInstanceState.getSerializable("CurrentStudent");
            selectedSheet = (StudentSheet)savedInstanceState.getSerializable("SelectedSheet");
            mStudentSheet = (StudentSheet)savedInstanceState.getSerializable("StudentSheet");
            mStudentListAdapter = new StudentListAdapter(StudentListMakerActivity.this, 0, mStudentSheet.getStudentList());
        }
        else {
            readStudentNo = null;
            currentStudent = new Student();
            selectedSheet = null;
            mStudentSheet = new StudentSheet();
            mStudentListAdapter = new StudentListAdapter(StudentListMakerActivity.this, 0);
        }

        // 設定情報にデフォルト値をセットする
        PreferenceManager.setDefaultValues(StudentListMakerActivity.this, R.xml.preference, false);

        /**
         * ListViewのレイアウトを変更する
         * 参考:リストビューをカスタマイズする | Tech Booster
         *      http://techbooster.org/android/ui/1282/
         *
         *      List14.java | Android Develpers
         *      http://developer.android.com/resources/samples/ApiDemos/src/com/example/android/apis/view/List14.html
         */
        studentListView = (ListView)findViewById(R.id.student_list);
        studentListView.setAdapter(mStudentListAdapter);
        studentListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                currentStudent = (Student)parent.getItemAtPosition(position);
                studentListView.invalidateViews();
            }
        });
        studentListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                studentListView.performItemClick(view, position, id);
                showDialog(StudentListMakerActivity.DIALOG_STUDENT_MENU);

                return true;
            }
        });
        int position = mStudentListAdapter.getPosition(currentStudent);
        if (position != -1) {
            studentListView.performItemClick(studentListView, position, studentListView.getItemIdAtPosition(position));
            studentListView.setSelection(position);
        }

        /**
         * NFCタグの情報を読み取る
         * 参考:i.2 高度な NFC - ソフトウェア技術ドキュメントを勝手に翻訳
         *      http://www.techdoctranslator.com/android/guide/nfc/advanced-nfc
         */
        mNfcAdapter = NfcAdapter.getDefaultAdapter(StudentListMakerActivity.this);
        mPendingIntent = PendingIntent.getActivity(StudentListMakerActivity.this, 0,
                                                   new Intent(StudentListMakerActivity.this, getClass())
                                                   .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        // NfcAdapter.ACTION_NDEF_DISCOVEREDだと拾えない
        IntentFilter mFilter = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        filters = new IntentFilter[] {mFilter};
        // どの種類のタグでも対応できるようにTagTechnologyクラスを指定する
        techs = new String[][] {new String[] {TagTechnology.class.getName()}};
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mNfcAdapter != null) {
            mNfcAdapter.enableForegroundDispatch(StudentListMakerActivity.this, mPendingIntent, filters, techs);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(StudentListMakerActivity.this);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case StudentListMakerActivity.REQUEST_CHOOSE_OPEN_FILE: {
                if (resultCode == Activity.RESULT_OK) {
                    String fileName = data.getStringExtra(FileChooseActivity.FILE_NAME);
                    String filePath = data.getStringExtra(FileChooseActivity.FILE_PATH);
                    try {
                        mStudentSheet = new StudentSheet(new File(filePath), StudentListMakerActivity.CHARACTER_CODE);
                        mStudentListAdapter = new StudentListAdapter(StudentListMakerActivity.this, 0, mStudentSheet.getStudentList());
                        studentListView.setAdapter(mStudentListAdapter);
                        isSaved = false;
                        Toast.makeText(StudentListMakerActivity.this, fileName + getString(R.string.notice_csv_file_opened), Toast.LENGTH_SHORT).show();
                    }
                    catch (IOException e) {
                        Toast.makeText(StudentListMakerActivity.this, fileName + getString(R.string.error_opening_failed), Toast.LENGTH_SHORT).show();
                        Log.e("onActivityResult", e.getMessage(), e);
                    }
                    catch (ArrayIndexOutOfBoundsException e) {
                        Toast.makeText(StudentListMakerActivity.this, getString(R.string.error_unsupported_list_file), Toast.LENGTH_SHORT).show();
                        Log.e("onActivityResult", e.getMessage(), e);
                    }
                }

                break;
            }
            case StudentListMakerActivity.REQUEST_CHOOSE_SAVE_FILE: {
                if (resultCode == Activity.RESULT_OK) {
                    String filePath = data.getStringExtra(FileChooseActivity.FILE_PATH);
                    destFile = new File(filePath);
                    if (destFile.exists()) {
                        showDialog(StudentListMakerActivity.DIALOG_ASK_OVERWRITE);
                    }
                    else {
                        saveCsvFile(destFile, StudentListMakerActivity.CHARACTER_CODE);
                    }
                }

                break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.student_list_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_new_sheet: {
                if (!isSaved) {
                    showDialog(StudentListMakerActivity.DIALOG_ASK_REMOVE_WITHOUT_SAVING);
                }
                else {
                    makeNewSheet();
                    Toast.makeText(StudentListMakerActivity.this, R.string.notice_new_sheet_created, Toast.LENGTH_SHORT).show();
                }

                break;
            }
            case R.id.menu_edit_class_name: {
                showDialog(StudentListMakerActivity.DIALOG_EDIT_CLASS_NAME);

                break;
            }
            case R.id.menu_add_attendance: {
                showDialog(StudentListMakerActivity.DIALOG_ADD_STUDENT_MENU);

                break;
            }
            case R.id.menu_refresh: {
                refreshStudentMaster();

                break;
            }
            case R.id.menu_open: {
                if (!isSaved) {
                    showDialog(StudentListMakerActivity.DIALOG_ASK_OPEN_WITHOUT_SAVING);
                }
                else {
                    Intent mIntent = new Intent(StudentListMakerActivity.this, FileChooseActivity.class);
                    mIntent.putExtra(FileChooseActivity.INIT_DIR_PATH, masterDir.getAbsolutePath());
                    mIntent.putExtra(FileChooseActivity.FILTER, ".*");
                    mIntent.putExtra(FileChooseActivity.EXTENSION, "csv");
                    startActivityForResult(mIntent, StudentListMakerActivity.REQUEST_CHOOSE_OPEN_FILE);
                }

                break;
            }
            case R.id.menu_save: {
                destFile = mStudentSheet.getBaseFile();
                if (destFile != null) {
                    showDialog(StudentListMakerActivity.DIALOG_ASK_OVERWRITE);
                }
                else {
                    Intent mIntent = new Intent(StudentListMakerActivity.this, FileChooseActivity.class);
                    mIntent.putExtra(FileChooseActivity.INIT_DIR_PATH, masterDir.getAbsolutePath());
                    mIntent.putExtra(FileChooseActivity.FILTER, ".*");
                    mIntent.putExtra(FileChooseActivity.EXTENSION, "csv");
                    startActivityForResult(mIntent, StudentListMakerActivity.REQUEST_CHOOSE_SAVE_FILE);
                }

                break;
            }
        }

        return true;
    }

    @Override
    public Dialog onCreateDialog(final int id) {
        Dialog retDialog = null;

        switch (id) {
            case StudentListMakerActivity.DIALOG_ASK_EXIT_WITHOUT_SAVING: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentListMakerActivity.this);
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setTitle(R.string.dialog_ask);
                builder.setMessage(R.string.dialog_ask_exit_without_saving);
                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        StudentListMakerActivity.super.onBackPressed();
                    }
                });
                builder.setNegativeButton(android.R.string.no, null);
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentListMakerActivity.DIALOG_ASK_REMOVE_WITHOUT_SAVING: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentListMakerActivity.this);
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setTitle(R.string.dialog_ask);
                builder.setMessage(R.string.dialog_ask_remove_without_saving);
                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        makeNewSheet();
                        Toast.makeText(StudentListMakerActivity.this, R.string.notice_new_sheet_created, Toast.LENGTH_SHORT).show();
                    }
                });
                builder.setNegativeButton(android.R.string.no, null);
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentListMakerActivity.DIALOG_ASK_OPEN_WITHOUT_SAVING: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentListMakerActivity.this);
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setTitle(R.string.dialog_ask);
                builder.setMessage(R.string.dialog_ask_remove_without_saving);
                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent mIntent = new Intent(StudentListMakerActivity.this, FileChooseActivity.class);
                        mIntent.putExtra(FileChooseActivity.INIT_DIR_PATH, masterDir.getAbsolutePath());
                        mIntent.putExtra(FileChooseActivity.FILTER, ".*");
                        mIntent.putExtra(FileChooseActivity.EXTENSION, "csv");
                        startActivityForResult(mIntent, StudentListMakerActivity.REQUEST_CHOOSE_OPEN_FILE);
                    }
                });
                builder.setNegativeButton(android.R.string.no, null);
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentListMakerActivity.DIALOG_ASK_ADD_STUDENT_BY_STUDENT_NO: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentListMakerActivity.this);
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setTitle(R.string.dialog_ask);
                builder.setMessage(R.string.dialog_ask_add_student_by_student_no);
                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 存在しない場合は他のリストを検索する
                        currentStudent = master.getStudentByStudentNo(readStudentNo);
                        if (currentStudent == null) {
                            // 他のリストにも存在しない場合は学籍番号のみで追加する
                            currentStudent = new Student(readStudentNo);
                        }
                        isSaved = false;
                        addStudent(currentStudent);
                        int position = mStudentListAdapter.getCount() - 1;
                        studentListView.performItemClick(studentListView, position, studentListView.getItemIdAtPosition(position));
                        studentListView.setSelection(position);
                    }
                });
                builder.setNegativeButton(android.R.string.no, null);
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentListMakerActivity.DIALOG_STUDENT_MENU: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentListMakerActivity.this);
                builder.setTitle(R.string.dialog_student_menu_title);
                builder.setItems(R.array.dialog_student_menu, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                        case 0:
                            // 編集
                            showDialog(StudentListMakerActivity.DIALOG_EDIT_STUDENT);

                            break;
                        case 1:
                            // NFCタグを全削除
                            showDialog(StudentListMakerActivity.DIALOG_ASK_REMOVE_NFC_ID);

                            break;
                        case 2:
                            // 削除
                            showDialog(StudentListMakerActivity.DIALOG_ASK_REMOVE_STUDENT);

                            break;
                        }
                    }
                });
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentListMakerActivity.DIALOG_EDIT_STUDENT: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentListMakerActivity.this);
                builder.setTitle(R.string.dialog_edit_student_title);

                LayoutInflater inflater = LayoutInflater.from(StudentListMakerActivity.this);
                View mView = inflater.inflate(R.layout.dialog_edit_student, null);
                editTextForStudentName = (EditText)mView.findViewById(R.id.dialog_student_name);
                editTextForStudentRuby = (EditText)mView.findViewById(R.id.dialog_student_ruby);

                builder.setView(mView);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String studentName = editTextForStudentName.getEditableText().toString();
                        currentStudent.setStudentName(studentName);
                        String studentRuby = editTextForStudentRuby.getEditableText().toString();
                        currentStudent.setStudentRuby(studentRuby);
                        studentListView.invalidateViews();
                        isSaved = false;
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentListMakerActivity.DIALOG_ASK_REMOVE_NFC_ID: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentListMakerActivity.this);
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setTitle(R.string.dialog_remove_nfc_id_title);
                builder.setMessage("");
                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        for (String nfcId : currentStudent.getNfcIds()) {
                            mStudentSheet.removeNfcId(nfcId);
                        }
                        currentStudent.removeAllNfcIds();
                        studentListView.invalidateViews();
                        isSaved = false;
                        Toast.makeText(StudentListMakerActivity.this,
                                       currentStudent.getStudentNo() + " " + currentStudent.getStudentName() + getString(R.string.notice_nfc_tag_removed),
                                       Toast.LENGTH_SHORT).show();
                    }
                });
                builder.setNegativeButton(android.R.string.no, null);
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentListMakerActivity.DIALOG_ASK_REMOVE_STUDENT: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentListMakerActivity.this);
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setTitle(R.string.dialog_remove_student_title);
                builder.setMessage("");
                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mStudentSheet.remove(currentStudent);
                        mStudentListAdapter.remove(currentStudent);
                        isSaved = false;
                        Toast.makeText(StudentListMakerActivity.this,
                                       currentStudent.getStudentNo() + " " + currentStudent.getStudentName() + getString(R.string.notice_student_removed),
                                       Toast.LENGTH_SHORT).show();
                        currentStudent = new Student();
                    }
                });
                builder.setNegativeButton(android.R.string.no, null);
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentListMakerActivity.DIALOG_EDIT_CLASS_NAME: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentListMakerActivity.this);
                builder.setTitle(R.string.dialog_edit_class_name_title);

                LayoutInflater inflater = LayoutInflater.from(StudentListMakerActivity.this);
                View mView = inflater.inflate(R.layout.dialog_edit_class_name, null);
                editTextForClassName = (EditText)mView.findViewById(R.id.dialog_class_name);

                builder.setView(mView);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String className = editTextForClassName.getEditableText().toString();
                        mStudentSheet.setClassName(className);
                        isSaved = false;
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentListMakerActivity.DIALOG_ADD_STUDENT_MENU: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentListMakerActivity.this);
                builder.setTitle(R.string.dialog_add_student_menu_title);
                builder.setItems(R.array.dialog_add_attendance_menu, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: {
                                // リストから追加する
                                if (master.size() != 0) {
                                    showDialog(StudentListMakerActivity.DIALOG_CSV_FILE_LIST);
                                }
                                else {
                                    Toast.makeText(StudentListMakerActivity.this, R.string.error_master_file_not_found, Toast.LENGTH_SHORT).show();
                                }

                                break;
                            }
                            case 1: {
                                // 学籍番号で検索する
                                if (master.size() != 0) {
                                    showDialog(StudentListMakerActivity.DIALOG_SEARCH_STUDENT_NO);
                                }
                                else {
                                    Toast.makeText(StudentListMakerActivity.this, R.string.error_master_file_not_found, Toast.LENGTH_SHORT).show();
                                }

                                break;
                            }
                            case 2: {
                                // 手動で登録する
                                showDialog(StudentListMakerActivity.DIALOG_INPUT_STUDENT_INFO);

                                break;
                            }
                        }
                    }
                });
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentListMakerActivity.DIALOG_CSV_FILE_LIST: {
                selectedSheet = null;
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentListMakerActivity.this);
                builder.setTitle(R.string.dialog_csv_file_list_title);
                String[] classNames = new String[master.size()];
                for (int i = 0; i < master.size(); i++) {
                    classNames[i] = master.getStudentSheet(i).getClassName();
                }
                builder.setItems(classNames, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selectedSheet = master.getStudentSheet(which);
                        showDialog(StudentListMakerActivity.DIALOG_STUDENT_LIST);
                    }
                });
                builder.setCancelable(true);
                retDialog = builder.create();
                retDialog.setOnDismissListener(new OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        removeDialog(id);
                    }
                });

                break;
            }
            case StudentListMakerActivity.DIALOG_STUDENT_LIST: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentListMakerActivity.this);
                builder.setTitle(selectedSheet.getClassName());
                final ArrayList<Student> mSheet = selectedSheet.getStudentList();
                String[] students = new String[mSheet.size()];
                for (int i = 0; i < mSheet.size(); i++) {
                    Student mStudent = mSheet.get(i);
                    students[i] = mStudent.getStudentNo() + " " + mStudent.getStudentName();
                }
                builder.setItems(students, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        currentStudent = mSheet.get(which);
                        int position;
                        if (!mStudentSheet.hasStudentNo(currentStudent.getStudentNo())) {
                            addStudent(currentStudent);
                            position = mStudentListAdapter.getCount() - 1;
                            isSaved = false;
                        }
                        else {
                            position = mStudentListAdapter.getPosition(currentStudent);
                            Toast.makeText(StudentListMakerActivity.this, R.string.error_student_already_readed, Toast.LENGTH_SHORT).show();
                        }
                        studentListView.performItemClick(studentListView, position, studentListView.getItemIdAtPosition(position));
                        studentListView.setSelection(position);
                    }
                });
                builder.setCancelable(true);
                retDialog = builder.create();
                retDialog.setOnDismissListener(new OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        removeDialog(id);
                    }
                });

                break;
            }
            case StudentListMakerActivity.DIALOG_SEARCH_STUDENT_NO: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentListMakerActivity.this);
                builder.setTitle(R.string.dialog_search_student_no_title);

                LayoutInflater inflater = LayoutInflater.from(StudentListMakerActivity.this);
                editTextForStudentNo = (EditText)inflater.inflate(R.layout.dialog_search_student_no, null);

                builder.setView(editTextForStudentNo);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String studentNo = editTextForStudentNo.getText().toString().toUpperCase();
                        if (studentNo.length() != 0) {
                            onStudentNoReaded(studentNo);
                        }
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentListMakerActivity.DIALOG_INPUT_STUDENT_INFO: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentListMakerActivity.this);
                builder.setTitle(R.string.dialog_input_student_info_title);

                LayoutInflater inflater = LayoutInflater.from(StudentListMakerActivity.this);
                View mView = inflater.inflate(R.layout.dialog_input_student_info, null);
                editTextForStudentNo   = (EditText)mView.findViewById(R.id.dialog_student_no);
                editTextForStudentName = (EditText)mView.findViewById(R.id.dialog_student_name);
                editTextForStudentRuby = (EditText)mView.findViewById(R.id.dialog_student_ruby);

                builder.setView(mView);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String studentNo = editTextForStudentNo.getEditableText().toString();
                        String studentName = editTextForStudentName.getEditableText().toString();
                        String studentRuby = editTextForStudentRuby.getEditableText().toString();

                        int position;
                        if (!mStudentSheet.hasStudentNo(studentNo)) {
                            currentStudent = new Student(studentNo, mStudentSheet.getClassName(),
                                                         studentName, studentRuby, (String[])null);
                            addStudent(currentStudent);
                            position = mStudentListAdapter.getCount() - 1;
                            isSaved = false;
                        }
                        else {
                            currentStudent = mStudentSheet.getByStudentNo(studentNo);
                            position = mStudentListAdapter.getPosition(currentStudent);
                        }
                        studentListView.performItemClick(studentListView, position, studentListView.getItemIdAtPosition(position));
                        studentListView.setSelection(position);
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentListMakerActivity.DIALOG_ASK_OVERWRITE: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentListMakerActivity.this);
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setTitle(R.string.dialog_ask);
                builder.setMessage("");
                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveCsvFile(destFile, StudentListMakerActivity.CHARACTER_CODE);
                    }
                });
                builder.setNeutralButton(R.string.dialog_save_as, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent mIntent = new Intent(StudentListMakerActivity.this, FileChooseActivity.class);
                        String initDirPath;
                        File parent = destFile.getParentFile();
                        if (parent != null) {
                            initDirPath = parent.getAbsolutePath();
                        }
                        else {
                            initDirPath = "/";
                        }
                        mIntent.putExtra(FileChooseActivity.INIT_DIR_PATH, initDirPath);
                        mIntent.putExtra(FileChooseActivity.FILTER, ".*");
                        mIntent.putExtra(FileChooseActivity.EXTENSION, "csv");
                        startActivityForResult(mIntent, StudentListMakerActivity.REQUEST_CHOOSE_SAVE_FILE);
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentListMakerActivity.DIALOG_REFRESHING_MASTER_FILE: {
                ProgressDialog mProgressDialog = new ProgressDialog(StudentListMakerActivity.this);
                mProgressDialog.setMessage(getString(R.string.dialog_refreshing_master_file));
                mProgressDialog.setCancelable(false);
                retDialog = mProgressDialog;

                break;
            }
        }

        return(retDialog);
    }

    @Override
    public void onPrepareDialog(int id, Dialog dialog) {
        AlertDialog mAlertDialog = null;
        if (dialog instanceof AlertDialog) {
            mAlertDialog = (AlertDialog)dialog;
        }

        switch (id) {
            case StudentListMakerActivity.DIALOG_STUDENT_MENU: {
                mAlertDialog.setTitle(currentStudent.getStudentNo() + " " + currentStudent.getStudentName());

                break;
            }
            case StudentListMakerActivity.DIALOG_EDIT_STUDENT: {
                mAlertDialog.setTitle(currentStudent.getStudentNo() + " " + currentStudent.getStudentName());
                editTextForStudentName.setText(currentStudent.getStudentName());
                editTextForStudentRuby.setText(currentStudent.getStudentRuby());

                break;
            }
            case StudentListMakerActivity.DIALOG_ASK_REMOVE_NFC_ID: {
                mAlertDialog.setMessage(currentStudent.getStudentNo() + " " + currentStudent.getStudentName()
                                        + getString(R.string.dialog_remove_nfc_id_message));

                break;
            }
            case StudentListMakerActivity.DIALOG_ASK_REMOVE_STUDENT: {
                mAlertDialog.setMessage(currentStudent.getStudentNo() + " " + currentStudent.getStudentName()
                                        + getString(R.string.dialog_remove_student_message));

                break;
            }
            case StudentListMakerActivity.DIALOG_EDIT_CLASS_NAME: {
                editTextForClassName.setText(mStudentSheet.getClassName());

                break;
            }
            case StudentListMakerActivity.DIALOG_SEARCH_STUDENT_NO: {
                editTextForStudentNo.setText("");

                break;
            }
            case StudentListMakerActivity.DIALOG_INPUT_STUDENT_INFO: {
                editTextForStudentNo.setText("");
                editTextForStudentName.setText("");
                editTextForStudentRuby.setText("");

                break;
            }
            case StudentListMakerActivity.DIALOG_ASK_OVERWRITE: {
                if (destFile != null) {
                    mAlertDialog.setMessage(destFile.getName() + getString(R.string.dialog_ask_overwrite));
                }

                break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (!isSaved) {
            showDialog(StudentListMakerActivity.DIALOG_ASK_EXIT_WITHOUT_SAVING);
        }
        else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean retBool;

        if (event.isPrintingKey()) {
            /**
             * 入力されたキーが文字の場合は処理を行う
             * 参考:Android onKey 時に KeyCode を文字に変えるには？ >> Tech Blog
             *      http://falco.sakura.ne.jp/tech/2011/09/android-onkey-時に-keycode-を文字に変えるには？/
             */
            onCharTyped((char)event.getUnicodeChar());

            retBool = true;
        }
        else {
            retBool = super.onKeyDown(keyCode, event);
        }

        return retBool;
    }

    @Override
    public void onNewIntent(Intent intent) {
        String action = intent.getAction();
        if (action.equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
            // NFCタグの読み取りで発生したインテントである場合
            onNfcTagReaded(intent);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("IsSaved", isSaved);
        outState.putString("ReadStudentNo", readStudentNo);
        outState.putSerializable("CurrentStudent", currentStudent);
        outState.putSerializable("SelectedSheet", selectedSheet);
        outState.putSerializable("StudentSheet", mStudentSheet);
    }

    /**
     * 出席データをCSV形式で保存する
     * @param csvFile 保存先のインスタンス
     * @param encode 書き込む際に使用する文字コード
     */
    public void saveCsvFile(File csvFile, String encode) {
        try {
            mStudentSheet.saveCsvFile(csvFile, encode);
            isSaved = true;
            Toast.makeText(StudentListMakerActivity.this, csvFile.getName() + getString(R.string.notice_csv_file_saved), Toast.LENGTH_SHORT).show();
        }
        catch (IOException e) {
            Toast.makeText(StudentListMakerActivity.this, csvFile.getName() + getString(R.string.error_saving_failed), Toast.LENGTH_SHORT).show();
            Log.e("onActivityResult", e.getMessage(), e);
        }
    }

    /**
     * 文字が入力された際に呼び出される
     * @param c 入力された文字
     */
    public void onCharTyped(char c) {
        if (Character.isLetter(c)) {
            inputBuffer.setLength(0);
            c = Character.toUpperCase(c);
        }
        inputBuffer.append(c);
        if (inputBuffer.length() == 6) {
            onStudentNoReaded(inputBuffer.toString());
        }
    }

    /**
     * 学籍番号を読み取った際に呼び出される
     * @param studentNo 学籍番号
     */
    public void onStudentNoReaded(String studentNo) {
        if (mStudentSheet.hasStudentNo(studentNo)) {
            // 学籍番号に対応するデータが存在する場合はその行を選択する
            currentStudent = mStudentSheet.getByStudentNo(studentNo);
            int position = mStudentListAdapter.getPosition(currentStudent);
            studentListView.performItemClick(studentListView, position, studentListView.getItemIdAtPosition(position));
            studentListView.setSelection(position);
        }
        else {
            if (mPreferenceUtil.getBehaviorStudentNo(PreferenceUtil.BEHAVIOR_DIALOG) == PreferenceUtil.BEHAVIOR_DIALOG) {
                readStudentNo = studentNo;
                showDialog(StudentListMakerActivity.DIALOG_ASK_ADD_STUDENT_BY_STUDENT_NO);
            }
            else {
                Toast.makeText(StudentListMakerActivity.this, R.string.error_student_not_found, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * NFCタグを読み取った際に呼び出される
     * @param inIntent NFCタグを読み取った際に発生したインテント
     */
    public void onNfcTagReaded(Intent inIntent) {
        if (currentStudent.getStudentNo().length() != 0) {
            StringBuilder rawId = new StringBuilder(Util.byteArrayToHexString(inIntent.getByteArrayExtra(NfcAdapter.EXTRA_ID)));
            while (rawId.length() < 16) {
                rawId.append("0");
            }
            String id = rawId.toString();
            if (!currentStudent.hasNfcId(id)) {
                // 登録されていないNFCタグであれば読み取り済みかどうかを調べ、
                // 読み取り済みでなければ追加
                if (!mStudentSheet.hasNfcId(id)) {
                    currentStudent.addNfcId(id);
                    mStudentSheet.addNfcId(id, currentStudent);
                }
                else {
                    Toast.makeText(StudentListMakerActivity.this, R.string.error_nfc_id_already_registered, Toast.LENGTH_SHORT).show();
                }
            }
            else {
                // 登録されているNFCタグであれば削除
                currentStudent.removeNfcId(id);
                mStudentSheet.removeNfcId(id);
            }
            studentListView.invalidateViews();
            isSaved = false;
        }
    }

    /**
     * 新規シートを作成する
     */
    public void makeNewSheet() {
        mStudentSheet = new StudentSheet();
        mStudentListAdapter = new StudentListAdapter(StudentListMakerActivity.this, 0);
        studentListView.setAdapter(mStudentListAdapter);
        isSaved = true;
    }

    /**
     * 学生データを追加する
     * @param inStudent 学生データ
     */
    public void addStudent(Student inStudent) {
        mStudentSheet.add(inStudent);
        mStudentListAdapter.add(inStudent);
    }

    /**
     * 学生マスタを読み込み直す
     */
    public void refreshStudentMaster() {
        showDialog(StudentListMakerActivity.DIALOG_REFRESHING_MASTER_FILE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    master.refresh();
                }
                catch (IOException e) {
                    Toast.makeText(StudentListMakerActivity.this, R.string.error_master_file_open_failed, Toast.LENGTH_SHORT).show();
                    Log.e("refreshStudentMaster", e.getMessage(), e);

                    finish();
                }
                finally {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                dismissDialog(StudentListMakerActivity.DIALOG_REFRESHING_MASTER_FILE);
                            }
                            catch (IllegalArgumentException e) {}
                        }
                    });
                }
            }
        }).start();
    }
}