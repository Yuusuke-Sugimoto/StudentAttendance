package jp.ddo.kingdragon;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.tech.TagTechnology;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.TextView;

/***
 * メイン画面
 *
 * @author 杉本祐介
 */
public class StudentListMakerActivity extends Activity {
    TextView mTextView;
    StringBuilder studentId;
    NfcAdapter adapter;
    PendingIntent mPendingIntent;

    IntentFilter[] filters;
    String[][] techs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mTextView = (TextView)findViewById(R.id.text);
        studentId = new StringBuilder();

        /***
         * NFCタグの情報を読み取る
         * 参考:i.2 高度な NFC - ソフトウェア技術ドキュメントを勝手に翻訳
         *      http://www.techdoctranslator.com/android/guide/nfc/advanced-nfc
         */
        adapter = NfcAdapter.getDefaultAdapter(StudentListMakerActivity.this);
        mPendingIntent = PendingIntent.getActivity(StudentListMakerActivity.this, 0,
                                                   new Intent(StudentListMakerActivity.this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean retBool = true;

        if(event.isPrintingKey()) {
            /***
             * 入力されたキーが文字の場合は処理を行う
             * 参考:Android onKey 時に KeyCode を文字に変えるには？ >> Tech Blog
             *      http://falco.sakura.ne.jp/tech/2011/09/android-onkey-時に-keycode-を文字に変えるには？/
             */
            onCharDetected((char)event.getUnicodeChar());
        }
        else {
            // それ以外の場合は処理を投げる
            retBool = super.onKeyDown(keyCode, event);
        }

        return(retBool);
    }

    @Override
    public void onNewIntent(Intent intent) {
        String action = intent.getAction();
        if(action.equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
            // NFCタグの読み取りで発生したインテントである場合
            byte[] id = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
            onNfcReaded(byteArrayToHexString(id));
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
            hexString.append(Integer.toHexString(temp));
        }

        return(hexString.toString());
    }

    /***
     * 文字を検出した時に呼び出される
     *
     * @param c
     *     検出された文字
     */
    public void onCharDetected(char c) {
        if(Character.isLetter(c)) {
            studentId.delete(0, studentId.length());
            c = Character.toUpperCase(c);
        }
        studentId.append(c);
        mTextView.setText(studentId.toString());
    }

    /***
     * NFCタグを読み取った際に呼び出される
     *
     * @param id
     *     読み取ったNFCタグのID
     */
    public void onNfcReaded(String id) {
        mTextView.setText(id);
    }
}