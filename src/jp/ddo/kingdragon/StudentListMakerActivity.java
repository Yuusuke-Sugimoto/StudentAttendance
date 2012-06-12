package jp.ddo.kingdragon;

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
import android.text.InputType;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

/**
 * メイン画面
 * @author 杉本祐介
 */
public class StudentListMakerActivity extends Activity {
    // 定数の宣言
    // リクエストコード
    private static final int REQUEST_CHOOSE_OPEN_FILE = 0;
    private static final int REQUEST_CHOOSE_SAVE_FILE = 1;
    // ダイアログのID
    private static final int DIALOG_EDIT_INFO     = 0;
    private static final int DIALOG_ASK_OVERWRITE = 1;

    // 変数の宣言
    /**
     * 保存用ディレクトリ
     */
    private File baseDir;
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
    private Sheet mSheet;
    /**
     * 保存先のファイル
     */
    private File saveFile;
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
        setContentView(R.layout.main);

        inputBuffer = new StringBuilder();
        currentStudent = new Student();
        mSheet = new Sheet();

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

        // 保存用ディレクトリの作成
        baseDir = new File(Environment.getExternalStorageDirectory(), "");
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
                mSheet = new Sheet(new File(filePath), "Shift_JIS");
                mStudentListAdapter = new StudentListAdapter(StudentListMakerActivity.this, 0, mSheet.getStudentList());
                studentListView.setAdapter(mStudentListAdapter);
                Toast.makeText(StudentListMakerActivity.this, fileName + getString(R.string.notice_csv_file_opened), Toast.LENGTH_SHORT).show();
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
                    if(mSheet.saveCsvFile(saveFile, "Shift_JIS")) {
                        Toast.makeText(StudentListMakerActivity.this, fileName + getString(R.string.notice_csv_file_saved), Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(StudentListMakerActivity.this, R.string.error_saving_failed, Toast.LENGTH_SHORT).show();
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
            mSheet = new Sheet();
            mStudentListAdapter = new StudentListAdapter(StudentListMakerActivity.this, 0);
            studentListView.setAdapter(mStudentListAdapter);
            Toast.makeText(StudentListMakerActivity.this, R.string.notice_new_sheet_created, Toast.LENGTH_SHORT).show();

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

        switch (id) {
        case StudentListMakerActivity.DIALOG_EDIT_INFO:
            builder = new AlertDialog.Builder(StudentListMakerActivity.this);
            builder.setTitle(R.string.menu_edit_info);
            
            LinearLayout layout = new LinearLayout(StudentListMakerActivity.this);
            layout.setOrientation(LinearLayout.VERTICAL);
            
            TextView textViewForSubject = new TextView(StudentListMakerActivity.this);
            textViewForSubject.setText(R.string.dialog_edit_subject_label);
            layout.addView(textViewForSubject);
            
            editTextForSubject = new EditText(StudentListMakerActivity.this);
            editTextForSubject.setInputType(InputType.TYPE_CLASS_TEXT);
            editTextForSubject.setMaxLines(1);
            editTextForSubject.setHint(R.string.dialog_edit_subject_hint);
            layout.addView(editTextForSubject);
            
            TextView textViewForTime = new TextView(StudentListMakerActivity.this);
            textViewForTime.setText(R.string.dialog_edit_time_label);
            layout.addView(textViewForTime);
            
            editTextForTime = new EditText(StudentListMakerActivity.this);
            editTextForTime.setInputType(InputType.TYPE_CLASS_TEXT);
            editTextForTime.setMaxLines(1);
            editTextForTime.setHint(R.string.dialog_edit_time_hint);
            layout.addView(editTextForTime);
            
            builder.setView(layout);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String subject = editTextForSubject.getEditableText().toString();
                    mSheet.setSubject(subject);
                    String time = editTextForTime.getEditableText().toString();
                    mSheet.setTime(time);
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);
            retDialog = builder.create();
            retDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    removeDialog(id);
                }
            });

            break;
        case StudentListMakerActivity.DIALOG_ASK_OVERWRITE:
            builder = new AlertDialog.Builder(StudentListMakerActivity.this);
            builder.setTitle(R.string.dialog_ask);
            builder.setMessage("");
            builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mSheet.saveCsvFile(saveFile, "Shift_JIS");
                    Toast.makeText(StudentListMakerActivity.this, saveFile.getName() + getString(R.string.notice_csv_file_saved), Toast.LENGTH_SHORT).show();
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
                    if(parent != null) {
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
        if(dialog instanceof AlertDialog) {
            mAlertDialog = (AlertDialog)dialog;
        }
        
        switch(id) {
        case StudentListMakerActivity.DIALOG_EDIT_INFO:
            editTextForSubject.setText(mSheet.getSubject());
            editTextForTime.setText(mSheet.getTime());
            
            break;
        case StudentListMakerActivity.DIALOG_ASK_OVERWRITE:
            mAlertDialog.setMessage(saveFile.getName() + getString(R.string.dialog_ask_overwrite));
            
            break;
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
     * バイト配列を16進数表現の文字列にして返す<br />
     * 参考:16進数文字列(String)⇔バイト配列(byte[]) - lambda {|diary| lambda { diary.succ! } }.call(hatena)<br />
     *      http://d.hatena.ne.jp/winebarrel/20041012/p1
     * @param bytes バイト配列
     * @return 16進数表現の文字列
     */
    public static String byteArrayToHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();

        for (byte b : bytes) {
            // 下位8ビットのみ取り出す
            int bottomByte = b & 0xff;
            if (bottomByte < 0x10) {
                // 10未満の場合は0を付加
                hexString.append("0");
            }
            hexString.append(Integer.toHexString(bottomByte).toUpperCase());
        }

        return hexString.toString();
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
        if (mSheet.hasStudentNo(studentNo)) {
            // 既に学籍番号に対応するデータが存在する場合はそのデータを取り出す
            currentStudent = mSheet.get(studentNo);
            // 対象の行を選択する
            studentListView.setSelection(mStudentListAdapter.getPosition(currentStudent));
        }
        else {
            // 存在しない場合は追加する
            currentStudent.setStudentNo(studentNo);
            mSheet.add(currentStudent);
            mStudentListAdapter.add(currentStudent);
            studentListView.setSelection(mStudentListAdapter.getCount() - 1);
        }
    }

    /**
     * NFCタグを読み取った際に呼び出される
     * @param inIntent NFCタグを読み取った際に発生したインテント
     */
    public void onNfcTagReaded(Intent inIntent) {
        if (currentStudent.getStudentNo().length() != 0) {
            StringBuilder id = new StringBuilder(byteArrayToHexString(inIntent.getByteArrayExtra(NfcAdapter.EXTRA_ID)));
            while (id.length() < 16) {
                id.append("0");
            }
            currentStudent.addNfcId(id.toString());
            studentListView.invalidateViews();
        }
    }
}