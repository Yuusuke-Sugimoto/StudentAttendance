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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

/**
 * メイン画面
 * @author 杉本祐介
 */
public class StudentAttendanceActivity extends Activity {
    // 定数の宣言
    // リクエストコード
    private static final int REQUEST_CHOOSE_OPEN_FILE = 0;
    private static final int REQUEST_CHOOSE_SAVE_FILE = 1;
    // ダイアログのID
    private static final int DIALOG_ATTENDANCE_MENU = 0;
    private static final int DIALOG_ASK_OVERWRITE   = 1;

    // 変数の宣言
    /**
     * 保存用ディレクトリ
     */
    private File baseDir;
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
        studentListView.setSelector(R.drawable.list_selector_background);
        mStudentListAdapter = new StudentListAdapter(StudentAttendanceActivity.this, 0);
        studentListView.setAdapter(mStudentListAdapter);
        studentListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                currentStudent = (Student)parent.getItemAtPosition(position);
                showDialog(StudentAttendanceActivity.DIALOG_ATTENDANCE_MENU);
            }
        });

        // 保存用ディレクトリの作成
        baseDir = new File(Environment.getExternalStorageDirectory(), "");
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            Toast.makeText(StudentAttendanceActivity.this, R.string.error_make_directory_failed, Toast.LENGTH_SHORT).show();

            finish();
        }

        mNfcAdapter = NfcAdapter.getDefaultAdapter(StudentAttendanceActivity.this);
        mPendingIntent = PendingIntent.getActivity(StudentAttendanceActivity.this, 0,
                                                   new Intent(StudentAttendanceActivity.this, getClass())
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
            mNfcAdapter.enableForegroundDispatch(StudentAttendanceActivity.this, mPendingIntent, filters, techs);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(StudentAttendanceActivity.this);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case StudentAttendanceActivity.REQUEST_CHOOSE_OPEN_FILE:
            if (resultCode == Activity.RESULT_OK) {
                String fileName = data.getStringExtra("fileName");
                String filePath = data.getStringExtra("filePath");
                try {
                    mSheet = new Sheet(new File(filePath), "Shift_JIS");
                    mStudentListAdapter = new StudentListAdapter(StudentAttendanceActivity.this, 0, mSheet.getStudentList());
                    studentListView.setAdapter(mStudentListAdapter);
                    Toast.makeText(StudentAttendanceActivity.this, fileName + getString(R.string.notice_csv_file_opened), Toast.LENGTH_SHORT).show();
                }
                catch (IOException e) {
                    Toast.makeText(StudentAttendanceActivity.this, fileName + getString(R.string.error_opening_failed), Toast.LENGTH_SHORT).show();
                    Log.e("onActivityResult", e.getMessage(), e);
                }
            }

            break;
        case StudentAttendanceActivity.REQUEST_CHOOSE_SAVE_FILE:
            if (resultCode == Activity.RESULT_OK) {
                String fileName = data.getStringExtra("fileName");
                String filePath = data.getStringExtra("filePath");
                saveFile = new File(filePath);
                if (saveFile.exists()) {
                    showDialog(StudentAttendanceActivity.DIALOG_ASK_OVERWRITE);
                }
                else {
                    try {
                        mSheet.saveCsvFile(saveFile, "Shift_JIS");
                        Toast.makeText(StudentAttendanceActivity.this, fileName + getString(R.string.notice_csv_file_saved), Toast.LENGTH_SHORT).show();
                    }
                    catch (IOException e) {
                        Toast.makeText(StudentAttendanceActivity.this, fileName + R.string.error_saving_failed, Toast.LENGTH_SHORT).show();
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
        case R.id.menu_open:
            mIntent = new Intent(StudentAttendanceActivity.this, FileChooseActivity.class);
            mIntent.putExtra("initDirPath", baseDir.getAbsolutePath());
            mIntent.putExtra("filter", ".*");
            mIntent.putExtra("extension", "csv");
            startActivityForResult(mIntent, StudentAttendanceActivity.REQUEST_CHOOSE_OPEN_FILE);

            break;
        case R.id.menu_save:
            mIntent = new Intent(StudentAttendanceActivity.this, FileChooseActivity.class);
            mIntent.putExtra("initDirPath", baseDir.getAbsolutePath());
            mIntent.putExtra("filter", ".*");
            mIntent.putExtra("extension", "csv");
            startActivityForResult(mIntent, StudentAttendanceActivity.REQUEST_CHOOSE_SAVE_FILE);

            break;
        }

        return true;
    }

    @Override
    public Dialog onCreateDialog(final int id) {
        Dialog retDialog = null;

        AlertDialog.Builder builder;

        switch (id) {
        case StudentAttendanceActivity.DIALOG_ATTENDANCE_MENU:
            builder = new AlertDialog.Builder(StudentAttendanceActivity.this);
            builder.setTitle(R.string.dialog_attendance_menu_title);
            builder.setItems(R.array.dialog_attendance_menu, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                    case 0:
                        // 出席
                        break;
                    case 1:
                        // 遅刻
                        break;
                    case 2:
                        // 早退
                        break;
                    case 3:
                        // 欠席
                        break;
                    }
                }
            });
            builder.setCancelable(true);
            retDialog = builder.create();

            break;
        case StudentAttendanceActivity.DIALOG_ASK_OVERWRITE:
            builder = new AlertDialog.Builder(StudentAttendanceActivity.this);
            builder.setTitle(R.string.dialog_ask);
            builder.setMessage("");
            builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        mSheet.saveCsvFile(saveFile, "Shift_JIS");
                        Toast.makeText(StudentAttendanceActivity.this, saveFile.getName() + getString(R.string.notice_csv_file_saved), Toast.LENGTH_SHORT).show();
                    }
                    catch (IOException e) {
                        Toast.makeText(StudentAttendanceActivity.this, saveFile.getName() + R.string.error_saving_failed, Toast.LENGTH_SHORT).show();
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
                    Intent mIntent = new Intent(StudentAttendanceActivity.this, FileChooseActivity.class);
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
                    startActivityForResult(mIntent, StudentAttendanceActivity.REQUEST_CHOOSE_SAVE_FILE);
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
        case StudentAttendanceActivity.DIALOG_ATTENDANCE_MENU:
            mAlertDialog.setTitle(currentStudent.getStudentNo() + " " + currentStudent.getStudentName());

            break;
        case StudentAttendanceActivity.DIALOG_ASK_OVERWRITE:
            mAlertDialog.setMessage(saveFile.getName() + getString(R.string.dialog_ask_overwrite));

            break;
        }
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
     * NFCタグを読み取った際に呼び出される
     * @param inIntent NFCタグを読み取った際に発生したインテント
     */
    public void onNfcTagReaded(Intent inIntent) {
        StringBuilder rawId = new StringBuilder(Util.byteArrayToHexString(inIntent.getByteArrayExtra(NfcAdapter.EXTRA_ID)));
        while(rawId.length() < 16) {
            rawId.append("0");
        }
        String id = rawId.toString();
    }
}