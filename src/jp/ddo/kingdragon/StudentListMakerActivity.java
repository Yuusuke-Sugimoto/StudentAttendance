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
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;

/***
 * メイン画面
 * @author 杉本祐介
 */
public class StudentListMakerActivity extends Activity {
    // 定数の宣言
    // リクエストコード
    public static final int REQUEST_CHOOSE_OPEN_FILE = 0;
    public static final int REQUEST_CHOOSE_SAVE_FILE = 1;
    // ダイアログのID
    public static final int DIALOG_SET_SUBJECT = 0;
    public static final int DIALOG_SET_TIME    = 1;

    // 変数の宣言
    /***
     * 保存用ディレクトリ
     */
    private File baseDir;
    /***
     * キーボード(バーコードリーダ)から入力された内容
     */
    private StringBuilder inputBuffer;
    /***
     * 現在扱っている学生データ
     */
    private Student currentStudent;
    /***
     * 学生の一覧を表示するビュー
     */
    private ListView studentListView;
    /***
     * 学生の一覧を表示するアダプタ
     */
    private StudentListAdapter mStudentListAdapter;
    /***
     * 現在編集しているシート
     */
    private Sheet mSheet;

    /***
     * NFCタグの読み取りに使用
     */
    private NfcAdapter mNfcAdapter;
    /***
     * NFCタグの読み取りに使用
     */
    private PendingIntent mPendingIntent;

    // 配列の宣言
    /***
     * 対応するインテントの種類
     */
    private IntentFilter[] filters;
    /***
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

        /***
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
        if(!baseDir.exists() && !baseDir.mkdirs()) {
            Toast.makeText(StudentListMakerActivity.this, R.string.error_make_directory_failed, Toast.LENGTH_SHORT).show();

            finish();
        }

        /***
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

        if(mNfcAdapter != null) {
            mNfcAdapter.enableForegroundDispatch(StudentListMakerActivity.this, mPendingIntent, filters, techs);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if(mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(StudentListMakerActivity.this);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
        case StudentListMakerActivity.REQUEST_CHOOSE_OPEN_FILE:
            if(resultCode == Activity.RESULT_OK) {
                String fileName = data.getStringExtra("fileName");
                String filePath = data.getStringExtra("filePath");
                mSheet = new Sheet(new File(filePath), "Shift_JIS");
                mStudentListAdapter = new StudentListAdapter(StudentListMakerActivity.this, 0, mSheet.getStudentList());
                studentListView.setAdapter(mStudentListAdapter);
                Toast.makeText(StudentListMakerActivity.this, fileName + getString(R.string.notice_csv_file_opened), Toast.LENGTH_SHORT).show();
            }

            break;
        case StudentListMakerActivity.REQUEST_CHOOSE_SAVE_FILE:
            break;
        default:
            break;
        }
    }

    /***
     * オプションメニューを作成
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);

        return true;
    }

    /***
     * オプションメニューの項目が選択された際の動作を設定
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retBool = false;

        switch(item.getItemId()) {
        case R.id.menu_set_subject:
            showDialog(StudentListMakerActivity.DIALOG_SET_SUBJECT);

            retBool = true;

            break;
        case R.id.menu_set_time:
            showDialog(StudentListMakerActivity.DIALOG_SET_TIME);

            retBool = true;

            break;
        case R.id.menu_open:
            Intent mIntent = new Intent(StudentListMakerActivity.this, FileChooseActivity.class);
            mIntent.putExtra("initDirPath", baseDir.getAbsolutePath());
            mIntent.putExtra("filter", ".*\\.csv");
            startActivityForResult(mIntent, StudentListMakerActivity.REQUEST_CHOOSE_OPEN_FILE);

            retBool = true;

            break;
        case R.id.menu_save:
            mSheet.saveCsvFile(new File(baseDir, "out.csv"), "Shift_JIS");
            Toast.makeText(StudentListMakerActivity.this, R.string.notice_csv_file_saved, Toast.LENGTH_SHORT).show();

            retBool = true;

            break;
        }

        return retBool;
    }

    /***
     * ダイアログを生成する
     */
    @Override
    public Dialog onCreateDialog(int id) {
        Dialog retDialog = null;

        AlertDialog.Builder builder;
        final EditText mEditText;

        switch(id) {
        case StudentListMakerActivity.DIALOG_SET_SUBJECT:
            builder = new AlertDialog.Builder(StudentListMakerActivity.this);
            builder.setTitle(R.string.menu_set_subject);
            mEditText = new EditText(StudentListMakerActivity.this);
            mEditText.setHint(R.string.dialog_set_subject_hint);
            mEditText.setText(mSheet.getSubject());
            builder.setView(mEditText);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String subject = mEditText.getEditableText().toString();
                    if(subject.length() != 0) {
                        mSheet.setSubject(subject);
                    }
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);
            retDialog = builder.create();
            retDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    removeDialog(0);
                }
            });

            break;
        case StudentListMakerActivity.DIALOG_SET_TIME:
            builder = new AlertDialog.Builder(StudentListMakerActivity.this);
            builder.setTitle(R.string.dialog_set_time_hint);
            mEditText = new EditText(StudentListMakerActivity.this);
            mEditText.setText(mSheet.getTime());
            builder.setView(mEditText);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String time = mEditText.getEditableText().toString();
                    if(time.length() != 0) {
                        mSheet.setTime(time);
                    }
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);
            retDialog = builder.create();
            retDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    removeDialog(1);
                }
            });

            break;
        }

        return(retDialog);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean retBool;

        if(event.isPrintingKey()) {
            /***
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
        if(action.equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
            // NFCタグの読み取りで発生したインテントである場合
            onNfcTagReaded(intent);
        }
    }

    /***
     * バイト配列を16進数表現の文字列にして返す<br />
     * 参考:16進数文字列(String)⇔バイト配列(byte[]) - lambda {|diary| lambda { diary.succ! } }.call(hatena)<br />
     *      http://d.hatena.ne.jp/winebarrel/20041012/p1
     * @param bytes バイト配列
     * @return 16進数表現の文字列
     */
    public static String byteArrayToHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();

        for(byte b : bytes) {
            // 下位8ビットのみ取り出す
            int bottomByte = b & 0xff;
            if(bottomByte < 0x10) {
                // 10未満の場合は0を付加
                hexString.append("0");
            }
            hexString.append(Integer.toHexString(bottomByte).toUpperCase());
        }

        return hexString.toString();
    }

    /***
     * 文字が入力された際に呼び出される
     * @param c 入力された文字
     */
    public void onCharTyped(char c) {
        if(Character.isLetter(c)) {
            inputBuffer.setLength(0);
            c = Character.toUpperCase(c);
            currentStudent = new Student();
        }
        inputBuffer.append(c);
        if(inputBuffer.length() == 6) {
            onStudentNoReaded(inputBuffer.toString());
        }
    }

    /***
     * 学籍番号を読み取った際に呼び出される
     * @param studentNo 学籍番号
     */
    public void onStudentNoReaded(String studentNo) {
        if(mSheet.hasStudentNo(studentNo)) {
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

    /***
     * NFCタグを読み取った際に呼び出される
     * @param inIntent NFCタグを読み取った際に発生したインテント
     */
    public void onNfcTagReaded(Intent inIntent) {
        if(currentStudent.getStudentNo().length() != 0) {
            StringBuilder id = new StringBuilder(byteArrayToHexString(inIntent.getByteArrayExtra(NfcAdapter.EXTRA_ID)));
            while(id.length() < 16) {
                id.append("0");
            }
            currentStudent.addNfcId(id.toString());
            studentListView.invalidateViews();
        }
    }
}