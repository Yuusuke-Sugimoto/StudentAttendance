package jp.ddo.kingdragon;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.tech.TagTechnology;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

/***
 * メイン画面
 *
 * @author 杉本祐介
 */
public class StudentListMakerActivity extends Activity {
    // 変数の宣言
    /***
     * 保存用ディレクトリ
     */
    private File baseDir;
    /***
     * 学籍番号
     */
    private StringBuilder studentNo;
    /***
     * 現在扱っている学生データ
     */
    private Student currentStudent;
    /***
     * ベースレイアウト
     */
    private LinearLayout base;
    /***
     * 現在編集しているテキストビュー
     */
    private TextView currentText;
    /***
     * 現在編集しているシート
     */
    private Sheet mSheet;

    /***
     * NFCタグの読み取りに使用
     */
    private NfcAdapter adapter;
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

        studentNo = new StringBuilder();
        currentStudent = new Student();
        mSheet = new Sheet();

        base = (LinearLayout)findViewById(R.id.base);
        currentText = new TextView(StudentListMakerActivity.this);
        base.addView(currentText);

        // 保存用ディレクトリの作成
        baseDir = new File(Environment.getExternalStorageDirectory(), "");
        try {
            if(!baseDir.exists() && !baseDir.mkdirs()) {
                Toast.makeText(StudentListMakerActivity.this, R.string.error_make_directory_failed, Toast.LENGTH_SHORT).show();

                finish();
            }
        }
        catch(Exception e) {
            Toast.makeText(StudentListMakerActivity.this, R.string.error_make_directory_failed, Toast.LENGTH_SHORT).show();
            Log.e("onCreate", e.getMessage(), e);

            finish();
        }

        /***
         * NFCタグの情報を読み取る
         * 参考:i.2 高度な NFC - ソフトウェア技術ドキュメントを勝手に翻訳
         *      http://www.techdoctranslator.com/android/guide/nfc/advanced-nfc
         */
        adapter = NfcAdapter.getDefaultAdapter(StudentListMakerActivity.this);
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

        if(adapter != null) {
            adapter.enableForegroundDispatch(StudentListMakerActivity.this, mPendingIntent, filters, techs);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if(adapter != null) {
            adapter.disableForegroundDispatch(StudentListMakerActivity.this);
        }
    }

    /***
     * オプションメニューを作成
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);

        return(true);
    }

    /***
     * オプションメニューの項目が選択された際の動作を設定
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retBool = false;

        switch(item.getItemId()) {
        case R.id.menu_open:
            mSheet = new Sheet(new File(baseDir, "test.csv"), "Shift_JIS");

            retBool = true;

            break;
        case R.id.menu_save:
            if(currentStudent.isDataPrepared() && mSheet.searchByStudentNo(currentStudent.getStudentNo()) == -1) {
                mSheet.add(currentStudent);
            }
            mSheet.saveCsvFile(new File(baseDir, "temp.csv"), "Shift_JIS");

            retBool = true;

            break;
        }

        return(retBool);
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

        return(retBool);
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
     *
     * @param bytes
     *     バイト配列
     * @return 16進数表現の文字列
     */
    public static String byteArrayToHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();

        for(byte b : bytes) {
            // 下位8ビットのみ取り出す
            int temp = b & 0xff;
            if(temp < 0x10) {
                // 10未満の場合は0を付加
                hexString.append("0");
            }
            hexString.append(Integer.toHexString(temp).toUpperCase());
        }

        return(hexString.toString());
    }

    /***
     * 文字が入力された際に呼び出される
     *
     * @param c
     *     入力された文字
     */
    public void onCharTyped(char c) {
        if(currentStudent.isDataPrepared()) {
            if(mSheet.searchByStudentNo(currentStudent.getStudentNo()) == -1) {
                mSheet.add(currentStudent);
            }
            currentStudent = new Student();
            currentText = new TextView(StudentListMakerActivity.this);
            base.addView(currentText);
        }

        if(Character.isLetter(c)) {
            studentNo.delete(0, studentNo.length());
            c = Character.toUpperCase(c);
        }
        studentNo.append(c);
        int index = mSheet.searchByStudentNo(studentNo.toString());
        if(index != -1) {
            currentStudent = mSheet.get(index);
        }
        else {
            currentStudent.setStudentNo(studentNo.toString());
        }
        currentText.setText(studentNo.toString());
    }

    /***
     * NFCタグを読み取った際に呼び出される
     *
     * @param inIntent
     *     NFCタグを読み取った際に発生したインテント
     */
    public void onNfcTagReaded(Intent inIntent) {
        if(studentNo.length() != 0) {
            String id = byteArrayToHexString(inIntent.getByteArrayExtra(NfcAdapter.EXTRA_ID));
            if(id.length() < 16) {
                StringBuilder temp = new StringBuilder();
                for(int i = 0; i < 16 - id.length(); i++) {
                    temp.append("0");
                }
                id += temp.toString();
            }
            currentStudent.addNfcId(id);
            TextView mTextView = new TextView(StudentListMakerActivity.this);
            mTextView.setText(id);
            base.addView(mTextView);
        }
    }
}