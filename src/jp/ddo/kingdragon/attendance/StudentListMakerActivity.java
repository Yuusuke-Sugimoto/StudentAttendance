package jp.ddo.kingdragon.attendance;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.tech.TagTechnology;
import android.os.Bundle;
import android.os.Environment;
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

import jp.ddo.kingdragon.attendance.filechoose.FileChooseActivity;
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
    private static final int DIALOG_ASK_EXIT_WITHOUT_SAVING   = 0;
    private static final int DIALOG_ASK_REMOVE_WITHOUT_SAVING = 1;
    private static final int DIALOG_STUDENT_MENU              = 2;
    private static final int DIALOG_EDIT_STUDENT              = 3;
    private static final int DIALOG_ASK_REMOVE_NFC_ID         = 4;
    private static final int DIALOG_ASK_REMOVE_STUDENT        = 5;
    private static final int DIALOG_EDIT_INFO                 = 6;
    private static final int DIALOG_ASK_OVERWRITE             = 7;

    // 変数の宣言
    /**
     * 保存済みかどうか
     */
    private boolean isSaved;

    /**
     * 保存用ディレクトリ
     */
    private File baseDir;
    /**
     * 保存先のファイル
     */
    private File saveFile;
    /**
     * キーボード(バーコードリーダ)から入力された内容
     */
    private StringBuilder inputBuffer;
    /**
     * 現在扱っている学生データ
     */
    private Student currentStudent;
    /**
     * 学生の一覧を表示するビュー
     */
    private ListView studentListView;
    /**
     * 学生の一覧を表示するアダプタ
     */
    private StudentListAdapter mStudentListAdapter;
    /**
     * 現在編集しているシート
     */
    private StudentSheet mStudentSheet;
    /**
     * 連番用のEditText
     */
    private EditText editTextForStudentNum;
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
     * 科目名用のEditText
     */
    private EditText editTextForSubject;
    /**
     * 授業時間用のEditText
     */
    private EditText editTextForTime;

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

        isSaved = true;
        inputBuffer = new StringBuilder();
        currentStudent = new Student();
        mStudentSheet = new StudentSheet();

        /**
         * ListViewのレイアウトを変更する
         * 参考:リストビューをカスタマイズする | Tech Booster
         *      http://techbooster.org/android/ui/1282/
         *
         *      List14.java | Android Develpers
         *      http://developer.android.com/resources/samples/ApiDemos/src/com/example/android/apis/view/List14.html
         */
        studentListView = (ListView)findViewById(R.id.student_list);
        mStudentListAdapter = new StudentListAdapter(StudentListMakerActivity.this, 0);
        studentListView.setAdapter(mStudentListAdapter);
        studentListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                currentStudent = (Student)parent.getItemAtPosition(position);
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

        // 保存用ディレクトリの作成
        baseDir = new File(Environment.getExternalStorageDirectory(), "StudentAttendance");
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            Toast.makeText(StudentListMakerActivity.this, R.string.error_make_directory_failed, Toast.LENGTH_SHORT).show();

            finish();
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
        case StudentListMakerActivity.REQUEST_CHOOSE_OPEN_FILE:
            if (resultCode == Activity.RESULT_OK) {
                String fileName = data.getStringExtra("fileName");
                String filePath = data.getStringExtra("filePath");
                try {
                    mStudentSheet = new StudentSheet(new File(filePath), "Shift_JIS");
                    mStudentListAdapter = new StudentListAdapter(StudentListMakerActivity.this, 0, mStudentSheet.getStudentList());
                    studentListView.setAdapter(mStudentListAdapter);
                    isSaved = false;
                    Toast.makeText(StudentListMakerActivity.this, fileName + getString(R.string.notice_csv_file_opened), Toast.LENGTH_SHORT).show();
                }
                catch (IOException e) {
                    Toast.makeText(StudentListMakerActivity.this, fileName + getString(R.string.error_opening_failed), Toast.LENGTH_SHORT).show();
                    Log.e("onActivityResult", e.getMessage(), e);
                }
            }

            break;
        case StudentListMakerActivity.REQUEST_CHOOSE_SAVE_FILE:
            if (resultCode == Activity.RESULT_OK) {
                String fileName = data.getStringExtra("fileName");
                String filePath = data.getStringExtra("filePath");
                saveFile = new File(filePath);
                if (saveFile.exists()) {
                    showDialog(StudentListMakerActivity.DIALOG_ASK_OVERWRITE);
                }
                else {
                    try {
                        mStudentSheet.saveCsvFile(saveFile, "Shift_JIS");
                        isSaved = true;
                        Toast.makeText(StudentListMakerActivity.this, fileName + getString(R.string.notice_csv_file_saved), Toast.LENGTH_SHORT).show();
                    }
                    catch (IOException e) {
                        Toast.makeText(StudentListMakerActivity.this, fileName + getString(R.string.error_saving_failed), Toast.LENGTH_SHORT).show();
                        Log.e("onActivityResult", e.getMessage(), e);
                    }
                }
            }

            break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.student_list_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent mIntent;

        switch (item.getItemId()) {
        case R.id.menu_new_sheet:
            if (!isSaved) {
                showDialog(StudentListMakerActivity.DIALOG_ASK_REMOVE_WITHOUT_SAVING);
            }
            else {
                makeNewSheet();
                Toast.makeText(StudentListMakerActivity.this, R.string.notice_new_sheet_created, Toast.LENGTH_SHORT).show();
            }

            break;
        case R.id.menu_edit_info:
            showDialog(StudentListMakerActivity.DIALOG_EDIT_INFO);

            break;
        case R.id.menu_open:
            mIntent = new Intent(StudentListMakerActivity.this, FileChooseActivity.class);
            mIntent.putExtra("initDirPath", baseDir.getAbsolutePath());
            mIntent.putExtra("filter", ".*");
            mIntent.putExtra("extension", "csv");
            startActivityForResult(mIntent, StudentListMakerActivity.REQUEST_CHOOSE_OPEN_FILE);

            break;
        case R.id.menu_save:
            mIntent = new Intent(StudentListMakerActivity.this, FileChooseActivity.class);
            mIntent.putExtra("initDirPath", baseDir.getAbsolutePath());
            mIntent.putExtra("filter", ".*");
            mIntent.putExtra("extension", "csv");
            startActivityForResult(mIntent, StudentListMakerActivity.REQUEST_CHOOSE_SAVE_FILE);

            break;
        }

        return true;
    }

    @Override
    public Dialog onCreateDialog(final int id) {
        Dialog retDialog = null;

        AlertDialog.Builder builder;
        LayoutInflater inflater;
        View mView;

        switch (id) {
        case StudentListMakerActivity.DIALOG_ASK_EXIT_WITHOUT_SAVING:
            builder = new AlertDialog.Builder(StudentListMakerActivity.this);
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
        case StudentListMakerActivity.DIALOG_ASK_REMOVE_WITHOUT_SAVING:
            builder = new AlertDialog.Builder(StudentListMakerActivity.this);
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
        case StudentListMakerActivity.DIALOG_STUDENT_MENU:
            builder = new AlertDialog.Builder(StudentListMakerActivity.this);
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
        case StudentListMakerActivity.DIALOG_EDIT_STUDENT:
            builder = new AlertDialog.Builder(StudentListMakerActivity.this);
            builder.setTitle(R.string.dialog_edit_student_title);

            inflater = LayoutInflater.from(StudentListMakerActivity.this);
            mView = inflater.inflate(R.layout.dialog_edit_student, null);
            editTextForStudentNum  = (EditText)mView.findViewById(R.id.dialog_student_num);
            editTextForStudentNo   = (EditText)mView.findViewById(R.id.dialog_student_no);
            editTextForClassName   = (EditText)mView.findViewById(R.id.dialog_class_name);
            editTextForStudentName = (EditText)mView.findViewById(R.id.dialog_student_name);
            editTextForStudentRuby = (EditText)mView.findViewById(R.id.dialog_student_ruby);

            builder.setView(mView);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String studentNum = editTextForStudentNum.getEditableText().toString();
                    currentStudent.setStudentNum(Integer.parseInt(studentNum));
                    String studentNo = editTextForStudentNo.getEditableText().toString();
                    currentStudent.setStudentNo(studentNo);
                    String className = editTextForClassName.getEditableText().toString();
                    currentStudent.setClassName(className);
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
        case StudentListMakerActivity.DIALOG_ASK_REMOVE_NFC_ID:
            builder = new AlertDialog.Builder(StudentListMakerActivity.this);
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setTitle(R.string.dialog_remove_nfc_id_title);
            builder.setMessage("");
            builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
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
        case StudentListMakerActivity.DIALOG_ASK_REMOVE_STUDENT:
            builder = new AlertDialog.Builder(StudentListMakerActivity.this);
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
        case StudentListMakerActivity.DIALOG_EDIT_INFO:
            builder = new AlertDialog.Builder(StudentListMakerActivity.this);
            builder.setTitle(R.string.dialog_edit_info_title);

            inflater = LayoutInflater.from(StudentListMakerActivity.this);
            mView = inflater.inflate(R.layout.dialog_edit_info, null);
            editTextForSubject = (EditText)mView.findViewById(R.id.dialog_subject);
            editTextForTime    = (EditText)mView.findViewById(R.id.dialog_time);

            builder.setView(mView);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String subject = editTextForSubject.getEditableText().toString();
                    mStudentSheet.setSubject(subject);
                    String time = editTextForTime.getEditableText().toString();
                    mStudentSheet.setTime(time);
                    isSaved = false;
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.setCancelable(true);
            retDialog = builder.create();

            break;
        case StudentListMakerActivity.DIALOG_ASK_OVERWRITE:
            builder = new AlertDialog.Builder(StudentListMakerActivity.this);
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setTitle(R.string.dialog_ask);
            builder.setMessage("");
            builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        mStudentSheet.saveCsvFile(saveFile, "Shift_JIS");
                        isSaved = true;
                        Toast.makeText(StudentListMakerActivity.this, saveFile.getName() + getString(R.string.notice_csv_file_saved), Toast.LENGTH_SHORT).show();
                    }
                    catch (IOException e) {
                        Toast.makeText(StudentListMakerActivity.this, saveFile.getName() + getString(R.string.error_saving_failed), Toast.LENGTH_SHORT).show();
                        Log.e("onActivityResult", e.getMessage(), e);
                    }
                }
            });
            builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    Intent mIntent = new Intent(StudentListMakerActivity.this, FileChooseActivity.class);
                    String initDirPath;
                    File parent = saveFile.getParentFile();
                    if (parent != null) {
                        initDirPath = parent.getAbsolutePath();
                    }
                    else {
                        initDirPath = "/";
                    }
                    mIntent.putExtra("initDirPath", initDirPath);
                    mIntent.putExtra("filter", ".*");
                    mIntent.putExtra("extension", "csv");
                    startActivityForResult(mIntent, StudentListMakerActivity.REQUEST_CHOOSE_SAVE_FILE);
                }
            });
            builder.setCancelable(true);
            retDialog = builder.create();

            break;
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
        case StudentListMakerActivity.DIALOG_STUDENT_MENU:
            mAlertDialog.setTitle(currentStudent.getStudentNo() + " " + currentStudent.getStudentName());

            break;
        case StudentListMakerActivity.DIALOG_EDIT_STUDENT:
            mAlertDialog.setTitle(currentStudent.getStudentNo() + " " + currentStudent.getStudentName());
            editTextForStudentNum.setText(String.valueOf(currentStudent.getStudentNum()));
            editTextForStudentNo.setText(currentStudent.getStudentNo());
            editTextForClassName.setText(currentStudent.getClassName());
            editTextForStudentName.setText(currentStudent.getStudentName());
            editTextForStudentRuby.setText(currentStudent.getStudentRuby());

            break;
        case StudentListMakerActivity.DIALOG_ASK_REMOVE_NFC_ID:
            mAlertDialog.setMessage(currentStudent.getStudentNo() + " " + currentStudent.getStudentName()
                                    + getString(R.string.dialog_remove_nfc_id_message));

            break;
        case StudentListMakerActivity.DIALOG_ASK_REMOVE_STUDENT:
            mAlertDialog.setMessage(currentStudent.getStudentNo() + " " + currentStudent.getStudentName()
                                    + getString(R.string.dialog_remove_student_message));

            break;
        case StudentListMakerActivity.DIALOG_EDIT_INFO:
            editTextForSubject.setText(mStudentSheet.getSubject());
            editTextForTime.setText(mStudentSheet.getTime());

            break;
        case StudentListMakerActivity.DIALOG_ASK_OVERWRITE:
            mAlertDialog.setMessage(saveFile.getName() + getString(R.string.dialog_ask_overwrite));

            break;
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

    /**
     * 文字が入力された際に呼び出される
     * @param c 入力された文字
     */
    public void onCharTyped(char c) {
        if (Character.isLetter(c)) {
            inputBuffer.setLength(0);
            c = Character.toUpperCase(c);
            currentStudent = new Student();
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
        int position;
        if (mStudentSheet.hasStudentNo(studentNo)) {
            // 既に学籍番号に対応するデータが存在する場合はその行を選択する
            position = mStudentListAdapter.getPosition(mStudentSheet.get(studentNo));
        }
        else {
            // 存在しない場合は追加する
            currentStudent.setStudentNo(studentNo);
            mStudentSheet.add(currentStudent);
            mStudentListAdapter.add(currentStudent);
            position = mStudentListAdapter.getCount() - 1;
            isSaved = false;
        }
        studentListView.performItemClick(studentListView, position, studentListView.getItemIdAtPosition(position));
        studentListView.setSelection(position);
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
                if (!mStudentSheet.isNfcIdReaded(id)) {
                    currentStudent.addNfcId(id);
                    mStudentSheet.addReadedNfcId(id);
                }
                else {
                    Toast.makeText(StudentListMakerActivity.this, R.string.error_nfc_id_already_readed, Toast.LENGTH_SHORT).show();
                }
            }
            else {
                // 登録されているNFCタグであれば削除
                currentStudent.removeNfcId(id);
                mStudentSheet.removeReadedNfcId(id);
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
}