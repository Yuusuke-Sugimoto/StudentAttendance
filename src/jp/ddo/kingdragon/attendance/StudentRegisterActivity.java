package jp.ddo.kingdragon.attendance;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;

import jp.ddo.kingdragon.attendance.student.Student;
import jp.ddo.kingdragon.attendance.student.StudentListAdapter;
import jp.ddo.kingdragon.attendance.student.StudentMaster;
import jp.ddo.kingdragon.attendance.student.StudentSheet;
import jp.ddo.kingdragon.attendance.util.PreferenceUtil;
import jp.ddo.kingdragon.attendance.util.Util;

/**
 * 学生登録画面
 * @author 杉本祐介
 */
public class StudentRegisterActivity extends Activity {
    // 定数の宣言
    // ダイアログのID
    private static final int DIALOG_ASK_EXIT_WITHOUT_SAVING       = 0;
    private static final int DIALOG_ASK_ADD_STUDENT_BY_STUDENT_NO = 1;
    private static final int DIALOG_STUDENT_MENU                  = 2;
    private static final int DIALOG_EDIT_STUDENT                  = 3;
    private static final int DIALOG_ASK_REMOVE_NFC_ID             = 4;
    private static final int DIALOG_ASK_REMOVE_STUDENT            = 5;
    private static final int DIALOG_ADD_CLASS                     = 6;
    private static final int DIALOG_FILE_ALREADY_EXISTS           = 7;
    private static final int DIALOG_FILE_CREATE_FAILED            = 8;
    private static final int DIALOG_ILLEGAL_FILE_NAME             = 9;
    private static final int DIALOG_FILE_NAME_IS_NULL             = 10;
    private static final int DIALOG_CLASS_ALREADY_EXISTS          = 11;
    private static final int DIALOG_OPERATION_MENU                = 12;
    private static final int DIALOG_SEARCH_STUDENT_NO             = 13;
    private static final int DIALOG_INPUT_STUDENT_INFO            = 14;
    private static final int DIALOG_EDIT_CLASS_NAME               = 15;
    private static final int DIALOG_ASK_OVERWRITE                 = 16;
    private static final int DIALOG_REFRESHING_MASTER_FILE        = 17;
    /** 使用する文字コード */
    private static final String CHARACTER_CODE = "Shift_JIS";

    // 変数の宣言
    /** アプリケーションクラス */
    private CustomApplication application;

    /** 他スレッドからのUIの更新に使用 */
    private Handler mHandler;

    /** 学生マスタが保存済みかどうか */
    private boolean isStudentMasterSaved;
    /** 現在編集中のシートが保存済みかどうか */
    private boolean isSheetSaved;
    /** 保存中かどうか */
    private boolean isSaving;
    /** 再読み込み中かどうか */
    private boolean isRefreshing;

    /** ベースフォルダ */
    private File baseDir;
    /** マスタフォルダ */
    private File masterDir;
    /** 保存先のファイル */
    private File destFile;

    /** キーボード(バーコードリーダ)から入力された内容 */
    private StringBuilder inputBuffer;
    /** 追加待ちの学籍番号 */
    private String readStudentNo;
    /** 最後に読み取ったNFCタグのUID */
    private String readNfcId;
    /** 現在扱っている学生データ */
    private Student currentStudent;
    /** 現在編集しているシートの元の所属名 */
    private String originClassName;
    /** 現在編集しているシート */
    private StudentSheet mStudentSheet;
    /** 学生マスタ */
    private StudentMaster master;

    /** UID用のTextView */
    private TextView textViewForNfcId;
    /** 登録情報用のTextView */
    private TextView textViewForRegistrationInfo;
    /** 学生の一覧を表示するビュー */
    private ListView studentListView;
    /** 学生の一覧を表示するアダプタ */
    private StudentListAdapter mStudentListAdapter;
    /** 表示する所属を選択するスピナー */
    private Spinner classSpinner;
    /** 所属追加時の所属名用のEditText */
    private EditText editTextForClassNameForAdd;
    /** 所属追加時のファイル名用のEditText */
    private EditText editTextForFileName;
    /** 編集時の所属名用のEditText */
    private EditText editTextForClassNameForEdit;
    /** 検索時の学籍番号用のEditText */
    private EditText editTextForStudentNoForSearch;
    /** 手動登録時の学籍番号用のEditText */
    private EditText editTextForStudentNoForManual;
    /** 氏名用のEditText */
    private EditText editTextForStudentName;
    /** カナ用のEditText */
    private EditText editTextForStudentRuby;
    /** 学生リスト読み込み状況表示用のProgressDialog */
    private ProgressDialog progressDialogForRefresh;
    /** 前回表示時のprogressDialogForRefreshのMaxの値 */
    private int prevMax;

    /** 設定内容の読み取り/変更に使用 */
    private PreferenceUtil mPreferenceUtil;

    /** NFCタグの読み取りに使用 */
    private NfcAdapter mNfcAdapter;
    /** NFCタグの読み取りに使用 */
    private PendingIntent mPendingIntent;

    // 配列の宣言
    /** 対応するインテントの種類 */
    private IntentFilter[] filters;
    /** 対応させるタグの一覧 */
    private String[][] techs;

    // リストの宣言
    /** 更新されたシートのリスト */
    private LinkedHashMap<String, StudentSheet> updatedSheets;

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.student_register);

        application = (CustomApplication)getApplication();

        mHandler = new Handler();

        isStudentMasterSaved = true;
        isSheetSaved = true;
        isSaving = false;
        isRefreshing = false;

        mPreferenceUtil = new PreferenceUtil(StudentRegisterActivity.this);

        // 各フォルダの作成
        baseDir = new File(Environment.getExternalStorageDirectory(), "StudentAttendance");
        masterDir = new File(baseDir, "StudentMaster");
        if (!masterDir.exists() && !masterDir.mkdirs()) {
            Toast.makeText(StudentRegisterActivity.this, R.string.error_make_master_directory_failed, Toast.LENGTH_SHORT).show();

            finish();
        }

        inputBuffer = new StringBuilder();
        master = application.getStudentMaster();
        if (master == null) {
            Toast.makeText(StudentRegisterActivity.this, R.string.error_master_file_open_failed, Toast.LENGTH_SHORT).show();

            finish();
        }
        else {
            master.setOnRefreshListener(new StudentMaster.OnRefreshListener() {
                @Override
                public void onRefreshBegin(final int num) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            isRefreshing = true;
                            showDialog(StudentRegisterActivity.DIALOG_REFRESHING_MASTER_FILE);
                            progressDialogForRefresh.setMax(num);
                            progressDialogForRefresh.setProgress(0);
                            progressDialogForRefresh.setMessage("");
                            prevMax = num;
                        }
                    });
                }

                @Override
                public void onOpenBegin(final String fileName) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressDialogForRefresh.setMessage(fileName + getString(R.string.notice_student_master_opening));
                        }
                    });
                }

                @Override
                public void onOpenFinish(String fileName) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressDialogForRefresh.incrementProgressBy(1);
                        }
                    });
                }

                @Override
                public void onRefreshFinish() {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressDialogForRefresh.setProgress(progressDialogForRefresh.getMax());
                            progressDialogForRefresh.setMessage(getString(R.string.notice_refresh_finish));

                            try {
                                dismissDialog(StudentRegisterActivity.DIALOG_REFRESHING_MASTER_FILE);
                            }
                            catch (IllegalArgumentException e) {}
                            isRefreshing = false;

                            ArrayAdapter<String> adapter = new ArrayAdapter<String>(StudentRegisterActivity.this, android.R.layout.simple_spinner_item,
                                                                                    master.getClassNames());
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            classSpinner.setAdapter(adapter);
                            int index = master.getIndexByClassName(originClassName);
                            if (index != -1) {
                                classSpinner.setSelection(index);
                            }
                        }
                    });
                }

                @Override
                public void onError(final String fileName, final IOException e) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressDialogForRefresh.incrementProgressBy(1);
                            Toast.makeText(StudentRegisterActivity.this, fileName + getString(R.string.error_opening_failed), Toast.LENGTH_SHORT).show();
                            Log.e("onCreate", e.getMessage(), e);
                        }
                    });
                }
            });
        }

        if (savedInstanceState != null) {
            // アクティビティ再生成前のデータがあれば復元する
            isStudentMasterSaved = savedInstanceState.getBoolean("IsStudentMasterSaved");
            isSheetSaved = savedInstanceState.getBoolean("IsSheetSaved");
            isSaving = savedInstanceState.getBoolean("IsSaving");
            isRefreshing = savedInstanceState.getBoolean("IsRefreshing");
            destFile = (File)savedInstanceState.getSerializable("DestFile");
            readStudentNo = savedInstanceState.getString("ReadStudentNo");
            readNfcId = savedInstanceState.getString("ReadNfcId");
            currentStudent = (Student)savedInstanceState.getSerializable("CurrentStudent");
            originClassName = savedInstanceState.getString("OriginClassName");
            mStudentSheet = (StudentSheet)savedInstanceState.getSerializable("StudentSheet");
            mStudentListAdapter = new StudentListAdapter(StudentRegisterActivity.this, 0, mStudentSheet.getStudentList());
            prevMax = savedInstanceState.getInt("PrevMax", -1);
            updatedSheets = (LinkedHashMap<String, StudentSheet>)savedInstanceState.getSerializable("UpdatedSheets");
        }
        else {
            readStudentNo = null;
            readNfcId = null;
            currentStudent = new Student();
            originClassName = "";
            mStudentSheet = new StudentSheet();
            mStudentListAdapter = new StudentListAdapter(StudentRegisterActivity.this, 0);
            prevMax = -1;
            updatedSheets = new LinkedHashMap<String, StudentSheet>();
        }

        // 設定情報にデフォルト値をセットする
        PreferenceManager.setDefaultValues(StudentRegisterActivity.this, R.xml.preference, false);

        textViewForNfcId = (TextView)findViewById(R.id.nfc_id);
        textViewForRegistrationInfo = (TextView)findViewById(R.id.registration_info);
        textViewForRegistrationInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (readNfcId != null) {
                    Student mStudent = getStudentByNfcId(readNfcId);
                    if (mStudent != null) {
                        currentStudent = mStudent;
                        classSpinner.setSelection(master.getIndexByClassName(currentStudent.getClassName()));
                    }
                }
            }
        });

        classSpinner = (Spinner)findViewById(R.id.class_spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(StudentRegisterActivity.this, android.R.layout.simple_spinner_item,
                                                                master.getClassNames());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        classSpinner.setAdapter(adapter);
        classSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isSheetSaved) {
                    // 更新があればリストに一時保存
                    updatedSheets.put(originClassName, mStudentSheet);
                }

                StudentSheet sheet = master.getStudentSheetByIndex(position);
                if (updatedSheets.containsKey(sheet.getClassName())) {
                    // 一時保存したリストがある場合
                    mStudentSheet = updatedSheets.get(sheet.getClassName());
                    isSheetSaved = false;
                }
                else {
                    mStudentSheet = sheet;
                    isSheetSaved = true;
                }
                originClassName = sheet.getClassName();
                mStudentListAdapter = new StudentListAdapter(StudentRegisterActivity.this, 0, mStudentSheet.getStudentList());
                studentListView.setAdapter(mStudentListAdapter);

                int mPosition = mStudentListAdapter.getPosition(currentStudent);
                if (mPosition != -1) {
                    studentListView.performItemClick(studentListView, mPosition, studentListView.getItemIdAtPosition(mPosition));
                    studentListView.setSelection(mPosition);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        int index = master.getIndexByClassName(originClassName);
        if (index != -1) {
            classSpinner.setSelection(index);
        }
        else {
            classSpinner.setSelection(0);
        }

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
                showDialog(StudentRegisterActivity.DIALOG_STUDENT_MENU);

                return true;
            }
        });

        /**
         * NFCタグの情報を読み取る
         * 参考:i.2 高度な NFC - ソフトウェア技術ドキュメントを勝手に翻訳
         *      http://www.techdoctranslator.com/android/guide/nfc/advanced-nfc
         */
        mNfcAdapter = NfcAdapter.getDefaultAdapter(StudentRegisterActivity.this);
        mPendingIntent = PendingIntent.getActivity(StudentRegisterActivity.this, 0,
                                                   new Intent(StudentRegisterActivity.this, getClass())
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

        master = application.getStudentMaster();
        if (master == null) {
            Toast.makeText(StudentRegisterActivity.this, R.string.error_master_file_open_failed, Toast.LENGTH_SHORT).show();

            finish();
        }

        if (mNfcAdapter != null) {
            mNfcAdapter.enableForegroundDispatch(StudentRegisterActivity.this, mPendingIntent, filters, techs);
        }

        if (readNfcId != null) {
            textViewForNfcId.setText(readNfcId);
            Student mStudent = getStudentByNfcId(readNfcId);
            if (mStudent != null) {
                textViewForRegistrationInfo.setText(mStudent.getClassName() + " " + mStudent.getStudentNo() + " " + mStudent.getStudentName());
            }
            else {
                textViewForRegistrationInfo.setText(R.string.register_not_registered);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mNfcAdapter != null) {
            if (!mNfcAdapter.isEnabled()) {
                Toast.makeText(StudentRegisterActivity.this, R.string.error_nfc_read_failed, Toast.LENGTH_SHORT).show();
            }
            mNfcAdapter.disableForegroundDispatch(StudentRegisterActivity.this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.student_register_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add_class: {
                showDialog(StudentRegisterActivity.DIALOG_ADD_CLASS);

                break;
            }
            case R.id.menu_setting: {
                Intent mIntent = new Intent(StudentRegisterActivity.this, SettingActivity.class);
                startActivity(mIntent);

                break;
            }
            case R.id.menu_operation: {
                showDialog(StudentRegisterActivity.DIALOG_OPERATION_MENU);

                break;
            }
            case R.id.menu_refresh: {
                refreshStudentMaster();

                break;
            }
            case R.id.menu_edit_class_name: {
                showDialog(StudentRegisterActivity.DIALOG_EDIT_CLASS_NAME);

                break;
            }
            case R.id.menu_save: {
                showDialog(StudentRegisterActivity.DIALOG_ASK_OVERWRITE);

                break;
            }
        }

        return true;
    }

    @Override
    public Dialog onCreateDialog(final int id) {
        Dialog retDialog = null;

        switch (id) {
            case StudentRegisterActivity.DIALOG_ASK_EXIT_WITHOUT_SAVING: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentRegisterActivity.this);
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setTitle(R.string.dialog_ask);
                builder.setMessage(R.string.dialog_ask_exit_without_saving);
                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        StudentRegisterActivity.super.onBackPressed();
                    }
                });
                builder.setNegativeButton(android.R.string.no, null);
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentRegisterActivity.DIALOG_ASK_ADD_STUDENT_BY_STUDENT_NO: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentRegisterActivity.this);
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setTitle(R.string.dialog_ask);
                builder.setMessage(R.string.dialog_ask_add_student_by_student_no);
                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        currentStudent = new Student(readStudentNo);
                        addStudent(currentStudent);
                        isStudentMasterSaved = false;
                        isSheetSaved = false;
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
            case StudentRegisterActivity.DIALOG_STUDENT_MENU: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentRegisterActivity.this);
                builder.setTitle(R.string.dialog_student_menu_title);
                builder.setItems(R.array.dialog_student_menu, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: {
                                // 編集
                                showDialog(StudentRegisterActivity.DIALOG_EDIT_STUDENT);

                                break;
                            }
                            case 1: {
                                // NFCタグを全削除
                                showDialog(StudentRegisterActivity.DIALOG_ASK_REMOVE_NFC_ID);

                                break;
                            }
                            case 2: {
                                // 削除
                                showDialog(StudentRegisterActivity.DIALOG_ASK_REMOVE_STUDENT);

                                break;
                            }
                        }
                    }
                });
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentRegisterActivity.DIALOG_EDIT_STUDENT: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentRegisterActivity.this);
                builder.setTitle(R.string.dialog_edit_student_title);

                LayoutInflater inflater = LayoutInflater.from(StudentRegisterActivity.this);
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
                        isStudentMasterSaved = false;
                        isSheetSaved = false;
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentRegisterActivity.DIALOG_ASK_REMOVE_NFC_ID: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentRegisterActivity.this);
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
                        isStudentMasterSaved = false;
                        isSheetSaved = false;
                        Toast.makeText(StudentRegisterActivity.this,
                                       currentStudent.getStudentNo() + " " + currentStudent.getStudentName() + getString(R.string.notice_nfc_id_removed),
                                       Toast.LENGTH_SHORT).show();
                    }
                });
                builder.setNegativeButton(android.R.string.no, null);
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentRegisterActivity.DIALOG_ASK_REMOVE_STUDENT: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentRegisterActivity.this);
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setTitle(R.string.dialog_remove_student_title);
                builder.setMessage("");
                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mStudentSheet.remove(currentStudent);
                        mStudentListAdapter.remove(currentStudent);
                        isStudentMasterSaved = false;
                        isSheetSaved = false;
                        Toast.makeText(StudentRegisterActivity.this,
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
            case StudentRegisterActivity.DIALOG_ADD_CLASS: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentRegisterActivity.this);
                builder.setTitle(R.string.dialog_add_class_title);

                LayoutInflater inflater = LayoutInflater.from(StudentRegisterActivity.this);
                View mView = inflater.inflate(R.layout.dialog_add_class, null);
                editTextForClassNameForAdd = (EditText)mView.findViewById(R.id.dialog_class_name);
                editTextForFileName        = (EditText)mView.findViewById(R.id.dialog_file_name);

                builder.setView(mView);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String className = editTextForClassNameForAdd.getEditableText().toString();
                        String fileName  = editTextForFileName.getEditableText().append(".csv").toString();
                        int index = master.getIndexByClassName(className);
                        if (index == -1) {
                            if (fileName.length() != 0) {
                                if (!fileName.matches(".*(<|>|:|\\*|\\?|\"|/|\\\\|\\||\u00a5).*")) {
                                    // 使用不可能な文字列(< > : * ? " / \ |)が含まれていなければファイルを作成
                                    File mFile = new File(masterDir, fileName);
                                    try {
                                        if (mFile.createNewFile()) {
                                            StudentSheet sheet = new StudentSheet();
                                            sheet.setClassName(className);
                                            sheet.saveCsvFile(mFile, StudentRegisterActivity.CHARACTER_CODE);
                                            editTextForClassNameForAdd.setText("");
                                            editTextForFileName.setText("");
                                            Toast.makeText(StudentRegisterActivity.this, fileName + getString(R.string.notice_file_created), Toast.LENGTH_SHORT).show();
                                            refreshStudentMaster();
                                        }
                                        else {
                                            showDialog(StudentRegisterActivity.DIALOG_FILE_ALREADY_EXISTS);
                                        }
                                    }
                                    catch (IOException e) {
                                        showDialog(StudentRegisterActivity.DIALOG_FILE_CREATE_FAILED);
                                        Log.e("onCreateDialog", e.getMessage(), e);
                                    }
                                }
                                else {
                                    showDialog(StudentRegisterActivity.DIALOG_ILLEGAL_FILE_NAME);
                                }
                            }
                            else {
                                showDialog(StudentRegisterActivity.DIALOG_FILE_NAME_IS_NULL);
                            }
                        }
                        else {
                            showDialog(StudentRegisterActivity.DIALOG_CLASS_ALREADY_EXISTS);
                        }
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentRegisterActivity.DIALOG_FILE_ALREADY_EXISTS:
            case StudentRegisterActivity.DIALOG_FILE_CREATE_FAILED:
            case StudentRegisterActivity.DIALOG_ILLEGAL_FILE_NAME:
            case StudentRegisterActivity.DIALOG_FILE_NAME_IS_NULL:
            case StudentRegisterActivity.DIALOG_CLASS_ALREADY_EXISTS: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentRegisterActivity.this);
                builder.setTitle(R.string.error);
                builder.setMessage("");
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setCancelable(true);
                retDialog = builder.create();
                retDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        showDialog(StudentRegisterActivity.DIALOG_ADD_CLASS);
                    }
                });

                break;
            }
            case StudentRegisterActivity.DIALOG_OPERATION_MENU: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentRegisterActivity.this);
                builder.setTitle(R.string.dialog_operation_menu_title);
                builder.setItems(R.array.dialog_operation_menu, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: {
                                // 学生番号で検索する
                                showDialog(StudentRegisterActivity.DIALOG_SEARCH_STUDENT_NO);

                                break;
                            }
                            case 1: {
                                // 学生を追加する
                                showDialog(StudentRegisterActivity.DIALOG_INPUT_STUDENT_INFO);

                                break;
                            }
                        }
                    }
                });
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentRegisterActivity.DIALOG_SEARCH_STUDENT_NO: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentRegisterActivity.this);
                builder.setTitle(R.string.dialog_search_student_no_title);

                LayoutInflater inflater = LayoutInflater.from(StudentRegisterActivity.this);
                editTextForStudentNoForSearch = (EditText)inflater.inflate(R.layout.dialog_search_student_no, null);

                builder.setView(editTextForStudentNoForSearch);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String studentNo = editTextForStudentNoForSearch.getText().toString().toUpperCase();
                        if (studentNo.length() != 0) {
                            onStudentNoRead(studentNo);
                        }
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentRegisterActivity.DIALOG_INPUT_STUDENT_INFO: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentRegisterActivity.this);
                builder.setTitle(R.string.dialog_input_student_info_title);

                LayoutInflater inflater = LayoutInflater.from(StudentRegisterActivity.this);
                View mView = inflater.inflate(R.layout.dialog_input_student_info_for_register, null);
                editTextForStudentNoForManual = (EditText)mView.findViewById(R.id.dialog_student_no);
                editTextForStudentName        = (EditText)mView.findViewById(R.id.dialog_student_name);
                editTextForStudentRuby        = (EditText)mView.findViewById(R.id.dialog_student_ruby);

                builder.setView(mView);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String studentNo   = editTextForStudentNoForManual.getEditableText().toString();
                        String studentName = editTextForStudentName.getEditableText().toString();
                        String studentRuby = editTextForStudentRuby.getEditableText().toString();

                        Student mStudent = master.getStudentByStudentNo(studentNo);
                        if (mStudent == null) {
                            currentStudent = new Student(studentNo, mStudentSheet.getClassName(),
                                                         studentName, studentRuby, (String[])null);
                            addStudent(currentStudent);
                            int position = mStudentListAdapter.getCount() - 1;
                            isStudentMasterSaved = false;
                            isSheetSaved = false;
                            studentListView.performItemClick(studentListView, position, studentListView.getItemIdAtPosition(position));
                            studentListView.setSelection(position);
                        }
                        else {
                            onStudentNoRead(mStudent.getStudentNo());
                        }
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentRegisterActivity.DIALOG_EDIT_CLASS_NAME: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentRegisterActivity.this);
                builder.setTitle(R.string.dialog_edit_class_name_title);

                LayoutInflater inflater = LayoutInflater.from(StudentRegisterActivity.this);
                View mView = inflater.inflate(R.layout.dialog_edit_class_name, null);
                editTextForClassNameForEdit = (EditText)mView.findViewById(R.id.dialog_class_name);

                builder.setView(mView);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String className = editTextForClassNameForEdit.getEditableText().toString();
                        mStudentSheet.setClassName(className);
                        isStudentMasterSaved = false;
                        isSheetSaved = false;
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentRegisterActivity.DIALOG_ASK_OVERWRITE: {
                AlertDialog.Builder builder = new AlertDialog.Builder(StudentRegisterActivity.this);
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setTitle(R.string.dialog_ask);
                builder.setMessage(R.string.dialog_ask_overwrite_student_master);
                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveStudentMaster();
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.setCancelable(true);
                retDialog = builder.create();

                break;
            }
            case StudentRegisterActivity.DIALOG_REFRESHING_MASTER_FILE: {
                progressDialogForRefresh = new ProgressDialog(StudentRegisterActivity.this);
                progressDialogForRefresh.setTitle(getString(R.string.dialog_refreshing_master_file_title));
                progressDialogForRefresh.setMessage("");
                if (prevMax != -1) {
                    progressDialogForRefresh.setMax(prevMax);
                }
                progressDialogForRefresh.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialogForRefresh.setCancelable(false);
                retDialog = progressDialogForRefresh;

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
            case StudentRegisterActivity.DIALOG_STUDENT_MENU: {
                mAlertDialog.setTitle(currentStudent.getStudentNo() + " " + currentStudent.getStudentName());

                break;
            }
            case StudentRegisterActivity.DIALOG_EDIT_STUDENT: {
                mAlertDialog.setTitle(currentStudent.getStudentNo() + " " + currentStudent.getStudentName());
                editTextForStudentName.setText(currentStudent.getStudentName());
                editTextForStudentRuby.setText(currentStudent.getStudentRuby());

                break;
            }
            case StudentRegisterActivity.DIALOG_ASK_REMOVE_NFC_ID: {
                mAlertDialog.setMessage(currentStudent.getStudentNo() + " " + currentStudent.getStudentName()
                                        + getString(R.string.dialog_remove_nfc_id_message));

                break;
            }
            case StudentRegisterActivity.DIALOG_ASK_REMOVE_STUDENT: {
                mAlertDialog.setMessage(currentStudent.getStudentNo() + " " + currentStudent.getStudentName()
                                        + getString(R.string.dialog_remove_student_message));

                break;
            }
            case StudentRegisterActivity.DIALOG_FILE_ALREADY_EXISTS: {
                mAlertDialog.setMessage(getString(R.string.error_file_already_exists));

                break;
            }
            case StudentRegisterActivity.DIALOG_FILE_CREATE_FAILED: {
                mAlertDialog.setMessage(getString(R.string.error_file_create_failed));

                break;
            }
            case StudentRegisterActivity.DIALOG_ILLEGAL_FILE_NAME: {
                mAlertDialog.setMessage(getString(R.string.error_illegal_file_name));

                break;
            }
            case StudentRegisterActivity.DIALOG_FILE_NAME_IS_NULL: {
                mAlertDialog.setMessage(getString(R.string.error_file_name_null));

                break;
            }
            case StudentRegisterActivity.DIALOG_CLASS_ALREADY_EXISTS: {
                mAlertDialog.setMessage(getString(R.string.error_class_already_exists));

                break;
            }
            case StudentRegisterActivity.DIALOG_EDIT_CLASS_NAME: {
                editTextForClassNameForEdit.setText(mStudentSheet.getClassName());

                break;
            }
            case StudentRegisterActivity.DIALOG_SEARCH_STUDENT_NO: {
                editTextForStudentNoForSearch.setText("");

                break;
            }
            case StudentRegisterActivity.DIALOG_INPUT_STUDENT_INFO: {
                editTextForStudentNoForManual.setText("");
                editTextForStudentName.setText("");
                editTextForStudentRuby.setText("");

                break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (!isStudentMasterSaved) {
            showDialog(StudentRegisterActivity.DIALOG_ASK_EXIT_WITHOUT_SAVING);
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
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
            // NFCタグの読み取りで発生したインテントである場合
            onNfcTagRead(intent);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("IsStudentMasterSaved", isStudentMasterSaved);
        outState.putBoolean("IsSheetSaved", isSheetSaved);
        outState.putBoolean("IsSaving", isSaving);
        outState.putBoolean("IsRefreshing", isRefreshing);
        outState.putSerializable("DestFile", destFile);
        outState.putString("ReadStudentNo", readStudentNo);
        outState.putString("ReadNfcId", readNfcId);
        outState.putSerializable("CurrentStudent", currentStudent);
        outState.putString("OriginClassName", originClassName);
        outState.putSerializable("StudentSheet", mStudentSheet);
        outState.putInt("PrevMax", prevMax);
        outState.putSerializable("UpdatedSheets", updatedSheets);
    }

    /**
     * 全ての学生マスタをCSV形式で保存する
     */
    private void saveStudentMaster() {
        if (updatedSheets.size() != 0 || !isSheetSaved) {
            isSaving = true;
            isStudentMasterSaved = true;
            for (String className : updatedSheets.keySet()) {
                StudentSheet sheet = updatedSheets.get(className);
                File baseFile = sheet.getBaseFile();
                try {
                    sheet.saveCsvFile(baseFile, StudentRegisterActivity.CHARACTER_CODE);
                    updatedSheets.remove(className);
                }
                catch (IOException e) {
                    Toast.makeText(StudentRegisterActivity.this, baseFile.getName() + getString(R.string.error_saving_failed), Toast.LENGTH_SHORT).show();
                    isStudentMasterSaved = false;
                    Log.e("saveStudentMaster", e.getMessage(), e);
                }
            }
            if (!isSheetSaved) {
                File baseFile = mStudentSheet.getBaseFile();
                try {
                    mStudentSheet.saveCsvFile(baseFile, StudentRegisterActivity.CHARACTER_CODE);
                    originClassName = mStudentSheet.getClassName();
                    isSheetSaved = true;
                }
                catch (IOException e) {
                    Toast.makeText(StudentRegisterActivity.this, baseFile.getName() + getString(R.string.error_saving_failed), Toast.LENGTH_SHORT).show();
                    isStudentMasterSaved = false;
                    Log.e("saveStudentMaster", e.getMessage(), e);
                }
            }
            Toast.makeText(StudentRegisterActivity.this, R.string.notice_student_master_saved, Toast.LENGTH_SHORT).show();
            isSaving = false;

            refreshStudentMaster();
        }
        else {
            Toast.makeText(StudentRegisterActivity.this, R.string.error_saving_data_null, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 文字が入力された際に呼び出される
     * @param c 入力された文字
     */
    private void onCharTyped(char c) {
        if (Character.isLetter(c)) {
            inputBuffer.setLength(0);
            c = Character.toUpperCase(c);
        }
        inputBuffer.append(c);
        if (inputBuffer.length() == 6) {
            onStudentNoRead(inputBuffer.toString());
        }
    }

    /**
     * 学籍番号を読み取った際に呼び出される
     * @param studentNo 学籍番号
     */
    private void onStudentNoRead(String studentNo) {
        if (!isSaving && !isRefreshing) {
            if (mStudentSheet.hasStudentNo(studentNo)) {
                // 学籍番号に対応するデータが存在する場合はその行を選択する
                currentStudent = mStudentSheet.getByStudentNo(studentNo);
                int position = mStudentListAdapter.getPosition(currentStudent);
                studentListView.performItemClick(studentListView, position, studentListView.getItemIdAtPosition(position));
                studentListView.setSelection(position);
            }
            else {
                Student mStudent = getStudentByStudentNo(studentNo);
                if (mStudent != null) {
                    currentStudent = mStudent;
                    classSpinner.setSelection(master.getIndexByClassName(currentStudent.getClassName()));
                }
                else {
                    if (mPreferenceUtil.getBehaviorStudentNo(PreferenceUtil.BEHAVIOR_DIALOG) == PreferenceUtil.BEHAVIOR_DIALOG) {
                        readStudentNo = studentNo;
                        showDialog(StudentRegisterActivity.DIALOG_ASK_ADD_STUDENT_BY_STUDENT_NO);
                    }
                    else {
                        Toast.makeText(StudentRegisterActivity.this, R.string.error_student_not_found, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    /**
     * NFCタグを読み取った際に呼び出される
     * @param inIntent NFCタグを読み取った際に発生したインテント
     */
    private void onNfcTagRead(Intent inIntent) {
        if (!isSaving && !isRefreshing) {
            readNfcId = Util.byteArrayToHexString(inIntent.getByteArrayExtra(NfcAdapter.EXTRA_ID));
            if (currentStudent.getStudentNo().length() != 0) {
                Student mStudent = getStudentByNfcId(readNfcId);
                if (mStudent == null) {
                    currentStudent.addNfcId(readNfcId);
                    mStudentSheet.addNfcId(readNfcId, currentStudent);
                    studentListView.invalidateViews();
                    isStudentMasterSaved = false;
                    isSheetSaved = false;
                }
                else {
                    if (mStudent.equals(currentStudent)) {
                        // 登録されているNFCタグであれば削除
                        currentStudent.removeNfcId(readNfcId);
                        mStudentSheet.removeNfcId(readNfcId);
                        studentListView.invalidateViews();
                    }
                    else {
                        Toast.makeText(StudentRegisterActivity.this, R.string.error_nfc_id_already_registered, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    /**
     * 学生データを追加する
     * @param inStudent 学生データ
     */
    private void addStudent(Student inStudent) {
        mStudentSheet.add(inStudent);
        mStudentListAdapter.add(inStudent);
    }

    /**
     * 指定された学籍番号を持つ学生データを取得する<br>
     * 編集中のリストがある場合はそれを参照する。
     * @param studentNo 学籍番号
     * @return 学生データ 該当するデータがなければnull
     */
    private Student getStudentByStudentNo(String studentNo) {
        Student retStudent = null;

        for (int i = 0; retStudent == null && i < master.size(); i++) {
            String className = master.getStudentSheetByIndex(i).getClassName();
            StudentSheet sheet;
            if (className.equals(originClassName)) {
                sheet = mStudentSheet;
            }
            else {
                if (updatedSheets.containsKey(className)) {
                    sheet = updatedSheets.get(className);
                }
                else {
                    sheet = master.getStudentSheetByClassName(className);
                }
            }
            retStudent = sheet.getByStudentNo(studentNo);
        }

        return retStudent;
    }

    /**
     * 指定されたNFCタグを持つ学生データを取得する<br>
     * 編集中のリストがある場合はそれを参照する。
     * @param id NFCタグのID
     * @return 学生データ 該当するデータがなければnull
     */
    private Student getStudentByNfcId(String id) {
        Student retStudent = null;

        for (int i = 0; retStudent == null && i < master.size(); i++) {
            String className = master.getStudentSheetByIndex(i).getClassName();
            StudentSheet sheet;
            if (className.equals(originClassName)) {
                sheet = mStudentSheet;
            }
            else {
                if (updatedSheets.containsKey(className)) {
                    sheet = updatedSheets.get(className);
                }
                else {
                    sheet = master.getStudentSheetByClassName(className);
                }
            }
            retStudent = sheet.getByNfcId(id);
        }

        return retStudent;
    }

    /** 学生マスタを読み込み直す */
    private void refreshStudentMaster() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    master.refresh();
                    application.setStudentMaster(master);
                }
                catch (UnsupportedEncodingException e) {
                    Toast.makeText(StudentRegisterActivity.this, R.string.error_master_file_open_failed, Toast.LENGTH_SHORT).show();
                    Log.e("refreshStudentMaster", e.getMessage(), e);

                    finish();
                }
            }
        }).start();
    }
}