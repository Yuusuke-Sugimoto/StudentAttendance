package jp.ddo.kingdragon.attendance;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;

import android.util.Log;

import jp.ddo.kingdragon.attendance.student.Attendance;

/**
 * サーバに送信する出席データを管理するクラス
 * @author 杉本祐介
 */
public class SendAttendanceQueue implements Iterable<Attendance> {
    // 変数の宣言
    /**
     * 送信先のアドレス
     */
    private String serverAddress;
    /**
     * 文字コード
     */
    private String characterCode;
    /**
     * 何ミリ秒でタイムアウトにするか
     */
    private int timeout;
    /**
     * 送信中の出席データ
     */
    private Attendance sendingAttendance;
    /**
     * 送信中かどうか
     */
    private boolean isSending;
    /**
     * このインスタンスに対応するリスナ
     */
    private SendAttendanceListener listener;

    /**
     * 送信待ちの出席データのキュー
     */
    private LinkedList<Attendance> queue;
    /**
     * 失敗した出席データのキュー　
     */
    private LinkedList<Attendance> incompleteAttendanceQueue;

    // コンストラクタ 
    /**
     * 送信先のアドレスと文字コード、タイムアウトの時間を指定してインスタンスを生成する
     * @param serverAddress 送信先のアドレス
     * @param characterCode 文字コード
     * @param タイムアウト(ミリ秒)
     */
    private SendAttendanceQueue(String serverAddress, String characterCode, int timeout) {
        this.serverAddress = serverAddress;
        this.characterCode = characterCode;
        this.timeout = timeout;
        sendingAttendance = null;
        isSending = false;
        listener = null;
        queue = new LinkedList<Attendance>();
        incompleteAttendanceQueue = new LinkedList<Attendance>();
    }

    @Override
    public Iterator<Attendance> iterator() {
        return queue.iterator();
    }
    
    /**
     * 送信中の出席データを返す
     * @return 送信中の出席データ 送信中でなければnull
     */
    public Attendance getSendingAttendance() {
        return sendingAttendance;
    }

    /**
     * 送信中かどうかを返す
     * @return 送信中ならばtrue そうでなければfalse
     */
    public boolean isSending() {
        return isSending;
    }
    
    /**
     * 待機中の出席データを削除する
     */
    public void removeWaitingAttendances() {
        queue.clear();
    }
    
    /**
     * 未送信の出席データを保持しているかどうかを返す
     * @return 保持していればtrue そうでなければfalse
     */
    public boolean hasIncompleteAttendances() {
        return !incompleteAttendanceQueue.isEmpty();
    }
    
    /**
     * 未送信の出席データのリストを返す
     * @return 未送信の出席データのリスト
     */
    public LinkedList<Attendance> getIncompleteAttendances() {
        return incompleteAttendanceQueue;
    }
    
    /**
     * 未送信の出席データを削除する
     */
    public void removeIncompleteAttendances() {
        incompleteAttendanceQueue.clear();
    }

    /**
     * リスナをセットする
     * @param listener リスナ
     */
    public void setSendAttendanceListener(SendAttendanceListener listener) {
        this.listener = listener;
    }

    /**
     * キューに出席データを追加する
     * @param inAttendance 出席データ
     */
    public void enqueue(Attendance inAttendance) {
        queue.offer(inAttendance);
        if (listener != null) {
            listener.onEnqueue(inAttendance);
        }
        if (!isSending) {
            execute();
        }
    }

    /**
     * 出席データを送信する
     */
    private void execute() {
        sendingAttendance = queue.poll();
        if (sendingAttendance != null) {
            isSending = true;
            if (listener != null) {
                listener.onPostStart(sendingAttendance);
            }
            sendAttendance(sendingAttendance);
        }
        else {
            isSending = false;
            if (listener != null) {
                listener.onAllTaskEnd();
            }
        }
    }

    /**
     * 出席データを送信する処理
     */
    private void sendAttendance(Attendance inAttendance) {
        final String studentNum = String.valueOf(inAttendance.getStudentNum());
        final String className = inAttendance.getClassName();
        final String studentNo = inAttendance.getStudentNo();
        final String studentName = inAttendance.getStudentName();
        final String studentRuby = inAttendance.getStudentRuby();
        final String statusString = inAttendance.getStatusString();
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        final String timeStamp = format.format(new Date(inAttendance.getTimeStamp()));
        final String latitude = String.valueOf(inAttendance.getLatitude());
        final String longitude = String.valueOf(inAttendance.getLongitude());

        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                BufferedReader br = null;
                /***
                 * POST送信
                 * 参考:挫折から始まる Androidアプリ開発日誌～ときどきJava Tips etc...～  Androidでhttp通信をしてみよう（HttpUrlConnectionによるPOST編）
                 *      http://yukimura1227.blog.fc2.com/blog-entry-36.html
                 */
                try {
                    URL mUrl = new URL(serverAddress);
                    connection = (HttpURLConnection)mUrl.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);
                    connection.setConnectTimeout(timeout);

                    PrintStream ps = new PrintStream(connection.getOutputStream());
                    ps.print("number=" + URLEncoder.encode(studentNum, characterCode)
                             + "&belong=" + URLEncoder.encode(className, characterCode)
                             + "&id=" + URLEncoder.encode(studentNo, characterCode)
                             + "&name=" + URLEncoder.encode(studentName, characterCode)
                             + "&kana=" + URLEncoder.encode(studentRuby, characterCode)
                             + "&safety=" + URLEncoder.encode(statusString, characterCode)
                             + "&time=" + URLEncoder.encode(timeStamp, characterCode)
                             + "&latitude=" + URLEncoder.encode(latitude, characterCode)
                             + "&longitude=" + URLEncoder.encode(longitude, characterCode));
                    ps.close();

                    br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    while (br.readLine() != null) {}
                }
                catch (IOException e) {
                    Log.e("sendAttendance", e.getMessage(), e);
                }
                finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                    if (br != null) {
                        try {
                            br.close();
                        }
                        catch (IOException e) {
                            Log.e("sendAttendance", e.getMessage(), e);
                        }
                    }
                }
            }
        }).start();
    }

    /**
     * 送信に成功した際に呼び出される
     * @param mPostTask 送信に成功した出席データ
     */
    public void onPostSuccess(Attendance mPostTask) {
        if (listener != null) {
            listener.onPostSuccess(mPostTask);
        }
    }

    /**
     * 送信に失敗した際に呼び出される
     * @param mPostTask 送信に失敗した出席データ
     */
    public void onPostFailure(Attendance mPostTask) {
        incompleteAttendanceQueue.offer(mPostTask);
        
        if (listener != null) {
            listener.onPostFailure(mPostTask);
        }
    }

    /**
     * 送信に終了した際に成功/失敗に関わらず呼び出される
     * @param mPostTask 送信が終了した出席データ
     */
    public void onPostEnd(Attendance mPostTask) {
        if (listener != null) {
            listener.onPostEnd(mPostTask);
        }

        execute();
    }
    
    /**
     * 送信に失敗した出席データをリトライする
     */
    public void retry() {
        Attendance task;
        while ((task = incompleteAttendanceQueue.poll()) != null) {
            enqueue(task);
        }
    }

    /**
     * 送信出席データの各動作に対応するリスナクラス
     * @author 杉本祐介
     */
    public interface SendAttendanceListener {
        /**
         * 送信出席データがキューに追加された際に呼び出される
         * @param mPostTask キューに追加された出席データ
         */
        void onEnqueue(Attendance mPostTask);

        /**
         * 送信を開始した際に呼び出される
         * @param mPostTask 送信を開始した出席データ
         */
        void onPostStart(Attendance mPostTask);

        /**
         * 送信に成功した際に呼び出される
         * @param mPostTask 送信に成功した出席データ
         */
        void onPostSuccess(Attendance mPostTask);

        /**
         * 送信に失敗した際に呼び出される
         * @param mPostTask 送信に失敗した出席データ
         */
        void onPostFailure(Attendance mPostTask);

        /**
         * 送信に終了した際に成功/失敗に関わらず呼び出される
         * @param mPostTask 送信が終了した出席データ
         */
        void onPostEnd(Attendance mPostTask);

        /**
         * 待機中の出席データがなくなった際に呼び出される
         */
        void onAllTaskEnd();
    }
}